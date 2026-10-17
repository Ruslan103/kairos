package com.example.kaizenkanban.domain.stats

import com.example.kaizenkanban.domain.model.Board
import com.example.kaizenkanban.domain.model.Column
import com.example.kaizenkanban.domain.model.TaskCardBody
import com.example.kaizenkanban.domain.model.TaskCardKind
import com.example.kaizenkanban.domain.model.RecurringDefaults
import com.example.kaizenkanban.domain.model.RecurringRhythm
import com.example.kaizenkanban.domain.model.RecurringTemplate
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.domain.model.TaskLink
import com.example.kaizenkanban.domain.model.TaskLinkGraph
import com.example.kaizenkanban.domain.model.TaskReminderDefaults
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
    IMPACT,
    /** Emit a KAIROS v1 command block the app can paste and apply. */
    COMMANDS
}

/**
 * Builds a universal planning prompt for any external AI chat (ChatGPT / DeepSeek / Claude).
 * Wording stays plain so a person can also read what was copied.
 */
object GoalAiPromptBuilder {

    private const val MAX_STEPS = 50
    private const val MAX_RECURRING = 20
    private const val DAY_MS = 86_400_000L

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
        periodLabel: String? = null,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.getDefault(),
        /** App default reminder when a dated task/goal has no custom time. */
        defaultReminderMinutes: Int = TaskReminderDefaults.DEFAULT_MINUTES
    ): String {
        val tasksById = projectTasks.filter { !it.isHidden }.associateBy { it.id }
        val columnById = columns.associateBy { it.id }
        val childrenMap = TaskLinkGraph.childrenOf(links)
        val steps = flattenSteps(
            rootId = goal.id,
            childrenMap = childrenMap,
            tasksById = tasksById,
            columnById = columnById,
            max = MAX_STEPS,
            nowMillis = nowMillis,
            timeZone = timeZone,
            locale = locale,
            defaultReminderMinutes = defaultReminderMinutes
        )
        val habitLines = pickHabitLines(goal.id, templates, columnById)

        return buildString {
            appendLine(roleLine(kind))
            appendLine()
            appendAppModel(kind)
            appendLine()
            appendClock(nowMillis, timeZone, locale)
            appendLine()
            appendContext(
                projectName = projectName,
                goal = goal,
                progressPercent = progressPercent,
                childCount = childCount,
                periodLabel = periodLabel,
                steps = steps,
                habits = habitLines,
                includeHabits = kind != GoalAiPromptKind.CRITERIA,
                nowMillis = nowMillis,
                timeZone = timeZone,
                locale = locale,
                defaultReminderMinutes = defaultReminderMinutes
            )
            if (kind == GoalAiPromptKind.COMMANDS) {
                appendLine()
                appendLine("### Hubs / columns you may use in column:")
                if (columns.isEmpty()) appendLine("(none)")
                else columns.sortedBy { it.title }.forEach { appendLine("- ${it.title}") }
            }
            appendLine()
            appendJob(kind)
            appendLine()
            if (kind == GoalAiPromptKind.COMMANDS) {
                appendLine("Write titles in the same language as the goal. Keep every command keyword in English.")
                appendLine("Start with KAIROS v1. Output the command block only.")
            } else {
                appendLine("Reply in the same language as the goal title.")
                appendLine("Start now.")
            }
        }.trim()
    }

    fun buildCommandsPrompt(
        projectName: String,
        selectedGoal: Task?,
        goals: List<Task>,
        boards: List<Board>,
        columns: List<Column>,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.getDefault()
    ): String {
        val fmt = dateTimeFormatter(timeZone, locale)
        val boardById = boards.associateBy { it.id }
        return buildString {
            appendLine("You write Kairos command scripts. Kairos is a personal goals / task-board app.")
            appendLine("The user already agreed a plan with you. Output ONLY the command block — no intro, no markdown fences.")
            appendLine()
            appendLine("## Clock")
            appendLine("Now (user local): ${fmt.format(Date(nowMillis))}")
            appendLine("Time zone: ${timeZone.id}")
            appendLine()
            appendLine("## Project")
            appendLine("Project: ${projectName.ifBlank { "(unnamed)" }}")
            if (selectedGoal != null) {
                appendLine("Selected goal (default parent): ${selectedGoal.title}")
            } else {
                appendLine("Selected goal: none — create one with goal: \"…\" [new] or bind an existing title.")
            }
            appendLine("Existing goals (exact titles; reuse unless [new]):")
            if (goals.isEmpty()) appendLine("- (none)")
            else goals.take(40).forEach { appendLine("- ${it.title}") }
            appendLine()
            appendLine("Hubs (board → column). Use these exact names in hub: / column: / board:")
            val hubs = columns.mapNotNull { col ->
                val board = boardById[col.boardId] ?: return@mapNotNull null
                "- ${board.name} → ${col.title}"
            }
            if (hubs.isEmpty()) appendLine("- (none)")
            else hubs.forEach { appendLine(it) }
            appendLine()
            appendCommandSpec()
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
        GoalAiPromptKind.COMMANDS ->
            "You output machine-readable KAIROS v1 commands so the user can paste them into Kairos and create the plan."
    }

    private fun StringBuilder.appendAppModel(kind: GoalAiPromptKind) {
        appendLine("## How Kairos works (use this language)")
        appendLine("- A goal is a task with a goal mark. Steps hang under it: goal → steps → sub-steps. A step may itself be a goal.")
        appendLine("- Status below: completed / still open / marked not done.")
        appendLine("- A board is split into hubs (columns). Name a hub only from the list in this prompt.")
        appendLine("- The project Diary is a journal (text, photos, its own reminders). It is not a task and has no command. Ideas and a Definition of Done belong there until the user turns them into cards.")
        appendLine("- One card = one task. Inner rows are not separate tasks.")
        appendLine("  · plain — title only.")
        appendLine("  · workout — exercises, each with a set count (default 4). Checks reset on the next habit instance.")
        appendLine("  · shop — shopping lines, optional quantity.")
        appendLine("  · meet — a call or meeting: agenda lines. The user attaches people in the app; do not invent contacts.")
        appendLine("- Impact (Вклад) is 1–5: how much this card moves THIS goal. 1 = small, 5 = decisive. It is not effort, duration, or urgency. Missing Impact counts as 1.")
        appendLine("- Eisenhower is importance, separate from Impact: Q1 urgent+important ×1.40, Q2 important ×1.20, Q3 urgent ×1.00, Q4 neither ×0.85. Unset ×1.00.")
        appendLine("- Quality 1–5 exists only after a card is completed (how well). Do not assign it to open work.")
        appendLine("- A visual group inside a hub is arranged by the user. Do not invent group names.")
        appendLine()
        appendRecurringRules()
        appendLine()
        appendTimingRules()
        when (kind) {
            GoalAiPromptKind.PLAN, GoalAiPromptKind.GAPS -> {
                appendLine("- Cover several kinds of action, not one habit alone. Separate one-time cards from habits.")
                appendLine("- A workout, a shopping list, or a contact is ONE card with inner lines — never one task per exercise, item, or agenda point.")
                appendLine("- An idea that is not yet an obligation goes to the Diary, not onto the board as a fake open task.")
            }
            GoalAiPromptKind.WEEK -> {
                appendLine("- Schedule cards that already exist and are still open. Add a new card only if a blocker is missing.")
                appendLine("- Keep the week realistic. Do not re-plan completed habit instances.")
            }
            GoalAiPromptKind.REFACTOR -> {
                appendLine("- Prefer rename, merge, split, or a better parent over a pile of new tasks.")
                appendLine("- Fold a scattered workout, shopping list, or contact back into one card. Never merge distinct habit instances that only share a title.")
            }
            GoalAiPromptKind.RECURRING -> {
                appendLine("- Design schedules: every day, chosen weekdays, every N days, or several clocks on one habit.")
                appendLine("- A habit may be a workout, shopping list, or contact template. Spawned cards are the regularity, not copies to delete.")
            }
            GoalAiPromptKind.RETRO -> {
                appendLine("- Judge from completed / still open / marked not done, Start and Finish against «now», and quality where it is set.")
                appendLine("- Suggest a few adjustments. Do not rewrite the tree unless it is actually tangled.")
            }
            GoalAiPromptKind.CRITERIA -> {
                appendLine("- A criterion is an observable check the user can paste into the Diary. It is not Impact and not a new task.")
                appendLine("- Prefer a few checks that a stranger could mark yes or no.")
            }
            GoalAiPromptKind.CHANCE -> {
                appendLine("- The progress % is a weighted score, not a probability.")
                appendLine("- Judge the mix of open, done, and not-done, the Finish of the goal, overdue load, and whether the plan is only one habit.")
            }
            GoalAiPromptKind.IMPACT -> {
                appendLine("- Rate the whole card against THIS goal. Do not rate each exercise or shopping line.")
                appendLine("- A short decisive card can be 4–5. A long card of busywork can be 1–2. The meaning of 3 stays the same across the tree.")
            }
            GoalAiPromptKind.COMMANDS -> {
                appendLine("- The block below the situation is the only reply. No fences, no commentary.")
                appendLine("- Create goals, tasks, habits, links, and card rows. Never delete, archive, or rename.")
                appendLine("- No diary command, no photos, no people, no remind-before lead, no groups.")
            }
        }
        appendLine("- Do not invent progress percentages or claim tasks already exist unless listed below.")
        appendLine()
        appendHowNumbersWork()
    }

    private fun StringBuilder.appendRecurringRules() {
        appendLine("## Recurring habits ≠ duplicates (critical)")
        appendLine("- Kairos creates a NEW task card each time a repeating habit fires (daily, weekdays, every N days, or several times per day).")
        appendLine("- Cards with the same title that are tagged as habit instances are REGULARITY of execution — not copy-paste mistakes.")
        appendLine("- Typical pattern: one instance completed (e.g. this morning) and another still open (e.g. this evening or tomorrow) = correct, expected.")
        appendLine("- Several reminder times on one habit (e.g. 07:00 and 20:00) produce separate cards for the same day — still not duplicates.")
        appendLine("- NEVER tell the user to delete/merge those instances as «duplicates». Suggest habit-template changes only if the schedule itself is wrong.")
        appendLine("- One-time steps with the same title and no habit tag may be real duplicates — only then suggest merge/rename.")
    }

    private fun StringBuilder.appendTimingRules() {
        appendLine("## Time: Start, remind, Finish (critical)")
        appendLine("- «Now» below is the user’s current local date/time. Use it as the reference clock.")
        appendLine("- Start (Приступить) = the day and clock when to begin. The card and calendar follow Start.")
        appendLine("- Reminder = when the phone notifies. It may match the Start clock, or be earlier.")
        appendLine("- Finish (Закончить) = optional latest day. If none is set, Finish is the Start day.")
        appendLine("- OVERDUE = still open and the Finish day is before today.")
        appendLine("- DUE TODAY = still open and the Start day is today (including later today).")
        appendLine("- PLANNED = still open with a Start day after today.")
        appendLine("- Completed cards are history. Do not schedule them again.")
        appendLine("- Order advice: overdue, then Start today, then planned.")
        appendLine("- If the goal has a Finish after its Start, that Finish bounds WEEK, CHANCE, and PLAN.")
        appendLine("- The app can also notify earlier than the reminder clock (hours, days, weeks, months before). Commands cannot set that lead — leave it for the user.")
        appendLine()
        appendLine("## Timer (separate from Finish)")
        appendLine("- Timer = countdown minutes the user starts from the card. A tap pauses, a flick cancels.")
        appendLine("- It is not estimated duration and not the Finish date. In commands it is its own line: timer: N")
    }

    private fun StringBuilder.appendHowNumbersWork() {
        appendLine("## How Kairos numbers work (decide yourself how much to weigh them)")
        appendLine("- Goal progress % is a weighted score over linked steps and sub-steps — not a raw count of cards.")
        appendLine("- A leaf’s weight is Impact (1–5, default 1) times Eisenhower importance (Q1 ×1.40, Q2 ×1.20, Q3 or unset ×1.00, Q4 ×0.85). A parent’s weight is the sum of its children.")
        appendLine("- Completed steps raise progress; “marked not done” pulls it down; stats-excluded steps do not count. Quality scales how much a completed step is worth.")
        appendLine("- If the user restarted tracking for a goal, older completed steps may no longer count toward that goal’s progress.")
        appendLine("- Likelihood / chance in the app is a rough heuristic (progress, recent movement, mix of habits vs one-offs, plan breadth, fit to the goal deadline) — not a calibrated probability.")
        appendLine("- Prefer the concrete step tree and habits below over any single percentage when they disagree.")
        appendLine("- You decide how to use these numbers; do not pretend they are scientifically precise.")
    }

    private fun StringBuilder.appendClock(
        nowMillis: Long,
        timeZone: TimeZone,
        locale: Locale
    ) {
        val fmt = dateTimeFormatter(timeZone, locale)
        appendLine("## Clock")
        appendLine("Now (user local): ${fmt.format(Date(nowMillis))}")
        appendLine("Time zone: ${timeZone.id}")
    }

    private fun StringBuilder.appendContext(
        projectName: String,
        goal: Task,
        progressPercent: Int,
        childCount: Int,
        periodLabel: String?,
        steps: List<String>,
        habits: List<String>,
        includeHabits: Boolean,
        nowMillis: Long,
        timeZone: TimeZone,
        locale: Locale,
        defaultReminderMinutes: Int
    ) {
        appendLine("## Current situation")
        appendLine("Project: ${projectName.ifBlank { "(unnamed)" }}")
        appendLine("Goal: ${goal.title}")
        if (goal.description.isNotBlank()) {
            appendLine("Notes on the goal: ${goal.description.trim().take(400)}")
        }
        appendLine(
            goalDeadlineLine(
                goal = goal,
                nowMillis = nowMillis,
                timeZone = timeZone,
                locale = locale,
                defaultReminderMinutes = defaultReminderMinutes
            )
        )
        appendLine("Approx. progress: $progressPercent% · linked steps now: $childCount")
        if (!periodLabel.isNullOrBlank()) {
            appendLine("Period the user is looking at in the app: $periodLabel")
        }
        appendLine()
        appendLine("### Steps already under this goal (nested; includes sub-steps)")
        appendLine("Format: title (status; timeline; hub; impact; card; habit tag if any)")
        if (steps.isEmpty()) {
            appendLine("(none yet)")
        } else {
            steps.forEach { appendLine(it) }
        }
        if (includeHabits) {
            appendLine()
            appendLine("### Repeating habit templates in the project (ones linked to the goal first)")
            appendLine("These are SCHEDULES that spawn task cards — not the cards themselves.")
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
                appendLine("1. If a decision needs a fact you do not have, ask up to 5 questions and stop.")
                appendLine("2. Otherwise answer in exactly these sections:")
                appendLine("### One-time cards")
                appendLine("- Title — parent — hub — type: plain|workout|shop|meet — Impact N — Q1|Q2|Q3|Q4 or omit — Start if it matters")
                appendLine("  Inner lines only for workout / shop / meet. Workout: name × sets. Shop: item × qty when it matters.")
                appendLine("### Habits")
                appendLine("- Title — daily | weekdays (which) | every N days — remind HH:mm (repeat if several) — hub — type — one line why")
                appendLine("### This week")
                appendLine("A short rhythm that uses both lists. Name what you left in the Diary and why.")
                appendLine("3. Prefer 15 new items or fewer. Do not repeat cards that already exist below.")
            }
            GoalAiPromptKind.GAPS -> {
                appendLine("1. Name the few gaps that actually block the goal: a missing kind of action, an overdue card, a workout/shop/contact exploded into many tasks, a plan that is only one habit.")
                appendLine("2. For each: why it matters, then 1–3 fixes (one card, a hub move, a habit, or a Diary note).")
                appendLine("3. Keep healthy habit instances out of the gap list.")
                appendLine("4. End with a prioritized fix list of at most 7 lines.")
            }
            GoalAiPromptKind.WEEK -> {
                appendLine("1. Plan the next 7 days from cards that are still open. Use «now».")
                appendLine("2. Format:")
                appendLine("### Overdue — do first")
                appendLine("### YYYY-MM-DD (weekday)")
                appendLine("- Exact existing title — why this day")
                appendLine("3. At most 3 new cards, and only for a missing blocker. Do not reschedule completed habit instances.")
            }
            GoalAiPromptKind.REFACTOR -> {
                appendLine("1. Propose only changes worth making. Format each as: before → after, and say rename / merge / split / reparent / make one workout|shop|meet card.")
                appendLine("2. Leave completed work meaning intact.")
                appendLine("3. Never merge habit instances that share a title.")
                appendLine("4. The user will apply this by hand. Keep it short.")
            }
            GoalAiPromptKind.RECURRING -> {
                appendLine("1. Propose 3–7 habits. For each: title, rhythm, weekday or interval, remind clock(s), hub, type, inner lines if workout/shop/meet, and one line why.")
                appendLine("2. If a listed template is weak, say what to change on the template. Do not call its spawned cards duplicates.")
                appendLine("3. Say how these habits sit next to the one-time cards already listed.")
                appendLine("4. A realistic week beats a dense one. Several clocks on one habit are fine when the goal needs them.")
            }
            GoalAiPromptKind.RETRO -> {
                appendLine("1. Three short parts: what moved the goal, what is stuck (overdue and long-open, not habit regularity), what the progress % is hiding.")
                appendLine("2. Then 3–5 changes: keep / cut / add / reorder. Tie each to a listed title.")
                appendLine("3. If quality is missing on important completed cards, say so once — do not nag.")
            }
            GoalAiPromptKind.CRITERIA -> {
                appendLine("1. Write a Definition of Done the user can paste into the Diary: 5–10 checks.")
                appendLine("2. Each check is one line a stranger could mark yes or no. No “better”, “more”, or “enough” unless you add a number or a fact.")
                appendLine("3. Add checks for a major open sub-goal only when the goal-level list would hide it.")
                appendLine("4. Do not create tasks and do not set Impact here.")
            }
            GoalAiPromptKind.CHANCE -> {
                appendLine("1. If key context is missing (hours/week, constraints) and there is no goal deadline below, ask up to 5 clarifying questions first.")
                appendLine("2. Otherwise give an honest chance estimate as a percentage range (e.g. 35–50%), not a fake precise number.")
                appendLine("3. Explain the main factors: deadline (if any), progress so far, open vs completed, overdue load, breadth of the plan, habits, risks.")
                appendLine("4. List 3–5 concrete moves that would raise the chance the most.")
                appendLine("5. Say clearly what would make the chance low or high.")
            }
            GoalAiPromptKind.IMPACT -> {
                appendLine("1. For each listed step (especially still open), suggest Impact 1–5 toward THIS goal.")
                appendLine("2. Format: step title → Impact N/5 — one short reason.")
                appendLine("3. If a step already has Impact, say keep / change (old → new) and why.")
                appendLine("4. Call out steps that look over- or under-weighted relative to siblings.")
                appendLine("5. Habit instances with the same title can share the same Impact suggestion.")
                appendLine("6. The user will type Impact on the card. Keep the list to the steps that should change.")
            }
            GoalAiPromptKind.COMMANDS -> {
                appendLine("Emit commands for the agreed cards only. Follow this spec exactly.")
                appendLine()
                appendCommandSpec()
            }
        }
    }

    private fun StringBuilder.appendCommandSpec() {
        appendLine("## Command language (KAIROS v1)")
        appendLine("First line exactly: KAIROS v1")
        appendLine("Optional: project: \"exact project name\"")
        appendLine("Optional: hub: \"hub title\" — default hub for new cards")
        appendLine("goal: \"Title\" [new]")
        appendLine("goal: \"Exact existing title\"")
        appendLine("task: \"Title\"")
        appendLine("habit: \"Title\"")
        appendLine("link: \"Parent title\" -> \"Child title\"")
        appendLine("Indent properties with spaces. Titles in double quotes.")
        appendLine("  parent: goal")
        appendLine("  parent: \"Exact parent title\"")
        appendLine("  due: YYYY-MM-DD | today | tomorrow   — Start (Приступить): the day to begin")
        appendLine("  start: HH:mm   — clock to begin; omit for the app default")
        appendLine("  remind: HH:mm   — notification clock; omit to notify at Start. On a habit, repeat the line for several times a day")
        appendLine("  deadline: YYYY-MM-DD | today | tomorrow   — Finish (Закончить): latest day. finish: is an alias. Omit and Finish is the Start day")
        appendLine("  timer: 20   — countdown minutes on the card. Not Finish and not a duration estimate")
        appendLine("  impact: 1-5")
        appendLine("  eisenhower: Q1|Q2|Q3|Q4    — IU / IN / NU / NN also work")
        appendLine("  hub: \"hub title\"    — column: and board: are aliases")
        appendLine("  type: plain | workout | shop | meet")
        appendLine("  line: \"Row text\" xN   — repeat for each inner row. Workout: N is sets (omit xN for 4). Shop: N is quantity. Meet: agenda text, ignore N")
        appendLine("Habit only:")
        appendLine("  rhythm: daily | weekdays | every_n_days | every 3 days")
        appendLine("  days: 1,3,5   — Mon=1 … Sun=7. Required when rhythm is weekdays")
        appendLine("  interval: N   — 1–999, for every_n_days")
        appendLine("  due: YYYY-MM-DD | today | tomorrow   — first day a card may appear")
        appendLine("  deadline_days: N")
        appendLine("  deadline_hours: N   — Finish offset after each instance Start")
        appendLine()
        appendLine("Rules:")
        appendLine("- Create only what was agreed. Never delete, archive, or rename.")
        appendLine("- Do not invent ids. Reuse an existing goal by its exact title; add [new] only for a new goal.")
        appendLine("- parent: goal means the selected goal, or the last goal: line. Omit parent to attach the same way, or to create a free task when no goal is selected.")
        appendLine("- due = Start day. start = clock. remind = notification clock only.")
        appendLine("- deadline / finish = Finish day. Overdue = Finish day before today.")
        appendLine("- timer is a separate setting (not Finish): the countdown the user starts on the card.")
        appendLine("- One task or habit for a workout, a shopping list, or a contact. Put exercises, items, and agenda on line: rows. Do not emit a task per row.")
        appendLine("- Do not invent hub names. Omit hub to use the project default.")
        appendLine("- No diary, note, photo, contact-person, group, or remind-before command.")
        appendLine("- Keep keywords in English even when titles are not. Prefer under 30 items.")
        appendLine()
        appendLine("Example:")
        appendLine("KAIROS v1")
        appendLine("goal: \"Run 10k\"")
        appendLine("task: \"Buy shoes\"")
        appendLine("  parent: goal")
        appendLine("  type: shop")
        appendLine("  line: \"Road shoes\"")
        appendLine("  due: tomorrow")
        appendLine("  impact: 2")
        appendLine("habit: \"Easy run\"")
        appendLine("  parent: goal")
        appendLine("  rhythm: weekdays")
        appendLine("  days: 1,3,5")
        appendLine("  remind: 07:00")
        appendLine("  type: workout")
        appendLine("  line: \"Easy run\" x1")
        appendLine("  timer: 20")
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
            val times = RecurringDefaults.effectiveReminderTimes(t.reminderTimesOfDay)
                .joinToString(", ") { formatMinutesOfDay(it) }
            "- ${t.title} · how often: ${howOftenLabel(t)} · reminds at: $times · column: $column" +
                (t.timerMinutes?.takeIf { it > 0 }?.let { " · timer ${it} min" } ?: "") +
                (cardSummary(t.cardType, t.cardPayload)?.let { " · $it" } ?: "")
        }
    }

    private fun cardSummary(cardType: String, cardPayload: String): String? {
        val kind = TaskCardKind.normalize(cardType)
        if (kind == TaskCardKind.PLAIN) return null
        val label = when (kind) {
            TaskCardKind.WORKOUT -> "workout"
            TaskCardKind.SHOP -> "shopping"
            TaskCardKind.MEET -> "contact"
            else -> return null
        }
        val lines = TaskCardBody.decode(kind, cardPayload).lines
            .filter { it.text.isNotBlank() }
            .take(6)
        if (lines.isEmpty()) return "card: $label"
        val shown = lines.joinToString(", ") { line ->
            when (kind) {
                TaskCardKind.WORKOUT ->
                    if (line.sets.isNotEmpty()) "${line.text} ×${line.sets.size}" else line.text
                TaskCardKind.SHOP ->
                    if (line.extra.isNotBlank()) "${line.text} ×${line.extra}" else line.text
                else -> line.text
            }
        }
        return "card: $label ($shown)"
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
        max: Int,
        nowMillis: Long,
        timeZone: TimeZone,
        locale: Locale,
        defaultReminderMinutes: Int
    ): List<String> {
        val out = mutableListOf<String>()
        val dayFmt = dateFormatter(timeZone, locale)
        val dateTimeFmt = dateTimeFormatter(timeZone, locale)
        fun walk(id: String, depth: Int) {
            if (out.size >= max) return
            childrenMap[id].orEmpty().forEach { childId ->
                if (out.size >= max) return
                val task = tasksById[childId] ?: return@forEach
                if (task.statsExcluded || task.isHidden) return@forEach
                val pad = "  ".repeat(depth)
                val status = when {
                    task.isCompleted -> "completed"
                    task.isNotDone -> "marked not done"
                    else -> "still open"
                }
                val column = columnById[task.columnId]?.title
                val parts = buildList {
                    add(status)
                    add(
                        timelineLabel(
                            task = task,
                            nowMillis = nowMillis,
                            dayFmt = dayFmt,
                            dateTimeFmt = dateTimeFmt,
                            defaultReminderMinutes = defaultReminderMinutes,
                            timeZone = timeZone
                        )
                    )
                    if (column != null) add("column: $column")
                    task.complexity?.let { add("impact $it/5") }
                    cardSummary(task.cardType, task.cardPayload)?.let { add(it) }
                    if (task.isCompleted) {
                        task.completionQuality?.let { add("quality $it/5") }
                    }
                    if (!task.recurringTemplateId.isNullOrBlank()) {
                        add("habit instance (regularity — not a duplicate)")
                    }
                    if (task.isGoal) add("also marked as a goal")
                }
                out += "$pad- ${task.title} (${parts.joinToString("; ")})"
                walk(childId, depth + 1)
            }
        }
        walk(rootId, 0)
        return out
    }

    private fun goalDeadlineLine(
        goal: Task,
        nowMillis: Long,
        timeZone: TimeZone,
        locale: Locale,
        defaultReminderMinutes: Int
    ): String {
        val due = goal.dueDate ?: return "Start: none"
        val dayFmt = dateFormatter(timeZone, locale)
        val day = dayFmt.format(Date(due))
        val startClock = formatMinutesOfDay(
            (goal.startMinutesOfDay ?: goal.reminderMinutesOfDay ?: defaultReminderMinutes)
                .coerceIn(0, 24 * 60 - 1)
        )
        val reminderClock = formatMinutesOfDay(
            (goal.reminderMinutesOfDay ?: goal.startMinutesOfDay ?: defaultReminderMinutes)
                .coerceIn(0, 24 * 60 - 1)
        )
        val deadline = goal.deadlineAt?.let { dayFmt.format(Date(it)) } ?: "same as start"
        val timerBit = goal.timerMinutes?.takeIf { it > 0 }?.let { " · timer ${it} min" }.orEmpty()
        val anchor = goal.deadlineAt ?: due
        val startToday = startOfLocalDay(nowMillis, timeZone)
        val status = when {
            goal.isCompleted -> "completed"
            anchor < startToday -> "OVERDUE"
            due < startToday + DAY_MS && due >= startToday -> "due today"
            else -> "planned"
        }
        return "Start: $day $startClock · reminder $reminderClock · Finish: $deadline$timerBit · $status"
    }

    private fun timelineLabel(
        task: Task,
        nowMillis: Long,
        dayFmt: SimpleDateFormat,
        dateTimeFmt: SimpleDateFormat,
        defaultReminderMinutes: Int,
        timeZone: TimeZone
    ): String {
        val due = task.dueDate
        val completedAt = task.completedAt
        val startClock = if (due != null) {
            formatMinutesOfDay(
                (task.startMinutesOfDay ?: task.reminderMinutesOfDay ?: defaultReminderMinutes)
                    .coerceIn(0, 24 * 60 - 1)
            )
        } else null
        val reminderClock = if (due != null) {
            formatMinutesOfDay(
                (task.reminderMinutesOfDay ?: task.startMinutesOfDay ?: defaultReminderMinutes)
                    .coerceIn(0, 24 * 60 - 1)
            )
        } else null
        val deadlineBit = when {
            due == null -> null
            task.deadlineAt == null -> "Finish same as start"
            else -> "Finish ${dayFmt.format(Date(task.deadlineAt))}"
        }
        val timerBit = task.timerMinutes?.takeIf { it > 0 }?.let { "timer ${it} min" }
        if (task.isCompleted) {
            return when {
                completedAt != null && due != null ->
                    "done ${dateTimeFmt.format(Date(completedAt))}; was start ${dayFmt.format(Date(due))} $startClock · reminder $reminderClock · $deadlineBit"
                completedAt != null ->
                    "done ${dateTimeFmt.format(Date(completedAt))}"
                due != null ->
                    "done; was start ${dayFmt.format(Date(due))} $startClock · reminder $reminderClock · $deadlineBit"
                else -> "done; no start date"
            }
        }
        if (due == null) return "no start date"
        val dueDay = dayFmt.format(Date(due))
        val anchor = task.deadlineAt ?: due
        val startToday = startOfLocalDay(nowMillis, timeZone)
        val timing = listOfNotNull(
            "start $dueDay $startClock",
            "reminder $reminderClock",
            deadlineBit,
            timerBit
        ).joinToString(" · ")
        return when {
            anchor < startToday -> "OVERDUE · $timing"
            due < startToday + DAY_MS && due >= startToday -> "due today · $timing"
            else -> "planned · $timing"
        }
    }

    private fun dateTimeFormatter(timeZone: TimeZone, locale: Locale): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd HH:mm", locale).apply {
            this.timeZone = timeZone
        }

    private fun dateFormatter(timeZone: TimeZone, locale: Locale): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd", locale).apply {
            this.timeZone = timeZone
        }

    private fun formatMinutesOfDay(minutes: Int): String {
        val clamped = minutes.coerceIn(0, 23 * 60 + 59)
        return String.format(Locale.US, "%02d:%02d", clamped / 60, clamped % 60)
    }

    private fun startOfLocalDay(nowMillis: Long, timeZone: TimeZone): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
