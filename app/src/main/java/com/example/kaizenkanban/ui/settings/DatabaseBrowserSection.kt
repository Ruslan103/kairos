package com.example.kaizenkanban.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kaizenkanban.domain.model.Board
import com.example.kaizenkanban.domain.model.Column
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.ui.components.DialogMarqueeText
import com.example.kaizenkanban.ui.i18n.LocalAppStrings
import com.example.kaizenkanban.ui.viewmodel.SharedViewModel

private enum class DbBrowserPanel { Tasks, Hubs, Boards }

@Composable
fun DatabaseBrowserSection(viewModel: SharedViewModel) {
    val s = LocalAppStrings.current
    val state by viewModel.state.collectAsState()
    var panel by remember { mutableStateOf<DbBrowserPanel?>(null) }
    var pendingHardDelete by remember { mutableStateOf<(() -> Unit)?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    Text(
        text = s.dataBrowserHint,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    OutlinedButton(
        onClick = {
            searchQuery = ""
            panel = DbBrowserPanel.Tasks
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(s.allTasksHeader(state.tasks.size))
    }
    OutlinedButton(
        onClick = {
            searchQuery = ""
            panel = DbBrowserPanel.Hubs
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(s.allHubsHeader(state.columns.size))
    }
    OutlinedButton(
        onClick = {
            searchQuery = ""
            panel = DbBrowserPanel.Boards
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(s.allBoardsHeader(state.boards.size))
    }

    panel?.let { current ->
        val allTasks = remember(state.tasks) {
            state.tasks.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenBy { it.dueDate ?: Long.MAX_VALUE }
                    .thenByDescending { it.createdAt }
            )
        }
        val filteredTasks = remember(allTasks, searchQuery) {
            val q = searchQuery.trim()
            if (q.isEmpty()) allTasks
            else allTasks.filter { it.title.contains(q, ignoreCase = true) }
        }
        val allHubs = remember(state.columns, state.boards) {
            state.columns.sortedWith(
                compareBy<Column> { col -> state.boards.find { it.id == col.boardId }?.name.orEmpty() }
                    .thenBy { it.position }
            )
        }
        val filteredHubs = remember(allHubs, searchQuery, state.boards) {
            val q = searchQuery.trim()
            if (q.isEmpty()) allHubs
            else allHubs.filter { hub ->
                s.localized(hub.title).contains(q, ignoreCase = true) ||
                    state.boards.find { it.id == hub.boardId }?.name?.let { s.localized(it) }
                        ?.contains(q, ignoreCase = true) == true
            }
        }
        val allBoards = remember(state.boards, state.projects) {
            state.boards.sortedWith(
                compareBy<Board> { board -> state.projects.find { it.id == board.projectId }?.name.orEmpty() }
                    .thenBy { it.name }
            )
        }
        val filteredBoards = remember(allBoards, searchQuery) {
            val q = searchQuery.trim()
            if (q.isEmpty()) allBoards
            else allBoards.filter {
                s.localized(it.name).contains(q, ignoreCase = true)
            }
        }
        val title = when (current) {
            DbBrowserPanel.Tasks -> s.allTasksHeader(allTasks.size)
            DbBrowserPanel.Hubs -> s.allHubsHeader(allHubs.size)
            DbBrowserPanel.Boards -> s.allBoardsHeader(allBoards.size)
        }

        AlertDialog(
            onDismissRequest = { panel = null },
            title = {
                DialogMarqueeText(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text(s.searchTasks) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    when (current) {
                        DbBrowserPanel.Tasks -> {
                            if (filteredTasks.isEmpty()) {
                                item {
                                    Text(
                                        if (allTasks.isEmpty()) s.noTasks else s.noMatchingTasks,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                items(filteredTasks, key = { "db-task-${it.id}" }) { task ->
                                    val column = state.columns.find { it.id == task.columnId }
                                    val board = column?.let { col -> state.boards.find { it.id == col.boardId } }
                                    val project = board?.let { b -> state.projects.find { it.id == b.projectId } }
                                    val location = listOfNotNull(
                                        project?.name,
                                        board?.name?.let { s.localized(it) },
                                        column?.title?.let { s.localized(it) }
                                    ).joinToString(" · ")
                                    val badges = buildList {
                                        if (task.isHidden) add(s.statusHidden)
                                        if (task.isBoardArchived) add(s.statusArchived)
                                        if (board?.isArchived == true) add(s.statusArchived)
                                    }.distinct().joinToString(" · ")
                                    DbBrowserRow(
                                        title = task.title,
                                        subtitle = listOfNotNull(
                                            location.takeIf { it.isNotBlank() },
                                            badges.takeIf { it.isNotBlank() }
                                        ).joinToString(" · "),
                                        onDelete = {
                                            pendingHardDelete = {
                                                viewModel.hardDeleteTask(task.id)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        DbBrowserPanel.Hubs -> {
                            if (filteredHubs.isEmpty()) {
                                item {
                                    Text(s.noHubs, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                items(filteredHubs, key = { "db-hub-${it.id}" }) { hub ->
                                    val board = state.boards.find { it.id == hub.boardId }
                                    val project = board?.let { b -> state.projects.find { it.id == b.projectId } }
                                    val location = listOfNotNull(
                                        project?.name,
                                        board?.name?.let { s.localized(it) }
                                    ).joinToString(" · ")
                                    val badge = if (board?.isArchived == true) s.statusArchived else null
                                    DbBrowserRow(
                                        title = s.localized(hub.title),
                                        subtitle = listOfNotNull(location, badge).joinToString(" · "),
                                        onDelete = {
                                            pendingHardDelete = {
                                                viewModel.hardDeleteColumn(hub.id)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        DbBrowserPanel.Boards -> {
                            if (filteredBoards.isEmpty()) {
                                item {
                                    Text(s.noBoards, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                items(filteredBoards, key = { "db-board-${it.id}" }) { board ->
                                    val project = state.projects.find { it.id == board.projectId }
                                    val badge = if (board.isArchived) s.statusArchived else null
                                    DbBrowserRow(
                                        title = s.localized(board.name),
                                        subtitle = listOfNotNull(project?.name, badge).joinToString(" · "),
                                        onDelete = {
                                            pendingHardDelete = {
                                                viewModel.hardDeleteBoard(board.id)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { panel = null }) { Text(s.close) }
            }
        )
    }

    pendingHardDelete?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingHardDelete = null },
            title = { Text(s.hardDeleteFromDb) },
            text = { Text(s.hardDeleteConfirm) },
            confirmButton = {
                TextButton(
                    onClick = {
                        action()
                        pendingHardDelete = null
                    }
                ) {
                    Text(s.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingHardDelete = null }) { Text(s.cancel) }
            }
        )
    }
}

@Composable
private fun DbBrowserRow(
    title: String,
    subtitle: String,
    onDelete: () -> Unit
) {
    val s = LocalAppStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = s.hardDeleteFromDb,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
