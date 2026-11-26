package com.lxzrvi.crescendo.ui.home

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.lxzrvi.crescendo.ui.onboarding.GradientSampler

/**
 * Contrast follows the known content immediately behind each edge: the live gradient and any
 * currently visible placeholder/settings surface. This avoids screenshot feedback and keeps
 * the border responsive while cards scroll or the gradient moves.
 */
internal fun Modifier.contextAwareBorder(
    gradientPhase: State<Float>,
    rootSize: State<IntSize>,
    listOrigin: State<Offset>,
    listState: LazyListState,
    background: Color,
    overlay: Color,
    dark: Boolean,
): Modifier = composed {
    var position by remember { mutableStateOf(Offset.Zero) }
    this
        .onGloballyPositioned { position = it.positionInRoot() }
        .drawWithContent {
            drawContent()
            val sampler = GradientSampler(
                size = rootSize.value,
                position = 0f,
                phase = gradientPhase.value,
                background = background,
            )
            val viewport = listOrigin.value
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val cardInset = 20.dp.toPx()

            fun edgeColor(x: Float, y: Float): Color {
                val point = position + Offset(x, y)
                var beneath = sampler.colorAt(point)
                val withinCardWidth = point.x >= viewport.x + cardInset &&
                    point.x <= rootSize.value.width - cardInset
                if (withinCardWidth) {
                    val listY = point.y - viewport.y
                    val item = visibleItems.firstOrNull { listY >= it.offset && listY < it.offset + it.size }
                    val key = item?.key as? String
                    val surface = when {
                        key == "blur-settings" -> settingsSurfaceColor(dark)
                        key?.startsWith("sample:") == true -> {
                            val number = key.substringAfterLast(':').toIntOrNull()
                            number?.let { placeholderSurfaceColor(dark, it) }
                        }
                        else -> null
                    }
                    if (surface != null) beneath = surface.compositeOver(beneath)
                }
                val apparent = overlay.compositeOver(beneath)
                val brightness = (apparent.red + apparent.green + apparent.blue) / 3f
                return if (apparent.luminance() > 0.22f) {
                    Color.Black.copy(alpha = (0.13f + 0.14f * (1f - brightness)).coerceIn(0.13f, 0.27f))
                } else {
                    Color.White.copy(alpha = (0.17f + 0.16f * brightness).coerceIn(0.17f, 0.33f))
                }
            }

            val stroke = 1.dp.toPx()
            val half = stroke / 2f
            val brush = Brush.linearGradient(
                colors = listOf(
                    edgeColor(size.width * 0.08f, size.height * 0.12f),
                    edgeColor(size.width * 0.32f, half),
                    edgeColor(size.width * 0.55f, size.height * 0.50f),
                    edgeColor(size.width * 0.78f, size.height - half),
                    edgeColor(size.width * 0.92f, size.height * 0.88f),
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            )
            val innerHeight = (size.height - stroke).coerceAtLeast(0f)
            drawRoundRect(
                brush = brush,
                topLeft = Offset(half, half),
                size = Size((size.width - stroke).coerceAtLeast(0f), innerHeight),
                cornerRadius = CornerRadius(innerHeight / 2f),
                style = Stroke(width = stroke),
            )
        }
}
