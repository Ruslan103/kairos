package com.example.kaizenkanban.ui.taskmeta

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.kaizenkanban.reminders.DueReminderScheduler
import com.example.kaizenkanban.ui.calendar.DueDateQuickPick
import com.example.kaizenkanban.ui.calendar.formatDateShort
import com.example.kaizenkanban.ui.components.DialogActionLine
import com.example.kaizenkanban.ui.components.DialogChoicePill
import com.example.kaizenkanban.ui.components.dialogPanelFill
import com.example.kaizenkanban.ui.i18n.LocalAppStrings
import com.example.kaizenkanban.ui.theme.buttonLine
import com.example.kaizenkanban.ui.theme.inProgressColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DeadlineKind { Date, Window }

enum class TaskWindowKind { Week, Month }

fun formatWhenClosedSummary(
    dueDate: Long?,
    startMinutes: Int?,
    defaultMinutes: Int,
    noneLabel: String,
    locale: Locale
): String {
    if (dueDate == null) return noneLabel
    val day = SimpleDateFormat("EEE", locale)
        .format(Date(dueDate))
        .trimEnd('.')
        .lowercase(locale)
    val clock = DueReminderScheduler.formatMinutesOfDay(startMinutes ?: defaultMinutes)
    return "$day $clock"
}

fun formatDeadlineClosedSummary(
    deadlineAt: Long?,
    window: TaskWindowKind?,
    noneLabel: String,
    weekLabel: String,
    monthLabel: String,
    locale: Locale
): String {
    val parts = buildList {
        if (deadlineAt != null) add(deadlineAt.formatDateShort(locale))
        when (window) {
            TaskWindowKind.Week -> add(weekLabel)
            TaskWindowKind.Month -> add(monthLabel)
            null -> Unit
        }
    }
    return parts.joinToString(" · ").ifBlank { noneLabel }
}

fun formatTimerClosedSummary(
    timerMinutes: Int?,
    noneLabel: String,
    valueLabel: (Int) -> String
): String {
    val minutes = timerMinutes?.takeIf { it > 0 } ?: return noneLabel
    return valueLabel(minutes)
}

fun showTaskClockPicker(
    context: Context,
    title: String,
    currentMinutes: Int?,
    defaultMinutes: Int,
    defaultLabel: String,
    pickLabel: String,
    onUseDefault: () -> Unit,
    onPicked: (Int) -> Unit
) {
    val hour = defaultMinutes / 60
    val minute = defaultMinutes % 60
    val defaultClock = DueReminderScheduler.formatClock(hour, minute)
    android.app.AlertDialog.Builder(context)
        .setTitle(title)
        .setItems(
            arrayOf("$defaultLabel ($defaultClock)", pickLabel)
        ) { _, which ->
            when (which) {
                0 -> onUseDefault()
                1 -> {
                    val initial = currentMinutes ?: defaultMinutes
                    android.app.TimePickerDialog(
                        context,
                        { _, pickedHour, pickedMinute ->
                            onPicked(pickedHour * 60 + pickedMinute)
                        },
                        initial / 60,
                        initial % 60,
                        true
                    ).show()
                }
            }
        }
        .show()
}

@Composable
fun TaskGoalToggle(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (checked) inProgressColor else dialogPanelFill())
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Flag,
            contentDescription = s.statsGoalLabel,
            modifier = Modifier.size(20.dp),
            tint = if (checked) androidx.compose.ui.graphics.Color.White
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TaskWhenPlate(
    dueDate: Long?,
    onDueDateChange: (Long?) -> Unit,
    onPickCustomDate: () -> Unit,
    startMinutes: Int?,
    reminderMinutes: Int?,
    defaultMinutes: Int,
    onStartMinutesChange: (Int?) -> Unit,
    onReminderMinutesChange: (Int?) -> Unit,
    remindBefore: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val defaultClock = DueReminderScheduler.formatMinutesOfDay(defaultMinutes)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DueDateQuickPick(
            selectedDueDate = dueDate,
            onSelect = onDueDateChange,
            onPickCustom = onPickCustomDate
        )
        if (dueDate != null) {
            DialogActionLine(
                label = s.setDueDate,
                value = startMinutes?.let { DueReminderScheduler.formatMinutesOfDay(it) } ?: defaultClock,
                onClick = {
                    showTaskClockPicker(
                        context = context,
                        title = s.setDueDate,
                        currentMinutes = startMinutes,
                        defaultMinutes = defaultMinutes,
                        defaultLabel = s.reminderUseDefault,
                        pickLabel = s.reminderPickTime,
                        onUseDefault = { onStartMinutesChange(null) },
                        onPicked = { onStartMinutesChange(it) }
                    )
                }
            )
            DialogActionLine(
                label = s.remindClockLabel,
                value = reminderMinutes?.let { DueReminderScheduler.formatMinutesOfDay(it) } ?: defaultClock,
                onClick = {
                    showTaskClockPicker(
                        context = context,
                        title = s.reminderTaskTime,
                        currentMinutes = reminderMinutes,
                        defaultMinutes = defaultMinutes,
                        defaultLabel = s.reminderUseDefault,
                        pickLabel = s.reminderPickTime,
                        onUseDefault = { onReminderMinutesChange(null) },
                        onPicked = { onReminderMinutesChange(it) }
                    )
                }
            )
            remindBefore()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskDeadlinePlate(
    deadlineAt: Long?,
    onDeadlineChange: (Long?) -> Unit,
    onPickCustomDeadline: () -> Unit,
    window: TaskWindowKind?,
    onWindowChange: (TaskWindowKind?) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    var editor by remember { mutableStateOf(DeadlineKind.Date) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DialogChoicePill(
                selected = editor == DeadlineKind.Date,
                onClick = { editor = DeadlineKind.Date },
                label = s.deadlineKindDate
            )
            DialogChoicePill(
                selected = editor == DeadlineKind.Window,
                onClick = { editor = DeadlineKind.Window },
                label = s.deadlineKindWindow
            )
        }
        when (editor) {
            DeadlineKind.Date -> DueDateQuickPick(
                selectedDueDate = deadlineAt,
                onSelect = onDeadlineChange,
                onPickCustom = onPickCustomDeadline
            )
            DeadlineKind.Window -> FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DialogChoicePill(
                    selected = window == TaskWindowKind.Week,
                    onClick = {
                        onWindowChange(if (window == TaskWindowKind.Week) null else TaskWindowKind.Week)
                    },
                    label = s.windowThisWeek
                )
                DialogChoicePill(
                    selected = window == TaskWindowKind.Month,
                    onClick = {
                        onWindowChange(if (window == TaskWindowKind.Month) null else TaskWindowKind.Month)
                    },
                    label = s.windowThisMonth
                )
            }
        }
    }
}

@Composable
fun TaskTimerPlate(
    timerMinutes: Int?,
    onTimerMinutesChange: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val minutes = timerMinutes?.takeIf { it > 0 }
    QuietStepperLine(
        label = s.timerMinutesLabel,
        valueText = minutes?.toString() ?: "—",
        onMinus = {
            val current = minutes
            if (current != null) {
                onTimerMinutesChange((current - 1).takeIf { it >= 1 })
            }
        },
        onPlus = {
            onTimerMinutesChange(
                if (minutes == null) 1 else (minutes + 1).coerceAtMost(999)
            )
        },
        minusEnabled = minutes != null,
        plusEnabled = (minutes ?: 0) < 999,
        modifier = modifier
    )
}

@Composable
private fun QuietStepButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = 1.dp,
                color = buttonLine().copy(alpha = if (enabled) 1f else 0.35f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = scheme.onSurface.copy(alpha = if (enabled) 1f else 0.35f),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun QuietStepperLine(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusEnabled: Boolean,
    plusEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuietStepButton(label = "−", enabled = minusEnabled, onClick = onMinus)
            Text(
                text = valueText,
                modifier = Modifier.widthIn(min = 40.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurface
            )
            QuietStepButton(label = "+", enabled = plusEnabled, onClick = onPlus)
        }
    }
}
