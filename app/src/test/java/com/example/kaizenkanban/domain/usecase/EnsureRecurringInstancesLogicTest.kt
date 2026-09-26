package com.example.kaizenkanban.domain.usecase

import com.example.kaizenkanban.data.mapper.weekdaysForLegacyTimesPerWeek
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class EnsureRecurringInstancesLogicTest {

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
}
