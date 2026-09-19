package com.example.kaizenkanban.domain.usecase

import com.example.kaizenkanban.domain.repository.KanbanRepository

class SetDefaultBoardUseCase(private val repository: KanbanRepository) {
    suspend operator fun invoke(boardId: String) {
        repository.setDefaultBoard(boardId)
    }
}