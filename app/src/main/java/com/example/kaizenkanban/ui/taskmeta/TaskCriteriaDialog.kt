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
import androidx.compose.ui.unit.dp
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.ui.components.DialogMarqueeText
import com.example.kaizenkanban.ui.components.DialogSectionDivider
import com.example.kaizenkanban.ui.i18n.LocalAppStrings

/**
 * Edit stats-related criteria: complexity, quality (if done).
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
    var quality by remember(task.id, task.completionQuality) { mutableStateOf(task.completionQuality) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            DialogMarqueeText(
                text = s.taskCriteriaTitle,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DialogMarqueeText(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )

                DialogSectionDivider()
                TaskComplexityPicker(
                    selected = complexity,
                    onSelect = {
                        complexity = it
                        onUpdate(task.copy(complexity = it))
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
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(s.done) }
        }
    )
}
