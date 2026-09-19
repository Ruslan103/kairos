package com.example.kaizenkanban.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val position: Int = 0
)

@Entity(tableName = "boards")
data class BoardEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
    val isDefault: Boolean = false,
    val isArchived: Boolean = false
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: Long
)

@Entity(tableName = "columns")
data class ColumnEntity(
    @PrimaryKey val id: String,
    val boardId: String,
    val title: String,
    val position: Int
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
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
    val linkedColumnIds: String = "",
    val repeatRule: String? = null,
    val reminderMinutesOfDay: Int? = null
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val text: String,
    val createdAt: Long
)

@Entity(tableName = "column_comments")
data class ColumnCommentEntity(
    @PrimaryKey val id: String,
    val columnId: String,
    val text: String,
    val createdAt: Long
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val role: String = "",
    val position: Int = 0
)