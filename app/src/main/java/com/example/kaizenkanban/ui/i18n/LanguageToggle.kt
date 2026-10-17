package com.example.kaizenkanban.ui.i18n

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kaizenkanban.ui.theme.buttonFill
import com.example.kaizenkanban.ui.theme.buttonLabel
import com.example.kaizenkanban.ui.theme.buttonLine

@Composable
fun LanguageToggle(modifier: Modifier = Modifier) {
    val controller = LocalLanguageController.current
    val language = LocalAppLanguage.current
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), shape)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LanguageChip(
            label = "EN",
            selected = language == AppLanguage.EN,
            onClick = { controller.select(AppLanguage.EN) }
        )
        LanguageChip(
            label = "RU",
            selected = language == AppLanguage.RU,
            onClick = { controller.select(AppLanguage.RU) }
        )
    }
}

@Composable
private fun LanguageChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = if (selected) buttonLabel() else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) buttonFill() else MaterialTheme.colorScheme.surface.copy(alpha = 0f))
            .then(if (selected) Modifier.border(1.dp, buttonLine(), shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
