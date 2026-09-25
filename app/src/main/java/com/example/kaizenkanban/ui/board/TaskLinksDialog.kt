package com.example.kaizenkanban.ui.board

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kaizenkanban.domain.model.Column
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.domain.model.TaskLink
import com.example.kaizenkanban.domain.model.TaskLinkGraph
import com.example.kaizenkanban.ui.components.DialogSectionDivider
import com.example.kaizenkanban.ui.i18n.LocalAppStrings

@Composable
fun TaskLinksDialog(
    task: Task,
    allTasks: List<Task>,
    columns: List<Column>,
    links: List<TaskLink>,
    onAddParent: (parentId: String) -> Boolean,
    onAddChild: (childId: String) -> Boolean,
    onRemoveLink: (parentId: String, childId: String) -> Unit,
    onDismiss: () -> Unit,
    onCycleRejected: () -> Unit = {}
) {
    val s = LocalAppStrings.current
    val parents = remember(task.id, links) {
        TaskLinkGraph.parentsOf(links)[task.id].orEmpty()
    }
    val children = remember(task.id, links) {
        TaskLinkGraph.childrenOf(links)[task.id].orEmpty()
    }
    val tasksById = remember(allTasks) { allTasks.associateBy { it.id } }
    var pickMode by remember { mutableStateOf<PickMode?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = s.taskLinksTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinkSectionHeader(
                    title = s.taskParents,
                    onAdd = { pickMode = PickMode.Parent }
                )
                if (parents.isEmpty()) {
                    Text(
                        text = s.taskLinksEmptyParents,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    parents.forEach { parentId ->
                        LinkedTaskRow(
                            title = tasksById[parentId]?.title ?: parentId,
                            onRemove = { onRemoveLink(parentId, task.id) }
                        )
                    }
                }

                DialogSectionDivider()

                LinkSectionHeader(
                    title = s.taskChildren,
                    onAdd = { pickMode = PickMode.Child }
                )
                if (children.isEmpty()) {
                    Text(
                        text = s.taskLinksEmptyChildren,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    children.forEach { childId ->
                        LinkedTaskRow(
                            title = tasksById[childId]?.title ?: childId,
                            onRemove = { onRemoveLink(task.id, childId) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(s.close) }
        }
    )

    val mode = pickMode
    if (mode != null) {
        val candidates = remember(mode, allTasks, links, task.id, parents, children) {
            val linked = when (mode) {
                PickMode.Parent -> parents.toSet()
                PickMode.Child -> children.toSet()
            }
            allTasks.filter { candidate ->
                candidate.id != task.id &&
                    candidate.id !in linked &&
                    !when (mode) {
                        PickMode.Parent -> TaskLinkGraph.wouldCreateCycle(links, candidate.id, task.id)
                        PickMode.Child -> TaskLinkGraph.wouldCreateCycle(links, task.id, candidate.id)
                    }
            }
        }
        TaskLinkPickerDialog(
            title = when (mode) {
                PickMode.Parent -> s.pickParentTask
                PickMode.Child -> s.pickChildTask
            },
            candidates = candidates,
            columns = columns,
            onPick = { candidate ->
                val ok = when (mode) {
                    PickMode.Parent -> onAddParent(candidate.id)
                    PickMode.Child -> onAddChild(candidate.id)
                }
                if (!ok) onCycleRejected()
                pickMode = null
            },
            onDismiss = { pickMode = null }
        )
    }
}

@Composable
fun TaskLinkPickerDialog(
    title: String,
    candidates: List<Task>,
    columns: List<Column>,
    onPick: (Task) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    var query by remember { mutableStateOf("") }
    val columnById = remember(columns) { columns.associateBy { it.id } }
    val columnOrder = remember(columns) {
        columns.mapIndexed { index, col -> col.id to index }.toMap()
    }
    val filtered = remember(candidates, query) {
        val q = query.trim()
        if (q.isEmpty()) candidates
        else candidates.filter { it.title.contains(q, ignoreCase = true) }
    }
    val grouped = remember(filtered, columnById, columnOrder) {
        filtered
            .groupBy { task -> task.columnId }
            .toList()
            .sortedBy { (columnId, _) -> columnOrder[columnId] ?: Int.MAX_VALUE }
            .map { (columnId, hubTasks) ->
                val hubTitle = columnById[columnId]?.let { s.localized(it.title) } ?: s.unknownHub
                Triple(columnId, hubTitle, hubTasks.sortedBy { it.title.lowercase() })
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    },
                    placeholder = { Text(s.searchTasks) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                if (filtered.isEmpty()) {
                    Text(
                        text = s.noTasksToLink,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        grouped.forEach { (columnId, hubTitle, hubTasks) ->
                            item(key = "hub-$columnId") {
                                Text(
                                    text = hubTitle,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                                )
                            }
                            items(hubTasks, key = { it.id }) { candidate ->
                                Text(
                                    text = candidate.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onPick(candidate) }
                                        .padding(vertical = 12.dp, horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(s.cancel) }
        }
    )
}

private enum class PickMode { Parent, Child }

@Composable
private fun LinkSectionHeader(title: String, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = null)
        }
    }
}

@Composable
private fun LinkedTaskRow(title: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Default.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
