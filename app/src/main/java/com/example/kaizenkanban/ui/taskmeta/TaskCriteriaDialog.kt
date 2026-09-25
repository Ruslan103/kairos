package com.example.kaizenkanban.ui.taskmeta

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kaizenkanban.domain.model.Eisenhower
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.ui.components.DialogSectionDivider
import com.example.kaizenkanban.ui.i18n.LocalAppStrings

/**
 * Edit stats-related criteria: complexity, duration, quality (if done),
 * importance/urgency.
 */
@Composable
fun TaskCriteriaDialog(
    task: Task,
    onDismiss: () -> Unit,
    onUpdate: (Task) -> Unit,
    onQuality: (Int?) -> Unit = {}
) {
    val s = LocalAppStrings.current
    var complexity by remember(task.id, task.complexity) { mutableStateOf(task.complexity) }
    var duration by remember(task.id, task.estimatedMinutes) { mutableStateOf(task.estimatedMinutes) }
    var quality by remember(task.id, task.completionQuality) { mutableStateOf(task.completionQuality) }
    var quadrant by remember(task.id, task.eisenhowerQuadrant) { mutableStateOf(task.eisenhowerQuadrant) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.taskCriteriaTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                DialogSectionDivider()
                TaskComplexityPicker(
                    selected = complexity,
                    onSelect = {
                        complexity = it
                        onUpdate(task.copy(complexity = it))
                    }
                )

                DialogSectionDivider()
                TaskDurationPicker(
                    selectedMinutes = duration,
                    onSelect = {
                        duration = it
                        onUpdate(task.copy(estimatedMinutes = it))
                    }
                )

                if (task.isCompleted) {
                    DialogSectionDivider()
                    Text(s.rateQualityTitle, fontWeight = FontWeight.SemiBold)
                    Text(
                        s.rateQualityHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (1..5).forEach { n ->
                            FilterChip(
                                selected = quality == n,
                                onClick = {
                                    quality = n
                                    onQuality(n)
                                },
                                label = { Text(n.toString()) }
                            )
                        }
                    }
                }

                DialogSectionDivider()
                Text(s.importanceLabel, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(null, Eisenhower.Q1, Eisenhower.Q2, Eisenhower.Q3, Eisenhower.Q4)
                        .forEach { q ->
                            FilterChip(
                                selected = quadrant == q,
                                onClick = {
                                    quadrant = q
                                    onUpdate(
                                        task.copy(
                                            eisenhowerQuadrant = q,
                                            showEisenhowerButtons = q != null || task.showEisenhowerButtons
                                        )
                                    )
                                },
                                label = {
                                    Text(
                                        q?.let { Eisenhower.getBadge(it) } ?: "—"
                                    )
                                }
                            )
                        }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(s.done) }
        }
    )
}
