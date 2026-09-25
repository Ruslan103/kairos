package com.example.kaizenkanban.domain.model

object TaskLinkGraph {
    fun childrenOf(links: List<TaskLink>): Map<String, List<String>> =
        links.groupBy({ it.parentId }, { it.childId })

    fun parentsOf(links: List<TaskLink>): Map<String, List<String>> =
        links.groupBy({ it.childId }, { it.parentId })

    fun wouldCreateCycle(links: List<TaskLink>, parentId: String, childId: String): Boolean {
        if (parentId == childId) return true
        if (links.any { it.parentId == parentId && it.childId == childId }) return false
        val children = childrenOf(links)
        val stack = ArrayDeque<String>()
        stack.add(childId)
        val seen = HashSet<String>()
        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            if (node == parentId) return true
            if (!seen.add(node)) continue
            children[node].orEmpty().forEach { stack.add(it) }
        }
        return false
    }

    /** Leaf = (complexity ?: 1) × importance; otherwise sum of children weights. */
    fun leafWeight(task: Task): Int {
        val complexity = task.complexity?.coerceIn(1, 5) ?: 1
        return (complexity * Eisenhower.importanceCents(task.eisenhowerQuadrant) / 100f)
            .toInt()
            .coerceAtLeast(1)
    }

    fun nodeWeight(
        taskId: String,
        tasksById: Map<String, Task>,
        links: List<TaskLink>,
        memo: MutableMap<String, Int> = mutableMapOf()
    ): Int {
        memo[taskId]?.let { return it }
        val kids = childrenOf(links)[taskId].orEmpty()
        val weight = if (kids.isEmpty()) {
            tasksById[taskId]?.let { leafWeight(it) } ?: 1
        } else {
            kids.sumOf { nodeWeight(it, tasksById, links, memo) }
        }
        memo[taskId] = weight
        return weight
    }

    fun relatedIds(taskId: String, links: List<TaskLink>): Set<String> {
        val parents = parentsOf(links)[taskId].orEmpty()
        val children = childrenOf(links)[taskId].orEmpty()
        return (parents + children).toSet()
    }
}
