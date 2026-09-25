package com.example.kaizenkanban.domain.model

data class Project(
    val id: String,
    val name: String,
    val position: Int = 0
)

data class Board(
    val id: String,
    val projectId: String,
    val name: String,
    val isDefault: Boolean = false,
    val isArchived: Boolean = false
)

data class Category(
    val id: String,
    val name: String,
    val color: Long
)

data class Column(
    val id: String,
    val boardId: String,
    val title: String,
    val position: Int
)

data class Task(
    val id: String,
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
    val linkedColumnIds: List<String> = emptyList(),
    /** null | daily | weekly (legacy; UI hidden, auto-spawn disabled) */
    val repeatRule: String? = null,
    /** Minutes from midnight; null = use app default reminder time */
    val reminderMinutesOfDay: Int? = null,
    /** Set when spawned from a project recurring template */
    val recurringTemplateId: String? = null,
    /** Planned weight 1–5; null → treat as 1 in progress formulas later */
    val complexity: Int? = null,
    /** Estimated duration in minutes */
    val estimatedMinutes: Int? = null,
    /** How well it was done 1–5; only meaningful when completed */
    val completionQuality: Int? = null,
    /** open | done | not_done — kept in sync with [isCompleted] for done/open */
    val workflowStatus: String = TaskWorkflow.OPEN,
    /** Cleared from hub; kept for Archive / stats. Not shown on boards. */
    val isBoardArchived: Boolean = false,
    /** Soft-exclude from Review stats (task remains). */
    val statsExcluded: Boolean = false,
    /** Explicit goal mark — shown in Review → Goals (including when archived). */
    val isGoal: Boolean = false,
    /** When set on a goal, older completed steps no longer count in that goal’s progress. */
    val goalStatsEpochMillis: Long? = null
) {
    fun isOnColumn(columnId: String): Boolean =
        this.columnId == columnId || linkedColumnIds.contains(columnId)

    val isNotDone: Boolean
        get() = !isCompleted && workflowStatus == TaskWorkflow.NOT_DONE
}

/** Snapshot kept after hard-delete from Archive so Review history survives. */
data class StatsJournalEntry(
    val id: String,
    val projectId: String,
    val sourceTaskId: String,
    val title: String,
    /** done | not_done */
    val kind: String,
    val eventAt: Long,
    val complexity: Int?,
    val completionQuality: Int?,
    val eisenhowerQuadrant: String?,
    val weight: Int,
    val leafScore: Float,
    val towardGoal: Boolean,
    /** Goal ids this entry contributed to (comma-separated in DB). */
    val relatedGoalIds: List<String> = emptyList(),
    val recurringTemplateId: String? = null,
    val createdAt: Long
) {
    companion object {
        const val KIND_DONE = "done"
        const val KIND_NOT_DONE = "not_done"
    }
}

object TaskWorkflow {
    const val OPEN = "open"
    const val DONE = "done"
    const val NOT_DONE = "not_done"

    fun normalize(raw: String?, isCompleted: Boolean): String = when {
        isCompleted -> DONE
        raw == NOT_DONE -> NOT_DONE
        else -> OPEN
    }
}

object TaskRepeat {
    const val DAILY = "daily"
    const val WEEKLY = "weekly"
}

/** Project-owned habit template (not a board / hub). */
data class RecurringTemplate(
    val id: String,
    val projectId: String,
    val title: String,
    val rhythm: String,
    /** Kept for DB/legacy round-trip; unused by UI (prefer [weekdays]). */
    val timesPerWeek: Int? = null,
    /** ISO weekdays 1=Mon … 7=Sun */
    val weekdays: Set<Int> = emptySet(),
    val targetBoardId: String,
    val targetColumnId: String,
    val enabled: Boolean = true,
    /** Minutes from midnight; each value → one instance that day */
    val reminderTimesOfDay: List<Int> = listOf(9 * 60),
    val eisenhowerQuadrant: String? = null,
    val showEisenhowerButtons: Boolean = false,
    /** Parent goals applied to each spawned instance. */
    val linkParentIds: List<String> = emptyList(),
    /** Child steps applied to each spawned instance. */
    val linkChildIds: List<String> = emptyList(),
    /** Impact weight 1–5 copied onto spawned instances (null → treat as 1). */
    val complexity: Int? = null,
    val createdAt: Long
)

object RecurringRhythm {
    const val DAILY = "daily"
    /** Legacy storage value — normalized to [WEEKDAYS] on read. */
    const val TIMES_PER_WEEK = "times_per_week"
    const val WEEKDAYS = "weekdays"
    /** Interval in days stored in [RecurringTemplate.timesPerWeek]. */
    const val EVERY_N_DAYS = "every_n_days"
}

data class Comment(
    val id: String,
    val taskId: String,
    val text: String,
    val createdAt: Long
)

data class ColumnComment(
    val id: String,
    val columnId: String,
    val text: String,
    val createdAt: Long
)

data class Contact(
    val id: String,
    val projectId: String,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val role: String = "",
    val position: Int = 0
)

/** Directed edge: [parentId] is a goal/container, [childId] is a step under it. */
data class TaskLink(
    val parentId: String,
    val childId: String,
    val createdAt: Long = System.currentTimeMillis()
)