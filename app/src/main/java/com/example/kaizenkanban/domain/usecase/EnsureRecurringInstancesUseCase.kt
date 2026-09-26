package com.example.kaizenkanban.domain.usecase

import com.example.kaizenkanban.domain.model.Board
import com.example.kaizenkanban.domain.model.Column
import com.example.kaizenkanban.domain.model.RecurringRhythm
import com.example.kaizenkanban.domain.model.RecurringTemplate
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.domain.model.TaskLink
import com.example.kaizenkanban.domain.model.TaskLinkGraph
import com.example.kaizenkanban.domain.repository.KanbanRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.UUID

data class RecurringTarget(
    val boardId: String,
    val columnId: String
)

object RecurringTargetResolver {
    fun resolveDefault(
        projectId: String,
        boards: List<Board>,
        columns: List<Column>
    ): RecurringTarget? {
        val projectBoards = boards.filter { it.projectId == projectId && !it.isArchived }
        if (projectBoards.isEmpty()) return null
        val board = projectBoards.find { it.isDefault }
            ?: projectBoards.find {
                it.name.contains("OKR", ignoreCase = true) ||
                    it.name.contains("Лестница целей", ignoreCase = true) ||
                    it.name.contains("Goal ladder", ignoreCase = true)
            }
            ?: projectBoards.first()
        val boardColumns = columns.filter { it.boardId == board.id }.sortedBy { it.position }
        if (boardColumns.isEmpty()) return null
        val column = boardColumns.find { col ->
            val t = col.title.trim().lowercase()
            t == "сделать" || t == "do" || t == "to do" || t == "todo" ||
                t == "сегодня" || t == "today" || t.contains("сегодня") || t.contains("today")
        } ?: boardColumns.first()
        return RecurringTarget(board.id, column.id)
    }
}

/**
 * Spawns today's task instances from enabled recurring templates.
 * One instance per configured time of day.
 */
class EnsureRecurringInstancesUseCase(
    private val repository: KanbanRepository
) {
    suspend operator fun invoke(
        templates: List<RecurringTemplate>,
        tasks: List<Task>,
        columns: List<Column>,
        now: Long = System.currentTimeMillis()
    ): Int {
        val startToday = startOfDay(now)
        val endToday = startToday + DAY_MS - 1
        val todayIsoDow = isoDayOfWeek(now)
        val columnIds = columns.map { it.id }.toSet()
        val workingLinks = repository.getAllTaskLinks().first().toMutableList()

        var created = 0
        for (template in templates) {
            if (!template.enabled) continue
            if (template.title.isBlank()) continue
            if (template.targetColumnId !in columnIds) continue

            val shouldCreateToday = when (template.rhythm) {
                RecurringRhythm.DAILY -> true
                RecurringRhythm.WEEKDAYS -> todayIsoDow in template.weekdays
                RecurringRhythm.EVERY_N_DAYS -> {
                    val n = (template.timesPerWeek ?: 2).coerceIn(2, 30)
                    val anchor = startOfDay(template.createdAt)
                    val days = ((startToday - anchor) / DAY_MS).coerceAtLeast(0L)
                    days % n == 0L
                }
                else -> false
            }
            if (!shouldCreateToday) continue

            val times = template.reminderTimesOfDay
                .map { it.coerceIn(0, 24 * 60 - 1) }
                .distinct()
                .sorted()
                .ifEmpty { listOf(9 * 60) }

            var nextPos = tasks
                .filter { it.columnId == template.targetColumnId }
                .maxOfOrNull { it.position }
                ?.plus(1)
                ?: 0

            for (minutes in times) {
                val skipKey = occurrenceKey(startToday, minutes)
                if (skipKey in template.skippedOccurrenceKeys) continue

                val already = tasks.any { task ->
                    task.recurringTemplateId == template.id &&
                        task.dueDate != null &&
                        task.dueDate in startToday..endToday &&
                        instanceMinutes(task) == minutes
                }
                if (already) continue

                val dueMillis = startToday + minutes * 60_000L
                val taskId = UUID.randomUUID().toString()
                repository.insertTask(
                    Task(
                        id = taskId,
                        columnId = template.targetColumnId,
                        title = template.title.trim(),
                        description = "",
                        position = nextPos++,
                        categoryId = null,
                        dueDate = dueMillis,
                        createdAt = now,
                        reminderMinutesOfDay = minutes,
                        eisenhowerQuadrant = template.eisenhowerQuadrant,
                        showEisenhowerButtons = template.showEisenhowerButtons ||
                            template.eisenhowerQuadrant != null,
                        recurringTemplateId = template.id,
                        complexity = template.complexity
                    )
                )
                template.linkParentIds.distinct().forEach { parentId ->
                    if (parentId == taskId) return@forEach
                    if (TaskLinkGraph.wouldCreateCycle(workingLinks, parentId, taskId)) return@forEach
                    val link = TaskLink(parentId = parentId, childId = taskId, createdAt = now)
                    repository.insertTaskLink(link)
                    workingLinks += link
                }
                template.linkChildIds.distinct().forEach { childId ->
                    if (childId == taskId) return@forEach
                    if (TaskLinkGraph.wouldCreateCycle(workingLinks, taskId, childId)) return@forEach
                    val link = TaskLink(parentId = taskId, childId = childId, createdAt = now)
                    repository.insertTaskLink(link)
                    workingLinks += link
                }
                created++
            }
        }
        return created
    }

    private fun instanceMinutes(task: Task): Int {
        task.reminderMinutesOfDay?.let { return it.coerceIn(0, 24 * 60 - 1) }
        val due = task.dueDate ?: return 12 * 60
        return Calendar.getInstance().apply { timeInMillis = due }.let {
            it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
        }
    }

    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000
        private const val SKIP_RETAIN_DAYS = 60

        fun occurrenceKey(dayStartMillis: Long, minutes: Int): String =
            "$dayStartMillis:${minutes.coerceIn(0, 24 * 60 - 1)}"

        /** Key for the day/time slot this instance occupies, or null if undated. */
        fun occurrenceKeyForTask(task: Task): String? {
            val due = task.dueDate ?: return null
            val dayStart = startOfDay(due)
            val minutes = task.reminderMinutesOfDay?.coerceIn(0, 24 * 60 - 1)
                ?: Calendar.getInstance().apply { timeInMillis = due }.let {
                    it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
                }
            return occurrenceKey(dayStart, minutes)
        }

        fun pruneSkippedKeys(keys: List<String>, now: Long = System.currentTimeMillis()): List<String> {
            val cutoff = startOfDay(now) - SKIP_RETAIN_DAYS * DAY_MS
            return keys.filter { key ->
                val dayPart = key.substringBefore(':').toLongOrNull() ?: return@filter false
                dayPart >= cutoff
            }.distinct()
        }

        fun withSkippedOccurrence(template: RecurringTemplate, key: String): RecurringTemplate {
            val pruned = pruneSkippedKeys(template.skippedOccurrenceKeys + key)
            return template.copy(skippedOccurrenceKeys = pruned)
        }

        fun withoutSkippedOccurrence(template: RecurringTemplate, key: String): RecurringTemplate =
            template.copy(skippedOccurrenceKeys = template.skippedOccurrenceKeys.filterNot { it == key })

        fun startOfDay(now: Long): Long =
            Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

        /** 1=Mon … 7=Sun */
        fun isoDayOfWeek(now: Long): Int {
            val cal = Calendar.getInstance().apply { timeInMillis = now }
            return when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                else -> 7
            }
        }
    }
}
