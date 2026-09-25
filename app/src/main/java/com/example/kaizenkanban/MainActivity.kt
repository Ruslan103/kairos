package com.example.kaizenkanban

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.kaizenkanban.data.local.AppDatabase
import com.example.kaizenkanban.data.local.KairosPreferences
import com.example.kaizenkanban.data.repository.KanbanRepositoryImpl
import com.example.kaizenkanban.domain.usecase.*
import com.example.kaizenkanban.reminders.DueReminderScheduler
import com.example.kaizenkanban.reminders.RecurringEnsureScheduler
import com.example.kaizenkanban.ui.i18n.AppLanguage
import com.example.kaizenkanban.ui.i18n.AppStrings
import com.example.kaizenkanban.ui.i18n.ProvideAppLanguage
import com.example.kaizenkanban.ui.navigation.AppNavigation
import com.example.kaizenkanban.ui.theme.KaizenKanbanTheme
import com.example.kaizenkanban.ui.theme.LocalAppThemeMode
import com.example.kaizenkanban.ui.theme.ProvideAppTheme
import com.example.kaizenkanban.ui.theme.ProvideEisenhowerPalette
import com.example.kaizenkanban.ui.theme.resolveDarkTheme
import com.example.kaizenkanban.ui.viewmodel.SharedViewModel
import com.example.kaizenkanban.ui.viewmodel.SharedViewModelFactory
import com.example.kaizenkanban.widget.KairosWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: SharedViewModel

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* reminders sync anyway; system may block until granted */ }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(this)
        val repository = KanbanRepositoryImpl(database.kanbanDao(), database)

        val kairosPrefs = KairosPreferences(this)
        val initializeDatabaseUseCase = InitializeDatabaseUseCase(
            repository,
            kairosPrefs.appLanguageCode,
            kairosPrefs
        )
        val addTaskUseCase = AddTaskUseCase(repository)
        val updateTaskUseCase = UpdateTaskUseCase(repository)
        val moveTaskUseCase = MoveTaskUseCase(repository)
        val addColumnUseCase = AddColumnUseCase(repository)
        val deleteColumnUseCase = DeleteColumnUseCase(repository)
        val renameColumnUseCase = RenameColumnUseCase(repository)
        val deleteTaskUseCase = DeleteTaskUseCase(repository)
        val setDefaultBoardUseCase = SetDefaultBoardUseCase(repository)
        val addCommentUseCase = AddCommentUseCase(repository)
        val deleteCommentUseCase = DeleteCommentUseCase(repository)
        val addColumnCommentUseCase = AddColumnCommentUseCase(repository)
        val deleteColumnCommentUseCase = DeleteColumnCommentUseCase(repository)
        val moveTaskToBoardUseCase = MoveTaskToBoardUseCase(repository, moveTaskUseCase)
        val exportDataUseCase = ExportDataUseCase(repository)
        val importDataUseCase = ImportDataUseCase(repository)
        val ensureRecurringInstancesUseCase = EnsureRecurringInstancesUseCase(repository)

        val factory = SharedViewModelFactory(
            repository,
            initializeDatabaseUseCase,
            addTaskUseCase,
            updateTaskUseCase,
            moveTaskUseCase,
            addColumnUseCase,
            deleteColumnUseCase,
            renameColumnUseCase,
            deleteTaskUseCase,
            setDefaultBoardUseCase,
            addCommentUseCase,
            deleteCommentUseCase,
            addColumnCommentUseCase,
            deleteColumnCommentUseCase,
            moveTaskToBoardUseCase,
            exportDataUseCase,
            importDataUseCase,
            ensureRecurringInstancesUseCase
        )
        viewModel = ViewModelProvider(this, factory)[SharedViewModel::class.java]

        intent?.let { handleIncomingIntent(it) }

        lifecycleScope.launch(Dispatchers.Default) {
            DueReminderScheduler.ensureChannel(this@MainActivity)
            RecurringEnsureScheduler.scheduleNext(this@MainActivity)
        }

        lifecycleScope.launch {
            viewModel.state
                .map { it.isInitialized }
                .first { it }
            // Let the first board frame paint before the permission dialog.
            delay(600.milliseconds)
            maybeRequestNotificationPermission()
        }

        lifecycleScope.launch {
            viewModel.state
                .map { it.isInitialized to it.tasks }
                .distinctUntilChanged()
                .collect { (initialized, tasks) ->
                    if (initialized) {
                        // Yield so Compose can draw the board before alarm/widget work.
                        delay(1)
                        withContext(Dispatchers.IO) {
                            DueReminderScheduler.sync(this@MainActivity, tasks)
                            KairosWidgetUpdater.updateAll(this@MainActivity, tasks)
                        }
                    }
                }
        }

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val prefs = remember { KairosPreferences(this) }
            ProvideAppLanguage(prefs) {
                ProvideAppTheme(prefs) {
                    ProvideEisenhowerPalette(prefs) {
                        val themeMode = LocalAppThemeMode.current
                        KaizenKanbanTheme(darkTheme = resolveDarkTheme(themeMode)) {
                            AppNavigation(
                                sharedViewModel = viewModel,
                                windowSizeClass = windowSizeClass.widthSizeClass
                            )
                        }
                    }
                }
            }
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIncomingIntent(it) }
    }

    private fun handleIncomingIntent(intent: Intent) {
        val taskId = intent.getStringExtra(DueReminderScheduler.EXTRA_TASK_ID)
        if (!taskId.isNullOrBlank()) {
            viewModel.requestOpenTask(taskId)
            intent.removeExtra(DueReminderScheduler.EXTRA_TASK_ID)
        }
        if (intent.getBooleanExtra(DueReminderScheduler.EXTRA_OPEN_QUICK_ADD, false)) {
            viewModel.requestQuickAdd()
            intent.removeExtra(DueReminderScheduler.EXTRA_OPEN_QUICK_ADD)
        }
        if (intent.getBooleanExtra(DueReminderScheduler.EXTRA_OPEN_VOICE, false)) {
            viewModel.requestVoiceAssistant()
            intent.removeExtra(DueReminderScheduler.EXTRA_OPEN_VOICE)
        }
        if (intent.getBooleanExtra(DueReminderScheduler.EXTRA_OPEN_ADD_TASK, false)) {
            viewModel.requestAddTask()
            intent.removeExtra(DueReminderScheduler.EXTRA_OPEN_ADD_TASK)
        }

        val uri = intent.data ?: return
        val strings = AppStrings(AppLanguage.fromCode(KairosPreferences(this).appLanguageCode))
        lifecycleScope.launch {
            try {
                if (uri.scheme == "kairos" && uri.host == "import") {
                    val count = viewModel.importFromDeepLink(uri)
                    Toast.makeText(this@MainActivity, strings.importedFromMessenger(count), Toast.LENGTH_LONG).show()
                } else if (uri.scheme == "content" || uri.scheme == "file") {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        val json = stream.bufferedReader().readText()
                        val count = viewModel.importFromJson(json)
                        Toast.makeText(this@MainActivity, strings.importedFromFile(count), Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, strings.importError(e.localizedMessage), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
