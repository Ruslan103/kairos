package com.example.kaizenkanban.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.kaizenkanban.data.local.KairosPreferences
import com.example.kaizenkanban.reminders.DueReminderScheduler
import com.example.kaizenkanban.ui.i18n.LanguageToggle
import com.example.kaizenkanban.ui.i18n.LocalAppStrings
import com.example.kaizenkanban.ui.onboarding.GesturesGuideDialog
import com.example.kaizenkanban.ui.onboarding.OnboardingDialog
import com.example.kaizenkanban.ui.onboarding.PlanningGuideDialog
import com.example.kaizenkanban.ui.resolveQuickAddTarget
import com.example.kaizenkanban.ui.sanitizeQuickAddPrefs
import com.example.kaizenkanban.ui.theme.EisenhowerPaletteToggle
import com.example.kaizenkanban.ui.theme.ThemeToggle
import com.example.kaizenkanban.ui.viewmodel.SharedViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SharedViewModel,
    onBack: () -> Unit
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val prefs = remember { KairosPreferences(context) }
    val scope = rememberCoroutineScope()
    var enterAddsTask by remember { mutableStateOf(prefs.enterAddsTask) }
    var showArchived by remember { mutableStateOf(prefs.showArchivedBoards) }
    var showOnboarding by remember { mutableStateOf(false) }
    var showPlanningGuide by remember { mutableStateOf(false) }
    var showGesturesGuide by remember { mutableStateOf(false) }
    var notificationsGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsGranted = granted
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val json = viewModel.exportToJson()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.bufferedWriter().use { it.write(json) }
                }
                Toast.makeText(context, s.exportSuccess, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, s.saveError(e.localizedMessage), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val json = stream.bufferedReader().readText()
                    val count = viewModel.importFromJson(json)
                    Toast.makeText(context, s.importedFromFile(count), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, s.importError(e.localizedMessage), Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showOnboarding) {
        OnboardingDialog(
            onFinished = {
                showOnboarding = false
                prefs.hasSeenOnboarding = true
            }
        )
    }
    if (showPlanningGuide) {
        PlanningGuideDialog(
            onDismiss = { showPlanningGuide = false }
        )
    }
    if (showGesturesGuide) {
        GesturesGuideDialog(
            onDismiss = { showGesturesGuide = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        s.settings,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingsSection(title = s.statsEpochTitle) {
                var epochTick by remember { mutableStateOf(0) }
                val epoch = remember(epochTick) { prefs.statsEpochMillis }
                val fmt = remember {
                    java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault())
                }
                Text(
                    text = fmt.format(java.util.Date(epoch)),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = s.statsEpochHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        prefs.resetStatsEpochToDefault()
                        epochTick++
                    }) { Text(s.statsEpochResetYear) }
                    TextButton(onClick = {
                        prefs.advanceStatsEpochTo(System.currentTimeMillis())
                        epochTick++
                    }) { Text(s.statsResetPeriod) }
                }
            }

            HorizontalDivider()

            SettingsSection(title = s.appearance) {
                Text(
                    text = s.themeLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                ThemeToggle()
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${s.themeSystem} · ${s.themeLight} · ${s.themeDark}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = s.eisenhowerPaletteLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                EisenhowerPaletteToggle()
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = s.eisenhowerPaletteHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider()

            SettingsSection(title = s.languageLabel) {
                LanguageToggle()
            }

            HorizontalDivider()

            SettingsSection(title = s.editingPrefs) {
                SettingsSwitchRow(
                    title = s.enterAddsTaskTitle,
                    subtitle = if (enterAddsTask) s.enterSavesTask else s.enterAddsNewline,
                    checked = enterAddsTask,
                    onCheckedChange = {
                        enterAddsTask = it
                        prefs.enterAddsTask = it
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                SettingsSwitchRow(
                    title = s.showArchivedBoards,
                    subtitle = null,
                    checked = showArchived,
                    onCheckedChange = {
                        showArchived = it
                        prefs.showArchivedBoards = it
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                QuickAddDestinationSettings(viewModel = viewModel, prefs = prefs)
            }

            HorizontalDivider()

            SettingsSection(title = s.dataAndBackup) {
                OutlinedButton(
                    onClick = { exportLauncher.launch("kairos_backup.json") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(s.exportFile)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        importLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(s.importFile)
                }
            }

            HorizontalDivider()

            SettingsSection(title = s.openSystemNotifications) {
                Text(
                    text = if (notificationsGranted) {
                        s.reminderChannelDesc
                    } else {
                        s.notificationsPermissionHint
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                var reminderHour by remember { mutableStateOf(prefs.reminderHour) }
                var reminderMinute by remember { mutableStateOf(prefs.reminderMinute) }
                OutlinedButton(
                    onClick = {
                        android.app.TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                reminderHour = hour
                                reminderMinute = minute
                                prefs.reminderHour = hour
                                prefs.reminderMinute = minute
                                DueReminderScheduler.sync(context, viewModel.state.value.tasks)
                            },
                            reminderHour,
                            reminderMinute,
                            true
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "${s.reminderDefaultTime}: ${DueReminderScheduler.formatClock(reminderHour, reminderMinute)}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = s.reminderDefaultTimeHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (!notificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    OutlinedButton(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(s.enableNotifications)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(s.openSystemNotifications)
                }
            }

            HorizontalDivider()

            SettingsSection(title = s.proSection) {
                var proUnlocked by remember { mutableStateOf(prefs.proUnlocked) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(s.proUnlockedStub, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            s.proUnlockedStubHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            s.voiceHintExamples,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = proUnlocked,
                        onCheckedChange = {
                            proUnlocked = it
                            prefs.proUnlocked = it
                        }
                    )
                }
            }

            HorizontalDivider()

            SettingsSection(title = s.helpSection) {
                TextButton(
                    onClick = { showPlanningGuide = true }
                ) {
                    Text(s.planningGuideOpen)
                }
                TextButton(
                    onClick = { showGesturesGuide = true }
                ) {
                    Text(s.gesturesGuideOpen)
                }
                TextButton(
                    onClick = {
                        prefs.hasSeenOnboarding = false
                        showOnboarding = true
                    }
                ) {
                    Text(s.showOnboardingAgain)
                }
            }

            HorizontalDivider()

            SettingsSection(title = s.aboutSection) {
                val versionName = remember(context) {
                    runCatching {
                        if (Build.VERSION.SDK_INT >= 33) {
                            context.packageManager.getPackageInfo(
                                context.packageName,
                                PackageManager.PackageInfoFlags.of(0)
                            ).versionName
                        } else {
                            @Suppress("DEPRECATION")
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName
                        }
                    }.getOrNull().orEmpty().ifBlank { "—" }
                }
                Text(
                    text = s.appVersionLabel(versionName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = s.licenseNotice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickAddDestinationSettings(
    viewModel: SharedViewModel,
    prefs: KairosPreferences
) {
    val s = LocalAppStrings.current
    val state by viewModel.state.collectAsState()
    var boardId by remember { mutableStateOf(prefs.quickAddBoardId) }
    var columnId by remember { mutableStateOf(prefs.quickAddColumnId) }
    var showBoardPicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.boards, state.columns) {
        sanitizeQuickAddPrefs(prefs, state.boards, state.columns)
        boardId = prefs.quickAddBoardId
        columnId = prefs.quickAddColumnId
    }

    val target = remember(state.boards, state.columns, boardId, columnId) {
        resolveQuickAddTarget(prefs, state.boards, state.columns)
    }

    Text(
        text = s.quickAddDestination,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
    Spacer(modifier = Modifier.height(6.dp))
    if (target != null) {
        Text(
            text = s.quickAddGoesTo(s.localized(target.board.name), s.localized(target.column.title)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    OutlinedButton(
        onClick = { showBoardPicker = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (boardId == null) {
                s.quickAddUsePrimary
            } else {
                state.boards.find { it.id == boardId }?.let { s.localized(it.name) } ?: s.quickAddPickBoard
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    val hubsForSelection = remember(state.boards, state.columns, boardId) {
        val activeBoards = state.boards.filter { !it.isArchived }
        val board = boardId?.let { id -> activeBoards.find { it.id == id } }
            ?: activeBoards.find { it.isDefault }
            ?: activeBoards.firstOrNull()
        board?.let { b ->
            state.columns.filter { it.boardId == b.id }.sortedBy { it.position }
        }.orEmpty()
    }

    if (hubsForSelection.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = s.quickAddPickHub,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            hubsForSelection.forEach { hub ->
                val selected = when {
                    columnId != null -> columnId == hub.id
                    else -> hub.id == hubsForSelection.first().id
                }
                FilterChip(
                    selected = selected,
                    onClick = {
                        columnId = hub.id
                        prefs.quickAddColumnId = hub.id
                    },
                    modifier = Modifier.widthIn(max = 168.dp),
                    label = {
                        Text(
                            text = s.localized(hub.title),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }
    }

    if (showBoardPicker) {
        val activeBoards = state.boards.filter { !it.isArchived }
        AlertDialog(
            onDismissRequest = { showBoardPicker = false },
            title = {
                Text(
                    text = s.quickAddPickBoard,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    fun selectPrimary() {
                        boardId = null
                        columnId = null
                        prefs.quickAddBoardId = null
                        prefs.quickAddColumnId = null
                        showBoardPicker = false
                    }
                    fun selectBoard(boardIdValue: String) {
                        boardId = boardIdValue
                        prefs.quickAddBoardId = boardIdValue
                        val firstHub = state.columns
                            .filter { it.boardId == boardIdValue }
                            .minByOrNull { it.position }
                        columnId = firstHub?.id
                        prefs.quickAddColumnId = firstHub?.id
                        showBoardPicker = false
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectPrimary() }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = boardId == null,
                            onClick = { selectPrimary() }
                        )
                        Text(
                            text = s.quickAddUsePrimary,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 4.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    activeBoards.forEach { board ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectBoard(board.id) }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = boardId == board.id,
                                onClick = { selectBoard(board.id) }
                            )
                            Text(
                                text = buildString {
                                    append(s.localized(board.name))
                                    if (board.isDefault) append(" ★")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 4.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBoardPicker = false }) { Text(s.cancel) }
            }
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        content()
    }
}
