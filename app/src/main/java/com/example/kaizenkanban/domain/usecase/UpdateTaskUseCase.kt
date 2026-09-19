package com.example.kaizenkanban.domain.usecase

import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.domain.repository.KanbanRepository

class UpdateTaskUseCase(private val repository: KanbanRepository) {
    suspend operator fun invoke(task: Task) {
        val normalized = if (task.dueDate == null && task.reminderMinutesOfDay != null) {
            task.copy(reminderMinutesOfDay = null)
        } else {
            task
        }
        repository.updateTasks(listOf(normalized))
    }
}