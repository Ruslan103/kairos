package com.example.kaizenkanban.domain.usecase

import com.example.kaizenkanban.domain.repository.KanbanRepository

class DeleteTaskUseCase(private val repository: KanbanRepository) {
    suspend operator fun invoke(taskId: String) {
        repository.deleteTask(taskId)
    }
}