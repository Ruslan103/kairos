package com.example.kaizenkanban.ui.board

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kaizenkanban.domain.model.CardLine
import com.example.kaizenkanban.domain.model.Contact
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.domain.model.TaskCardBody
import com.example.kaizenkanban.domain.model.TaskCardKind
import com.example.kaizenkanban.domain.model.TaskWorkflow
import com.example.kaizenkanban.reminders.TaskTimerScheduler
import com.example.kaizenkanban.ui.taskmeta.CompletionQualityDialog
import com.example.kaizenkanban.ui.viewmodel.SharedViewModel
import kotlinx.coroutines.launch
import com.example.kaizenkanban.ui.components.DialogChoicePill
import com.example.kaizenkanban.ui.components.DialogDisclosureRow
import com.example.kaizenkanban.ui.components.DialogSheetTonalElevation
import com.example.kaizenkanban.ui.components.dialogSheetFill
import com.example.kaizenkanban.ui.i18n.LocalAppStrings
import com.example.kaizenkanban.ui.theme.buttonFill
import com.example.kaizenkanban.ui.theme.buttonLabel
import com.example.kaizenkanban.ui.theme.buttonLine
import com.example.kaizenkanban.ui.theme.deleteButtonColor
import kotlinx.coroutines.delay

private val RoseCheck = Color(0xFFB76E79)
private val TrackIdle = Color(0x22000000)
private const val DefaultWorkoutSets = 4
private const val MaxWorkoutSets = 30

fun taskCardProgress(cardType: String, cardPayload: String): Pair<String, Float>? {
    if (TaskCardKind.normalize(cardType) == TaskCardKind.PLAIN) return null
    val body = TaskCardBody.decode(cardType, cardPayload)
    val (done, total) = body.progressCounts()
    return "$done/$total" to body.fraction()
}

@Composable
fun TaskCardProgressLine(
    label: String,
    fraction: Float,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val clamped = fraction.coerceIn(0f, 1f)
    val brush = when {
        clamped >= 0.999f -> Brush.horizontalGradient(
            listOf(
                Color(0xFF8A4010),
                Color(0xFFF0C14A),
                Color(0xFFFFF6C8),
                Color(0xFFF6D15A),
                Color(0xFFC37C22)
            )
        )
        clamped >= 0.5f -> Brush.horizontalGradient(
            listOf(Color(0xFF097969), Color(0xFF097969), Color(0xFF097969))
        )
        else -> Brush.horizontalGradient(
            listOf(Color(0xFF800020), Color(0xFFC48A96), Color(0xFF800020))
        )
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(TrackIdle)
        ) {
            if (clamped > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(clamped)
                        .background(brush)
                )
            }
        }
    }
}

@Composable
fun TaskCardTypeDisclosure(
    kind: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onPick: (String) -> Unit
) {
    val s = LocalAppStrings.current
    DialogDisclosureRow(
        label = s.cardTypeLabel,
        value = s.cardTypeName(kind),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        icon = Icons.Default.Category
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                TaskCardKind.PLAIN,
                TaskCardKind.WORKOUT,
                TaskCardKind.SHOP,
                TaskCardKind.MEET
            ).forEach { option ->
                DialogChoicePill(
                    selected = TaskCardKind.normalize(kind) == option,
                    onClick = { onPick(option) },
                    label = s.cardTypeName(option)
                )
            }
        }
    }
}

@Composable
fun TaskCardSetupDialog(
    initial: TaskCardBody,
    contacts: List<Contact>,
    onDismiss: () -> Unit,
    onDone: (TaskCardBody) -> Unit,
    onImportContact: (name: String, phone: String, email: String, social: String) -> String = { _, _, _, _ -> "" }
) {
    val s = LocalAppStrings.current
    val kind = TaskCardKind.normalize(initial.kind)
    var lines by remember(initial) { mutableStateOf(initial.lines) }
    var contactIds by remember(initial) { mutableStateOf(initial.contactIds) }
    var addedContacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var phoneDraft by remember { mutableStateOf<PhoneContactDraft?>(null) }
    val shownContacts = (contacts + addedContacts).uniqueByPerson()
    val pickPhone = rememberPhoneContactImport { phoneDraft = it }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogSheetFill(),
        tonalElevation = DialogSheetTonalElevation,
        shape = RoundedCornerShape(10.dp),
        title = { Text(s.cardTypeName(kind)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (kind) {
                    TaskCardKind.WORKOUT -> lines.forEach { line ->
                        WorkoutSetupRow(
                            line = line,
                            onChange = { next -> lines = lines.map { if (it.id == line.id) next else it } },
                            onRemove = { lines = lines.filter { it.id != line.id } }
                        )
                    }
                    TaskCardKind.SHOP -> lines.forEach { line ->
                        NamedExtraRow(
                            line = line,
                            nameLabel = s.cardItem,
                            extraLabel = s.cardQty,
                            onChange = { next -> lines = lines.map { if (it.id == line.id) next else it } },
                            onRemove = { lines = lines.filter { it.id != line.id } }
                        )
                    }
                    TaskCardKind.MEET -> {
                        Text(s.cardContact, style = MaterialTheme.typography.bodyMedium)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DialogChoicePill(
                                selected = false,
                                onClick = pickPhone,
                                label = s.cardFromPhone
                            )
                            DialogChoicePill(
                                selected = contactIds.isEmpty(),
                                onClick = { contactIds = emptyList() },
                                label = s.cardNoContact
                            )
                            shownContacts.forEach { contact ->
                                DialogChoicePill(
                                    selected = contact.id in contactIds,
                                    onClick = {
                                        contactIds = if (contact.id in contactIds) {
                                            contactIds.filter { it != contact.id }
                                        } else {
                                            contactIds + contact.id
                                        }
                                    },
                                    label = contact.name
                                )
                            }
                        }
                        lines.forEach { line ->
                            NamedExtraRow(
                                line = line,
                                nameLabel = s.cardAgenda,
                                extraLabel = null,
                                onChange = { next -> lines = lines.map { if (it.id == line.id) next else it } },
                                onRemove = { lines = lines.filter { it.id != line.id } }
                            )
                        }
                    }
                }
                if (kind != TaskCardKind.PLAIN) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TaskCardPlusButton(onClick = {
                            lines = lines + when (kind) {
                                TaskCardKind.WORKOUT -> CardLine(sets = List(DefaultWorkoutSets) { false })
                                else -> CardLine()
                            }
                        })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cleaned = lines.mapNotNull { line ->
                    if (line.text.isBlank()) return@mapNotNull null
                    if (kind == TaskCardKind.WORKOUT) {
                        val count = line.sets.size.coerceIn(1, MaxWorkoutSets)
                        line.copy(sets = List(count) { i -> line.sets.getOrNull(i) == true })
                    } else {
                        line
                    }
                }
                onDone(
                    initial.copy(
                        kind = kind,
                        lines = cleaned,
                        contactIds = contactIds
                    )
                )
            }) { Text(s.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.cancel) }
        }
    )
    phoneDraft?.let { draft ->
        PhoneContactConfirmDialog(
            draft = draft,
            onDismiss = { phoneDraft = null },
            onSave = { name, phone, social ->
                val existing = (contacts + addedContacts).matchPerson(name, phone)
                val id = existing?.id ?: onImportContact(name, phone, draft.email, social)
                if (existing == null && id.isNotBlank()) {
                    addedContacts = addedContacts + Contact(
                        id = id,
                        projectId = "",
                        name = name,
                        phone = phone,
                        email = draft.email,
                        social = social
                    )
                }
                if (id.isNotBlank() && id !in contactIds) {
                    contactIds = contactIds + id
                }
                phoneDraft = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCardScreen(
    taskId: String,
    viewModel: SharedViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val task = state.tasks.find { it.id == taskId }
    if (task == null) {
        LaunchedEffect(state.isInitialized) {
            if (state.isInitialized) onBack()
        }
        return
    }
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val projectId = state.boards.find { board ->
        state.columns.any { it.id == task.columnId && it.boardId == board.id }
    }?.projectId.orEmpty()
    val contacts = state.contacts.filter { it.projectId == projectId }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var qualityTask by remember { mutableStateOf<Task?>(null) }
    fun onBodyChange(body: TaskCardBody) {
        val current = state.tasks.find { it.id == taskId } ?: return
        val finished = body.isFullyChecked()
        val complete = finished && !current.isCompleted && !current.isNotDone
        val reopen = !finished && current.isCompleted && !current.isNotDone
        viewModel.updateTask(
            current.copy(
                cardType = body.kind,
                cardPayload = body.encode(),
                isCompleted = if (complete) true else if (reopen) false else current.isCompleted,
                completedAt = when {
                    complete -> System.currentTimeMillis()
                    reopen -> null
                    else -> current.completedAt
                },
                workflowStatus = when {
                    complete -> TaskWorkflow.DONE
                    reopen -> TaskWorkflow.OPEN
                    else -> current.workflowStatus
                },
                completionQuality = if (reopen) null else current.completionQuality
            )
        )
        if (complete) {
            TaskTimerScheduler.cancel(context, current.id)
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = s.taskMarkedDone,
                    actionLabel = s.rateQuality,
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    qualityTask = current.copy(
                        isCompleted = true,
                        workflowStatus = TaskWorkflow.DONE
                    )
                }
            }
        }
    }
    val onImportContact: (String, String, String, String) -> String = { name, phone, email, social ->
        viewModel.addContact(projectId, name, phone, email, "", social)
    }
    var body by remember(task.id) {
        mutableStateOf(TaskCardBody.decode(task.cardType, task.cardPayload))
    }
    fun push(next: TaskCardBody) {
        body = next
        onBodyChange(next)
    }
    var writeContact by remember { mutableStateOf<Contact?>(null) }
    var showClock by remember { mutableStateOf(false) }
    var showAddExercise by remember { mutableStateOf(false) }
    var editingLine by remember { mutableStateOf<CardLine?>(null) }
    var phoneDraft by remember { mutableStateOf<PhoneContactDraft?>(null) }
    var imported by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var draftLineIds by remember(task.id) { mutableStateOf(setOf<String>()) }
    val pickPhone = rememberPhoneContactImport { phoneDraft = it }
    var running by remember(task.id) { mutableStateOf(false) }
    var left by remember(task.id) { mutableIntStateOf(body.timerSeconds.coerceAtLeast(0)) }
    val kind = TaskCardKind.normalize(body.kind)
    val knownContacts = (contacts + imported).uniqueByPerson()
    val linkedContacts = body.contactIds.mapNotNull { id -> knownContacts.find { it.id == id } }

    LaunchedEffect(running, task.id) {
        while (running) {
            delay(1000)
            if (left <= 1) {
                left = 0
                running = false
            } else {
                left -= 1
            }
        }
    }

    val pageColor = MaterialTheme.colorScheme.background
    Scaffold(
        containerColor = pageColor,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (kind == TaskCardKind.MEET) {
                    TaskCardIconButton(
                        icon = Icons.Default.ContactPhone,
                        contentDescription = s.cardFromPhone,
                        onClick = pickPhone,
                        size = 56.dp
                    )
                }
                TaskCardPlusButton(
                    size = 56.dp,
                    onClick = {
                        if (kind == TaskCardKind.WORKOUT) {
                            showAddExercise = true
                        } else {
                            val added = CardLine()
                            draftLineIds = draftLineIds + added.id
                            push(body.copy(lines = body.lines + added))
                        }
                    }
                )
            }
        },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = pageColor),
                title = {
                    Text(
                        task.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                }
            )
        }
    ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 96.dp)
            ) {
                when (kind) {
                    TaskCardKind.WORKOUT -> {
                        WorkoutTimerRow(
                            seconds = if (running) left else body.timerSeconds,
                            running = running,
                            onDigits = {
                                running = false
                                showClock = true
                            },
                            onToggle = {
                                if (running) {
                                    running = false
                                } else if (body.timerSeconds > 0) {
                                    left = body.timerSeconds
                                    running = true
                                } else {
                                    showClock = true
                                }
                            }
                        )
                        body.lines.forEach { line ->
                            val allDone = line.sets.isNotEmpty() && line.sets.all { it }
                            val editing = line.id in draftLineIds || line.text.isBlank()
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (editing) {
                                        DraftLineField(
                                            value = line.text,
                                            placeholder = s.cardExercise,
                                            onValueChange = { text ->
                                                push(body.copy(lines = body.lines.map {
                                                    if (it.id == line.id) it.copy(text = text) else it
                                                }))
                                            },
                                            onFinish = { text ->
                                                if (text.isNotBlank()) {
                                                    draftLineIds = draftLineIds - line.id
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        ExpandableLineText(
                                            text = line.text,
                                            style = MaterialTheme.typography.bodyLarge,
                                            textDecoration = if (allDone) TextDecoration.LineThrough else null,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (line.extra.isNotBlank()) {
                                            Text(
                                                text = line.extra,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(start = 12.dp)
                                            )
                                        }
                                        LineEditButton(onClick = { editingLine = line })
                                    }
                                    WineDeleteButton(onClick = {
                                        draftLineIds = draftLineIds - line.id
                                        push(body.copy(lines = body.lines.filter { it.id != line.id }))
                                    })
                                }
                                WorkoutSetMarks(
                                    sets = line.sets,
                                    onToggle = { index ->
                                        val previousDone = index == 0 || line.sets[index - 1]
                                        if (!previousDone) return@WorkoutSetMarks
                                        val sets = if (line.sets[index]) {
                                            line.sets.mapIndexed { i, done -> if (i >= index) false else done }
                                        } else {
                                            line.sets.mapIndexed { i, done -> if (i == index) true else done }
                                        }
                                        push(body.copy(lines = body.lines.map {
                                            if (it.id == line.id) it.copy(sets = sets) else it
                                        }))
                                    }
                                )
                                CardRule()
                            }
                        }
                    }
                    TaskCardKind.SHOP -> body.lines.forEach { line ->
                        CheckLine(
                            checked = line.done,
                            title = line.text,
                            extra = line.extra,
                            placeholder = s.cardItem,
                            editing = line.id in draftLineIds || line.text.isBlank(),
                            onTitle = { text ->
                                push(body.copy(lines = body.lines.map {
                                    if (it.id == line.id) it.copy(text = text) else it
                                }))
                            },
                            onFinishEdit = { text ->
                                if (text.isNotBlank()) draftLineIds = draftLineIds - line.id
                            },
                            onEdit = { editingLine = line },
                            onToggle = {
                                push(body.copy(lines = body.lines.map {
                                    if (it.id == line.id) it.copy(done = !it.done) else it
                                }))
                            },
                            onRemove = {
                                draftLineIds = draftLineIds - line.id
                                push(body.copy(lines = body.lines.filter { it.id != line.id }))
                            }
                        )
                    }
                    TaskCardKind.MEET -> {
                        if (linkedContacts.isEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = s.cardNoContact,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            CardRule()
                        } else {
                            linkedContacts.forEach { person ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = person.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TaskCardIconButton(
                                        icon = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = s.cardWrite,
                                        onClick = { writeContact = person }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    TaskCardIconButton(
                                        icon = Icons.Default.Call,
                                        contentDescription = s.cardCall,
                                        onClick = { dialContact(context, person, s.cardNoPhone) }
                                    )
                                    WineDeleteButton(onClick = {
                                        push(body.copy(contactIds = body.contactIds.filter { it != person.id }))
                                    })
                                }
                                CardRule()
                            }
                        }
                        body.lines.forEach { line ->
                            CheckLine(
                                checked = line.done,
                                title = line.text,
                                extra = "",
                                placeholder = s.cardAgenda,
                                editing = line.id in draftLineIds || line.text.isBlank(),
                                onTitle = { text ->
                                    push(body.copy(lines = body.lines.map {
                                        if (it.id == line.id) it.copy(text = text) else it
                                    }))
                                },
                                onFinishEdit = { text ->
                                    if (text.isNotBlank()) draftLineIds = draftLineIds - line.id
                                },
                                onEdit = { editingLine = line },
                                onToggle = {
                                    push(body.copy(lines = body.lines.map {
                                        if (it.id == line.id) it.copy(done = !it.done) else it
                                    }))
                                },
                                onRemove = {
                                    draftLineIds = draftLineIds - line.id
                                    push(body.copy(lines = body.lines.filter { it.id != line.id }))
                                }
                            )
                        }
                        QuietField(
                            value = body.outcome,
                            placeholder = s.cardOutcome,
                            onValueChange = { push(body.copy(outcome = it)) },
                            modifier = Modifier.padding(top = 20.dp)
                        )
                    }
                }
            }
    }
    qualityTask?.let { rated ->
        CompletionQualityDialog(
            title = rated.title,
            current = state.tasks.find { it.id == rated.id }?.completionQuality,
            onDismiss = { qualityTask = null },
            onSave = { q -> viewModel.setTaskCompletionQuality(rated.id, q) }
        )
    }

    editingLine?.let { line ->
        if (kind == TaskCardKind.WORKOUT) {
            AddExerciseDialog(
                initialName = line.text,
                initialSets = line.sets.size.coerceAtLeast(1),
                initialExtra = line.extra,
                confirmLabel = s.save,
                onDismiss = { editingLine = null },
                onSave = { name, setCount, extra ->
                    val sets = List(setCount) { index -> line.sets.getOrElse(index) { false } }
                    push(
                        body.copy(
                            lines = body.lines.map {
                                if (it.id == line.id) it.copy(text = name, extra = extra, sets = sets) else it
                            }
                        )
                    )
                    editingLine = null
                }
            )
        } else {
            EditLineDialog(
                nameLabel = if (kind == TaskCardKind.SHOP) s.cardItem else s.cardAgenda,
                initialName = line.text,
                extraLabel = if (kind == TaskCardKind.SHOP) s.cardQty else null,
                initialExtra = line.extra,
                onDismiss = { editingLine = null },
                onSave = { name, extra ->
                    push(
                        body.copy(
                            lines = body.lines.map {
                                if (it.id == line.id) it.copy(text = name, extra = extra) else it
                            }
                        )
                    )
                    editingLine = null
                }
            )
        }
    }
    if (showAddExercise) {
        AddExerciseDialog(
            onDismiss = { showAddExercise = false },
            onSave = { name, setCount, extra ->
                push(
                    body.copy(
                        lines = body.lines + CardLine(
                            text = name,
                            extra = extra,
                            sets = List(setCount) { false }
                        )
                    )
                )
                showAddExercise = false
            }
        )
    }
    if (showClock) {
        TimerSetDialog(
            initialSeconds = body.timerSeconds,
            onDismiss = { showClock = false },
            onApply = { seconds ->
                left = seconds
                push(body.copy(timerSeconds = seconds))
                showClock = false
            }
        )
    }
    writeContact?.let { person ->
        ContactWriteDialog(contact = person, onDismiss = { writeContact = null })
    }
    phoneDraft?.let { draft ->
        PhoneContactConfirmDialog(
            draft = draft,
            onDismiss = { phoneDraft = null },
            onSave = { name, phone, social ->
                val existing = knownContacts.matchPerson(name, phone)
                val id = existing?.id ?: onImportContact(name, phone, draft.email, social)
                if (existing == null && id.isNotBlank()) {
                    imported = imported + Contact(
                        id = id,
                        projectId = "",
                        name = name,
                        phone = phone,
                        email = draft.email,
                        social = social
                    )
                }
                if (id.isNotBlank()) push(body.plusContact(id))
                phoneDraft = null
            }
        )
    }
}

@Composable
private fun PhoneContactConfirmDialog(
    draft: PhoneContactDraft,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, social: String) -> Unit
) {
    val s = LocalAppStrings.current
    var name by remember(draft) { mutableStateOf(draft.name) }
    var phone by remember(draft) { mutableStateOf(draft.phone) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogSheetFill(),
        tonalElevation = DialogSheetTonalElevation,
        shape = RoundedCornerShape(10.dp),
        title = { Text(s.cardFromPhone) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(s.name) },
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), phone.trim(), draft.social.trim()) },
                enabled = name.isNotBlank()
            ) { Text(s.add) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.cancel) }
        }
    )
}

@Composable
private fun TimerSetDialog(
    initialSeconds: Int,
    onDismiss: () -> Unit,
    onApply: (Int) -> Unit
) {
    val s = LocalAppStrings.current
    var minutes by remember { mutableStateOf((initialSeconds / 60).toString()) }
    var seconds by remember { mutableStateOf((initialSeconds % 60).toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogSheetFill(),
        tonalElevation = DialogSheetTonalElevation,
        shape = RoundedCornerShape(10.dp),
        title = { Text(s.cardTypeWorkout) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { minutes = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text(s.cardMinutes) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = seconds,
                    onValueChange = { seconds = it.filter { ch -> ch.isDigit() }.take(2) },
                    label = { Text(s.cardSeconds) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val min = minutes.toIntOrNull()?.coerceAtLeast(0) ?: 0
                val sec = (seconds.toIntOrNull() ?: 0).coerceIn(0, 59)
                onApply(min * 60 + sec)
            }) { Text(s.cardReady) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.cancel) }
        }
    )
}

@Composable
private fun WorkoutTimerRow(
    seconds: Int,
    running: Boolean,
    onDigits: () -> Unit,
    onToggle: () -> Unit
) {
    val s = LocalAppStrings.current
    val shown = seconds.coerceAtLeast(0)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
    ) {
        Text(
            text = "%02d:%02d".format(shown / 60, shown % 60),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.clickable(onClick = onDigits)
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(buttonFill())
                .border(1.5.dp, buttonLine(), CircleShape)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (running) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (running) s.cardReady else s.cardMinutes,
                tint = buttonLabel(),
                modifier = Modifier.size(22.dp)
            )
        }
    }
    CardRule()
}

@Composable
private fun WorkoutSetupRow(
    line: CardLine,
    onChange: (CardLine) -> Unit,
    onRemove: () -> Unit
) {
    val s = LocalAppStrings.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = line.text,
                onValueChange = { onChange(line.copy(text = it)) },
                label = { Text(s.cardExercise) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.Delete,
                contentDescription = s.delete,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(22.dp)
                    .clickable(onClick = onRemove),
                tint = deleteButtonColor()
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = line.sets.size.toString(),
                onValueChange = { raw ->
                    val n = raw.filter { it.isDigit() }.take(2).toIntOrNull() ?: 0
                    onChange(line.copy(sets = List(n.coerceIn(0, MaxWorkoutSets)) { i -> line.sets.getOrNull(i) == true }))
                },
                label = { Text(s.cardSets) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(96.dp)
            )
            OutlinedTextField(
                value = line.extra,
                onValueChange = { onChange(line.copy(extra = it)) },
                label = { Text(s.cardRepsWeight) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NamedExtraRow(
    line: CardLine,
    nameLabel: String,
    extraLabel: String?,
    onChange: (CardLine) -> Unit,
    onRemove: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = line.text,
            onValueChange = { onChange(line.copy(text = it)) },
            label = { Text(nameLabel) },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        if (extraLabel != null) {
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = line.extra,
                onValueChange = { onChange(line.copy(extra = it)) },
                label = { Text(extraLabel) },
                singleLine = true,
                modifier = Modifier.width(96.dp)
            )
        }
        Icon(
            Icons.Default.Delete,
            contentDescription = null,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(22.dp)
                .clickable(onClick = onRemove),
            tint = deleteButtonColor()
        )
    }
}

@Composable
private fun ExpandableLineText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textDecoration: TextDecoration? = null
) {
    var expanded by remember(text) { mutableStateOf(false) }
    var canExpand by remember(text) { mutableStateOf(false) }
    Text(
        text = text,
        style = style,
        color = color,
        textDecoration = textDecoration,
        maxLines = if (expanded) Int.MAX_VALUE else 1,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { result ->
            if (!expanded && result.hasVisualOverflow != canExpand) {
                canExpand = result.hasVisualOverflow
            }
        },
        modifier = modifier.then(
            if (expanded || canExpand) {
                Modifier.clickable { expanded = !expanded }
            } else {
                Modifier
            }
        )
    )
}

@Composable
private fun CheckLine(
    checked: Boolean,
    title: String,
    extra: String,
    placeholder: String,
    editing: Boolean,
    onTitle: (String) -> Unit,
    onFinishEdit: (String) -> Unit,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onRemove: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
        ) {
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                RoseDot(checked = checked, onClick = onToggle)
            }
            if (editing) {
                DraftLineField(
                    value = title,
                    placeholder = placeholder,
                    onValueChange = onTitle,
                    onFinish = onFinishEdit,
                    modifier = Modifier.weight(1f)
                )
            } else {
                ExpandableLineText(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (checked) TextDecoration.LineThrough else null,
                    modifier = Modifier.weight(1f)
                )
                if (extra.isNotBlank()) {
                    Text(
                        text = extra,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
                LineEditButton(onClick = onEdit)
            }
            WineDeleteButton(onClick = onRemove)
        }
        CardRule()
    }
}

@Composable
private fun DraftLineField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onFinish: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    QuietField(
        value = value,
        placeholder = placeholder,
        onValueChange = onValueChange,
        singleLine = true,
        modifier = modifier
            .focusRequester(focus)
            .onFocusChanged { if (!it.isFocused) onFinish(value) }
    )
}

@Composable
private fun QuietField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false
) {
    val style = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onBackground
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = style,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = style,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                }
                inner()
            }
        }
    )
}

@Composable
private fun CardRule() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun LineEditButton(onClick: () -> Unit) {
    val s = LocalAppStrings.current
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Edit,
            contentDescription = s.edit,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WineDeleteButton(onClick: () -> Unit) {
    val s = LocalAppStrings.current
    Box(
        modifier = Modifier
            .padding(start = 4.dp)
            .size(40.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Delete,
            contentDescription = s.delete,
            modifier = Modifier.size(20.dp),
            tint = deleteButtonColor()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkoutSetMarks(
    sets: List<Boolean>,
    onToggle: (Int) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        sets.forEachIndexed { index, on ->
            val enabled = index == 0 || sets[index - 1]
            RoseDot(
                checked = on,
                enabled = enabled,
                size = 40.dp,
                label = "${index + 1}",
                onClick = { if (enabled) onToggle(index) }
            )
        }
    }
}

@Composable
private fun RoseDot(
    checked: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    size: androidx.compose.ui.unit.Dp = 22.dp,
    label: String? = null
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(if (checked) RoseCheck else Color.Transparent, CircleShape)
            .border(
                1.5.dp,
                when {
                    checked -> RoseCheck
                    enabled -> buttonLine()
                    else -> buttonLine().copy(alpha = 0.35f)
                },
                CircleShape
            )
            .clip(CircleShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    checked -> Color.White
                    enabled -> buttonLabel()
                    else -> buttonLabel().copy(alpha = 0.35f)
                }
            )
        } else if (checked) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}

@Composable
private fun EditLineDialog(
    nameLabel: String,
    initialName: String,
    extraLabel: String?,
    initialExtra: String,
    onDismiss: () -> Unit,
    onSave: (name: String, extra: String) -> Unit
) {
    val s = LocalAppStrings.current
    var name by remember { mutableStateOf(initialName) }
    var extra by remember { mutableStateOf(initialExtra) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogSheetFill(),
        tonalElevation = DialogSheetTonalElevation,
        shape = RoundedCornerShape(10.dp),
        title = { Text(nameLabel) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(nameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (extraLabel != null) {
                    OutlinedTextField(
                        value = extra,
                        onValueChange = { extra = it },
                        label = { Text(extraLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), extra.trim()) },
                enabled = name.isNotBlank()
            ) { Text(s.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.cancel) }
        }
    )
}

@Composable
private fun AddExerciseDialog(
    initialName: String = "",
    initialSets: Int = DefaultWorkoutSets,
    initialExtra: String = "",
    confirmLabel: String? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, setCount: Int, extra: String) -> Unit
) {
    val s = LocalAppStrings.current
    var name by remember { mutableStateOf(initialName) }
    var sets by remember { mutableStateOf(initialSets.toString()) }
    var extra by remember { mutableStateOf(initialExtra) }
    val setCount = sets.toIntOrNull()?.coerceIn(1, MaxWorkoutSets) ?: DefaultWorkoutSets
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogSheetFill(),
        tonalElevation = DialogSheetTonalElevation,
        shape = RoundedCornerShape(10.dp),
        title = { Text(s.cardExercise) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(s.cardExercise) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it.filter { ch -> ch.isDigit() }.take(2) },
                    label = { Text(s.cardSets) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = extra,
                    onValueChange = { extra = it },
                    label = { Text(s.cardRepsWeight) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), setCount, extra.trim()) },
                enabled = name.isNotBlank()
            ) { Text(confirmLabel ?: s.add) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.cancel) }
        }
    )
}

@Composable
private fun TaskCardIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 44.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(buttonFill())
            .border(1.5.dp, buttonLine(), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = buttonLabel(),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun TaskCardPlusButton(
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 44.dp
) {
    val s = LocalAppStrings.current
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(buttonFill())
            .border(1.5.dp, buttonLine(), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = s.add,
            tint = buttonLabel(),
            modifier = Modifier.size(22.dp)
        )
    }
}

private fun List<Contact>.matchPerson(name: String, phone: String): Contact? {
    val digits = phone.filter { it.isDigit() }
    val trimmed = name.trim()
    return firstOrNull { contact ->
        val known = contact.phone.filter { it.isDigit() }
        when {
            digits.isNotEmpty() && known == digits -> true
            trimmed.isNotEmpty() && contact.name.trim().equals(trimmed, ignoreCase = true) -> true
            else -> false
        }
    }
}

private fun List<Contact>.uniqueByPerson(): List<Contact> {
    val seenPhones = mutableSetOf<String>()
    val seenNames = mutableSetOf<String>()
    return filter { contact ->
        val digits = contact.phone.filter { it.isDigit() }
        val name = contact.name.trim().lowercase()
        val phoneNew = digits.isEmpty() || seenPhones.add(digits)
        val nameNew = name.isEmpty() || seenNames.add(name)
        phoneNew && nameNew
    }
}

@Composable
fun ContactWriteDialog(
    contact: Contact?,
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogSheetFill(),
        tonalElevation = DialogSheetTonalElevation,
        shape = RoundedCornerShape(10.dp),
        title = { Text(s.cardWrite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DialogChoicePill(
                    selected = false,
                    onClick = {
                        onDismiss()
                        openMessenger(context, contact, telegram = true, s.cardNoPhone, s.cardNoApp)
                    },
                    label = s.cardTelegram
                )
                DialogChoicePill(
                    selected = false,
                    onClick = {
                        onDismiss()
                        openMessenger(context, contact, telegram = false, s.cardNoPhone, s.cardNoApp)
                    },
                    label = s.cardWhatsApp
                )
                if (!contact?.social.isNullOrBlank()) {
                    DialogChoicePill(
                        selected = false,
                        onClick = {
                            onDismiss()
                            openSocial(context, contact, s.cardNoApp)
                        },
                        label = s.cardSocial
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(s.cancel) }
        }
    )
}

private fun dialContact(context: android.content.Context, contact: Contact?, emptyMessage: String) {
    val phone = contact?.phone?.trim().orEmpty()
    if (phone.isEmpty()) {
        Toast.makeText(context, emptyMessage, Toast.LENGTH_SHORT).show()
        return
    }
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
    } catch (_: Exception) {
        Toast.makeText(context, emptyMessage, Toast.LENGTH_SHORT).show()
    }
}

private fun openMessenger(
    context: android.content.Context,
    contact: Contact?,
    telegram: Boolean,
    emptyMessage: String,
    noAppMessage: String
) {
    val uri = if (telegram) telegramUri(contact) else whatsAppUri(contact)
    if (uri == null) {
        Toast.makeText(context, emptyMessage, Toast.LENGTH_SHORT).show()
        return
    }
    openExternal(context, uri, noAppMessage)
}

private fun openSocial(
    context: android.content.Context,
    contact: Contact?,
    noAppMessage: String
) {
    val uri = socialUri(contact?.social.orEmpty()) ?: return
    openExternal(context, uri, noAppMessage)
}

private fun openExternal(context: android.content.Context, uri: Uri, noAppMessage: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: Exception) {
        Toast.makeText(context, noAppMessage, Toast.LENGTH_SHORT).show()
    }
}

private fun telegramUri(contact: Contact?): Uri? {
    contact?.social.orEmpty().split(',', '\n').map { it.trim() }.forEach { part ->
        val link = Regex("""(?i)(?:https?://)?(?:t\.me|telegram\.me)/(\+?[A-Za-z0-9_]+)""").find(part)
        if (link != null) return Uri.parse("https://t.me/${link.groupValues[1]}")
        val labeled = Regex("""(?i)telegram\s*[: ]\s*@?([A-Za-z0-9_]{3,})""").find(part)
        if (labeled != null) return Uri.parse("https://t.me/${labeled.groupValues[1]}")
    }
    val digits = contact?.phone?.filter { it.isDigit() }.orEmpty()
    return if (digits.isEmpty()) null else Uri.parse("https://t.me/+$digits")
}

private fun whatsAppUri(contact: Contact?): Uri? {
    contact?.social.orEmpty().split(',', '\n').map { it.trim() }.forEach { part ->
        val link = Regex("""(?i)(?:https?://)?wa\.me/(\d+)""").find(part)
        if (link != null) return Uri.parse("https://wa.me/${link.groupValues[1]}")
    }
    val digits = contact?.phone?.filter { it.isDigit() }.orEmpty()
    return if (digits.isEmpty()) null else Uri.parse("https://wa.me/$digits")
}

private fun socialUri(social: String): Uri? {
    val part = social.split(',', '\n').map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: return null
    val withScheme = when {
        part.startsWith("http://", true) || part.startsWith("https://", true) -> part
        part.startsWith("@") && part.length > 1 -> "https://t.me/${part.removePrefix("@")}"
        part.contains('.') && !part.contains(' ') -> "https://$part"
        Regex("""^[A-Za-z0-9_]{3,}$""").matches(part) -> "https://t.me/$part"
        else -> return null
    }
    return Uri.parse(withScheme)
}
