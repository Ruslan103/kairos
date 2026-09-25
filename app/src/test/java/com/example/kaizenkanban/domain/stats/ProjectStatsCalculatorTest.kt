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
        isGoal: Boolean = false
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
        isGoal = isGoal
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
}
