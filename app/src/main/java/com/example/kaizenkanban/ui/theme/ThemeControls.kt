package com.example.kaizenkanban.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kaizenkanban.data.local.KairosPreferences
import com.example.kaizenkanban.ui.i18n.LocalAppStrings

class ThemeController(initial: AppThemeMode, private val persist: (AppThemeMode) -> Unit) {
    var mode by mutableStateOf(initial)
        private set

    fun select(value: AppThemeMode) {
        if (mode == value) return
        mode = value
        persist(value)
    }
}

val LocalThemeController = staticCompositionLocalOf<ThemeController> {
    error("ThemeController is not provided")
}

val LocalAppThemeMode = staticCompositionLocalOf { AppThemeMode.SYSTEM }

@Composable
fun ProvideAppTheme(
    prefs: KairosPreferences,
    content: @Composable () -> Unit
) {
    val controller = remember {
        ThemeController(AppThemeMode.fromCode(prefs.themeMode)) { prefs.themeMode = it.code }
    }
    CompositionLocalProvider(
        LocalThemeController provides controller,
        LocalAppThemeMode provides controller.mode,
        content = content
    )
}

@Composable
fun ThemeToggle(modifier: Modifier = Modifier) {
    val controller = LocalThemeController.current
    val mode = LocalAppThemeMode.current
    val s = LocalAppStrings.current
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), shape)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ThemeChip("A", s.themeSystem, mode == AppThemeMode.SYSTEM) { controller.select(AppThemeMode.SYSTEM) }
        ThemeChip("L", s.themeLight, mode == AppThemeMode.LIGHT) { controller.select(AppThemeMode.LIGHT) }
        ThemeChip("D", s.themeDark, mode == AppThemeMode.DARK) { controller.select(AppThemeMode.DARK) }
    }
}

@Composable
private fun ThemeChip(
    short: String,
    @Suppress("UNUSED_PARAMETER") label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(6.dp)
    Text(
        text = short,
        fontSize = 11.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(shape)
            .then(
                if (selected) Modifier.background(MaterialTheme.colorScheme.primary)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
