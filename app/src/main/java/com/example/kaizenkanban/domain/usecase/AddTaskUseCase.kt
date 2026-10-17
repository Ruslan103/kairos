package com.example.kaizenkanban.domain.usecase

import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.domain.model.TaskCardKind
import com.example.kaizenkanban.domain.model.TaskWorkflow
import com.example.kaizenkanban.domain.repository.KanbanRepository
import java.util.UUID

class AddTaskUseCase(private val repository: KanbanRepository) {
    suspend operator fun invoke(
        title: String,
        columnId: String,
        categoryId: String?,
        dueDate: Long?,
        currentTasksInColumn: List<Task>,
        eisenhowerQuadrant: String? = null,
        showEisenhowerButtons: Boolean = true,
        reminderMinutesOfDay: Int? = null,
        startMinutesOfDay: Int? = null,
        deadlineAt: Long? = null,
        recurringTemplateId: String? = null,
        complexity: Int? = null,
        estimatedMinutes: Int? = null,
        isGoal: Boolean = false,
        timerMinutes: Int? = null,
        cardType: String = TaskCardKind.PLAIN,
        cardPayload: String = ""
    ): String? {
        if (title.isBlank()) return null

        val nextPosition = currentTasksInColumn.maxOfOrNull { it.position }?.plus(1) ?: 0
        val task = Task(
            id = UUID.randomUUID().toString(),
            title = title,
            description = "",
            columnId = columnId,
            position = nextPosition,
            categoryId = categoryId,
            dueDate = dueDate,
            createdAt = System.currentTimeMillis(),
            eisenhowerQuadrant = eisenhowerQuadrant,
            showEisenhowerButtons = showEisenhowerButtons,
            repeatRule = null,
            reminderMinutesOfDay = if (dueDate != null) reminderMinutesOfDay else null,
            startMinutesOfDay = if (dueDate != null) startMinutesOfDay else null,
            deadlineAt = deadlineAt,
            recurringTemplateId = recurringTemplateId,
            complexity = complexity?.coerceIn(1, 5),
            estimatedMinutes = estimatedMinutes?.takeIf { it > 0 },
            isGoal = isGoal,
            timerMinutes = timerMinutes?.takeIf { it > 0 },
            cardType = TaskCardKind.normalize(cardType),
            cardPayload = if (TaskCardKind.normalize(cardType) == TaskCardKind.PLAIN) "" else cardPayload,
            workflowStatus = TaskWorkflow.OPEN
        )
        repository.insertTask(task)
        return task.id
    }
}
