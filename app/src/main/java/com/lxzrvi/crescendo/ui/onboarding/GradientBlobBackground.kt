package com.lxzrvi.crescendo.ui.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import com.lxzrvi.crescendo.ui.theme.AmoledBlack
import com.lxzrvi.crescendo.ui.theme.Blush
import com.lxzrvi.crescendo.ui.theme.NeonRed
import com.lxzrvi.crescendo.ui.theme.RedDeep
import com.lxzrvi.crescendo.ui.theme.RedSoft
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
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

private fun smoothstep(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** Gaussian falloff for soft, blended edges. */
private fun softStops(color: Color, alpha: Float, n: Int = 16, k: Double = 4.2): Array<Pair<Float, Color>> {
    val edge = exp(-k)
    return Array(n + 1) { i ->
        val t = i / n.toDouble()
        val a = ((exp(-k * t * t) - edge) / (1.0 - edge)).toFloat()
        t.toFloat() to color.copy(alpha = alpha * a)
    }
}

// Symmetrical brushes can be cached once, then moved instead of rebuilt each animation frame.
private fun baseStops(color: Color, n: Int = 60): Array<Pair<Float, Color>> =
    Array(n + 1) { i ->
        val t = i / n.toFloat()
        val distance = abs(2f * t - 1f)
        t to color.copy(alpha = 0.9f * (1f - smoothstep((distance - 0.30f) / 0.35f)))
    }

private fun inkStops(deep: Color, n: Int = 80): Array<Pair<Float, Color>> =
    Array(n + 1) { i ->
        val t = i / n.toFloat()
        val distance = abs(2f * t - 1f)
        val fade = ((distance - 0.34f) / 0.48f).coerceIn(0f, 1f)
        val tone = lerp(deep, AmoledBlack, smoothstep((fade - 0.30f) / 0.70f))
        t to tone.copy(alpha = smoothstep(fade))
    }

/** Shared drift keeps the gradient continuous when onboarding hands off to Home. */
@Composable
fun rememberGradientPhase(): State<Float> {
    val transition = rememberInfiniteTransition(label = "blobs")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 24000, easing = LinearEasing)),
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
    Box(
        modifier
            .background(AmoledBlack)
            .drawWithCache {
                val w = size.width
                val h = size.height
                val glow = lerp(Blush, Color.White, 0.5f)
                val deep = lerp(RedDeep, AmoledBlack, 0.78f)

                val lights = listOf(
                    Blob(NeonRed, 0.10f, 0.36f, 1.05f, amp = 0.05f, phase = 0.0f),
                    Blob(RedDeep, 0.95f, 0.02f, 0.90f, amp = 0.04f, phase = 1.7f),
                    Blob(RedDeep, 0.95f, 0.38f, 0.75f, amp = 0.04f, phase = 2.4f),
                    Blob(RedSoft, 0.45f, 0.30f, 0.55f, alpha = 0.55f, amp = 0.05f, phase = 5.0f),
                    Blob(glow, 0.70f, 0.22f, 0.80f, alpha = 0.95f, amp = 0.06f, phase = 3.1f),
                    Blob(Blush, 0.05f, 0.03f, 0.70f, alpha = 0.90f, amp = 0.05f, phase = 4.4f),
                )
                val inks = listOf(
                    Blob(AmoledBlack, 0.10f, 0.84f, 1.10f),
                    Blob(AmoledBlack, 0.72f, 0.70f, 0.85f),
                    Blob(AmoledBlack, 1.00f, 0.76f, 0.70f),
                )
                val lightCenters = lights.map { Offset(it.cx * w, it.cy * h) }
                val topInkCenters = inks.map { Offset(it.cx * w, it.cy * h) }
                val bottomInkCenters = inks.map { Offset(it.cx * w, (1f - it.cy) * h) }

                fun brushOf(b: Blob, center: Offset) = Brush.radialGradient(
                    *softStops(b.color, b.alpha),
                    center = center,
                    radius = b.r * w,
                )

                val baseBrush = Brush.verticalGradient(*baseStops(RedDeep), startY = -h, endY = h)
                val lightBrushes = lights.mapIndexed { i, b -> brushOf(b, lightCenters[i]) }
                val inkFade = Brush.verticalGradient(*inkStops(deep), startY = -h, endY = h)
                val topInkBrushes = inks.mapIndexed { i, b -> brushOf(b, topInkCenters[i]) }
                val bottomInkBrushes = inks.mapIndexed { i, b -> brushOf(b, bottomInkCenters[i]) }

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

                    // Move and reshape the cached fade: the welcome top stays black longer.
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
                    inks.forEachIndexed { i, b ->
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
