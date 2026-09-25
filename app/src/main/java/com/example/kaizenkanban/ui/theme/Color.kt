package com.example.kaizenkanban.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Modern Clean aesthetic
// Light Theme
val md_theme_light_primary = Color(0xFF000000)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFF2F2F2)
val md_theme_light_onPrimaryContainer = Color(0xFF000000)
val md_theme_light_secondary = Color(0xFF262626)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFEFEFEF)
val md_theme_light_onSecondaryContainer = Color(0xFF262626)
val md_theme_light_background = Color(0xFFFFFFFF)
val md_theme_light_onBackground = Color(0xFF000000)
val md_theme_light_surface = Color(0xFFFFFFFF)
val md_theme_light_onSurface = Color(0xFF000000)
val md_theme_light_surfaceVariant = Color(0xFFFAFAFA)
val md_theme_light_onSurfaceVariant = Color(0xFF262626)
val md_theme_light_outlineVariant = Color(0xFFDBDBDB)
val md_theme_light_error = Color(0xFFED4956)

// Dark Theme
val md_theme_dark_primary = Color(0xFFFFFFFF)
val md_theme_dark_onPrimary = Color(0xFF000000)
val md_theme_dark_primaryContainer = Color(0xFF262626)
val md_theme_dark_onPrimaryContainer = Color(0xFFFFFFFF)
val md_theme_dark_secondary = Color(0xFFF5F5F5)
val md_theme_dark_onSecondary = Color(0xFF000000)
val md_theme_dark_secondaryContainer = Color(0xFF262626)
val md_theme_dark_onSecondaryContainer = Color(0xFFF5F5F5)
val md_theme_dark_background = Color(0xFF000000)
val md_theme_dark_onBackground = Color(0xFFF5F5F5)
val md_theme_dark_surface = Color(0xFF000000)
val md_theme_dark_onSurface = Color(0xFFF5F5F5)
val md_theme_dark_surfaceVariant = Color(0xFF121212)
val md_theme_dark_onSurfaceVariant = Color(0xFFF5F5F5)
val md_theme_dark_outlineVariant = Color(0xFF363636)
val md_theme_dark_error = Color(0xFFED4956)

// Button Gradient with modern sleek look
val newProjectGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF3B82F6),
        Color(0xFF8B5CF6)
    )
)

// Vibrant Electric Blue to Purple gradient for active in-progress task highlighting
val inProgressColor = Color(0xFF2563EB)
val inProgressGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF3B82F6), // Electric Blue
        Color(0xFF8B5CF6)  // Vibrant Purple
    )
)

// Vibrant berry-to-sunset gradient for action/delete buttons
val deleteButtonGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF833AB4), // Purple
        Color(0xFFC13584), // Magenta
        Color(0xFFE1306C), // Vibrant Rose
        Color(0xFFFD1D1D), // Crimson
        Color(0xFFF77737)  // Sunset Orange
    )
)

val deleteButtonColor = Color(0xFFE1306C)

/** Line/bar accents for stats charts — same family as action gradients. */
val chartProgress = Color(0xFF0095F6)
val chartForecast = Color(0xFFE1306C)
val chartToward = Color(0xFF0095F6)
val chartTrivia = Color(0xFF8B5CF6)

val chartProgressBrush = Brush.horizontalGradient(
    colors = listOf(Color(0xFF0095F6), Color(0xFF833AB4))
)
val chartForecastBrush = Brush.horizontalGradient(
    colors = listOf(Color(0xFFE1306C), Color(0xFFF77737))
)
val chartTowardBrush = Brush.verticalGradient(
    colors = listOf(Color(0xFF38BDF8), Color(0xFF0095F6))
)
val chartTriviaBrush = Brush.verticalGradient(
    colors = listOf(Color(0xFFC084FC), Color(0xFF8B5CF6))
)

// Helper to generate a smooth, vibrant gradient for each status
fun getStatusBrush(colorLong: Long): Brush {
    val base = Color(colorLong)
    val color1 = when (colorLong) {
        0xFFFF2A5F -> Color(0xFFFF2A5F)
        0xFF9D2CFF -> Color(0xFF9D2CFF)
        0xFF0095F6 -> Color(0xFF0095F6)
        0xFFFFB020 -> Color(0xFFFFB020)
        0xFF00D287 -> Color(0xFF00D287)
        0xFFAAAAAA -> Color(0xFF78909C)
        else -> base
    }
    val color2 = when (colorLong) {
        0xFFFF2A5F -> Color(0xFFFF7597)
        0xFF9D2CFF -> Color(0xFFD47AFF)
        0xFF0095F6 -> Color(0xFF38BDF8)
        0xFFFFB020 -> Color(0xFFFFD166)
        0xFF00D287 -> Color(0xFF4ADE80)
        0xFFAAAAAA -> Color(0xFFB0BEC5)
        else -> Color(
            red = (base.red * 0.7f + 0.3f).coerceIn(0f, 1f),
            green = (base.green * 0.7f + 0.3f).coerceIn(0f, 1f),
            blue = (base.blue * 0.7f + 0.3f).coerceIn(0f, 1f),
            alpha = base.alpha
        )
    }
    return Brush.linearGradient(listOf(color1, color2))
}