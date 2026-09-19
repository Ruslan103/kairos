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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.ui.board.BoardScreen
import com.example.kaizenkanban.ui.calendar.CalendarScreen
import com.example.kaizenkanban.ui.projects.ProjectsScreen
import com.example.kaizenkanban.ui.settings.SettingsScreen
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

private fun NavHostController.openBoard(boardId: String, columnId: String? = null, taskId: String? = null) {
    val route = buildString {
        append("board/$boardId")
        val query = buildList {
            if (!columnId.isNullOrBlank()) add("columnId=$columnId")
            if (!taskId.isNullOrBlank()) add("taskId=$taskId")
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

    LaunchedEffect(pendingQuickAdd, state.isInitialized) {
        if (!pendingQuickAdd || !state.isInitialized) return@LaunchedEffect
        if (navController.currentDestination?.route != "projects") {
            navController.goToProjects()
        }
    }

    LaunchedEffect(pendingOpenTaskId, state.isInitialized, state.tasks, state.columns) {
        val taskId = pendingOpenTaskId ?: return@LaunchedEffect
        if (!state.isInitialized) return@LaunchedEffect
        if (sharedViewModel.pendingQuickAdd.value) return@LaunchedEffect
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
                            sharedViewModel.pendingQuickAdd.value
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
                        onNavigateToCalendar = {
                            navController.navigate("calendar") {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToSettings = {
                            navController.navigate("settings") {
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
                onNavigateToCalendar = {
                    navController.navigate("calendar") {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate("settings") {
                        launchSingleTop = true
                    }
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
            route = "board/{boardId}?columnId={columnId}&taskId={taskId}",
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
                }
            )
        ) { backStackEntry ->
            val boardId = backStackEntry.arguments?.getString("boardId") ?: return@composable
            val columnId = backStackEntry.arguments?.getString("columnId")?.takeIf { it.isNotBlank() }
            val taskId = backStackEntry.arguments?.getString("taskId")?.takeIf { it.isNotBlank() }
            BoardScreen(
                boardId = boardId,
                initialColumnId = columnId,
                initialTaskId = taskId,
                viewModel = sharedViewModel,
                windowSizeClass = windowSizeClass,
                onBack = { navController.goToProjects() },
                onNavigateToCalendar = {
                    navController.navigate("calendar") {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate("settings") {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("calendar") {
            CalendarScreen(
                viewModel = sharedViewModel,
                onBack = { navController.popBackStack() },
                onOpenTask = { boardId, columnId, taskId ->
                    navController.openBoard(boardId, columnId, taskId)
                }
            )
        }
    }
}
