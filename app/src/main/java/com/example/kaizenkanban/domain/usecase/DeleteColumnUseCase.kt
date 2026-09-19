package com.example.kaizenkanban.domain.usecase

import com.example.kaizenkanban.domain.repository.KanbanRepository

class DeleteColumnUseCase(private val repository: KanbanRepository) {
    suspend operator fun invoke(columnId: String) {
        repository.deleteColumn(columnId)
    }
}