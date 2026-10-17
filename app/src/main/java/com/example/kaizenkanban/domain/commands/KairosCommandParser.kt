package com.example.kaizenkanban.domain.commands

import com.example.kaizenkanban.domain.model.RecurringRhythm

/**
 * Parses a pasted KAIROS v1 command block into structured commands.
 *
 * Example:
 * ```
 * KAIROS v1
 * goal: "Run 10k" [new]
 * task: "Buy shoes"
 *   parent: goal
 *   due: 2026-09-28
 *   remind: 09:00
 *   impact: 2
 *   eisenhower: Q1
 * ```
 */
object KairosCommandParser {

    private val HEADER = Regex("""^\s*KAIROS\s+v1\s*$""", RegexOption.IGNORE_CASE)
    private val CMD = Regex(
        """^(goal|task|habit|link)\s*:\s*(.+)$""",
        RegexOption.IGNORE_CASE
    )
    private val PROP = Regex(
        """^(parent|due|start|remind|deadline|finish|timer|impact|eisenhower|hub|column|board|rhythm|days|interval|deadline_days|deadline_hours|child|type|line)\s*:\s*(.+)$""",
        RegexOption.IGNORE_CASE
    )
    private val QUOTED = Regex("""^"([^"]*)"(.*)$""")
    private val FLAG_NEW = Regex("""\[new\]""", RegexOption.IGNORE_CASE)
    private val FLAG_ID = Regex("""\[id\s*:\s*([^\]]+)\]""", RegexOption.IGNORE_CASE)
    private val TIME = Regex("""^(\d{1,2}):(\d{2})$""")
    private val YMD = Regex("""^(\d{4})-(\d{2})-(\d{2})$""")

    fun parse(raw: String): KairosParseResult {
        val text = extractBlock(raw.trim())
        if (text.isBlank()) {
            return KairosParseResult(false, emptyList(), listOf("Empty command block"))
        }
        val lines = text.lines()
        val firstNonBlank = lines.firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        if (!HEADER.matches(firstNonBlank)) {
            return KairosParseResult(
                false,
                emptyList(),
                listOf("Block must start with: KAIROS v1")
            )
        }

        val commands = mutableListOf<KairosCommand>()
        val errors = mutableListOf<String>()
        var goalCount = 0
        var taskCount = 0
        var habitCount = 0
        var linkCount = 0

        var current: MutableCmd? = null

        fun flush() {
            val c = current ?: return
            when (val built = c.build(errors)) {
                null -> Unit
                else -> commands += built
            }
            current = null
        }

        for ((index, rawLine) in lines.withIndex()) {
            if (index == 0) continue
            val line = rawLine.trimEnd()
            val trimmed = line.trim()
            if (line.isBlank() || trimmed.startsWith("#") || trimmed == "```") continue

            val cmdMatch = CMD.matchEntire(trimmed)
            if (cmdMatch != null) {
                flush()
                val kind = cmdMatch.groupValues[1].lowercase()
                val rest = cmdMatch.groupValues[2].trim()
                when (kind) {
                    "goal" -> {
                        val key = "g${goalCount++}"
                        val (title, createNew, existingId) = parseGoalHead(rest, errors, key)
                        if (title != null) {
                            current = MutableCmd.Goal(key, title, createNew, existingId)
                        }
                    }
                    "task" -> {
                        val key = "t${taskCount++}"
                        val title = parseQuotedTitle(rest, errors, "task $key")
                        if (title != null) current = MutableCmd.Task(key, title)
                    }
                    "habit" -> {
                        val key = "h${habitCount++}"
                        val title = parseQuotedTitle(rest, errors, "habit $key")
                        if (title != null) current = MutableCmd.Habit(key, title)
                    }
                    "link" -> {
                        flush()
                        val key = "l${linkCount++}"
                        // link: "parent" -> "child"  OR link with props
                        val arrow = Regex("""^"([^"]+)"\s*->\s*"([^"]+)"$""").matchEntire(rest)
                        if (arrow != null) {
                            commands += KairosCommand.Link(
                                localKey = key,
                                parentRef = arrow.groupValues[1].trim(),
                                childRef = arrow.groupValues[2].trim()
                            )
                        } else {
                            current = MutableCmd.Link(key)
                            // allow bare link: then parent/child as props
                            if (rest.isNotBlank() && !rest.equals("→", ignoreCase = true)) {
                                errors += "link $key: use link: \"Parent\" -> \"Child\" or parent:/child: props"
                            }
                        }
                    }
                }
                continue
            }

            val propMatch = PROP.matchEntire(trimmed)
            if (propMatch != null) {
                val c = current
                if (c == null) {
                    errors += "Property without command: ${propMatch.groupValues[1]}"
                    continue
                }
                applyProp(c, propMatch.groupValues[1].lowercase(), propMatch.groupValues[2].trim(), errors)
                continue
            }

            // Ignore project:/hub: at top level as optional metadata for AI
            if (trimmed.startsWith("project:", ignoreCase = true) ||
                trimmed.startsWith("hub:", ignoreCase = true) && current == null
            ) {
                continue
            }

            errors += "Unrecognized line: ${trimmed.take(80)}"
        }
        flush()

        return KairosParseResult(true, commands, errors)
    }

    /** Strip markdown fences / leading chatter; keep from KAIROS v1 onward. */
    fun extractBlock(raw: String): String {
        var t = raw.trim()
        if (t.startsWith("```")) {
            t = t.removePrefix("```").trimStart()
            if (t.startsWith("text", ignoreCase = true) || t.startsWith("kairos", ignoreCase = true)) {
                t = t.substringAfter('\n', t)
            }
            if (t.endsWith("```")) t = t.removeSuffix("```").trimEnd()
        }
        val idx = Regex("""(?im)^\s*KAIROS\s+v1\s*$""").find(t)?.range?.first
        if (idx != null) t = t.substring(idx).trim()
        // Drop a trailing markdown fence if the model wrapped the block.
        val fenceClose = Regex("""(?m)^\s*```\s*$""")
        val closeMatch = fenceClose.findAll(t).lastOrNull()
        if (closeMatch != null && closeMatch.range.first > 0) {
            t = t.substring(0, closeMatch.range.first).trimEnd()
        }
        return t
    }

    private fun parseGoalHead(
        rest: String,
        errors: MutableList<String>,
        key: String
    ): Triple<String?, Boolean, String?> {
        val q = QUOTED.matchEntire(rest.trim())
        if (q == null) {
            errors += "goal $key: title must be in quotes"
            return Triple(null, true, null)
        }
        val title = q.groupValues[1].trim()
        if (title.isBlank()) {
            errors += "goal $key: empty title"
            return Triple(null, true, null)
        }
        val flags = q.groupValues[2]
        val id = FLAG_ID.find(flags)?.groupValues?.get(1)?.trim()
        val forceNew = FLAG_NEW.containsMatchIn(flags)
        // [id:x] → reuse that id; bare title → planner matches existing goal by title or creates
        return Triple(title, forceNew, if (!forceNew) id else null)
    }

    private fun parseQuotedTitle(rest: String, errors: MutableList<String>, label: String): String? {
        val q = QUOTED.matchEntire(rest.trim())
            ?: Regex("""^([^\[\]]+?)(\s*\[.*\])?$""").matchEntire(rest.trim())
        return if (q != null) {
            val title = q.groupValues[1].trim().removeSurrounding("\"")
            if (title.isBlank()) {
                errors += "$label: empty title"
                null
            } else title
        } else {
            errors += "$label: title must be in quotes"
            null
        }
    }

    private fun applyProp(
        cmd: MutableCmd,
        name: String,
        value: String,
        errors: MutableList<String>
    ) {
        when (cmd) {
            is MutableCmd.Goal -> when (name) {
                "due" -> cmd.dueDateYmd = parseYmd(value, errors, cmd.localKey)
                "start" -> cmd.startMinutes = parseTime(value, errors, cmd.localKey)
                "remind" -> cmd.remindMinutes = parseTime(value, errors, cmd.localKey)
                "deadline", "finish" -> cmd.deadlineYmd = parseYmd(value, errors, cmd.localKey)
                "timer" -> cmd.timerMinutes = parseTimer(value, errors, cmd.localKey)
                "impact" -> cmd.impact = parseImpact(value, errors, cmd.localKey)
                "eisenhower" -> cmd.eisenhower = parseEisenhower(value, errors, cmd.localKey)
                "hub", "column", "board" -> cmd.hubOrColumn = unquote(value)
                "type" -> cmd.cardType = parseCardType(value, errors, cmd.localKey)
                "line" -> parseCardLine(value, errors, cmd.localKey)?.let { cmd.cardLines = cmd.cardLines + it }
                else -> errors += "goal ${cmd.localKey}: unsupported property $name"
            }
            is MutableCmd.Task -> when (name) {
                "parent" -> cmd.parentRef = unquote(value)
                "due" -> cmd.dueDateYmd = parseYmd(value, errors, cmd.localKey)
                "start" -> cmd.startMinutes = parseTime(value, errors, cmd.localKey)
                "remind" -> cmd.remindMinutes = parseTime(value, errors, cmd.localKey)
                "deadline", "finish" -> cmd.deadlineYmd = parseYmd(value, errors, cmd.localKey)
                "timer" -> cmd.timerMinutes = parseTimer(value, errors, cmd.localKey)
                "impact" -> cmd.impact = parseImpact(value, errors, cmd.localKey)
                "eisenhower" -> cmd.eisenhower = parseEisenhower(value, errors, cmd.localKey)
                "hub", "column", "board" -> cmd.hubOrColumn = unquote(value)
                "type" -> cmd.cardType = parseCardType(value, errors, cmd.localKey)
                "line" -> parseCardLine(value, errors, cmd.localKey)?.let { cmd.cardLines = cmd.cardLines + it }
                else -> errors += "task ${cmd.localKey}: unsupported property $name"
            }
            is MutableCmd.Habit -> when (name) {
                "parent" -> cmd.parentRef = unquote(value)
                "due" -> cmd.dueDateYmd = parseYmd(value, errors, cmd.localKey)
                "start", "remind" -> {
                    val one = parseTime(value, errors, cmd.localKey)
                    if (one != null) cmd.remindMinutesList = cmd.remindMinutesList + one
                }
                "timer" -> cmd.timerMinutes = parseTimer(value, errors, cmd.localKey)
                "deadline_days" -> cmd.deadlineOffsetDays = parseNonNegInt(value, errors, cmd.localKey, 0, 999)
                "deadline_hours" -> cmd.deadlineOffsetHours = parseNonNegInt(value, errors, cmd.localKey, 0, 999)
                "impact" -> cmd.impact = parseImpact(value, errors, cmd.localKey)
                "eisenhower" -> cmd.eisenhower = parseEisenhower(value, errors, cmd.localKey)
                "hub", "column", "board" -> cmd.hubOrColumn = unquote(value)
                "rhythm" -> {
                    val parsed = parseRhythm(value, errors, cmd.localKey)
                    cmd.rhythm = parsed.first
                    if (parsed.second != null && cmd.intervalDays == null) {
                        cmd.intervalDays = parsed.second
                    }
                }
                "days" -> cmd.weekdays = parseDays(value, errors, cmd.localKey)
                "interval" -> cmd.intervalDays = value.trim().toIntOrNull()?.let { RecurringRhythm.intervalDays(it) }
                    ?: run {
                        errors += "habit ${cmd.localKey}: bad interval"
                        null
                    }
                "type" -> cmd.cardType = parseCardType(value, errors, cmd.localKey)
                "line" -> parseCardLine(value, errors, cmd.localKey)?.let { cmd.cardLines = cmd.cardLines + it }
                else -> errors += "habit ${cmd.localKey}: unsupported property $name"
            }
            is MutableCmd.Link -> when (name) {
                "parent" -> cmd.parentRef = unquote(value)
                "child" -> cmd.childRef = unquote(value)
                else -> errors += "link ${cmd.localKey}: unsupported property $name"
            }
        }
    }

    fun parseTime(value: String, errors: MutableList<String>, key: String): Int? {
        val m = TIME.matchEntire(value.trim())
        if (m == null) {
            errors += "$key: bad time (use HH:mm): $value"
            return null
        }
        val h = m.groupValues[1].toInt()
        val min = m.groupValues[2].toInt()
        if (h !in 0..23 || min !in 0..59) {
            errors += "$key: time out of range: $value"
            return null
        }
        return h * 60 + min
    }

    fun parseYmd(value: String, errors: MutableList<String>, key: String): String? {
        val v = value.trim().lowercase().replace('ё', 'е')
        if (v in setOf("today", "сегодня", "tomorrow", "завтра")) return v
        val m = YMD.matchEntire(value.trim())
        if (m == null) {
            errors += "$key: bad date (use YYYY-MM-DD, today, or tomorrow): $value"
            return null
        }
        return value.trim()
    }

    fun parseTimer(value: String, errors: MutableList<String>, key: String): Int? {
        val digits = value.trim().lowercase()
            .removeSuffix("min")
            .removeSuffix("mins")
            .removeSuffix("minutes")
            .removeSuffix("мин")
            .removeSuffix("м")
            .trim()
            .toIntOrNull()
        if (digits == null || digits !in 1..999) {
            errors += "$key: timer must be 1–999 minutes"
            return null
        }
        return digits
    }

    fun parseNonNegInt(value: String, errors: MutableList<String>, key: String, min: Int, max: Int): Int? {
        val n = value.trim().toIntOrNull()
        if (n == null || n !in min..max) {
            errors += "$key: expected $min–$max, got $value"
            return null
        }
        return n
    }

    fun parseRhythm(value: String, errors: MutableList<String>, key: String): Pair<String, Int?> {
        val v = value.trim().lowercase().replace('ё', 'е')
        Regex("""^every\s+(\d+)\s+days?$""").matchEntire(v)?.let {
            return "every_n_days" to RecurringRhythm.intervalDays(it.groupValues[1].toInt())
        }
        Regex("""^каждые\s+(\d+)\s+(день|дня|дней)$""").matchEntire(v)?.let {
            return "every_n_days" to RecurringRhythm.intervalDays(it.groupValues[1].toInt())
        }
        return when (v) {
            "daily", "every_day", "every day", "каждый день" -> "daily" to null
            "weekdays", "по дням", "по будням" -> "weekdays" to null
            "every_n_days", "every_n", "каждые" -> "every_n_days" to null
            else -> {
                errors += "habit $key: unknown rhythm $value"
                "daily" to null
            }
        }
    }

    fun parseCardType(value: String, errors: MutableList<String>, key: String): String {
        val v = value.trim().lowercase().replace('ё', 'е')
        return when (v) {
            "plain", "обычная", "обычный" -> "plain"
            "workout", "train", "training", "тренировка" -> "workout"
            "shop", "shopping", "покупки" -> "shop"
            "meet", "contact", "meeting", "связь" -> "meet"
            else -> {
                errors += "$key: type must be plain, workout, shop, or meet"
                "plain"
            }
        }
    }

    fun parseCardLine(value: String, errors: MutableList<String>, key: String): KairosCardLine? {
        val trimmed = value.trim()
        val match = Regex("""^"([^"]*)"(?:\s*[xх×]\s*(\d+))?$""", RegexOption.IGNORE_CASE)
            .matchEntire(trimmed)
            ?: Regex("""^(.+?)(?:\s*[xх×]\s*(\d+))?$""").matchEntire(trimmed)
        if (match == null) {
            errors += "$key: bad line"
            return null
        }
        val text = match.groupValues[1].trim().removeSurrounding("\"")
        if (text.isBlank()) {
            errors += "$key: empty line"
            return null
        }
        val countRaw = match.groupValues[2]
        val count = if (countRaw.isBlank()) {
            null
        } else {
            val n = countRaw.toIntOrNull()
            if (n == null || n !in 1..999) {
                errors += "$key: line count must be 1–999"
                return null
            }
            n
        }
        return KairosCardLine(text, count)
    }

    fun parseImpact(value: String, errors: MutableList<String>, key: String): Int? {
        val n = value.trim().toIntOrNull()
        if (n == null || n !in 1..5) {
            errors += "$key: impact must be 1–5"
            return null
        }
        return n
    }

    /** Accepts Q1–Q4, IU/IN/NU/NN, or Russian short forms. */
    fun parseEisenhower(value: String, errors: MutableList<String>, key: String): String? {
        val v = value.trim().uppercase().replace(" ", "")
        return when (v) {
            "Q1", "IU", "UI", "ВАЖНО-СРОЧНО", "СРОЧНО-ВАЖНО" -> "Q1"
            "Q2", "IN", "NI", "ВАЖНО" -> "Q2"
            "Q3", "NU", "UN", "СРОЧНО" -> "Q3"
            "Q4", "NN", "ПРОЧЕЕ" -> "Q4"
            else -> {
                errors += "$key: eisenhower must be Q1–Q4 (or IU/IN/NU/NN)"
                null
            }
        }
    }

    fun parseDays(value: String, errors: MutableList<String>, key: String): Set<Int> {
        val parts = value.split(',', ' ', ';').map { it.trim() }.filter { it.isNotEmpty() }
        val out = mutableSetOf<Int>()
        for (p in parts) {
            val n = when (p.lowercase()) {
                "mon", "пн", "1" -> 1
                "tue", "вт", "2" -> 2
                "wed", "ср", "3" -> 3
                "thu", "чт", "4" -> 4
                "fri", "пт", "5" -> 5
                "sat", "сб", "6" -> 6
                "sun", "вс", "7" -> 7
                else -> p.toIntOrNull()
            }
            if (n == null || n !in 1..7) {
                errors += "$key: bad weekday: $p"
            } else out += n
        }
        return out
    }

    private fun rejectLinesOnPlain(
        cardType: String,
        lines: List<KairosCardLine>,
        errors: MutableList<String>,
        label: String
    ) {
        if (lines.isNotEmpty() && cardType == "plain") {
            errors += "$label: line needs type: workout, shop, or meet"
        }
    }

    private fun unquote(value: String): String =
        value.trim().removeSurrounding("\"")

    private sealed class MutableCmd {
        abstract val localKey: String
        abstract fun build(errors: MutableList<String>): KairosCommand?

        class Goal(
            override val localKey: String,
            val title: String,
            val createNew: Boolean,
            val existingId: String?
        ) : MutableCmd() {
            var dueDateYmd: String? = null
            var remindMinutes: Int? = null
            var impact: Int? = null
            var eisenhower: String? = null
            var hubOrColumn: String? = null
            var startMinutes: Int? = null
            var deadlineYmd: String? = null
            var timerMinutes: Int? = null
            var cardType: String = "plain"
            var cardLines: List<KairosCardLine> = emptyList()
            override fun build(errors: MutableList<String>): KairosCommand {
                rejectLinesOnPlain(cardType, cardLines, errors, "goal $localKey")
                return KairosCommand.Goal(
                    localKey, title, createNew, existingId, dueDateYmd, remindMinutes, impact, eisenhower, hubOrColumn,
                    startMinutes, deadlineYmd, timerMinutes, cardType, cardLines
                )
            }
        }

        class Task(
            override val localKey: String,
            val title: String
        ) : MutableCmd() {
            var parentRef: String? = null
            var dueDateYmd: String? = null
            var remindMinutes: Int? = null
            var impact: Int? = null
            var eisenhower: String? = null
            var hubOrColumn: String? = null
            var startMinutes: Int? = null
            var deadlineYmd: String? = null
            var timerMinutes: Int? = null
            var cardType: String = "plain"
            var cardLines: List<KairosCardLine> = emptyList()
            override fun build(errors: MutableList<String>): KairosCommand {
                rejectLinesOnPlain(cardType, cardLines, errors, "task $localKey")
                return KairosCommand.Task(
                    localKey, title, parentRef, dueDateYmd, remindMinutes, impact, eisenhower, hubOrColumn,
                    startMinutes, deadlineYmd, timerMinutes, cardType, cardLines
                )
            }
        }

        class Habit(
            override val localKey: String,
            val title: String
        ) : MutableCmd() {
            var parentRef: String? = null
            var rhythm: String = "daily"
            var weekdays: Set<Int> = emptySet()
            var intervalDays: Int? = null
            var remindMinutesList: List<Int> = emptyList()
            var impact: Int? = null
            var eisenhower: String? = null
            var hubOrColumn: String? = null
            var dueDateYmd: String? = null
            var timerMinutes: Int? = null
            var deadlineOffsetDays: Int? = null
            var deadlineOffsetHours: Int? = null
            var cardType: String = "plain"
            var cardLines: List<KairosCardLine> = emptyList()
            override fun build(errors: MutableList<String>): KairosCommand {
                val r = when (rhythm) {
                    "daily", "weekdays", "every_n_days" -> rhythm
                    else -> {
                        errors += "habit $localKey: unknown rhythm $rhythm"
                        "daily"
                    }
                }
                rejectLinesOnPlain(cardType, cardLines, errors, "habit $localKey")
                return KairosCommand.Habit(
                    localKey, title, parentRef, r, weekdays, intervalDays,
                    remindMinutesList, impact, eisenhower, hubOrColumn,
                    dueDateYmd, timerMinutes, deadlineOffsetDays, deadlineOffsetHours,
                    cardType, cardLines
                )
            }
        }

        class Link(override val localKey: String) : MutableCmd() {
            var parentRef: String? = null
            var childRef: String? = null
            override fun build(errors: MutableList<String>): KairosCommand? {
                val p = parentRef
                val c = childRef
                if (p.isNullOrBlank() || c.isNullOrBlank()) {
                    errors += "link $localKey: need parent and child"
                    return null
                }
                return KairosCommand.Link(localKey, p, c)
            }
        }
    }
}
