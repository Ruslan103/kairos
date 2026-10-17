package com.example.kaizenkanban.ui.recurring

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kaizenkanban.data.local.KairosPreferences
import com.example.kaizenkanban.domain.model.Board
import com.example.kaizenkanban.domain.model.Eisenhower
import com.example.kaizenkanban.domain.model.HubGroupOrdering
import com.example.kaizenkanban.domain.model.Column
import com.example.kaizenkanban.domain.model.Contact
import com.example.kaizenkanban.domain.model.RecurringTemplate
import com.example.kaizenkanban.domain.model.TaskCardBody
import com.example.kaizenkanban.domain.model.TaskCardKind
import com.example.kaizenkanban.domain.model.RemindLeadUnit
import com.example.kaizenkanban.domain.model.RecurringDefaults
import com.example.kaizenkanban.domain.model.RecurringRhythm
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.domain.model.TaskLink
import com.example.kaizenkanban.domain.model.TaskLinkGraph
import com.example.kaizenkanban.domain.usecase.EnsureRecurringInstancesUseCase
import com.example.kaizenkanban.domain.usecase.RecurringTargetResolver
import com.example.kaizenkanban.reminders.DueReminderScheduler
import com.example.kaizenkanban.ui.board.EisenhowerQuadrantChip
import com.example.kaizenkanban.ui.board.TaskCardSetupDialog
import com.example.kaizenkanban.ui.board.TaskCardTypeDisclosure
import com.example.kaizenkanban.ui.board.hubGroupCapBackground
import com.example.kaizenkanban.ui.board.hubGroupGoldFill
import com.example.kaizenkanban.ui.board.hubGroupMarkGoldFill
import com.example.kaizenkanban.ui.board.hubGroupPlate
import com.example.kaizenkanban.ui.board.TaskLinkPickerDialog
import com.example.kaizenkanban.ui.calendar.formatDateShort
import com.example.kaizenkanban.ui.calendar.localMillisToUtcPicker
import com.example.kaizenkanban.ui.calendar.utcPickerMillisToLocalNoon
import com.example.kaizenkanban.ui.components.DialogActionLine
import com.example.kaizenkanban.ui.components.DialogDisclosureRow
import com.example.kaizenkanban.ui.components.DialogMarqueeText
import com.example.kaizenkanban.ui.components.DialogPanel
import com.example.kaizenkanban.ui.components.DialogSheetTonalElevation
import com.example.kaizenkanban.ui.components.TaskDialogBlock
import com.example.kaizenkanban.ui.components.dialogSheetFill
import com.example.kaizenkanban.ui.i18n.LocalAppStrings
import com.example.kaizenkanban.ui.taskmeta.TaskTimerPlate
import com.example.kaizenkanban.ui.taskmeta.formatTimerClosedSummary
import com.example.kaizenkanban.ui.theme.LiterataBold
import com.example.kaizenkanban.ui.theme.buttonFill
import com.example.kaizenkanban.ui.theme.buttonLabel
import com.example.kaizenkanban.ui.theme.buttonLine
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
    var searchQuery by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    val searchFocus = remember { FocusRequester() }
    val filteredTemplates = remember(templates, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) templates
        else templates.filter { it.title.contains(q, ignoreCase = true) }
    }
    val projectBoards = remember(state.boards, projectId) {
        state.boards.filter { it.projectId == projectId && !it.isArchived }
    }
    val projectTasks = remember(state.tasks, state.columns, state.boards, projectId) {
        val boardIds = state.boards.filter { it.projectId == projectId }.map { it.id }.toSet()
        val columnIds = state.columns.filter { it.boardId in boardIds }.map { it.id }.toSet()
        state.tasks.filter { it.columnId in columnIds && !it.isHidden }
    }

    LaunchedEffect(projectId) {
        viewModel.ensureTodayRecurringInstances()
    }

    var dialogTemplate by remember { mutableStateOf<RecurringTemplate?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var selectMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var addTargetGroupId by remember { mutableStateOf<String?>(null) }
    var renameGroupId by remember { mutableStateOf<String?>(null) }
    var renameGroupDraft by remember { mutableStateOf("") }
    var groupMetaTick by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val hubPrefs = remember { KairosPreferences(context) }
    BackHandler(enabled = selectMode) {
        selectMode = false
        selectedIds = emptySet()
        addTargetGroupId = null
    }

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
                },
                actions = {
                    if (templates.isNotEmpty()) {
                        IconButton(onClick = {
                            searchOpen = !searchOpen
                            if (!searchOpen) searchQuery = ""
                        }) {
                            Icon(
                                if (searchOpen) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = s.searchRecurring,
                                tint = if (searchOpen || searchQuery.isNotBlank()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!selectMode) {
                FloatingActionButton(
                    onClick = { showCreate = true },
                    modifier = Modifier.border(1.dp, buttonLine(), RoundedCornerShape(10.dp)),
                    shape = RoundedCornerShape(10.dp),
                    containerColor = buttonFill(),
                    contentColor = buttonLabel()
                ) {
                    Icon(Icons.Default.Add, contentDescription = s.recurringAdd)
                }
            }
        },
        bottomBar = {
            if (selectMode) {
                Surface(tonalElevation = 3.dp, shadowElevation = 4.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.selectedCount(selectedIds.size),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            selectMode = false
                            selectedIds = emptySet()
                            addTargetGroupId = null
                        }) {
                            Icon(Icons.Default.Close, contentDescription = s.cancelSelection)
                        }
                        val canUngroup = addTargetGroupId == null && templates.any {
                            it.id in selectedIds && !it.hubGroupId.isNullOrBlank()
                        }
                        if (canUngroup) {
                            IconButton(onClick = {
                                clearEmptyRecurringGroupMeta(templates, selectedIds, hubPrefs)
                                viewModel.ungroupRecurringTemplates(selectedIds)
                                groupMetaTick++
                                selectMode = false
                                selectedIds = emptySet()
                            }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = s.ungroupTasks,
                                    modifier = Modifier.hubGroupMarkGoldFill(),
                                    tint = Color.White
                                )
                            }
                        }
                        IconButton(onClick = {
                            val target = addTargetGroupId
                            val picked = templates.filter { it.id in selectedIds }
                            if (target != null) {
                                val seedColumn = templates.firstOrNull { it.hubGroupId == target }?.targetColumnId
                                val toAdd = picked.filter { it.hubGroupId != target }
                                when {
                                    toAdd.isEmpty() ->
                                        Toast.makeText(context, s.addToGroupNeedTasks, Toast.LENGTH_SHORT).show()
                                    seedColumn != null && toAdd.any { it.targetColumnId != seedColumn } ->
                                        Toast.makeText(context, s.groupSameHubOnly, Toast.LENGTH_SHORT).show()
                                    viewModel.addRecurringTemplatesToGroup(target, toAdd.map { it.id }) -> {
                                        clearEmptyRecurringGroupMeta(
                                            templates,
                                            toAdd.map { it.id }.toSet(),
                                            hubPrefs
                                        )
                                        groupMetaTick++
                                        selectMode = false
                                        selectedIds = emptySet()
                                        addTargetGroupId = null
                                    }
                                }
                            } else when {
                                picked.map { it.targetColumnId }.distinct().size > 1 ->
                                    Toast.makeText(context, s.groupSameHubOnly, Toast.LENGTH_SHORT).show()
                                viewModel.groupRecurringTemplates(selectedIds) == null ->
                                    Toast.makeText(context, s.groupNeedTwo, Toast.LENGTH_SHORT).show()
                                else -> {
                                    selectMode = false
                                    selectedIds = emptySet()
                                }
                            }
                        }) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (searchOpen) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .focusRequester(searchFocus),
                        singleLine = true,
                        placeholder = { Text(s.searchRecurring) }
                    )
                    LaunchedEffect(searchOpen) {
                        if (searchOpen) searchFocus.requestFocus()
                    }
                }
                if (filteredTemplates.isEmpty()) {
                    Text(
                        s.noMatchingTasks,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val searching = searchQuery.isNotBlank()
                    val recurringRows = remember(filteredTemplates, groupMetaTick, searching) {
                        recurringListRows(orderRecurringForGroups(filteredTemplates)).filter { row ->
                            if (row !is RecurringRow.Entry || !row.inGroup || searching) true
                            else {
                                val gid = row.template.hubGroupId
                                gid == null || !hubPrefs.isRecurringGroupCollapsed(gid)
                            }
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        itemsIndexed(
                            recurringRows,
                            key = { _, row ->
                                when (row) {
                                    is RecurringRow.Header -> "group-${row.groupId}"
                                    is RecurringRow.Entry -> row.template.id
                                }
                            }
                        ) { index, row ->
                            when (row) {
                                is RecurringRow.Header -> {
                                    val collapsed = hubPrefs.isRecurringGroupCollapsed(row.groupId)
                                        .also { groupMetaTick }
                                    val customTitle = hubPrefs.hubGroupTitle(row.groupId)
                                        .also { groupMetaTick }
                                    val next = recurringRows.getOrNull(index + 1)
                                    val groupOpen = !collapsed &&
                                        next is RecurringRow.Entry &&
                                        next.template.hubGroupId == row.groupId
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .hubGroupPlate(
                                                MaterialTheme.colorScheme.surface.luminance() < 0.5f
                                            )
                                            .padding(start = 8.dp, end = 0.dp, top = 0.dp, bottom = 0.dp)
                                            .hubGroupCapBackground()
                                            .padding(start = 10.dp, end = 2.dp, top = 6.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    hubPrefs.setRecurringGroupCollapsed(row.groupId, !collapsed)
                                                    groupMetaTick++
                                                }
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Text(
                                                customTitle.ifBlank { s.hubGroupLabel },
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontFamily = LiterataBold,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                s.hubGroupMembers(row.count),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                renameGroupId = row.groupId
                                                renameGroupDraft = customTitle
                                            },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = s.hubGroupRename,
                                                modifier = Modifier.hubGroupGoldFill(),
                                                tint = Color.White
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                addTargetGroupId = row.groupId
                                                selectMode = true
                                                selectedIds = emptySet()
                                                Toast.makeText(
                                                    context,
                                                    s.addToGroupHint,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = s.addToGroup,
                                                modifier = Modifier.hubGroupMarkGoldFill(),
                                                tint = Color.White
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.ungroupRecurringTemplates(
                                                    templates.filter { it.hubGroupId == row.groupId }.map { it.id }
                                                )
                                                hubPrefs.clearHubGroupMeta(row.groupId)
                                                groupMetaTick++
                                            },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = s.ungroupTasks,
                                                modifier = Modifier.hubGroupMarkGoldFill(),
                                                tint = Color.White
                                            )
                                        }
                                    }
                                    if (!groupOpen) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                    }
                                }
                                is RecurringRow.Entry -> {
                                    val template = row.template
                                    val next = recurringRows.getOrNull(index + 1)
                                    val groupEnds = row.inGroup && (
                                        next !is RecurringRow.Entry ||
                                            next.template.hubGroupId != template.hubGroupId
                                        )
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (row.inGroup) {
                                                    Modifier
                                                        .hubGroupPlate(
                                                            MaterialTheme.colorScheme.surface.luminance() < 0.5f
                                                        )
                                                        .padding(horizontal = 8.dp)
                                                } else {
                                                    Modifier
                                                }
                                            )
                                    ) {
                                    RecurringTemplateRow(
                                        template = template,
                                        boards = state.boards,
                                        columns = state.columns,
                                        selectionMode = selectMode,
                                        isSelected = template.id in selectedIds,
                                        showDivider = next !is RecurringRow.Entry ||
                                            next.template.hubGroupId != template.hubGroupId,
                                        onClick = {
                                            if (selectMode) {
                                                selectedIds = if (template.id in selectedIds) {
                                                    selectedIds - template.id
                                                } else {
                                                    selectedIds + template.id
                                                }
                                            } else {
                                                dialogTemplate = template
                                            }
                                        },
                                        onLongClick = {
                                            selectMode = true
                                            selectedIds = if (template.id in selectedIds) {
                                                selectedIds
                                            } else {
                                                selectedIds + template.id
                                            }
                                        },
                                        onToggle = { enabled ->
                                            viewModel.setRecurringTemplateEnabled(template.id, enabled)
                                        },
                                        onDelete = {
                                            viewModel.deleteRecurringTemplate(template.id)
                                        },
                                        onRemoveFromGroup = if (row.inGroup) {
                                            {
                                                val gid = template.hubGroupId
                                                viewModel.ungroupRecurringTemplates(listOf(template.id))
                                                if (gid != null &&
                                                    templates.none { it.hubGroupId == gid && it.id != template.id }
                                                ) {
                                                    hubPrefs.clearHubGroupMeta(gid)
                                                    groupMetaTick++
                                                }
                                            }
                                        } else {
                                            null
                                        }
                                    )
                                    if (row.inGroup && !groupEnds) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                    }
                                    if (groupEnds) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(72.dp)) }
                    }
                }
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
            contacts = state.contacts.filter { it.projectId == projectId },
            onImportContact = { name, phone, email, social ->
                viewModel.addContact(projectId, name, phone, email, "", social)
            },
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
            contacts = state.contacts.filter { it.projectId == projectId },
            onImportContact = { name, phone, email, social ->
                viewModel.addContact(projectId, name, phone, email, "", social)
            },
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
                    Text(s.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameGroupId = null }) {
                    Text(s.cancel)
                }
            }
        )
    }
}

private sealed class RecurringRow {
    data class Header(val groupId: String, val count: Int) : RecurringRow()
    data class Entry(val template: RecurringTemplate, val inGroup: Boolean) : RecurringRow()
}

private fun clearEmptyRecurringGroupMeta(
    templates: List<RecurringTemplate>,
    leavingIds: Set<String>,
    prefs: KairosPreferences
) {
    val leavingGroups = templates
        .filter { it.id in leavingIds }
        .mapNotNull { it.hubGroupId?.takeIf { id -> id.isNotBlank() } }
        .toSet()
    leavingGroups.forEach { gid ->
        val remains = templates.any { it.hubGroupId == gid && it.id !in leavingIds }
        if (!remains) prefs.clearHubGroupMeta(gid)
    }
}

private fun orderRecurringForGroups(templates: List<RecurringTemplate>): List<RecurringTemplate> =
    HubGroupOrdering.groupsFirst(templates) { it.hubGroupId }

private fun recurringListRows(templates: List<RecurringTemplate>): List<RecurringRow> {
    val rows = mutableListOf<RecurringRow>()
    var index = 0
    while (index < templates.size) {
        val template = templates[index]
        val groupId = template.hubGroupId?.takeIf { it.isNotBlank() }
        val block = if (groupId == null) {
            listOf(template)
        } else {
            templates.drop(index).takeWhile { it.hubGroupId == groupId }
        }
        if (groupId != null && block.size >= 2) {
            rows += RecurringRow.Header(groupId, block.size)
            block.forEach { rows += RecurringRow.Entry(it, inGroup = true) }
            index += block.size
        } else {
            rows += RecurringRow.Entry(template, inGroup = false)
            index++
        }
    }
    return rows
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecurringTemplateRow(
    template: RecurringTemplate,
    boards: List<Board>,
    columns: List<Column>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onRemoveFromGroup: (() -> Unit)? = null,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    showDivider: Boolean = true
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val prefs = remember { KairosPreferences(context) }
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
            .then(
                if (isSelected) {
                    Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                } else {
                    Modifier
                }
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                },
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
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
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(iterations = Int.MAX_VALUE)
            )
            val timeLabel = RecurringDefaults.effectiveReminderTimes(
                stored = template.reminderTimesOfDay,
                reminderDefault = prefs.reminderMinutesOfDay
            ).joinToString(" ") { DueReminderScheduler.formatMinutesOfDay(it) }
                .ifBlank { null }
            val startDay = EnsureRecurringInstancesUseCase.effectiveStartsOnDay(
                template.startsOnDayMillis,
                template.createdAt
            )
            val startLabel = "${s.recurringStartsOn} ${startDay.formatDateShort()}"
            val deadlineLabel = deadlineOffsetLabel(
                template.deadlineOffsetDays,
                template.deadlineOffsetHours,
                s.recurringDeadlineDaysShort,
                s.recurringDeadlineHoursShort
            )?.let { "${s.deadlineLabel} $it" }
            val eisenhowerLabel = template.eisenhowerQuadrant?.takeIf {
                template.showEisenhowerButtons || template.eisenhowerQuadrant != null
            }
            val impactLabel = template.complexity?.let { "${s.taskComplexity} $it" }
            Text(
                listOfNotNull(
                    rhythmLabel(template),
                    timeLabel,
                    startLabel,
                    deadlineLabel,
                    eisenhowerLabel,
                    impactLabel,
                    dest
                ).joinToString(" · "),
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
                if (onRemoveFromGroup != null) {
                    DropdownMenuItem(
                        text = {
                            Text(s.removeFromGroup)
                        },
                        onClick = {
                            menuOpen = false
                            onRemoveFromGroup()
                        }
                    )
                }
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
    if (showDivider) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { DialogMarqueeText(text = s.recurringDelete, fontWeight = FontWeight.Bold, maxLines = 2) },
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
            s.recurringEveryNDaysLabel(RecurringRhythm.intervalDays(template.timesPerWeek))
        else -> template.rhythm
    }
}

private fun deadlineOffsetLabel(
    days: Int?,
    hours: Int?,
    daysWord: String,
    hoursWord: String
): String? {
    val d = (days ?: 0).coerceAtLeast(0)
    val h = (hours ?: 0).coerceAtLeast(0)
    if (d == 0 && h == 0) return null
    return buildList {
        if (d > 0) add("$d $daysWord")
        if (h > 0) add("$h $hoursWord")
    }.joinToString(" ")
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
    contacts: List<Contact> = emptyList(),
    onImportContact: (name: String, phone: String, email: String, social: String) -> String = { _, _, _, _ -> "" },
    defaultBoardId: String?,
    defaultColumnId: String?,
    onDismiss: () -> Unit,
    onSave: (RecurringTemplate) -> Unit,
    onDelete: (() -> Unit)?
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val prefs = remember { KairosPreferences(context) }
    val defaultReminderMinutes = prefs.reminderMinutesOfDay.coerceIn(0, 24 * 60 - 1)
    val keyboard = LocalSoftwareKeyboardController.current
    val titleFocusRequester = remember { FocusRequester() }
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var cardType by remember(initial) { mutableStateOf(TaskCardKind.normalize(initial?.cardType)) }
    var cardPayload by remember(initial) { mutableStateOf(initial?.cardPayload.orEmpty()) }
    var showCardSetup by remember(initial) { mutableStateOf(false) }
    var setupKind by remember(initial) { mutableStateOf(TaskCardKind.PLAIN) }
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
        mutableIntStateOf(RecurringRhythm.intervalDays(initial?.timesPerWeek))
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
    var quadrant by remember(initial) { mutableStateOf(initial?.eisenhowerQuadrant) }
    var complexity by remember(initial) { mutableStateOf(initial?.complexity) }
    var remindBefore by remember(initial) {
        mutableStateOf((initial?.remindLeadAmount ?: 0) > 0)
    }
    var remindAmount by remember(initial) {
        mutableStateOf((initial?.remindLeadAmount ?: 1).coerceAtLeast(1))
    }
    var remindUnit by remember(initial) {
        mutableStateOf(
            RemindLeadUnit.fromId(initial?.remindLeadUnit) ?: RemindLeadUnit.DAY
        )
    }
    var reminderTimes by remember(initial) {
        mutableStateOf(
            RecurringDefaults.effectiveReminderTimes(
                stored = initial?.reminderTimesOfDay.orEmpty(),
                reminderDefault = defaultReminderMinutes
            )
        )
    }
    var startsOnDayMillis by remember(initial) {
        mutableStateOf(
            EnsureRecurringInstancesUseCase.effectiveStartsOnDay(
                initial?.startsOnDayMillis ?: 0L,
                initial?.createdAt ?: System.currentTimeMillis()
            )
        )
    }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var deadlineDays by remember(initial) {
        mutableIntStateOf((initial?.deadlineOffsetDays ?: 0).coerceIn(0, 999))
    }
    var deadlineHours by remember(initial) {
        mutableIntStateOf((initial?.deadlineOffsetHours ?: 0).coerceIn(0, 999))
    }
    var linkParentIds by remember(initial) {
        mutableStateOf(initial?.linkParentIds?.toSet().orEmpty())
    }
    var linkChildIds by remember(initial) {
        mutableStateOf(initial?.linkChildIds?.toSet().orEmpty())
    }
    var linkPickMode by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var openBlock by remember { mutableStateOf<TaskDialogBlock?>(null) }
    var timerMinutes by remember(initial) {
        mutableStateOf(initial?.timerMinutes?.takeIf { it > 0 })
    }

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
        containerColor = dialogSheetFill(),
        tonalElevation = DialogSheetTonalElevation,
        title = {
            DialogMarqueeText(
                text = if (initial == null) s.recurringAdd else s.recurringEdit,
                fontWeight = FontWeight.Bold,
                maxLines = 2
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

                DialogPanel {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuietSegments(
                            labels = listOf(s.recurringDaily, s.recurringWeekdays, s.recurringEveryNDays),
                            selectedIndex = when (rhythm) {
                                RecurringRhythm.WEEKDAYS -> 1
                                RecurringRhythm.EVERY_N_DAYS -> 2
                                else -> 0
                            },
                            onSelect = { index ->
                                rhythm = when (index) {
                                    1 -> RecurringRhythm.WEEKDAYS
                                    2 -> RecurringRhythm.EVERY_N_DAYS
                                    else -> RecurringRhythm.DAILY
                                }
                            }
                        )

                        if (rhythm == RecurringRhythm.WEEKDAYS) {
                            val dayLabels = listOf(
                                1 to s.weekdayMon, 2 to s.weekdayTue, 3 to s.weekdayWed,
                                4 to s.weekdayThu, 5 to s.weekdayFri, 6 to s.weekdaySat, 7 to s.weekdaySun
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                dayLabels.forEach { (day, label) ->
                                    QuietMark(
                                        label = label,
                                        selected = day in weekdays,
                                        onClick = {
                                            weekdays = if (day in weekdays) weekdays - day else weekdays + day
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        if (rhythm == RecurringRhythm.EVERY_N_DAYS) {
                            QuietStepper(
                                valueText = intervalDays.toString(),
                                onMinus = {
                                    intervalDays = (intervalDays - 1)
                                        .coerceAtLeast(RecurringRhythm.INTERVAL_MIN)
                                },
                                onPlus = {
                                    intervalDays = (intervalDays + 1)
                                        .coerceAtMost(RecurringRhythm.INTERVAL_MAX)
                                },
                                minusEnabled = intervalDays > RecurringRhythm.INTERVAL_MIN,
                                plusEnabled = intervalDays < RecurringRhythm.INTERVAL_MAX,
                                suffix = s.recurringDeadlineDaysShort
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(2, 3, 7, 14, 30).forEach { n ->
                                    QuietMark(
                                        label = n.toString(),
                                        selected = intervalDays == n,
                                        onClick = { intervalDays = n },
                                        modifier = Modifier.width(36.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                val whenSlots = reminderTimes.joinToString(" ") {
                    DueReminderScheduler.formatMinutesOfDay(it)
                }
                val startDay = EnsureRecurringInstancesUseCase.startOfDay(startsOnDayMillis)
                val todayStart = EnsureRecurringInstancesUseCase.startOfDay(System.currentTimeMillis())
                val whenDateBit = if (startDay != todayStart) startsOnDayMillis.formatDateShort() else null
                TaskCardTypeDisclosure(
                    kind = cardType,
                    expanded = openBlock == TaskDialogBlock.Type,
                    onExpandedChange = { open ->
                        openBlock = if (open) TaskDialogBlock.Type else null
                    },
                    onPick = { kind ->
                        if (kind == TaskCardKind.PLAIN) {
                            cardType = TaskCardKind.PLAIN
                            cardPayload = ""
                        } else {
                            setupKind = kind
                            showCardSetup = true
                        }
                    }
                )
                DialogDisclosureRow(
                    label = s.whenRowLabel,
                    value = listOfNotNull(whenSlots.takeIf { it.isNotBlank() }, whenDateBit)
                        .joinToString(" · "),
                    expanded = openBlock == TaskDialogBlock.When,
                    onExpandedChange = { open ->
                        openBlock = if (open) TaskDialogBlock.When else null
                    },
                    icon = Icons.Default.Schedule,
                    emphasizeValue = true
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        reminderTimes.forEach { minutes ->
                            QuietTimeToken(
                                label = DueReminderScheduler.formatMinutesOfDay(minutes),
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
                                onRemove = if (reminderTimes.size > 1) {
                                    { reminderTimes = reminderTimes.filterNot { it == minutes } }
                                } else {
                                    null
                                },
                                removeLabel = s.delete
                            )
                        }
                        QuietTextAction(
                            label = "+ ${s.recurringTimeAdd}",
                            onClick = {
                                val seed = reminderTimes.lastOrNull() ?: defaultReminderMinutes
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
                            }
                        )
                    }
                    QuietRemindBefore(
                        enabled = remindBefore,
                        amount = remindAmount,
                        unit = remindUnit,
                        onEnabledChange = { remindBefore = it },
                        onAmountChange = { remindAmount = it },
                        onUnitChange = { remindUnit = it }
                    )
                    DialogActionLine(
                        label = s.recurringStartsOn,
                        value = startsOnDayMillis.formatDateShort(),
                        onClick = { showStartDatePicker = true }
                    )
                }

                val offsetSummary = deadlineOffsetLabel(
                    days = deadlineDays,
                    hours = deadlineHours,
                    daysWord = s.recurringDeadlineDaysShort,
                    hoursWord = s.recurringDeadlineHoursShort
                )?.let { "+$it" }
                DialogDisclosureRow(
                    label = s.deadlineRowLabel,
                    value = offsetSummary ?: s.deadlineNone,
                    expanded = openBlock == TaskDialogBlock.Deadline,
                    onExpandedChange = { open ->
                        openBlock = if (open) TaskDialogBlock.Deadline else null
                    },
                    icon = Icons.Default.DateRange,
                    emphasizeValue = true
                ) {
                    RecurringDeadlinePlate(
                        deadlineDays = deadlineDays,
                        deadlineHours = deadlineHours,
                        onDaysChange = { deadlineDays = it },
                        onHoursChange = { deadlineHours = it },
                        onClearOffset = {
                            deadlineDays = 0
                            deadlineHours = 0
                        }
                    )
                }

                DialogDisclosureRow(
                    label = s.timerRowLabel,
                    value = formatTimerClosedSummary(
                        timerMinutes = timerMinutes,
                        noneLabel = s.timerNone,
                        valueLabel = s::timerMinutesValue
                    ),
                    expanded = openBlock == TaskDialogBlock.Timer,
                    onExpandedChange = { open ->
                        openBlock = if (open) TaskDialogBlock.Timer else null
                    },
                    icon = Icons.Default.Timer,
                    emphasizeValue = true
                ) {
                    TaskTimerPlate(
                        timerMinutes = timerMinutes,
                        onTimerMinutesChange = { timerMinutes = it }
                    )
                }

                val whereBoard = boards.find { it.id == boardId }?.let { s.localized(it.name) }
                val whereHub = boardColumns.find { it.id == columnId }?.let { s.localized(it.title) }
                DialogDisclosureRow(
                    label = s.whereRowLabel,
                    value = listOfNotNull(whereBoard, whereHub).joinToString(" · ")
                        .ifBlank { s.recurringNoBoards },
                    expanded = openBlock == TaskDialogBlock.Where,
                    onExpandedChange = { open ->
                        openBlock = if (open) TaskDialogBlock.Where else null
                    },
                    icon = Icons.Default.Place,
                    emphasizeValue = true
                ) {
                    if (boards.isEmpty()) {
                        Text(
                            s.recurringNoBoards,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            QuietCaption(s.board)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                boards.forEach { board ->
                                    QuietChoice(
                                        label = s.localized(board.name),
                                        selected = boardId == board.id,
                                        onClick = {
                                            boardId = board.id
                                            columnId = columns
                                                .filter { it.boardId == board.id }
                                                .minByOrNull { it.position }
                                                ?.id
                                                .orEmpty()
                                        }
                                    )
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            QuietCaption(s.hub)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                boardColumns.forEach { col ->
                                    QuietChoice(
                                        label = s.localized(col.title),
                                        selected = columnId == col.id,
                                        onClick = { columnId = col.id }
                                    )
                                }
                            }
                        }
                    }
                }

                DialogDisclosureRow(
                    label = s.eisenhowerButtons,
                    value = Eisenhower.getBadge(quadrant).ifBlank { s.deadlineNone },
                    expanded = openBlock == TaskDialogBlock.Priority,
                    onExpandedChange = { open ->
                        openBlock = if (open) TaskDialogBlock.Priority else null
                    },
                    icon = Icons.Default.PriorityHigh,
                    emphasizeValue = true
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Eisenhower.Q1,
                            Eisenhower.Q2,
                            Eisenhower.Q3,
                            Eisenhower.Q4
                        ).forEach { q ->
                            EisenhowerQuadrantChip(
                                qKey = q,
                                qLabel = Eisenhower.getBadge(q),
                                isSelected = quadrant == q,
                                dimUnselected = quadrant != null && quadrant != q,
                                onClick = {
                                    quadrant = if (quadrant == q) null else q
                                }
                            )
                        }
                    }
                }
                DialogDisclosureRow(
                    label = s.taskComplexity,
                    value = complexity?.toString() ?: s.deadlineNone,
                    expanded = openBlock == TaskDialogBlock.Impact,
                    onExpandedChange = { open ->
                        openBlock = if (open) TaskDialogBlock.Impact else null
                    },
                    icon = Icons.Default.Bolt,
                    emphasizeValue = true
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..5).forEach { n ->
                            QuietMark(
                                label = n.toString(),
                                selected = complexity == n,
                                onClick = {
                                    complexity = if (complexity == n) null else n
                                },
                                modifier = Modifier.width(36.dp)
                            )
                        }
                    }
                }
                DialogDisclosureRow(
                    label = s.recurringActive,
                    value = if (enabled) s.recurringActive else s.timerNone,
                    expanded = openBlock == TaskDialogBlock.Active,
                    onExpandedChange = { open ->
                        openBlock = if (open) TaskDialogBlock.Active else null
                    },
                    icon = Icons.Default.CheckCircle,
                    emphasizeValue = true
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(s.recurringActive)
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }
                }
                DialogDisclosureRow(
                    label = s.taskParents,
                    value = when (linkParentIds.size) {
                        0 -> s.deadlineNone
                        1 -> tasksById[linkParentIds.first()]?.title ?: s.deadlineNone
                        else -> s.linkedToGoalsCount(linkParentIds.size)
                    },
                    expanded = openBlock == TaskDialogBlock.Goals,
                    onExpandedChange = { open ->
                        openBlock = if (open) TaskDialogBlock.Goals else null
                    },
                    icon = Icons.Default.Flag,
                    emphasizeValue = true
                ) {
                    linkParentIds.forEach { parentId ->
                        val parentTitle = tasksById[parentId]?.title ?: parentId
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = s.linkedToGoal(parentTitle),
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
                }
                DialogDisclosureRow(
                    label = s.taskChildren,
                    value = when (linkChildIds.size) {
                        0 -> s.deadlineNone
                        1 -> tasksById[linkChildIds.first()]?.title ?: s.deadlineNone
                        else -> linkChildIds.size.toString()
                    },
                    expanded = openBlock == TaskDialogBlock.Steps,
                    onExpandedChange = { open ->
                        openBlock = if (open) TaskDialogBlock.Steps else null
                    },
                    icon = Icons.Default.AccountTree,
                    emphasizeValue = true
                ) {
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
                }

                if (onDelete != null) {
                    DialogPanel {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable { confirmDelete = true }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(s.delete, color = deleteButtonColor())
                        }
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
                    val offsetDays = deadlineDays.coerceIn(0, 999)
                    val offsetHours = deadlineHours.coerceIn(0, 999)
                    val hasDeadlineOffset = offsetDays > 0 || offsetHours > 0
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
                            reminderTimesOfDay = reminderTimes.ifEmpty {
                                listOf(defaultReminderMinutes)
                            },
                            eisenhowerQuadrant = quadrant,
                            showEisenhowerButtons = true,
                            linkParentIds = linkParentIds.toList(),
                            linkChildIds = linkChildIds.toList(),
                            complexity = complexity,
                            skippedOccurrenceKeys = initial?.skippedOccurrenceKeys.orEmpty(),
                            startsOnDayMillis = EnsureRecurringInstancesUseCase.startOfDay(startsOnDayMillis),
                            deadlineOffsetDays = offsetDays.takeIf { hasDeadlineOffset },
                            deadlineOffsetHours = offsetHours.takeIf { hasDeadlineOffset },
                            createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                            remindLeadAmount = if (remindBefore) remindAmount else null,
                            remindLeadUnit = if (remindBefore) remindUnit.id else null,
                            timerMinutes = timerMinutes?.takeIf { it > 0 },
                            hubGroupId = initial?.hubGroupId,
                            cardType = cardType,
                            cardPayload = if (cardType == TaskCardKind.PLAIN) "" else cardPayload
                        )
                    )
                },
                enabled = title.isNotBlank() && boardId.isNotBlank() && columnId.isNotBlank() &&
                    (rhythm != RecurringRhythm.WEEKDAYS || weekdays.isNotEmpty()) &&
                    (rhythm != RecurringRhythm.EVERY_N_DAYS ||
                        intervalDays in RecurringRhythm.INTERVAL_MIN..RecurringRhythm.INTERVAL_MAX)
            ) { Text(s.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.cancel) }
        }
    )

    if (showStartDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = localMillisToUtcPicker(startsOnDayMillis)
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let {
                            startsOnDayMillis = EnsureRecurringInstancesUseCase.startOfDay(
                                utcPickerMillisToLocalNoon(it)
                            )
                        }
                        showStartDatePicker = false
                    }
                ) { Text(s.save) }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text(s.cancel) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

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
                        candidate.isGoal &&
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
            emptyMessage = if (pickMode == "parent") s.noGoalsToLink else s.noTasksToLink,
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
            title = { DialogMarqueeText(text = s.delete, fontWeight = FontWeight.Bold, maxLines = 2) },
            text = { Text(s.recurringDeleteConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(s.delete, color = deleteButtonColor()) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(s.cancel) }
            }
        )
    }

    if (showCardSetup) {
        val initialBody = if (TaskCardKind.normalize(cardType) == setupKind) {
            TaskCardBody.decode(setupKind, cardPayload).copy(kind = setupKind)
        } else {
            TaskCardBody(kind = setupKind)
        }
        TaskCardSetupDialog(
            initial = initialBody,
            contacts = contacts,
            onImportContact = onImportContact,
            onDismiss = { showCardSetup = false },
            onDone = { body ->
                cardType = body.kind
                cardPayload = body.encode()
                showCardSetup = false
            }
        )
    }
}

@Composable
private fun RecurringDeadlinePlate(
    deadlineDays: Int,
    deadlineHours: Int,
    onDaysChange: (Int) -> Unit,
    onHoursChange: (Int) -> Unit,
    onClearOffset: () -> Unit
) {
    val s = LocalAppStrings.current
    val hasOffset = deadlineDays > 0 || deadlineHours > 0
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuietStepperLine(
            label = s.recurringDeadlineDays,
            valueText = deadlineDays.toString(),
            onMinus = { onDaysChange((deadlineDays - 1).coerceAtLeast(0)) },
            onPlus = { onDaysChange((deadlineDays + 1).coerceAtMost(999)) },
            minusEnabled = deadlineDays > 0,
            plusEnabled = deadlineDays < 999
        )
        QuietStepperLine(
            label = s.recurringDeadlineHours,
            valueText = deadlineHours.toString(),
            onMinus = { onHoursChange((deadlineHours - 1).coerceAtLeast(0)) },
            onPlus = { onHoursChange((deadlineHours + 1).coerceAtMost(999)) },
            minusEnabled = deadlineHours > 0,
            plusEnabled = deadlineHours < 999
        )
        if (hasOffset) {
            TextButton(onClick = onClearOffset) {
                Text(s.recurringDeadlineClear)
            }
        }
    }
}

@Composable
private fun QuietCaption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun QuietSegments(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(scheme.surface)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(if (singleLine) 36.dp else 44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) scheme.primary else Color.Transparent)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) scheme.onPrimary else scheme.onSurface,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    maxLines = if (singleLine) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun QuietMark(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) scheme.primary else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else buttonLine(),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) scheme.onPrimary else scheme.onSurface,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            maxLines = 1
        )
    }
}

@Composable
private fun QuietChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) scheme.primary else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else buttonLine(),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) scheme.onPrimary else scheme.onSurface,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QuietStepButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = 1.dp,
                color = buttonLine().copy(alpha = if (enabled) 1f else 0.35f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = scheme.onSurface.copy(alpha = if (enabled) 1f else 0.35f),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun QuietStepper(
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusEnabled: Boolean,
    plusEnabled: Boolean,
    suffix: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuietStepButton(label = "−", enabled = minusEnabled, onClick = onMinus)
        Text(
            text = valueText,
            modifier = Modifier.widthIn(min = 28.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurface
        )
        QuietStepButton(label = "+", enabled = plusEnabled, onClick = onPlus)
        if (suffix != null) {
            Text(
                text = suffix,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuietStepperLine(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusEnabled: Boolean,
    plusEnabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        QuietStepper(
            valueText = valueText,
            onMinus = onMinus,
            onPlus = onPlus,
            minusEnabled = minusEnabled,
            plusEnabled = plusEnabled
        )
    }
}

@Composable
private fun QuietTimeToken(
    label: String,
    onClick: () -> Unit,
    onRemove: (() -> Unit)?,
    removeLabel: String
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, buttonLine(), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = if (onRemove != null) 4.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            color = scheme.onSurface
        )
        if (onRemove != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = removeLabel,
                    modifier = Modifier.size(16.dp),
                    tint = scheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuietTextAction(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun QuietRemindBefore(
    enabled: Boolean,
    amount: Int,
    unit: RemindLeadUnit,
    onEnabledChange: (Boolean) -> Unit,
    onAmountChange: (Int) -> Unit,
    onUnitChange: (RemindLeadUnit) -> Unit
) {
    val s = LocalAppStrings.current
    val units = RemindLeadUnit.entries
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = s.remindBefore,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
        if (enabled) {
            QuietStepper(
                valueText = amount.coerceAtLeast(1).toString(),
                onMinus = { onAmountChange((amount - 1).coerceAtLeast(1)) },
                onPlus = { onAmountChange((amount + 1).coerceAtMost(999)) },
                minusEnabled = amount > 1,
                plusEnabled = amount < 999
            )
            QuietSegments(
                labels = units.map { s.remindUnitShort(it) },
                selectedIndex = units.indexOf(unit).coerceAtLeast(0),
                onSelect = { onUnitChange(units[it]) },
                singleLine = true
            )
            Text(
                text = s.remindBeforeEach(amount, s.remindUnitShort(unit)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

