package com.example.kaizenkanban.domain.stats

import com.example.kaizenkanban.domain.model.StatsJournalEntry
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.domain.model.TaskLink
import com.example.kaizenkanban.domain.model.TaskWorkflow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectStatsCalculatorTest {

    private fun task(
        id: String,
        completed: Boolean = false,
        quality: Int? = null,
        complexity: Int? = null,
        completedAt: Long? = null,
        workflow: String = if (completed) TaskWorkflow.DONE else TaskWorkflow.OPEN,
        quadrant: String? = null,
        isGoal: Boolean = false,
        recurringTemplateId: String? = null
    ) = Task(
        id = id,
        columnId = "c",
        title = id,
        description = "",
        position = 0,
        categoryId = null,
        dueDate = null,
        isCompleted = completed,
        completedAt = completedAt,
        createdAt = 0L,
        complexity = complexity,
        completionQuality = quality,
        workflowStatus = workflow,
        eisenhowerQuadrant = quadrant,
        isGoal = isGoal,
        recurringTemplateId = recurringTemplateId
    )

    @Test
    fun progress_weightedChildren() {
        val tasks = listOf(
            task("g"),
            task("a", completed = true, complexity = 2),
            task("b", completed = false, complexity = 2)
        )
        val links = listOf(TaskLink("g", "a"), TaskLink("g", "b"))
        val p = GoalProgressCalculator.progressOf("g", tasks.associateBy { it.id }, links)
        assertEquals(0.5f, p, 0.001f)
    }

    @Test
    fun leafScore_notDoneUsesFullStepInverse() {
        // Unmarked done = +1; not-done = −1 so weight (impact×importance) scales the hit.
        assertEquals(
            -1f,
            GoalProgressCalculator.leafScore(
                task("a", completed = false, workflow = TaskWorkflow.NOT_DONE)
            ),
            0.001f
        )
        val tasks = listOf(
            task("g", isGoal = true),
            task("a", completed = false, complexity = 2, workflow = TaskWorkflow.NOT_DONE),
            task("b", completed = true, complexity = 2)
        )
        val links = listOf(TaskLink("g", "a"), TaskLink("g", "b"))
        val p = GoalProgressCalculator.progressOf("g", tasks.associateBy { it.id }, links)
        // (2*(−1) + 2*(+1)) / 4 = 0
        assertEquals(0f, p, 0.001f)
    }

    @Test
    fun hatP_withoutFocus() {
        val now = 1_700_000_000_000L
        val tasks = listOf(
            task("g", isGoal = true),
            task("a", completed = true, complexity = 1, completedAt = now - 1000)
        )
        val links = listOf(TaskLink("g", "a"))
        val snap = ProjectStatsCalculator.compute(
            projectTasks = tasks,
            links = links,
            templates = emptyList(),
            period = StatsPeriod.WEEK,
            epochMillis = 0L,
            now = now
        )
        assertTrue(snap.selectedGoal != null)
        assertTrue(snap.hatP != null && snap.hatP!! in 0f..1f)
        assertEquals(100, ProjectStatsCalculator.percent(snap.selectedGoal!!.progress))
    }

    @Test
    fun chartEnd_matchesRingEfficiency() {
        val now = 1_700_000_000_000L
        val epoch = now - 3 * StatsPeriodWindows.DAY_MS
        val withGhost = ProjectStatsCalculator.compute(
            projectTasks = listOf(
                task("g", isGoal = true),
                task("a", completed = true, completedAt = now - 1_000)
            ),
            links = listOf(TaskLink("g", "a")),
            templates = emptyList(),
            period = StatsPeriod.NOW,
            epochMillis = epoch,
            journal = listOf(
                StatsJournalEntry(
                    id = "j",
                    projectId = "p",
                    sourceTaskId = "gone",
                    title = "gone",
                    kind = StatsJournalEntry.KIND_NOT_DONE,
                    eventAt = now - 5_000,
                    complexity = 1,
                    completionQuality = null,
                    eisenhowerQuadrant = null,
                    weight = 1,
                    leafScore = -1f,
                    towardGoal = true,
                    relatedGoalIds = listOf("g"),
                    createdAt = now - 5_000
                )
            ),
            now = now,
            selectedGoalId = "g"
        )
        assertEquals(
            withGhost.hatP!!,
            withGhost.goalProgressSeries.last().forecast!!,
            0.001f
        )

        val live = ProjectStatsCalculator.compute(
            projectTasks = listOf(
                task("g", isGoal = true),
                task("done", completed = true, completedAt = now - 2_000, complexity = 2),
                task("miss", workflow = TaskWorkflow.NOT_DONE, complexity = 2)
            ),
            links = listOf(TaskLink("g", "done"), TaskLink("g", "miss")),
            templates = emptyList(),
            period = StatsPeriod.NOW,
            epochMillis = epoch,
            now = now,
            selectedGoalId = "g"
        )
        assertEquals(live.hatP!!, live.goalProgressSeries.last().forecast!!, 0.001f)
    }

    @Test
    fun completionOutlook_lowChanceLandsAfterDeadline() {
        val start = StatsPeriodWindows.startOfLocalDay(1_700_000_000_000L)
        val deadline = start + 30 * StatsPeriodWindows.DAY_MS
        val series = (0..14).map { i ->
            com.example.kaizenkanban.domain.stats.ProgressPoint(
                dayStart = start - (14 - i) * StatsPeriodWindows.DAY_MS,
                progress = 0.3f + 0.1f * (i / 14f)
            )
        }
        val outlook = ProjectStatsCalculator.completionOutlook(
            progress = 0.4f,
            series = series,
            now = start,
            chance = 0.2f,
            deadlineMillis = deadline
        )
        val onDate = outlook as com.example.kaizenkanban.domain.stats.CompletionOutlook.OnDate
        assertTrue(onDate.dayMillis > deadline)
        assertEquals(start + 150 * StatsPeriodWindows.DAY_MS, onDate.dayMillis)
    }

    @Test
    fun completionOutlook_fullChanceMatchesDeadline() {
        val start = StatsPeriodWindows.startOfLocalDay(1_700_000_000_000L)
        val deadline = start + 200 * StatsPeriodWindows.DAY_MS
        val series = (0..14).map { i ->
            com.example.kaizenkanban.domain.stats.ProgressPoint(
                dayStart = start - (14 - i) * StatsPeriodWindows.DAY_MS,
                progress = 0.3f + 0.1f * (i / 14f)
            )
        }
        val outlook = ProjectStatsCalculator.completionOutlook(
            progress = 0.4f,
            series = series,
            now = start,
            chance = 1f,
            deadlineMillis = deadline
        )
        val onDate = outlook as com.example.kaizenkanban.domain.stats.CompletionOutlook.OnDate
        assertEquals(deadline, onDate.dayMillis)
    }

    @Test
    fun completionOutlook_usesRecentClosurePace() {
        val start = StatsPeriodWindows.startOfLocalDay(1_700_000_000_000L)
        val series = (0..14).map { i ->
            com.example.kaizenkanban.domain.stats.ProgressPoint(
                dayStart = start - (14 - i) * StatsPeriodWindows.DAY_MS,
                progress = 0.3f + 0.1f * (i / 14f)
            )
        }
        val outlook = ProjectStatsCalculator.completionOutlook(
            progress = 0.4f,
            series = series,
            now = start + 12 * 60 * 60 * 1000
        )
        val onDate = outlook as com.example.kaizenkanban.domain.stats.CompletionOutlook.OnDate
        assertEquals(start + 84 * StatsPeriodWindows.DAY_MS, onDate.dayMillis)
    }

    @Test
    fun completionOutlook_noDateWhenClosureDoesNotRise() {
        val start = StatsPeriodWindows.startOfLocalDay(1_700_000_000_000L)
        val series = listOf(
            com.example.kaizenkanban.domain.stats.ProgressPoint(start - StatsPeriodWindows.DAY_MS, 0.5f),
            com.example.kaizenkanban.domain.stats.ProgressPoint(start, 0.4f)
        )
        val outlook = ProjectStatsCalculator.completionOutlook(0.4f, series, start)
        assertTrue(outlook is com.example.kaizenkanban.domain.stats.CompletionOutlook.NoPace)
    }

    @Test
    fun hatP_notDoneCanBeNegative() {
        val now = 1_700_000_000_000L
        val snap = ProjectStatsCalculator.compute(
            projectTasks = listOf(
                task("g", isGoal = true),
                task("a", workflow = TaskWorkflow.NOT_DONE)
            ),
            links = listOf(TaskLink("g", "a")),
            templates = emptyList(),
            period = StatsPeriod.NOW,
            epochMillis = now - StatsPeriodWindows.DAY_MS,
            now = now
        )
        assertTrue(snap.hatP!! < 0f)
        assertTrue(snap.goalProgressSeries.any { (it.forecast ?: 0f) < 0f })
    }

    @Test
    fun now_usesEpoch() {
        val now = 1_700_000_000_000L
        val epoch = now - 10L * StatsPeriodWindows.DAY_MS
        val window = StatsPeriodWindows.window(StatsPeriod.NOW, now, epoch)
        assertEquals(epoch, window.startMillis)
    }

    @Test
    fun progress_keepsJournalGhostsAfterArchiveClear() {
        val tasks = listOf(task("g", isGoal = true))
        val links = emptyList<TaskLink>()
        val journal = listOf(
            StatsJournalEntry(
                id = "j1",
                projectId = "p",
                sourceTaskId = "a",
                title = "a",
                kind = StatsJournalEntry.KIND_DONE,
                eventAt = 1_000L,
                complexity = 2,
                completionQuality = null,
                eisenhowerQuadrant = null,
                weight = 2,
                leafScore = 1f,
                towardGoal = true,
                relatedGoalIds = listOf("g"),
                createdAt = 1_000L
            ),
            StatsJournalEntry(
                id = "j2",
                projectId = "p",
                sourceTaskId = "b",
                title = "b",
                kind = StatsJournalEntry.KIND_DONE,
                eventAt = 1_000L,
                complexity = 2,
                completionQuality = null,
                eisenhowerQuadrant = null,
                weight = 2,
                leafScore = 0f,
                towardGoal = true,
                relatedGoalIds = listOf("g"),
                createdAt = 1_000L
            )
        )
        val p = GoalProgressCalculator.progressOf(
            "g",
            tasks.associateBy { it.id },
            links,
            journal = journal
        )
        // (2*1 + 2*0) / 4 = 0.5 — same as live weighted children case
        assertEquals(0.5f, p, 0.001f)
    }

    @Test
    fun planBreadth_narrowSingleBranchVsWideFourBranches() {
        val now = 1_700_000_000_000L
        val doneAt = now - 2_000L

        val narrowTasks = listOf(
            task("g", isGoal = true),
            task(
                "words",
                completed = true,
                complexity = 1,
                completedAt = doneAt,
                recurringTemplateId = "tpl-words"
            )
        )
        val narrowLinks = listOf(TaskLink("g", "words"))
        val narrow = ProjectStatsCalculator.computePlanBreadth(
            goalId = "g",
            tasksById = narrowTasks.associateBy { it.id },
            links = narrowLinks,
            liveDone = narrowTasks.filter { it.isCompleted },
            journalDone = emptyList()
        )

        val wideTasks = listOf(
            task("g", isGoal = true),
            task("words", completed = true, complexity = 1, completedAt = doneAt),
            task("listen", completed = true, complexity = 1, completedAt = doneAt),
            task("speak", completed = true, complexity = 1, completedAt = doneAt),
            task("grammar", completed = true, complexity = 1, completedAt = doneAt)
        )
        val wideLinks = listOf(
            TaskLink("g", "words"),
            TaskLink("g", "listen"),
            TaskLink("g", "speak"),
            TaskLink("g", "grammar")
        )
        val wide = ProjectStatsCalculator.computePlanBreadth(
            goalId = "g",
            tasksById = wideTasks.associateBy { it.id },
            links = wideLinks,
            liveDone = wideTasks.filter { it.isCompleted },
            journalDone = emptyList()
        )

        val fiveTasks = listOf(task("g", isGoal = true)) + (1..5).map { i ->
            task("step$i", completed = true, complexity = 1, completedAt = doneAt)
        }
        val fiveLinks = (1..5).map { i -> TaskLink("g", "step$i") }
        val five = ProjectStatsCalculator.computePlanBreadth(
            goalId = "g",
            tasksById = fiveTasks.associateBy { it.id },
            links = fiveLinks,
            liveDone = fiveTasks.filter { it.isCompleted },
            journalDone = emptyList()
        )

        assertEquals(1, narrow.branchCount)
        assertEquals(4, wide.branchCount)
        assertEquals(4, wide.touchedBranchCount)
        assertEquals(5, five.branchCount)
        assertTrue(
            "wide breadth (${wide.breadth}) should exceed narrow (${narrow.breadth})",
            wide.breadth > narrow.breadth + 0.4f
        )
        assertTrue(wide.breadth < 0.85f)
        assertTrue(five.breadth >= 0.95f)
        assertTrue(narrow.breadth < 0.20f)
    }

    @Test
    fun planBreadth_importantOrdinaryOutweighsRecurringOnly() {
        val doneAt = 1_700_000_000_000L - 1_000L
        val recurringOnly = listOf(
            task("g", isGoal = true),
            task("habit-a", completed = true, completedAt = doneAt, recurringTemplateId = "tpl-a"),
            task("habit-b", completed = true, completedAt = doneAt, recurringTemplateId = "tpl-b")
        )
        val recurringLinks = listOf(TaskLink("g", "habit-a"), TaskLink("g", "habit-b"))
        val thin = ProjectStatsCalculator.computePlanBreadth(
            goalId = "g",
            tasksById = recurringOnly.associateBy { it.id },
            links = recurringLinks,
            liveDone = recurringOnly.filter { it.isCompleted },
            journalDone = emptyList()
        )
        val mixed = listOf(
            task("g", isGoal = true),
            task("habit-a", completed = true, completedAt = doneAt, recurringTemplateId = "tpl-a"),
            task(
                "essay",
                completed = true,
                completedAt = doneAt,
                quality = 5,
                quadrant = "Q1"
            )
        )
        val mixedLinks = listOf(TaskLink("g", "habit-a"), TaskLink("g", "essay"))
        val broader = ProjectStatsCalculator.computePlanBreadth(
            goalId = "g",
            tasksById = mixed.associateBy { it.id },
            links = mixedLinks,
            liveDone = mixed.filter { it.isCompleted },
            journalDone = emptyList()
        )
        assertTrue(
            "ordinary plan (${broader.breadth}) should exceed recurring-only (${thin.breadth})",
            broader.breadth > thin.breadth + 0.15f
        )
    }

    @Test
    fun planBreadth_recurringConcentrationPenaltyOnMultiBranch() {
        val now = 1_700_000_000_000L
        val doneAt = now - 1_000L
        val tasks = listOf(
            task("g", isGoal = true),
            task(
                "words",
                completed = true,
                complexity = 2,
                completedAt = doneAt,
                recurringTemplateId = "tpl-words"
            ),
            task("listen", completed = false, complexity = 1),
            task("speak", completed = false, complexity = 1)
        )
        val links = listOf(
            TaskLink("g", "words"),
            TaskLink("g", "listen"),
            TaskLink("g", "speak")
        )
        val concentrated = ProjectStatsCalculator.computePlanBreadth(
            goalId = "g",
            tasksById = tasks.associateBy { it.id },
            links = links,
            liveDone = tasks.filter { it.isCompleted },
            journalDone = emptyList()
        )
        // Coverage 3/5=0.6, one recurring touch out of five → raw ~0.42, then ×0.62
        assertEquals(3, concentrated.branchCount)
        assertEquals(1, concentrated.touchedBranchCount)
        assertTrue(concentrated.breadth < 0.50f)
        assertTrue(concentrated.breadth > 0.20f)
    }

    @Test
    fun scheduleFit_noDeadline_isNeutral() {
        assertEquals(
            0.5f,
            ProjectStatsCalculator.scheduleFit(
                progress = 0.2f,
                deadlineAt = null,
                startAt = null,
                createdAt = 0L,
                now = 1_700_000_000_000L
            ),
            0.001f
        )
    }

    @Test
    fun scheduleFit_midwayOnTrack_isNearNeutral() {
        val start = day(2026, 9, 1)
        val deadline = day(2026, 9, 21)
        val now = day(2026, 9, 11) + 12 * 60 * 60_000L
        // 10 of 20 days elapsed → expected 0.5; progress 0.5 → fit 0.5
        assertEquals(
            0.5f,
            ProjectStatsCalculator.scheduleFit(
                progress = 0.5f,
                deadlineAt = deadline,
                startAt = start,
                createdAt = start,
                now = now
            ),
            0.001f
        )
    }

    @Test
    fun scheduleFit_overdueIncomplete_isLow() {
        val start = day(2026, 9, 1)
        val deadline = day(2026, 9, 10)
        val now = day(2026, 9, 20)
        val fit = ProjectStatsCalculator.scheduleFit(
            progress = 0.2f,
            deadlineAt = deadline,
            startAt = start,
            createdAt = start,
            now = now
        )
        assertTrue(fit < 0.35f)
        assertTrue(fit >= 0.15f)
    }

    @Test
    fun scheduleFit_aheadOfSchedule_isHigh() {
        val start = day(2026, 9, 1)
        val deadline = day(2026, 9, 21)
        val now = day(2026, 9, 6)
        // 5/20 = 0.25 expected; progress 0.8 → ahead 0.55 → fit ~0.775
        val fit = ProjectStatsCalculator.scheduleFit(
            progress = 0.8f,
            deadlineAt = deadline,
            startAt = start,
            createdAt = start,
            now = now
        )
        assertTrue(fit > 0.7f)
    }

    @Test
    fun outcomeCounts_doneAndNotDone_splitRecurring() {
        val now = 1_700_000_000_000L
        val tasks = listOf(
            task("g", isGoal = true),
            task("done", completed = true, completedAt = now - 1000),
            task("done-r", completed = true, completedAt = now - 2000, recurringTemplateId = "habit"),
            task("open"),
            task("miss", workflow = TaskWorkflow.NOT_DONE),
            task("miss-r", workflow = TaskWorkflow.NOT_DONE, recurringTemplateId = "habit")
        )
        val journal = listOf(
            journal("j-done", StatsJournalEntry.KIND_DONE, now - 500),
            journal("j-miss", StatsJournalEntry.KIND_NOT_DONE, now - 400, recurringTemplateId = "habit")
        )
        val snap = ProjectStatsCalculator.compute(
            projectTasks = tasks,
            links = emptyList(),
            templates = emptyList(),
            period = StatsPeriod.NOW,
            epochMillis = 0L,
            journal = journal,
            now = now
        )
        assertEquals(3, snap.doneInPeriod)
        assertEquals(1, snap.recurringFact)
        assertEquals(3, snap.notDoneCount)
        assertEquals(2, snap.recurringNotDoneCount)
        assertEquals(2, snap.openTaskCount)
    }

    @Test
    fun archiveClear_keepsEfficiencyAndCounts() {
        val now = 1_700_000_000_000L
        val tasks = listOf(
            task("g", isGoal = true),
            task("mid"),
            task("done", completed = true, completedAt = now - 1000, quality = 4, complexity = 2),
            task("open", complexity = 2),
            task("miss", workflow = TaskWorkflow.NOT_DONE, complexity = 1),
            task("loose", completed = true, completedAt = now - 500)
        )
        val links = listOf(
            TaskLink("g", "mid"),
            TaskLink("mid", "done"),
            TaskLink("mid", "open"),
            TaskLink("g", "miss")
        )
        val clearedIds = setOf("done", "open", "miss", "loose")
        val before = ProjectStatsCalculator.compute(
            projectTasks = tasks.map { t ->
                if (t.id in clearedIds) t.copy(isBoardArchived = true) else t
            },
            links = links,
            templates = emptyList(),
            period = StatsPeriod.NOW,
            epochMillis = 0L,
            now = now,
            selectedGoalId = "g"
        )
        val journal = tasks.filter { it.id in clearedIds }.map { cleared ->
            val kind = when {
                cleared.isCompleted -> StatsJournalEntry.KIND_DONE
                cleared.isNotDone -> StatsJournalEntry.KIND_NOT_DONE
                else -> StatsJournalEntry.KIND_OPEN
            }
            ProjectStatsCalculator.journalFromTask(cleared, "p", links, tasks, kind)
        }
        val after = ProjectStatsCalculator.compute(
            projectTasks = tasks.filter { it.id !in clearedIds },
            links = links.filter { it.parentId !in clearedIds && it.childId !in clearedIds },
            templates = emptyList(),
            period = StatsPeriod.NOW,
            epochMillis = 0L,
            journal = journal,
            now = now,
            selectedGoalId = "g"
        )
        assertEquals(before.selectedGoal!!.progress, after.selectedGoal!!.progress, 0.001f)
        assertEquals(before.hatP!!, after.hatP!!, 0.001f)
        assertEquals(before.chanceToComplete!!, after.chanceToComplete!!, 0.001f)
        assertEquals(before.doneInPeriod, after.doneInPeriod)
        assertEquals(before.notDoneCount, after.notDoneCount)
        assertEquals(before.openTaskCount, after.openTaskCount)
        assertEquals(before.triviaCount, after.triviaCount)
        assertEquals(before.selectedGoalDoneCount, after.selectedGoalDoneCount)
        assertEquals(before.selectedGoalNotDoneCount, after.selectedGoalNotDoneCount)
    }

    @Test
    fun goalDetails_countOnlySelectedGoal() {
        val now = 1_700_000_000_000L
        val tasks = listOf(
            task("g", isGoal = true),
            task("a", completed = true, completedAt = now - 1000, recurringTemplateId = "habit"),
            task("b", workflow = TaskWorkflow.NOT_DONE),
            task("h", isGoal = true),
            task("c", completed = true, completedAt = now - 500),
            task("d", workflow = TaskWorkflow.NOT_DONE, recurringTemplateId = "other")
        )
        val links = listOf(
            TaskLink("g", "a"),
            TaskLink("g", "b"),
            TaskLink("h", "c"),
            TaskLink("h", "d")
        )
        val snap = ProjectStatsCalculator.compute(
            projectTasks = tasks,
            links = links,
            templates = emptyList(),
            period = StatsPeriod.NOW,
            epochMillis = 0L,
            journal = listOf(
                journal("gone", StatsJournalEntry.KIND_DONE, now - 200, recurringTemplateId = "habit")
                    .copy(relatedGoalIds = listOf("g"))
            ),
            now = now,
            selectedGoalId = "g"
        )
        assertEquals(3, snap.doneInPeriod)
        assertEquals(2, snap.selectedGoalDoneCount)
        assertEquals(1, snap.selectedGoalNotDoneCount)
        assertEquals(2, snap.selectedGoalRecurringDoneCount)
        assertEquals(0, snap.selectedGoalRecurringNotDoneCount)
    }

    @Test
    fun chance_behindDeadlineLowersChance() {
        val base = ProjectStatsCalculator.chanceToComplete(
            hatP = 0.5f,
            progress = 0.4f,
            periodDelta = 0.1f,
            rhythmShare = 0.5f,
            triviaShare = 0.1f,
            planBreadth = 0.5f,
            scheduleFit = 0.5f
        )
        val behind = ProjectStatsCalculator.chanceToComplete(
            hatP = 0.5f,
            progress = 0.4f,
            periodDelta = 0.1f,
            rhythmShare = 0.5f,
            triviaShare = 0.1f,
            planBreadth = 0.5f,
            scheduleFit = 0.2f
        )
        assertTrue(behind < base)
        assertEquals(0.15f * 0.3f, base - behind, 0.001f)
    }

    private fun journal(
        id: String,
        kind: String,
        eventAt: Long,
        recurringTemplateId: String? = null
    ) = StatsJournalEntry(
        id = id,
        projectId = "p",
        sourceTaskId = id,
        title = id,
        kind = kind,
        eventAt = eventAt,
        complexity = 1,
        completionQuality = null,
        eisenhowerQuadrant = null,
        weight = 1,
        leafScore = if (kind == StatsJournalEntry.KIND_NOT_DONE) -1f else 1f,
        towardGoal = false,
        recurringTemplateId = recurringTemplateId,
        createdAt = eventAt
    )

    private fun day(y: Int, m: Int, d: Int): Long =
        java.util.Calendar.getInstance().apply {
            set(y, m - 1, d, 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
}
