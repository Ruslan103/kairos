package com.example.kaizenkanban.domain.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskTimerPlannerTest {
    @Test
    fun endAtAddsMinutesFromNow() {
        val now = 1_000_000L
        assertEquals(now + 20 * 60_000L, TaskTimerPlanner.endAtMillis(now, 20))
        assertNull(TaskTimerPlanner.endAtMillis(now, null))
        assertNull(TaskTimerPlanner.endAtMillis(now, 0))
    }

    @Test
    fun canStartOnlyWhenIdleOrExpired() {
        val now = 5_000L
        assertTrue(TaskTimerPlanner.canStart(null, now))
        assertTrue(TaskTimerPlanner.canStart(now, now))
        assertTrue(TaskTimerPlanner.canStart(now - 1, now))
        assertFalse(TaskTimerPlanner.canStart(now + 1, now))
    }

    @Test
    fun remainHandSitsOnTheMinuteMark() {
        assertEquals(0f, TaskTimerPlanner.remainHandDegrees(0), 0.01f)
        assertEquals(90f, TaskTimerPlanner.remainHandDegrees(15 * 60_000L), 0.01f)
        assertEquals(180f, TaskTimerPlanner.remainHandDegrees(30 * 60_000L), 0.01f)
        assertEquals(0f, TaskTimerPlanner.remainHandDegrees(60 * 60_000L), 0.01f)
    }

    @Test
    fun remainClockPadsSeconds() {
        assertEquals("0:00", TaskTimerPlanner.remainClock(0))
        assertEquals("1:05", TaskTimerPlanner.remainClock(65_000))
        assertEquals("20:00", TaskTimerPlanner.remainClock(20 * 60_000L))
    }

    @Test
    fun remainProgressTracksElapsedShare() {
        assertEquals(1f, TaskTimerPlanner.remainProgress(20 * 60_000L, 20), 0.001f)
        assertEquals(0.5f, TaskTimerPlanner.remainProgress(10 * 60_000L, 20), 0.001f)
        assertEquals(0f, TaskTimerPlanner.remainProgress(0, 20), 0.001f)
        assertEquals(0f, TaskTimerPlanner.remainProgress(10_000L, null), 0.001f)
    }
}
