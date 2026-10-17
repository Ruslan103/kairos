package com.example.kaizenkanban.ui.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.kaizenkanban.data.local.KairosPreferences
import com.example.kaizenkanban.domain.calendar.CalendarEvent
import com.example.kaizenkanban.domain.calendar.CalendarEventBuilder
import com.example.kaizenkanban.ui.theme.buttonFill
import com.example.kaizenkanban.ui.theme.buttonLabel
import com.example.kaizenkanban.ui.theme.buttonLine
import com.example.kaizenkanban.ui.theme.chartForecast
import com.example.kaizenkanban.ui.theme.chartProgress
import com.example.kaizenkanban.ui.theme.completedMark
import com.example.kaizenkanban.reminders.DueReminderScheduler
import com.example.kaizenkanban.domain.model.Eisenhower
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.ui.i18n.AppStrings
import com.example.kaizenkanban.ui.i18n.KanbanNames
import com.example.kaizenkanban.ui.i18n.LocalAppLanguage
import com.example.kaizenkanban.ui.components.DialogMarqueeText
import com.example.kaizenkanban.ui.i18n.LocalAppStrings
import com.example.kaizenkanban.ui.viewmodel.SharedViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private const val DAY_MS = 86_400_000L

fun Long.formatDateShort(locale: Locale = Locale.getDefault()): String =
    SimpleDateFormat("dd MMM", locale).format(Date(this))

fun Long.formatDateTimeShort(locale: Locale = Locale.getDefault()): String =
    SimpleDateFormat("dd MMM, HH:mm", locale).format(Date(this))

fun startOfLocalDayMillis(now: Long = System.currentTimeMillis()): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = now
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

fun Long.isOverdueDate(now: Long = System.currentTimeMillis()): Boolean =
    this < startOfLocalDayMillis(now)

/** Overdue only by the deadline; an empty deadline uses the start day. */
fun Task.isPastDeadline(now: Long = System.currentTimeMillis()): Boolean {
    val anchor = deadlineAt ?: dueDate ?: return false
    return !isCompleted && anchor.isOverdueDate(now)
}

fun Long.isDueToday(now: Long = System.currentTimeMillis()): Boolean {
    val start = startOfLocalDayMillis(now)
    return this >= start && this < start + DAY_MS
}

enum class DueDayBucket {
    OVERDUE, TODAY, THIS_WEEK, LATER, DONE
}

fun Long.dueDayBucket(isCompleted: Boolean, now: Long = System.currentTimeMillis()): DueDayBucket {
    if (isCompleted) return DueDayBucket.DONE
    val start = startOfLocalDayMillis(now)
    return when {
        this < start -> DueDayBucket.OVERDUE
        this < start + DAY_MS -> DueDayBucket.TODAY
        this < start + 7 * DAY_MS -> DueDayBucket.THIS_WEEK
        else -> DueDayBucket.LATER
    }
}

fun Long.relativeDueLabel(
    strings: AppStrings,
    locale: Locale,
    isCompleted: Boolean = false,
    now: Long = System.currentTimeMillis(),
    /** Start clock. Null plus [defaultReminderMinutes] uses the settings time. */
    reminderMinutesOfDay: Int? = null,
    defaultReminderMinutes: Int? = null,
    /** False when the start day has passed but the deadline has not. */
    markOverdue: Boolean = true
): String {
    if (isCompleted) return formatDateShort(locale)
    val start = startOfLocalDayMillis(now)
    val dayLabel = when {
        this < start && markOverdue -> strings.dueOverdue
        this < start -> formatDateShort(locale)
        this < start + DAY_MS -> strings.dueToday
        this < start + 2 * DAY_MS -> strings.dueTomorrow
        else -> formatDateShort(locale)
    }
    val reminderMinutes = reminderMinutesOfDay ?: defaultReminderMinutes
    val time = if (reminderMinutes != null) {
        formatReminderClock(reminderMinutes)
    } else {
        formatDueClock(this)
    }
    return if (time != null) "$dayLabel $time" else dayLabel
}

private fun formatReminderClock(minutesOfDay: Int): String {
    val clamped = minutesOfDay.coerceIn(0, 23 * 60 + 59)
    return String.format("%02d:%02d", clamped / 60, clamped % 60)
}

/** HH:mm when the due instant has a meaningful clock time (not midnight). Legacy fallback. */
private fun formatDueClock(millis: Long): String? {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
    val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val minute = cal.get(java.util.Calendar.MINUTE)
    if (hour == 0 && minute == 0) return null
    return String.format("%02d:%02d", hour, minute)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(
    projectId: String,
    viewModel: SharedViewModel,
    onBack: () -> Unit,
    onOpenTask: (boardId: String, columnId: String, taskId: String) -> Unit = { _, _, _ -> },
    onAddTask: (boardId: String, columnId: String, dueMillis: Long) -> Unit = { _, _, _ -> }
) {
    val state by viewModel.state.collectAsState()
    val s = LocalAppStrings.current
    val dateLocale = LocalAppLanguage.current.locale
    val context = LocalContext.current
    var monthMode by rememberSaveable { mutableStateOf(true) }
    var visibleMonthStart by remember { mutableStateOf(startOfLocalMonthMillis()) }
    var selectedDayStart by remember { mutableStateOf(startOfLocalDayMillis()) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var suppressSearchHits by rememberSaveable { mutableStateOf(false) }
    var focusEventKey by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { KairosPreferences(context) }

    val project = state.projects.find { it.id == projectId }
    val projectBoardIds = remember(state.boards, projectId) {
        state.boards.filter { it.projectId == projectId }.map { it.id }.toSet()
    }
    val projectColumnIds = remember(state.columns, projectBoardIds) {
        state.columns.filter { it.boardId in projectBoardIds }.map { it.id }.toSet()
    }
    val projectTaskIds = remember(state.tasks, projectColumnIds) {
        state.tasks
            .filter {
                it.columnId in projectColumnIds ||
                    it.linkedColumnIds.any { id -> id in projectColumnIds }
            }
            .map { it.id }
            .toSet()
    }
    val projectTemplates = remember(state.recurringTemplates, projectId) {
        state.recurringTemplates.filter { it.projectId == projectId }
    }

    val monthEnd = remember(visibleMonthStart) { shiftLocalMonth(visibleMonthStart, 1) - 1 }
    val listRangeStart = remember { startOfLocalDayMillis() - 60L * DAY_MS }
    val listRangeEnd = remember { startOfLocalDayMillis() + 90L * DAY_MS }

    var showDayReminder by remember { mutableStateOf(false) }
    var dayReminderText by rememberSaveable { mutableStateOf("") }
    var dayReminderMinutes by rememberSaveable { mutableStateOf(prefs.reminderMinutesOfDay) }
    var dayListExpanded by rememberSaveable { mutableStateOf(false) }

    val events = remember(
        state.tasks, state.reminders, projectId, projectTaskIds, projectTemplates,
        monthMode, visibleMonthStart, monthEnd
    ) {
        val rangeStart = if (monthMode) visibleMonthStart else listRangeStart
        val rangeEnd = if (monthMode) monthEnd else listRangeEnd
        val built = CalendarEventBuilder.build(
            tasks = state.tasks,
            projectTaskIds = projectTaskIds,
            templates = projectTemplates,
            rangeStart = rangeStart,
            rangeEnd = rangeEnd
        )
        val startDay = startOfLocalDayMillis(rangeStart)
        val endDay = startOfLocalDayMillis(rangeEnd)
        val notes = state.reminders.mapNotNull { reminder ->
            if (reminder.projectId != projectId || reminder.text.isBlank()) return@mapNotNull null
            val day = startOfLocalDayMillis(reminder.triggerAtMillis)
            if (day < startDay || day > endDay) return@mapNotNull null
            val clock = java.util.Calendar.getInstance().apply { timeInMillis = reminder.triggerAtMillis }
            CalendarEvent(
                id = reminder.id,
                title = reminder.text,
                dayStartMillis = day,
                sortMinutes = clock.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
                    clock.get(java.util.Calendar.MINUTE),
                isNote = true
            )
        }
        (built + notes).sortedWith(
            compareBy({ it.dayStartMillis }, { it.sortMinutes }, { it.title.lowercase() })
        )
    }

    val highlight = remember(focusEventKey, events, state.tasks) {
        CalendarEventBuilder.highlight(focusEventKey, events, state.tasks)
    }
    val focusDays = highlight?.occurrenceDays.orEmpty()
    val deadlineDays = highlight?.deadlineDays.orEmpty()

    val searchHits = remember(searchQuery, events) {
        val q = searchQuery.trim()
        if (q.isEmpty()) emptyList()
        else events.filter { it.title.contains(q, ignoreCase = true) }
            .distinctBy { event ->
                when {
                    event.isNote -> event.id
                    event.task != null -> event.task.id
                    else -> "tpl:${event.templateId}:${event.title}"
                }
            }
            .take(12)
    }

    fun requestAddForSelectedDay() {
        val boards = state.boards.filter { it.projectId == projectId && !it.isArchived }
        val board = boards.find { it.isDefault } ?: boards.firstOrNull()
        if (board == null) {
            android.widget.Toast.makeText(context, s.noPrimaryBoard, android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val cols = state.columns.filter { it.boardId == board.id }.sortedBy { it.position }
        if (cols.isEmpty()) {
            android.widget.Toast.makeText(context, s.boardHasNoHubs, android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val markedId = prefs.getPrimaryHubId(board.id)
        val hub = cols.find { it.id == markedId }
            ?: cols.find { col ->
                val t = col.title.trim().lowercase()
                t == "сделать" || t == "do" || t == "to do" || t == "todo" ||
                    t == "сегодня" || t == "today"
            }
            ?: cols.first()
        val dueAt = Calendar.getInstance().apply {
            timeInMillis = selectedDayStart
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        onAddTask(board.id, hub.id, dueAt)
    }

    fun completeTaskWithUndo(task: Task) {
        coroutineScope.launch {
            val markedDone = viewModel.completeOrReopen(task)
            if (markedDone) {
                val result = snackbarHostState.showSnackbar(
                    message = s.taskMarkedDone,
                    actionLabel = s.undo,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.undoComplete(task.id)
                }
            }
        }
    }

    fun selectSearchHit(event: CalendarEvent) {
        focusEventKey = when {
            event.isNote -> event.id
            !event.templateId.isNullOrBlank() -> "tpl:${event.templateId}"
            else -> event.task?.id ?: event.id
        }
        selectedDayStart = event.dayStartMillis
        dayListExpanded = true
        if (event.dayStartMillis < visibleMonthStart ||
            event.dayStartMillis >= shiftLocalMonth(visibleMonthStart, 1)
        ) {
            visibleMonthStart = startOfLocalMonthMillis(event.dayStartMillis)
        }
        suppressSearchHits = true
    }

    fun selectEvent(event: CalendarEvent) {
        val next = when {
            event.isNote -> event.id
            !event.templateId.isNullOrBlank() -> "tpl:${event.templateId}"
            else -> event.task?.id ?: event.id
        }
        focusEventKey = if (focusEventKey == next) null else next
    }

    val taskCountByDay = remember(events) { events.groupingBy { it.dayStartMillis }.eachCount() }
    val dayEvents = remember(events, selectedDayStart) {
        events.filter { it.dayStartMillis == selectedDayStart }
    }
    val sections = remember(events) {
        DueDayBucket.entries.mapNotNull { bucket ->
            val items = events.filter { event ->
                val due = event.task?.dueDate ?: (event.dayStartMillis + event.sortMinutes * 60_000L)
                due.dueDayBucket(event.isCompleted) == bucket
            }
            if (items.isEmpty()) null else bucket to items
        }
    }
    val monthTitle = remember(visibleMonthStart, dateLocale) {
        SimpleDateFormat("LLLL yyyy", dateLocale).format(Date(visibleMonthStart))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(dateLocale) else it.toString() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(s.calendar, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (project != null) {
                            Text(
                                s.localized(project.name),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
                actions = {
                    IconButton(onClick = { monthMode = !monthMode }) {
                        Icon(
                            imageVector = if (monthMode) Icons.Default.ViewList else Icons.Default.CalendarViewMonth,
                            contentDescription = if (monthMode) s.calendarList else s.calendarMonth
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            if (monthMode) {
                Column(horizontalAlignment = Alignment.End) {
                    FloatingActionButton(
                        onClick = {
                            dayReminderText = ""
                            dayReminderMinutes = prefs.reminderMinutesOfDay
                            showDayReminder = true
                        },
                        modifier = Modifier.border(1.dp, buttonLine(), RoundedCornerShape(10.dp)),
                        shape = RoundedCornerShape(10.dp),
                        containerColor = buttonFill(),
                        contentColor = buttonLabel()
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = s.addDayReminder)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    FloatingActionButton(
                        onClick = { requestAddForSelectedDay() },
                        modifier = Modifier.border(1.dp, buttonLine(), RoundedCornerShape(10.dp)),
                        shape = RoundedCornerShape(10.dp),
                        containerColor = buttonFill(),
                        contentColor = buttonLabel()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = s.addTaskForDay)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (monthMode) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 160.dp)
            ) {
                item(key = "search") {
                    CalendarSearchBlock(
                        query = searchQuery,
                        onQueryChange = {
                            searchQuery = it
                            suppressSearchHits = false
                        },
                        hits = if (suppressSearchHits) emptyList() else searchHits,
                        focusTitle = highlight?.title,
                        onSelectHit = { selectSearchHit(it) },
                        onClearFocus = { focusEventKey = null }
                    )
                }
                item(key = "month-header") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { visibleMonthStart = shiftLocalMonth(visibleMonthStart, -1) }) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null)
                        }
                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = { visibleMonthStart = shiftLocalMonth(visibleMonthStart, 1) }) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                        }
                    }
                }
                item(key = "month-grid") {
                    MonthCalendarGrid(
                        monthStart = visibleMonthStart,
                        selectedDayStart = selectedDayStart,
                        taskCountByDay = taskCountByDay,
                        highlightedDays = focusDays,
                        deadlineDays = deadlineDays,
                        locale = dateLocale,
                        onSelectDay = {
                            selectedDayStart = it
                            dayListExpanded = true
                        }
                    )
                }
                if (highlight != null && (focusDays.isNotEmpty() || deadlineDays.isNotEmpty())) {
                    item(key = "focus-legend") {
                        CalendarFocusLegend(
                            showDays = focusDays.isNotEmpty(),
                            showDeadline = deadlineDays.isNotEmpty()
                        )
                    }
                }
                item(key = "day-header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { dayListExpanded = !dayListExpanded }
                            .padding(top = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = SimpleDateFormat("d MMMM", dateLocale).format(Date(selectedDayStart)),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (dayEvents.isNotEmpty()) {
                            Text(
                                text = dayEvents.size.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        Icon(
                            imageVector = if (dayListExpanded) {
                                Icons.Default.KeyboardArrowUp
                            } else {
                                Icons.Default.KeyboardArrowDown
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (dayListExpanded) {
                    if (dayEvents.isEmpty()) {
                        item(key = "day-empty") {
                            Text(
                                text = s.noTasksOnDay,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    } else {
                        items(dayEvents, key = { it.id }) { event ->
                            CalendarEventRow(
                                event = event,
                                viewModel = viewModel,
                                onOpenTask = onOpenTask,
                                onToggleCompleted = { completeTaskWithUndo(it) },
                                highlighted = CalendarEventBuilder.eventMatchesFocus(event, focusEventKey),
                                onSelect = { selectEvent(event) }
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item(key = "search-list") {
                    CalendarSearchBlock(
                        query = searchQuery,
                        onQueryChange = {
                            searchQuery = it
                            suppressSearchHits = false
                        },
                        hits = if (suppressSearchHits) emptyList() else searchHits,
                        focusTitle = highlight?.title,
                        onSelectHit = { selectSearchHit(it) },
                        onClearFocus = { focusEventKey = null }
                    )
                }
                if (events.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            s.noTasksWithDueDate,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                } else {
                    sections.forEach { (bucket, bucketEvents) ->
                        item(key = "section-$bucket") {
                            Text(
                                text = when (bucket) {
                                    DueDayBucket.OVERDUE -> s.dueOverdue
                                    DueDayBucket.TODAY -> s.dueToday
                                    DueDayBucket.THIS_WEEK -> s.dueThisWeek
                                    DueDayBucket.LATER -> s.dueLater
                                    DueDayBucket.DONE -> s.filterDone
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = when (bucket) {
                                    DueDayBucket.OVERDUE -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(bucketEvents, key = { it.id }) { event ->
                            CalendarEventRow(
                                event = event,
                                viewModel = viewModel,
                                onOpenTask = onOpenTask,
                                onToggleCompleted = { completeTaskWithUndo(it) },
                                highlighted = CalendarEventBuilder.eventMatchesFocus(event, focusEventKey),
                                onSelect = { selectEvent(event) }
                            )
                        }
                    }
                }
            }
        }
    }
    if (showDayReminder) {
        AlertDialog(
            onDismissRequest = { showDayReminder = false },
            title = { Text(s.addDayReminder, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        SimpleDateFormat("d MMMM", dateLocale).format(Date(selectedDayStart)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = dayReminderText,
                        onValueChange = { dayReminderText = it },
                        label = { Text(s.reminderTextLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    OutlinedButton(
                        onClick = {
                            android.app.TimePickerDialog(
                                context,
                                { _, hour, minute -> dayReminderMinutes = hour * 60 + minute },
                                dayReminderMinutes / 60,
                                dayReminderMinutes % 60,
                                true
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(DueReminderScheduler.formatMinutesOfDay(dayReminderMinutes))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val body = dayReminderText.trim()
                        if (body.isBlank()) return@TextButton
                        val trigger = java.util.Calendar.getInstance().apply {
                            timeInMillis = selectedDayStart
                            set(java.util.Calendar.HOUR_OF_DAY, dayReminderMinutes / 60)
                            set(java.util.Calendar.MINUTE, dayReminderMinutes % 60)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        viewModel.addStandaloneReminder(projectId, body, trigger)
                        showDayReminder = false
                        android.widget.Toast.makeText(
                            context,
                            s.reminderSaved,
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    enabled = dayReminderText.isNotBlank()
                ) { Text(s.save) }
            },
            dismissButton = {
                TextButton(onClick = { showDayReminder = false }) { Text(s.cancel) }
            }
        )
    }
}

@Composable
private fun CalendarSearchBlock(
    query: String,
    onQueryChange: (String) -> Unit,
    hits: List<CalendarEvent>,
    focusTitle: String?,
    onSelectHit: (CalendarEvent) -> Unit,
    onClearFocus: () -> Unit
) {
    val s = LocalAppStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(s.calendarSearchHint) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = s.clear)
                    }
                }
            }
        )
        if (focusTitle != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = s.calendarFocusOn(focusTitle),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = chartProgress(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                TextButton(onClick = onClearFocus) { Text(s.calendarFocusClear) }
            }
        }
        if (query.isNotBlank()) {
            hits.forEach { hit ->
                Text(
                    text = hit.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectHit(hit) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CalendarEventRow(
    event: CalendarEvent,
    viewModel: SharedViewModel,
    onOpenTask: (boardId: String, columnId: String, taskId: String) -> Unit,
    onToggleCompleted: (Task) -> Unit,
    highlighted: Boolean = false,
    onSelect: () -> Unit = {}
) {
    val task = event.task
    if (event.isNote) {
        val s = LocalAppStrings.current
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
            colors = CardDefaults.cardColors(
                containerColor = if (highlighted) {
                    chartProgress().copy(alpha = 0.16f)
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                }
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = s.noteReminderTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = DueReminderScheduler.formatMinutesOfDay(event.sortMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { viewModel.deleteReminder(event.id) }) {
                    Icon(Icons.Default.Close, contentDescription = s.deleteReminder)
                }
            }
        }
    } else if (task != null) {
        CalendarTaskCard(
            task = task,
            viewModel = viewModel,
            onOpenTask = onOpenTask,
            onToggleCompleted = { onToggleCompleted(task) },
            highlighted = highlighted,
            onSelect = onSelect
        )
    } else {
        val s = LocalAppStrings.current
        val clocks = event.startMinutes.ifEmpty { listOf(event.sortMinutes) }
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
            colors = CardDefaults.cardColors(
                containerColor = if (highlighted) {
                    chartProgress().copy(alpha = 0.16f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                }
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = if (highlighted) 2.dp else 1.dp,
                color = if (highlighted) chartProgress() else MaterialTheme.colorScheme.outlineVariant
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = clocks.joinToString(" · ") { minutes ->
                        s.startAt(DueReminderScheduler.formatMinutesOfDay(minutes))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = chartProgress(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = s.calendarProjectedBadge,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun startOfLocalMonthMillis(now: Long = System.currentTimeMillis()): Long {
    return Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun shiftLocalMonth(monthStart: Long, deltaMonths: Int): Long {
    return Calendar.getInstance().apply {
        timeInMillis = monthStart
        add(Calendar.MONTH, deltaMonths)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

@Composable
private fun MonthCalendarGrid(
    monthStart: Long,
    selectedDayStart: Long,
    taskCountByDay: Map<Long, Int>,
    highlightedDays: Set<Long> = emptySet(),
    deadlineDays: Set<Long> = emptySet(),
    locale: Locale,
    onSelectDay: (Long) -> Unit
) {
    val todayStart = remember { startOfLocalDayMillis() }
    val cells = remember(monthStart) { buildMonthCells(monthStart) }
    val weekdays = remember(locale) {
        val symbols = java.text.DateFormatSymbols(locale)
        val names = symbols.shortWeekdays
        listOf(names[2], names[3], names[4], names[5], names[6], names[7], names[1])
    }
    val weekendTint = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
    val focusTint = chartProgress().copy(alpha = 0.22f)
    val deadlineTint = chartForecast().copy(alpha = 0.22f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdays.forEachIndexed { index, label ->
                val weekendHeader = index >= 5
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (weekendHeader) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                week.forEach { dayStart ->
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp)) {
                        if (dayStart != null) {
                            val count = taskCountByDay[dayStart] ?: 0
                            val selected = dayStart == selectedDayStart
                            val isToday = dayStart == todayStart
                            val isWeekend = CalendarEventBuilder.isWeekend(dayStart)
                            val isFocus = dayStart in highlightedDays
                            val isDeadline = dayStart in deadlineDays
                            val dayNum = Calendar.getInstance().apply { timeInMillis = dayStart }
                                .get(Calendar.DAY_OF_MONTH)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            selected -> MaterialTheme.colorScheme.primary
                                            isDeadline && !isFocus -> deadlineTint
                                            isFocus -> focusTint
                                            isToday -> MaterialTheme.colorScheme.primaryContainer
                                            isWeekend -> weekendTint
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable { onSelectDay(dayStart) }
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected || isToday || isFocus || isDeadline) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Medium
                                    },
                                    color = when {
                                        selected -> MaterialTheme.colorScheme.onPrimary
                                        isDeadline && !isFocus -> chartForecast()
                                        isFocus -> chartProgress()
                                        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                        isWeekend -> MaterialTheme.colorScheme.onSecondaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (isDeadline) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(chartForecast())
                                    )
                                } else if (count > 0) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (selected) MaterialTheme.colorScheme.onPrimary
                                                else if (isFocus) chartProgress()
                                                else MaterialTheme.colorScheme.primary
                                            )
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarFocusLegend(showDays: Boolean, showDeadline: Boolean) {
    val s = LocalAppStrings.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showDays) {
            LegendSwatch(color = chartProgress(), label = s.calendarLegendDays)
        }
        if (showDeadline) {
            LegendSwatch(color = chartForecast(), label = s.calendarLegendDeadline)
        }
    }
}

@Composable
private fun LegendSwatch(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun buildMonthCells(monthStart: Long): List<Long?> {
    val cal = Calendar.getInstance().apply { timeInMillis = monthStart }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDow = ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7)
    val cells = MutableList<Long?>(firstDow) { null }
    repeat(daysInMonth) { index ->
        val dayCal = Calendar.getInstance().apply {
            timeInMillis = monthStart
            set(Calendar.DAY_OF_MONTH, index + 1)
        }
        cells.add(startOfLocalDayMillis(dayCal.timeInMillis))
    }
    while (cells.size % 7 != 0) cells.add(null)
    return cells
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun CalendarTaskCard(
    task: Task,
    viewModel: SharedViewModel,
    onOpenTask: (boardId: String, columnId: String, taskId: String) -> Unit,
    onToggleCompleted: () -> Unit,
    highlighted: Boolean = false,
    onSelect: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val s = LocalAppStrings.current
    val dateLocale = LocalAppLanguage.current.locale
    val context = LocalContext.current
    val prefs = remember { KairosPreferences(context) }
    val isOverdue = task.isPastDeadline()
    val homeColumn = state.columns.find { it.id == task.columnId }
        ?: state.columns.find { it.id in task.linkedColumnIds }
    val board = homeColumn?.let { col -> state.boards.find { it.id == col.boardId } }
    val project = board?.let { b -> state.projects.find { it.id == b.projectId } }
    val location = listOfNotNull(
        project?.name,
        board?.name?.let { s.localized(it) },
        homeColumn?.title?.let { s.localized(it) },
        if (task.isBoardArchived) s.statsTabArchive else null
    ).joinToString(" · ")
    val hasLinks = task.linkedColumnIds.any { linkId ->
        val boardName = state.columns.find { it.id == linkId }
            ?.let { col -> state.boards.find { it.id == col.boardId }?.name }
        boardName != null && !KanbanNames.isEisenhowerBoard(boardName)
    }
    var showDateEditor by remember(task.id) { mutableStateOf(false) }
    var showDatePicker by remember(task.id) { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = task.dueDate?.let { localMillisToUtcPicker(it) }
    )

    if (showDateEditor) {
        AlertDialog(
            onDismissRequest = { showDateEditor = false },
            title = { DialogMarqueeText(text = s.changeDueDate, fontWeight = FontWeight.Bold, maxLines = 2) },
            text = {
                DueDateQuickPick(
                    selectedDueDate = task.dueDate,
                    onSelect = {
                        viewModel.updateTask(
                            task.copy(
                                dueDate = it,
                                reminderMinutesOfDay = if (it == null) null else task.reminderMinutesOfDay
                            )
                        )
                        showDateEditor = false
                    },
                    onPickCustom = { showDatePicker = true }
                )
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDateEditor = false }) { Text(s.cancel) }
            }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = datePickerState.selectedDateMillis
                        if (selected != null) {
                            viewModel.updateTask(task.copy(dueDate = utcPickerMillisToLocalNoon(selected)))
                        }
                        showDatePicker = false
                        showDateEditor = false
                    }
                ) { Text(s.ok) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(s.cancel) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onSelect,
                onLongClick = { showDateEditor = true }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) chartProgress().copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (highlighted) 2.dp else 1.dp,
            color = when {
                highlighted -> chartProgress()
                isOverdue -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        ),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleCompleted,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = s.completed,
                    tint = if (task.isCompleted) completedMark() else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (location.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hasLinks) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (task.eisenhowerQuadrant != null) {
                        Text(
                            text = Eisenhower.getBadge(task.eisenhowerQuadrant),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                lineHeight = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = task.dueDate?.relativeDueLabel(
                            s,
                            dateLocale,
                            task.isCompleted,
                            reminderMinutesOfDay = task.startMinutesOfDay ?: task.reminderMinutesOfDay,
                            defaultReminderMinutes = prefs.reminderMinutesOfDay,
                            markOverdue = isOverdue
                        ).orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (task.isCompleted && task.completedAt != null) {
                        Text(
                            text = s.closedAt(task.completedAt.formatDateTimeShort(dateLocale)),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (homeColumn != null) {
                        TextButton(
                            onClick = { onOpenTask(homeColumn.boardId, homeColumn.id, task.id) },
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                        ) {
                            Text(s.calendarOpenTask, maxLines = 1)
                        }
                    }
                    val taskCommentsCount = state.comments.count { it.taskId == task.id }
                    if (taskCommentsCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Comment,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = taskCommentsCount.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
