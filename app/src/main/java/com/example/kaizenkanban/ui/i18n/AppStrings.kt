package com.example.kaizenkanban.ui.i18n

import com.example.kaizenkanban.domain.model.Eisenhower

class AppStrings(private val appLanguage: AppLanguage) {
    private fun t(en: String, ru: String): String =
        if (appLanguage == AppLanguage.RU) ru else en

    fun quoted(value: String): String =
        if (appLanguage == AppLanguage.RU) "«$value»" else "\"$value\""

    fun localized(stored: String): String = NAME_MAP[appLanguage]?.get(stored.trim()) ?: stored

    fun eisenhowerTitle(quadrant: String?): String = when (quadrant) {
        Eisenhower.Q1 -> t("Urgent and important", "Срочно и важно")
        Eisenhower.Q2 -> t("Important, not urgent", "Важно, не срочно")
        Eisenhower.Q3 -> t("Urgent, not important", "Срочно, не важно")
        Eisenhower.Q4 -> t("Not urgent and not important", "Не срочно и не важно")
        else -> ""
    }

    val myProjects get() = t("My Projects", "Мои Проекты")
    val shareViaMessenger get() = t("Share via messenger", "Отправить в мессенджер")
    val exportFile get() = t("Export file", "Выгрузить файл")
    val importFile get() = t("Import file", "Загрузить файл")
    val calendar get() = t("Calendar", "Календарь")
    val createProject get() = t("Create project", "Создать проект")
    val newProject get() = t("New project", "Новый проект")
    val noProjectsYet get() = t("You don't have any projects yet", "У вас пока нет проектов")
    val projectName get() = t("Project name", "Название проекта")
    val create get() = t("Create", "Создать")
    val cancel get() = t("Cancel", "Отмена")
    val save get() = t("Save", "Сохранить")
    val add get() = t("Add", "Добавить")
    val close get() = t("Close", "Закрыть")
    val ok get() = t("OK", "ОК")
    val undo get() = t("Undo", "Отменить")
    val taskDeletedEverywhere get() = t("Task deleted from all boards", "Задача удалена со всех досок")
    val deleteTaskEverywhere get() = t(
        "This is the same task on every board where it appears. It will be removed everywhere.",
        "Это одна и та же задача на всех досках, где она есть. Она будет удалена везде."
    )
    val chooseHub get() = t("Choose a hub", "Выберите хаб")
    val done get() = t("Done", "Готово")
    val renameProject get() = t("Rename project", "Переименовать проект")
    val deleteProject get() = t("Delete project", "Удалить проект")
    val newBoard get() = t("New board", "Новая доска")
    val boardName get() = t("Board name", "Название доски")
    val renameBoard get() = t("Rename board", "Переименовать доску")
    val deleteBoard get() = t("Delete board", "Удалить доску")
    val deleteTask get() = t("Delete task", "Удалить задачу")
    val deleteWithContents get() = t("Everything inside will be deleted too.", "Вместе с ним удалится всё содержимое.")
    val cannotDeleteLastHub get() = t("A board needs at least one hub", "На доске должен остаться хотя бы один хаб")
    val cannotDeleteDefaultProject get() = t(
        "The default project can’t be deleted. Create another project first, or keep this one.",
        "Проект по умолчанию нельзя удалить. Создайте другой проект или оставьте этот."
    )
    val cannotDeleteDefaultBoard get() = t(
        "This board can’t be deleted. Archive it to hide it.",
        "Эту доску нельзя удалить. Скройте её в архив."
    )
    val cannotDeleteLastProject get() = t(
        "Keep at least one project",
        "Должен остаться хотя бы один проект"
    )
    val boardHasNoHubs get() = t("This board has no hubs yet", "На этой доске ещё нет хабов")
    val taskRemovedFromBoard get() = t("Removed from this board", "Убрано с этой доски")
    val removeFromThisBoard get() = t("Remove from board", "Убрать с доски")
    val focusTask get() = t("Focus", "Фокус")
    val clearFocus get() = t("Clear focus", "Сбросить фокус")
    val goToFocus get() = t("Go to focus", "К фокусу")
    val changeDueDate get() = t("Change due date", "Сменить срок")
    val linkedOnBoards get() = t("On other boards", "Ещё на досках")
    val createFirstProject get() = t("Create your first project", "Создайте первый проект")
    val hiddenLabel get() = t("Hidden", "Скрыта")
    val importOverwriteTitle get() = t("Import backup?", "Импортировать резервную копию?")
    val importOverwriteText get() = t(
        "Items with the same id will be overwritten. Continue?",
        "Записи с теми же id будут перезаписаны. Продолжить?"
    )
    val dragProject get() = t("Drag project", "Перетащить проект")
    val moveUp get() = t("Move up", "Переместить вверх")
    val moveDown get() = t("Move down", "Переместить вниз")
    val projectMenu get() = t("Project menu", "Меню проекта")
    val noBoards get() = t("No boards yet. Create the first one!", "Нет досок. Создайте первую!")
    val addBoard get() = t("Add board", "Добавить доску")
    val contacts get() = t("Contacts", "Контакты")
    val noContacts get() = t("No contacts yet. Add people on this project.", "Нет контактов. Добавьте людей проекта.")
    val addContact get() = t("Add contact", "Добавить контакт")
    val primaryBoard get() = t("Primary", "Основная")
    val boardMenu get() = t("Board menu", "Меню доски")
    val unsetPrimary get() = t("Remove from primary", "Убрать из основных")
    val setPrimary get() = t("Make primary", "Сделать основной")
    val call get() = t("Call", "Позвонить")
    val writeEmail get() = t("Email", "Написать")
    val contactMenu get() = t("Contact menu", "Меню контакта")
    val edit get() = t("Edit", "Редактировать")
    val deleteContact get() = t("Delete contact", "Удалить контакт")
    val newContact get() = t("New contact", "Новый контакт")
    val editContact get() = t("Edit contact", "Редактировать контакт")
    val name get() = t("Name", "Имя")
    val role get() = t("Role", "Роль")
    val phone get() = t("Phone", "Телефон")
    val email get() = t("Email", "Почта")
    val developer get() = t("Developer", "Разработчик")
    val developerHint get() = t("Questions and feedback", "Вопросы и обратная связь")
    val developerName get() = t("Ruslan", "Руслан")
    val telegram get() = t("Telegram", "Telegram")
    val developerEmailLabel get() = t("Email", "Почта")
    val developerEmailValue get() = "JIuMaPk@gmail.ru"
    val licenseNotice get() = t(
        "Kairos © 2026 Ruslan. Non-commercial license: free to use and share with attribution. Sale or commercial use requires the author’s written consent (JIuMaPk@gmail.ru).",
        "Kairos © 2026 Руслан. Некоммерческая лицензия: можно использовать и распространять с указанием автора. Продажа и коммерческое использование — только с письменного согласия автора (JIuMaPk@gmail.ru)."
    )
    val languageLabel get() = t("Language", "Язык")
    val settings get() = t("Settings", "Настройки")
    val appearance get() = t("Appearance", "Оформление")
    val editingPrefs get() = t("Editing", "Ввод")
    val dataAndBackup get() = t("Data & backup", "Данные и бэкап")
    val aboutSection get() = t("About", "О приложении")
    fun appVersionLabel(version: String) = t("Version $version", "Версия $version")
    val helpSection get() = t("Help", "Справка")
    val showOnboardingAgain get() = t("Show tips again", "Показать подсказки снова")
    val planningGuideTitle get() = t("How to plan", "Как планировать")
    val planningGuideOpen get() = t("Planning guide", "Гайд по планированию")
    val planningGuideGotIt get() = t("Got it", "Понятно")
    val planningGuideIntro get() = t(
        "Sometimes you stay busy all day and still feel like nothing really moved. Often the tasks and the bigger goal sit apart.\n\nIn Kairos you can keep today’s step and the bigger goal in one picture. Then it’s easier to choose what to do — and the day feels less wasted.\n\nYou don’t have to plan only this way. Make other projects and boards for work, home, study — whatever fits. Below is one approach that works well when you’re tired of checking off tasks with no sense of progress.",
        "Бывает так: весь день что-то делал, а к вечеру непонятно, к чему это было. Часто задачи сами по себе, а большая цель — где-то отдельно.\n\nВ Kairos можно держать и шаг на сегодня, и большую цель в одной картине. Тогда проще выбирать, за что браться, и меньше чувства зря потраченного дня.\n\nТак планировать не обязательно. Можно завести другие проекты и доски — под работу, дом, учёбу — как удобно. Ниже один вариант, который заходит, когда надоело закрывать задачи без ощущения прогресса."
    )
    val planningGuideHubsTitle get() = t("OKR hubs — from step to main goal", "Хабы OKR — от шага к главной цели")
    val planningGuideHubsBody get() = t(
        "Left to right: from a concrete step to the main goal.\n\n• Today — for this day (e.g. leave work on time)\n• Soon — next few days (e.g. two evenings this week without work)\n• Ongoing — what you want to keep (e.g. not working late every night)\n• Main Goal — what it’s all for (e.g. quiet evenings when your head is free)\n\nA normal to-do list is usually one flat layer. Here, while the main goal sits on the right, it’s easier not to drown in small stuff. And while “Today” sits on the left, the main goal doesn’t stay only in your head.",
        "Слева направо: от конкретного шага к главной цели.\n\n• Сегодня — на этот день (например, уйти с работы вовремя)\n• Скоро — на ближайшие дни (например, два вечера на неделе без работы)\n• Постоянно — то, что хочешь держать (например, не сидеть до ночи за задачами)\n• Главная цель — зачем всё это (например, спокойные вечера, когда голова свободна)\n\nВ обычном to-do всё лежит одним слоем. Тут другое: пока справа есть главная цель, легче не тонуть в мелочи. А пока слева есть «сегодня», цель не остаётся только в голове."
    )
    val planningGuideDayTitle get() = t("During the day", "Как пользоваться днём")
    val planningGuideDayBody get() = t(
        "1. Look at Today and Soon.\n2. Don’t pack the day full.\n3. Pick one task, turn on In progress, finish it.\n4. Then take the next one.\n\nThat’s the difference from jumping across ten tasks: with one in focus there’s less noise, and a better chance to finish what matters.\n\nDon’t keep new ideas in your head — quick-add them and return to the current task.\nWrite concrete steps: not “sort out work”, but “send the email” or “finish the report”.",
        "1. Посмотри «Сегодня» и «Скоро».\n2. Не набивай день под завязку.\n3. Выбери одно дело, включи «В работе» и сделай его.\n4. Потом бери следующее.\n\nВ этом отличие от прыжков по десяти задачам сразу: пока одно в фокусе, меньше шума в голове и выше шанс закрыть важное.\n\nНовую мысль не крути в уме — запиши быстрой задачей и вернись к текущему.\nПиши конкретно: не «разобраться с работой», а «отправить письмо» или «закрыть отчёт»."
    )
    val planningGuideRhythmTitle get() = t("Morning, evening, week", "Утро, вечер, неделя")
    val planningGuideRhythmBody get() = t(
        "Morning. What from Soon moves into Today? Turn on In progress.\nEvening. Mark what’s done. Move, delete, or leave the rest for tomorrow.\nOnce a week. Check Ongoing and Main Goal. Open the Eisenhower matrix: is everything only urgent while the important waits again?\n\nThis is where many people feel the difference: by week’s end you see not only how much you did, but where you moved.",
        "Утро. Что из «Скоро» перенесёшь в «Сегодня»? Включи «В работе».\nВечер. Отметь сделанное. Что не успел — перенеси, удали или оставь на завтра.\nРаз в неделю. Глянь «Постоянно» и «Главную цель». И матрицу Эйзенхауэра: не забито ли всё только срочным, пока важное снова ждёт.\n\nМногие здесь ловят разницу: к концу недели видно не только сколько сделал, а куда сдвинулся."
    )
    val planningGuideOwnTitle get() = t("If you want it your way", "Если хочешь по-своему")
    val planningGuideOwnBody get() = t(
        "Make your own project, hubs, and order — that’s fine.\nTry this approach on at least one main goal: you’ll feel faster how it differs from a plain list.",
        "Создай свой проект, свои хабы, свой порядок — нормально.\nА этот способ имеет смысл попробовать хотя бы на одной главной цели: так быстрее чувствуешь, чем он отличается от простого списка."
    )
    val planningGuideStartTitle get() = t("Where to start", "С чего начать")
    val planningGuideStartBody get() = t(
        "1. Write the Main Goal in a short line.\n2. In Ongoing — a habit toward it.\n3. In Soon — what to do in the next few days.\n4. In Today — one step for this day.\n5. Turn on In progress and do it.\n\nTry that for one evening — then compare the feeling with ticking off ten list items. Often you won’t want to go back to a bare list.",
        "1. Напиши «Главную цель» коротко.\n2. В «Постоянно» — привычку к ней.\n3. В «Скоро» — что сделать в ближайшие дни.\n4. В «Сегодня» — один шаг на этот день.\n5. Включи «В работе» и сделай его.\n\nСделай так один вечер — и сам сравнишь ощущение с обычным «вычеркнуть десять пунктов». Часто после этого возвращаться к голому списку уже не хочется."
    )
    val openSystemNotifications get() = t("Notification settings", "Настройки уведомлений")
    val notificationsDeniedBanner get() = t(
        "Reminders are off. Enable notifications to get due-date alerts.",
        "Напоминания выключены. Включите уведомления, чтобы получать сроки."
    )
    val enableNotifications get() = t("Enable", "Включить")
    val dismissBanner get() = t("Dismiss", "Скрыть")
    val searchProjectsPlaceholder get() = t("Search tasks…", "Поиск задач…")
    val calendarMonth get() = t("Month", "Месяц")
    val calendarList get() = t("List", "Список")
    val noTasksOnDay get() = t("No tasks on this day", "Нет задач на этот день")
    val addTaskForDay get() = t("Add task for this day", "Задача на этот день")
    val hiddenTasksBadge get() = t("Hidden", "Скрытые")
    val onboardingTitle4 get() = t("Swipe & hide", "Свайп и скрытие")
    val onboardingText4 get() = t(
        "Swipe a card left for More and Delete. Hide clutter via More; tap the eye on a hub to show hidden again.",
        "Свайп влево — Ещё и Удалить. Скрыть задачу можно в Ещё; глаз на хабе снова показывает скрытые."
    )
    val onboardingTitle5 get() = t("Eisenhower colors", "Цвета Эйзенхауэра")
    val onboardingText5 get() = t(
        "Q1–Q4 buttons paint the task. Compact mode fills the card in that color.",
        "Кнопки Q1–Q4 красят задачу. В компактном режиме карточка заливается этим цветом."
    )
    val board get() = t("Board", "Доска")
    val hideBoardsList get() = t("Hide board list", "Скрыть список досок")
    val showBoardsList get() = t("Show board list", "Показать список досок")
    val hubs get() = t("Hubs", "Хабы")
    val addHub get() = t("Add hub", "Добавить хаб")
    val hubOrder get() = t("Hub order", "Порядок хабов")
    val manageStatuses get() = t("Manage statuses", "Управление статусами")
    val hideMenu get() = t("Hide menu", "Скрыть меню")
    val moreActions get() = t("More", "Ещё")
    val showMenu get() = t("Show menu", "Показать меню")
    val projects get() = t("Projects", "Проекты")
    val setPrimaryHub get() = t("Set as primary hub", "Сделать основным хабом")
    val primaryHub get() = t("Primary hub", "Основной хаб")
    val executionMode get() = t("Execution mode", "Режим исполнения")
    val planningMode get() = t("Planning mode", "Режим планирования")
    val addTask get() = t("Add task", "Добавить задачу")
    val newHub get() = t("New hub", "Новый хаб")
    val newTask get() = t("New task", "Новая задача")
    val task get() = t("Task", "Задача")
    val enterAddsTaskTitle get() = t("Enter adds a task", "Enter добавляет задачу")
    val enterSavesTask get() = t("The Enter key saves the task", "Клавиша Enter сохраняет задачу")
    val enterAddsNewline get() = t("Enter inserts a new line", "Enter переносит на новую строку")
    val hub get() = t("Hub", "Хаб")
    val hubLabel get() = t("Hub:", "Хаб:")
    val eisenhowerButtons get() = t("Eisenhower matrix buttons", "Кнопки матрицы Эйзенхауэра")
    val setDueDate get() = t("Set due date", "Задать срок")
    val taskCategory get() = t("Task category:", "Категория задачи:")
    val noStatusSelected get() = t("No status selected", "Без статуса (не выбран)")
    val hubName get() = t("Hub name", "Название хаба")
    val newName get() = t("New name", "Новое название")
    val renameHub get() = t("Rename hub", "Переименовать хаб")
    val deleteHub get() = t("Delete hub", "Удалить хаб")
    val editTask get() = t("Edit task", "Редактировать задачу")
    val changeHubBoard get() = t("Change hub / board", "Сменить хаб / доску")
    val changeHub get() = t("Change hub", "Сменить хаб")
    val dropToMoveBoard get() = t("Drag here to move to another board:", "Перетащите сюда для переноса на другую доску:")
    val releaseToMove get() = t("Release to move", "Отпустите для переноса")
    val addNewStatus get() = t("Add a new status", "Добавить новый статус")
    val statusName get() = t("Status name", "Название статуса")
    val delete get() = t("Delete", "Удалить")
    val hubRules get() = t("Rules and comments", "Правила и комментарии")
    val hideHiddenTasks get() = t("Hide hidden tasks", "Скрыть спрятанные задачи")
    val showHiddenTasks get() = t("Show hidden", "Показать скрытые")
    val noTasks get() = t("No tasks", "Нет задач")
    val allTasks get() = t("All tasks", "Все задачи")
    fun allTasksHeader(count: Int) = t("All tasks ($count)", "Все задачи ($count)")
    fun allHubsHeader(count: Int) = t("All hubs ($count)", "Все хабы ($count)")
    fun allBoardsHeader(count: Int) = t("All boards ($count)", "Все доски ($count)")
    val noHubs get() = t("No hubs", "Нет хабов")
    val hide get() = t("Hide", "Скрыть")
    val show get() = t("Show", "Показать")
    val clear get() = t("Clear", "Очистить")
    val includedInHub get() = t("Included in hub:", "Включена в хаб:")
    val taskOrigin get() = t("Task origin", "Откуда задача")
    val relatedBoards get() = t("Linked boards", "Связанные доски")
    val comments get() = t("Comments", "Комментарии")
    val showTask get() = t("Show task", "Показать задачу")
    val hideTask get() = t("Hide task", "Скрыть задачу")
    val inProgress get() = t("In progress", "В работе")
    val completed get() = t("Done", "Выполнено")
    val nowInProgress get() = t("NOW IN PROGRESS", "СЕЙЧАС В РАБОТЕ")
    val swipeForActions get() = t("Swipe left for actions", "Свайп влево для действий")
    val matrix get() = t("Matrix:", "Матрица:")
    val previousHub get() = t("Previous hub", "Предыдущий хаб")
    val nextHub get() = t("Next hub", "Следующий хаб")
    val noCommentsYet get() = t("No comments yet.\nWrite the first one!", "Пока нет комментариев.\nНапишите первый!")
    val writeComment get() = t("Write a comment...", "Написать комментарий...")
    val send get() = t("Send", "Отправить")
    val deleteComment get() = t("Delete comment", "Удалить комментарий")
    val noHubRulesYet get() = t("No rules or comments for this hub yet.\nWrite the first rule below!", "Пока нет правил или комментариев к этому хабу.\nНапишите первое правило ниже!")
    val writeRule get() = t("Write a rule or note...", "Написать правило или заметку...")
    val copyTask get() = t("Also show", "Ещё и здесь")
    val moveTask get() = t("Move", "Перенос")
    val move get() = t("Move", "Перенос")
    val copy get() = t("Link", "Связь")
    val copyAction get() = t("Link", "Связь")
    val alsoShowHint get() = t(
        "Same task on another board. Edits and delete apply everywhere.",
        "Та же задача на другой доске. Правки и удаление — везде."
    )
    val quickJump get() = t("Quick jump:", "Быстрый переход:")
    val copyToHubOnBoard get() = t("Link to hub here:", "Связать с хабом здесь:")
    val hubsOnThisBoard get() = t("Hubs on this board:", "Хабы этой доски:")
    val here get() = t("Here", "Здесь")
    val copyToOtherBoard get() = t("Link on another board:", "Связать на другой доске:")
    val orOtherBoard get() = t("Other board:", "Другая доска:")
    val left get() = t("Left", "Влево")
    val right get() = t("Right", "Вправо")
    val back get() = t("Back", "Назад")
    val hideCompleted get() = t("Hide done", "Скрыть готовые")
    val showCompleted get() = t("Show done", "Показать готовые")
    val searchTasks get() = t("Search tasks", "Поиск задач")
    val filterOverdue get() = t("Overdue", "Просрочено")
    val filterActive get() = t("Active", "В деле")
    val filterDone get() = t("Done", "Готово")
    val filterAll get() = t("All", "Все")
    val noMatchingTasks get() = t("No matching tasks", "Нет подходящих задач")
    val noTasksWithDueDate get() = t("No tasks with a due date", "Нет задач с установленным сроком")
    val dueOverdue get() = t("Overdue", "Просрочено")
    val dueToday get() = t("Today", "Сегодня")
    val dueTomorrow get() = t("Tomorrow", "Завтра")
    val dueThisWeek get() = t("This week", "На неделе")
    val dueLater get() = t("Later", "Позже")
    val filterToday get() = t("Today", "Сегодня")
    val pickDate get() = t("Date", "Дата")
    val clearDueDate get() = t("No date", "Без срока")
    val markDone get() = t("Done", "Готово")
    val reminderChannelName get() = t("Due dates", "Сроки задач")
    val reminderChannelDesc get() = t(
        "Reminders on the due day at your chosen time, and again while overdue",
        "Напоминания в день срока в выбранное время и повторно, пока задача просрочена"
    )
    val reminderDefaultTime get() = t("Default reminder time", "Время напоминания")
    val reminderDefaultTimeHint get() = t(
        "Used for all tasks unless a task has its own time",
        "Для всех задач, если у задачи не задано своё"
    )
    val reminderTaskTime get() = t("Reminder time", "Время напоминания")
    val reminderUseDefault get() = t("Use default", "Как в настройках")
    val reminderPickTime get() = t("Pick time", "Выбрать время")
    fun reminderAt(time: String) = t("Reminder $time", "Напоминание $time")
    fun reminderCustom(time: String) = t("Custom $time", "Своё $time")
    val reminderTitle get() = t("Task due today", "Срок задачи сегодня")
    val reminderOverdueTitle get() = t("Still overdue", "Всё ещё просрочено")
    val widgetTitle get() = t("Kairos", "Kairos")
    val widgetFocus get() = t("Focus", "В работе")
    val widgetDueSection get() = t("Today / overdue", "Сегодня / просрочено")
    val widgetEmpty get() = t("No focus or due tasks", "Нет фокуса и срочных задач")
    val widgetFocusOnlyTitle get() = t("Kairos Focus", "Kairos Фокус")
    val widgetFocusOnlyDesc get() = t("Focus task only", "Только задача в работе")
    val archiveBoard get() = t("Archive board", "В архив")
    val unarchiveBoard get() = t("Restore board", "Вернуть из архива")
    val showArchivedBoards get() = t("Show archived", "Показать архив")
    val hideArchivedBoards get() = t("Hide archived", "Скрыть архив")
    val archivedLabel get() = t("Archived", "Архив")
    val boardTemplate get() = t("Template", "Шаблон")
    val templateInbox get() = t("Inbox only", "Только Inbox")
    val templateFlow get() = t("Flow", "Поток")
    val templateEisenhower get() = t("Eisenhower", "Эйзенхауэр")
    val exportProject get() = t("Export project", "Экспорт проекта")
    val shareProject get() = t("Share project", "Поделиться проектом")
    val themeSystem get() = t("System", "Система")
    val themeLight get() = t("Light", "Светлая")
    val themeDark get() = t("Dark", "Тёмная")
    val themeLabel get() = t("Theme", "Тема")
    val eisenhowerPaletteLabel get() = t("Eisenhower colors", "Цвета Эйзенхауэра")
    val eisenhowerPaletteHint get() = t(
        "Applies to Q1–Q4 buttons and task backgrounds",
        "Для кнопок Q1–Q4 и фона задач"
    )
    val eisenhowerPaletteBerry get() = t("Berry", "Ягодная")
    val eisenhowerPaletteSunset get() = t("Sunset", "Закат")
    val eisenhowerPaletteOcean get() = t("Ocean", "Океан")
    val weekStats get() = t("This week", "На этой неделе")
    fun weekCompleted(count: Int) = t(
        "$count done",
        "$count готово"
    )
    fun weekCreated(count: Int) = t(
        "$count created",
        "$count создано"
    )
    val overdueOnly get() = t("Overdue only", "Только просроченные")
    val sortHubByImportance get() = t("Sort hub by importance", "Сортировать хаб по важности")
    val sortHubByDue get() = t("Sort hub by due date", "Сортировать хаб по сроку")
    val hubSortedByImportance get() = t("Hub sorted by importance", "Хаб отсортирован по важности")
    val hubSortedByDue get() = t("Hub sorted by due date", "Хаб отсортирован по сроку")
    val hubSortNothingToSort get() = t("No tasks to sort in this hub", "В этом хабе нечего сортировать")
    val markDoneSwipe get() = t("Done", "Готово")
    val taskMarkedDone get() = t("Marked done", "Отмечено готовым")
    val repeatNone get() = t("No repeat", "Без повтора")
    val repeatDaily get() = t("Daily", "Ежедневно")
    val repeatWeekly get() = t("Weekly", "Еженедельно")
    val repeatLabel get() = t("Repeat", "Повтор")
    val collapseProjects get() = t("Collapse all", "Свернуть все")
    val expandProjects get() = t("Expand all", "Развернуть все")
    val onboardingSkip get() = t("Skip", "Пропустить")
    val onboardingNext get() = t("Next", "Далее")
    val onboardingDone get() = t("Got it", "Понятно")
    val onboardingTitle1 get() = t("Boards & hubs", "Доски и хабы")
    val onboardingText1 get() = t(
        "Star a primary board to open it on launch. Add hubs with templates.",
        "Отметьте основную доску — она откроется при запуске. Хабы можно из шаблонов."
    )
    val onboardingTitle2 get() = t("Due dates & reminders", "Сроки и напоминания")
    val onboardingText2 get() = t(
        "Use Today/Tomorrow chips. Morning reminders include Done and Tomorrow.",
        "Чипы Сегодня/Завтра задают срок. Утром придёт напоминание с Готово и Завтра."
    )
    val onboardingTitle3 get() = t("Focus & widgets", "Фокус и виджеты")
    val onboardingText3 get() = t(
        "Bolt marks the one task in progress. Add home widgets for focus and due list.",
        "Молния — задача в работе. На домашний экран можно вынести виджеты."
    )
    val widgetQuickAdd get() = t("Quick add", "Быстрая задача")
    val quickAddTask get() = t("Quick add", "Быстрая задача")
    val quickAddHint get() = t("Adds to the primary board inbox", "В Inbox основной доски")
    fun quickAddGoesTo(board: String, hub: String) = t(
        "Adds to $board · $hub",
        "Попадёт в $board · $hub"
    )
    val quickAddDestination get() = t("Quick add destination", "Куда падает быстрая задача")
    val quickAddUsePrimary get() = t("Primary board (default)", "Основная доска (по умолчанию)")
    val quickAddPickBoard get() = t("Board", "Доска")
    val quickAddPickHub get() = t("Hub", "Хаб")
    val noPrimaryBoard get() = t("Set a primary board first", "Сначала сделайте доску основной")
    val inboxHub get() = t("Inbox", "Входящие")
    val taskAdded get() = t("Task added", "Задача добавлена")
    val notificationsPermissionHint get() = t(
        "Allow notifications to get due-date reminders",
        "Разрешите уведомления, чтобы получать напоминания о сроках"
    )
    val shareTasksVia get() = t("Share tasks via...", "Отправить задачи через...")
    val cannotOpenDialer get() = t("Could not open the phone app", "Не удалось открыть звонок")
    val cannotOpenEmail get() = t("Could not open email", "Не удалось открыть почту")
    val cannotOpenLink get() = t("Could not open the link", "Не удалось открыть ссылку")
    val invalidImportLink get() = t("Invalid import link", "Неверная ссылка импорта")
    val exportSuccess get() = t("Tasks exported successfully!", "Задачи успешно экспортированы!")
    val clearCompletedTitle get() = t("Clear completed?", "Очистить выполненные?")
    val removedFromMatrix get() = t("Removed from the Eisenhower matrix", "Удалено из матрицы Эйзенхауэра")

    fun saveError(details: String?) = t("Save error: $details", "Ошибка сохранения: $details")
    fun importError(details: String?) = t(
        "Import error: ${details ?: "invalid file"}",
        "Ошибка импорта: ${details ?: "некорректный файл"}"
    )
    fun importSuccess(count: Int) = t("Imported $count tasks!", "Успешно загружено $count задач!")
    fun sharePrepareError(details: String?) = t("Could not prepare sharing: $details", "Ошибка подготовки отправки: $details")
    fun importedFromMessenger(count: Int) = t("Imported $count tasks from messenger!", "Импортировано $count задач из мессенджера!")
    fun importedFromFile(count: Int) = t("Imported $count tasks from file!", "Импортировано $count задач из файла!")
    fun taskMovedToBoard(name: String) = t("Task moved to board ${quoted(name)}", "Задача перенесена на доску ${quoted(name)}")
    fun inProgressToast(title: String) = t("In progress: ${quoted(title)}", "В работе: ${quoted(title)}")
    fun commentsCount(count: Int) = t("Comments ($count)", "Комментарии ($count)")
    fun taskMovedToHub(name: String) = t("Task moved to ${quoted(name)}", "Задача перемещена в ${quoted(name)}")
    fun taskAlsoOnHub(name: String) = t("Task is also shown in ${quoted(name)}", "Задача также отображается в ${quoted(name)}")
    fun taskAlsoOnBoard(name: String) = t("Task is also shown on board ${quoted(name)}", "Задача также отображается на доске ${quoted(name)}")
    val mirrorReorderHint get() = t(
        "Mirrored tasks keep the order of their home board",
        "Зеркальные задачи сохраняют порядок домашней доски"
    )
    fun addedToMatrix(quadrant: String?) = t(
        "Added to matrix: ${quoted(eisenhowerTitle(quadrant))}",
        "Добавлено в матрицу: ${quoted(eisenhowerTitle(quadrant))}"
    )
    fun movedToHub(name: String) = t("Moved to ${quoted(name)}", "Перемещено в ${quoted(name)}")
    fun completedCount(count: Int) = t("Done ($count)", "Выполнено ($count)")
    fun clearCompletedText(count: Int, hub: String) = t(
        "Delete these $count completed tasks from hub ${quoted(hub)}? They will be removed from every board.",
        "Удалить эти выполненные задачи ($count) из хаба ${quoted(hub)}? Они пропадут со всех досок."
    )
    fun closedAt(time: String) = t("Closed: $time", "Закрыто: $time")
    fun hubWithName(name: String) = t("Hub: ${quoted(name)}", "Хаб: ${quoted(name)}")
    fun toHub(name: String) = t("To $name", "В $name")

    private companion object {
        val NAME_MAP = mapOf(
            AppLanguage.EN to mapOf(
                "Сегодня" to "Today",
                "Сейчас" to "Today",
                "Скоро" to "Soon",
                "Постоянно" to "Ongoing",
                "Мечта" to "Main Goal",
                "Действия" to "Actions",
                "Спринты" to "Sprints",
                "Метрики" to "Metrics",
                "Главная цель" to "Main Goal",
                "Срочно и важно" to "Urgent and important",
                "Важно, не срочно" to "Important, not urgent",
                "Срочно, не важно" to "Urgent, not important",
                "Не срочно и не важно" to "Not urgent and not important",
                "Матрица Эйзенхауэра" to "Eisenhower Matrix",
                "Размышление" to "Reflection",
                "Изучение" to "Learning",
                "Контроль" to "Control",
                "Исполнение" to "Execution",
                "Без категории" to "Uncategorized",
                "На неделе" to "This week",
                "Входящие" to "Inbox",
                "Inbox" to "Inbox"
            ),
            AppLanguage.RU to mapOf(
                "Today" to "Сегодня",
                "Now" to "Сегодня",
                "Soon" to "Скоро",
                "Ongoing" to "Постоянно",
                "Constantly" to "Постоянно",
                "Dream" to "Главная цель",
                "Actions" to "Действия",
                "Sprints" to "Спринты",
                "Metrics" to "Метрики",
                "Main Goal" to "Главная цель",
                "Urgent and important" to "Срочно и важно",
                "Important, not urgent" to "Важно, не срочно",
                "Urgent, not important" to "Срочно, не важно",
                "Not urgent and not important" to "Не срочно и не важно",
                "Eisenhower Matrix" to "Матрица Эйзенхауэра",
                "Reflection" to "Размышление",
                "Learning" to "Изучение",
                "Control" to "Контроль",
                "Execution" to "Исполнение",
                "Uncategorized" to "Без категории",
                "This week" to "На неделе",
                "Inbox" to "Входящие"
            )
        )
    }
}
