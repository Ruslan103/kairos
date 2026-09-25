package com.example.kaizenkanban.voice

sealed class VoiceCommand {
    data class AddTask(val title: String) : VoiceCommand()
}

sealed class VoiceActionResult {
    data class Added(val title: String) : VoiceActionResult()
    data class NotUnderstood(val raw: String) : VoiceActionResult()
    data object NoDestination : VoiceActionResult()
    data object ProRequired : VoiceActionResult()
    data class Error(val message: String?) : VoiceActionResult()
}
