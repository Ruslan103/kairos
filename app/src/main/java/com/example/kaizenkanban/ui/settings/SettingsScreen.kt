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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.kaizenkanban.data.local.KairosPreferences
import com.example.kaizenkanban.reminders.DueReminderScheduler
import com.example.kaizenkanban.reminders.RecurringEnsureScheduler
import com.example.kaizenkanban.ui.calendar.localMillisToUtcPicker
import com.example.kaizenkanban.ui.calendar.startOfLocalDayMillis
import com.example.kaizenkanban.ui.calendar.utcPickerMillisToLocalNoon
import com.example.kaizenkanban.ui.i18n.LanguageToggle
import com.example.kaizenkanban.ui.i18n.LocalAppStrings
import com.example.kaizenkanban.ui.onboarding.GesturesGuideDialog
import com.example.kaizenkanban.ui.onboarding.OnboardingDialog
import com.example.kaizenkanban.ui.onboarding.PlanningGuideDialog
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
                var showEpochPicker by remember { mutableStateOf(false) }
                val epoch = remember(epochTick) { prefs.statsEpochMillis }
                val fmt = remember {
                    java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault())
                }
                Text(
                    text = fmt.format(java.util.Date(epoch)),
                    modifier = Modifier.clickable { showEpochPicker = true },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = {
                    prefs.resetStatsEpochToDefault()
                    epochTick++
                }) { Text(s.statsEpochResetYear) }
                if (showEpochPicker) {
                    val epochPickerState = rememberDatePickerState(
                        initialSelectedDateMillis = localMillisToUtcPicker(epoch)
                    )
                    DatePickerDialog(
                        onDismissRequest = { showEpochPicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                val selected = epochPickerState.selectedDateMillis
                                if (selected != null) {
                                    val noon = utcPickerMillisToLocalNoon(selected)
                                    prefs.statsEpochMillis = startOfLocalDayMillis(noon)
                                    epochTick++
                                }
                                showEpochPicker = false
                            }) { Text(s.ok) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEpochPicker = false }) { Text(s.cancel) }
                        }
                    ) {
                        DatePicker(state = epochPickerState)
                    }
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
                    text = "${s.themeSystem} · ${s.themeLight} · ${s.themeDark} · ${s.themeSlate}",
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
            }

            HorizontalDivider()

            SettingsSection(title = s.dataAndBackup) {
                OutlinedButton(
                    onClick = { exportLauncher.launch("kairos_backup.json") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(s.exportFile)
                }
                Text(
                    text = s.exportPhotosWarning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
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

            SettingsSection(title = s.dataBrowser) {
                DatabaseBrowserSection(viewModel = viewModel)
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
                                RecurringEnsureScheduler.scheduleNext(context)
                                viewModel.ensureTodayRecurringInstances()
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
                Spacer(modifier = Modifier.height(12.dp))
                var spawnHour by remember { mutableStateOf(prefs.spawnHour) }
                var spawnMinute by remember { mutableStateOf(prefs.spawnMinute) }
                OutlinedButton(
                    onClick = {
                        android.app.TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                spawnHour = hour
                                spawnMinute = minute
                                prefs.spawnHour = hour
                                prefs.spawnMinute = minute
                                RecurringEnsureScheduler.scheduleNext(context)
                                viewModel.ensureTodayRecurringInstances()
                            },
                            spawnHour,
                            spawnMinute,
                            true
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "${s.recurringSpawnDefaultTime}: ${DueReminderScheduler.formatClock(spawnHour, spawnMinute)}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = s.recurringSpawnDefaultTimeHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
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
