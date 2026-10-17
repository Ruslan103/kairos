package com.example.kaizenkanban.ui.notes

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.kaizenkanban.data.local.KairosPreferences
import com.example.kaizenkanban.domain.model.NoteLine
import com.example.kaizenkanban.domain.model.NotePhoto
import com.example.kaizenkanban.domain.model.ProjectNote
import com.example.kaizenkanban.domain.model.displayTitle
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.reminders.DueReminderScheduler
import com.example.kaizenkanban.ui.board.TaskLinkPickerDialog
import com.example.kaizenkanban.ui.calendar.localMillisToUtcPicker
import com.example.kaizenkanban.ui.calendar.startOfLocalDayMillis
import com.example.kaizenkanban.ui.calendar.utcPickerMillisToLocalNoon
import com.example.kaizenkanban.ui.i18n.LocalAppLanguage
import com.example.kaizenkanban.ui.i18n.LocalAppStrings
import com.example.kaizenkanban.ui.theme.buttonFill
import com.example.kaizenkanban.ui.theme.buttonLabel
import com.example.kaizenkanban.ui.theme.buttonLine
import com.example.kaizenkanban.ui.viewmodel.SharedViewModel
import com.example.kaizenkanban.voice.SpeechRecognizerClient
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotesScreen(
    projectId: String,
    viewModel: SharedViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val dateLocale = LocalAppLanguage.current.locale
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    var editorNote by remember { mutableStateOf<ProjectNote?>(null) }
    var showNewEditor by remember { mutableStateOf(false) }
    var autoStartVoice by remember { mutableStateOf(false) }
    var notePendingDelete by remember { mutableStateOf<ProjectNote?>(null) }
    var previewPhotoUri by remember { mutableStateOf<String?>(null) }

    val pendingAddNote by viewModel.pendingAddNote.collectAsState()
    val pendingVoiceNote by viewModel.pendingVoiceNote.collectAsState()

    LaunchedEffect(pendingAddNote, projectId) {
        if (!pendingAddNote) return@LaunchedEffect
        val voice = viewModel.consumePendingVoiceNote()
        viewModel.consumePendingAddNote()
        editorNote = null
        autoStartVoice = voice
        showNewEditor = true
    }

    val project = state.projects.find { it.id == projectId }
    val notes = remember(state.projectNotes, projectId, searchQuery) {
        val q = searchQuery.trim()
        state.projectNotes
            .filter { it.projectId == projectId }
            .filter {
                q.isEmpty() ||
                    it.title.contains(q, ignoreCase = true) ||
                    it.body.contains(q, ignoreCase = true)
            }
            .sortedWith(
                compareByDescending<ProjectNote> { it.pinned }
                    .thenBy { it.position }
                    .thenByDescending { it.createdAt }
            )
    }
    val pageColor = MaterialTheme.colorScheme.background
    Scaffold(
        containerColor = pageColor,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = pageColor),
                title = {
                    if (project != null) {
                        Text(
                            s.localized(project.name),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        searchOpen = !searchOpen
                        if (!searchOpen) searchQuery = ""
                    }) {
                        Icon(
                            if (searchOpen) Icons.Outlined.Close else Icons.Outlined.Search,
                            contentDescription = s.noteSearchHint
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(buttonFill())
                    .border(1.5.dp, buttonLine(), CircleShape)
                    .combinedClickable(
                        onClick = {
                            editorNote = null
                            autoStartVoice = false
                            showNewEditor = true
                        },
                        onLongClick = {
                            editorNote = null
                            autoStartVoice = true
                            showNewEditor = true
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = s.newNote)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (searchOpen) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    placeholder = { Text(s.noteSearchHint) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp)
                )
            }
            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            s.notesEmpty,
                            style = diaryTitleStyle()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            s.notesEmptyHint,
                            style = diaryBodyStyle().copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            dateLocale = dateLocale,
                            onOpen = {
                                editorNote = note
                                autoStartVoice = false
                                showNewEditor = true
                            },
                            onPin = { viewModel.setNotePinned(note.id, !note.pinned) },
                            onMoveUp = { viewModel.moveProjectNote(note.id, -1) },
                            onMoveDown = { viewModel.moveProjectNote(note.id, 1) },
                            onDelete = { notePendingDelete = note },
                            onConvert = {
                                scope.launch {
                                    val taskId = viewModel.convertNoteToTask(note.id)
                                    if (taskId != null) {
                                        Toast.makeText(context, s.noteConverted, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showNewEditor) {
        NoteEditorDialog(
            projectId = projectId,
            existing = editorNote,
            tasks = state.tasks.filter { task ->
                val boardId = state.columns.find { it.id == task.columnId }?.boardId
                state.boards.any { it.id == boardId && it.projectId == projectId }
            },
            columns = state.columns,
            viewModel = viewModel,
            autoStartVoice = autoStartVoice,
            onOpenPhoto = { uri -> previewPhotoUri = uri },
            onDismiss = {
                showNewEditor = false
                editorNote = null
                autoStartVoice = false
            }
        )
    }

    previewPhotoUri?.let { uri ->
        Dialog(
            onDismissRequest = { previewPhotoUri = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.85f))
                    .clickable { previewPhotoUri = null },
                contentAlignment = Alignment.Center
            ) {
                GalleryUriThumbnail(
                    mediaUri = uri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }

    notePendingDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { notePendingDelete = null },
            title = { Text(s.noteDeleteConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteProjectNote(note.id)
                    notePendingDelete = null
                }) { Text(s.delete) }
            },
            dismissButton = {
                TextButton(onClick = { notePendingDelete = null }) { Text(s.cancel) }
            }
        )
    }
}

@Composable
private fun NoteCard(
    note: ProjectNote,
    dateLocale: Locale,
    onOpen: () -> Unit,
    onPin: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onConvert: () -> Unit
) {
    val s = LocalAppStrings.current
    var menuExpanded by remember { mutableStateOf(false) }
    val stamp = remember(note.createdAt, dateLocale) {
        SimpleDateFormat("d MMM yyyy, HH:mm", dateLocale).format(Date(note.createdAt))
    }
    val headline = note.displayTitle()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 20.dp, top = 6.dp, end = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = headline.ifBlank { stamp },
                        style = diaryListTitleStyle(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (headline.isNotBlank()) {
                        Text(
                            stamp,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                if (note.pinned) {
                    Icon(
                        Icons.Outlined.Bookmark,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = s.moreActions)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(s.edit) },
                            onClick = { menuExpanded = false; onOpen() },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (note.pinned) s.noteUnpin else s.notePin) },
                            onClick = { menuExpanded = false; onPin() },
                            leadingIcon = { Icon(Icons.Default.PushPin, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(s.noteMoveUp) },
                            onClick = { menuExpanded = false; onMoveUp() },
                            leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(s.noteMoveDown) },
                            onClick = { menuExpanded = false; onMoveDown() },
                            leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(s.noteConvertToTask) },
                            onClick = { menuExpanded = false; onConvert() },
                            leadingIcon = { Icon(Icons.Default.TaskAlt, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(s.delete) },
                            onClick = { menuExpanded = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, null) }
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditorDialog(
    projectId: String,
    existing: ProjectNote?,
    tasks: List<Task>,
    columns: List<com.example.kaizenkanban.domain.model.Column>,
    viewModel: SharedViewModel,
    autoStartVoice: Boolean = false,
    onOpenPhoto: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val dateLocale = LocalAppLanguage.current.locale
    val keyboard = LocalSoftwareKeyboardController.current
    var title by remember(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    val linesState = remember(existing?.id) {
        mutableStateOf(draftLinesFor(existing, viewModel.state.value.noteLines))
    }
    var lines by linesState
    val focusedLineId = remember(existing?.id) { mutableStateOf(lines.lastOrNull()?.id) }
    val prefs = remember { KairosPreferences(context) }
    val enterSaves = prefs.enterAddsTask
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    fun requesterFor(id: String) = focusRequesters.getOrPut(id) { FocusRequester() }
    var attachLineId by remember { mutableStateOf<String?>(null) }
    var pinned by remember(existing?.id) { mutableStateOf(existing?.pinned == true) }
    var linkedTaskId by remember(existing?.id) { mutableStateOf(existing?.linkedTaskId) }
    var reminderAt by remember(existing?.id) { mutableStateOf(existing?.reminderAtMillis) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showLinkPicker by remember { mutableStateOf(false) }
    var listening by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var workingNoteId by remember(existing?.id) { mutableStateOf(existing?.id) }
    val editorState by viewModel.state.collectAsState()
    val photos = remember(editorState.notePhotos, workingNoteId) {
        val id = workingNoteId ?: return@remember emptyList()
        editorState.notePhotos.filter { it.noteId == id }
    }
    val isNewNote = existing == null

    val speechClient = remember {
        SpeechRecognizerClient(
            context = context,
            onResult = { text ->
                linesState.value = applySpokenLine(
                    lines = linesState.value,
                    focusedId = focusedLineId.value,
                    spoken = text,
                    joinCurrent = prefs.enterAddsTask
                )
            },
            onError = {
                Toast.makeText(context, s.voiceError, Toast.LENGTH_SHORT).show()
            },
            onListeningChanged = { listening = it }
        )
    }
    DisposableEffect(Unit) {
        onDispose { speechClient.destroy() }
    }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val noteId = workingNoteId
        val lineId = attachLineId
        if (uri != null && noteId != null && lineId != null) {
            viewModel.addNotePhoto(noteId, lineId, uri) { err ->
                when (err) {
                    "limit" -> Toast.makeText(context, s.notePhotosLimit, Toast.LENGTH_SHORT).show()
                    "error" -> Toast.makeText(context, s.voiceError, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        val noteId = workingNoteId
        val lineId = attachLineId
        if (ok && uri != null && noteId != null && lineId != null) {
            viewModel.addNotePhoto(noteId, lineId, uri) { err ->
                when (err) {
                    "limit" -> Toast.makeText(context, s.notePhotosLimit, Toast.LENGTH_SHORT).show()
                    "error" -> Toast.makeText(context, s.voiceError, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun startCameraCapture() {
        try {
            val file = File(context.cacheDir, "note_photo_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            pendingCameraUri = uri
            takePicture.launch(uri)
        } catch (_: Exception) {
            Toast.makeText(context, s.voiceError, Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCameraCapture()
        else Toast.makeText(context, s.cameraPermissionNeeded, Toast.LENGTH_SHORT).show()
    }
    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) speechClient.start(dateLocale) else {
            Toast.makeText(context, s.voiceError, Toast.LENGTH_SHORT).show()
        }
    }

    fun startVoiceCapture() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) speechClient.start(dateLocale) else {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(Unit) {
        delay(160)
        val id = linesState.value.lastOrNull()?.id ?: return@LaunchedEffect
        runCatching {
            focusRequesters[id]?.requestFocus()
            keyboard?.show()
        }
    }

    LaunchedEffect(autoStartVoice) {
        if (!autoStartVoice) return@LaunchedEffect
        delay(280)
        startVoiceCapture()
    }

    fun keptLines(forceLineId: String? = null): List<NoteLine> {
        val now = System.currentTimeMillis()
        return lines.mapIndexedNotNull { index, line ->
            val hasPhoto = photos.any { it.lineId == line.id }
            if (line.text.isBlank() && !hasPhoto && line.id != forceLineId) return@mapIndexedNotNull null
            NoteLine(
                id = line.id,
                noteId = workingNoteId ?: existing?.id.orEmpty(),
                text = line.text,
                createdAt = line.createdAt ?: now,
                position = index
            )
        }.mapIndexed { index, line -> line.copy(position = index) }
    }

    fun ensureSavedThen(forceLineId: String, block: (String) -> Unit) {
        val now = System.currentTimeMillis()
        lines = lines.map { line ->
            if (line.id == forceLineId && line.createdAt == null) line.copy(createdAt = now) else line
        }
        val kept = keptLines(forceLineId)
        if (kept.isEmpty() && title.isBlank()) {
            Toast.makeText(context, s.noteBodyHint, Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.upsertProjectNote(
            projectId = projectId,
            body = kept.joinToString("\n") { it.text },
            title = title,
            existingId = workingNoteId ?: existing?.id,
            pinned = pinned,
            linkedTaskId = linkedTaskId,
            reminderAtMillis = reminderAt,
            lines = kept
        ) { newId ->
            workingNoteId = newId
            block(newId)
        }
    }

    fun saveNote() {
        val kept = keptLines()
        if (kept.isEmpty() && title.isBlank()) {
            Toast.makeText(context, s.noteBodyHint, Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.upsertProjectNote(
            projectId = projectId,
            body = kept.joinToString("\n") { it.text },
            title = title,
            existingId = workingNoteId ?: existing?.id,
            pinned = pinned,
            linkedTaskId = linkedTaskId,
            reminderAtMillis = reminderAt,
            lines = kept
        )
        onDismiss()
    }

    fun onLineText(id: String, text: String) {
        val now = System.currentTimeMillis()
        var next = lines.map { line ->
            if (line.id != id) line
            else line.copy(
                text = text,
                createdAt = line.createdAt ?: text.takeIf { it.isNotEmpty() }?.let { now }
            )
        }
        if (next.lastOrNull()?.text?.isNotEmpty() == true) {
            next = next + blankDraftLine(next.size)
        }
        lines = next
    }

    fun attachFromGallery(lineId: String) {
        attachLineId = lineId
        ensureSavedThen(lineId) {
            pickImage.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }
    }

    fun attachFromCamera(lineId: String) {
        attachLineId = lineId
        ensureSavedThen(lineId) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) startCameraCapture()
            else cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    val pageColor = MaterialTheme.colorScheme.background
    val ruleColor = MaterialTheme.colorScheme.outlineVariant
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = pageColor) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = s.cancel)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    DiaryToolButton(
                        icon = Icons.Outlined.Check,
                        description = s.save,
                        active = true,
                        onClick = { saveNote() }
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val titleStyle = diaryTitleStyle()
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 2.dp),
                        textStyle = titleStyle,
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                        decorationBox = { inner ->
                            Box {
                                if (title.isEmpty()) {
                                    Text(
                                        s.noteTitleHint,
                                        style = titleStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                    )
                                }
                                inner()
                            }
                        }
                    )
                    lines.forEach { line ->
                        DiaryLineEditor(
                            line = line,
                            photos = photos.filter { it.lineId == line.id },
                            dateLocale = dateLocale,
                            ruleColor = ruleColor,
                            showHint = line.id == lines.lastOrNull()?.id,
                            hint = s.noteBodyHint,
                            saveOnEnter = isNewNote && enterSaves,
                            focusRequester = requesterFor(line.id),
                            onText = { onLineText(line.id, it) },
                            onFocus = { focusedLineId.value = line.id },
                            onNext = {
                                val idx = lines.indexOfFirst { it.id == line.id }
                                val next = lines.getOrNull(idx + 1)
                                if (next != null) {
                                    runCatching { focusRequesters[next.id]?.requestFocus() }
                                }
                            },
                            onDone = { if (isNewNote && enterSaves) saveNote() },
                            onOpenPhoto = onOpenPhoto,
                            onDeletePhoto = { viewModel.deleteNotePhoto(it) }
                        )
                    }
                }
                val activeLineId = focusedLineId.value ?: lines.lastOrNull()?.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DiaryToolButton(
                        icon = Icons.Outlined.Mic,
                        description = s.noteVoice,
                        active = listening,
                        onClick = { startVoiceCapture() }
                    )
                    DiaryToolButton(
                        icon = Icons.Outlined.Image,
                        description = s.notePhotos,
                        onClick = { activeLineId?.let { attachFromGallery(it) } }
                    )
                    DiaryToolButton(
                        icon = Icons.Outlined.PhotoCamera,
                        description = s.takePhoto,
                        onClick = { activeLineId?.let { attachFromCamera(it) } }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    DiaryToolButton(
                        icon = if (pinned) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        description = if (pinned) s.noteUnpin else s.notePin,
                        active = pinned,
                        onClick = { pinned = !pinned }
                    )
                    DiaryToolButton(
                        icon = Icons.Outlined.Event,
                        description = if (reminderAt != null) s.noteReminderClear else s.noteReminder,
                        active = reminderAt != null,
                        onClick = {
                            if (reminderAt != null) reminderAt = null else showDatePicker = true
                        }
                    )
                    DiaryToolButton(
                        icon = Icons.Outlined.Link,
                        description = if (linkedTaskId != null) s.noteUnlinkTask else s.noteLinkTask,
                        active = linkedTaskId != null,
                        onClick = {
                            if (linkedTaskId != null) linkedTaskId = null else showLinkPicker = true
                        }
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = localMillisToUtcPicker(
                reminderAt ?: startOfLocalDayMillis()
            )
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val day = pickerState.selectedDateMillis?.let { utcPickerMillisToLocalNoon(it) }
                    showDatePicker = false
                    if (day == null) return@TextButton
                    val defaultMinutes = 9 * 60
                    android.app.TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            reminderAt = java.util.Calendar.getInstance().apply {
                                timeInMillis = day
                                set(java.util.Calendar.HOUR_OF_DAY, hour)
                                set(java.util.Calendar.MINUTE, minute)
                                set(java.util.Calendar.SECOND, 0)
                                set(java.util.Calendar.MILLISECOND, 0)
                            }.timeInMillis
                        },
                        defaultMinutes / 60,
                        defaultMinutes % 60,
                        true
                    ).show()
                }) { Text(s.ok) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(s.cancel) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showLinkPicker) {
        TaskLinkPickerDialog(
            title = s.noteLinkTask,
            candidates = tasks,
            columns = columns,
            emptyMessage = s.noTasksToLink,
            onPick = { task ->
                linkedTaskId = task.id
                showLinkPicker = false
            },
            onDismiss = { showLinkPicker = false }
        )
    }
}

@Composable
private fun diaryTitleStyle(): TextStyle = TextStyle(
    fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
    fontSize = 24.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 28.sp,
    color = MaterialTheme.colorScheme.onBackground
)

@Composable
private fun diaryListTitleStyle(): TextStyle = TextStyle(
    fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
    fontSize = 20.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 24.sp,
    color = MaterialTheme.colorScheme.onBackground
)

@Composable
private fun diaryBodyStyle(): TextStyle = TextStyle(
    fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
    fontSize = 17.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 22.sp,
    color = MaterialTheme.colorScheme.onBackground
)

@Composable
private fun DiaryToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(
                if (active) MaterialTheme.colorScheme.onBackground else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = description,
            modifier = Modifier.size(20.dp),
            tint = if (active) MaterialTheme.colorScheme.background
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
        )
    }
}

@Composable
private fun DiaryLineEditor(
    line: DraftLine,
    photos: List<NotePhoto>,
    dateLocale: Locale,
    ruleColor: Color,
    showHint: Boolean,
    hint: String,
    saveOnEnter: Boolean,
    focusRequester: FocusRequester,
    onText: (String) -> Unit,
    onFocus: () -> Unit,
    onNext: () -> Unit,
    onDone: () -> Unit,
    onOpenPhoto: (String) -> Unit,
    onDeletePhoto: (String) -> Unit
) {
    val textStyle = diaryBodyStyle()
    Column(modifier = Modifier.fillMaxWidth()) {
        if (line.createdAt != null) {
            Text(
                text = lineAddedLabel(line.createdAt, dateLocale),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        BasicTextField(
            value = line.text,
            onValueChange = onText,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { if (it.isFocused) onFocus() }
                .padding(top = 1.dp, bottom = 2.dp)
                .drawBehind {
                    val y = size.height - 1f
                    drawLine(
                        color = ruleColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                },
            textStyle = textStyle,
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
            keyboardOptions = KeyboardOptions(
                imeAction = if (saveOnEnter) ImeAction.Done else ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { onNext() },
                onDone = { onDone() }
            ),
            decorationBox = { inner ->
                Box(modifier = Modifier.padding(bottom = 3.dp)) {
                    if (showHint && line.text.isEmpty()) {
                        Text(
                            hint,
                            style = textStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    inner()
                }
            }
        )
        if (photos.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                photos.forEach { photo ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(4.dp))
                    ) {
                        GalleryUriThumbnail(
                            mediaUri = photo.mediaUri,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onOpenPhoto(photo.mediaUri) }
                        )
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .size(16.dp)
                                .clickable { onDeletePhoto(photo.id) },
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

private data class DraftLine(
    val id: String,
    val text: String,
    val createdAt: Long?,
    val position: Int
)

private fun blankDraftLine(position: Int) = DraftLine(
    id = java.util.UUID.randomUUID().toString(),
    text = "",
    createdAt = null,
    position = position
)

private fun draftLinesFor(existing: ProjectNote?, stored: List<NoteLine>): List<DraftLine> {
    val fromDb = if (existing == null) {
        emptyList()
    } else {
        stored.filter { it.noteId == existing.id }.sortedBy { it.position }
    }
    val base = when {
        fromDb.isNotEmpty() -> fromDb.map { DraftLine(it.id, it.text, it.createdAt, it.position) }
        existing != null && existing.body.isNotBlank() ->
            com.example.kaizenkanban.domain.model.linesFromNoteBody(
                existing.id,
                existing.body,
                existing.createdAt
            ).map { DraftLine(it.id, it.text, it.createdAt, it.position) }
        else -> emptyList()
    }
    return base + blankDraftLine(base.size)
}

private fun applySpokenLine(
    lines: List<DraftLine>,
    focusedId: String?,
    spoken: String,
    joinCurrent: Boolean
): List<DraftLine> {
    val text = spoken.trim()
    if (text.isEmpty()) return lines
    val now = System.currentTimeMillis()
    val target = lines.find { it.id == focusedId } ?: lines.lastOrNull()
        ?: return listOf(DraftLine(java.util.UUID.randomUUID().toString(), text, now, 0), blankDraftLine(1))
    if (target.text.isBlank()) {
        val filled = lines.map { line ->
            if (line.id == target.id) line.copy(text = text, createdAt = line.createdAt ?: now) else line
        }
        return if (filled.lastOrNull()?.text?.isEmpty() == true) filled else filled + blankDraftLine(filled.size)
    }
    if (joinCurrent) {
        return lines.map { line ->
            if (line.id == target.id) line.copy(text = "${line.text.trim()} $text") else line
        }
    }
    val insertAt = lines.indexOfFirst { it.id == target.id }.let { if (it < 0) lines.size else it + 1 }
    val added = DraftLine(java.util.UUID.randomUUID().toString(), text, now, insertAt)
    val next = lines.toMutableList()
    next.add(insertAt.coerceAtMost(next.size), added)
    val placed = next.mapIndexed { index, line -> line.copy(position = index) }
    return if (placed.lastOrNull()?.text?.isEmpty() == true) placed else placed + blankDraftLine(placed.size)
}

private fun lineAddedLabel(millis: Long, locale: Locale): String =
    SimpleDateFormat("d MMM · HH:mm", locale).format(Date(millis))

@Composable
private fun GalleryUriThumbnail(
    mediaUri: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val bitmap = remember(mediaUri) {
        runCatching {
            context.contentResolver.openInputStream(Uri.parse(mediaUri))?.use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
            }
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
