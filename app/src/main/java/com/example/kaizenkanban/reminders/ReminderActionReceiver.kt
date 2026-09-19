package com.example.kaizenkanban.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Explicit-intent actions from our notifications only.
 * Keep [android:exported]=false so other apps cannot fire MARK_DONE / SNOOZE / UNDO.
 */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            DueReminderReceiver.ACTION_MARK_DONE -> {
                val taskId = intent.getStringExtra(DueReminderScheduler.EXTRA_TASK_ID).orEmpty()
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        TaskQuickActions.markDone(context, taskId)
                    } finally {
                        pending.finish()
                    }
                }
            }
            DueReminderReceiver.ACTION_SNOOZE_TOMORROW -> {
                val taskId = intent.getStringExtra(DueReminderScheduler.EXTRA_TASK_ID).orEmpty()
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        TaskQuickActions.snoozeToTomorrow(context, taskId)
                    } finally {
                        pending.finish()
                    }
                }
            }
            DueReminderReceiver.ACTION_UNDO_DONE -> {
                val taskId = intent.getStringExtra(DueReminderScheduler.EXTRA_TASK_ID).orEmpty()
                val spawnedId = intent.getStringExtra(TaskQuickActions.EXTRA_SPAWNED_TASK_ID)
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        TaskQuickActions.undoDone(context, taskId, spawnedId)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
