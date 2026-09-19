package com.example.kaizenkanban.domain.model

import java.util.Calendar

object TaskRepeatHelper {
    fun nextDueDate(currentDue: Long?, rule: String, now: Long = System.currentTimeMillis()): Long {
        val startOfToday = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val noonToday = Calendar.getInstance().apply {
            timeInMillis = startOfToday
            set(Calendar.HOUR_OF_DAY, 12)
        }.timeInMillis
        val base = currentDue?.takeIf { it >= startOfToday } ?: noonToday
        val days = when (rule) {
            TaskRepeat.WEEKLY -> 7
            else -> 1
        }
        return Calendar.getInstance().apply {
            timeInMillis = base
            add(Calendar.DAY_OF_YEAR, days)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
