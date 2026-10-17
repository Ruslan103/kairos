package com.example.kaizenkanban.domain.reminders

import com.example.kaizenkanban.domain.calendar.CalendarEventBuilder
import com.example.kaizenkanban.domain.model.KairosReminder
import com.example.kaizenkanban.domain.model.RecurringDefaults
import com.example.kaizenkanban.domain.model.RecurringTemplate
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.domain.model.RemindLeadCalculator
import com.example.kaizenkanban.domain.model.RemindLeadUnit
import com.example.kaizenkanban.domain.usecase.EnsureRecurringInstancesUseCase
import java.util.Calendar
import java.util.UUID

/** Concrete reminders for the next habit slots, fired [lead] before each reminder time. */
object RecurringReminderPlanner {
    const val HORIZON_DAYS = 60
    const val MAX_REMINDERS = 48

    fun plan(
        template: RecurringTemplate,
        now: Long = System.currentTimeMillis()
    ): List<KairosReminder> {
        val amount = template.remindLeadAmount ?: return emptyList()
        val unit = RemindLeadUnit.fromId(template.remindLeadUnit) ?: return emptyList()
        if (!template.enabled || amount <= 0 || template.title.isBlank()) return emptyList()
        val times = RecurringDefaults.effectiveReminderTimes(template.reminderTimesOfDay)
        val activeFrom = EnsureRecurringInstancesUseCase.effectiveStartsOnDay(
            template.startsOnDayMillis,
            template.createdAt
        )
        val skipped = template.skippedOccurrenceKeys.toSet()
        var day = maxOf(CalendarEventBuilder.startOfDay(now), activeFrom)
        val end = CalendarEventBuilder.startOfDay(now + HORIZON_DAYS * 86_400_000L)
        val out = mutableListOf<KairosReminder>()
        while (day <= end && out.size < MAX_REMINDERS) {
            if (CalendarEventBuilder.matchesRhythm(template, day, activeFrom)) {
                for (minutes in times) {
                    if (out.size >= MAX_REMINDERS) break
                    val key = EnsureRecurringInstancesUseCase.occurrenceKey(day, minutes)
                    if (key in skipped) continue
                    val anchor = RemindLeadCalculator.atClock(day, minutes)
                    val trigger = RemindLeadCalculator.subtract(anchor, amount, unit)
                    if (trigger <= now) continue
                    out += KairosReminder(
                        id = idFor(template.id, day, minutes),
                        projectId = template.projectId,
                        text = template.title,
                        triggerAtMillis = trigger,
                        recurringTemplateId = template.id,
                        leadAmount = amount,
                        leadUnit = unit.id,
                        createdAt = now
                    )
                }
            }
            day = nextDay(day)
        }
        return out
    }

    private fun nextDay(dayStart: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = dayStart
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    fun idFor(templateId: String, dayStart: Long, minutes: Int): String =
        UUID.nameUUIDFromBytes("kairos-rem:$templateId:$dayStart:$minutes".toByteArray()).toString()

    /** Lead reminder for the habit slot this instance was spawned from. */
    fun idForTask(task: Task): String? {
        val templateId = task.recurringTemplateId?.takeIf { it.isNotBlank() } ?: return null
        val due = task.dueDate ?: return null
        val minutes = task.reminderMinutesOfDay ?: task.startMinutesOfDay ?: return null
        return idFor(templateId, CalendarEventBuilder.startOfDay(due), minutes)
    }
}
