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

        assertEquals(1, narrow.branchCount)
        assertEquals(4, wide.branchCount)
        assertEquals(4, wide.touchedBranchCount)
        assertTrue(
            "wide breadth (${wide.breadth}) should exceed narrow (${narrow.breadth})",
            wide.breadth > narrow.breadth + 0.15f
        )
        assertTrue(wide.breadth in 0.85f..1.01f)
        assertTrue(narrow.breadth < 0.45f)
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
        // Coverage 3/4=0.75, diversity 1/3≈0.33 → raw ~0.56, then *0.72 for concentration
        assertEquals(3, concentrated.branchCount)
        assertEquals(1, concentrated.touchedBranchCount)
        assertTrue(concentrated.breadth < 0.50f)
        assertTrue(concentrated.breadth > 0.20f)
    }

    @Test
    fun chance_widePlanBeatsNarrowHabitWithoutDoubling() {
        val hat = 0.5f
        val progress = 0.4f
        val delta = 0.1f
        val rhythm = 0.8f
        val trivia = 0.1f
        val narrowChance = ProjectStatsCalculator.chanceToComplete(
            hatP = hat,
            progress = progress,
            periodDelta = delta,
            rhythmShare = rhythm,
            triviaShare = trivia,
            planBreadth = 0.25f
        )
        val wideChance = ProjectStatsCalculator.chanceToComplete(
            hatP = hat,
            progress = progress,
            periodDelta = delta,
            rhythmShare = rhythm,
            triviaShare = trivia,
            planBreadth = 1.0f
        )
        assertTrue(wideChance > narrowChance)
        // 10% weight on breadth → delta ≈ 0.075, not a doubling
        val deltaChance = wideChance - narrowChance
        assertTrue(deltaChance in 0.05f..0.12f)
        assertTrue(wideChance < narrowChance * 1.5f)
    }
}
