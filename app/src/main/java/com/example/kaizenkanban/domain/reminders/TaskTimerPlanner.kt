package com.example.kaizenkanban.domain.reminders

object TaskTimerPlanner {
    fun normalizedMinutes(raw: Int?): Int? = raw?.takeIf { it > 0 }?.coerceIn(1, 999)

    fun endAtMillis(nowMillis: Long, minutes: Int?): Long? {
        val duration = normalizedMinutes(minutes) ?: return null
        return nowMillis + duration * 60_000L
    }

    fun canStart(existingEndMillis: Long?, nowMillis: Long): Boolean =
        existingEndMillis == null || existingEndMillis <= nowMillis

    fun remainingMillis(endAtMillis: Long?, nowMillis: Long): Long {
        val end = endAtMillis ?: return 0L
        return (end - nowMillis).coerceAtLeast(0L)
    }

    /** Clockwise degrees from 12 for a minute hand of the time still left. */
    fun remainHandDegrees(remainMillis: Long): Float {
        val minuteMs = 60_000L
        val withinHour = remainMillis.coerceAtLeast(0L) % (60L * minuteMs)
        return withinHour.toFloat() / minuteMs.toFloat() / 60f * 360f
    }

    fun remainClock(remainMillis: Long): String {
        val totalSec = (remainMillis / 1000L).coerceAtLeast(0L).toInt()
        val minutes = totalSec / 60
        val seconds = totalSec % 60
        return "%d:%02d".format(minutes, seconds)
    }

    fun remainProgress(remainMillis: Long, timerMinutes: Int?): Float {
        val total = (normalizedMinutes(timerMinutes) ?: return 0f) * 60_000f
        if (total <= 0f) return 0f
        return (remainMillis / total).coerceIn(0f, 1f)
    }
}
