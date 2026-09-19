package com.example.kaizenkanban.domain.usecase

import com.example.kaizenkanban.domain.model.Column
import com.example.kaizenkanban.domain.repository.KanbanRepository
import java.util.UUID

class AddColumnUseCase(private val repository: KanbanRepository) {
    suspend operator fun invoke(title: String, boardId: String, currentColumns: List<Column>) {
        if (title.isBlank()) return
        
        val nextPosition = currentColumns.maxOfOrNull { it.position }?.plus(1) ?: 0
        val column = Column(
            id = UUID.randomUUID().toString(),
            boardId = boardId,
            title = title,
            position = nextPosition
        )
        repository.insertColumn(column)
    }
}