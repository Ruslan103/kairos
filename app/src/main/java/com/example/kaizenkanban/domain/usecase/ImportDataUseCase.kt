package com.example.kaizenkanban.domain.usecase

import com.example.kaizenkanban.domain.model.KairosTransferData
import com.example.kaizenkanban.domain.model.TaskLink
import com.example.kaizenkanban.domain.model.TaskLinkGraph
import com.example.kaizenkanban.domain.repository.KanbanRepository
import kotlinx.coroutines.flow.first

class ImportDataUseCase(private val repository: KanbanRepository) {
    suspend operator fun invoke(data: KairosTransferData): Int {
        data.projects.forEach { project ->
            repository.insertProject(project)
        }

        val importingDefault = data.boards.any { it.isDefault }
        if (importingDefault) {
            repository.clearDefaultBoards()
        }

        data.boards.forEach { board ->
            repository.insertBoard(board)
        }

        // Keep exactly one primary if several arrived as default.
        val defaults = data.boards.filter { it.isDefault }
        if (defaults.size > 1) {
            val keep = defaults.find {
                it.name.contains("OKR", ignoreCase = true) ||
                    it.name.contains("Лестница целей", ignoreCase = true) ||
                    it.name.contains("Goal ladder", ignoreCase = true)
            } ?: defaults.first()
            repository.setDefaultBoard(keep.id)
        }

        data.categories.forEach { category ->
            repository.insertCategory(category)
        }

        if (data.columns.isNotEmpty()) {
            repository.insertColumns(data.columns)
        }

        val knownColumnIds = data.columns.map { it.id }.toSet()
        val importedTaskIds = mutableSetOf<String>()
        if (knownColumnIds.isNotEmpty()) {
            data.tasks.forEach { task ->
                if (task.columnId !in knownColumnIds) return@forEach
                repository.insertTask(
                    task.copy(linkedColumnIds = task.linkedColumnIds.filter { it in knownColumnIds })
                )
                importedTaskIds += task.id
            }
        }

        data.comments.forEach { comment ->
            if (comment.taskId in importedTaskIds) {
                repository.insertComment(comment)
            }
        }

        if (data.columnComments.isNotEmpty() && knownColumnIds.isNotEmpty()) {
            repository.insertColumnComments(
                data.columnComments.filter { it.columnId in knownColumnIds }
            )
        }

        if (data.contacts.isNotEmpty()) {
            repository.insertContacts(data.contacts)
        }

        val importedLinks = mutableListOf<TaskLink>()
        data.taskLinks.forEach { link ->
            if (link.parentId == link.childId) return@forEach
            if (link.parentId !in importedTaskIds || link.childId !in importedTaskIds) return@forEach
            if (TaskLinkGraph.wouldCreateCycle(importedLinks, link.parentId, link.childId)) return@forEach
            repository.insertTaskLink(link)
            importedLinks += link
        }

        // Ensure a usable primary remains after REPLACE merges (in-session, not only on next init).
        val boards = repository.getAllBoards().first()
        val active = boards.filter { !it.isArchived }
        if (active.none { it.isDefault }) {
            val next = active.firstOrNull {
                it.name.contains("OKR", ignoreCase = true) ||
                    it.name.contains("Лестница целей", ignoreCase = true) ||
                    it.name.contains("Goal ladder", ignoreCase = true)
            }
                ?: active.firstOrNull()
                ?: boards.firstOrNull()
            next?.let { repository.setDefaultBoard(it.id) }
        } else if (boards.count { it.isDefault } > 1) {
            val keep = active.find {
                it.isDefault && (
                    it.name.contains("OKR", ignoreCase = true) ||
                        it.name.contains("Лестница целей", ignoreCase = true) ||
                        it.name.contains("Goal ladder", ignoreCase = true)
                    )
            }
                ?: active.find { it.isDefault }
                ?: boards.first { it.isDefault }
            repository.setDefaultBoard(keep.id)
        }

        return importedTaskIds.size
    }
}
