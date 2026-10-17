package com.example.kaizenkanban.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kaizenkanban.ui.components.dialogSheetFill

@Composable
fun TaskTitleThoughtBubble(
    title: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val fill = dialogSheetFill()
    val darkUi = scheme.background.luminance() < 0.5f
    val outline = if (darkUi) {
        Color.White.copy(alpha = 0.72f)
    } else {
        Color(0xFF1A1A1A).copy(alpha = 0.62f)
    }
    val highlight = if (darkUi) {
        Color.White.copy(alpha = 0.22f)
    } else {
        Color.White.copy(alpha = 0.55f)
    }
    val shadow = Color.Black.copy(alpha = if (darkUi) 0.45f else 0.16f)
    val maxTextHeight = (LocalConfiguration.current.screenHeightDp * 0.34f).dp
    val density = LocalDensity.current
    val layoutDir = LocalLayoutDirection.current
    val tailReserve = 46.dp

    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = with(density) { 2.5.dp.toPx() }
            val inset = stroke / 2f
            val bodyBottom = size.height - with(density) { tailReserve.toPx() }
            val bottomR = with(density) { 28.dp.toPx() }
            val ltr = layoutDir == LayoutDirection.Ltr
            val body = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(
                            left = inset,
                            top = 0f,
                            right = size.width - inset,
                            bottom = bodyBottom
                        ),
                        topLeft = CornerRadius.Zero,
                        topRight = CornerRadius.Zero,
                        bottomLeft = CornerRadius(bottomR),
                        bottomRight = CornerRadius(bottomR)
                    )
                )
            }
            val r1 = with(density) { 12.dp.toPx() }
            val r2 = with(density) { 8.dp.toPx() }
            val r3 = with(density) { 4.8.dp.toPx() }
            val startX = if (ltr) {
                inset + with(density) { 34.dp.toPx() }
            } else {
                size.width - inset - with(density) { 34.dp.toPx() }
            }
            val dx = with(density) { 15.dp.toPx() } * if (ltr) -1f else 1f
            val c1 = Offset(
                x = startX,
                y = bodyBottom - with(density) { 4.dp.toPx() }
            )
            val c2 = Offset(
                x = c1.x + dx,
                y = c1.y + with(density) { 16.dp.toPx() }
            )
            val c3 = Offset(
                x = c2.x + dx * 0.9f,
                y = c2.y + with(density) { 14.dp.toPx() }
            )
            fun oval(center: Offset, radius: Float) = Path().apply {
                addOval(
                    Rect(
                        left = center.x - radius,
                        top = center.y - radius,
                        right = center.x + radius,
                        bottom = center.y + radius
                    )
                )
            }
            fun union(a: Path, b: Path): Path {
                val out = Path()
                out.op(a, b, PathOperation.Union)
                return out
            }
            val united = union(union(union(body, oval(c1, r1)), oval(c2, r2)), oval(c3, r3))

            val shadowPath = Path().apply { addPath(united) }
            shadowPath.translate(Offset(0f, with(density) { 3.dp.toPx() }))
            drawPath(shadowPath, shadow, style = Fill)
            drawPath(united, fill, style = Fill)
            drawPath(
                united,
                highlight,
                style = Stroke(
                    width = with(density) { 3.2.dp.toPx() },
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            drawPath(
                united,
                outline,
                style = Stroke(
                    width = stroke,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium
            ),
            color = scheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = tailReserve)
                .heightIn(max = maxTextHeight)
                .verticalScroll(rememberScrollState())
        )
    }
}
