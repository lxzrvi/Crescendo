package com.lxzrvi.crescendo.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

/**
 * The rim is a second, differently tinted sample of the actual live source, clipped to a thin
 * capsule. It therefore follows real pixels at each part of the edge (including scrolling cards),
 * rather than guessing a single color from the page's known gradient. The opaque mode draws only
 * the static highlight, so it never leaks page content through a "blur off" control.
 */
@Composable
internal fun LiveGlassBorder(
    input: HazeInput.Sources,
    liveBlur: Boolean,
    background: Color,
    dark: Boolean,
    modifier: Modifier = Modifier,
) {
    val rimStyle = remember(background, dark) {
        HazeBlurStyle {
            blurEnabled(true)
            blurRadius(5.dp)
            noiseFactor(0f)
            backgroundColor(background)
            colorEffects(
                listOf(
                    HazeColorEffect.tint(
                        if (dark) Color.White.copy(alpha = 0.25f)
                        else Color.Black.copy(alpha = 0.17f),
                    ),
                ),
            )
            fallbackColorEffect(
                HazeColorEffect.tint(
                    if (dark) Color(0xFF55555A) else Color(0xFFC5C2C2),
                ),
            )
        }
    }
    val sample = if (liveBlur) Modifier.hazeBlur(input = input, style = rimStyle) else Modifier
    Box(
        modifier = modifier
            .drawWithCache {
                val thickness = 1.5.dp.toPx()
                val innerWidth = (size.width - thickness * 2f).coerceAtLeast(0f)
                val innerHeight = (size.height - thickness * 2f).coerceAtLeast(0f)
                val outer = Path().apply {
                    addRoundRect(
                        RoundRect(0f, 0f, size.width, size.height, CornerRadius(size.height / 2f)),
                    )
                }
                val inner = Path().apply {
                    addRoundRect(
                        RoundRect(
                            thickness,
                            thickness,
                            thickness + innerWidth,
                            thickness + innerHeight,
                            CornerRadius(innerHeight / 2f),
                        ),
                    )
                }
                val shine = Brush.verticalGradient(
                    colors = if (dark) {
                        listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.04f))
                    } else {
                        listOf(Color.White.copy(alpha = 0.40f), Color.Black.copy(alpha = 0.09f))
                    },
                )
                onDrawWithContent {
                    clipPath(outer) {
                        clipPath(inner, clipOp = ClipOp.Difference) { this@onDrawWithContent.drawContent() }
                    }
                    val line = 0.7.dp.toPx()
                    drawRoundRect(
                        brush = shine,
                        topLeft = Offset(line / 2f, line / 2f),
                        size = Size(
                            (size.width - line).coerceAtLeast(0f),
                            (size.height - line).coerceAtLeast(0f),
                        ),
                        cornerRadius = CornerRadius((size.height - line).coerceAtLeast(0f) / 2f),
                        style = Stroke(line),
                    )
                }
            }
            .then(sample),
    )
}
