package com.example.kaizenkanban.domain.model

/**
 * Ordering helpers for hub task groups ([Task.hubGroupId]).
 */
object HubGroupOrdering {

    /**
     * Pack [selectedIds] as one contiguous block at the top of the hub;
     * non-selected tasks keep relative order below.
     */
    fun packGroupAtTop(
        orderedHome: List<Task>,
        selectedIds: Set<String>
    ): List<Task> {
        if (selectedIds.isEmpty()) return orderedHome
        val selectedOrdered = orderedHome.filter { it.id in selectedIds }
        if (selectedOrdered.isEmpty()) return orderedHome
        val rest = orderedHome.filter { it.id !in selectedIds }
        return selectedOrdered + rest
    }

    /**
     * Pull [selectedIds] into one contiguous block at the first selected index;
     * non-selected tasks keep relative order outside the block.
     */
    @Deprecated("Use packGroupAtTop — hub groups live at the top")
    fun packSelectedContiguous(
        orderedHome: List<Task>,
        selectedIds: Set<String>
    ): List<Task> {
        if (selectedIds.size < 2) return orderedHome
        val selectedOrdered = orderedHome.filter { it.id in selectedIds }
        if (selectedOrdered.size < 2) return orderedHome
        val firstIdx = orderedHome.indexOfFirst { it.id in selectedIds }
        if (firstIdx < 0) return orderedHome
        val before = orderedHome.take(firstIdx).filter { it.id !in selectedIds }
        val after = orderedHome.drop(firstIdx).filter { it.id !in selectedIds }
        return before + selectedOrdered + after
    }

    /**
     * Real groups (two or more members) first, in the order they appear,
     * then everything else in its original order.
     */
    fun <T> groupsFirst(ordered: List<T>, groupIdOf: (T) -> String?): List<T> {
        if (ordered.size < 2) return ordered
        val counts = HashMap<String, Int>()
        for (item in ordered) {
            val gid = groupIdOf(item)?.takeIf { it.isNotBlank() } ?: continue
            counts[gid] = (counts[gid] ?: 0) + 1
        }
        val real = counts.filterValues { it >= 2 }.keys
        if (real.isEmpty()) return ordered
        val members = LinkedHashMap<String, MutableList<T>>()
        val loose = ArrayList<T>(ordered.size)
        for (item in ordered) {
            val gid = groupIdOf(item)?.takeIf { it.isNotBlank() }
            if (gid != null && gid in real) {
                members.getOrPut(gid) { mutableListOf() }.add(item)
            } else {
                loose.add(item)
            }
        }
        val grouped = ArrayList<T>(ordered.size - loose.size)
        val seen = HashSet<String>()
        for (item in ordered) {
            val gid = groupIdOf(item)?.takeIf { it.isNotBlank() } ?: continue
            if (gid in real && seen.add(gid)) {
                grouped.addAll(members[gid].orEmpty())
            }
        }
        return grouped + loose
    }

    /** True when [block] is a multi-member hub group. */
    fun isGroupBlock(block: List<Task>): Boolean =
        block.size >= 2 && !block.first().hubGroupId.isNullOrBlank()

    /**
     * Sort preserving group blocks: sort inside each group, then place all real
     * groups first (ordered by [cmp]), then ungrouped tasks (also by [cmp]).
     */
    fun sortPreservingGroups(
        orderedHome: List<Task>,
        cmp: Comparator<Task>
    ): List<Task> {
        if (orderedHome.isEmpty()) return orderedHome
        val blocks = mutableListOf<List<Task>>()
        var i = 0
        while (i < orderedHome.size) {
            val t = orderedHome[i]
            val gid = t.hubGroupId
            if (gid != null) {
                val end = (i + 1 until orderedHome.size)
                    .firstOrNull { orderedHome[it].hubGroupId != gid }
                    ?: orderedHome.size
                val block = orderedHome.subList(i, end).sortedWith(cmp)
                blocks += block
                i = end
            } else {
                blocks += listOf(t)
                i++
            }
        }
        val groupBlocks = blocks.filter { isGroupBlock(it) }
            .sortedWith { a, b -> cmp.compare(a.minWith(cmp), b.minWith(cmp)) }
        val loose = blocks.filterNot { isGroupBlock(it) }
            .sortedWith { a, b -> cmp.compare(a.minWith(cmp), b.minWith(cmp)) }
        return (groupBlocks + loose).flatten()
    }

    /**
     * Hub group for a dropped task.
     *
     * The insert slot is inside a plate only when both neighbors already share that
     * group. A slot above a group, below it, or in the gap between two groups stays
     * ungrouped — otherwise pulling a card out of its plate drops it into the neighbor.
     *
     * [pointerOverSource] keeps the card's current group when the finger never left
     * the source card (an edge card has only one grouped neighbor, which is not
     * "inside" the plate).
     */
    fun resolveDropGroupId(
        before: Task?,
        after: Task?,
        draggedGroupId: String? = null,
        pointerOverSource: Boolean = false
    ): String? {
        if (pointerOverSource) return draggedGroupId?.takeIf { it.isNotBlank() }
        val beforeG = before?.hubGroupId?.takeIf { it.isNotBlank() }
        val afterG = after?.hubGroupId?.takeIf { it.isNotBlank() }
        return if (beforeG != null && beforeG == afterG) beforeG else null
    }
}
