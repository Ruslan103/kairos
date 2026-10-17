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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
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

private val plainSwatch = EisenhowerSwatch(Color.Transparent, Color.Transparent, Color.Transparent)

enum class EisenhowerPaletteMode {
    /** Neutral frames, no quadrant hue. Default. */
    PLAIN,

    /** Red · violet · pink · slate */
    BERRY,

    /** Red · violet · cyan · slate */
    SUNSET,

    /** Red · emerald · teal · slate */
    OCEAN,

    /** Red · emerald · cyan · slate */
    INK,

    /** Red · black · gray · light gray */
    PEN,

    /** Red · green · emerald · slate */
    GARDEN;

    companion object {
        fun fromCode(code: String?): EisenhowerPaletteMode = when (code?.lowercase()) {
            "berry" -> BERRY
            "sunset" -> SUNSET
            "ocean" -> OCEAN
            "ink" -> INK
            "pen" -> PEN
            "garden" -> GARDEN
            else -> PLAIN
        }
    }

    val code: String
        get() = when (this) {
            PLAIN -> "plain"
            BERRY -> "berry"
            SUNSET -> "sunset"
            OCEAN -> "ocean"
            INK -> "ink"
            PEN -> "pen"
            GARDEN -> "garden"
        }

    fun colors(): EisenhowerPaletteColors = when (this) {
        PLAIN -> EisenhowerPaletteColors(
            q1 = plainSwatch,
            q2 = plainSwatch,
            q3 = plainSwatch,
            q4 = plainSwatch
        )
        BERRY -> EisenhowerPaletteColors(
            q1 = EisenhowerSwatch(inkWine, Color(0xFF5C0017), inkWineOnDark),
            q2 = EisenhowerSwatch(inkPlum, Color(0xFF5B21B6), inkPlumOnDark),
            q3 = EisenhowerSwatch(Color(0xFFDB2777), Color(0xFFBE185D), Color(0xFFF9A8D4)),
            q4 = EisenhowerSwatch(inkStone, Color(0xFF334155), inkStoneOnDark)
        )
        SUNSET -> EisenhowerPaletteColors(
            q1 = EisenhowerSwatch(inkWine, Color(0xFF5C0017), inkWineOnDark),
            q2 = EisenhowerSwatch(Color(0xFF8B5CF6), Color(0xFF7C3AED), Color(0xFFDDD6FE)),
            q3 = EisenhowerSwatch(inkUmber, Color(0xFF155E75), inkUmberOnDark),
            q4 = EisenhowerSwatch(inkStone, Color(0xFF334155), inkStoneOnDark)
        )
        OCEAN -> EisenhowerPaletteColors(
            q1 = EisenhowerSwatch(inkWine, Color(0xFF5C0017), inkWineOnDark),
            q2 = EisenhowerSwatch(inkNavy, Color(0xFF097969), inkNavyOnDark),
            q3 = EisenhowerSwatch(inkTeal, Color(0xFF115E59), inkTealOnDark),
            q4 = EisenhowerSwatch(inkStone, Color(0xFF334155), inkStoneOnDark)
        )
        INK -> EisenhowerPaletteColors(
            q1 = EisenhowerSwatch(inkWine, Color(0xFF5C0017), inkWineOnDark),
            q2 = EisenhowerSwatch(inkNavy, Color(0xFF097969), inkNavyOnDark),
            q3 = EisenhowerSwatch(inkUmber, Color(0xFF155E75), inkUmberOnDark),
            q4 = EisenhowerSwatch(inkStone, Color(0xFF334155), inkStoneOnDark)
        )
        PEN -> EisenhowerPaletteColors(
            q1 = EisenhowerSwatch(inkWine, Color(0xFF5C0017), inkWineOnDark),
            q2 = EisenhowerSwatch(ink, inkNight, inkNightText),
            q3 = EisenhowerSwatch(Color(0xFF525252), Color(0xFF404040), Color(0xFFD4D4D4)),
            q4 = EisenhowerSwatch(Color(0xFFA3A3A3), Color(0xFF737373), Color(0xFFE5E5E5))
        )
        GARDEN -> EisenhowerPaletteColors(
            q1 = EisenhowerSwatch(inkWine, Color(0xFF5C0017), inkWineOnDark),
            q2 = EisenhowerSwatch(inkOlive, Color(0xFF097969), inkOliveOnDark),
            q3 = EisenhowerSwatch(inkNavy, Color(0xFF097969), inkNavyOnDark),
            q4 = EisenhowerSwatch(inkStone, Color(0xFF334155), inkStoneOnDark)
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

val LocalEisenhowerPalette = staticCompositionLocalOf { EisenhowerPaletteMode.PLAIN }

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
                EisenhowerPaletteMode.PLAIN -> s.eisenhowerPalettePlain
                EisenhowerPaletteMode.BERRY -> s.eisenhowerPaletteBerry
                EisenhowerPaletteMode.SUNSET -> s.eisenhowerPaletteSunset
                EisenhowerPaletteMode.OCEAN -> s.eisenhowerPaletteOcean
                EisenhowerPaletteMode.INK -> s.eisenhowerPaletteInk
                EisenhowerPaletteMode.PEN -> s.eisenhowerPalettePen
                EisenhowerPaletteMode.GARDEN -> s.eisenhowerPaletteGarden
            }
            PaletteOptionChip(
                label = label,
                colors = mode.colors(),
                plain = mode == EisenhowerPaletteMode.PLAIN,
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
    plain: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
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
        val onDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (plain) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .border(1.dp, buttonLine(), CircleShape)
                    )
                }
            } else {
                listOf(colors.q1, colors.q2, colors.q3, colors.q4).forEach { swatch ->
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(slateReadable(if (onDark) swatch.light else swatch.solid))
                    )
                }
            }
        }
    }
}

@Composable
fun eisenhowerPlain(): Boolean =
    LocalEisenhowerPalette.current == EisenhowerPaletteMode.PLAIN

/** Quadrant frame. Plain mode keeps a neutral line instead of a hue. */
@Composable
fun eisenhowerFrameColor(selected: Boolean): Color {
    if (eisenhowerPlain()) return buttonLine().copy(alpha = if (selected) 1f else 0.72f)
    return if (selected) Color.White else Color.White.copy(alpha = 0.28f)
}

@Composable
fun eisenhowerSolid(quadrant: String?): Color {
    if (eisenhowerPlain()) return buttonLine()
    val swatch = LocalEisenhowerPalette.current.colors().swatch(quadrant)
    return if (swatch == null) {
        inkOnPaper(inkStone, inkStoneOnDark)
    } else {
        slateReadable(inkOnPaper(swatch.solid, swatch.light))
    }
}

/** Pale wash of the quadrant hue, so the card stays light instead of a heavy color block. */
@Composable
fun eisenhowerGradient(quadrant: String?): Brush {
    if (eisenhowerPlain()) return SolidColor(buttonFill())
    val swatch = LocalEisenhowerPalette.current.colors().swatch(quadrant)
    val slate = LocalAppThemeMode.current == AppThemeMode.SLATE
    val solid = if (slate) (swatch?.light ?: inkStoneOnDark) else (swatch?.solid ?: inkStone)
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val base = if (dark) MaterialTheme.colorScheme.surface else Color.White
    val amount = when {
        slate -> 0.48f
        dark -> 0.34f
        else -> 0.14f
    }
    return SolidColor(lerp(base, solid, amount))
}

@Composable
private fun slateReadable(color: Color): Color {
    if (LocalAppThemeMode.current != AppThemeMode.SLATE) return color
    val background = MaterialTheme.colorScheme.surface
    val lighter = maxOf(color.luminance(), background.luminance())
    val darker = minOf(color.luminance(), background.luminance())
    val ratio = (lighter + 0.05f) / (darker + 0.05f)
    return if (ratio >= 3f) color else lerp(color, Color.White, 0.62f)
}

@Composable
fun eisenhowerOnColor(quadrant: String?): Color {
    if (eisenhowerPlain()) return buttonLabel()
    val swatch = LocalEisenhowerPalette.current.colors().swatch(quadrant)
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return if (dark) Color.White else (swatch?.solid ?: ink)
}
