@file:OptIn(ExperimentalTextApi::class)

package com.v2ray.ang.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R

// Фирменная палитра a4flow: бумага, чернила, единственный красный акцент.
internal val A4Paper = Color(0xFFF4EDE3)
internal val A4PaperCard = Color(0xFFFBF6EC)
internal val A4Ink = Color(0xFF16130F)
internal val A4Red = Color(0xFFC81E26)
internal val A4Border = Color(0xFFE0D6C5)
internal val A4TextMuted = Color(0xFF847B6D)
internal val A4OnDarkMuted = Color(0xFFCFC7BA)

internal val A4Overshoot = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

private fun geologica(weight: FontWeight) = Font(
    resId = R.font.geologica,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private fun golos(weight: FontWeight) = Font(
    resId = R.font.golos_text,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Шрифт вордмарка «a4vpn» с сайта — только это начертание, кириллица в нём не нужна. */
internal val A4Unbounded = FontFamily(Font(resId = R.font.unbounded, weight = FontWeight.ExtraBold))

internal val A4Geologica = FontFamily(
    geologica(FontWeight.Medium),
    geologica(FontWeight.SemiBold),
    geologica(FontWeight.Bold),
    geologica(FontWeight.ExtraBold),
    geologica(FontWeight.Black),
)

internal val A4Golos = FontFamily(
    golos(FontWeight.Normal),
    golos(FontWeight.Medium),
    golos(FontWeight.SemiBold),
    golos(FontWeight.Bold),
)

internal val A4Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = A4Geologica,
        fontWeight = FontWeight.Black,
        fontSize = 40.sp,
        lineHeight = 42.sp,
        letterSpacing = (-1).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = A4Geologica,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.5).sp,
    ),
    titleMedium = TextStyle(fontFamily = A4Geologica, fontWeight = FontWeight.Bold, fontSize = 17.sp, letterSpacing = (-0.2).sp),
    bodyMedium = TextStyle(fontFamily = A4Golos, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = A4Golos, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = A4Geologica, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontFamily = A4Geologica, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.sp),
)

@Composable
internal fun A4Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = A4Red,
            onPrimary = Color.White,
            background = A4Paper,
            surface = A4Paper,
            onBackground = A4Ink,
            onSurface = A4Ink,
        ),
        typography = A4Typography,
        content = content,
    )
}

/** Клик с пружинным сжатием, без стандартного ripple. */
internal fun Modifier.springClick(scale: Float = 0.97f, onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(
        targetValue = if (pressed) scale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "pressScale",
    )
    this
        .graphicsLayer {
            scaleX = s
            scaleY = s
        }
        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
}

/** Красный подзаголовок раздела капсом, как «ЧИТАЙТЕ ТАКЖЕ» на сайте. */
@Composable
internal fun A4SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = A4Red,
        modifier = modifier,
    )
}

/** Индикатор пинга: четыре столбика, заполненность зависит от качества. */
@Composable
internal fun A4PingBars(pingMs: Long, modifier: Modifier = Modifier) {
    // Пороги под «полный» пинг (поднятие ядра + TLS + HTTP-запрос наружу), а не
    // голый сетевой RTT — он по своей природе на сотни мс дороже.
    val level = when {
        pingMs <= 0 -> 0
        pingMs < 150 -> 4
        pingMs < 250 -> 3
        pingMs < 400 -> 2
        else -> 1
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        val heights = listOf(5.dp, 8.dp, 11.dp, 14.dp)
        heights.forEachIndexed { i, h ->
            val filled = i < level
            val barHeight by animateDpAsState(
                targetValue = if (filled) h else 4.dp,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "bar$i",
            )
            Box(
                Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (filled) A4Red else A4Border),
            )
        }
    }
}

/** Появление элемента списка каскадом, с лёгким овершутом. */
@Composable
internal fun A4StaggerIn(index: Int, content: @Composable () -> Unit) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visible,
        enter = fadeIn(tween(300, delayMillis = index * 55)) +
            slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(430, delayMillis = index * 55, easing = A4Overshoot),
            ),
    ) {
        content()
    }
}

/**
 * Название сервера с бегущим серебристым бликом слева направо — знак того, что
 * VPN активен на этом сервере. [shimmer] выключен — обычный текст без анимации.
 */
@Composable
internal fun ShimmerText(
    text: String,
    style: TextStyle,
    color: Color,
    shimmer: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!shimmer) {
        Text(text, style = style, color = color, modifier = modifier)
        return
    }
    val progress by rememberInfiniteTransition(label = "nameShimmer").animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "nameShimmerProgress",
    )
    var widthPx by remember { mutableFloatStateOf(0f) }
    val brush = if (widthPx <= 0f) {
        SolidColor(color)
    } else {
        val center = progress * widthPx
        val band = widthPx * 0.35f
        Brush.linearGradient(
            colors = listOf(color, color, Color.White, color, color),
            start = Offset(center - band, 0f),
            end = Offset(center + band, 0f),
        )
    }
    Text(
        text,
        style = style.copy(brush = brush),
        modifier = modifier.onGloballyPositioned { widthPx = it.size.width.toFloat() },
    )
}
