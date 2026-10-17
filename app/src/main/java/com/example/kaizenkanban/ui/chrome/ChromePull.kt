package com.example.kaizenkanban.ui.chrome

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/** Share of the chrome height a pull must reach before it locks open. */
internal const val ChromeOpenCommitFraction = 0.97f

/** Upward hide moves the chrome this many times faster than the finger. */
internal const val ChromeHideGain = 2.6f

/** Board only: a short upward swipe hides the header. The matrix keeps [ChromeHideGain]. */
internal const val BoardChromeHideGain = 14f

/** Snap-back when a pull is released before the chrome locks open. */
internal val ChromePullSettleSpring: SpringSpec<Float> =
    spring(dampingRatio = 0.86f, stiffness = 420f)

/**
 * How much of the finger travel a hidden chrome follows.
 * About 0.20 at rest, about 0.12 fully extended.
 */
internal fun chromeOpenPullGain(extension: Float): Float {
    val t = extension.coerceIn(0f, 1f)
    return 1f / (5.0f + 3.6f * t)
}

/**
 * Board header only. About 0.09 at rest, about 0.06 fully extended —
 * opening takes a harder taut pull. The matrix keeps [chromeOpenPullGain].
 */
internal fun boardChromeOpenPullGain(extension: Float): Float {
    val t = extension.coerceIn(0f, 1f)
    return 1f / (11.0f + 6.0f * t)
}

/** Pull-down reveal. The chrome lags the finger and tightens as it extends. */
internal fun chromeOpenPullPx(
    currentPx: Float,
    dy: Float,
    span: Float,
    gain: (Float) -> Float = ::chromeOpenPullGain,
): Float {
    val safe = span.coerceAtLeast(1f)
    val extension = (currentPx / safe).coerceIn(0f, 1f)
    val damped = dy * gain(extension)
    return (currentPx + damped).coerceIn(0f, safe * 1.15f)
}

internal fun collapseChromePull(
    currentPx: Float,
    dy: Float,
    span: Float,
    gain: Float = ChromeHideGain,
): Float = (currentPx + dy * gain).coerceIn(0f, span)

internal fun chromePullLocksOpen(pullPx: Float, span: Float): Boolean =
    pullPx >= span.coerceAtLeast(1f) * ChromeOpenCommitFraction
