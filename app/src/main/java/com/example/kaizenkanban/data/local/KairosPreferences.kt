package com.example.kaizenkanban.data.local

import android.content.Context

class KairosPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var appLanguageCode: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var enterAddsTask: Boolean
        get() = prefs.getBoolean(KEY_ENTER_ADDS_TASK, true)
        set(value) = prefs.edit().putBoolean(KEY_ENTER_ADDS_TASK, value).apply()

    var inProgressTaskId: String?
        get() = prefs.getString(KEY_IN_PROGRESS_TASK, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().putString(KEY_IN_PROGRESS_TASK, value.orEmpty()).apply()
        }

    var scheduledReminderTaskIds: Set<String>
        get() = prefs.getStringSet(KEY_REMINDER_TASKS, emptySet()).orEmpty()
        set(value) = prefs.edit().putStringSet(KEY_REMINDER_TASKS, value).apply()

    /** Default local hour (0–23) for due-date reminders */
    var reminderHour: Int
        get() = prefs.getInt(KEY_REMINDER_HOUR, 9).coerceIn(0, 23)
        set(value) = prefs.edit().putInt(KEY_REMINDER_HOUR, value.coerceIn(0, 23)).apply()

    /** Default local minute (0–59) for due-date reminders */
    var reminderMinute: Int
        get() = prefs.getInt(KEY_REMINDER_MINUTE, 0).coerceIn(0, 59)
        set(value) = prefs.edit().putInt(KEY_REMINDER_MINUTE, value.coerceIn(0, 59)).apply()

    /**
     * Board for quick-add tasks. Blank = primary (default) board.
     */
    var quickAddBoardId: String?
        get() = prefs.getString(KEY_QUICK_ADD_BOARD, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().putString(KEY_QUICK_ADD_BOARD, value.orEmpty()).apply()
        }

    /**
     * Hub/column for quick-add tasks. Blank = first hub on the chosen board.
     */
    var quickAddColumnId: String?
        get() = prefs.getString(KEY_QUICK_ADD_COLUMN, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().putString(KEY_QUICK_ADD_COLUMN, value.orEmpty()).apply()
        }

    /** system | light | dark */
    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    /** berry | sunset | ocean — colors Q1–Q4 buttons and task fills */
    var eisenhowerPalette: String
        get() {
            val raw = prefs.getString(KEY_EISENHOWER_PALETTE, "berry") ?: "berry"
            return when (raw) {
                "berry", "sunset", "ocean" -> raw
                else -> {
                    prefs.edit().putString(KEY_EISENHOWER_PALETTE, "berry").apply()
                    "berry"
                }
            }
        }
        set(value) = prefs.edit().putString(KEY_EISENHOWER_PALETTE, value).apply()

    var showArchivedBoards: Boolean
        get() = prefs.getBoolean(KEY_SHOW_ARCHIVED, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_ARCHIVED, value).apply()

    var hasSeenOnboarding: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING, value).apply()

    /** One-time planning guide on the OKR board */
    var hasSeenOkrPlanningGuide: Boolean
        get() = prefs.getBoolean(KEY_OKR_PLANNING_GUIDE, false)
        set(value) = prefs.edit().putBoolean(KEY_OKR_PLANNING_GUIDE, value).apply()

    var projectsCollapsed: Boolean
        get() = prefs.getBoolean(KEY_PROJECTS_COLLAPSED, false)
        set(value) = prefs.edit().putBoolean(KEY_PROJECTS_COLLAPSED, value).apply()

    /** Survives rename of the OKR board so init does not create a duplicate. */
    var okrBoardId: String?
        get() = prefs.getString(KEY_OKR_BOARD_ID, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().putString(KEY_OKR_BOARD_ID, value.orEmpty()).apply()
        }

    /** Survives rename of the Eisenhower board so init does not create a duplicate. */
    var eisenhowerBoardId: String?
        get() = prefs.getString(KEY_EISENHOWER_BOARD_ID, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().putString(KEY_EISENHOWER_BOARD_ID, value.orEmpty()).apply()
        }

    fun isColumnManuallyOrdered(columnId: String): Boolean {
        return prefs.getStringSet(KEY_MANUAL_ORDER_COLUMNS, emptySet())?.contains(columnId) == true
    }

    fun markColumnManuallyOrdered(columnId: String) {
        val current = prefs.getStringSet(KEY_MANUAL_ORDER_COLUMNS, emptySet()).orEmpty()
        if (columnId in current) return
        prefs.edit().putStringSet(KEY_MANUAL_ORDER_COLUMNS, current + columnId).apply()
    }

    /** Primary (home) hub for a board — opened first when entering the board. */
    fun getPrimaryHubId(boardId: String): String? {
        return prefs.getString(primaryHubKey(boardId), null)?.takeIf { it.isNotBlank() }
    }

    fun setPrimaryHubId(boardId: String, columnId: String?) {
        prefs.edit().putString(primaryHubKey(boardId), columnId.orEmpty()).apply()
    }

    /**
     * Bump when [InitializeDatabaseUseCase] seed/migration logic changes so warm
     * starts re-run the full path once, then skip expensive scans again.
     */
    var dbSeedVersion: Int
        get() = prefs.getInt(KEY_DB_SEED_VERSION, 0)
        set(value) = prefs.edit().putInt(KEY_DB_SEED_VERSION, value).apply()

    private fun primaryHubKey(boardId: String) = "$KEY_PRIMARY_HUB_PREFIX$boardId"

    private companion object {
        const val PREFS_NAME = "kairos_prefs"
        const val KEY_LANGUAGE = "app_language"
        const val KEY_ENTER_ADDS_TASK = "enter_adds_task"
        const val KEY_IN_PROGRESS_TASK = "in_progress_task_id"
        const val KEY_REMINDER_TASKS = "reminder_task_ids"
        const val KEY_REMINDER_HOUR = "reminder_hour"
        const val KEY_REMINDER_MINUTE = "reminder_minute"
        const val KEY_QUICK_ADD_BOARD = "quick_add_board_id"
        const val KEY_QUICK_ADD_COLUMN = "quick_add_column_id"
        const val KEY_MANUAL_ORDER_COLUMNS = "manual_order_columns"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_EISENHOWER_PALETTE = "eisenhower_palette"
        const val KEY_SHOW_ARCHIVED = "show_archived_boards"
        const val KEY_ONBOARDING = "has_seen_onboarding"
        const val KEY_OKR_PLANNING_GUIDE = "has_seen_okr_planning_guide"
        const val KEY_PROJECTS_COLLAPSED = "projects_collapsed"
        const val KEY_OKR_BOARD_ID = "okr_board_id"
        const val KEY_EISENHOWER_BOARD_ID = "eisenhower_board_id"
        const val KEY_PRIMARY_HUB_PREFIX = "primary_hub_"
        const val KEY_DB_SEED_VERSION = "db_seed_version"
    }
}
