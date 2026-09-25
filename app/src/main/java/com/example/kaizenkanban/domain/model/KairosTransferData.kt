package com.example.kaizenkanban.domain.model

data class KairosTransferData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val app: String = "Kairos",
    val projects: List<Project> = emptyList(),
    val boards: List<Board> = emptyList(),
    val categories: List<Category> = emptyList(),
    val columns: List<Column> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val columnComments: List<ColumnComment> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val taskLinks: List<TaskLink> = emptyList()
)
