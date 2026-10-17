package com.example.kaizenkanban.domain.commands

import com.example.kaizenkanban.domain.model.Board
import com.example.kaizenkanban.domain.model.Column
import com.example.kaizenkanban.domain.model.RecurringRhythm
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.domain.model.TaskLink
import com.example.kaizenkanban.domain.model.TaskReminderDefaults
import com.example.kaizenkanban.domain.usecase.RecurringTargetResolver
import java.util.Calendar
import java.util.TimeZone

/**
 * Resolves parsed commands against the current project: columns, goals, parents.
 */
object KairosCommandPlanner {

    data class Context(
    val projectId: String,
    val boards: List<Board>,
    val columns: List<Column>,
        val tasks: List<Task>,
    val links: List<TaskLink>,
        val selectedGoalId: String? = null,
        val defaultReminderMinutes: Int = TaskReminderDefaults.DEFAULT_MINUTES,
        val timeZone: TimeZone = TimeZone.getDefault(),
        val nowMillis: Long = System.currentTimeMillis()
    )

    fun plan(parse: KairosParseResult, ctx: Context): KairosPlan {
        val errors = parse.errors.toMutableList()
        val warnings = mutableListOf<String>()
        if (!parse.headerOk) {
            return KairosPlan(emptyList(), emptyList(), errors, warnings)
        }
        if (parse.commands.isEmpty() && errors.isEmpty()) {
            errors += "No commands in block"
            return KairosPlan(emptyList(), emptyList(), errors, warnings)
        }

        val defaultTarget = RecurringTargetResolver.resolveDefault(
            ctx.projectId, ctx.boards, ctx.columns
        )
        if (defaultTarget == null) {
            errors += "Project has no board/column to place tasks"
            return KairosPlan(emptyList(), emptyList(), errors, warnings)
        }

        val actions = mutableListOf<KairosPlannedAction>()
        val preview = mutableListOf<String>()

        // localKey → title for parent resolution within the batch
        val localTitles = mutableMapOf<String, String>()
        // special: "goal" → last declared goal localKey
        var currentGoalKey: String? = null
        val goalKeys = mutableListOf<String>()

        // Pre-register titles for forward refs within batch
        parse.commands.forEach { cmd ->
            when (cmd) {
                is KairosCommand.Goal -> localTitles[cmd.localKey] = cmd.title
                is KairosCommand.Task -> localTitles[cmd.localKey] = cmd.title
                is KairosCommand.Habit -> localTitles[cmd.localKey] = cmd.title
                is KairosCommand.Link -> Unit
            }
        }

        fun resolveColumn(hint: String?): Pair<String, String> {
            // returns columnId to boardId via column
            if (!hint.isNullOrBlank()) {
                val h = hint.trim()
                val col = ctx.columns.firstOrNull {
                    it.title.equals(h, ignoreCase = true) ||
                        it.title.contains(h, ignoreCase = true)
                }
                if (col != null) return col.id to col.boardId
                val board = ctx.boards.firstOrNull {
                    it.projectId == ctx.projectId &&
                        (it.name.equals(h, ignoreCase = true) || it.name.contains(h, ignoreCase = true))
                }
                if (board != null) {
                    val first = ctx.columns.filter { it.boardId == board.id }.minByOrNull { it.position }
                    if (first != null) return first.id to board.id
                }
                warnings += "Hub/column «$h» not found — using default"
            }
            return defaultTarget.columnId to defaultTarget.boardId
        }

        fun resolveParent(ref: String?): Pair<String?, String?> {
            // returns (localKey, existingId)
            if (ref.isNullOrBlank()) return null to null
            val r = ref.trim()
            if (r.equals("goal", ignoreCase = true)) {
                val gk = currentGoalKey
                if (gk != null) return gk to null
                val sel = ctx.selectedGoalId
                if (sel != null) return null to sel
                errors += "parent: goal — no goal in block and none selected"
                return null to null
            }
            // local key?
            if (r in localTitles) return r to null
            // match by title among new locals
            val byLocalTitle = localTitles.entries.firstOrNull { it.value.equals(r, ignoreCase = true) }
            if (byLocalTitle != null) return byLocalTitle.key to null
            // existing task in project
            val existing = ctx.tasks.firstOrNull { it.title.equals(r, ignoreCase = true) }
            if (existing != null) return null to existing.id
            errors += "Parent not found: «$r»"
            return null to null
        }

        fun attachParent(ref: String?): Pair<String?, String?> {
            if (!ref.isNullOrBlank()) return resolveParent(ref)
            if (currentGoalKey != null) return currentGoalKey to null
            val sel = ctx.selectedGoalId
            if (sel != null) return null to sel
            return null to null
        }

        fun dueMillis(ymd: String?): Long? {
            if (ymd.isNullOrBlank()) return null
            val cal = Calendar.getInstance(ctx.timeZone).apply {
                timeInMillis = ctx.nowMillis
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            when (ymd.trim().lowercase().replace('ё', 'е')) {
                "today", "сегодня" -> return cal.timeInMillis
                "tomorrow", "завтра" -> {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    return cal.timeInMillis
                }
            }
            val parts = ymd.split('-').mapNotNull { it.toIntOrNull() }
            if (parts.size != 3) return null
            cal.set(Calendar.YEAR, parts[0])
            cal.set(Calendar.MONTH, parts[1] - 1)
            cal.set(Calendar.DAY_OF_MONTH, parts[2])
            return cal.timeInMillis
        }

        fun remindOrDefault(due: Long?, remind: Int?): Int? {
            if (due == null) return null
            return remind ?: ctx.defaultReminderMinutes
        }

        for (cmd in parse.commands) {
            when (cmd) {
                is KairosCommand.Goal -> {
                    val reuseId = when {
                        !cmd.existingId.isNullOrBlank() -> {
                            if (ctx.tasks.none { it.id == cmd.existingId }) {
                                errors += "Goal id not found: ${cmd.existingId}"
                                null
                            } else cmd.existingId
                        }
                        !cmd.createNew -> ctx.tasks.firstOrNull {
                            it.isGoal && it.title.equals(cmd.title, ignoreCase = true)
                        }?.id
                        else -> null
                    }
                    if (reuseId == null && !cmd.existingId.isNullOrBlank() && !cmd.createNew) {
                        continue
                    }
                    if (reuseId != null && !cmd.createNew) {
                        actions += KairosPlannedAction.ReuseGoal(cmd.localKey, reuseId, cmd.title)
                        preview += "Goal «${cmd.title}» — use existing"
                        currentGoalKey = cmd.localKey
                        goalKeys += cmd.localKey
                    } else {
                        val (columnId, _) = resolveColumn(cmd.hubOrColumn)
                        val due = dueMillis(cmd.dueDateYmd)
                        val deadline = dueMillis(cmd.deadlineYmd)
                        val (cardType, cardPayload) = encodeKairosCard(cmd.cardType, cmd.cardLines)
                        actions += KairosPlannedAction.CreateGoal(
                            localKey = cmd.localKey,
                            title = cmd.title,
                            columnId = columnId,
                            dueDateMillis = due,
                            reminderMinutesOfDay = remindOrDefault(due, cmd.remindMinutes ?: cmd.startMinutes),
                            complexity = cmd.impact,
                            eisenhowerQuadrant = cmd.eisenhower,
                            startMinutesOfDay = cmd.startMinutes,
                            deadlineAtMillis = deadline,
                            timerMinutes = cmd.timerMinutes?.takeIf { it > 0 },
                            cardType = cardType,
                            cardPayload = cardPayload
                        )
                        val bits = buildList {
                            add("create")
                            due?.let { add("start ${cmd.dueDateYmd}") }
                            cmd.startMinutes?.let { add("at ${formatMin(it)}") }
                            cmd.remindMinutes?.let { add("remind ${formatMin(it)}") }
                                ?: due?.let { add("remind ${formatMin(cmd.startMinutes ?: ctx.defaultReminderMinutes)}") }
                            deadline?.let { add("finish ${cmd.deadlineYmd}") }
                            cmd.timerMinutes?.takeIf { it > 0 }?.let { add("timer ${it}m") }
                            cmd.eisenhower?.let { add(it) }
                            cmd.impact?.let { add("impact $it/5") }
                            cardPreview(cmd.cardType, cmd.cardLines)?.let { add(it) }
                        }
                        preview += "Goal «${cmd.title}» — ${bits.joinToString(" · ")}"
                        currentGoalKey = cmd.localKey
                        goalKeys += cmd.localKey
                    }
                }
                is KairosCommand.Task -> {
                    val (columnId, _) = resolveColumn(cmd.hubOrColumn)
                    val (pLocal, pExist) = attachParent(cmd.parentRef)
                    val due = dueMillis(cmd.dueDateYmd)
                    val deadline = dueMillis(cmd.deadlineYmd)
                    val (cardType, cardPayload) = encodeKairosCard(cmd.cardType, cmd.cardLines)
                    actions += KairosPlannedAction.CreateTask(
                        localKey = cmd.localKey,
                        title = cmd.title,
                        columnId = columnId,
                        parentLocalKey = pLocal,
                        parentExistingId = pExist,
                        dueDateMillis = due,
                        reminderMinutesOfDay = remindOrDefault(due, cmd.remindMinutes ?: cmd.startMinutes),
                        complexity = cmd.impact,
                        eisenhowerQuadrant = cmd.eisenhower,
                        isGoal = false,
                        startMinutesOfDay = cmd.startMinutes,
                        deadlineAtMillis = deadline,
                        timerMinutes = cmd.timerMinutes?.takeIf { it > 0 },
                        cardType = cardType,
                        cardPayload = cardPayload
                    )
                    val bits = buildList {
                        add("task")
                        due?.let { add("start ${cmd.dueDateYmd}") }
                        cmd.startMinutes?.let { add("at ${formatMin(it)}") }
                        remindOrDefault(due, cmd.remindMinutes ?: cmd.startMinutes)?.let { add("remind ${formatMin(it)}") }
                        deadline?.let { add("finish ${cmd.deadlineYmd}") }
                        cmd.timerMinutes?.takeIf { it > 0 }?.let { add("timer ${it}m") }
                        cmd.eisenhower?.let { add(it) }
                        cmd.impact?.let { add("impact $it/5") }
                        cardPreview(cmd.cardType, cmd.cardLines)?.let { add(it) }
                        when {
                            pLocal != null -> add("under ${localTitles[pLocal] ?: pLocal}")
                            pExist != null -> add("under existing")
                        }
                    }
                    preview += "Task «${cmd.title}» — ${bits.joinToString(" · ")}"
                }
                is KairosCommand.Habit -> {
                    val (columnId, boardId) = resolveColumn(cmd.hubOrColumn)
                    val (pLocal, pExist) = attachParent(cmd.parentRef)
                    val times = cmd.remindMinutesList.ifEmpty { listOf(ctx.defaultReminderMinutes) }
                    val rhythm = when (cmd.rhythm) {
                        "weekdays" -> RecurringRhythm.WEEKDAYS
                        "every_n_days" -> RecurringRhythm.EVERY_N_DAYS
                        else -> RecurringRhythm.DAILY
                    }
                    if (rhythm == RecurringRhythm.WEEKDAYS && cmd.weekdays.isEmpty()) {
                        errors += "habit «${cmd.title}»: weekdays rhythm needs days: 1,3,5"
                        continue
                    }
                    val startsOn = dueMillis(cmd.dueDateYmd) ?: 0L
                    val (cardType, cardPayload) = encodeKairosCard(cmd.cardType, cmd.cardLines)
                    actions += KairosPlannedAction.CreateHabit(
                        localKey = cmd.localKey,
                        title = cmd.title,
                        targetBoardId = boardId,
                        targetColumnId = columnId,
                        parentLocalKey = pLocal,
                        parentExistingId = pExist,
                        rhythm = rhythm,
                        weekdays = if (rhythm == RecurringRhythm.WEEKDAYS) cmd.weekdays else emptySet(),
                        intervalDays = if (rhythm == RecurringRhythm.EVERY_N_DAYS) {
                            RecurringRhythm.intervalDays(cmd.intervalDays)
                        } else null,
                        reminderTimesOfDay = times,
                        complexity = cmd.impact,
                        eisenhowerQuadrant = cmd.eisenhower,
                        startsOnDayMillis = startsOn,
                        deadlineOffsetDays = cmd.deadlineOffsetDays?.takeIf { it > 0 },
                        deadlineOffsetHours = cmd.deadlineOffsetHours?.takeIf { it > 0 },
                        timerMinutes = cmd.timerMinutes?.takeIf { it > 0 },
                        cardType = cardType,
                        cardPayload = cardPayload
                    )
                    val habitBits = buildList {
                        add(rhythm)
                        add("reminds ${times.joinToString { formatMin(it) }}")
                        cardPreview(cmd.cardType, cmd.cardLines)?.let { add(it) }
                        cmd.timerMinutes?.takeIf { it > 0 }?.let { add("timer ${it}m") }
                        cmd.dueDateYmd?.let { add("from $it") }
                    }
                    preview += "Habit «${cmd.title}» — ${habitBits.joinToString(" · ")}"
                }
                is KairosCommand.Link -> {
                    val (pLocal, pExist) = resolveParent(cmd.parentRef)
                    val (cLocal, cExist) = resolveParent(cmd.childRef)
                    if ((pLocal == null && pExist == null) || (cLocal == null && cExist == null)) {
                        continue
                    }
                    actions += KairosPlannedAction.CreateLink(pLocal, pExist, cLocal, cExist)
                    preview += "Link «${cmd.parentRef}» → «${cmd.childRef}»"
                }
            }
        }

        return KairosPlan(actions, preview, errors, warnings)
    }

    private fun cardPreview(type: String, lines: List<KairosCardLine>): String? {
        if (type == "plain" || type.isBlank()) return null
        val label = when (type) {
            "workout" -> "workout"
            "shop" -> "shopping"
            "meet" -> "contact"
            else -> type
        }
        return if (lines.isEmpty()) label else "$label · ${lines.size} lines"
    }

    private fun formatMin(minutes: Int): String {
        val m = minutes.coerceIn(0, 24 * 60 - 1)
        return String.format("%02d:%02d", m / 60, m % 60)
    }
}
