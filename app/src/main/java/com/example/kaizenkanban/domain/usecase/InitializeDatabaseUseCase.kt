package com.example.kaizenkanban.domain.usecase

import com.example.kaizenkanban.data.local.KairosPreferences
import com.example.kaizenkanban.domain.model.Board
import com.example.kaizenkanban.domain.model.Category
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

        // 1. OKR Board setup (clean typography, no emojis)
        val okrHubTitles = listOf("Сегодня", "Скоро", "Постоянно", "Мечта")
        var okrBoard = findOkrBoard(allBoards, allColumns)
        if (okrBoard == null) {
            repository.clearDefaultBoards()
            okrBoard = Board(
                id = UUID.randomUUID().toString(),
                projectId = mainProject.id,
                name = "OKR",
                isDefault = true
            )
            repository.insertBoard(okrBoard)

            val okrColumns = okrHubTitles.mapIndexed { index, title ->
                Column(
                    id = UUID.randomUUID().toString(),
                    boardId = okrBoard.id,
                    title = title,
                    position = index
                )
            }
            repository.insertColumns(okrColumns)
        } else {
            // Clean board name if it had emojis
            if (okrBoard.name.contains("🎯") || okrBoard.name != cleanEmoji(okrBoard.name)) {
                okrBoard = okrBoard.copy(name = cleanEmoji(okrBoard.name))
                repository.insertBoard(okrBoard)
            }

            val okrCols = allColumns.filter { it.boardId == okrBoard.id }.sortedBy { it.position }
            if (okrCols.isNotEmpty()) {
                val needReversal = okrCols.first().title.contains("Стратег", ignoreCase = true)
                val sourceList = if (needReversal) okrCols.reversed() else okrCols
                val cleanedCols = sourceList.mapIndexed { index, col ->
                    val mapped = mapOkrHubTitle(col.title)
                    // Only remap known legacy titles; never force-replace all hubs when one is legacy.
                    col.copy(title = mapped ?: cleanEmoji(col.title), position = index)
                }
                repository.insertColumns(cleanedCols)
                refreshDefaultRulesForColumns(
                    cleanedCols,
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
                ?: activeBoards.find { it.name.contains("OKR", ignoreCase = true) }
                ?: activeBoards.firstOrNull()
                ?: currentBoards.firstOrNull()
            targetDefault?.let { repository.setDefaultBoard(it.id) }
        } else if (currentBoards.count { it.isDefault } > 1) {
            val keep = activeBoards.find { it.isDefault && it.id == prefs?.okrBoardId }
                ?: activeBoards.find { it.isDefault && it.name.contains("OKR", ignoreCase = true) }
                ?: activeBoards.find { it.isDefault }
                ?: currentBoards.first { it.isDefault }
            repository.setDefaultBoard(keep.id)
        }

        // 4. Default categories
        if (repository.getCategoriesCount() == 0) {
            val defaultCategories = listOf(
                Category(UUID.randomUUID().toString(), "Размышление", 0xFF9D2CFF),
                Category(UUID.randomUUID().toString(), "Изучение", 0xFFFFB020),
                Category(UUID.randomUUID().toString(), "Контроль", 0xFFFF2A5F),
                Category(UUID.randomUUID().toString(), "Исполнение", 0xFF00D287),
                Category(UUID.randomUUID().toString(), "Без категории", 0xFFAAAAAA)
            )
            defaultCategories.forEach { repository.insertCategory(it) }
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

    private fun findOkrBoard(boards: List<Board>, columns: List<Column>): Board? {
        prefs?.okrBoardId?.let { id -> boards.find { it.id == id } }?.let { return it }
        boards.find { it.name.contains("OKR", ignoreCase = true) }?.let { return it }
        return boards.firstOrNull { board ->
            val hubs = columns.filter { it.boardId == board.id }
            hubs.size == 4 && hubs.count { mapOkrHubTitle(it.title) != null } >= 3
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
     * Canonical OKR hub titles (stored in Russian; [AppStrings.localized] maps for EN UI).
     * Legacy: Действия / Спринты / Метрики / Главная цель (and EN equivalents).
     */
    private fun mapOkrHubTitle(title: String): String? {
        val t = title.trim()
        val lower = t.lowercase()
        return when {
            lower == "сегодня" || lower == "today" ||
                lower == "сейчас" || lower == "now" ||
                lower.contains("действ") || lower == "actions" -> "Сегодня"

            lower == "скоро" || lower == "soon" ||
                lower.contains("спринт") || lower.contains("такт") || lower == "sprints" -> "Скоро"

            lower == "постоянно" || lower == "ongoing" || lower == "constantly" ||
                lower.contains("метрик") || lower.contains("результ") || lower == "metrics" -> "Постоянно"

            lower == "мечта" || lower == "dream" ||
                lower.contains("главная цель") || lower.contains("стратег") ||
                lower == "main goal" -> "Мечта"

            else -> null
        }
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

            clean == "сегодня" || clean == "today" ||
                clean == "сейчас" || clean == "now" || clean.contains("действ") || clean == "actions" ->
                rule(
                    "Rules for Today:\n" +
                        "• Concrete actions for today — one clear step you can do now.\n" +
                        "• Write as a verb: “Go to the gym”, “Do bench press”.",
                    "Правила для хаба «Сегодня»:\n" +
                        "• Конкретное действие на сегодня — один понятный шаг.\n" +
                        "• Пишите глаголом: «Сходить в зал», «Сделать жим»."
                )

            clean == "скоро" || clean == "soon" ||
                clean.contains("спринт") || clean.contains("такт") || clean == "sprints" ->
                rule(
                    "Rules for Soon:\n" +
                        "• The next stretch (about a week): e.g. “Gym 3 times this week”.\n" +
                        "• Pull today’s step into Today from here.",
                    "Правила для хаба «Скоро»:\n" +
                        "• Ближайший этап (примерно неделя): например «Зал 3 раза на этой неделе».\n" +
                        "• Отсюда берите сегодняшний шаг в «Сегодня»."
                )

            clean == "постоянно" || clean == "ongoing" || clean == "constantly" ||
                clean.contains("метрик") || clean.contains("результ") || clean == "metrics" ->
                rule(
                    "Rules for Ongoing:\n" +
                        "• The habit or practice that keeps you on course: e.g. “Go to the gym”.\n" +
                        "• Not one day — something you keep repeating.",
                    "Правила для хаба «Постоянно»:\n" +
                        "• Привычка или практика, которая держит курс: например «Ходить в зал».\n" +
                        "• Не разовое дело, а то, что повторяете регулярно."
                )

            clean == "мечта" || clean == "dream" ||
                clean.contains("главная цель") || clean.contains("стратег") || clean == "main goal" ->
                rule(
                    "Rules for Dream:\n" +
                        "• The big picture you are moving toward: e.g. “A fit body”.\n" +
                        "• Everything to the left is how you get there.",
                    "Правила для хаба «Мечта»:\n" +
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
                        "• The 7-day focus. Pull from here into Today.",
                    "Правила для хаба «На неделе»:\n" +
                        "• Фокус на 7 дней. То, что важно продвинуть на текущей неделе.\n" +
                        "• Из этого хаба задачи отбираются в хаб «Сегодня»."
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
