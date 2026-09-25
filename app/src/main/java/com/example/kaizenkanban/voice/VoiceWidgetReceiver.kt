package com.example.kaizenkanban.voice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.kaizenkanban.MainActivity
import com.example.kaizenkanban.reminders.DueReminderScheduler

/**
 * Legacy widget action: opens the app and starts dictate there
 * (in-widget capture left the mic button stuck red on some launchers).
 */
class VoiceWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_START) return
        context.applicationContext.startActivity(
            Intent(context.applicationContext, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                putExtra(DueReminderScheduler.EXTRA_OPEN_VOICE, true)
            }
        )
    }

    companion object {
        const val ACTION_START = "com.example.kaizenkanban.action.START_VOICE_CAPTURE"
    }
}
