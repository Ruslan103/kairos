package com.example.kaizenkanban.domain.calendar

import com.example.kaizenkanban.domain.model.RecurringRhythm
import com.example.kaizenkanban.domain.model.RecurringTemplate
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.domain.model.TaskWorkflow
import com.example.kaizenkanban.domain.usecase.EnsureRecurringInstancesUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CalendarEventBuilderTest {

    private fun day(y: Int, m: Int, d: Int): Long =
        Calendar.getInstance().apply {
            set(y, m - 1, d, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun skipsTasksWithoutDueDate() {
        val task = Task(
            id = "1", columnId = "c", title = "No date", description = "",
            position = 0, categoryId = null, dueDate = null, createdAt = 0L
        )
        val events = CalendarEventBuilder.build(
            tasks = listOf(task),
            projectTaskIds = setOf("1"),
            templates = emptyList(),
            rangeStart = day(2026, 9, 1),
            rangeEnd = day(2026, 9, 30),
            now = day(2026, 9, 15)
        )
        assertTrue(events.isEmpty())
    }

    @Test
    fun includesCompletedAndArchived() {
        val due = day(2026, 9, 20) + 12 * 60 * 60_000L
        val done = Task(
            id = "1", columnId = "c", title = "Done", description = "",
            position = 0, categoryId = null, dueDate = due, createdAt = 0L,
            isCompleted = true
        )
        val archived = Task(
            id = "2", columnId = "c", title = "Arch", description = "",
            position = 1, categoryId = null, dueDate = due, createdAt = 0L,
            isBoardArchived = true
        )
        val events = CalendarEventBuilder.build(
            tasks = listOf(done, archived),
            projectTaskIds = setOf("1", "2"),
            templates = emptyList(),
            rangeStart = day(2026, 9, 1),
            rangeEnd = day(2026, 9, 30),
            now = day(2026, 9, 15)
        )
        assertEquals(2, events.size)
        assertTrue(events.any { it.isCompleted })
        assertTrue(events.any { it.isArchived })
    }

    @Test
    fun projectsFutureDailyHabit() {
        val tpl = RecurringTemplate(
            id = "h1",
            projectId = "p",
            title = "Run",
            rhythm = RecurringRhythm.DAILY,
            targetBoardId = "b",
            targetColumnId = "c",
            reminderTimesOfDay = listOf(7 * 60),
            createdAt = day(2026, 9, 1)
        )
        val events = CalendarEventBuilder.build(
            tasks = emptyList(),
            projectTaskIds = emptySet(),
            templates = listOf(tpl),
            rangeStart = day(2026, 9, 1),
            rangeEnd = day(2026, 9, 3),
            now = day(2026, 9, 1)
        )
        assertTrue(events.all { it.isProjected })
        assertEquals(3, events.size)
    }

    @Test
    fun deletedHabitBeforeStatusIsNotProjected() {
        val start = day(2026, 9, 1)
        val skippedDay = day(2026, 9, 2)
        val minutes = 7 * 60
        val tpl = RecurringTemplate(
            id = "h1",
            projectId = "p",
            title = "Run",
            rhythm = RecurringRhythm.DAILY,
            targetBoardId = "b",
            targetColumnId = "c",
            reminderTimesOfDay = listOf(minutes),
            skippedOccurrenceKeys = listOf(
                EnsureRecurringInstancesUseCase.occurrenceKey(skippedDay, minutes)
            ),
            createdAt = start
        )
        val events = CalendarEventBuilder.build(
            tasks = emptyList(),
            projectTaskIds = emptySet(),
            templates = listOf(tpl),
            rangeStart = start,
            rangeEnd = day(2026, 9, 3),
            now = start
        )
        assertFalse(events.any { it.dayStartMillis == skippedDay })
        assertEquals(2, events.size)
        assertEquals(listOf(minutes), events.first().startMinutes)
    }

    @Test
    fun archivedNotDoneStaysEvenWhenHidden() {
        val due = day(2026, 9, 20) + 12 * 60 * 60_000L
        val task = Task(
            id = "1", columnId = "c", title = "Missed", description = "",
            position = 0, categoryId = null, dueDate = due, createdAt = 0L,
            isHidden = true,
            isBoardArchived = true,
            workflowStatus = TaskWorkflow.NOT_DONE
        )
        val events = CalendarEventBuilder.build(
            tasks = listOf(task),
            projectTaskIds = setOf("1"),
            templates = emptyList(),
            rangeStart = day(2026, 9, 1),
            rangeEnd = day(2026, 9, 30),
            now = day(2026, 9, 15)
        )
        assertEquals(1, events.size)
        assertTrue(events.single().isArchived)
    }

    @Test
    fun hiddenOpenHabitIsNotReplacedByAProjection() {
        val start = day(2026, 9, 1)
        val due = start + 7 * 60 * 60_000L
        val task = Task(
            id = "1", columnId = "c", title = "Run", description = "",
            position = 0, categoryId = null, dueDate = due, createdAt = 0L,
            isHidden = true,
            recurringTemplateId = "h1"
        )
        val tpl = RecurringTemplate(
            id = "h1",
            projectId = "p",
            title = "Run",
            rhythm = RecurringRhythm.DAILY,
            targetBoardId = "b",
            targetColumnId = "c",
            reminderTimesOfDay = listOf(7 * 60),
            createdAt = start
        )
        val events = CalendarEventBuilder.build(
            tasks = listOf(task),
            projectTaskIds = setOf("1"),
            templates = listOf(tpl),
            rangeStart = start,
            rangeEnd = day(2026, 9, 2),
            now = start
        )
        assertTrue(events.none { it.dayStartMillis == start })
        assertEquals(1, events.size)
        assertTrue(events.single().isProjected)
    }

    @Test
    fun highlightMarksOccurrenceAndDeadline() {
        val start = day(2026, 9, 20)
        val due = start + 9 * 60 * 60_000L
        val deadline = day(2026, 9, 25) + 12 * 60 * 60_000L
        val task = Task(
            id = "1", columnId = "c", title = "Ship", description = "",
            position = 0, categoryId = null, dueDate = due, createdAt = 0L,
            deadlineAt = deadline
        )
        val events = CalendarEventBuilder.build(
            tasks = listOf(task),
            projectTaskIds = setOf("1"),
            templates = emptyList(),
            rangeStart = day(2026, 9, 1),
            rangeEnd = day(2026, 9, 30),
            now = day(2026, 9, 15)
        )
        val highlight = CalendarEventBuilder.highlight("1", events, listOf(task))
        assertEquals("Ship", highlight?.title)
        assertEquals(setOf(start), highlight?.occurrenceDays)
        assertEquals(setOf(day(2026, 9, 25)), highlight?.deadlineDays)
    }

    @Test
    fun weekendDetection() {
        // 2026-09-26 is Saturday
        assertTrue(CalendarEventBuilder.isWeekend(day(2026, 9, 26)))
        assertTrue(CalendarEventBuilder.isWeekend(day(2026, 9, 27)))
        assertFalse(CalendarEventBuilder.isWeekend(day(2026, 9, 28)))
    }
}
