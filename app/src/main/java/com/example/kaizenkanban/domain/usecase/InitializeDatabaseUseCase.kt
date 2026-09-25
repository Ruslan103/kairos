package com.example.kaizenkanban.domain.usecase

import com.example.kaizenkanban.data.local.KairosPreferences
import com.example.kaizenkanban.domain.model.Board
import com.example.kaizenkanban.domain.model.Column
import com.example.kaizenkanban.domain.model.ColumnComment
import com.example.kaizenkanban.domain.model.Project
import com.example.kaizenkanban.domain.repository.KanbanRepository
import java.util.UUID
import kotlinx.coroutines.flow.first

class InitializeDatabaseUseCase(
    private val repository: KanbanRepository,
    private val languageCode: String = "en",
    private val prefs: KairosPreferences? = null
) {
    suspend operator fun invoke() {
        // Warm start: skip emoji renames, hub remaps, and comment backfills when already seeded.
        if (prefs != null && prefs.dbSeedVersion >= SEED_VERSION) {
            val projects = repository.getProjects().first()
            val hasBoards = prefs.okrBoardId != null && prefs.eisenhowerBoardId != null
            if (projects.isNotEmpty() && hasBoards && repository.getCategoriesCount() > 0) {
                return
            }
        }

        runFullSeed()
        prefs?.dbSeedVersion = SEED_VERSION
    }

    private suspend fun runFullSeed() {
        var projects = repository.getProjects().first()
        if (projects.isEmpty()) {
            val defaultProject = Project(id = UUID.randomUUID().toString(), name = "Kairos", position = 0)
            repository.insertProject(defaultProject)
            projects = listOf(defaultProject)
        } else {
            // Rename legacy "Мой первый проект" to "Kairos" and ensure valid order positions
            projects.forEachIndexed { index, p ->
                val shouldRename = p.name.contains("первый проект", ignoreCase = true) || 
                                   p.name.contains("мой проект", ignoreCase = true)
                val newName = if (shouldRename) "Kairos" else p.name
                if (shouldRename || p.position != index) {
                    repository.insertProject(p.copy(name = newName, position = index))
                }
            }
            projects = repository.getProjects().first()
        }
        val mainProject = projects.first()

        val allBoards = repository.getAllBoards().first()
        val allTasks = repository.getAllTasks().first()
        val allColumns = repository.getAllColumns().first()

        // Helper to strip emojis for clean, modern typography
        fun cleanEmoji(text: String): String =
            text.replace("🎯", "")
                .replace("⚖️", "")
                .replace("⚡", "")
                .replace("📅", "")
                .replace("📈", "")
                .replace("🔥", "")
                .replace("🌟", "")
                .replace("💤", "")
                .trim()

        // 1. Goal ladder board (stored RU titles; UI maps via AppStrings.localized)
        val goalLadderHubTitles = GOAL_LADDER_HUB_TITLES
        var okrBoard = findOkrBoard(allBoards, allColumns)
        if (okrBoard == null) {
            repository.clearDefaultBoards()
            okrBoard = Board(
                id = UUID.randomUUID().toString(),
                projectId = mainProject.id,
                name = GOAL_LADDER_BOARD_NAME,
                isDefault = true
            )
            repository.insertBoard(okrBoard)

            val okrColumns = goalLadderHubTitles.mapIndexed { index, title ->
                Column(
                    id = UUID.randomUUID().toString(),
                    boardId = okrBoard.id,
                    title = title,
                    position = index
                )
            }
            repository.insertColumns(okrColumns)
            okrColumns.find { it.title == PRIMARY_GOAL_LADDER_HUB }?.let {
                prefs?.setPrimaryHubId(okrBoard.id, it.id)
            }
        } else {
            val cleanedName = cleanEmoji(okrBoard.name)
            val renamedBoard = when {
                cleanedName.equals("OKR", ignoreCase = true) ||
                    cleanedName.contains("OKR", ignoreCase = true) ->
                    GOAL_LADDER_BOARD_NAME
                cleanedName != okrBoard.name -> cleanedName
                else -> okrBoard.name
            }
            if (renamedBoard != okrBoard.name) {
                okrBoard = okrBoard.copy(name = renamedBoard)
                repository.insertBoard(okrBoard)
            }

            val okrCols = allColumns.filter { it.boardId == okrBoard.id }.sortedBy { it.position }
            if (okrCols.isNotEmpty()) {
                val needReversal = okrCols.first().title.contains("Стратег", ignoreCase = true)
                val sourceList = if (needReversal) okrCols.reversed() else okrCols
                val renamed = sourceList.map { col ->
                    val mapped = mapOkrHubTitle(col.title)
                    col.copy(title = mapped ?: cleanEmoji(col.title))
                }
                val migrated = ensureGoalLadderHubs(okrBoard.id, renamed)
                repository.insertColumns(migrated)
                migrated.find { it.title == PRIMARY_GOAL_LADDER_HUB }?.let {
                    prefs?.setPrimaryHubId(okrBoard.id, it.id)
                }
                refreshDefaultRulesForColumns(
                    migrated,
                    languageCode.startsWith("ru")
                )
            }
        }
        prefs?.okrBoardId = okrBoard.id

        // 2. Eisenhower Matrix Board setup (clean typography, no emojis)
        var eisenhowerBoard = findEisenhowerBoard(allBoards, allColumns)
        if (eisenhowerBoard == null) {
            val newEisenhowerBoard = Board(
                id = UUID.randomUUID().toString(),
                projectId = mainProject.id,
                name = "Матрица Эйзенхауэра",
                isDefault = false
            )
            repository.insertBoard(newEisenhowerBoard)
            eisenhowerBoard = newEisenhowerBoard

            val eisenhowerTitles = listOf(
                "Срочно и важно",
                "Важно, не срочно",
                "Срочно, не важно",
                "Не срочно и не важно"
            )
            val eisenhowerColumns = eisenhowerTitles.mapIndexed { index, title ->
                Column(
                    id = UUID.randomUUID().toString(),
                    boardId = newEisenhowerBoard.id,
                    title = title,
                    position = index
                )
            }
            repository.insertColumns(eisenhowerColumns)
        } else {
            // Clean board name if it had emojis
            if (eisenhowerBoard.name.contains("⚖️") || eisenhowerBoard.name != cleanEmoji(eisenhowerBoard.name)) {
                eisenhowerBoard = eisenhowerBoard.copy(name = cleanEmoji(eisenhowerBoard.name))
                repository.insertBoard(eisenhowerBoard)
            }

            val eCols = allColumns.filter { it.boardId == eisenhowerBoard.id }.sortedBy { it.position }
            if (eCols.any { it.title != cleanEmoji(it.title) }) {
                val cleanedCols = eCols.map { col -> col.copy(title = cleanEmoji(col.title)) }
                repository.insertColumns(cleanedCols)
            }
        }
        prefs?.eisenhowerBoardId = eisenhowerBoard.id

        // 3. Remove old "Основная доска" if empty, or ensure OKR is default
        val oldBoard = allBoards.find { it.name.equals("Основная доска", ignoreCase = true) }
        if (oldBoard != null) {
            val oldColumnIds = allColumns.filter { it.boardId == oldBoard.id }.map { it.id }.toSet()
            val tasksInOldBoard = allTasks.filter { it.columnId in oldColumnIds }
            if (tasksInOldBoard.isEmpty()) {
                repository.deleteBoard(oldBoard.id)
            }
        }

        // Ensure at least one non-archived board is marked as default
        val currentBoards = repository.getAllBoards().first()
        val activeBoards = currentBoards.filter { !it.isArchived }
        val hasUsableDefault = activeBoards.any { it.isDefault }
        if (!hasUsableDefault) {
            val targetDefault = activeBoards.find { it.id == prefs?.okrBoardId }
                ?: activeBoards.find { isGoalLadderBoardName(it.name) }
                ?: activeBoards.firstOrNull()
                ?: currentBoards.firstOrNull()
            targetDefault?.let { repository.setDefaultBoard(it.id) }
        } else if (currentBoards.count { it.isDefault } > 1) {
            val keep = activeBoards.find { it.isDefault && it.id == prefs?.okrBoardId }
                ?: activeBoards.find { it.isDefault && isGoalLadderBoardName(it.name) }
                ?: activeBoards.find { it.isDefault }
                ?: currentBoards.first { it.isDefault }
            repository.setDefaultBoard(keep.id)
        }

        // 5. Default rules/comments for columns (Hubs)
        val existingColumnComments = repository.getAllColumnComments().first()
        val allCurrentColumns = repository.getAllColumns().first()
        allCurrentColumns.forEach { col ->
            val hasComments = existingColumnComments.any { it.columnId == col.id }
            if (!hasComments) {
                val ruleText = getDefaultRuleForColumn(col.title, languageCode.startsWith("ru"))
                if (ruleText != null) {
                    repository.insertColumnComment(
                        ColumnComment(
                            id = UUID.randomUUID().toString(),
                            columnId = col.id,
                            text = ruleText,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    companion object {
        /** Increment when seed/migration steps above change and must re-run once. */
        const val SEED_VERSION = 2

        const val GOAL_LADDER_BOARD_NAME = "Лестница целей"
        const val PRIMARY_GOAL_LADDER_HUB = "Сделать"
        val GOAL_LADDER_HUB_TITLES = listOf(
            "На стороне контрагента",
            "Сделать",
            "Планирую",
            "Держу курс",
            "Главная цель"
        )

        fun isGoalLadderBoardName(name: String?): Boolean {
            val n = name.orEmpty()
            return n.contains("OKR", ignoreCase = true) ||
                n.contains("Лестница целей", ignoreCase = true) ||
                n.contains("Goal ladder", ignoreCase = true)
        }
    }

    private fun findOkrBoard(boards: List<Board>, columns: List<Column>): Board? {
        prefs?.okrBoardId?.let { id -> boards.find { it.id == id } }?.let { return it }
        boards.find { isGoalLadderBoardName(it.name) }?.let { return it }
        return boards.firstOrNull { board ->
            val hubs = columns.filter { it.boardId == board.id }
            hubs.count { mapOkrHubTitle(it.title) != null } >= 3
        }
    }

    private fun findEisenhowerBoard(boards: List<Board>, columns: List<Column>): Board? {
        prefs?.eisenhowerBoardId?.let { id -> boards.find { it.id == id } }?.let { return it }
        boards.find {
            it.name.contains("Эйзенхауэр", ignoreCase = true) ||
                it.name.contains("Eisenhower", ignoreCase = true)
        }?.let { return it }
        return boards.firstOrNull { board ->
            val hubs = columns.filter { it.boardId == board.id }.sortedBy { it.position }
            hubs.size == 4 && hubs.count { isEisenhowerHubTitle(it.title) } >= 3
        }
    }

    private fun isEisenhowerHubTitle(title: String): Boolean {
        val t = title.lowercase()
        return t.contains("срочно и важно") || t.contains("urgent and important") ||
            t.contains("важно, не срочно") || t.contains("important, not urgent") ||
            t.contains("срочно, не важно") || t.contains("urgent, not important") ||
            t.contains("не срочно и не важно") || t.contains("not urgent and not important")
    }

    /**
     * Canonical goal-ladder hub titles (stored in Russian; [AppStrings.localized] maps for EN UI).
     * Legacy: Сегодня / Скоро / Постоянно / Действия / Спринты / Метрики / Мечта / Dream.
     */
    private fun mapOkrHubTitle(title: String): String? {
        val t = title.trim()
        val lower = t.lowercase()
        return when {
            lower.contains("контрагент") ||
                lower.contains("counterparty") ||
                lower.contains("waiting on") ||
                lower == "on their side" -> "На стороне контрагента"

            lower == "сделать" || lower == "do" || lower == "to do" || lower == "todo" ||
                lower == "сегодня" || lower == "today" ||
                lower == "сейчас" || lower == "now" ||
                lower.contains("действ") || lower == "actions" -> "Сделать"

            lower == "планирую" || lower == "planning" || lower == "plan" ||
                lower == "скоро" || lower == "soon" ||
                lower.contains("спринт") || lower.contains("такт") || lower == "sprints" -> "Планирую"

            lower == "держу курс" || lower == "holding course" || lower == "on course" ||
                lower == "постоянно" || lower == "ongoing" || lower == "constantly" ||
                lower.contains("метрик") || lower.contains("результ") || lower == "metrics" -> "Держу курс"

            lower == "мечта" || lower == "dream" ||
                lower.contains("главная цель") || lower.contains("стратег") ||
                lower == "main goal" -> "Главная цель"

            else -> null
        }
    }

    /** Ensure canonical hubs exist and sit in the expected order; keep custom hubs after. */
    private fun ensureGoalLadderHubs(boardId: String, existing: List<Column>): List<Column> {
        val remaining = existing.toMutableList()
        val ordered = mutableListOf<Column>()
        GOAL_LADDER_HUB_TITLES.forEach { title ->
            val idx = remaining.indexOfFirst { it.title.equals(title, ignoreCase = true) }
            if (idx >= 0) {
                ordered += remaining.removeAt(idx).copy(title = title, position = ordered.size)
            } else {
                ordered += Column(
                    id = UUID.randomUUID().toString(),
                    boardId = boardId,
                    title = title,
                    position = ordered.size
                )
            }
        }
        remaining.forEach { col ->
            ordered += col.copy(position = ordered.size)
        }
        return ordered
    }

    private suspend fun refreshDefaultRulesForColumns(columns: List<Column>, russian: Boolean) {
        val existing = repository.getAllColumnComments().first()
        columns.forEach { col ->
            val ruleText = getDefaultRuleForColumn(col.title, russian) ?: return@forEach
            val colComments = existing.filter { it.columnId == col.id }
            val systemRules = colComments.filter {
                it.text.startsWith("Правила для") || it.text.startsWith("Rules for")
            }
            if (systemRules.size == 1 && colComments.size == 1) {
                val old = systemRules.first()
                if (old.text != ruleText) {
                    repository.deleteColumnComment(old.id)
                    repository.insertColumnComment(
                        ColumnComment(
                            id = UUID.randomUUID().toString(),
                            columnId = col.id,
                            text = ruleText,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    private fun getDefaultRuleForColumn(title: String, russian: Boolean): String? {
        val clean = title.trim().lowercase()
        fun rule(en: String, ru: String) = if (russian) ru else en
        return when {
            clean == "inbox" || clean.contains("входящ") ->
                rule(
                    "Rules for Inbox:\n" +
                        "• New items land here until you sort them.\n" +
                        "• Review this hub often and move work to the right board.",
                    "Правила для «Входящих»:\n" +
                        "• Сюда попадают новые пункты, пока вы их не разберёте.\n" +
                        "• Регулярно просматривайте этот хаб и переносите задачи на нужные доски."
                )

            clean.contains("контрагент") || clean.contains("counterparty") ||
                clean.contains("waiting on") ->
                rule(
                    "Rules for Waiting on others:\n" +
                        "• Work that is blocked on someone else — client, partner, teammate.\n" +
                        "• Park it here so it does not clog “Do”. Follow up when you can.",
                    "Правила для хаба «На стороне контрагента»:\n" +
                        "• Задачи, которые ждут ответа или действия от другого человека — клиент, партнёр, коллега.\n" +
                        "• Держите их здесь, чтобы не забивали «Сделать». Возвращайтесь с напоминанием."
                )

            clean == "сделать" || clean == "do" || clean == "to do" || clean == "todo" ||
                clean == "сегодня" || clean == "today" ||
                clean == "сейчас" || clean == "now" || clean.contains("действ") || clean == "actions" ->
                rule(
                    "Rules for Do:\n" +
                        "• Concrete actions for today — one clear step you can do now.\n" +
                        "• Write as a verb: “Go to the gym”, “Do bench press”.",
                    "Правила для хаба «Сделать»:\n" +
                        "• Конкретное действие на сегодня — один понятный шаг.\n" +
                        "• Пишите глаголом: «Сходить в зал», «Сделать жим»."
                )

            clean == "планирую" || clean == "planning" || clean == "plan" ||
                clean == "скоро" || clean == "soon" ||
                clean.contains("спринт") || clean.contains("такт") || clean == "sprints" ->
                rule(
                    "Rules for Planning:\n" +
                        "• The next stretch (about a week): e.g. “Gym 3 times this week”.\n" +
                        "• Pull today’s step into Do from here.",
                    "Правила для хаба «Планирую»:\n" +
                        "• Ближайший этап (примерно неделя): например «Зал 3 раза на этой неделе».\n" +
                        "• Отсюда берите сегодняшний шаг в «Сделать»."
                )

            clean == "держу курс" || clean == "holding course" || clean == "on course" ||
                clean == "постоянно" || clean == "ongoing" || clean == "constantly" ||
                clean.contains("метрик") || clean.contains("результ") || clean == "metrics" ->
                rule(
                    "Rules for Holding course:\n" +
                        "• The habit or practice that keeps you on course: e.g. “Go to the gym”.\n" +
                        "• Not one day — something you keep repeating.",
                    "Правила для хаба «Держу курс»:\n" +
                        "• Привычка или практика, которая держит курс: например «Ходить в зал».\n" +
                        "• Не разовое дело, а то, что повторяете регулярно."
                )

            clean == "мечта" || clean == "dream" ||
                clean.contains("главная цель") || clean.contains("стратег") || clean == "main goal" ->
                rule(
                    "Rules for Main Goal:\n" +
                        "• The big picture you are moving toward: e.g. “A fit body”.\n" +
                        "• Everything to the left is how you get there.",
                    "Правила для хаба «Главная цель»:\n" +
                        "• Большая картина, к которой идёте: например «Накаченное тело».\n" +
                        "• Всё слева — как вы к ней приходите."
                )

            clean.contains("срочно и важно") || clean.contains("urgent and important") ->
                rule(
                    "Rules for Urgent and important (Q1):\n" +
                        "• Crises, deadlines, and problems that need you now.",
                    "Правила для хаба «Срочно и важно» (Q1 — Сделай сейчас):\n" +
                        "• Кризисы, горящие дедлайны, неотложные проблемы и форс-мажоры.\n" +
                        "• Дела, невыполнение которых прямо сейчас приведет к серьезным негативным последствиям.\n" +
                        "• Выполняйте лично и незамедлительно в первую очередь!"
                )

            clean.contains("важно, не срочно") || clean.contains("важно и не срочно") ||
                clean.contains("не срочно, но важно") || clean.contains("important, not urgent") ->
                rule(
                    "Rules for Important, not urgent (Q2):\n" +
                        "• Planning, health, learning, relationships — schedule time every day.",
                    "Правила для хаба «Важно, не срочно» (Q2 — Стратегия и развитие):\n" +
                        "• Самый главный квадрант личной эффективности и долгосрочного успеха!\n" +
                        "• Стратегическое планирование, здоровье, спорт, обучение, отношения, предотвращение кризисов.\n" +
                        "• Выделяйте фиксированное время в календаре для задач из этого хаба каждый день."
                )

            clean.contains("срочно, не важно") || clean.contains("срочно и не важно") ||
                clean.contains("urgent, not important") ->
                rule(
                    "Rules for Urgent, not important (Q3):\n" +
                        "• Interruptions and other people’s requests. Delegate when you can.",
                    "Правила для хаба «Срочно, не важно» (Q3 — Делегируй):\n" +
                        "• Срочные дела, создающие иллюзию бурной деятельности, но не приближающие к вашим истинным целям.\n" +
                        "• Чужие просьбы, неожиданные звонки, мелкая текучка.\n" +
                        "• Рекомендуется делегировать, автоматизировать или выполнять с минимальными затратами сил."
                )

            clean.contains("не срочно и не важно") || clean.contains("не важно, не срочно") ||
                clean.contains("not urgent and not important") ->
                rule(
                    "Rules for Not urgent and not important (Q4):\n" +
                        "• Time wasters. Cut them or drop them.",
                    "Правила для хаба «Не срочно и не важно» (Q4 — Откажись / Удали):\n" +
                        "• Пожиратели времени (хронофаги): бессмысленный серфинг, пустые разговоры, рутина без пользы.\n" +
                        "• Задачи, которые можно не делать вовсе без каких-либо потерь.\n" +
                        "• Безжалостно вычеркивайте или сокращайте до минимума!"
                )

            clean.contains("на неделе") || clean.contains("this week") ->
                rule(
                    "Rules for This week:\n" +
                        "• The 7-day focus. Pull from here into Do.",
                    "Правила для хаба «На неделе»:\n" +
                        "• Фокус на 7 дней. То, что важно продвинуть на текущей неделе.\n" +
                        "• Из этого хаба задачи отбираются в хаб «Сделать»."
                )

            clean.contains("на месяц") ->
                rule(
                    "Rules for This month:\n" +
                        "• 30-day tactical goals and milestones.",
                    "Правила для хаба «На месяц»:\n" +
                        "• Тактические цели на 30 дней. Проекты и ключевые этапы месяца."
                )

            clean.contains("на год") ->
                rule(
                    "Rules for This year:\n" +
                        "• Strategic goals for the current year.",
                    "Правила для хаба «На год»:\n" +
                        "• Стратегические ориентиры и цели на текущий год."
                )

            clean.contains("на жизнь") ->
                rule(
                    "Rules for Life:\n" +
                        "• Long-term values, mission, and direction.",
                    "Правила для хаба «На жизнь»:\n" +
                        "• Фундаментальные жизненные ценности, миссия, мечты и долгосрочные ориентиры."
                )

            else -> null
        }
    }
}
