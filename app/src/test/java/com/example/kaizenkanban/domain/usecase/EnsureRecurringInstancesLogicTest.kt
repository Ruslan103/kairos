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
}
