package com.example.kaizenkanban.domain.commands

import com.example.kaizenkanban.domain.model.RecurringTemplate
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.domain.model.TaskLink
import com.example.kaizenkanban.domain.model.TaskLinkGraph
import com.example.kaizenkanban.domain.model.TaskWorkflow
import com.example.kaizenkanban.domain.repository.KanbanRepository
import java.util.UUID

/** Applies a validated [KairosPlan] to the repository (create-only). */
class KairosCommandApplier(
    private val repository: KanbanRepository
) {
    suspend fun apply(
        plan: KairosPlan,
        projectId: String,
        existingTasks: List<Task>,
        existingLinks: List<TaskLink> = emptyList()
    ): KairosApplyResult {
        if (!plan.canApply) {
            return KairosApplyResult(0, 0, 0, 0, plan.errors.ifEmpty { listOf("Nothing to apply") })
        }
        val idByLocal = mutableMapOf<String, String>()
        var goals = 0
        var tasks = 0
        var habits = 0
        var links = 0
        val errors = mutableListOf<String>()
        val createdTaskIds = mutableListOf<String>()
        val createdHabitIds = mutableListOf<String>()
        val createdLinkPairs = mutableListOf<Pair<String, String>>()
        val workingLinks = existingLinks.toMutableList()

        var nextPosByColumn = existingTasks
            .groupBy { it.columnId }
            .mapValues { (_, list) -> (list.maxOfOrNull { it.position } ?: -1) + 1 }
            .toMutableMap()

        fun nextPos(columnId: String): Int {
            val p = nextPosByColumn[columnId] ?: 0
            nextPosByColumn[columnId] = p + 1
            return p
        }

        suspend fun insertLink(parentId: String, childId: String): Boolean {
            if (parentId == childId) return false
            if (TaskLinkGraph.wouldCreateCycle(workingLinks, parentId, childId)) {
                errors += "Skipped link (cycle): $parentId → $childId"
                return false
            }
            if (workingLinks.any { it.parentId == parentId && it.childId == childId }) return true
            val link = TaskLink(parentId, childId, System.currentTimeMillis())
            repository.insertTaskLink(link)
            workingLinks += link
            createdLinkPairs += parentId to childId
            links++
            return true
        }

        for (action in plan.actions) {
            when (action) {
                is KairosPlannedAction.ReuseGoal -> {
                    idByLocal[action.localKey] = action.existingId
                }
                is KairosPlannedAction.CreateGoal -> {
                    val id = UUID.randomUUID().toString()
                    val task = Task(
                        id = id,
                        columnId = action.columnId,
                        title = action.title.trim(),
                        description = "",
                        position = nextPos(action.columnId),
                        categoryId = null,
                        dueDate = action.dueDateMillis,
                        createdAt = System.currentTimeMillis(),
                        eisenhowerQuadrant = action.eisenhowerQuadrant,
                        showEisenhowerButtons = action.eisenhowerQuadrant != null,
                        reminderMinutesOfDay = action.reminderMinutesOfDay,
                        complexity = action.complexity,
                        workflowStatus = TaskWorkflow.OPEN,
                        isGoal = true,
                        startMinutesOfDay = action.startMinutesOfDay,
                        deadlineAt = action.deadlineAtMillis,
                        timerMinutes = action.timerMinutes,
                        cardType = action.cardType,
                        cardPayload = action.cardPayload
                    )
                    repository.insertTask(task)
                    idByLocal[action.localKey] = id
                    createdTaskIds += id
                    goals++
                }
                is KairosPlannedAction.CreateTask -> {
                    val id = UUID.randomUUID().toString()
                    val task = Task(
                        id = id,
                        columnId = action.columnId,
                        title = action.title.trim(),
                        description = "",
                        position = nextPos(action.columnId),
                        categoryId = null,
                        dueDate = action.dueDateMillis,
                        createdAt = System.currentTimeMillis(),
                        eisenhowerQuadrant = action.eisenhowerQuadrant,
                        showEisenhowerButtons = action.eisenhowerQuadrant != null,
                        reminderMinutesOfDay = action.reminderMinutesOfDay,
                        complexity = action.complexity,
                        workflowStatus = TaskWorkflow.OPEN,
                        isGoal = action.isGoal,
                        startMinutesOfDay = action.startMinutesOfDay,
                        deadlineAt = action.deadlineAtMillis,
                        timerMinutes = action.timerMinutes,
                        cardType = action.cardType,
                        cardPayload = action.cardPayload
                    )
                    repository.insertTask(task)
                    idByLocal[action.localKey] = id
                    createdTaskIds += id
                    tasks++
                    val parentId = action.parentLocalKey?.let { idByLocal[it] }
                        ?: action.parentExistingId
                    if (parentId != null) insertLink(parentId, id)
                }
                is KairosPlannedAction.CreateHabit -> {
                    val id = UUID.randomUUID().toString()
                    val parentId = action.parentLocalKey?.let { idByLocal[it] }
                        ?: action.parentExistingId
                    val template = RecurringTemplate(
                        id = id,
                        projectId = projectId,
                        title = action.title.trim(),
                        rhythm = action.rhythm,
                        timesPerWeek = action.intervalDays,
                        weekdays = action.weekdays,
                        targetBoardId = action.targetBoardId,
                        targetColumnId = action.targetColumnId,
                        enabled = true,
                        reminderTimesOfDay = action.reminderTimesOfDay,
                        eisenhowerQuadrant = action.eisenhowerQuadrant,
                        showEisenhowerButtons = action.eisenhowerQuadrant != null,
                        linkParentIds = listOfNotNull(parentId),
                        linkChildIds = emptyList(),
                        complexity = action.complexity,
                        createdAt = System.currentTimeMillis(),
                        startsOnDayMillis = action.startsOnDayMillis,
                        deadlineOffsetDays = action.deadlineOffsetDays,
                        deadlineOffsetHours = action.deadlineOffsetHours,
                        timerMinutes = action.timerMinutes,
                        cardType = action.cardType,
                        cardPayload = action.cardPayload
                    )
                    repository.insertRecurringTemplate(template)
                    idByLocal[action.localKey] = id
                    createdHabitIds += id
                    habits++
                }
                is KairosPlannedAction.CreateLink -> {
                    val parentId = action.parentLocalKey?.let { idByLocal[it] }
                        ?: action.parentExistingId
                    val childId = action.childLocalKey?.let { idByLocal[it] }
                        ?: action.childExistingId
                    if (parentId != null && childId != null) insertLink(parentId, childId)
                    else errors += "Link skipped: unresolved parent/child"
                }
            }
        }

        return KairosApplyResult(
            createdGoals = goals,
            createdTasks = tasks,
            createdHabits = habits,
            createdLinks = links,
            errors = errors,
            createdTaskIds = createdTaskIds,
            createdHabitIds = createdHabitIds,
            createdLinksPairs = createdLinkPairs
        )
    }
}
