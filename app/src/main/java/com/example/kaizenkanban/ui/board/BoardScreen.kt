package com.example.kaizenkanban.ui.board

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import com.example.kaizenkanban.domain.model.Eisenhower
import com.example.kaizenkanban.domain.model.TaskRepeat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.kaizenkanban.data.local.KairosPreferences
import com.example.kaizenkanban.reminders.DueReminderScheduler
import com.example.kaizenkanban.ui.calendar.DueDateQuickPick
import com.example.kaizenkanban.ui.calendar.isDueToday
import com.example.kaizenkanban.ui.calendar.isOverdueDate
import com.example.kaizenkanban.ui.calendar.localMillisToUtcPicker
import com.example.kaizenkanban.ui.calendar.relativeDueLabel
import com.example.kaizenkanban.ui.calendar.utcPickerMillisToLocalNoon
import com.example.kaizenkanban.ui.i18n.KanbanNames
import com.example.kaizenkanban.ui.i18n.LocalAppLanguage
import com.example.kaizenkanban.ui.i18n.LocalAppStrings
import com.example.kaizenkanban.ui.onboarding.PlanningGuideDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.kaizenkanban.domain.model.Board
import com.example.kaizenkanban.domain.model.Column
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.domain.model.Category
import com.example.kaizenkanban.domain.model.Comment
import com.example.kaizenkanban.domain.model.ColumnComment
import com.example.kaizenkanban.domain.model.Project
import com.example.kaizenkanban.ui.theme.eisenhowerGradient
import com.example.kaizenkanban.ui.theme.eisenhowerOnColor
import com.example.kaizenkanban.ui.theme.eisenhowerSolid
import com.example.kaizenkanban.ui.theme.modeSwitchGradient
import com.example.kaizenkanban.ui.theme.getStatusBrush
import com.example.kaizenkanban.ui.theme.deleteButtonGradient
import com.example.kaizenkanban.ui.theme.deleteButtonColor
import com.example.kaizenkanban.ui.theme.newProjectGradient
import com.example.kaizenkanban.ui.theme.inProgressColor
import com.example.kaizenkanban.ui.theme.inProgressGradient
import com.example.kaizenkanban.ui.viewmodel.SharedViewModel

fun Long.formatDate(locale: Locale = Locale.getDefault()): String = SimpleDateFormat("dd MMM yyyy", locale).format(Date(this))
fun Long.formatDateTime(locale: Locale = Locale.getDefault()): String = SimpleDateFormat("dd MMM yyyy, HH:mm", locale).format(Date(this))

private data class TaskDropPreview(
    val columnId: String,
    val insertIndex: Int
)

data class RelatedBoardLink(
    val board: Board,
    val columnId: String,
    val hubTitle: String
)

private fun hubOnBoard(
    task: Task,
    board: Board,
    columns: List<Column>
): Column? {
    val boardColumns = columns.filter { it.boardId == board.id }.sortedBy { it.position }
    if (boardColumns.isEmpty()) return null
    boardColumns.find { it.id == task.columnId }?.let { return it }
    boardColumns.find { task.linkedColumnIds.contains(it.id) }?.let { return it }
    if (KanbanNames.isEisenhowerBoard(board.name) && task.eisenhowerQuadrant != null) {
        val index = when (task.eisenhowerQuadrant) {
            Eisenhower.Q1 -> 0
            Eisenhower.Q2 -> 1
            Eisenhower.Q3 -> 2
            else -> 3
        }
        return boardColumns.getOrNull(index) ?: boardColumns.first()
    }
    return boardColumns.first()
}

private fun quadrantForColumn(column: Column, allColumns: List<Column>): String {
    val boardCols = allColumns.filter { it.boardId == column.boardId }.sortedBy { it.position }
    val index = boardCols.indexOfFirst { it.id == column.id }.coerceAtLeast(0)
    return when {
        KanbanNames.isQ1Hub(column.title) || index == 0 -> Eisenhower.Q1
        KanbanNames.isQ2Hub(column.title) || index == 1 -> Eisenhower.Q2
        KanbanNames.isQ3Hub(column.title) || index == 2 -> Eisenhower.Q3
        else -> Eisenhower.Q4
    }
}

private fun taskAppearsOnColumn(
    task: Task,
    column: Column,
    allColumns: List<Column>,
    boards: List<Board>
): Boolean {
    if (task.isOnColumn(column.id)) return true
    val board = boards.find { it.id == column.boardId } ?: return false
    if (!KanbanNames.isEisenhowerBoard(board.name) || task.eisenhowerQuadrant == null) return false
    return task.eisenhowerQuadrant == quadrantForColumn(column, allColumns)
}

private fun relatedBoardsForTask(
    task: Task,
    currentBoardId: String,
    columns: List<Column>,
    boards: List<Board>
): List<RelatedBoardLink> {
    val columnIds = buildSet {
        add(task.columnId)
        addAll(task.linkedColumnIds)
    }
    val boardIds = columns
        .filter { it.id in columnIds }
        .map { it.boardId }
        .toMutableSet()
    if (task.eisenhowerQuadrant != null) {
        val homeProjectId = columns.find { it.id == task.columnId }
            ?.let { col -> boards.find { it.id == col.boardId }?.projectId }
        boards.filter {
            KanbanNames.isEisenhowerBoard(it.name) &&
                (homeProjectId == null || it.projectId == homeProjectId)
        }.forEach { boardIds += it.id }
    }
    return boards
        .filter { it.id in boardIds && it.id != currentBoardId }
        .distinctBy { it.id }
        .mapNotNull { board ->
            val hub = hubOnBoard(task, board, columns) ?: return@mapNotNull null
            RelatedBoardLink(board = board, columnId = hub.id, hubTitle = hub.title)
        }
}

@Composable
private fun RelatedBoardsButton(
    links: List<RelatedBoardLink>,
    onOpen: (RelatedBoardLink) -> Unit,
    iconTint: Color,
    background: Color,
    size: androidx.compose.ui.unit.Dp = 36.dp
) {
    if (links.isEmpty()) return
    val s = LocalAppStrings.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(10.dp))
                .background(background)
                .clickable {
                    if (links.size == 1) onOpen(links.first())
                    else expanded = true
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = s.relatedBoards,
                modifier = Modifier.size(18.dp),
                tint = iconTint
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Text(
                text = s.relatedBoards,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            links.forEach { link ->
                DropdownMenuItem(
                    text = {
                        androidx.compose.foundation.layout.Column {
                            Text(
                                text = s.localized(link.board.name),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = s.localized(link.hubTitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onOpen(link)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BoardScreen(
    boardId: String,
    viewModel: SharedViewModel,
    windowSizeClass: WindowWidthSizeClass,
    onBack: () -> Unit,
    onNavigateToCalendar: () -> Unit = {},
    initialColumnId: String? = null,
    initialTaskId: String? = null
) {
    val context = LocalContext.current
    val s = LocalAppStrings.current
    val dateLocale = LocalAppLanguage.current.locale
    val kairosPrefs = remember { KairosPreferences(context) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val state by viewModel.state.collectAsState()

    var activeBoardId by rememberSaveable(boardId) { mutableStateOf(boardId) }
    var pendingHubColumnId by remember { mutableStateOf(initialColumnId?.takeIf { it.isNotBlank() }) }
    var highlightedTaskId by remember { mutableStateOf(initialTaskId?.takeIf { it.isNotBlank() }) }
    LaunchedEffect(initialTaskId) {
        val id = initialTaskId?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        highlightedTaskId = id
        delay(2800)
        if (highlightedTaskId == id) highlightedTaskId = null
    }
    var boardSearchOpen by rememberSaveable { mutableStateOf(false) }
    var boardSearchQuery by rememberSaveable { mutableStateOf("") }
    var boardOverdueOnly by rememberSaveable { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    var boardNavStack by remember(boardId) { mutableStateOf(listOf(boardId)) }
    val board = state.boards.find { it.id == activeBoardId } ?: state.boards.find { it.id == boardId } ?: state.boards.firstOrNull()
    val currentBoardId = board?.id ?: activeBoardId
    val columns = remember(state.columns, currentBoardId) {
        state.columns.filter { it.boardId == currentBoardId }.sortedBy { it.position }
    }
    val isEisenhowerBoard = KanbanNames.isEisenhowerBoard(board?.name)
    val isOkrBoard = KanbanNames.isOkrBoard(board?.name)
    val tasks = state.tasks // All tasks, we'll filter them by columnId and Eisenhower quadrant
    val categories = state.categories
    val comments = state.comments
    val commentCountByTaskId = remember(comments) {
        comments.groupingBy { it.taskId }.eachCount()
    }
    val columnCommentCountById = remember(state.columnComments, columns) {
        val hubIds = columns.mapTo(mutableSetOf()) { it.id }
        state.columnComments
            .filter { it.columnId in hubIds }
            .groupingBy { it.columnId }
            .eachCount()
    }
    val columnIdToBoardId = remember(state.columns) {
        state.columns.associate { it.id to it.boardId }
    }
    val boardIdToProjectId = remember(state.boards) {
        state.boards.associate { it.id to it.projectId }
    }

    fun getQuadrantForCol(col: Column, colIndex: Int): String? {
        if (!isEisenhowerBoard) return null
        return when {
            KanbanNames.isQ1Hub(col.title) || colIndex == 0 -> Eisenhower.Q1
            KanbanNames.isQ2Hub(col.title) || colIndex == 1 -> Eisenhower.Q2
            KanbanNames.isQ3Hub(col.title) || colIndex == 2 -> Eisenhower.Q3
            KanbanNames.isQ4Hub(col.title) || colIndex == 3 -> Eisenhower.Q4
            else -> null
        }
    }

    fun sortHubTasks(columnId: String, hubTasks: List<Task>): List<Task> {
        val autoCompare = compareBy<Task> { it.isCompleted }
            .thenBy { it.isHidden }
            .thenBy { task ->
                val due = task.dueDate
                when {
                    due == null || task.isCompleted -> 3
                    due.isOverdueDate() -> 0
                    due.isDueToday() -> 1
                    else -> 2
                }
            }
            .thenBy { it.dueDate ?: Long.MAX_VALUE }
            .thenBy { Eisenhower.getRank(it.eisenhowerQuadrant) }
            .thenBy { it.position }

        if (!kairosPrefs.isColumnManuallyOrdered(columnId)) {
            return hubTasks.sortedWith(autoCompare)
        }
        // Manual order: homes follow rewritten `position` (DnD / hub sort). Do not re-bucket by due.
        // Visitors keep auto ordering among themselves after homes in the same completed/hidden group.
        return hubTasks.sortedWith(
            compareBy<Task> { it.isCompleted }
                .thenBy { it.isHidden }
                .thenBy { if (it.columnId == columnId) 0 else 1 }
                .thenComparator { a, b ->
                    val aHome = a.columnId == columnId
                    val bHome = b.columnId == columnId
                    when {
                        aHome && bHome -> a.position.compareTo(b.position)
                        !aHome && !bHome -> autoCompare.compare(a, b)
                        else -> 0
                    }
                }
        )
    }

    /** Hub membership without search / overdue-only filters (for sort & full reorder). */
    fun hubTasksBase(column: Column, colIndex: Int): List<Task> {
        val projectId = board?.projectId
        val colQuadrant = getQuadrantForCol(column, colIndex)
        return if (isEisenhowerBoard && colQuadrant != null) {
            tasks.filter { task ->
                val homeBoardId = columnIdToBoardId[task.columnId]
                val homeProjectId = homeBoardId?.let { boardIdToProjectId[it] }
                val sameProject = homeProjectId == null || homeProjectId == projectId
                sameProject && (
                    (task.columnId == column.id && task.eisenhowerQuadrant == null) ||
                        task.eisenhowerQuadrant == colQuadrant ||
                        task.linkedColumnIds.contains(column.id)
                    )
            }
        } else {
            tasks.filter { it.isOnColumn(column.id) }
        }
    }

    val tasksByHubId = remember(
        tasks,
        columns,
        currentBoardId,
        isEisenhowerBoard,
        board?.projectId,
        columnIdToBoardId,
        boardIdToProjectId,
        boardSearchQuery,
        boardOverdueOnly
    ) {
        val projectId = board?.projectId
        val query = boardSearchQuery.trim()
        val columnIdsOnBoard = columns.mapTo(mutableSetOf()) { it.id }
        // Narrow candidate set once before per-hub filters (faster board open / switch).
        val candidateTasks = if (isEisenhowerBoard) {
            tasks.filter { task ->
                val homeBoardId = columnIdToBoardId[task.columnId]
                val homeProjectId = homeBoardId?.let { boardIdToProjectId[it] }
                homeProjectId == null || homeProjectId == projectId ||
                    task.linkedColumnIds.any { it in columnIdsOnBoard }
            }
        } else {
            tasks.filter { task ->
                task.columnId in columnIdsOnBoard ||
                    task.linkedColumnIds.any { it in columnIdsOnBoard }
            }
        }
        buildMap {
            columns.forEachIndexed { colIndex, column ->
                val colQuadrant = when {
                    !isEisenhowerBoard -> null
                    KanbanNames.isQ1Hub(column.title) || colIndex == 0 -> Eisenhower.Q1
                    KanbanNames.isQ2Hub(column.title) || colIndex == 1 -> Eisenhower.Q2
                    KanbanNames.isQ3Hub(column.title) || colIndex == 2 -> Eisenhower.Q3
                    KanbanNames.isQ4Hub(column.title) || colIndex == 3 -> Eisenhower.Q4
                    else -> null
                }
                val unsorted = if (isEisenhowerBoard && colQuadrant != null) {
                    candidateTasks.filter { task ->
                        val homeBoardId = columnIdToBoardId[task.columnId]
                        val homeProjectId = homeBoardId?.let { boardIdToProjectId[it] }
                        val sameProject = homeProjectId == null || homeProjectId == projectId
                        sameProject && (
                            (task.columnId == column.id && task.eisenhowerQuadrant == null) ||
                                task.eisenhowerQuadrant == colQuadrant ||
                                task.linkedColumnIds.contains(column.id)
                            )
                    }
                } else {
                    candidateTasks.filter { it.isOnColumn(column.id) }
                }
                val sorted = sortHubTasks(column.id, unsorted)
                val searched = if (query.isEmpty()) sorted
                else sorted.filter { it.title.contains(query, ignoreCase = true) }
                put(
                    column.id,
                    if (!boardOverdueOnly) searched
                    else searched.filter { !it.isCompleted && it.dueDate != null && it.dueDate.isOverdueDate() }
                )
            }
        }
    }

    fun tasksInHub(column: Column, colIndex: Int = 0): List<Task> =
        tasksByHubId[column.id] ?: emptyList()

    fun openBoardAtHub(targetBoardId: String, targetColumnId: String? = null) {
        if (targetColumnId != null) pendingHubColumnId = targetColumnId
        if (activeBoardId != targetBoardId) {
            boardNavStack = boardNavStack + targetBoardId
            activeBoardId = targetBoardId
        }
    }

    BackHandler {
        when {
            boardSearchOpen -> {
                boardSearchOpen = false
                boardSearchQuery = ""
                keyboardController?.hide()
            }
            boardNavStack.size > 1 -> {
                val next = boardNavStack.dropLast(1)
                boardNavStack = next
                activeBoardId = next.last()
            }
            activeBoardId != boardId -> activeBoardId = boardId
            else -> onBack()
        }
    }

    val otherBoards = remember(state.boards, currentBoardId) {
        state.boards.filter { it.id != currentBoardId && !it.isArchived }
    }

    var isAddingTaskToColumn by remember { mutableStateOf<String?>(null) }
    val taskFocusRequester = remember { FocusRequester() }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskCategory by remember { mutableStateOf<Category?>(null) }
    var newTaskDueDate by remember { mutableStateOf<Long?>(null) }
    var newTaskShowEisenhower by remember { mutableStateOf(true) }
    var newTaskQuadrant by remember { mutableStateOf<String?>(null) }
    var newTaskRepeatRule by remember { mutableStateOf<String?>(null) }
    var newTaskReminderMinutes by remember { mutableStateOf<Int?>(null) }
    var showNewTaskDatePicker by remember { mutableStateOf(false) }
    val newDatePickerState = rememberDatePickerState()
    
    var isAddingColumn by remember { mutableStateOf(false) }
    var newColumnTitle by remember { mutableStateOf("") }
    
    var columnToRename by remember { mutableStateOf<Column?>(null) }
    var renameColumnTitle by remember { mutableStateOf("") }
    var columnToDelete by remember { mutableStateOf<Column?>(null) }
    
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var editTaskTitle by remember { mutableStateOf("") }
    var editTaskCategory by remember { mutableStateOf<Category?>(null) }
    var editTaskDueDate by remember { mutableStateOf<Long?>(null) }
    var editTaskShowEisenhower by remember { mutableStateOf(false) }
    var editTaskQuadrant by remember { mutableStateOf<String?>(null) }
    var editTaskRepeatRule by remember { mutableStateOf<String?>(null) }
    var editTaskReminderMinutes by remember { mutableStateOf<Int?>(null) }
    var showEditTaskDatePicker by remember { mutableStateOf(false) }
    val editDatePickerState = rememberDatePickerState()
    
    var showManageCategories by remember { mutableStateOf(false) }
    var showReorderColumnsDialog by remember { mutableStateOf(false) }
    var showColumnHeaders by rememberSaveable { mutableStateOf(false) }
    var showBoardsList by rememberSaveable { mutableStateOf(false) }
    var showTopBar by rememberSaveable { mutableStateOf(true) }
    var topBarMenuExpanded by remember { mutableStateOf(false) }
    val compactWidth = windowSizeClass == WindowWidthSizeClass.Compact
    var taskForComments by remember { mutableStateOf<Task?>(null) }
    var columnForComments by remember { mutableStateOf<Column?>(null) }
    var taskForMove by remember { mutableStateOf<Task?>(null) }
    var taskForMoveHubId by remember { mutableStateOf<String?>(null) }
    var pendingExpandBoardId by remember { mutableStateOf<String?>(null) }
    var inProgressTaskId by rememberSaveable { mutableStateOf(kairosPrefs.inProgressTaskId) }
    var enterAddsTask by remember { mutableStateOf(kairosPrefs.enterAddsTask) }
    LaunchedEffect(isAddingTaskToColumn, taskToEdit) {
        if (isAddingTaskToColumn != null || taskToEdit != null) {
            enterAddsTask = kairosPrefs.enterAddsTask
        }
    }
    var isExecutionMode by remember { mutableStateOf(kairosPrefs.isExecutionMode) }
    var showOkrPlanningGuide by remember {
        mutableStateOf(isOkrBoard && !kairosPrefs.hasSeenOkrPlanningGuide)
    }
    LaunchedEffect(isOkrBoard, currentBoardId) {
        if (isOkrBoard && !kairosPrefs.hasSeenOkrPlanningGuide) {
            showOkrPlanningGuide = true
        }
    }

    LaunchedEffect(tasks) {
        if (inProgressTaskId != null) {
            val currentTask = tasks.find { it.id == inProgressTaskId }
            if (currentTask == null || currentTask.isCompleted) {
                inProgressTaskId = null
                kairosPrefs.inProgressTaskId = null
            }
        }
    }

    val lazyListState = rememberLazyListState()
    val tabRowState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)

    LaunchedEffect(currentBoardId, pendingHubColumnId, columns) {
        val hubId = pendingHubColumnId ?: return@LaunchedEffect
        val index = columns.indexOfFirst { it.id == hubId }
        if (index >= 0) {
            lazyListState.scrollToItem(index)
            pendingHubColumnId = null
            return@LaunchedEffect
        }
        val hubBoardId = state.columns.find { it.id == hubId }?.boardId
        if (hubBoardId == null || hubBoardId == currentBoardId) {
            pendingHubColumnId = null
        }
    }

    val currentColumnIndex by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex.coerceIn(0, (columns.size - 1).coerceAtLeast(0))
        }
    }

    LaunchedEffect(currentColumnIndex) {
        if (columns.isNotEmpty()) {
            tabRowState.scrollToItem((currentColumnIndex - 1).coerceAtLeast(0))
        }
    }

    // Drag state — position floats stay out of composition so DnD doesn't rebuild the whole board
    var draggedTask by remember { mutableStateOf<Task?>(null) }
    val dragPosX = remember { mutableFloatStateOf(0f) }
    val dragPosY = remember { mutableFloatStateOf(0f) }
    val columnBounds = remember { mutableMapOf<String, androidx.compose.ui.geometry.Rect>() }
    val taskBounds = remember { mutableMapOf<String, androidx.compose.ui.geometry.Rect>() }
    val contentWidthPx = remember { mutableFloatStateOf(0f) }
    val contentOriginX = remember { mutableFloatStateOf(0f) }
    val contentOriginY = remember { mutableFloatStateOf(0f) }
    val hubScrollStates = remember { mutableStateMapOf<String, androidx.compose.foundation.lazy.LazyListState>() }
    val hubListBounds = remember { mutableMapOf<String, androidx.compose.ui.geometry.Rect>() }
    val otherBoardBounds = remember { mutableMapOf<String, androidx.compose.ui.geometry.Rect>() }
    var lastDropPreview by remember { mutableStateOf<TaskDropPreview?>(null) }
    var dropPreview by remember { mutableStateOf<TaskDropPreview?>(null) }
    var hoveredOtherBoardId by remember { mutableStateOf<String?>(null) }

    fun currentDragPosition(): Offset = Offset(dragPosX.floatValue, dragPosY.floatValue)

    fun resolveColumnDrop(
        position: Offset,
        dragged: Task,
        currentPreview: TaskDropPreview?,
        slotHeightPx: Float
    ): Pair<String, Int>? {
        val hitColumns = columnBounds.entries.filter { (colId, rect) ->
            columns.any { it.id == colId } && rect.contains(position)
        }
        val dropTargetColumn = hitColumns.minByOrNull { (_, rect) ->
            kotlin.math.abs(position.x - rect.center.x)
        }?.key ?: return null
        val dropIndex = columns.indexOfFirst { it.id == dropTargetColumn }
        val targetCol = columns.getOrNull(dropIndex) ?: return null
        val tasksInDropColumn = tasksInHub(targetCol, dropIndex)
            .filter { !it.isCompleted && !it.isHidden && it.id != dragged.id }
        val previewInsert = if (currentPreview?.columnId == dropTargetColumn) {
            currentPreview.insertIndex
        } else {
            Int.MAX_VALUE
        }
        var newPosition = tasksInDropColumn.size
        for ((index, t) in tasksInDropColumn.withIndex()) {
            val tBounds = taskBounds[t.id] ?: continue
            val adjustedCenterY = if (index >= previewInsert) tBounds.center.y - slotHeightPx else tBounds.center.y
            if (position.y < adjustedCenterY) {
                newPosition = index
                break
            }
        }
        return dropTargetColumn to newPosition
    }
    
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val density = LocalDensity.current
    val screenWidthPx = with(density) { screenWidth.toPx() }

    val columnWidth = screenWidth - 32.dp
    val dropSlotHeightPx = with(density) {
        (if (isExecutionMode) 56.dp else 84.dp).toPx() + 12.dp.toPx()
    }

    LaunchedEffect(draggedTask != null) {
        if (draggedTask == null) {
            dropPreview = null
            hoveredOtherBoardId = null
            return@LaunchedEffect
        }
        val edgeH = with(density) { 88.dp.toPx() }
        val edgeV = with(density) { 80.dp.toPx() }
        val maxStep = with(density) { 14.dp.toPx() }
        var frame = 0
        var lastPreviewCol: String? = null
        var lastPreviewIndex = Int.MIN_VALUE
        while (isActive && draggedTask != null) {
            val pos = currentDragPosition()
            val originX = contentOriginX.floatValue
            val widthPx = contentWidthPx.floatValue.takeIf { it > 0f } ?: screenWidthPx
            val localX = pos.x - originX
            val scrollingHoriz = when {
                localX > widthPx - edgeH && lazyListState.canScrollForward -> {
                    val ratio = ((localX - (widthPx - edgeH)) / edgeH).coerceIn(0.2f, 1f)
                    lazyListState.scrollBy(maxStep * ratio)
                    true
                }
                localX <= edgeH && lazyListState.canScrollBackward -> {
                    val ratio = ((edgeH - localX) / edgeH).coerceIn(0.2f, 1f)
                    lazyListState.scrollBy(-maxStep * ratio)
                    true
                }
                else -> false
            }

            val colId = columnBounds.entries.firstOrNull { it.value.contains(pos) }?.key
            if (colId != null) {
                val hubState = hubScrollStates[colId]
                val listRect = hubListBounds[colId]
                if (hubState != null && listRect != null) {
                    when {
                        pos.y < listRect.top + edgeV && hubState.canScrollBackward -> {
                            val ratio = ((listRect.top + edgeV - pos.y) / edgeV).coerceIn(0.2f, 1f)
                            hubState.scrollBy(-maxStep * ratio)
                        }
                        pos.y > listRect.bottom - edgeV && hubState.canScrollForward -> {
                            val ratio = ((pos.y - (listRect.bottom - edgeV)) / edgeV).coerceIn(0.2f, 1f)
                            hubState.scrollBy(maxStep * ratio)
                        }
                    }
                }
            }

            frame++
            if (!scrollingHoriz || frame % 3 == 0) {
                val hoveredBoard = otherBoardBounds.entries.firstOrNull { it.value.contains(pos) }?.key
                if (hoveredBoard != hoveredOtherBoardId) {
                    hoveredOtherBoardId = hoveredBoard
                }
                val dragged = draggedTask
                val nextPreview = if (dragged == null || hoveredBoard != null) {
                    null
                } else {
                    resolveColumnDrop(pos, dragged, lastDropPreview, dropSlotHeightPx)?.let { (colIdResolved, index) ->
                        TaskDropPreview(colIdResolved, index)
                    }
                }
                val previewChanged = nextPreview?.columnId != lastPreviewCol ||
                    nextPreview?.insertIndex != lastPreviewIndex
                if (previewChanged) {
                    lastPreviewCol = nextPreview?.columnId
                    lastPreviewIndex = nextPreview?.insertIndex ?: Int.MIN_VALUE
                    dropPreview = nextPreview
                }
                lastDropPreview = nextPreview
            }
            delay(16)
        }
    }

    fun finishTaskDrag() {
        val dragged = draggedTask ?: return
        val dragPos = currentDragPosition()
        val targetOtherBoardId = otherBoardBounds.entries.find { it.value.contains(dragPos) }?.key
        if (targetOtherBoardId != null) {
            taskForMove = dragged
            taskForMoveHubId = columns.find { taskAppearsOnColumn(dragged, it, state.columns, state.boards) }?.id
                ?: dragged.columnId
            pendingExpandBoardId = targetOtherBoardId
        } else {
            val preview = lastDropPreview
            val resolved = if (preview != null && columnBounds[preview.columnId]?.contains(dragPos) == true) {
                preview.columnId to preview.insertIndex
            } else {
                resolveColumnDrop(dragPos, dragged, lastDropPreview, dropSlotHeightPx)
            }
            if (resolved != null) {
                val (dropTargetColumn, visualPosition) = resolved
                val dropIndex = columns.indexOfFirst { it.id == dropTargetColumn }
                val targetCol = columns.getOrNull(dropIndex)
                val targetQuadrant = if (targetCol != null) getQuadrantForCol(targetCol, dropIndex) else null
                // Drop index from on-screen (possibly filtered) list; persist against full hub homes.
                val visualActive = if (targetCol != null) {
                    tasksInHub(targetCol, dropIndex).filter {
                        !it.isCompleted && !it.isHidden && it.id != dragged.id
                    }
                } else {
                    emptyList()
                }
                val clampedVisual = visualPosition.coerceIn(0, visualActive.size)
                val fullHubActive = if (targetCol != null) {
                    sortHubTasks(dropTargetColumn, hubTasksBase(targetCol, dropIndex))
                        .filter { !it.isCompleted && !it.isHidden && it.id != dragged.id }
                } else {
                    emptyList()
                }
                val homeVisualOrder = fullHubActive.filter { it.columnId == dropTargetColumn }
                val homesBeforeDrop = visualActive
                    .take(clampedVisual)
                    .filter { it.columnId == dropTargetColumn }
                val homeInsertIndex = when {
                    homesBeforeDrop.isNotEmpty() -> {
                        val lastId = homesBeforeDrop.last().id
                        val idx = homeVisualOrder.indexOfFirst { it.id == lastId }
                        if (idx >= 0) idx + 1
                        else homesBeforeDrop.size.coerceIn(0, homeVisualOrder.size)
                    }
                    else -> {
                        val firstAfterId = visualActive
                            .drop(clampedVisual)
                            .firstOrNull { it.columnId == dropTargetColumn }
                            ?.id
                        when {
                            firstAfterId != null ->
                                homeVisualOrder.indexOfFirst { it.id == firstAfterId }
                                    .coerceAtLeast(0)
                            clampedVisual == 0 -> 0
                            else -> homeVisualOrder.size
                        }
                    }
                }

                var didReorder = false
                if (isEisenhowerBoard && targetQuadrant != null) {
                    val isFromAnotherBoard = state.columns.find { it.id == dragged.columnId }?.boardId != currentBoardId
                    if (isFromAnotherBoard) {
                        if (dragged.eisenhowerQuadrant == targetQuadrant) {
                            Toast.makeText(context, s.mirrorReorderHint, Toast.LENGTH_SHORT).show()
                        } else {
                            // Quadrant-only change: do not mark hub manually ordered.
                            viewModel.setTaskQuadrant(dragged, targetQuadrant)
                        }
                    } else {
                        viewModel.moveTask(
                            dragged.copy(eisenhowerQuadrant = targetQuadrant),
                            dropTargetColumn,
                            homeInsertIndex,
                            tasks,
                            targetVisibleOrder = homeVisualOrder
                        )
                        didReorder = true
                    }
                } else {
                    val homeBoardId = state.columns.find { it.id == dragged.columnId }?.boardId
                    val reorderingMirror = homeBoardId != null &&
                        homeBoardId != currentBoardId &&
                        dragged.linkedColumnIds.contains(dropTargetColumn)
                    if (reorderingMirror) {
                        Toast.makeText(context, s.mirrorReorderHint, Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.moveTask(
                            dragged,
                            dropTargetColumn,
                            homeInsertIndex,
                            tasks,
                            targetVisibleOrder = homeVisualOrder
                        )
                        didReorder = true
                    }
                }
                if (didReorder) {
                    kairosPrefs.markColumnManuallyOrdered(dropTargetColumn)
                }

                if (dropIndex >= 0) {
                    coroutineScope.launch {
                        lazyListState.scrollToItem(dropIndex)
                    }
                }
            }
        }
        draggedTask = null
        dropPreview = null
        hoveredOtherBoardId = null
        lastDropPreview = null
    }

    fun focusedHubIndex(): Int {
        val visible = lazyListState.layoutInfo.visibleItemsInfo
        if (visible.isEmpty()) {
            return currentColumnIndex.coerceIn(0, (columns.size - 1).coerceAtLeast(0))
        }
        val viewportCenter =
            (lazyListState.layoutInfo.viewportStartOffset + lazyListState.layoutInfo.viewportEndOffset) / 2
        return visible.minByOrNull { info ->
            kotlin.math.abs((info.offset + info.size / 2) - viewportCenter)
        }?.index?.coerceIn(0, (columns.size - 1).coerceAtLeast(0))
            ?: currentColumnIndex.coerceIn(0, (columns.size - 1).coerceAtLeast(0))
    }

    fun sortCurrentHubByImportance() {
        val idx = focusedHubIndex()
        val col = columns.getOrNull(idx) ?: return
        val homeSorted = hubTasksBase(col, idx)
            .filter { !it.isCompleted && !it.isHidden && it.columnId == col.id }
            .sortedWith(
                compareBy<Task> { Eisenhower.getRank(it.eisenhowerQuadrant) }
                    .thenBy { it.dueDate ?: Long.MAX_VALUE }
                    .thenBy { it.position }
            )
        if (homeSorted.isEmpty()) {
            Toast.makeText(context, s.hubSortNothingToSort, Toast.LENGTH_SHORT).show()
            return
        }
        kairosPrefs.markColumnManuallyOrdered(col.id)
        viewModel.reorderHubHomeTasks(col.id, homeSorted.map { it.id })
        Toast.makeText(context, s.hubSortedByImportance, Toast.LENGTH_SHORT).show()
    }

    fun sortCurrentHubByDue() {
        val idx = focusedHubIndex()
        val col = columns.getOrNull(idx) ?: return
        val homeSorted = hubTasksBase(col, idx)
            .filter { !it.isCompleted && !it.isHidden && it.columnId == col.id }
            .sortedWith(
                compareBy<Task> { task ->
                    val due = task.dueDate
                    when {
                        due == null -> 3
                        due.isOverdueDate() -> 0
                        due.isDueToday() -> 1
                        else -> 2
                    }
                }
                    .thenBy { it.dueDate ?: Long.MAX_VALUE }
                    .thenBy { Eisenhower.getRank(it.eisenhowerQuadrant) }
                    .thenBy { it.position }
            )
        if (homeSorted.isEmpty()) {
            Toast.makeText(context, s.hubSortNothingToSort, Toast.LENGTH_SHORT).show()
            return
        }
        kairosPrefs.markColumnManuallyOrdered(col.id)
        viewModel.reorderHubHomeTasks(col.id, homeSorted.map { it.id })
        Toast.makeText(context, s.hubSortedByDue, Toast.LENGTH_SHORT).show()
    }

    fun resetNewTaskForm() {
        isAddingTaskToColumn = null
        newTaskTitle = ""
        newTaskCategory = null
        newTaskDueDate = null
        newTaskShowEisenhower = true
        newTaskQuadrant = null
        newTaskRepeatRule = null
        newTaskReminderMinutes = null
    }

    fun submitNewTask() {
        if (newTaskTitle.isBlank() || isAddingTaskToColumn == null) return
        val currentTasks = tasks.filter { it.columnId == isAddingTaskToColumn }
        val targetIndex = columns.indexOfFirst { it.id == isAddingTaskToColumn }
        val catId = if (newTaskCategory != null && !KanbanNames.isUncategorized(newTaskCategory!!.name)) newTaskCategory!!.id else null
        val targetCol = columns.find { it.id == isAddingTaskToColumn }
        val targetQuadrant = if (newTaskShowEisenhower) {
            newTaskQuadrant ?: (if (targetCol != null) getQuadrantForCol(targetCol, targetIndex) else null)
        } else null
        viewModel.addTask(
            title = newTaskTitle,
            columnId = isAddingTaskToColumn!!,
            categoryId = catId,
            dueDate = newTaskDueDate,
            currentTasks = currentTasks,
            eisenhowerQuadrant = targetQuadrant,
            showEisenhowerButtons = newTaskShowEisenhower,
            repeatRule = newTaskRepeatRule,
            reminderMinutesOfDay = if (newTaskDueDate != null) newTaskReminderMinutes else null
        )
        if (targetIndex >= 0) {
            coroutineScope.launch {
                lazyListState.scrollToItem(targetIndex)
            }
        }
        keyboardController?.hide()
        resetNewTaskForm()
    }

    LaunchedEffect(boardSearchQuery) {
        val query = boardSearchQuery.trim()
        if (query.isEmpty() || columns.isEmpty()) return@LaunchedEffect
        val matchIndex = columns.indexOfFirst { col ->
            val index = columns.indexOf(col)
            tasksInHub(col, index).any { !it.isCompleted }
        }
        if (matchIndex >= 0) {
            lazyListState.scrollToItem(matchIndex)
        }
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

    fun deleteTaskWithUndo(taskId: String) {
        viewModel.deleteTask(taskId)
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = s.taskDeletedEverywhere,
                actionLabel = s.undo,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete(taskId)
            } else {
                viewModel.discardDeletedSnapshot(taskId)
            }
        }
    }

    Scaffold(
        topBar = {
                AnimatedVisibility(
                    visible = showTopBar,
                    enter = expandVertically(animationSpec = tween(120)) + fadeIn(animationSpec = tween(100)),
                    exit = shrinkVertically(animationSpec = tween(90)) + fadeOut(animationSpec = tween(70))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                    TopAppBar(
                title = {
                    Text(
                        text = board?.name?.let { s.localized(it) } ?: s.board,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            initialDelayMillis = 1200,
                            delayMillis = 1500
                        )
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            boardSearchOpen = !boardSearchOpen
                            if (!boardSearchOpen) {
                                boardSearchQuery = ""
                                keyboardController?.hide()
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (boardSearchOpen) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = s.searchTasks,
                            tint = if (boardSearchOpen || boardSearchQuery.isNotBlank()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    if (!compactWidth) {
                        IconButton(
                            onClick = { showReorderColumnsDialog = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = s.hubOrder,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(
                            onClick = onNavigateToCalendar,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = s.calendar,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        IconButton(
                            onClick = { showManageCategories = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Label,
                                contentDescription = s.manageStatuses,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                showTopBar = false
                                showBoardsList = false
                                showColumnHeaders = false
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = s.hideMenu,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Box {
                        IconButton(
                            onClick = { topBarMenuExpanded = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = s.moreActions,
                                tint = if (boardOverdueOnly) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = topBarMenuExpanded,
                            onDismissRequest = { topBarMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(s.overdueOnly) },
                                onClick = {
                                    topBarMenuExpanded = false
                                    boardOverdueOnly = !boardOverdueOnly
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (boardOverdueOnly) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            LocalContentColor.current
                                        }
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(s.sortHubByImportance) },
                                onClick = {
                                    topBarMenuExpanded = false
                                    sortCurrentHubByImportance()
                                },
                                leadingIcon = { Icon(Icons.Default.PriorityHigh, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(s.sortHubByDue) },
                                onClick = {
                                    topBarMenuExpanded = false
                                    sortCurrentHubByDue()
                                },
                                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(s.addHub) },
                                onClick = {
                                    topBarMenuExpanded = false
                                    isAddingColumn = true
                                },
                                leadingIcon = { Icon(Icons.Default.PostAdd, contentDescription = null) }
                            )
                            if (compactWidth) {
                                DropdownMenuItem(
                                    text = { Text(s.hubOrder) },
                                    onClick = {
                                        topBarMenuExpanded = false
                                        showReorderColumnsDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(s.calendar) },
                                    onClick = {
                                        topBarMenuExpanded = false
                                        onNavigateToCalendar()
                                    },
                                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(s.manageStatuses) },
                                    onClick = {
                                        topBarMenuExpanded = false
                                        showManageCategories = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(s.hideMenu) },
                                    onClick = {
                                        topBarMenuExpanded = false
                                        showTopBar = false
                                        showBoardsList = false
                                        showColumnHeaders = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, contentDescription = null) }
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = s.projects,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
                    AnimatedVisibility(
                        visible = boardSearchOpen,
                        enter = expandVertically(animationSpec = tween(120)) + fadeIn(animationSpec = tween(100)),
                        exit = shrinkVertically(animationSpec = tween(90)) + fadeOut(animationSpec = tween(70))
                    ) {
                        Column {
                            OutlinedTextField(
                                value = boardSearchQuery,
                                onValueChange = { boardSearchQuery = it },
                                singleLine = true,
                                placeholder = {
                                    Text(
                                        s.searchTasks,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                trailingIcon = {
                                    if (boardSearchQuery.isNotBlank()) {
                                        IconButton(onClick = { boardSearchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = s.clearFocus)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .focusRequester(searchFocusRequester),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                shape = RoundedCornerShape(12.dp)
                            )
                            LaunchedEffect(boardSearchOpen) {
                                if (boardSearchOpen) {
                                    searchFocusRequester.requestFocus()
                                }
                            }
                        }
                    }
                    }
                }
        },
        floatingActionButton = {
            if (draggedTask == null && columns.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(modeSwitchGradient)
                            .clickable {
                                isExecutionMode = !isExecutionMode
                                kairosPrefs.isExecutionMode = isExecutionMode
                                val modeLabel = if (isExecutionMode) s.executionMode else s.planningMode
                                Toast.makeText(context, modeLabel, Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isExecutionMode) Icons.Default.Bolt else Icons.Default.ViewAgenda,
                            contentDescription = if (isExecutionMode) s.executionMode else s.planningMode,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(10.dp, CircleShape)
                            .clip(CircleShape)
                            .background(newProjectGradient)
                            .clickable {
                                val visible = lazyListState.layoutInfo.visibleItemsInfo
                                val targetIndex = if (visible.isEmpty()) {
                                    currentColumnIndex
                                } else {
                                    val viewportCenter =
                                        (lazyListState.layoutInfo.viewportStartOffset + lazyListState.layoutInfo.viewportEndOffset) / 2
                                    visible.minByOrNull { item ->
                                        kotlin.math.abs((item.offset + item.size / 2) - viewportCenter)
                                    }?.index ?: currentColumnIndex
                                }.coerceIn(0, (columns.size - 1).coerceAtLeast(0))
                                val targetColId = columns.getOrNull(targetIndex)?.id ?: columns.firstOrNull()?.id
                                isAddingTaskToColumn = targetColId
                                newTaskShowEisenhower = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = s.addTask,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = if (showTopBar) WindowInsets.safeDrawing else WindowInsets.statusBars
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .onGloballyPositioned { coordinates ->
                    val origin = coordinates.positionInRoot()
                    contentOriginX.floatValue = origin.x
                    contentOriginY.floatValue = origin.y
                    contentWidthPx.floatValue = coordinates.size.width.toFloat()
                }
                .pointerInput(draggedTask?.id) {
                    if (draggedTask == null) return@pointerInput
                    // Observe the active pointer even after the source card gesture consumes it
                    // (or is cancelled by LazyRow scroll). Absolute root coords → smooth drag.
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            val change = event.changes.firstOrNull() ?: continue
                            dragPosX.floatValue = contentOriginX.floatValue + change.position.x
                            dragPosY.floatValue = contentOriginY.floatValue + change.position.y
                            if (change.changedToUpIgnoreConsumed()) {
                                finishTaskDrag()
                                break
                            }
                        }
                    }
                }
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 1-Tap Quick Board Switcher (OKR ↔ Матрица Эйзенхауэра) - Hidden by default!
                val currentProjectBoards = remember(state.boards, board) {
                    if (board != null) {
                        state.boards.filter { it.projectId == board.projectId && !it.isArchived }
                    } else {
                        state.boards.filter { !it.isArchived }
                    }
                }

                AnimatedVisibility(
                    visible = showBoardsList && currentProjectBoards.isNotEmpty(),
                    enter = expandVertically(animationSpec = tween(120)) + fadeIn(animationSpec = tween(100)),
                    exit = shrinkVertically(animationSpec = tween(90)) + fadeOut(animationSpec = tween(70))
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(currentProjectBoards, key = { it.id }) { b ->
                            val isSelected = b.id == currentBoardId
                            val isBoardEisenhower = KanbanNames.isEisenhowerBoard(b.name)
                            val boardColumnIds = state.columns.filter { it.boardId == b.id }.map { it.id }.toSet()
                            val boardActiveTaskCount = if (isBoardEisenhower) {
                                val projectId = b.projectId
                                tasks.count { task ->
                                    if (task.isCompleted || task.isHidden) return@count false
                                    val homeProjectId = state.columns.find { it.id == task.columnId }
                                        ?.let { col -> state.boards.find { it.id == col.boardId }?.projectId }
                                    homeProjectId == projectId && (
                                        task.eisenhowerQuadrant != null ||
                                            task.columnId in boardColumnIds ||
                                            task.linkedColumnIds.any { id -> id in boardColumnIds }
                                        )
                                }
                            } else {
                                tasks.count {
                                    !it.isCompleted && !it.isHidden && (
                                        it.columnId in boardColumnIds ||
                                            it.linkedColumnIds.any { id -> id in boardColumnIds }
                                        )
                                }
                            }

                            Surface(
                                onClick = {
                                    if (activeBoardId != b.id) {
                                        openBoardAtHub(b.id)
                                        coroutineScope.launch {
                                            lazyListState.scrollToItem(0)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .widthIn(min = 112.dp, max = 180.dp)
                                    .heightIn(min = 48.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 2.dp else 1.2.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                ),
                                shadowElevation = if (isSelected) 2.dp else 0.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = s.localized(b.name),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (boardActiveTaskCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.outlineVariant
                                                )
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = boardActiveTaskCount.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val focusTask = remember(tasks, inProgressTaskId) {
                    inProgressTaskId?.let { id -> tasks.find { it.id == id && !it.isCompleted } }
                }
                val focusJump = remember(
                    focusTask,
                    columns,
                    currentBoardId,
                    currentColumnIndex,
                    state.columns,
                    state.boards
                ) {
                    val task = focusTask ?: return@remember null
                    columns.forEachIndexed { index, col ->
                        if (taskAppearsOnColumn(task, col, state.columns, state.boards)) {
                            return@remember if (index == currentColumnIndex) {
                                null
                            } else {
                                Triple(currentBoardId, col.id, index to task.title)
                            }
                        }
                    }
                    val related = relatedBoardsForTask(task, currentBoardId, state.columns, state.boards)
                    if (related.isNotEmpty()) {
                        val link = related.first()
                        return@remember Triple(link.board.id, link.columnId, null to task.title)
                    }
                    val home = state.columns.find { it.id == task.columnId } ?: return@remember null
                    if (home.boardId == currentBoardId) null
                    else Triple(home.boardId, home.id, null to task.title)
                }
                if (focusJump != null) {
                    val (targetBoardId, targetColumnId, scrollAndTitle) = focusJump
                    val (scrollIndex, focusTitle) = scrollAndTitle
                    Surface(
                        onClick = {
                            if (targetBoardId == currentBoardId && scrollIndex != null) {
                                coroutineScope.launch {
                                    lazyListState.scrollToItem(scrollIndex)
                                }
                            } else {
                                openBoardAtHub(targetBoardId, targetColumnId)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = s.goToFocus,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = focusTitle,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = s.goToFocus,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Collapsible Column Switcher bar (chips toggled by "Хабы" in TopAppBar)
                AnimatedVisibility(
                    visible = showColumnHeaders && columns.isNotEmpty(),
                    enter = expandVertically(animationSpec = tween(120)) + fadeIn(animationSpec = tween(100)),
                    exit = shrinkVertically(animationSpec = tween(90)) + fadeOut(animationSpec = tween(70))
                ) {
                    LazyRow(
                        state = tabRowState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                            itemsIndexed(columns, key = { _, col -> col.id }) { index, col ->
                                val isSelected = index == currentColumnIndex
                                val colTaskCount = tasksInHub(col, index).count { !it.isCompleted && !it.isHidden }

                                Surface(
                                    onClick = {
                                        coroutineScope.launch {
                                            lazyListState.scrollToItem(index)
                                        }
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                    shadowElevation = if (isSelected) 3.dp else 0.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .widthIn(max = 220.dp)
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = s.localized(col.title),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (colTaskCount > 0) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) Color.White.copy(alpha = 0.25f)
                                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = colTaskCount.toString(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                            }
                        }
                    }
                }

                LazyRow(
                    state = lazyListState,
                    flingBehavior = if (draggedTask != null) ScrollableDefaults.flingBehavior() else flingBehavior,
                    userScrollEnabled = draggedTask == null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = if (showTopBar) 16.dp else 4.dp,
                        bottom = 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(columns, key = { _, col -> col.id }) { index, column ->
                        val columnTasks = tasksInHub(column, index)
                        val prevColumn = if (index > 0) columns[index - 1] else null
                        val nextColumn = if (index < columns.size - 1) columns[index + 1] else null
                        
                        ColumnItem(
                            column = column,
                            columnIndex = index,
                            tasks = columnTasks,
                            categories = categories,
                            comments = comments,
                            allColumns = state.columns,
                            boards = state.boards,
                            currentBoardId = currentBoardId,
                            onOpenBoard = { targetBoardId, targetColumnId ->
                                openBoardAtHub(targetBoardId, targetColumnId)
                            },
                            isEisenhowerBoard = isEisenhowerBoard,
                            prevColumn = prevColumn,
                            nextColumn = nextColumn,
                            activeInProgressTaskId = inProgressTaskId,
                            highlightedTaskId = highlightedTaskId,
                            onToggleInProgress = { task ->
                                val next = if (inProgressTaskId == task.id) null else task.id
                                inProgressTaskId = next
                                kairosPrefs.inProgressTaskId = next
                                if (next != null) {
                                    Toast.makeText(context, s.inProgressToast(task.title), Toast.LENGTH_SHORT).show()
                                }
                            },
                            onQuickMoveTask = { task, targetColId ->
                                val targetCol = columns.find { it.id == targetColId }
                                val targetIndex = columns.indexOfFirst { it.id == targetColId }
                                val targetQuadrant = if (targetCol != null) getQuadrantForCol(targetCol, targetIndex) else null

                                if (isEisenhowerBoard && targetQuadrant != null) {
                                    val isFromAnotherBoard = state.columns.find { it.id == task.columnId }?.boardId != currentBoardId
                                    if (isFromAnotherBoard) {
                                        viewModel.setTaskQuadrant(task, targetQuadrant)
                                    } else {
                                        val tasksInTargetCol = tasks.filter { it.columnId == targetColId }
                                        viewModel.moveTask(task.copy(eisenhowerQuadrant = targetQuadrant), targetColId, tasksInTargetCol.size, tasks)
                                    }
                                } else {
                                    val tasksInTargetCol = tasks.filter { it.columnId == targetColId }
                                    viewModel.moveTask(task, targetColId, tasksInTargetCol.size, tasks)
                                }

                                if (targetIndex >= 0) {
                                    coroutineScope.launch {
                                        lazyListState.scrollToItem(targetIndex)
                                    }
                                }
                            },
                            onColumnPositioned = { rect ->
                                columnBounds[column.id] = rect
                            },
                            onTaskPositioned = { task, rect ->
                                taskBounds[task.id] = rect
                            },
                            onDragStart = { task, offset ->
                                draggedTask = task
                                dragPosX.floatValue = offset.x
                                dragPosY.floatValue = offset.y
                            },
                            onDrag = { offset ->
                                // Fallback while root Final-pass tracker attaches.
                                dragPosX.floatValue += offset.x
                                dragPosY.floatValue += offset.y
                            },
                            onDragEnd = {
                                finishTaskDrag()
                            },
                            columnWidth = columnWidth,
                            onDeleteColumnClick = {
                                if (columns.size <= 1) {
                                    Toast.makeText(context, s.cannotDeleteLastHub, Toast.LENGTH_SHORT).show()
                                } else {
                                    columnToDelete = column
                                }
                            },
                            onRenameColumnClick = {
                                columnToRename = column
                                renameColumnTitle = column.title
                            },
                            onRevealHubsList = {
                                showTopBar = true
                                showColumnHeaders = true
                            },
                            onRevealBoardsList = {
                                showTopBar = true
                                showColumnHeaders = true
                                showBoardsList = true
                            },
                            onCollapseOneLevel = {
                                when {
                                    showBoardsList -> showBoardsList = false
                                    showColumnHeaders -> showColumnHeaders = false
                                }
                            },
                            onCollapseAllLists = {
                                showBoardsList = false
                                showColumnHeaders = false
                            },
                            onCommentColumnClick = { columnForComments = column },
                            columnCommentsCount = columnCommentCountById[column.id] ?: 0,
                            commentCountByTaskId = commentCountByTaskId,
                            onEditTaskClick = { task ->
                                taskToEdit = task
                                editTaskTitle = task.title
                                editTaskCategory = categories.find { it.id == task.categoryId }
                                editTaskDueDate = task.dueDate
                                editDatePickerState.selectedDateMillis = task.dueDate?.let { localMillisToUtcPicker(it) }
                                editTaskShowEisenhower = task.showEisenhowerButtons || task.eisenhowerQuadrant != null
                                editTaskQuadrant = task.eisenhowerQuadrant
                                editTaskRepeatRule = task.repeatRule
                                editTaskReminderMinutes = task.reminderMinutesOfDay
                            },
                            onDeleteTaskClick = { taskId ->
                                deleteTaskWithUndo(taskId)
                            },
                            onCommentClick = { task -> taskForComments = task },
                            onMoveTaskClick = { task ->
                                taskForMove = task
                                taskForMoveHubId = column.id
                            },
                            draggedTaskId = draggedTask?.id,
                            isCompactMode = isExecutionMode,
                            isDropTarget = dropPreview?.columnId == column.id,
                            dropInsertIndex = dropPreview?.takeIf { it.columnId == column.id }?.insertIndex,
                            onHubScrollState = { hubScrollStates[column.id] = it },
                            onHubListPositioned = { rect ->
                                hubListBounds[column.id] = rect
                            },
                            onToggleCompleted = { completeTaskWithUndo(it) },
                            viewModel = viewModel
                        )
                    }
                    
                    item {
                        OutlinedButton(
                            onClick = { isAddingColumn = true },
                            modifier = Modifier
                                .width(columnWidth)
                                .padding(top = 12.dp)
                                .height(64.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                s.newHub,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Dialogs — only one exclusive modal at a time (nested date pickers stay with parent).
            val exclusiveDialog = when {
                showOkrPlanningGuide -> "planning"
                columnToDelete != null -> "deleteColumn"
                taskForMove != null -> "move"
                taskForComments != null -> "taskComments"
                columnForComments != null -> "columnComments"
                isAddingTaskToColumn != null -> "addTask"
                taskToEdit != null -> "editTask"
                isAddingColumn -> "addColumn"
                columnToRename != null -> "renameColumn"
                showManageCategories -> "categories"
                showReorderColumnsDialog -> "reorder"
                else -> null
            }

            if (exclusiveDialog == "planning") {
                PlanningGuideDialog(
                    onDismiss = {
                        showOkrPlanningGuide = false
                        kairosPrefs.hasSeenOkrPlanningGuide = true
                    }
                )
            }

            if (exclusiveDialog == "addTask") {
                LaunchedEffect(isAddingTaskToColumn) {
                    taskFocusRequester.requestFocus()
                }

                AlertDialog(
                    onDismissRequest = { resetNewTaskForm() },
                    title = { Text(s.newTask, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = newTaskTitle,
                                onValueChange = { newTaskTitle = it },
                                label = { Text(s.task) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(taskFocusRequester),
                                singleLine = enterAddsTask,
                                maxLines = if (enterAddsTask) 1 else 4,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = if (enterAddsTask) ImeAction.Done else ImeAction.Default
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (enterAddsTask) submitNewTask()
                                    }
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (enterAddsTask) s.enterSavesTask else s.enterAddsNewline,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            if (columns.size > 1) {
                                Text(s.hubLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(columns) { col ->
                                        val isSelected = isAddingTaskToColumn == col.id
                                        Box(
                                            modifier = Modifier
                                                .widthIn(max = 200.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary 
                                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                                                )
                                                .border(
                                                    width = 1.2.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    isAddingTaskToColumn = col.id
                                                }
                                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = s.localized(col.title),
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        newTaskShowEisenhower = !newTaskShowEisenhower
                                        if (!newTaskShowEisenhower) newTaskQuadrant = null
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = s.eisenhowerButtons,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )
                                Switch(
                                    checked = newTaskShowEisenhower,
                                    onCheckedChange = {
                                        newTaskShowEisenhower = it
                                        if (!it) newTaskQuadrant = null
                                    }
                                )
                            }
                            if (newTaskShowEisenhower) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val quadrants = listOf(
                                        Eisenhower.Q1 to "Q1",
                                        Eisenhower.Q2 to "Q2",
                                        Eisenhower.Q3 to "Q3",
                                        Eisenhower.Q4 to "Q4"
                                    )
                                    quadrants.forEach { (qKey, qLabel) ->
                                        EisenhowerQuadrantChip(
                                            qKey = qKey,
                                            qLabel = qLabel,
                                            isSelected = newTaskQuadrant == qKey,
                                            dimUnselected = newTaskQuadrant != null && newTaskQuadrant != qKey,
                                            onClick = { newTaskQuadrant = if (newTaskQuadrant == qKey) null else qKey }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(s.setDueDate, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            DueDateQuickPick(
                                selectedDueDate = newTaskDueDate,
                                onSelect = {
                                    newTaskDueDate = it
                                    if (it == null) newTaskReminderMinutes = null
                                },
                                onPickCustom = { showNewTaskDatePicker = true }
                            )
                            if (newTaskDueDate != null) {
                                Text(
                                    text = newTaskDueDate!!.formatDate(dateLocale),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                val defaultClock = DueReminderScheduler.formatClock(
                                    kairosPrefs.reminderHour,
                                    kairosPrefs.reminderMinute
                                )
                                val reminderLabel = newTaskReminderMinutes?.let {
                                    s.reminderCustom(DueReminderScheduler.formatMinutesOfDay(it))
                                } ?: s.reminderAt(defaultClock)
                                TextButton(
                                    onClick = {
                                        android.app.AlertDialog.Builder(context)
                                            .setTitle(s.reminderTaskTime)
                                            .setItems(
                                                arrayOf(
                                                    "${s.reminderUseDefault} ($defaultClock)",
                                                    s.reminderPickTime
                                                )
                                            ) { _, which ->
                                                when (which) {
                                                    0 -> newTaskReminderMinutes = null
                                                    1 -> {
                                                        val initial = newTaskReminderMinutes
                                                            ?: (kairosPrefs.reminderHour * 60 + kairosPrefs.reminderMinute)
                                                        android.app.TimePickerDialog(
                                                            context,
                                                            { _, hour, minute ->
                                                                newTaskReminderMinutes = hour * 60 + minute
                                                            },
                                                            initial / 60,
                                                            initial % 60,
                                                            true
                                                        ).show()
                                                    }
                                                }
                                            }
                                            .show()
                                    }
                                ) {
                                    Text(reminderLabel)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(s.repeatLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = newTaskRepeatRule == null,
                                    onClick = { newTaskRepeatRule = null },
                                    label = { Text(s.repeatNone) }
                                )
                                FilterChip(
                                    selected = newTaskRepeatRule == TaskRepeat.DAILY,
                                    onClick = { newTaskRepeatRule = TaskRepeat.DAILY },
                                    label = { Text(s.repeatDaily) }
                                )
                                FilterChip(
                                    selected = newTaskRepeatRule == TaskRepeat.WEEKLY,
                                    onClick = { newTaskRepeatRule = TaskRepeat.WEEKLY },
                                    label = { Text(s.repeatWeekly) }
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(s.taskCategory, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(categories) { category ->
                                    val isSelected = newTaskCategory?.id == category.id
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(getStatusBrush(category.color))
                                            .border(
                                                width = if (isSelected) 3.5.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.4f),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                newTaskCategory = if (newTaskCategory?.id == category.id) null else category
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .background(Color.White, CircleShape)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = newTaskCategory?.name?.let { s.localized(it) } ?: s.noStatusSelected,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (newTaskCategory != null) Color(newTaskCategory!!.color) else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { submitNewTask() }) {
                            Text(s.add, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { resetNewTaskForm() }) {
                            Text(s.cancel, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                )
            }
            
            if (exclusiveDialog == "addTask" && showNewTaskDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showNewTaskDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = { 
                            newTaskDueDate = newDatePickerState.selectedDateMillis?.let { utcPickerMillisToLocalNoon(it) }
                            showNewTaskDatePicker = false 
                        }) { Text(s.ok) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNewTaskDatePicker = false }) { Text(s.cancel) }
                    }
                ) {
                    DatePicker(state = newDatePickerState)
                }
            }
            
            if (exclusiveDialog == "addColumn") {
                AlertDialog(
                    onDismissRequest = { isAddingColumn = false },
                    title = { Text(s.newHub) },
                    text = {
                        OutlinedTextField(
                            value = newColumnTitle,
                            onValueChange = { newColumnTitle = it },
                            label = { Text(s.hubName) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.addColumn(newColumnTitle, boardId, columns)
                            newColumnTitle = ""
                            isAddingColumn = false
                        }) {
                            Text(s.add, style = MaterialTheme.typography.titleMedium)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { isAddingColumn = false }) {
                            Text(s.cancel, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                )
            }
            
            if (exclusiveDialog == "renameColumn") {
                AlertDialog(
                    onDismissRequest = { columnToRename = null },
                    title = { Text(s.renameHub) },
                    text = {
                        OutlinedTextField(
                            value = renameColumnTitle,
                            onValueChange = { renameColumnTitle = it },
                            label = { Text(s.newName) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.renameColumn(columnToRename!!.id, renameColumnTitle)
                            columnToRename = null
                        }) {
                            Text(s.save, style = MaterialTheme.typography.titleMedium)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { columnToRename = null }) {
                            Text(s.cancel, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                )
            }

            if (exclusiveDialog == "deleteColumn") {
                val hub = columnToDelete!!
                AlertDialog(
                    onDismissRequest = { columnToDelete = null },
                    title = { Text(s.deleteHub) },
                    text = {
                        androidx.compose.foundation.layout.Column {
                            Text(s.quoted(s.localized(hub.title)))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(s.deleteWithContents)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.deleteColumn(hub.id)
                            columnToDelete = null
                        }) {
                            Text(s.delete, color = deleteButtonColor, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { columnToDelete = null }) { Text(s.cancel) }
                    }
                )
            }
            
            if (exclusiveDialog == "editTask") {
                fun saveEditedTask() {
                    if (editTaskTitle.isNotBlank()) {
                        val catId = if (editTaskCategory != null && !KanbanNames.isUncategorized(editTaskCategory!!.name)) editTaskCategory!!.id else null
                        val updatedTask = taskToEdit!!.copy(
                            title = editTaskTitle,
                            categoryId = catId,
                            dueDate = editTaskDueDate,
                            showEisenhowerButtons = editTaskShowEisenhower,
                            eisenhowerQuadrant = if (editTaskShowEisenhower) editTaskQuadrant else null,
                            repeatRule = editTaskRepeatRule,
                            reminderMinutesOfDay = if (editTaskDueDate != null) editTaskReminderMinutes else null
                        )
                        viewModel.updateTask(updatedTask)
                    }
                    keyboardController?.hide()
                    taskToEdit = null
                }
                AlertDialog(
                    onDismissRequest = { taskToEdit = null },
                    title = { Text(s.editTask) },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = editTaskTitle,
                                onValueChange = { editTaskTitle = it },
                                label = { Text(s.task) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = enterAddsTask,
                                maxLines = if (enterAddsTask) 1 else 4,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = if (enterAddsTask) ImeAction.Done else ImeAction.Default
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (enterAddsTask) saveEditedTask()
                                    }
                                )
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(s.taskCategory, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(categories) { category ->
                                    val isSelected = editTaskCategory?.id == category.id
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(getStatusBrush(category.color))
                                            .border(
                                                width = if (isSelected) 3.5.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.4f),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                editTaskCategory = if (editTaskCategory?.id == category.id) null else category
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .background(Color.White, CircleShape)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = editTaskCategory?.name?.let { s.localized(it) } ?: s.noStatusSelected,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (editTaskCategory != null) Color(editTaskCategory!!.color) else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Eisenhower Matrix buttons setting
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        editTaskShowEisenhower = !editTaskShowEisenhower
                                        if (!editTaskShowEisenhower) editTaskQuadrant = null
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = s.eisenhowerButtons,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )
                                Switch(
                                    checked = editTaskShowEisenhower,
                                    onCheckedChange = {
                                        editTaskShowEisenhower = it
                                        if (!it) editTaskQuadrant = null
                                    }
                                )
                            }
                            if (editTaskShowEisenhower) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val quadrants = listOf(
                                        Eisenhower.Q1 to "Q1",
                                        Eisenhower.Q2 to "Q2",
                                        Eisenhower.Q3 to "Q3",
                                        Eisenhower.Q4 to "Q4"
                                    )
                                    quadrants.forEach { (qKey, qLabel) ->
                                        EisenhowerQuadrantChip(
                                            qKey = qKey,
                                            qLabel = qLabel,
                                            isSelected = editTaskQuadrant == qKey,
                                            dimUnselected = editTaskQuadrant != null && editTaskQuadrant != qKey,
                                            onClick = { editTaskQuadrant = if (editTaskQuadrant == qKey) null else qKey }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(s.setDueDate, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            DueDateQuickPick(
                                selectedDueDate = editTaskDueDate,
                                onSelect = {
                                    editTaskDueDate = it
                                    if (it == null) editTaskReminderMinutes = null
                                },
                                onPickCustom = { showEditTaskDatePicker = true }
                            )
                            if (editTaskDueDate != null) {
                                Text(
                                    text = editTaskDueDate!!.formatDate(dateLocale),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                val defaultClock = DueReminderScheduler.formatClock(
                                    kairosPrefs.reminderHour,
                                    kairosPrefs.reminderMinute
                                )
                                val reminderLabel = editTaskReminderMinutes?.let {
                                    s.reminderCustom(DueReminderScheduler.formatMinutesOfDay(it))
                                } ?: s.reminderAt(defaultClock)
                                TextButton(
                                    onClick = {
                                        android.app.AlertDialog.Builder(context)
                                            .setTitle(s.reminderTaskTime)
                                            .setItems(
                                                arrayOf(
                                                    "${s.reminderUseDefault} ($defaultClock)",
                                                    s.reminderPickTime
                                                )
                                            ) { _, which ->
                                                when (which) {
                                                    0 -> editTaskReminderMinutes = null
                                                    1 -> {
                                                        val initial = editTaskReminderMinutes
                                                            ?: (kairosPrefs.reminderHour * 60 + kairosPrefs.reminderMinute)
                                                        android.app.TimePickerDialog(
                                                            context,
                                                            { _, hour, minute ->
                                                                editTaskReminderMinutes = hour * 60 + minute
                                                            },
                                                            initial / 60,
                                                            initial % 60,
                                                            true
                                                        ).show()
                                                    }
                                                }
                                            }
                                            .show()
                                    }
                                ) {
                                    Text(reminderLabel)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(s.repeatLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = editTaskRepeatRule == null,
                                    onClick = { editTaskRepeatRule = null },
                                    label = { Text(s.repeatNone) }
                                )
                                FilterChip(
                                    selected = editTaskRepeatRule == TaskRepeat.DAILY,
                                    onClick = { editTaskRepeatRule = TaskRepeat.DAILY },
                                    label = { Text(s.repeatDaily) }
                                )
                                FilterChip(
                                    selected = editTaskRepeatRule == TaskRepeat.WEEKLY,
                                    onClick = { editTaskRepeatRule = TaskRepeat.WEEKLY },
                                    label = { Text(s.repeatWeekly) }
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            val editTaskCommentsCount = comments.count { it.taskId == taskToEdit?.id }
                            OutlinedButton(
                                onClick = { taskForComments = taskToEdit },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Comment,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(s.commentsCount(editTaskCommentsCount), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    val moving = taskToEdit
                                    taskForMove = moving
                                    taskForMoveHubId = moving?.let { edited ->
                                        columns.find { taskAppearsOnColumn(edited, it, state.columns, state.boards) }?.id
                                            ?: edited.columnId
                                    }
                                    taskToEdit = null
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(s.changeHubBoard, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { saveEditedTask() }) {
                            Text(s.save, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { taskToEdit = null }) {
                            Text(s.cancel, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                )
            }
            
            if (exclusiveDialog == "editTask" && showEditTaskDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showEditTaskDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = { 
                            editTaskDueDate = editDatePickerState.selectedDateMillis?.let { utcPickerMillisToLocalNoon(it) }
                            showEditTaskDatePicker = false 
                        }) { Text(s.ok) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditTaskDatePicker = false }) { Text(s.cancel) }
                    }
                ) {
                    DatePicker(state = editDatePickerState)
                }
            }
            
            if (exclusiveDialog == "categories") {
                ManageCategoriesDialog(
                    categories = categories,
                    viewModel = viewModel,
                    onDismiss = { showManageCategories = false }
                )
            }

            if (exclusiveDialog == "taskComments") {
                val taskComments = comments.filter { it.taskId == taskForComments!!.id }
                TaskCommentsDialog(
                    task = taskForComments!!,
                    comments = taskComments,
                    onAddComment = { text -> viewModel.addComment(taskForComments!!.id, text) },
                    onDeleteComment = { commentId -> viewModel.deleteComment(commentId) },
                    onDismiss = { taskForComments = null }
                )
            }

            if (exclusiveDialog == "columnComments") {
                val colComments = state.columnComments.filter { it.columnId == columnForComments!!.id }
                HubCommentsDialog(
                    column = columnForComments!!,
                    comments = colComments,
                    onAddComment = { text -> viewModel.addColumnComment(columnForComments!!.id, text) },
                    onDeleteComment = { commentId -> viewModel.deleteColumnComment(commentId) },
                    onDismiss = { columnForComments = null }
                )
            }

            if (exclusiveDialog == "move") {
                val sourceHubId = taskForMoveHubId
                    ?: columns.find { taskAppearsOnColumn(taskForMove!!, it, state.columns, state.boards) }?.id
                    ?: columns.getOrNull(lazyListState.firstVisibleItemIndex)?.id.orEmpty()
                fun dismissMoveDialog() {
                    taskForMove = null
                    taskForMoveHubId = null
                    pendingExpandBoardId = null
                }
                MoveTaskDialog(
                    task = taskForMove!!,
                    columns = columns,
                    allColumns = state.columns,
                    sourceColumnId = sourceHubId,
                    otherBoards = otherBoards,
                    boards = state.boards,
                    projects = state.projects,
                    initialExpandedBoardId = pendingExpandBoardId,
                    onMoveToColumn = { targetColId ->
                        val targetIndex = columns.indexOfFirst { it.id == targetColId }
                        val targetCol = state.columns.find { it.id == targetColId }
                        val tasksInTargetCol = tasks.filter { it.columnId == targetColId }
                        viewModel.moveTask(
                            taskForMove!!,
                            targetColId,
                            tasksInTargetCol.size,
                            tasks,
                            changeHome = true,
                            fromBoardId = currentBoardId
                        )
                        if (targetIndex >= 0) {
                            coroutineScope.launch {
                                lazyListState.scrollToItem(targetIndex)
                            }
                            Toast.makeText(context, s.taskMovedToHub(s.localized(targetCol?.title.orEmpty())), Toast.LENGTH_SHORT).show()
                        } else {
                            val targetBoard = state.boards.find { it.id == targetCol?.boardId }
                            Toast.makeText(context, s.taskMovedToBoard(s.localized(targetBoard?.name.orEmpty())), Toast.LENGTH_SHORT).show()
                        }
                        dismissMoveDialog()
                    },
                    onMoveToBoard = { targetBoardId ->
                        val targetBoard = otherBoards.find { it.id == targetBoardId }
                        if (state.columns.none { it.boardId == targetBoardId }) {
                            Toast.makeText(context, s.boardHasNoHubs, Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.moveTaskToBoard(taskForMove!!, targetBoardId, fromBoardId = currentBoardId)
                            Toast.makeText(context, s.taskMovedToBoard(s.localized(targetBoard?.name.orEmpty())), Toast.LENGTH_SHORT).show()
                        }
                        dismissMoveDialog()
                    },
                    onCopyToColumn = { targetColId ->
                        val targetCol = state.columns.find { it.id == targetColId }
                        viewModel.copyTaskToColumn(taskForMove!!, targetColId)
                        if (columns.any { it.id == targetColId }) {
                            Toast.makeText(context, s.taskAlsoOnHub(s.localized(targetCol?.title.orEmpty())), Toast.LENGTH_SHORT).show()
                        } else {
                            val targetBoard = state.boards.find { it.id == targetCol?.boardId }
                            Toast.makeText(context, s.taskAlsoOnBoard(s.localized(targetBoard?.name.orEmpty())), Toast.LENGTH_SHORT).show()
                        }
                        dismissMoveDialog()
                    },
                    onCopyToBoard = { targetBoardId ->
                        val targetBoard = otherBoards.find { it.id == targetBoardId }
                        if (state.columns.none { it.boardId == targetBoardId }) {
                            Toast.makeText(context, s.boardHasNoHubs, Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.copyTaskToBoard(taskForMove!!, targetBoardId)
                            Toast.makeText(context, s.taskAlsoOnBoard(s.localized(targetBoard?.name.orEmpty())), Toast.LENGTH_SHORT).show()
                        }
                        dismissMoveDialog()
                    },
                    onDismiss = { dismissMoveDialog() }
                )
            }

            if (exclusiveDialog == "reorder") {
                ReorderColumnsDialog(
                    columns = columns,
                    onMoveColumn = { colId, direction ->
                        val currIdx = columns.indexOfFirst { it.id == colId }
                        val newIdx = currIdx + direction
                        viewModel.moveColumn(colId, direction, currentBoardId)
                        if (newIdx in columns.indices) {
                            coroutineScope.launch {
                                lazyListState.scrollToItem(newIdx)
                            }
                        }
                    },
                    onDismiss = { showReorderColumnsDialog = false }
                )
            }

            // Other boards drop target when dragging
            if (draggedTask != null && otherBoards.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = s.dropToMoveBoard,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(otherBoards) { targetBoard ->
                                val isHovered = hoveredOtherBoardId == targetBoard.id
                                val parentProj = state.projects.find { it.id == targetBoard.projectId }
                                
                                Box(
                                    modifier = Modifier
                                        .onGloballyPositioned { coordinates ->
                                            otherBoardBounds[targetBoard.id] = androidx.compose.ui.geometry.Rect(
                                                offset = coordinates.positionInRoot(),
                                                size = androidx.compose.ui.geometry.Size(
                                                    coordinates.size.width.toFloat(),
                                                    coordinates.size.height.toFloat()
                                                )
                                            )
                                        }
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isHovered) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .border(
                                            width = if (isHovered) 3.dp else 1.dp,
                                            color = if (isHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = s.localized(targetBoard.name),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isHovered) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (parentProj != null) {
                                            Text(
                                                text = parentProj.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                        if (isHovered) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = s.releaseToMove,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating dragged card (visual only — pointer tracked on parent Box Final pass)
            draggedTask?.let { task ->
                val cardWidth = columnWidth - 24.dp
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(100f)
                ) {
                    TaskCard(
                        task = task,
                        categories = categories,
                        viewModel = viewModel,
                        isCompact = isExecutionMode,
                        isDragPreview = true,
                        modifier = Modifier
                            .width(cardWidth)
                            .graphicsLayer {
                                translationX = dragPosX.floatValue - contentOriginX.floatValue -
                                    (cardWidth.toPx() / 2f)
                                translationY = dragPosY.floatValue - contentOriginY.floatValue -
                                    45.dp.toPx()
                                scaleX = 1.03f
                                scaleY = 1.03f
                                alpha = 0.95f
                                shadowElevation = 16.dp.toPx()
                            },
                        onPositioned = {},
                        onDragStart = { _ -> },
                        onDrag = { _ -> },
                        onDragEnd = {},
                        onEditClick = {},
                        onDeleteClick = {}
                    )
                }
            }

            if (!showTopBar) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .clickable { showTopBar = true }
                        .padding(horizontal = 24.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = s.showMenu,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ManageCategoriesDialog(
    categories: List<Category>,
    viewModel: SharedViewModel,
    onDismiss: () -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var editingName by remember { mutableStateOf("") }

    val s = LocalAppStrings.current
    val colors = listOf(0xFFFF2A5F, 0xFF9D2CFF, 0xFF0095F6, 0xFFFFB020, 0xFF00D287, 0xFFAAAAAA)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.manageStatuses, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(categories, key = { it.id }) { category ->
                    if (editingCategory?.id == category.id) {
                        Column {
                            OutlinedTextField(
                                value = editingName,
                                onValueChange = { editingName = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(colors) { color ->
                                    val isSelected = category.color == color
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(getStatusBrush(color))
                                            .border(
                                                width = if (isSelected) 3.5.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.4f),
                                                shape = CircleShape
                                            )
                                            .clickable { viewModel.updateCategory(category.copy(color = color)) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Box(modifier = Modifier.size(10.dp).background(Color.White, CircleShape))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { editingCategory = null }) { Text(s.cancel, style = MaterialTheme.typography.titleMedium) }
                                TextButton(onClick = {
                                    if (editingName.isNotBlank()) {
                                        viewModel.updateCategory(category.copy(name = editingName))
                                        editingCategory = null
                                    }
                                }) { Text(s.save, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(getStatusBrush(category.color)))
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = s.localized(category.name),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = {
                                    editingCategory = category
                                    editingName = category.name
                                }, modifier = Modifier.size(44.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = s.edit, modifier = Modifier.size(24.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(deleteButtonGradient)
                                        .clickable { viewModel.deleteCategory(category.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = s.delete, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
                
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(s.addNewStatus, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            placeholder = { Text(s.statusName) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        IconButton(
                            onClick = {
                                if (newCategoryName.isNotBlank()) {
                                    viewModel.addCategory(newCategoryName, colors.random())
                                    newCategoryName = ""
                                }
                            }, 
                            modifier = Modifier
                                .size(52.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = s.add, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(s.close, style = MaterialTheme.typography.titleMedium) }
        }
    )
}

@Composable
fun ColumnItem(
    column: Column,
    columnIndex: Int = 0,
    tasks: List<Task>,
    categories: List<Category>,
    comments: List<Comment>,
    commentCountByTaskId: Map<String, Int> = emptyMap(),
    allColumns: List<Column> = emptyList(),
    boards: List<Board> = emptyList(),
    currentBoardId: String = "",
    onOpenBoard: (boardId: String, columnId: String) -> Unit = { _, _ -> },
    isEisenhowerBoard: Boolean = false,
    prevColumn: Column? = null,
    nextColumn: Column? = null,
    activeInProgressTaskId: String? = null,
    highlightedTaskId: String? = null,
    onToggleInProgress: (Task) -> Unit = {},
    onQuickMoveTask: ((Task, String) -> Unit)? = null,
    onColumnPositioned: (androidx.compose.ui.geometry.Rect) -> Unit,
    onTaskPositioned: (Task, androidx.compose.ui.geometry.Rect) -> Unit,
    onDragStart: (Task, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    columnWidth: androidx.compose.ui.unit.Dp,
    onDeleteColumnClick: () -> Unit,
    onRenameColumnClick: () -> Unit,
    onRevealHubsList: () -> Unit = {},
    onRevealBoardsList: () -> Unit = {},
    onCollapseOneLevel: () -> Unit = {},
    onCollapseAllLists: () -> Unit = {},
    onEditTaskClick: (Task) -> Unit,
    onDeleteTaskClick: (String) -> Unit,
    onCommentClick: (Task) -> Unit,
    onMoveTaskClick: (Task) -> Unit,
    columnCommentsCount: Int = 0,
    onCommentColumnClick: () -> Unit = {},
    draggedTaskId: String? = null,
    isCompactMode: Boolean = false,
    isDropTarget: Boolean = false,
    dropInsertIndex: Int? = null,
    onHubScrollState: (androidx.compose.foundation.lazy.LazyListState) -> Unit = {},
    onHubListPositioned: (androidx.compose.ui.geometry.Rect) -> Unit = {},
    onToggleCompleted: (Task) -> Unit = {},
    viewModel: SharedViewModel
) {
    val context = LocalContext.current
    val s = LocalAppStrings.current
    var showHiddenTasks by remember { mutableStateOf(false) }
    val hiddenCount = remember(tasks) { tasks.count { it.isHidden } }
    val activeTasks = remember(tasks, showHiddenTasks) {
        tasks.filter { !it.isCompleted && (showHiddenTasks || !it.isHidden) }
    }
    val visibleActiveTasks = activeTasks
    val completedTasks = remember(tasks, showHiddenTasks) {
        tasks.filter { it.isCompleted && (showHiddenTasks || !it.isHidden) }.sortedByDescending { it.completedAt ?: 0L }
    }
    var showCompleted by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val hubListState = rememberLazyListState()
    LaunchedEffect(hubListState) {
        onHubScrollState(hubListState)
    }
    var hubSwipeOffsetX by remember { mutableFloatStateOf(0f) }
    val hubMaxSwipePx = with(density) { 192.dp.toPx() }
    val hubPullHubsPx = with(density) { 48.dp.toPx() }
    val hubPullBoardsPx = with(density) { 96.dp.toPx() }

    val hubColors = listOf(
        Color(0xFF2563EB), // Blue
        Color(0xFF8B5CF6), // Purple
        Color(0xFF0D9488), // Teal
        Color(0xFFEA580C), // Orange
        Color(0xFF10B981)  // Emerald
    )
    val hubColor = hubColors[columnIndex % hubColors.size]

    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .width(columnWidth)
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isDropTarget) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = if (isDropTarget) 2.dp else 0.dp,
                color = if (isDropTarget) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .onGloballyPositioned { coordinates ->
                val bounds = androidx.compose.ui.geometry.Rect(
                    offset = coordinates.positionInRoot(),
                    size = androidx.compose.ui.geometry.Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
                )
                onColumnPositioned(bounds)
            }
            .padding(12.dp)
    ) {
        // Swipeable Hub Header: pulling left reveals Rename and Delete
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
                .padding(bottom = 12.dp)
        ) {
            // Revealed Actions behind header (completely invisible when offset is 0)
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .height(44.dp)
                    .padding(end = 4.dp)
                    .graphicsLayer {
                        alpha = ((-hubSwipeOffsetX) / 30f).coerceIn(0f, 1f)
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Comments button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable {
                            hubSwipeOffsetX = 0f
                            onCommentColumnClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Comment,
                        contentDescription = s.hubRules,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    if (columnCommentsCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = columnCommentsCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = androidx.compose.ui.unit.TextUnit(9f, androidx.compose.ui.unit.TextUnitType.Sp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (showHiddenTasks) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondaryContainer
                        )
                        .clickable {
                            hubSwipeOffsetX = 0f
                            showHiddenTasks = !showHiddenTasks
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (showHiddenTasks) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (showHiddenTasks) s.hideHiddenTasks else s.showHiddenTasks,
                        modifier = Modifier.size(18.dp),
                        tint = if (showHiddenTasks) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    if (hiddenCount > 0 && !showHiddenTasks) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = hiddenCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = androidx.compose.ui.unit.TextUnit(9f, androidx.compose.ui.unit.TextUnitType.Sp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable {
                            hubSwipeOffsetX = 0f
                            onRenameColumnClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = s.renameHub,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(deleteButtonGradient)
                        .clickable {
                            hubSwipeOffsetX = 0f
                            onDeleteColumnClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = s.deleteHub,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                }
            }

            // Foreground header that slides left
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .offset { IntOffset(hubSwipeOffsetX.roundToInt(), 0) }
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(column.id) {
                        var totalX = 0f
                        var totalY = 0f
                        var dragMode: Int? = null // 1 = horizontal, 2 = vertical
                        var revealHubsFired = false
                        var revealBoardsFired = false
                        var collapseStepFired = false
                        var collapseAllFired = false
                        detectDragGestures(
                            onDragStart = {
                                totalX = 0f
                                totalY = 0f
                                dragMode = null
                                revealHubsFired = false
                                revealBoardsFired = false
                                collapseStepFired = false
                                collapseAllFired = false
                            },
                            onDrag = { change, dragAmount ->
                                totalX += dragAmount.x
                                totalY += dragAmount.y
                                if (dragMode == null && (abs(totalX) > 10f || abs(totalY) > 10f)) {
                                    dragMode = if (abs(totalY) > abs(totalX)) 2 else 1
                                }
                                when (dragMode) {
                                    1 -> {
                                        change.consume()
                                        hubSwipeOffsetX =
                                            (hubSwipeOffsetX + dragAmount.x).coerceIn(-hubMaxSwipePx, 0f)
                                    }
                                    2 -> {
                                        change.consume()
                                        when {
                                            totalY >= hubPullBoardsPx && !revealBoardsFired -> {
                                                revealBoardsFired = true
                                                revealHubsFired = true
                                                onRevealBoardsList()
                                            }
                                            totalY >= hubPullHubsPx && !revealHubsFired -> {
                                                revealHubsFired = true
                                                onRevealHubsList()
                                            }
                                            totalY <= -hubPullBoardsPx && !collapseAllFired -> {
                                                collapseAllFired = true
                                                collapseStepFired = true
                                                onCollapseAllLists()
                                            }
                                            totalY <= -hubPullHubsPx && !collapseStepFired -> {
                                                collapseStepFired = true
                                                onCollapseOneLevel()
                                            }
                                        }
                                    }
                                }
                            },
                            onDragEnd = {
                                if (dragMode == 1) {
                                    hubSwipeOffsetX =
                                        if (hubSwipeOffsetX < -hubMaxSwipePx * 0.4f) -hubMaxSwipePx else 0f
                                }
                                dragMode = null
                            },
                            onDragCancel = {
                                if (dragMode == 1) hubSwipeOffsetX = 0f
                                dragMode = null
                            }
                        )
                    }
                    .clickable {
                        if (hubSwipeOffsetX < 0f) hubSwipeOffsetX = 0f
                    }
                    .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 4.dp, height = 20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(hubColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = s.localized(column.title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (hiddenCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (showHiddenTasks) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.secondaryContainer
                            )
                            .clickable { showHiddenTasks = !showHiddenTasks },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (showHiddenTasks) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showHiddenTasks) s.hideHiddenTasks else s.showHiddenTasks,
                            modifier = Modifier.size(18.dp),
                            tint = if (showHiddenTasks) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                        if (!showHiddenTasks) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                                    .padding(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = hiddenCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onError,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = androidx.compose.ui.unit.TextUnit(9f, androidx.compose.ui.unit.TextUnitType.Sp)
                                )
                            }
                        }
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = hubColor.copy(alpha = 0.65f)
                )
            }
        }

        LazyColumn(
            state = hubListState,
            modifier = Modifier
                .fillMaxHeight()
                .onGloballyPositioned { coordinates ->
                    onHubListPositioned(
                        androidx.compose.ui.geometry.Rect(
                            offset = coordinates.positionInRoot(),
                            size = androidx.compose.ui.geometry.Size(
                                coordinates.size.width.toFloat(),
                                coordinates.size.height.toFloat()
                            )
                        )
                    )
                },
            contentPadding = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (visibleActiveTasks.isEmpty() && completedTasks.isEmpty() && dropInsertIndex == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = s.noTasks,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                itemsIndexed(visibleActiveTasks, key = { _, task -> task.id }) { index, task ->
                    val taskCommentsCount = commentCountByTaskId[task.id] ?: 0
                    val relatedBoards = relatedBoardsForTask(task, currentBoardId, allColumns, boards)
                    val filteredIndex = if (draggedTaskId == null) index
                    else visibleActiveTasks.take(index).count { it.id != draggedTaskId }

                    androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth()) {
                        if (dropInsertIndex != null && task.id != draggedTaskId && dropInsertIndex == filteredIndex) {
                            DropInsertSlot(isCompact = isCompactMode)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        TaskCard(
                        task = task,
                        categories = categories,
                        viewModel = viewModel,
                        commentsCount = taskCommentsCount,
                        isInProgress = activeInProgressTaskId == task.id,
                        isHighlighted = highlightedTaskId == task.id,
                        isAnyTaskInProgress = activeInProgressTaskId != null,
                        isDragging = draggedTaskId == task.id,
                        isCompact = isCompactMode,
                        relatedBoards = relatedBoards,
                        onOpenRelatedBoard = onOpenBoard,
                        onRemoveFromBoard = if (relatedBoards.isNotEmpty()) {
                            {
                                viewModel.removeTaskFromBoard(task, currentBoardId)
                                Toast.makeText(context, s.taskRemovedFromBoard, Toast.LENGTH_SHORT).show()
                            }
                        } else null,
                        onSetQuadrant = { q ->
                            viewModel.setTaskQuadrant(task, q)
                            val msg = if (q != null) {
                                s.addedToMatrix(q)
                            } else {
                                s.removedFromMatrix
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        onToggleInProgress = { onToggleInProgress(task) },
                        prevColumnTitle = prevColumn?.title,
                        nextColumnTitle = nextColumn?.title,
                        onQuickMovePrev = if (prevColumn != null && onQuickMoveTask != null) {
                            {
                                onQuickMoveTask(task, prevColumn.id)
                                Toast.makeText(context, s.movedToHub(s.localized(prevColumn.title)), Toast.LENGTH_SHORT).show()
                            }
                        } else null,
                        onQuickMoveNext = if (nextColumn != null && onQuickMoveTask != null) {
                            {
                                onQuickMoveTask(task, nextColumn.id)
                                Toast.makeText(context, s.movedToHub(s.localized(nextColumn.title)), Toast.LENGTH_SHORT).show()
                            }
                        } else null,
                        onCommentClick = { onCommentClick(task) },
                        onMoveClick = { onMoveTaskClick(task) },
                        onPositioned = { rect -> onTaskPositioned(task, rect) },
                        onDragStart = { offset -> onDragStart(task, offset) },
                        onDrag = onDrag,
                        onDragEnd = onDragEnd,
                        onEditClick = { onEditTaskClick(task) },
                        onDeleteClick = { onDeleteTaskClick(task.id) },
                        onToggleCompleted = { onToggleCompleted(task) }
                    )
                    }
                }
                if (dropInsertIndex != null && dropInsertIndex >= visibleActiveTasks.count { it.id != draggedTaskId }) {
                    item(key = "drop-slot-end") {
                        DropInsertSlot(isCompact = isCompactMode)
                    }
                }

                if (completedTasks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showCompleted = !showCompleted }
                                        .padding(vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (showCompleted) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (showCompleted) s.hide else s.show,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = s.completedCount(completedTasks.size),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(deleteButtonGradient)
                                        .clickable { showClearConfirmDialog = true }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(s.clear, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }

                    if (showCompleted) {
                        items(completedTasks, key = { it.id }) { task ->
                            val taskCommentsCount = commentCountByTaskId[task.id] ?: 0
                            val relatedBoards = relatedBoardsForTask(task, currentBoardId, allColumns, boards)

                            TaskCard(
                                task = task,
                                categories = categories,
                                viewModel = viewModel,
                                commentsCount = taskCommentsCount,
                                isInProgress = false,
                                isAnyTaskInProgress = activeInProgressTaskId != null,
                                isCompact = isCompactMode,
                                relatedBoards = relatedBoards,
                                onOpenRelatedBoard = onOpenBoard,
                                onSetQuadrant = { q ->
                                    viewModel.setTaskQuadrant(task, q)
                                    val msg = if (q != null) {
                                        s.addedToMatrix(q)
                                    } else {
                                        s.removedFromMatrix
                                    }
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                onToggleInProgress = {},
                                prevColumnTitle = prevColumn?.title,
                                nextColumnTitle = nextColumn?.title,
                                onQuickMovePrev = if (prevColumn != null && onQuickMoveTask != null) {
                                    {
                                        onQuickMoveTask(task, prevColumn.id)
                                        Toast.makeText(context, s.movedToHub(s.localized(prevColumn.title)), Toast.LENGTH_SHORT).show()
                                    }
                                } else null,
                                onQuickMoveNext = if (nextColumn != null && onQuickMoveTask != null) {
                                    {
                                        onQuickMoveTask(task, nextColumn.id)
                                        Toast.makeText(context, s.movedToHub(s.localized(nextColumn.title)), Toast.LENGTH_SHORT).show()
                                    }
                                } else null,
                                onCommentClick = { onCommentClick(task) },
                                onMoveClick = { onMoveTaskClick(task) },
                                onPositioned = { rect -> onTaskPositioned(task, rect) },
                                onDragStart = { offset -> onDragStart(task, offset) },
                                onDrag = onDrag,
                                onDragEnd = onDragEnd,
                                onEditClick = { onEditTaskClick(task) },
                                onDeleteClick = { onDeleteTaskClick(task.id) },
                                onToggleCompleted = { onToggleCompleted(task) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text(s.clearCompletedTitle, fontWeight = FontWeight.Bold) },
            text = { Text(s.clearCompletedText(completedTasks.size, s.localized(column.title))) },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(deleteButtonGradient)
                        .clickable {
                            viewModel.clearCompletedTasks(column.id)
                            showClearConfirmDialog = false
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(s.clear, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(s.cancel, style = MaterialTheme.typography.titleMedium)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: Task,
    categories: List<Category>,
    viewModel: SharedViewModel,
    modifier: Modifier = Modifier,
    commentsCount: Int = 0,
    isInProgress: Boolean = false,
    isHighlighted: Boolean = false,
    isAnyTaskInProgress: Boolean = false,
    isDragging: Boolean = false,
    isCompact: Boolean = false,
    isDragPreview: Boolean = false,
    relatedBoards: List<RelatedBoardLink> = emptyList(),
    onOpenRelatedBoard: (boardId: String, columnId: String) -> Unit = { _, _ -> },
    onRemoveFromBoard: (() -> Unit)? = null,
    onSetQuadrant: ((String?) -> Unit)? = null,
    onToggleInProgress: () -> Unit = {},
    prevColumnTitle: String? = null,
    nextColumnTitle: String? = null,
    onQuickMovePrev: (() -> Unit)? = null,
    onQuickMoveNext: (() -> Unit)? = null,
    onCommentClick: () -> Unit = {},
    onMoveClick: () -> Unit = {},
    onPositioned: (androidx.compose.ui.geometry.Rect) -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleCompleted: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val s = LocalAppStrings.current
    val dateLocale = LocalAppLanguage.current.locale
    val category = categories.find { it.id == task.categoryId }
    val hasStatus = category != null && !KanbanNames.isUncategorized(category.name)

    val toggleCompleted = {
        onToggleCompleted?.invoke() ?: viewModel.toggleTaskCompletion(task)
    }

    val targetAlpha = when {
        isDragging -> 0.18f
        isAnyTaskInProgress && !isInProgress -> 0.32f
        task.isHidden -> 0.55f
        else -> 1f
    }

    val density = LocalDensity.current
    var swipeOffsetX by remember(task.id) { mutableFloatStateOf(0f) }
    var cardWidthPx by remember(task.id) { mutableFloatStateOf(0f) }
    var showSwipeMoreMenu by remember(task.id) { mutableStateOf(false) }
    // Two full buttons: More · Delete
    val actionsWidthPx = with(density) { (44.dp * 2 + 8.dp * 1 + 8.dp).toPx() }
    val peekPx = with(density) { 56.dp.toPx() }
    val maxSwipePx = remember(actionsWidthPx, cardWidthPx, peekPx) {
        if (cardWidthPx <= 0f) {
            actionsWidthPx
        } else {
            minOf(actionsWidthPx, (cardWidthPx - peekPx).coerceAtLeast(0f))
        }
    }
    var cardBoundsInRoot by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var showDueEditor by remember(task.id) { mutableStateOf(false) }
    var showDueDatePicker by remember(task.id) { mutableStateOf(false) }
    val dueDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = task.dueDate?.let { localMillisToUtcPicker(it) }
    )

    if (showDueEditor) {
        AlertDialog(
            onDismissRequest = { showDueEditor = false },
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
                        showDueEditor = false
                    },
                    onPickCustom = { showDueDatePicker = true }
                )
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDueEditor = false }) { Text(s.cancel) }
            }
        )
    }
    if (showDueDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = dueDatePickerState.selectedDateMillis
                    if (selected != null) {
                        viewModel.updateTask(task.copy(dueDate = utcPickerMillisToLocalNoon(selected)))
                    }
                    showDueDatePicker = false
                    showDueEditor = false
                }) { Text(s.ok) }
            },
            dismissButton = {
                TextButton(onClick = { showDueDatePicker = false }) { Text(s.cancel) }
            }
        ) {
            DatePicker(state = dueDatePickerState)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .graphicsLayer { alpha = targetAlpha }
            .onSizeChanged { cardWidthPx = it.width.toFloat() }
            .onGloballyPositioned { coordinates ->
                val bounds = androidx.compose.ui.geometry.Rect(
                    offset = coordinates.positionInRoot(),
                    size = androidx.compose.ui.geometry.Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
                )
                cardBoundsInRoot = bounds
                onPositioned(bounds)
            }
            .then(
                if (isDragPreview) Modifier else Modifier.pointerInput(task.id, maxSwipePx) {
                    detectHorizontalDragGestures(
                        onDragStart = {},
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            swipeOffsetX = (swipeOffsetX + dragAmount).coerceIn(-maxSwipePx, 0f)
                        },
                        onDragEnd = {
                            swipeOffsetX = if (swipeOffsetX < -maxSwipePx * 0.35f) -maxSwipePx else 0f
                        },
                        onDragCancel = {
                            swipeOffsetX = 0f
                        }
                    )
                }
            )
    ) {
        // Primary swipe actions — always fully visible; extras live under "More"
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
                .graphicsLayer {
                    alpha = ((-swipeOffsetX) / 30f).coerceIn(0f, 1f)
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable { showSwipeMoreMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = s.moreActions,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                DropdownMenu(
                    expanded = showSwipeMoreMenu,
                    onDismissRequest = { showSwipeMoreMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (commentsCount > 0) s.commentsCount(commentsCount) else s.comments,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        onClick = {
                            showSwipeMoreMenu = false
                            swipeOffsetX = 0f
                            onCommentClick()
                        },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(s.edit, style = MaterialTheme.typography.bodyLarge) },
                        onClick = {
                            showSwipeMoreMenu = false
                            swipeOffsetX = 0f
                            onEditClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (task.isHidden) s.showTask else s.hideTask,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        onClick = {
                            showSwipeMoreMenu = false
                            swipeOffsetX = 0f
                            viewModel.toggleTaskHidden(task)
                        },
                        leadingIcon = {
                            Icon(
                                if (task.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    )
                    if (task.dueDate != null) {
                        val prefs = remember { KairosPreferences(context) }
                        val defaultClock = DueReminderScheduler.formatClock(
                            prefs.reminderHour,
                            prefs.reminderMinute
                        )
                        val reminderLabel = task.reminderMinutesOfDay?.let {
                            s.reminderCustom(DueReminderScheduler.formatMinutesOfDay(it))
                        } ?: s.reminderAt(defaultClock)
                        DropdownMenuItem(
                            text = {
                                Text(reminderLabel, style = MaterialTheme.typography.bodyLarge)
                            },
                            onClick = {
                                showSwipeMoreMenu = false
                                swipeOffsetX = 0f
                                android.app.AlertDialog.Builder(context)
                                    .setTitle(s.reminderTaskTime)
                                    .setItems(
                                        arrayOf(
                                            "${s.reminderUseDefault} ($defaultClock)",
                                            s.reminderPickTime
                                        )
                                    ) { _, which ->
                                        when (which) {
                                            0 -> viewModel.updateTask(task.copy(reminderMinutesOfDay = null))
                                            1 -> {
                                                val initial = task.reminderMinutesOfDay
                                                    ?: (prefs.reminderHour * 60 + prefs.reminderMinute)
                                                android.app.TimePickerDialog(
                                                    context,
                                                    { _, hour, minute ->
                                                        viewModel.updateTask(
                                                            task.copy(reminderMinutesOfDay = hour * 60 + minute)
                                                        )
                                                    },
                                                    initial / 60,
                                                    initial % 60,
                                                    true
                                                ).show()
                                            }
                                        }
                                    }
                                    .show()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Schedule, contentDescription = null)
                            }
                        )
                    }
                    if (relatedBoards.isNotEmpty()) {
                        relatedBoards.forEach { link ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        s.localized(link.board.name),
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                onClick = {
                                    showSwipeMoreMenu = false
                                    swipeOffsetX = 0f
                                    onOpenRelatedBoard(link.board.id, link.columnId)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Link, contentDescription = null)
                                }
                            )
                        }
                    }
                    if (onRemoveFromBoard != null) {
                        DropdownMenuItem(
                            text = { Text(s.removeFromThisBoard, style = MaterialTheme.typography.bodyLarge) },
                            onClick = {
                                showSwipeMoreMenu = false
                                swipeOffsetX = 0f
                                onRemoveFromBoard()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.LinkOff, contentDescription = null)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(s.moveTask, style = MaterialTheme.typography.bodyLarge) },
                        onClick = {
                            showSwipeMoreMenu = false
                            swipeOffsetX = 0f
                            onMoveClick()
                        },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null)
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(deleteButtonGradient)
                    .clickable {
                        swipeOffsetX = 0f
                        onDeleteClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = s.delete,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
            }
        }

        if (isCompact) {
            val compactEisenhower = !task.isCompleted && task.eisenhowerQuadrant != null
            val compactOnColor = if (compactEisenhower) {
                eisenhowerOnColor(task.eisenhowerQuadrant)
            } else if (task.isCompleted) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(swipeOffsetX.roundToInt(), 0) }
                    .then(
                        if (isDragPreview) Modifier else Modifier
                            .pointerInput(task.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { localOffset ->
                                        swipeOffsetX = 0f
                                        onDragStart(cardBoundsInRoot.topLeft + localOffset)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        onDrag(dragAmount)
                                    },
                                    // End = finger up (finish). Cancel = scroll disposed the
                                    // source gesture — overlay keeps the drag alive.
                                    onDragEnd = { onDragEnd() },
                                    onDragCancel = {}
                                )
                            }
                    )
                    .then(
                        if (compactEisenhower) {
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(eisenhowerGradient(task.eisenhowerQuadrant))
                        } else Modifier
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        compactEisenhower -> Color.Transparent
                        task.isCompleted -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.surface
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isInProgress) 6.dp else 0.dp),
                border = when {
                    isHighlighted -> androidx.compose.foundation.BorderStroke(2.5.dp, MaterialTheme.colorScheme.tertiary)
                    isInProgress -> androidx.compose.foundation.BorderStroke(2.dp, inProgressGradient)
                    compactEisenhower -> androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                    category != null && !KanbanNames.isUncategorized(category.name) ->
                        androidx.compose.foundation.BorderStroke(1.5.dp, Color(category.color).copy(alpha = 0.7f))
                    task.isCompleted ->
                        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    else ->
                        androidx.compose.foundation.BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (compactEisenhower) Modifier.background(Color.Black.copy(alpha = 0.22f))
                            else Modifier
                        )
                        .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var titleExpanded by remember(task.id) { mutableStateOf(false) }
                    var titleOverflows by remember(task.id, task.title) { mutableStateOf(false) }
                    val compactOverdue = !task.isCompleted && task.dueDate != null && task.dueDate.isOverdueDate()
                    if (compactOverdue) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.error)
                        )
                    }
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Normal,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 0.15.sp,
                            lineHeight = 22.sp
                        ),
                        maxLines = if (titleExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        color = compactOnColor,
                        onTextLayout = { result ->
                            if (!titleExpanded) titleOverflows = result.hasVisualOverflow
                        },
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(task.id, titleOverflows, titleExpanded) {
                                detectTapGestures(
                                    onTap = {
                                        if (swipeOffsetX < 0f) {
                                            swipeOffsetX = 0f
                                        } else if (titleOverflows || titleExpanded) {
                                            titleExpanded = !titleExpanded
                                        }
                                    },
                                    onDoubleTap = {
                                        swipeOffsetX = 0f
                                        onMoveClick()
                                    }
                                )
                            }
                    )
                    if (task.dueDate != null) {
                        Text(
                            text = task.dueDate.relativeDueLabel(s, dateLocale, task.isCompleted),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                compactOverdue -> if (compactEisenhower) Color.White else MaterialTheme.colorScheme.error
                                compactEisenhower -> Color.White.copy(alpha = 0.85f)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { showDueEditor = true }
                        )
                    }
                    if (relatedBoards.isNotEmpty()) {
                        RelatedBoardsButton(
                            links = relatedBoards,
                            onOpen = { link -> onOpenRelatedBoard(link.board.id, link.columnId) },
                            iconTint = if (compactEisenhower) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                            background = if (compactEisenhower) {
                                Color.White.copy(alpha = 0.22f)
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                            size = 36.dp
                        )
                    }
                    if (!task.isCompleted) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .then(
                                    if (isInProgress) Modifier.background(inProgressGradient)
                                    else if (compactEisenhower) Modifier
                                        .background(Color.White.copy(alpha = 0.22f))
                                        .border(1.2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                    else Modifier
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(1.2.dp, inProgressColor.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                                )
                                .clickable { onToggleInProgress() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = s.inProgress,
                                modifier = Modifier.size(24.dp),
                                tint = if (isInProgress || compactEisenhower) Color.White else inProgressColor
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (task.isCompleted) Modifier.background(Color(0xFF00C853))
                                else if (compactEisenhower) Modifier
                                    .background(Color.White.copy(alpha = 0.22f))
                                    .border(1.5.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                                else Modifier
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.5.dp, Color(0xFF00C853).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                            )
                            .clickable { toggleCompleted() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = s.completed,
                            modifier = Modifier.size(24.dp),
                            tint = when {
                                task.isCompleted -> Color.White
                                compactEisenhower -> Color.White
                                else -> Color(0xFF00C853)
                            }
                        )
                    }
                }
            }
        } else {
        // Foreground Card (solid opaque background prevents background buttons from showing through)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(swipeOffsetX.roundToInt(), 0) }
                .then(
                    if (isDragPreview) Modifier else Modifier
                        .pointerInput(task.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { localOffset ->
                                    swipeOffsetX = 0f
                                    onDragStart(cardBoundsInRoot.topLeft + localOffset)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onDrag(dragAmount)
                                },
                                // End = finger up (finish). Cancel = scroll disposed the
                                // source gesture — overlay keeps the drag alive.
                                onDragEnd = { onDragEnd() },
                                onDragCancel = {}
                            )
                        }
                )
                .clickable(enabled = !isDragPreview) {
                    if (swipeOffsetX < 0f) {
                        swipeOffsetX = 0f
                    }
                },
            colors = CardDefaults.cardColors(
                containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isInProgress) 6.dp else 0.dp),
            border = when {
                isInProgress -> androidx.compose.foundation.BorderStroke(2.dp, inProgressGradient)
                category != null && !KanbanNames.isUncategorized(category.name) -> 
                    androidx.compose.foundation.BorderStroke(1.5.dp, Color(category.color).copy(alpha = 0.7f))
                task.eisenhowerQuadrant != null ->
                    androidx.compose.foundation.BorderStroke(1.5.dp, eisenhowerSolid(task.eisenhowerQuadrant).copy(alpha = 0.75f))
                task.isCompleted -> 
                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                else -> 
                    androidx.compose.foundation.BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            },
            shape = RoundedCornerShape(12.dp)
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Main card body
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)
                ) {
                    // Top row: Title + In-Progress indicator + subtle left swipe hint
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        ) {
                            if (isInProgress) {
                                Box(
                                    modifier = Modifier
                                        .padding(bottom = 6.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(inProgressGradient)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = s.nowInProgress,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (task.isCompleted) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (relatedBoards.isNotEmpty()) {
                            RelatedBoardsButton(
                                links = relatedBoards,
                                onOpen = { link -> onOpenRelatedBoard(link.board.id, link.columnId) },
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                background = MaterialTheme.colorScheme.secondaryContainer,
                                size = 32.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = s.swipeForActions,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                    }

                // Completed timestamp
                if (task.isCompleted) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = Color(0xFF00C853)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val closedText = if (task.completedAt != null) {
                            s.closedAt(task.completedAt.formatDateTime(dateLocale))
                        } else {
                            s.completed
                        }
                        Text(
                            text = closedText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }

                // Metadata: Status (ONLY if specified!), Due date, Comments
                if (hasStatus || task.dueDate != null || commentsCount > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Status: "Если статус не указан то его не нужно отображать."
                        if (category != null && !KanbanNames.isUncategorized(category.name)) {
                            Surface(
                                modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(category.color).copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(category.color).copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(getStatusBrush(category.color))
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = s.localized(category.name),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(category.color),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        // Right side of metadata: Comments count & Due date
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Comments count badge if comments exist
                            if (commentsCount > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Comment,
                                        contentDescription = s.comments,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = commentsCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Due date
                            if (task.dueDate != null) {
                                val overdue = !task.isCompleted && task.dueDate.isOverdueDate()
                                Spacer(modifier = Modifier.width(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { showDueEditor = true }
                                ) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = task.dueDate.relativeDueLabel(s, dateLocale, task.isCompleted),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // 1-Tap Eisenhower Matrix Quadrant Selector (only if enabled or assigned)
                if (task.showEisenhowerButtons || task.eisenhowerQuadrant != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = s.matrix,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 72.dp)
                        )

                        val quadrants = listOf(
                            Eisenhower.Q1 to "Q1",
                            Eisenhower.Q2 to "Q2",
                            Eisenhower.Q3 to "Q3",
                            Eisenhower.Q4 to "Q4"
                        )

                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quadrants.forEach { (qKey, qLabel) ->
                                val isSelected = task.eisenhowerQuadrant == qKey
                                EisenhowerQuadrantChip(
                                    qKey = qKey,
                                    qLabel = qLabel,
                                    isSelected = isSelected,
                                    dimUnselected = task.eisenhowerQuadrant != null && !isSelected,
                                    onClick = {
                                        onSetQuadrant?.invoke(if (isSelected) null else qKey)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Ergonomic Thumb-Zone Action Buttons: "В работе" and "Выполнено"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!task.isCompleted) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .then(
                                    if (isInProgress) Modifier.background(inProgressGradient)
                                    else Modifier
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(1.2.dp, inProgressColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                )
                                .clickable { onToggleInProgress() }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isInProgress) Color.White else inProgressColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = s.inProgress,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isInProgress) Color.White else inProgressColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                        }
                    }

                    // "Выполнено" button (at the bottom, ultra accessible for thumb!)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .then(
                                if (task.isCompleted) Modifier.background(Color(0xFF00C853))
                                else Modifier
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.5.dp, Color(0xFF00C853).copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                            )
                            .clickable { toggleCompleted() }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (task.isCompleted) Color.White else Color(0xFF00C853)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = s.completed,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (task.isCompleted) Color.White else Color(0xFF00C853),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }
                }
            }

            // Move Hub/Board Footer: Segmented full-width bar without empty voids or overflowing text
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.8.dp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Move to Previous Hub (Left chevron)
                val hasPrev = onQuickMovePrev != null
                Surface(
                    onClick = onQuickMovePrev ?: {},
                    enabled = hasPrev,
                    shape = RoundedCornerShape(8.dp),
                    color = if (hasPrev) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.2.dp,
                        if (hasPrev) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = if (prevColumnTitle != null) s.toHub(s.localized(prevColumnTitle)) else s.previousHub,
                            modifier = Modifier.size(22.dp),
                            tint = if (hasPrev) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        )
                    }
                }

                // 2. Change Hub / Board (Center prominent button)
                Surface(
                    onClick = onMoveClick,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .weight(1.8f)
                        .height(36.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                            contentDescription = s.changeHubBoard,
                            modifier = Modifier.size(17.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = s.changeHub,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }

                // 3. Move to Next Hub (Right chevron)
                val hasNext = onQuickMoveNext != null
                Surface(
                    onClick = onQuickMoveNext ?: {},
                    enabled = hasNext,
                    shape = RoundedCornerShape(8.dp),
                    color = if (hasNext) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.2.dp,
                        if (hasNext) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = if (nextColumnTitle != null) s.toHub(s.localized(nextColumnTitle)) else s.nextHub,
                            modifier = Modifier.size(22.dp),
                            tint = if (hasNext) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        )
                    }
                }
            }
        }
        }
        }
    }
}

@Composable
fun TaskCommentsDialog(
    task: Task,
    comments: List<Comment>,
    onAddComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    val dateLocale = LocalAppLanguage.current.locale
    var commentText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Comment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = s.comments,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                if (comments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = s.noCommentsYet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(comments, key = { it.id }) { comment ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = comment.text,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = comment.createdAt.formatDateTime(dateLocale),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(deleteButtonGradient)
                                            .clickable { onDeleteComment(comment.id) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = s.deleteComment,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text(s.writeComment) },
                        modifier = Modifier.weight(1f),
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                onAddComment(commentText)
                                commentText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = s.send,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(s.close, style = MaterialTheme.typography.titleMedium)
            }
        }
    )
}

@Composable
fun HubCommentsDialog(
    column: Column,
    comments: List<ColumnComment>,
    onAddComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    val dateLocale = LocalAppLanguage.current.locale
    var commentText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Comment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = s.hubRules,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = s.hubWithName(s.localized(column.title)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                if (comments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = s.noHubRulesYet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(comments, key = { it.id }) { comment ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = comment.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = comment.createdAt.formatDateTime(dateLocale),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(deleteButtonGradient)
                                            .clickable { onDeleteComment(comment.id) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = s.deleteComment,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text(s.writeRule, style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f),
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                onAddComment(commentText)
                                commentText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = s.send,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(s.close, style = MaterialTheme.typography.titleMedium)
            }
        }
    )
}

@Composable
fun MoveTaskDialog(
    task: Task,
    columns: List<Column>,
    allColumns: List<Column>,
    sourceColumnId: String,
    otherBoards: List<Board>,
    boards: List<Board>,
    projects: List<Project>,
    initialExpandedBoardId: String? = null,
    onMoveToColumn: (columnId: String) -> Unit,
    onMoveToBoard: (boardId: String) -> Unit,
    onCopyToColumn: (columnId: String) -> Unit,
    onCopyToBoard: (boardId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    var isCopyMode by remember { mutableStateOf(false) }
    var expandedBoardId by remember { mutableStateOf(initialExpandedBoardId) }
    val currentColumnIndex = columns.indexOfFirst { it.id == sourceColumnId }.takeIf { it >= 0 }
        ?: columns.indexOfFirst { col -> taskAppearsOnColumn(task, col, allColumns, boards) }
    val prevColumn = if (currentColumnIndex > 0) columns[currentColumnIndex - 1] else null
    val nextColumn = if (currentColumnIndex >= 0 && currentColumnIndex < columns.size - 1) columns[currentColumnIndex + 1] else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isCopyMode) Icons.Default.Link else Icons.AutoMirrored.Filled.DriveFileMove,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCopyMode) s.copyTask else s.moveTask,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Mode Selector: icon + short one-line label
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        onClick = { isCopyMode = false },
                        shape = RoundedCornerShape(8.dp),
                        color = if (!isCopyMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                                contentDescription = s.move,
                                modifier = Modifier.size(20.dp),
                                tint = if (!isCopyMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = s.move,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (!isCopyMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isCopyMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        onClick = { isCopyMode = true },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isCopyMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = s.copy,
                                modifier = Modifier.size(20.dp),
                                tint = if (isCopyMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = s.copy,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isCopyMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCopyMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (isCopyMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = s.alsoShowHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quick Move adjacent columns (only in move mode)
                if (!isCopyMode && (prevColumn != null || nextColumn != null)) {
                    item {
                        Text(
                            text = s.quickJump,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (prevColumn != null) {
                                Button(
                                    onClick = { onMoveToColumn(prevColumn.id) },
                                    modifier = Modifier.weight(1f).heightIn(min = 46.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = s.localized(prevColumn.title),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }
                            }
                            if (nextColumn != null) {
                                Button(
                                    onClick = { onMoveToColumn(nextColumn.id) },
                                    modifier = Modifier.weight(1f).heightIn(min = 46.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = s.localized(nextColumn.title),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // All columns on this board
                item {
                    if (!isCopyMode && (prevColumn != null || nextColumn != null)) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                    Text(
                        text = if (isCopyMode) s.copyToHubOnBoard else s.hubsOnThisBoard,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(columns, key = { it.id }) { col ->
                    val isCurrent = taskAppearsOnColumn(task, col, allColumns, boards)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = !isCurrent) {
                                if (isCopyMode) onCopyToColumn(col.id) else onMoveToColumn(col.id)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent && !isCopyMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isCurrent && !isCopyMode) 1.5.dp else 1.dp,
                            color = if (isCurrent && !isCopyMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = s.localized(col.title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isCurrent && !isCopyMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent && !isCopyMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            if (isCopyMode) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = s.copy,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else if (isCurrent) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = s.here,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // Other boards
                if (otherBoards.isNotEmpty()) {
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = if (isCopyMode) s.copyToOtherBoard else s.orOtherBoard,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(otherBoards, key = { it.id }) { targetBoard ->
                        val parentProject = projects.find { it.id == targetBoard.projectId }
                        val hubs = allColumns.filter { it.boardId == targetBoard.id }.sortedBy { it.position }
                        val expanded = expandedBoardId == targetBoard.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp)),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (hubs.isEmpty()) {
                                                if (isCopyMode) onCopyToBoard(targetBoard.id) else onMoveToBoard(targetBoard.id)
                                            } else {
                                                expandedBoardId = if (expanded) null else targetBoard.id
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = s.localized(targetBoard.name),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (parentProject != null) {
                                            Text(
                                                text = parentProject.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = s.chooseHub,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (expanded) {
                                    if (hubs.isEmpty()) {
                                        Text(
                                            text = s.boardHasNoHubs,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
                                        )
                                    } else {
                                        hubs.forEach { hub ->
                                            val alreadyHere = taskAppearsOnColumn(task, hub, allColumns, boards)
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable(enabled = !alreadyHere) {
                                                        if (isCopyMode) onCopyToColumn(hub.id) else onMoveToColumn(hub.id)
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = s.localized(hub.title),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (alreadyHere && !isCopyMode) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    },
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                                )
                                                if (isCopyMode) {
                                                    Icon(
                                                        imageVector = Icons.Default.Link,
                                                        contentDescription = s.copy,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                } else if (alreadyHere) {
                                                    Text(
                                                        text = s.here,
                                                        style = MaterialTheme.typography.labelSmall,
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
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(s.cancel, style = MaterialTheme.typography.titleMedium)
            }
        }
    )
}

@Composable
fun ReorderColumnsDialog(
    columns: List<Column>,
    onMoveColumn: (columnId: String, direction: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    s.hubOrder,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(columns, key = { _, col -> col.id }) { index, col ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${index + 1}.",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = s.localized(col.title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { onMoveColumn(col.id, -1) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = s.left,
                                        tint = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onMoveColumn(col.id, 1) },
                                    enabled = index < columns.size - 1,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = s.right,
                                        tint = if (index < columns.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(s.done, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun DropInsertSlot(isCompact: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isCompact) 56.dp else 84.dp)
            .border(
                2.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                RoundedCornerShape(12.dp)
            )
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                RoundedCornerShape(12.dp)
            )
    )
}

@Composable
fun RowScope.EisenhowerQuadrantChip(
    qKey: String,
    qLabel: String,
    isSelected: Boolean,
    dimUnselected: Boolean = false,
    onClick: () -> Unit
) {
    val qColor = eisenhowerSolid(qKey)
    val brush = eisenhowerGradient(qKey)
    val fillAlpha = when {
        isSelected -> 1f
        dimUnselected -> 0.08f
        else -> 0.22f
    }
    val labelAlpha = when {
        isSelected -> 1f
        dimUnselected -> 0.35f
        else -> 1f
    }
    Box(
        modifier = Modifier
            .weight(1f)
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
                .alpha(fillAlpha)
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.5.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            )
        }
        Text(
            text = qLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (isSelected) Color.White else qColor.copy(alpha = labelAlpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
        )
    }
}