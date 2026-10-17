package com.example.kaizenkanban.domain.model

/**
 * Start clock used when ordering by due: explicit «Приступить» time, else settings default.
 */
object TaskDueSort {
    fun startMinutes(task: Task, defaultMinutes: Int): Int =
        (task.startMinutesOfDay ?: defaultMinutes).coerceIn(0, 23 * 60 + 59)
}
