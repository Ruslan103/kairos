package com.example.kaizenkanban.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskDueSortTest {

    private fun task(startMinutes: Int?) = Task(
        id = "t",
        columnId = "hub",
        title = "t",
        description = "",
        position = 0,
        categoryId = null,
        dueDate = 1L,
        createdAt = 0L,
        startMinutesOfDay = startMinutes
    )

    @Test
    fun usesExplicitStartClock() {
        assertEquals(8 * 60 + 15, TaskDueSort.startMinutes(task(8 * 60 + 15), defaultMinutes = 9 * 60))
    }

    @Test
    fun fallsBackToSettingsDefault() {
        assertEquals(9 * 60, TaskDueSort.startMinutes(task(null), defaultMinutes = 9 * 60))
    }

    @Test
    fun sortsSameDayByStartClockIncludingDefault() {
        val morning = task(8 * 60)
        val defaultNine = task(null)
        val evening = task(18 * 60)
        val default = 9 * 60
        val ordered = listOf(evening, defaultNine, morning).sortedWith(
            compareBy { TaskDueSort.startMinutes(it, default) }
        )
        assertEquals(listOf(morning, defaultNine, evening), ordered)
    }
}
