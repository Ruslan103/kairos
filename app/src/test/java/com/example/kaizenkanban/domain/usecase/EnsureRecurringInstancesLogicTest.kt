package com.example.kaizenkanban.domain.usecase

import com.example.kaizenkanban.data.mapper.weekdaysForLegacyTimesPerWeek
import com.example.kaizenkanban.domain.model.Board
import com.example.kaizenkanban.domain.model.Column
import com.example.kaizenkanban.domain.model.RecurringDefaults
import com.example.kaizenkanban.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class EnsureRecurringInstancesLogicTest {

    @Test
    fun effectiveReminderTimes_legacySpawnBecomesDefaultReminder() {
        assertEquals(
            listOf(9 * 60),
            RecurringDefaults.effectiveReminderTimes(
                stored = listOf(RecurringDefaults.DEFAULT_SPAWN_MINUTES),
                reminderDefault = 9 * 60
            )
        )
        assertEquals(
            listOf(9 * 60),
            RecurringDefaults.effectiveReminderTimes(
                stored = emptyList(),
                reminderDefault = 9 * 60
            )
        )
        assertEquals(
            listOf(7 * 60, 20 * 60),
            RecurringDefaults.effectiveReminderTimes(
                stored = listOf(20 * 60, 7 * 60),
                reminderDefault = 9 * 60
            )
        )
    }

    @Test
    fun effectiveCreateMinutes_earlyWhenReminderBeforeSpawn() {
        assertEquals(
            2 * 60,
            EnsureRecurringInstancesUseCase.effectiveCreateMinutes(
                spawnMinutes = 3 * 60 + 30,
                reminderMinutes = 2 * 60
            )
        )
    }

    @Test
    fun effectiveCreateMinutes_atSpawnWhenReminderAfterOrEqual() {
        assertEquals(
            3 * 60 + 30,
            EnsureRecurringInstancesUseCase.effectiveCreateMinutes(
                spawnMinutes = 3 * 60 + 30,
                reminderMinutes = 9 * 60
            )
        )
        assertEquals(
            3 * 60 + 30,
            EnsureRecurringInstancesUseCase.effectiveCreateMinutes(
                spawnMinutes = 3 * 60 + 30,
                reminderMinutes = 3 * 60 + 30
            )
        )
    }

    @Test
    fun isoDayOfWeekThursday() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 24, 15, 0, 0) // Thursday
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(4, EnsureRecurringInstancesUseCase.isoDayOfWeek(cal.timeInMillis))
    }

    @Test
    fun legacyTimesPerWeekSpreadsAcrossWeek() {
        assertEquals(setOf(1, 3, 5), weekdaysForLegacyTimesPerWeek(3))
        assertEquals(setOf(1, 4), weekdaysForLegacyTimesPerWeek(2))
        assertEquals((1..7).toSet(), weekdaysForLegacyTimesPerWeek(7))
    }

    @Test
    fun occurrenceKeyStableForDayAndMinutes() {
        val day = EnsureRecurringInstancesUseCase.startOfDay(System.currentTimeMillis())
        assertEquals("$day:540", EnsureRecurringInstancesUseCase.occurrenceKey(day, 540))
    }

    @Test
    fun pruneSkippedKeysDropsOldEntries() {
        val today = EnsureRecurringInstancesUseCase.startOfDay(System.currentTimeMillis())
        val old = today - 90L * 24 * 60 * 60 * 1000
        val recent = today - 5L * 24 * 60 * 60 * 1000
        val pruned = EnsureRecurringInstancesUseCase.pruneSkippedKeys(
            listOf("$old:540", "$recent:540", "bad", "$today:900"),
            now = today + 12 * 60 * 60 * 1000
        )
        assertEquals(listOf("$recent:540", "$today:900"), pruned)
    }

    @Test
    fun editedInstanceTimeStillCoversTemplateSlot() {
        val day = EnsureRecurringInstancesUseCase.startOfDay(System.currentTimeMillis())
        val edited = task(
            dueDate = day + 10 * 60 * 60_000L,
            reminderMinutesOfDay = 600 // was 540, user changed time on instance
        )
        val need = EnsureRecurringInstancesUseCase.uncoveredTemplateSlots(
            templateMinutes = listOf(540),
            todayInstances = listOf(edited),
            dayStartMillis = day,
            skippedKeys = emptySet()
        )
        assertTrue(need.isEmpty())
    }

    @Test
    fun twoSlotsOneEditedInstanceCreatesOnlyMissingSlot() {
        val day = EnsureRecurringInstancesUseCase.startOfDay(System.currentTimeMillis())
        val editedMorning = task(
            dueDate = day + 10 * 60 * 60_000L,
            reminderMinutesOfDay = 600
        )
        val need = EnsureRecurringInstancesUseCase.uncoveredTemplateSlots(
            templateMinutes = listOf(540, 1080),
            todayInstances = listOf(editedMorning),
            dayStartMillis = day,
            skippedKeys = emptySet()
        )
        assertEquals(listOf(1080), need)
    }

    @Test
    fun exactMatchesPreferExactMinutes() {
        val day = EnsureRecurringInstancesUseCase.startOfDay(System.currentTimeMillis())
        val morning = task(dueDate = day + 9 * 60 * 60_000L, reminderMinutesOfDay = 540)
        val evening = task(dueDate = day + 18 * 60 * 60_000L, reminderMinutesOfDay = 1080)
        val need = EnsureRecurringInstancesUseCase.uncoveredTemplateSlots(
            templateMinutes = listOf(540, 1080),
            todayInstances = listOf(morning, evening),
            dayStartMillis = day,
            skippedKeys = emptySet()
        )
        assertTrue(need.isEmpty())
    }

    @Test
    fun skippedSlotNotCreatedEvenWithoutInstance() {
        val day = EnsureRecurringInstancesUseCase.startOfDay(System.currentTimeMillis())
        val skip = EnsureRecurringInstancesUseCase.occurrenceKey(day, 540)
        val need = EnsureRecurringInstancesUseCase.uncoveredTemplateSlots(
            templateMinutes = listOf(540),
            todayInstances = emptyList(),
            dayStartMillis = day,
            skippedKeys = setOf(skip)
        )
        assertTrue(need.isEmpty())
    }

    @Test
    fun futureStartsOnBlocksSpawn() {
        val today = EnsureRecurringInstancesUseCase.startOfDay(System.currentTimeMillis())
        val tomorrow = today + 24L * 60 * 60 * 1000
        val activeFrom = EnsureRecurringInstancesUseCase.effectiveStartsOnDay(tomorrow, today)
        assertEquals(tomorrow, activeFrom)
        assertTrue(today < activeFrom)
    }

    @Test
    fun openInstancesBeforeStartKeepsCardsOnOrAfterStartDay() {
        val today = EnsureRecurringInstancesUseCase.startOfDay(System.currentTimeMillis())
        val later = today + 7L * 24 * 60 * 60 * 1000
        val open = task(dueDate = today + 9 * 60 * 60_000L, reminderMinutesOfDay = 540)
        val done = open.copy(id = "done", isCompleted = true)
        val other = open.copy(id = "other", recurringTemplateId = "other")
        val onStart = open.copy(id = "on-start", dueDate = later + 9 * 60 * 60_000L)
        val ids = EnsureRecurringInstancesUseCase.openInstancesBeforeStart(
            tasks = listOf(open, done, other, onStart),
            templateId = "tpl",
            activeFrom = later
        ).map { it.id }
        assertEquals(listOf(open.id), ids)
    }

    @Test
    fun legacyZeroStartsOnUsesCreatedAtDay() {
        val created = EnsureRecurringInstancesUseCase.startOfDay(System.currentTimeMillis()) - 3L * 24 * 60 * 60 * 1000
        val activeFrom = EnsureRecurringInstancesUseCase.effectiveStartsOnDay(0L, created + 12 * 60 * 60 * 1000)
        assertEquals(created, activeFrom)
    }

    @Test
    fun dueTodayMovesOntoPrimaryHubAtSpawnTime() {
        val day = EnsureRecurringInstancesUseCase.startOfDay(System.currentTimeMillis())
        val end = day + 24L * 60 * 60 * 1000 - 1
        val columns = listOf(
            Column("inbox", "board", "Inbox", 0),
            Column("primary", "board", "Today", 1),
            Column("other", "board-2", "Later", 0)
        )
        val stay = plain("stay", "primary", 0, day + 8 * 60 * 60_000L)
        val today = plain("today", "inbox", 2, day + 15 * 60 * 60_000L, links = listOf("primary"))
        val habit = plain("habit", "inbox", 1, day + 9 * 60 * 60_000L, recurring = "tpl")
        val done = plain("done", "inbox", 0, day + 60 * 60_000L, completed = true)
        val tomorrow = plain("tomorrow", "inbox", 3, end + 1)
        val elsewhere = plain("elsewhere", "other", 0, day + 4 * 60 * 60_000L)
        val updates = EnsureRecurringInstancesUseCase.planDueTodayPrimaryMoves(
            tasks = listOf(stay, today, habit, done, tomorrow, elsewhere),
            columns = columns,
            primaryHubByBoardId = mapOf("board" to "primary"),
            dayStart = day,
            dayEnd = end
        )
        val moved = updates.find { it.id == "today" }
        assertEquals("primary", moved?.columnId)
        assertTrue(moved?.linkedColumnIds?.contains("primary") != true)
        assertEquals(1, moved?.position)
        assertEquals("inbox", updates.find { it.id == "habit" }?.columnId)
        assertEquals("inbox", updates.find { it.id == "tomorrow" }?.columnId)
        assertEquals("inbox", updates.find { it.id == "done" }?.columnId)
        assertTrue(updates.none { it.id == "elsewhere" })
        assertEquals(0, updates.find { it.id == "stay" }?.position)
    }

    @Test
    fun dueTodaySweepWaitsForSpawnAndRunsOnce() {
        val day = EnsureRecurringInstancesUseCase.startOfDay(System.currentTimeMillis())
        val before = day + 2 * 60 * 60_000L
        val after = day + 4 * 60 * 60_000L
        assertTrue(
            !EnsureRecurringInstancesUseCase.shouldSweepDueToday(
                now = before,
                spawnMinutes = 3 * 60 + 30,
                sweptDayStart = 0L,
                onlySlotsReadyByNow = true,
                hasPrimaryHub = true
            )
        )
        assertTrue(
            EnsureRecurringInstancesUseCase.shouldSweepDueToday(
                now = after,
                spawnMinutes = 3 * 60 + 30,
                sweptDayStart = 0L,
                onlySlotsReadyByNow = true,
                hasPrimaryHub = true
            )
        )
        assertTrue(
            !EnsureRecurringInstancesUseCase.shouldSweepDueToday(
                now = after,
                spawnMinutes = 3 * 60 + 30,
                sweptDayStart = day,
                onlySlotsReadyByNow = true,
                hasPrimaryHub = true
            )
        )
    }

    @Test
    fun resolvePrimaryHubsSkipsArchivedAndStale() {
        val boards = listOf(
            Board("live", "p", "Live", isArchived = false),
            Board("old", "p", "Old", isArchived = true)
        )
        val columns = listOf(
            Column("hub", "live", "Today", 0),
            Column("gone", "old", "Old hub", 0)
        )
        val resolved = EnsureRecurringInstancesUseCase.resolvePrimaryHubs(boards, columns) { boardId ->
            if (boardId == "live") "missing" else "gone"
        }
        assertTrue(resolved.isEmpty())
    }

    @Test
    fun instanceDeadlineAddsDaysAndHoursFromStart() {
        val start = 1_700_000_000_000L
        assertNull(EnsureRecurringInstancesUseCase.instanceDeadlineAt(start, null, null))
        assertNull(EnsureRecurringInstancesUseCase.instanceDeadlineAt(start, 0, 0))
        val twoDaysThreeHours = 2L * 24 * 60 * 60 * 1000 + 3L * 60 * 60 * 1000
        assertEquals(
            start + twoDaysThreeHours,
            EnsureRecurringInstancesUseCase.instanceDeadlineAt(start, 2, 3)
        )
        assertEquals(
            start + 5L * 60 * 60 * 1000,
            EnsureRecurringInstancesUseCase.instanceDeadlineAt(start, null, 5)
        )
    }

    private fun plain(
        id: String,
        columnId: String,
        position: Int,
        dueDate: Long?,
        completed: Boolean = false,
        links: List<String> = emptyList(),
        recurring: String? = null
    ): Task = Task(
        id = id,
        columnId = columnId,
        title = id,
        description = "",
        position = position,
        categoryId = null,
        dueDate = dueDate,
        isCompleted = completed,
        createdAt = 0L,
        linkedColumnIds = links,
        recurringTemplateId = recurring
    )

    private fun task(dueDate: Long, reminderMinutesOfDay: Int?): Task = Task(
        id = "t-${reminderMinutesOfDay ?: dueDate}",
        columnId = "col",
        title = "Habit",
        description = "",
        position = 0,
        categoryId = null,
        dueDate = dueDate,
        createdAt = dueDate,
        reminderMinutesOfDay = reminderMinutesOfDay,
        recurringTemplateId = "tpl"
    )
}
