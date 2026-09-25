package com.example.kaizenkanban.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.example.kaizenkanban.MainActivity
import com.example.kaizenkanban.R
import com.example.kaizenkanban.data.local.KairosPreferences
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.reminders.DueReminderScheduler
import com.example.kaizenkanban.reminders.TaskQuickActions
import com.example.kaizenkanban.ui.i18n.AppLanguage
import com.example.kaizenkanban.ui.i18n.AppStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class KairosFocusWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tasks = TaskQuickActions.loadIncompleteDueTasks(context)
                appWidgetIds.forEach { id ->
                    KairosFocusWidgetUpdater.updateWidget(context, appWidgetManager, id, tasks)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Keeps call sites stable; refreshes the (only) home-screen widget. */
object KairosWidgetUpdater {
    fun updateAll(context: Context, tasks: List<Task>? = null) {
        val appContext = context.applicationContext
        if (tasks != null) {
            KairosFocusWidgetUpdater.updateAll(appContext, tasks)
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            KairosFocusWidgetUpdater.updateAll(
                appContext,
                TaskQuickActions.loadIncompleteDueTasks(appContext)
            )
        }
    }
}

object KairosFocusWidgetUpdater {
    fun updateAll(context: Context, tasks: List<Task>) {
        val appContext = context.applicationContext
        // Clear any leftover "listening" flag from older in-widget capture builds.
        KairosPreferences(appContext).voiceListeningActive = false
        val manager = AppWidgetManager.getInstance(appContext)
        val ids = manager.getAppWidgetIds(
            ComponentName(appContext, KairosFocusWidgetProvider::class.java)
        )
        if (ids.isEmpty()) return
        ids.forEach { id -> updateWidget(appContext, manager, id, tasks) }
    }

    fun updateWidget(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        tasks: List<Task>
    ) {
        val strings = AppStrings(AppLanguage.fromCode(KairosPreferences(context).appLanguageCode))
        val prefs = KairosPreferences(context)
        prefs.voiceListeningActive = false
        val focusId = prefs.inProgressTaskId
        val focusTask = focusId?.let { id -> tasks.find { it.id == id && !it.isCompleted } }
        val views = RemoteViews(context.packageName, R.layout.kairos_focus_widget)

        // Mic opens the app and starts dictate there — keeps the widget button idle-colored.
        val voiceIntent = PendingIntent.getActivity(
            context,
            4,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(DueReminderScheduler.EXTRA_OPEN_VOICE, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val addTaskIntent = PendingIntent.getActivity(
            context,
            5,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(DueReminderScheduler.EXTRA_OPEN_ADD_TASK, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (focusTask != null) {
            views.setViewVisibility(R.id.focus_widget_empty_actions, View.GONE)
            views.setViewVisibility(R.id.focus_widget_content, View.VISIBLE)
            views.setTextViewText(R.id.focus_widget_label, strings.widgetFocus)
            views.setTextViewText(R.id.focus_widget_title, focusTask.title)
            views.setOnClickPendingIntent(
                R.id.focus_widget_title,
                PendingIntent.getActivity(
                    context,
                    40_000 + focusTask.id.hashCode(),
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(DueReminderScheduler.EXTRA_TASK_ID, focusTask.id)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            views.setContentDescription(R.id.focus_widget_done, strings.markDone)
            views.setOnClickPendingIntent(
                R.id.focus_widget_done,
                PendingIntent.getBroadcast(
                    context,
                    50_000 + focusTask.id.hashCode(),
                    Intent(context, com.example.kaizenkanban.reminders.ReminderActionReceiver::class.java).apply {
                        action = com.example.kaizenkanban.reminders.DueReminderReceiver.ACTION_MARK_DONE
                        putExtra(DueReminderScheduler.EXTRA_TASK_ID, focusTask.id)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else {
            views.setViewVisibility(R.id.focus_widget_content, View.GONE)
            views.setViewVisibility(R.id.focus_widget_empty_actions, View.VISIBLE)
            views.setContentDescription(R.id.focus_widget_empty_add, strings.addTask)
            views.setOnClickPendingIntent(R.id.focus_widget_empty_add, addTaskIntent)
            views.setContentDescription(R.id.focus_widget_empty_voice, strings.widgetVoice)
            views.setOnClickPendingIntent(R.id.focus_widget_empty_voice, voiceIntent)
            views.setInt(R.id.focus_widget_empty_voice, "setBackgroundResource", 0)
            views.setImageViewBitmap(R.id.focus_widget_empty_voice, renderIdleMicBitmap(context))
        }

        manager.updateAppWidget(appWidgetId, views)
    }

    private fun renderIdleMicBitmap(context: Context): Bitmap {
        val density = context.resources.displayMetrics.density
        val size = (56f * density).toInt().coerceAtLeast(48)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = size / 2f
        val cy = size / 2f
        val radius = size / 2f
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                0f,
                size.toFloat(),
                size.toFloat(),
                0f,
                intArrayOf(0xFF4F46E5.toInt(), 0xFFA855F7.toInt()),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, radius, fill)
        val icon = ContextCompat.getDrawable(context, R.drawable.ic_widget_mic) ?: return bitmap
        val pad = (14f * density).toInt()
        icon.setBounds(pad, pad, size - pad, size - pad)
        icon.draw(canvas)
        return bitmap
    }
}
