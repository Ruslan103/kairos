package com.example.kaizenkanban.domain.calendar

import com.example.kaizenkanban.domain.model.RecurringRhythm
import com.example.kaizenkanban.domain.model.RecurringTemplate
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.domain.usecase.EnsureRecurringInstancesUseCase
import java.util.Calendar

private const val DAY_MS = 86_400_000L

/**
 * One calendar row: a real dated task, or a future projected habit occurrence
 * (not yet spawned on the hub).
 */
/** Days of a selected task, plus deadline days when the deadline is its own date. */
data class CalendarHighlight(
    val title: String,
    val occurrenceDays: Set<Long>,
    val deadlineDays: Set<Long>
)

data class CalendarEvent(
    val id: String,
    val title: String,
    val dayStartMillis: Long,
    val sortMinutes: Int,
    val task: Task? = null,
    val templateId: String? = null,
    val isProjected: Boolean = false,
    val isNote: Boolean = false,
    /** Start clocks for a projected habit (one card can carry several slots). */
    val startMinutes: List<Int> = emptyList()
) {
    val isCompleted: Boolean get() = task?.isCompleted == true
    val isArchived: Boolean get() = task?.isBoardArchived == true
}

object CalendarEventBuilder {

    /**
     * Dated tasks in [projectTaskIds] plus projected future habit slots in [rangeStart]..[rangeEnd]
     * (inclusive day starts). Open hidden tasks are omitted. Completed, not-done, and
     * board-archived tasks stay. A deleted habit instance (skipped before a status) is not projected.
     * Tasks without a due date never appear.
     */
    fun build(
        tasks: List<Task>,
        projectTaskIds: Set<String>,
        templates: List<RecurringTemplate>,
        rangeStart: Long,
        rangeEnd: Long,
        now: Long = System.currentTimeMillis()
    ): List<CalendarEvent> {
        val start = startOfDay(rangeStart)
        val end = startOfDay(rangeEnd)
        val inProject = tasks.asSequence().filter { it.id in projectTaskIds }
        val real = inProject
            .filter { it.dueDate != null }
            .filter { task ->
                task.isCompleted || task.isNotDone || task.isBoardArchived || !task.isHidden
            }
            .mapNotNull { task ->
                val due = task.dueDate ?: return@mapNotNull null
                val day = startOfDay(due)
                CalendarEvent(
                    id = task.id,
                    title = task.title,
                    dayStartMillis = day,
                    sortMinutes = EnsureRecurringInstancesUseCase.instanceMinutes(task),
                    task = task,
                    templateId = task.recurringTemplateId,
                    isProjected = false
                )
            }
            .toList()

        val coveredKeys = inProject
            .filter { it.dueDate != null && !it.recurringTemplateId.isNullOrBlank() }
            .map { task ->
                val due = task.dueDate ?: return@map ""
                "${task.recurringTemplateId}:${startOfDay(due)}"
            }
            .filter { it.isNotBlank() }
            .toHashSet()

        val today = startOfDay(now)
        val projected = mutableListOf<CalendarEvent>()
        for (template in templates) {
            if (!template.enabled || template.title.isBlank()) continue
            val times = template.reminderTimesOfDay
                .map { it.coerceIn(0, 24 * 60 - 1) }
                .distinct()
                .sorted()
                .ifEmpty { listOf(9 * 60) }
            val skipped = template.skippedOccurrenceKeys.toSet()
            val activeFrom = EnsureRecurringInstancesUseCase.effectiveStartsOnDay(
                template.startsOnDayMillis,
                template.createdAt
            )
            var day = maxOf(start, activeFrom, today) // project today+future only (past covered by real tasks)
            while (day <= end) {
                if (matchesRhythm(template, day, activeFrom)) {
                    val key = "${template.id}:$day"
                    val openTimes = times.filter { minutes ->
                        EnsureRecurringInstancesUseCase.occurrenceKey(day, minutes) !in skipped
                    }
                    val covered = key in coveredKeys || real.any {
                        it.templateId == template.id && it.dayStartMillis == day
                    }
                    if (!covered && openTimes.isNotEmpty()) {
                        val minutes = openTimes.first()
                        projected += CalendarEvent(
                            id = "proj:${template.id}:$day:$minutes",
                            title = template.title,
                            dayStartMillis = day,
                            sortMinutes = minutes,
                            task = null,
                            templateId = template.id,
                            isProjected = true,
                            startMinutes = openTimes
                        )
                        coveredKeys += key
                    }
                }
                day += DAY_MS
            }
        }

        return (real + projected).sortedWith(
            compareBy({ it.dayStartMillis }, { it.sortMinutes }, { it.title.lowercase() })
        )
    }

    fun matchesRhythm(template: RecurringTemplate, dayStart: Long, activeFrom: Long): Boolean {
        if (dayStart < activeFrom) return false
        return when (template.rhythm) {
            RecurringRhythm.DAILY -> true
            RecurringRhythm.WEEKDAYS -> isoDayOfWeek(dayStart) in template.weekdays
            RecurringRhythm.EVERY_N_DAYS -> {
                val n = RecurringRhythm.intervalDays(template.timesPerWeek)
                val days = ((dayStart - activeFrom) / DAY_MS).coerceAtLeast(0L)
                days % n == 0L
            }
            else -> false
        }
    }

    fun startOfDay(millis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    fun isoDayOfWeek(millis: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
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

    fun isWeekend(dayStart: Long): Boolean {
        val dow = isoDayOfWeek(dayStart)
        return dow == 6 || dow == 7
    }

    fun eventMatchesFocus(event: CalendarEvent, focusKey: String?): Boolean {
        val key = focusKey ?: return false
        return when {
            key.startsWith("tpl:") -> event.templateId == key.removePrefix("tpl:")
            event.isNote -> event.id == key
            else -> event.task?.id == key || event.id == key
        }
    }

    /**
     * Occurrence days in [events] plus deadline days of the selected task
     * (or of every instance, when a habit is selected).
     */
    fun highlight(
        focusKey: String?,
        events: List<CalendarEvent>,
        tasks: List<Task>
    ): CalendarHighlight? {
        val key = focusKey?.takeIf { it.isNotBlank() } ?: return null
        if (key.startsWith("tpl:")) {
            val tplId = key.removePrefix("tpl:")
            val related = tasks.filter { it.recurringTemplateId == tplId }
            val days = events.filter { it.templateId == tplId }.map { it.dayStartMillis }.toSet()
            val title = events.find { it.templateId == tplId }?.title
                ?: related.firstOrNull()?.title
                ?: return null
            return CalendarHighlight(title, days, deadlineDays(related))
        }
        val task = tasks.find { it.id == key }
        if (task != null) {
            val tpl = task.recurringTemplateId
            val days = if (!tpl.isNullOrBlank()) {
                events.filter { it.templateId == tpl || it.id == key }.map { it.dayStartMillis }.toSet()
            } else {
                task.dueDate?.let { setOf(startOfDay(it)) }.orEmpty()
            }
            val related = if (!tpl.isNullOrBlank()) {
                tasks.filter { it.recurringTemplateId == tpl }
            } else {
                listOf(task)
            }
            return CalendarHighlight(task.title, days, deadlineDays(related))
        }
        val note = events.find { it.id == key } ?: return null
        return CalendarHighlight(note.title, setOf(note.dayStartMillis), emptySet())
    }

    private fun deadlineDays(tasks: List<Task>): Set<Long> =
        tasks.mapNotNull { task -> task.deadlineAt?.let(::startOfDay) }.toSet()
}
