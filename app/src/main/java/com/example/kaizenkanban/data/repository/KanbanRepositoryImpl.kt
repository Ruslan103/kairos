package com.example.kaizenkanban.data.repository

import com.example.kaizenkanban.data.local.AppDatabase
import com.example.kaizenkanban.data.local.KanbanDao
import com.example.kaizenkanban.data.local.TaskAttachmentStore
import com.example.kaizenkanban.data.mapper.toDomain
import com.example.kaizenkanban.data.mapper.toEntity
import com.example.kaizenkanban.data.mapper.toLinkedColumnIds
import com.example.kaizenkanban.data.mapper.toLinkedColumnIdsStorage
import com.example.kaizenkanban.domain.model.*
import com.example.kaizenkanban.domain.repository.KanbanRepository
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class KanbanRepositoryImpl(
    private val dao: KanbanDao,
    private val database: AppDatabase? = null,
    private val attachmentStore: TaskAttachmentStore? = null
) : KanbanRepository {

    private suspend fun <T> transactional(block: suspend () -> T): T {
        val db = database
        return if (db != null) {
            db.withTransaction { block() }
        } else {
            block()
        }
    }

    // Projects
    override fun getProjects(): Flow<List<Project>> = dao.getProjects().map { list -> list.map { it.toDomain() } }
    override suspend fun insertProject(project: Project) = dao.insertProject(project.toEntity())
    override suspend fun insertProjects(projects: List<Project>) = dao.insertProjects(projects.map { it.toEntity() })
    override suspend fun deleteProject(projectId: String) = transactional {
        val boardIds = dao.getAllBoards().first()
            .filter { it.projectId == projectId }
            .map { it.id }
        boardIds.forEach { id -> deleteBoardInternal(id) }
        dao.deleteContactsByProject(projectId)
        dao.deleteRecurringTemplatesByProject(projectId)
        dao.deleteProject(projectId)
    }
    override suspend fun renameProject(projectId: String, newName: String) = dao.renameProject(projectId, newName)
    
    // Boards
    override fun getBoardsByProject(projectId: String): Flow<List<Board>> = dao.getBoardsByProject(projectId).map { list -> list.map { it.toDomain() } }
    override fun getAllBoards(): Flow<List<Board>> = dao.getAllBoards().map { list -> list.map { it.toDomain() } }
    override suspend fun insertBoard(board: Board) = dao.insertBoard(board.toEntity())
    override suspend fun deleteBoard(boardId: String) = transactional {
        deleteBoardInternal(boardId)
    }

    private suspend fun deleteBoardInternal(boardId: String) {
        val columnIds = dao.getAllColumns().first()
            .filter { it.boardId == boardId }
            .map { it.id }
            .toSet()
        removeColumnsAndContents(columnIds)
        dao.deleteBoard(boardId)
    }
    override suspend fun renameBoard(boardId: String, newName: String) = dao.renameBoard(boardId, newName)
    
    override suspend fun setDefaultBoard(boardId: String) = transactional {
        dao.clearDefaultBoards()
        dao.setDefaultBoard(boardId)
    }

    override suspend fun clearDefaultBoards() {
        dao.clearDefaultBoards()
    }

    override suspend fun setBoardArchived(boardId: String, archived: Boolean) {
        dao.setBoardArchived(boardId, archived)
        if (archived) {
            val boards = dao.getAllBoards().first()
            val board = boards.find { it.id == boardId } ?: return
            if (board.isDefault) {
                dao.clearDefaultBoards()
                val candidates = boards.filter { it.id != boardId && !it.isArchived }
                val next = candidates.firstOrNull {
                    it.projectId == board.projectId && (
                        it.name.contains("OKR", ignoreCase = true) ||
                            it.name.contains("Лестница целей", ignoreCase = true) ||
                            it.name.contains("Goal ladder", ignoreCase = true)
                        )
                }
                    ?: candidates.firstOrNull { it.projectId == board.projectId }
                    ?: candidates.firstOrNull {
                        it.name.contains("OKR", ignoreCase = true) ||
                            it.name.contains("Лестница целей", ignoreCase = true) ||
                            it.name.contains("Goal ladder", ignoreCase = true)
                    }
                    ?: candidates.firstOrNull()
                if (next != null) {
                    dao.setDefaultBoard(next.id)
                }
            }
        }
    }

    // Categories
    override fun getCategories(): Flow<List<Category>> = dao.getCategories().map { list -> list.map { it.toDomain() } }
    override suspend fun getCategoriesCount(): Int = dao.getCategoriesCount()
    override suspend fun insertCategory(category: Category) = dao.insertCategory(category.toEntity())
    override suspend fun updateCategory(category: Category) = dao.updateCategory(category.toEntity())
    override suspend fun deleteCategory(categoryId: String) = dao.deleteCategory(categoryId)

    // Columns
    override fun getAllColumns(): Flow<List<Column>> = dao.getAllColumns().map { list -> list.map { it.toDomain() } }
    override fun getColumnsByBoard(boardId: String): Flow<List<Column>> = dao.getColumnsByBoard(boardId).map { list -> list.map { it.toDomain() } }
    override suspend fun insertColumn(column: Column) = dao.insertColumn(column.toEntity())
    override suspend fun insertColumns(columns: List<Column>) = dao.insertColumns(columns.map { it.toEntity() })
    override suspend fun deleteColumn(columnId: String) = transactional {
        removeColumnsAndContents(setOf(columnId))
    }
    override suspend fun renameColumn(columnId: String, newTitle: String) = dao.renameColumn(columnId, newTitle)
    override suspend fun getColumnsCount(boardId: String): Int = dao.getColumnsCount(boardId)

    // Tasks
    override fun getAllTasks(): Flow<List<Task>> = dao.getAllTasks().map { list -> list.map { it.toDomain() } }
    override suspend fun insertTask(task: Task) = dao.insertTask(task.toEntity())
    override suspend fun updateTasks(tasks: List<Task>) = dao.updateTasks(tasks.map { it.toEntity() })
    override suspend fun deleteTask(taskId: String) {
        val attachments = dao.getAttachmentsForTask(taskId)
        dao.deleteCommentsByTask(taskId)
        dao.deleteTaskLinksForTask(taskId)
        dao.deleteAttachmentsByTask(taskId)
        dao.deleteTask(taskId)
        attachments.forEach { attachmentStore?.deleteRelative(it.relativePath) }
    }
    override suspend fun deleteTasksByColumn(columnId: String) = dao.deleteTasksByColumn(columnId)
    override suspend fun deleteCompletedTasksByColumn(columnId: String) = dao.deleteCompletedTasksByColumn(columnId)

    // Comments
    override fun getAllComments(): Flow<List<Comment>> = dao.getAllComments().map { list -> list.map { it.toDomain() } }
    override suspend fun insertComment(comment: Comment) = dao.insertComment(comment.toEntity())
    override suspend fun deleteComment(commentId: String) = dao.deleteComment(commentId)

    // Column Comments
    override fun getAllColumnComments(): Flow<List<ColumnComment>> = dao.getAllColumnComments().map { list -> list.map { it.toDomain() } }
    override suspend fun insertColumnComment(comment: ColumnComment) = dao.insertColumnComment(comment.toEntity())
    override suspend fun insertColumnComments(comments: List<ColumnComment>) = dao.insertColumnComments(comments.map { it.toEntity() })
    override suspend fun deleteColumnComment(commentId: String) = dao.deleteColumnComment(commentId)

    // Contacts
    override fun getAllContacts(): Flow<List<Contact>> = dao.getAllContacts().map { list -> list.map { it.toDomain() } }
    override suspend fun insertContact(contact: Contact) = dao.insertContact(contact.toEntity())
    override suspend fun insertContacts(contacts: List<Contact>) = dao.insertContacts(contacts.map { it.toEntity() })
    override suspend fun deleteContact(contactId: String) = dao.deleteContact(contactId)
    override suspend fun deleteContactsByProject(projectId: String) = dao.deleteContactsByProject(projectId)

    // Recurring templates
    override fun getAllRecurringTemplates(): Flow<List<RecurringTemplate>> =
        dao.getAllRecurringTemplates().map { list -> list.map { it.toDomain() } }

    override suspend fun insertRecurringTemplate(template: RecurringTemplate) =
        dao.insertRecurringTemplate(template.toEntity())

    override suspend fun updateRecurringTemplate(template: RecurringTemplate) =
        dao.updateRecurringTemplate(template.toEntity())

    override suspend fun deleteRecurringTemplate(templateId: String) =
        dao.deleteRecurringTemplate(templateId)

    override suspend fun deleteRecurringTemplatesByProject(projectId: String) =
        dao.deleteRecurringTemplatesByProject(projectId)

    // Task links
    override fun getAllTaskLinks(): Flow<List<TaskLink>> =
        dao.getAllTaskLinks().map { list -> list.map { it.toDomain() } }

    override suspend fun insertTaskLink(link: TaskLink) =
        dao.insertTaskLink(link.toEntity())

    override suspend fun deleteTaskLink(parentId: String, childId: String) =
        dao.deleteTaskLink(parentId, childId)

    override suspend fun deleteTaskLinksForTask(taskId: String) =
        dao.deleteTaskLinksForTask(taskId)

    override fun getAllTaskAttachments(): Flow<List<TaskAttachment>> =
        dao.getAllTaskAttachments().map { list -> list.map { it.toDomain() } }

    override suspend fun insertTaskAttachment(attachment: TaskAttachment) =
        dao.insertTaskAttachment(attachment.toEntity())

    override suspend fun deleteTaskAttachment(id: String) {
        val existing = dao.getAllTaskAttachments().first().find { it.id == id }
        dao.deleteTaskAttachment(id)
        if (existing != null) attachmentStore?.deleteRelative(existing.relativePath)
    }

    override suspend fun deleteAttachmentsByTask(taskId: String) {
        val existing = dao.getAttachmentsForTask(taskId)
        dao.deleteAttachmentsByTask(taskId)
        existing.forEach { attachmentStore?.deleteRelative(it.relativePath) }
    }

    override suspend fun getAttachmentsForTask(taskId: String): List<TaskAttachment> =
        dao.getAttachmentsForTask(taskId).map { it.toDomain() }

    override fun getAllStatsJournal(): Flow<List<StatsJournalEntry>> =
        dao.getAllStatsJournal().map { list -> list.map { it.toDomain() } }

    override suspend fun insertStatsJournal(entry: StatsJournalEntry) =
        dao.insertStatsJournal(entry.toEntity())

    override suspend fun deleteStatsJournal(id: String) =
        dao.deleteStatsJournal(id)

    private suspend fun removeColumnsAndContents(columnIds: Set<String>) {
        if (columnIds.isEmpty()) return
        val allTasks = dao.getAllTasks().first()
        val updatedTasks = mutableListOf<com.example.kaizenkanban.data.local.TaskEntity>()
        val nextPosByColumn = mutableMapOf<String, Int>()
        fun nextPosition(columnId: String): Int {
            val next = nextPosByColumn.getOrPut(columnId) {
                allTasks.count { it.columnId == columnId && it.columnId !in columnIds }
            }
            nextPosByColumn[columnId] = next + 1
            return next
        }
        allTasks.forEach { task ->
            val links = task.linkedColumnIds.toLinkedColumnIds().filterNot { it in columnIds }
            val primaryRemoved = task.columnId in columnIds
            when {
                primaryRemoved && links.isEmpty() -> {
                    val attachments = dao.getAttachmentsForTask(task.id)
                    dao.deleteCommentsByTask(task.id)
                    dao.deleteTaskLinksForTask(task.id)
                    dao.deleteAttachmentsByTask(task.id)
                    dao.deleteTask(task.id)
                    attachments.forEach { attachmentStore?.deleteRelative(it.relativePath) }
                }
                primaryRemoved -> {
                    val newHome = links.first()
                    updatedTasks += task.copy(
                        columnId = newHome,
                        linkedColumnIds = links.drop(1).toLinkedColumnIdsStorage(),
                        position = nextPosition(newHome)
                    )
                }
                links.size != task.linkedColumnIds.toLinkedColumnIds().size ->
                    updatedTasks += task.copy(linkedColumnIds = links.toLinkedColumnIdsStorage())
            }
        }
        if (updatedTasks.isNotEmpty()) dao.updateTasks(updatedTasks)
        columnIds.forEach { columnId ->
            dao.deleteColumnCommentsByColumn(columnId)
            dao.deleteColumn(columnId)
        }
    }
}