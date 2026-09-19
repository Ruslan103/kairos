package com.example.kaizenkanban.domain.usecase

import com.example.kaizenkanban.domain.repository.KanbanRepository

class RenameColumnUseCase(private val repository: KanbanRepository) {
    suspend operator fun invoke(columnId: String, newTitle: String) {
        if (newTitle.isNotBlank()) {
            repository.renameColumn(columnId, newTitle)
        }
    }
}