package com.example.kaizenkanban.ui.i18n

import com.example.kaizenkanban.domain.model.Eisenhower
import com.example.kaizenkanban.domain.model.TaskCardKind

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

    fun eisenhowerTitleShort(quadrant: String?): String = when (quadrant) {
        Eisenhower.Q1 -> t("Urgent\nImportant", "Срочно\nВажно")
        Eisenhower.Q2 -> t("Important\nNot urgent", "Важно\nНе срочно")
        Eisenhower.Q3 -> t("Urgent\nNot important", "Срочно\nНе важно")
        Eisenhower.Q4 -> t("Not urgent\nNot important", "Не срочно\nНе важно")
        else -> ""
    }

    val myProjects get() = t("My Projects", "Мои Проекты")
    val shareViaMessenger get() = t("Share via messenger", "Отправить в мессенджер")
    val exportFile get() = t("Export file", "Выгрузить файл")
    val importFile get() = t("Import file", "Загрузить файл")
    val calendar get() = t("Calendar", "Календарь")
    val notes get() = t("Diary", "Дневник")
    val notesEmpty get() = t("Nothing written yet", "Пока пусто")
    val notesEmptyHint get() = t("Tap + to write the first line", "Нажмите +, чтобы оставить первую строку")
    val newNote get() = t("New entry", "Новая запись")
    val editNote get() = t("Entry", "Запись")
    val noteTitleHint get() = t("Title", "Заголовок")
    val noteBodyHint get() = t("Write a line…", "Новая строка…")
    val noteSearchHint get() = t("Search the diary…", "Поиск по дневнику…")
    val noteDeleteConfirm get() = t("Delete this entry?", "Удалить эту запись?")
    val notePin get() = t("Pin to top", "Закрепить сверху")
    val noteUnpin get() = t("Unpin", "Открепить")
    val noteReminder get() = t("Reminder", "Напоминание")
    val noteReminderClear get() = t("Remove reminder", "Убрать напоминание")
    val noteLinkTask get() = t("Link to goal or task", "Связать с целью или задачей")
    val noteUnlinkTask get() = t("Remove link", "Убрать связь")
    val noteConvertToTask get() = t("Turn into task", "Сделать задачей")
    val noteConverted get() = t("Entry became a task", "Запись стала задачей")
    val notePhotos get() = t("Photos", "Фото")
    val notePhotosLimit get() = t("Photo limit reached", "Достигнут лимит фото")
    val notePhotoMissing get() = t("Photo unavailable", "Фото недоступно")
    val noteVoice get() = t("Dictate", "Диктовать")
    val noteMoveUp get() = t("Move up", "Выше")
    val noteMoveDown get() = t("Move down", "Ниже")
    val calendarSearchHint get() = t("Search dated tasks…", "Поиск задач со сроком…")
    val calendarProjectedBadge get() = t("Scheduled habit", "Запланированная привычка")
    val calendarFocusClear get() = t("Clear highlight", "Снять выделение")
    fun calendarFocusOn(title: String) = t("Highlighted: $title", "Выделено: $title")
    val calendarLegendDays get() = t("Task days", "Дни задачи")
    val calendarLegendDeadline get() = t("Deadline", "Дедлайн")
    val calendarOpenTask get() = t("Open", "Открыть")
    val recurringIntervalAny get() = t("Any number from 1 to 999", "Любое число от 1 до 999")
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
    val changeDueDate get() = t("Change Start", "Сменить «Приступить»")
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

    // Recurring templates (project-owned)
    val recurringTitle get() = t("Recurring", "Регулярные")
    val eisenhowerMatrixTitle get() = t("Priorities", "Приоритеты")
    val eisenhowerAxisUrgent get() = t("Urgent", "Срочно")
    val eisenhowerAxisNotUrgent get() = t("Not urgent", "Не срочно")
    fun eisenhowerCount(n: Int) = t("$n tasks", "$n задач")
    val eisenhowerEmptyCell get() = t("Empty", "Пусто")
    val eisenhowerMove get() = t("Move in matrix", "Переместить в матрице")
    val eisenhowerClearQuadrant get() = t("Remove from matrix", "Убрать из матрицы")
    val recurringAdd get() = t("Add recurring", "Добавить регулярную")
    val recurringEdit get() = t("Edit recurring", "Регулярная задача")
    val recurringEmpty get() = t("No recurring tasks yet", "Пока нет регулярных задач")
    val recurringEmptyHint get() = t(
        "Templates live with the project. Instances appear in the chosen hub.",
        "Шаблоны принадлежат проекту. Экземпляры появляются в выбранном хабе."
    )
    val recurringRhythm get() = t("Rhythm", "Ритм")
    val recurringDaily get() = t("Every day", "Каждый день")
    val recurringWeekdays get() = t("On weekdays", "По дням")
    val recurringEveryNDays get() = t("Every N days", "Каждые N дней")
    fun recurringEveryNDaysLabel(n: Int) = t("Every $n days", "Каждые $n дн.")
    val recurringIntervalDays get() = t("Interval (days)", "Интервал (дни)")
    val recurringDestination get() = t("Create instances in", "Куда создавать")
    val recurringTime get() = t("Reminder times", "Время напоминания")
    val recurringTimePick get() = t("Pick reminder time", "Выбрать время напоминания")
    val recurringTimeAdd get() = t("Start", "приступить")
    val recurringTimeHint get() = t(
        "When to remind for each instance. Each time → a separate task. Create time is only in Settings. Default matches the app reminder.",
        "Когда напоминать по каждому экземпляру. Каждое время — отдельная задача. Время создания — только в Настройках. По умолчанию как напоминание в приложении."
    )
    val recurringStartsOn get() = t("Start from", "Начинать с")
    val recurringStartsOnHint get() = t(
        "Instances are created only on and after this date. Default is today.",
        "Экземпляры создаются только с этой даты. По умолчанию — сегодня."
    )
    val recurringDeadlineHint get() = t(
        "Days and hours after each instance starts. Leave empty for no deadline.",
        "Дни и часы после старта каждого экземпляра. Пусто — без дедлайна."
    )
    val recurringDeadlineDays get() = t("Days", "Дней")
    val recurringDeadlineHours get() = t("Hours", "Часов")
    val recurringDeadlineDaysShort get() = t("d", "дн.")
    val recurringDeadlineHoursShort get() = t("h", "ч.")
    val recurringDeadlineClear get() = t("Remove deadline", "Убрать дедлайн")
    val recurringActive get() = t("Active", "Активна")
    val recurringNoBoards get() = t("Add a board in this project first", "Сначала добавьте доску в проект")
    val recurringDeleteConfirm get() = t(
        "Delete this recurring template? Today's instances already created stay as normal tasks.",
        "Удалить этот шаблон? Уже созданные на сегодня задачи останутся обычными."
    )
    val weekdayMon get() = t("Mon", "Пн")
    val weekdayTue get() = t("Tue", "Вт")
    val weekdayWed get() = t("Wed", "Ср")
    val weekdayThu get() = t("Thu", "Чт")
    val weekdayFri get() = t("Fri", "Пт")
    val weekdaySat get() = t("Sat", "Сб")
    val weekdaySun get() = t("Sun", "Вс")
    val taskTitleLabel get() = t("Title", "Название")
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
    val planningGuideHubsTitle get() = t(
        "Goal ladder hubs — from step to main goal",
        "Хабы лестницы целей — от шага к главной цели"
    )
    val planningGuideHubsBody get() = t(
        "Left to right: from a parked wait to the main goal.\n\n• Waiting on others — blocked on someone else\n• Do — for this day (e.g. leave work on time)\n• Planning — next few days (e.g. two evenings this week without work)\n• Holding course — what you want to keep (e.g. not working late every night)\n• Main Goal — what it is for (e.g. quiet evenings when your head is free).\n\nA normal to-do list is usually one flat layer. Here, while the main goal sits on the right, it is easier not to drown in small stuff. And while Do sits near the left, the main goal does not stay only in your head.",
        "Слева направо: от ожидания ответа к главной цели.\n\n• На стороне контрагента — ждёт другого человека\n• Сделать — на этот день (например, уйти с работы вовремя)\n• Планирую — на ближайшие дни (например, два вечера на неделе без работы)\n• Держу курс — то, что хочешь держать (например, не сидеть до ночи за задачами)\n• Главная цель — зачем всё это (например, спокойные вечера, когда голова свободна)\n\nВ обычном to-do всё лежит одним слоем. Тут другое: пока справа есть главная цель, легче не тонуть в мелочи. А пока слева есть «Сделать», цель не остаётся только в голове."
    )
    val planningGuideDayTitle get() = t("During the day", "Как пользоваться днём")
    val planningGuideDayBody get() = t(
        "1. Look at Do and Planning.\n2. Don’t pack the day full.\n3. Pick one task and finish it.\n4. Then take the next one.\n\nThat’s the difference from jumping across ten tasks: with one in focus there’s less noise, and a better chance to finish what matters.\n\nDon’t keep new ideas in your head — quick-add a task, or park them in the project Diary (with a photo or reminder if needed), then return to the current work.\nWrite concrete steps: not “sort out work”, but “send the email” or “finish the report”.",
        "1. Посмотри «Сделать» и «Планирую».\n2. Не набивай день под завязку.\n3. Выбери одно дело и сделай его.\n4. Потом бери следующее.\n\nВ этом отличие от прыжков по десяти задачам сразу: пока одно в фокусе, меньше шума в голове и выше шанс закрыть важное.\n\nНовую мысль не крути в уме — быстрая задача или запись в «Дневнике» проекта (с фото или напоминанием при необходимости), и снова к текущему.\nПиши конкретно: не «разобраться с работой», а «отправить письмо» или «закрыть отчёт»."
    )
    val planningGuideRhythmTitle get() = t("Morning, evening, week", "Утро, вечер, неделя")
    val planningGuideRhythmBody get() = t(
        "Morning. What from Planning moves into Do?\nEvening. Mark what’s done. Move, delete, or leave the rest for tomorrow.\nOnce a week. Check Holding course and Main Goal. Open Priorities: is everything only urgent while the important waits again?\n\nThis is where many people feel the difference: by week’s end you see not only how much you did, but where you moved.",
        "Утро. Что из «Планирую» перенесёшь в «Сделать»?\nВечер. Отметь сделанное. Что не успел — перенеси, удали или оставь на завтра.\nРаз в неделю. Глянь «Держу курс» и «Главную цель». И «Приоритеты»: не забито ли всё только срочным, пока важное снова ждёт.\n\nМногие здесь ловят разницу: к концу недели видно не только сколько сделал, а куда сдвинулся."
    )
    val planningGuideOwnTitle get() = t("If you want it your way", "Если хочешь по-своему")
    val planningGuideOwnBody get() = t(
        "Make your own project, hubs, and order — that’s fine. The project Diary holds a title, lines, photos, and reminders that are not tasks yet; you can turn an entry into a task later.\nTry this approach on at least one main goal: you’ll feel faster how it differs from a plain list.",
        "Создай свой проект, свои хабы, свой порядок — нормально. В «Дневнике» проекта — заголовок, строки, фото и напоминания без обязательства; запись потом можно сделать задачей.\nА этот способ имеет смысл попробовать хотя бы на одной главной цели: так быстрее чувствуешь, чем он отличается от простого списка."
    )
    val planningGuideStartTitle get() = t("Where to start", "С чего начать")
    val planningGuideStartBody get() = t(
        "1. Write the Main Goal in a short line.\n2. In Holding course — a habit toward it.\n3. In Planning — what to do in the next few days.\n4. In Do — one step for this day, and do it.\n\nTry that for one evening — then compare the feeling with ticking off ten list items. Often you won’t want to go back to a bare list.",
        "1. Напиши «Главную цель» коротко.\n2. В «Держу курс» — привычку к ней.\n3. В «Планирую» — что сделать в ближайшие дни.\n4. В «Сделать» — один шаг на этот день и сделай его.\n\nСделай так один вечер — и сам сравнишь ощущение с обычным «вычеркнуть десять пунктов». Часто после этого возвращаться к голому списку уже не хочется."
    )

    val gesturesGuideOpen get() = t("Taps and swipes", "Нажатия и жесты")
    val gesturesGuideTitle get() = t("How do I…?", "Как сделать…?")
    val gesturesGuideOpenProjectsTitle get() = t(
        "How do I open all my projects?",
        "Как открыть все проекты?"
    )
    val gesturesGuideOpenProjectsBody get() = t(
        "Tap the back arrow in the top menu. If the menu is hidden, pull the list down or double-tap the hub title.",
        "Нажмите стрелку назад в верхнем меню. Если меню скрыто — потяните список вниз или дважды нажмите на название хаба."
    )
    val gesturesGuideSwitchListsTitle get() = t(
        "How do I switch to another list of tasks?",
        "Как перейти к другому списку задач?"
    )
    val gesturesGuideSwitchListsBody get() = t(
        "Swipe left or right between hubs.",
        "Листайте хабы влево или вправо."
    )
    val gesturesGuideShowMenuTitle get() = t(
        "How do I show or hide the top menu?",
        "Как показать или спрятать верхнее меню?"
    )
    val gesturesGuideShowMenuBody get() = t(
        "At the top of the list, pull down to show the menu and pull up to hide it.\nDouble-tap the hub title — the menu hides or shows again.",
        "Вверху списка потяните вниз — меню появится, вверх — спрячется.\nДважды нажмите на название хаба — меню скроется или снова покажется."
    )
    val gesturesGuideMoveTaskTitle get() = t(
        "How do I move a task?",
        "Как переместить задачу?"
    )
    val gesturesGuideMoveTaskBody get() = t(
        "Hold the card and move your finger to drag it to a new place or another hub. A hold without moving selects the card.",
        "Удерживайте карточку и сдвиньте палец — она перетащится в нужное место или в другой хаб. Удержание без сдвига выделяет карточку."
    )
    val gesturesGuideEditTaskTitle get() = t(
        "How do I rename or edit a task?",
        "Как изменить задачу?"
    )
    val gesturesGuideEditTaskBody get() = t(
        "Double-tap the task text to edit the title, Start, Finish, Timer, and the rest. On a finished or not-done task, a double-tap opens the criteria. On a workout, shopping, or contact card, one tap opens its lines.",
        "Дважды нажмите на текст — откроется правка: название, «Приступить», «Закончить», «Таймер» и остальное. У готовой или несделанной задачи двойное нажатие открывает критерии. У тренировки, покупок и связи одно нажатие открывает строки карточки."
    )
    val gesturesGuideLongTitleTitle get() = t(
        "How do I mark a task done or not done?",
        "Как отметить задачу готовой или несделанной?"
    )
    val gesturesGuideLongTitleBody get() = t(
        "On the card, the check marks it done and the cross marks it not done. Tap the same button again to clear the mark. The title wraps on the card.",
        "На карточке галочка отмечает готовой, крестик — несделанной. Повторное нажатие той же кнопки снимает отметку. Название на карточке переносится целиком."
    )
    val gesturesGuideMoreActionsTitle get() = t(
        "Where are the other actions?",
        "Где остальные действия?"
    )
    val gesturesGuideMoreActionsBody get() = t(
        "On the task, ⋮ opens edit, comments, move, the timer, and a link to a goal. More holds photos, hide, and delete.\nOn the hub, ⋮ opens comments, overdue only, and sort. More holds the primary hub, rename, and delete.\nAt the top of the board, ⋮ opens projects, a new hub, and the hub list.",
        "У задачи ⋮ — правка, комментарии, перенос, таймер и привязка к цели. В «Ещё» — фото, скрыть и удалить.\nУ хаба ⋮ — комментарии, только просроченные и сортировка. В «Ещё» — основной хаб, переименовать и удалить.\nВверху доски ⋮ — проекты, новый хаб и список хабов."
    )
    val gesturesGuideInProgressTitle get() = t(
        "How do I mark what I’m doing right now?",
        "Как отметить, чем занимаюсь сейчас?"
    )
    val gesturesGuideInProgressBody get() = t(
        "On the card open ⋮ and choose In progress (lightning). The card gets a highlight. Only one task is in focus.",
        "На карточке откройте ⋮ и выберите «В работе» (молния). Карточка подсветится. В фокусе только одно дело."
    )
    val gesturesGuideDueDateTitle get() = t(
        "How do I set Start (when to begin)?",
        "Как задать «Приступить»?"
    )
    val gesturesGuideDueDateBody get() = t(
        "Add or edit the task and open Start — Today, Tomorrow, or a date, then a clock and Remind. Finish is a separate row: a last day, or this week / this month. Timer is its own row. On the card, tap the date to change the start day.",
        "Добавьте или измените задачу и откройте «Приступить» — Сегодня, Завтра или дата, затем часы и «Напомнить». «Закончить» — отдельный ряд: последний день или «на этой неделе» / «в этом месяце». «Таймер» — свой ряд. На карточке нажатие на дату меняет день «Приступить»."
    )
    val gesturesGuideTimerTitle get() = t(
        "How do I use the timer?",
        "Как пользоваться таймером?"
    )
    val gesturesGuideTimerBody get() = t(
        "In add or edit, open Timer and set the minutes. On the task open ⋮ and choose Start timer. A clock appears at the bottom: tap to pause or resume, flick it away to cancel. A sound plays when time is up. Timer is not Finish.",
        "В добавлении или правке откройте «Таймер» и задайте минуты. У задачи откройте ⋮ и выберите «Запустить таймер». Внизу появится циферблат: нажатие — пауза или продолжение, смахивание — отмена. В конце играет звук. Таймер — не «Закончить»."
    )
    val gesturesGuideSelectGroupTitle get() = t(
        "How do I group tasks in a hub?",
        "Как сгруппировать задачи в хабе?"
    )
    val gesturesGuideSelectGroupBody get() = t(
        "Long-press a card and release to select it, tap more cards, then tap the grid icon at the bottom. You need at least two. The × on the group header ungroups them.",
        "Долгое нажатие без сдвига выделяет карточку, затем отметьте ещё и нажмите значок сетки внизу. Нужны хотя бы две. Крестик на заголовке группы снимает её."
    )
    val gesturesGuideLinkGoalTitle get() = t(
        "How do I link a task to a goal?",
        "Как привязать задачу к цели?"
    )
    val gesturesGuideLinkGoalBody get() = t(
        "In the task menu (⋮) choose link to goal, tap a goal or step, then confirm with the checkmark.",
        "В меню задачи (⋮) выберите привязку к цели, нажмите на цель или шаг, затем подтвердите галочкой."
    )
    val gesturesGuidePhotosTitle get() = t(
        "How do I add a photo to a task?",
        "Как добавить фото к задаче?"
    )
    val gesturesGuidePhotosBody get() = t(
        "On the task open ⋮ → More → Photos. Take a picture or pick one from the gallery. Tap a photo to view it full screen.",
        "У задачи откройте ⋮ → Ещё → Фото. Снимите кадр или выберите из галереи. Нажмите на фото, чтобы открыть на весь экран."
    )
    val gesturesGuideNotesTitle get() = t(
        "How do I open the project diary?",
        "Как открыть дневник проекта?"
    )
    val gesturesGuideNotesBody get() = t(
        "On the projects screen expand the project: Calendar, Diary, Priorities, Recurring, Review, AI prompts. In the Diary, + starts an entry and a long press on + dictates. Photos, a reminder, and a link to a task are inside the entry. Turn into a task is in the entry’s ⋮ menu.",
        "На экране проектов раскройте проект: Календарь, Дневник, Приоритеты, Регулярные, Обзор, Промпты для ИИ. В «Дневнике» плюс — запись, долгое нажатие на плюс — надиктовать. Фото, напоминание и связь с задачей — внутри записи. «Сделать задачей» — в меню ⋮ у записи."
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
    val onboardingTitle4 get() = t("Menu & hide", "Меню и скрытие")
    val onboardingText4 get() = t(
        "At the top of the list, pull down to show the menu and up to hide it. Double-tap the hub title to hide or show it. Hide a task from ⋮ → More on the card.",
        "Вверху списка потяните вниз — меню появится, вверх — спрячется. Двойной тап по названию хаба скрывает или показывает его. Скрыть задачу — ⋮ → Ещё на карточке."
    )
    val onboardingTitle5 get() = t("Priorities", "Приоритеты")
    val onboardingText5 get() = t(
        "Open Priorities from the project row. Tasks with a color sit in that cell; move them with ⋮ on the matrix.",
        "«Приоритеты» — в карточке проекта. Задачи с цветом попадают в клетку; перемещайте их через ⋮ на матрице."
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
    /** Nested overflow page: more rarely used actions. */
    val moreMenuMore get() = t("More…", "Ещё…")
    val hubsJumpMenu get() = t("Hubs…", "Хабы…")
    val sortHubMenu get() = t("Sort…", "Сортировка…")
    val sortHubByImportanceShort get() = t("By importance", "По важности")
    val sortHubByDueShort get() = t("By due date", "По сроку")
    val showMenu get() = t("Show menu", "Показать меню")
    val pullDownForMenu get() = t("Pull down for menu", "Потяните вниз — меню")
    val projects get() = t("Projects", "Проекты")
    val setPrimaryHub get() = t("Set as primary hub", "Сделать основным хабом")
    val primaryHub get() = t("Primary hub", "Основной хаб")
    val addTask get() = t("Add task", "Добавить задачу")
    val newHub get() = t("New hub", "Новый хаб")
    val newTask get() = t("New task", "Новая задача")
    val task get() = t("Task", "Задача")
    val cardTypeLabel get() = t("Type", "Тип")
    val cardTypePlain get() = t("Plain", "Обычная")
    val cardTypeWorkout get() = t("Workout", "Тренировка")
    val cardTypeShop get() = t("Shopping", "Покупки")
    val cardTypeMeet get() = t("Contact", "Связь")
    fun cardTypeName(kind: String): String = when (kind) {
        TaskCardKind.WORKOUT -> cardTypeWorkout
        TaskCardKind.SHOP -> cardTypeShop
        TaskCardKind.MEET -> cardTypeMeet
        else -> cardTypePlain
    }
    val cardExercise get() = t("Exercise", "Упражнение")
    val cardSets get() = t("Sets", "Подходы")
    val cardRepsWeight get() = t("Reps · weight", "Повторы · вес")
    val cardItem get() = t("Item", "Пункт")
    val cardQty get() = t("Qty", "Кол-во")
    val cardAgenda get() = t("Agenda", "План")
    val cardOutcome get() = t("How it ended", "Чем закончилось")
    val cardWrite get() = t("Message", "Написать")
    val cardCall get() = t("Call", "Позвонить")
    val cardTelegram get() = t("Telegram", "Telegram")
    val cardWhatsApp get() = t("WhatsApp", "WhatsApp")
    val cardContact get() = t("Contact", "Контакт")
    val cardNoContact get() = t("No contact", "Без контакта")
    val cardFromPhone get() = t("From phone", "Из телефона")
    val cardSocial get() = t("Social", "Соцсеть")
    val cardContactsDenied get() = t("Contacts permission is needed", "Нужен доступ к контактам")
    val cardNoPhone get() = t("No phone number", "Нет номера")
    val cardNoApp get() = t("No app for this", "Нет приложения")
    val cardReady get() = t("Done", "Готово")
    val cardMinutes get() = t("Minutes", "Минуты")
    val cardSeconds get() = t("Seconds", "Секунды")
    val enterAddsTaskTitle get() = t("Enter adds a task", "Enter добавляет задачу")
    val enterSavesTask get() = t("The Enter key saves the task", "Клавиша Enter сохраняет задачу")
    val enterAddsNewline get() = t("Enter inserts a new line", "Enter переносит на новую строку")
    val hub get() = t("Hub", "Хаб")
    val hubLabel get() = t("Hub:", "Хаб:")
    val eisenhowerButtons get() = t("Priority", "Приоритет")
    val taskComplexity get() = t("Impact", "Вклад")
    val taskComplexityHint get() = t(
        "How much this step moves the goal (1 — small, 5 — major). Not how hard it felt.",
        "Насколько шаг двигает цель (1 — мелкий, 5 — крупный). Не то, насколько было тяжело делать."
    )
    fun taskDurationMinutes(m: Int) = t("$m min", "$m мин")
    val rateQuality get() = t("Rate", "Оценить")
    val rateQualityTitle get() = t("How well was it done?", "Насколько хорошо сделано?")
    val rateQualityHint get() = t("1 — barely, 5 — excellent", "1 — едва, 5 — отлично")
    val rateQualitySkip get() = t("Skip", "Пропустить")
    fun qualityLabel(q: Int) = t("Quality: $q", "Оценка: $q")
    val markNotDone get() = t("Not done", "Не выполнено")
    val clearNotDone get() = t("Clear “not done”", "Снять «не выполнено»")
    val notDoneBadge get() = t("not done", "не сделано")
    val setDueDate get() = t("When to start", "Приступить")
    fun startAt(time: String) = t("Start $time", "Приступить $time")
    fun startAtDefault(time: String) = t("Start $time (default)", "Приступить $time (как в настройках)")
    val deadlineLabel get() = t("Finish", "Закончить")
    val whenRowLabel get() = t("Start", "Приступить")
    val whenNone get() = t("no date", "без даты")
    val deadlineRowLabel get() = t("Finish", "Закончить")
    val deadlineNone get() = t("none", "нет")
    fun finishUntil(date: String) = t("until $date", "до $date")
    val deadlineKindDate get() = t("Date", "Дата")
    val deadlineKindWindow get() = t("Window", "Окно")
    val windowThisWeek get() = t("this week", "на этой неделе")
    val windowThisMonth get() = t("this month", "в этом месяце")
    val timerRowLabel get() = t("Timer", "Таймер")
    val timerNone get() = t("off", "выкл")
    val timerRowHint get() = t(
        "Countdown on the card. Start it with the timer button; tap again to reset. Not the Finish date.",
        "Отсчёт на карточке. Запуск кнопкой таймера, повторное нажатие сбрасывает. Это не «Закончить»."
    )
    val timerMinutesLabel get() = t("Minutes", "Минуты")
    fun timerMinutesValue(m: Int) = t("timer $m min", "таймер $m мин")
    val timerStart get() = t("Start timer", "Запустить таймер")
    val timerPause get() = t("Pause", "Пауза")
    val timerResume get() = t("Resume", "Продолжить")
    val timerDoneTitle get() = t("Time's up", "Время вышло")
    val timerChannelName get() = t("Timers", "Таймеры")
    val timerChannelDesc get() = t(
        "Sound when a task timer ends",
        "Звук, когда таймер задачи заканчивается"
    )
    val remindClockLabel get() = t("Remind", "Напомнить")
    val whereRowLabel get() = t("Where", "Куда")
    val deadlineHint get() = t(
        "Optional. If empty, Finish is the Start day.",
        "Необязательно. Если пусто — Закончить в день «Приступить»."
    )
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
    val hubRules get() = t("Comments", "Комментарии")
    val hubSystemRules get() = t("Hub rules", "Правила хаба")
    val hubSystemRulesShow get() = t("Show hub rules", "Показать правила хаба")
    val hubSystemRulesHide get() = t("Hide hub rules", "Скрыть правила хаба")
    val noHubRulesYet get() = t("No comments yet.\nWrite the first one below.", "Пока нет комментариев.\nНапишите первый ниже.")
    val statsDetails get() = t("Details", "Подробнее")
    val taskDialogMore get() = t("More options", "Ещё параметры")
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
    val writeRule get() = t("Write a note...", "Написать заметку...")
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
    val reminderDefaultTime get() = t("Default time to start", "Время, когда приступить")
    val reminderDefaultTimeHint get() = t(
        "Default clock for when to start a dated task, and for the reminder if you don't pick another. No time if the task has no start date.",
        "По умолчанию для «Приступить» и для напоминания, если своё время не задано. Без даты старта — без времени."
    )
    val recurringSpawnDefaultTime get() = t("Recurring create time", "Время создания регулярных")
    val recurringSpawnDefaultTimeHint get() = t(
        "When today’s recurring tasks appear on the hub (default 03:30). If a task’s reminder is earlier, it is created at that reminder time instead.",
        "Когда сегодняшние регулярные задачи появляются на хабе (по умолчанию 03:30). Если напоминание задачи раньше — создаётся в это время напоминания."
    )
    val reminderTaskTime get() = t("Reminder time", "Время напоминания")
    val reminderUseDefault get() = t("Use default", "Как в настройках")
    val reminderPickTime get() = t("Pick time", "Выбрать время")
    fun reminderAt(time: String) = t("Reminder $time", "Напоминание $time")
    fun reminderCustom(time: String) = t("Custom $time", "Своё $time")
    val noteReminderChannelName get() = t("Reminders", "Напоминания")
    val noteReminderChannelDesc get() = t(
        "Date and time reminders you set yourself",
        "Напоминания на дату и время, которые вы ставите сами"
    )
    val noteReminderTitle get() = t("Reminder", "Напоминание")
    val remindBefore get() = t("Remind before", "Напомнить за")
    val remindAmount get() = t("Amount", "Сколько")
    val reminderTextLabel get() = t("Reminder text", "Текст напоминания")
    val reminderNeedsDue get() = t("Set a start date first", "Сначала укажите, когда приступить")
    fun remindUnitShort(unit: com.example.kaizenkanban.domain.model.RemindLeadUnit) = when (unit) {
        com.example.kaizenkanban.domain.model.RemindLeadUnit.HOUR -> t("hr", "час")
        com.example.kaizenkanban.domain.model.RemindLeadUnit.DAY -> t("day", "день")
        com.example.kaizenkanban.domain.model.RemindLeadUnit.WEEK -> t("wk", "нед")
        com.example.kaizenkanban.domain.model.RemindLeadUnit.MONTH -> t("mo", "мес")
    }
    fun remindBeforePreview(whenText: String) = t("Fires $whenText", "Сработает $whenText")
    fun remindBeforeEach(amount: Int, unitLabel: String) = t(
        "$amount $unitLabel before each reminder",
        "За $amount $unitLabel до каждого напоминания"
    )
    val addDayReminder get() = t("Reminder", "Напоминание")
    val reminderSaved get() = t("Reminder saved", "Напоминание сохранено")
    val deleteReminder get() = t("Delete reminder", "Удалить напоминание")
    val reminderTitle get() = t("Task due today", "Срок задачи сегодня")
    val reminderOverdueTitle get() = t("Still overdue", "Всё ещё просрочено")
    val widgetTitle get() = t("Kairos", "Kairos")
    val widgetFocus get() = t("Focus", "В работе")
    val widgetDueSection get() = t("Today / overdue", "Сегодня / просрочено")
    val widgetEmpty get() = t("No focus or due tasks", "Нет фокуса и срочных задач")
    val widgetFocusOnlyTitle get() = t("Kairos", "Kairos")
    val widgetFocusOnlyDesc get() = t(
        "Focus task, or quick add and voice",
        "Фокус или быстрая задача и голос"
    )
    val archiveBoard get() = t("Archive board", "В архив")
    val unarchiveBoard get() = t("Restore board", "Вернуть из архива")
    val showArchivedBoards get() = t("Show archived", "Показать архив")
    val hideArchivedBoards get() = t("Hide archived", "Скрыть архив")
    val archivedLabel get() = t("Archived", "Архив")
    val boardTemplate get() = t("Template", "Шаблон")
    val templateInbox get() = t("Inbox only", "Только Inbox")
    val templateFlow get() = t("Flow", "Поток")
    val templateEisenhower get() = t("Priorities", "Приоритеты")
    val exportProject get() = t("Export project", "Экспорт проекта")
    val shareProject get() = t("Share project", "Поделиться проектом")
    val themeSystem get() = t("System", "Система")
    val themeLight get() = t("Light", "Светлая")
    val themeDark get() = t("Dark", "Тёмная")
    val themeSlate get() = t("Slate", "Сланец")
    val themeLabel get() = t("Theme", "Тема")
    val eisenhowerPaletteLabel get() = t("Eisenhower colors", "Цвета Эйзенхауэра")
    val eisenhowerPaletteHint get() = t(
        "Applies to Priorities cells and painted task cards",
        "Для клеток «Приоритетов» и цветных карточек"
    )
    val eisenhowerPalettePlain get() = t("Plain", "Без цвета")
    val eisenhowerPaletteBerry get() = t("Berry", "Ягодная")
    val eisenhowerPaletteSunset get() = t("Sunset", "Закат")
    val eisenhowerPaletteOcean get() = t("Ocean", "Океан")
    val eisenhowerPaletteInk get() = t("Ink", "Чернила")
    val eisenhowerPalettePen get() = t("Pen", "Перо")
    val eisenhowerPaletteGarden get() = t("Garden", "Сад")
    val weekStats get() = t("This week", "На этой неделе")
    val statsLoading get() = t("Calculating…", "Считаем…")
    val statsTitle get() = t("Review", "Обзор")
    val statsTabGoals get() = t("Goals", "Цели")
    val statsTabForecast get() = t("Movement", "Движение")
    val statsTabPeriod get() = t("Overview", "Итог")
    val statsTabArchive get() = t("Archive", "Архив")
    val statsArchiveDone get() = t("Done", "Готово")
    val statsArchiveNotDone get() = t("Not done", "Не вып.")
    val statsArchiveEmpty get() = t("Archive is empty", "Архив пуст")
    val statsArchiveNotDoneHint get() = t(
        "−step weight · cannot restore",
        "−вес шага · на доску не вернуть"
    )
    val statsExcludeFromStats get() = t("Ignore in stats", "Не учитывать")
    val statsIncludeInStats get() = t("Count in stats", "Учитывать")
    val statsDeleteToJournal get() = t("Delete (keep in journal)", "Удалить (в журнал)")
    val statsClearArchive get() = t("Clear archive", "Очистить архив")
    fun statsClearArchiveConfirm(n: Int) = t(
        "Delete $n archived task(s)? They stay in stats via the journal.",
        "Удалить $n задач(и) из архива? В статистике они останутся через журнал."
    )
    val statsClearingArchive get() = t("Clearing archive…", "Очистка архива…")
    fun statsClearingArchiveProgress(done: Int, total: Int) = t(
        "Processed $done of $total",
        "Обработано $done из $total"
    )
    val statsAddGoal get() = t("Add goal", "Добавить цель")
    val statsRemoveGoal get() = t("Remove from goals", "Убрать из целей")
    val markAsGoal get() = t("Mark as goal", "Пометить как цель")
    val unmarkAsGoal get() = t("Unmark goal", "Снять пометку цели")
    val statsGoalArchivedBadge get() = t("archived", "архив")
    val statsMore get() = t("More", "Ещё")
    val statsEpochResetYear get() = t("From install", "С установки")
    val statsEpochTitle get() = t("Stats from", "Статистика с")
    val statsGoalLabel get() = t("Goal", "Цель")
    val statsNoGoals get() = t(
        "Add a goal or link tasks as goals and steps",
        "Добавьте цель или свяжите задачи как цели и шаги"
    )
    fun statsGoalClosureLine(delta: String) = t("Goal closure: $delta", "Закрытие цели: $delta")
    fun statsChanceLine(delta: String) = t("Likelihood: $delta", "Вероятность: $delta")
    fun statsPeriodDelta(pp: Int, period: com.example.kaizenkanban.domain.stats.StatsPeriod): String {
        val sign = if (pp >= 0) "+" else ""
        val window = when (period) {
            com.example.kaizenkanban.domain.stats.StatsPeriod.TODAY -> t("today", "сегодня")
            com.example.kaizenkanban.domain.stats.StatsPeriod.YESTERDAY -> t("yesterday", "вчера")
            com.example.kaizenkanban.domain.stats.StatsPeriod.WEEK -> t("week", "неделю")
            com.example.kaizenkanban.domain.stats.StatsPeriod.MONTH -> t("month", "месяц")
            com.example.kaizenkanban.domain.stats.StatsPeriod.MONTHS_3 -> t("3 months", "3 месяца")
            com.example.kaizenkanban.domain.stats.StatsPeriod.MONTHS_6 -> t("half a year", "полгода")
            com.example.kaizenkanban.domain.stats.StatsPeriod.YEAR -> t("year", "год")
            com.example.kaizenkanban.domain.stats.StatsPeriod.NOW -> t("total", "всего")
        }
        return t("$sign$pp% this $window", "$sign$pp% за $window")
    }
    val statsForecastTitle get() = t("Efficiency", "Эффективность")
    val statsScoreOutOfTen get() = t("of 10", "из 10")
    fun statsForecastBreakdown(p: Int, r: Int, o: Int) = t(
        "progress $p% · rhythm $r% · off goal $o%",
        "прогресс $p% · ритм $r% · без цели $o%"
    )
    val statsPeriodOverallTitle get() = t("Overall movement", "Общее движение")
    val statsPeriodTowardShare get() = t("Toward goals share", "Доля к целям")
    val statsPeriodOverallHint get() = t(
        "All work in the period, not one goal",
        "Вся работа за период, не одна цель"
    )
    val statsChartToward get() = t("Toward goals", "К целям")
    val statsChartTrivia get() = t("Off goal", "Без цели")
    val statsChartActivity get() = t("By day", "По дням")
    val statsChartActivityHint get() = t("Bar height = completions", "Высота = закрытия")
    val statsChanceTitle get() = t("Likelihood of completion", "Вероятность выполнения")
    fun statsChanceTitleUntil(date: String) = t(
        "Likelihood of completion by $date",
        "Вероятность выполнения до $date"
    )
    fun statsLikelyDate(date: String) = t(
        "Likely finish: $date",
        "Вероятная дата выполнения: $date"
    )
    val statsLikelyDateNoPace get() = t(
        "At this pace the goal is not getting closer",
        "При таком темпе цель не приближается"
    )
    val statsLikelyDateReached get() = t("The goal is already closed", "Цель уже закрыта")
    val statsChartAxisCount get() = t("count", "шт.")
    val importanceLabel get() = t("Importance", "Важность")
    val statsEditTask get() = t("Edit details", "Параметры")
    val statsTaskSettings get() = t("Task settings", "Настройки задачи")
    val taskCriteriaTitle get() = t("Task criteria", "Критерии задачи")
    fun statsDoneTotal(n: Int) = t("Total done: $n", "Всего выполнено: $n")
    fun statsTaskTotal(n: Int) = t("Tasks total: $n", "Всего задач: $n")
    fun statsNotDoneTotal(n: Int) = t("Total not done: $n", "Всего не выполнено: $n")
    fun statsRecurringOfDone(n: Int) = t("Recurring done: $n", "Регулярных выполнено: $n")
    fun statsRecurringOfNotDone(n: Int) = t("Recurring not done: $n", "Регулярных не выполнено: $n")
    fun statsTrivia(n: Int) = t("Off goal: $n", "Без цели: $n")
    fun statsPeriodLabel(period: com.example.kaizenkanban.domain.stats.StatsPeriod): String =
        when (period) {
            com.example.kaizenkanban.domain.stats.StatsPeriod.NOW -> t("Overview", "Итог")
            com.example.kaizenkanban.domain.stats.StatsPeriod.TODAY -> t("Today", "Сегодня")
            com.example.kaizenkanban.domain.stats.StatsPeriod.YESTERDAY -> t("Yesterday", "Вчера")
            com.example.kaizenkanban.domain.stats.StatsPeriod.WEEK -> t("Week", "Неделя")
            com.example.kaizenkanban.domain.stats.StatsPeriod.MONTH -> t("Month", "Месяц")
            com.example.kaizenkanban.domain.stats.StatsPeriod.MONTHS_3 -> t("3 months", "3 месяца")
            com.example.kaizenkanban.domain.stats.StatsPeriod.MONTHS_6 -> t("6 months", "Полгода")
            com.example.kaizenkanban.domain.stats.StatsPeriod.YEAR -> t("Year", "Год")
        }
    val statsViewModeOverall get() = t("Overview", "Итог")
    val statsViewModeDynamics get() = t("Changes", "Динамика")
    val statsViewModeOverallHint get() = t(
        "Absolute picture from the stats start date",
        "Картина целиком с даты начала учёта"
    )
    val statsViewModeDynamicsHint get() = t(
        "How much changed in the selected window",
        "Насколько изменилось в выбранном окне"
    )
    fun statsHowPeriodWent(period: com.example.kaizenkanban.domain.stats.StatsPeriod): String =
        when (period) {
            com.example.kaizenkanban.domain.stats.StatsPeriod.TODAY ->
                t("How is today going", "Как идёт сегодня")
            com.example.kaizenkanban.domain.stats.StatsPeriod.YESTERDAY ->
                t("How did yesterday go", "Как прошёл вчерашний день")
            com.example.kaizenkanban.domain.stats.StatsPeriod.WEEK ->
                t("How did the week go", "Как прошла неделя")
            com.example.kaizenkanban.domain.stats.StatsPeriod.MONTH ->
                t("How did the month go", "Как прошёл месяц")
            com.example.kaizenkanban.domain.stats.StatsPeriod.MONTHS_3 ->
                t("How did 3 months go", "Как прошли 3 месяца")
            com.example.kaizenkanban.domain.stats.StatsPeriod.MONTHS_6 ->
                t("How did half a year go", "Как прошло полгода")
            com.example.kaizenkanban.domain.stats.StatsPeriod.YEAR ->
                t("How did the year go", "Как прошёл год")
            com.example.kaizenkanban.domain.stats.StatsPeriod.NOW ->
                t("Overview", "Итог")
        }
    val statsFromStartSection get() = t("Overall picture", "Общая картина")
    val statsResetGoalEpoch get() = t("Restart goal tracking", "Начать учёт цели заново")
    val statsResetGoalEpochHint get() = t(
        "Older completed steps stop counting for this goal. History stays in Archive.",
        "Старые закрытые шаги перестанут считаться для этой цели. История останется в Архиве."
    )
    val aiPromptsTitle get() = t("AI prompts", "Промпты для ИИ")
    val aiPromptsIntro get() = t(
        "Choose a goal, copy a prompt into any AI chat, then paste the reply’s KAIROS v1 commands below. Commands create cards, habits, and links.",
        "Выберите цель, скопируйте промпт в любой чат с ИИ, затем вставьте ниже блок команд KAIROS v1. Команды создают карточки, привычки и связи."
    )
    val aiPromptsGoalLabel get() = t("Goal for the prompt", "Цель для промпта")
    val aiPromptsNoGoals get() = t(
        "Mark a task as a goal first (More on the card), then come back here.",
        "Сначала отметьте задачу как цель (Ещё на карточке), потом вернитесь сюда."
    )
    val aiPromptsNeedGoal get() = t("Select a goal first", "Сначала выберите цель")
    val aiPromptsCopied get() = t(
        "Prompt copied — paste it into any AI chat",
        "Промпт скопирован — вставьте в любой чат с ИИ"
    )
    val aiPromptsCopyAction get() = t("Copy prompt", "Копировать промпт")
    fun aiPromptKindTitle(kind: com.example.kaizenkanban.domain.stats.GoalAiPromptKind): String =
        when (kind) {
            com.example.kaizenkanban.domain.stats.GoalAiPromptKind.PLAN ->
                t("Plan next steps", "Спланировать шаги")
            com.example.kaizenkanban.domain.stats.GoalAiPromptKind.GAPS ->
                t("Find gaps", "Найти пробелы")
            com.example.kaizenkanban.domain.stats.GoalAiPromptKind.WEEK ->
                t("Plan the next 7 days", "План на 7 дней")
            com.example.kaizenkanban.domain.stats.GoalAiPromptKind.REFACTOR ->
                t("Simplify the step list", "Упростить список шагов")
            com.example.kaizenkanban.domain.stats.GoalAiPromptKind.RECURRING ->
                t("Suggest repeating habits", "Предложить привычки")
            com.example.kaizenkanban.domain.stats.GoalAiPromptKind.RETRO ->
                t("Review how it’s going", "Разобрать, как идёт")
            com.example.kaizenkanban.domain.stats.GoalAiPromptKind.CRITERIA ->
                t("Define success criteria", "Критерии успеха")
            com.example.kaizenkanban.domain.stats.GoalAiPromptKind.CHANCE ->
                t("Estimate chance of success", "Оценить шансы на успех")
            com.example.kaizenkanban.domain.stats.GoalAiPromptKind.IMPACT ->
                t("Rate step Impact (1–5)", "Оценить вклад шагов (1–5)")
            com.example.kaizenkanban.domain.stats.GoalAiPromptKind.COMMANDS ->
                t("Write Kairos commands", "Команды для Kairos")
        }
    fun aiPromptKindHint(kind: com.example.kaizenkanban.domain.stats.GoalAiPromptKind): String =
        when (kind) {
            com.example.kaizenkanban.domain.stats.GoalAiPromptKind.PLAN ->
                t(
                    "One-time cards and habits, including workout, shopping, and contact",
                    "Разовые карточки и привычки, в том числе тренировка, покупки и связь"
                )
            com.example.kaizenkanban.domain.stats.GoalAiPromptKind.GAPS ->
                t(
                    "What’s missing or stuck, and what to fix",
                    "Что не хватает или буксует, и что поправить"
                )
            com.example.kaizenkanban.domain.stats.GoalAiPromptKind.WEEK ->
                t(
                    "Realistic checklist from steps still open",
                    "Реалистичный чеклист из ещё открытых шагов"
                )
            com.example.kaizenkanban.domain.stats.GoalAiPromptKind.REFACTOR ->
                t(
                    "Rename, merge, and tidy the structure",
                    "Переименовать, объединить и навести порядок"
                )
            com.example.kaizenkanban.domain.stats.GoalAiPromptKind.RECURRING ->
                t(
                    "Only habits: how often and why",
                    "Только привычки: как часто и зачем"
                )
            com.example.kaizenkanban.domain.stats.GoalAiPromptKind.RETRO ->
                t(
                    "What worked, what stalled, what to change",
                    "Что сработало, что буксует, что менять"
                )
            com.example.kaizenkanban.domain.stats.GoalAiPromptKind.CRITERIA ->
                t(
                    "Clear checks for Done — save them in the Diary if useful",
                    "Понятные признаки «готово» — при желании в Дневник"
                )
            com.example.kaizenkanban.domain.stats.GoalAiPromptKind.CHANCE ->
                t(
                    "How realistic the goal looks now, and how to raise odds",
                    "Насколько реалистична цель сейчас и как повысить шансы"
                )
            com.example.kaizenkanban.domain.stats.GoalAiPromptKind.IMPACT ->
                t(
                    "How much each step moves the goal — not how hard it is",
                    "Насколько каждый шаг двигает цель — не насколько он тяжёлый"
                )
            com.example.kaizenkanban.domain.stats.GoalAiPromptKind.COMMANDS ->
                t(
                    "After you agree on a plan — a block the app can apply, including card rows",
                    "Когда план согласован — блок, который приложение применит, включая строки карточек"
                )
        }
    val aiCommandsTitle get() = t("Command line", "Командная строка")
    val aiCommandsHint get() = t(
        "Paste a KAIROS v1 block, preview, then apply. Creates goals, tasks, habits, links, and card rows (workout, shopping, contact). Diary text is written by hand.",
        "Вставьте блок KAIROS v1, посмотрите предпросмотр и примените. Создаёт цели, задачи, привычки, связи и строки карточек (тренировка, покупки, связь). Текст дневника — вручную."
    )
    val aiCommandsFieldLabel get() = t("Commands", "Команды")
    val aiCommandsParse get() = t("Preview", "Разобрать")
    val aiCommandsApply get() = t("Apply", "Применить")
    val aiCommandsClear get() = t("Clear", "Очистить")
    val aiCommandsUndo get() = t("Undo prompt", "Откатить промпт")
    fun aiCommandsUndoLeft(count: Int) = t(
        "Can roll back $count applied prompt(s)",
        "Можно откатить промптов: $count"
    )
    val aiCommandsUndone get() = t(
        "Last prompt rolled back",
        "Последний промпт откатан"
    )
    val aiCommandsUndoEmpty get() = t("Nothing to roll back", "Нечего откатывать")
    val aiCommandsNothing get() = t("Nothing to apply", "Нечего применять")
    val aiCommandsPreviewEmpty get() = t("No preview yet", "Пока нет предпросмотра")
    val aiCommandsWarnings get() = t("Warnings", "Предупреждения")
    val aiCommandsErrors get() = t("Errors", "Ошибки")
    fun aiCommandsApplied(goals: Int, tasks: Int, habits: Int, links: Int) = t(
        "Created: $goals goals, $tasks tasks, $habits habits, $links links",
        "Создано: целей $goals, задач $tasks, привычек $habits, связей $links"
    )
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
        "The back arrow opens projects and their boards. On the board, ⋮ jumps between hubs. The primary hub is in the hub’s More — it shows today’s date. Swipe hubs left and right. The project Diary holds entries with photos and reminders.",
        "Стрелка назад — проекты и их доски. На доске ⋮ переключает хабы. Основной хаб — в «Ещё» у хаба, на нём сегодняшняя дата. Хабы листаются влево-вправо. «Дневник» в карточке проекта — записи с фото и напоминаниями."
    )
    val onboardingTitle2 get() = t("Start, Finish, Timer, reminders", "Приступить, Закончить, Таймер, напоминания")
    val onboardingText2 get() = t(
        "Start is when to begin (Today/Tomorrow and a clock). Finish is the optional last day. Start a timer from the task ⋮; a clock appears, a tap pauses, a flick cancels. The reminder offers Done and Tomorrow.",
        "«Приступить» — когда начать (Сегодня/Завтра и часы). «Закончить» — необязательный последний день. Таймер запускается из ⋮ задачи: появится циферблат, нажатие — пауза, смахивание — отмена. В напоминании есть Готово и Завтра."
    )
    val onboardingTitle3 get() = t("Widgets", "Виджеты")
    val onboardingText3 get() = t(
        "Home widgets: add a task, dictate a task, or add a diary entry.",
        "Виджеты: быстрая задача, надиктовать задачу или запись в дневник."
    )
    val widgetQuickAdd get() = t("Quick add", "Быстрая задача")
    val widgetVoice get() = t("Dictate task", "Надиктовать задачу")
    val widgetAddNote get() = t("Diary", "Дневник")
    val widgetVoiceNote get() = t("Dictate note", "Надиктовать заметку")
    val searchArchive get() = t("Search archive…", "Поиск в архиве…")
    val searchRecurring get() = t("Search recurring…", "Поиск регулярных…")
    val dataBrowser get() = t("All data", "Все данные")
    val dataBrowserHint get() = t(
        "Everything in the database, including hidden and archived. Delete removes permanently.",
        "Всё из базы, включая скрытые и архивные. Удаление безвозвратное."
    )
    val hardDeleteFromDb get() = t("Delete from database", "Удалить из базы")
    val hardDeleteConfirm get() = t(
        "Permanently delete from the database? This cannot be undone.",
        "Удалить из базы навсегда? Это нельзя отменить."
    )
    val statusHidden get() = t("Hidden", "Скрыта")
    val statusArchived get() = t("Archived", "Архив")
    val quickAddTask get() = t("Quick add", "Быстрая задача")
    val quickAddHint get() = t("Adds to the primary board inbox", "В Inbox основной доски")
    fun quickAddGoesTo(board: String, hub: String) = t(
        "Adds to $board · $hub",
        "Попадёт в $board · $hub"
    )
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
    val clearAll get() = t("Clear all", "Очистить все")
    val clearCompletedTitle get() = t("Clear completed?", "Очистить выполненные?")
    val clearNotDoneTitle get() = t("Clear not done?", "Очистить невыполненные?")
    val recurringDelete get() = t("Delete template", "Удалить шаблон")
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
    fun notDoneCount(count: Int) = t("Not done ($count)", "Не выполнено ($count)")
    val taskLinksTitle get() = t("Goals & steps", "Цели и шаги")
    val taskParents get() = t("Goals", "Цели")
    val taskChildren get() = t("Steps", "Шаги")
    val taskLinksEmptyParents get() = t("Not linked to a goal yet", "Пока не привязано к цели")
    val taskLinksEmptyChildren get() = t("No steps yet", "Пока нет шагов")
    val pickParentTask get() = t("Pick a goal", "Выберите цель")
    val pickChildTask get() = t("Pick a step", "Выберите шаг")
    val noTasksToLink get() = t("No suitable tasks", "Нет подходящих задач")
    val noGoalsToLink get() = t("No goals yet — mark a task as a goal first", "Пока нет целей — сначала пометьте задачу как цель")
    val taskLinkCycleRejected get() = t("That link would create a cycle", "Такая связь создаст цикл")
    fun parentChip(title: String) = t("↑ $title", "↑ $title")
    fun childrenChip(count: Int) = t("↓ $count", "↓ $count")
    fun childrenChipLabel(title: String) = t("↓ $title", "↓ $title")
    fun linkedToGoal(title: String) = t("Goal: $title", "К цели: $title")
    fun linkedToGoalsCount(count: Int) = t("Goals: $count", "К целям: $count")
    val linkToGoal get() = t("Link to goal", "Привязать к цели")
    val manageTaskLinks get() = t("Goals & steps", "Цели и шаги")
    val goalSteps get() = t("Goal steps", "Шаги цели")
    val addParentLink get() = t("Link to goal", "К какой цели?")
    val addChildLink get() = t("Link existing step", "Привязать шаг")
    val createStep get() = t("New step", "+ шаг")
    val createStepTitle get() = t("New step under goal", "Новый шаг к цели")
    val createStepHint get() = t("Step title", "Название шага")
    val linkAdvanced get() = t("More link options", "Дополнительно")
    val unknownHub get() = t("Other hub", "Другой хаб")
    val linkFocusMode get() = t("Related tasks", "Связанные задачи")
    val linkFocusExit get() = t("Show all", "Показать все")
    val linkFocusEmptyHub get() = t("No related tasks in this hub", "В этом хабе нет связанных задач")
    fun linkPickParentMode(title: String) = t(
        "Pick a parent for ${quoted(title)}",
        "Выберите родителя для ${quoted(title)}"
    )
    val linkPickParentHint get() = t(
        "Tap a goal or step, then confirm with the checkmark — confirm again to unlink",
        "Нажмите на цель или шаг, затем подтвердите галочкой — ещё раз галочкой, чтобы отвязать"
    )
    val linkPickConfirm get() = t("Confirm link", "Подтвердить связь")
    val linkPickSelectFirst get() = t("Select a goal first", "Сначала выберите цель")
    fun linkParentLinked(title: String) = t("Linked to ${quoted(title)}", "Привязано к ${quoted(title)}")
    fun linkParentUnlinked(title: String) = t("Unlinked from ${quoted(title)}", "Отвязано от ${quoted(title)}")
    val linkPickEmpty get() = t(
        "No goals on the board yet — mark a task as a goal first",
        "На доске пока нет целей — сначала пометьте задачу как цель"
    )
    val selectTasks get() = t("Select", "Выбрать")
    val groupTasks get() = t("Group", "Сгруппировать")
    val ungroupTasks get() = t("Ungroup", "Разгруппировать")
    val removeFromGroup get() = t("Remove from group", "Убрать из группы")
    val addToGroup get() = t("Add to group", "В группу")
    val addToGroupHint get() = t(
        "Select tasks, then tap + to add them to this group",
        "Выберите задачи и нажмите +, чтобы добавить в группу"
    )
    val addToGroupNeedTasks get() = t("Select tasks to add", "Выберите задачи для добавления")
    val cancelSelection get() = t("Cancel", "Отмена")
    fun selectedCount(count: Int) = t("Selected: $count", "Выбрано: $count")
    val hubGroupLabel get() = t("Group", "Группа")
    val hubGroupRename get() = t("Group name", "Название группы")
    val hubGroupNameHint get() = t("Optional name", "Необязательное имя")
    fun hubGroupMembers(count: Int) = t("$count tasks", "$count задач")
    val hubGroupExpand get() = t("Expand group", "Развернуть группу")
    val hubGroupCollapse get() = t("Collapse group", "Свернуть группу")
    val hubGroupFocus get() = t("Only this group", "Только эта группа")
    val hubGroupFocusExit get() = t("Back", "Назад")
    val hubUngroupedLabel get() = t("No group", "Без группы")
    val hubUngroupedFocus get() = t("Tasks without a group", "Задачи без группы")
    val groupNeedTwo get() = t(
        "Select at least 2 tasks, or include a task from an existing group",
        "Выберите хотя бы 2 задачи или задачу из существующей группы"
    )
    val groupSameHubOnly get() = t("Group tasks from the same hub", "Группируйте задачи одного хаба")
    val taskDescription get() = t("Notes", "Заметки")
    val taskDescriptionHint get() = t("Short notes for this task", "Краткие заметки к задаче")
    val taskPhotos get() = t("Photos", "Фото")
    fun taskPhotosCount(count: Int) = t("Photos ($count)", "Фото ($count)")
    val taskPhotosEmpty get() = t("No photos yet", "Пока нет фото")
    val taskPhotosAdd get() = t("Add photo", "Добавить фото")
    val takePhoto get() = t("Take photo", "Сфотографировать")
    val cameraPermissionNeeded get() = t(
        "Camera permission is required to take photos",
        "Для съёмки нужно разрешение на камеру"
    )
    val taskPhotosLimit get() = t("Up to 10 photos per task", "Не больше 10 фото на задачу")
    val taskPhotosDelete get() = t("Delete photo", "Удалить фото")
    val exportPhotosWarning get() = t(
        "JSON backup does not include photo files",
        "JSON-бэкап не включает файлы фото"
    )
    fun moreTasksCount(count: Int) = t("+$count more", "ещё $count")
    fun clearCompletedText(count: Int, hub: String) = t(
        "Move these $count completed tasks from hub ${quoted(hub)} to Archive? They leave the board but stay in Review.",
        "Перенести эти выполненные задачи ($count) из хаба ${quoted(hub)} в Архив? С доски исчезнут, в Обзоре останутся."
    )
    fun clearNotDoneText(count: Int, hub: String) = t(
        "Move these $count not-done tasks from hub ${quoted(hub)} to Archive? They cannot be restored to the board.",
        "Перенести эти невыполненные задачи ($count) из хаба ${quoted(hub)} в Архив? На доску вернуть нельзя."
    )
    fun closedAt(time: String) = t("Closed: $time", "Закрыто: $time")
    fun hubWithName(name: String) = t("Hub: ${quoted(name)}", "Хаб: ${quoted(name)}")
    fun toHub(name: String) = t("To $name", "В $name")

    // Voice assistant (PRO)
    val voiceAssistant get() = t("Dictate task", "Надиктовать задачу")
    val voiceListening get() = t("Listening…", "Слушаю…")
    val voiceListeningHint get() = t(
        "Say the task title",
        "Назовите название задачи"
    )
    val voiceListeningChannel get() = t("Voice capture", "Голосовой ввод")
    val voiceNotUnderstood get() = t(
        "Say the task title, e.g. buy milk",
        "Назовите задачу, например: купить молоко"
    )
    fun voiceAdded(title: String) = t("Added: ${quoted(title)}", "Добавлено: ${quoted(title)}")
    val voiceNoDestination get() = t("No board/hub for new tasks", "Нет доски/хаба для задачи")
    val voiceNoMatch get() = t("Didn't catch that — try again", "Не расслышала — повторите")
    val voiceNetworkError get() = t("Internet needed for voice", "Нужен интернет для голоса")
    val voiceUnavailable get() = t("Speech recognition unavailable", "Распознавание речи недоступно")
    val voiceError get() = t("Couldn’t add the task", "Не удалось добавить задачу")
    val proFeatureLocked get() = t(
        "Kairos PRO required — enable the PRO stub in Settings",
        "Нужен Kairos PRO — включите заглушку PRO в Настройках"
    )
    val proSection get() = t("Kairos PRO", "Kairos PRO")
    val proUnlockedStub get() = t("PRO unlocked (stub)", "PRO включён (заглушка)")
    val proUnlockedStubHint get() = t(
        "Temporary switch until Play Billing. Voice and task photos use this flag.",
        "Временный переключатель до Play Billing. Голос и фото к задачам смотрят на него."
    )
    val voiceHintExamples get() = t(
        "Tap the mic and say the task title, e.g. «buy milk»",
        "Нажмите микрофон и назовите задачу, например: «купить молоко»"
    )
    val voiceMicPermissionNeeded get() = t(
        "Microphone permission is required to dictate tasks",
        "Для надиктовки задач нужно разрешение на микрофон"
    )

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
                "На стороне контрагента" to "Waiting on others",
                "Сделать" to "Do",
                "Планирую" to "Planning",
                "Держу курс" to "Holding course",
                "Лестница целей" to "Goal ladder",
                "OKR" to "Goal ladder",
                "Срочно и важно" to "Urgent and important",
                "Важно, не срочно" to "Important, not urgent",
                "Срочно, не важно" to "Urgent, not important",
                "Не срочно и не важно" to "Not urgent and not important",
                "Матрица Эйзенхауэра" to "Priorities",
                "Приоритеты" to "Priorities",
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
                "Waiting on others" to "На стороне контрагента",
                "Do" to "Сделать",
                "Planning" to "Планирую",
                "Holding course" to "Держу курс",
                "Goal ladder" to "Лестница целей",
                "OKR" to "Лестница целей",
                "Urgent and important" to "Срочно и важно",
                "Important, not urgent" to "Важно, не срочно",
                "Urgent, not important" to "Срочно, не важно",
                "Not urgent and not important" to "Не срочно и не важно",
                "Eisenhower Matrix" to "Приоритеты",
                "Priorities" to "Приоритеты",
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
