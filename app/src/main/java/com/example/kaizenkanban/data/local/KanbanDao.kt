package com.example.kaizenkanban.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KanbanDao {
    // Projects
    @Query("SELECT * FROM projects ORDER BY position ASC, name ASC")
    fun getProjects(): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectEntity>)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProject(projectId: String)
    
    @Query("UPDATE projects SET name = :newName WHERE id = :projectId")
    suspend fun renameProject(projectId: String, newName: String)

    // Boards
    @Query("SELECT * FROM boards WHERE projectId = :projectId ORDER BY name ASC")
    fun getBoardsByProject(projectId: String): Flow<List<BoardEntity>>

    @Query("SELECT * FROM boards ORDER BY name ASC")
    fun getAllBoards(): Flow<List<BoardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoard(board: BoardEntity)

    @Query("DELETE FROM boards WHERE id = :boardId")
    suspend fun deleteBoard(boardId: String)
    
    @Query("UPDATE boards SET name = :newName WHERE id = :boardId")
    suspend fun renameBoard(boardId: String, newName: String)

    @Query("UPDATE boards SET isDefault = 0")
    suspend fun clearDefaultBoards()

    @Query("UPDATE boards SET isDefault = 1 WHERE id = :boardId")
    suspend fun setDefaultBoard(boardId: String)

    @Query("UPDATE boards SET isArchived = :archived WHERE id = :boardId")
    suspend fun setBoardArchived(boardId: String, archived: Boolean)

    // Categories
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getCategories(): Flow<List<CategoryEntity>>
    
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoriesCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategory(categoryId: String)

    @Query("SELECT * FROM columns ORDER BY position ASC")
    fun getAllColumns(): Flow<List<ColumnEntity>>
    @Query("SELECT * FROM columns WHERE boardId = :boardId ORDER BY position ASC")
    fun getColumnsByBoard(boardId: String): Flow<List<ColumnEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColumn(column: ColumnEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColumns(columns: List<ColumnEntity>)

    @Query("DELETE FROM columns WHERE id = :columnId")
    suspend fun deleteColumn(columnId: String)
    
    @Query("UPDATE columns SET title = :newTitle WHERE id = :columnId")
    suspend fun renameColumn(columnId: String, newTitle: String)

    @Query("SELECT COUNT(*) FROM columns WHERE boardId = :boardId")
    suspend fun getColumnsCount(boardId: String): Int

    // Tasks
    @Query("SELECT * FROM tasks ORDER BY columnId, position ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Update
    suspend fun updateTasks(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Query("DELETE FROM tasks WHERE columnId = :columnId")
    suspend fun deleteTasksByColumn(columnId: String)

    @Query("DELETE FROM tasks WHERE columnId = :columnId AND isCompleted = 1")
    suspend fun deleteCompletedTasksByColumn(columnId: String)

    // Comments
    @Query("SELECT * FROM comments ORDER BY createdAt ASC")
    fun getAllComments(): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Query("DELETE FROM comments WHERE id = :commentId")
    suspend fun deleteComment(commentId: String)

    @Query("DELETE FROM comments WHERE taskId = :taskId")
    suspend fun deleteCommentsByTask(taskId: String)

    // Column Comments
    @Query("SELECT * FROM column_comments ORDER BY createdAt ASC")
    fun getAllColumnComments(): Flow<List<ColumnCommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColumnComment(comment: ColumnCommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColumnComments(comments: List<ColumnCommentEntity>)

    @Query("DELETE FROM column_comments WHERE id = :commentId")
    suspend fun deleteColumnComment(commentId: String)

    @Query("DELETE FROM column_comments WHERE columnId = :columnId")
    suspend fun deleteColumnCommentsByColumn(columnId: String)

    // Contacts
    @Query("SELECT * FROM contacts ORDER BY position ASC, name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Query("DELETE FROM contacts WHERE id = :contactId")
    suspend fun deleteContact(contactId: String)

    @Query("DELETE FROM contacts WHERE projectId = :projectId")
    suspend fun deleteContactsByProject(projectId: String)

    // Recurring templates
    @Query("SELECT * FROM recurring_templates ORDER BY createdAt ASC")
    fun getAllRecurringTemplates(): Flow<List<RecurringTemplateEntity>>

    @Query("SELECT * FROM recurring_templates WHERE projectId = :projectId ORDER BY createdAt ASC")
    fun getRecurringTemplatesByProject(projectId: String): Flow<List<RecurringTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTemplate(template: RecurringTemplateEntity)

    @Update
    suspend fun updateRecurringTemplate(template: RecurringTemplateEntity)

    @Query("DELETE FROM recurring_templates WHERE id = :templateId")
    suspend fun deleteRecurringTemplate(templateId: String)

    @Query("DELETE FROM recurring_templates WHERE projectId = :projectId")
    suspend fun deleteRecurringTemplatesByProject(projectId: String)

    @Query("SELECT * FROM kairos_reminders ORDER BY triggerAtMillis ASC")
    fun getAllReminders(): Flow<List<KairosReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: KairosReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<KairosReminderEntity>)

    @Query("DELETE FROM kairos_reminders WHERE id = :id")
    suspend fun deleteReminder(id: String)

    @Query("DELETE FROM kairos_reminders WHERE taskId = :taskId")
    suspend fun deleteRemindersForTask(taskId: String)

    @Query("DELETE FROM kairos_reminders WHERE recurringTemplateId = :templateId")
    suspend fun deleteRemindersForTemplate(templateId: String)

    @Query("DELETE FROM kairos_reminders WHERE projectId = :projectId")
    suspend fun deleteRemindersByProject(projectId: String)

    // Task links (DAG)
    @Query("SELECT * FROM task_links ORDER BY createdAt ASC")
    fun getAllTaskLinks(): Flow<List<TaskLinkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskLink(link: TaskLinkEntity)

    @Query("DELETE FROM task_links WHERE parentId = :parentId AND childId = :childId")
    suspend fun deleteTaskLink(parentId: String, childId: String)

    @Query("DELETE FROM task_links WHERE parentId = :taskId OR childId = :taskId")
    suspend fun deleteTaskLinksForTask(taskId: String)

    // Task attachments (local photos)
    @Query("SELECT * FROM task_attachments ORDER BY sortOrder ASC, createdAt ASC")
    fun getAllTaskAttachments(): Flow<List<TaskAttachmentEntity>>

    @Query("SELECT * FROM task_attachments WHERE taskId = :taskId ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getAttachmentsForTask(taskId: String): List<TaskAttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskAttachment(attachment: TaskAttachmentEntity)

    @Query("DELETE FROM task_attachments WHERE id = :id")
    suspend fun deleteTaskAttachment(id: String)

    @Query("DELETE FROM task_attachments WHERE taskId = :taskId")
    suspend fun deleteAttachmentsByTask(taskId: String)

    // Stats journal (survives hard-delete from Archive)
    @Query("SELECT * FROM stats_journal ORDER BY eventAt DESC")
    fun getAllStatsJournal(): Flow<List<StatsJournalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatsJournal(entry: StatsJournalEntity)

    @Query("DELETE FROM stats_journal WHERE id = :id")
    suspend fun deleteStatsJournal(id: String)

    @Query("DELETE FROM stats_journal WHERE projectId = :projectId")
    suspend fun deleteStatsJournalByProject(projectId: String)

    // Project notes
    @Query("SELECT * FROM project_notes ORDER BY pinned DESC, position ASC, createdAt DESC")
    fun getAllProjectNotes(): Flow<List<ProjectNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectNote(note: ProjectNoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectNotes(notes: List<ProjectNoteEntity>)

    @Query("DELETE FROM project_notes WHERE id = :id")
    suspend fun deleteProjectNote(id: String)

    @Query("DELETE FROM project_notes WHERE projectId = :projectId")
    suspend fun deleteProjectNotesByProject(projectId: String)

    @Query("SELECT * FROM note_lines ORDER BY position ASC, createdAt ASC")
    fun getAllNoteLines(): Flow<List<NoteLineEntity>>

    @Query("SELECT * FROM note_lines WHERE noteId = :noteId ORDER BY position ASC, createdAt ASC")
    suspend fun getLinesForNote(noteId: String): List<NoteLineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteLines(lines: List<NoteLineEntity>)

    @Query("DELETE FROM note_lines WHERE id = :id")
    suspend fun deleteNoteLine(id: String)

    @Query("DELETE FROM note_lines WHERE noteId = :noteId")
    suspend fun deleteNoteLinesByNote(noteId: String)

    @Query("SELECT * FROM note_photos ORDER BY sortOrder ASC, createdAt ASC")
    fun getAllNotePhotos(): Flow<List<NotePhotoEntity>>

    @Query("SELECT * FROM note_photos WHERE noteId = :noteId ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getPhotosForNote(noteId: String): List<NotePhotoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotePhoto(photo: NotePhotoEntity)

    @Query("DELETE FROM note_photos WHERE id = :id")
    suspend fun deleteNotePhoto(id: String)

    @Query("DELETE FROM note_photos WHERE noteId = :noteId")
    suspend fun deleteNotePhotosByNote(noteId: String)

    @Query("SELECT * FROM note_photos WHERE lineId = :lineId")
    suspend fun getPhotosForLine(lineId: String): List<NotePhotoEntity>

    @Query("DELETE FROM note_photos WHERE lineId = :lineId")
    suspend fun deleteNotePhotosByLine(lineId: String)
}