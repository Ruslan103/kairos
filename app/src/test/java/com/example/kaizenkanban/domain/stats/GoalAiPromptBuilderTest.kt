package com.example.kaizenkanban.domain.stats

import com.example.kaizenkanban.domain.model.Column
import com.example.kaizenkanban.domain.model.RecurringRhythm
import com.example.kaizenkanban.domain.model.RecurringTemplate
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.domain.model.TaskLink
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class GoalAiPromptBuilderTest {

    private val tz = TimeZone.getTimeZone("UTC")
    private val locale = Locale.US

    private fun day(hour: Int, minute: Int = 0): Long {
        return Calendar.getInstance(tz).apply {
            set(2026, Calendar.SEPTEMBER, 26, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun task(
        id: String,
        title: String,
        completed: Boolean = false,
        due: Long? = null,
        completedAt: Long? = null,
        recurringId: String? = null,
        reminderMinutes: Int? = null
    ) = Task(
        id = id,
        columnId = "col",
        title = title,
        description = "",
        position = 0,
        categoryId = null,
        dueDate = due,
        isCompleted = completed,
        completedAt = completedAt,
        createdAt = 0L,
        recurringTemplateId = recurringId,
        reminderMinutesOfDay = reminderMinutes
    )

    @Test
    fun promptExplainsRecurringAndIncludesClockAndTimeline() {
        val goal = task("g", "Health", due = day(12, 0) + 5 * 86_400_000L, reminderMinutes = 8 * 60)
        val morningDone = task(
            id = "a",
            title = "Stretch",
            completed = true,
            due = day(7, 0),
            completedAt = day(7, 15),
            recurringId = "habit1",
            reminderMinutes = 7 * 60
        )
        val eveningOpen = task(
            id = "b",
            title = "Stretch",
            due = day(20, 0),
            recurringId = "habit1",
            reminderMinutes = 20 * 60
        )
        val overdue = task(
            id = "c",
            title = "Buy vitamins",
            due = day(8, 0) - 2 * 86_400_000L,
            reminderMinutes = 9 * 60
        )
        val links = listOf(
            TaskLink(parentId = "g", childId = "a", createdAt = 0L),
            TaskLink(parentId = "g", childId = "b", createdAt = 0L),
            TaskLink(parentId = "g", childId = "c", createdAt = 0L)
        )
        val templates = listOf(
            RecurringTemplate(
                id = "habit1",
                projectId = "p",
                title = "Stretch",
                rhythm = RecurringRhythm.DAILY,
                targetBoardId = "b",
                targetColumnId = "col",
                reminderTimesOfDay = listOf(7 * 60, 20 * 60),
                linkParentIds = listOf("g"),
                createdAt = 0L
            )
        )
        val columns = listOf(
            Column(id = "col", boardId = "b", title = "Do", position = 0)
        )
        val now = day(15, 30)
        val prompt = GoalAiPromptBuilder.build(
            kind = GoalAiPromptKind.WEEK,
            projectName = "Me",
            goal = goal,
            projectTasks = listOf(goal, morningDone, eveningOpen, overdue),
            links = links,
            columns = columns,
            templates = templates,
            childCount = 3,
            progressPercent = 40,
            nowMillis = now,
            timeZone = tz,
            locale = locale,
            defaultReminderMinutes = 9 * 60
        )

        assertTrue(prompt.contains("Now (user local): 2026-09-26 15:30"))
        assertTrue(prompt.contains("Start: 2026-10-01 08:00 · reminder 08:00 · Finish: same as start · planned"))
        assertTrue(prompt.contains("## Timer (separate from Finish)"))
        assertTrue(prompt.contains("habit instance (regularity — not a duplicate)"))
        assertTrue(prompt.contains("OVERDUE"))
        assertTrue(prompt.contains("due today"))
        assertTrue(prompt.contains("reminder 09:00"))
        assertTrue(prompt.contains("reminds at: 07:00, 20:00"))
        assertTrue(prompt.contains("Recurring habits ≠ duplicates"))
        assertTrue(prompt.contains("project Diary"))
        assertTrue(prompt.contains("workout"))
        assertFalse(prompt.contains("Project Notes"))
        assertFalse(prompt.contains("blindly duplicate existing steps"))
    }

    @Test
    fun promptShowsDeadlineNoneWhenGoalHasNoDueDate() {
        val goal = task("g", "Health")
        val prompt = GoalAiPromptBuilder.build(
            kind = GoalAiPromptKind.CHANCE,
            projectName = "Me",
            goal = goal,
            projectTasks = listOf(goal),
            links = emptyList(),
            columns = emptyList(),
            templates = emptyList(),
            childCount = 0,
            progressPercent = 0,
            nowMillis = day(12),
            timeZone = tz,
            locale = locale
        )
        assertTrue(prompt.contains("Start: none"))
    }

    @Test
    fun commandsPromptDocumentsWorkingFields() {
        val prompt = GoalAiPromptBuilder.buildCommandsPrompt(
            projectName = "Me",
            selectedGoal = task("g", "Health"),
            goals = listOf(task("g", "Health")),
            boards = emptyList(),
            columns = emptyList()
        )
        assertTrue(prompt.contains("timer: 20"))
        assertTrue(prompt.contains("timer is a separate setting (not Finish)"))
        assertTrue(prompt.contains("due: YYYY-MM-DD | today | tomorrow"))
        assertTrue(prompt.contains("start: HH:mm"))
        assertTrue(prompt.contains("deadline: YYYY-MM-DD | today | tomorrow"))
        assertTrue(prompt.contains("Finish (Закончить)"))
        assertTrue(prompt.contains("Start (Приступить)"))
        assertTrue(prompt.contains("every 3 days"))
        assertTrue(prompt.contains("type: plain | workout | shop | meet"))
        assertTrue(prompt.contains("line: \"Row text\" xN"))
        assertFalse(prompt.contains("note:"))
    }
}
