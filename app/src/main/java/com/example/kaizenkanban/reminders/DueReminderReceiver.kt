package com.example.kaizenkanban.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.kaizenkanban.data.local.AppDatabase
import com.example.kaizenkanban.data.local.KairosPreferences
import com.example.kaizenkanban.data.repository.KanbanRepositoryImpl
import com.example.kaizenkanban.domain.reminders.RecurringReminderPlanner
import com.example.kaizenkanban.domain.usecase.EnsureRecurringInstancesUseCase
import com.example.kaizenkanban.ui.calendar.isPastDeadline
import com.example.kaizenkanban.widget.KairosWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DueReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            NoteReminderScheduler.ACTION_NOTE_REMINDER -> {
                val reminderId = intent.getStringExtra(NoteReminderScheduler.EXTRA_REMINDER_ID).orEmpty()
                val text = intent.getStringExtra(NoteReminderScheduler.EXTRA_REMINDER_TEXT).orEmpty()
                if (reminderId.isBlank() || text.isBlank()) return
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = KanbanRepositoryImpl(AppDatabase.getDatabase(context).kanbanDao())
                        val reminder = repo.getAllReminders().first().find { it.id == reminderId }
                        val tasks = repo.getAllTasks().first()
                        val linkedTaskId = reminder?.taskId
                        val linkedClosed = !linkedTaskId.isNullOrBlank() && run {
                            val task = tasks.find { it.id == linkedTaskId }
                            task == null || task.isCompleted || task.isNotDone
                        }
                        val occurrenceClosed = reminder?.recurringTemplateId?.takeIf { it.isNotBlank() } != null &&
                            tasks.any { task ->
                                (task.isCompleted || task.isNotDone) &&
                                    RecurringReminderPlanner.idForTask(task) == reminderId
                            }
                        if (linkedClosed || occurrenceClosed) {
                            NoteReminderScheduler.dismiss(context, reminderId)
                            return@launch
                        }
                        NoteReminderScheduler.show(context, reminderId, text)
                    } finally {
                        pending.finish()
                    }
                }
            }
            TaskTimerScheduler.ACTION_TASK_TIMER -> {
                val taskId = intent.getStringExtra(DueReminderScheduler.EXTRA_TASK_ID).orEmpty()
                val fallbackTitle = intent.getStringExtra(DueReminderScheduler.EXTRA_TASK_TITLE).orEmpty()
                if (taskId.isBlank()) return
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = KanbanRepositoryImpl(AppDatabase.getDatabase(context).kanbanDao())
                        val task = repo.getAllTasks().first().find { it.id == taskId }
                        if (task == null || task.isCompleted || task.isHidden || !task.showsTimerAction) {
                            TaskTimerScheduler.cancel(context, taskId)
                            return@launch
                        }
                        TaskTimerScheduler.announceFinished(
                            context = context,
                            taskId = task.id,
                            title = task.title.ifBlank { fallbackTitle }
                        )
                    } finally {
                        pending.finish()
                    }
                }
            }
            ACTION_DUE_REMINDER -> {
                val taskId = intent.getStringExtra(DueReminderScheduler.EXTRA_TASK_ID).orEmpty()
                val fallbackTitle = intent.getStringExtra(DueReminderScheduler.EXTRA_TASK_TITLE).orEmpty()
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = KanbanRepositoryImpl(AppDatabase.getDatabase(context).kanbanDao())
                        val task = repo.getAllTasks().first().find { it.id == taskId }
                        if (task == null || task.isCompleted || task.isNotDone) {
                            DueReminderScheduler.dismiss(context, taskId)
                            return@launch
                        }
                        if (task.isHidden || task.dueDate == null) return@launch
                        val overdue = task.isPastDeadline()
                        DueReminderNotifier.show(
                            context = context,
                            taskId = task.id,
                            title = task.title.ifBlank { fallbackTitle },
                            overdue = overdue
                        )
                        // Do not re-sync here: overdue catch-up used to reschedule daily spam.
                        // Sync still runs on boot, task edits, and mark-done.
                        KairosWidgetUpdater.updateAll(context, repo.getAllTasks().first())
                    } finally {
                        pending.finish()
                    }
                }
            }
            ACTION_ENSURE_RECURRING -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        spawnTodayRecurring(context)
                    } finally {
                        RecurringEnsureScheduler.scheduleNext(context)
                        pending.finish()
                    }
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = KanbanRepositoryImpl(AppDatabase.getDatabase(context).kanbanDao())
                        val tasks = repo.getAllTasks().first()
                        DueReminderScheduler.sync(context, tasks)
                        TaskTimerScheduler.sync(context, tasks)
                        NoteReminderScheduler.sync(context, repo.getAllReminders().first())
                        spawnTodayRecurring(context)
                        RecurringEnsureScheduler.scheduleNext(context)
                        KairosWidgetUpdater.updateAll(context, repo.getAllTasks().first())
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    private suspend fun spawnTodayRecurring(context: Context) {
        val repo = KanbanRepositoryImpl(AppDatabase.getDatabase(context).kanbanDao())
        val templates = repo.getAllRecurringTemplates().first()
        val prefs = KairosPreferences(context)
        val now = System.currentTimeMillis()
        val boards = repo.getAllBoards().first()
        val columns = repo.getAllColumns().first()
        val primaryHubs = EnsureRecurringInstancesUseCase.resolvePrimaryHubs(
            boards = boards,
            columns = columns,
            storedHubId = { boardId -> prefs.getPrimaryHubId(boardId) }
        )
        val willSweep = EnsureRecurringInstancesUseCase.shouldSweepDueToday(
            now = now,
            spawnMinutes = prefs.spawnMinutesOfDay,
            sweptDayStart = prefs.dueTodayPrimarySweepDay,
            onlySlotsReadyByNow = true,
            hasPrimaryHub = primaryHubs.isNotEmpty()
        )
        if (templates.none { it.enabled } && !willSweep) return
        var swept = false
        val created = EnsureRecurringInstancesUseCase(repo)(
            templates = templates,
            columns = columns,
            now = now,
            defaultReminderMinutes = prefs.reminderMinutesOfDay,
            defaultSpawnMinutes = prefs.spawnMinutesOfDay,
            onlySlotsReadyByNow = true,
            primaryHubByBoardId = primaryHubs,
            dueTodaySweptDay = prefs.dueTodayPrimarySweepDay,
            onDueTodaySwept = { day ->
                prefs.dueTodayPrimarySweepDay = day
                swept = true
            }
        )
        if (created > 0 || swept) {
            val refreshed = repo.getAllTasks().first()
            if (created > 0) DueReminderScheduler.sync(context, refreshed)
            KairosWidgetUpdater.updateAll(context, refreshed)
        }
    }

    companion object {
        const val ACTION_DUE_REMINDER = "com.example.kaizenkanban.action.DUE_REMINDER"
        const val ACTION_ENSURE_RECURRING = "com.example.kaizenkanban.action.ENSURE_RECURRING"
        const val ACTION_MARK_DONE = "com.example.kaizenkanban.action.MARK_DONE"
        const val ACTION_SNOOZE_TOMORROW = "com.example.kaizenkanban.action.SNOOZE_TOMORROW"
        const val ACTION_UNDO_DONE = "com.example.kaizenkanban.action.UNDO_DONE"
    }
}
