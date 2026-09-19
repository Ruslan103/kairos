package com.example.kaizenkanban.domain.model

data class Project(
    val id: String,
    val name: String,
    val position: Int = 0
)

data class Board(
    val id: String,
    val projectId: String,
    val name: String,
    val isDefault: Boolean = false,
    val isArchived: Boolean = false
)

data class Category(
    val id: String,
    val name: String,
    val color: Long
)

data class Column(
    val id: String,
    val boardId: String,
    val title: String,
    val position: Int
)

data class Task(
    val id: String,
    val columnId: String,
    val title: String,
    val description: String,
    val position: Int,
    val categoryId: String?,
    val dueDate: Long?,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long,
    val eisenhowerQuadrant: String? = null,
    val showEisenhowerButtons: Boolean = false,
    val isHidden: Boolean = false,
    val linkedColumnIds: List<String> = emptyList(),
    /** null | daily | weekly */
    val repeatRule: String? = null,
    /** Minutes from midnight; null = use app default reminder time */
    val reminderMinutesOfDay: Int? = null
) {
    fun isOnColumn(columnId: String): Boolean =
        this.columnId == columnId || linkedColumnIds.contains(columnId)
}

object TaskRepeat {
    const val DAILY = "daily"
    const val WEEKLY = "weekly"
}

data class Comment(
    val id: String,
    val taskId: String,
    val text: String,
    val createdAt: Long
)

data class ColumnComment(
    val id: String,
    val columnId: String,
    val text: String,
    val createdAt: Long
)

data class Contact(
    val id: String,
    val projectId: String,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val role: String = "",
    val position: Int = 0
)