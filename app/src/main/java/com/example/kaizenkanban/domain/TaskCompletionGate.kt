package com.example.kaizenkanban.domain

import kotlinx.coroutines.sync.Mutex

/**
 * Shared lock for complete / reopen / spawn / snooze across UI ([com.example.kaizenkanban.ui.viewmodel.SharedViewModel])
 * and notification/widget paths ([com.example.kaizenkanban.reminders.TaskQuickActions]).
 */
object TaskCompletionGate {
    val mutex = Mutex()
}
