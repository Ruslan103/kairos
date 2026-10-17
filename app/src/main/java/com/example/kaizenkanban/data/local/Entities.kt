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
    val reminderMinutesOfDay: Int? = null,
    val recurringTemplateId: String? = null,
    val complexity: Int? = null,
    val estimatedMinutes: Int? = null,
    val completionQuality: Int? = null,
    val workflowStatus: String = "open",
    val isBoardArchived: Boolean = false,
    val statsExcluded: Boolean = false,
    val isGoal: Boolean = false,
    val goalStatsEpochMillis: Long? = null,
    val hubGroupId: String? = null,
    val startMinutesOfDay: Int? = null,
    val deadlineAt: Long? = null,
    val timerMinutes: Int? = null,
    /** plain | workout | shop | meet */
    val cardType: String = "plain",
    val cardPayload: String = ""
)

@Entity(tableName = "stats_journal")
data class StatsJournalEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val sourceTaskId: String,
    val title: String,
    val kind: String,
    val eventAt: Long,
    val complexity: Int? = null,
    val completionQuality: Int? = null,
    val eisenhowerQuadrant: String? = null,
    val weight: Int = 1,
    val leafScore: Float = 0f,
    val towardGoal: Boolean = false,
    /** Comma-separated goal task ids this entry counts toward. */
    val relatedGoalIds: String = "",
    val recurringTemplateId: String? = null,
    val createdAt: Long,
    val parentIds: String = "",
    val childIds: String = "",
    val isGoal: Boolean = false,
    val goalStatsEpochMillis: Long? = null,
    val dueDate: Long? = null,
    val deadlineAt: Long? = null,
    val taskCreatedAt: Long = 0L,
    val replacesTask: Boolean = false
)

@Entity(tableName = "recurring_templates")
data class RecurringTemplateEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val title: String,
    /** daily | weekdays (legacy: times_per_week normalized on read) */
    val rhythm: String,
    val timesPerWeek: Int? = null,
    /** Comma-separated ISO weekdays 1=Mon … 7=Sun */
    val weekdays: String = "",
    val targetBoardId: String,
    val targetColumnId: String,
    val enabled: Boolean = true,
    /** Comma-separated reminder minutes from midnight, e.g. "540,900,1260" */
    val reminderTimesOfDay: String = "540",
    val eisenhowerQuadrant: String? = null,
    val showEisenhowerButtons: Boolean = false,
    /** Comma-separated parent task ids for spawned instances */
    val linkParentIds: String = "",
    /** Comma-separated child task ids for spawned instances */
    val linkChildIds: String = "",
    val complexity: Int? = null,
    /** Comma-separated `"dayStart:minutes"` keys for dismissed occurrences */
    val skippedOccurrenceKeys: String = "",
    /**
     * Local start-of-day millis; instances spawn only on/after this day.
     * 0 = legacy (use createdAt's day).
     */
    val startsOnDayMillis: Long = 0L,
    /** Days after each instance start. Combined with [deadlineOffsetHours]. Null = no deadline. */
    val deadlineOffsetDays: Int? = null,
    /** Hours after each instance start, added on top of [deadlineOffsetDays]. */
    val deadlineOffsetHours: Int? = null,
    val createdAt: Long,
    /** Lead before each habit reminder slot. Null = no extra reminder. */
    val remindLeadAmount: Int? = null,
    /** hour | day | week | month */
    val remindLeadUnit: String? = null,
    /** Countdown minutes copied onto each spawned instance. */
    val timerMinutes: Int? = null,
    /** Shared with spawned hub tasks so a habit group stays one block. */
    val hubGroupId: String? = null,
    /** plain | workout | shop | meet. Copied onto each instance. */
    val cardType: String = "plain",
    val cardPayload: String = ""
)

@Entity(tableName = "kairos_reminders")
data class KairosReminderEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val text: String,
    val triggerAtMillis: Long,
    val taskId: String? = null,
    val recurringTemplateId: String? = null,
    val leadAmount: Int? = null,
    val leadUnit: String? = null,
    val createdAt: Long
)

@Entity(tableName = "project_notes")
data class ProjectNoteEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
    val position: Int = 0,
    val pinned: Boolean = false,
    val linkedTaskId: String? = null,
    val reminderAtMillis: Long? = null,
    val title: String = ""
)

@Entity(tableName = "note_lines")
data class NoteLineEntity(
    @PrimaryKey val id: String,
    val noteId: String,
    val text: String,
    val createdAt: Long,
    val position: Int
)

@Entity(tableName = "note_photos")
data class NotePhotoEntity(
    @PrimaryKey val id: String,
    val noteId: String,
    /** MediaStore content URI string. */
    val mediaUri: String,
    val createdAt: Long = 0L,
    val sortOrder: Int = 0,
    val lineId: String = ""
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
    val position: Int = 0,
    val social: String = ""
)

@Entity(
    tableName = "task_links",
    primaryKeys = ["parentId", "childId"]
)
data class TaskLinkEntity(
    val parentId: String,
    val childId: String,
    val createdAt: Long = 0L
)

@Entity(tableName = "task_attachments")
data class TaskAttachmentEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val relativePath: String,
    val mimeType: String = "image/jpeg",
    val createdAt: Long = 0L,
    val sortOrder: Int = 0
)