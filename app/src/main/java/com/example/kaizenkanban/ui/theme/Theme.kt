package com.example.kaizenkanban.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kaizenkanban.R
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    outlineVariant = md_theme_light_outlineVariant,
    error = md_theme_light_error
)

private val DarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    outlineVariant = md_theme_dark_outlineVariant,
    error = md_theme_dark_error
)

private val SlateColors = darkColorScheme(
    primary = inkSlateText,
    onPrimary = inkSlateOnBright,
    primaryContainer = inkSlateSunken,
    onPrimaryContainer = inkSlateText,
    secondary = inkSlateTextMuted,
    onSecondary = inkSlateOnBright,
    secondaryContainer = inkSlateSunken,
    onSecondaryContainer = inkSlateText,
    tertiary = inkSlateAccent,
    onTertiary = inkSlateOnBright,
    background = inkSlate,
    onBackground = inkSlateText,
    surface = inkSlateCard,
    onSurface = inkSlateText,
    surfaceVariant = inkSlateChip,
    onSurfaceVariant = inkSlateTextMuted,
    outline = inkSlateLine,
    outlineVariant = inkSlatePencil,
    error = inkSlateWine,
    surfaceContainerLowest = inkSlateSunken,
    surfaceContainerLow = inkSlateCard,
    surfaceContainer = inkSlateChip,
    surfaceContainerHigh = inkSlate,
    surfaceContainerHighest = inkSlate,
    surfaceBright = inkSlate,
    surfaceDim = inkSlateSunken
)

private val AppCorner = RoundedCornerShape(10.dp)

private val AppShapes = Shapes(
    extraSmall = AppCorner,
    small = AppCorner,
    medium = AppCorner,
    large = AppCorner,
    extraLarge = AppCorner
)

private val Literata = FontFamily(
    Font(R.font.literata, FontWeight.Normal),
    Font(R.font.literata, FontWeight.Medium),
    Font(R.font.literata, FontWeight.SemiBold),
    Font(R.font.literata, FontWeight.Bold)
)

/** Single bold cut so group titles actually pick the variable-font weight. */
@OptIn(ExperimentalTextApi::class)
internal val LiterataBold = FontFamily(
    Font(
        R.font.literata,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Bold.weight)
        )
    )
)

private fun androidx.compose.ui.text.TextStyle.asTitle() = copy(
    fontFamily = Literata,
    fontWeight = FontWeight.Medium
)

private fun androidx.compose.ui.text.TextStyle.asUi() = copy(fontFamily = Literata)

private val AppTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.asTitle(),
        displayMedium = base.displayMedium.asTitle(),
        displaySmall = base.displaySmall.asTitle(),
        headlineLarge = base.headlineLarge.asTitle(),
        headlineMedium = base.headlineMedium.asTitle(),
        headlineSmall = base.headlineSmall.asTitle(),
        titleLarge = base.titleLarge.asTitle(),
        titleMedium = base.titleMedium.asTitle(),
        titleSmall = base.titleSmall.asUi(),
        bodyLarge = base.bodyLarge.asUi(),
        bodyMedium = base.bodyMedium.asUi(),
        bodySmall = base.bodySmall.asUi(),
        labelLarge = base.labelLarge.asUi(),
        labelMedium = base.labelMedium.asUi(),
        labelSmall = base.labelSmall.asUi()
    )
}

@Composable
fun KaizenKanbanTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = resolveDarkTheme(themeMode)
    val colorScheme = when {
        themeMode == AppThemeMode.SLATE -> SlateColors
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bars = colorScheme.background.toArgb()
            val lightBars = colorScheme.background.luminance() > 0.5f
            window.statusBarColor = bars
            window.navigationBarColor = bars
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = lightBars
            controller.isAppearanceLightNavigationBars = lightBars
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

@Composable
fun resolveDarkTheme(mode: AppThemeMode): Boolean = when (mode) {
    AppThemeMode.LIGHT, AppThemeMode.SLATE -> false
    AppThemeMode.DARK -> true
    AppThemeMode.SYSTEM -> isSystemInDarkTheme()
}