package com.example.kaizenkanban.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * Fires once per day at 00:01 so recurring templates spawn hub instances
 * without waiting for the user to open the app.
 */
object RecurringEnsureScheduler {
    const val REQUEST_CODE = 71_001
    private const val HOUR = 0
    private const val MINUTE = 1

    fun scheduleNext(context: Context, now: Long = System.currentTimeMillis()) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        val triggerAt = nextTriggerMillis(now)
        val pending = pendingIntent(appContext)
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

    fun nextTriggerMillis(now: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, HOUR)
            set(Calendar.MINUTE, MINUTE)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DueReminderReceiver::class.java).apply {
            action = DueReminderReceiver.ACTION_ENSURE_RECURRING
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
