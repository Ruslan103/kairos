package com.example.kaizenkanban.data.mapper

import com.example.kaizenkanban.data.local.*
import com.example.kaizenkanban.domain.model.*

fun ProjectEntity.toDomain() = Project(id, name, position)
fun Project.toEntity() = ProjectEntity(id, name, position)

fun BoardEntity.toDomain() = Board(id, projectId, name, isDefault, isArchived)
fun Board.toEntity() = BoardEntity(id, projectId, name, isDefault, isArchived)

fun CategoryEntity.toDomain() = Category(id, name, color)
fun Category.toEntity() = CategoryEntity(id, name, color)

fun ColumnEntity.toDomain() = Column(id, boardId, title, position)
fun Column.toEntity() = ColumnEntity(id, boardId, title, position)

fun String.toLinkedColumnIds(): List<String> =
    split(',').map { it.trim() }.filter { it.isNotEmpty() }

fun List<String>.toLinkedColumnIdsStorage(): String = joinToString(",")

fun TaskEntity.toDomain() = Task(
    id = id,
    columnId = columnId,
    title = title,
    description = description,
    position = position,
    categoryId = categoryId,
    dueDate = dueDate,
    isCompleted = isCompleted,
    completedAt = completedAt,
    createdAt = createdAt,
    eisenhowerQuadrant = eisenhowerQuadrant,
    showEisenhowerButtons = showEisenhowerButtons,
    isHidden = isHidden,
    linkedColumnIds = linkedColumnIds.toLinkedColumnIds(),
    repeatRule = repeatRule,
    reminderMinutesOfDay = reminderMinutesOfDay,
    recurringTemplateId = recurringTemplateId,
    complexity = complexity?.coerceIn(1, 5),
    estimatedMinutes = estimatedMinutes?.takeIf { it > 0 },
    completionQuality = completionQuality?.coerceIn(1, 5),
    workflowStatus = TaskWorkflow.normalize(workflowStatus, isCompleted),
    isBoardArchived = isBoardArchived,
    statsExcluded = statsExcluded,
    isGoal = isGoal,
    goalStatsEpochMillis = goalStatsEpochMillis
)

fun Task.toEntity() = TaskEntity(
    id = id,
    columnId = columnId,
    title = title,
    description = description,
    position = position,
    categoryId = categoryId,
    dueDate = dueDate,
    isCompleted = isCompleted,
    completedAt = completedAt,
    createdAt = createdAt,
    eisenhowerQuadrant = eisenhowerQuadrant,
    showEisenhowerButtons = showEisenhowerButtons,
    isHidden = isHidden,
    linkedColumnIds = linkedColumnIds.toLinkedColumnIdsStorage(),
    repeatRule = repeatRule,
    reminderMinutesOfDay = reminderMinutesOfDay,
    recurringTemplateId = recurringTemplateId,
    complexity = complexity?.coerceIn(1, 5),
    estimatedMinutes = estimatedMinutes?.takeIf { it > 0 },
    completionQuality = completionQuality?.coerceIn(1, 5),
    workflowStatus = TaskWorkflow.normalize(workflowStatus, isCompleted),
    isBoardArchived = isBoardArchived,
    statsExcluded = statsExcluded,
    isGoal = isGoal,
    goalStatsEpochMillis = goalStatsEpochMillis
)

fun StatsJournalEntity.toDomain() = StatsJournalEntry(
    id = id,
    projectId = projectId,
    sourceTaskId = sourceTaskId,
    title = title,
    kind = kind,
    eventAt = eventAt,
    complexity = complexity,
    completionQuality = completionQuality,
    eisenhowerQuadrant = eisenhowerQuadrant,
    weight = weight,
    leafScore = leafScore,
    towardGoal = towardGoal,
    relatedGoalIds = relatedGoalIds.split(',').map { it.trim() }.filter { it.isNotEmpty() },
    recurringTemplateId = recurringTemplateId,
    createdAt = createdAt
)

fun StatsJournalEntry.toEntity() = StatsJournalEntity(
    id = id,
    projectId = projectId,
    sourceTaskId = sourceTaskId,
    title = title,
    kind = kind,
    eventAt = eventAt,
    complexity = complexity,
    completionQuality = completionQuality,
    eisenhowerQuadrant = eisenhowerQuadrant,
    weight = weight,
    leafScore = leafScore,
    towardGoal = towardGoal,
    relatedGoalIds = relatedGoalIds.joinToString(","),
    recurringTemplateId = recurringTemplateId,
    createdAt = createdAt
)

fun String.toWeekdaySet(): Set<Int> =
    split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..7 }.toSet()

fun Set<Int>.toWeekdaysStorage(): String =
    filter { it in 1..7 }.sorted().joinToString(",")

fun String.toMinutesList(): List<Int> =
    split(',')
        .mapNotNull { it.trim().toIntOrNull() }
        .map { it.coerceIn(0, 24 * 60 - 1) }
        .distinct()
        .sorted()

fun List<Int>.toMinutesStorage(): String =
    map { it.coerceIn(0, 24 * 60 - 1) }.distinct().sorted().joinToString(",")
        .ifBlank { "540" }

fun RecurringTemplateEntity.toDomain(): RecurringTemplate {
    val storedWeekdays = weekdays.toWeekdaySet()
    val (normalizedRhythm, normalizedWeekdays, intervalDays) = when (rhythm) {
        RecurringRhythm.TIMES_PER_WEEK ->
            Triple(
                RecurringRhythm.WEEKDAYS,
                storedWeekdays.ifEmpty { weekdaysForLegacyTimesPerWeek(timesPerWeek ?: 3) },
                null
            )
        RecurringRhythm.WEEKDAYS ->
            Triple(
                RecurringRhythm.WEEKDAYS,
                storedWeekdays.ifEmpty { setOf(1, 3, 5) },
                null
            )
        RecurringRhythm.EVERY_N_DAYS ->
            Triple(
                RecurringRhythm.EVERY_N_DAYS,
                emptySet(),
                (timesPerWeek ?: 2).coerceIn(2, 30)
            )
        else ->
            Triple(RecurringRhythm.DAILY, emptySet(), null)
    }
    return RecurringTemplate(
        id = id,
        projectId = projectId,
        title = title,
        rhythm = normalizedRhythm,
        timesPerWeek = intervalDays,
        weekdays = normalizedWeekdays,
        targetBoardId = targetBoardId,
        targetColumnId = targetColumnId,
        enabled = enabled,
        reminderTimesOfDay = reminderTimesOfDay.toMinutesList().ifEmpty { listOf(9 * 60) },
        eisenhowerQuadrant = eisenhowerQuadrant,
        showEisenhowerButtons = showEisenhowerButtons || eisenhowerQuadrant != null,
        linkParentIds = linkParentIds.toIdList(),
        linkChildIds = linkChildIds.toIdList(),
        complexity = complexity?.coerceIn(1, 5),
        createdAt = createdAt
    )
}

fun RecurringTemplate.toEntity() = RecurringTemplateEntity(
    id = id,
    projectId = projectId,
    title = title,
    rhythm = when (rhythm) {
        RecurringRhythm.WEEKDAYS -> RecurringRhythm.WEEKDAYS
        RecurringRhythm.EVERY_N_DAYS -> RecurringRhythm.EVERY_N_DAYS
        else -> RecurringRhythm.DAILY
    },
    timesPerWeek = if (rhythm == RecurringRhythm.EVERY_N_DAYS) {
        (timesPerWeek ?: 2).coerceIn(2, 30)
    } else {
        null
    },
    weekdays = if (rhythm == RecurringRhythm.WEEKDAYS) weekdays.toWeekdaysStorage() else "",
    targetBoardId = targetBoardId,
    targetColumnId = targetColumnId,
    enabled = enabled,
    reminderTimesOfDay = reminderTimesOfDay.toMinutesStorage(),
    eisenhowerQuadrant = if (showEisenhowerButtons) eisenhowerQuadrant else null,
    showEisenhowerButtons = showEisenhowerButtons,
    linkParentIds = linkParentIds.toIdStorage(),
    linkChildIds = linkChildIds.toIdStorage(),
    complexity = complexity?.coerceIn(1, 5),
    createdAt = createdAt
)

private fun String.toIdList(): List<String> =
    split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()

private fun List<String>.toIdStorage(): String =
    distinct().filter { it.isNotBlank() }.joinToString(",")

/** Spread N days evenly across Mon–Sun (legacy «N× / week»). */
fun weekdaysForLegacyTimesPerWeek(n: Int): Set<Int> {
    val count = n.coerceIn(1, 7)
    if (count == 7) return (1..7).toSet()
    return (0 until count).map { i -> 1 + (i * 7) / count }.toSet()
}

fun CommentEntity.toDomain() = Comment(id, taskId, text, createdAt)
fun Comment.toEntity() = CommentEntity(id, taskId, text, createdAt)

fun ColumnCommentEntity.toDomain() = ColumnComment(id, columnId, text, createdAt)
fun ColumnComment.toEntity() = ColumnCommentEntity(id, columnId, text, createdAt)

fun ContactEntity.toDomain() = Contact(id, projectId, name, phone, email, role, position)
fun Contact.toEntity() = ContactEntity(id, projectId, name, phone, email, role, position)

fun TaskLinkEntity.toDomain() = TaskLink(parentId, childId, createdAt)
fun TaskLink.toEntity() = TaskLinkEntity(parentId, childId, createdAt)