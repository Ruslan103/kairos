package com.example.kaizenkanban.ui.projects

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.core.content.FileProvider
import com.example.kaizenkanban.data.transfer.KairosTransferHelper
import com.example.kaizenkanban.data.local.KairosPreferences
import com.example.kaizenkanban.domain.model.Board
import com.example.kaizenkanban.domain.model.BoardTemplate
import com.example.kaizenkanban.domain.model.Column
import com.example.kaizenkanban.domain.model.Contact
import com.example.kaizenkanban.domain.model.Project
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.ui.calendar.DueDateQuickPick
import com.example.kaizenkanban.ui.calendar.formatDateShort
import com.example.kaizenkanban.ui.calendar.isDueToday
import com.example.kaizenkanban.ui.calendar.isOverdueDate
import com.example.kaizenkanban.ui.calendar.localMillisToUtcPicker
import com.example.kaizenkanban.ui.calendar.relativeDueLabel
import com.example.kaizenkanban.ui.calendar.startOfLocalDayMillis
import com.example.kaizenkanban.ui.calendar.utcPickerMillisToLocalNoon
import com.example.kaizenkanban.ui.i18n.AppLanguage
import com.example.kaizenkanban.ui.i18n.AppStrings
import com.example.kaizenkanban.ui.i18n.KanbanNames
import com.example.kaizenkanban.ui.i18n.LocalAppLanguage
import com.example.kaizenkanban.ui.i18n.LocalAppStrings
import com.example.kaizenkanban.ui.onboarding.OnboardingDialog
import com.example.kaizenkanban.ui.resolveQuickAddTarget
import com.example.kaizenkanban.ui.sanitizeQuickAddPrefs
import com.example.kaizenkanban.ui.theme.deleteButtonColor
import com.example.kaizenkanban.ui.theme.deleteButtonGradient
import com.example.kaizenkanban.ui.theme.newProjectGradient
import com.example.kaizenkanban.ui.viewmodel.SharedViewModel
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: SharedViewModel,
    onNavigateToBoard: (boardId: String, columnId: String?) -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    consumePendingQuickAdd: Boolean = true
) {
    val state by viewModel.state.collectAsState()
    val pendingQuickAdd by viewModel.pendingQuickAdd.collectAsState()
    
    var isAddingProject by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    var isQuickAdding by remember { mutableStateOf(false) }
    var quickAddTitle by remember { mutableStateOf("") }
    var quickAddDueDate by remember { mutableStateOf<Long?>(null) }
    var showQuickAddDatePicker by remember { mutableStateOf(false) }

    var projectToRename by remember { mutableStateOf<Project?>(null) }
    var renameProjectName by remember { mutableStateOf("") }

    var isAddingBoardToProject by remember { mutableStateOf<String?>(null) }
    var newBoardName by remember { mutableStateOf("") }
    var newBoardTemplate by remember { mutableStateOf(BoardTemplate.INBOX) }
    
    var boardToRename by remember { mutableStateOf<Board?>(null) }
    var renameBoardName by remember { mutableStateOf("") }
    var exportProjectId by remember { mutableStateOf<String?>(null) }

    var contactDialogProjectId by remember { mutableStateOf<String?>(null) }
    var contactToEdit by remember { mutableStateOf<Contact?>(null) }
    var overviewPanel by remember { mutableStateOf<OverviewPanel?>(null) }
    var taskSearchQuery by remember { mutableStateOf("") }
    var taskFilter by remember { mutableStateOf(TaskOverviewFilter.ACTIVE) }
    var pendingDelete by remember { mutableStateOf<PendingDelete?>(null) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var projectsMenuExpanded by remember { mutableStateOf(false) }
    var globalSearchOpen by remember { mutableStateOf(false) }
    var notifBannerDismissed by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val s = LocalAppStrings.current
    val dateLocale = LocalAppLanguage.current.locale
    val kairosPrefs = remember { KairosPreferences(context) }
    var showArchivedBoards by remember { mutableStateOf(kairosPrefs.showArchivedBoards) }
    var focusTaskId by remember { mutableStateOf(kairosPrefs.inProgressTaskId) }
    var projectsCollapsed by remember { mutableStateOf(kairosPrefs.projectsCollapsed) }
    var showOnboarding by remember { mutableStateOf(!kairosPrefs.hasSeenOnboarding) }
    var notificationsGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsGranted = granted
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val expandedProjects = remember { mutableStateMapOf<String, Boolean>() }
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                showArchivedBoards = kairosPrefs.showArchivedBoards
                notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(state.tasks) {
        focusTaskId = kairosPrefs.inProgressTaskId
    }
    LaunchedEffect(pendingQuickAdd, consumePendingQuickAdd) {
        if (consumePendingQuickAdd && pendingQuickAdd) {
            isQuickAdding = true
            viewModel.clearPendingQuickAdd()
        }
    }
    fun completeTaskWithUndo(task: Task) {
        coroutineScope.launch {
            val markedDone = viewModel.completeOrReopen(task)
            if (markedDone) {
                if (focusTaskId == task.id) {
                    focusTaskId = null
                    kairosPrefs.inProgressTaskId = null
                }
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
    val focusTask = remember(state.tasks, focusTaskId) {
        focusTaskId?.let { id -> state.tasks.find { it.id == id && !it.isCompleted } }
    }
    val density = LocalDensity.current
    var draggedProjectId by remember { mutableStateOf<String?>(null) }
    var draggedOffsetY by remember { mutableStateOf(0f) }
    val reorderThreshold = with(density) { 68.dp.toPx() }
    val sortedProjects = remember(state.projects) { state.projects.sortedBy { it.position } }
    val overviewTaskCount = remember(state.tasks) { state.tasks.size }
    val overviewHubCount = remember(state.columns) { state.columns.size }
    val overviewBoardCount = remember(state.boards, showArchivedBoards) {
        state.boards.count { showArchivedBoards || !it.isArchived }
    }
    val needOverviewLists = overviewPanel != null || globalSearchOpen
    val allTasks = remember(state.tasks, needOverviewLists) {
        if (!needOverviewLists) emptyList()
        else state.tasks.sortedWith(
            compareBy<Task> { it.isCompleted }
                .thenBy { it.dueDate ?: Long.MAX_VALUE }
                .thenByDescending { it.createdAt }
        )
    }
    val filteredAllTasks = remember(allTasks, taskSearchQuery, taskFilter, needOverviewLists) {
        if (!needOverviewLists) emptyList()
        else {
            val query = taskSearchQuery.trim()
            allTasks.filter { task ->
                val matchesQuery = query.isEmpty() || task.title.contains(query, ignoreCase = true)
                val matchesFilter = when (taskFilter) {
                    TaskOverviewFilter.ACTIVE -> !task.isCompleted
                    TaskOverviewFilter.TODAY -> !task.isCompleted && task.dueDate != null && task.dueDate.isDueToday()
                    TaskOverviewFilter.OVERDUE -> !task.isCompleted && task.dueDate != null && task.dueDate.isOverdueDate()
                    TaskOverviewFilter.DONE -> task.isCompleted
                    TaskOverviewFilter.ALL -> true
                }
                matchesQuery && matchesFilter
            }
        }
    }
    val allHubs = remember(state.columns, state.boards, needOverviewLists) {
        if (!needOverviewLists) emptyList()
        else state.columns.sortedWith(
            compareBy<Column> { col -> state.boards.find { it.id == col.boardId }?.name.orEmpty() }
                .thenBy { it.position }
        )
    }
    val allBoardsList = remember(state.boards, state.projects, showArchivedBoards, needOverviewLists) {
        if (!needOverviewLists) emptyList()
        else state.boards
            .filter { showArchivedBoards || !it.isArchived }
            .sortedWith(
                compareBy<Board> { board -> state.projects.find { it.id == board.projectId }?.name.orEmpty() }
                    .thenBy { it.name }
            )
    }

    // Export launcher (CreateDocument)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val projectId = exportProjectId
                    val json = if (projectId != null) {
                        viewModel.exportProjectToJson(projectId)
                    } else {
                        viewModel.exportToJson()
                    }
                    exportProjectId = null
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.bufferedWriter().use { it.write(json) }
                    }
                    Toast.makeText(context, s.exportSuccess, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    exportProjectId = null
                    Toast.makeText(context, s.saveError(e.localizedMessage), Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            exportProjectId = null
        }
    }

    // Import launcher (OpenDocument)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader().readText()
                    } ?: return@launch
                    val count = viewModel.importFromJson(text)
                    Toast.makeText(context, s.importSuccess(count), Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, s.importError(e.localizedMessage), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Share via messenger
    fun shareViaMessenger(projectId: String? = null) {
        coroutineScope.launch {
            try {
                val data = if (projectId != null) {
                    viewModel.exportProjectData(projectId)
                } else {
                    viewModel.exportData()
                }
                val json = KairosTransferHelper.toJson(data)
                val summaryText = KairosTransferHelper.createSummaryText(data)

                val cacheFile = File(context.cacheDir, if (projectId != null) "kairos_project.kairos" else "kairos_tasks.kairos")
                cacheFile.writeText(json)
                val contentUri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    cacheFile
                )

                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_TEXT, summaryText)
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(sendIntent, s.shareTasksVia)
                context.startActivity(chooser)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, s.sharePrepareError(e.localizedMessage), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val weekStartMs = remember { startOfLocalDayMillis() - 6L * 86_400_000L }
    val weekCompletedCount = remember(state.tasks, weekStartMs) {
        state.tasks.count { task ->
            task.isCompleted && (task.completedAt ?: 0L) >= weekStartMs
        }
    }
    val weekCreatedCount = remember(state.tasks, weekStartMs) {
        state.tasks.count { it.createdAt >= weekStartMs }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 8.dp, top = 2.dp, bottom = 2.dp)
            ) {
                Text(
                    text = s.myProjects,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            globalSearchOpen = !globalSearchOpen
                            if (globalSearchOpen) {
                                overviewPanel = OverviewPanel.Tasks
                            } else {
                                taskSearchQuery = ""
                                if (overviewPanel == OverviewPanel.Tasks) {
                                    overviewPanel = null
                                }
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            if (globalSearchOpen) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = s.searchTasks,
                            tint = if (globalSearchOpen || taskSearchQuery.isNotBlank()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { projectsMenuExpanded = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = s.moreActions,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = projectsMenuExpanded,
                            onDismissRequest = { projectsMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(s.settings) },
                                onClick = {
                                    projectsMenuExpanded = false
                                    onNavigateToSettings()
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(s.calendar) },
                                onClick = {
                                    projectsMenuExpanded = false
                                    onNavigateToCalendar()
                                },
                                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(s.allTasksHeader(overviewTaskCount)) },
                                onClick = {
                                    projectsMenuExpanded = false
                                    overviewPanel = OverviewPanel.Tasks
                                },
                                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(s.allHubsHeader(overviewHubCount)) },
                                onClick = {
                                    projectsMenuExpanded = false
                                    overviewPanel = OverviewPanel.Hubs
                                },
                                leadingIcon = { Icon(Icons.Default.ViewWeek, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(s.allBoardsHeader(overviewBoardCount)) },
                                onClick = {
                                    projectsMenuExpanded = false
                                    overviewPanel = OverviewPanel.Boards
                                },
                                leadingIcon = { Icon(Icons.Default.GridView, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (showArchivedBoards) s.hideArchivedBoards else s.showArchivedBoards
                                    )
                                },
                                onClick = {
                                    projectsMenuExpanded = false
                                    showArchivedBoards = !showArchivedBoards
                                    kairosPrefs.showArchivedBoards = showArchivedBoards
                                },
                                leadingIcon = {
                                    Icon(
                                        if (showArchivedBoards) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(s.shareViaMessenger) },
                                onClick = {
                                    projectsMenuExpanded = false
                                    shareViaMessenger()
                                },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(s.exportFile) },
                                onClick = {
                                    projectsMenuExpanded = false
                                    exportProjectId = null
                                    exportLauncher.launch("kairos_backup.json")
                                },
                                leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(s.importFile) },
                                onClick = {
                                    projectsMenuExpanded = false
                                    showImportConfirm = true
                                },
                                leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null) }
                            )
                        }
                    }
                }
                if (globalSearchOpen) {
                    OutlinedTextField(
                        value = taskSearchQuery,
                        onValueChange = {
                            taskSearchQuery = it
                            overviewPanel = OverviewPanel.Tasks
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        singleLine = true,
                        placeholder = { Text(s.searchProjectsPlaceholder) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                Text(
                    text = "${s.weekStats}: ${s.weekCompleted(weekCompletedCount)} · ${s.weekCreated(weekCreatedCount)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 220.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), CircleShape)
                        .clickable { isQuickAdding = true }
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = s.quickAddTask,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            s.quickAddTask,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .widthIn(max = 220.dp)
                        .shadow(10.dp, CircleShape)
                        .clip(CircleShape)
                        .background(newProjectGradient)
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                        .clickable { isAddingProject = true }
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = s.createProject,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            s.newProject,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!notificationsGranted && !notifBannerDismissed) {
                item(key = "notif-banner") {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = s.notificationsDeniedBanner,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notifPermissionLauncher.launch(
                                                Manifest.permission.POST_NOTIFICATIONS
                                            )
                                        } else {
                                            val intent = Intent(
                                                Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                            ).apply {
                                                putExtra(
                                                    Settings.EXTRA_APP_PACKAGE,
                                                    context.packageName
                                                )
                                            }
                                            context.startActivity(intent)
                                        }
                                    }
                                ) {
                                    Text(s.enableNotifications)
                                }
                                TextButton(
                                    onClick = {
                                        val intent = Intent(
                                            Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                        ).apply {
                                            putExtra(
                                                Settings.EXTRA_APP_PACKAGE,
                                                context.packageName
                                            )
                                        }
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Text(s.openSystemNotifications)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(onClick = { notifBannerDismissed = true }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = s.dismissBanner
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (focusTask != null) {
                item(key = "focus-task") {
                    val column = state.columns.find { it.id == focusTask.columnId }
                    Surface(
                        onClick = {
                            column?.boardId?.let { onNavigateToBoard(it, column.id) }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = s.focusTask,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = s.focusTask,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = focusTask.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { completeTaskWithUndo(focusTask) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = s.markDone,
                                    tint = Color(0xFF00C853),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            TextButton(onClick = {
                                focusTaskId = null
                                kairosPrefs.inProgressTaskId = null
                            }) {
                                Text(s.clearFocus, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            if (sortedProjects.isEmpty()) {
                item(key = "empty-projects") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = s.noProjectsYet,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = s.createFirstProject,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { isAddingProject = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(s.newProject, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            itemsIndexed(sortedProjects, key = { _, project -> project.id }) { index, project ->
                    val isDragging = draggedProjectId == project.id
                    ProjectCard(
                        project = project,
                        boards = state.boards.filter {
                            it.projectId == project.id && (showArchivedBoards || !it.isArchived)
                        },
                        contacts = state.contacts.filter { it.projectId == project.id },
                        expanded = expandedProjects[project.id] ?: !projectsCollapsed,
                        onToggleExpanded = {
                            val currently = expandedProjects[project.id] ?: !projectsCollapsed
                            expandedProjects[project.id] = !currently
                        },
                        onNavigateToBoard = { boardId -> onNavigateToBoard(boardId, null) },
                        onSetDefault = { board ->
                            if (board.isDefault) viewModel.clearDefaultBoard()
                            else viewModel.setDefaultBoard(board.id)
                        },
                        onRenameProject = { 
                            projectToRename = project
                            renameProjectName = project.name
                        },
                        onDeleteProject = {
                            if (!viewModel.canDeleteProject(project.id)) {
                                val msg = if (state.projects.size <= 1) s.cannotDeleteLastProject
                                else s.cannotDeleteDefaultProject
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            } else {
                                pendingDelete = PendingDelete(
                                    title = s.deleteProject,
                                    name = project.name,
                                    warnContents = true
                                ) { viewModel.deleteProject(project.id) }
                            }
                        },
                        canDeleteProject = viewModel.canDeleteProject(project.id),
                        onAddBoard = {
                            newBoardTemplate = BoardTemplate.INBOX
                            newBoardName = ""
                            isAddingBoardToProject = project.id
                        },
                        onRenameBoard = { board ->
                            boardToRename = board
                            renameBoardName = board.name
                        },
                        onArchiveBoard = { board ->
                            viewModel.setBoardArchived(board.id, !board.isArchived)
                        },
                        onDeleteBoard = { board ->
                            if (viewModel.isProtectedBoard(board)) {
                                Toast.makeText(context, s.cannotDeleteDefaultBoard, Toast.LENGTH_SHORT).show()
                            } else {
                                pendingDelete = PendingDelete(
                                    title = s.deleteBoard,
                                    name = s.localized(board.name),
                                    warnContents = true
                                ) { viewModel.deleteBoard(board.id) }
                            }
                        },
                        canDeleteBoard = { board -> !viewModel.isProtectedBoard(board) },
                        onExportProject = {
                            exportProjectId = project.id
                            exportLauncher.launch("kairos_${project.name}.json")
                        },
                        onShareProject = { shareViaMessenger(project.id) },
                        onAddContact = {
                            contactToEdit = null
                            contactDialogProjectId = project.id
                        },
                        onEditContact = { contact ->
                            contactToEdit = contact
                            contactDialogProjectId = project.id
                        },
                        onDeleteContact = { contact ->
                            pendingDelete = PendingDelete(
                                title = s.deleteContact,
                                name = contact.name,
                                warnContents = false
                            ) { viewModel.deleteContact(contact.id) }
                        },
                        isFirst = index == 0,
                        isLast = index == sortedProjects.lastIndex,
                        canReorder = sortedProjects.size > 1,
                        onMoveUp = { viewModel.moveProject(project.id, -1) },
                        onMoveDown = { viewModel.moveProject(project.id, 1) },
                        onDragStart = {
                            draggedProjectId = project.id
                            draggedOffsetY = 0f
                        },
                        onDrag = { deltaY ->
                            draggedOffsetY += deltaY
                            if (draggedOffsetY > reorderThreshold && index < sortedProjects.lastIndex) {
                                viewModel.moveProject(project.id, 1)
                                draggedOffsetY = 0f
                            } else if (draggedOffsetY < -reorderThreshold && index > 0) {
                                viewModel.moveProject(project.id, -1)
                                draggedOffsetY = 0f
                            }
                        },
                        onDragEnd = {
                            draggedProjectId = null
                            draggedOffsetY = 0f
                        },
                        dragOffsetY = draggedOffsetY,
                        isDragging = isDragging
                    )
                }
                item { DeveloperContactsCard() }
                item { Spacer(modifier = Modifier.height(72.dp)) }
        }

        if (overviewPanel != null) {
            OverviewPanelDialog(
                panel = overviewPanel!!,
                onDismiss = { overviewPanel = null },
                allTasks = allTasks,
                filteredAllTasks = filteredAllTasks,
                allHubs = allHubs,
                allBoardsList = allBoardsList,
                state = state,
                taskSearchQuery = taskSearchQuery,
                onTaskSearchQueryChange = { taskSearchQuery = it },
                taskFilter = taskFilter,
                onTaskFilterChange = { taskFilter = it },
                onCompleteTask = { completeTaskWithUndo(it) },
                onOpenBoard = onNavigateToBoard,
                onDeleteTask = { task ->
                    val deletedId = task.id
                    viewModel.deleteTask(deletedId)
                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = s.taskDeletedEverywhere,
                            actionLabel = s.undo,
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.undoDelete(deletedId)
                        } else {
                            viewModel.discardDeletedSnapshot(deletedId)
                        }
                    }
                },
                onRequestDeleteHub = { hub ->
                    val hubsOnBoard = state.columns.count { it.boardId == hub.boardId }
                    if (hubsOnBoard <= 1) {
                        Toast.makeText(context, s.cannotDeleteLastHub, Toast.LENGTH_SHORT).show()
                    } else {
                        pendingDelete = PendingDelete(
                            title = s.deleteHub,
                            name = s.localized(hub.title),
                            warnContents = true
                        ) { viewModel.deleteColumn(hub.id) }
                    }
                },
                onRequestDeleteBoard = { board ->
                    val protectedBoard = viewModel.isProtectedBoard(board)
                    if (protectedBoard) {
                        Toast.makeText(context, s.cannotDeleteDefaultBoard, Toast.LENGTH_SHORT).show()
                    } else {
                        pendingDelete = PendingDelete(
                            title = s.deleteBoard,
                            name = s.localized(board.name),
                            warnContents = true
                        ) { viewModel.deleteBoard(board.id) }
                    }
                },
                isProtectedBoard = { viewModel.isProtectedBoard(it) },
                dateLocale = dateLocale
            )
        }

        if (isQuickAdding) {
            val quickDatePickerState = rememberDatePickerState(
                initialSelectedDateMillis = quickAddDueDate?.let { localMillisToUtcPicker(it) }
            )
            fun submitQuickAdd() {
                val title = quickAddTitle.trim()
                if (title.isBlank()) return
                sanitizeQuickAddPrefs(kairosPrefs, state.boards, state.columns)
                val target = resolveQuickAddTarget(kairosPrefs, state.boards, state.columns)
                if (target == null) {
                    val msg = if (state.boards.none { !it.isArchived }) {
                        s.noPrimaryBoard
                    } else {
                        s.boardHasNoHubs
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    return
                }
                viewModel.addTask(
                    title = title,
                    columnId = target.column.id,
                    categoryId = null,
                    dueDate = quickAddDueDate,
                    currentTasks = state.tasks.filter { it.columnId == target.column.id }
                )
                Toast.makeText(context, s.taskAdded, Toast.LENGTH_SHORT).show()
                quickAddTitle = ""
                quickAddDueDate = null
                isQuickAdding = false
            }
            val quickTarget = remember(state.boards, state.columns, isQuickAdding) {
                resolveQuickAddTarget(kairosPrefs, state.boards, state.columns)
            }
            AlertDialog(
                onDismissRequest = { isQuickAdding = false },
                title = {
                    Text(
                        text = s.quickAddTask,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (quickTarget != null) {
                                s.quickAddGoesTo(
                                    s.localized(quickTarget.board.name),
                                    s.localized(quickTarget.column.title)
                                )
                            } else {
                                s.quickAddHint
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        OutlinedTextField(
                            value = quickAddTitle,
                            onValueChange = { quickAddTitle = it },
                            label = { Text(s.newTask, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = kairosPrefs.enterAddsTask,
                            maxLines = if (kairosPrefs.enterAddsTask) 1 else 4,
                            keyboardOptions = KeyboardOptions(
                                imeAction = if (kairosPrefs.enterAddsTask) ImeAction.Done else ImeAction.Default
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (kairosPrefs.enterAddsTask) submitQuickAdd()
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        DueDateQuickPick(
                            selectedDueDate = quickAddDueDate,
                            onSelect = { quickAddDueDate = it },
                            onPickCustom = { showQuickAddDatePicker = true }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { submitQuickAdd() }) { Text(s.create) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        quickAddTitle = ""
                        quickAddDueDate = null
                        isQuickAdding = false
                    }) { Text(s.cancel) }
                }
            )
            if (showQuickAddDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showQuickAddDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            quickAddDueDate = quickDatePickerState.selectedDateMillis?.let {
                                utcPickerMillisToLocalNoon(it)
                            }
                            showQuickAddDatePicker = false
                        }) { Text(s.ok) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showQuickAddDatePicker = false }) { Text(s.cancel) }
                    }
                ) {
                    DatePicker(state = quickDatePickerState)
                }
            }
        }

        if (isAddingProject) {
            AlertDialog(
                onDismissRequest = { isAddingProject = false },
                title = { Text(s.newProject) },
                text = {
                    OutlinedTextField(
                        value = newProjectName,
                        onValueChange = { newProjectName = it },
                        label = { Text(s.projectName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newProjectName.isNotBlank()) viewModel.addProject(newProjectName)
                        newProjectName = ""
                        isAddingProject = false
                    }) { Text(s.create) }
                },
                dismissButton = {
                    TextButton(onClick = { isAddingProject = false }) { Text(s.cancel) }
                }
            )
        }

        if (projectToRename != null) {
            AlertDialog(
                onDismissRequest = { projectToRename = null },
                title = { Text(s.renameProject) },
                text = {
                    OutlinedTextField(
                        value = renameProjectName,
                        onValueChange = { renameProjectName = it },
                        label = { Text(s.projectName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (renameProjectName.isNotBlank()) {
                            viewModel.renameProject(projectToRename!!.id, renameProjectName)
                        }
                        projectToRename = null
                    }) { Text(s.save) }
                },
                dismissButton = {
                    TextButton(onClick = { projectToRename = null }) { Text(s.cancel) }
                }
            )
        }

        if (isAddingBoardToProject != null) {
            AlertDialog(
                onDismissRequest = { isAddingBoardToProject = null },
                title = { Text(s.newBoard) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newBoardName,
                            onValueChange = { newBoardName = it },
                            label = { Text(s.boardName) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(s.boardTemplate, style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = newBoardTemplate == BoardTemplate.INBOX,
                                onClick = { newBoardTemplate = BoardTemplate.INBOX },
                                label = { Text(s.templateInbox) }
                            )
                            FilterChip(
                                selected = newBoardTemplate == BoardTemplate.FLOW,
                                onClick = { newBoardTemplate = BoardTemplate.FLOW },
                                label = { Text(s.templateFlow) }
                            )
                            FilterChip(
                                selected = newBoardTemplate == BoardTemplate.EISENHOWER,
                                onClick = { newBoardTemplate = BoardTemplate.EISENHOWER },
                                label = { Text(s.templateEisenhower) }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newBoardName.isNotBlank()) {
                            viewModel.addBoard(isAddingBoardToProject!!, newBoardName, newBoardTemplate)
                        }
                        newBoardName = ""
                        newBoardTemplate = BoardTemplate.INBOX
                        isAddingBoardToProject = null
                    }) { Text(s.create) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        newBoardName = ""
                        newBoardTemplate = BoardTemplate.INBOX
                        isAddingBoardToProject = null
                    }) { Text(s.cancel) }
                }
            )
        }
        
        if (boardToRename != null) {
            AlertDialog(
                onDismissRequest = { boardToRename = null },
                title = { Text(s.renameBoard) },
                text = {
                    OutlinedTextField(
                        value = renameBoardName,
                        onValueChange = { renameBoardName = it },
                        label = { Text(s.boardName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (renameBoardName.isNotBlank()) {
                            viewModel.renameBoard(boardToRename!!.id, renameBoardName)
                        }
                        boardToRename = null
                    }) { Text(s.save) }
                },
                dismissButton = {
                    TextButton(onClick = { boardToRename = null }) { Text(s.cancel) }
                }
            )
        }

        if (contactDialogProjectId != null) {
            ContactDialog(
                contact = contactToEdit,
                onDismiss = {
                    contactDialogProjectId = null
                    contactToEdit = null
                },
                onSave = { name, phone, email, role ->
                    val projectId = contactDialogProjectId
                    if (projectId != null) {
                        val existing = contactToEdit
                        if (existing != null) {
                            viewModel.updateContact(
                                existing.copy(
                                    name = name.trim(),
                                    phone = phone.trim(),
                                    email = email.trim(),
                                    role = role.trim()
                                )
                            )
                        } else {
                            viewModel.addContact(projectId, name, phone, email, role)
                        }
                    }
                    contactDialogProjectId = null
                    contactToEdit = null
                }
            )
        }

        if (pendingDelete != null) {
            val delete = pendingDelete!!
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text(delete.title) },
                text = {
                    Column {
                        Text(s.quoted(delete.name))
                        if (delete.warnContents) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(s.deleteWithContents)
                        }
                        if (delete.warnEverywhere) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(s.deleteTaskEverywhere)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        delete.onConfirm()
                        pendingDelete = null
                    }) {
                        Text(s.delete, color = deleteButtonColor, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) { Text(s.cancel) }
                }
            )
        }

        if (showImportConfirm) {
            AlertDialog(
                onDismissRequest = { showImportConfirm = false },
                title = { Text(s.importOverwriteTitle) },
                text = { Text(s.importOverwriteText) },
                confirmButton = {
                    TextButton(onClick = {
                        showImportConfirm = false
                        importLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                    }) { Text(s.ok) }
                },
                dismissButton = {
                    TextButton(onClick = { showImportConfirm = false }) { Text(s.cancel) }
                }
            )
        }

        if (showOnboarding) {
            OnboardingDialog(
                onFinished = {
                    showOnboarding = false
                    kairosPrefs.hasSeenOnboarding = true
                }
            )
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    boards: List<Board>,
    contacts: List<Contact>,
    expanded: Boolean = true,
    onToggleExpanded: () -> Unit = {},
    onNavigateToBoard: (String) -> Unit,
    onSetDefault: (Board) -> Unit,
    onRenameProject: () -> Unit,
    onDeleteProject: () -> Unit,
    canDeleteProject: Boolean = true,
    onAddBoard: () -> Unit,
    onRenameBoard: (Board) -> Unit,
    onArchiveBoard: (Board) -> Unit,
    onDeleteBoard: (Board) -> Unit,
    canDeleteBoard: (Board) -> Boolean = { true },
    onExportProject: () -> Unit = {},
    onShareProject: () -> Unit = {},
    onAddContact: () -> Unit,
    onEditContact: (Contact) -> Unit,
    onDeleteContact: (Contact) -> Unit,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    canReorder: Boolean = false,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onDragStart: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {},
    dragOffsetY: Float = 0f,
    isDragging: Boolean = false
) {
    val s = LocalAppStrings.current
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, if (isDragging) dragOffsetY.roundToInt() else 0) }
            .zIndex(if (isDragging) 10f else 0f)
            .shadow(if (isDragging) 10.dp else 0.dp, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 6.dp else 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            if (isDragging) 2.dp else 1.2.dp,
            if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Project Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleExpanded() }
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) s.hide else s.show,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    if (canReorder) {
                        // Drag Handle (press and drag to reorder project)
                        Box(
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                .pointerInput(project.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { onDragStart() },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            onDrag(dragAmount.y)
                                        },
                                        onDragEnd = { onDragEnd() },
                                        onDragCancel = { onDragEnd() }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = s.dragProject,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = project.name, 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canReorder) {
                        IconButton(
                            onClick = onMoveUp,
                            enabled = !isFirst,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                contentDescription = s.moveUp,
                                modifier = Modifier.size(22.dp),
                                tint = if (!isFirst) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )
                        }
                        IconButton(
                            onClick = onMoveDown,
                            enabled = !isLast,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = s.moveDown,
                                modifier = Modifier.size(22.dp),
                                tint = if (!isLast) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )
                        }
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = s.projectMenu, modifier = Modifier.size(24.dp))
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            if (canReorder && !isFirst) {
                                DropdownMenuItem(
                                    text = { Text(s.moveUp, style = MaterialTheme.typography.bodyLarge) },
                                    onClick = { onMoveUp(); menuExpanded = false },
                                    leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, null, modifier = Modifier.size(22.dp)) }
                                )
                            }
                            if (canReorder && !isLast) {
                                DropdownMenuItem(
                                    text = { Text(s.moveDown, style = MaterialTheme.typography.bodyLarge) },
                                    onClick = { onMoveDown(); menuExpanded = false },
                                    leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(22.dp)) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(s.renameProject, style = MaterialTheme.typography.bodyLarge) },
                                onClick = { onRenameProject(); menuExpanded = false },
                                leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(22.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text(s.exportProject, style = MaterialTheme.typography.bodyLarge) },
                                onClick = { onExportProject(); menuExpanded = false },
                                leadingIcon = { Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(22.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text(s.shareProject, style = MaterialTheme.typography.bodyLarge) },
                                onClick = { onShareProject(); menuExpanded = false },
                                leadingIcon = { Icon(Icons.Default.Share, null, modifier = Modifier.size(22.dp)) }
                            )
                            if (canDeleteProject) {
                                DropdownMenuItem(
                                    text = { Text(s.deleteProject, color = deleteButtonColor, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge) },
                                    onClick = { onDeleteProject(); menuExpanded = false },
                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = deleteButtonColor, modifier = Modifier.size(22.dp)) }
                                )
                            }
                        }
                    }
                }
            }

            if (expanded) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Boards List
            if (boards.isEmpty()) {
                Text(s.noBoards, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                boards.forEach { board ->
                    BoardItem(
                        board = board,
                        onNavigate = { onNavigateToBoard(board.id) },
                        onSetDefault = { onSetDefault(board) },
                        onRename = { onRenameBoard(board) },
                        onArchive = { onArchiveBoard(board) },
                        onDelete = { onDeleteBoard(board) },
                        canDelete = canDeleteBoard(board)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Add Board Button (Large, prominent with color accent)
            OutlinedButton(
                onClick = onAddBoard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(s.addBoard, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = s.contacts,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (contacts.isEmpty()) {
                Text(
                    s.noContacts,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                contacts.forEach { contact ->
                    ContactItem(
                        contact = contact,
                        onEdit = { onEditContact(contact) },
                        onDelete = { onDeleteContact(contact) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onAddContact,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(s.addContact, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            }
            }
        }
    }
}

@Composable
fun BoardItem(
    board: Board, 
    onNavigate: () -> Unit,
    onSetDefault: () -> Unit,
    onRename: () -> Unit,
    onArchive: () -> Unit = {},
    onDelete: () -> Unit,
    canDelete: Boolean = true
) {
    val s = LocalAppStrings.current
    var boardMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate() }
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .then(if (board.isArchived) Modifier.alpha(0.55f) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            // Modern mini kanban icon with colored border
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                    .border(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(3.dp)
                ) {
                    Box(modifier = Modifier.width(3.dp).height(12.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp)))
                    Box(modifier = Modifier.width(3.dp).height(8.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(1.dp)))
                    Box(modifier = Modifier.width(3.dp).height(10.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(1.dp)))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = s.localized(board.name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (board.isArchived) {
                    Text(
                        text = s.archivedLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (board.isDefault) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Filled.Star, contentDescription = s.primaryBoard, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
        }
        
        Box {
            IconButton(onClick = { boardMenuExpanded = true }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = s.boardMenu, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            }
            DropdownMenu(
                expanded = boardMenuExpanded,
                onDismissRequest = { boardMenuExpanded = false }
            ) {
                if (!board.isArchived) {
                    DropdownMenuItem(
                        text = { Text(if (board.isDefault) s.unsetPrimary else s.setPrimary, style = MaterialTheme.typography.bodyLarge) },
                        onClick = { onSetDefault(); boardMenuExpanded = false },
                        leadingIcon = { Icon(if (board.isDefault) Icons.Outlined.Star else Icons.Filled.Star, null, modifier = Modifier.size(22.dp)) }
                    )
                }
                DropdownMenuItem(
                    text = { Text(s.renameBoard, style = MaterialTheme.typography.bodyLarge) },
                    onClick = { onRename(); boardMenuExpanded = false },
                    leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(22.dp)) }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            if (board.isArchived) s.unarchiveBoard else s.archiveBoard,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    onClick = { onArchive(); boardMenuExpanded = false },
                    leadingIcon = {
                        Icon(
                            if (board.isArchived) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            null,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                )
                if (canDelete) {
                    DropdownMenuItem(
                        text = { Text(s.deleteBoard, color = deleteButtonColor, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge) },
                        onClick = { onDelete(); boardMenuExpanded = false },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = deleteButtonColor, modifier = Modifier.size(22.dp)) }
                    )
                }
            }
        }
    }
}

@Composable
fun ContactItem(
    contact: Contact,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val s = LocalAppStrings.current
    var menuExpanded by remember { mutableStateOf(false) }
    val initial = contact.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (contact.role.isNotBlank()) {
                Text(
                    text = contact.role,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val details = listOf(contact.phone, contact.email).filter { it.isNotBlank() }
            if (details.isNotEmpty()) {
                Text(
                    text = details.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (contact.phone.isNotBlank()) {
            IconButton(
                onClick = { openDialer(context, contact.phone) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Call,
                    contentDescription = s.call,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (contact.email.isNotBlank()) {
            IconButton(
                onClick = { openEmail(context, contact.email) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Email,
                    contentDescription = s.writeEmail,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Box {
            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = s.contactMenu,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(s.edit, style = MaterialTheme.typography.bodyLarge) },
                    onClick = { onEdit(); menuExpanded = false },
                    leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(22.dp)) }
                )
                DropdownMenuItem(
                    text = { Text(s.deleteContact, color = deleteButtonColor, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge) },
                    onClick = { onDelete(); menuExpanded = false },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = deleteButtonColor, modifier = Modifier.size(22.dp)) }
                )
            }
        }
    }
}

@Composable
fun ContactDialog(
    contact: Contact?,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, email: String, role: String) -> Unit
) {
    val s = LocalAppStrings.current
    var name by remember { mutableStateOf(contact?.name.orEmpty()) }
    var role by remember { mutableStateOf(contact?.role.orEmpty()) }
    var phone by remember { mutableStateOf(contact?.phone.orEmpty()) }
    var email by remember { mutableStateOf(contact?.email.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (contact == null) s.newContact else s.editContact) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(s.name) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text(s.role) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(s.phone) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(s.email) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, phone, email, role) },
                enabled = name.isNotBlank()
            ) { Text(if (contact == null) s.add else s.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.cancel) }
        }
    )
}

private enum class TaskOverviewFilter {
    ACTIVE, TODAY, OVERDUE, DONE, ALL
}

private enum class OverviewPanel {
    Tasks, Hubs, Boards
}

@Composable
private fun OverviewPanelDialog(
    panel: OverviewPanel,
    onDismiss: () -> Unit,
    allTasks: List<Task>,
    filteredAllTasks: List<Task>,
    allHubs: List<Column>,
    allBoardsList: List<Board>,
    state: com.example.kaizenkanban.ui.viewmodel.AppState,
    taskSearchQuery: String,
    onTaskSearchQueryChange: (String) -> Unit,
    taskFilter: TaskOverviewFilter,
    onTaskFilterChange: (TaskOverviewFilter) -> Unit,
    onCompleteTask: (Task) -> Unit,
    onOpenBoard: (boardId: String, columnId: String?) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onRequestDeleteHub: (Column) -> Unit,
    onRequestDeleteBoard: (Board) -> Unit,
    isProtectedBoard: (Board) -> Boolean,
    dateLocale: java.util.Locale
) {
    val s = LocalAppStrings.current
    val title = when (panel) {
        OverviewPanel.Tasks -> s.allTasksHeader(allTasks.size)
        OverviewPanel.Hubs -> s.allHubsHeader(allHubs.size)
        OverviewPanel.Boards -> s.allBoardsHeader(allBoardsList.size)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (panel) {
                    OverviewPanel.Tasks -> {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = taskSearchQuery,
                                    onValueChange = onTaskSearchQueryChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    placeholder = { Text(s.searchTasks) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        TaskOverviewFilter.ACTIVE to s.filterActive,
                                        TaskOverviewFilter.TODAY to s.filterToday,
                                        TaskOverviewFilter.OVERDUE to s.filterOverdue,
                                        TaskOverviewFilter.DONE to s.filterDone,
                                        TaskOverviewFilter.ALL to s.filterAll
                                    ).forEach { (filter, label) ->
                                        FilterChip(
                                            selected = taskFilter == filter,
                                            onClick = { onTaskFilterChange(filter) },
                                            label = {
                                                Text(
                                                    text = label,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        if (filteredAllTasks.isEmpty()) {
                            item {
                                Text(
                                    text = if (allTasks.isEmpty()) s.noTasks else s.noMatchingTasks,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(filteredAllTasks, key = { "ov-task-${it.id}" }) { task ->
                                val column = state.columns.find { it.id == task.columnId }
                                val board = column?.let { col -> state.boards.find { it.id == col.boardId } }
                                val project = board?.let { b -> state.projects.find { it.id == b.projectId } }
                                val location = listOfNotNull(
                                    project?.name,
                                    board?.name?.let { s.localized(it) },
                                    column?.title?.let { s.localized(it) }
                                ).joinToString(" · ")
                                OverviewTaskRow(
                                    task = task,
                                    location = location,
                                    dueDateLabel = task.dueDate?.relativeDueLabel(s, dateLocale, task.isCompleted).orEmpty(),
                                    linkedBoardCount = extraBoardCount(task, state.columns, state.boards),
                                    onToggleCompleted = { onCompleteTask(task) },
                                    onOpen = {
                                        val boardId = column?.boardId
                                        if (boardId != null) {
                                            onDismiss()
                                            onOpenBoard(boardId, column?.id)
                                        }
                                    },
                                    onDelete = { onDeleteTask(task) }
                                )
                            }
                        }
                    }
                    OverviewPanel.Hubs -> {
                        if (allHubs.isEmpty()) {
                            item {
                                Text(
                                    text = s.noHubs,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(allHubs, key = { "ov-hub-${it.id}" }) { hub ->
                                val board = state.boards.find { it.id == hub.boardId }
                                val project = board?.let { b -> state.projects.find { it.id == b.projectId } }
                                val location = listOfNotNull(
                                    project?.name,
                                    board?.name?.let { s.localized(it) }
                                ).joinToString(" · ")
                                OverviewDeletableRow(
                                    title = s.localized(hub.title),
                                    subtitle = location,
                                    onOpen = {
                                        val boardId = board?.id
                                        if (boardId != null) {
                                            onDismiss()
                                            onOpenBoard(boardId, hub.id)
                                        }
                                    },
                                    onDelete = { onRequestDeleteHub(hub) }
                                )
                            }
                        }
                    }
                    OverviewPanel.Boards -> {
                        if (allBoardsList.isEmpty()) {
                            item {
                                Text(
                                    text = s.noBoards,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(allBoardsList, key = { "ov-board-${it.id}" }) { board ->
                                val project = state.projects.find { it.id == board.projectId }
                                val protectedBoard = isProtectedBoard(board)
                                OverviewDeletableRow(
                                    title = s.localized(board.name),
                                    subtitle = project?.name.orEmpty(),
                                    onOpen = {
                                        onDismiss()
                                        onOpenBoard(board.id, null)
                                    },
                                    canDelete = !protectedBoard,
                                    onDelete = { onRequestDeleteBoard(board) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(s.close)
            }
        }
    )
}

private fun appStrings(context: android.content.Context): AppStrings =
    AppStrings(AppLanguage.fromCode(KairosPreferences(context).appLanguageCode))

private data class PendingDelete(
    val title: String,
    val name: String,
    val warnContents: Boolean,
    val warnEverywhere: Boolean = false,
    val onConfirm: () -> Unit
)

@Composable
private fun OverviewDeletableRow(
    title: String,
    subtitle: String,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean = true
) {
    val s = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (canDelete) {
                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = s.delete,
                        tint = deleteButtonColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewTaskRow(
    task: Task,
    location: String,
    dueDateLabel: String,
    linkedBoardCount: Int = 0,
    onToggleCompleted: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val s = LocalAppStrings.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isHidden) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        IconButton(onClick = onToggleCompleted, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                contentDescription = s.completed,
                tint = if (task.isCompleted) Color(0xFF00C853) else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (linkedBoardCount > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = s.linkedOnBoards,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "+$linkedBoardCount",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            if (location.isNotBlank()) {
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (dueDateLabel.isNotBlank()) {
                val overdue = !task.isCompleted && task.dueDate != null && task.dueDate.isOverdueDate()
                Text(
                    text = dueDateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (task.isHidden) {
                Text(
                    text = s.hiddenLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = s.deleteTask,
                tint = deleteButtonColor,
                modifier = Modifier.size(22.dp)
            )
        }
        }
    }
}

private fun extraBoardCount(
    task: Task,
    columns: List<Column>,
    boards: List<Board>
): Int {
    val boardIds = linkedMutableSetOfBoards(task, columns, boards)
    return (boardIds.size - 1).coerceAtLeast(0)
}

private fun linkedMutableSetOfBoards(
    task: Task,
    columns: List<Column>,
    boards: List<Board>
): Set<String> {
    val ids = mutableSetOf<String>()
    columns.find { it.id == task.columnId }?.boardId?.let { ids += it }
    task.linkedColumnIds.forEach { linkId ->
        columns.find { it.id == linkId }?.boardId?.let { ids += it }
    }
    if (task.eisenhowerQuadrant != null) {
        val homeProjectId = columns.find { it.id == task.columnId }
            ?.let { col -> boards.find { it.id == col.boardId }?.projectId }
        boards.filter {
            KanbanNames.isEisenhowerBoard(it.name) &&
                (homeProjectId == null || it.projectId == homeProjectId)
        }.forEach { ids += it.id }
    }
    return ids
}

@Composable
fun DeveloperContactsCard() {
    val context = LocalContext.current
    val s = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = s.developer,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = s.developerName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = s.developerHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { openTelegram(context) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = s.telegram,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = s.telegram,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "@ruslan_kx",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { openEmail(context, s.developerEmailValue) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = s.developerEmailLabel,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = s.developerEmailLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = s.developerEmailValue,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun openDialer(context: android.content.Context, phone: String) {
    val digits = phone.filter { it.isDigit() || it == '+' }
    if (digits.isBlank()) return
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits")))
    } catch (e: Exception) {
        Toast.makeText(context, appStrings(context).cannotOpenDialer, Toast.LENGTH_SHORT).show()
    }
}

private fun openEmail(context: android.content.Context, email: String) {
    if (email.isBlank()) return
    try {
        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
    } catch (e: Exception) {
        Toast.makeText(context, appStrings(context).cannotOpenEmail, Toast.LENGTH_SHORT).show()
    }
}

private fun openTelegram(context: android.content.Context) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/ruslan_kx")))
    } catch (e: Exception) {
        Toast.makeText(context, appStrings(context).cannotOpenLink, Toast.LENGTH_SHORT).show()
    }
}