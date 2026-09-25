package com.example.kaizenkanban.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.kaizenkanban.MainActivity
import com.example.kaizenkanban.R
import com.example.kaizenkanban.data.local.KairosPreferences
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.ui.i18n.AppLanguage
import com.example.kaizenkanban.ui.i18n.AppStrings
import java.util.Calendar
import java.util.Locale

object DueReminderScheduler {
    const val CHANNEL_ID = "kairos_due_reminders_v2"
    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_TASK_TITLE = "task_title"
    const val EXTRA_OPEN_QUICK_ADD = "open_quick_add"
    const val EXTRA_OPEN_VOICE = "open_voice_assistant"
    const val EXTRA_OPEN_ADD_TASK = "open_add_task"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val strings = appStrings(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            strings.reminderChannelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = strings.reminderChannelDesc
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 180, 100, 180)
            setSound(
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
        manager.createNotificationChannel(channel)
    }

    fun sync(context: Context, tasks: List<Task>) {
        ensureChannel(context)
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        val incompleteWithDue = tasks.filter { !it.isCompleted && it.dueDate != null }
        val activeIds = incompleteWithDue.map { it.id }.toSet()

        val prefs = KairosPreferences(appContext)
        prefs.scheduledReminderTaskIds
            .filter { it !in activeIds }
            .forEach { cancel(appContext, alarmManager, it) }

        incompleteWithDue.forEach { task ->
            schedule(appContext, alarmManager, prefs, task)
        }
        prefs.scheduledReminderTaskIds = activeIds
    }

    fun schedule(context: Context, task: Task) {
        val due = task.dueDate ?: return
        if (task.isCompleted) {
            cancel(context, task.id)
            return
        }
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val prefs = KairosPreferences(context)
        schedule(context.applicationContext, alarmManager, prefs, task.copy(dueDate = due))
        prefs.scheduledReminderTaskIds = prefs.scheduledReminderTaskIds + task.id
    }

    fun cancel(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        cancel(context.applicationContext, alarmManager, taskId)
        val prefs = KairosPreferences(context)
        prefs.scheduledReminderTaskIds = prefs.scheduledReminderTaskIds - taskId
    }

    private fun schedule(
        context: Context,
        alarmManager: AlarmManager,
        prefs: KairosPreferences,
        task: Task
    ) {
        val due = task.dueDate ?: return
        val (hour, minute) = resolveReminderClock(prefs, task)
        val triggerAt = reminderTriggerMillis(due, hour, minute) ?: run {
            cancel(context, alarmManager, task.id)
            return
        }

        val intent = Intent(context, DueReminderReceiver::class.java).apply {
            action = DueReminderReceiver.ACTION_DUE_REMINDER
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TASK_TITLE, task.title)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode(task.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                @Suppress("DEPRECATION")
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (_: Exception) {
            // Some devices restrict alarms; skip silently
        }
    }

    private fun cancel(context: Context, alarmManager: AlarmManager, taskId: String) {
        val intent = Intent(context, DueReminderReceiver::class.java).apply {
            action = DueReminderReceiver.ACTION_DUE_REMINDER
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }

    fun resolveReminderClock(prefs: KairosPreferences, task: Task): Pair<Int, Int> {
        val custom = task.reminderMinutesOfDay
        return if (custom != null) {
            val clamped = custom.coerceIn(0, 23 * 60 + 59)
            (clamped / 60) to (clamped % 60)
        } else {
            prefs.reminderHour to prefs.reminderMinute
        }
    }

    /**
     * Reminder at [hourOfDay]:[minute] on the due day when still ahead.
     * Once that moment has passed (including overdue prior days), returns null —
     * no daily catch-up spam for unfinished overdue tasks.
     */
    fun reminderTriggerMillis(
        dueDate: Long,
        hourOfDay: Int,
        minute: Int,
        now: Long = System.currentTimeMillis()
    ): Long? {
        val dueAt = Calendar.getInstance().apply {
            timeInMillis = dueDate
            set(Calendar.HOUR_OF_DAY, hourOfDay.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return if (dueAt > now) dueAt else null
    }

    fun formatClock(hour: Int, minute: Int): String =
        String.format(Locale.getDefault(), "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))

    fun formatMinutesOfDay(minutes: Int): String {
        val clamped = minutes.coerceIn(0, 23 * 60 + 59)
        return formatClock(clamped / 60, clamped % 60)
    }

    private fun requestCode(taskId: String): Int = taskId.hashCode()

    fun appStrings(context: Context): AppStrings =
        AppStrings(AppLanguage.fromCode(KairosPreferences(context).appLanguageCode))
}

object DueReminderNotifier {
    fun show(context: Context, taskId: String, title: String, overdue: Boolean = false) {
        DueReminderScheduler.ensureChannel(context)
        val strings = DueReminderScheduler.appStrings(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(DueReminderScheduler.EXTRA_TASK_ID, taskId)
        }
        val contentPending = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val donePending = PendingIntent.getBroadcast(
            context,
            taskId.hashCode() xor 0x11_00_00,
            Intent(context, ReminderActionReceiver::class.java).apply {
                action = DueReminderReceiver.ACTION_MARK_DONE
                putExtra(DueReminderScheduler.EXTRA_TASK_ID, taskId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozePending = PendingIntent.getBroadcast(
            context,
            taskId.hashCode() xor 0x22_00_00,
            Intent(context, ReminderActionReceiver::class.java).apply {
                action = DueReminderReceiver.ACTION_SNOOZE_TOMORROW
                putExtra(DueReminderScheduler.EXTRA_TASK_ID, taskId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, DueReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(if (overdue) strings.reminderOverdueTitle else strings.reminderTitle)
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(title))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setContentIntent(contentPending)
            .addAction(0, strings.markDone, donePending)
            .addAction(0, strings.dueTomorrow, snoozePending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(taskId.hashCode(), notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted
        }
    }
}
