package com.example.kaizenkanban.data.mapper

import com.example.kaizenkanban.data.local.*
import com.example.kaizenkanban.domain.model.*

fun ProjectEntity.toDomain() = Project(id, name, position)
fun Project.toEntity() = ProjectEntity(id, name, position)

fun BoardEntity.toDomain() = Board(id, projectId, name, isDefault, isArchived)
fun Board.toEntity() = BoardEntity(id, projectId, name, isDefault, isArchived)

fun CategoryEntity.toDomain() = Category(id, name, color)
fun Category.toEntity() = CategoryEntity(id, name, color)

fun ColumnEntity.toDomain() = Column(id, boardId, title, position)
fun Column.toEntity() = ColumnEntity(id, boardId, title, position)

fun String.toLinkedColumnIds(): List<String> =
    split(',').map { it.trim() }.filter { it.isNotEmpty() }

fun List<String>.toLinkedColumnIdsStorage(): String = joinToString(",")

fun TaskEntity.toDomain() = Task(
    id,
    columnId,
    title,
    description,
    position,
    categoryId,
    dueDate,
    isCompleted,
    completedAt,
    createdAt,
    eisenhowerQuadrant,
    showEisenhowerButtons,
    isHidden,
    linkedColumnIds.toLinkedColumnIds(),
    repeatRule,
    reminderMinutesOfDay
)
fun Task.toEntity() = TaskEntity(
    id,
    columnId,
    title,
    description,
    position,
    categoryId,
    dueDate,
    isCompleted,
    completedAt,
    createdAt,
    eisenhowerQuadrant,
    showEisenhowerButtons,
    isHidden,
    linkedColumnIds.toLinkedColumnIdsStorage(),
    repeatRule,
    reminderMinutesOfDay
)

fun CommentEntity.toDomain() = Comment(id, taskId, text, createdAt)
fun Comment.toEntity() = CommentEntity(id, taskId, text, createdAt)

fun ColumnCommentEntity.toDomain() = ColumnComment(id, columnId, text, createdAt)
fun ColumnComment.toEntity() = ColumnCommentEntity(id, columnId, text, createdAt)

fun ContactEntity.toDomain() = Contact(id, projectId, name, phone, email, role, position)
fun Contact.toEntity() = ContactEntity(id, projectId, name, phone, email, role, position)