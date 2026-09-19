package com.example.kaizenkanban.ui.calendar

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kaizenkanban.ui.i18n.LocalAppStrings
import java.util.Calendar
import java.util.TimeZone

fun localNoonForDayOffset(offsetDays: Int, now: Long = System.currentTimeMillis()): Long {
    return Calendar.getInstance().apply {
        timeInMillis = startOfLocalDayMillis(now)
        add(Calendar.DAY_OF_YEAR, offsetDays)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

fun Long.isSameLocalDay(other: Long): Boolean =
    startOfLocalDayMillis(this) == startOfLocalDayMillis(other)

fun utcPickerMillisToLocalNoon(millis: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

fun localMillisToUtcPicker(millis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = millis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(Calendar.YEAR, local.get(Calendar.YEAR))
        set(Calendar.MONTH, local.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

@Composable
fun DueDateQuickPick(
    selectedDueDate: Long?,
    onSelect: (Long?) -> Unit,
    onPickCustom: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val today = localNoonForDayOffset(0)
    val tomorrow = localNoonForDayOffset(1)
    val todaySelected = selectedDueDate != null && selectedDueDate.isSameLocalDay(today)
    val tomorrowSelected = selectedDueDate != null && selectedDueDate.isSameLocalDay(tomorrow)
    val customSelected = selectedDueDate != null && !todaySelected && !tomorrowSelected

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = todaySelected,
            onClick = { onSelect(today) },
            label = {
                Text(s.dueToday, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
            }
        )
        FilterChip(
            selected = tomorrowSelected,
            onClick = { onSelect(tomorrow) },
            label = {
                Text(s.dueTomorrow, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
            }
        )
        FilterChip(
            selected = customSelected,
            onClick = onPickCustom,
            label = {
                Text(s.pickDate, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
            }
        )
        if (selectedDueDate != null) {
            FilterChip(
                selected = false,
                onClick = { onSelect(null) },
                label = {
                    Text(s.clearDueDate, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                }
            )
        }
    }
}
