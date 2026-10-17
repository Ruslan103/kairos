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
import com.example.kaizenkanban.domain.model.KairosReminder

object NoteReminderScheduler {
    const val CHANNEL_ID = "kairos_note_reminders"
    const val EXTRA_REMINDER_ID = "note_reminder_id"
    const val EXTRA_REMINDER_TEXT = "note_reminder_text"
    const val ACTION_NOTE_REMINDER = "com.example.kaizenkanban.action.NOTE_REMINDER"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val strings = DueReminderScheduler.appStrings(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            strings.noteReminderChannelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = strings.noteReminderChannelDesc
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun sync(context: Context, reminders: List<KairosReminder>) {
        ensureChannel(context)
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        val now = System.currentTimeMillis()
        val upcoming = reminders.filter { it.triggerAtMillis > now && it.text.isNotBlank() }
        val activeIds = upcoming.map { it.id }.toSet()
        val prefs = KairosPreferences(appContext)
        prefs.scheduledNoteReminderIds
            .filter { it !in activeIds }
            .forEach { cancel(appContext, alarmManager, it) }
        upcoming.forEach { schedule(appContext, alarmManager, it) }
        prefs.scheduledNoteReminderIds = activeIds
    }

    private fun schedule(context: Context, alarmManager: AlarmManager, reminder: KairosReminder) {
        val intent = Intent(context, DueReminderReceiver::class.java).apply {
            action = ACTION_NOTE_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_REMINDER_TEXT, reminder.text)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode(reminder.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.triggerAtMillis,
                    pending
                )
            } else {
                @Suppress("DEPRECATION")
                alarmManager.set(AlarmManager.RTC_WAKEUP, reminder.triggerAtMillis, pending)
            }
        } catch (_: Exception) {
            // Some devices restrict alarms
        }
    }

    private fun cancel(context: Context, alarmManager: AlarmManager, reminderId: String) {
        val intent = Intent(context, DueReminderReceiver::class.java).apply {
            action = ACTION_NOTE_REMINDER
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode(reminderId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }

    fun show(context: Context, reminderId: String, text: String) {
        ensureChannel(context)
        val strings = DueReminderScheduler.appStrings(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPending = PendingIntent.getActivity(
            context,
            requestCode(reminderId),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(strings.noteReminderTitle)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(contentPending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(requestCode(reminderId), notification)
        } catch (_: SecurityException) {
            // Notification permission not granted
        }
    }

    fun dismiss(context: Context, reminderId: String) {
        if (reminderId.isBlank()) return
        NotificationManagerCompat.from(context.applicationContext).cancel(requestCode(reminderId))
    }

    private fun requestCode(reminderId: String): Int = ("note:$reminderId").hashCode()
}
