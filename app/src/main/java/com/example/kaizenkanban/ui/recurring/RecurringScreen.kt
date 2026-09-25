package com.example.kaizenkanban.ui.recurring

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kaizenkanban.domain.model.Board
import com.example.kaizenkanban.domain.model.Column
import com.example.kaizenkanban.domain.model.Eisenhower
import com.example.kaizenkanban.domain.model.RecurringRhythm
import com.example.kaizenkanban.domain.model.RecurringTemplate
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.domain.model.TaskLink
import com.example.kaizenkanban.domain.model.TaskLinkGraph
import com.example.kaizenkanban.domain.usecase.RecurringTargetResolver
import com.example.kaizenkanban.reminders.DueReminderScheduler
import com.example.kaizenkanban.ui.board.EisenhowerQuadrantChip
import com.example.kaizenkanban.ui.board.TaskLinkPickerDialog
import com.example.kaizenkanban.ui.components.DialogSectionDivider
import com.example.kaizenkanban.ui.i18n.LocalAppStrings
import com.example.kaizenkanban.ui.taskmeta.TaskComplexityPicker
import com.example.kaizenkanban.ui.theme.deleteButtonColor
import com.example.kaizenkanban.ui.viewmodel.SharedViewModel
import java.util.UUID
import kotlinx.coroutines.delay
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    projectId: String,
    viewModel: SharedViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val s = LocalAppStrings.current
    val project = state.projects.find { it.id == projectId }
    val templates = remember(state.recurringTemplates, projectId) {
        state.recurringTemplates.filter { it.projectId == projectId }
    }
    val projectBoards = remember(state.boards, projectId) {
        state.boards.filter { it.projectId == projectId && !it.isArchived }
    }
    val projectTasks = remember(state.tasks, state.columns, state.boards, projectId) {
        val boardIds = state.boards.filter { it.projectId == projectId }.map { it.id }.toSet()
        val columnIds = state.columns.filter { it.boardId in boardIds }.map { it.id }.toSet()
        state.tasks.filter { it.columnId in columnIds }
    }

    LaunchedEffect(projectId) {
        viewModel.ensureTodayRecurringInstances()
    }

    var dialogTemplate by remember { mutableStateOf<RecurringTemplate?>(null) }
    var showCreate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(s.recurringTitle, fontWeight = FontWeight.Bold)
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
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreate = true },
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = s.recurringAdd)
            }
        }
    ) { padding ->
        if (templates.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Repeat,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    s.recurringEmpty,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    s.recurringEmptyHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { showCreate = true }) {
                    Text(s.recurringAdd)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(templates, key = { it.id }) { template ->
                    RecurringTemplateRow(
                        template = template,
                        boards = state.boards,
                        columns = state.columns,
                        onClick = { dialogTemplate = template },
                        onToggle = { enabled ->
                            viewModel.setRecurringTemplateEnabled(template.id, enabled)
                        },
                        onDelete = {
                            viewModel.deleteRecurringTemplate(template.id)
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    if (showCreate) {
        val defaults = RecurringTargetResolver.resolveDefault(
            projectId, state.boards, state.columns
        )
        RecurringTemplateDialog(
            projectId = projectId,
            initial = null,
            boards = projectBoards,
            columns = state.columns,
            projectTasks = projectTasks,
            taskLinks = state.taskLinks,
            defaultBoardId = defaults?.boardId,
            defaultColumnId = defaults?.columnId,
            onDismiss = { showCreate = false },
            onSave = { template ->
                viewModel.upsertRecurringTemplate(template)
                showCreate = false
            },
            onDelete = null
        )
    }

    dialogTemplate?.let { editing ->
        RecurringTemplateDialog(
            projectId = projectId,
            initial = editing,
            boards = projectBoards,
            columns = state.columns,
            projectTasks = projectTasks,
            taskLinks = state.taskLinks,
            defaultBoardId = editing.targetBoardId,
            defaultColumnId = editing.targetColumnId,
            onDismiss = { dialogTemplate = null },
            onSave = { template ->
                viewModel.upsertRecurringTemplate(template)
                dialogTemplate = null
            },
            onDelete = {
                viewModel.deleteRecurringTemplate(editing.id)
                dialogTemplate = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecurringTemplateRow(
    template: RecurringTemplate,
    boards: List<Board>,
    columns: List<Column>,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val s = LocalAppStrings.current
    val hub = columns.find { it.id == template.targetColumnId }
    val board = boards.find { it.id == template.targetBoardId }
        ?: hub?.let { h -> boards.find { it.id == h.boardId } }
    val dest = listOfNotNull(
        board?.let { s.localized(it.name) },
        hub?.let { s.localized(it.title) }
    ).joinToString(" → ").ifBlank { "—" }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Repeat,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                template.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(iterations = Int.MAX_VALUE)
            )
            val timeLabel = template.reminderTimesOfDay
                .sorted()
                .joinToString(" ") { DueReminderScheduler.formatMinutesOfDay(it) }
                .ifBlank { null }
            val eisenhowerLabel = template.eisenhowerQuadrant?.takeIf {
                template.showEisenhowerButtons || template.eisenhowerQuadrant != null
            }
            val impactLabel = template.complexity?.let { "${s.taskComplexity} $it" }
            Text(
                listOfNotNull(rhythmLabel(template), timeLabel, eisenhowerLabel, impactLabel, dest)
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Switch(
            checked = template.enabled,
            onCheckedChange = onToggle
        )
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = s.statsMore)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(s.recurringDelete) },
                    onClick = {
                        menuOpen = false
                        confirmDelete = true
                    }
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(s.recurringDelete, fontWeight = FontWeight.Bold) },
            text = { Text(s.recurringDeleteConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(s.delete) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(s.cancel) }
            }
        )
    }
}

@Composable
private fun rhythmLabel(template: RecurringTemplate): String {
    val s = LocalAppStrings.current
    return when (template.rhythm) {
        RecurringRhythm.DAILY -> s.recurringDaily
        RecurringRhythm.WEEKDAYS -> {
            val labels = listOf(s.weekdayMon, s.weekdayTue, s.weekdayWed, s.weekdayThu, s.weekdayFri, s.weekdaySat, s.weekdaySun)
            template.weekdays.sorted().joinToString(" ") { d -> labels.getOrNull(d - 1) ?: d.toString() }
                .ifBlank { s.recurringWeekdays }
        }
        RecurringRhythm.EVERY_N_DAYS ->
            s.recurringEveryNDaysLabel((template.timesPerWeek ?: 2).coerceIn(2, 30))
        else -> template.rhythm
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RecurringTemplateDialog(
    projectId: String,
    initial: RecurringTemplate?,
    boards: List<Board>,
    columns: List<Column>,
    projectTasks: List<Task>,
    taskLinks: List<TaskLink>,
    defaultBoardId: String?,
    defaultColumnId: String?,
    onDismiss: () -> Unit,
    onSave: (RecurringTemplate) -> Unit,
    onDelete: (() -> Unit)?
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val titleFocusRequester = remember { FocusRequester() }
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var rhythm by remember(initial) {
        mutableStateOf(
            when (initial?.rhythm) {
                RecurringRhythm.WEEKDAYS -> RecurringRhythm.WEEKDAYS
                RecurringRhythm.EVERY_N_DAYS -> RecurringRhythm.EVERY_N_DAYS
                else -> RecurringRhythm.DAILY
            }
        )
    }
    var weekdays by remember(initial) {
        mutableStateOf(initial?.weekdays?.takeIf { it.isNotEmpty() } ?: setOf(1, 3, 5))
    }
    var intervalDays by remember(initial) {
        mutableStateOf((initial?.timesPerWeek ?: 2).coerceIn(2, 30))
    }
    var boardId by remember(initial, defaultBoardId, boards) {
        mutableStateOf(
            initial?.targetBoardId
                ?: defaultBoardId
                ?: boards.firstOrNull()?.id.orEmpty()
        )
    }
    var columnId by remember(initial, defaultColumnId, boardId, columns) {
        val boardCols = columns.filter { it.boardId == boardId }.sortedBy { it.position }
        mutableStateOf(
            initial?.targetColumnId?.takeIf { id -> boardCols.any { it.id == id } }
                ?: defaultColumnId?.takeIf { id -> boardCols.any { it.id == id } }
                ?: boardCols.firstOrNull()?.id.orEmpty()
        )
    }
    var enabled by remember(initial) { mutableStateOf(initial?.enabled ?: true) }
    var showEisenhower by remember(initial) {
        mutableStateOf(initial?.showEisenhowerButtons == true || initial?.eisenhowerQuadrant != null)
    }
    var quadrant by remember(initial) { mutableStateOf(initial?.eisenhowerQuadrant) }
    var complexity by remember(initial) { mutableStateOf(initial?.complexity) }
    var reminderTimes by remember(initial) {
        mutableStateOf(
            initial?.reminderTimesOfDay?.sorted()?.ifEmpty { listOf(9 * 60) } ?: listOf(9 * 60)
        )
    }
    var linkParentIds by remember(initial) {
        mutableStateOf(initial?.linkParentIds?.toSet().orEmpty())
    }
    var linkChildIds by remember(initial) {
        mutableStateOf(initial?.linkChildIds?.toSet().orEmpty())
    }
    var linkPickMode by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val boardColumns = remember(boardId, columns) {
        columns.filter { it.boardId == boardId }.sortedBy { it.position }
    }
    val tasksById = remember(projectTasks) { projectTasks.associateBy { it.id } }
    val projectColumns = remember(columns, boards, projectId) {
        val boardIds = boards.filter { it.projectId == projectId }.map { it.id }.toSet()
        columns.filter { it.boardId in boardIds }.sortedBy { it.position }
    }

    LaunchedEffect(initial) {
        if (initial == null) {
            delay(80)
            titleFocusRequester.requestFocus()
            keyboard?.show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) s.recurringAdd else s.recurringEdit,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(s.taskTitleLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocusRequester)
                )

                DialogSectionDivider()
                Text(s.recurringTime, fontWeight = FontWeight.SemiBold)
                Text(
                    s.recurringTimeHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    reminderTimes.forEach { minutes ->
                        InputChip(
                            selected = true,
                            onClick = {
                                android.app.TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        val next = hour * 60 + minute
                                        reminderTimes = reminderTimes
                                            .map { if (it == minutes) next else it }
                                            .distinct()
                                            .sorted()
                                    },
                                    minutes / 60,
                                    minutes % 60,
                                    true
                                ).show()
                            },
                            label = { Text(DueReminderScheduler.formatMinutesOfDay(minutes)) },
                            trailingIcon = if (reminderTimes.size > 1) {
                                {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = s.delete,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable {
                                                reminderTimes = reminderTimes.filterNot { it == minutes }
                                            }
                                    )
                                }
                            } else null
                        )
                    }
                    FilterChip(
                        selected = false,
                        onClick = {
                            val seed = reminderTimes.lastOrNull() ?: (9 * 60)
                            android.app.TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    val next = hour * 60 + minute
                                    if (next !in reminderTimes) {
                                        reminderTimes = (reminderTimes + next).sorted()
                                    }
                                },
                                seed / 60,
                                seed % 60,
                                true
                            ).show()
                        },
                        label = { Text("+ ${s.recurringTimeAdd}") }
                    )
                }

                DialogSectionDivider()
                Text(s.recurringRhythm, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = rhythm == RecurringRhythm.DAILY,
                        onClick = { rhythm = RecurringRhythm.DAILY },
                        label = { Text(s.recurringDaily) }
                    )
                    FilterChip(
                        selected = rhythm == RecurringRhythm.WEEKDAYS,
                        onClick = { rhythm = RecurringRhythm.WEEKDAYS },
                        label = { Text(s.recurringWeekdays) }
                    )
                    FilterChip(
                        selected = rhythm == RecurringRhythm.EVERY_N_DAYS,
                        onClick = { rhythm = RecurringRhythm.EVERY_N_DAYS },
                        label = { Text(s.recurringEveryNDays) }
                    )
                }

                if (rhythm == RecurringRhythm.WEEKDAYS) {
                    val dayLabels = listOf(
                        1 to s.weekdayMon, 2 to s.weekdayTue, 3 to s.weekdayWed,
                        4 to s.weekdayThu, 5 to s.weekdayFri, 6 to s.weekdaySat, 7 to s.weekdaySun
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        dayLabels.forEach { (day, label) ->
                            FilterChip(
                                selected = day in weekdays,
                                onClick = {
                                    weekdays = if (day in weekdays) weekdays - day else weekdays + day
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
                if (rhythm == RecurringRhythm.EVERY_N_DAYS) {
                    Text(s.recurringIntervalDays, style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(2, 3, 4, 5, 7, 10, 14).forEach { n ->
                            FilterChip(
                                selected = intervalDays == n,
                                onClick = { intervalDays = n },
                                label = { Text(n.toString()) }
                            )
                        }
                    }
                }

                DialogSectionDivider()
                Text(s.recurringDestination, fontWeight = FontWeight.SemiBold)
                if (boards.isEmpty()) {
                    Text(
                        s.recurringNoBoards,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(s.board, style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        boards.forEach { board ->
                            FilterChip(
                                selected = boardId == board.id,
                                onClick = {
                                    boardId = board.id
                                    columnId = columns
                                        .filter { it.boardId == board.id }
                                        .minByOrNull { it.position }
                                        ?.id
                                        .orEmpty()
                                },
                                label = {
                                    Text(
                                        s.localized(board.name),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                    Text(s.hub, style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        boardColumns.forEach { col ->
                            FilterChip(
                                selected = columnId == col.id,
                                onClick = { columnId = col.id },
                                label = {
                                    Text(
                                        s.localized(col.title),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }

                DialogSectionDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        s.eisenhowerButtons,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Switch(
                        checked = showEisenhower,
                        onCheckedChange = {
                            showEisenhower = it
                            if (!it) quadrant = null
                        }
                    )
                }
                if (showEisenhower) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Eisenhower.Q1 to "Q1",
                            Eisenhower.Q2 to "Q2",
                            Eisenhower.Q3 to "Q3",
                            Eisenhower.Q4 to "Q4"
                        ).forEach { (qKey, qLabel) ->
                            EisenhowerQuadrantChip(
                                qKey = qKey,
                                qLabel = qLabel,
                                isSelected = quadrant == qKey,
                                dimUnselected = quadrant != null && quadrant != qKey,
                                onClick = { quadrant = if (quadrant == qKey) null else qKey }
                            )
                        }
                    }
                }

                DialogSectionDivider()
                TaskComplexityPicker(
                    selected = complexity,
                    onSelect = { complexity = it }
                )

                DialogSectionDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(s.recurringActive)
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }

                DialogSectionDivider()
                Text(s.manageTaskLinks, fontWeight = FontWeight.SemiBold)
                Text(
                    s.taskParents,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                linkParentIds.forEach { parentId ->
                    val parentTitle = tasksById[parentId]?.title ?: parentId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.parentChip(parentTitle),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { linkParentIds = linkParentIds - parentId }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                }
                TextButton(onClick = { linkPickMode = "parent" }) {
                    Text(s.addParentLink)
                }
                DialogSectionDivider()
                Text(
                    s.taskChildren,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                linkChildIds.forEach { childId ->
                    val childTitle = tasksById[childId]?.title ?: childId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.childrenChipLabel(childTitle),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { linkChildIds = linkChildIds - childId }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                }
                TextButton(onClick = { linkPickMode = "child" }) {
                    Text(s.addChildLink)
                }

                if (onDelete != null) {
                    DialogSectionDivider()
                    TextButton(onClick = { confirmDelete = true }) {
                        Text(s.delete, color = deleteButtonColor)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = title.trim()
                    if (trimmed.isBlank() || boardId.isBlank() || columnId.isBlank()) return@TextButton
                    if (rhythm == RecurringRhythm.WEEKDAYS && weekdays.isEmpty()) return@TextButton
                    onSave(
                        RecurringTemplate(
                            id = initial?.id ?: UUID.randomUUID().toString(),
                            projectId = projectId,
                            title = trimmed,
                            rhythm = rhythm,
                            timesPerWeek = if (rhythm == RecurringRhythm.EVERY_N_DAYS) intervalDays else null,
                            weekdays = if (rhythm == RecurringRhythm.WEEKDAYS) weekdays else emptySet(),
                            targetBoardId = boardId,
                            targetColumnId = columnId,
                            enabled = enabled,
                            reminderTimesOfDay = reminderTimes.ifEmpty { listOf(9 * 60) },
                            eisenhowerQuadrant = if (showEisenhower) quadrant else null,
                            showEisenhowerButtons = showEisenhower,
                            linkParentIds = linkParentIds.toList(),
                            linkChildIds = linkChildIds.toList(),
                            complexity = complexity,
                            createdAt = initial?.createdAt ?: System.currentTimeMillis()
                        )
                    )
                },
                enabled = title.isNotBlank() && boardId.isNotBlank() && columnId.isNotBlank() &&
                    (rhythm != RecurringRhythm.WEEKDAYS || weekdays.isNotEmpty()) &&
                    (rhythm != RecurringRhythm.EVERY_N_DAYS || intervalDays in 2..30)
            ) { Text(s.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.cancel) }
        }
    )

    linkPickMode?.let { pickMode ->
        val candidates = remember(
            pickMode,
            projectTasks,
            linkParentIds,
            linkChildIds,
            taskLinks
        ) {
            projectTasks.filter { candidate ->
                when (pickMode) {
                    "parent" -> {
                        candidate.id !in linkParentIds &&
                            candidate.id !in linkChildIds &&
                            linkChildIds.none {
                                TaskLinkGraph.wouldCreateCycle(taskLinks, candidate.id, it)
                            }
                    }
                    else -> {
                        candidate.id !in linkChildIds &&
                            candidate.id !in linkParentIds &&
                            linkParentIds.none {
                                TaskLinkGraph.wouldCreateCycle(taskLinks, it, candidate.id)
                            }
                    }
                }
            }
        }
        TaskLinkPickerDialog(
            title = if (pickMode == "parent") s.pickParentTask else s.pickChildTask,
            candidates = candidates,
            columns = projectColumns,
            onPick = { candidate ->
                if (pickMode == "parent") {
                    linkParentIds = linkParentIds + candidate.id
                } else {
                    linkChildIds = linkChildIds + candidate.id
                }
                linkPickMode = null
            },
            onDismiss = { linkPickMode = null }
        )
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(s.delete, fontWeight = FontWeight.Bold) },
            text = { Text(s.recurringDeleteConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(s.delete, color = deleteButtonColor) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(s.cancel) }
            }
        )
    }
}
