package com.example.kaizenkanban.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.kaizenkanban.data.local.KairosPreferences
import java.util.Calendar

/**
 * Schedules daily ensure alarms at the recurring create (spawn) time from settings
 * and at the default reminder time when it differs (covers early-create and late slots).
 */
object RecurringEnsureScheduler {
    const val REQUEST_CODE = 71_001
    private const val REQUEST_CODE_REMINDER = 71_002

    fun scheduleNext(context: Context, now: Long = System.currentTimeMillis()) {
        val appContext = context.applicationContext
        val prefs = KairosPreferences(appContext)
        val spawn = prefs.spawnMinutesOfDay.coerceIn(0, 24 * 60 - 1)
        val reminder = prefs.reminderMinutesOfDay.coerceIn(0, 24 * 60 - 1)
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return

        scheduleAt(alarmManager, appContext, REQUEST_CODE, nextAtMinutes(now, spawn))
        if (reminder != spawn) {
            scheduleAt(alarmManager, appContext, REQUEST_CODE_REMINDER, nextAtMinutes(now, reminder))
        } else {
            cancel(alarmManager, appContext, REQUEST_CODE_REMINDER)
        }
    }

    private fun nextAtMinutes(now: Long, minutesOfDay: Int): Long {
        val hour = minutesOfDay / 60
        val minute = minutesOfDay % 60
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }

    private fun scheduleAt(
        alarmManager: AlarmManager,
        context: Context,
        requestCode: Int,
        triggerAt: Long
    ) {
        val pending = pendingIntent(context, requestCode)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                @Suppress("DEPRECATION")
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (_: Exception) {
            // Device may restrict alarms; in-app ensure remains as fallback.
        }
    }

    private fun cancel(alarmManager: AlarmManager, context: Context, requestCode: Int) {
        try {
            alarmManager.cancel(pendingIntent(context, requestCode))
        } catch (_: Exception) {
        }
    }

    private fun pendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, DueReminderReceiver::class.java).apply {
            action = DueReminderReceiver.ACTION_ENSURE_RECURRING
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
