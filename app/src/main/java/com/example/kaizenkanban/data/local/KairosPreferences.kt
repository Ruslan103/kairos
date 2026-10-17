package com.example.kaizenkanban.data.local

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.util.Calendar

class KairosPreferences(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var appLanguageCode: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var enterAddsTask: Boolean
        get() = prefs.getBoolean(KEY_ENTER_ADDS_TASK, true)
        set(value) = prefs.edit().putBoolean(KEY_ENTER_ADDS_TASK, value).apply()

    var inProgressTaskId: String?
        get() = prefs.getString(KEY_IN_PROGRESS_TASK, null)?.takeIf { it.isNotBlank() }
        set(value) {
            if (!value.isNullOrBlank()) recordFocusDay(System.currentTimeMillis())
            prefs.edit().putString(KEY_IN_PROGRESS_TASK, value.orEmpty()).apply()
        }

    /** Local midnight millis for days when focus was activated (last ~400). */
    fun focusDayStarts(): Set<Long> =
        prefs.getStringSet(KEY_FOCUS_DAYS, emptySet()).orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()

    fun recordFocusDay(nowMillis: Long = System.currentTimeMillis()) {
        val dayStart = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val next = (focusDayStarts() + dayStart)
            .sortedDescending()
            .take(400)
            .map { it.toString() }
            .toSet()
        prefs.edit().putStringSet(KEY_FOCUS_DAYS, next).apply()
    }

    var scheduledReminderTaskIds: Set<String>
        get() = prefs.getStringSet(KEY_REMINDER_TASKS, emptySet()).orEmpty()
        set(value) = prefs.edit().putStringSet(KEY_REMINDER_TASKS, value).apply()

    var scheduledNoteReminderIds: Set<String>
        get() = prefs.getStringSet(KEY_NOTE_REMINDERS, emptySet()).orEmpty()
        set(value) = prefs.edit().putStringSet(KEY_NOTE_REMINDERS, value).apply()

    /** Default local hour (0–23) for due-date reminders */
    var reminderHour: Int
        get() = prefs.getInt(KEY_REMINDER_HOUR, 9).coerceIn(0, 23)
        set(value) = prefs.edit().putInt(KEY_REMINDER_HOUR, value.coerceIn(0, 23)).apply()

    /** Default local minute (0–59) for due-date reminders */
    var reminderMinute: Int
        get() = prefs.getInt(KEY_REMINDER_MINUTE, 0).coerceIn(0, 59)
        set(value) = prefs.edit().putInt(KEY_REMINDER_MINUTE, value.coerceIn(0, 59)).apply()

    val reminderMinutesOfDay: Int
        get() = reminderHour * 60 + reminderMinute

    /** Default local hour (0–23) when recurring templates create today's tasks */
    var spawnHour: Int
        get() = prefs.getInt(KEY_SPAWN_HOUR, 3).coerceIn(0, 23)
        set(value) = prefs.edit().putInt(KEY_SPAWN_HOUR, value.coerceIn(0, 23)).apply()

    /** Default local minute (0–59) when recurring templates create today's tasks */
    var spawnMinute: Int
        get() = prefs.getInt(KEY_SPAWN_MINUTE, 30).coerceIn(0, 59)
        set(value) = prefs.edit().putInt(KEY_SPAWN_MINUTE, value.coerceIn(0, 59)).apply()

    val spawnMinutesOfDay: Int
        get() = spawnHour * 60 + spawnMinute

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

    /** plain | pen | ink | garden | berry | sunset | ocean — colors Q1–Q4 buttons and task fills */
    var eisenhowerPalette: String
        get() {
            val raw = prefs.getString(KEY_EISENHOWER_PALETTE, "plain") ?: "plain"
            return when (raw) {
                "plain", "pen", "ink", "garden", "berry", "sunset", "ocean" -> raw
                else -> {
                    prefs.edit().putString(KEY_EISENHOWER_PALETTE, "plain").apply()
                    "plain"
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

    fun hubGroupTitle(groupId: String): String =
        prefs.getString(hubGroupTitleKey(groupId), null).orEmpty()

    fun setHubGroupTitle(groupId: String, title: String) {
        val trimmed = title.trim()
        val ed = prefs.edit()
        if (trimmed.isEmpty()) ed.remove(hubGroupTitleKey(groupId))
        else ed.putString(hubGroupTitleKey(groupId), trimmed)
        ed.apply()
    }

    fun isHubGroupCollapsed(groupId: String): Boolean =
        prefs.getStringSet(KEY_HUB_GROUPS_COLLAPSED, emptySet()).orEmpty().contains(groupId)

    fun setHubGroupCollapsed(groupId: String, collapsed: Boolean) {
        val current = prefs.getStringSet(KEY_HUB_GROUPS_COLLAPSED, emptySet()).orEmpty()
        val next = if (collapsed) current + groupId else current - groupId
        prefs.edit().putStringSet(KEY_HUB_GROUPS_COLLAPSED, next).apply()
    }

    fun isRecurringGroupCollapsed(groupId: String): Boolean =
        prefs.getStringSet(KEY_RECURRING_GROUPS_COLLAPSED, emptySet()).orEmpty().contains(groupId)

    fun setRecurringGroupCollapsed(groupId: String, collapsed: Boolean) {
        val current = prefs.getStringSet(KEY_RECURRING_GROUPS_COLLAPSED, emptySet()).orEmpty()
        val next = if (collapsed) current + groupId else current - groupId
        prefs.edit().putStringSet(KEY_RECURRING_GROUPS_COLLAPSED, next).apply()
    }

    fun clearHubGroupMeta(groupId: String) {
        val collapsed = prefs.getStringSet(KEY_HUB_GROUPS_COLLAPSED, emptySet()).orEmpty()
        val recurringCollapsed = prefs.getStringSet(KEY_RECURRING_GROUPS_COLLAPSED, emptySet()).orEmpty()
        prefs.edit()
            .remove(hubGroupTitleKey(groupId))
            .putStringSet(KEY_HUB_GROUPS_COLLAPSED, collapsed - groupId)
            .putStringSet(KEY_RECURRING_GROUPS_COLLAPSED, recurringCollapsed - groupId)
            .apply()
    }

    fun clearHubGroupMetaMany(groupIds: Collection<String>) {
        if (groupIds.isEmpty()) return
        val gone = groupIds.toSet()
        val collapsed = prefs.getStringSet(KEY_HUB_GROUPS_COLLAPSED, emptySet()).orEmpty()
        val recurringCollapsed = prefs.getStringSet(KEY_RECURRING_GROUPS_COLLAPSED, emptySet()).orEmpty()
        val ed = prefs.edit()
        groupIds.forEach { ed.remove(hubGroupTitleKey(it)) }
        ed.putStringSet(KEY_HUB_GROUPS_COLLAPSED, collapsed - gone)
            .putStringSet(KEY_RECURRING_GROUPS_COLLAPSED, recurringCollapsed - gone)
            .apply()
    }

    private fun hubGroupTitleKey(groupId: String) = "$KEY_HUB_GROUP_TITLE_PREFIX$groupId"

    /** Primary (home) hub for a board — opened first when entering the board. */
    fun getPrimaryHubId(boardId: String): String? {
        return prefs.getString(primaryHubKey(boardId), null)?.takeIf { it.isNotBlank() }
    }

    fun setPrimaryHubId(boardId: String, columnId: String?) {
        prefs.edit().putString(primaryHubKey(boardId), columnId.orEmpty()).apply()
    }

    /** Local start-of-day when due-today tasks were last moved onto primary hubs. */
    var dueTodayPrimarySweepDay: Long
        get() = prefs.getLong(KEY_DUE_TODAY_PRIMARY_SWEEP_DAY, 0L)
        set(value) = prefs.edit().putLong(KEY_DUE_TODAY_PRIMARY_SWEEP_DAY, value).apply()

    /**
     * Hub that is the goals list for a board. Kept by id so a rename does not drop it.
     * Cleared when that hub is deleted.
     */
    fun getGoalsHubId(boardId: String): String? =
        prefs.getString(goalsHubKey(boardId), null)?.takeIf { it.isNotBlank() }

    fun setGoalsHubId(boardId: String, columnId: String?) {
        prefs.edit().putString(goalsHubKey(boardId), columnId.orEmpty()).apply()
    }

    /**
     * Bump when [InitializeDatabaseUseCase] seed/migration logic changes so warm
     * starts re-run the full path once, then skip expensive scans again.
     */
    var dbSeedVersion: Int
        get() = prefs.getInt(KEY_DB_SEED_VERSION, 0)
        set(value) = prefs.edit().putInt(KEY_DB_SEED_VERSION, value).apply()

    /**
     * PRO unlock stub (default on). Replace with Play Billing / license key later.
     */
    var proUnlocked: Boolean
        get() = prefs.getBoolean(KEY_PRO_UNLOCKED, true)
        set(value) = prefs.edit().putBoolean(KEY_PRO_UNLOCKED, value).apply()

    /** Widget mic is actively capturing (for red / pulse visuals). */
    var voiceListeningActive: Boolean
        get() = prefs.getBoolean(KEY_VOICE_LISTENING, false)
        set(value) {
            // commit(): widget update must read the new value immediately (apply() races).
            prefs.edit().putBoolean(KEY_VOICE_LISTENING, value).commit()
        }

    /**
     * Soft stats epoch (millis). 0 → start of the local day the app was first installed.
     * Completions before this are ignored in Review.
     * A stored 1 January (the previous default) is replaced once with the install day.
     */
    var statsEpochMillis: Long
        get() {
            val stored = prefs.getLong(KEY_STATS_EPOCH, 0L)
            if (!prefs.contains(KEY_STATS_EPOCH_MIGRATED)) {
                val editor = prefs.edit().putBoolean(KEY_STATS_EPOCH_MIGRATED, true)
                if (stored <= 0L || isLegacyJanuaryFirst(stored)) {
                    val installDay = defaultStatsEpochMillis()
                    editor.putLong(KEY_STATS_EPOCH, installDay).apply()
                    return installDay
                }
                editor.apply()
            }
            return if (stored > 0L) stored else defaultStatsEpochMillis()
        }
        set(value) = prefs.edit().putLong(KEY_STATS_EPOCH, value.coerceAtLeast(0L)).apply()

    private fun isLegacyJanuaryFirst(millis: Long): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return cal.get(Calendar.MONTH) == Calendar.JANUARY &&
            cal.get(Calendar.DAY_OF_MONTH) == 1 &&
            cal.get(Calendar.HOUR_OF_DAY) == 0 &&
            cal.get(Calendar.MINUTE) == 0 &&
            cal.get(Calendar.SECOND) == 0 &&
            cal.get(Calendar.MILLISECOND) == 0
    }

    /** Midnight of the first-install day. Updates do not move this. */
    fun defaultStatsEpochMillis(): Long = startOfLocalDay(firstInstallTimeMillis())

    /**
     * Instant of the first install. PackageInfo.firstInstallTime stays put across
     * updates; lastUpdateTime is not used. Saved on first read so a later update
     * cannot replace it.
     */
    fun firstInstallTimeMillis(): Long {
        val stored = prefs.getLong(KEY_FIRST_INSTALL, 0L)
        if (stored > 0L) return stored
        val installed = readPackageFirstInstallTime()
        prefs.edit().putLong(KEY_FIRST_INSTALL, installed).apply()
        return installed
    }

    private fun readPackageFirstInstallTime(): Long {
        return try {
            val info = if (Build.VERSION.SDK_INT >= 33) {
                appContext.packageManager.getPackageInfo(
                    appContext.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            }
            info.firstInstallTime.takeIf { it > 0L } ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun startOfLocalDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun resetStatsEpochToDefault() {
        prefs.edit().putLong(KEY_STATS_EPOCH, defaultStatsEpochMillis()).apply()
    }

    /** Move epoch forward so the given period no longer counts (soft reset). */
    fun advanceStatsEpochTo(millis: Long) {
        statsEpochMillis = millis.coerceAtLeast(statsEpochMillis)
    }

    fun pinnedStatsGoalIds(projectId: String): Set<String> =
        prefs.getStringSet(pinnedGoalsKey(projectId), emptySet()).orEmpty()

    fun setPinnedStatsGoalIds(projectId: String, ids: Set<String>) {
        prefs.edit().putStringSet(pinnedGoalsKey(projectId), ids).apply()
    }

    fun hiddenStatsGoalIds(projectId: String): Set<String> =
        prefs.getStringSet(hiddenGoalsKey(projectId), emptySet()).orEmpty()

    fun setHiddenStatsGoalIds(projectId: String, ids: Set<String>) {
        prefs.edit().putStringSet(hiddenGoalsKey(projectId), ids).apply()
    }

    fun kairosUndoLog(projectId: String): String =
        prefs.getString(kairosUndoKey(projectId), null).orEmpty()

    fun setKairosUndoLog(projectId: String, value: String) {
        prefs.edit().putString(kairosUndoKey(projectId), value).apply()
    }

    private fun kairosUndoKey(projectId: String) = "$KEY_KAIROS_UNDO_PREFIX$projectId"

    private fun pinnedGoalsKey(projectId: String) = "$KEY_PINNED_GOALS_PREFIX$projectId"
    private fun hiddenGoalsKey(projectId: String) = "$KEY_HIDDEN_GOALS_PREFIX$projectId"

    private fun primaryHubKey(boardId: String) = "$KEY_PRIMARY_HUB_PREFIX$boardId"
    private fun goalsHubKey(boardId: String) = "$KEY_GOALS_HUB_PREFIX$boardId"

    fun taskTimerEnds(): Map<String, Long> = readIdLongs(KEY_TASK_TIMERS)

    fun taskTimerEnd(taskId: String): Long? = taskTimerEnds()[taskId]

    fun putTaskTimerEnd(taskId: String, endAtMillis: Long) {
        if (taskId.isBlank()) return
        val next = taskTimerEnds() + (taskId to endAtMillis)
        writeTaskTimerEnds(next)
    }

    fun removeTaskTimerEnd(taskId: String) {
        writeTaskTimerEnds(taskTimerEnds() - taskId)
    }

    fun taskTimerPaused(): Map<String, Long> = readIdLongs(KEY_TASK_TIMER_PAUSED)

    fun taskTimerPaused(taskId: String): Long? = taskTimerPaused()[taskId]

    fun putTaskTimerPaused(taskId: String, remainMillis: Long) {
        if (taskId.isBlank()) return
        writeIdLongs(KEY_TASK_TIMER_PAUSED, taskTimerPaused() + (taskId to remainMillis))
    }

    fun removeTaskTimerPaused(taskId: String) {
        writeIdLongs(KEY_TASK_TIMER_PAUSED, taskTimerPaused() - taskId)
    }

    private fun writeTaskTimerEnds(value: Map<String, Long>) {
        writeIdLongs(KEY_TASK_TIMERS, value)
    }

    private fun readIdLongs(key: String): Map<String, Long> {
        val raw = prefs.getString(key, "").orEmpty()
        if (raw.isBlank()) return emptyMap()
        return raw.split('|').mapNotNull { part ->
            val sep = part.indexOf(':')
            if (sep <= 0) return@mapNotNull null
            val id = part.substring(0, sep)
            val value = part.substring(sep + 1).toLongOrNull() ?: return@mapNotNull null
            if (id.isBlank()) null else id to value
        }.toMap()
    }

    private fun writeIdLongs(key: String, value: Map<String, Long>) {
        val packed = value.entries.joinToString("|") { "${it.key}:${it.value}" }
        prefs.edit().putString(key, packed).apply()
    }

    private companion object {
        const val PREFS_NAME = "kairos_prefs"
        const val KEY_LANGUAGE = "app_language"
        const val KEY_ENTER_ADDS_TASK = "enter_adds_task"
        const val KEY_IN_PROGRESS_TASK = "in_progress_task_id"
        const val KEY_FOCUS_DAYS = "focus_day_starts"
        const val KEY_REMINDER_TASKS = "reminder_task_ids"
        const val KEY_NOTE_REMINDERS = "note_reminder_ids"
        const val KEY_TASK_TIMERS = "task_timer_ends"
        const val KEY_TASK_TIMER_PAUSED = "task_timer_paused"
        const val KEY_REMINDER_HOUR = "reminder_hour"
        const val KEY_REMINDER_MINUTE = "reminder_minute"
        const val KEY_SPAWN_HOUR = "recurring_spawn_hour"
        const val KEY_SPAWN_MINUTE = "recurring_spawn_minute"
        const val KEY_QUICK_ADD_BOARD = "quick_add_board_id"
        const val KEY_QUICK_ADD_COLUMN = "quick_add_column_id"
        const val KEY_MANUAL_ORDER_COLUMNS = "manual_order_columns"
        const val KEY_HUB_GROUP_TITLE_PREFIX = "hub_group_title_"
        const val KEY_HUB_GROUPS_COLLAPSED = "hub_groups_collapsed"
        const val KEY_RECURRING_GROUPS_COLLAPSED = "recurring_groups_collapsed"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_EISENHOWER_PALETTE = "eisenhower_palette"
        const val KEY_SHOW_ARCHIVED = "show_archived_boards"
        const val KEY_ONBOARDING = "has_seen_onboarding"
        const val KEY_OKR_PLANNING_GUIDE = "has_seen_okr_planning_guide"
        const val KEY_PROJECTS_COLLAPSED = "projects_collapsed"
        const val KEY_OKR_BOARD_ID = "okr_board_id"
        const val KEY_EISENHOWER_BOARD_ID = "eisenhower_board_id"
        const val KEY_PRIMARY_HUB_PREFIX = "primary_hub_"
        const val KEY_DUE_TODAY_PRIMARY_SWEEP_DAY = "due_today_primary_sweep_day"
        const val KEY_GOALS_HUB_PREFIX = "goals_hub_"
        const val KEY_DB_SEED_VERSION = "db_seed_version"
        const val KEY_PRO_UNLOCKED = "pro_unlocked"
        const val KEY_VOICE_LISTENING = "voice_listening_active"
        const val KEY_STATS_EPOCH = "stats_epoch_millis"
        const val KEY_STATS_EPOCH_MIGRATED = "stats_epoch_install_migrated"
        const val KEY_FIRST_INSTALL = "first_install_time_millis"
        const val KEY_KAIROS_UNDO_PREFIX = "kairos_undo_"
        const val KEY_PINNED_GOALS_PREFIX = "stats_pinned_goals_"
        const val KEY_HIDDEN_GOALS_PREFIX = "stats_hidden_goals_"
    }
}
