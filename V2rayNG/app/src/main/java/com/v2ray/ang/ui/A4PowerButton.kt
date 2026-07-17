package com.v2ray.ang.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

internal enum class A4ConnState { Disconnected, Connecting, Connected }

private class Particle(val angle: Float, val dist: Float, val sizeDp: Float, val dark: Boolean)

private val burstParticles: List<Particle> = run {
    val rnd = Random(7)
    List(16) { i ->
        Particle(
            angle = (i / 16f) * (2f * PI.toFloat()) + rnd.nextFloat() * 0.35f,
            dist = 0.55f + rnd.nextFloat() * 0.5f,
            sizeDp = 5f + rnd.nextFloat() * 6f,
            dark = i % 3 == 0,
        )
    }
}

/**
 * Главная круглая кнопка подключения:
 *  - дыхание в покое, пружинное сжатие при нажатии
 *  - «марширующие муравьи» по кольцу при подключении + спиннер
 *  - в момент подключения: заливка красным, разлёт частиц, эхо-кольца
 */
@Composable
internal fun A4PowerButton(
    conn: A4ConnState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "press",
    )

    val infinite = rememberInfiniteTransition(label = "power")
    val breath by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )
    val dashPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = -84f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing)),
        label = "dash",
    )
    val ripple by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "ripple",
    )
    val spin by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "spin",
    )

    val fill by animateColorAsState(
        targetValue = when {
            !enabled && conn == A4ConnState.Disconnected -> A4TextMuted
            conn == A4ConnState.Connected -> A4Red
            else -> A4Ink
        },
        animationSpec = tween(450),
        label = "fill",
    )

    // разовый разлёт частиц + упругий «поп» в момент подключения
    val burst = remember { Animatable(1f) }
    val pop = remember { Animatable(1f) }
    var prevConn by remember { mutableStateOf<A4ConnState?>(null) }
    LaunchedEffect(conn) {
        val wasConnecting = prevConn != null && prevConn != A4ConnState.Connected
        prevConn = conn
        if (conn == A4ConnState.Connected && wasConnecting) {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            burst.snapTo(0f)
            pop.snapTo(0.88f)
            burst.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
            pop.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
            )
        }
    }

    Box(modifier.size(250.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val btnR = 88.dp.toPx()

            // эхо-кольца, расходящиеся от кнопки
            if (conn == A4ConnState.Connected) {
                repeat(3) { i ->
                    val p = (ripple + i / 3f) % 1f
                    drawCircle(
                        color = A4Red,
                        radius = btnR * (1f + p * 0.42f),
                        center = center,
                        style = Stroke(2.dp.toPx()),
                        alpha = (1f - p) * 0.35f,
                    )
                }
            }

            // пунктирное кольцо при подключении
            if (conn == A4ConnState.Connecting) {
                drawCircle(
                    color = A4Red,
                    radius = btnR + 13.dp.toPx(),
                    center = center,
                    style = Stroke(
                        width = 2.5f.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(26f, 16f), dashPhase),
                    ),
                )
            }

            // разлёт частиц
            if (burst.value < 1f) {
                val t = burst.value
                burstParticles.forEach { p ->
                    val r = btnR + t * p.dist * btnR * 1.6f
                    val x = center.x + cos(p.angle) * r
                    val y = center.y + sin(p.angle) * r
                    val half = p.sizeDp.dp.toPx() / 2f
                    rotate(degrees = t * 220f, pivot = Offset(x, y)) {
                        drawRect(
                            color = if (p.dark) A4Ink else A4Red,
                            topLeft = Offset(x - half, y - half),
                            size = Size(half * 2, half * 2),
                            alpha = (1f - t),
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .size(176.dp)
                .graphicsLayer {
                    val idle = if (conn == A4ConnState.Disconnected && enabled) breath else 1f
                    val s = pressScale * idle * pop.value
                    scaleX = s
                    scaleY = s
                }
                .clip(CircleShape)
                .background(fill)
                .clickable(interactionSource = interaction, indication = null) {
                    if (enabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(64.dp)) {
                val stroke = 6.dp.toPx()
                val r = size.minDimension / 2f - stroke
                if (conn == A4ConnState.Connecting) {
                    rotate(spin) {
                        drawArc(
                            color = Color.White,
                            startAngle = 0f,
                            sweepAngle = 120f,
                            useCenter = false,
                            topLeft = Offset(center.x - r, center.y - r),
                            size = Size(r * 2, r * 2),
                            style = Stroke(stroke, cap = StrokeCap.Round),
                        )
                    }
                    val r2 = r * 0.55f
                    rotate(-spin * 1.6f) {
                        drawArc(
                            color = Color.White.copy(alpha = 0.45f),
                            startAngle = 180f,
                            sweepAngle = 100f,
                            useCenter = false,
                            topLeft = Offset(center.x - r2, center.y - r2),
                            size = Size(r2 * 2, r2 * 2),
                            style = Stroke(stroke * 0.6f, cap = StrokeCap.Round),
                        )
                    }
                } else {
                    // символ питания: разомкнутое кольцо + черта
                    drawArc(
                        color = Color.White,
                        startAngle = -60f,
                        sweepAngle = 300f,
                        useCenter = false,
                        topLeft = Offset(center.x - r, center.y - r),
                        size = Size(r * 2, r * 2),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(center.x, center.y - r * 1.15f),
                        end = Offset(center.x, center.y - r * 0.3f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}
