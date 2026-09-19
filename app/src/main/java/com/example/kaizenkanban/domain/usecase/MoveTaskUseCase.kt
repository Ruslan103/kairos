package com.example.kaizenkanban.domain.usecase

import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.domain.repository.KanbanRepository

class MoveTaskUseCase(private val repository: KanbanRepository) {
    /**
     * @param targetVisibleOrder display-ordered active siblings in the target hub (excluding [task]),
     *   used so DnD insert index matches what the user sees (due/importance sort or prior manual order).
     *   When null, falls back to position order among home-column tasks.
     */
    suspend operator fun invoke(
        task: Task,
        newColumnId: String,
        newPosition: Int,
        allTasks: List<Task>,
        targetVisibleOrder: List<Task>? = null
    ) {
        val oldColumnId = task.columnId
        val moved = task.copy(
            columnId = newColumnId,
            linkedColumnIds = task.linkedColumnIds.filter { it != newColumnId }
        )

        val targetOthers = allTasks.filter { it.columnId == newColumnId && it.id != task.id }
        val targetVisible = when {
            targetVisibleOrder != null -> {
                val orderIds = targetVisibleOrder.map { it.id }.toSet()
                val ordered = targetVisibleOrder
                    .filter { it.columnId == newColumnId && it.id != task.id && !it.isCompleted && !it.isHidden }
                    .toMutableList()
                // Home tasks not shown in the visual list (should be rare) keep relative position order at end.
                val missing = targetOthers
                    .filter { !it.isCompleted && !it.isHidden && it.id !in orderIds }
                    .sortedBy { it.position }
                (ordered + missing).toMutableList()
            }
            else -> targetOthers.filter { !it.isCompleted && !it.isHidden }.sortedBy { it.position }.toMutableList()
        }
        val targetHidden = targetOthers.filter { !it.isCompleted && it.isHidden }.sortedBy { it.position }.toMutableList()
        val targetCompleted = targetOthers.filter { it.isCompleted }.sortedBy { it.position }.toMutableList()

        when {
            moved.isCompleted -> targetCompleted.add(newPosition.coerceIn(0, targetCompleted.size), moved)
            moved.isHidden -> targetHidden.add(newPosition.coerceIn(0, targetHidden.size), moved)
            else -> targetVisible.add(newPosition.coerceIn(0, targetVisible.size), moved)
        }

        val updates = mutableListOf<Task>()
        updates += renumber(targetVisible + targetHidden + targetCompleted)

        if (oldColumnId != newColumnId) {
            val sourceRemaining = allTasks
                .filter { it.columnId == oldColumnId && it.id != task.id }
                .sortedBy { it.position }
            val sourceVisible = sourceRemaining.filter { !it.isCompleted && !it.isHidden }
            val sourceHidden = sourceRemaining.filter { !it.isCompleted && it.isHidden }
            val sourceCompleted = sourceRemaining.filter { it.isCompleted }
            updates += renumber(sourceVisible + sourceHidden + sourceCompleted)
        }

        repository.updateTasks(updates)
    }

    /**
     * Rewrite active home-task positions for [columnId] to match [orderedActiveIds] (hub sort / reset manual).
     * Tasks in this column but not in [orderedActiveIds] (e.g. home here, shown in another Matrix hub)
     * keep their relative slots — they are not shoved to the end.
     */
    suspend fun reorderHomeTasks(columnId: String, orderedActiveIds: List<String>, allTasks: List<Task>) {
        val home = allTasks.filter { it.columnId == columnId }
        val byId = home.associateBy { it.id }
        val orderedActive = orderedActiveIds.mapNotNull { byId[it] }.filter { !it.isCompleted && !it.isHidden }
        val orderedIds = orderedActive.map { it.id }.toSet()
        val allActive = home.filter { !it.isCompleted && !it.isHidden }.sortedBy { it.position }
        val queue = ArrayDeque(orderedActive)
        val newActive = ArrayList<Task>(allActive.size)
        for (t in allActive) {
            if (t.id in orderedIds) {
                if (queue.isNotEmpty()) newActive.add(queue.removeFirst())
            } else {
                newActive.add(t)
            }
        }
        while (queue.isNotEmpty()) {
            newActive.add(queue.removeFirst())
        }
        val hidden = home.filter { !it.isCompleted && it.isHidden }.sortedBy { it.position }
        val completed = home.filter { it.isCompleted }.sortedBy { it.position }
        repository.updateTasks(renumber(newActive + hidden + completed))
    }

    private fun renumber(ordered: List<Task>): List<Task> =
        ordered.mapIndexed { index, t -> t.copy(position = index) }
}
