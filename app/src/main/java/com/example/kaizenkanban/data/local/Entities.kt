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
    val hubGroupId: String? = null
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
    val createdAt: Long
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
    /** Comma-separated minutes from midnight, e.g. "540,900,1260" */
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
    val createdAt: Long
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