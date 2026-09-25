package com.example.kaizenkanban.ui.eisenhower

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kaizenkanban.domain.model.Eisenhower
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.ui.i18n.KanbanNames
import com.example.kaizenkanban.ui.i18n.LocalAppStrings
import com.example.kaizenkanban.ui.theme.eisenhowerGradient
import com.example.kaizenkanban.ui.theme.eisenhowerOnColor
import com.example.kaizenkanban.ui.viewmodel.SharedViewModel

/**
 * Compact 2×2 summary (counts) + full-width list for the selected quadrant.
 * Fits small screens better than four task lists at once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EisenhowerMatrixScreen(
    projectId: String,
    viewModel: SharedViewModel,
    onBack: () -> Unit,
    onOpenTask: (boardId: String, columnId: String, taskId: String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val s = LocalAppStrings.current
    val project = state.projects.find { it.id == projectId }
    val projectBoardIds = remember(state.boards, projectId) {
        state.boards.filter { it.projectId == projectId }.map { it.id }.toSet()
    }
    val projectColumnIds = remember(state.columns, projectBoardIds) {
        state.columns.filter { it.boardId in projectBoardIds }.map { it.id }.toSet()
    }
    val columnToBoard = remember(state.columns) {
        state.columns.associate { it.id to it.boardId }
    }

    val byQuadrant = remember(state.tasks, projectColumnIds) {
        state.tasks
            .filter {
                !it.isCompleted &&
                    !it.isHidden &&
                    it.columnId in projectColumnIds &&
                    it.eisenhowerQuadrant != null
            }
            .groupBy { it.eisenhowerQuadrant.orEmpty() }
    }

    var selectedQuadrant by rememberSaveable { mutableStateOf(Eisenhower.Q1) }
    val selectedTasks = byQuadrant[selectedQuadrant].orEmpty()

    val untagged = remember(state.tasks, projectColumnIds, columnToBoard, state.boards) {
        val nonMatrixColumns = projectColumnIds.filter { colId ->
            val boardId = columnToBoard[colId] ?: return@filter false
            val board = state.boards.find { it.id == boardId } ?: return@filter false
            !KanbanNames.isEisenhowerBoard(board.name)
        }.toSet()
        state.tasks.count {
            !it.isCompleted &&
                !it.isHidden &&
                it.columnId in nonMatrixColumns &&
                it.eisenhowerQuadrant == null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(s.eisenhowerMatrixTitle, fontWeight = FontWeight.Bold)
                        if (project != null) {
                            Text(
                                project.name,
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
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                s.eisenhowerMatrixHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 6.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AxisLabel(s.eisenhowerAxisUrgent, Modifier.weight(1f))
                AxisLabel(s.eisenhowerAxisNotUrgent, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(4.dp))

            SummaryGrid(
                counts = mapOf(
                    Eisenhower.Q1 to byQuadrant[Eisenhower.Q1].orEmpty().size,
                    Eisenhower.Q2 to byQuadrant[Eisenhower.Q2].orEmpty().size,
                    Eisenhower.Q3 to byQuadrant[Eisenhower.Q3].orEmpty().size,
                    Eisenhower.Q4 to byQuadrant[Eisenhower.Q4].orEmpty().size
                ),
                selected = selectedQuadrant,
                onSelect = { selectedQuadrant = it }
            )

            if (untagged > 0) {
                Text(
                    s.eisenhowerUntaggedHint(untagged),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "${Eisenhower.getBadge(selectedQuadrant)} · ${s.eisenhowerTitle(selectedQuadrant)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                s.eisenhowerCount(selectedTasks.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            if (selectedTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        s.eisenhowerEmptyCell,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedTasks, key = { it.id }) { task ->
                        MatrixTaskCard(
                            task = task,
                            quadrant = selectedQuadrant,
                            onOpen = {
                                val boardId = columnToBoard[task.columnId] ?: return@MatrixTaskCard
                                onOpenTask(boardId, task.columnId, task.id)
                            },
                            onMove = { q ->
                                viewModel.updateTask(
                                    task.copy(
                                        eisenhowerQuadrant = q,
                                        showEisenhowerButtons = true
                                    )
                                )
                            },
                            onClear = {
                                viewModel.updateTask(
                                    task.copy(
                                        eisenhowerQuadrant = null,
                                        showEisenhowerButtons = false
                                    )
                                )
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AxisLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SummaryGrid(
    counts: Map<String, Int>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SummaryCell(
                quadrant = Eisenhower.Q1,
                count = counts[Eisenhower.Q1] ?: 0,
                selected = selected == Eisenhower.Q1,
                onClick = { onSelect(Eisenhower.Q1) },
                modifier = Modifier.weight(1f)
            )
            SummaryCell(
                quadrant = Eisenhower.Q2,
                count = counts[Eisenhower.Q2] ?: 0,
                selected = selected == Eisenhower.Q2,
                onClick = { onSelect(Eisenhower.Q2) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SummaryCell(
                quadrant = Eisenhower.Q3,
                count = counts[Eisenhower.Q3] ?: 0,
                selected = selected == Eisenhower.Q3,
                onClick = { onSelect(Eisenhower.Q3) },
                modifier = Modifier.weight(1f)
            )
            SummaryCell(
                quadrant = Eisenhower.Q4,
                count = counts[Eisenhower.Q4] ?: 0,
                selected = selected == Eisenhower.Q4,
                onClick = { onSelect(Eisenhower.Q4) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryCell(
    quadrant: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val shape = RoundedCornerShape(12.dp)
    val borderWidth = if (selected) 2.5.dp else 1.dp
    val borderColor = if (selected) Color.White else Color.White.copy(alpha = 0.28f)
    Column(
        modifier = modifier
            .heightIn(min = 72.dp, max = 80.dp)
            .clip(shape)
            .background(eisenhowerGradient(quadrant))
            .border(borderWidth, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Eisenhower.getBadge(quadrant),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = eisenhowerOnColor(quadrant)
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = eisenhowerOnColor(quadrant)
            )
        }
        Text(
            text = s.eisenhowerTitleShort(quadrant),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = eisenhowerOnColor(quadrant).copy(alpha = 0.95f),
            maxLines = 2,
            overflow = TextOverflow.Clip,
            lineHeight = MaterialTheme.typography.labelSmall.lineHeight
        )
    }
}

@Composable
private fun MatrixTaskCard(
    task: Task,
    quadrant: String,
    onOpen: () -> Unit,
    onMove: (String) -> Unit,
    onClear: () -> Unit
) {
    val s = LocalAppStrings.current
    var menuOpen by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(eisenhowerGradient(quadrant))
            .border(1.dp, Color.White.copy(alpha = 0.28f), shape)
            .clickable(onClick = onOpen)
            .background(Color.Black.copy(alpha = 0.18f))
            .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = eisenhowerOnColor(quadrant),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = s.eisenhowerMove,
                    tint = eisenhowerOnColor(quadrant)
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                listOf(Eisenhower.Q1, Eisenhower.Q2, Eisenhower.Q3, Eisenhower.Q4).forEach { q ->
                    DropdownMenuItem(
                        text = { Text("${Eisenhower.getBadge(q)} · ${s.eisenhowerTitle(q)}") },
                        onClick = {
                            menuOpen = false
                            onMove(q)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(s.eisenhowerClearQuadrant) },
                    onClick = {
                        menuOpen = false
                        onClear()
                    }
                )
            }
        }
    }
}
