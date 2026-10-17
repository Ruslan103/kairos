package com.example.kaizenkanban.domain.commands

import com.example.kaizenkanban.domain.model.TaskCardKind
import java.util.UUID

/** One inner row of a workout, shopping list, or contact card. */
data class KairosCardLine(
    val text: String,
    /** Workout: set count. Shop: quantity. Meet: unused. */
    val count: Int? = null
)

/**
 * Same JSON shape as [com.example.kaizenkanban.domain.model.TaskCardBody.encode].
 * Built by hand so command planning does not need Android's org.json.
 */
fun encodeKairosCard(cardType: String, lines: List<KairosCardLine>): Pair<String, String> {
    val kind = TaskCardKind.normalize(cardType)
    if (kind == TaskCardKind.PLAIN) return TaskCardKind.PLAIN to ""
    val rows = lines.joinToString(",") { line ->
        val extra: String
        val sets: String
        when (kind) {
            TaskCardKind.WORKOUT -> {
                val n = (line.count ?: 4).coerceIn(1, 30)
                extra = ""
                sets = List(n) { "false" }.joinToString(",", "[", "]")
            }
            TaskCardKind.SHOP -> {
                extra = line.count?.takeIf { it > 0 }?.toString().orEmpty()
                sets = "[]"
            }
            else -> {
                extra = ""
                sets = "[]"
            }
        }
        buildString {
            append("{\"id\":").append(jsonString(UUID.randomUUID().toString()))
            append(",\"text\":").append(jsonString(line.text))
            append(",\"extra\":").append(jsonString(extra))
            append(",\"done\":false,\"sets\":").append(sets).append("}")
        }
    }
    val payload = "{\"lines\":[$rows],\"contactIds\":[],\"contactId\":null,\"outcome\":\"\"," +
        "\"readKind\":\"book\",\"totalPages\":0,\"currentPage\":0,\"timerSeconds\":90}"
    return kind to payload
}

private fun jsonString(value: String): String = buildString {
    append('"')
    value.forEach { ch ->
        when (ch) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(ch)
        }
    }
    append('"')
}

/**
 * Parsed KAIROS v1 command block (create-only: goals, tasks, habits, links).
 */
data class KairosParseResult(
    val headerOk: Boolean,
    val commands: List<KairosCommand>,
    val errors: List<String>
)

sealed class KairosCommand {
    abstract val localKey: String

    data class Goal(
        override val localKey: String,
        val title: String,
        val createNew: Boolean,
        val existingId: String? = null,
        val dueDateYmd: String? = null,
        val remindMinutes: Int? = null,
        val impact: Int? = null,
        val eisenhower: String? = null,
        val hubOrColumn: String? = null,
        val startMinutes: Int? = null,
        val deadlineYmd: String? = null,
        val timerMinutes: Int? = null,
        val cardType: String = "plain",
        val cardLines: List<KairosCardLine> = emptyList()
    ) : KairosCommand()

    data class Task(
        override val localKey: String,
        val title: String,
        val parentRef: String? = null,
        val dueDateYmd: String? = null,
        val remindMinutes: Int? = null,
        val impact: Int? = null,
        val eisenhower: String? = null,
        val hubOrColumn: String? = null,
        val startMinutes: Int? = null,
        val deadlineYmd: String? = null,
        val timerMinutes: Int? = null,
        val cardType: String = "plain",
        val cardLines: List<KairosCardLine> = emptyList()
    ) : KairosCommand()

    data class Habit(
        override val localKey: String,
        val title: String,
        val parentRef: String? = null,
        val rhythm: String = "daily",
        val weekdays: Set<Int> = emptySet(),
        val intervalDays: Int? = null,
        val remindMinutesList: List<Int> = emptyList(),
        val impact: Int? = null,
        val eisenhower: String? = null,
        val hubOrColumn: String? = null,
        val dueDateYmd: String? = null,
        val timerMinutes: Int? = null,
        val deadlineOffsetDays: Int? = null,
        val deadlineOffsetHours: Int? = null,
        val cardType: String = "plain",
        val cardLines: List<KairosCardLine> = emptyList()
    ) : KairosCommand()

    data class Link(
        override val localKey: String,
        val parentRef: String,
        val childRef: String
    ) : KairosCommand()
}

data class KairosPlan(
    val actions: List<KairosPlannedAction>,
    val previewLines: List<String>,
    val errors: List<String>,
    val warnings: List<String>
) {
    val canApply: Boolean get() = errors.isEmpty() && actions.isNotEmpty()
}

sealed class KairosPlannedAction {
    data class CreateGoal(
        val localKey: String,
        val title: String,
        val columnId: String,
        val dueDateMillis: Long?,
        val reminderMinutesOfDay: Int?,
        val complexity: Int?,
        val eisenhowerQuadrant: String?,
        val startMinutesOfDay: Int? = null,
        val deadlineAtMillis: Long? = null,
        val timerMinutes: Int? = null,
        val cardType: String = "plain",
        val cardPayload: String = ""
    ) : KairosPlannedAction()

    data class ReuseGoal(
        val localKey: String,
        val existingId: String,
        val title: String
    ) : KairosPlannedAction()

    data class CreateTask(
        val localKey: String,
        val title: String,
        val columnId: String,
        val parentLocalKey: String?,
        val parentExistingId: String?,
        val dueDateMillis: Long?,
        val reminderMinutesOfDay: Int?,
        val complexity: Int?,
        val eisenhowerQuadrant: String?,
        val isGoal: Boolean = false,
        val startMinutesOfDay: Int? = null,
        val deadlineAtMillis: Long? = null,
        val timerMinutes: Int? = null,
        val cardType: String = "plain",
        val cardPayload: String = ""
    ) : KairosPlannedAction()

    data class CreateHabit(
        val localKey: String,
        val title: String,
        val targetBoardId: String,
        val targetColumnId: String,
        val parentLocalKey: String?,
        val parentExistingId: String?,
        val rhythm: String,
        val weekdays: Set<Int>,
        val intervalDays: Int?,
        val reminderTimesOfDay: List<Int>,
        val complexity: Int?,
        val eisenhowerQuadrant: String?,
        val startsOnDayMillis: Long = 0L,
        val deadlineOffsetDays: Int? = null,
        val deadlineOffsetHours: Int? = null,
        val timerMinutes: Int? = null,
        val cardType: String = "plain",
        val cardPayload: String = ""
    ) : KairosPlannedAction()

    data class CreateLink(
        val parentLocalKey: String?,
        val parentExistingId: String?,
        val childLocalKey: String?,
        val childExistingId: String?
    ) : KairosPlannedAction()
}

data class KairosApplyResult(
    val createdGoals: Int,
    val createdTasks: Int,
    val createdHabits: Int,
    val createdLinks: Int,
    val errors: List<String>,
    val createdTaskIds: List<String> = emptyList(),
    val createdHabitIds: List<String> = emptyList(),
    val createdLinksPairs: List<Pair<String, String>> = emptyList()
)

/** One applied command block that can be rolled back. */
data class KairosApplyUndo(
    val taskIds: List<String>,
    val habitIds: List<String>,
    val links: List<Pair<String, String>>
)
