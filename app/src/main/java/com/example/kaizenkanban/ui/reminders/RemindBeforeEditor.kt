package com.example.kaizenkanban.ui.reminders

import com.example.kaizenkanban.domain.model.RemindLeadCalculator
import com.example.kaizenkanban.domain.model.RemindLeadUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.kaizenkanban.ui.i18n.LocalAppStrings

fun leadReminderPreview(
    enabled: Boolean,
    dueDate: Long?,
    reminderMinutes: Int?,
    amount: Int,
    unit: RemindLeadUnit,
    defaultMinutes: Int,
    locale: Locale,
    firesLabel: (String) -> String
): String? {
    if (!enabled || dueDate == null) return null
    val anchor = RemindLeadCalculator.taskAnchor(dueDate, reminderMinutes, defaultMinutes)
    val trigger = RemindLeadCalculator.subtract(anchor, amount, unit)
    val formatted = SimpleDateFormat("d MMM, HH:mm", locale).format(Date(trigger))
    return firesLabel(formatted)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemindBeforeEditor(
    enabled: Boolean,
    amount: Int,
    unit: RemindLeadUnit,
    onEnabledChange: (Boolean) -> Unit,
    onAmountChange: (Int) -> Unit,
    onUnitChange: (RemindLeadUnit) -> Unit,
    text: String? = null,
    onTextChange: ((String) -> Unit)? = null,
    preview: String? = null,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = s.remindBefore,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
        if (enabled) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = amount.coerceAtLeast(1).toString(),
                    onValueChange = { raw ->
                        val n = raw.filter { it.isDigit() }.take(3).toIntOrNull()
                        if (n != null && n > 0) onAmountChange(n.coerceIn(1, 999))
                    },
                    modifier = Modifier.width(88.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text(s.remindAmount) }
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    RemindLeadUnit.entries.forEach { option ->
                        FilterChip(
                            selected = unit == option,
                            onClick = { onUnitChange(option) },
                            label = { Text(s.remindUnitShort(option)) }
                        )
                    }
                }
            }
            if (onTextChange != null) {
                OutlinedTextField(
                    value = text.orEmpty(),
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(s.reminderTextLabel) },
                    singleLine = true
                )
            }
            if (!preview.isNullOrBlank()) {
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
