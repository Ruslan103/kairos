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
import com.example.kaizenkanban.ui.calendar.isDueToday
import com.example.kaizenkanban.ui.calendar.isOverdueDate
import com.example.kaizenkanban.ui.i18n.AppLanguage
import com.example.kaizenkanban.ui.i18n.AppStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class KairosWidgetProvider : AppWidgetProvider() {
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
                    KairosWidgetUpdater.updateWidget(context, appWidgetManager, id, tasks)
                }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_MARK_FOCUS_DONE -> {
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

    companion object {
        const val ACTION_MARK_FOCUS_DONE = "com.example.kaizenkanban.action.WIDGET_MARK_FOCUS_DONE"
    }
}

object KairosWidgetUpdater {
    private val rowIds = intArrayOf(
        R.id.widget_task_1,
        R.id.widget_task_2,
        R.id.widget_task_3,
        R.id.widget_task_4,
        R.id.widget_task_5
    )

    fun updateAll(context: Context, tasks: List<Task>? = null) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        fun apply(loaded: List<Task>) {
            manager.getAppWidgetIds(ComponentName(appContext, KairosWidgetProvider::class.java))
                .forEach { id -> updateWidget(appContext, manager, id, loaded) }
            KairosFocusWidgetUpdater.updateAll(appContext, loaded)
        }
        if (tasks != null) {
            apply(tasks)
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            apply(TaskQuickActions.loadIncompleteDueTasks(appContext))
        }
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
        val dueLines = tasks
            .filter { !it.isCompleted && it.dueDate != null && (it.dueDate.isOverdueDate() || it.dueDate.isDueToday()) }
            .sortedWith(
                compareBy<Task> { task ->
                    when {
                        task.dueDate!!.isOverdueDate() -> 0
                        else -> 1
                    }
                }.thenBy { it.dueDate }
            )
            .filter { it.id != focusTask?.id }
            .take(5)

        val views = RemoteViews(context.packageName, R.layout.kairos_widget)
        views.setTextViewText(R.id.widget_title, strings.widgetTitle)

        if (focusTask != null) {
            views.setViewVisibility(R.id.widget_focus_block, View.VISIBLE)
            views.setTextViewText(R.id.widget_focus_label, strings.widgetFocus)
            views.setTextViewText(R.id.widget_focus_title, focusTask.title)
            views.setOnClickPendingIntent(
                R.id.widget_focus_title,
                openTaskPending(context, focusTask.id, requestCode = 10_000 + focusTask.id.hashCode())
            )
            views.setTextViewText(R.id.widget_focus_done, strings.markDone)
            views.setOnClickPendingIntent(
                R.id.widget_focus_done,
                PendingIntent.getBroadcast(
                    context,
                    20_000 + focusTask.id.hashCode(),
                    Intent(context, com.example.kaizenkanban.reminders.ReminderActionReceiver::class.java).apply {
                        action = com.example.kaizenkanban.reminders.DueReminderReceiver.ACTION_MARK_DONE
                        putExtra(DueReminderScheduler.EXTRA_TASK_ID, focusTask.id)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else {
            views.setViewVisibility(R.id.widget_focus_block, View.GONE)
        }

        views.setTextViewText(R.id.widget_due_label, strings.widgetDueSection)
        rowIds.forEachIndexed { index, viewId ->
            val task = dueLines.getOrNull(index)
            if (task == null) {
                views.setViewVisibility(viewId, View.GONE)
            } else {
                views.setViewVisibility(viewId, View.VISIBLE)
                val prefix = if (task.dueDate!!.isOverdueDate()) "⚠ " else "• "
                views.setTextViewText(viewId, prefix + task.title)
                views.setOnClickPendingIntent(
                    viewId,
                    openTaskPending(context, task.id, requestCode = 30_000 + task.id.hashCode() + index)
                )
            }
        }

        if (focusTask == null && dueLines.isEmpty()) {
            views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
            views.setTextViewText(R.id.widget_empty, strings.widgetEmpty)
        } else {
            views.setViewVisibility(R.id.widget_empty, View.GONE)
        }

        views.setTextViewText(R.id.widget_quick_add, strings.widgetQuickAdd)
        views.setOnClickPendingIntent(
            R.id.widget_quick_add,
            PendingIntent.getActivity(
                context,
                2,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(DueReminderScheduler.EXTRA_OPEN_QUICK_ADD, true)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        views.setOnClickPendingIntent(
            R.id.widget_root,
            PendingIntent.getActivity(
                context,
                1,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        manager.updateAppWidget(appWidgetId, views)
    }

    private fun openTaskPending(context: Context, taskId: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(DueReminderScheduler.EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
