package com.example.kaizenkanban.voice

import com.example.kaizenkanban.data.local.KairosPreferences
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.pro.Entitlements
import com.example.kaizenkanban.pro.ProFeature
import com.example.kaizenkanban.ui.resolveQuickAddTarget
import com.example.kaizenkanban.ui.viewmodel.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VoiceCommandExecutor(
    private val viewModel: SharedViewModel,
    private val prefs: KairosPreferences
) {
    /**
     * Mic always adds a task. [openColumnId] = current hub on the board; null → Quick Add target.
     */
    suspend fun execute(
        rawText: String,
        openColumnId: String?
    ): VoiceActionResult = withContext(Dispatchers.Main.immediate) {
        if (!Entitlements.has(prefs, ProFeature.VoiceAssistant)) {
            return@withContext VoiceActionResult.ProRequired
        }
        val command = VoiceCommandParser.parse(rawText)
            ?: return@withContext VoiceActionResult.NotUnderstood(rawText.trim())

        addTask(command.title, openColumnId, viewModel.state.value.tasks)
    }

    private fun addTask(
        title: String,
        openColumnId: String?,
        tasks: List<Task>
    ): VoiceActionResult {
        val state = viewModel.state.value
        val columnId = openColumnId?.takeIf { id -> state.columns.any { it.id == id } }
            ?: resolveQuickAddTarget(prefs, state.boards, state.columns)?.column?.id
            ?: return VoiceActionResult.NoDestination

        val currentTasks = tasks.filter { it.columnId == columnId }
        viewModel.addTask(
            title = title,
            columnId = columnId,
            categoryId = null,
            dueDate = null,
            currentTasks = currentTasks
        )
        return VoiceActionResult.Added(title)
    }
}
