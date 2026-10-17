package com.example.kaizenkanban.domain.model

import java.util.Calendar

/** A standalone notification: date, time, and text. */
data class KairosReminder(
    val id: String,
    val projectId: String,
    val text: String,
    val triggerAtMillis: Long,
    val taskId: String? = null,
    val recurringTemplateId: String? = null,
    /** How far before the task or habit slot this fires. Null for a calendar pick. */
    val leadAmount: Int? = null,
    val leadUnit: String? = null,
    val createdAt: Long
)

enum class RemindLeadUnit(val id: String) {
    HOUR("hour"),
    DAY("day"),
    WEEK("week"),
    MONTH("month");

    companion object {
        fun fromId(raw: String?): RemindLeadUnit? = entries.find { it.id == raw }
    }
}

object RemindLeadCalculator {
    fun atClock(dayStartMillis: Long, minutesOfDay: Int): Long {
        val minutes = minutesOfDay.coerceIn(0, 23 * 60 + 59)
        return Calendar.getInstance().apply {
            timeInMillis = dayStartMillis
            set(Calendar.HOUR_OF_DAY, minutes / 60)
            set(Calendar.MINUTE, minutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun taskAnchor(dueDate: Long, reminderMinutesOfDay: Int?, defaultMinutes: Int): Long {
        val minutes = reminderMinutesOfDay ?: defaultMinutes
        return atClock(dueDate, minutes)
    }

    fun subtract(anchorMillis: Long, amount: Int, unit: RemindLeadUnit): Long {
        val steps = amount.coerceAtLeast(0)
        return Calendar.getInstance().apply {
            timeInMillis = anchorMillis
            when (unit) {
                RemindLeadUnit.HOUR -> add(Calendar.HOUR_OF_DAY, -steps)
                RemindLeadUnit.DAY -> add(Calendar.DAY_OF_YEAR, -steps)
                RemindLeadUnit.WEEK -> add(Calendar.DAY_OF_YEAR, -steps * 7)
                RemindLeadUnit.MONTH -> add(Calendar.MONTH, -steps)
            }
        }.timeInMillis
    }
}
