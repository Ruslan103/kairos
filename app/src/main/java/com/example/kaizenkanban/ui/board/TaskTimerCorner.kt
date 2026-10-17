package com.example.kaizenkanban.ui.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.domain.reminders.TaskTimerPlanner
import com.example.kaizenkanban.reminders.TaskTimerScheduler
import com.example.kaizenkanban.ui.components.dialogSheetFill
import com.example.kaizenkanban.ui.i18n.LocalAppStrings
import com.example.kaizenkanban.ui.theme.buttonLine
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

@Composable
fun TaskTimerCorner(
    tasks: List<Task>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val revision = TaskTimerScheduler.revision
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val faces = remember(tasks, revision, now) {
        tasks.mapNotNull { task ->
            if (!task.showsTimerAction) return@mapNotNull null
            val paused = TaskTimerScheduler.isPaused(context, task.id)
            val running = TaskTimerScheduler.isRunning(context, task.id, now)
            if (!paused && !running) return@mapNotNull null
            val left = TaskTimerScheduler.remainingMillis(context, task.id, now)
            if (!paused && left <= 0L) null else TimerFace(task, left, paused)
        }
    }
    val anyRunning = faces.any { !it.paused }
    LaunchedEffect(revision, anyRunning) {
        if (!anyRunning) return@LaunchedEffect
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    if (faces.isEmpty()) return
    val density = LocalDensity.current
    val navBottom = WindowInsets.navigationBars.getBottom(density)
    Box(modifier) {
        faces.forEachIndexed { index, face ->
            val rest = with(density) {
                Offset(
                    x = 12.dp.toPx(),
                    y = -(16.dp.toPx() + navBottom + index * 118.dp.toPx())
                )
            }
            key(face.task.id) {
                TimerClock(
                    modifier = Modifier.align(Alignment.BottomStart),
                    rest = rest,
                    remainMillis = face.remainMillis,
                    paused = face.paused,
                    onClick = {
                        val at = System.currentTimeMillis()
                        if (face.paused) {
                            TaskTimerScheduler.start(context, face.task, at)
                        } else {
                            TaskTimerScheduler.pause(context, face.task.id, at)
                        }
                        now = at
                    },
                    onDismiss = { TaskTimerScheduler.cancel(context, face.task.id) }
                )
            }
        }
    }
}

private data class TimerFace(
    val task: Task,
    val remainMillis: Long,
    val paused: Boolean
)

@Composable
private fun TimerClock(
    modifier: Modifier,
    rest: Offset,
    remainMillis: Long,
    paused: Boolean,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    val scheme = MaterialTheme.colorScheme
    val ink = scheme.onSurface
    val quiet = scheme.onSurfaceVariant
    val clock = TaskTimerPlanner.remainClock(remainMillis)
    val hand = if (paused) quiet else ink
    var offset by remember { mutableStateOf(Offset.Zero) }
    var dismissing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentClick = rememberUpdatedState(onClick)
    val currentDismiss = rememberUpdatedState(onDismiss)
    val place = rest
    Column(
        modifier = modifier
            .offset {
                IntOffset(
                    (place.x + offset.x).roundToInt(),
                    (place.y + offset.y).roundToInt()
                )
            }
            .pointerInput(Unit) {
                val slop = viewConfiguration.touchSlop
                val dismissTravel = 48.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (dismissing) return@awaitEachGesture
                    val tracker = VelocityTracker()
                    tracker.addPosition(down.uptimeMillis, down.position)
                    var pastSlop = false
                    var dragged = Offset.Zero
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            tracker.addPosition(change.uptimeMillis, change.position)
                            if (!pastSlop) {
                                currentClick.value()
                            } else {
                                val velocity = tracker.calculateVelocity()
                                val speed = hypot(velocity.x, velocity.y)
                                val travelled = hypot(dragged.x, dragged.y)
                                if (speed >= DISMISS_SPEED_PX_PER_SEC && travelled >= dismissTravel) {
                                    dismissing = true
                                    val dir = Offset(velocity.x / speed, velocity.y / speed)
                                    val target = offset + dir * DISMISS_EXIT_PX
                                    scope.launch {
                                        val anim = Animatable(offset, Offset.VectorConverter)
                                        anim.animateTo(target, tween(durationMillis = 160)) {
                                            offset = value
                                        }
                                        currentDismiss.value()
                                    }
                                }
                            }
                            break
                        }
                        val delta = change.positionChange()
                        tracker.addPosition(change.uptimeMillis, change.position)
                        dragged += delta
                        if (!pastSlop && hypot(dragged.x, dragged.y) > slop) pastSlop = true
                        if (pastSlop) {
                            change.consume()
                            offset += delta
                        }
                    }
                }
            }
            .semantics {
                contentDescription = if (paused) s.timerResume else s.timerPause
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(dialogSheetFill())
                .border(1.dp, buttonLine(), CircleShape)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f
                val tickStroke = 1.dp.toPx()
                for (mark in 0 until 12) {
                    val angle = Math.toRadians(mark * 30.0 - 90.0)
                    val cardinal = mark % 3 == 0
                    val outer = radius - 6.dp.toPx()
                    val inner = radius - if (cardinal) 14.dp.toPx() else 11.dp.toPx()
                    val dx = cos(angle).toFloat()
                    val dy = sin(angle).toFloat()
                    drawLine(
                        color = if (cardinal) ink else quiet,
                        start = Offset(center.x + dx * inner, center.y + dy * inner),
                        end = Offset(center.x + dx * outer, center.y + dy * outer),
                        strokeWidth = if (cardinal) tickStroke * 1.4f else tickStroke,
                        cap = StrokeCap.Round
                    )
                }
                val handAngle = Math.toRadians(
                    TaskTimerPlanner.remainHandDegrees(remainMillis).toDouble() - 90.0
                )
                val hx = cos(handAngle).toFloat()
                val hy = sin(handAngle).toFloat()
                drawLine(
                    color = hand,
                    start = center,
                    end = Offset(center.x + hx * radius * 0.62f, center.y + hy * radius * 0.62f),
                    strokeWidth = 1.75.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(color = hand, radius = 2.25.dp.toPx(), center = center)
            }
        }
        Text(
            text = clock,
            modifier = Modifier.padding(top = 4.dp),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            color = if (paused) quiet else ink
        )
    }
}

private const val DISMISS_SPEED_PX_PER_SEC = 3200f
private const val DISMISS_EXIT_PX = 1600f
