package com.example.kaizenkanban.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.kaizenkanban.data.local.AppDatabase
import com.example.kaizenkanban.data.repository.KanbanRepositoryImpl
import com.example.kaizenkanban.domain.usecase.EnsureRecurringInstancesUseCase
import com.example.kaizenkanban.ui.calendar.isOverdueDate
import com.example.kaizenkanban.widget.KairosWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DueReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_DUE_REMINDER -> {
                val taskId = intent.getStringExtra(DueReminderScheduler.EXTRA_TASK_ID).orEmpty()
                val fallbackTitle = intent.getStringExtra(DueReminderScheduler.EXTRA_TASK_TITLE).orEmpty()
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = KanbanRepositoryImpl(AppDatabase.getDatabase(context).kanbanDao())
                        val task = repo.getAllTasks().first().find { it.id == taskId }
                        if (task == null || task.isCompleted || task.dueDate == null) return@launch
                        val overdue = task.dueDate.isOverdueDate()
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
        if (templates.none { it.enabled }) return
        val tasks = repo.getAllTasks().first()
        val columns = repo.getAllColumns().first()
        val created = EnsureRecurringInstancesUseCase(repo)(templates, tasks, columns)
        if (created > 0) {
            val refreshed = repo.getAllTasks().first()
            DueReminderScheduler.sync(context, refreshed)
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
