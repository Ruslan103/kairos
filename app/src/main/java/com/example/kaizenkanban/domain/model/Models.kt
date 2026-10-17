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
    /** Minutes from midnight; null = use app default reminder when a due date is set. */
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
    val goalStatsEpochMillis: Long? = null,
    /** Optional visual group inside a hub; same id = same block. */
    val hubGroupId: String? = null,
    /**
     * When to start, minutes from midnight.
     * Null = app default (settings). If still null, the reminder clock is used for display.
     */
    val startMinutesOfDay: Int? = null,
    /** Latest day. Null = the start day ([dueDate]) is the deadline. */
    val deadlineAt: Long? = null,
    /** Countdown minutes started from the card button. Null / 0 = no timer. */
    val timerMinutes: Int? = null,
    /** plain | workout | shop | meet. Inner rows live in [cardPayload]. */
    val cardType: String = TaskCardKind.PLAIN,
    /** JSON body for [cardType]. Empty for a plain card. */
    val cardPayload: String = ""
) {
    fun isOnColumn(columnId: String): Boolean =
        this.columnId == columnId || linkedColumnIds.contains(columnId)

    val isNotDone: Boolean
        get() = !isCompleted && workflowStatus == TaskWorkflow.NOT_DONE

    val showsTimerAction: Boolean
        get() = !isCompleted && !isNotDone && (timerMinutes ?: 0) > 0
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
    val createdAt: Long,
    /** Parents this task had when it was cleared. Empty on older journal rows. */
    val parentIds: List<String> = emptyList(),
    /** Children this task had when it was cleared. */
    val childIds: List<String> = emptyList(),
    val isGoal: Boolean = false,
    val goalStatsEpochMillis: Long? = null,
    val dueDate: Long? = null,
    val deadlineAt: Long? = null,
    val taskCreatedAt: Long = 0L,
    /**
     * True when this row should stand in for the deleted task in every stat.
     * Older rows stay flat ghosts and do not rebuild the tree.
     */
    val replacesTask: Boolean = false
) {
    companion object {
        const val KIND_DONE = "done"
        const val KIND_NOT_DONE = "not_done"
        const val KIND_OPEN = "open"
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
    /** Minutes from midnight; each value → one instance whose start time and reminder time are this clock. Create time is settings-only. */
    val reminderTimesOfDay: List<Int> = listOf(TaskReminderDefaults.DEFAULT_MINUTES),
    val eisenhowerQuadrant: String? = null,
    val showEisenhowerButtons: Boolean = false,
    /** Parent goals applied to each spawned instance. */
    val linkParentIds: List<String> = emptyList(),
    /** Child steps applied to each spawned instance. */
    val linkChildIds: List<String> = emptyList(),
    /** Impact weight 1–5 copied onto spawned instances (null → treat as 1). */
    val complexity: Int? = null,
    /**
     * Occurrence keys `"$dayStartMillis:$minutes"` that were dismissed for the day
     * (user deleted the hub instance). Prevents same-day respawn.
     */
    val skippedOccurrenceKeys: List<String> = emptyList(),
    /**
     * Local start-of-day millis from which instances may be created.
     * 0 / unset → treat as start of [createdAt]'s day (legacy).
     */
    val startsOnDayMillis: Long = 0L,
    /** Days after the instance start. Together with [deadlineOffsetHours]. Null = no deadline. */
    val deadlineOffsetDays: Int? = null,
    /** Hours after the instance start, added to [deadlineOffsetDays]. */
    val deadlineOffsetHours: Int? = null,
    val createdAt: Long,
    /** Notify this long before each habit reminder slot. */
    val remindLeadAmount: Int? = null,
    /** [com.example.kaizenkanban.domain.model.RemindLeadUnit] id. */
    val remindLeadUnit: String? = null,
    /** Countdown minutes copied onto each spawned instance. Null / 0 = no timer. */
    val timerMinutes: Int? = null,
    /** Copied onto each spawned instance as [Task.hubGroupId]. */
    val hubGroupId: String? = null,
    /** Copied onto each spawned instance. Checks start cleared. */
    val cardType: String = TaskCardKind.PLAIN,
    val cardPayload: String = ""
)

object RecurringRhythm {
    const val DAILY = "daily"
    /** Legacy storage value — normalized to [WEEKDAYS] on read. */
    const val TIMES_PER_WEEK = "times_per_week"
    const val WEEKDAYS = "weekdays"
    /** Interval in days stored in [RecurringTemplate.timesPerWeek]. */
    const val EVERY_N_DAYS = "every_n_days"
    const val INTERVAL_MIN = 1
    const val INTERVAL_MAX = 999

    /** Any day count in [INTERVAL_MIN]..[INTERVAL_MAX]. Missing value stays 2. */
    fun intervalDays(raw: Int?): Int = (raw ?: 2).coerceIn(INTERVAL_MIN, INTERVAL_MAX)
}

/** Defaults for task due-date reminders (not recurring spawn). */
object TaskReminderDefaults {
    /** 09:00 — used when a task has a due date and no custom reminder. */
    const val DEFAULT_MINUTES = 9 * 60
}

/** Defaults for recurring template spawn (create) times — settings only, not per-template. */
object RecurringDefaults {
    /** 03:30 — when instances appear on the hub if none set in Settings. */
    const val DEFAULT_SPAWN_MINUTES = 3 * 60 + 30

    @Deprecated(
        "Spawn time, not reminder — use DEFAULT_SPAWN_MINUTES",
        ReplaceWith("RecurringDefaults.DEFAULT_SPAWN_MINUTES")
    )
    const val DEFAULT_REMINDER_MINUTES = DEFAULT_SPAWN_MINUTES

    /**
     * Template reminder slots. Legacy sole value [DEFAULT_SPAWN_MINUTES] was create-time
     * and is treated as unset → [reminderDefault].
     */
    fun effectiveReminderTimes(
        stored: List<Int>,
        reminderDefault: Int = TaskReminderDefaults.DEFAULT_MINUTES
    ): List<Int> {
        val raw = stored
            .map { it.coerceIn(0, 24 * 60 - 1) }
            .distinct()
            .sorted()
        val fallback = reminderDefault.coerceIn(0, 24 * 60 - 1)
        if (raw.isEmpty() || raw == listOf(DEFAULT_SPAWN_MINUTES)) {
            return listOf(fallback)
        }
        return raw
    }
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
    val position: Int = 0,
    /** Telegram, Instagram, or another link from the phone book. */
    val social: String = ""
)

/** Directed edge: [parentId] is a goal/container, [childId] is a step under it. */
data class TaskLink(
    val parentId: String,
    val childId: String,
    val createdAt: Long = System.currentTimeMillis()
)

/** Local image attached to a task (files live under app filesDir). */
data class TaskAttachment(
    val id: String,
    val taskId: String,
    /** Path relative to app filesDir, e.g. attachments/{projectId}/{taskId}/{id}.jpg */
    val relativePath: String,
    val mimeType: String = "image/jpeg",
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
) {
    companion object {
        const val MAX_PER_TASK = 10
    }
}