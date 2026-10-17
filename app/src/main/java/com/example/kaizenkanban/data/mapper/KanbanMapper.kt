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
    goalStatsEpochMillis = goalStatsEpochMillis,
    hubGroupId = hubGroupId,
    startMinutesOfDay = startMinutesOfDay?.coerceIn(0, 23 * 60 + 59),
    deadlineAt = deadlineAt,
    timerMinutes = timerMinutes?.takeIf { it > 0 },
    cardType = TaskCardKind.normalize(cardType),
    cardPayload = cardPayload
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
    goalStatsEpochMillis = goalStatsEpochMillis,
    hubGroupId = hubGroupId,
    startMinutesOfDay = startMinutesOfDay?.coerceIn(0, 23 * 60 + 59),
    deadlineAt = deadlineAt,
    timerMinutes = timerMinutes?.takeIf { it > 0 },
    cardType = TaskCardKind.normalize(cardType),
    cardPayload = cardPayload
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
    createdAt = createdAt,
    parentIds = parentIds.split(',').map { it.trim() }.filter { it.isNotEmpty() },
    childIds = childIds.split(',').map { it.trim() }.filter { it.isNotEmpty() },
    isGoal = isGoal,
    goalStatsEpochMillis = goalStatsEpochMillis,
    dueDate = dueDate,
    deadlineAt = deadlineAt,
    taskCreatedAt = taskCreatedAt,
    replacesTask = replacesTask
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
    createdAt = createdAt,
    parentIds = parentIds.joinToString(","),
    childIds = childIds.joinToString(","),
    isGoal = isGoal,
    goalStatsEpochMillis = goalStatsEpochMillis,
    dueDate = dueDate,
    deadlineAt = deadlineAt,
    taskCreatedAt = taskCreatedAt,
    replacesTask = replacesTask
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
        .ifBlank { TaskReminderDefaults.DEFAULT_MINUTES.toString() }

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
                RecurringRhythm.intervalDays(timesPerWeek)
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
        reminderTimesOfDay = RecurringDefaults.effectiveReminderTimes(
            stored = reminderTimesOfDay.toMinutesList(),
            reminderDefault = TaskReminderDefaults.DEFAULT_MINUTES
        ),
        eisenhowerQuadrant = eisenhowerQuadrant,
        showEisenhowerButtons = showEisenhowerButtons || eisenhowerQuadrant != null,
        linkParentIds = linkParentIds.toIdList(),
        linkChildIds = linkChildIds.toIdList(),
        complexity = complexity?.coerceIn(1, 5),
        skippedOccurrenceKeys = skippedOccurrenceKeys.toIdList(),
        startsOnDayMillis = startsOnDayMillis,
        deadlineOffsetDays = deadlineOffsetDays?.takeIf { (it > 0) || (deadlineOffsetHours ?: 0) > 0 },
        deadlineOffsetHours = deadlineOffsetHours?.takeIf { (it > 0) || (deadlineOffsetDays ?: 0) > 0 },
        createdAt = createdAt,
        remindLeadAmount = remindLeadAmount?.takeIf { it > 0 },
        remindLeadUnit = remindLeadUnit,
        timerMinutes = timerMinutes?.takeIf { it > 0 },
        hubGroupId = hubGroupId?.takeIf { it.isNotBlank() },
        cardType = TaskCardKind.normalize(cardType),
        cardPayload = cardPayload
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
        RecurringRhythm.intervalDays(timesPerWeek)
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
    skippedOccurrenceKeys = skippedOccurrenceKeys.toIdStorage(),
    startsOnDayMillis = startsOnDayMillis,
    deadlineOffsetDays = deadlineOffsetDays?.coerceAtLeast(0),
    deadlineOffsetHours = deadlineOffsetHours?.coerceAtLeast(0),
    createdAt = createdAt,
    remindLeadAmount = remindLeadAmount?.takeIf { it > 0 },
    remindLeadUnit = remindLeadUnit,
    timerMinutes = timerMinutes?.takeIf { it > 0 },
    hubGroupId = hubGroupId?.takeIf { it.isNotBlank() },
    cardType = TaskCardKind.normalize(cardType),
    cardPayload = cardPayload
)

fun KairosReminderEntity.toDomain() = com.example.kaizenkanban.domain.model.KairosReminder(
    id = id,
    projectId = projectId,
    text = text,
    triggerAtMillis = triggerAtMillis,
    taskId = taskId,
    recurringTemplateId = recurringTemplateId,
    leadAmount = leadAmount,
    leadUnit = leadUnit,
    createdAt = createdAt
)

fun com.example.kaizenkanban.domain.model.KairosReminder.toEntity() = KairosReminderEntity(
    id = id,
    projectId = projectId,
    text = text,
    triggerAtMillis = triggerAtMillis,
    taskId = taskId,
    recurringTemplateId = recurringTemplateId,
    leadAmount = leadAmount,
    leadUnit = leadUnit,
    createdAt = createdAt
)

fun ProjectNoteEntity.toDomain() = ProjectNote(
    id = id,
    projectId = projectId,
    body = body,
    createdAt = createdAt,
    updatedAt = updatedAt,
    position = position,
    pinned = pinned,
    linkedTaskId = linkedTaskId,
    reminderAtMillis = reminderAtMillis,
    title = title
)

fun ProjectNote.toEntity() = ProjectNoteEntity(
    id = id,
    projectId = projectId,
    body = body,
    createdAt = createdAt,
    updatedAt = updatedAt,
    position = position,
    pinned = pinned,
    linkedTaskId = linkedTaskId,
    reminderAtMillis = reminderAtMillis,
    title = title
)

fun NoteLineEntity.toDomain() = NoteLine(
    id = id,
    noteId = noteId,
    text = text,
    createdAt = createdAt,
    position = position
)

fun NoteLine.toEntity() = NoteLineEntity(
    id = id,
    noteId = noteId,
    text = text,
    createdAt = createdAt,
    position = position
)

fun NotePhotoEntity.toDomain() = NotePhoto(
    id = id,
    noteId = noteId,
    mediaUri = mediaUri,
    createdAt = createdAt,
    sortOrder = sortOrder,
    lineId = lineId
)

fun NotePhoto.toEntity() = NotePhotoEntity(
    id = id,
    noteId = noteId,
    mediaUri = mediaUri,
    createdAt = createdAt,
    sortOrder = sortOrder,
    lineId = lineId
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

fun ContactEntity.toDomain() = Contact(id, projectId, name, phone, email, role, position, social)
fun Contact.toEntity() = ContactEntity(id, projectId, name, phone, email, role, position, social)

fun TaskLinkEntity.toDomain() = TaskLink(parentId, childId, createdAt)
fun TaskLink.toEntity() = TaskLinkEntity(parentId, childId, createdAt)

fun TaskAttachmentEntity.toDomain() = TaskAttachment(
    id = id,
    taskId = taskId,
    relativePath = relativePath,
    mimeType = mimeType,
    createdAt = createdAt,
    sortOrder = sortOrder
)

fun TaskAttachment.toEntity() = TaskAttachmentEntity(
    id = id,
    taskId = taskId,
    relativePath = relativePath,
    mimeType = mimeType,
    createdAt = createdAt,
    sortOrder = sortOrder
)