package com.example.kaizenkanban.reminders

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.kaizenkanban.R
import com.example.kaizenkanban.data.local.AppDatabase
import com.example.kaizenkanban.data.local.KairosPreferences
import com.example.kaizenkanban.data.repository.KanbanRepositoryImpl
import com.example.kaizenkanban.domain.TaskCompletionGate
import com.example.kaizenkanban.domain.model.TaskWorkflow
import com.example.kaizenkanban.ui.calendar.localNoonForDayOffset
import com.example.kaizenkanban.widget.KairosWidgetUpdater
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock

object TaskQuickActions {
    const val EXTRA_SPAWNED_TASK_ID = "spawned_task_id"

    private fun repository(context: Context): KanbanRepositoryImpl {
        val db = AppDatabase.getDatabase(context.applicationContext)
        return KanbanRepositoryImpl(db.kanbanDao(), db)
    }

    suspend fun markDone(context: Context, taskId: String): Boolean = TaskCompletionGate.mutex.withLock {
        if (taskId.isBlank()) return false
        val appContext = context.applicationContext
        val repo = repository(appContext)
        val task = repo.getAllTasks().first().find { it.id == taskId } ?: return false
        if (task.isCompleted) {
            dismissNotification(appContext, taskId)
            return true
        }
        val completedAt = System.currentTimeMillis()
        repo.updateTasks(
            listOf(
                task.copy(
                    isCompleted = true,
                    completedAt = completedAt,
                    workflowStatus = TaskWorkflow.DONE
                )
            )
        )
        // Legacy repeatRule auto-spawn disabled (recurring templates replace it).
        val prefs = KairosPreferences(appContext)
        if (prefs.inProgressTaskId == taskId) {
            prefs.inProgressTaskId = null
        }
        val tasks = repo.getAllTasks().first()
        DueReminderScheduler.sync(appContext, tasks)
        dismissNotification(appContext, taskId)
        KairosWidgetUpdater.updateAll(appContext, tasks)
        showUndoDoneNotification(appContext, taskId, task.title, spawnedTaskId = null)
        return true
    }

    suspend fun undoDone(context: Context, taskId: String, spawnedTaskId: String? = null): Boolean =
        TaskCompletionGate.mutex.withLock {
            if (taskId.isBlank()) return false
            val appContext = context.applicationContext
            val repo = repository(appContext)
            val task = repo.getAllTasks().first().find { it.id == taskId } ?: return false
            repo.updateTasks(
                listOf(
                    task.copy(
                        isCompleted = false,
                        completedAt = null,
                        workflowStatus = TaskWorkflow.OPEN,
                        completionQuality = null
                    )
                )
            )
            spawnedTaskId?.takeIf { it.isNotBlank() }?.let { repo.deleteTask(it) }
            val tasks = repo.getAllTasks().first()
            DueReminderScheduler.sync(appContext, tasks)
            NotificationManagerCompat.from(appContext).cancel(UNDO_NOTIFY_BASE + taskId.hashCode())
            KairosWidgetUpdater.updateAll(appContext, tasks)
            return true
        }

    suspend fun snoozeToTomorrow(context: Context, taskId: String): Boolean = TaskCompletionGate.mutex.withLock {
        if (taskId.isBlank()) return false
        val appContext = context.applicationContext
        val repo = repository(appContext)
        val task = repo.getAllTasks().first().find { it.id == taskId } ?: return false
        if (task.isCompleted) {
            dismissNotification(appContext, taskId)
            return true
        }
        repo.updateTasks(listOf(task.copy(dueDate = localNoonForDayOffset(1))))
        val tasks = repo.getAllTasks().first()
        DueReminderScheduler.sync(appContext, tasks)
        dismissNotification(appContext, taskId)
        KairosWidgetUpdater.updateAll(appContext, tasks)
        return true
    }

    suspend fun loadIncompleteDueTasks(context: Context) =
        repository(context).getAllTasks().first()

    private fun showUndoDoneNotification(
        context: Context,
        taskId: String,
        title: String,
        spawnedTaskId: String?
    ) {
        DueReminderScheduler.ensureChannel(context)
        val strings = DueReminderScheduler.appStrings(context)
        val undoPending = PendingIntent.getBroadcast(
            context,
            taskId.hashCode() xor 0x33_00_00,
            Intent(context, ReminderActionReceiver::class.java).apply {
                action = DueReminderReceiver.ACTION_UNDO_DONE
                putExtra(DueReminderScheduler.EXTRA_TASK_ID, taskId)
                if (!spawnedTaskId.isNullOrBlank()) {
                    putExtra(EXTRA_SPAWNED_TASK_ID, spawnedTaskId)
                }
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, DueReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(strings.taskMarkedDone)
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(0, strings.undo, undoPending)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(UNDO_NOTIFY_BASE + taskId.hashCode(), notification)
        } catch (_: SecurityException) {
        }
    }

    private fun dismissNotification(context: Context, taskId: String) {
        NotificationManagerCompat.from(context).cancel(taskId.hashCode())
    }

    private const val UNDO_NOTIFY_BASE = 0x51_00_00_00
}
