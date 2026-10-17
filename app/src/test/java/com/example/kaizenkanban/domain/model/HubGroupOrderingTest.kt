package com.example.kaizenkanban.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HubGroupOrderingTest {

    private fun task(
        id: String,
        position: Int,
        groupId: String? = null,
        due: Long? = null
    ) = Task(
        id = id,
        columnId = "hub",
        title = id,
        description = "",
        position = position,
        categoryId = null,
        dueDate = due,
        createdAt = 0L,
        hubGroupId = groupId
    )

    @Test
    fun packGroupAtTop_putsSelectionFirst() {
        val ordered = listOf(
            task("a", 0),
            task("b", 1),
            task("c", 2),
            task("d", 3),
            task("e", 4)
        )
        val packed = HubGroupOrdering.packGroupAtTop(ordered, setOf("c", "e"))
        assertEquals(listOf("c", "e", "a", "b", "d"), packed.map { it.id })
    }

    @Test
    fun sortPreservingGroups_groupsFirstThenLoose() {
        val ordered = listOf(
            task("u1", 0, due = 30),
            task("g2", 1, groupId = "g", due = 20),
            task("g1", 2, groupId = "g", due = 10),
            task("u2", 3, due = 5)
        )
        val cmp = compareBy<Task> { it.dueDate ?: Long.MAX_VALUE }
        val sorted = HubGroupOrdering.sortPreservingGroups(ordered, cmp)
        // Group block first (sorted inside), then ungrouped by due: u2(5), u1(30)
        assertEquals(listOf("g1", "g2", "u2", "u1"), sorted.map { it.id })
    }

    @Test
    fun groupsFirst_putsEveryGroupBeforeLooseTasks() {
        val ordered = listOf(
            task("u1", 0),
            task("a1", 1, groupId = "a"),
            task("u2", 2),
            task("b1", 3, groupId = "b"),
            task("a2", 4, groupId = "a"),
            task("b2", 5, groupId = "b")
        )
        val orderedIds = HubGroupOrdering.groupsFirst(ordered) { it.hubGroupId }.map { it.id }
        assertEquals(listOf("a1", "a2", "b1", "b2", "u1", "u2"), orderedIds)
    }

    @Test
    fun resolveDropGroupId_joinsOnlyInsideSameGroup() {
        val g = task("a", 0, groupId = "g")
        val g2 = task("b", 1, groupId = "g")
        val other = task("d", 2, groupId = "other")
        val u = task("c", 3)
        assertEquals("g", HubGroupOrdering.resolveDropGroupId(g, g2))
        assertNull(HubGroupOrdering.resolveDropGroupId(g2, u))
        assertNull(HubGroupOrdering.resolveDropGroupId(null, g))
        assertNull(HubGroupOrdering.resolveDropGroupId(g, other))
        assertNull(HubGroupOrdering.resolveDropGroupId(u, other))
        assertNull(HubGroupOrdering.resolveDropGroupId(u, null))
        assertNull(HubGroupOrdering.resolveDropGroupId(null, null))
        assertEquals(
            "g",
            HubGroupOrdering.resolveDropGroupId(g2, u, draggedGroupId = "g", pointerOverSource = true)
        )
        assertNull(
            HubGroupOrdering.resolveDropGroupId(null, null, draggedGroupId = null, pointerOverSource = true)
        )
    }
}
