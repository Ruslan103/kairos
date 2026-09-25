package com.example.kaizenkanban.domain.stats

import com.example.kaizenkanban.domain.model.RecurringRhythm
import com.example.kaizenkanban.domain.model.RecurringTemplate
import com.example.kaizenkanban.domain.model.StatsJournalEntry
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.domain.model.TaskLink
import com.example.kaizenkanban.domain.model.TaskLinkGraph
import com.example.kaizenkanban.domain.model.TaskWorkflow
import java.util.Calendar
import kotlin.math.roundToInt

enum class StatsPeriod {
    NOW,
    DAY,
    WEEK,
    MONTH,
    MONTHS_3,
    MONTHS_6,
    YEAR
}

data class PeriodWindow(
    val startMillis: Long,
    val endMillis: Long,
    val dayCount: Int
)

data class GoalProgress(
    val goalId: String,
    val goalTitle: String,
    /** May be negative when not_done penalties outweigh completions. */
    val progress: Float,
    val periodDelta: Float,
    val childCount: Int,
    val childWeightSum: Int,
    val isArchived: Boolean = false
)

data class DailyActivityPoint(
    val dayStart: Long,
    val towardCount: Int,
    val triviaCount: Int
)

data class ProgressPoint(
    val dayStart: Long,
    /** Goal progress P at end of day (−1…1). */
    val progress: Float,
    /** Forecast ˆp at end of day (0…1), null if no goal context. */
    val forecast: Float? = null
)

data class ProjectStatsSnapshot(
    val period: StatsPeriod,
    val window: PeriodWindow,
    val goals: List<GoalProgress>,
    val selectedGoal: GoalProgress?,
    val towardGoalsCount: Int,
    val towardGoalsWeight: Int,
    val triviaCount: Int,
    val triviaWeight: Int,
    val recurringFact: Int,
    val recurringPlan: Int,
    val ratedDone: Int,
    val doneInPeriod: Int,
    val unratedDoneIds: List<String>,
    val hatP: Float?,
    /** Heuristic 0…1 “chance to finish” for the selected goal (not a true probability). */
    val chanceToComplete: Float?,
    /**
     * Overall pace for the period (not goal-specific):
     * share of done weight toward any goal + rhythm − trivia.
     */
    val periodHatP: Float?,
    /** Share of done weight that went toward any goal (0…1); null if nothing done. */
    val towardShare: Float?,
    val rhythmShare: Float,
    val triviaShare: Float,
    /** How many percentage points trivia subtracts from the model (0.20 × O). */
    val triviaPenalty: Float,
    /** How many percentage points rhythm adds (0.15 × R). */
    val rhythmBonus: Float,
    val dailyActivity: List<DailyActivityPoint>,
    val goalProgressSeries: List<ProgressPoint>
)

object StatsPeriodWindows {
    fun window(
        period: StatsPeriod,
        now: Long = System.currentTimeMillis(),
        epochMillis: Long = 0L
    ): PeriodWindow {
        val end = now
        val startOfToday = startOfLocalDay(now)
        val rawStart = when (period) {
            StatsPeriod.NOW -> epochMillis.coerceAtMost(startOfToday)
            StatsPeriod.DAY -> startOfToday
            StatsPeriod.WEEK -> startOfLocalWeek(now)
            StatsPeriod.MONTH -> startOfLocalMonth(now)
            StatsPeriod.MONTHS_3 -> addCalendarMonths(startOfLocalMonth(now), -2)
            StatsPeriod.MONTHS_6 -> addCalendarMonths(startOfLocalMonth(now), -5)
            StatsPeriod.YEAR -> startOfLocalYear(now)
        }
        val start = maxOf(rawStart, epochMillis)
        val days = (((end - start) / DAY_MS).toInt() + 1).coerceAtLeast(1)
        return PeriodWindow(startMillis = start, endMillis = end, dayCount = days)
    }

    fun startOfLocalDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /** Monday 00:00 of the local week containing [millis]. */
    fun startOfLocalWeek(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = startOfLocalDay(millis)
            firstDayOfWeek = Calendar.MONDAY
            val dow = get(Calendar.DAY_OF_WEEK)
            val daysFromMonday = when (dow) {
                Calendar.MONDAY -> 0
                Calendar.SUNDAY -> 6
                else -> (dow - Calendar.MONDAY + 7) % 7
            }
            add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        }
        return cal.timeInMillis
    }

    fun startOfLocalMonth(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = startOfLocalDay(millis)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.timeInMillis
    }

    fun startOfLocalYear(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = startOfLocalDay(millis)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.timeInMillis
    }

    private fun addCalendarMonths(startOfDay: Long, months: Int): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = startOfDay }
        cal.add(Calendar.MONTH, months)
        return cal.timeInMillis
    }

    const val DAY_MS = 24L * 60 * 60 * 1000
}

object GoalProgressCalculator {
    /**
     * Not-done is the inverse of an unmarked done (+1): the step’s weight
     * (impact × importance) fully subtracts from goal progress.
     */
    const val NOT_DONE_SCORE = -1f

    fun leafScore(task: Task): Float = when {
        task.statsExcluded -> 0f
        task.isNotDone -> NOT_DONE_SCORE
        task.workflowStatus == TaskWorkflow.NOT_DONE -> NOT_DONE_SCORE
        !task.isCompleted -> 0f
        task.completionQuality == null -> 1f
        else -> (task.completionQuality.coerceIn(1, 5) / 5f)
    }

    /** Completed before the goal’s stats epoch no longer affect that goal’s progress. */
    fun countsInGoalProgress(task: Task, goalEpochMillis: Long?): Boolean {
        if (goalEpochMillis == null || goalEpochMillis <= 0L) return true
        if (task.statsExcluded) return false
        if (task.isCompleted) {
            val at = task.completedAt ?: return false
            return at >= goalEpochMillis
        }
        return true
    }

    fun countsJournalInGoalProgress(entry: StatsJournalEntry, goalEpochMillis: Long?): Boolean {
        if (goalEpochMillis == null || goalEpochMillis <= 0L) return true
        return entry.eventAt >= goalEpochMillis
    }

    fun journalGhostsForGoal(
        goalId: String,
        tasksById: Map<String, Task>,
        journal: List<StatsJournalEntry>,
        goalEpochMillis: Long?
    ): List<StatsJournalEntry> =
        journal.filter { entry ->
            goalId in entry.relatedGoalIds &&
                entry.sourceTaskId !in tasksById &&
                countsJournalInGoalProgress(entry, goalEpochMillis)
        }

    fun nodeScore(
        taskId: String,
        tasksById: Map<String, Task>,
        links: List<TaskLink>,
        memo: MutableMap<String, Float> = mutableMapOf(),
        goalEpochMillis: Long? = null,
        journal: List<StatsJournalEntry> = emptyList()
    ): Float {
        memo[taskId]?.let { return it }
        val kids = TaskLinkGraph.childrenOf(links)[taskId].orEmpty()
        val score = if (kids.isEmpty()) {
            val ghosts = journalGhostsForGoal(taskId, tasksById, journal, goalEpochMillis)
            if (ghosts.isNotEmpty()) {
                progressOf(taskId, tasksById, links, memo, goalEpochMillis, journal)
            } else {
                val task = tasksById[taskId]
                when {
                    task == null -> 0f
                    !countsInGoalProgress(task, goalEpochMillis) -> 0f
                    else -> leafScore(task)
                }
            }
        } else {
            progressOf(taskId, tasksById, links, memo, goalEpochMillis, journal)
        }
        memo[taskId] = score
        return score
    }

    fun progressOf(
        goalId: String,
        tasksById: Map<String, Task>,
        links: List<TaskLink>,
        scoreMemo: MutableMap<String, Float> = mutableMapOf(),
        goalEpochMillis: Long? = tasksById[goalId]?.goalStatsEpochMillis,
        journal: List<StatsJournalEntry> = emptyList()
    ): Float {
        val kids = TaskLinkGraph.childrenOf(links)[goalId].orEmpty()
        val ghosts = journalGhostsForGoal(goalId, tasksById, journal, goalEpochMillis)
        if (kids.isEmpty() && ghosts.isEmpty()) {
            val task = tasksById[goalId] ?: return 0f
            if (!countsInGoalProgress(task, goalEpochMillis)) return 0f
            return leafScore(task)
        }
        val weightMemo = mutableMapOf<String, Int>()
        var num = 0.0
        var den = 0.0
        kids.forEach { childId ->
            val child = tasksById[childId]
            val childKids = TaskLinkGraph.childrenOf(links)[childId].orEmpty()
            val childGhosts = journalGhostsForGoal(childId, tasksById, journal, goalEpochMillis)
            if (childKids.isEmpty() && childGhosts.isEmpty() && child != null &&
                !countsInGoalProgress(child, goalEpochMillis)
            ) {
                return@forEach
            }
            val w = TaskLinkGraph.nodeWeight(childId, tasksById, links, weightMemo).toDouble()
            if (w <= 0.0) return@forEach
            val s = nodeScore(childId, tasksById, links, scoreMemo, goalEpochMillis, journal).toDouble()
            num += w * s
            den += w
        }
        ghosts.forEach { entry ->
            val w = entry.weight.toDouble().coerceAtLeast(1.0)
            num += w * entry.leafScore.toDouble()
            den += w
        }
        if (den <= 0.0) return 0f
        return (num / den).toFloat().coerceIn(-1f, 1f)
    }

    fun periodDelta(
        goalId: String,
        tasksById: Map<String, Task>,
        links: List<TaskLink>,
        window: PeriodWindow,
        journal: List<StatsJournalEntry> = emptyList()
    ): Float {
        val epoch = tasksById[goalId]?.goalStatsEpochMillis ?: 0L
        val start = maxOf(window.startMillis, epoch)
        val end = window.endMillis
        if (start > end) return 0f
        val clipped = PeriodWindow(start, end, window.dayCount)
        return periodDeltaUnclipped(goalId, tasksById, links, clipped, journal)
    }

    private fun periodDeltaUnclipped(
        goalId: String,
        tasksById: Map<String, Task>,
        links: List<TaskLink>,
        window: PeriodWindow,
        journal: List<StatsJournalEntry>
    ): Float {
        val kids = TaskLinkGraph.childrenOf(links)[goalId].orEmpty()
        val ghosts = journalGhostsForGoal(
            goalId,
            tasksById,
            journal,
            tasksById[goalId]?.goalStatsEpochMillis
        )
        if (kids.isEmpty() && ghosts.isEmpty()) {
            val task = tasksById[goalId] ?: return 0f
            val at = task.completedAt ?: return 0f
            if (!task.isCompleted || at !in window.startMillis..window.endMillis) return 0f
            return leafScore(task).coerceIn(-1f, 1f)
        }
        val weightMemo = mutableMapOf<String, Int>()
        val denLive = kids.sumOf {
            TaskLinkGraph.nodeWeight(it, tasksById, links, weightMemo)
        }.toFloat()
        val denGhost = ghosts.sumOf { it.weight.coerceAtLeast(1) }.toFloat()
        val den = (denLive + denGhost).coerceAtLeast(1f)
        val gainedLive = kids.sumOf { childId ->
            val task = tasksById[childId] ?: return@sumOf 0.0
            when {
                task.isNotDone -> {
                    TaskLinkGraph.nodeWeight(childId, tasksById, links, weightMemo) *
                        leafScore(task).toDouble()
                }
                task.isCompleted && task.completedAt != null &&
                    task.completedAt in window.startMillis..window.endMillis -> {
                    TaskLinkGraph.nodeWeight(childId, tasksById, links, weightMemo) *
                        leafScore(task).toDouble()
                }
                else -> 0.0
            }
        }.toFloat()
        val gainedGhost = ghosts.sumOf { entry ->
            if (entry.eventAt in window.startMillis..window.endMillis) {
                entry.weight.coerceAtLeast(1) * entry.leafScore.toDouble()
            } else {
                0.0
            }
        }.toFloat()
        return ((gainedLive + gainedGhost) / den).coerceIn(-1f, 1f)
    }

    fun resolveGoals(
        tasks: List<Task>,
        links: List<TaskLink>
    ): List<Task> {
        val tasksById = tasks.associateBy { it.id }
        return tasks
            .filter { it.isGoal && !it.statsExcluded }
            .sortedByDescending { TaskLinkGraph.nodeWeight(it.id, tasksById, links) }
    }
}

object ProjectStatsCalculator {
    fun compute(
        projectTasks: List<Task>,
        links: List<TaskLink>,
        templates: List<RecurringTemplate>,
        period: StatsPeriod,
        epochMillis: Long,
        journal: List<StatsJournalEntry> = emptyList(),
        now: Long = System.currentTimeMillis(),
        selectedGoalId: String? = null
    ): ProjectStatsSnapshot {
        val window = StatsPeriodWindows.window(period, now, epochMillis)
        val activeTasks = projectTasks.filter { !it.statsExcluded }
        val tasksById = activeTasks.associateBy { it.id }
        val projectLinks = links.filter { it.parentId in tasksById && it.childId in tasksById }
        val resolved = GoalProgressCalculator.resolveGoals(activeTasks, projectLinks)
        val rootIds = resolved.map { it.id }.toSet()
        val linkedToGoal = descendantsOf(rootIds, projectLinks)

        val goals = resolved.map { root ->
            val p = GoalProgressCalculator.progressOf(
                root.id, tasksById, projectLinks, journal = journal
            )
            GoalProgress(
                goalId = root.id,
                goalTitle = root.title,
                progress = p,
                periodDelta = GoalProgressCalculator.periodDelta(
                    root.id, tasksById, projectLinks, window, journal
                ),
                childCount = TaskLinkGraph.childrenOf(projectLinks)[root.id].orEmpty().size +
                    GoalProgressCalculator.journalGhostsForGoal(
                        root.id, tasksById, journal, root.goalStatsEpochMillis
                    ).size,
                childWeightSum = TaskLinkGraph.childrenOf(projectLinks)[root.id].orEmpty().sumOf {
                    TaskLinkGraph.nodeWeight(it, tasksById, projectLinks)
                } + GoalProgressCalculator.journalGhostsForGoal(
                    root.id, tasksById, journal, root.goalStatsEpochMillis
                ).sumOf { it.weight.coerceAtLeast(1) },
                isArchived = root.isBoardArchived
            )
        }
        val selected = goals.find { it.goalId == selectedGoalId } ?: goals.firstOrNull()

        val liveDone = activeTasks.filter { task ->
            task.isCompleted &&
                task.completedAt != null &&
                task.completedAt in window.startMillis..window.endMillis
        }
        val journalDone = journal.filter { entry ->
            entry.kind == StatsJournalEntry.KIND_DONE &&
                entry.eventAt in window.startMillis..window.endMillis
        }

        val towardLive = liveDone.filter { it.id in linkedToGoal || it.id in rootIds }
        val triviaLive = liveDone.filter { it.id !in linkedToGoal && it.id !in rootIds }
        val towardJournal = journalDone.filter { it.towardGoal }
        val triviaJournal = journalDone.filter { !it.towardGoal }

        val weightMemo = mutableMapOf<String, Int>()
        fun w(id: String) = TaskLinkGraph.nodeWeight(id, tasksById, projectLinks, weightMemo)

        val towardWeight = towardLive.sumOf { w(it.id) } + towardJournal.sumOf { it.weight }
        val triviaWeight = triviaLive.sumOf { w(it.id) } + triviaJournal.sumOf { it.weight }
        val doneWeight = (towardWeight + triviaWeight).coerceAtLeast(1)
        val towardCount = towardLive.size + towardJournal.size
        val triviaCount = triviaLive.size + triviaJournal.size
        val doneInPeriod = liveDone.size + journalDone.size

        val rated = liveDone.count { it.completionQuality != null } +
            journalDone.count { it.completionQuality != null }
        val unrated = liveDone.filter { it.completionQuality == null }.map { it.id }

        val plan = recurringPlan(templates, window, now)
        val fact = liveDone.count { it.recurringTemplateId != null } +
            journalDone.count { it.recurringTemplateId != null }
        val rhythm = if (plan <= 0) 0f else (fact.toFloat() / plan).coerceIn(0f, 1f)
        val triviaShare = (triviaWeight.toFloat() / doneWeight).coerceIn(0f, 1f)
        val rhythmBonus = 0.15f * rhythm
        val triviaPenalty = 0.20f * triviaShare

        val hatP = selected?.let { g ->
            (g.progress + rhythmBonus - triviaPenalty).coerceIn(0f, 1f)
        }
        val chance = selected?.let { g ->
            chanceToComplete(
                hatP = hatP ?: 0f,
                progress = g.progress,
                periodDelta = g.periodDelta,
                rhythmShare = rhythm,
                triviaShare = triviaShare
            )
        }
        val towardShare = if (doneInPeriod <= 0) {
            null
        } else {
            (towardWeight.toFloat() / doneWeight).coerceIn(0f, 1f)
        }
        val periodHatP = towardShare?.let { share ->
            (share + rhythmBonus - triviaPenalty).coerceIn(0f, 1f)
        }

        val daily = buildDailyActivity(
            liveDone = liveDone,
            journalDone = journalDone,
            linkedToGoal = linkedToGoal,
            rootIds = rootIds,
            window = window
        )
        val series = selected?.let {
            buildProgressSeries(
                goalId = it.goalId,
                tasksById = tasksById,
                links = projectLinks,
                window = window,
                now = now,
                templates = templates,
                liveDone = liveDone,
                journalDone = journalDone,
                linkedToGoal = linkedToGoal,
                rootIds = rootIds
            )
        }.orEmpty()

        return ProjectStatsSnapshot(
            period = period,
            window = window,
            goals = goals,
            selectedGoal = selected,
            towardGoalsCount = towardCount,
            towardGoalsWeight = towardWeight,
            triviaCount = triviaCount,
            triviaWeight = triviaWeight,
            recurringFact = fact,
            recurringPlan = plan,
            ratedDone = rated,
            doneInPeriod = doneInPeriod,
            unratedDoneIds = unrated,
            hatP = hatP,
            chanceToComplete = chance,
            periodHatP = periodHatP,
            towardShare = towardShare,
            rhythmShare = rhythm,
            triviaShare = triviaShare,
            triviaPenalty = triviaPenalty,
            rhythmBonus = rhythmBonus,
            dailyActivity = daily,
            goalProgressSeries = series
        )
    }

    /**
     * Heuristic index (0…1), not a calibrated probability.
     * Combines assessment, momentum this period, rhythm, and low trivia.
     */
    fun chanceToComplete(
        hatP: Float,
        progress: Float,
        periodDelta: Float,
        rhythmShare: Float,
        triviaShare: Float
    ): Float {
        val p = progress.coerceIn(-1f, 1f).coerceAtLeast(0f)
        val momentum = periodDelta.coerceIn(0f, 1f)
        return (
            0.45f * hatP.coerceIn(0f, 1f) +
                0.20f * momentum +
                0.15f * rhythmShare.coerceIn(0f, 1f) +
                0.10f * (1f - triviaShare.coerceIn(0f, 1f)) +
                0.10f * p
            ).coerceIn(0f, 1f)
    }

    private fun buildDailyActivity(
        liveDone: List<Task>,
        journalDone: List<StatsJournalEntry>,
        linkedToGoal: Set<String>,
        rootIds: Set<String>,
        window: PeriodWindow
    ): List<DailyActivityPoint> {
        val buckets = linkedMapOf<Long, Pair<Int, Int>>()
        var day = StatsPeriodWindows.startOfLocalDay(window.startMillis)
        val endDay = StatsPeriodWindows.startOfLocalDay(window.endMillis)
        while (day <= endDay) {
            buckets[day] = 0 to 0
            day += StatsPeriodWindows.DAY_MS
        }
        fun add(at: Long, toward: Boolean) {
            val d = StatsPeriodWindows.startOfLocalDay(at)
            val cur = buckets[d] ?: return
            buckets[d] = if (toward) cur.first + 1 to cur.second else cur.first to cur.second + 1
        }
        liveDone.forEach { t ->
            val at = t.completedAt ?: return@forEach
            add(at, t.id in linkedToGoal || t.id in rootIds)
        }
        journalDone.forEach { e -> add(e.eventAt, e.towardGoal) }
        return buckets.map { (d, p) -> DailyActivityPoint(d, p.first, p.second) }
    }

    private fun buildProgressSeries(
        goalId: String,
        tasksById: Map<String, Task>,
        links: List<TaskLink>,
        window: PeriodWindow,
        now: Long,
        templates: List<RecurringTemplate>,
        liveDone: List<Task>,
        journalDone: List<StatsJournalEntry>,
        linkedToGoal: Set<String>,
        rootIds: Set<String>
    ): List<ProgressPoint> {
        val weightMemo = mutableMapOf<String, Int>()
        fun w(id: String) = TaskLinkGraph.nodeWeight(id, tasksById, links, weightMemo)
        val points = mutableListOf<ProgressPoint>()
        var day = StatsPeriodWindows.startOfLocalDay(window.startMillis)
        val endDay = StatsPeriodWindows.startOfLocalDay(minOf(window.endMillis, now))
        while (day <= endDay) {
            val dayEnd = day + StatsPeriodWindows.DAY_MS - 1
            val virtual = tasksById.mapValues { (_, task) ->
                when {
                    task.isNotDone -> task
                    task.isCompleted && (task.completedAt ?: Long.MAX_VALUE) > dayEnd ->
                        task.copy(
                            isCompleted = false,
                            completedAt = null,
                            completionQuality = null,
                            workflowStatus = TaskWorkflow.OPEN
                        )
                    else -> task
                }
            }
            val journalUpToDay = journalDone.filter { it.eventAt <= dayEnd }
            val p = GoalProgressCalculator.progressOf(
                goalId, virtual, links, journal = journalUpToDay
            )

            val doneToDay = liveDone.filter {
                val at = it.completedAt ?: return@filter false
                at in window.startMillis..dayEnd
            }
            val journalToDay = journalDone.filter { it.eventAt in window.startMillis..dayEnd }
            var towardW = 0
            var triviaW = 0
            doneToDay.forEach { t ->
                val weight = w(t.id)
                if (t.id in linkedToGoal || t.id in rootIds) towardW += weight else triviaW += weight
            }
            journalToDay.forEach { e ->
                if (e.towardGoal) towardW += e.weight else triviaW += e.weight
            }
            val doneW = (towardW + triviaW).coerceAtLeast(1)
            val o = (triviaW.toFloat() / doneW).coerceIn(0f, 1f)
            val subWindow = PeriodWindow(
                startMillis = window.startMillis,
                endMillis = dayEnd,
                dayCount = (((dayEnd - window.startMillis) / StatsPeriodWindows.DAY_MS).toInt() + 1)
                    .coerceAtLeast(1)
            )
            val plan = recurringPlan(templates, subWindow, dayEnd)
            val fact = doneToDay.count { it.recurringTemplateId != null } +
                journalToDay.count { it.recurringTemplateId != null }
            val r = if (plan <= 0) 0f else (fact.toFloat() / plan).coerceIn(0f, 1f)
            val forecast = (p + 0.15f * r - 0.20f * o).coerceIn(0f, 1f)

            points += ProgressPoint(dayStart = day, progress = p, forecast = forecast)
            day += StatsPeriodWindows.DAY_MS
        }
        return points
    }

    private fun descendantsOf(roots: Set<String>, links: List<TaskLink>): Set<String> {
        if (roots.isEmpty()) return emptySet()
        val children = TaskLinkGraph.childrenOf(links)
        val out = mutableSetOf<String>()
        val stack = ArrayDeque(roots)
        val seen = HashSet<String>()
        while (stack.isNotEmpty()) {
            val id = stack.removeLast()
            if (!seen.add(id)) continue
            children[id].orEmpty().forEach { child ->
                out += child
                stack += child
            }
        }
        return out
    }

    private fun recurringPlan(
        templates: List<RecurringTemplate>,
        window: PeriodWindow,
        now: Long
    ): Int {
        val enabled = templates.filter { it.enabled }
        if (enabled.isEmpty()) return 0
        var plan = 0
        var day = StatsPeriodWindows.startOfLocalDay(window.startMillis)
        val endDay = StatsPeriodWindows.startOfLocalDay(window.endMillis.coerceAtMost(now))
        while (day <= endDay) {
            val dow = isoDayOfWeek(day)
            enabled.forEach { template ->
                val times = template.reminderTimesOfDay.ifEmpty { listOf(9 * 60) }.size
                val due = when (template.rhythm) {
                    RecurringRhythm.DAILY -> true
                    RecurringRhythm.WEEKDAYS -> dow in template.weekdays
                    RecurringRhythm.EVERY_N_DAYS -> {
                        val n = (template.timesPerWeek ?: 2).coerceIn(2, 30)
                        val anchor = StatsPeriodWindows.startOfLocalDay(template.createdAt)
                        val days = ((day - anchor) / StatsPeriodWindows.DAY_MS).coerceAtLeast(0L)
                        days % n == 0L
                    }
                    else -> false
                }
                if (due) plan += times
            }
            day += StatsPeriodWindows.DAY_MS
        }
        return plan
    }

    private fun isoDayOfWeek(millis: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
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

    fun percent(value: Float): Int = (value * 100f).roundToInt().coerceIn(-100, 100)

    fun journalFromTask(
        task: Task,
        projectId: String,
        links: List<TaskLink>,
        projectTasks: List<Task>,
        kind: String
    ): StatsJournalEntry {
        val tasksById = projectTasks.associateBy { it.id }
        val projectLinks = links.filter { it.parentId in tasksById && it.childId in tasksById }
        val roots = GoalProgressCalculator.resolveGoals(
            projectTasks.filter { !it.statsExcluded },
            projectLinks
        ).map { it.id }.toSet()
        val linked = descendantsOf(roots, projectLinks)
        val relatedGoals = roots.filter { rootId ->
            task.id == rootId || task.id in descendantsOf(setOf(rootId), projectLinks)
        }
        val toward = task.id in roots || task.id in linked || relatedGoals.isNotEmpty()
        val weight = TaskLinkGraph.nodeWeight(task.id, tasksById, projectLinks)
        val score = when (kind) {
            StatsJournalEntry.KIND_NOT_DONE -> GoalProgressCalculator.NOT_DONE_SCORE
            else -> GoalProgressCalculator.leafScore(
                task.copy(isCompleted = true, workflowStatus = TaskWorkflow.DONE)
            )
        }
        val eventAt = when (kind) {
            StatsJournalEntry.KIND_DONE -> task.completedAt ?: System.currentTimeMillis()
            else -> System.currentTimeMillis()
        }
        return StatsJournalEntry(
            id = java.util.UUID.randomUUID().toString(),
            projectId = projectId,
            sourceTaskId = task.id,
            title = task.title,
            kind = kind,
            eventAt = eventAt,
            complexity = task.complexity,
            completionQuality = task.completionQuality,
            eisenhowerQuadrant = task.eisenhowerQuadrant,
            weight = weight,
            leafScore = score,
            towardGoal = toward,
            relatedGoalIds = relatedGoals,
            recurringTemplateId = task.recurringTemplateId,
            createdAt = System.currentTimeMillis()
        )
    }
}
