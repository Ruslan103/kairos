package com.example.kaizenkanban.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.kaizenkanban.data.local.KairosPreferences
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.ui.aiprompts.AiPromptsScreen
import com.example.kaizenkanban.ui.board.BoardScreen
import com.example.kaizenkanban.ui.board.TaskCardScreen
import com.example.kaizenkanban.ui.board.TaskTimerCorner
import com.example.kaizenkanban.ui.calendar.CalendarScreen
import com.example.kaizenkanban.ui.eisenhower.EisenhowerMatrixScreen
import com.example.kaizenkanban.ui.i18n.KanbanNames
import com.example.kaizenkanban.ui.projects.ProjectsScreen
import com.example.kaizenkanban.ui.recurring.RecurringScreen
import com.example.kaizenkanban.ui.settings.SettingsScreen
import com.example.kaizenkanban.ui.stats.StatsScreen
import com.example.kaizenkanban.ui.viewmodel.AppState
import com.example.kaizenkanban.ui.viewmodel.SharedViewModel
import kotlinx.coroutines.delay

private fun resolveTaskRoute(task: Task, state: AppState): String? {
    val column = state.columns.find { it.id == task.columnId }
        ?: task.linkedColumnIds.firstNotNullOfOrNull { linkedId ->
            state.columns.find { it.id == linkedId }
        }
        ?: return null
    return "board/${column.boardId}?columnId=${column.id}&taskId=${task.id}"
}

private fun NavHostController.goToProjects() {
    navigate("projects") {
        popUpTo("root") { inclusive = false }
        launchSingleTop = true
    }
}

private fun NavHostController.openBoard(
    boardId: String,
    columnId: String? = null,
    taskId: String? = null,
    addTask: Boolean = false,
    dueMillis: Long? = null
) {
    val route = buildString {
        append("board/$boardId")
        val query = buildList {
            if (!columnId.isNullOrBlank()) add("columnId=$columnId")
            if (!taskId.isNullOrBlank()) add("taskId=$taskId")
            if (addTask) add("addTask=1")
            if (dueMillis != null) add("due=$dueMillis")
        }
        if (query.isNotEmpty()) {
            append('?')
            append(query.joinToString("&"))
        }
    }
    navigate(route) {
        popUpTo("root") { inclusive = false }
        launchSingleTop = true
    }
}

@Composable
fun AppNavigation(
    sharedViewModel: SharedViewModel,
    windowSizeClass: WindowWidthSizeClass,
    navController: NavHostController = rememberNavController()
) {
    val state by sharedViewModel.state.collectAsState()
    val pendingOpenTaskId by sharedViewModel.pendingOpenTaskId.collectAsState()
    val pendingQuickAdd by sharedViewModel.pendingQuickAdd.collectAsState()
    val pendingAddTask by sharedViewModel.pendingAddTask.collectAsState()
    val pendingAddNote by sharedViewModel.pendingAddNote.collectAsState()
    val context = LocalContext.current
    val kairosPrefs = remember { KairosPreferences(context) }

    LaunchedEffect(pendingQuickAdd, state.isInitialized) {
        if (!pendingQuickAdd || !state.isInitialized) return@LaunchedEffect
        if (navController.currentDestination?.route != "projects") {
            navController.goToProjects()
        }
    }

    LaunchedEffect(pendingAddTask, state.isInitialized) {
        if (!pendingAddTask || !state.isInitialized) return@LaunchedEffect
        val board = state.boards.find { it.isDefault && !it.isArchived }
            ?: state.boards.firstOrNull { !it.isArchived }
            ?: return@LaunchedEffect
        val boardColumns = state.columns.filter { it.boardId == board.id }.sortedBy { it.position }
        if (boardColumns.isEmpty()) return@LaunchedEffect
        val preferredHub = kairosPrefs.getPrimaryHubId(board.id)
        val columnId = preferredHub?.takeIf { id -> boardColumns.any { it.id == id } }
            ?: boardColumns.first().id
        sharedViewModel.hasAutoNavigated = true
        val args = navController.currentBackStackEntry?.arguments
        val alreadyOnBoard = navController.currentDestination?.route?.startsWith("board/") == true &&
            args?.getString("boardId") == board.id
        if (!alreadyOnBoard) {
            navController.openBoard(board.id, columnId, addTask = true)
        }
    }

    LaunchedEffect(pendingAddNote, state.isInitialized) {
        if (!pendingAddNote || !state.isInitialized) return@LaunchedEffect
        val board = state.boards.find { it.isDefault && !it.isArchived }
            ?: state.boards.firstOrNull { !it.isArchived }
            ?: return@LaunchedEffect
        sharedViewModel.hasAutoNavigated = true
        val route = "notes/${board.projectId}"
        val alreadyOnNotes = navController.currentDestination?.route == "notes/{projectId}" &&
            navController.currentBackStackEntry?.arguments?.getString("projectId") == board.projectId
        if (!alreadyOnNotes) {
            navController.navigate(route) {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(pendingOpenTaskId, state.isInitialized, state.tasks, state.columns) {
        val taskId = pendingOpenTaskId ?: return@LaunchedEffect
        if (!state.isInitialized) return@LaunchedEffect
        if (sharedViewModel.pendingQuickAdd.value) return@LaunchedEffect
        if (sharedViewModel.pendingAddTask.value) return@LaunchedEffect
        if (sharedViewModel.pendingAddNote.value) return@LaunchedEffect
        val task = state.tasks.find { it.id == taskId }
        if (task == null) {
            sharedViewModel.clearPendingOpenTask()
            return@LaunchedEffect
        }
        val route = resolveTaskRoute(task, state) ?: run {
            sharedViewModel.clearPendingOpenTask()
            return@LaunchedEffect
        }
        sharedViewModel.clearPendingOpenTask()
        sharedViewModel.hasAutoNavigated = true
        val column = state.columns.find { it.id == task.columnId }
            ?: task.linkedColumnIds.firstNotNullOfOrNull { linkedId ->
                state.columns.find { it.id == linkedId }
            }
        if (column != null) {
            navController.openBoard(column.boardId, column.id, task.id)
        } else {
            navController.navigate(route) {
                launchSingleTop = true
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
    NavHost(
        navController = navController,
        startDestination = "root",
        enterTransition = { fadeIn(animationSpec = tween(90)) },
        exitTransition = { fadeOut(animationSpec = tween(70)) },
        popEnterTransition = { fadeIn(animationSpec = tween(90)) },
        popExitTransition = { fadeOut(animationSpec = tween(70)) }
    ) {
        composable("root") {
            if (!state.isInitialized) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            } else {
                var showProjects by remember {
                    mutableStateOf(sharedViewModel.hasAutoNavigated)
                }
                LaunchedEffect(Unit) {
                    if (!sharedViewModel.hasAutoNavigated) {
                        sharedViewModel.hasAutoNavigated = true
                        val skipAutoBoard = sharedViewModel.pendingOpenTaskId.value != null ||
                            sharedViewModel.pendingQuickAdd.value ||
                            sharedViewModel.pendingAddTask.value ||
                            sharedViewModel.pendingAddNote.value
                        if (!skipAutoBoard) {
                            val primary = state.boards.find { it.isDefault && !it.isArchived }
                                ?: state.boards.firstOrNull { !it.isArchived }
                            if (primary != null) {
                                navController.openBoard(primary.id)
                                // Defer Projects composition until after board is on screen.
                                delay(48)
                            }
                        }
                    }
                    showProjects = true
                }
                if (!showProjects) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else {
                    ProjectsScreen(
                        viewModel = sharedViewModel,
                        consumePendingQuickAdd = false,
                        onNavigateToBoard = { boardId, columnId ->
                            navController.openBoard(boardId, columnId)
                        },
                        onNavigateToCalendar = { projectId ->
                            navController.navigate("calendar/$projectId") {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToSettings = {
                            navController.navigate("settings") {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToRecurring = { projectId ->
                            navController.navigate("recurring/$projectId") {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToEisenhower = { projectId ->
                            navController.navigate("eisenhower/$projectId") {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToStats = { projectId ->
                            navController.navigate("stats/$projectId") {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToAiPrompts = { projectId ->
                            navController.navigate("aiPrompts/$projectId") {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToNotes = { projectId ->
                            navController.navigate("notes/$projectId") {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }

        composable("projects") {
            ProjectsScreen(
                viewModel = sharedViewModel,
                consumePendingQuickAdd = true,
                onNavigateToBoard = { boardId, columnId ->
                    navController.openBoard(boardId, columnId)
                },
                onNavigateToCalendar = { projectId ->
                    navController.navigate("calendar/$projectId") {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate("settings") {
                        launchSingleTop = true
                    }
                },
                onNavigateToRecurring = { projectId ->
                    navController.navigate("recurring/$projectId") {
                        launchSingleTop = true
                    }
                },
                onNavigateToEisenhower = { projectId ->
                    navController.navigate("eisenhower/$projectId") {
                        launchSingleTop = true
                    }
                },
                onNavigateToStats = { projectId ->
                    navController.navigate("stats/$projectId") {
                        launchSingleTop = true
                    }
                },
                onNavigateToAiPrompts = { projectId ->
                    navController.navigate("aiPrompts/$projectId") {
                        launchSingleTop = true
                    }
                },
                onNavigateToNotes = { projectId ->
                    navController.navigate("notes/$projectId") {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = "recurring/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            RecurringScreen(
                projectId = projectId,
                viewModel = sharedViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "stats/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            StatsScreen(
                projectId = projectId,
                viewModel = sharedViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "aiPrompts/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            AiPromptsScreen(
                projectId = projectId,
                viewModel = sharedViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "eisenhower/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            EisenhowerMatrixScreen(
                projectId = projectId,
                viewModel = sharedViewModel,
                onBack = { navController.popBackStack() },
                onOpenTask = { boardId, columnId, taskId ->
                    navController.openBoard(boardId, columnId, taskId)
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = sharedViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "board/{boardId}?columnId={columnId}&taskId={taskId}&addTask={addTask}&due={due}",
            arguments = listOf(
                navArgument("boardId") { type = NavType.StringType },
                navArgument("columnId") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                },
                navArgument("taskId") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                },
                navArgument("addTask") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                },
                navArgument("due") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val boardId = backStackEntry.arguments?.getString("boardId") ?: return@composable
            val columnId = backStackEntry.arguments?.getString("columnId")?.takeIf { it.isNotBlank() }
            val taskId = backStackEntry.arguments?.getString("taskId")?.takeIf { it.isNotBlank() }
            val openAddTask = backStackEntry.arguments?.getString("addTask") == "1"
            val dueMillis = backStackEntry.arguments?.getString("due")?.toLongOrNull()
            val board = state.boards.find { it.id == boardId }
            if (board != null && KanbanNames.isEisenhowerBoard(board.name)) {
                LaunchedEffect(board.id) {
                    navController.navigate("eisenhower/${board.projectId}") {
                        launchSingleTop = true
                    }
                }
                return@composable
            }
            BoardScreen(
                boardId = boardId,
                initialColumnId = columnId,
                initialTaskId = taskId,
                initialOpenAddTask = openAddTask,
                initialDueMillis = dueMillis,
                viewModel = sharedViewModel,
                windowSizeClass = windowSizeClass,
                onOpenTypedTask = { typedTaskId ->
                    navController.navigate("card/$typedTaskId") {
                        launchSingleTop = true
                    }
                },
                onBack = { navController.goToProjects() },
                onOpenEisenhowerMatrix = { projectId ->
                    navController.navigate("eisenhower/$projectId") {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = "card/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: return@composable
            TaskCardScreen(
                taskId = taskId,
                viewModel = sharedViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "notes/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            com.example.kaizenkanban.ui.notes.NotesScreen(
                projectId = projectId,
                viewModel = sharedViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "calendar/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            CalendarScreen(
                projectId = projectId,
                viewModel = sharedViewModel,
                onBack = { navController.popBackStack() },
                onOpenTask = { boardId, columnId, taskId ->
                    navController.openBoard(boardId, columnId, taskId)
                },
                onAddTask = { boardId, columnId, dueMillis ->
                    navController.openBoard(boardId, columnId, addTask = true, dueMillis = dueMillis)
                }
            )
        }
    }
    TaskTimerCorner(
        tasks = state.tasks,
        modifier = Modifier.fillMaxSize()
    )
    }
}
