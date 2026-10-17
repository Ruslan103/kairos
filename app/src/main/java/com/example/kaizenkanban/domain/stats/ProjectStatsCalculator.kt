package com.example.kaizenkanban.domain.stats

import com.example.kaizenkanban.domain.model.Eisenhower
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
    TODAY,
    YESTERDAY,
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

sealed class CompletionOutlook {
    /** Goal closure is already about full. */
    data object Reached : CompletionOutlook()

    /** Recent closure is flat or falling, so no finish date. */
    data object NoPace : CompletionOutlook()

    /** If the recent pace holds, closure reaches 1 on this local day. */
    data class OnDate(val dayMillis: Long) : CompletionOutlook()
}

data class ProgressPoint(
    val dayStart: Long,
    /** Goal progress P at end of day (−1…1). */
    val progress: Float,
    /** Forecast ˆp at end of day (−1…1), null if no goal context. */
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
    /** Live tasks marked not done, plus journal not-done events in the window. */
    val notDoneCount: Int = 0,
    /** Subset of not-done that came from a recurring template. */
    val recurringNotDoneCount: Int = 0,
    /** Live tasks still open: not completed and not marked not done. */
    val openTaskCount: Int = 0,
    /** Done tasks of the selected goal (itself and its steps), in the window. */
    val selectedGoalDoneCount: Int = 0,
    val selectedGoalNotDoneCount: Int = 0,
    val selectedGoalRecurringDoneCount: Int = 0,
    val selectedGoalRecurringNotDoneCount: Int = 0,
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
    /**
     * Goal-specific plan breadth 0…1 (coverage + diversity of levers).
     * Null when no selected goal.
     */
    val planBreadth: Float? = null,
    /** Active direct children of the selected goal (levers). */
    val planBranchCount: Int = 0,
    /** Distinct levers touched by toward-done work in the period. */
    val planTouchedBranchCount: Int = 0,
    /**
     * Change in [chanceToComplete] over the selected period (now − start).
     * Null when no selected goal.
     */
    val chancePeriodDelta: Float? = null,
    /** Deadline the likelihood refers to: explicit deadline, or the start day when that is the deadline. */
    val selectedGoalDeadlineMillis: Long? = null,
    /**
     * How well current progress matches the goal deadline timeline (0…1).
     * 0.5 = on track or no usable deadline; higher = ahead; lower = behind.
     * Null when no selected goal.
     */
    val scheduleFit: Float? = null,
    /** Signed tilt applied from [scheduleFit] (−SCHEDULE_TILT…+SCHEDULE_TILT). */
    val scheduleBonus: Float? = null,
    val dailyActivity: List<DailyActivityPoint>,
    val goalProgressSeries: List<ProgressPoint>
)

object StatsPeriodWindows {
    fun window(
        period: StatsPeriod,
        now: Long = System.currentTimeMillis(),
        epochMillis: Long = 0L
    ): PeriodWindow {
        val startOfToday = startOfLocalDay(now)
        val end = when (period) {
            StatsPeriod.YESTERDAY -> startOfToday - 1L
            else -> now
        }
        val rawStart = when (period) {
            StatsPeriod.NOW -> epochMillis.coerceAtMost(startOfToday)
            StatsPeriod.TODAY -> startOfToday
            StatsPeriod.YESTERDAY -> startOfToday - DAY_MS
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

    /** One-pass index: relatedGoalId → journal rows mentioning that id. */
    fun indexJournalByRelatedGoal(
        journal: List<StatsJournalEntry>
    ): Map<String, List<StatsJournalEntry>> {
        if (journal.isEmpty()) return emptyMap()
        val map = LinkedHashMap<String, MutableList<StatsJournalEntry>>()
        journal.forEach { entry ->
            entry.relatedGoalIds.forEach { goalId ->
                map.getOrPut(goalId) { mutableListOf() }.add(entry)
            }
        }
        return map
    }

    fun journalGhostsForGoal(
        goalId: String,
        tasksById: Map<String, Task>,
        journal: List<StatsJournalEntry>,
        goalEpochMillis: Long?
    ): List<StatsJournalEntry> =
        journalGhostsForGoal(
            goalId,
            tasksById,
            indexJournalByRelatedGoal(journal),
            goalEpochMillis
        )

    fun journalGhostsForGoal(
        goalId: String,
        tasksById: Map<String, Task>,
        journalByRelatedGoal: Map<String, List<StatsJournalEntry>>,
        goalEpochMillis: Long?
    ): List<StatsJournalEntry> =
        journalByRelatedGoal[goalId].orEmpty().filter { entry ->
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
        val childrenMap = TaskLinkGraph.childrenOf(links)
        val journalByGoal = indexJournalByRelatedGoal(journal)
        return nodeScore(
            taskId, tasksById, childrenMap, journalByGoal, memo, goalEpochMillis
        )
    }

    private fun nodeScore(
        taskId: String,
        tasksById: Map<String, Task>,
        childrenMap: Map<String, List<String>>,
        journalByRelatedGoal: Map<String, List<StatsJournalEntry>>,
        memo: MutableMap<String, Float>,
        goalEpochMillis: Long?
    ): Float {
        memo[taskId]?.let { return it }
        val kids = childrenMap[taskId].orEmpty()
        val score = if (kids.isEmpty()) {
            val ghosts = journalGhostsForGoal(
                taskId, tasksById, journalByRelatedGoal, goalEpochMillis
            )
            if (ghosts.isNotEmpty()) {
                progressOf(
                    taskId, tasksById, childrenMap, journalByRelatedGoal,
                    memo, goalEpochMillis
                )
            } else {
                val task = tasksById[taskId]
                when {
                    task == null -> 0f
                    !countsInGoalProgress(task, goalEpochMillis) -> 0f
                    else -> leafScore(task)
                }
            }
        } else {
            progressOf(
                taskId, tasksById, childrenMap, journalByRelatedGoal,
                memo, goalEpochMillis
            )
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
        val childrenMap = TaskLinkGraph.childrenOf(links)
        val journalByGoal = indexJournalByRelatedGoal(journal)
        return progressOf(
            goalId, tasksById, childrenMap, journalByGoal, scoreMemo, goalEpochMillis
        )
    }

    fun progressOf(
        goalId: String,
        tasksById: Map<String, Task>,
        childrenMap: Map<String, List<String>>,
        journalByRelatedGoal: Map<String, List<StatsJournalEntry>>,
        scoreMemo: MutableMap<String, Float> = mutableMapOf(),
        goalEpochMillis: Long? = tasksById[goalId]?.goalStatsEpochMillis
    ): Float {
        val kids = childrenMap[goalId].orEmpty()
        val ghosts = journalGhostsForGoal(
            goalId, tasksById, journalByRelatedGoal, goalEpochMillis
        )
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
            val childKids = childrenMap[childId].orEmpty()
            val childGhosts = journalGhostsForGoal(
                childId, tasksById, journalByRelatedGoal, goalEpochMillis
            )
            if (childKids.isEmpty() && childGhosts.isEmpty() && child != null &&
                !countsInGoalProgress(child, goalEpochMillis)
            ) {
                return@forEach
            }
            val w = TaskLinkGraph.nodeWeight(
                childId, tasksById, childrenMap, weightMemo
            ).toDouble()
            if (w <= 0.0) return@forEach
            val s = nodeScore(
                childId, tasksById, childrenMap, journalByRelatedGoal,
                scoreMemo, goalEpochMillis
            ).toDouble()
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

    /**
     * Goal progress as of [asOfMillis] (completions / journal after that instant are ignored).
     */
    fun progressAsOf(
        goalId: String,
        tasksById: Map<String, Task>,
        links: List<TaskLink>,
        asOfMillis: Long,
        journal: List<StatsJournalEntry> = emptyList()
    ): Float {
        val childrenMap = TaskLinkGraph.childrenOf(links)
        return progressAsOf(
            goalId, tasksById, childrenMap, asOfMillis, journal
        )
    }

    fun progressAsOf(
        goalId: String,
        tasksById: Map<String, Task>,
        childrenMap: Map<String, List<String>>,
        asOfMillis: Long,
        journal: List<StatsJournalEntry> = emptyList()
    ): Float {
        val overrides = HashMap<String, Task>()
        tasksById.values.forEach { task ->
            if (!task.isNotDone &&
                task.isCompleted &&
                (task.completedAt ?: Long.MAX_VALUE) > asOfMillis
            ) {
                overrides[task.id] = task.copy(
                    isCompleted = false,
                    completedAt = null,
                    completionQuality = null,
                    workflowStatus = TaskWorkflow.OPEN
                )
            }
        }
        val virtual = if (overrides.isEmpty()) tasksById else tasksById + overrides
        val journalUpTo = journal.filter { it.eventAt <= asOfMillis }
        val journalByGoal = indexJournalByRelatedGoal(journalUpTo)
        return progressOf(
            goalId, virtual, childrenMap, journalByGoal,
            goalEpochMillis = virtual[goalId]?.goalStatsEpochMillis
        )
    }

    fun periodDelta(
        goalId: String,
        tasksById: Map<String, Task>,
        links: List<TaskLink>,
        window: PeriodWindow,
        journal: List<StatsJournalEntry> = emptyList()
    ): Float {
        val childrenMap = TaskLinkGraph.childrenOf(links)
        val journalByGoal = indexJournalByRelatedGoal(journal)
        return periodDelta(goalId, tasksById, childrenMap, journalByGoal, window)
    }

    fun periodDelta(
        goalId: String,
        tasksById: Map<String, Task>,
        childrenMap: Map<String, List<String>>,
        journalByRelatedGoal: Map<String, List<StatsJournalEntry>>,
        window: PeriodWindow
    ): Float {
        val epoch = tasksById[goalId]?.goalStatsEpochMillis ?: 0L
        val start = maxOf(window.startMillis, epoch)
        val end = window.endMillis
        if (start > end) return 0f
        val clipped = PeriodWindow(start, end, window.dayCount)
        return periodDeltaUnclipped(
            goalId, tasksById, childrenMap, journalByRelatedGoal, clipped
        )
    }

    private fun periodDeltaUnclipped(
        goalId: String,
        tasksById: Map<String, Task>,
        childrenMap: Map<String, List<String>>,
        journalByRelatedGoal: Map<String, List<StatsJournalEntry>>,
        window: PeriodWindow
    ): Float {
        val kids = childrenMap[goalId].orEmpty()
        val ghosts = journalGhostsForGoal(
            goalId,
            tasksById,
            journalByRelatedGoal,
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
            TaskLinkGraph.nodeWeight(it, tasksById, childrenMap, weightMemo)
        }.toFloat()
        val denGhost = ghosts.sumOf { it.weight.coerceAtLeast(1) }.toFloat()
        val den = (denLive + denGhost).coerceAtLeast(1f)
        val gainedLive = kids.sumOf { childId ->
            val task = tasksById[childId] ?: return@sumOf 0.0
            when {
                task.isNotDone -> {
                    TaskLinkGraph.nodeWeight(childId, tasksById, childrenMap, weightMemo) *
                        leafScore(task).toDouble()
                }
                task.isCompleted && task.completedAt != null &&
                    task.completedAt in window.startMillis..window.endMillis -> {
                    TaskLinkGraph.nodeWeight(childId, tasksById, childrenMap, weightMemo) *
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
        val childrenMap = TaskLinkGraph.childrenOf(links)
        return tasks
            .filter { it.isGoal && !it.statsExcluded }
            .sortedByDescending {
                TaskLinkGraph.nodeWeight(it.id, tasksById, childrenMap)
            }
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
        val replay = replayClearedArchive(projectTasks, links, journal)
        val activeTasks = replay.tasks.filter { !it.statsExcluded }
        val tasksById = activeTasks.associateBy { it.id }
        val projectLinks = replay.links.filter { it.parentId in tasksById && it.childId in tasksById }
        val childrenMap = TaskLinkGraph.childrenOf(projectLinks)
        val statsJournal = replay.journal
        val journalByGoal = GoalProgressCalculator.indexJournalByRelatedGoal(statsJournal)
        val resolved = GoalProgressCalculator.resolveGoals(activeTasks, projectLinks)
        val rootIds = resolved.map { it.id }.toSet()
        val linkedToGoal = descendantsOf(rootIds, childrenMap)

        val goals = resolved.map { root ->
            val p = GoalProgressCalculator.progressOf(
                root.id, tasksById, childrenMap, journalByGoal
            )
            val rootKids = childrenMap[root.id].orEmpty()
            val rootGhosts = GoalProgressCalculator.journalGhostsForGoal(
                root.id, tasksById, journalByGoal, root.goalStatsEpochMillis
            )
            GoalProgress(
                goalId = root.id,
                goalTitle = root.title,
                progress = p,
                periodDelta = GoalProgressCalculator.periodDelta(
                    root.id, tasksById, childrenMap, journalByGoal, window
                ),
                childCount = rootKids.size + rootGhosts.size,
                childWeightSum = rootKids.sumOf {
                    TaskLinkGraph.nodeWeight(it, tasksById, childrenMap)
                } + rootGhosts.sumOf { it.weight.coerceAtLeast(1) },
                isArchived = root.isBoardArchived
            )
        }
        val selected = goals.find { it.goalId == selectedGoalId } ?: goals.firstOrNull()

        val liveDone = activeTasks.filter { task ->
            task.isCompleted &&
                task.completedAt != null &&
                task.completedAt in window.startMillis..window.endMillis
        }
        val journalDone = statsJournal.filter { entry ->
            entry.kind == StatsJournalEntry.KIND_DONE &&
                entry.eventAt in window.startMillis..window.endMillis
        }

        val towardLive = liveDone.filter { it.id in linkedToGoal || it.id in rootIds }
        val triviaLive = liveDone.filter { it.id !in linkedToGoal && it.id !in rootIds }
        val towardJournal = journalDone.filter { it.towardGoal }
        val triviaJournal = journalDone.filter { !it.towardGoal }

        val weightMemo = mutableMapOf<String, Int>()
        fun w(id: String) = TaskLinkGraph.nodeWeight(id, tasksById, childrenMap, weightMemo)

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
        val liveNotDone = activeTasks.filter { it.isNotDone }
        val journalNotDone = statsJournal.filter { entry ->
            entry.kind == StatsJournalEntry.KIND_NOT_DONE &&
                entry.eventAt in window.startMillis..window.endMillis
        }
        val notDoneCount = liveNotDone.size + journalNotDone.size
        val recurringNotDoneCount = liveNotDone.count { it.recurringTemplateId != null } +
            journalNotDone.count { it.recurringTemplateId != null }
        val openTaskCount = activeTasks.count { !it.isCompleted && !it.isNotDone }
        val selectedTree = selected?.goalId?.let { id ->
            descendantsOf(setOf(id), childrenMap) + id
        }.orEmpty()
        val goalLiveDone = liveDone.filter { it.id in selectedTree }
        val goalJournalDone = journalDone.filter { entry ->
            selected != null && selected.goalId in entry.relatedGoalIds
        }
        val goalLiveNotDone = liveNotDone.filter { it.id in selectedTree }
        val goalJournalNotDone = journalNotDone.filter { entry ->
            selected != null && selected.goalId in entry.relatedGoalIds
        }
        val selectedGoalDoneCount = goalLiveDone.size + goalJournalDone.size
        val selectedGoalNotDoneCount = goalLiveNotDone.size + goalJournalNotDone.size
        val selectedGoalRecurringDoneCount =
            goalLiveDone.count { it.recurringTemplateId != null } +
                goalJournalDone.count { it.recurringTemplateId != null }
        val selectedGoalRecurringNotDoneCount =
            goalLiveNotDone.count { it.recurringTemplateId != null } +
                goalJournalNotDone.count { it.recurringTemplateId != null }
        val rhythm = if (plan <= 0) 0f else (fact.toFloat() / plan).coerceIn(0f, 1f)
        val triviaShare = (triviaWeight.toFloat() / doneWeight).coerceIn(0f, 1f)
        val rhythmBonus = 0.15f * rhythm
        val triviaPenalty = 0.20f * triviaShare

        val selectedGoalTask = selected?.let { tasksById[it.goalId] }
        val scheduleFitValue = selected?.let { g ->
            val goalTask = selectedGoalTask
            if (goalTask == null) {
                0.5f
            } else {
                scheduleFit(
                    progress = g.progress,
                    deadlineAt = goalTask.deadlineAt,
                    startAt = goalTask.dueDate,
                    createdAt = goalTask.createdAt,
                    now = now
                )
            }
        }
        val scheduleBonusValue = scheduleFitValue?.let { SCHEDULE_TILT * (it - 0.5f) }

        val hatP = selected?.let { g ->
            (g.progress + rhythmBonus - triviaPenalty + (scheduleBonusValue ?: 0f)).coerceIn(-1f, 1f)
        }
        val planBreadthInfo = selected?.let { g ->
            computePlanBreadth(
                goalId = g.goalId,
                tasksById = tasksById,
                links = projectLinks,
                childrenMap = childrenMap,
                liveDone = liveDone,
                journalDone = journalDone,
                weightOf = { id -> w(id) }
            )
        }
        val chance = selected?.let { g ->
            chanceToComplete(
                hatP = hatP ?: 0f,
                progress = g.progress,
                periodDelta = g.periodDelta,
                rhythmShare = rhythm,
                triviaShare = triviaShare,
                planBreadth = planBreadthInfo?.breadth ?: 0f,
                scheduleFit = scheduleFitValue ?: 0.5f
            )
        }
        val chancePeriodDelta = if (selected != null && chance != null) {
            val justBefore = window.startMillis - 1L
            val progressStart = GoalProgressCalculator.progressAsOf(
                goalId = selected.goalId,
                tasksById = tasksById,
                childrenMap = childrenMap,
                asOfMillis = justBefore,
                journal = statsJournal
            )
            val breadthStart = computePlanBreadth(
                goalId = selected.goalId,
                tasksById = tasksById,
                links = projectLinks,
                childrenMap = childrenMap,
                liveDone = emptyList(),
                journalDone = emptyList(),
                weightOf = { id -> w(id) }
            )
            val scheduleStart = selectedGoalTask?.let { goalTask ->
                scheduleFit(
                    progress = progressStart,
                    deadlineAt = goalTask.deadlineAt,
                    startAt = goalTask.dueDate,
                    createdAt = goalTask.createdAt,
                    now = justBefore.coerceAtLeast(0L)
                )
            } ?: 0.5f
            // Chance at period open: structure + progress so far, no momentum/rhythm/trivia of this window yet.
            val chanceStart = chanceToComplete(
                hatP = progressStart.coerceIn(0f, 1f),
                progress = progressStart,
                periodDelta = 0f,
                rhythmShare = 0f,
                triviaShare = 0f,
                planBreadth = breadthStart.breadth,
                scheduleFit = scheduleStart
            )
            (chance - chanceStart).coerceIn(-1f, 1f)
        } else {
            null
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
                childrenMap = childrenMap,
                window = window,
                now = now,
                templates = templates,
                liveDone = liveDone,
                journal = statsJournal,
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
            notDoneCount = notDoneCount,
            recurringNotDoneCount = recurringNotDoneCount,
            openTaskCount = openTaskCount,
            selectedGoalDoneCount = selectedGoalDoneCount,
            selectedGoalNotDoneCount = selectedGoalNotDoneCount,
            selectedGoalRecurringDoneCount = selectedGoalRecurringDoneCount,
            selectedGoalRecurringNotDoneCount = selectedGoalRecurringNotDoneCount,
            unratedDoneIds = unrated,
            hatP = hatP,
            chanceToComplete = chance,
            periodHatP = periodHatP,
            towardShare = towardShare,
            rhythmShare = rhythm,
            triviaShare = triviaShare,
            triviaPenalty = triviaPenalty,
            rhythmBonus = rhythmBonus,
            planBreadth = planBreadthInfo?.breadth,
            planBranchCount = planBreadthInfo?.branchCount ?: 0,
            planTouchedBranchCount = planBreadthInfo?.touchedBranchCount ?: 0,
            chancePeriodDelta = chancePeriodDelta,
            selectedGoalDeadlineMillis = selectedGoalTask?.deadlineAt ?: selectedGoalTask?.dueDate,
            scheduleFit = scheduleFitValue,
            scheduleBonus = scheduleBonusValue,
            dailyActivity = daily,
            goalProgressSeries = series
        )
    }

    /**
     * Soft saturation: 5 different direct subtasks → full coverage score.
     * Further cards do not raise the chance.
     */
    const val PLAN_BREADTH_SATURATION = 5

    /** Max signed tilt from schedule fit on movement / chance / chart forecast (±7.5%). */
    const val SCHEDULE_TILT = 0.15f

    data class PlanBreadthInfo(
        val breadth: Float,
        val branchCount: Int,
        val touchedBranchCount: Int
    )

    /**
     * How well [progress] matches the goal timeline to its deadline.
     * Uses deadline (or start day when deadline is empty). No usable date → 0.5 (neutral).
     * Does not change progress itself — only schedule health for movement / chance / forecast.
     */
    fun scheduleFit(
        progress: Float,
        deadlineAt: Long?,
        startAt: Long?,
        createdAt: Long,
        now: Long = System.currentTimeMillis()
    ): Float {
        val deadline = deadlineAt ?: startAt ?: return 0.5f
        val deadlineDay = StatsPeriodWindows.startOfLocalDay(deadline)
        val today = StatsPeriodWindows.startOfLocalDay(now)
        val p = progress.coerceIn(-1f, 1f).coerceAtLeast(0f)

        val originRaw = when {
            startAt != null -> StatsPeriodWindows.startOfLocalDay(startAt)
            else -> {
                val createdDay = StatsPeriodWindows.startOfLocalDay(createdAt)
                if (createdDay < deadlineDay) createdDay
                else deadlineDay - 14L * StatsPeriodWindows.DAY_MS
            }
        }
        val originDay = minOf(originRaw, deadlineDay)
        val spanDays = ((deadlineDay - originDay) / StatsPeriodWindows.DAY_MS)
            .toInt()
            .coerceAtLeast(1)

        if (today > deadlineDay) {
            return if (p >= 0.999f) 1f else (0.15f + 0.35f * p).coerceIn(0f, 0.5f)
        }

        val elapsedDays = ((today - originDay) / StatsPeriodWindows.DAY_MS)
            .toInt()
            .coerceIn(0, spanDays)
        val expected = elapsedDays.toFloat() / spanDays.toFloat()
        val ahead = (p - expected).coerceIn(-1f, 1f)
        return (0.5f + 0.5f * ahead).coerceIn(0f, 1f)
    }

    /**
     * Heuristic index (0…1), not a calibrated probability.
     * Combines assessment, momentum, rhythm, low trivia, progress, plan breadth, and schedule fit.
     */
    fun chanceToComplete(
        hatP: Float,
        progress: Float,
        periodDelta: Float,
        rhythmShare: Float,
        triviaShare: Float,
        planBreadth: Float = 0f,
        scheduleFit: Float = 0.5f
    ): Float {
        val p = progress.coerceIn(-1f, 1f).coerceAtLeast(0f)
        val momentum = periodDelta.coerceIn(0f, 1f)
        val base = (
            0.45f * hatP.coerceIn(0f, 1f) +
                0.15f * momentum +
                0.12f * rhythmShare.coerceIn(0f, 1f) +
                0.10f * (1f - triviaShare.coerceIn(0f, 1f)) +
                0.08f * p +
                0.10f * planBreadth.coerceIn(0f, 1f)
            ).coerceIn(0f, 1f)
        return (base + SCHEDULE_TILT * (scheduleFit.coerceIn(0f, 1f) - 0.5f)).coerceIn(0f, 1f)
    }

    /**
     * Plan breadth for a goal: coverage of active levers + diversity of toward-work.
     * One or two recurring levers and no ordinary step score lower.
     * An important, well-rated ordinary step counts more than another repeat of the same habit.
     */
    fun computePlanBreadth(
        goalId: String,
        tasksById: Map<String, Task>,
        links: List<TaskLink>,
        liveDone: List<Task>,
        journalDone: List<StatsJournalEntry>,
        childrenMap: Map<String, List<String>> = TaskLinkGraph.childrenOf(links),
        weightOf: (String) -> Int = { id ->
            TaskLinkGraph.nodeWeight(id, tasksById, childrenMap)
        }
    ): PlanBreadthInfo {
        val directKids = childrenMap[goalId].orEmpty()
        val activeBranches = directKids.filter { id ->
            val t = tasksById[id] ?: return@filter false
            !t.statsExcluded && !t.isBoardArchived
        }
        val branchCount = activeBranches.size
        val coverage = (branchCount.toFloat() / PLAN_BREADTH_SATURATION.toFloat()).coerceIn(0f, 1f)

        val branchOf = branchMembership(goalId, activeBranches, childrenMap)
        val goalDescendants = descendantsOf(setOf(goalId), childrenMap)

        val towardLive = liveDone.filter { it.id in goalDescendants || it.id == goalId }
        val towardJournal = journalDone.filter { entry ->
            entry.towardGoal && (goalId in entry.relatedGoalIds || entry.sourceTaskId in goalDescendants)
        }

        val recurringRoots = activeBranches.filter { id ->
            tasksById[id]?.recurringTemplateId != null
        }.toSet()
        val creditByBranch = mutableMapOf<String, Float>()
        var towardWeight = 0
        val weightByRecurring = mutableMapOf<String, Int>()

        fun touch(branchId: String, recurringTask: Boolean, quadrant: String?, quality: Int?) {
            val credit = when {
                branchId in recurringRoots || recurringTask -> 1f
                else -> ordinaryTouchCredit(quadrant, quality)
            }
            val prev = creditByBranch[branchId]
            if (prev == null || credit > prev) creditByBranch[branchId] = credit
        }

        towardLive.forEach { task ->
            val w = weightOf(task.id).coerceAtLeast(1)
            towardWeight += w
            val branchId = branchOf[task.id] ?: return@forEach
            touch(branchId, task.recurringTemplateId != null, task.eisenhowerQuadrant, task.completionQuality)
            val key = task.recurringTemplateId
            if (key != null) {
                weightByRecurring[key] = (weightByRecurring[key] ?: 0) + w
            }
        }
        towardJournal.forEach { entry ->
            val w = entry.weight.coerceAtLeast(1)
            towardWeight += w
            val source = entry.sourceTaskId
            val branchId = branchOf[source]
                ?: source.takeIf { it in activeBranches }
            if (branchId != null) {
                touch(
                    branchId,
                    entry.recurringTemplateId != null,
                    entry.eisenhowerQuadrant,
                    entry.completionQuality
                )
            }
            val key = entry.recurringTemplateId
            if (key != null) {
                weightByRecurring[key] = (weightByRecurring[key] ?: 0) + w
            }
        }

        val touchedCount = creditByBranch.size
        val diversity = when {
            branchCount <= 0 -> 0f
            towardWeight <= 0 -> coverage
            else -> (creditByBranch.values.sum() / PLAN_BREADTH_SATURATION.toFloat()).coerceIn(0f, 1f)
        }

        var breadth = (0.55f * coverage + 0.45f * diversity).coerceIn(0f, 1f)

        val ordinaryBranchCount = branchCount - recurringRoots.size
        if (ordinaryBranchCount == 0 && recurringRoots.size in 1..2) {
            breadth = (breadth * 0.45f).coerceIn(0f, 1f)
        } else if (branchCount >= 2 && towardWeight > 0) {
            val maxRecurring = weightByRecurring.values.maxOrNull() ?: 0
            val share = maxRecurring.toFloat() / towardWeight.toFloat()
            if (share >= 0.70f) {
                breadth = (breadth * 0.62f).coerceIn(0f, 1f)
            }
        }

        return PlanBreadthInfo(
            breadth = breadth,
            branchCount = branchCount,
            touchedBranchCount = touchedCount
        )
    }

    /**
     * One ordinary step versus one recurring repeat (credit 1).
     * Important and well-rated ordinary work can count up to about two repeats.
     */
    private fun ordinaryTouchCredit(quadrant: String?, quality: Int?): Float {
        val importance = Eisenhower.importanceCents(quadrant) / 100f
        val qualityFactor = quality?.coerceIn(1, 5)?.div(5f) ?: 0.55f
        return (1.1f * importance * (0.7f + 0.55f * qualityFactor)).coerceIn(0.85f, 2.2f)
    }

    /** Map every descendant task id → its direct-child branch under [goalId]. */
    private fun branchMembership(
        goalId: String,
        activeBranches: List<String>,
        childrenMap: Map<String, List<String>>
    ): Map<String, String> {
        val out = mutableMapOf<String, String>()
        activeBranches.forEach { branchId ->
            out[branchId] = branchId
            val stack = ArrayDeque(childrenMap[branchId].orEmpty())
            val seen = mutableSetOf(branchId)
            while (stack.isNotEmpty()) {
                val id = stack.removeFirst()
                if (!seen.add(id)) continue
                out[id] = branchId
                childrenMap[id].orEmpty().forEach { stack.add(it) }
            }
        }
        // goal root itself is not a branch lever
        out.remove(goalId)
        return out
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
        childrenMap: Map<String, List<String>>,
        window: PeriodWindow,
        now: Long,
        templates: List<RecurringTemplate>,
        liveDone: List<Task>,
        journal: List<StatsJournalEntry>,
        linkedToGoal: Set<String>,
        rootIds: Set<String>
    ): List<ProgressPoint> {
        val weightMemo = mutableMapOf<String, Int>()
        fun w(id: String) = TaskLinkGraph.nodeWeight(id, tasksById, childrenMap, weightMemo)
        val enabledTemplates = templates.filter { it.enabled }
        val liveSorted = liveDone
            .filter { it.completedAt != null }
            .sortedBy { it.completedAt!! }
        val journalSorted = journal.sortedBy { it.eventAt }

        val points = mutableListOf<ProgressPoint>()
        var day = StatsPeriodWindows.startOfLocalDay(window.startMillis)
        val endDay = StatsPeriodWindows.startOfLocalDay(minOf(window.endMillis, now))
        var liveIdx = 0
        var journalIdx = 0
        val journalToDay = mutableListOf<StatsJournalEntry>()
        var cumulativePlan = 0
        var towardW = 0
        var triviaW = 0
        var fact = 0

        while (day <= endDay) {
            val dayEnd = day + StatsPeriodWindows.DAY_MS - 1

            while (liveIdx < liveSorted.size) {
                val task = liveSorted[liveIdx]
                val at = task.completedAt ?: break
                if (at > dayEnd) break
                if (at >= window.startMillis) {
                    val weight = w(task.id)
                    if (task.id in linkedToGoal || task.id in rootIds) towardW += weight
                    else triviaW += weight
                    if (task.recurringTemplateId != null) fact++
                }
                liveIdx++
            }
            while (journalIdx < journalSorted.size) {
                val entry = journalSorted[journalIdx]
                if (entry.eventAt > dayEnd) break
                // Progress uses every journal row up to this day, including not-done.
                // Rhythm and off-goal use only done rows inside the stats window.
                journalToDay += entry
                if (entry.kind == StatsJournalEntry.KIND_DONE &&
                    entry.eventAt >= window.startMillis
                ) {
                    if (entry.towardGoal) towardW += entry.weight else triviaW += entry.weight
                    if (entry.recurringTemplateId != null) fact++
                }
                journalIdx++
            }

            cumulativePlan += recurringPlanForDay(enabledTemplates, day)

            val overrides = HashMap<String, Task>()
            tasksById.values.forEach { task ->
                val doneAt = task.completedAt ?: now
                if (!task.isNotDone && task.isCompleted && doneAt > dayEnd) {
                    overrides[task.id] = task.copy(
                        isCompleted = false,
                        completedAt = null,
                        completionQuality = null,
                        workflowStatus = TaskWorkflow.OPEN
                    )
                }
            }
            val virtual = if (overrides.isEmpty()) tasksById else tasksById + overrides
            val journalByGoal = GoalProgressCalculator.indexJournalByRelatedGoal(journalToDay)
            val p = GoalProgressCalculator.progressOf(
                goalId, virtual, childrenMap, journalByGoal
            )

            val doneW = (towardW + triviaW).coerceAtLeast(1)
            val o = (triviaW.toFloat() / doneW).coerceIn(0f, 1f)
            val r = if (cumulativePlan <= 0) {
                0f
            } else {
                (fact.toFloat() / cumulativePlan).coerceIn(0f, 1f)
            }
            val goal = tasksById[goalId]
            val schedule = if (goal != null) {
                scheduleFit(
                    progress = p,
                    deadlineAt = goal.deadlineAt,
                    startAt = goal.dueDate,
                    createdAt = goal.createdAt,
                    now = minOf(dayEnd, now)
                )
            } else {
                0.5f
            }
            val forecast = (
                p + 0.15f * r - 0.20f * o + SCHEDULE_TILT * (schedule - 0.5f)
                ).coerceIn(-1f, 1f)

            points += ProgressPoint(dayStart = day, progress = p, forecast = forecast)
            day += StatsPeriodWindows.DAY_MS
        }
        return points
    }

    private fun descendantsOf(
        roots: Set<String>,
        childrenMap: Map<String, List<String>>
    ): Set<String> {
        if (roots.isEmpty()) return emptySet()
        val out = mutableSetOf<String>()
        val stack = ArrayDeque(roots)
        val seen = HashSet<String>()
        while (stack.isNotEmpty()) {
            val id = stack.removeLast()
            if (!seen.add(id)) continue
            childrenMap[id].orEmpty().forEach { child ->
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
            plan += recurringPlanForDay(enabled, day)
            day += StatsPeriodWindows.DAY_MS
        }
        return plan
    }

    private fun recurringPlanForDay(
        enabledTemplates: List<RecurringTemplate>,
        day: Long
    ): Int {
        if (enabledTemplates.isEmpty()) return 0
        val dow = isoDayOfWeek(day)
        var plan = 0
        enabledTemplates.forEach { template ->
            val times = com.example.kaizenkanban.domain.model.RecurringDefaults
                .effectiveReminderTimes(template.reminderTimesOfDay)
                .size
            val due = when (template.rhythm) {
                RecurringRhythm.DAILY -> true
                RecurringRhythm.WEEKDAYS -> dow in template.weekdays
                RecurringRhythm.EVERY_N_DAYS -> {
                    val n = RecurringRhythm.intervalDays(template.timesPerWeek)
                    val anchor = StatsPeriodWindows.startOfLocalDay(template.createdAt)
                    val days = ((day - anchor) / StatsPeriodWindows.DAY_MS).coerceAtLeast(0L)
                    days % n == 0L
                }
                else -> false
            }
            if (due) plan += times
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

    /**
     * Finish day from the whole closure history.
     * With a deadline, [chance] is the likelihood of finishing by that day:
     * 100% lands on the deadline, a lower chance lands on that day or later.
     * Overall pace can push the date further out, never earlier than the chance allows.
     */
    fun completionOutlook(
        progress: Float,
        series: List<ProgressPoint>,
        now: Long,
        chance: Float? = null,
        deadlineMillis: Long? = null
    ): CompletionOutlook {
        if (progress >= 0.999f) return CompletionOutlook.Reached
        val today = StatsPeriodWindows.startOfLocalDay(now)
        val paceDays = overallPaceDays(progress, series, today)
        val deadlineDay = deadlineMillis?.let { StatsPeriodWindows.startOfLocalDay(it) }
        if (deadlineDay != null && chance != null) {
            if (chance >= 0.995f) return CompletionOutlook.OnDate(deadlineDay)
            val daysUntil = ((deadlineDay - today) / StatsPeriodWindows.DAY_MS).toInt()
            val q = chance.coerceIn(0.01f, 0.994f)
            val byChance = if (daysUntil > 0) {
                kotlin.math.ceil(daysUntil / q.toDouble()).toLong()
                    .coerceAtLeast(daysUntil + 1L)
            } else {
                null
            }
            val days = when {
                byChance != null && paceDays != null -> maxOf(byChance, paceDays)
                byChance != null -> byChance
                paceDays != null -> paceDays.coerceAtLeast(1L)
                else -> return CompletionOutlook.NoPace
            }
            if (days > 36_500L) return CompletionOutlook.NoPace
            return CompletionOutlook.OnDate(today + days * StatsPeriodWindows.DAY_MS)
        }
        val days = paceDays ?: return CompletionOutlook.NoPace
        if (days == 0L) return CompletionOutlook.Reached
        if (days > 36_500L) return CompletionOutlook.NoPace
        return CompletionOutlook.OnDate(today + days * StatsPeriodWindows.DAY_MS)
    }

    /** Days until closure if the pace from the first history point to the last one holds. */
    private fun overallPaceDays(
        progress: Float,
        series: List<ProgressPoint>,
        today: Long
    ): Long? {
        val points = series.filter { it.dayStart <= today }.sortedBy { it.dayStart }
        if (points.size < 2) return null
        val start = points.first()
        val end = points.last()
        val spanDays = ((end.dayStart - start.dayStart) / StatsPeriodWindows.DAY_MS).toInt()
        if (spanDays < 1) return null
        val delta = end.progress - start.progress
        if (delta <= 0.0005f) return null
        val remaining = (1f - progress).coerceAtLeast(0f)
        if (remaining <= 0.001f) return 0L
        val rawDays = remaining.toDouble() / (delta.toDouble() / spanDays.toDouble())
        return kotlin.math.ceil(rawDays - 1e-3).toLong().coerceAtLeast(1L)
    }

    fun journalFromTask(
        task: Task,
        projectId: String,
        links: List<TaskLink>,
        projectTasks: List<Task>,
        kind: String
    ): StatsJournalEntry {
        val tasksById = projectTasks.associateBy { it.id }
        val projectLinks = links.filter { it.parentId in tasksById && it.childId in tasksById }
        val childrenMap = TaskLinkGraph.childrenOf(projectLinks)
        val roots = GoalProgressCalculator.resolveGoals(
            projectTasks.filter { !it.statsExcluded },
            projectLinks
        ).map { it.id }.toSet()
        val linked = descendantsOf(roots, childrenMap)
        val relatedGoals = roots.filter { rootId ->
            task.id == rootId || task.id in descendantsOf(setOf(rootId), childrenMap)
        }
        val toward = task.id in roots || task.id in linked || relatedGoals.isNotEmpty()
        val weight = TaskLinkGraph.nodeWeight(task.id, tasksById, childrenMap)
        val score = when (kind) {
            StatsJournalEntry.KIND_NOT_DONE -> GoalProgressCalculator.NOT_DONE_SCORE
            StatsJournalEntry.KIND_OPEN -> 0f
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
            createdAt = System.currentTimeMillis(),
            parentIds = projectLinks.filter { it.childId == task.id }.map { it.parentId }.distinct(),
            childIds = projectLinks.filter { it.parentId == task.id }.map { it.childId }.distinct(),
            isGoal = task.isGoal,
            goalStatsEpochMillis = task.goalStatsEpochMillis,
            dueDate = task.dueDate,
            deadlineAt = task.deadlineAt,
            taskCreatedAt = task.createdAt,
            replacesTask = true
        )
    }

    /**
     * Cleared archive rows that fully replace a task are put back into the
     * graph, then dropped from [journal] so counts and progress are not applied twice.
     * Older journal rows stay as flat ghosts.
     */
    internal fun replayClearedArchive(
        tasks: List<Task>,
        links: List<TaskLink>,
        journal: List<StatsJournalEntry>
    ): ArchiveReplay {
        val liveIds = tasks.map { it.id }.toSet()
        val restoring = journal
            .filter { it.replacesTask && it.sourceTaskId !in liveIds }
            .distinctBy { it.sourceTaskId }
        if (restoring.isEmpty()) return ArchiveReplay(tasks, links, journal)
        val restoredIds = restoring.map { it.sourceTaskId }.toSet()
        val virtuals = restoring.map { it.toVirtualTask() }
        val extraLinks = restoring.flatMap { entry ->
            entry.parentIds.map { parentId -> TaskLink(parentId, entry.sourceTaskId, createdAt = 0L) } +
                entry.childIds.map { childId -> TaskLink(entry.sourceTaskId, childId, createdAt = 0L) }
        }
        val mergedTasks = tasks + virtuals
        val ids = mergedTasks.map { it.id }.toSet()
        val mergedLinks = (links + extraLinks)
            .filter { it.parentId in ids && it.childId in ids && it.parentId != it.childId }
            .distinctBy { it.parentId to it.childId }
        return ArchiveReplay(
            tasks = mergedTasks,
            links = mergedLinks,
            journal = journal.filter { it.sourceTaskId !in restoredIds }
        )
    }

    private fun StatsJournalEntry.toVirtualTask(): Task {
        val completed = kind == StatsJournalEntry.KIND_DONE
        val notDone = kind == StatsJournalEntry.KIND_NOT_DONE
        return Task(
            id = sourceTaskId,
            columnId = "archive",
            title = title,
            description = "",
            position = 0,
            categoryId = null,
            dueDate = dueDate,
            isCompleted = completed,
            completedAt = if (completed) eventAt else null,
            createdAt = if (taskCreatedAt > 0L) taskCreatedAt else createdAt,
            eisenhowerQuadrant = eisenhowerQuadrant,
            complexity = complexity,
            completionQuality = completionQuality,
            workflowStatus = when {
                completed -> TaskWorkflow.DONE
                notDone -> TaskWorkflow.NOT_DONE
                else -> TaskWorkflow.OPEN
            },
            isBoardArchived = true,
            isGoal = isGoal,
            goalStatsEpochMillis = goalStatsEpochMillis,
            deadlineAt = deadlineAt,
            recurringTemplateId = recurringTemplateId
        )
    }
}

internal data class ArchiveReplay(
    val tasks: List<Task>,
    val links: List<TaskLink>,
    val journal: List<StatsJournalEntry>
)
