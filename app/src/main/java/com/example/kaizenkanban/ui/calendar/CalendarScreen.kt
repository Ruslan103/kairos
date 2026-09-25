package com.example.kaizenkanban.ui.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import com.example.kaizenkanban.domain.model.Eisenhower
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.ui.i18n.AppStrings
import com.example.kaizenkanban.ui.i18n.LocalAppLanguage
import com.example.kaizenkanban.ui.i18n.LocalAppStrings
import com.example.kaizenkanban.ui.theme.eisenhowerSolid
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
    now: Long = System.currentTimeMillis()
): String {
    if (isCompleted) return formatDateShort(locale)
    val start = startOfLocalDayMillis(now)
    val dayLabel = when {
        this < start -> strings.dueOverdue
        this < start + DAY_MS -> strings.dueToday
        this < start + 2 * DAY_MS -> strings.dueTomorrow
        else -> formatDateShort(locale)
    }
    val time = formatDueClock(this)
    return if (time != null) "$dayLabel $time" else dayLabel
}

/** HH:mm when the due instant has a meaningful clock time (not midnight). */
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
    viewModel: SharedViewModel,
    onBack: () -> Unit,
    onOpenTask: (boardId: String, columnId: String, taskId: String) -> Unit = { _, _, _ -> }
) {
    val state by viewModel.state.collectAsState()
    val s = LocalAppStrings.current
    val dateLocale = LocalAppLanguage.current.locale
    val context = androidx.compose.ui.platform.LocalContext.current
    var showCompleted by rememberSaveable { mutableStateOf(false) }
    var monthMode by rememberSaveable { mutableStateOf(true) }
    var visibleMonthStart by remember {
        mutableStateOf(startOfLocalMonthMillis())
    }
    var selectedDayStart by remember {
        mutableStateOf(startOfLocalDayMillis())
    }
    var showAddForDay by remember { mutableStateOf(false) }
    var addTitle by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

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

    fun submitAddForDay() {
        val title = addTitle.trim()
        if (title.isBlank()) return
        val primary = state.boards.find { it.isDefault }
        if (primary == null) {
            android.widget.Toast.makeText(context, s.noPrimaryBoard, android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val inbox = state.columns
            .filter { it.boardId == primary.id }
            .minByOrNull { it.position }
        if (inbox == null) {
            android.widget.Toast.makeText(context, s.boardHasNoHubs, android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val dueAt = Calendar.getInstance().apply {
            timeInMillis = selectedDayStart
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        viewModel.addTask(
            title = title,
            columnId = inbox.id,
            categoryId = null,
            dueDate = dueAt,
            currentTasks = state.tasks
        )
        android.widget.Toast.makeText(context, s.taskAdded, android.widget.Toast.LENGTH_SHORT).show()
        addTitle = ""
        showAddForDay = false
    }

    val tasksWithDates = remember(state.tasks, showCompleted) {
        state.tasks
            .filter { it.dueDate != null && (showCompleted || !it.isCompleted) }
            .sortedWith(compareBy({ it.isCompleted }, { it.dueDate }))
    }

    val sections = remember(tasksWithDates) {
        DueDayBucket.entries.mapNotNull { bucket ->
            val items = tasksWithDates.filter { task ->
                task.dueDate!!.dueDayBucket(task.isCompleted) == bucket
            }
            if (items.isEmpty()) null else bucket to items
        }
    }

    val taskCountByDay = remember(tasksWithDates) {
        tasksWithDates
            .mapNotNull { it.dueDate }
            .groupingBy { startOfLocalDayMillis(it) }
            .eachCount()
    }

    val dayTasks = remember(tasksWithDates, selectedDayStart) {
        tasksWithDates.filter { task ->
            task.dueDate != null && startOfLocalDayMillis(task.dueDate) == selectedDayStart
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
                    Text(
                        s.calendar,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                    IconButton(onClick = { showCompleted = !showCompleted }) {
                        Icon(
                            imageVector = if (showCompleted) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showCompleted) s.hideCompleted else s.showCompleted
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
                FloatingActionButton(onClick = { showAddForDay = true }) {
                    Icon(Icons.Default.Add, contentDescription = s.addTaskForDay)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (monthMode) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                item(key = "month-header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                visibleMonthStart = shiftLocalMonth(visibleMonthStart, -1)
                            }
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowLeft,
                                contentDescription = null
                            )
                        }
                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = {
                                visibleMonthStart = shiftLocalMonth(visibleMonthStart, 1)
                            }
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowRight,
                                contentDescription = null
                            )
                        }
                    }
                }
                item(key = "month-grid") {
                    MonthCalendarGrid(
                        monthStart = visibleMonthStart,
                        selectedDayStart = selectedDayStart,
                        taskCountByDay = taskCountByDay,
                        locale = dateLocale,
                        onSelectDay = { selectedDayStart = it }
                    )
                }
                item(key = "day-header") {
                    val dayLabel = SimpleDateFormat("d MMMM", dateLocale)
                        .format(Date(selectedDayStart))
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                if (dayTasks.isEmpty()) {
                    item(key = "day-empty") {
                        Text(
                            text = s.noTasksOnDay,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        OutlinedButton(
                            onClick = { showAddForDay = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(s.addTaskForDay)
                        }
                    }
                } else {
                    items(dayTasks, key = { it.id }) { task ->
                        CalendarTaskCard(
                            task = task,
                            viewModel = viewModel,
                            onOpenTask = onOpenTask,
                            onToggleCompleted = { completeTaskWithUndo(task) }
                        )
                    }
                }
            }
        } else if (tasksWithDates.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    s.noTasksWithDueDate,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                sections.forEach { (bucket, tasks) ->
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
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(tasks, key = { it.id }) { task ->
                        CalendarTaskCard(
                            task = task,
                            viewModel = viewModel,
                            onOpenTask = onOpenTask,
                            onToggleCompleted = { completeTaskWithUndo(task) }
                        )
                    }
                }
            }
        }
    }

    if (showAddForDay) {
        AlertDialog(
            onDismissRequest = { showAddForDay = false },
            title = { Text(s.addTaskForDay) },
            text = {
                OutlinedTextField(
                    value = addTitle,
                    onValueChange = { addTitle = it },
                    label = { Text(s.newTask) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { submitAddForDay() }) { Text(s.create) }
            },
            dismissButton = {
                TextButton(onClick = {
                    addTitle = ""
                    showAddForDay = false
                }) { Text(s.cancel) }
            }
        )
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
    locale: Locale,
    onSelectDay: (Long) -> Unit
) {
    val todayStart = remember { startOfLocalDayMillis() }
    val cells = remember(monthStart) { buildMonthCells(monthStart) }
    val weekdays = remember(locale) {
        val symbols = java.text.DateFormatSymbols(locale)
        val names = symbols.shortWeekdays
        // Calendar: 1=Sunday … 7=Saturday; Monday-first grid
        listOf(names[2], names[3], names[4], names[5], names[6], names[7], names[1])
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdays.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                week.forEach { dayStart ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                    ) {
                        if (dayStart != null) {
                            val count = taskCountByDay[dayStart] ?: 0
                            val selected = dayStart == selectedDayStart
                            val isToday = dayStart == todayStart
                            val dayNum = Calendar.getInstance().apply {
                                timeInMillis = dayStart
                            }.get(Calendar.DAY_OF_MONTH)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            selected -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primaryContainer
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
                                    fontWeight = if (selected || isToday) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        selected -> MaterialTheme.colorScheme.onPrimary
                                        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (count > 0) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (selected) {
                                                    MaterialTheme.colorScheme.onPrimary
                                                } else {
                                                    MaterialTheme.colorScheme.primary
                                                }
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

private fun buildMonthCells(monthStart: Long): List<Long?> {
    val cal = Calendar.getInstance().apply { timeInMillis = monthStart }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    // Monday=0 … Sunday=6
    val firstDow = ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7)
    val cells = MutableList<Long?>(firstDow) { null }
    repeat(daysInMonth) { index ->
        val dayCal = Calendar.getInstance().apply {
            timeInMillis = monthStart
            set(Calendar.DAY_OF_MONTH, index + 1)
        }
        cells.add(startOfLocalDayMillis(dayCal.timeInMillis))
    }
    while (cells.size % 7 != 0) {
        cells.add(null)
    }
    return cells
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun CalendarTaskCard(
    task: Task,
    viewModel: SharedViewModel,
    onOpenTask: (boardId: String, columnId: String, taskId: String) -> Unit,
    onToggleCompleted: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val s = LocalAppStrings.current
    val dateLocale = LocalAppLanguage.current.locale
    val isOverdue = !task.isCompleted && task.dueDate != null && task.dueDate.isOverdueDate()
    val homeColumn = state.columns.find { it.id == task.columnId }
        ?: state.columns.find { it.id in task.linkedColumnIds }
    val board = homeColumn?.let { col -> state.boards.find { it.id == col.boardId } }
    val project = board?.let { b -> state.projects.find { it.id == b.projectId } }
    val location = listOfNotNull(
        project?.name,
        board?.name?.let { s.localized(it) },
        homeColumn?.title?.let { s.localized(it) }
    ).joinToString(" · ")
    val hasLinks = task.linkedColumnIds.isNotEmpty() || task.eisenhowerQuadrant != null
    var showDateEditor by remember(task.id) { mutableStateOf(false) }
    var showDatePicker by remember(task.id) { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = task.dueDate?.let { localMillisToUtcPicker(it) }
    )

    if (showDateEditor) {
        AlertDialog(
            onDismissRequest = { showDateEditor = false },
            title = { Text(s.changeDueDate) },
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
                onClick = {
                    val column = homeColumn ?: return@combinedClickable
                    onOpenTask(column.boardId, column.id, task.id)
                },
                onLongClick = { showDateEditor = true }
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
        ),
        shape = RoundedCornerShape(8.dp),
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
                    tint = if (task.isCompleted) Color(0xFF00C853) else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
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
                    Text(
                        text = task.dueDate?.relativeDueLabel(s, dateLocale, task.isCompleted).orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (task.eisenhowerQuadrant != null) {
                        val qColor = eisenhowerSolid(task.eisenhowerQuadrant)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = qColor.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, qColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = Eisenhower.getBadge(task.eisenhowerQuadrant),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = qColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
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
