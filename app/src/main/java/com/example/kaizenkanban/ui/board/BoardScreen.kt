package com.example.kaizenkanban.ui.board

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInFull
import com.example.kaizenkanban.domain.model.Eisenhower
import com.example.kaizenkanban.domain.model.HubGroupOrdering
import com.example.kaizenkanban.domain.model.TaskLinkGraph
import com.example.kaizenkanban.domain.model.TaskDueSort
import com.example.kaizenkanban.domain.model.TaskWorkflow
import com.example.kaizenkanban.ui.taskmeta.CompletionQualityDialog
import com.example.kaizenkanban.ui.taskmeta.TaskComplexityPicker
import com.example.kaizenkanban.domain.model.RemindLeadCalculator
import com.example.kaizenkanban.domain.model.RemindLeadUnit
import com.example.kaizenkanban.domain.reminders.TaskTimerPlanner
import com.example.kaizenkanban.ui.reminders.RemindBeforeEditor
import com.example.kaizenkanban.ui.reminders.leadReminderPreview
import com.example.kaizenkanban.ui.chrome.BoardChromeHideGain
import com.example.kaizenkanban.ui.chrome.ChromePullSettleSpring
import com.example.kaizenkanban.ui.chrome.boardChromeOpenPullGain
import com.example.kaizenkanban.ui.chrome.chromeOpenPullPx
import com.example.kaizenkanban.ui.chrome.chromePullLocksOpen
import com.example.kaizenkanban.ui.chrome.collapseChromePull
import com.example.kaizenkanban.ui.components.DialogDisclosureRow
import com.example.kaizenkanban.ui.components.DialogMarqueeText
import com.example.kaizenkanban.ui.components.DialogPanel
import com.example.kaizenkanban.ui.components.DialogSectionDivider
import com.example.kaizenkanban.ui.components.DialogSheetTonalElevation
import com.example.kaizenkanban.ui.components.TaskDialogBlock
import com.example.kaizenkanban.ui.components.dialogSheetFill
import com.example.kaizenkanban.ui.taskmeta.TaskDeadlinePlate
import com.example.kaizenkanban.ui.taskmeta.TaskGoalToggle
import com.example.kaizenkanban.ui.taskmeta.TaskTimerPlate
import com.example.kaizenkanban.ui.taskmeta.TaskWhenPlate
import com.example.kaizenkanban.ui.taskmeta.TaskWindowKind
import com.example.kaizenkanban.ui.taskmeta.formatDeadlineClosedSummary
import com.example.kaizenkanban.ui.taskmeta.formatTimerClosedSummary
import com.example.kaizenkanban.ui.taskmeta.formatWhenClosedSummary
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.coroutines.cancellation.CancellationException
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.kaizenkanban.data.local.KairosPreferences
import com.example.kaizenkanban.pro.Entitlements
import com.example.kaizenkanban.pro.ProFeature
import com.example.kaizenkanban.widget.KairosWidgetUpdater
import com.example.kaizenkanban.reminders.DueReminderScheduler
import com.example.kaizenkanban.reminders.TaskTimerScheduler
import com.example.kaizenkanban.ui.calendar.DueDateQuickPick
import com.example.kaizenkanban.ui.calendar.formatDateShort
import com.example.kaizenkanban.ui.calendar.isDueToday
import com.example.kaizenkanban.ui.calendar.isOverdueDate
import com.example.kaizenkanban.ui.calendar.isPastDeadline
import com.example.kaizenkanban.ui.calendar.isSameLocalDay
import com.example.kaizenkanban.ui.calendar.localMillisToUtcPicker
import com.example.kaizenkanban.ui.calendar.localNoonForDayOffset
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
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.kaizenkanban.domain.model.Board
import com.example.kaizenkanban.domain.model.Column
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.domain.model.TaskAttachment
import com.example.kaizenkanban.domain.model.TaskCardBody
import com.example.kaizenkanban.domain.model.TaskCardKind
import com.example.kaizenkanban.domain.model.Comment
import com.example.kaizenkanban.domain.model.ColumnComment
import com.example.kaizenkanban.domain.model.Project
import com.example.kaizenkanban.ui.theme.eisenhowerGradient
import com.example.kaizenkanban.ui.theme.eisenhowerOnColor
import com.example.kaizenkanban.ui.theme.eisenhowerPlain
import com.example.kaizenkanban.ui.theme.eisenhowerSolid
import com.example.kaizenkanban.ui.theme.LiterataBold
import com.example.kaizenkanban.ui.theme.buttonFill
import com.example.kaizenkanban.ui.theme.buttonLabel
import com.example.kaizenkanban.ui.theme.buttonLine
import com.example.kaizenkanban.ui.theme.completedMark
import com.example.kaizenkanban.ui.theme.deleteButtonColor
import com.example.kaizenkanban.ui.theme.inProgressColor
import com.example.kaizenkanban.ui.theme.inProgressBrush
import com.example.kaizenkanban.ui.theme.inProgressMark
import com.example.kaizenkanban.ui.theme.AppThemeMode
import com.example.kaizenkanban.ui.theme.LocalAppThemeMode
import com.example.kaizenkanban.ui.theme.inkNight
import com.example.kaizenkanban.ui.theme.inkNightLine
import com.example.kaizenkanban.ui.theme.inkNightRaised
import com.example.kaizenkanban.ui.theme.inkSlate
import com.example.kaizenkanban.ui.theme.inkSlateCard
import com.example.kaizenkanban.ui.theme.inkSlateLine
import com.example.kaizenkanban.ui.taskmeta.TaskCriteriaDialog
import com.example.kaizenkanban.ui.voice.rememberVoiceAssistantController
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

/** Tasks that occupy the hub's active list — the same set the drop slot is drawn against. */
private fun Task.isActiveDropTarget(): Boolean =
    !isBoardArchived && !isCompleted && !isNotDone && !isHidden

data class RelatedBoardLink(
    val board: Board,
    val columnId: String,
    val hubTitle: String
)

data class TaskLinkNeighbor(
    val id: String,
    val title: String,
    val isParent: Boolean
)

/** Quiet paper wash while a task's links are in focus. */
private data class LinkFocusChrome(
    val board: Color,
    val banner: Color,
    val bannerContent: Color,
    val hub: Color,
    val hubBorder: Color,
)

@Composable
private fun linkFocusChrome(): LinkFocusChrome {
    if (LocalAppThemeMode.current == AppThemeMode.SLATE) {
        return LinkFocusChrome(
            board = inkSlate,
            banner = Color(0xFF800020),
            bannerContent = Color(0xFFFAFAFA),
            hub = inkSlateCard,
            hubBorder = inkSlateLine,
        )
    }
    return if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        LinkFocusChrome(
            board = inkNight,
            banner = Color(0xFF800020),
            bannerContent = Color(0xFFFAFAFA),
            hub = inkNightRaised,
            hubBorder = inkNightLine,
        )
    } else {
        LinkFocusChrome(
            board = Color(0xFFF5F5F5),
            banner = Color(0xFF800020),
            bannerContent = Color(0xFFFAFAFA),
            hub = Color(0xFFFFFFFF),
            hubBorder = Color(0xFFE5E5E5),
        )
    }
}

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
        .toSet()
    return boards
        .filter {
            it.id in boardIds &&
                it.id != currentBoardId &&
                !KanbanNames.isEisenhowerBoard(it.name)
        }
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

private fun isSystemHubRule(text: String): Boolean =
    text.startsWith("Правила для") || text.startsWith("Rules for")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BoardScreen(
    boardId: String,
    viewModel: SharedViewModel,
    windowSizeClass: WindowWidthSizeClass,
    onBack: () -> Unit,
    onOpenEisenhowerMatrix: (projectId: String) -> Unit = {},
    initialColumnId: String? = null,
    initialTaskId: String? = null,
    initialOpenAddTask: Boolean = false,
    initialDueMillis: Long? = null,
    onOpenTypedTask: (taskId: String) -> Unit = {}
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
    var titlePreviewTaskId by remember { mutableStateOf<String?>(null) }
    var linkFocusIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var linkFocusAnchorId by remember { mutableStateOf<String?>(null) }
    /** When set, the board shows only this group's tasks — no hubs or extra chrome. */
    var focusedGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    var linksDialogTask by remember { mutableStateOf<Task?>(null) }
    /** When set, board shows only goals + their steps; tap assigns parent to this task. */
    var linkPickParentFor by remember { mutableStateOf<Task?>(null) }
    /** Candidate parent selected in pick mode; link applies only after confirm. */
    var linkPickPendingParentId by remember { mutableStateOf<String?>(null) }
    var photosDialogTask by remember { mutableStateOf<Task?>(null) }
    var pendingCameraTaskId by remember { mutableStateOf<String?>(null) }
    var pendingCameraOutputUri by remember { mutableStateOf<Uri?>(null) }
    LaunchedEffect(initialTaskId) {
        val id = initialTaskId?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        highlightedTaskId = id
        delay(2800)
        if (highlightedTaskId == id) highlightedTaskId = null
    }
    var boardSearchOpen by rememberSaveable { mutableStateOf(false) }
    var boardSearchQuery by rememberSaveable { mutableStateOf("") }
    var overdueOnlyHubIds by rememberSaveable { mutableStateOf(listOf<String>()) }
    val searchFocusRequester = remember { FocusRequester() }
    var boardNavStack by remember(boardId) { mutableStateOf(listOf(boardId)) }
    val board = state.boards.find { it.id == activeBoardId } ?: state.boards.find { it.id == boardId } ?: state.boards.firstOrNull()
    val currentBoardId = board?.id ?: activeBoardId

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val taskId = pendingCameraTaskId
        val uri = pendingCameraOutputUri
        pendingCameraTaskId = null
        pendingCameraOutputUri = null
        if (success && taskId != null && uri != null) {
            val projectId = board?.projectId.orEmpty()
            viewModel.addTaskPhoto(taskId, projectId, uri) { err ->
                when (err) {
                    "limit" -> Toast.makeText(context, s.taskPhotosLimit, Toast.LENGTH_SHORT).show()
                    "error" -> Toast.makeText(context, s.voiceError, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun launchCameraCapture() {
        try {
            val photoFile = File(context.cacheDir, "task_photo_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            pendingCameraOutputUri = uri
            takePictureLauncher.launch(uri)
        } catch (_: Exception) {
            pendingCameraTaskId = null
            pendingCameraOutputUri = null
            Toast.makeText(context, s.voiceError, Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCameraCapture()
        } else {
            pendingCameraTaskId = null
            Toast.makeText(context, s.cameraPermissionNeeded, Toast.LENGTH_SHORT).show()
        }
    }

    fun requestTakePhoto(task: Task) {
        if (!Entitlements.has(kairosPrefs, ProFeature.TaskPhotos)) {
            Toast.makeText(context, s.proFeatureLocked, Toast.LENGTH_SHORT).show()
            return
        }
        val count = state.taskAttachments.count { it.taskId == task.id }
        if (count >= TaskAttachment.MAX_PER_TASK) {
            Toast.makeText(context, s.taskPhotosLimit, Toast.LENGTH_SHORT).show()
            return
        }
        pendingCameraTaskId = task.id
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            launchCameraCapture()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val columns = remember(state.columns, currentBoardId) {
        state.columns.filter { it.boardId == currentBoardId }.sortedBy { it.position }
    }
    val isEisenhowerBoard = KanbanNames.isEisenhowerBoard(board?.name)
    val isOkrBoard = KanbanNames.isOkrBoard(board?.name)
    val tasks = state.tasks // All tasks, we'll filter them by columnId and Eisenhower quadrant
    val titlePreviewTask = titlePreviewTaskId?.let { id -> tasks.find { it.id == id } }
    LaunchedEffect(titlePreviewTaskId, tasks) {
        if (titlePreviewTaskId != null && titlePreviewTask == null) {
            titlePreviewTaskId = null
        }
    }
    val comments = state.comments
    val commentCountByTaskId = remember(comments) {
        comments.groupingBy { it.taskId }.eachCount()
    }
    val columnCommentCountById = remember(state.columnComments, columns) {
        val hubIds = columns.mapTo(mutableSetOf()) { it.id }
        state.columnComments
            .filter { it.columnId in hubIds && !isSystemHubRule(it.text) }
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
                    task.isPastDeadline() -> 0
                    due.isDueToday() -> 1
                    else -> 2
                }
            }
            .thenBy { it.dueDate ?: Long.MAX_VALUE }
            .thenBy { TaskDueSort.startMinutes(it, kairosPrefs.reminderMinutesOfDay) }
            .thenBy { Eisenhower.getRank(it.eisenhowerQuadrant) }
            .thenBy { it.position }

        if (!kairosPrefs.isColumnManuallyOrdered(columnId)) {
            val activeHomes = hubTasks.filter {
                !it.isCompleted && !it.isHidden && it.columnId == columnId
            }
            val activeVisitors = hubTasks.filter {
                !it.isCompleted && !it.isHidden && it.columnId != columnId
            }
            val parked = hubTasks.filter { it.isCompleted || it.isHidden }
            return HubGroupOrdering.sortPreservingGroups(activeHomes, autoCompare) +
                activeVisitors.sortedWith(autoCompare) +
                parked.sortedWith(autoCompare)
        }
        // Manual order: homes follow rewritten `position` (DnD / hub sort). Do not re-bucket by due.
        // Groups stay above loose tasks; visitors keep auto ordering after homes.
        val ordered = hubTasks.sortedWith(
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
        val split = ordered.indexOfFirst {
            it.isCompleted || it.isHidden || it.columnId != columnId
        }
        val homeEnd = if (split < 0) ordered.size else split
        val homes = HubGroupOrdering.groupsFirst(ordered.subList(0, homeEnd)) { it.hubGroupId }
        return homes + ordered.subList(homeEnd, ordered.size)
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
        overdueOnlyHubIds
    ) {
        val projectId = board?.projectId
        val query = boardSearchQuery.trim()
        val overdueHubs = overdueOnlyHubIds.toSet()
        val columnIdsOnBoard = columns.mapTo(mutableSetOf()) { it.id }
        // Narrow candidate set once before per-hub filters (faster board open / switch).
        val candidateTasks = if (isEisenhowerBoard) {
            tasks.filter { task ->
                if (task.isBoardArchived) return@filter false
                val homeBoardId = columnIdToBoardId[task.columnId]
                val homeProjectId = homeBoardId?.let { boardIdToProjectId[it] }
                homeProjectId == null || homeProjectId == projectId ||
                    task.linkedColumnIds.any { it in columnIdsOnBoard }
            }
        } else {
            tasks.filter { task ->
                if (task.isBoardArchived) return@filter false
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
                    if (column.id !in overdueHubs) searched
                    else searched.filter { it.isPastDeadline() }
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

    val photoTaskIds = remember(state.taskAttachments) {
        state.taskAttachments.mapTo(HashSet()) { it.taskId }
    }

    val tasksById = remember(state.tasks) { state.tasks.associateBy { it.id } }
    val linkNeighborsByTaskId = remember(state.taskLinks, tasksById) {
        val parents = TaskLinkGraph.parentsOf(state.taskLinks)
        val children = TaskLinkGraph.childrenOf(state.taskLinks)
        val ids = (parents.keys + children.keys).toSet()
        ids.associateWith { taskId ->
            val ups = parents[taskId].orEmpty().mapNotNull { id ->
                tasksById[id]?.takeUnless { it.isHidden }
                    ?.let { TaskLinkNeighbor(id, it.title, isParent = true) }
            }
            val downs = children[taskId].orEmpty().mapNotNull { id ->
                tasksById[id]?.takeUnless { it.isHidden }
                    ?.let { TaskLinkNeighbor(id, it.title, isParent = false) }
            }
            ups + downs
        }.filterValues { it.isNotEmpty() }
    }
    val projectLinkCandidates = remember(state.tasks, state.columns, state.boards, board?.projectId) {
        val projectId = board?.projectId
        val visible = state.tasks.filter { !it.isHidden }
        if (projectId == null) visible
        else {
            val boardIds = state.boards.filter { it.projectId == projectId }.map { it.id }.toSet()
            val columnIds = state.columns.filter { it.boardId in boardIds }.map { it.id }.toSet()
            visible.filter { it.columnId in columnIds }
        }
    }
    val projectColumns = remember(state.columns, state.boards, board?.projectId) {
        val projectId = board?.projectId
        if (projectId == null) state.columns.sortedBy { it.position }
        else {
            val boardIds = state.boards.filter { it.projectId == projectId }.map { it.id }.toSet()
            state.columns.filter { it.boardId in boardIds }.sortedBy { it.position }
        }
    }

    fun clearLinkFocus() {
        linkFocusIds = emptySet()
        linkFocusAnchorId = null
    }

    fun clearLinkPickParent() {
        linkPickParentFor = null
        linkPickPendingParentId = null
    }

    /** Goals and all descendants via task links (within [projectLinkCandidates]). */
    val linkPickCandidateIds = remember(
        linkPickParentFor?.id,
        projectLinkCandidates,
        state.taskLinks
    ) {
        val childId = linkPickParentFor?.id ?: return@remember emptySet()
        val goals = projectLinkCandidates.filter { it.isGoal }.map { it.id }
        if (goals.isEmpty()) return@remember emptySet()
        val childrenMap = TaskLinkGraph.childrenOf(state.taskLinks)
        val ids = goals.toMutableSet()
        val queue = ArrayDeque(goals)
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            childrenMap[id].orEmpty().forEach { child ->
                if (ids.add(child)) queue.add(child)
            }
        }
        ids - childId
    }

    fun startLinkPickParent(task: Task) {
        clearLinkFocus()
        linkPickPendingParentId = null
        linkPickParentFor = task
    }

    val linkPickAlreadyParentIds = remember(linkPickParentFor?.id, state.taskLinks) {
        val childId = linkPickParentFor?.id ?: return@remember emptySet()
        state.taskLinks.filter { it.childId == childId }.mapTo(mutableSetOf()) { it.parentId }
    }

    fun selectLinkPickCandidate(parentCandidate: Task) {
        val child = linkPickParentFor ?: return
        if (parentCandidate.id == child.id) return
        linkPickPendingParentId =
            if (linkPickPendingParentId == parentCandidate.id) null else parentCandidate.id
    }

    fun confirmLinkPickParent() {
        val child = linkPickParentFor ?: return
        val parentId = linkPickPendingParentId ?: run {
            Toast.makeText(context, s.linkPickSelectFirst, Toast.LENGTH_SHORT).show()
            return
        }
        val parentCandidate = state.tasks.find { it.id == parentId } ?: return
        val already = state.taskLinks.any {
            it.parentId == parentCandidate.id && it.childId == child.id
        }
        if (already) {
            viewModel.removeTaskLink(parentCandidate.id, child.id)
            Toast.makeText(context, s.linkParentUnlinked(parentCandidate.title), Toast.LENGTH_SHORT).show()
        } else {
            val ok = viewModel.addTaskLink(parentCandidate.id, child.id)
            if (!ok) {
                Toast.makeText(context, s.taskLinkCycleRejected, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, s.linkParentLinked(parentCandidate.title), Toast.LENGTH_SHORT).show()
            }
        }
        linkPickPendingParentId = null
    }

    fun openBoardAtHub(targetBoardId: String, targetColumnId: String? = null) {
        val targetBoard = state.boards.find { it.id == targetBoardId }
        if (targetBoard != null && KanbanNames.isEisenhowerBoard(targetBoard.name)) {
            onOpenEisenhowerMatrix(targetBoard.projectId)
            return
        }
        if (targetColumnId != null) pendingHubColumnId = targetColumnId
        if (activeBoardId != targetBoardId) {
            boardNavStack = boardNavStack + targetBoardId
            activeBoardId = targetBoardId
        }
    }

    BackHandler {
        when {
            focusedGroupId != null -> focusedGroupId = null
            linkPickParentFor != null -> clearLinkPickParent()
            linkFocusIds.isNotEmpty() -> clearLinkFocus()
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
        state.boards.filter {
            it.id != currentBoardId && !it.isArchived && !KanbanNames.isEisenhowerBoard(it.name)
        }
    }

    var isAddingTaskToColumn by remember { mutableStateOf<String?>(null) }
    val taskFocusRequester = remember { FocusRequester() }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskDueDate by remember { mutableStateOf<Long?>(null) }
    var newTaskQuadrant by remember { mutableStateOf<String?>(null) }
    var newTaskComplexity by remember { mutableStateOf<Int?>(null) }
    var newTaskReminderMinutes by remember { mutableStateOf<Int?>(null) }
    var newTaskStartMinutes by remember { mutableStateOf<Int?>(null) }
    var newTaskDeadline by remember { mutableStateOf<Long?>(null) }
    var showNewTaskDeadlinePicker by remember { mutableStateOf(false) }
    var newTaskRemindBefore by remember { mutableStateOf(false) }
    var newTaskRemindAmount by remember { mutableStateOf(1) }
    var newTaskRemindUnit by remember { mutableStateOf(RemindLeadUnit.DAY) }
    var newTaskRemindText by remember { mutableStateOf("") }
    var newTaskParentIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var newTaskChildIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var newTaskLinkPickMode by remember { mutableStateOf<String?>(null) } // "parent" | "child"
    var newTaskOpenBlock by remember { mutableStateOf<TaskDialogBlock?>(null) }
    var newTaskIsGoal by remember { mutableStateOf(false) }
    var newTaskWindowKind by remember { mutableStateOf<TaskWindowKind?>(null) }
    var newTaskTimerMinutes by remember { mutableStateOf<Int?>(null) }
    var newTaskCardType by remember { mutableStateOf(TaskCardKind.PLAIN) }
    var newTaskCardPayload by remember { mutableStateOf("") }
    var editTaskCardType by remember { mutableStateOf(TaskCardKind.PLAIN) }
    var editTaskCardPayload by remember { mutableStateOf("") }
    var cardSetupFor by remember { mutableStateOf<String?>(null) }
    var cardSetupKind by remember { mutableStateOf(TaskCardKind.PLAIN) }
    var showNewTaskDatePicker by remember { mutableStateOf(false) }
    val newDatePickerState = rememberDatePickerState()
    val newDeadlinePickerState = rememberDatePickerState()
    
    var isAddingColumn by remember { mutableStateOf(false) }
    var newColumnTitle by remember { mutableStateOf("") }
    
    var columnToRename by remember { mutableStateOf<Column?>(null) }
    var renameColumnTitle by remember { mutableStateOf("") }
    var columnToDelete by remember { mutableStateOf<Column?>(null) }
    
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var editTaskTitle by remember { mutableStateOf("") }
    var editTaskDueDate by remember { mutableStateOf<Long?>(null) }
    var editTaskQuadrant by remember { mutableStateOf<String?>(null) }
    var editTaskComplexity by remember { mutableStateOf<Int?>(null) }
    var editTaskReminderMinutes by remember { mutableStateOf<Int?>(null) }
    var editTaskStartMinutes by remember { mutableStateOf<Int?>(null) }
    var editTaskDeadline by remember { mutableStateOf<Long?>(null) }
    var showEditTaskDeadlinePicker by remember { mutableStateOf(false) }
    var editTaskOpenBlock by remember { mutableStateOf<TaskDialogBlock?>(null) }
    var editTaskIsGoal by remember { mutableStateOf(false) }
    var editTaskWindowKind by remember { mutableStateOf<TaskWindowKind?>(null) }
    var editTaskTimerMinutes by remember { mutableStateOf<Int?>(null) }
    var editTaskRemindBefore by remember { mutableStateOf(false) }
    var editTaskRemindAmount by remember { mutableStateOf(1) }
    var editTaskRemindUnit by remember { mutableStateOf(RemindLeadUnit.DAY) }
    var editTaskRemindText by remember { mutableStateOf("") }
    var editTaskDescription by remember { mutableStateOf("") }
    var showEditTaskDatePicker by remember { mutableStateOf(false) }
    val editDatePickerState = rememberDatePickerState()
    val editDeadlinePickerState = rememberDatePickerState()
    var qualityDialogTask by remember { mutableStateOf<Task?>(null) }
    var criteriaDialogTask by remember { mutableStateOf<Task?>(null) }
    
    var showReorderColumnsDialog by remember { mutableStateOf(false) }
    var showJumpHubsDialog by remember { mutableStateOf(false) }
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
            if (currentTask == null || currentTask.isCompleted || currentTask.isHidden) {
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
    // Soft pager snap: low stiffness so the edge hand-off eases in (not a hard catch).
    val hubSnapDecay = rememberSplineBasedDecay<Float>()
    val hubSnapDensity = LocalDensity.current
    val flingBehavior = remember(lazyListState, hubSnapDecay, hubSnapDensity) {
        val snapThresholdPx = with(hubSnapDensity) { 1.dp.toPx() }
        val softSnap = spring(
            dampingRatio = 0.92f,
            stiffness = 110f,
            visibilityThreshold = snapThresholdPx
        )
        val softLowVelocity = spring(
            dampingRatio = 0.94f,
            stiffness = 85f,
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

    fun resolveOpenHubColumnId(): String? {
        if (columns.isEmpty()) return null
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
        return columns.getOrNull(targetIndex)?.id
    }

    val voiceAssistant = rememberVoiceAssistantController(
        viewModel = viewModel,
        openColumnIdProvider = { resolveOpenHubColumnId() }
    )
    val pendingVoiceAssistant by viewModel.pendingVoiceAssistant.collectAsState()
    val voiceMicClick = rememberUpdatedState(voiceAssistant.onMicClick)
    LaunchedEffect(pendingVoiceAssistant) {
        if (!pendingVoiceAssistant) return@LaunchedEffect
        if (!viewModel.consumePendingVoiceAssistant()) return@LaunchedEffect
        voiceMicClick.value.invoke()
    }

    val pendingAddTask by viewModel.pendingAddTask.collectAsState()
    var handledNavAddTask by remember(boardId) { mutableStateOf(false) }
    var pendingCalendarDue by remember { mutableStateOf(initialDueMillis) }
    fun openAddTaskDialog() {
        val preferred = kairosPrefs.getPrimaryHubId(currentBoardId)
        val fromCalendar = pendingCalendarDue != null
        val targetColId = (if (fromCalendar) initialColumnId else null)
            ?.takeIf { id -> columns.any { it.id == id } }
            ?: preferred?.takeIf { id -> columns.any { it.id == id } }
            ?: resolveOpenHubColumnId()
            ?: columns.firstOrNull()?.id
        if (targetColId == null) return
        showOkrPlanningGuide = false
        isAddingTaskToColumn = targetColId
        newTaskIsGoal = viewModel.isGoalByHub(targetColId)
        pendingCalendarDue?.let { due ->
            newTaskDueDate = due
            newTaskOpenBlock = TaskDialogBlock.When
            pendingCalendarDue = null
        }
    }
    LaunchedEffect(pendingAddTask, columns.isNotEmpty(), currentBoardId) {
        if (!pendingAddTask || columns.isEmpty()) return@LaunchedEffect
        // Let navigation finish so a recreated BoardScreen owns the one-shot.
        kotlinx.coroutines.delay(80)
        if (!viewModel.consumePendingAddTask()) return@LaunchedEffect
        openAddTaskDialog()
    }
    LaunchedEffect(initialOpenAddTask, columns.isNotEmpty(), currentBoardId) {
        if (!initialOpenAddTask || columns.isEmpty() || handledNavAddTask) return@LaunchedEffect
        handledNavAddTask = true
        kotlinx.coroutines.delay(80)
        viewModel.consumePendingAddTask()
        openAddTaskDialog()
    }

    fun isPrimaryHub(columnId: String, index: Int): Boolean {
        val preferred = primaryHubId ?: return index == 0
        val stillExists = columns.any { it.id == preferred }
        return if (stillExists) columnId == preferred else index == 0
    }

    LaunchedEffect(currentBoardId) {
        primaryHubId = kairosPrefs.getPrimaryHubId(currentBoardId)
        viewModel.ensureTodayRecurringInstances()
    }

    // Resolve the primary hub, then open it once per board visit.
    // The flag is set only after scrollToItem returns, so a cancelled first layout retries.
    LaunchedEffect(currentBoardId, columns.map { it.id }, pendingHubColumnId) {
        if (columns.isEmpty()) return@LaunchedEffect
        val stored = kairosPrefs.getPrimaryHubId(currentBoardId)
        val resolved = if (stored != null && columns.any { it.id == stored }) {
            stored
        } else {
            val fallback = columns.find { col ->
                val t = col.title.trim().lowercase()
                t == "сделать" || t == "do" || t == "to do" || t == "todo" ||
                    t == "сегодня" || t == "today"
            }?.id ?: columns.first().id
            kairosPrefs.setPrimaryHubId(currentBoardId, fallback)
            fallback
        }
        if (primaryHubId != resolved) primaryHubId = resolved
        if (pendingHubColumnId != null) return@LaunchedEffect
        if (lastBoardOpenedForPrimary == currentBoardId) return@LaunchedEffect
        val index = columns.indexOfFirst { it.id == resolved }.coerceAtLeast(0)
        snapshotFlow { lazyListState.layoutInfo.totalItemsCount }
            .first { it > index }
        lazyListState.scrollToItem(realToVirtual(index))
        currentColumnIndex = index
        lastBoardOpenedForPrimary = currentBoardId
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
    /** Hub whose tasks are in selection mode. Board chrome follows the settled hub. */
    var selectingColumnId by remember { mutableStateOf<String?>(null) }
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

    // Lite while paging: keep pages cheap until settle commits, otherwise the leaving
    // hub briefly rebuilds full TaskCards (classic end hitch).
    // Split flags: horizontal scroll must kill chrome-pull immediately (gesture arena),
    // but full→lite waits until the page has moved so the swipe start stays smooth.
    var hubSettledVirtual by remember { mutableIntStateOf(0) }
    var hubHorizontalScrolling by remember { mutableStateOf(false) }
    var hubPagingActive by remember { mutableStateOf(false) }

    LaunchedEffect(lazyListState, hubPagerCount) {
        snapshotFlow {
            val scrolling = lazyListState.isScrollInProgress
            val first = lazyListState.firstVisibleItemIndex
            val offset = lazyListState.firstVisibleItemScrollOffset
            val pageW = lazyListState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == first }
                ?.size
                ?: 0
            // ~40% of page: early finger motion must not rebuild full→lite cards.
            val progressed = first != hubSettledVirtual ||
                (pageW > 0 && offset > pageW * 2 / 5)
            Triple(scrolling, progressed, draggedTask != null)
        }
            .distinctUntilChanged()
            .collectLatest { (scrolling, progressed, dragging) ->
                if (dragging) {
                    hubHorizontalScrolling = false
                    return@collectLatest
                }
                if (scrolling) {
                    hubHorizontalScrolling = true
                    if (progressed) hubPagingActive = true
                    return@collectLatest
                }
                // Scroll ended: commit settled page BEFORE clearing paging so the leaving
                // hub never flips full while still in the LazyRow composition window.
                kotlinx.coroutines.delay(48)
                val center = hubVirtualNearCenter()
                hubSettledVirtual = center
                hubPagingActive = false
                hubHorizontalScrolling = false
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

    fun matchesDragRow(task: Task): Boolean {
        if (!task.isActiveDropTarget()) return false
        if (linkFocusIds.isNotEmpty() && task.id !in linkFocusIds) return false
        return true
    }

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
            .filter { matchesDragRow(it) && it.id != dragged.id }
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
                        matchesDragRow(it) && it.id != dragged.id
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
                // Neighbors of the on-screen slot. Home order can still contain not-done
                // tasks between them; those are not drawn in this list, so they must not
                // decide the plate.
                val beforeLanding = homesBeforeDrop.lastOrNull()
                val afterLanding = visualActive
                    .drop(clampedVisual)
                    .firstOrNull { it.columnId == dropTargetColumn }
                val pointerOverSource = taskBounds[dragged.id]?.contains(dragPos) == true &&
                    dragged.columnId == dropTargetColumn
                val dropGroupId = HubGroupOrdering.resolveDropGroupId(
                    beforeLanding,
                    afterLanding,
                    dragged.hubGroupId,
                    pointerOverSource
                )
                val draggedForMove = dragged.copy(hubGroupId = dropGroupId)
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
                            draggedForMove.copy(eisenhowerQuadrant = targetQuadrant),
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
                            draggedForMove,
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

    fun sortHubByImportance(columnId: String) {
        val idx = columns.indexOfFirst { it.id == columnId }
        val col = columns.getOrNull(idx) ?: return
        val home = hubTasksBase(col, idx)
            .filter { !it.isCompleted && !it.isHidden && it.columnId == col.id }
            .sortedBy { it.position }
        val cmp = compareBy<Task> { Eisenhower.getRank(it.eisenhowerQuadrant) }
                    .thenBy { it.dueDate ?: Long.MAX_VALUE }
                    .thenBy { it.position }
        val homeSorted = HubGroupOrdering.sortPreservingGroups(home, cmp)
        if (homeSorted.isEmpty()) {
            Toast.makeText(context, s.hubSortNothingToSort, Toast.LENGTH_SHORT).show()
            return
        }
        kairosPrefs.markColumnManuallyOrdered(col.id)
        viewModel.reorderHubHomeTasks(col.id, homeSorted.map { it.id })
        Toast.makeText(context, s.hubSortedByImportance, Toast.LENGTH_SHORT).show()
    }

    fun sortHubByDue(columnId: String) {
        val idx = columns.indexOfFirst { it.id == columnId }
        val col = columns.getOrNull(idx) ?: return
        val home = hubTasksBase(col, idx)
            .filter { !it.isCompleted && !it.isHidden && it.columnId == col.id }
            .sortedBy { it.position }
        val cmp = compareBy<Task> { task ->
                    val due = task.dueDate
                    when {
                        due == null -> 3
                        task.isPastDeadline() -> 0
                        due.isDueToday() -> 1
                        else -> 2
                    }
                }
                    .thenBy { it.dueDate ?: Long.MAX_VALUE }
                    .thenBy { TaskDueSort.startMinutes(it, kairosPrefs.reminderMinutesOfDay) }
                    .thenBy { Eisenhower.getRank(it.eisenhowerQuadrant) }
                    .thenBy { it.position }
        val homeSorted = HubGroupOrdering.sortPreservingGroups(home, cmp)
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
        newTaskDueDate = null
        newTaskQuadrant = null
        newTaskComplexity = null
        newTaskReminderMinutes = null
        newTaskStartMinutes = null
        newTaskDeadline = null
        newTaskRemindBefore = false
        newTaskRemindAmount = 1
        newTaskRemindUnit = RemindLeadUnit.DAY
        newTaskRemindText = ""
        newTaskParentIds = emptySet()
        newTaskChildIds = emptySet()
        newTaskLinkPickMode = null
        newTaskOpenBlock = null
        newTaskIsGoal = false
        newTaskWindowKind = null
        newTaskTimerMinutes = null
        newTaskCardType = TaskCardKind.PLAIN
        newTaskCardPayload = ""
        cardSetupFor = null
    }

    fun submitNewTask() {
        if (newTaskTitle.isBlank() || isAddingTaskToColumn == null) return
        val currentTasks = tasks.filter { it.columnId == isAddingTaskToColumn }
        val targetIndex = columns.indexOfFirst { it.id == isAddingTaskToColumn }
        val targetCol = columns.find { it.id == isAddingTaskToColumn }
        val targetQuadrant = newTaskQuadrant
            ?: targetCol?.let { getQuadrantForCol(it, targetIndex) }
        viewModel.addTask(
            title = newTaskTitle,
            columnId = isAddingTaskToColumn!!,
            categoryId = null,
            dueDate = newTaskDueDate,
            currentTasks = currentTasks,
            eisenhowerQuadrant = targetQuadrant,
            showEisenhowerButtons = true,
            reminderMinutesOfDay = if (newTaskDueDate != null) newTaskReminderMinutes else null,
            startMinutesOfDay = if (newTaskDueDate != null) newTaskStartMinutes else null,
            deadlineAt = newTaskDeadline,
            complexity = newTaskComplexity,
            timerMinutes = newTaskTimerMinutes,
            parentIds = newTaskParentIds.toList(),
            childIds = newTaskChildIds.toList(),
            noteLeadAmount = if (newTaskRemindBefore) newTaskRemindAmount else null,
            noteLeadUnit = if (newTaskRemindBefore) newTaskRemindUnit else null,
            noteText = newTaskRemindText.ifBlank { newTaskTitle },
            isGoal = newTaskIsGoal || viewModel.isGoalByHub(isAddingTaskToColumn!!),
            cardType = newTaskCardType,
            cardPayload = newTaskCardPayload
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
                TaskTimerScheduler.cancel(context, task.id)
                val result = snackbarHostState.showSnackbar(
                    message = s.taskMarkedDone,
                    actionLabel = s.rateQuality,
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    qualityDialogTask = state.tasks.find { it.id == task.id } ?: task.copy(
                        isCompleted = true,
                        workflowStatus = TaskWorkflow.DONE
                    )
                }
            }
        }
    }

    fun deleteTaskWithUndo(taskId: String) {
        TaskTimerScheduler.cancel(context, taskId)
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
            if (chromePullLocksOpen(chromePullPx, boardChromeMaxPx)) {
                showTopBar = true
                chromePullPx = 0f
            } else if (chromePullPx > 0f) {
                val anim = Animatable(chromePullPx)
                anim.animateTo(0f, animationSpec = ChromePullSettleSpring) {
                    chromePullPx = value
                }
            }
        }
    }

    val settledHubId = columns.getOrNull(virtualToReal(hubSettledVirtual))?.id
    val boardChromeMuted = draggedTask != null ||
        (selectingColumnId != null && selectingColumnId == settledHubId)
    LaunchedEffect(boardChromeMuted) {
        if (!boardChromeMuted) return@LaunchedEffect
        topBarMenuExpanded = false
        if (boardSearchOpen) {
            boardSearchOpen = false
            keyboardController?.hide()
        }
    }

    val titleBubbleOpen = titlePreviewTaskId != null
    var bubbleTitle by remember { mutableStateOf("") }
    SideEffect {
        titlePreviewTask?.let { bubbleTitle = it.title }
    }
    val dismissTitleBubble = rememberUpdatedState {
        titlePreviewTaskId = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(titleBubbleOpen) {
                if (!titleBubbleOpen) return@pointerInput
                awaitPointerEventScope {
                    var downPos = Offset.Zero
                    var pointerId: PointerId? = null
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.forEach { change ->
                            if (change.changedToDownIgnoreConsumed()) {
                                downPos = change.position
                                pointerId = change.id
                            } else if (
                                pointerId != null &&
                                change.id == pointerId &&
                                change.changedToUpIgnoreConsumed()
                            ) {
                                val isTap =
                                    (change.position - downPos).getDistance() < viewConfiguration.touchSlop
                                if (isTap) {
                                    dismissTitleBubble.value()
                                }
                                pointerId = null
                            }
                        }
                    }
                }
            }
    ) {
    Scaffold(
        topBar = {
                val focusId = focusedGroupId
                if (focusId != null) {
                    GroupFocusTopBar(
                        title = if (focusId == UngroupedFocusId) {
                            s.hubUngroupedLabel
                        } else {
                            kairosPrefs.hubGroupTitle(focusId).ifBlank { s.hubGroupLabel }
                        },
                        onClose = { focusedGroupId = null }
                    )
                } else {
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
                        modifier = (
                            if (showTopBar) {
                                Modifier.fillMaxWidth()
                            } else {
                                Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                            }
                        ).mutedWhen(boardChromeMuted)
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
                                .padding(start = 4.dp, end = AlignedMoreEdgeGutter),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                    IconButton(
                        onClick = { onBack() },
                        modifier = Modifier.size(BoardTopBarActionSize)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = s.back,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
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
                        modifier = Modifier.weight(1f)
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                                text = { Text(s.addHub) },
                                onClick = {
                                    topBarMenuExpanded = false
                                    isAddingColumn = true
                                },
                                leadingIcon = { Icon(Icons.Default.PostAdd, contentDescription = null) }
                            )
                            if (columns.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text(s.hubsJumpMenu) },
                                    onClick = {
                                        topBarMenuExpanded = false
                                        showJumpHubsDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.GridView, contentDescription = null) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null
                                        )
                                    }
                                )
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
                                shape = RoundedCornerShape(10.dp)
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
                } // focused group replaces board chrome
        },
        floatingActionButton = {
            if (draggedTask == null && columns.isNotEmpty() && linkPickParentFor == null && focusedGroupId == null) {
                val micListening = voiceAssistant.uiState.listening
                val addShape = CircleShape
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .mutedWhen(boardChromeMuted)
                        .clip(addShape)
                        .background(buttonFill())
                        .border(
                            width = 1.5.dp,
                            color = if (micListening) deleteButtonColor() else buttonLine(),
                            shape = addShape
                        )
                        .then(
                            if (boardChromeMuted) {
                                Modifier
                            } else {
                                Modifier.combinedClickable(
                                    onClick = {
                                        val targetColId = resolveOpenHubColumnId()
                                            ?: columns.firstOrNull()?.id
                                        isAddingTaskToColumn = targetColId
                                    },
                                    onLongClick = { voiceAssistant.onMicClick() }
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (micListening) Icons.Default.Mic else Icons.Default.Add,
                        contentDescription = if (micListening) s.voiceAssistant else s.addTask,
                        tint = if (micListening) deleteButtonColor() else buttonLabel(),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        val linkFocusActive = linkFocusIds.isNotEmpty() || linkPickParentFor != null
        val linkFocus = linkFocusChrome()
        val boardBg by animateColorAsState(
            targetValue = if (linkFocusActive) linkFocus.board else MaterialTheme.colorScheme.background,
            animationSpec = tween(220),
            label = "linkFocusBoardBg"
        )
        @Composable
        fun BoardColumnsPane() {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxSize()
            ) {
                AnimatedVisibility(
                    visible = focusedGroupId == null &&
                        (linkPickParentFor != null || linkFocusIds.isNotEmpty()),
                    enter = expandVertically(animationSpec = tween(120)) + fadeIn(animationSpec = tween(100)),
                    exit = shrinkVertically(animationSpec = tween(90)) + fadeOut(animationSpec = tween(70))
                ) {
                    val pickFor = linkPickParentFor
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .mutedWhen(boardChromeMuted)
                            .clip(RoundedCornerShape(10.dp))
                            .background(linkFocus.banner)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.AccountTree,
                            contentDescription = null,
                            tint = linkFocus.bannerContent,
                            modifier = Modifier.size(20.dp)
                        )
                            Text(
                                text = if (pickFor != null) {
                                    s.linkPickParentMode(pickFor.title)
                                } else {
                                    s.linkFocusMode
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = linkFocus.bannerContent,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        if (pickFor != null) {
                            IconButton(
                                onClick = { confirmLinkPickParent() },
                                enabled = linkPickPendingParentId != null
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = s.linkPickConfirm,
                                    tint = if (linkPickPendingParentId != null) {
                                        linkFocus.bannerContent
                                    } else {
                                        linkFocus.bannerContent.copy(alpha = 0.35f)
                                    }
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                if (pickFor != null) clearLinkPickParent()
                                else clearLinkFocus()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = s.linkFocusExit,
                                tint = linkFocus.bannerContent
                            )
                        }
                    }
                }

                // 1-Tap Quick Board Switcher (OKR ↔ Матрица Эйзенхауэра) - Hidden by default!
                val currentProjectBoards = remember(state.boards, board) {
                    if (board != null) {
                        state.boards.filter { it.projectId == board.projectId && !it.isArchived }
                    } else {
                        state.boards.filter { !it.isArchived }
                    }
                }

                AnimatedVisibility(
                    visible = focusedGroupId == null &&
                        showBoardsList && currentProjectBoards.isNotEmpty(),
                    enter = expandVertically(animationSpec = tween(80)) + fadeIn(animationSpec = tween(70)),
                    exit = shrinkVertically(animationSpec = tween(70)) + fadeOut(animationSpec = tween(60))
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .mutedWhen(boardChromeMuted),
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
                                shape = RoundedCornerShape(10.dp),
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
                    inProgressTaskId?.let { id ->
                        tasks.find { it.id == id && !it.isCompleted && !it.isHidden }
                    }
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
                    visible = focusedGroupId == null &&
                        showColumnHeaders && columns.isNotEmpty(),
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
                                    shape = RoundedCornerShape(10.dp),
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
                                            TodayCalendarMark(
                                                contentDescription = s.primaryHub,
                                                modifier = Modifier.size(18.dp),
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
                    userScrollEnabled = draggedTask == null && focusedGroupId == null,
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
                                        .height(64.dp)
                                        .mutedWhen(boardChromeMuted),
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
                        val useLiteCards = linkPickParentFor == null &&
                            draggedTask == null &&
                            (hubPagingActive || virtual != hubSettledVirtual)

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
                            commentCountByTaskId = commentCountByTaskId,
                            currentBoardId = currentBoardId,
                            overdueOnly = column.id in overdueOnlyHubIds,
                            onToggleOverdueOnly = {
                                overdueOnlyHubIds = if (column.id in overdueOnlyHubIds) {
                                    overdueOnlyHubIds - column.id
                                } else {
                                    overdueOnlyHubIds + column.id
                                }
                            },
                            onOpenBoard = { targetBoardId, targetColumnId ->
                                openBoardAtHub(targetBoardId, targetColumnId)
                            },
                            activeInProgressTaskId = inProgressTaskId,
                            highlightedTaskId = highlightedTaskId,
                            linkFocusIds = linkFocusIds,
                            groupFocusId = focusedGroupId,
                            onFocusGroup = { gid ->
                                clearLinkFocus()
                                clearLinkPickParent()
                                focusedGroupId = gid
                            },
                            linkPickFilterIds = if (linkPickParentFor != null) linkPickCandidateIds else null,
                            linkPickAlreadyParentIds = if (linkPickParentFor != null) {
                                linkPickAlreadyParentIds
                            } else {
                                emptySet()
                            },
                            linkNeighborsByTaskId = linkNeighborsByTaskId,
                            onLinkChipClick = { taskId ->
                                if (linkPickParentFor == null) {
                                    if (linkFocusAnchorId == taskId) {
                                        clearLinkFocus()
                                    } else {
                                        val related = TaskLinkGraph.relatedIds(taskId, state.taskLinks)
                                        linkFocusAnchorId = taskId
                                        linkFocusIds = related + taskId
                                    }
                                }
                            },
                            onOpenTaskLinks = { task -> startLinkPickParent(task) },
                            onOpenGoalSteps = { task -> linksDialogTask = task },
                            onOpenCardBody = { task -> onOpenTypedTask(task.id) },
                            onLinkPickParent = { candidate -> selectLinkPickCandidate(candidate) },
                            linkPickPendingParentId = linkPickPendingParentId,
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
                            isPrimaryHub = isPrimaryHub(column.id, index),
                            onSetPrimaryHub = {
                                primaryHubId = column.id
                                kairosPrefs.setPrimaryHubId(currentBoardId, column.id)
                            },
                            onSortByImportance = { sortHubByImportance(column.id) },
                            onSortByDue = { sortHubByDue(column.id) },
                            onCommentColumnClick = { columnForComments = column },
                            columnCommentsCount = columnCommentCountById[column.id] ?: 0,
                            onEditTaskClick = { task ->
                                taskToEdit = task
                                editTaskTitle = task.title
                                editTaskDescription = task.description
                                editTaskDueDate = task.dueDate
                                editDatePickerState.selectedDateMillis = task.dueDate?.let { localMillisToUtcPicker(it) }
                                editDeadlinePickerState.selectedDateMillis =
                                    task.deadlineAt?.let { localMillisToUtcPicker(it) }
                                editTaskQuadrant = task.eisenhowerQuadrant
                                editTaskComplexity = task.complexity
                                editTaskReminderMinutes = task.reminderMinutesOfDay
                                editTaskStartMinutes = task.startMinutesOfDay
                                editTaskDeadline = task.deadlineAt
                                val existingNote = state.reminders.find { it.taskId == task.id && (it.leadAmount ?: 0) > 0 }
                                editTaskRemindBefore = existingNote != null
                                editTaskRemindAmount = (existingNote?.leadAmount ?: 1).coerceAtLeast(1)
                                editTaskRemindUnit = RemindLeadUnit.fromId(existingNote?.leadUnit) ?: RemindLeadUnit.DAY
                                editTaskRemindText = existingNote?.text ?: task.title
                                editTaskIsGoal = task.isGoal || viewModel.isGoalByHub(task.columnId)
                                editTaskOpenBlock = null
                                editTaskWindowKind = null
                                editTaskTimerMinutes = task.timerMinutes?.takeIf { it > 0 }
                                editTaskCardType = TaskCardKind.normalize(task.cardType)
                                editTaskCardPayload = task.cardPayload
                            },
                            onOpenTaskCriteria = { task -> criteriaDialogTask = task },
                            onDeleteTaskClick = { taskId ->
                                deleteTaskWithUndo(taskId)
                            },
                            onCommentClick = { task -> taskForComments = task },
                            onOpenPhotos = { task -> photosDialogTask = task },
                            onTakePhoto = { task -> requestTakePhoto(task) },
                            onMoveTaskClick = { task ->
                                taskForMove = task
                                taskForMoveHubId = column.id
                            },
                            onPreviewTitle = { task ->
                                titlePreviewTaskId = task.id
                            },
                            draggedTaskId = draggedTask?.id,
                            chromePullEnabled = !boardChromeMuted &&
                                !hubHorizontalScrolling &&
                                focusedGroupId == null,
                            dropPreviewState = dropPreviewState,
                            observeDropPreview = !useLiteCards,
                            reportPositions = draggedTask != null && !useLiteCards,
                            useLiteCards = useLiteCards,
                            relatedBoardsByTaskId = relatedBoardsByTaskId,
                            photoTaskIds = photoTaskIds,
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
                                    chromePullPx = chromeOpenPullPx(
                                        chromePullPx,
                                        dy,
                                        boardChromeMaxPx,
                                        ::boardChromeOpenPullGain,
                                    )
                                }
                            },
                            onChromePullEnd = { settleChromePull() },
                            onSelectionActiveChanged = { columnId, active ->
                                selectingColumnId = when {
                                    active -> columnId
                                    selectingColumnId == columnId -> null
                                    else -> selectingColumnId
                                }
                            },
                            onChromeCollapseDelta = { dy ->
                                when {
                                    dy >= 0f -> 0f
                                    !showTopBar && chromePullPx <= 0f -> 0f
                                    else -> {
                                        val wasExpanded = showTopBar
                                        if (wasExpanded) {
                                            showTopBar = false
                                            boardSearchOpen = false
                                            showBoardsList = false
                                            showColumnHeaders = false
                                        }
                                        val span = boardChromeMaxPx.coerceAtLeast(1f)
                                        val from = if (wasExpanded) span else chromePullPx
                                        val next = collapseChromePull(from, dy, span, BoardChromeHideGain)
                                        val consumed =
                                            ((next - from) / BoardChromeHideGain).coerceIn(dy, 0f)
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

                if (focusJump != null && focusedGroupId == null) {
                    val (targetBoardId, targetColumnId, scrollAndTitle) = focusJump
                    val (scrollIndex, focusTitle) = scrollAndTitle
                    // Overlay — must not change LazyRow height when hub selection settles (that caused swipe jitter).
                    Surface(
                        onClick = {
                            if (boardChromeMuted) return@Surface
                            if (targetBoardId == currentBoardId && scrollIndex != null) {
                                coroutineScope.launch {
                                    lazyListState.animateScrollToItem(realToVirtual(scrollIndex))
                                }
                            } else {
                                openBoardAtHub(targetBoardId, targetColumnId)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 88.dp)
                            .zIndex(2f)
                            .mutedWhen(boardChromeMuted),
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
        }

        @Composable
        fun BoardAddDialogs(exclusiveDialog: String?) {
            if (cardSetupFor != null) {
                val existingType = if (cardSetupFor == "edit") editTaskCardType else newTaskCardType
                val existingPayload = if (cardSetupFor == "edit") editTaskCardPayload else newTaskCardPayload
                val initial = if (TaskCardKind.normalize(existingType) == cardSetupKind) {
                    TaskCardBody.decode(cardSetupKind, existingPayload).copy(kind = cardSetupKind)
                } else {
                    TaskCardBody(kind = cardSetupKind)
                }
                TaskCardSetupDialog(
                    initial = initial,
                    contacts = state.contacts.filter { it.projectId == board?.projectId },
                    onImportContact = { name, phone, email, social ->
                        viewModel.addContact(board?.projectId.orEmpty(), name, phone, email, "", social)
                    },
                    onDismiss = { cardSetupFor = null },
                    onDone = { body ->
                        val encoded = body.encode()
                        if (cardSetupFor == "edit") {
                            editTaskCardType = body.kind
                            editTaskCardPayload = encoded
                        } else {
                            newTaskCardType = body.kind
                            newTaskCardPayload = encoded
                        }
                        cardSetupFor = null
                    }
                )
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
                    containerColor = dialogSheetFill(),
                    tonalElevation = DialogSheetTonalElevation,
                    title = { DialogMarqueeText(text = s.newTask, fontWeight = FontWeight.Bold, maxLines = 2) },
                    text = {
                        val addTaskScroll = rememberScrollState()
                        val addTaskMaxHeight =
                            (LocalConfiguration.current.screenHeightDp * 0.55f).dp
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = addTaskMaxHeight)
                                .verticalScroll(addTaskScroll),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val newTaskGoalLocked = isAddingTaskToColumn?.let { viewModel.isGoalByHub(it) } == true
                            val defaultClockMinutes = kairosPrefs.reminderMinutesOfDay
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newTaskTitle,
                                    onValueChange = { newTaskTitle = it },
                                    label = { Text(s.task) },
                                    modifier = Modifier
                                        .weight(1f)
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
                                TaskGoalToggle(
                                    checked = newTaskGoalLocked || newTaskIsGoal,
                                    enabled = !newTaskGoalLocked,
                                    onCheckedChange = { newTaskIsGoal = it }
                                )
                            }

                            TaskCardTypeDisclosure(
                                kind = newTaskCardType,
                                expanded = newTaskOpenBlock == TaskDialogBlock.Type,
                                onExpandedChange = { open ->
                                    newTaskOpenBlock = if (open) TaskDialogBlock.Type else null
                                },
                                onPick = { kind ->
                                    if (kind == TaskCardKind.PLAIN) {
                                        newTaskCardType = TaskCardKind.PLAIN
                                        newTaskCardPayload = ""
                                    } else {
                                        cardSetupKind = kind
                                        cardSetupFor = "new"
                                    }
                                }
                            )

                            DialogDisclosureRow(
                                label = s.whenRowLabel,
                                value = formatWhenClosedSummary(
                                    dueDate = newTaskDueDate,
                                    startMinutes = newTaskStartMinutes,
                                    defaultMinutes = defaultClockMinutes,
                                    noneLabel = s.whenNone,
                                    locale = dateLocale
                                ),
                                expanded = newTaskOpenBlock == TaskDialogBlock.When,
                                onExpandedChange = { open ->
                                    newTaskOpenBlock = if (open) TaskDialogBlock.When else null
                                },
                                icon = Icons.Default.Schedule
                            ) {
                                TaskWhenPlate(
                                    dueDate = newTaskDueDate,
                                    onDueDateChange = {
                                        newTaskDueDate = it
                                        if (it == null) {
                                            newTaskReminderMinutes = null
                                            newTaskStartMinutes = null
                                            newTaskRemindBefore = false
                                        }
                                    },
                                    onPickCustomDate = {
                                        newDatePickerState.selectedDateMillis =
                                            (newTaskDueDate ?: localNoonForDayOffset(0)).let { localMillisToUtcPicker(it) }
                                        showNewTaskDeadlinePicker = false
                                        showNewTaskDatePicker = true
                                    },
                                    startMinutes = newTaskStartMinutes,
                                    reminderMinutes = newTaskReminderMinutes,
                                    defaultMinutes = defaultClockMinutes,
                                    onStartMinutesChange = { newTaskStartMinutes = it },
                                    onReminderMinutesChange = { newTaskReminderMinutes = it },
                                    remindBefore = {
                                        RemindBeforeEditor(
                                            enabled = newTaskRemindBefore,
                                            amount = newTaskRemindAmount,
                                            unit = newTaskRemindUnit,
                                            text = newTaskRemindText,
                                            preview = leadReminderPreview(
                                                enabled = newTaskRemindBefore,
                                                dueDate = newTaskDueDate,
                                                reminderMinutes = newTaskStartMinutes ?: newTaskReminderMinutes,
                                                amount = newTaskRemindAmount,
                                                unit = newTaskRemindUnit,
                                                defaultMinutes = defaultClockMinutes,
                                                locale = dateLocale,
                                                firesLabel = s::remindBeforePreview
                                            ),
                                            onEnabledChange = { on ->
                                                if (on && newTaskDueDate == null) {
                                                    Toast.makeText(context, s.reminderNeedsDue, Toast.LENGTH_SHORT).show()
                                                    return@RemindBeforeEditor
                                                }
                                                newTaskRemindBefore = on
                                                if (on && newTaskRemindText.isBlank()) newTaskRemindText = newTaskTitle
                                            },
                                            onAmountChange = { newTaskRemindAmount = it },
                                            onUnitChange = { newTaskRemindUnit = it },
                                            onTextChange = { newTaskRemindText = it }
                                        )
                                    }
                                )
                            }

                            DialogDisclosureRow(
                                label = s.deadlineRowLabel,
                                value = formatDeadlineClosedSummary(
                                    deadlineAt = newTaskDeadline,
                                    window = newTaskWindowKind,
                                    noneLabel = s.deadlineNone,
                                    weekLabel = s.windowThisWeek,
                                    monthLabel = s.windowThisMonth,
                                    locale = dateLocale
                                ),
                                expanded = newTaskOpenBlock == TaskDialogBlock.Deadline,
                                onExpandedChange = { open ->
                                    newTaskOpenBlock = if (open) TaskDialogBlock.Deadline else null
                                },
                                icon = Icons.Default.DateRange
                            ) {
                                TaskDeadlinePlate(
                                    deadlineAt = newTaskDeadline,
                                    onDeadlineChange = { newTaskDeadline = it },
                                    onPickCustomDeadline = {
                                        newDeadlinePickerState.selectedDateMillis =
                                            (newTaskDeadline ?: localNoonForDayOffset(0)).let { localMillisToUtcPicker(it) }
                                        showNewTaskDatePicker = false
                                        showNewTaskDeadlinePicker = true
                                    },
                                    window = newTaskWindowKind,
                                    onWindowChange = { newTaskWindowKind = it }
                                )
                            }

                            DialogDisclosureRow(
                                label = s.timerRowLabel,
                                value = formatTimerClosedSummary(
                                    timerMinutes = newTaskTimerMinutes,
                                    noneLabel = s.timerNone,
                                    valueLabel = s::timerMinutesValue
                                ),
                                expanded = newTaskOpenBlock == TaskDialogBlock.Timer,
                                onExpandedChange = { open ->
                                    newTaskOpenBlock = if (open) TaskDialogBlock.Timer else null
                                },
                                icon = Icons.Default.Timer
                            ) {
                                TaskTimerPlate(
                                    timerMinutes = newTaskTimerMinutes,
                                    onTimerMinutesChange = { newTaskTimerMinutes = it }
                                )
                            }

                            if (columns.size > 1) {
                                DialogPanel {
                                    LazyRow(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                    items(columns) { col ->
                                        val isSelected = isAddingTaskToColumn == col.id
                                        Box(
                                            modifier = Modifier
                                                .widthIn(max = 200.dp)
                                                .height(40.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surface
                                                )
                                                .clickable {
                                                    isAddingTaskToColumn = col.id
                                                    if (viewModel.isGoalByHub(col.id)) newTaskIsGoal = true
                                                }
                                                .padding(horizontal = 14.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = s.localized(col.title),
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    }
                                }
                            }

                            DialogDisclosureRow(
                                label = s.eisenhowerButtons,
                                value = Eisenhower.getBadge(newTaskQuadrant).ifBlank { s.deadlineNone },
                                expanded = newTaskOpenBlock == TaskDialogBlock.Priority,
                                onExpandedChange = { open ->
                                    newTaskOpenBlock = if (open) TaskDialogBlock.Priority else null
                                },
                                icon = Icons.Default.PriorityHigh
                            ) {
                                EisenhowerPriorityEditor(
                                    enabled = true,
                                    quadrant = newTaskQuadrant,
                                    onEnabledChange = {},
                                    onQuadrantChange = { newTaskQuadrant = it },
                                    showToggle = false
                                )
                            }
                            DialogDisclosureRow(
                                label = s.taskComplexity,
                                value = newTaskComplexity?.toString() ?: s.deadlineNone,
                                expanded = newTaskOpenBlock == TaskDialogBlock.Impact,
                                onExpandedChange = { open ->
                                    newTaskOpenBlock = if (open) TaskDialogBlock.Impact else null
                                },
                                icon = Icons.Default.Bolt
                            ) {
                                TaskComplexityPicker(
                                    selected = newTaskComplexity,
                                    onSelect = { newTaskComplexity = it },
                                    showTitle = false
                                )
                            }
                            DialogDisclosureRow(
                                label = s.taskParents,
                                value = when (newTaskParentIds.size) {
                                    0 -> s.deadlineNone
                                    1 -> tasksById[newTaskParentIds.first()]?.title ?: s.deadlineNone
                                    else -> s.linkedToGoalsCount(newTaskParentIds.size)
                                },
                                expanded = newTaskOpenBlock == TaskDialogBlock.Goals,
                                onExpandedChange = { open ->
                                    newTaskOpenBlock = if (open) TaskDialogBlock.Goals else null
                                },
                                icon = Icons.Default.Flag
                            ) {
                                newTaskParentIds.forEach { parentId ->
                                    val title = tasksById[parentId]?.title ?: parentId
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = s.linkedToGoal(title),
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = {
                                            newTaskParentIds = newTaskParentIds - parentId
                                        }) {
                                            Icon(Icons.Default.Close, contentDescription = null)
                                        }
                                    }
                                }
                                TextButton(onClick = { newTaskLinkPickMode = "parent" }) {
                                    Icon(Icons.Default.AccountTree, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(s.addParentLink, maxLines = 1)
                                }
                            }
                            DialogDisclosureRow(
                                label = s.taskChildren,
                                value = when (newTaskChildIds.size) {
                                    0 -> s.deadlineNone
                                    1 -> tasksById[newTaskChildIds.first()]?.title ?: s.deadlineNone
                                    else -> newTaskChildIds.size.toString()
                                },
                                expanded = newTaskOpenBlock == TaskDialogBlock.Steps,
                                onExpandedChange = { open ->
                                    newTaskOpenBlock = if (open) TaskDialogBlock.Steps else null
                                },
                                icon = Icons.Default.AccountTree
                            ) {
                                newTaskChildIds.forEach { childId ->
                                    val title = tasksById[childId]?.title ?: childId
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = s.childrenChipLabel(title),
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = {
                                            newTaskChildIds = newTaskChildIds - childId
                                        }) {
                                            Icon(Icons.Default.Close, contentDescription = null)
                                        }
                                    }
                                }
                                TextButton(onClick = { newTaskLinkPickMode = "child" }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(s.addChildLink, maxLines = 1)
                                }
                            }
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

            if (exclusiveDialog == "addTask" && newTaskLinkPickMode != null) {
                val pickMode = newTaskLinkPickMode!!
                val candidates = remember(
                    pickMode,
                    projectLinkCandidates,
                    newTaskParentIds,
                    newTaskChildIds,
                    state.taskLinks
                ) {
                    projectLinkCandidates.filter { candidate ->
                        when (pickMode) {
                            "parent" -> {
                                candidate.isGoal &&
                                    candidate.id !in newTaskParentIds &&
                                    candidate.id !in newTaskChildIds &&
                                    newTaskChildIds.none {
                                        TaskLinkGraph.wouldCreateCycle(state.taskLinks, candidate.id, it)
                                    }
                            }
                            else -> {
                                candidate.id !in newTaskChildIds &&
                                    candidate.id !in newTaskParentIds &&
                                    newTaskParentIds.none {
                                        TaskLinkGraph.wouldCreateCycle(state.taskLinks, it, candidate.id)
                                    }
                            }
                        }
                    }
                }
                TaskLinkPickerDialog(
                    title = if (pickMode == "parent") s.pickParentTask else s.pickChildTask,
                    candidates = candidates,
                    columns = projectColumns,
                    emptyMessage = if (pickMode == "parent") s.noGoalsToLink else s.noTasksToLink,
                    onPick = { candidate ->
                        if (pickMode == "parent") {
                            newTaskParentIds = newTaskParentIds + candidate.id
                        } else {
                            newTaskChildIds = newTaskChildIds + candidate.id
                        }
                        newTaskLinkPickMode = null
                    },
                    onDismiss = { newTaskLinkPickMode = null }
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

            if (exclusiveDialog == "addTask" && showNewTaskDeadlinePicker) {
                DatePickerDialog(
                    onDismissRequest = { showNewTaskDeadlinePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            newTaskDeadline = newDeadlinePickerState.selectedDateMillis?.let { utcPickerMillisToLocalNoon(it) }
                            showNewTaskDeadlinePicker = false
                        }) { Text(s.ok) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNewTaskDeadlinePicker = false }) { Text(s.cancel) }
                    }
                ) {
                    DatePicker(state = newDeadlinePickerState)
                }
            }
            
            if (exclusiveDialog == "addColumn") {
                AlertDialog(
                    onDismissRequest = { isAddingColumn = false },
                    title = { DialogMarqueeText(text = s.newHub, fontWeight = FontWeight.Bold, maxLines = 2) },
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
                    title = { DialogMarqueeText(text = s.renameHub, fontWeight = FontWeight.Bold, maxLines = 2) },
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
                    title = { DialogMarqueeText(text = s.deleteHub, fontWeight = FontWeight.Bold, maxLines = 2) },
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
                            Text(s.delete, color = deleteButtonColor(), fontWeight = FontWeight.SemiBold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { columnToDelete = null }) { Text(s.cancel) }
                    }
                )
            }
        }

        @Composable
        fun BoardEditDialogs(exclusiveDialog: String?) {
            if (exclusiveDialog == "editTask") {
                fun saveEditedTask() {
                    if (editTaskTitle.isNotBlank()) {
                        val updatedTask = taskToEdit!!.copy(
                            title = editTaskTitle,
                            description = editTaskDescription,
                            categoryId = null,
                            dueDate = editTaskDueDate,
                            showEisenhowerButtons = true,
                            eisenhowerQuadrant = editTaskQuadrant,
                            complexity = editTaskComplexity,
                            reminderMinutesOfDay = if (editTaskDueDate != null) editTaskReminderMinutes else null,
                            startMinutesOfDay = if (editTaskDueDate != null) editTaskStartMinutes else null,
                            deadlineAt = editTaskDeadline,
                            isGoal = editTaskIsGoal || viewModel.isGoalByHub(taskToEdit!!.columnId),
                            timerMinutes = editTaskTimerMinutes?.takeIf { it > 0 },
                            cardType = editTaskCardType,
                            cardPayload = if (editTaskCardType == TaskCardKind.PLAIN) "" else editTaskCardPayload
                        )
                        viewModel.updateTask(updatedTask)
                        if (editTaskTimerMinutes?.takeIf { it > 0 } != taskToEdit!!.timerMinutes) {
                            TaskTimerScheduler.cancel(context, taskToEdit!!.id)
                        }
                        viewModel.setTaskLeadReminder(
                            task = updatedTask,
                            amount = if (editTaskRemindBefore) editTaskRemindAmount else null,
                            unit = if (editTaskRemindBefore) editTaskRemindUnit else null,
                            text = editTaskRemindText.ifBlank { editTaskTitle }
                        )
                    }
                    keyboardController?.hide()
                    taskToEdit = null
                }
                AlertDialog(
                    onDismissRequest = { taskToEdit = null },
                    containerColor = dialogSheetFill(),
                    tonalElevation = DialogSheetTonalElevation,
                    title = { DialogMarqueeText(text = s.editTask, fontWeight = FontWeight.Bold, maxLines = 2) },
                    text = {
                        val editTaskScroll = rememberScrollState()
                        val editTaskMaxHeight =
                            (LocalConfiguration.current.screenHeightDp * 0.55f).dp
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = editTaskMaxHeight)
                                .verticalScroll(editTaskScroll),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val editTaskGoalLocked = taskToEdit?.columnId?.let { viewModel.isGoalByHub(it) } == true
                            val editDefaultClockMinutes = kairosPrefs.reminderMinutesOfDay
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = editTaskTitle,
                                    onValueChange = { editTaskTitle = it },
                                    label = { Text(s.task) },
                                    modifier = Modifier.weight(1f),
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
                                TaskGoalToggle(
                                    checked = editTaskGoalLocked || editTaskIsGoal,
                                    enabled = !editTaskGoalLocked,
                                    onCheckedChange = { editTaskIsGoal = it }
                                )
                            }
                            OutlinedTextField(
                                value = editTaskDescription,
                                onValueChange = { editTaskDescription = it },
                                label = { Text(s.taskDescription) },
                                placeholder = { Text(s.taskDescriptionHint) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 5
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TaskCardTypeDisclosure(
                                kind = editTaskCardType,
                                expanded = editTaskOpenBlock == TaskDialogBlock.Type,
                                onExpandedChange = { open ->
                                    editTaskOpenBlock = if (open) TaskDialogBlock.Type else null
                                },
                                onPick = { kind ->
                                    if (kind == TaskCardKind.PLAIN) {
                                        editTaskCardType = TaskCardKind.PLAIN
                                        editTaskCardPayload = ""
                                    } else {
                                        cardSetupKind = kind
                                        cardSetupFor = "edit"
                                    }
                                }
                            )
                            DialogDisclosureRow(
                                label = s.whenRowLabel,
                                value = formatWhenClosedSummary(
                                    dueDate = editTaskDueDate,
                                    startMinutes = editTaskStartMinutes,
                                    defaultMinutes = editDefaultClockMinutes,
                                    noneLabel = s.whenNone,
                                    locale = dateLocale
                                ),
                                expanded = editTaskOpenBlock == TaskDialogBlock.When,
                                onExpandedChange = { open ->
                                    editTaskOpenBlock = if (open) TaskDialogBlock.When else null
                                },
                                icon = Icons.Default.Schedule
                            ) {
                                TaskWhenPlate(
                                    dueDate = editTaskDueDate,
                                    onDueDateChange = {
                                        editTaskDueDate = it
                                        if (it == null) {
                                            editTaskReminderMinutes = null
                                            editTaskStartMinutes = null
                                            editTaskRemindBefore = false
                                        }
                                    },
                                    onPickCustomDate = {
                                        editDatePickerState.selectedDateMillis =
                                            (editTaskDueDate ?: localNoonForDayOffset(0)).let { localMillisToUtcPicker(it) }
                                        showEditTaskDeadlinePicker = false
                                        showEditTaskDatePicker = true
                                    },
                                    startMinutes = editTaskStartMinutes,
                                    reminderMinutes = editTaskReminderMinutes,
                                    defaultMinutes = editDefaultClockMinutes,
                                    onStartMinutesChange = { editTaskStartMinutes = it },
                                    onReminderMinutesChange = { editTaskReminderMinutes = it },
                                    remindBefore = {
                                        RemindBeforeEditor(
                                            enabled = editTaskRemindBefore,
                                            amount = editTaskRemindAmount,
                                            unit = editTaskRemindUnit,
                                            text = editTaskRemindText,
                                            preview = leadReminderPreview(
                                                enabled = editTaskRemindBefore,
                                                dueDate = editTaskDueDate,
                                                reminderMinutes = editTaskStartMinutes ?: editTaskReminderMinutes,
                                                amount = editTaskRemindAmount,
                                                unit = editTaskRemindUnit,
                                                defaultMinutes = editDefaultClockMinutes,
                                                locale = dateLocale,
                                                firesLabel = s::remindBeforePreview
                                            ),
                                            onEnabledChange = { on ->
                                                if (on && editTaskDueDate == null) {
                                                    Toast.makeText(context, s.reminderNeedsDue, Toast.LENGTH_SHORT).show()
                                                    return@RemindBeforeEditor
                                                }
                                                editTaskRemindBefore = on
                                                if (on && editTaskRemindText.isBlank()) editTaskRemindText = editTaskTitle
                                            },
                                            onAmountChange = { editTaskRemindAmount = it },
                                            onUnitChange = { editTaskRemindUnit = it },
                                            onTextChange = { editTaskRemindText = it }
                                        )
                                    }
                                )
                            }
                            DialogDisclosureRow(
                                label = s.deadlineRowLabel,
                                value = formatDeadlineClosedSummary(
                                    deadlineAt = editTaskDeadline,
                                    window = editTaskWindowKind,
                                    noneLabel = s.deadlineNone,
                                    weekLabel = s.windowThisWeek,
                                    monthLabel = s.windowThisMonth,
                                    locale = dateLocale
                                ),
                                expanded = editTaskOpenBlock == TaskDialogBlock.Deadline,
                                onExpandedChange = { open ->
                                    editTaskOpenBlock = if (open) TaskDialogBlock.Deadline else null
                                },
                                icon = Icons.Default.DateRange
                            ) {
                                TaskDeadlinePlate(
                                    deadlineAt = editTaskDeadline,
                                    onDeadlineChange = { editTaskDeadline = it },
                                    onPickCustomDeadline = {
                                        editDeadlinePickerState.selectedDateMillis =
                                            (editTaskDeadline ?: localNoonForDayOffset(0)).let { localMillisToUtcPicker(it) }
                                        showEditTaskDatePicker = false
                                        showEditTaskDeadlinePicker = true
                                    },
                                    window = editTaskWindowKind,
                                    onWindowChange = { editTaskWindowKind = it }
                                )
                            }

                            DialogDisclosureRow(
                                label = s.timerRowLabel,
                                value = formatTimerClosedSummary(
                                    timerMinutes = editTaskTimerMinutes,
                                    noneLabel = s.timerNone,
                                    valueLabel = s::timerMinutesValue
                                ),
                                expanded = editTaskOpenBlock == TaskDialogBlock.Timer,
                                onExpandedChange = { open ->
                                    editTaskOpenBlock = if (open) TaskDialogBlock.Timer else null
                                },
                                icon = Icons.Default.Timer
                            ) {
                                TaskTimerPlate(
                                    timerMinutes = editTaskTimerMinutes,
                                    onTimerMinutesChange = { editTaskTimerMinutes = it }
                                )
                            }
                            DialogDisclosureRow(
                                label = s.eisenhowerButtons,
                                value = Eisenhower.getBadge(editTaskQuadrant).ifBlank { s.deadlineNone },
                                expanded = editTaskOpenBlock == TaskDialogBlock.Priority,
                                onExpandedChange = { open ->
                                    editTaskOpenBlock = if (open) TaskDialogBlock.Priority else null
                                },
                                icon = Icons.Default.PriorityHigh
                            ) {
                                EisenhowerPriorityEditor(
                                    enabled = true,
                                    quadrant = editTaskQuadrant,
                                    onEnabledChange = {},
                                    onQuadrantChange = { editTaskQuadrant = it },
                                    showToggle = false
                                )
                            }
                            DialogDisclosureRow(
                                label = s.taskComplexity,
                                value = editTaskComplexity?.toString() ?: s.deadlineNone,
                                expanded = editTaskOpenBlock == TaskDialogBlock.Impact,
                                onExpandedChange = { open ->
                                    editTaskOpenBlock = if (open) TaskDialogBlock.Impact else null
                                },
                                icon = Icons.Default.Bolt
                            ) {
                                TaskComplexityPicker(
                                    selected = editTaskComplexity,
                                    onSelect = { editTaskComplexity = it },
                                    showTitle = false
                                )
                            }
                            DialogPanel {
                            val editTaskCommentsCount = comments.count { it.taskId == taskToEdit?.id }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clickable { taskForComments = taskToEdit }
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Comment,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                DialogMarqueeText(
                                    text = s.commentsCount(editTaskCommentsCount),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    fillMaxWidth = false,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                            }
                            DialogPanel {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clickable {
                                        val moving = taskToEdit
                                        taskForMove = moving
                                        taskForMoveHubId = moving?.let { edited ->
                                            columns.find { taskAppearsOnColumn(edited, it, state.columns, state.boards) }?.id
                                                ?: edited.columnId
                                        }
                                        taskToEdit = null
                                    }
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                DialogMarqueeText(
                                    text = s.changeHubBoard,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    fillMaxWidth = false,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
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

            if (exclusiveDialog == "editTask" && showEditTaskDeadlinePicker) {
                DatePickerDialog(
                    onDismissRequest = { showEditTaskDeadlinePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            editTaskDeadline = editDeadlinePickerState.selectedDateMillis?.let { utcPickerMillisToLocalNoon(it) }
                            showEditTaskDeadlinePicker = false
                        }) { Text(s.ok) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditTaskDeadlinePicker = false }) { Text(s.cancel) }
                    }
                ) {
                    DatePicker(state = editDeadlinePickerState)
                }
            }
            
            qualityDialogTask?.let { rated ->
                CompletionQualityDialog(
                    title = rated.title,
                    current = state.tasks.find { it.id == rated.id }?.completionQuality,
                    onDismiss = { qualityDialogTask = null },
                    onSave = { q -> viewModel.setTaskCompletionQuality(rated.id, q) }
                )
            }

            criteriaDialogTask?.let { target ->
                val live = state.tasks.find { it.id == target.id } ?: target
                TaskCriteriaDialog(
                    task = live,
                    onDismiss = { criteriaDialogTask = null },
                    onUpdate = { viewModel.updateTask(it) },
                    onQuality = { q -> viewModel.setTaskCompletionQuality(live.id, q) }
                )
            }

            linksDialogTask?.let { linked ->
                TaskLinksDialog(
                    task = linked,
                    allTasks = projectLinkCandidates,
                    columns = projectColumns,
                    links = state.taskLinks,
                    onAddParent = { parentId ->
                        val ok = viewModel.addTaskLink(parentId, linked.id)
                        if (!ok) {
                            Toast.makeText(context, s.taskLinkCycleRejected, Toast.LENGTH_SHORT).show()
                        }
                        ok
                    },
                    onAddChild = { childId ->
                        val ok = viewModel.addTaskLink(linked.id, childId)
                        if (!ok) {
                            Toast.makeText(context, s.taskLinkCycleRejected, Toast.LENGTH_SHORT).show()
                        }
                        ok
                    },
                    onRemoveLink = { parentId, childId -> viewModel.removeTaskLink(parentId, childId) },
                    onDismiss = { linksDialogTask = null },
                    onCycleRejected = {
                        Toast.makeText(context, s.taskLinkCycleRejected, Toast.LENGTH_SHORT).show()
                    }
                )
            }

            photosDialogTask?.let { photoTask ->
                val projectId = board?.projectId.orEmpty()
                val attachments = state.taskAttachments.filter { it.taskId == photoTask.id }
                TaskPhotosDialog(
                    task = photoTask,
                    attachments = attachments,
                    resolvePreviewRef = { rel -> viewModel.attachmentPreviewRef(rel) },
                    canEdit = Entitlements.has(kairosPrefs, ProFeature.TaskPhotos),
                    onAddPhoto = { uri ->
                        viewModel.addTaskPhoto(photoTask.id, projectId, uri) { err ->
                            when (err) {
                                "limit" -> Toast.makeText(context, s.taskPhotosLimit, Toast.LENGTH_SHORT).show()
                                "error" -> Toast.makeText(context, s.voiceError, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onTakePhoto = { requestTakePhoto(photoTask) },
                    onDeletePhoto = { id -> viewModel.deleteTaskPhoto(id) },
                    onProRequired = {
                        Toast.makeText(context, s.proFeatureLocked, Toast.LENGTH_SHORT).show()
                    },
                    onDismiss = { photosDialogTask = null }
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

            if (exclusiveDialog == "jumpHubs") {
                AlertDialog(
                    onDismissRequest = { showJumpHubsDialog = false },
                    title = {
                        DialogMarqueeText(
                            text = s.allHubsHeader(columns.size),
                            fontWeight = FontWeight.Bold,
                            maxLines = 2
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            columns.forEachIndexed { index, col ->
                                val isSelected = index == currentColumnIndex
                                val showPrimaryMark = isPrimaryHub(col.id, index)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            showJumpHubsDialog = false
                                            if (index != currentColumnIndex) {
                                                coroutineScope.launch {
                                                    lazyListState.animateScrollToItem(
                                                        realToVirtual(index)
                                                    )
                                                }
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (showPrimaryMark) {
                                        TodayCalendarMark(
                                            contentDescription = s.primaryHub,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.size(22.dp))
                                    }
                                    Text(
                                        text = s.localized(col.title),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) {
                                            FontWeight.SemiBold
                                        } else {
                                            FontWeight.Normal
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showJumpHubsDialog = false }) {
                            Text(s.close)
                        }
                    }
                )
            }
        }

        @Composable
        fun BoardDialogsPane() {
            // Dialogs — only one exclusive modal at a time (nested date pickers stay with parent).
            val exclusiveDialog = when {
                columnToDelete != null -> "deleteColumn"
                taskForMove != null -> "move"
                taskForComments != null -> "taskComments"
                columnForComments != null -> "columnComments"
                isAddingTaskToColumn != null -> "addTask"
                showOkrPlanningGuide -> "planning"
                taskToEdit != null -> "editTask"
                isAddingColumn -> "addColumn"
                columnToRename != null -> "renameColumn"
                showReorderColumnsDialog -> "reorder"
                showJumpHubsDialog -> "jumpHubs"
                else -> null
            }

            BoardAddDialogs(exclusiveDialog)
            BoardEditDialogs(exclusiveDialog)
        }

        @Composable
        fun BoxScope.BoardDragChrome() {
            // Other boards drop target when dragging
            if (draggedTask != null && otherBoards.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(10.dp)),
                    shape = RoundedCornerShape(10.dp),
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
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isHovered) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .border(
                                            width = if (isHovered) 3.dp else 1.dp,
                                            color = if (isHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(10.dp)
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
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(boardBg)
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
            BoardColumnsPane()
            BoardDialogsPane()
            BoardDragChrome()
        }
    }

    AnimatedVisibility(
        visible = titleBubbleOpen,
        enter = fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
            slideInVertically(
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f)
            ) { fullHeight -> -fullHeight / 2 } +
            scaleIn(
                animationSpec = spring(dampingRatio = 0.64f, stiffness = 340f),
                initialScale = 0.78f,
                transformOrigin = TransformOrigin(0.5f, 0f)
            ),
        exit = fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing)) +
            slideOutVertically(
                animationSpec = tween(170, easing = FastOutSlowInEasing)
            ) { fullHeight -> -fullHeight / 3 } +
            scaleOut(
                animationSpec = tween(170, easing = FastOutSlowInEasing),
                targetScale = 0.9f,
                transformOrigin = TransformOrigin(0.5f, 0f)
            ),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .zIndex(20f)
    ) {
        TaskTitleThoughtBubble(
            title = bubbleTitle,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { dismissTitleBubble.value() }
        )
    }
    }
}

/** Calendar glyph with today's day-of-month. Marks the primary hub. */
@Composable
private fun TodayCalendarMark(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = null,
) {
    val day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString()
    val textMeasurer = rememberTextMeasurer()
    Canvas(
        modifier = modifier.semantics {
            if (contentDescription != null) {
                this.contentDescription = contentDescription
            }
        }
    ) {
        val w = size.width
        val h = size.height
        val stroke = (w * 0.08f).coerceAtLeast(1f)
        val radius = w * 0.16f
        val bodyTop = h * 0.18f
        val bodyLeft = stroke / 2f
        val bodyRight = w - stroke / 2f
        val bodyBottom = h - stroke / 2f
        val headerBottom = bodyTop + (bodyBottom - bodyTop) * 0.24f

        drawRoundRect(
            color = tint,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyRight - bodyLeft, bodyBottom - bodyTop),
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = stroke)
        )
        drawLine(
            color = tint,
            start = Offset(bodyLeft, headerBottom),
            end = Offset(bodyRight, headerBottom),
            strokeWidth = stroke
        )
        val ring = stroke * 1.4f
        drawLine(
            color = tint,
            start = Offset(w * 0.32f, 0f),
            end = Offset(w * 0.32f, bodyTop + stroke * 0.6f),
            strokeWidth = ring,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.68f, 0f),
            end = Offset(w * 0.68f, bodyTop + stroke * 0.6f),
            strokeWidth = ring,
            cap = StrokeCap.Round
        )

        val fontSize = (h * 0.42f / density).sp
        val layout = textMeasurer.measure(
            text = day,
            style = TextStyle(
                color = tint,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                lineHeight = fontSize
            )
        )
        val textTop = headerBottom + (bodyBottom - headerBottom - layout.size.height) / 2f
        drawText(
            textLayoutResult = layout,
            topLeft = Offset((w - layout.size.width) / 2f, textTop)
        )
    }
}

/** Fades chrome and swallows taps so only selection / move controls stay active. */
private const val ChromeMutedAlpha = 0.38f

private fun Modifier.mutedWhen(muted: Boolean): Modifier {
    if (!muted) return this
    return this
        .alpha(ChromeMutedAlpha)
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    event.changes.forEach { it.consume() }
                }
            }
        }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColumnItem(
    column: Column,
    columnIndex: Int = 0,
    tasks: List<Task>,
    commentCountByTaskId: Map<String, Int> = emptyMap(),
    currentBoardId: String = "",
    overdueOnly: Boolean = false,
    onToggleOverdueOnly: () -> Unit = {},
    onOpenBoard: (boardId: String, columnId: String) -> Unit = { _, _ -> },
    activeInProgressTaskId: String? = null,
    highlightedTaskId: String? = null,
    linkFocusIds: Set<String> = emptySet(),
    /** When non-null, this hub shows only that group's tasks and hides hub chrome. */
    groupFocusId: String? = null,
    onFocusGroup: (String) -> Unit = {},
    /** When non-null, only these task ids are shown (link-pick-parent mode). */
    linkPickFilterIds: Set<String>? = null,
    linkPickAlreadyParentIds: Set<String> = emptySet(),
    linkPickPendingParentId: String? = null,
    linkNeighborsByTaskId: Map<String, List<TaskLinkNeighbor>> = emptyMap(),
    onLinkChipClick: (String) -> Unit = {},
    onOpenTaskLinks: (Task) -> Unit = {},
    onOpenPhotos: (Task) -> Unit = {},
    onTakePhoto: (Task) -> Unit = {},
    onCreateStep: (Task) -> Unit = {},
    onOpenGoalSteps: (Task) -> Unit = {},
    onOpenCardBody: (Task) -> Unit = {},
    onLinkPickParent: (Task) -> Unit = {},
    onToggleInProgress: (Task) -> Unit = {},
    onColumnPositioned: (androidx.compose.ui.geometry.Rect) -> Unit,
    onTaskPositioned: (Task, androidx.compose.ui.geometry.Rect) -> Unit,
    onDragStart: (Task, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDeleteColumnClick: () -> Unit,
    onRenameColumnClick: () -> Unit,
    isPrimaryHub: Boolean = false,
    onSetPrimaryHub: () -> Unit = {},
    onSortByImportance: () -> Unit = {},
    onSortByDue: () -> Unit = {},
    onEditTaskClick: (Task) -> Unit,
    onOpenTaskCriteria: (Task) -> Unit = {},
    onDeleteTaskClick: (String) -> Unit,
    onCommentClick: (Task) -> Unit,
    onMoveTaskClick: (Task) -> Unit,
    onPreviewTitle: (Task) -> Unit = {},
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
    photoTaskIds: Set<String> = emptySet(),
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
    onSelectionActiveChanged: (columnId: String, active: Boolean) -> Unit = { _, _ -> },
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
    val activeTasks = remember(tasks, showHiddenTasks, linkFocusIds, linkPickFilterIds, groupFocusId) {
        val base = tasks.filter {
            !it.isBoardArchived &&
                !it.isCompleted &&
                !it.isNotDone &&
                (showHiddenTasks || !it.isHidden) &&
                (linkFocusIds.isEmpty() || it.id in linkFocusIds) &&
                (linkPickFilterIds == null || it.id in linkPickFilterIds)
        }
        when (groupFocusId) {
            null -> base
            UngroupedFocusId -> {
                val sizes = base.groupingBy { it.hubGroupId }.eachCount()
                base.filter { task ->
                    val id = task.hubGroupId
                    id == null || (sizes[id] ?: 0) < 2
                }
            }
            else -> base.filter { it.hubGroupId == groupFocusId }
        }
    }
    val ungroupedCount = remember(tasks, showHiddenTasks) {
        val base = tasks.filter {
            !it.isBoardArchived &&
                !it.isCompleted &&
                !it.isNotDone &&
                (showHiddenTasks || !it.isHidden)
        }
        val sizes = base.groupingBy { it.hubGroupId }.eachCount()
        base.count { task ->
            val id = task.hubGroupId
            id == null || (sizes[id] ?: 0) < 2
        }
    }
    val visibleActiveTasks = activeTasks
    val notDoneTasks = remember(tasks, showHiddenTasks, linkFocusIds, linkPickFilterIds) {
        tasks.filter {
            !it.isBoardArchived &&
                it.isNotDone &&
                (showHiddenTasks || !it.isHidden) &&
                (linkFocusIds.isEmpty() || it.id in linkFocusIds) &&
                (linkPickFilterIds == null || it.id in linkPickFilterIds)
        }
    }
    val completedTasks = remember(tasks, showHiddenTasks, linkFocusIds, linkPickFilterIds) {
        tasks.filter {
            !it.isBoardArchived &&
                it.isCompleted &&
                (showHiddenTasks || !it.isHidden) &&
                (linkFocusIds.isEmpty() || it.id in linkFocusIds) &&
                (linkPickFilterIds == null || it.id in linkPickFilterIds)
        }.sortedByDescending { it.completedAt ?: 0L }
    }
    var showCompleted by remember { mutableStateOf(false) }
    var showNotDone by remember { mutableStateOf(false) }
    LaunchedEffect(linkPickFilterIds) {
        if (linkPickFilterIds != null) {
            showCompleted = true
            showNotDone = true
        }
    }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showClearNotDoneDialog by remember { mutableStateOf(false) }
    var completedHeaderMenuOpen by remember { mutableStateOf(false) }
    var selectMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    /** When set, selection "group" action adds tasks into this existing group. */
    var addTargetGroupId by remember { mutableStateOf<String?>(null) }
    val hubPrefs = remember { KairosPreferences(context) }
    var groupMetaTick by remember { mutableIntStateOf(0) }
    var renameGroupId by remember { mutableStateOf<String?>(null) }
    var renameGroupDraft by remember { mutableStateOf("") }
    BackHandler(enabled = selectMode) {
        selectMode = false
        selectedIds = emptySet()
        addTargetGroupId = null
    }
    val hubChromeMuted = selectMode || draggedTaskId != null
    LaunchedEffect(selectMode) {
        onSelectionActiveChanged(column.id, selectMode)
    }
    DisposableEffect(column.id) {
        onDispose { onSelectionActiveChanged(column.id, false) }
    }
    val groupFocusActive = groupFocusId != null
    LaunchedEffect(groupFocusId) {
        if (groupFocusId != null) {
            selectMode = false
            selectedIds = emptySet()
            addTargetGroupId = null
        }
    }
    var notDoneHeaderMenuOpen by remember { mutableStateOf(false) }

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
    var hubMenuPage by remember { mutableStateOf(HubMoreMenuPage.Root) }
    LaunchedEffect(hubMenuExpanded) {
        if (!hubMenuExpanded) hubMenuPage = HubMoreMenuPage.Root
    }
    LaunchedEffect(hubChromeMuted) {
        if (!hubChromeMuted) return@LaunchedEffect
        hubMenuExpanded = false
        notDoneHeaderMenuOpen = false
        completedHeaderMenuOpen = false
    }
    // Non-Compose counter: bump while lite without recomposing the hub list mid-swipe.
    val actionsRevealEpoch = remember { java.util.concurrent.atomic.AtomicInteger(0) }
    LaunchedEffect(useLiteCards) {
        if (useLiteCards) {
            actionsRevealEpoch.incrementAndGet()
        }
    }

    val linkFocusActive = !groupFocusActive &&
        (linkFocusIds.isNotEmpty() || linkPickFilterIds != null)
    val linkFocus = linkFocusChrome()
    val hubBg by animateColorAsState(
        targetValue = when {
            isDropTarget -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            linkFocusActive -> linkFocus.hub
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(220),
        label = "linkFocusHubBg"
    )

    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .then(
                if (groupFocusActive) {
                    Modifier
                } else {
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(hubBg)
                        .border(
                            width = when {
                                isDropTarget -> 2.dp
                                linkFocusActive -> 1.5.dp
                                else -> 0.dp
                            },
                            color = when {
                                isDropTarget -> MaterialTheme.colorScheme.primary
                                linkFocusActive -> linkFocus.hubBorder
                                else -> Color.Transparent
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                }
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
        if (!groupFocusActive) {
        // Hub header: menu for actions (no pull gestures — keeps hub paging smooth)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(HubTitleStripHeight)
                .padding(bottom = if (immersiveChrome) 0.dp else 2.dp)
                // end gutter matches TopAppBar actions end padding (4.dp) so ⋮ stacks vertically
                .padding(start = 8.dp, end = 0.dp)
                .mutedWhen(hubChromeMuted),
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
                if (isPrimaryHub) {
                    TodayCalendarMark(
                        contentDescription = s.primaryHub,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                            Text(
                    text = s.localized(column.title),
                    style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        lineHeight = 26.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = if (useLiteCards) TextOverflow.Ellipsis else TextOverflow.Clip,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            // Marquee only on the settled hub — continuous scroll on peek
                            // neighbors costs frames during horizontal paging.
                            if (!useLiteCards) {
                                Modifier.basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    initialDelayMillis = 1200,
                                    delayMillis = 1500
                                )
                            } else {
                                Modifier
                            }
                        )
                )
            }
            if (ungroupedCount > 0) {
                Box(
                    modifier = Modifier
                        .size(AlignedMoreButtonSize)
                        .clickable { onFocusGroup(UngroupedFocusId) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = s.hubUngroupedFocus,
                        modifier = Modifier.size(AlignedMoreIconSize),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
                    if (hiddenCount > 0) {
                Box(
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .size(32.dp)
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
            AlignedMoreMenuButton(
                expanded = hubMenuExpanded,
                onExpandedChange = { hubMenuExpanded = it },
                tint = if (overdueOnly) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            ) {
                when (hubMenuPage) {
                    HubMoreMenuPage.Root -> {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (columnCommentsCount > 0) {
                                        s.commentsCount(columnCommentsCount)
                                    } else {
                                        s.hubRules
                                    }
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
                        DropdownMenuItem(
                            text = { Text(s.overdueOnly) },
                            onClick = {
                                hubMenuExpanded = false
                                onToggleOverdueOnly()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (overdueOnly) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        LocalContentColor.current
                                    }
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(s.sortHubMenu) },
                            onClick = { hubMenuPage = HubMoreMenuPage.Sort },
                            leadingIcon = { Icon(Icons.Default.PriorityHigh, contentDescription = null) },
                            trailingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(s.moreMenuMore) },
                            onClick = { hubMenuPage = HubMoreMenuPage.More },
                            leadingIcon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
                            trailingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                    HubMoreMenuPage.Sort -> {
                        DropdownMenuItem(
                            text = { Text(s.back) },
                            onClick = { hubMenuPage = HubMoreMenuPage.Root },
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = null
                                )
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                        DropdownMenuItem(
                            text = { Text(s.sortHubByImportanceShort) },
                            onClick = {
                                hubMenuExpanded = false
                                onSortByImportance()
                            },
                            leadingIcon = { Icon(Icons.Default.PriorityHigh, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(s.sortHubByDueShort) },
                            onClick = {
                                hubMenuExpanded = false
                                onSortByDue()
                            },
                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                        )
                    }
                    HubMoreMenuPage.More -> {
                        DropdownMenuItem(
                            text = { Text(s.back) },
                            onClick = { hubMenuPage = HubMoreMenuPage.Root },
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = null
                                )
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                        DropdownMenuItem(
                            text = { Text(if (isPrimaryHub) s.primaryHub else s.setPrimaryHub) },
                            onClick = {
                                hubMenuExpanded = false
                                onSetPrimaryHub()
                            },
                            leadingIcon = {
                                TodayCalendarMark(
                                    modifier = Modifier.size(24.dp),
                                    tint = if (isPrimaryHub) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        LocalContentColor.current
                                    }
                                )
                            },
                            trailingIcon = if (isPrimaryHub) {
                                {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            } else {
                                null
                            }
                        )
                        if (hiddenCount > 0) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (showHiddenTasks) s.hideHiddenTasks else s.showHiddenTasks
                                    )
                                },
                                onClick = {
                                    hubMenuExpanded = false
                                    showHiddenTasks = !showHiddenTasks
                                },
                                leadingIcon = {
                                    Icon(
                                        if (showHiddenTasks) {
                                            Icons.Default.Visibility
                                        } else {
                                            Icons.Default.VisibilityOff
                                        },
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
            }
        }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .nestedScroll(chromePullConnection)
        ) {
        LazyColumn(
            state = hubListState,
            userScrollEnabled = !useLiteCards && draggedTaskId == null,
            modifier = Modifier
                .fillMaxSize()
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
            contentPadding = PaddingValues(bottom = if (groupFocusActive) 24.dp else 140.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Same item count for lite and full — peek limit caused a vertical jump on settle.
            val listTasks = visibleActiveTasks
            val hubEmpty = visibleActiveTasks.isEmpty() && insertIndex == null &&
                (groupFocusActive || (notDoneTasks.isEmpty() && completedTasks.isEmpty()))
            if (hubEmpty) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                linkPickFilterIds != null -> s.linkPickEmpty
                                linkFocusIds.isNotEmpty() -> s.linkFocusEmptyHub
                                else -> s.noTasks
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                itemsIndexed(listTasks, key = { _, task -> task.id }) { index, task ->
                    val groupId = task.hubGroupId
                    val groupSize = if (groupId != null) {
                        listTasks.count { it.hubGroupId == groupId }
                    } else 0
                    val inGroup = !groupFocusActive && groupId != null && groupSize >= 2
                    val isGroupStart = inGroup && listTasks.getOrNull(index - 1)?.hubGroupId != groupId
                    val isGroupEnd = inGroup && listTasks.getOrNull(index + 1)?.hubGroupId != groupId
                    val filteredIndex = if (draggedTaskId == null) index
                    else visibleActiveTasks.take(index).count { it.id != draggedTaskId }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (!useLiteCards && insertIndex != null && task.id != draggedTaskId && insertIndex == filteredIndex) {
                            DropInsertSlot()
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        val metaVersion = groupMetaTick
                        val groupCollapsed = inGroup &&
                            groupId != null &&
                            hubPrefs.isHubGroupCollapsed(groupId).also { metaVersion }
                        val showMember = !groupCollapsed
                        val plateOpen = inGroup && (isGroupStart || showMember)
                        val closesPlate = plateOpen && (groupCollapsed || isGroupEnd)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (plateOpen) {
                                        Modifier
                                            .hubGroupPlate(
                                                dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                                            )
                                            .padding(horizontal = 8.dp)
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                        if (isGroupStart) {
                            val customTitle = hubPrefs.hubGroupTitle(groupId!!)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .hubGroupCapBackground()
                                    .padding(start = 4.dp, end = 2.dp, top = 6.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .then(
                                            if (hubChromeMuted) {
                                                Modifier
                                            } else {
                                                Modifier.clickable {
                                                    hubPrefs.setHubGroupCollapsed(groupId, !groupCollapsed)
                                                    groupMetaTick++
                                                }
                                            }
                                        )
                                        .padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                                ) {
                                    Text(
                                        text = customTitle.ifBlank { s.hubGroupLabel },
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = LiterataBold,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = s.hubGroupMembers(groupSize),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (!useLiteCards) {
                                    Box(modifier = Modifier.mutedWhen(hubChromeMuted)) {
                                    HubGroupPlateActions(
                                        revealToken = actionsRevealEpoch.get(),
                                        onFocus = { onFocusGroup(groupId) },
                                        onRename = {
                                            renameGroupId = groupId
                                            renameGroupDraft = customTitle
                                        },
                                        onAdd = {
                                            addTargetGroupId = groupId
                                            selectMode = true
                                            selectedIds = emptySet()
                                            Toast.makeText(context, s.addToGroupHint, Toast.LENGTH_SHORT).show()
                                        },
                                        onUngroup = {
                                            val ids = listTasks
                                                .filter { it.hubGroupId == groupId }
                                                .map { it.id }
                                            val cleared = viewModel.ungroupHubTasks(ids)
                                            hubPrefs.clearHubGroupMetaMany(cleared)
                                            groupMetaTick++
                                        }
                                    )
                                    }
                                } else {
                                    Spacer(
                                        modifier = Modifier
                                            .width(HubGroupPlateActionsWidth)
                                            .height(HubGroupPlateIconSlot)
                                    )
                                }
                            }
                            if (groupCollapsed) {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                        if (showMember) {
                            if (useLiteCards) {
                                LiteTaskRow(
                                    title = task.title,
                                    eisenhowerQuadrant = task.eisenhowerQuadrant.takeIf {
                                        !task.isCompleted && !task.isNotDone
                                    },
                                    dueDate = task.dueDate,
                                    isInProgress = activeInProgressTaskId == task.id,
                                    isCompleted = false,
                                    isGoal = task.isGoal,
                                    isRecurring = !task.recurringTemplateId.isNullOrBlank(),
                                    hasDeadline = task.deadlineAt != null,
                                    hasRelated = relatedBoardsByTaskId[task.id].orEmpty().isNotEmpty(),
                                    hasTaskLinks = linkNeighborsByTaskId[task.id].orEmpty().isNotEmpty(),
                                    hasPhotos = task.id in photoTaskIds,
                                    hasTimer = task.showsTimerAction,
                                    cardType = task.cardType,
                                    cardPayload = task.cardPayload
                                )
                            } else {
                                val taskCommentsCount = commentCountByTaskId[task.id] ?: 0
                                val relatedBoards = relatedBoardsByTaskId[task.id].orEmpty()
                        TaskCard(
                        task = task,
                        viewModel = viewModel,
                        commentsCount = taskCommentsCount,
                        isInProgress = activeInProgressTaskId == task.id,
                                    isHighlighted = highlightedTaskId == task.id ||
                                        (linkFocusIds.isNotEmpty() && task.id in linkFocusIds) ||
                                        (task.id in linkPickAlreadyParentIds),
                        isAnyTaskInProgress = activeInProgressTaskId != null,
                        isDragging = draggedTaskId == task.id,
                        relatedBoards = relatedBoards,
                                    linkNeighbors = linkNeighborsByTaskId[task.id].orEmpty(),
                                    onLinkChipClick = { onLinkChipClick(task.id) },
                                    onOpenTaskLinks = { onOpenTaskLinks(task) },
                                    onOpenPhotos = { onOpenPhotos(task) },
                                    onTakePhoto = { onTakePhoto(task) },
                                    onCreateStep = { onCreateStep(task) },
                                    onOpenGoalSteps = { onOpenGoalSteps(task) },
                                    onOpenCardBody = { onOpenCardBody(task) },
                                    linkPickMode = linkPickFilterIds != null,
                                    isLinkPickParent = task.id in linkPickAlreadyParentIds,
                                    isLinkPickPending = task.id == linkPickPendingParentId,
                                    onLinkPick = { onLinkPickParent(task) },
                                    selectionMode = selectMode,
                                    actionsLocked = selectMode || draggedTaskId != null,
                                    isSelected = task.id in selectedIds,
                                    onToggleSelected = {
                                        selectedIds = if (task.id in selectedIds) selectedIds - task.id
                                        else selectedIds + task.id
                                    },
                                    onEnterSelection = {
                                        selectMode = true
                                        selectedIds = setOf(task.id)
                                    },
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
                                    onDragStart = { offset ->
                                        if (groupFocusActive) return@TaskCard
                                        selectMode = false
                                        selectedIds = emptySet()
                                        addTargetGroupId = null
                                        onDragStart(task, offset)
                                    },
                                    onDrag = onDrag,
                                    onDragEnd = onDragEnd,
                                    onEditClick = { onEditTaskClick(task) },
                                    onPreviewTitle = { onPreviewTitle(task) },
                                    onCriteriaClick = { onOpenTaskCriteria(task) },
                                    onDeleteClick = { onDeleteTaskClick(task.id) },
                                    onToggleCompleted = { onToggleCompleted(task) },
                                    actionsRevealToken = actionsRevealEpoch.get()
                                )
                            }
                            Spacer(
                                modifier = Modifier.height(
                                    when {
                                        !inGroup -> 12.dp
                                        isGroupEnd -> 8.dp
                                        else -> 6.dp
                                    }
                                )
                            )
                        }
                        }
                        if (closesPlate) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
                if (!useLiteCards && insertIndex != null && insertIndex >= visibleActiveTasks.count { it.id != draggedTaskId }) {
                    item(key = "drop-slot-end") {
                        DropInsertSlot()
                    }
                }

                if (!groupFocusActive && notDoneTasks.isNotEmpty()) {
                    item(key = "not-done-header") {
                        HubFoldSectionHeader(
                            title = s.notDoneCount(notDoneTasks.size),
                            expanded = showNotDone,
                            interactive = !useLiteCards && !hubChromeMuted,
                            dimmed = hubChromeMuted,
                            menuOpen = notDoneHeaderMenuOpen,
                            onToggle = { showNotDone = !showNotDone },
                            onMenuClick = { notDoneHeaderMenuOpen = true },
                            onMenuDismiss = { notDoneHeaderMenuOpen = false },
                            onClearAll = {
                                notDoneHeaderMenuOpen = false
                                showClearNotDoneDialog = true
                            },
                            rowVerticalPadding = 12.dp,
                        )
                    }
                    if (!useLiteCards && showNotDone) {
                            items(notDoneTasks, key = { "nd-${it.id}" }) { task ->
                                val taskCommentsCount = commentCountByTaskId[task.id] ?: 0
                                val relatedBoards = relatedBoardsByTaskId[task.id].orEmpty()
                                TaskCard(
                                    task = task,
                                    viewModel = viewModel,
                                    commentsCount = taskCommentsCount,
                                    isInProgress = false,
                                    isHighlighted = (linkFocusIds.isNotEmpty() && task.id in linkFocusIds) ||
                                        task.id in linkPickAlreadyParentIds,
                                    isAnyTaskInProgress = activeInProgressTaskId != null,
                                    relatedBoards = relatedBoards,
                                    linkNeighbors = linkNeighborsByTaskId[task.id].orEmpty(),
                                    onLinkChipClick = { onLinkChipClick(task.id) },
                                    onOpenTaskLinks = { onOpenTaskLinks(task) },
                                    onOpenPhotos = { onOpenPhotos(task) },
                                    onTakePhoto = { onTakePhoto(task) },
                                    onCreateStep = { onCreateStep(task) },
                                    onOpenGoalSteps = { onOpenGoalSteps(task) },
                                    onOpenCardBody = { onOpenCardBody(task) },
                                    linkPickMode = linkPickFilterIds != null,
                                    isLinkPickParent = task.id in linkPickAlreadyParentIds,
                                    isLinkPickPending = task.id == linkPickPendingParentId,
                                    onLinkPick = { onLinkPickParent(task) },
                                    selectionMode = selectMode,
                                    actionsLocked = selectMode || draggedTaskId != null,
                                    isSelected = task.id in selectedIds,
                                    onToggleSelected = {
                                        selectedIds = if (task.id in selectedIds) selectedIds - task.id
                                        else selectedIds + task.id
                                    },
                                    onEnterSelection = {
                                        selectMode = true
                                        selectedIds = setOf(task.id)
                                    },
                                    onOpenRelatedBoard = onOpenBoard,
                                    onSetQuadrant = { q ->
                                        viewModel.setTaskQuadrant(task, q)
                                        val msg = if (q != null) s.addedToMatrix(q) else s.removedFromMatrix
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    },
                                    onToggleInProgress = {},
                        onCommentClick = { onCommentClick(task) },
                        onMoveClick = { onMoveTaskClick(task) },
                        reportPosition = reportPositions,
                        onPositioned = { rect -> onTaskPositioned(task, rect) },
                                    onDragStart = { offset ->
                                        selectMode = false
                                        selectedIds = emptySet()
                                        addTargetGroupId = null
                                        onDragStart(task, offset)
                                    },
                        onDrag = onDrag,
                        onDragEnd = onDragEnd,
                        onEditClick = { onEditTaskClick(task) },
                                    onPreviewTitle = { onPreviewTitle(task) },
                                    onCriteriaClick = { onOpenTaskCriteria(task) },
                        onDeleteClick = { onDeleteTaskClick(task.id) },
                        onToggleCompleted = { onToggleCompleted(task) }
                    )
                    }
                }
                }

                if (!groupFocusActive && completedTasks.isNotEmpty()) {
                    item(key = "completed-header") {
                        HubFoldSectionHeader(
                            title = s.completedCount(completedTasks.size),
                            expanded = showCompleted,
                            interactive = !useLiteCards && !hubChromeMuted,
                            dimmed = hubChromeMuted,
                            menuOpen = completedHeaderMenuOpen,
                            onToggle = { showCompleted = !showCompleted },
                            onMenuClick = { completedHeaderMenuOpen = true },
                            onMenuDismiss = { completedHeaderMenuOpen = false },
                            onClearAll = {
                                completedHeaderMenuOpen = false
                                showClearConfirmDialog = true
                            },
                            rowVerticalPadding = 6.dp,
                            titleVerticalPadding = 6.dp,
                        )
                    }
                    if (!useLiteCards && showCompleted) {
                        items(completedTasks, key = { it.id }) { task ->
                            val taskCommentsCount = commentCountByTaskId[task.id] ?: 0
                            val relatedBoards = relatedBoardsByTaskId[task.id].orEmpty()

                            TaskCard(
                                task = task,
                                viewModel = viewModel,
                                commentsCount = taskCommentsCount,
                                isInProgress = false,
                                isHighlighted = (linkFocusIds.isNotEmpty() && task.id in linkFocusIds) ||
                                    task.id in linkPickAlreadyParentIds,
                                isAnyTaskInProgress = activeInProgressTaskId != null,
                                relatedBoards = relatedBoards,
                                linkNeighbors = linkNeighborsByTaskId[task.id].orEmpty(),
                                onLinkChipClick = { onLinkChipClick(task.id) },
                                onOpenTaskLinks = { onOpenTaskLinks(task) },
                                onOpenPhotos = { onOpenPhotos(task) },
                                onTakePhoto = { onTakePhoto(task) },
                                onCreateStep = { onCreateStep(task) },
                                onOpenGoalSteps = { onOpenGoalSteps(task) },
                                    onOpenCardBody = { onOpenCardBody(task) },
                                linkPickMode = linkPickFilterIds != null,
                                isLinkPickParent = task.id in linkPickAlreadyParentIds,
                                isLinkPickPending = task.id == linkPickPendingParentId,
                                onLinkPick = { onLinkPickParent(task) },
                                selectionMode = selectMode,
                                    actionsLocked = selectMode || draggedTaskId != null,
                                isSelected = task.id in selectedIds,
                                onToggleSelected = {
                                    selectedIds = if (task.id in selectedIds) selectedIds - task.id
                                    else selectedIds + task.id
                                },
                                onEnterSelection = {
                                    selectMode = true
                                    selectedIds = setOf(task.id)
                                },
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
                                onDragStart = { offset ->
                                    selectMode = false
                                    selectedIds = emptySet()
                                    addTargetGroupId = null
                                    onDragStart(task, offset)
                                },
                                onDrag = onDrag,
                                onDragEnd = onDragEnd,
                                onEditClick = { onEditTaskClick(task) },
                                onPreviewTitle = { onPreviewTitle(task) },
                                onCriteriaClick = { onOpenTaskCriteria(task) },
                                onDeleteClick = { onDeleteTaskClick(task.id) },
                                onToggleCompleted = { onToggleCompleted(task) }
                            )
                        }
                    }
                }
            }
        }

        if (selectMode) {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .zIndex(2f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Keep actions clear of the voice mic + add FABs on the trailing edge.
                        .padding(start = 8.dp, end = 80.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = s.selectedCount(selectedIds.size),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            selectMode = false
                            selectedIds = emptySet()
                            addTargetGroupId = null
                        }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = s.cancelSelection)
                    }
                    val canUngroup = selectedIds.any { id ->
                        tasks.find { it.id == id }?.hubGroupId != null
                    }
                    if (canUngroup && addTargetGroupId == null) {
                        IconButton(
                            onClick = {
                                val cleared = viewModel.ungroupHubTasks(selectedIds)
                                hubPrefs.clearHubGroupMetaMany(cleared)
                                groupMetaTick++
                                selectMode = false
                                selectedIds = emptySet()
                                addTargetGroupId = null
                            }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = s.ungroupTasks,
                                modifier = Modifier.hubGroupMarkGoldFill(),
                                tint = Color.White
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            val target = addTargetGroupId
                            when {
                                target != null -> {
                                    val toAdd = selectedIds.filter { id ->
                                        tasks.find { it.id == id }?.hubGroupId != target
                                    }
                                    if (toAdd.isEmpty()) {
                                        Toast.makeText(context, s.addToGroupNeedTasks, Toast.LENGTH_SHORT).show()
                                    } else if (viewModel.addTasksToHubGroup(target, toAdd)) {
                                        hubPrefs.markColumnManuallyOrdered(column.id)
                                        selectMode = false
                                        selectedIds = emptySet()
                                        addTargetGroupId = null
                                    }
                                }
                                selectedIds.isEmpty() -> {
                                    Toast.makeText(context, s.groupNeedTwo, Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    val groupId = viewModel.groupHubTasks(selectedIds)
                                    if (groupId == null) {
                                        Toast.makeText(context, s.groupNeedTwo, Toast.LENGTH_SHORT).show()
                                    } else {
                                        hubPrefs.markColumnManuallyOrdered(column.id)
                                        selectMode = false
                                        selectedIds = emptySet()
                                        addTargetGroupId = null
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            if (addTargetGroupId != null) Icons.Default.Add else Icons.Default.GridView,
                            contentDescription = if (addTargetGroupId != null) s.addToGroup else s.groupTasks,
                            modifier = if (addTargetGroupId != null) {
                                Modifier.hubGroupMarkGoldFill()
                            } else {
                                Modifier.hubGroupGoldFill()
                            },
                            tint = Color.White
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
            title = { DialogMarqueeText(text = s.clearCompletedTitle, fontWeight = FontWeight.Bold, maxLines = 2) },
            text = {
                DialogMarqueeText(
                    text = s.clearCompletedText(completedTasks.size, s.localized(column.title)),
                    maxLines = 4
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(buttonFill())
                        .border(1.dp, deleteButtonColor(), RoundedCornerShape(10.dp))
                        .clickable {
                            viewModel.clearCompletedTasks(column.id)
                            showClearConfirmDialog = false
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(s.clear, color = deleteButtonColor(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(s.cancel, style = MaterialTheme.typography.titleMedium)
                }
            }
        )
    }

    if (showClearNotDoneDialog) {
        AlertDialog(
            onDismissRequest = { showClearNotDoneDialog = false },
            title = { DialogMarqueeText(text = s.clearNotDoneTitle, fontWeight = FontWeight.Bold, maxLines = 2) },
            text = {
                DialogMarqueeText(
                    text = s.clearNotDoneText(notDoneTasks.size, s.localized(column.title)),
                    maxLines = 4
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(buttonFill())
                        .border(1.dp, deleteButtonColor(), RoundedCornerShape(10.dp))
                        .clickable {
                            viewModel.clearNotDoneTasks(column.id)
                            showClearNotDoneDialog = false
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(s.clear, color = deleteButtonColor(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearNotDoneDialog = false }) {
                    Text(s.cancel, style = MaterialTheme.typography.titleMedium)
                }
            }
        )
    }

    renameGroupId?.let { gid ->
        AlertDialog(
            onDismissRequest = { renameGroupId = null },
            title = {
                DialogMarqueeText(
                    text = s.hubGroupRename,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
            },
            text = {
                OutlinedTextField(
                    value = renameGroupDraft,
                    onValueChange = { renameGroupDraft = it },
                    singleLine = true,
                    placeholder = { Text(s.hubGroupNameHint) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        hubPrefs.setHubGroupTitle(gid, renameGroupDraft)
                        groupMetaTick++
                        renameGroupId = null
                    }
                ) {
                    Text(
                        s.save,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { renameGroupId = null }) {
                    Text(
                        s.cancel,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        )
    }
}

/**
 * Long-press enters selection. The next real movement starts a drag with no second
 * long-press and no extra full touch-slop wait. Releasing without that move leaves
 * selection on. A scroll before the long-press does not toggle selection.
 * Short tap while already selecting toggles membership via [onTap].
 *
 * Callers must keep this pointerInput attached for the whole gesture — stripping it when
 * [isDragging] becomes true cancels [drag] immediately and drag never starts.
 */
private suspend fun PointerInputScope.detectSelectThenDragGestures(
    onSelect: () -> Unit,
    onTap: () -> Unit = {},
    onDragStart: (Offset) -> Unit,
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit = {}
) {
    val postSelectSlop = 4.dp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val firstLong = awaitLongPressOrCancellation(down.id)
        if (firstLong == null) {
            val change = currentEvent.changes.firstOrNull { it.id == down.id }
            if (change != null && !change.pressed) {
                val moved = (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                if (!moved) onTap()
            }
            return@awaitEachGesture
        }
        onSelect()
        val anchor = currentEvent.changes.firstOrNull { it.id == firstLong.id }?.position
            ?: firstLong.position
        val moved = awaitMovePastSlopOrLift(firstLong.id, anchor, postSelectSlop)
        if (moved == null) {
            return@awaitEachGesture
        }
        try {
            onDragStart(moved.position)
            val dragSucceeded = drag(pointerId = moved.id) { change ->
                onDrag(change, change.positionChange())
                change.consume()
            }
            if (dragSucceeded) onDragEnd() else onDragCancel()
        } catch (c: CancellationException) {
            onDragCancel()
            throw c
        }
    }
}

/**
 * After selection, drag starts as soon as the finger leaves [anchor] by [slop].
 * Distance uses absolute position so an already-consumed delta still counts.
 * Lift without that move returns null and leaves selection in place.
 * Moves are consumed on the initial pass so the list does not scroll first.
 */
private suspend fun AwaitPointerEventScope.awaitMovePastSlopOrLift(
    pointerId: PointerId,
    anchor: Offset,
    slop: Float
): PointerInputChange? {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        val change = event.changes.firstOrNull { it.id == pointerId } ?: return null
        if (!change.pressed || change.changedToUpIgnoreConsumed()) return null
        val moved = (change.position - anchor).getDistance() > slop
        change.consume()
        if (moved) return change
    }
}

private enum class HubMoreMenuPage { Root, Sort, More }
private enum class TaskMoreMenuPage { Root, More }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TaskCard(
    task: Task,
    viewModel: SharedViewModel,
    modifier: Modifier = Modifier,
    commentsCount: Int = 0,
    isInProgress: Boolean = false,
    isHighlighted: Boolean = false,
    isAnyTaskInProgress: Boolean = false,
    isDragging: Boolean = false,
    isDragPreview: Boolean = false,
    relatedBoards: List<RelatedBoardLink> = emptyList(),
    linkNeighbors: List<TaskLinkNeighbor> = emptyList(),
    onLinkChipClick: () -> Unit = {},
    onOpenTaskLinks: () -> Unit = {},
    onOpenPhotos: () -> Unit = {},
    onTakePhoto: () -> Unit = {},
    onCreateStep: () -> Unit = {},
    onOpenGoalSteps: () -> Unit = {},
    onOpenCardBody: () -> Unit = {},
    linkPickMode: Boolean = false,
    isLinkPickParent: Boolean = false,
    isLinkPickPending: Boolean = false,
    onLinkPick: () -> Unit = {},
    selectionMode: Boolean = false,
    /** Selection or drag: card actions stay visible but do not run. */
    actionsLocked: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelected: () -> Unit = {},
    onEnterSelection: () -> Unit = {},
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
    onPreviewTitle: () -> Unit = {},
    onCriteriaClick: () -> Unit = {},
    onDeleteClick: () -> Unit,
    onToggleCompleted: (() -> Unit)? = null,
    /** When > 0 after lite→full settle, actions and the more button slide in together. */
    actionsRevealToken: Int = 0
) {
    val context = LocalContext.current
    val s = LocalAppStrings.current
    val dateLocale = LocalAppLanguage.current.locale
    val appState by viewModel.state.collectAsState()
    val prefs = remember { KairosPreferences(context) }

    val toggleCompleted = {
        onToggleCompleted?.invoke() ?: viewModel.toggleTaskCompletion(task)
    }

    val targetAlpha = when {
        isDragging -> 0.18f
        isAnyTaskInProgress && !isInProgress -> 0.32f
        // Selection mode: dim everything except the chosen cards so picks pop.
        selectionMode && !isSelected && !isLinkPickPending -> 0.40f
        task.isHidden -> 0.55f
        else -> 1f
    }

    val selectionFill = inProgressMark().copy(alpha = 0.16f)
    val selectionStroke = inProgressMark()

    var moreMenuExpanded by remember(task.id) { mutableStateOf(false) }
    var taskMenuPage by remember(task.id) { mutableStateOf(TaskMoreMenuPage.Root) }
    LaunchedEffect(moreMenuExpanded) {
        if (!moreMenuExpanded) taskMenuPage = TaskMoreMenuPage.Root
    }
    LaunchedEffect(actionsLocked) {
        if (actionsLocked) moreMenuExpanded = false
    }
    // token > 0: start fully off-screen (clipped), then slide R→L. token == 0: already settled, show.
    val actionsReveal = remember(task.id, actionsRevealToken) {
        Animatable(if (actionsRevealToken > 0) 0f else 1f)
    }
    val density = LocalDensity.current
    val showTimerButton = task.showsTimerAction
    val actionsClusterWidth = taskActionsClusterWidth(showTimerButton)
    val trailingSlidePx = with(density) { TaskTrailingRevealDistance.toPx() }
    var timerRunning by remember(task.id) {
        mutableStateOf(TaskTimerScheduler.isRunning(context, task.id))
    }
    var remainMs by remember(task.id) { mutableLongStateOf(0L) }
    LaunchedEffect(task.id, showTimerButton) {
        if (!showTimerButton) {
            TaskTimerScheduler.cancel(context, task.id)
            timerRunning = false
            remainMs = 0L
        } else {
            timerRunning = TaskTimerScheduler.isRunning(context, task.id)
        }
    }
    LaunchedEffect(task.id, timerRunning) {
        if (!timerRunning) {
            remainMs = 0L
            return@LaunchedEffect
        }
        while (true) {
            val left = TaskTimerScheduler.remainingMillis(context, task.id)
            remainMs = left
            if (left <= 0L) {
                TaskTimerScheduler.announceFinished(context, task.id, task.title)
                timerRunning = false
                break
            }
            delay(1000)
        }
    }
    LaunchedEffect(task.id, actionsRevealToken) {
        if (actionsRevealToken > 0) {
            actionsReveal.snapTo(0f)
            actionsReveal.animateTo(
                1f,
                animationSpec = tween(durationMillis = TrailingRevealMillis, easing = FastOutSlowInEasing)
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
    var showQualityDialog by remember(task.id) { mutableStateOf(false) }
    val dueDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = task.dueDate?.let { localMillisToUtcPicker(it) }
    )

    if (showDueEditor) {
        AlertDialog(
            onDismissRequest = { showDueEditor = false },
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

    if (showQualityDialog) {
        CompletionQualityDialog(
            title = task.title,
            current = task.completionQuality,
            onDismiss = { showQualityDialog = false },
            onSave = { q -> viewModel.setTaskCompletionQuality(task.id, q) }
        )
    }

    val compactEisenhower = !task.isCompleted && !task.isNotDone && task.eisenhowerQuadrant != null
    val compactOverdue = !task.isNotDone && task.isPastDeadline()
    val cardShape = RoundedCornerShape(10.dp)
    val compactOnColor = if (task.isCompleted || task.isNotDone) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val latestSelectionMode = rememberUpdatedState(selectionMode)
    val latestActionsLocked = rememberUpdatedState(actionsLocked)
    val latestIsSelected = rememberUpdatedState(isSelected)
    val latestLinkPickMode = rememberUpdatedState(linkPickMode)
    val latestOnEnterSelection = rememberUpdatedState(onEnterSelection)
    val latestOnToggleSelected = rememberUpdatedState(onToggleSelected)
    val latestOnDragStart = rememberUpdatedState(onDragStart)
    val latestOnDrag = rememberUpdatedState(onDrag)
    val latestOnDragEnd = rememberUpdatedState(onDragEnd)
    val latestOnOpenCardBody = rememberUpdatedState(onOpenCardBody)
    val latestCardType = rememberUpdatedState(task.cardType)
    // Set for the whole drag so a short move does not also count as a title tap.
    val titleGestureLock = remember { mutableStateOf(false) }

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
                when {
                    linkPickMode -> Modifier.clickable { onLinkPick() }
                    // Keep pointerInput while this card is the drag source — removing it
                    // cancels the in-flight drag() and the move never starts.
                    isDragPreview -> Modifier
                    else -> Modifier.pointerInput(task.id) {
                        detectSelectThenDragGestures(
                            onSelect = {
                                moreMenuExpanded = false
                                when {
                                    latestLinkPickMode.value -> Unit
                                    !latestSelectionMode.value -> latestOnEnterSelection.value()
                                    !latestIsSelected.value -> latestOnToggleSelected.value()
                                    // Already selected: a following move escalates to drag.
                                }
                            },
                            onTap = {
                                if (latestSelectionMode.value) {
                                    latestOnToggleSelected.value()
                                }
                            },
                            onDragStart = { localOffset ->
                                titleGestureLock.value = true
                                moreMenuExpanded = false
                                latestOnDragStart.value(
                                    (layoutCoordsRef.coords?.positionInRoot() ?: Offset.Zero) + localOffset
                                )
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                latestOnDrag.value(dragAmount)
                            },
                            onDragEnd = {
                                titleGestureLock.value = false
                                latestOnDragEnd.value()
                            },
                            onDragCancel = { titleGestureLock.value = false }
                        )
                    }
                }
            )
            .then(
                if (isSelected || isLinkPickPending) {
                    Modifier
                        .clip(cardShape)
                        .background(selectionFill)
                } else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = when {
            isSelected || isLinkPickPending ->
                androidx.compose.foundation.BorderStroke(1.5.dp, selectionStroke)
            isLinkPickParent ->
                androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            isHighlighted ->
                androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary)
            isInProgress -> androidx.compose.foundation.BorderStroke(1.5.dp, inProgressBrush())
            else -> null
        },
        shape = cardShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
        Row(
            modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TaskCardRowContentMinHeight),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TaskRowTrailingGap)
            ) {
                TaskLeadingMarks(
                    selectionMode = selectionMode,
                    isSelected = isSelected,
                    onToggleSelected = onToggleSelected,
                    isRecurring = !task.recurringTemplateId.isNullOrBlank(),
                    isGoal = task.isGoal,
                    hasTimer = task.showsTimerAction,
                    recurringDescription = s.recurringTitle,
                    goalDescription = s.statsGoalLabel,
                    timerDescription = s.timerRowLabel,
                    iconTint = compactOnColor.copy(alpha = 0.75f),
                    goalTint = inProgressMark(),
                    selectionStroke = inProgressColor
                )
                TaskCardTitleBlock(
                    title = task.title,
                    titleColor = compactOnColor.copy(alpha = if (task.isNotDone) 0.72f else 1f),
                    titleDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    maxLines = Int.MAX_VALUE,
                    modifier = if (linkPickMode) {
                        Modifier
                    } else {
                        Modifier.pointerInput(task.id) {
                            detectTapGestures(
                                onTap = {
                                    if (
                                        TaskCardKind.normalize(latestCardType.value) == TaskCardKind.PLAIN ||
                                        titleGestureLock.value ||
                                        latestSelectionMode.value ||
                                        latestActionsLocked.value ||
                                        latestLinkPickMode.value
                                    ) {
                                        return@detectTapGestures
                                    }
                                    latestOnOpenCardBody.value()
                                },
                                onDoubleTap = {
                                    if (
                                        titleGestureLock.value ||
                                        latestSelectionMode.value ||
                                        latestActionsLocked.value ||
                                        latestLinkPickMode.value
                                    ) {
                                        return@detectTapGestures
                                    }
                                    if (task.isCompleted || task.isNotDone) onCriteriaClick()
                                    else onEditClick()
                                }
                            )
                        }
                    }
                )
                if (task.isNotDone) {
                    Text(
                        text = s.notDoneBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                if (!isDragPreview && !linkPickMode) {
                    Box(
                        modifier = Modifier
                            .width(TaskTrailingRevealDistance)
                            .height(TaskActionButtonSize)
                            .clipToBounds()
                            .mutedWhen(actionsLocked)
                    ) {
                    Row(
                        modifier = Modifier.graphicsLayer {
                            // One strip: done, not-done, and more travel the same distance.
                            translationX = (1f - actionsReveal.value) * trailingSlidePx
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TaskRowTrailingGap)
                    ) {
                    Box(
                        modifier = Modifier
                            .width(actionsClusterWidth)
                            .height(TaskActionButtonSize),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(TaskActionsClusterGap)
                        ) {
                            if (!task.isCompleted) {
                                GraphiteCircleButton(
                                    onClick = {
                                        if (task.isNotDone) viewModel.clearTaskNotDone(task)
                                        else viewModel.markTaskNotDone(task)
                                    },
                                    contentDescription = if (task.isNotDone) s.clearNotDone else s.markNotDone,
                                    icon = Icons.Default.Close,
                                    fill = if (task.isNotDone) {
                                        MaterialTheme.colorScheme.outline
                                    } else {
                                        buttonFill()
                                    },
                                    tint = if (task.isNotDone) Color.White else buttonLabel()
                                )
                            }
                            GraphiteCircleButton(
                                onClick = { toggleCompleted() },
                                contentDescription = s.completed,
                                icon = Icons.Default.Check,
                                borderColor = if (task.isCompleted) completedMark() else buttonLine(),
                                tint = if (task.isCompleted) completedMark() else buttonLabel()
                            )
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
                            onExpandedChange = { moreMenuExpanded = it }
                        ) {
                            when (taskMenuPage) {
                                TaskMoreMenuPage.Root -> {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
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
                                                if (commentsCount > 0) {
                                                    s.commentsCount(commentsCount)
                                                } else {
                                                    s.comments
                                                },
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
                                        text = { Text(s.moveTask, style = MaterialTheme.typography.bodyLarge) },
                                        onClick = {
                                            moreMenuExpanded = false
                                            onMoveClick()
                                        },
                                        leadingIcon = {
                                            Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null)
                                        }
                                    )
                                    if (showTimerButton) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    if (timerRunning) {
                                                        TaskTimerPlanner.remainClock(remainMs)
                                                    } else {
                                                        s.timerStart
                                                    },
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                            },
                                            onClick = {
                                                if (timerRunning) {
                                                    TaskTimerScheduler.cancel(context, task.id)
                                                    timerRunning = false
                                                    remainMs = 0L
                                                } else if (TaskTimerScheduler.start(context, task) != null) {
                                                    timerRunning = true
                                                }
                                                moreMenuExpanded = false
                                            },
                                            leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) }
                                        )
                                    }
                                    if (task.isCompleted) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    task.completionQuality?.let { s.qualityLabel(it) }
                                                        ?: s.rateQuality,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                            },
                                            onClick = {
                                                moreMenuExpanded = false
                                                showQualityDialog = true
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.Star, contentDescription = null)
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Text(s.linkToGoal, style = MaterialTheme.typography.bodyLarge)
                                        },
                                        onClick = {
                                            moreMenuExpanded = false
                                            onOpenTaskLinks()
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.AccountTree, contentDescription = null)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(s.moreMenuMore, style = MaterialTheme.typography.bodyLarge)
                                        },
                                        onClick = { taskMenuPage = TaskMoreMenuPage.More },
                                        leadingIcon = {
                                            Icon(Icons.Default.MoreVert, contentDescription = null)
                                        },
                                        trailingIcon = {
                                            Icon(
                                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = null
                                            )
                                        }
                                    )
                                }
                                TaskMoreMenuPage.More -> {
                                    DropdownMenuItem(
                                        text = {
                                            Text(s.back, style = MaterialTheme.typography.bodyLarge)
                                        },
                                        onClick = { taskMenuPage = TaskMoreMenuPage.Root },
                                        leadingIcon = {
                                            Icon(
                                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                                contentDescription = null
                                            )
                                        }
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                    if (task.isGoal) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(s.goalSteps, style = MaterialTheme.typography.bodyLarge)
                                            },
                                            onClick = {
                                                moreMenuExpanded = false
                                                onOpenGoalSteps()
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.AccountTree, contentDescription = null)
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Text(s.takePhoto, style = MaterialTheme.typography.bodyLarge)
                                        },
                                        onClick = {
                                            moreMenuExpanded = false
                                            onTakePhoto()
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            val photoCount = appState.taskAttachments.count {
                                                it.taskId == task.id
                                            }
                                            Text(
                                                if (photoCount > 0) {
                                                    s.taskPhotosCount(photoCount)
                                                } else {
                                                    s.taskPhotos
                                                },
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        },
                                        onClick = {
                                            moreMenuExpanded = false
                                            onOpenPhotos()
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Image, contentDescription = null)
                                        }
                                    )
                                    if (!viewModel.isGoalByHub(task.columnId)) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (task.isGoal) s.unmarkAsGoal else s.markAsGoal,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        },
                                        onClick = {
                                            moreMenuExpanded = false
                                            viewModel.updateTask(task.copy(isGoal = !task.isGoal))
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Flag,
                                                contentDescription = null,
                                                tint = if (task.isGoal) {
                                                    inProgressMark()
                                                } else {
                                                    LocalContentColor.current
                                                }
                                            )
                                        }
                                    )
                                    }
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
                                                if (task.isHidden) {
                                                    Icons.Default.Visibility
                                                } else {
                                                    Icons.Default.VisibilityOff
                                                },
                                                contentDescription = null
                                            )
                                        }
                                    )
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
                                            text = {
                                                Text(
                                                    s.removeFromThisBoard,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                            },
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
                                        text = {
                                            Text(s.delete, style = MaterialTheme.typography.bodyLarge)
                                        },
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
                    }
                    }
                }
            }
            TaskCardProgressUnderIcons(
                cardType = task.cardType,
                cardPayload = task.cardPayload,
                onClick = if (linkPickMode || actionsLocked) null else onOpenCardBody
            )
            // Bottom strip: links, quadrant, then photos / related / due at the end.
            val hasLinkChips = linkNeighbors.isNotEmpty()
            val photoCount = remember(task.id, appState.taskAttachments) {
                appState.taskAttachments.count { it.taskId == task.id }
            }
            val quadrantBadge = if (compactEisenhower) {
                Eisenhower.getBadge(task.eisenhowerQuadrant).takeIf { it.isNotEmpty() }
            } else {
                null
            }
            if (!isDragPreview && !linkPickMode && (
                quadrantBadge != null ||
                    task.dueDate != null ||
                    task.deadlineAt != null ||
                    relatedBoards.isNotEmpty() ||
                    hasLinkChips ||
                    photoCount > 0
            )) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TaskBottomMetaHeight)
                        .padding(end = 2.dp)
                        .mutedWhen(actionsLocked),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Fixed slot so the links icon does not shift due / related when it appears.
                    Box(
                        modifier = Modifier.size(TaskRelatedCornerSize),
                            contentAlignment = Alignment.Center
                        ) {
                        if (hasLinkChips) {
                            TaskLinksButton(
                                neighbors = linkNeighbors,
                                onActivateFocus = onLinkChipClick,
                                onManageLinks = onOpenTaskLinks
                            )
                        }
                    }
                    if (quadrantBadge != null) {
                        Text(
                            text = quadrantBadge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                lineHeight = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (photoCount > 0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onOpenPhotos() }
                                    ) {
                                        Icon(
                                Icons.Default.Image,
                                contentDescription = s.taskPhotos,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                                        Text(
                                text = "$photoCount",
                                            style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                        if (relatedBoards.isNotEmpty()) {
                            RelatedBoardsButton(
                                links = relatedBoards,
                                onOpen = { link -> onOpenRelatedBoard(link.board.id, link.columnId) },
                            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                            background = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f),
                            size = TaskRelatedCornerSize
                        )
                    }
                    val dueBit = task.dueDate?.relativeDueLabel(
                        s,
                        dateLocale,
                        task.isCompleted,
                        reminderMinutesOfDay = task.startMinutesOfDay ?: task.reminderMinutesOfDay,
                        defaultReminderMinutes = prefs.reminderMinutesOfDay,
                        markOverdue = task.isPastDeadline()
                    )
                    val finishBit = task.deadlineAt?.takeUnless { finish ->
                        val start = task.dueDate
                        start != null && finish.isSameLocalDay(start)
                    }?.let { s.finishUntil(it.formatDateShort(dateLocale)) }
                    val dateLine = listOfNotNull(dueBit, finishBit).joinToString(" · ")
                    if (dateLine.isNotEmpty()) {
                                    Text(
                            text = dateLine,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            color = when {
                                compactOverdue -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                                        maxLines = 1,
                            softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                .widthIn(
                                    min = AlignedMoreButtonSize,
                                    max = if (finishBit != null) 168.dp else TaskDueLabelMaxWidth
                                )
                                .clickable { showDueEditor = true }
                        )
                    }
                                }
                            }
                        }
                    }
                }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskLinksButton(
    neighbors: List<TaskLinkNeighbor>,
    onActivateFocus: () -> Unit,
    onManageLinks: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    if (neighbors.isEmpty()) return
    val s = LocalAppStrings.current
    var expanded by remember { mutableStateOf(false) }
    val bg = Color.Transparent
    val iconSize = TaskRelatedCornerSize * 0.5f
    Box {
                        Box(
                            modifier = Modifier
                .size(TaskRelatedCornerSize)
                .clip(RoundedCornerShape(10.dp))
                .background(bg)
                .combinedClickable(
                    onClick = {
                        if (neighbors.size == 1) onActivateFocus()
                        else expanded = true
                    },
                    onLongClick = onManageLinks
                ),
                            contentAlignment = Alignment.Center
                            ) {
                                Icon(
                imageVector = Icons.Default.AccountTree,
                contentDescription = s.linkToGoal,
                modifier = Modifier.size(iconSize.coerceAtLeast(10.dp)),
                tint = tint
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            neighbors.forEach { neighbor ->
                DropdownMenuItem(
                    text = {
                            Text(
                            text = if (neighbor.isParent) {
                                s.parentChip(neighbor.title)
                            } else {
                                s.childrenChipLabel(neighbor.title)
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        expanded = false
                        onActivateFocus()
                    }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(s.linkToGoal) },
                onClick = {
                    expanded = false
                    onManageLinks()
                },
                leadingIcon = {
                    Icon(Icons.Default.AccountTree, contentDescription = null)
                }
            )
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
                    DialogMarqueeText(
                        text = s.comments,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        fillMaxWidth = false,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                DialogMarqueeText(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
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
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(buttonFill())
                        .border(1.dp, deleteButtonColor(), RoundedCornerShape(10.dp))
                                            .clickable { onDeleteComment(comment.id) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = s.deleteComment,
                                            tint = deleteButtonColor(),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                DialogSectionDivider()

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
                        shape = RoundedCornerShape(10.dp)
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
                            .background(buttonFill(), RoundedCornerShape(10.dp))
                            .border(1.dp, buttonLine(), RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = s.send,
                            tint = buttonLabel(),
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
    var showSystemRules by remember { mutableStateOf(false) }
    val systemRules = remember(comments) { comments.filter { isSystemHubRule(it.text) } }
    val userComments = remember(comments) { comments.filterNot { isSystemHubRule(it.text) } }

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
                    DialogMarqueeText(
                        text = s.hubRules,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        fillMaxWidth = false,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                DialogMarqueeText(
                    text = s.hubWithName(s.localized(column.title)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                if (systemRules.isNotEmpty()) {
                    TextButton(
                        onClick = { showSystemRules = !showSystemRules },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (showSystemRules) s.hubSystemRulesHide else s.hubSystemRulesShow,
                            maxLines = 1
                        )
                    }
                    if (showSystemRules) {
                        systemRules.forEach { comment ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = s.hubSystemRules,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = comment.text,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                if (userComments.isEmpty()) {
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
                        items(userComments, key = { it.id }) { comment ->
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
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(buttonFill())
                        .border(1.dp, deleteButtonColor(), RoundedCornerShape(10.dp))
                                            .clickable { onDeleteComment(comment.id) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = s.deleteComment,
                                            tint = deleteButtonColor(),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                DialogSectionDivider()

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
                        shape = RoundedCornerShape(10.dp)
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
                            .background(buttonFill(), RoundedCornerShape(10.dp))
                            .border(1.dp, buttonLine(), RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = s.send,
                            tint = buttonLabel(),
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
                    DialogMarqueeText(
                        text = if (isCopyMode) s.copyTask else s.moveTask,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        fillMaxWidth = false,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                DialogMarqueeText(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
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
                        shape = RoundedCornerShape(10.dp),
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
                            DialogMarqueeText(
                                text = s.move,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (!isCopyMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isCopyMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    Surface(
                        onClick = { isCopyMode = true },
                        shape = RoundedCornerShape(10.dp),
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
                            DialogMarqueeText(
                                text = s.copy,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isCopyMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCopyMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
                if (isCopyMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DialogMarqueeText(
                        text = s.alsoShowHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3
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
                                    DialogMarqueeText(
                                        text = s.localized(prevColumn.title),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        fillMaxWidth = false,
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
                                        containerColor = buttonFill(),
                                        contentColor = buttonLabel()
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    DialogMarqueeText(
                                        text = s.localized(nextColumn.title),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        fillMaxWidth = false,
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
                        DialogSectionDivider()
                    }
                    DialogMarqueeText(
                        text = if (isCopyMode) s.copyToHubOnBoard else s.hubsOnThisBoard,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
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
                            DialogMarqueeText(
                                text = s.localized(col.title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isCurrent && !isCopyMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent && !isCopyMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                fillMaxWidth = false,
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
                        DialogSectionDivider()
                        DialogMarqueeText(
                            text = if (isCopyMode) s.copyToOtherBoard else s.orOtherBoard,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
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
                                        DialogMarqueeText(
                                            text = s.localized(targetBoard.name),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2
                                        )
                                        if (parentProject != null) {
                                            DialogMarqueeText(
                                                text = parentProject.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                maxLines = 1
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
                                            DialogSectionDivider()
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
                                                DialogMarqueeText(
                                                    text = s.localized(hub.title),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (alreadyHere && !isCopyMode) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    },
                                                    maxLines = 2,
                                                    fillMaxWidth = false,
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
                DialogMarqueeText(
                    text = s.hubOrder,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    fillMaxWidth = false,
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
                                DialogMarqueeText(
                                    text = s.localized(col.title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    fillMaxWidth = false,
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

/** Graphite circle used for done and not-done. The sign stays optically centered. */
@Composable
private fun GraphiteCircleButton(
    onClick: () -> Unit,
    contentDescription: String,
    icon: ImageVector,
    tint: Color = buttonLabel(),
    borderColor: Color = buttonLine(),
    fill: Color = buttonFill()
) {
    Box(
        modifier = Modifier
            .size(TaskActionButtonSize)
            .clip(CircleShape)
            .background(fill)
            .border(1.5.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Shared ⋮ control for board / hub / task.
 * Button slot is always [AlignedMoreButtonSize]; popup is end-aligned to that slot so every
 * open menu shares the same trailing X (stacked under each other).
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
    val sheetScroll = rememberScrollState()
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
            ModalBottomSheet(
                onDismissRequest = { onExpandedChange(false) },
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp,
                dragHandle = {
                    BottomSheetDefaults.DragHandle(
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .verticalScroll(sheetScroll)
                        .padding(bottom = 12.dp),
                    content = content
                )
            }
        }
    }
}

/** Board focus id for tasks that are not in a visible group. */
private const val UngroupedFocusId = "\u0000ungrouped"

/** Pins the ⋮ glyph in a fixed slot shared by the board, hub, and task rows. */
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
/** Leading badge icons (goal flag / recurring), stacked in the selection column. */
private val TaskLeadingBadgeSize = 16.dp
private val TaskLeadingStackGap = 2.dp
/** Always reserved for bolt+check so title width matches lite peek and settle fade. */
private val TaskActionsClusterWidth = TaskActionButtonSize * 2 + TaskActionsClusterGap

private fun taskActionsClusterWidth(showTimer: Boolean) =
    if (showTimer) TaskActionsClusterWidth else TaskActionsClusterWidth
/** Fixed leading slot for selection check — empty when not selecting so cards do not shift. */
private val TaskSelectionCheckSlot = 22.dp
/**
 * Same thought-cloud as the title bubble that opens on tap: rounded body and three
 * shrinking circles. The tail is smaller and sits in the bottom-right, clear of the content.
 */
private object TaskCloudShape : Shape {
    @Suppress("UNUSED_PARAMETER")
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): Outline {
        val strokeInset = with(density) { (TaskCloudStroke / 2).toPx() }
        val radius = with(density) { 16.dp.toPx() }
        val tailReserve = with(density) { TaskCloudTailReserve.toPx() }
        val bodyBottom = (size.height - tailReserve).coerceAtLeast(radius * 2 + strokeInset)
        val body = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(
                        left = strokeInset,
                        top = strokeInset,
                        right = size.width - strokeInset,
                        bottom = bodyBottom
                    ),
                    cornerRadius = CornerRadius(radius)
                )
            )
        }
        val r1 = with(density) { 4.2.dp.toPx() }
        val r2 = with(density) { 2.8.dp.toPx() }
        val r3 = with(density) { 1.7.dp.toPx() }
        val stepX = with(density) { 4.2.dp.toPx() }
        val c1 = Offset(
            x = size.width - strokeInset - with(density) { 11.dp.toPx() },
            y = bodyBottom - with(density) { 1.dp.toPx() }
        )
        val c2 = Offset(
            x = c1.x + stepX,
            y = c1.y + with(density) { 5.2.dp.toPx() }
        )
        val c3 = Offset(
            x = c2.x + with(density) { 3.4.dp.toPx() },
            y = c2.y + with(density) { 4.4.dp.toPx() }
        )
        fun oval(center: Offset, bubbleRadius: Float) = Path().apply {
            addOval(
                Rect(
                    left = center.x - bubbleRadius,
                    top = center.y - bubbleRadius,
                    right = center.x + bubbleRadius,
                    bottom = center.y + bubbleRadius
                )
            )
        }
        fun union(a: Path, b: Path): Path {
            val out = Path()
            out.op(a, b, PathOperation.Union)
            return out
        }
        return Outline.Generic(union(union(union(body, oval(c1, r1)), oval(c2, r2)), oval(c3, r3)))
    }
}

/** Space under the card body for the thought-cloud tail and its shadow. */
private val TaskCloudTailReserve = 16.dp
/** Extra inset so dates and buttons stay above the tail circles and the stroke. */
private val TaskCloudContentClearance = 6.dp
/** Keeps the trailing buttons and the date off the stroke and the tail. */
private val TaskCloudEndPad = 8.dp
private val TaskCloudStroke = 4.25.dp
/** Keeps the thicker stroke from reaching text and buttons. */
private val TaskCloudFrameGuard = 2.dp
private val TaskCloudShadowOffset = 2.5.dp

@Composable
private fun taskCloudShadowColor(): Color {
    val darkUi = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return Color.Black.copy(alpha = if (darkUi) 0.42f else 0.16f)
}

private fun DrawScope.drawTaskCloudShadow(color: Color) {
    val outline = TaskCloudShape.createOutline(size, layoutDirection, this)
    val path = (outline as? Outline.Generic)?.path ?: return
    val shadow = Path().apply { addPath(path) }
    shadow.translate(Offset(0f, TaskCloudShadowOffset.toPx()))
    drawPath(shadow, color)
}

/** Shadow sits under the cloud, inside the card, so it does not tint text or buttons. */
private fun Modifier.taskCloudDropShadow(color: Color): Modifier = drawBehind {
    drawTaskCloudShadow(color)
}

/**
 * Done / not-done fold header. Shown while paging too, so the title does not pop in
 * when the hub settles. Clicks stay off until the page stops.
 */
@Composable
private fun HubFoldSectionHeader(
    title: String,
    expanded: Boolean,
    interactive: Boolean,
    dimmed: Boolean = false,
    menuOpen: Boolean,
    onToggle: () -> Unit,
    onMenuClick: () -> Unit,
    onMenuDismiss: () -> Unit,
    onClearAll: () -> Unit,
    rowVerticalPadding: Dp,
    titleVerticalPadding: Dp = 0.dp,
) {
    val s = LocalAppStrings.current
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    Spacer(modifier = Modifier.height(8.dp))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .mutedWhen(dimmed),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = rowVerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .then(if (interactive) Modifier.clickable(onClick = onToggle) else Modifier)
                    .padding(vertical = titleVerticalPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (expanded) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = if (expanded) s.hide else s.show,
                    modifier = Modifier.size(24.dp),
                    tint = labelColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = labelColor
                )
            }
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                if (interactive) {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = s.statsMore,
                            tint = labelColor
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = onMenuDismiss
                    ) {
                        DropdownMenuItem(
                            text = { Text(s.clearAll) },
                            onClick = onClearAll
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = null,
                        tint = labelColor
                    )
                }
            }
        }
    }
}

/** Gap between title / actions / ⋮ in TaskCard Row (Arrangement.spacedBy). */
private val TaskRowTrailingGap = 4.dp
/** Settle slide shared by task actions and group icons. */
private const val TrailingRevealMillis = 400
/** Distance the task action strip travels; group icons match this speed. */
private val TaskTrailingRevealDistance =
    TaskActionsClusterWidth + TaskRowTrailingGap + AlignedMoreButtonSize
/** Due / related / task-link bottom meta strip — fixed height so lite→full does not jump. */
private val TaskDueUnderMoreHeight = 16.dp
private val TaskRelatedCornerSize = 22.dp
private val TaskBottomMetaHeight = TaskRelatedCornerSize
/** Equal top/bottom pad around the button row. */
private val TaskCardRowVerticalPad = 6.dp
/** Content row height = action buttons exactly. */
private val TaskCardRowContentMinHeight = TaskActionButtonSize
/** Due label may be wider than ⋮ so text is not ellipsized away. */
private val TaskDueLabelMaxWidth = 108.dp
/** Related-board / task-link control in the bottom meta strip (does not shift title). */
@Composable
private fun TaskTrailingWidthReserve(showTimer: Boolean = false) {
    Box(
        modifier = Modifier
            .width(taskActionsClusterWidth(showTimer))
            .height(TaskActionButtonSize)
    )
    Box(
        modifier = Modifier
            .width(AlignedMoreButtonSize)
            .height(TaskActionButtonSize)
    )
}

private val HubGroupHairline = 3.dp
private val HubGroupCapLine = 1.dp
private val HubGroupCapLight = Color(0xFF708090)
/** Champagne highlight so marked group icons stay visible on white. */
private val HubGroupGleam = listOf(
    Color(0xFF8A4010),
    Color(0xFFF0C14A),
    Color(0xFFE4C16A),
    Color(0xFFF6D15A),
    Color(0xFFC37C22)
)

@Composable
private fun taskFrameBrush(): Brush = SolidColor(MaterialTheme.colorScheme.outline)

/** Thin slate rule around a group title row. */
@Composable
internal fun Modifier.hubGroupCapBackground(): Modifier {
    return border(HubGroupCapLine, HubGroupCapLight, RoundedCornerShape(10.dp))
}

/** Slate rule down the left of a hub group, same color as the title border. */
internal fun Modifier.hubGroupPlate(@Suppress("UNUSED_PARAMETER") dark: Boolean): Modifier {
    return this.drawBehind {
        val stroke = HubGroupHairline.toPx()
        drawRect(
            color = HubGroupCapLight,
            size = androidx.compose.ui.geometry.Size(stroke, size.height)
        )
    }
}

/** Champagne gleam for group action icons. */
internal fun Modifier.hubGroupGoldFill(): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.horizontalGradient(
                HubGroupGleam,
                startX = 0f,
                endX = size.width
            ),
            blendMode = BlendMode.SrcIn
        )
    }

/** Diagonal gleam so a thin + or × crosses more than one gold. */
internal fun Modifier.hubGroupMarkGoldFill(): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.linearGradient(
                listOf(
                    Color(0xFF8A4010),
                    Color(0xFFF0C14A),
                    Color(0xFFFFF6C8),
                    Color(0xFFF6D15A),
                    Color(0xFFC37C22)
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            ),
            blendMode = BlendMode.SrcIn
        )
    }

private val HubGroupPlateIconSlot = 48.dp
private val HubGroupPlateActionsWidth = HubGroupPlateIconSlot * 4

/** Focus / rename / add / ungroup. Same slide speed as the task action strip. */
@Composable
private fun HubGroupPlateActions(
    revealToken: Int,
    onFocus: () -> Unit,
    onRename: () -> Unit,
    onAdd: () -> Unit,
    onUngroup: () -> Unit,
) {
    val s = LocalAppStrings.current
    val reveal = remember(revealToken) {
        Animatable(if (revealToken > 0) 0f else 1f)
    }
    val density = LocalDensity.current
    val slidePx = with(density) { HubGroupPlateActionsWidth.toPx() }
    val revealMillis = (
        TrailingRevealMillis * HubGroupPlateActionsWidth.value / TaskTrailingRevealDistance.value
    ).toInt().coerceAtLeast(1)
    LaunchedEffect(revealToken) {
        if (revealToken > 0) {
            reveal.snapTo(0f)
            reveal.animateTo(
                1f,
                animationSpec = tween(durationMillis = revealMillis, easing = FastOutSlowInEasing)
            )
        } else {
            reveal.snapTo(1f)
        }
    }
    Box(
        modifier = Modifier
            .width(HubGroupPlateActionsWidth)
            .height(HubGroupPlateIconSlot)
            .clipToBounds()
    ) {
        Row(
            modifier = Modifier.graphicsLayer {
                translationX = (1f - reveal.value) * slidePx
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            HubGroupPlateIconButton(
                onClick = onFocus,
                icon = Icons.Default.OpenInFull,
                contentDescription = s.hubGroupFocus
            )
            HubGroupPlateIconButton(
                onClick = onRename,
                icon = Icons.Default.Edit,
                contentDescription = s.hubGroupRename
            )
            HubGroupPlateIconButton(
                onClick = onAdd,
                icon = Icons.Default.Add,
                contentDescription = s.addToGroup,
                marked = true
            )
            HubGroupPlateIconButton(
                onClick = onUngroup,
                icon = Icons.Default.Close,
                contentDescription = s.ungroupTasks,
                marked = true
            )
        }
    }
}

@Composable
private fun HubGroupPlateIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    marked: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(HubGroupPlateIconSlot)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(24.dp)
                .then(if (marked) Modifier.hubGroupMarkGoldFill() else Modifier.hubGroupGoldFill()),
            tint = Color.White
        )
    }
}

@Composable
private fun GroupFocusTopBar(
    title: String,
    onClose: () -> Unit,
) {
    val s = LocalAppStrings.current
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(BoardTopBarContentHeight)
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = LiterataBold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    lineHeight = 26.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = s.hubGroupFocusExit,
                    modifier = Modifier.hubGroupGoldFill(),
                    tint = Color.White
                )
            }
        }
    }
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
                RoundedCornerShape(10.dp)
            )
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                RoundedCornerShape(10.dp)
            )
    )
}

/** Peek stand-in for TaskCard — same title block, trailing slots, and bottom meta height. */
@Composable
private fun LiteTaskRow(
    title: String,
    eisenhowerQuadrant: String? = null,
    dueDate: Long? = null,
    isInProgress: Boolean = false,
    isCompleted: Boolean = false,
    isGoal: Boolean = false,
    isRecurring: Boolean = false,
    hasDeadline: Boolean = false,
    hasRelated: Boolean = false,
    hasTaskLinks: Boolean = false,
    hasPhotos: Boolean = false,
    hasTimer: Boolean = false,
    cardType: String = TaskCardKind.PLAIN,
    cardPayload: String = ""
) {
    val onColor = when {
        isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TaskCardRowContentMinHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TaskRowTrailingGap)
        ) {
            TaskLeadingMarks(
                selectionMode = false,
                isSelected = false,
                onToggleSelected = {},
                isRecurring = isRecurring,
                isGoal = isGoal,
                hasTimer = hasTimer,
                recurringDescription = null,
                goalDescription = null,
                timerDescription = null,
                iconTint = onColor.copy(alpha = 0.75f),
                goalTint = inProgressMark(),
                selectionStroke = inProgressColor
            )
            TaskCardTitleBlock(
                title = title,
                titleColor = onColor.copy(alpha = if (isCompleted) 0.55f else 1f),
                titleDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                maxLines = Int.MAX_VALUE
            )
            // Same slots as TaskCard (actions cluster + ⋮) so title width never jumps.
            TaskTrailingWidthReserve(showTimer = hasTimer)
        }
        TaskCardProgressUnderIcons(
            cardType = cardType,
            cardPayload = cardPayload
        )
        if (
            dueDate != null ||
            hasDeadline ||
            hasRelated ||
            hasTaskLinks ||
            hasPhotos ||
            eisenhowerQuadrant != null
        ) {
            Spacer(modifier = Modifier.height(TaskBottomMetaHeight))
        }
    }
}

/**
 * Title line shared by lite and full cards so a hub settle does not change row height.
 */
@Composable
private fun RowScope.TaskCardTitleBlock(
    title: String,
    titleColor: Color,
    titleDecoration: TextDecoration?,
    maxLines: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
            maxLines = maxLines,
            softWrap = true,
            overflow = TextOverflow.Ellipsis,
            textDecoration = titleDecoration,
            color = titleColor,
            modifier = modifier
        )
    }
}

/**
 * Progress sits under the title row, inset past the leading and trailing icon slots,
 * so its tap target does not cover those buttons.
 */
@Composable
private fun TaskCardProgressUnderIcons(
    cardType: String,
    cardPayload: String,
    onClick: (() -> Unit)? = null
) {
    val progress = taskCardProgress(cardType, cardPayload) ?: return
    TaskCardProgressLine(
        label = progress.first,
        fraction = progress.second,
        onClick = onClick,
        modifier = Modifier.padding(
            start = TaskSelectionCheckSlot + TaskRowTrailingGap,
            end = TaskTrailingRevealDistance + TaskRowTrailingGap,
            top = 2.dp,
            bottom = 2.dp
        )
    )
}

/**
 * Selection mark, recurring, and goal share one column so the title keeps the rest of the row.
 * The column width stays put, so entering selection or showing a badge does not shift the text.
 */
@Composable
private fun TaskLeadingMarks(
    selectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelected: () -> Unit,
    isRecurring: Boolean,
    isGoal: Boolean,
    hasTimer: Boolean,
    recurringDescription: String?,
    goalDescription: String?,
    timerDescription: String?,
    iconTint: Color,
    goalTint: Color,
    selectionStroke: Color,
    idleCheckTint: Color? = null
) {
    val hasMark = selectionMode || isRecurring || isGoal
    Box(
        modifier = Modifier
            .width(TaskSelectionCheckSlot)
            .heightIn(min = TaskActionButtonSize)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TaskLeadingStackGap)
        ) {
            if (selectionMode) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(TaskSelectionCheckSlot)
                            .clip(CircleShape)
                            .background(selectionStroke)
                            .clickable(onClick = onToggleSelected),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = idleCheckTint
                            ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .size(TaskSelectionCheckSlot)
                            .clickable(onClick = onToggleSelected)
                    )
                }
            }
            if (isRecurring) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = recurringDescription,
                    tint = iconTint,
                    modifier = Modifier.size(TaskLeadingBadgeSize)
                )
            }
            if (isGoal) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = goalDescription,
                    tint = goalTint,
                    modifier = Modifier.size(TaskLeadingBadgeSize)
                )
            }
        }
        if (hasTimer) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = timerDescription,
                tint = iconTint,
                modifier = Modifier
                    .align(if (hasMark) Alignment.TopCenter else Alignment.Center)
                    .size(12.dp)
            )
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
    val badge = if (!isCompleted) Eisenhower.getBadge(eisenhowerQuadrant) else ""
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (badge.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TaskBottomMetaHeight),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.size(TaskRelatedCornerSize))
                    Text(
                        text = badge,
                        modifier = Modifier.padding(start = 6.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            lineHeight = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun EisenhowerPriorityEditor(
    enabled: Boolean,
    quadrant: String?,
    onEnabledChange: (Boolean) -> Unit,
    onQuadrantChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    showToggle: Boolean = true
) {
    val s = LocalAppStrings.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showToggle) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = s.eisenhowerButtons,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
        }
        if (!showToggle || enabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(Eisenhower.Q1, Eisenhower.Q2, Eisenhower.Q3, Eisenhower.Q4).forEach { q ->
                    EisenhowerQuadrantChip(
                        qKey = q,
                        qLabel = Eisenhower.getBadge(q),
                        isSelected = quadrant == q,
                        dimUnselected = quadrant != null && quadrant != q,
                        onClick = { onQuadrantChange(if (quadrant == q) null else q) }
                    )
                }
            }
        }
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
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
                .alpha(fillAlpha)
        )
        if (isSelected || eisenhowerPlain()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = qColor.copy(alpha = if (isSelected) 0.9f else 0.72f),
                        shape = RoundedCornerShape(10.dp)
                    )
            )
        }
        Text(
            text = qLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (isSelected) eisenhowerOnColor(qKey) else qColor.copy(alpha = labelAlpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
        )
    }
}
