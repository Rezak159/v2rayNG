package com.v2ray.ang.ui

import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

private val blurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** Фон, который может быть размыт только нижней стеклянной навигацией. */
@Stable
internal class A4GlassBackdrop {
    internal var blurred: GraphicsLayer? = null
    internal var origin: LayoutCoordinates? = null

    private var ticks = 0
    internal var version by mutableStateOf(0, neverEqualPolicy())

    internal fun invalidate() {
        version = ++ticks
    }
}

@Composable
internal fun rememberA4GlassBackdrop(): A4GlassBackdrop = remember { A4GlassBackdrop() }

/** Записывает существующий фон в слой, который нижняя панель берёт для блюра. */
internal fun Modifier.a4GlassBackdropSource(
    backdrop: A4GlassBackdrop,
    radius: Dp = 30.dp,
): Modifier = composed {
    val positioned = Modifier.onGloballyPositioned { backdrop.origin = it }
    if (!blurSupported) return@composed positioned

    val content = rememberGraphicsLayer()
    val blurred = rememberGraphicsLayer()
    val radiusPx = with(LocalDensity.current) { radius.toPx() }

    positioned.drawWithContent {
        content.record { this@drawWithContent.drawContent() }
        blurred.renderEffect = BlurEffect(radiusPx, radiusPx)
        blurred.record { drawLayer(content) }
        backdrop.blurred = blurred
        drawLayer(content)
        backdrop.invalidate()
    }
}

/** Liquid Glass material, scoped solely to the bottom navigation. */
internal fun Modifier.a4LiquidGlass(
    shape: Shape,
    backdrop: A4GlassBackdrop,
    milk: Float,
    elevation: Dp,
): Modifier = composed {
    var position by remember { mutableStateOf(Offset.Zero) }
    val locate = Modifier.onGloballyPositioned { coords ->
        position = backdrop.origin?.localPositionOf(coords, Offset.Zero) ?: Offset.Zero
    }
    val shadowLayer = rememberGraphicsLayer()

    this
        .drawBehind {
            val blur = elevation.toPx()
            if (blurSupported && blur > 0f) {
                val pad = blur * 3f
                val outline = shape.createOutline(size, layoutDirection, this)
                shadowLayer.renderEffect = BlurEffect(blur, blur)
                shadowLayer.record(
                    density = this,
                    layoutDirection = layoutDirection,
                    size = IntSize(
                        (size.width + pad * 2f).toInt(),
                        (size.height + pad * 2f).toInt(),
                    ),
                ) {
                    translate(pad, pad) {
                        drawOutline(outline, color = A4Ink.copy(alpha = 0.20f))
                    }
                }
                clipPath(outline.asPath(), androidx.compose.ui.graphics.ClipOp.Difference) {
                    translate(-pad, -pad + blur * 0.7f) { drawLayer(shadowLayer) }
                }
            } else {
                val drop = blur * 1.8f
                val inset = size.width * 0.035f
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(A4Ink.copy(alpha = 0.14f), Color.Transparent),
                        startY = size.height - 2f,
                        endY = size.height + drop,
                    ),
                    topLeft = Offset(inset, size.height - 2f),
                    size = Size(size.width - inset * 2f, drop + 2f),
                )
            }
        }
        .then(locate)
        .clip(shape)
        .drawBehind {
            backdrop.version
            backdrop.blurred?.let { layer ->
                translate(-position.x, -position.y) { drawLayer(layer) }
            }
            val top = if (blurSupported) 0.30f else 0.78f
            val bottom = if (blurSupported) 0.10f else 0.64f
            drawRect(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = top * milk),
                        Color.White.copy(alpha = bottom * milk),
                    ),
                ),
            )
            val lip = 7.dp.toPx()
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
                    startY = 0f,
                    endY = lip,
                ),
                size = Size(size.width, lip),
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, A4Ink.copy(alpha = 0.07f)),
                    startY = size.height - lip,
                    endY = size.height,
                ),
                topLeft = Offset(0f, size.height - lip),
                size = Size(size.width, lip),
            )
        }
        .border(1.2.dp, A4GlassRim, shape)
}

private fun Outline.asPath(): Path = when (this) {
    is Outline.Generic -> path
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Rectangle -> Path().apply { addRect(rect) }
}

private val A4GlassRim = Brush.linearGradient(
    0f to Color.White.copy(alpha = 0.98f),
    0.32f to Color.White.copy(alpha = 0.25f),
    0.62f to A4Ink.copy(alpha = 0.06f),
    1f to Color.White.copy(alpha = 0.75f),
)
