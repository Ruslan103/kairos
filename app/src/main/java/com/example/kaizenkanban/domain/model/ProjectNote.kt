package com.example.kaizenkanban.domain.model

/** Project-scoped diary entry. Photos live in the device gallery. */
data class ProjectNote(
    val id: String,
    val projectId: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
    val position: Int = 0,
    val pinned: Boolean = false,
    val linkedTaskId: String? = null,
    /** When set, also drives a [KairosReminder] and calendar entry. */
    val reminderAtMillis: Long? = null,
    val title: String = ""
)

/** Headline for the feed. Falls back to the first non-blank line when [ProjectNote.title] is empty. */
fun ProjectNote.displayTitle(): String {
    val heading = title.trim()
    if (heading.isNotEmpty()) return heading
    return body.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()
}

/**
 * Lines shown under the headline.
 * When the title is empty, the first line is already the headline, so it is left out here.
 */
fun ProjectNote.displayLines(): String {
    val text = body.trim()
    if (text.isEmpty()) return ""
    if (title.isNotBlank()) return text
    val lines = text.lines()
    if (lines.size <= 1) return ""
    return lines.drop(1).joinToString("\n").trim()
}

/** One written line inside a diary entry. [createdAt] is when the line was added. */
data class NoteLine(
    val id: String,
    val noteId: String,
    val text: String,
    val createdAt: Long,
    val position: Int = 0
)

/** Split a legacy note body into lines. Every line keeps the note's [createdAt]. */
fun linesFromNoteBody(noteId: String, body: String, createdAt: Long): List<NoteLine> {
    val parts = body.split('\n').dropLastWhile { it.isBlank() }
    if (parts.isEmpty() || (parts.size == 1 && parts[0].isEmpty())) return emptyList()
    return parts.mapIndexed { index, text ->
        NoteLine(
            id = java.util.UUID.randomUUID().toString(),
            noteId = noteId,
            text = text,
            createdAt = createdAt,
            position = index
        )
    }
}

/** Gallery photo linked to one [NoteLine] inside a [ProjectNote]. */
data class NotePhoto(
    val id: String,
    val noteId: String,
    val mediaUri: String,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0,
    val lineId: String = ""
) {
    companion object {
        const val MAX_PER_NOTE = 10
    }
}

object NoteReminderIds {
    fun forNote(noteId: String): String = "note-rem:$noteId"
}
