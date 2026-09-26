package com.example.kaizenkanban.domain.stats

import com.example.kaizenkanban.domain.model.Column
import com.example.kaizenkanban.domain.model.RecurringRhythm
import com.example.kaizenkanban.domain.model.RecurringTemplate
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.domain.model.TaskLink
import com.example.kaizenkanban.domain.model.TaskLinkGraph

enum class GoalAiPromptKind {
    /** Design / fill gaps with one-offs + habits. */
    PLAN,
    /** Focus on missing levers and stuck open work. */
    GAPS,
    /** What to do in the next 7 days from existing open steps. */
    WEEK,
    /** Simplify / rename / merge the step tree. */
    REFACTOR,
    /** Repeating habits only. */
    RECURRING,
    /** Retro of progress for the selected stats period. */
    RETRO,
    /** Definition of Done / success criteria. */
    CRITERIA,
    /** Estimate chances of reaching the goal. */
    CHANCE,
    /** Suggest Impact (1–5) for each step toward the goal. */
    IMPACT
}

/**
 * Builds a universal planning prompt for any external AI chat.
 * Wording stays plain so a person can also read what was copied.
 */
object GoalAiPromptBuilder {

    private const val MAX_STEPS = 50
    private const val MAX_RECURRING = 20

    fun build(
        kind: GoalAiPromptKind,
        projectName: String,
        goal: Task,
        projectTasks: List<Task>,
        links: List<TaskLink>,
        columns: List<Column>,
        templates: List<RecurringTemplate>,
        childCount: Int,
        progressPercent: Int,
        periodLabel: String? = null
    ): String {
        val tasksById = projectTasks.associateBy { it.id }
        val columnById = columns.associateBy { it.id }
        val childrenMap = TaskLinkGraph.childrenOf(links)
        val steps = flattenSteps(goal.id, childrenMap, tasksById, columnById, max = MAX_STEPS)
        val habitLines = pickHabitLines(goal.id, templates, columnById)

        return buildString {
            appendLine(roleLine(kind))
            appendLine()
            appendAppModel(kind)
            appendLine()
            appendContext(
                projectName = projectName,
                goal = goal,
                progressPercent = progressPercent,
                childCount = childCount,
                periodLabel = periodLabel,
                steps = steps,
                habits = habitLines,
                includeHabits = kind != GoalAiPromptKind.CRITERIA
            )
            appendLine()
            appendJob(kind)
            appendLine()
            appendLine("Reply in the same language as the goal title.")
            appendLine("Start now.")
        }.trim()
    }

    private fun roleLine(kind: GoalAiPromptKind): String = when (kind) {
        GoalAiPromptKind.PLAN ->
            "You help plan work in Kairos — a personal goals and task-board app. Design concrete next steps for ONE goal."
        GoalAiPromptKind.GAPS ->
            "You help find gaps and stuck work under ONE goal in Kairos (personal goals / task boards)."
        GoalAiPromptKind.WEEK ->
            "You are a weekly coach for Kairos. Build a realistic plan for the next 7 days for ONE goal."
        GoalAiPromptKind.REFACTOR ->
            "You help clean up the step structure of ONE goal in Kairos. Do not invent fake progress."
        GoalAiPromptKind.RECURRING ->
            "You design repeating habits that support ONE goal in Kairos."
        GoalAiPromptKind.RETRO ->
            "You run a short retro for ONE goal in Kairos: what worked, what stalled, what to change."
        GoalAiPromptKind.CRITERIA ->
            "You help define clear success criteria for ONE goal in Kairos."
        GoalAiPromptKind.CHANCE ->
            "You assess how realistic it is to reach ONE goal in Kairos, given the current plan and progress."
        GoalAiPromptKind.IMPACT ->
            "You rate how much each step moves ONE goal in Kairos — Impact on a 1–5 scale."
    }

    private fun StringBuilder.appendAppModel(kind: GoalAiPromptKind) {
        appendLine("## How Kairos works (use this language)")
        appendLine("- A goal is a special task. Steps hang under it as parent → child (goal → steps → sub-steps).")
        appendLine("- Step status in the list below: completed / still open / marked not done.")
        appendLine("- Boards are split into columns (for example Course / Plan / Do). Say which column a step belongs to when useful.")
        appendLine("- Impact (Вклад) is 1–5: how much the step moves the goal (1 = small, 5 = major). It is NOT how hard the work felt.")
        appendLine("- In the app, step weight ≈ Impact × importance (from Eisenhower). Missing Impact is treated as 1.")
        when (kind) {
            GoalAiPromptKind.PLAN, GoalAiPromptKind.GAPS -> {
                appendLine("- Prefer several different kinds of action, not only one repeating habit.")
                appendLine("- Separate one-time steps from repeating habits.")
            }
            GoalAiPromptKind.WEEK -> {
                appendLine("- Prefer steps that already exist and are still open; add new ones only if something critical is missing.")
                appendLine("- Keep the week realistic (time and energy).")
            }
            GoalAiPromptKind.REFACTOR -> {
                appendLine("- Prefer rename / merge / reorder / move under another parent over adding many new tasks.")
                appendLine("- Keep the meaning of completed work.")
            }
            GoalAiPromptKind.RECURRING -> {
                appendLine("- Focus on how often something repeats: every day, weekdays, or every N days.")
                appendLine("- Do not dump a long one-time backlog.")
            }
            GoalAiPromptKind.RETRO -> {
                appendLine("- Judgement from completed vs still open vs marked not done.")
                appendLine("- Suggest small next adjustments, not a full rewrite unless needed.")
            }
            GoalAiPromptKind.CRITERIA -> {
                appendLine("- Criteria should be observable and checkable in the app.")
                appendLine("- Prefer a few clear success checks over vague wishes.")
            }
            GoalAiPromptKind.CHANCE -> {
                appendLine("- Treat the app progress % as rough, not a true probability.")
                appendLine("- Judge from the mix of completed / still open / marked not done, plan breadth, and habits.")
                appendLine("- Prefer several different kinds of action over reliance on one habit alone.")
            }
            GoalAiPromptKind.IMPACT -> {
                appendLine("- Rate relative contribution to THIS goal, not effort, time spent, or urgency alone.")
                appendLine("- A tiny but decisive step can be 4–5; a long busywork step can be 1–2.")
                appendLine("- Keep the scale consistent across the whole tree (same meaning of 3 everywhere).")
            }
        }
        appendLine("- Do not invent progress percentages or claim tasks already exist unless listed below.")
        appendLine()
        appendHowNumbersWork()
    }

    private fun StringBuilder.appendHowNumbersWork() {
        appendLine("## How Kairos numbers work (decide yourself how much to weigh them)")
        appendLine("- Goal progress % is a weighted score over linked steps and sub-steps — not a raw count of cards.")
        appendLine("- Each leaf step’s weight uses Impact (1–5, default 1) and Eisenhower importance; parent weight is the sum of children.")
        appendLine("- Completed steps raise progress; “marked not done” pulls it down; ignored (stats-excluded) steps do not count.")
        appendLine("- If the user restarted tracking for a goal, older completed steps may no longer count toward that goal’s progress.")
        appendLine("- Likelihood / chance in the app is a rough heuristic (progress, recent movement, mix of habits vs one-offs, plan breadth) — not a calibrated probability.")
        appendLine("- Prefer the concrete step tree and habits below over any single percentage when they disagree.")
        appendLine("- You decide how to use these numbers; do not pretend they are scientifically precise.")
    }

    private fun StringBuilder.appendContext(
        projectName: String,
        goal: Task,
        progressPercent: Int,
        childCount: Int,
        periodLabel: String?,
        steps: List<String>,
        habits: List<String>,
        includeHabits: Boolean
    ) {
        appendLine("## Current situation")
        appendLine("Project: ${projectName.ifBlank { "(unnamed)" }}")
        appendLine("Goal: ${goal.title}")
        if (goal.description.isNotBlank()) {
            appendLine("Notes on the goal: ${goal.description.trim().take(400)}")
        }
        appendLine("Approx. progress: $progressPercent% · linked steps now: $childCount")
        if (!periodLabel.isNullOrBlank()) {
            appendLine("Period the user is looking at in the app: $periodLabel")
        }
        appendLine()
        appendLine("### Steps already under this goal (nested; includes sub-steps)")
        if (steps.isEmpty()) {
            appendLine("(none yet)")
        } else {
            steps.forEach { appendLine(it) }
        }
        if (includeHabits) {
            appendLine()
            appendLine("### Repeating habits in the project (ones linked to the goal first)")
            if (habits.isEmpty()) {
                appendLine("(none)")
            } else {
                habits.forEach { appendLine(it) }
            }
        }
    }

    private fun StringBuilder.appendJob(kind: GoalAiPromptKind) {
        appendLine("## Your job")
        when (kind) {
            GoalAiPromptKind.PLAN -> {
                appendLine("1. If information is insufficient, ask up to 5 clarifying questions first (then wait).")
                appendLine("2. When enough context, propose:")
                appendLine("   A) One-time steps (title, optional parent, suggested column, Impact 1–5).")
                appendLine("   B) Repeating habits (title, how often, why it serves the goal).")
                appendLine("   C) A light order / weekly rhythm so several kinds of action are covered.")
                appendLine("3. Do not blindly duplicate existing steps; improve and fill gaps.")
                appendLine("4. Keep the list small enough to enter manually into Kairos.")
            }
            GoalAiPromptKind.GAPS -> {
                appendLine("1. List the biggest gaps: missing kinds of action, stalled open steps, weak mix of approaches.")
                appendLine("2. For each gap: why it matters + 1–3 concrete fixes (new step, move column, or habit).")
                appendLine("3. Call out items marked not done or long-open that need a decision.")
                appendLine("4. End with a short prioritized fix list.")
            }
            GoalAiPromptKind.WEEK -> {
                appendLine("1. Build a focused plan for the next 7 days from steps that are still open (and critical missing ones only).")
                appendLine("2. Output a day-by-day or weekday checklist; reuse existing titles when possible.")
                appendLine("3. Mark what to defer; avoid overload.")
                appendLine("4. Optionally add at most 3 new tiny steps if a blocker is missing.")
            }
            GoalAiPromptKind.REFACTOR -> {
                appendLine("1. Propose renames, merges, splits, and moving steps under a better parent.")
                appendLine("2. Show before → after for each change; keep completed items intact in meaning.")
                appendLine("3. Aim for a clearer hierarchy the user can apply by hand.")
                appendLine("4. Do not invent a huge new backlog.")
            }
            GoalAiPromptKind.RECURRING -> {
                appendLine("1. Propose 3–7 repeating habits that serve this goal (title, how often, column, why).")
                appendLine("2. Prefer improving/replacing weak existing habits over duplicates.")
                appendLine("3. Explain how habits complement one-time steps already listed.")
                appendLine("4. Keep the schedule realistic.")
            }
            GoalAiPromptKind.RETRO -> {
                appendLine("1. Summarize momentum from completed / still open / marked not done and the progress %.")
                appendLine("2. What worked, what stalled, what looks like noise.")
                appendLine("3. Suggest 3–5 adjustments for the next period (keep / cut / add / reorder).")
                appendLine("4. Stay concrete and tied to the listed steps.")
            }
            GoalAiPromptKind.CRITERIA -> {
                appendLine("1. Propose a clear Definition of Done for the goal (5–10 checkable criteria).")
                appendLine("2. Optionally add success checks for major open sub-goals/steps.")
                appendLine("3. Phrase criteria so they can become notes or checklist items in Kairos.")
                appendLine("4. Avoid vague words (better, more, enough) unless made measurable.")
            }
            GoalAiPromptKind.CHANCE -> {
                appendLine("1. If key context is missing (deadline, hours/week, constraints), ask up to 5 clarifying questions first.")
                appendLine("2. Otherwise give an honest chance estimate as a percentage range (e.g. 35–50%), not a fake precise number.")
                appendLine("3. Explain the main factors: progress so far, open vs completed steps, breadth of the plan, habits, obvious risks.")
                appendLine("4. List 3–5 concrete moves that would raise the chance the most.")
                appendLine("5. Say clearly what would make the chance low or high.")
            }
            GoalAiPromptKind.IMPACT -> {
                appendLine("1. For each listed step (especially still open), suggest Impact 1–5 toward THIS goal.")
                appendLine("2. Format: step title → Impact N/5 — one short reason.")
                appendLine("3. If a step already has Impact, say keep / change (old → new) and why.")
                appendLine("4. Call out steps that look over- or under-weighted relative to siblings.")
                appendLine("5. Do not invent new steps unless a critical missing lever needs a placeholder with suggested Impact.")
                appendLine("6. Keep the output short enough to enter manually into Kairos (criteria → Impact).")
            }
        }
    }

    private fun pickHabitLines(
        goalId: String,
        templates: List<RecurringTemplate>,
        columnById: Map<String, Column>
    ): List<String> {
        val linked = templates
            .filter { it.enabled && (goalId in it.linkParentIds || goalId in it.linkChildIds) }
            .take(MAX_RECURRING)
        val chosen = if (linked.isNotEmpty()) {
            linked
        } else {
            templates.filter { it.enabled }.take(MAX_RECURRING)
        }
        return chosen.map { t ->
            val column = columnById[t.targetColumnId]?.title ?: t.targetColumnId
            "- ${t.title} · how often: ${howOftenLabel(t)} · column: $column"
        }
    }

    private fun howOftenLabel(t: RecurringTemplate): String = when (t.rhythm) {
        RecurringRhythm.DAILY -> "every day"
        RecurringRhythm.WEEKDAYS -> "on weekdays ${t.weekdays.sorted()}"
        RecurringRhythm.EVERY_N_DAYS -> "every ${t.timesPerWeek ?: 2} days"
        else -> t.rhythm
    }

    private fun flattenSteps(
        rootId: String,
        childrenMap: Map<String, List<String>>,
        tasksById: Map<String, Task>,
        columnById: Map<String, Column>,
        max: Int
    ): List<String> {
        val out = mutableListOf<String>()
        fun walk(id: String, depth: Int) {
            if (out.size >= max) return
            childrenMap[id].orEmpty().forEach { childId ->
                if (out.size >= max) return
                val task = tasksById[childId] ?: return@forEach
                if (task.statsExcluded) return@forEach
                val pad = "  ".repeat(depth)
                val status = when {
                    task.isCompleted -> "completed"
                    task.isNotDone -> "marked not done"
                    else -> "still open"
                }
                val column = columnById[task.columnId]?.title
                val parts = buildList {
                    add(status)
                    if (column != null) add("column: $column")
                    task.complexity?.let { add("impact $it/5") }
                    if (task.complexity == null) add("impact not set (defaults to 1)")
                    if (task.isGoal) add("also marked as a goal")
                }
                out += "$pad- ${task.title} (${parts.joinToString("; ")})"
                walk(childId, depth + 1)
            }
        }
        walk(rootId, 0)
        return out
    }
}
