package com.example.kaizenkanban.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kaizenkanban.data.local.KairosPreferences
import com.example.kaizenkanban.ui.i18n.LocalAppStrings

data class EisenhowerSwatch(
    val solid: Color,
    val dark: Color,
    val light: Color
)

data class EisenhowerPaletteColors(
    val q1: EisenhowerSwatch,
    val q2: EisenhowerSwatch,
    val q3: EisenhowerSwatch,
    val q4: EisenhowerSwatch
) {
    fun swatch(quadrant: String?): EisenhowerSwatch? = when (quadrant) {
        "Q1" -> q1
        "Q2" -> q2
        "Q3" -> q3
        "Q4" -> q4
        else -> null
    }
}

enum class EisenhowerPaletteMode {
    /** Rose · violet · magenta · slate */
    BERRY,

    /** Rose · purple · gold · slate */
    SUNSET,

    /** Crimson · indigo · cyan · slate */
    OCEAN;

    companion object {
        fun fromCode(code: String?): EisenhowerPaletteMode = when (code?.lowercase()) {
            "sunset" -> SUNSET
            "ocean" -> OCEAN
            else -> BERRY
        }
    }

    val code: String
        get() = when (this) {
            BERRY -> "berry"
            SUNSET -> "sunset"
            OCEAN -> "ocean"
        }

    fun colors(): EisenhowerPaletteColors = when (this) {
        BERRY -> EisenhowerPaletteColors(
            q1 = EisenhowerSwatch(Color(0xFFE11D48), Color(0xFFBE123C), Color(0xFFFB7185)),
            q2 = EisenhowerSwatch(Color(0xFF7C3AED), Color(0xFF6D28D9), Color(0xFFA78BFA)),
            q3 = EisenhowerSwatch(Color(0xFFC13584), Color(0xFF833AB4), Color(0xFFE1306C)),
            q4 = EisenhowerSwatch(Color(0xFF64748B), Color(0xFF475569), Color(0xFF94A3B8))
        )
        SUNSET -> EisenhowerPaletteColors(
            q1 = EisenhowerSwatch(Color(0xFFFD1D1D), Color(0xFFE1306C), Color(0xFFFF6B81)),
            q2 = EisenhowerSwatch(Color(0xFF833AB4), Color(0xFF5B2C8A), Color(0xFFC13584)),
            q3 = EisenhowerSwatch(Color(0xFFFCAF45), Color(0xFFF77737), Color(0xFFFFD166)),
            q4 = EisenhowerSwatch(Color(0xFF6B7280), Color(0xFF4B5563), Color(0xFF9CA3AF))
        )
        OCEAN -> EisenhowerPaletteColors(
            q1 = EisenhowerSwatch(Color(0xFFE11D48), Color(0xFFBE123C), Color(0xFFFB7185)),
            q2 = EisenhowerSwatch(Color(0xFF405DE6), Color(0xFF5851DB), Color(0xFF7B8CFF)),
            q3 = EisenhowerSwatch(Color(0xFF0095F6), Color(0xFF0077CC), Color(0xFF38BDF8)),
            q4 = EisenhowerSwatch(Color(0xFF64748B), Color(0xFF475569), Color(0xFF94A3B8))
        )
    }
}

class EisenhowerPaletteController(
    initial: EisenhowerPaletteMode,
    private val persist: (EisenhowerPaletteMode) -> Unit
) {
    var mode by mutableStateOf(initial)
        private set

    fun select(value: EisenhowerPaletteMode) {
        if (mode == value) return
        mode = value
        persist(value)
    }
}

val LocalEisenhowerPaletteController = staticCompositionLocalOf<EisenhowerPaletteController> {
    error("EisenhowerPaletteController is not provided")
}

val LocalEisenhowerPalette = staticCompositionLocalOf { EisenhowerPaletteMode.BERRY }

@Composable
fun ProvideEisenhowerPalette(
    prefs: KairosPreferences,
    content: @Composable () -> Unit
) {
    val controller = remember {
        EisenhowerPaletteController(EisenhowerPaletteMode.fromCode(prefs.eisenhowerPalette)) {
            prefs.eisenhowerPalette = it.code
        }
    }
    val mode = controller.mode
    CompositionLocalProvider(
        LocalEisenhowerPaletteController provides controller,
        LocalEisenhowerPalette provides mode,
        content = content
    )
}

@Composable
fun EisenhowerPaletteToggle(modifier: Modifier = Modifier) {
    val controller = LocalEisenhowerPaletteController.current
    val selected = LocalEisenhowerPalette.current
    val s = LocalAppStrings.current
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EisenhowerPaletteMode.entries.forEach { mode ->
            val label = when (mode) {
                EisenhowerPaletteMode.BERRY -> s.eisenhowerPaletteBerry
                EisenhowerPaletteMode.SUNSET -> s.eisenhowerPaletteSunset
                EisenhowerPaletteMode.OCEAN -> s.eisenhowerPaletteOcean
            }
            PaletteOptionChip(
                label = label,
                colors = mode.colors(),
                selected = selected == mode,
                onClick = { controller.select(mode) }
            )
        }
    }
}

@Composable
private fun PaletteOptionChip(
    label: String,
    colors: EisenhowerPaletteColors,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .clip(shape)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(colors.q1.solid, colors.q2.solid, colors.q3.solid, colors.q4.solid).forEach { color ->
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

@Composable
fun eisenhowerSolid(quadrant: String?): Color {
    val swatch = LocalEisenhowerPalette.current.colors().swatch(quadrant)
    return swatch?.solid ?: Color(0xFF8E8E8E)
}

@Composable
fun eisenhowerGradient(quadrant: String?): Brush {
    val swatch = LocalEisenhowerPalette.current.colors().swatch(quadrant)
    return if (swatch == null) {
        Brush.linearGradient(listOf(Color(0xFF8E8E8E), Color(0xFFB8B8B8)))
    } else {
        Brush.linearGradient(listOf(swatch.dark, swatch.solid, swatch.light))
    }
}

@Composable
fun eisenhowerOnColor(@Suppress("UNUSED_PARAMETER") quadrant: String?): Color = Color.White
