package com.example.kaizenkanban.domain.commands

import com.example.kaizenkanban.domain.model.Board
import com.example.kaizenkanban.domain.model.Column
import com.example.kaizenkanban.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class KairosCommandParserTest {

    @Test
    fun parsesGoalTaskHabitBlock() {
        val raw = """
            Here is the plan:
            ```
            KAIROS v1
            goal: "Run 10k" [new]
              due: 2026-10-01
              remind: 08:00
            task: "Buy shoes"
              parent: goal
              due: 2026-09-28
              remind: 09:00
              impact: 2
              eisenhower: IU
            habit: "Easy run"
              parent: goal
              rhythm: weekdays
              days: 1,3,5
              remind: 07:00
              impact: 4
            ```
        """.trimIndent()
        val result = KairosCommandParser.parse(raw)
        assertTrue(result.headerOk)
        assertTrue(result.errors.isEmpty())
        assertEquals(3, result.commands.size)
        val goal = result.commands[0] as KairosCommand.Goal
        assertEquals("Run 10k", goal.title)
        assertTrue(goal.createNew)
        assertEquals("2026-10-01", goal.dueDateYmd)
        assertEquals(8 * 60, goal.remindMinutes)
        val task = result.commands[1] as KairosCommand.Task
        assertEquals("Buy shoes", task.title)
        assertEquals("goal", task.parentRef)
        assertEquals("Q1", task.eisenhower)
        assertEquals(2, task.impact)
        val habit = result.commands[2] as KairosCommand.Habit
        assertEquals(setOf(1, 3, 5), habit.weekdays)
        assertEquals(listOf(7 * 60), habit.remindMinutesList)
    }

    @Test
    fun rejectsMissingHeader() {
        val result = KairosCommandParser.parse("task: \"X\"")
        assertFalse(result.headerOk)
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun plannerCreatesGoalAndChildTask() {
        val parse = KairosCommandParser.parse(
            """
            KAIROS v1
            goal: "Health" [new]
            task: "Vitamins"
              parent: goal
              due: 2026-09-30
              impact: 3
              eisenhower: Q2
            """.trimIndent()
        )
        val plan = KairosCommandPlanner.plan(
            parse,
            KairosCommandPlanner.Context(
                projectId = "p",
                boards = listOf(Board("b", "p", "OKR", isDefault = true)),
                columns = listOf(Column("c", "b", "Do", 0)),
                tasks = emptyList(),
                links = emptyList(),
                timeZone = TimeZone.getTimeZone("UTC")
            )
        )
        assertTrue(plan.errors.joinToString(), plan.canApply)
        assertEquals(2, plan.actions.size)
        assertTrue(plan.actions[0] is KairosPlannedAction.CreateGoal)
        assertTrue(plan.actions[1] is KairosPlannedAction.CreateTask)
        val task = plan.actions[1] as KairosPlannedAction.CreateTask
        assertEquals("g0", task.parentLocalKey)
        assertEquals("Q2", task.eisenhowerQuadrant)
        assertEquals(3, task.complexity)
    }

    @Test
    fun plannerReusesExistingGoalByTitle() {
        val existing = Task(
            id = "goal1",
            columnId = "c",
            title = "Health",
            description = "",
            position = 0,
            categoryId = null,
            dueDate = null,
            createdAt = 0L,
            isGoal = true
        )
        val parse = KairosCommandParser.parse(
            """
            KAIROS v1
            goal: "Health"
            task: "Walk"
              parent: goal
            """.trimIndent()
        )
        val plan = KairosCommandPlanner.plan(
            parse,
            KairosCommandPlanner.Context(
                projectId = "p",
                boards = listOf(Board("b", "p", "OKR", isDefault = true)),
                columns = listOf(Column("c", "b", "Do", 0)),
                tasks = listOf(existing),
                links = emptyList()
            )
        )
        assertTrue(plan.canApply)
        assertTrue(plan.actions[0] is KairosPlannedAction.ReuseGoal)
        val reuse = plan.actions[0] as KairosPlannedAction.ReuseGoal
        assertEquals("goal1", reuse.existingId)
    }

    @Test
    fun parsesStartFinishTimerTodayAndRelativeRhythm() {
        val raw = """
            KAIROS v1
            task: "Focus block"
              due: today
              start: 14:30
              remind: 14:00
              finish: tomorrow
              timer: 25
              board: "Do"
            habit: "Walk"
              rhythm: every 3 days
              timer: 15
              deadline_hours: 2
        """.trimIndent()
        val result = KairosCommandParser.parse(raw)
        assertTrue(result.errors.joinToString(), result.errors.isEmpty())
        val task = result.commands[0] as KairosCommand.Task
        assertEquals("today", task.dueDateYmd)
        assertEquals(14 * 60 + 30, task.startMinutes)
        assertEquals(14 * 60, task.remindMinutes)
        assertEquals("tomorrow", task.deadlineYmd)
        assertEquals(25, task.timerMinutes)
        assertEquals("Do", task.hubOrColumn)
        val habit = result.commands[1] as KairosCommand.Habit
        assertEquals("every_n_days", habit.rhythm)
        assertEquals(3, habit.intervalDays)
        assertEquals(15, habit.timerMinutes)
        assertEquals(2, habit.deadlineOffsetHours)
    }

    @Test
    fun plannerResolvesTodayAndDoesNotRequireParent() {
        val parse = KairosCommandParser.parse(
            """
            KAIROS v1
            task: "Solo"
              due: today
              start: 10:00
              timer: 20
            """.trimIndent()
        )
        val now = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, java.util.Calendar.SEPTEMBER, 29, 15, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val plan = KairosCommandPlanner.plan(
            parse,
            KairosCommandPlanner.Context(
                projectId = "p",
                boards = listOf(Board("b", "p", "OKR", isDefault = true)),
                columns = listOf(Column("c", "b", "Do", 0)),
                tasks = emptyList(),
                links = emptyList(),
                timeZone = TimeZone.getTimeZone("UTC"),
                nowMillis = now
            )
        )
        assertTrue(plan.errors.joinToString(), plan.canApply)
        val task = plan.actions[0] as KairosPlannedAction.CreateTask
        assertEquals(null, task.parentLocalKey)
        assertEquals(null, task.parentExistingId)
        assertEquals(20, task.timerMinutes)
        assertEquals(10 * 60, task.startMinutesOfDay)
        val cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = task.dueDateMillis!!
        }
        assertEquals(2026, cal.get(java.util.Calendar.YEAR))
        assertEquals(java.util.Calendar.SEPTEMBER, cal.get(java.util.Calendar.MONTH))
        assertEquals(29, cal.get(java.util.Calendar.DAY_OF_MONTH))
        assertTrue(plan.previewLines[0].contains("timer 20m"))
    }

    @Test
    fun parsesWorkoutShopAndContactAsOneCard() {
        val parse = KairosCommandParser.parse(
            """
            KAIROS v1
            task: "Legs"
              type: workout
              line: "Squat" x3
              line: "Row"
            task: "Store"
              type: покупки
              line: "Milk" x2
            task: "Dentist"
              type: связь
              line: "Confirm time"
            """.trimIndent()
        )
        assertTrue(parse.errors.joinToString(), parse.errors.isEmpty())
        val workout = parse.commands[0] as KairosCommand.Task
        assertEquals("workout", workout.cardType)
        assertEquals(2, workout.cardLines.size)
        assertEquals(3, workout.cardLines[0].count)
        assertEquals(null, workout.cardLines[1].count)
        val plan = KairosCommandPlanner.plan(
            parse,
            KairosCommandPlanner.Context(
                projectId = "p",
                boards = listOf(Board("b", "p", "OKR", isDefault = true)),
                columns = listOf(Column("c", "b", "Do", 0)),
                tasks = emptyList(),
                links = emptyList(),
                timeZone = TimeZone.getTimeZone("UTC")
            )
        )
        assertTrue(plan.errors.joinToString(), plan.canApply)
        val created = plan.actions[0] as KairosPlannedAction.CreateTask
        assertEquals("workout", created.cardType)
        assertTrue(created.cardPayload.contains("Squat"))
        val shop = plan.actions[1] as KairosPlannedAction.CreateTask
        assertEquals("shop", shop.cardType)
        assertTrue(shop.cardPayload.contains("\"extra\":\"2\""))
        val meet = plan.actions[2] as KairosPlannedAction.CreateTask
        assertEquals("meet", meet.cardType)
        assertTrue(plan.previewLines[0].contains("workout · 2 lines"))
    }

    @Test
    fun lineOnPlainCardIsAnError() {
        val parse = KairosCommandParser.parse(
            """
            KAIROS v1
            task: "Loose"
              line: "Should not be its own card"
            """.trimIndent()
        )
        assertTrue(parse.errors.any { it.contains("type: workout") })
    }
}
