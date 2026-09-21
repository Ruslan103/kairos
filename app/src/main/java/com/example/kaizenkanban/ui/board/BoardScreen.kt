package com.example.kaizenkanban.ui.board

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.snapping.SnapFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPositionInLayout
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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.rememberSplineBasedDecay
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.Velocity
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
import com.example.kaizenkanban.widget.KairosWidgetUpdater
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow
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
import com.example.kaizenkanban.ui.theme.getStatusBrush
import com.example.kaizenkanban.ui.theme.deleteButtonGradient
import com.example.kaizenkanban.ui.theme.deleteButtonColor
import com.example.kaizenkanban.ui.theme.newProjectGradient
import com.example.kaizenkanban.ui.theme.inProgressColor
import com.example.kaizenkanban.ui.theme.inProgressGradient
import com.example.kaizenkanban.ui.viewmodel.SharedViewModel

fun Long.formatDate(locale: Locale = Locale.getDefault()): String =
    cachedDateFormat(locale, "dd MMM yyyy").format(Date(this))

fun Long.formatDateTime(locale: Locale = Locale.getDefault()): String =
    cachedDateFormat(locale, "dd MMM yyyy, HH:mm").format(Date(this))

private val dateFormatCache =
    ThreadLocal.withInitial { HashMap<String, SimpleDateFormat>() }

private fun cachedDateFormat(locale: Locale, pattern: String): SimpleDateFormat {
    val key = "${locale.toLanguageTag()}|$pattern"
    val map = dateFormatCache.get()
    return map.getOrPut(key) { SimpleDateFormat(pattern, locale) }
}

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
    val iconSize = size * 0.5f
    Box {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp))
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
                modifier = Modifier.size(iconSize.coerceAtLeast(10.dp)),
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
    onNavigateToSettings: () -> Unit = {},
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

    // Precompute once — relatedBoardsForTask is expensive if run per TaskCard recomposition.
    val relatedBoardsByTaskId = remember(tasksByHubId, state.columns, state.boards, currentBoardId) {
        buildMap {
            tasksByHubId.values.asSequence()
                .flatten()
                .distinctBy { it.id }
                .forEach { task ->
                    put(
                        task.id,
                        relatedBoardsForTask(task, currentBoardId, state.columns, state.boards)
                    )
                }
        }
    }

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
    /** Telegram-archive style: pull-down peek of board chrome while top bar is hidden (px). */
    var chromePullPx by remember { mutableFloatStateOf(0f) }
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
                KairosWidgetUpdater.updateAll(context, tasks)
            }
        }
    }

    val lazyListState = rememberLazyListState()
    val tabRowState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    // Soft pager snap: very low stiffness so page-edge hand-off eases in instead of catching.
    val hubSnapDecay = rememberSplineBasedDecay<Float>()
    val hubSnapDensity = LocalDensity.current
    val flingBehavior = remember(lazyListState, hubSnapDecay, hubSnapDensity) {
        val snapThresholdPx = with(hubSnapDensity) { 0.5.dp.toPx() }
        val softSnap = spring(
            dampingRatio = 0.96f,
            stiffness = 38f,
            visibilityThreshold = snapThresholdPx
        )
        val softLowVelocity = spring(
            dampingRatio = 0.98f,
            stiffness = 28f,
            visibilityThreshold = snapThresholdPx
        )
        val snappingLayout = SnapLayoutInfoProvider(
            lazyListState = lazyListState,
            positionInLayout = SnapPositionInLayout { _, _, beforeContentPadding, _, _ ->
                beforeContentPadding
            }
        )
        SnapFlingBehavior(
            snapLayoutInfoProvider = snappingLayout,
            lowVelocityAnimationSpec = softLowVelocity,
            highVelocityAnimationSpec = hubSnapDecay,
            snapAnimationSpec = softSnap
        )
    }
    var currentColumnIndex by remember { mutableIntStateOf(0) }
    var primaryHubId by remember(currentBoardId) {
        mutableStateOf(kairosPrefs.getPrimaryHubId(currentBoardId))
    }
    var lastBoardOpenedForPrimary by remember { mutableStateOf<String?>(null) }

    val hubCount = columns.size
    // Pager: [hubs…][addHub] — finite carousel (no wrap clones).
    val hubPagerCount = hubCount + 1
    val addHubVirtual = hubCount

    fun realToVirtual(real: Int): Int =
        real.coerceIn(0, (hubCount - 1).coerceAtLeast(0))

    fun isAddHubVirtual(virtual: Int): Boolean = virtual == addHubVirtual

    fun virtualToReal(virtual: Int): Int {
        if (hubCount <= 0) return 0
        return virtual.coerceIn(0, hubCount - 1)
    }

    fun primaryHubIndex(): Int {
        if (columns.isEmpty()) return 0
        val preferred = primaryHubId
        val idx = if (preferred != null) columns.indexOfFirst { it.id == preferred } else -1
        return if (idx >= 0) idx else 0
    }

    fun isPrimaryHub(columnId: String, index: Int): Boolean {
        val preferred = primaryHubId ?: return index == 0
        val stillExists = columns.any { it.id == preferred }
        return if (stillExists) columnId == preferred else index == 0
    }

    LaunchedEffect(currentBoardId) {
        primaryHubId = kairosPrefs.getPrimaryHubId(currentBoardId)
    }

    // Keep primary hub by id (not by list index). Clear stale ids; if unset, lock to the first
    // hub so reorder/swipe cannot make the ★ jump to a different column.
    LaunchedEffect(currentBoardId, columns.map { it.id }) {
        if (columns.isEmpty()) return@LaunchedEffect
        val stored = kairosPrefs.getPrimaryHubId(currentBoardId)
        when {
            stored != null && columns.any { it.id == stored } -> {
                if (primaryHubId != stored) primaryHubId = stored
            }
            else -> {
                val fallback = columns.first().id
                primaryHubId = fallback
                kairosPrefs.setPrimaryHubId(currentBoardId, fallback)
            }
        }
    }

    // Open primary hub only when the board actually changes (not on every task/columns emit).
    LaunchedEffect(currentBoardId, columns.isNotEmpty()) {
        if (pendingHubColumnId != null) return@LaunchedEffect
        if (columns.isEmpty()) return@LaunchedEffect
        if (lastBoardOpenedForPrimary == currentBoardId) {
            if (currentColumnIndex !in columns.indices) {
                val index = primaryHubIndex()
                currentColumnIndex = index
                lazyListState.scrollToItem(realToVirtual(index))
            }
            return@LaunchedEffect
        }
        lastBoardOpenedForPrimary = currentBoardId
        val index = primaryHubIndex()
        currentColumnIndex = index
        lazyListState.scrollToItem(realToVirtual(index))
    }

    LaunchedEffect(currentBoardId, pendingHubColumnId, columns) {
        val hubId = pendingHubColumnId ?: return@LaunchedEffect
        val index = columns.indexOfFirst { it.id == hubId }
        if (index >= 0) {
            lastBoardOpenedForPrimary = currentBoardId
            lazyListState.animateScrollToItem(realToVirtual(index))
            currentColumnIndex = index
            pendingHubColumnId = null
            return@LaunchedEffect
        }
        val hubBoardId = state.columns.find { it.id == hubId }?.boardId
        if (hubBoardId == null || hubBoardId == currentBoardId) {
            pendingHubColumnId = null
        }
    }

    // Prefer the hub closest to viewport center; update selection only after scroll settles
    // to avoid tab/chip jitter while snap fling is still moving.
    fun hubVirtualNearCenter(): Int {
        val layout = lazyListState.layoutInfo
        val visible = layout.visibleItemsInfo
        val maxIndex = (hubPagerCount - 1).coerceAtLeast(0)
        if (visible.isEmpty()) {
            return lazyListState.firstVisibleItemIndex.coerceIn(0, maxIndex)
        }
        val viewportCenter = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
        return visible.minByOrNull { info ->
            kotlin.math.abs((info.offset + info.size / 2) - viewportCenter)
        }?.index?.coerceIn(0, maxIndex)
            ?: lazyListState.firstVisibleItemIndex.coerceIn(0, maxIndex)
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
    val dropPreviewState = remember { mutableStateOf<TaskDropPreview?>(null) }
    var hoveredOtherBoardId by remember { mutableStateOf<String?>(null) }

    // Only the settled hub keeps full TaskCards; peeks stay lite. Settled page updates after
    // scroll stops — so the open hub keeps its buttons for the whole swipe (cheap enough;
    // the FPS win is lite neighbors + deferred settle, not stripping the current page).
    var hubSettledVirtual by remember { mutableIntStateOf(0) }
    /** Pull-down grabber appears with task action buttons after hub settle — not mid-swipe. */
    var hubGrabberVisible by remember { mutableStateOf(true) }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (scrolling) hubGrabberVisible = false
            }
    }

    LaunchedEffect(lazyListState, hubPagerCount) {
        snapshotFlow {
            Triple(
                lazyListState.isScrollInProgress,
                draggedTask != null,
                hubVirtualNearCenter()
            )
        }
            .distinctUntilChanged()
            .collectLatest { (scrolling, dragging, center) ->
                if (dragging || scrolling) {
                    hubGrabberVisible = false
                    return@collectLatest
                }
                // Wait for soft snap ease to finish before swapping lite→full.
                kotlinx.coroutines.delay(200)
                hubSettledVirtual = center
                hubGrabberVisible = !isAddHubVirtual(center)
                if (!isAddHubVirtual(center)) {
                    val real = virtualToReal(center)
                    if (real != currentColumnIndex) currentColumnIndex = real
                }
            }
    }

    LaunchedEffect(currentColumnIndex, columns.size) {
        if (columns.isNotEmpty() && !lazyListState.isScrollInProgress) {
            val tabTarget = (currentColumnIndex - 1).coerceAtLeast(0)
            if (tabRowState.firstVisibleItemIndex != tabTarget) {
                tabRowState.scrollToItem(tabTarget)
            }
        }
    }

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

    // Same side inset as TopAppBar actions end padding → ⋮ buttons share one trailing edge
    val columnWidth = screenWidth - (AlignedMoreEdgeGutter * 2)
    val dropSlotHeightPx = with(density) {
        56.dp.toPx() + 12.dp.toPx()
    }

    LaunchedEffect(draggedTask != null) {
        if (draggedTask == null) {
            dropPreviewState.value = null
            hoveredOtherBoardId = null
            return@LaunchedEffect
        }
        val edgeH = with(density) { 88.dp.toPx() }
        val edgeV = with(density) { 80.dp.toPx() }
        val maxStep = with(density) { 14.dp.toPx() }
        val minMovePx = with(density) { 6.dp.toPx() }
        var frame = 0
        var lastPreviewCol: String? = null
        var lastPreviewIndex = Int.MIN_VALUE
        var lastResolveX = Float.NaN
        var lastResolveY = Float.NaN
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
            val movedEnough = lastResolveX.isNaN() ||
                kotlin.math.abs(pos.x - lastResolveX) >= minMovePx ||
                kotlin.math.abs(pos.y - lastResolveY) >= minMovePx
            // Edge-scroll every tick; resolve drop slot only when pointer moved or hub page scrolled.
            val shouldResolve = movedEnough || (scrollingHoriz && frame % 3 == 0)
            if (shouldResolve && (!scrollingHoriz || frame % 3 == 0 || movedEnough)) {
                lastResolveX = pos.x
                lastResolveY = pos.y
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
                    dropPreviewState.value = nextPreview
                }
                lastDropPreview = nextPreview
            }
            delay(16)
        }
    }

    fun finishTaskDrag() {
        // Clear first so root Final-pass + card onDragEnd cannot double-commit the drop.
        val dragged = draggedTask ?: return
        draggedTask = null
        val previewSnapshot = lastDropPreview
        dropPreviewState.value = null
        hoveredOtherBoardId = null
        lastDropPreview = null

        val dragPos = currentDragPosition()
        val targetOtherBoardId = otherBoardBounds.entries.find { it.value.contains(dragPos) }?.key
        if (targetOtherBoardId != null) {
            taskForMove = dragged
            taskForMoveHubId = columns.find { taskAppearsOnColumn(dragged, it, state.columns, state.boards) }?.id
                ?: dragged.columnId
            pendingExpandBoardId = targetOtherBoardId
        } else {
            val resolved = if (previewSnapshot != null &&
                columnBounds[previewSnapshot.columnId]?.contains(dragPos) == true
            ) {
                previewSnapshot.columnId to previewSnapshot.insertIndex
            } else {
                resolveColumnDrop(dragPos, dragged, previewSnapshot, dropSlotHeightPx)
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
                        lazyListState.animateScrollToItem(realToVirtual(dropIndex))
                    }
                }
            }
        }
    }

    fun focusedHubIndex(): Int {
        val visible = lazyListState.layoutInfo.visibleItemsInfo
        val maxVirtual = (hubPagerCount - 1).coerceAtLeast(0)
        val virtual = if (visible.isEmpty()) {
            lazyListState.firstVisibleItemIndex.coerceIn(0, maxVirtual)
        } else {
            val viewportCenter =
                (lazyListState.layoutInfo.viewportStartOffset + lazyListState.layoutInfo.viewportEndOffset) / 2
            visible.minByOrNull { info ->
                kotlin.math.abs((info.offset + info.size / 2) - viewportCenter)
            }?.index?.coerceIn(0, maxVirtual)
                ?: lazyListState.firstVisibleItemIndex.coerceIn(0, maxVirtual)
        }
        if (isAddHubVirtual(virtual)) {
            return currentColumnIndex.coerceIn(0, (columns.size - 1).coerceAtLeast(0))
        }
        return virtualToReal(virtual)
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
                lazyListState.animateScrollToItem(realToVirtual(targetIndex))
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
            lazyListState.animateScrollToItem(realToVirtual(matchIndex))
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

    fun hideBoardChrome() {
        showTopBar = false
        showBoardsList = false
        showColumnHeaders = false
        boardSearchOpen = false
        chromePullPx = 0f
        topBarMenuExpanded = false
    }

    val boardChromeMaxPx = WindowInsets.statusBars.getTop(density).toFloat() +
        with(density) { BoardTopBarContentHeight.toPx() }

    fun settleChromePull() {
        coroutineScope.launch {
            val threshold = boardChromeMaxPx * 0.42f
            if (chromePullPx >= threshold) {
                showTopBar = true
                chromePullPx = 0f
            } else if (chromePullPx > 0f) {
                val anim = Animatable(chromePullPx)
                anim.animateTo(
                    0f,
                    animationSpec = spring(dampingRatio = 0.86f, stiffness = 420f)
                ) {
                    chromePullPx = value
                }
            }
        }
    }

    Scaffold(
        topBar = {
                val peekProgress =
                    if (showTopBar) 1f else (chromePullPx / boardChromeMaxPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
                if (peekProgress > 0.001f) {
                    val peekClipHeight = with(density) { (boardChromeMaxPx * peekProgress).toDp() }
                    Box(
                        modifier = if (showTopBar) {
                            Modifier.fillMaxWidth()
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .height(peekClipHeight)
                                .clipToBounds()
                        }
                    ) {
                    Column(
                        modifier = if (showTopBar) {
                            Modifier.fillMaxWidth()
                        } else {
                            Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                        }
                    ) {
                    // Compact board chrome (shorter than default 64.dp TopAppBar).
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .height(BoardTopBarContentHeight)
                                .padding(start = 12.dp, end = AlignedMoreEdgeGutter),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                    Text(
                        text = board?.name?.let { s.localized(it) } ?: s.board,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            lineHeight = 26.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { onBack() }
                                )
                            }
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                initialDelayMillis = 1200,
                                delayMillis = 1500
                            )
                    )
                    IconButton(
                        onClick = {
                            boardSearchOpen = !boardSearchOpen
                            if (!boardSearchOpen) {
                                boardSearchQuery = ""
                                keyboardController?.hide()
                            }
                        },
                        modifier = Modifier.size(BoardTopBarActionSize)
                    ) {
                        Icon(
                            imageVector = if (boardSearchOpen) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = s.searchTasks,
                            tint = if (boardSearchOpen || boardSearchQuery.isNotBlank()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (!compactWidth) {
                        IconButton(
                            onClick = { showReorderColumnsDialog = true },
                            modifier = Modifier.size(BoardTopBarActionSize)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = s.hubOrder,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        IconButton(
                            onClick = onNavigateToCalendar,
                            modifier = Modifier.size(BoardTopBarActionSize)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = s.calendar,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { showManageCategories = true },
                            modifier = Modifier.size(BoardTopBarActionSize)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Label,
                                contentDescription = s.manageStatuses,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { hideBoardChrome() },
                            modifier = Modifier.size(BoardTopBarActionSize)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = s.hideMenu,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    AlignedMoreMenuButton(
                        expanded = topBarMenuExpanded,
                        onExpandedChange = { topBarMenuExpanded = it },
                        tint = if (boardOverdueOnly) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    ) {
                            DropdownMenuItem(
                                text = { Text(s.projects) },
                                onClick = {
                                    topBarMenuExpanded = false
                                    onBack()
                                },
                                leadingIcon = { Icon(Icons.Default.GridView, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(s.settings) },
                                onClick = {
                                    topBarMenuExpanded = false
                                    onNavigateToSettings()
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                            )
                            val currentHub = columns.getOrNull(currentColumnIndex)
                            val isCurrentPrimary = currentHub != null && isPrimaryHub(currentHub.id, currentColumnIndex)
                            DropdownMenuItem(
                                text = { Text(if (isCurrentPrimary) s.primaryHub else s.setPrimaryHub) },
                                onClick = {
                                    topBarMenuExpanded = false
                                    val hub = columns.getOrNull(currentColumnIndex) ?: return@DropdownMenuItem
                                    primaryHubId = hub.id
                                    kairosPrefs.setPrimaryHubId(currentBoardId, hub.id)
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (isCurrentPrimary) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            LocalContentColor.current
                                        }
                                    )
                                },
                                trailingIcon = if (isCurrentPrimary) {
                                    {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                } else {
                                    null
                                }
                            )
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
                            if (columns.isNotEmpty()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                Text(
                                    text = s.allHubsHeader(columns.size),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                                Column(
                                    modifier = Modifier
                                        .heightIn(max = 280.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    columns.forEachIndexed { index, col ->
                                        val isSelected = index == currentColumnIndex
                                        val showPrimaryMark = isPrimaryHub(col.id, index)
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = s.localized(col.title),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    fontWeight = if (isSelected) {
                                                        FontWeight.SemiBold
                                                    } else {
                                                        FontWeight.Normal
                                                    }
                                                )
                                            },
                                            onClick = {
                                                topBarMenuExpanded = false
                                                if (index != currentColumnIndex) {
                                                    coroutineScope.launch {
                                                        lazyListState.animateScrollToItem(
                                                            realToVirtual(index)
                                                        )
                                                    }
                                                }
                                            },
                                            leadingIcon = if (showPrimaryMark) {
                                                {
                                                    Icon(
                                                        Icons.Default.Star,
                                                        contentDescription = s.primaryHub,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            } else {
                                                null
                                            },
                                            trailingIcon = if (isSelected) {
                                                {
                                                    Icon(Icons.Default.Check, contentDescription = null)
                                                }
                                            } else {
                                                null
                                            }
                                        )
                                    }
                                }
                            }
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
                                        hideBoardChrome()
                                    },
                                    leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, contentDescription = null) }
                                )
                            }
                    } // AlignedMoreMenuButton
                        } // Row
                    } // Surface
                    if (showTopBar) {
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
                    } // if showTopBar (search)
                    } // Column chrome
                    } // Box peek/clip
                } // if peekProgress
        },
        floatingActionButton = {
            if (draggedTask == null && columns.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(10.dp, CircleShape)
                        .clip(CircleShape)
                        .background(newProjectGradient)
                        .clickable {
                            val visible = lazyListState.layoutInfo.visibleItemsInfo
                            val maxVirtual = (hubPagerCount - 1).coerceAtLeast(0)
                            val virtual = if (visible.isEmpty()) {
                                realToVirtual(currentColumnIndex)
                            } else {
                                val viewportCenter =
                                    (lazyListState.layoutInfo.viewportStartOffset + lazyListState.layoutInfo.viewportEndOffset) / 2
                                visible.minByOrNull { item ->
                                    kotlin.math.abs((item.offset + item.size / 2) - viewportCenter)
                                }?.index ?: realToVirtual(currentColumnIndex)
                            }.coerceIn(0, maxVirtual)
                            val targetIndex = if (isAddHubVirtual(virtual)) {
                                currentColumnIndex
                            } else {
                                virtualToReal(virtual)
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
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
                    enter = expandVertically(animationSpec = tween(80)) + fadeIn(animationSpec = tween(70)),
                    exit = shrinkVertically(animationSpec = tween(70)) + fadeOut(animationSpec = tween(60))
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

                // Collapsible Column Switcher bar (chips toggled by "Хабы" in TopAppBar)
                AnimatedVisibility(
                    visible = showColumnHeaders && columns.isNotEmpty(),
                    enter = expandVertically(animationSpec = tween(80)) + fadeIn(animationSpec = tween(70)),
                    exit = shrinkVertically(animationSpec = tween(70)) + fadeOut(animationSpec = tween(60))
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
                                val showPrimaryMark = isPrimaryHub(col.id, index)

                                Surface(
                                    onClick = {
                                        coroutineScope.launch {
                                            lazyListState.animateScrollToItem(realToVirtual(index))
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
                                        if (showPrimaryMark) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = s.primaryHub,
                                                modifier = Modifier.size(14.dp),
                                                tint = if (isSelected) {
                                                    MaterialTheme.colorScheme.onPrimary
                                                } else {
                                                    MaterialTheme.colorScheme.primary
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                LazyRow(
                    state = lazyListState,
                    flingBehavior = if (draggedTask != null) ScrollableDefaults.flingBehavior() else flingBehavior,
                    userScrollEnabled = draggedTask == null,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(
                        count = hubPagerCount,
                        key = { virtual ->
                            if (isAddHubVirtual(virtual)) "add-hub-page"
                            else columns[virtualToReal(virtual)].id
                        },
                        contentType = { virtual ->
                            if (isAddHubVirtual(virtual)) 0 else 1
                        }
                    ) { virtual ->
                        if (isAddHubVirtual(virtual)) {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxWidth()
                                    .padding(horizontal = AlignedMoreEdgeGutter),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                OutlinedButton(
                                    onClick = { isAddingColumn = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                        .height(64.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.5.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
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
                            return@items
                        }

                        val index = virtualToReal(virtual)
                        val column = columns[index]
                        val columnTasks = tasksInHub(column, index)
                        val useLiteCards = draggedTask == null && virtual != hubSettledVirtual

                        Box(
                            modifier = Modifier
                                .fillParentMaxWidth()
                                .padding(horizontal = AlignedMoreEdgeGutter),
                            contentAlignment = Alignment.TopCenter
                        ) {
                        ColumnItem(
                            column = column,
                            columnIndex = index,
                            tasks = columnTasks,
                            categories = categories,
                            commentCountByTaskId = commentCountByTaskId,
                            currentBoardId = currentBoardId,
                            onOpenBoard = { targetBoardId, targetColumnId ->
                                openBoardAtHub(targetBoardId, targetColumnId)
                            },
                            activeInProgressTaskId = inProgressTaskId,
                            highlightedTaskId = highlightedTaskId,
                            onToggleInProgress = { task ->
                                val next = if (inProgressTaskId == task.id) null else task.id
                                inProgressTaskId = next
                                kairosPrefs.inProgressTaskId = next
                                KairosWidgetUpdater.updateAll(context, tasks)
                                if (next != null) {
                                    Toast.makeText(context, s.inProgressToast(task.title), Toast.LENGTH_SHORT).show()
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
                            onCommentColumnClick = { columnForComments = column },
                            columnCommentsCount = columnCommentCountById[column.id] ?: 0,
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
                            chromePullEnabled = draggedTask == null,
                            dropPreviewState = dropPreviewState,
                            observeDropPreview = !useLiteCards,
                            reportPositions = draggedTask != null && !useLiteCards,
                            useLiteCards = useLiteCards,
                            relatedBoardsByTaskId = relatedBoardsByTaskId,
                            onHubScrollState = {
                                hubScrollStates[column.id] = it
                            },
                            onHubListPositioned = { rect ->
                                hubListBounds[column.id] = rect
                            },
                            onToggleCompleted = { completeTaskWithUndo(it) },
                            immersiveChrome = !showTopBar && chromePullPx <= 0.5f,
                            onHubTitleDoubleTap = {
                                if (showTopBar) hideBoardChrome() else {
                                    showTopBar = true
                                    chromePullPx = 0f
                                }
                            },
                            boardChromeExpanded = showTopBar,
                            onChromePullDelta = { dy ->
                                if (!showTopBar) {
                                    val room = (boardChromeMaxPx * 1.15f - chromePullPx).coerceAtLeast(0f)
                                    val damped = dy * (
                                        0.55f + 0.45f * (room / (boardChromeMaxPx * 1.15f)).coerceIn(0f, 1f)
                                    )
                                    chromePullPx =
                                        (chromePullPx + damped).coerceIn(0f, boardChromeMaxPx * 1.15f)
                                }
                            },
                            onChromePullEnd = { settleChromePull() },
                            onChromeCollapseDelta = { dy ->
                                when {
                                    dy >= 0f -> 0f
                                    showTopBar -> {
                                        showTopBar = false
                                        boardSearchOpen = false
                                        showBoardsList = false
                                        showColumnHeaders = false
                                        chromePullPx =
                                            (boardChromeMaxPx + dy).coerceIn(0f, boardChromeMaxPx)
                                        dy
                                    }
                                    chromePullPx <= 0f -> 0f
                                    else -> {
                                        val next = (chromePullPx + dy).coerceAtLeast(0f)
                                        val consumed = next - chromePullPx
                                        chromePullPx = next
                                        consumed
                                    }
                                }
                            },
                            viewModel = viewModel
                        )
                        }
                    }
                }

                if (focusJump != null) {
                    val (targetBoardId, targetColumnId, scrollAndTitle) = focusJump
                    val (scrollIndex, focusTitle) = scrollAndTitle
                    // Overlay — must not change LazyRow height when hub selection settles (that caused swipe jitter).
                    Surface(
                        onClick = {
                            if (targetBoardId == currentBoardId && scrollIndex != null) {
                                coroutineScope.launch {
                                    lazyListState.animateScrollToItem(realToVirtual(scrollIndex))
                                }
                            } else {
                                openBoardAtHub(targetBoardId, targetColumnId)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .zIndex(2f),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        ),
                        shadowElevation = 4.dp
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
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        val next = !enterAddsTask
                                        enterAddsTask = next
                                        kairosPrefs.enterAddsTask = next
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = s.enterSavesTask,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )
                                Switch(
                                    checked = enterAddsTask,
                                    onCheckedChange = {
                                        enterAddsTask = it
                                        kairosPrefs.enterAddsTask = it
                                    }
                                )
                            }
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
                    ?: columns.getOrNull(virtualToReal(lazyListState.firstVisibleItemIndex))?.id.orEmpty()
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
                                lazyListState.animateScrollToItem(realToVirtual(targetIndex))
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
                                lazyListState.animateScrollToItem(realToVirtual(newIdx))
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
                    DragGhostCard(
                        title = task.title,
                        eisenhowerQuadrant = task.eisenhowerQuadrant,
                        isCompleted = task.isCompleted,
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
                            }
                    )
                }
            }

            AnimatedVisibility(
                visible = !showTopBar && chromePullPx <= 0.5f && hubGrabberVisible,
                enter = fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) +
                    expandVertically(animationSpec = tween(400, easing = FastOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(90)) +
                    shrinkVertically(animationSpec = tween(90)),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .clickable {
                            showTopBar = true
                            chromePullPx = 0f
                        }
                        .padding(top = 8.dp, bottom = 4.dp, start = 32.dp, end = 32.dp)
                        .semantics { contentDescription = s.pullDownForMenu },
                    contentAlignment = Alignment.Center
                ) {
                    // Sheet-style grabber (Telegram / Material bottom sheet).
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColumnItem(
    column: Column,
    columnIndex: Int = 0,
    tasks: List<Task>,
    categories: List<Category>,
    commentCountByTaskId: Map<String, Int> = emptyMap(),
    currentBoardId: String = "",
    onOpenBoard: (boardId: String, columnId: String) -> Unit = { _, _ -> },
    activeInProgressTaskId: String? = null,
    highlightedTaskId: String? = null,
    onToggleInProgress: (Task) -> Unit = {},
    onColumnPositioned: (androidx.compose.ui.geometry.Rect) -> Unit,
    onTaskPositioned: (Task, androidx.compose.ui.geometry.Rect) -> Unit,
    onDragStart: (Task, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDeleteColumnClick: () -> Unit,
    onRenameColumnClick: () -> Unit,
    onEditTaskClick: (Task) -> Unit,
    onDeleteTaskClick: (String) -> Unit,
    onCommentClick: (Task) -> Unit,
    onMoveTaskClick: (Task) -> Unit,
    columnCommentsCount: Int = 0,
    onCommentColumnClick: () -> Unit = {},
    draggedTaskId: String? = null,
    /** False while a task is being dragged — chrome pull must not steal the gesture. */
    chromePullEnabled: Boolean = true,
    dropPreviewState: State<TaskDropPreview?>,
    observeDropPreview: Boolean = true,
    reportPositions: Boolean = false,
    useLiteCards: Boolean = false,
    relatedBoardsByTaskId: Map<String, List<RelatedBoardLink>> = emptyMap(),
    onHubScrollState: (androidx.compose.foundation.lazy.LazyListState) -> Unit = {},
    onHubListPositioned: (androidx.compose.ui.geometry.Rect) -> Unit = {},
    onToggleCompleted: (Task) -> Unit = {},
    /** Board chrome hidden — use a shorter hub title strip. */
    immersiveChrome: Boolean = false,
    onHubTitleDoubleTap: () -> Unit = {},
    boardChromeExpanded: Boolean = true,
    onChromePullDelta: (Float) -> Unit = {},
    onChromePullEnd: () -> Unit = {},
    /** Collapse expanded/peeking chrome; return consumed vertical px (negative dy). */
    onChromeCollapseDelta: (Float) -> Float = { 0f },
    viewModel: SharedViewModel
) {
    val context = LocalContext.current
    val s = LocalAppStrings.current
    val isDropTarget by remember(column.id, observeDropPreview) {
        derivedStateOf {
            observeDropPreview && dropPreviewState.value?.columnId == column.id
        }
    }
    val dropInsertIndex by remember(column.id, observeDropPreview) {
        derivedStateOf {
            if (!observeDropPreview) null
            else dropPreviewState.value?.takeIf { it.columnId == column.id }?.insertIndex
        }
    }
    val insertIndex = dropInsertIndex
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
    // Telegram-archive pull: vertical-only nested scroll on the task list — never touches hub LazyRow.
    // Disabled while dragging a task so the board chrome does not collapse under the finger.
    val chromePullConnection = remember(
        hubListState,
        useLiteCards,
        chromePullEnabled,
        boardChromeExpanded,
        onChromePullDelta,
        onChromePullEnd,
        onChromeCollapseDelta
    ) {
        object : NestedScrollConnection {
            private fun atListTop(): Boolean =
                hubListState.firstVisibleItemIndex == 0 &&
                    hubListState.firstVisibleItemScrollOffset == 0

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!chromePullEnabled || useLiteCards || available.y >= 0f) return Offset.Zero
                // Finger moving up / content scrolling down → collapse chrome first (vertical only).
                if (boardChromeExpanded) {
                    if (!atListTop()) return Offset.Zero
                    val consumedY = onChromeCollapseDelta(available.y)
                    return if (consumedY != 0f) Offset(0f, consumedY) else Offset.Zero
                }
                val consumedY = onChromeCollapseDelta(available.y)
                return if (consumedY != 0f) Offset(0f, consumedY) else Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!chromePullEnabled || useLiteCards || available.y <= 0f || !atListTop()) {
                    return Offset.Zero
                }
                // Overscroll at top while chrome hidden → peek like Telegram Archive.
                if (!boardChromeExpanded) {
                    onChromePullDelta(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (chromePullEnabled && !useLiteCards && !boardChromeExpanded) {
                    onChromePullEnd()
                }
                // Do not consume fling velocity (esp. horizontal hub paging).
                return Velocity.Zero
            }
        }
    }
    LaunchedEffect(hubListState.isScrollInProgress, useLiteCards, boardChromeExpanded, chromePullEnabled) {
        if (
            chromePullEnabled &&
            !hubListState.isScrollInProgress &&
            !useLiteCards &&
            !boardChromeExpanded
        ) {
            onChromePullEnd()
        }
    }
    var hubMenuExpanded by remember { mutableStateOf(false) }
    // Non-Compose counter: bump while lite without recomposing the hub list mid-swipe.
    val actionsRevealEpoch = remember { java.util.concurrent.atomic.AtomicInteger(0) }
    LaunchedEffect(useLiteCards) {
        if (useLiteCards) {
            actionsRevealEpoch.incrementAndGet()
        }
    }

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
            .fillMaxWidth()
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
            .then(
                if (reportPositions) {
                    Modifier.onGloballyPositioned { coordinates ->
                        onColumnPositioned(
                            androidx.compose.ui.geometry.Rect(
                                offset = coordinates.positionInRoot(),
                                size = androidx.compose.ui.geometry.Size(
                                    coordinates.size.width.toFloat(),
                                    coordinates.size.height.toFloat()
                                )
                            )
                        )
                    }
                } else {
                    Modifier
                }
            )
            .padding(
                horizontal = 0.dp,
                vertical = if (immersiveChrome) 2.dp else 4.dp
            )
    ) {
        // Hub header: menu for actions (no pull gestures — keeps hub paging smooth)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(HubTitleStripHeight)
                .padding(bottom = if (immersiveChrome) 0.dp else 2.dp)
                // end gutter matches TopAppBar actions end padding (4.dp) so ⋮ stacks vertically
                .padding(start = 8.dp, end = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(column.id) {
                        detectTapGestures(onDoubleTap = { onHubTitleDoubleTap() })
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = 20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(hubColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = s.localized(column.title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        lineHeight = 26.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
                    if (hiddenCount > 0) {
                Box(
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
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
            AlignedMoreMenuButton(
                expanded = hubMenuExpanded,
                onExpandedChange = { hubMenuExpanded = it }
            ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (columnCommentsCount > 0) s.commentsCount(columnCommentsCount) else s.hubRules
                            )
                        },
                        onClick = {
                            hubMenuExpanded = false
                            onCommentColumnClick()
                        },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = null)
                        }
                    )
                    if (hiddenCount > 0) {
                        DropdownMenuItem(
                            text = {
                                Text(if (showHiddenTasks) s.hideHiddenTasks else s.showHiddenTasks)
                            },
                            onClick = {
                                hubMenuExpanded = false
                                showHiddenTasks = !showHiddenTasks
                            },
                            leadingIcon = {
                                Icon(
                                    if (showHiddenTasks) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(s.renameHub) },
                        onClick = {
                            hubMenuExpanded = false
                            onRenameColumnClick()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(s.deleteHub) },
                        onClick = {
                            hubMenuExpanded = false
                            onDeleteColumnClick()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                    )
            }
        }
        LazyColumn(
            state = hubListState,
            userScrollEnabled = !useLiteCards && draggedTaskId == null,
            modifier = Modifier
                .fillMaxHeight()
                .nestedScroll(chromePullConnection)
                .then(
                    if (reportPositions) {
                        Modifier.onGloballyPositioned { coordinates ->
                            onHubListPositioned(
                                androidx.compose.ui.geometry.Rect(
                                    offset = coordinates.positionInRoot(),
                                    size = androidx.compose.ui.geometry.Size(
                                        coordinates.size.width.toFloat(),
                                        coordinates.size.height.toFloat()
                                    )
                                )
                            )
                        }
                    } else {
                        Modifier
                    }
                ),
            contentPadding = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Same item count for lite and full — peek limit caused a vertical jump on settle.
            val listTasks = visibleActiveTasks
            if (visibleActiveTasks.isEmpty() && completedTasks.isEmpty() && insertIndex == null) {
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
                itemsIndexed(listTasks, key = { _, task -> task.id }) { index, task ->
                    if (useLiteCards) {
                        val category = categories.find { it.id == task.categoryId }
                        LiteTaskRow(
                            title = task.title,
                            eisenhowerQuadrant = task.eisenhowerQuadrant.takeIf { !task.isCompleted },
                            categoryColor = category
                                ?.takeUnless { KanbanNames.isUncategorized(it.name) }
                                ?.let { Color(it.color) },
                            isOverdue = !task.isCompleted && task.dueDate != null && task.dueDate.isOverdueDate(),
                            isInProgress = activeInProgressTaskId == task.id,
                            isCompleted = false,
                            hasDueDate = task.dueDate != null,
                            hasRelated = relatedBoardsByTaskId[task.id].orEmpty().isNotEmpty()
                        )
                    } else {
                    val taskCommentsCount = commentCountByTaskId[task.id] ?: 0
                    val relatedBoards = relatedBoardsByTaskId[task.id].orEmpty()
                    val filteredIndex = if (draggedTaskId == null) index
                    else visibleActiveTasks.take(index).count { it.id != draggedTaskId }

                    androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth()) {
                        if (insertIndex != null && task.id != draggedTaskId && insertIndex == filteredIndex) {
                            DropInsertSlot()
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
                            onCommentClick = { onCommentClick(task) },
                            onMoveClick = { onMoveTaskClick(task) },
                            reportPosition = reportPositions,
                            onPositioned = { rect -> onTaskPositioned(task, rect) },
                            onDragStart = { offset -> onDragStart(task, offset) },
                            onDrag = onDrag,
                            onDragEnd = onDragEnd,
                            onEditClick = { onEditTaskClick(task) },
                            onDeleteClick = { onDeleteTaskClick(task.id) },
                            onToggleCompleted = { onToggleCompleted(task) },
                            actionsRevealToken = actionsRevealEpoch.get()
                        )
                    }
                    }
                }
                if (!useLiteCards && insertIndex != null && insertIndex >= visibleActiveTasks.count { it.id != draggedTaskId }) {
                    item(key = "drop-slot-end") {
                        DropInsertSlot()
                    }
                }

                if (completedTasks.isNotEmpty()) {
                    if (useLiteCards) {
                        item(key = "completed-lite") {
                            Text(
                                text = s.completedCount(completedTasks.size),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    } else {
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
                            val relatedBoards = relatedBoardsByTaskId[task.id].orEmpty()

                            TaskCard(
                                task = task,
                                categories = categories,
                                viewModel = viewModel,
                                commentsCount = taskCommentsCount,
                                isInProgress = false,
                                isAnyTaskInProgress = activeInProgressTaskId != null,
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
                                onCommentClick = { onCommentClick(task) },
                                onMoveClick = { onMoveTaskClick(task) },
                                reportPosition = reportPositions,
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
    isDragPreview: Boolean = false,
    relatedBoards: List<RelatedBoardLink> = emptyList(),
    onOpenRelatedBoard: (boardId: String, columnId: String) -> Unit = { _, _ -> },
    onRemoveFromBoard: (() -> Unit)? = null,
    onSetQuadrant: ((String?) -> Unit)? = null,
    onToggleInProgress: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    onMoveClick: () -> Unit = {},
    reportPosition: Boolean = false,
    onPositioned: (androidx.compose.ui.geometry.Rect) -> Unit = {},
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleCompleted: (() -> Unit)? = null,
    /** When > 0 after lite→full settle, actions slide in from the right inside a reserved slot. */
    actionsRevealToken: Int = 0
) {
    val context = LocalContext.current
    val s = LocalAppStrings.current
    val dateLocale = LocalAppLanguage.current.locale
    val category = remember(categories, task.categoryId) {
        categories.find { it.id == task.categoryId }
    }

    val toggleCompleted = {
        onToggleCompleted?.invoke() ?: viewModel.toggleTaskCompletion(task)
    }

    val targetAlpha = when {
        isDragging -> 0.18f
        isAnyTaskInProgress && !isInProgress -> 0.32f
        task.isHidden -> 0.55f
        else -> 1f
    }

    var moreMenuExpanded by remember(task.id) { mutableStateOf(false) }
    // token > 0: start fully off-screen (clipped), then slide R→L. token == 0: already settled, show.
    val actionsReveal = remember(task.id, actionsRevealToken) {
        Animatable(if (actionsRevealToken > 0) 0f else 1f)
    }
    val density = LocalDensity.current
    val actionsSlidePx = with(density) { TaskActionsClusterWidth.toPx() }
    LaunchedEffect(task.id, actionsRevealToken) {
        if (actionsRevealToken > 0) {
            actionsReveal.snapTo(0f)
            actionsReveal.animateTo(
                1f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        } else {
            actionsReveal.snapTo(1f)
        }
    }
    val layoutCoordsRef = remember {
        object {
            var coords: androidx.compose.ui.layout.LayoutCoordinates? = null
        }
    }
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

    val compactEisenhower = !task.isCompleted && task.eisenhowerQuadrant != null
    val compactOnColor = if (compactEisenhower) {
        eisenhowerOnColor(task.eisenhowerQuadrant)
    } else if (task.isCompleted) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val compactOverdue = !task.isCompleted && task.dueDate != null && task.dueDate.isOverdueDate()

    // Title + reserved trailing slot (actions + ⋮). Settle: load in place, then slide R→L inside slot.
    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = targetAlpha }
            .onGloballyPositioned { coordinates ->
                layoutCoordsRef.coords = coordinates
                if (reportPosition) {
                    onPositioned(
                        androidx.compose.ui.geometry.Rect(
                            offset = coordinates.positionInRoot(),
                            size = androidx.compose.ui.geometry.Size(
                                coordinates.size.width.toFloat(),
                                coordinates.size.height.toFloat()
                            )
                        )
                    )
                }
            }
            .then(
                if (isDragPreview || isDragging) Modifier else Modifier
                    .pointerInput(task.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { localOffset ->
                                moreMenuExpanded = false
                                onDragStart(
                                    (layoutCoordsRef.coords?.positionInRoot() ?: Offset.Zero) + localOffset
                                )
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount)
                            },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = {}
                        )
                    }
            )
            .then(
                if (compactEisenhower) {
                    Modifier
                        .clip(TaskBubbleShape)
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
        shape = TaskBubbleShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (compactEisenhower) Modifier.background(Color.Black.copy(alpha = 0.22f))
                    else Modifier
                )
                .padding(
                    start = 12.dp,
                    end = 0.dp,
                    top = TaskCardRowVerticalPad,
                    bottom = TaskCardRowVerticalPad
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TaskCardRowContentMinHeight),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TaskRowTrailingGap)
            ) {
                var titleExpanded by remember(task.id) { mutableStateOf(false) }
                var titleOverflows by remember(task.id, task.title) { mutableStateOf(false) }
                if (compactOverdue) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(40.dp)
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
                                    if (titleOverflows || titleExpanded) {
                                        titleExpanded = !titleExpanded
                                    }
                                },
                                onDoubleTap = { onMoveClick() }
                            )
                        }
                )
                if (!isDragPreview) {
                    Box(
                        modifier = Modifier
                            .width(TaskActionsClusterWidth)
                            .height(TaskActionButtonSize)
                            .clipToBounds(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(TaskActionsClusterGap),
                            modifier = Modifier.graphicsLayer {
                                // Opaque but clipped while off to the right — no fade “pop”.
                                translationX = (1f - actionsReveal.value) * actionsSlidePx
                            }
                        ) {
                            if (!task.isCompleted) {
                                Box(
                                    modifier = Modifier
                                        .size(TaskActionButtonSize)
                                        .clip(RoundedCornerShape(14.dp))
                                        .then(
                                            if (isInProgress) Modifier.background(inProgressGradient)
                                            else if (compactEisenhower) Modifier
                                                .background(Color.White.copy(alpha = 0.22f))
                                                .border(1.2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                                            else Modifier
                                                .background(MaterialTheme.colorScheme.surface)
                                                .border(1.2.dp, inProgressColor.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                                        )
                                        .clickable { onToggleInProgress() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = s.inProgress,
                                        modifier = Modifier.size(TaskActionIconSize),
                                        tint = if (isInProgress || compactEisenhower) Color.White else inProgressColor
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(TaskActionButtonSize)
                                    .clip(RoundedCornerShape(14.dp))
                                    .then(
                                        if (task.isCompleted) Modifier.background(Color(0xFF00C853))
                                        else if (compactEisenhower) Modifier
                                            .background(Color.White.copy(alpha = 0.22f))
                                            .border(1.5.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                                        else Modifier
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(1.5.dp, Color(0xFF00C853).copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                                    )
                                    .clickable { toggleCompleted() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                                    contentDescription = s.completed,
                                    modifier = Modifier.size(TaskActionIconSize),
                                    tint = when {
                                        task.isCompleted -> Color.White
                                        compactEisenhower -> Color.White
                                        else -> Color(0xFF00C853)
                                    }
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .width(AlignedMoreButtonSize)
                            .height(TaskActionButtonSize),
                        contentAlignment = Alignment.Center
                    ) {
                        AlignedMoreMenuButton(
                            expanded = moreMenuExpanded,
                            onExpandedChange = { moreMenuExpanded = it },
                            tint = if (compactEisenhower) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (commentsCount > 0) s.commentsCount(commentsCount) else s.comments,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                onClick = {
                                    moreMenuExpanded = false
                                    onCommentClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(s.edit, style = MaterialTheme.typography.bodyLarge) },
                                onClick = {
                                    moreMenuExpanded = false
                                    onEditClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (task.isHidden) s.showTask else s.hideTask,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                onClick = {
                                    moreMenuExpanded = false
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
                                        moreMenuExpanded = false
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
                                            moreMenuExpanded = false
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
                                        moreMenuExpanded = false
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
                                    moreMenuExpanded = false
                                    onMoveClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(s.delete, style = MaterialTheme.typography.bodyLarge) },
                                onClick = {
                                    moreMenuExpanded = false
                                    onDeleteClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            }
            // Bottom strip under ⋮: related next to due (end). If no due, related takes that spot.
            if (!isDragPreview && (task.dueDate != null || relatedBoards.isNotEmpty())) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = TaskBottomMetaHeight)
                        .padding(top = 2.dp, end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    if (relatedBoards.isNotEmpty()) {
                        RelatedBoardsButton(
                            links = relatedBoards,
                            onOpen = { link -> onOpenRelatedBoard(link.board.id, link.columnId) },
                            iconTint = if (compactEisenhower) {
                                Color.White.copy(alpha = 0.92f)
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            background = if (compactEisenhower) {
                                Color.White.copy(alpha = 0.22f)
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f)
                            },
                            size = TaskRelatedCornerSize
                        )
                    }
                    if (task.dueDate != null) {
                        Text(
                            text = task.dueDate.relativeDueLabel(s, dateLocale, task.isCompleted),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            color = when {
                                compactOverdue -> if (compactEisenhower) Color.White else MaterialTheme.colorScheme.error
                                compactEisenhower -> Color.White.copy(alpha = 0.9f)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .widthIn(min = AlignedMoreButtonSize, max = TaskDueLabelMaxWidth)
                                .clickable { showDueEditor = true }
                        )
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
    val prevColumn = when {
        columns.size < 2 || currentColumnIndex !in columns.indices -> null
        currentColumnIndex > 0 -> columns[currentColumnIndex - 1]
        else -> columns.last()
    }
    val nextColumn = when {
        columns.size < 2 || currentColumnIndex !in columns.indices -> null
        currentColumnIndex < columns.size - 1 -> columns[currentColumnIndex + 1]
        else -> columns.first()
    }

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

/**
 * Shared ⋮ control for board / hub / task.
 * Button slot is always [AlignedMoreButtonSize]; popup is end-aligned to that slot so every
 * open menu shares the same trailing X (stacked under each other).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlignedMoreMenuButton(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    /** If set, short tap runs this instead of opening the menu. */
    onClick: (() -> Unit)? = null,
    /** Optional long-press (e.g. open menu while short tap expands a cluster). */
    onLongClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val s = LocalAppStrings.current
    val density = LocalDensity.current
    val positionProvider = remember(density) {
        EndAlignedMenuPositionProvider(density)
    }
    Box(modifier = Modifier.size(AlignedMoreButtonSize)) {
        val openMenu = { onExpandedChange(true) }
        Box(
            modifier = Modifier
                .size(AlignedMoreButtonSize)
                .then(
                    if (onLongClick != null) {
                        Modifier.combinedClickable(
                            onClick = { onClick?.invoke() ?: openMenu() },
                            onLongClick = onLongClick
                        )
                    } else {
                        Modifier.clickable { onClick?.invoke() ?: openMenu() }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = s.moreActions,
                tint = tint,
                modifier = Modifier.size(AlignedMoreIconSize)
            )
        }
        if (expanded) {
            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = { onExpandedChange(false) },
                properties = PopupProperties(focusable = true)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    tonalElevation = 3.dp,
                    shadowElevation = 3.dp,
                    modifier = Modifier.width(AlignedMoreMenuWidth)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        content()
                    }
                }
            }
        }
    }
}

/** Pins menu's right edge to the ⋮ button's right edge. */
private class EndAlignedMenuPositionProvider(
    private val density: androidx.compose.ui.unit.Density
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val x = anchorBounds.right - popupContentSize.width
        val y = anchorBounds.bottom + with(density) { AlignedMoreMenuGap.toPx() }.roundToInt()
        return IntOffset(x, y)
    }
}

/** Compact board title row (was Material TopAppBar ~64.dp). */
private val BoardTopBarContentHeight = 48.dp
private val BoardTopBarActionSize = 40.dp
/** Hub title row — fits Manrope 22sp + ⋮ without growing the chrome tall. */
private val HubTitleStripHeight = 44.dp
/** Matches Material3 TopAppBar actions end padding and hub page side inset. */
private val AlignedMoreEdgeGutter = 4.dp
private val AlignedMoreButtonSize = 40.dp
private val AlignedMoreIconSize = 22.dp
private val AlignedMoreMenuWidth = 280.dp
private val AlignedMoreMenuGap = 0.dp
/** Task card quick-actions (bolt / check) — Material min touch target. */
private val TaskActionButtonSize = 48.dp
private val TaskActionIconSize = 26.dp
private val TaskActionsClusterGap = 4.dp
/** Always reserved for bolt+check so title width matches lite peek and settle fade. */
private val TaskActionsClusterWidth = TaskActionButtonSize * 2 + TaskActionsClusterGap
/** Outgoing chat-bubble frame for tasks (tail on the bottom-end). */
private val TaskBubbleShape = RoundedCornerShape(
    topStart = 20.dp,
    topEnd = 20.dp,
    bottomStart = 20.dp,
    bottomEnd = 3.dp
)
/** Gap between title / actions / ⋮ in TaskCard Row (Arrangement.spacedBy). */
private val TaskRowTrailingGap = 6.dp
/** Due / related bottom meta strip height (fits link badge + labelSmall). */
private val TaskDueUnderMoreHeight = 16.dp
private val TaskBottomMetaHeight = 22.dp
/** Equal top/bottom pad around the button row. */
private val TaskCardRowVerticalPad = 6.dp
/** Content row height = action buttons exactly. */
private val TaskCardRowContentMinHeight = TaskActionButtonSize
/** Due label may be wider than ⋮ so text is not ellipsized away. */
private val TaskDueLabelMaxWidth = 72.dp
/** Related-board control in the bottom meta strip (does not shift title). */
private val TaskRelatedCornerSize = 22.dp

/** Empty trailing slots matching TaskCard actions + ⋮ (same Row spacedBy). */
@Composable
private fun TaskTrailingWidthReserve() {
    Box(
        modifier = Modifier
            .width(TaskActionsClusterWidth)
            .height(TaskActionButtonSize)
    )
    Box(
        modifier = Modifier
            .width(AlignedMoreButtonSize)
            .height(TaskActionButtonSize)
    )
}

@Composable
private fun DropInsertSlot() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
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

/** Peek stand-in for TaskCard — same trailing width and equal vertical pad as settled chrome. */
@Composable
private fun LiteTaskRow(
    title: String,
    eisenhowerQuadrant: String? = null,
    categoryColor: Color? = null,
    isOverdue: Boolean = false,
    isInProgress: Boolean = false,
    isCompleted: Boolean = false,
    hasDueDate: Boolean = false,
    hasRelated: Boolean = false
) {
    val painted = !isCompleted && eisenhowerQuadrant != null
    val onColor = when {
        painted -> eisenhowerOnColor(eisenhowerQuadrant)
        isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderColor = when {
        isInProgress -> inProgressColor
        painted -> Color.White.copy(alpha = 0.28f)
        categoryColor != null -> categoryColor.copy(alpha = 0.7f)
        isCompleted -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TaskBubbleShape)
            .then(
                if (painted) Modifier.background(eisenhowerGradient(eisenhowerQuadrant))
                else Modifier.background(
                    if (isCompleted) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.surface
                )
            )
            .border(
                width = if (isInProgress) 2.dp else 1.2.dp,
                color = borderColor,
                shape = TaskBubbleShape
            )
            .then(
                if (painted) Modifier.background(Color.Black.copy(alpha = 0.18f))
                else Modifier
            )
            .padding(
                start = 12.dp,
                end = 0.dp,
                top = TaskCardRowVerticalPad,
                bottom = TaskCardRowVerticalPad
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TaskCardRowContentMinHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TaskRowTrailingGap)
        ) {
            if (isOverdue) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.error)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.15.sp,
                    lineHeight = 22.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = onColor.copy(alpha = if (isCompleted) 0.55f else 1f),
                textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f)
            )
            // Same two slots as TaskCard (actions cluster + ⋮) so title width never jumps.
            TaskTrailingWidthReserve()
        }
        if (hasDueDate || hasRelated) {
            Spacer(modifier = Modifier.height(TaskBottomMetaHeight + 2.dp))
        }
    }
}

/** Drag preview without TaskCard swipe/menus — keeps the 60fps ghost cheap. */
@Composable
private fun DragGhostCard(
    title: String,
    eisenhowerQuadrant: String?,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    val painted = !isCompleted && eisenhowerQuadrant != null
    Card(
        modifier = modifier.then(
            if (painted) {
                Modifier
                    .clip(TaskBubbleShape)
                    .background(eisenhowerGradient(eisenhowerQuadrant))
            } else Modifier
        ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                painted -> Color.Transparent
                isCompleted -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            if (painted) Color.White.copy(alpha = 0.28f)
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        ),
        shape = TaskBubbleShape
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = if (painted) eisenhowerOnColor(eisenhowerQuadrant) else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (painted) Modifier.background(Color.Black.copy(alpha = 0.22f)) else Modifier)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
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
