package com.example.kaizenkanban.voice

/**
 * Mic always creates a task: spoken text becomes the title.
 * Optional "add task …" prefixes are stripped when present.
 */
object VoiceCommandParser {

    fun parse(raw: String): VoiceCommand.AddTask? {
        val text = normalize(raw)
        if (text.isBlank()) return null
        val title = stripOptionalAddPrefix(text).trimTitle()
        if (title.isBlank()) return null
        return VoiceCommand.AddTask(title)
    }

    private fun normalize(raw: String): String {
        var t = raw.trim().lowercase()
            .replace('ё', 'е')
            .replace(',', ' ')
            .replace(Regex("\\s+"), " ")
        val prefixes = listOf(
            "ок кайрос ",
            "окей кайрос ",
            "кайрос ",
            "ok kairos ",
            "okay kairos ",
            "hey kairos ",
            "kairos "
        )
        for (p in prefixes) {
            if (t.startsWith(p)) {
                t = t.removePrefix(p).trim()
                break
            }
        }
        return t.trim(' ', ',', '.', '!', '?')
    }

    /** Keep "купить молоко" if user still says "добавь задачу купить молоко". */
    private fun stripOptionalAddPrefix(text: String): String {
        val patterns = listOf(
            Regex("^добавь(?:те)?\\s+задач[уа]\\s+(.+)$"),
            Regex("^добавить\\s+задач[уа]\\s+(.+)$"),
            Regex("^новая\\s+задача\\s+(.+)$"),
            Regex("^создай\\s+задач[уа]\\s+(.+)$"),
            Regex("^создать\\s+задач[уа]\\s+(.+)$"),
            Regex("^add\\s+task\\s+(.+)$"),
            Regex("^create\\s+task\\s+(.+)$"),
            Regex("^new\\s+task\\s+(.+)$")
        )
        for (p in patterns) {
            val m = p.find(text) ?: continue
            return m.groupValues[1].trim()
        }
        return text
    }

    private fun String.trimTitle(): String =
        trim().trim(' ', ',', '.', '!', '?', '"', '«', '»')
}
