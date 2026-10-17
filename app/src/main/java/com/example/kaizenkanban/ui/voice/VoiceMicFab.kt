package com.example.kaizenkanban.ui.voice

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.example.kaizenkanban.ui.theme.buttonFill
import com.example.kaizenkanban.ui.theme.buttonLabel
import com.example.kaizenkanban.ui.theme.buttonLine
import com.example.kaizenkanban.ui.theme.inkWine

@Composable
fun VoiceMicFab(
    listening: Boolean,
    busy: Boolean = false,
    online: Boolean = true,
    onClick: () -> Unit,
    contentDescription: String
) {
    val pulse = rememberInfiniteTransition(label = "voiceMicPulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (listening) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voiceMicScale"
    )
    val ringAlpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = if (listening) 0.9f else 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voiceMicRing"
    )
    val dimmed = !online && !listening

    Box(
        modifier = Modifier
            .size(56.dp)
            .alpha(if (dimmed) 0.42f else 1f)
            .scale(if (listening) scale else 1f)
            .shadow(if (listening) 6.dp else if (dimmed) 2.dp else 4.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(if (listening) inkWine.copy(alpha = 0.14f) else buttonFill())
            .border(
                width = if (listening) 2.dp else 1.dp,
                color = if (listening) {
                    inkWine.copy(alpha = ringAlpha)
                } else {
                    buttonLine()
                },
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(enabled = !busy, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Mic,
            contentDescription = contentDescription,
            tint = if (listening) inkWine else buttonLabel(),
            modifier = Modifier.size(26.dp)
        )
    }
}
