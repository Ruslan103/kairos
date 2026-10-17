package com.example.kaizenkanban.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.kaizenkanban.MainActivity
import com.example.kaizenkanban.R
import com.example.kaizenkanban.data.local.KairosPreferences
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.domain.reminders.TaskTimerPlanner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

object TaskTimerScheduler {
    const val ACTION_TASK_TIMER = "com.example.kaizenkanban.action.TASK_TIMER"
    private const val CHANNEL_ID = "kairos_task_timer"
    private const val DONE_SOUND_MS = 2200

    var revision by mutableIntStateOf(0)
        private set

    private fun bump() {
        revision++
    }

    private val soundHandler = Handler(Looper.getMainLooper())
    private var doneTone: ToneGenerator? = null
    private var doneRingtone: android.media.Ringtone? = null

    fun start(context: Context, task: Task, nowMillis: Long = System.currentTimeMillis()): Long? {
        if (!task.showsTimerAction) return null
        val appContext = context.applicationContext
        val prefs = KairosPreferences(appContext)
        val paused = prefs.taskTimerPaused(task.id)
        if (paused != null && paused > 0L) {
            prefs.removeTaskTimerPaused(task.id)
            val endAt = nowMillis + paused
            prefs.putTaskTimerEnd(task.id, endAt)
            scheduleAlarm(appContext, task.id, task.title, endAt)
            bump()
            return endAt
        }
        val existing = prefs.taskTimerEnd(task.id)
        if (!TaskTimerPlanner.canStart(existing, nowMillis)) return existing
        val endAt = TaskTimerPlanner.endAtMillis(nowMillis, task.timerMinutes) ?: return null
        prefs.putTaskTimerEnd(task.id, endAt)
        scheduleAlarm(appContext, task.id, task.title, endAt)
        bump()
        return endAt
    }

    fun pause(context: Context, taskId: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (taskId.isBlank()) return false
        val appContext = context.applicationContext
        val prefs = KairosPreferences(appContext)
        val end = prefs.taskTimerEnd(taskId) ?: return false
        val left = end - nowMillis
        if (left <= 0L) return false
        prefs.removeTaskTimerEnd(taskId)
        cancelAlarm(appContext, taskId)
        prefs.putTaskTimerPaused(taskId, left)
        bump()
        return true
    }

    fun isRunning(context: Context, taskId: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val end = KairosPreferences(context).taskTimerEnd(taskId) ?: return false
        return end > nowMillis
    }

    fun isPaused(context: Context, taskId: String): Boolean =
        (KairosPreferences(context).taskTimerPaused(taskId) ?: 0L) > 0L

    fun remainingMillis(context: Context, taskId: String, nowMillis: Long = System.currentTimeMillis()): Long {
        val prefs = KairosPreferences(context)
        val end = prefs.taskTimerEnd(taskId)
        if (end != null) return TaskTimerPlanner.remainingMillis(end, nowMillis)
        return (prefs.taskTimerPaused(taskId) ?: 0L).coerceAtLeast(0L)
    }

    fun dismiss(context: Context, taskId: String) {
        if (taskId.isBlank()) return
        NotificationManagerCompat.from(context.applicationContext).cancel(notifyId(taskId))
    }

    fun cancel(context: Context, taskId: String) {
        val appContext = context.applicationContext
        val prefs = KairosPreferences(appContext)
        val had = prefs.taskTimerEnd(taskId) != null || prefs.taskTimerPaused(taskId) != null
        prefs.removeTaskTimerEnd(taskId)
        prefs.removeTaskTimerPaused(taskId)
        cancelAlarm(appContext, taskId)
        dismiss(appContext, taskId)
        stopDoneSound()
        if (had) bump()
    }

    fun clearFired(context: Context, taskId: String) {
        val prefs = KairosPreferences(context.applicationContext)
        if (prefs.taskTimerEnd(taskId) == null && prefs.taskTimerPaused(taskId) == null) return
        prefs.removeTaskTimerEnd(taskId)
        prefs.removeTaskTimerPaused(taskId)
        bump()
    }

    fun announceFinished(context: Context, taskId: String, title: String) {
        val appContext = context.applicationContext
        val prefs = KairosPreferences(appContext)
        if (prefs.taskTimerEnd(taskId) == null) return
        prefs.removeTaskTimerEnd(taskId)
        prefs.removeTaskTimerPaused(taskId)
        cancelAlarm(appContext, taskId)
        playDoneSound(appContext)
        showFinished(appContext, taskId, title)
        bump()
    }

    fun sync(context: Context, tasks: List<Task>, nowMillis: Long = System.currentTimeMillis()) {
        val appContext = context.applicationContext
        val prefs = KairosPreferences(appContext)
        val allowed = tasks.filter { it.showsTimerAction }.map { it.id }.toSet()
        val stored = prefs.taskTimerEnds().keys + prefs.taskTimerPaused().keys
        stored.forEach { taskId ->
            if (taskId !in allowed) {
                cancel(appContext, taskId)
            }
        }
        tasks.filter { it.isCompleted || it.isNotDone }.forEach { dismiss(appContext, it.id) }
        val live = prefs.taskTimerEnds()
        tasks.filter { it.id in live.keys }.forEach { task ->
            val endAt = live[task.id] ?: return@forEach
            if (endAt > nowMillis) {
                scheduleAlarm(appContext, task.id, task.title, endAt)
            }
        }
    }

    fun showFinished(context: Context, taskId: String, title: String) {
        ensureChannel(context)
        val strings = DueReminderScheduler.appStrings(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(DueReminderScheduler.EXTRA_TASK_ID, taskId)
        }
        val contentPending = PendingIntent.getActivity(
            context,
            notifyId(taskId),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(strings.timerDoneTitle)
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(title))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .setContentIntent(contentPending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notifyId(taskId), notification)
        } catch (_: SecurityException) {
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val strings = DueReminderScheduler.appStrings(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            strings.timerChannelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = strings.timerChannelDesc
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 220, 120, 220, 120, 320)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    private fun playDoneSound(context: Context) {
        stopDoneSound()
        val appContext = context.applicationContext
        soundHandler.post {
            if (startTone(appContext)) return@post
            startRingtone(appContext)
        }
        soundHandler.postDelayed({ stopDoneSound() }, DONE_SOUND_MS.toLong())
    }

    private fun startTone(context: Context): Boolean {
        return try {
            val audio = context.getSystemService(AudioManager::class.java)
            val stream = if ((audio?.getStreamVolume(AudioManager.STREAM_ALARM) ?: 0) > 0) {
                AudioManager.STREAM_ALARM
            } else {
                AudioManager.STREAM_NOTIFICATION
            }
            val tone = ToneGenerator(stream, 100)
            doneTone = tone
            tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, DONE_SOUND_MS)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun startRingtone(context: Context) {
        try {
            val uri = Settings.System.DEFAULT_ALARM_ALERT_URI
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return
            val ringtone = RingtoneManager.getRingtone(context, uri) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.isLooping = false
            }
            doneRingtone = ringtone
            ringtone.play()
        } catch (_: Exception) {
        }
    }

    private fun stopDoneSound() {
        soundHandler.removeCallbacksAndMessages(null)
        try {
            doneTone?.stopTone()
            doneTone?.release()
        } catch (_: Exception) {
        }
        doneTone = null
        try {
            doneRingtone?.stop()
        } catch (_: Exception) {
        }
        doneRingtone = null
    }

    private fun scheduleAlarm(context: Context, taskId: String, title: String, endAt: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = pendingIntent(context, taskId, title)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, pending)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, endAt, pending)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, endAt, pending)
        }
    }

    private fun cancelAlarm(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(pendingIntent(context, taskId, title = ""))
    }

    private fun pendingIntent(context: Context, taskId: String, title: String): PendingIntent {
        val intent = Intent(context, DueReminderReceiver::class.java).apply {
            action = ACTION_TASK_TIMER
            putExtra(DueReminderScheduler.EXTRA_TASK_ID, taskId)
            putExtra(DueReminderScheduler.EXTRA_TASK_TITLE, title)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCode(taskId: String): Int = taskId.hashCode() xor 0x7A11_0000

    private fun notifyId(taskId: String): Int = taskId.hashCode() xor 0x71_00_00
}
