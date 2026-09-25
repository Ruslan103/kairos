package com.example.kaizenkanban.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskLinkGraphTest {

    @Test
    fun wouldCreateCycle_selfLink() {
        assertTrue(TaskLinkGraph.wouldCreateCycle(emptyList(), "a", "a"))
    }

    @Test
    fun wouldCreateCycle_detectsLoop() {
        val links = listOf(
            TaskLink("a", "b"),
            TaskLink("b", "c")
        )
        assertTrue(TaskLinkGraph.wouldCreateCycle(links, "c", "a"))
        // Shortcut edge a→c is fine (still a DAG).
        assertFalse(TaskLinkGraph.wouldCreateCycle(links, "a", "c"))
        assertFalse(TaskLinkGraph.wouldCreateCycle(links, "a", "d"))
    }

    @Test
    fun nodeWeight_sumsChildren() {
        val tasks = mapOf(
            "goal" to Task(
                id = "goal",
                columnId = "c",
                title = "G",
                description = "",
                position = 0,
                categoryId = null,
                dueDate = null,
                createdAt = 0L,
                complexity = 5
            ),
            "s1" to Task(
                id = "s1",
                columnId = "c",
                title = "S1",
                description = "",
                position = 0,
                categoryId = null,
                dueDate = null,
                createdAt = 0L,
                complexity = 2
            ),
            "s2" to Task(
                id = "s2",
                columnId = "c",
                title = "S2",
                description = "",
                position = 0,
                categoryId = null,
                dueDate = null,
                createdAt = 0L,
                complexity = 3
            )
        )
        val links = listOf(TaskLink("goal", "s1"), TaskLink("goal", "s2"))
        assertEquals(5, TaskLinkGraph.nodeWeight("goal", tasks, links))
        assertEquals(2, TaskLinkGraph.nodeWeight("s1", tasks, links))
    }

    @Test
    fun relatedIds_parentsAndChildren() {
        val links = listOf(TaskLink("p", "x"), TaskLink("x", "c"))
        assertEquals(setOf("p", "c"), TaskLinkGraph.relatedIds("x", links))
    }
}
