package com.example.kaizenkanban.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class RemindLeadCalculatorTest {
    @Test
    fun subtractsDaysFromAnchorClock() {
        val anchor = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 28, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val trigger = RemindLeadCalculator.subtract(anchor, 2, RemindLeadUnit.DAY)
        val result = Calendar.getInstance().apply { timeInMillis = trigger }
        assertEquals(26, result.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, result.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, result.get(Calendar.MINUTE))
    }
}
