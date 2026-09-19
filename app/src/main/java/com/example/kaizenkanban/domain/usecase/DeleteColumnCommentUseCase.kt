package com.example.kaizenkanban.domain.usecase

import com.example.kaizenkanban.domain.repository.KanbanRepository

class DeleteColumnCommentUseCase(private val repository: KanbanRepository) {
    suspend operator fun invoke(commentId: String) {
        repository.deleteColumnComment(commentId)
    }
}
