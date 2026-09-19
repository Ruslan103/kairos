package com.example.kaizenkanban.domain.usecase

import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.domain.repository.KanbanRepository
import java.util.UUID

class AddTaskUseCase(private val repository: KanbanRepository) {
    suspend operator fun invoke(
        title: String,
        columnId: String,
        categoryId: String?,
        dueDate: Long?,
        currentTasksInColumn: List<Task>,
        eisenhowerQuadrant: String? = null,
        showEisenhowerButtons: Boolean = true,
        repeatRule: String? = null,
        reminderMinutesOfDay: Int? = null
    ) {
        if (title.isBlank()) return
        
        val nextPosition = currentTasksInColumn.maxOfOrNull { it.position }?.plus(1) ?: 0
        val task = Task(
            id = UUID.randomUUID().toString(),
            title = title,
            description = "",
            columnId = columnId,
            position = nextPosition,
            categoryId = categoryId,
            dueDate = dueDate,
            createdAt = System.currentTimeMillis(),
            eisenhowerQuadrant = eisenhowerQuadrant,
            showEisenhowerButtons = showEisenhowerButtons,
            repeatRule = repeatRule,
            reminderMinutesOfDay = if (dueDate != null) reminderMinutesOfDay else null
        )
        repository.insertTask(task)
    }
}