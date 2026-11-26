package com.lxzrvi.crescendo.ui.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.IntSize
import com.lxzrvi.crescendo.ui.theme.Blush
import com.lxzrvi.crescendo.ui.theme.NeonRed
import com.lxzrvi.crescendo.ui.theme.RedDeep
import com.lxzrvi.crescendo.ui.theme.RedSoft
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sin

/** Blob position is a screen fraction; radius and drift use screen width. */
private class Blob(
    val color: Color,
    val cx: Float,
    val cy: Float,
    val r: Float,
    val alpha: Float = 1f,
    val amp: Float = 0f,
    val phase: Float = 0f,
)

private val lights = listOf(
    Blob(NeonRed, 0.10f, 0.36f, 1.05f, amp = 0.05f, phase = 0.0f),
    Blob(RedDeep, 0.95f, 0.02f, 0.90f, amp = 0.04f, phase = 1.7f),
    Blob(RedDeep, 0.95f, 0.38f, 0.75f, amp = 0.04f, phase = 2.4f),
    Blob(RedSoft, 0.45f, 0.30f, 0.55f, alpha = 0.55f, amp = 0.05f, phase = 5.0f),
    Blob(lerp(Blush, Color.White, 0.5f), 0.70f, 0.22f, 0.80f, alpha = 0.95f, amp = 0.06f, phase = 3.1f),
    Blob(Blush, 0.05f, 0.03f, 0.70f, alpha = 0.90f, amp = 0.05f, phase = 4.4f),
)

private val inkShapes = listOf(
    Blob(Color.Black, 0.10f, 0.84f, 1.10f),
    Blob(Color.Black, 0.72f, 0.70f, 0.85f),
    Blob(Color.Black, 1.00f, 0.76f, 0.70f),
)

private fun smoothstep(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun baseOpacity(background: Color) = if (background.luminance() > 0.5f) 0.30f else 0.90f
private fun lightOpacity(background: Color) = if (background.luminance() > 0.5f) 0.52f else 1f
private val gaussianEdge = exp(-4.2)

/** Gaussian falloff for soft, blended edges. */
private fun gaussianAlpha(distance: Float, alpha: Float): Float {
    if (distance >= 1f) return 0f
    return (((exp(-4.2 * distance * distance) - gaussianEdge) / (1.0 - gaussianEdge)) * alpha)
        .toFloat().coerceIn(0f, 1f)
}

private fun softStops(color: Color, alpha: Float, n: Int = 16): Array<Pair<Float, Color>> =
    Array(n + 1) { i ->
        val t = i / n.toFloat()
        t to color.copy(alpha = gaussianAlpha(t, alpha))
    }

// Symmetrical brushes can be cached once, then moved instead of rebuilt each animation frame.
private fun baseStops(color: Color, opacity: Float, n: Int = 60): Array<Pair<Float, Color>> =
    Array(n + 1) { i ->
        val t = i / n.toFloat()
        val distance = abs(2f * t - 1f)
        t to color.copy(alpha = opacity * (1f - smoothstep((distance - 0.30f) / 0.35f)))
    }

private fun inkStops(deep: Color, background: Color, n: Int = 80): Array<Pair<Float, Color>> =
    Array(n + 1) { i ->
        val t = i / n.toFloat()
        val distance = abs(2f * t - 1f)
        val fade = ((distance - 0.34f) / 0.48f).coerceIn(0f, 1f)
        val tone = lerp(deep, background, smoothstep((fade - 0.30f) / 0.70f))
        t to tone.copy(alpha = smoothstep(fade))
    }

/** Where the gradient layer is drawn relative to the screen. */
internal data class GradientLayerTransform(
    val x: Float = 0f,
    val y: Float = 0f,
    val alpha: Float = 1f,
)

/**
 * Samples the same layers as the backdrop, including its animated drift. This is used only
 * for the small footer ink, so it remains legible over both bright and dark parts of the glow.
 */
internal class GradientSampler(
    size: IntSize,
    position: Float,
    phase: Float,
    private val background: Color,
) {
    private val w = size.width.toFloat()
    private val h = size.height.toFloat()
    private val placement = position.coerceIn(0f, 1f)
    private val centerY = placement * h
    private val deep = lerp(RedDeep, background, 0.78f)
    private val baseStrength = baseOpacity(background)
    private val blobOpacity = lightOpacity(background)
    private val topInkAlpha = 1f - smoothstep(placement * 2f)
    private val bottomInkAlpha = smoothstep((placement - 0.5f) * 2f)
    private val lightCenters = lights.map { b ->
        val drift = phase * 2f * PI.toFloat()
        Offset(
            b.cx * w + sin(drift + b.phase) * b.amp * w,
            b.cy * h + cos(drift + b.phase) * b.amp * w * 0.6f + 0.68f * centerY,
        )
    }

    fun colorAt(point: Offset): Color {
        if (w <= 0f || h <= 0f || point.x < 0f || point.x > w || point.y < 0f || point.y > h) {
            return background
        }
        val distance = abs(point.y / h - placement)
        val alpha = baseStrength * (1f - smoothstep((distance - 0.30f) / 0.35f))
        var result = RedDeep.copy(alpha = alpha).compositeOver(background)

        lights.forEachIndexed { i, b ->
            val center = lightCenters[i]
            val d = hypot(point.x - center.x, point.y - center.y) / (b.r * w)
            if (d < 1f) {
                result = b.color.copy(alpha = gaussianAlpha(d, b.alpha * blobOpacity)).compositeOver(result)
            }
        }

        val fadeStart = 0.34f - 0.07f * placement
        val fadeEnd = 0.82f - 0.15f * placement
        val fade = ((distance - fadeStart) / (fadeEnd - fadeStart)).coerceIn(0f, 1f)
        if (fade > 0f) {
            val tone = lerp(deep, background, smoothstep((fade - 0.30f) / 0.70f))
            result = tone.copy(alpha = smoothstep(fade)).compositeOver(result)
        }

        inkShapes.forEach { b ->
            val radius = b.r * w
            if (topInkAlpha > 0f) {
                val d = hypot(point.x - b.cx * w, point.y - b.cy * h) / radius
                if (d < 1f) {
                    result = background.copy(alpha = gaussianAlpha(d, 1f) * topInkAlpha).compositeOver(result)
                }
            }
            if (bottomInkAlpha > 0f) {
                val d = hypot(point.x - b.cx * w, point.y - (1f - b.cy) * h) / radius
                if (d < 1f) {
                    result = background.copy(alpha = gaussianAlpha(d, 1f) * bottomInkAlpha).compositeOver(result)
                }
            }
        }
        return result
    }
}

/** Shared drift keeps the gradient continuous across the onboarding pages. */
@Composable
fun rememberGradientPhase(): State<Float> {
    val transition = rememberInfiniteTransition(label = "blobs")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 20000, easing = LinearEasing)),
        label = "phase",
    )
}

/** position: 0 = top, 1 = lower welcome glow. The lambda is read during drawing. */
@Composable
fun GradientBlobBackground(
    phase: State<Float>,
    modifier: Modifier = Modifier,
    position: () -> Float = { 0f },
) {
    val background = MaterialTheme.colorScheme.background
    Box(
        modifier
            .background(background)
            .drawWithCache {
                val w = size.width
                val h = size.height
                val deep = lerp(RedDeep, background, 0.78f)
                val lightCenters = lights.map { Offset(it.cx * w, it.cy * h) }
                val topInkCenters = inkShapes.map { Offset(it.cx * w, it.cy * h) }
                val bottomInkCenters = inkShapes.map { Offset(it.cx * w, (1f - it.cy) * h) }

                fun brushOf(b: Blob, center: Offset, color: Color = b.color, opacity: Float = 1f) =
                    Brush.radialGradient(
                        *softStops(color, b.alpha * opacity),
                        center = center,
                        radius = b.r * w,
                    )

                val baseBrush = Brush.verticalGradient(
                    *baseStops(RedDeep, baseOpacity(background)), startY = -h, endY = h,
                )
                val lightBrushes = lights.mapIndexed { i, b ->
                    brushOf(b, lightCenters[i], opacity = lightOpacity(background))
                }
                val inkFade = Brush.verticalGradient(*inkStops(deep, background), startY = -h, endY = h)
                val topInkBrushes = inkShapes.mapIndexed { i, b -> brushOf(b, topInkCenters[i], background) }
                val bottomInkBrushes = inkShapes.mapIndexed { i, b -> brushOf(b, bottomInkCenters[i], background) }

                onDrawBehind {
                    // Read animated values here so shaders and Gaussian stops stay cached.
                    val placement = position().coerceIn(0f, 1f)
                    val drift = phase.value * 2f * PI.toFloat()
                    val centerY = placement * h

                    translate(top = centerY) {
                        drawRect(baseBrush, topLeft = Offset(0f, -h), size = Size(w, 2f * h))
                    }

                    lights.forEachIndexed { i, b ->
                        val dx = sin(drift + b.phase) * b.amp * w
                        val dy = cos(drift + b.phase) * b.amp * w * 0.6f + 0.68f * centerY
                        translate(dx, dy) {
                            drawCircle(
                                brush = lightBrushes[i],
                                radius = b.r * w,
                                center = lightCenters[i],
                            )
                        }
                    }

                    val fadeStart = 0.34f - 0.07f * placement
                    val fadeEnd = 0.82f - 0.15f * placement
                    val fadeScale = (fadeEnd - fadeStart) / 0.48f
                    val fadeShift = (fadeStart - 0.34f * fadeScale) * h
                    translate(top = centerY + fadeShift) {
                        scale(scaleX = 1f, scaleY = fadeScale, pivot = Offset.Zero) {
                            drawRect(inkFade, topLeft = Offset.Zero, size = Size(w, 2f * h))
                        }
                    }
                    translate(top = centerY - fadeShift) {
                        scale(scaleX = 1f, scaleY = fadeScale, pivot = Offset.Zero) {
                            drawRect(inkFade, topLeft = Offset(0f, -2f * h), size = Size(w, 2f * h))
                        }
                    }

                    val topInkAlpha = 1f - smoothstep(placement * 2f)
                    val bottomInkAlpha = smoothstep((placement - 0.5f) * 2f)
                    inkShapes.forEachIndexed { i, b ->
                        if (topInkAlpha > 0f) {
                            drawCircle(
                                brush = topInkBrushes[i],
                                radius = b.r * w,
                                center = topInkCenters[i],
                                alpha = topInkAlpha,
                            )
                        }
                        if (bottomInkAlpha > 0f) {
                            drawCircle(
                                brush = bottomInkBrushes[i],
                                radius = b.r * w,
                                center = bottomInkCenters[i],
                                alpha = bottomInkAlpha,
                            )
                        }
                    }
                }
            }
    )
}
