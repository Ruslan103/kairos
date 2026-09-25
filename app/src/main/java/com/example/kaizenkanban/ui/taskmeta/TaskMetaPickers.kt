package com.example.kaizenkanban.ui.taskmeta

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
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
import com.example.kaizenkanban.ui.i18n.LocalAppStrings

@Composable
fun TaskComplexityPicker(
    selected: Int?,
    onSelect: (Int?) -> Unit
) {
    val s = LocalAppStrings.current
    Text(s.taskComplexity, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        s.taskComplexityHint,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        (1..5).forEach { n ->
            FilterChip(
                selected = selected == n,
                onClick = { onSelect(if (selected == n) null else n) },
                label = { Text(n.toString()) }
            )
        }
    }
}

@Composable
fun TaskDurationPicker(
    selectedMinutes: Int?,
    onSelect: (Int?) -> Unit
) {
    val s = LocalAppStrings.current
    val presets = listOf(15, 30, 60, 120)
    Text(s.taskDuration, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedMinutes == null,
            onClick = { onSelect(null) },
            label = { Text(s.taskDurationNone) }
        )
        presets.forEach { mins ->
            FilterChip(
                selected = selectedMinutes == mins,
                onClick = { onSelect(if (selectedMinutes == mins) null else mins) },
                label = { Text(s.taskDurationMinutes(mins)) }
            )
        }
    }
}

@Composable
fun CompletionQualityDialog(
    title: String,
    current: Int?,
    onDismiss: () -> Unit,
    onSave: (Int?) -> Unit
) {
    val s = LocalAppStrings.current
    var selected by remember(current) { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.rateQualityTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    s.rateQualityHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..5).forEach { n ->
                        FilterChip(
                            selected = selected == n,
                            onClick = { selected = n },
                            label = { Text(n.toString()) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(selected)
                    onDismiss()
                },
                enabled = selected != null
            ) { Text(s.save) }
        },
        dismissButton = {
            TextButton(onClick = {
                onSave(null)
                onDismiss()
            }) { Text(s.rateQualitySkip) }
        }
    )
}
