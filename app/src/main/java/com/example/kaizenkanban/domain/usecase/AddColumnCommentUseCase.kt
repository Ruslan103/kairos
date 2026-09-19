package com.example.kaizenkanban.domain.usecase

import com.example.kaizenkanban.domain.model.ColumnComment
import com.example.kaizenkanban.domain.repository.KanbanRepository
import java.util.UUID

class AddColumnCommentUseCase(private val repository: KanbanRepository) {
    suspend operator fun invoke(columnId: String, text: String) {
        if (text.isBlank()) return
        val comment = ColumnComment(
            id = UUID.randomUUID().toString(),
            columnId = columnId,
            text = text.trim(),
            createdAt = System.currentTimeMillis()
        )
        repository.insertColumnComment(comment)
    }
}
