package com.example.kaizenkanban.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow

/**
 * Dialog / info-window labels: wrap onto up to [maxLines] first; if still clipped,
 * switch to a single-line horizontal marquee so nothing is truncated with ellipsis.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialogMarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    textDecoration: TextDecoration? = null,
    maxLines: Int = 2,
    fillMaxWidth: Boolean = true
) {
    var useMarquee by remember(text, maxLines) { mutableStateOf(maxLines <= 1) }
    val base = if (fillMaxWidth) modifier.fillMaxWidth() else modifier

    if (useMarquee) {
        Text(
            text = text,
            style = style,
            color = color,
            fontWeight = fontWeight,
            textAlign = textAlign,
            textDecoration = textDecoration,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = base.basicMarquee(
                iterations = Int.MAX_VALUE,
                initialDelayMillis = 900,
                delayMillis = 1400
            )
        )
    } else {
        Text(
            text = text,
            style = style,
            color = color,
            fontWeight = fontWeight,
            textAlign = textAlign,
            textDecoration = textDecoration,
            maxLines = maxLines.coerceAtLeast(1),
            softWrap = true,
            overflow = TextOverflow.Clip,
            onTextLayout = { result ->
                if (result.hasVisualOverflow) useMarquee = true
            },
            modifier = base
        )
    }
}
