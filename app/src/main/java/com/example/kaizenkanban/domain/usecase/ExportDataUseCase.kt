package com.example.kaizenkanban.domain.usecase

import com.example.kaizenkanban.domain.model.KairosTransferData
import com.example.kaizenkanban.domain.repository.KanbanRepository
import kotlinx.coroutines.flow.first

class ExportDataUseCase(private val repository: KanbanRepository) {
    suspend operator fun invoke(): KairosTransferData {
        val projects = repository.getProjects().first()
        val boards = repository.getAllBoards().first()
        val categories = repository.getCategories().first()
        val columns = repository.getAllColumns().first()
        val tasks = repository.getAllTasks().first()
        val comments = repository.getAllComments().first()
        val columnComments = repository.getAllColumnComments().first()
        val contacts = repository.getAllContacts().first()
        val taskLinks = repository.getAllTaskLinks().first()

        return KairosTransferData(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            app = "Kairos",
            projects = projects,
            boards = boards,
            categories = categories,
            columns = columns,
            tasks = tasks,
            comments = comments,
            columnComments = columnComments,
            contacts = contacts,
            taskLinks = taskLinks
        )
    }
}
