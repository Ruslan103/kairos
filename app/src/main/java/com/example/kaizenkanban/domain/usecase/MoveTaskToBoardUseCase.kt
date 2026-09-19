package com.example.kaizenkanban.domain.usecase

import com.example.kaizenkanban.domain.model.Column
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.domain.repository.KanbanRepository

class MoveTaskToBoardUseCase(
    private val repository: KanbanRepository,
    private val moveTaskUseCase: MoveTaskUseCase
) {
    suspend operator fun invoke(
        task: Task,
        targetBoardId: String,
        allColumns: List<Column>,
        allTasks: List<Task>
    ) {
        val targetColumns = allColumns.filter { it.boardId == targetBoardId }.sortedBy { it.position }
        if (targetColumns.isNotEmpty()) {
            val targetColumn = targetColumns.first()
            val tasksInTarget = allTasks.filter { it.columnId == targetColumn.id }
            moveTaskUseCase(task, targetColumn.id, tasksInTarget.size, allTasks)
        }
    }
}
