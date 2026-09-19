package com.example.kaizenkanban.domain.usecase

import com.example.kaizenkanban.domain.model.Comment
import com.example.kaizenkanban.domain.repository.KanbanRepository
import java.util.UUID

class AddCommentUseCase(private val repository: KanbanRepository) {
    suspend operator fun invoke(taskId: String, text: String) {
        if (text.isBlank()) return
        val comment = Comment(
            id = UUID.randomUUID().toString(),
            taskId = taskId,
            text = text.trim(),
            createdAt = System.currentTimeMillis()
        )
        repository.insertComment(comment)
    }
}
