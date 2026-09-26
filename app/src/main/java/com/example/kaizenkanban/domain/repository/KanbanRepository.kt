package com.example.kaizenkanban.domain.repository

import com.example.kaizenkanban.domain.model.*
import kotlinx.coroutines.flow.Flow

interface KanbanRepository {
    // Projects
    fun getProjects(): Flow<List<Project>>
    suspend fun insertProject(project: Project)
    suspend fun insertProjects(projects: List<Project>)
    suspend fun deleteProject(projectId: String)
    suspend fun renameProject(projectId: String, newName: String)

    // Boards
    fun getBoardsByProject(projectId: String): Flow<List<Board>>
    fun getAllBoards(): Flow<List<Board>>
    suspend fun insertBoard(board: Board)
    suspend fun deleteBoard(boardId: String)
    suspend fun renameBoard(boardId: String, newName: String)
    suspend fun setDefaultBoard(boardId: String)
    suspend fun clearDefaultBoards()
    suspend fun setBoardArchived(boardId: String, archived: Boolean)

    // Categories
    fun getCategories(): Flow<List<Category>>
    suspend fun getCategoriesCount(): Int
    suspend fun insertCategory(category: Category)
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(categoryId: String)

    fun getAllColumns(): Flow<List<Column>>
    fun getColumnsByBoard(boardId: String): Flow<List<Column>>
    suspend fun insertColumn(column: Column)
    suspend fun insertColumns(columns: List<Column>)
    suspend fun deleteColumn(columnId: String)
    suspend fun renameColumn(columnId: String, newTitle: String)
    suspend fun getColumnsCount(boardId: String): Int

    // Tasks
    fun getAllTasks(): Flow<List<Task>>
    suspend fun insertTask(task: Task)
    suspend fun updateTasks(tasks: List<Task>)
    suspend fun deleteTask(taskId: String)
    suspend fun deleteTasksByColumn(columnId: String)
    suspend fun deleteCompletedTasksByColumn(columnId: String)

    // Comments
    fun getAllComments(): Flow<List<Comment>>
    suspend fun insertComment(comment: Comment)
    suspend fun deleteComment(commentId: String)

    // Column Comments
    fun getAllColumnComments(): Flow<List<ColumnComment>>
    suspend fun insertColumnComment(comment: ColumnComment)
    suspend fun insertColumnComments(comments: List<ColumnComment>)
    suspend fun deleteColumnComment(commentId: String)

    // Contacts
    fun getAllContacts(): Flow<List<Contact>>
    suspend fun insertContact(contact: Contact)
    suspend fun insertContacts(contacts: List<Contact>)
    suspend fun deleteContact(contactId: String)
    suspend fun deleteContactsByProject(projectId: String)

    // Recurring templates
    fun getAllRecurringTemplates(): Flow<List<RecurringTemplate>>
    suspend fun insertRecurringTemplate(template: RecurringTemplate)
    suspend fun updateRecurringTemplate(template: RecurringTemplate)
    suspend fun deleteRecurringTemplate(templateId: String)
    suspend fun deleteRecurringTemplatesByProject(projectId: String)

    // Task links (DAG)
    fun getAllTaskLinks(): Flow<List<TaskLink>>
    suspend fun insertTaskLink(link: TaskLink)
    suspend fun deleteTaskLink(parentId: String, childId: String)
    suspend fun deleteTaskLinksForTask(taskId: String)

    // Task attachments (local photos)
    fun getAllTaskAttachments(): Flow<List<TaskAttachment>>
    suspend fun insertTaskAttachment(attachment: TaskAttachment)
    suspend fun deleteTaskAttachment(id: String)
    suspend fun deleteAttachmentsByTask(taskId: String)
    suspend fun getAttachmentsForTask(taskId: String): List<TaskAttachment>

    // Stats journal
    fun getAllStatsJournal(): Flow<List<StatsJournalEntry>>
    suspend fun insertStatsJournal(entry: StatsJournalEntry)
    suspend fun deleteStatsJournal(id: String)
}