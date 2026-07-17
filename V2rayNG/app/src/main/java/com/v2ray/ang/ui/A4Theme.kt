@file:OptIn(ExperimentalTextApi::class)

package com.v2ray.ang.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

/** Чёрная плашка с тонкой красной полосой слева — «важная мысль». */
@Composable
internal fun A4BlackPlate(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(A4Ink)
            .height(IntrinsicSize.Min),
    ) {
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(A4Red),
        )
        Box(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            content()
        }
    }
}

/** Чёрный квадрат с номером шага — «01», «02», «03». */
@Composable
internal fun A4StepBadge(number: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(26.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(A4Ink),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number,
            color = Color.White,
            fontFamily = A4Geologica,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
        )
    }
}

/** Индикатор пинга: четыре столбика, заполненность зависит от качества. */
@Composable
internal fun A4PingBars(pingMs: Long, modifier: Modifier = Modifier) {
    val level = when {
        pingMs <= 0 -> 0
        pingMs < 80 -> 4
        pingMs < 150 -> 3
        pingMs < 300 -> 2
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
