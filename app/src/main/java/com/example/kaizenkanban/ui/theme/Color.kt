package com.example.kaizenkanban.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance

// Clear black, white, and one set of pure hues. No gray mixed into the color.
val ink = Color(0xFF111111)
val inkPaper = Color(0xFFFFFFFF)
val inkPaperRaised = Color(0xFFFFFFFF)
val inkPaperSunken = Color(0xFFF5F5F5)
val inkLine = Color(0xFFE5E5E5)
val inkPencil = Color(0xFFD4D4D4)

val inkNight = Color(0xFF2E2E2E)
val inkNightRaised = Color(0xFF3C3C3C)
val inkNightLine = Color(0xFF545454)
val inkNightPencil = Color(0xFF767676)
val inkNightText = Color(0xFFFAFAFA)

// Slate paper near #708090. Page is a step darker so light text holds;
// cards sit deeper, and buttons are a darker slate with a light glyph.
val inkSlate = Color(0xFF6B7C8C)
val inkSlateCard = Color(0xFF516070)
val inkSlateChip = Color(0xFF5E6E7E)
val inkSlateSunken = Color(0xFF44525F)
val inkSlateLine = Color(0xFFD5DEE6)
val inkSlatePencil = Color(0xFFC5D0D8)
val inkSlateText = Color(0xFFF4F7F9)
val inkSlateTextMuted = Color(0xFFE2E8ED)
val inkSlateButton = Color(0xFF3E4C58)
val inkSlateButtonLine = Color(0xFFD5DEE6)
val inkSlateButtonLabel = Color(0xFFF4F7F9)
val inkSlateOnBright = Color(0xFF1E2A32)
val inkSlateAccent = Color(0xFF8EE8D4)
val inkSlateWine = Color(0xFFF8D0D8)
val inkSlatePlum = Color(0xFFDDD4FF)

val inkWine = Color(0xFF800020)
val inkWineOnDark = Color(0xFFC46A80)
val inkNavy = Color(0xFF097969)
val inkNavyOnDark = Color(0xFF097969)
val inkOlive = Color(0xFF097969)
val inkOliveOnDark = Color(0xFF097969)
val inkUmber = Color(0xFF06B6D4)
val inkUmberOnDark = Color(0xFF67E8F9)
val inkPlum = Color(0xFF7C3AED)
val inkPlumOnDark = Color(0xFFC4B5FD)
val inkStone = Color(0xFF64748B)
val inkStoneOnDark = Color(0xFFCBD5E1)
val inkTeal = Color(0xFF14B8A6)
val inkTealOnDark = Color(0xFF5EEAD4)

val inkButtonFill = Color(0xFFFFFFFF)
val inkButtonFillDark = Color(0xFF3C3C3C)
val inkButtonLine = Color(0xFFA3A3A3)
val inkButtonLineDark = Color(0xFF8A8A8A)

val hubInkOnLight = listOf(inkNavy, inkPlum, inkOlive, inkUmber, inkTeal)
val hubInkOnDark = listOf(inkNavyOnDark, inkPlumOnDark, inkOliveOnDark, inkUmberOnDark, inkTealOnDark)

val inkGraphite = Color(0xFF404040)
val inkGraphiteLine = Color(0xFF737373)
val inkGraphiteOnDark = Color(0xFFE5E5E5)
val inkGraphiteLineDark = Color(0xFFA3A3A3)

@Composable
@ReadOnlyComposable
fun buttonFill(): Color = inkOnPaper(inkButtonFill, inkNightRaised, inkSlateButton)

@Composable
@ReadOnlyComposable
fun buttonLine(): Color = inkOnPaper(inkGraphiteLine, inkGraphiteLineDark, inkSlateButtonLine)

@Composable
@ReadOnlyComposable
fun buttonLabel(): Color = inkOnPaper(inkGraphite, inkGraphiteOnDark, inkSlateButtonLabel)

@Composable
@ReadOnlyComposable
fun inkOnPaper(onLight: Color, onDark: Color, onSlate: Color = onDark): Color = when {
    LocalAppThemeMode.current == AppThemeMode.SLATE -> onSlate
    MaterialTheme.colorScheme.surface.luminance() < 0.5f -> onDark
    else -> onLight
}

@Composable
@ReadOnlyComposable
fun completedMark(): Color = inkOnPaper(inkOlive, inkOliveOnDark, inkSlateAccent)

@Composable
@ReadOnlyComposable
fun inProgressMark(): Color = inkOnPaper(inkNavy, inkNavyOnDark, inkSlateAccent)

@Composable
@ReadOnlyComposable
fun inProgressBrush(): Brush = SolidColor(inProgressMark())

@Composable
@ReadOnlyComposable
fun deleteButtonColor(): Color = inkOnPaper(inkWine, inkWineOnDark, inkSlateWine)

// Light theme
val md_theme_light_primary = ink
val md_theme_light_onPrimary = inkPaperRaised
val md_theme_light_primaryContainer = inkPaperSunken
val md_theme_light_onPrimaryContainer = ink
val md_theme_light_secondary = Color(0xFF404040)
val md_theme_light_onSecondary = inkPaperRaised
val md_theme_light_secondaryContainer = inkPaperSunken
val md_theme_light_onSecondaryContainer = Color(0xFF404040)
val md_theme_light_tertiary = inkUmber
val md_theme_light_onTertiary = inkPaperRaised
val md_theme_light_background = inkPaper
val md_theme_light_onBackground = ink
val md_theme_light_surface = inkPaperRaised
val md_theme_light_onSurface = ink
val md_theme_light_surfaceVariant = inkPaperSunken
val md_theme_light_onSurfaceVariant = Color(0xFF404040)
val md_theme_light_outline = inkPencil
val md_theme_light_outlineVariant = inkLine
val md_theme_light_error = inkWine

// Dark theme
val md_theme_dark_primary = inkNightText
val md_theme_dark_onPrimary = inkNight
val md_theme_dark_primaryContainer = inkNightRaised
val md_theme_dark_onPrimaryContainer = inkNightText
val md_theme_dark_secondary = inkNightText
val md_theme_dark_onSecondary = inkNight
val md_theme_dark_secondaryContainer = Color(0xFF464646)
val md_theme_dark_onSecondaryContainer = inkNightText
val md_theme_dark_tertiary = inkUmberOnDark
val md_theme_dark_onTertiary = inkNight
val md_theme_dark_background = inkNight
val md_theme_dark_onBackground = inkNightText
val md_theme_dark_surface = inkNight
val md_theme_dark_onSurface = inkNightText
val md_theme_dark_surfaceVariant = inkNightRaised
val md_theme_dark_onSurfaceVariant = Color(0xFFE5E5E5)
val md_theme_dark_outline = inkNightPencil
val md_theme_dark_outlineVariant = inkNightLine
val md_theme_dark_error = inkWineOnDark

/** Primary action fill. Prefer theme primary on buttons that must flip in the dark theme. */
val newProjectGradient: Brush = SolidColor(ink)

/** Active-task fill behind a white glyph. Borders and labels use [inProgressMark]. */
val inProgressColor = inkNavy

/** Light control fill. Destructive actions use [deleteButtonColor] on top of it. */
val deleteButtonGradient: Brush
    @Composable
    @ReadOnlyComposable
    get() = SolidColor(buttonFill())

@Composable
@ReadOnlyComposable
fun chartProgress(): Color = inkOnPaper(inkNavy, inkNavyOnDark, inkSlateAccent)

@Composable
@ReadOnlyComposable
fun chartForecast(): Color = inkOnPaper(inkWine, inkWineOnDark, inkSlateWine)

@Composable
@ReadOnlyComposable
fun chartToward(): Color = chartProgress()

@Composable
@ReadOnlyComposable
fun chartTrivia(): Color = inkOnPaper(inkPlum, inkPlumOnDark, inkSlatePlum)

/** Flat ink for legacy status colors. Unknown values stay as stored. */
fun getStatusBrush(colorLong: Long): Brush {
    val color = when (colorLong) {
        0xFFFF2A5F -> inkWine
        0xFF9D2CFF -> inkPlum
        0xFF0095F6 -> inkNavy
        0xFFFFB020 -> inkUmber
        0xFF00D287 -> inkOlive
        0xFFAAAAAA -> inkStone
        else -> Color(colorLong)
    }
    return SolidColor(color)
}
