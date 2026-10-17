package com.example.kaizenkanban.domain.usecase

import com.example.kaizenkanban.domain.model.Board
import com.example.kaizenkanban.domain.model.Column
import com.example.kaizenkanban.domain.model.RecurringDefaults
import com.example.kaizenkanban.domain.model.RecurringRhythm
import com.example.kaizenkanban.domain.model.RecurringTemplate
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.domain.model.TaskCardBody
import com.example.kaizenkanban.domain.model.TaskCardKind
import com.example.kaizenkanban.domain.model.TaskLink
import com.example.kaizenkanban.domain.model.TaskLinkGraph
import com.example.kaizenkanban.domain.model.TaskReminderDefaults
import com.example.kaizenkanban.domain.repository.KanbanRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar
import java.util.UUID

data class RecurringTarget(
    val boardId: String,
    val columnId: String
)

object RecurringTargetResolver {
    fun resolveDefault(
        projectId: String,
        boards: List<Board>,
        columns: List<Column>
    ): RecurringTarget? {
        val projectBoards = boards.filter { it.projectId == projectId && !it.isArchived }
        if (projectBoards.isEmpty()) return null
        val board = projectBoards.find { it.isDefault }
            ?: projectBoards.find {
                it.name.contains("OKR", ignoreCase = true) ||
                    it.name.contains("Лестница целей", ignoreCase = true) ||
                    it.name.contains("Goal ladder", ignoreCase = true)
            }
            ?: projectBoards.first()
        val boardColumns = columns.filter { it.boardId == board.id }.sortedBy { it.position }
        if (boardColumns.isEmpty()) return null
        val column = boardColumns.find { col ->
            val t = col.title.trim().lowercase()
            t == "сделать" || t == "do" || t == "to do" || t == "todo" ||
                t == "сегодня" || t == "today" || t.contains("сегодня") || t.contains("today")
        } ?: boardColumns.first()
        return RecurringTarget(board.id, column.id)
    }
}

/**
 * Spawns today's task instances from enabled recurring templates.
 * Each template time is both the start clock and the reminder clock of one task.
 * Hub create time is [defaultSpawnMinutes] only.
 *
 * Calls from the open app and from the alarm share one lock and read tasks from the
 * database inside it, so a second pass sees cards the first pass just inserted.
 */
class EnsureRecurringInstancesUseCase(
    private val repository: KanbanRepository
) {
    suspend operator fun invoke(
        templates: List<RecurringTemplate>,
        columns: List<Column>,
        now: Long = System.currentTimeMillis(),
        /** Task reminder default (minutes from midnight), used when template has no times / early-create. */
        defaultReminderMinutes: Int = TaskReminderDefaults.DEFAULT_MINUTES,
        /** Settings-only: when instances appear on the hub. */
        defaultSpawnMinutes: Int = RecurringDefaults.DEFAULT_SPAWN_MINUTES,
        /** If true, only create slots whose effective create time has already passed today. */
        onlySlotsReadyByNow: Boolean = true,
        /** Board id → hub marked primary on that board. */
        primaryHubByBoardId: Map<String, String> = emptyMap(),
        /** Start-of-day millis of the last due-today sweep. 0 = never. */
        dueTodaySweptDay: Long = 0L,
        /** Called once when today's sweep has run, including when nothing needed to move. */
        onDueTodaySwept: ((Long) -> Unit)? = null
    ): Int = ensureLock.withLock {
        val tasks = repository.getAllTasks().first()
        val startToday = startOfDay(now)
        val endToday = startToday + DAY_MS - 1
        val todayIsoDow = isoDayOfWeek(now)
        val nowMinutes = minutesOfDay(now)
        val columnIds = columns.map { it.id }.toSet()
        val workingLinks = repository.getAllTaskLinks().first().toMutableList()
        val reminderDefault = defaultReminderMinutes.coerceIn(0, 24 * 60 - 1)
        val spawnDefault = defaultSpawnMinutes.coerceIn(0, 24 * 60 - 1)

        val alignStart = tasks.mapNotNull { task ->
            if (task.recurringTemplateId.isNullOrBlank() || task.startMinutesOfDay != null) return@mapNotNull null
            val minutes = task.reminderMinutesOfDay ?: return@mapNotNull null
            task.copy(startMinutesOfDay = minutes.coerceIn(0, 24 * 60 - 1))
        }
        if (alignStart.isNotEmpty()) repository.updateTasks(alignStart)

        val working = tasks.toMutableList()
        var created = 0
        for (template in templates) {
            if (!template.enabled) continue
            if (template.title.isBlank()) continue
            if (template.targetColumnId !in columnIds) continue

            val activeFrom = effectiveStartsOnDay(template.startsOnDayMillis, template.createdAt)
            val premature = openInstancesBeforeStart(working, template.id, activeFrom)
            if (premature.isNotEmpty()) {
                val prematureIds = premature.map { it.id }.toSet()
                premature.forEach { repository.deleteTask(it.id) }
                working.removeAll { it.id in prematureIds }
                workingLinks.removeAll { it.parentId in prematureIds || it.childId in prematureIds }
            }
            if (startToday < activeFrom) continue

            val shouldCreateToday = when (template.rhythm) {
                RecurringRhythm.DAILY -> true
                RecurringRhythm.WEEKDAYS -> todayIsoDow in template.weekdays
                RecurringRhythm.EVERY_N_DAYS -> {
                    val n = RecurringRhythm.intervalDays(template.timesPerWeek)
                    val days = ((startToday - activeFrom) / DAY_MS).coerceAtLeast(0L)
                    days % n == 0L
                }
                else -> false
            }
            if (!shouldCreateToday) continue

            val times = RecurringDefaults.effectiveReminderTimes(
                stored = template.reminderTimesOfDay,
                reminderDefault = reminderDefault
            )

            val todayInstances = working.filter { task ->
                task.recurringTemplateId == template.id &&
                    task.dueDate != null &&
                    task.dueDate in startToday..endToday
            }
            val minutesToCreate = uncoveredTemplateSlots(
                templateMinutes = times,
                todayInstances = todayInstances,
                dayStartMillis = startToday,
                skippedKeys = template.skippedOccurrenceKeys.toSet()
            ).filter { reminderMinutes ->
                if (!onlySlotsReadyByNow) return@filter true
                val createAt = effectiveCreateMinutes(spawnDefault, reminderMinutes)
                nowMinutes >= createAt
            }

            val groupId = template.hubGroupId?.takeIf { it.isNotBlank() }
            for (reminderMinutes in minutesToCreate) {
                // dueDate clock encodes the slot for matching; create time is settings-only.
                // Start and reminder are the same clock. "Remind before" stays a lead on the template.
                val dueMillis = startToday + reminderMinutes * 60_000L
                val taskId = UUID.randomUUID().toString()
                val columnId = template.targetColumnId
                val position = working
                    .filter { it.columnId == columnId }
                    .maxOfOrNull { it.position }
                    ?.plus(1)
                    ?: 0
                val task = Task(
                    id = taskId,
                    columnId = columnId,
                    title = template.title.trim(),
                    description = "",
                    position = position,
                    categoryId = null,
                    dueDate = dueMillis,
                    deadlineAt = EnsureRecurringInstancesUseCase.instanceDeadlineAt(
                        dueMillis,
                        template.deadlineOffsetDays,
                        template.deadlineOffsetHours
                    ),
                    createdAt = now,
                    startMinutesOfDay = reminderMinutes,
                    reminderMinutesOfDay = reminderMinutes,
                    eisenhowerQuadrant = template.eisenhowerQuadrant,
                    showEisenhowerButtons = template.showEisenhowerButtons ||
                        template.eisenhowerQuadrant != null,
                    recurringTemplateId = template.id,
                    complexity = template.complexity,
                    timerMinutes = template.timerMinutes?.takeIf { it > 0 },
                    hubGroupId = groupId,
                    cardType = TaskCardKind.normalize(template.cardType),
                    cardPayload = TaskCardBody.decode(template.cardType, template.cardPayload)
                        .cleared()
                        .encode()
                )
                repository.insertTask(task)
                working += task
                if (groupId != null) {
                    coalesceColumnGroup(working, columnId, groupId)
                }
                template.linkParentIds.distinct().forEach { parentId ->
                    if (parentId == taskId) return@forEach
                    if (TaskLinkGraph.wouldCreateCycle(workingLinks, parentId, taskId)) return@forEach
                    val link = TaskLink(parentId = parentId, childId = taskId, createdAt = now)
                    repository.insertTaskLink(link)
                    workingLinks += link
                }
                template.linkChildIds.distinct().forEach { childId ->
                    if (childId == taskId) return@forEach
                    if (TaskLinkGraph.wouldCreateCycle(workingLinks, taskId, childId)) return@forEach
                    val link = TaskLink(parentId = taskId, childId = childId, createdAt = now)
                    repository.insertTaskLink(link)
                    workingLinks += link
                }
                created++
            }
        }
        if (
            shouldSweepDueToday(
                now = now,
                spawnMinutes = spawnDefault,
                sweptDayStart = dueTodaySweptDay,
                onlySlotsReadyByNow = onlySlotsReadyByNow,
                hasPrimaryHub = primaryHubByBoardId.isNotEmpty()
            )
        ) {
            val updates = planDueTodayPrimaryMoves(
                tasks = working,
                columns = columns,
                primaryHubByBoardId = primaryHubByBoardId,
                dayStart = startToday,
                dayEnd = endToday
            )
            if (updates.isNotEmpty()) {
                repository.updateTasks(updates)
                val byId = updates.associateBy { it.id }
                for (i in working.indices) {
                    val updated = byId[working[i].id] ?: continue
                    working[i] = updated
                }
            }
            onDueTodaySwept?.invoke(startToday)
        }
        return created
    }

    /** Pull members of [groupId] into one block where the group already starts. */
    private suspend fun coalesceColumnGroup(
        working: MutableList<Task>,
        columnId: String,
        groupId: String
    ) {
        val home = working.filter { it.columnId == columnId }.sortedBy { it.position }
        val memberIds = home.filter { it.hubGroupId == groupId }.map { it.id }.toSet()
        val packed = packGroupContiguous(home, memberIds)
        if (packed.map { it.id } == home.map { it.id }) return
        val renumbered = packed.mapIndexed { index, task -> task.copy(position = index) }
        repository.updateTasks(renumbered)
        val byId = renumbered.associateBy { it.id }
        for (i in working.indices) {
            val updated = byId[working[i].id] ?: continue
            working[i] = updated
        }
    }

    private fun packGroupContiguous(ordered: List<Task>, memberIds: Set<String>): List<Task> {
        if (memberIds.size < 2) return ordered
        val selectedOrdered = ordered.filter { it.id in memberIds }
        if (selectedOrdered.size < 2) return ordered
        val firstIdx = ordered.indexOfFirst { it.id in memberIds }
        if (firstIdx < 0) return ordered
        val before = ordered.take(firstIdx).filter { it.id !in memberIds }
        val after = ordered.drop(firstIdx).filter { it.id !in memberIds }
        return before + selectedOrdered + after
    }

    companion object {
        private val ensureLock = Mutex()
        private const val DAY_MS = 24L * 60 * 60 * 1000
        private const val SKIP_RETAIN_DAYS = 60

        fun occurrenceKey(dayStartMillis: Long, minutes: Int): String =
            "$dayStartMillis:${minutes.coerceIn(0, 24 * 60 - 1)}"

        /**
         * When to create a hub instance for a reminder slot.
         * If the reminder is earlier than settings spawn, create at reminder so the alarm can fire.
         */
        fun effectiveCreateMinutes(spawnMinutes: Int, reminderMinutes: Int): Int {
            val spawn = spawnMinutes.coerceIn(0, 24 * 60 - 1)
            val reminder = reminderMinutes.coerceIn(0, 24 * 60 - 1)
            return if (reminder < spawn) reminder else spawn
        }

        fun minutesOfDay(now: Long): Int =
            Calendar.getInstance().apply { timeInMillis = now }.let {
                it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
            }

        /** First local day this template may spawn instances. */
        fun effectiveStartsOnDay(startsOnDayMillis: Long, createdAt: Long): Long =
            startOfDay(if (startsOnDayMillis > 0L) startsOnDayMillis else createdAt)

        /** Open cards spawned before the template's start day. Completed and not-done stay. */
        fun openInstancesBeforeStart(
            tasks: List<Task>,
            templateId: String,
            activeFrom: Long
        ): List<Task> = tasks.filter { task ->
            task.recurringTemplateId == templateId &&
                !task.isCompleted &&
                !task.isNotDone &&
                !task.isBoardArchived &&
                task.dueDate != null &&
                startOfDay(task.dueDate) < activeFrom
        }

        /** Deadline millis from an instance start, or null when both offsets are empty. */
        fun instanceDeadlineAt(startMillis: Long, offsetDays: Int?, offsetHours: Int?): Long? {
            val days = (offsetDays ?: 0).coerceAtLeast(0)
            val hours = (offsetHours ?: 0).coerceAtLeast(0)
            if (days == 0 && hours == 0) return null
            return startMillis + days * DAY_MS + hours * 60L * 60L * 1000L
        }

        /**
         * Minutes-of-day used to match an instance to a template reminder slot.
         * Prefer dueDate clock (slot identity); then custom reminder.
         */
        fun instanceMinutes(task: Task): Int {
            val due = task.dueDate
            if (due != null) {
                return Calendar.getInstance().apply { timeInMillis = due }.let {
                    it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
                }
            }
            task.reminderMinutesOfDay?.let { return it.coerceIn(0, 24 * 60 - 1) }
            return TaskReminderDefaults.DEFAULT_MINUTES
        }

        /**
         * Template spawn slots that still need a new task today.
         * Exact minute matches first; leftover same-day instances (e.g. user edited
         * due time) still cover a slot so ensure does not spawn duplicates.
         */
        fun uncoveredTemplateSlots(
            templateMinutes: List<Int>,
            todayInstances: List<Task>,
            dayStartMillis: Long,
            skippedKeys: Set<String>
        ): List<Int> {
            val pool = todayInstances.toMutableList()
            val needCreate = mutableListOf<Int>()
            for (minutes in templateMinutes.map { it.coerceIn(0, 24 * 60 - 1) }.distinct().sorted()) {
                val skipKey = occurrenceKey(dayStartMillis, minutes)
                if (skipKey in skippedKeys) continue

                val exactIdx = pool.indexOfFirst { instanceMinutes(it) == minutes }
                if (exactIdx >= 0) {
                    pool.removeAt(exactIdx)
                    continue
                }
                if (pool.isNotEmpty()) {
                    pool.removeAt(0)
                    continue
                }
                needCreate += minutes
            }
            return needCreate
        }

        /** Key for the day/spawn slot this instance occupies, or null if undated. */
        fun occurrenceKeyForTask(task: Task): String? {
            val due = task.dueDate ?: return null
            val dayStart = startOfDay(due)
            val minutes = instanceMinutes(task)
            return occurrenceKey(dayStart, minutes)
        }

        fun pruneSkippedKeys(keys: List<String>, now: Long = System.currentTimeMillis()): List<String> {
            val cutoff = startOfDay(now) - SKIP_RETAIN_DAYS * DAY_MS
            return keys.filter { key ->
                val dayPart = key.substringBefore(':').toLongOrNull() ?: return@filter false
                dayPart >= cutoff
            }.distinct()
        }

        fun withSkippedOccurrence(template: RecurringTemplate, key: String): RecurringTemplate {
            val pruned = pruneSkippedKeys(template.skippedOccurrenceKeys + key)
            return template.copy(skippedOccurrenceKeys = pruned)
        }

        fun withoutSkippedOccurrence(template: RecurringTemplate, key: String): RecurringTemplate =
            template.copy(skippedOccurrenceKeys = template.skippedOccurrenceKeys.filterNot { it == key })

        /**
         * Active hubs the user marked primary. Archived boards and stale ids are dropped.
         */
        fun resolvePrimaryHubs(
            boards: List<Board>,
            columns: List<Column>,
            storedHubId: (boardId: String) -> String?
        ): Map<String, String> {
            val activeBoardIds = boards.filter { !it.isArchived }.map { it.id }.toSet()
            return columns
                .filter { it.boardId in activeBoardIds }
                .groupBy { it.boardId }
                .mapNotNull { (boardId, cols) ->
                    val hubId = storedHubId(boardId)?.takeIf { id -> cols.any { it.id == id } }
                        ?: return@mapNotNull null
                    boardId to hubId
                }
                .toMap()
        }

        /**
         * Due-today sweep runs once, at the same clock as recurring creation:
         * the settings spawn time (or immediately when slots are not gated by now).
         */
        fun shouldSweepDueToday(
            now: Long,
            spawnMinutes: Int,
            sweptDayStart: Long,
            onlySlotsReadyByNow: Boolean,
            hasPrimaryHub: Boolean
        ): Boolean {
            if (!hasPrimaryHub) return false
            if (sweptDayStart == startOfDay(now)) return false
            if (!onlySlotsReadyByNow) return true
            return minutesOfDay(now) >= spawnMinutes.coerceIn(0, 24 * 60 - 1)
        }

        /**
         * Home-column moves for open tasks whose due day is [dayStart].
         * Recurring instances stay on the hub their template chose.
         * Returns every task whose column or position changes, or empty when nothing moves.
         */
        fun planDueTodayPrimaryMoves(
            tasks: List<Task>,
            columns: List<Column>,
            primaryHubByBoardId: Map<String, String>,
            dayStart: Long,
            dayEnd: Long
        ): List<Task> {
            val columnBoard = columns.associate { it.id to it.boardId }
            val validPrimary = primaryHubByBoardId.filter { (boardId, hubId) ->
                columnBoard[hubId] == boardId
            }
            if (validPrimary.isEmpty()) return emptyList()
            val movers = tasks.filter { task ->
                if (task.isCompleted || task.isHidden || task.isBoardArchived) return@filter false
                if (!task.recurringTemplateId.isNullOrBlank()) return@filter false
                val due = task.dueDate ?: return@filter false
                if (due !in dayStart..dayEnd) return@filter false
                val boardId = columnBoard[task.columnId] ?: return@filter false
                val primary = validPrimary[boardId] ?: return@filter false
                task.columnId != primary
            }
            if (movers.isEmpty()) return emptyList()
            val moverIds = movers.map { it.id }.toSet()
            val updates = mutableListOf<Task>()
            val byTarget = movers.groupBy { task ->
                validPrimary.getValue(columnBoard.getValue(task.columnId))
            }
            for ((targetId, incoming) in byTarget) {
                val staying = tasks.filter { it.columnId == targetId && it.id !in moverIds }
                val active = staying.filter { !it.isCompleted && !it.isHidden }.sortedBy { it.position }
                val hidden = staying.filter { !it.isCompleted && it.isHidden }.sortedBy { it.position }
                val completed = staying.filter { it.isCompleted }.sortedBy { it.position }
                val arrived = incoming
                    .sortedWith(compareBy<Task> { it.dueDate ?: Long.MAX_VALUE }.thenBy { it.position }.thenBy { it.id })
                    .map { task ->
                        task.copy(
                            columnId = targetId,
                            linkedColumnIds = task.linkedColumnIds.filter { it != targetId }
                        )
                    }
                updates += renumberPositions(active + arrived + hidden + completed)
            }
            for (sourceId in movers.map { it.columnId }.toSet()) {
                val remaining = tasks
                    .filter { it.columnId == sourceId && it.id !in moverIds }
                    .sortedBy { it.position }
                val active = remaining.filter { !it.isCompleted && !it.isHidden }
                val hidden = remaining.filter { !it.isCompleted && it.isHidden }
                val completed = remaining.filter { it.isCompleted }
                updates += renumberPositions(active + hidden + completed)
            }
            return updates
        }

        private fun renumberPositions(ordered: List<Task>): List<Task> =
            ordered.mapIndexed { index, task -> task.copy(position = index) }

        fun startOfDay(now: Long): Long =
            Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

        /** 1=Mon … 7=Sun */
        fun isoDayOfWeek(now: Long): Int {
            val cal = Calendar.getInstance().apply { timeInMillis = now }
            return when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                else -> 7
            }
        }
    }
}
