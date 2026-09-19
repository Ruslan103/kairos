package com.example.kaizenkanban.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
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

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            KairosWidgetProvider.ACTION_MARK_FOCUS_DONE -> {
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
            else -> super.onReceive(context, intent)
        }
    }
}

object KairosFocusWidgetUpdater {
    fun updateAll(context: Context, tasks: List<Task>) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val ids = manager.getAppWidgetIds(ComponentName(appContext, KairosFocusWidgetProvider::class.java))
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
        val focusId = prefs.inProgressTaskId
        val focusTask = focusId?.let { id -> tasks.find { it.id == id && !it.isCompleted } }
        val views = RemoteViews(context.packageName, R.layout.kairos_focus_widget)
        views.setTextViewText(R.id.focus_widget_label, strings.widgetFocus)

        if (focusTask != null) {
            views.setViewVisibility(R.id.focus_widget_title, View.VISIBLE)
            views.setViewVisibility(R.id.focus_widget_done, View.VISIBLE)
            views.setViewVisibility(R.id.focus_widget_empty, View.GONE)
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
            views.setTextViewText(R.id.focus_widget_done, strings.markDone)
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
            views.setViewVisibility(R.id.focus_widget_title, View.GONE)
            views.setViewVisibility(R.id.focus_widget_done, View.GONE)
            views.setViewVisibility(R.id.focus_widget_empty, View.VISIBLE)
            views.setTextViewText(R.id.focus_widget_empty, strings.widgetEmpty)
            views.setOnClickPendingIntent(
                R.id.focus_widget_root,
                PendingIntent.getActivity(
                    context,
                    2,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }
        manager.updateAppWidget(appWidgetId, views)
    }
}
