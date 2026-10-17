package com.example.kaizenkanban.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
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
import com.example.kaizenkanban.ui.theme.AppThemeMode
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
        val focusTask = focusId?.let { id ->
            tasks.find { it.id == id && !it.isCompleted && !it.isHidden }
        }
        val views = RemoteViews(context.packageName, R.layout.kairos_focus_widget)
        applyPaperStyle(context, prefs, views)

        // Mic opens the app and starts dictate there.
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
        val addNoteIntent = PendingIntent.getActivity(
            context,
            6,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(DueReminderScheduler.EXTRA_OPEN_ADD_NOTE, true)
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
            views.setContentDescription(R.id.focus_widget_empty_note, strings.widgetAddNote)
            views.setOnClickPendingIntent(R.id.focus_widget_empty_note, addNoteIntent)
        }

        manager.updateAppWidget(appWidgetId, views)
    }

    private fun applyPaperStyle(context: Context, prefs: KairosPreferences, views: RemoteViews) {
        val paper = widgetPaper(context, prefs)
        val background = when (paper) {
            WidgetPaper.DARK -> R.drawable.kairos_widget_background_dark
            WidgetPaper.SLATE -> R.drawable.kairos_widget_background_slate
            WidgetPaper.LIGHT -> R.drawable.kairos_widget_background
        }
        val button = when (paper) {
            WidgetPaper.DARK -> R.drawable.widget_btn_add_bg_dark
            WidgetPaper.SLATE -> R.drawable.widget_btn_add_bg_slate
            WidgetPaper.LIGHT -> R.drawable.widget_btn_add_bg
        }
        val title = when (paper) {
            WidgetPaper.LIGHT -> 0xFF111111.toInt()
            WidgetPaper.SLATE -> 0xFFF4F7F9.toInt()
            WidgetPaper.DARK -> 0xFFFAFAFA.toInt()
        }
        val label = when (paper) {
            WidgetPaper.LIGHT -> 0xFF737373.toInt()
            WidgetPaper.SLATE -> 0xFFE2E8ED.toInt()
            WidgetPaper.DARK -> 0xFFA3A3A3.toInt()
        }
        val icon = when (paper) {
            WidgetPaper.LIGHT -> 0xFF404040.toInt()
            WidgetPaper.SLATE -> 0xFFF4F7F9.toInt()
            WidgetPaper.DARK -> 0xFFE5E5E5.toInt()
        }
        views.setInt(R.id.focus_widget_root, "setBackgroundResource", background)
        views.setTextColor(R.id.focus_widget_label, label)
        views.setTextColor(R.id.focus_widget_title, title)
        listOf(
            R.id.focus_widget_empty_add,
            R.id.focus_widget_empty_voice,
            R.id.focus_widget_empty_note,
            R.id.focus_widget_done
        ).forEach { id ->
            views.setInt(id, "setBackgroundResource", button)
            views.setInt(id, "setColorFilter", icon)
        }
    }

    private enum class WidgetPaper { LIGHT, DARK, SLATE }

    private fun widgetPaper(context: Context, prefs: KairosPreferences): WidgetPaper {
        return when (AppThemeMode.fromCode(prefs.themeMode)) {
            AppThemeMode.LIGHT -> WidgetPaper.LIGHT
            AppThemeMode.DARK -> WidgetPaper.DARK
            AppThemeMode.SLATE -> WidgetPaper.SLATE
            AppThemeMode.SYSTEM -> {
                val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                if (night == Configuration.UI_MODE_NIGHT_YES) WidgetPaper.DARK else WidgetPaper.LIGHT
            }
        }
    }
}
