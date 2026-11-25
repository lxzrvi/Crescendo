package com.lxzrvi.crescendo.ui.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

/** Ek blob: position/radius screen ke fraction mein (cx, cy = w/h ka %, r = width ka %). */
private class Blob(
    val color: Color,
    val cx: Float,
    val cy: Float,
    val r: Float,
    val alpha: Float = 1f,
    val amp: Float = 0f,     // drift ki range (width ka %)
    val phase: Float = 0f,   // har blob ka alag timing
)

private fun smoothstep(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** Gaussian falloff: andar se bahar bilkul smooth, koi disc/edge nahi dikhta (real blur jaisa). */
private fun softStops(color: Color, alpha: Float, n: Int = 16, k: Double = 4.2): Array<Pair<Float, Color>> {
    val edge = exp(-k)
    return Array(n + 1) { i ->
        val t = i / n.toDouble()
        val a = ((exp(-k * t * t) - edge) / (1.0 - edge)).toFloat()
        t.toFloat() to color.copy(alpha = alpha * a)
    }
}

/** Soft base that follows the glow from the top edge to the bottom edge. */
private fun baseStops(color: Color, position: Float, n: Int = 30): Array<Pair<Float, Color>> =
    Array(n + 1) { i ->
        val y = i / n.toFloat()
        val distance = abs(y - position)
        y to color.copy(alpha = 0.9f * (1f - smoothstep((distance - 0.30f) / 0.35f)))
    }

/** Blend away from the moving glow into pure AMOLED black, without a hard edge. */
private fun inkStops(deep: Color, position: Float, n: Int = 40): Array<Pair<Float, Color>> =
    Array(n + 1) { i ->
        val y = i / n.toFloat()
        val distance = abs(y - position)
        // On welcome, leave more of the top pure black and fade gently into the lower glow.
        val fadeStart = 0.34f - 0.07f * position
        val fadeEnd = 0.82f - 0.15f * position
        val t = ((distance - fadeStart) / (fadeEnd - fadeStart)).coerceIn(0f, 1f)
        val tone = lerp(deep, AmoledBlack, smoothstep((t - 0.30f) / 0.70f))
        y to tone.copy(alpha = smoothstep(t))
    }

/** position: 0 = permission pages (top), 1 = welcome page (bottom). */
@Composable
fun GradientBlobBackground(modifier: Modifier = Modifier, position: Float = 0f) {
    val transition = rememberInfiniteTransition(label = "blobs")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 24000, easing = LinearEasing)),
        label = "phase",
    )

    Box(
        modifier
            .background(AmoledBlack)
            .drawWithCache {
                val w = size.width
                val h = size.height

                val glow = lerp(Blush, Color.White, 0.5f)
                val deep = lerp(RedDeep, AmoledBlack, 0.78f)

                // Peeche se aage: rang ki blobs (halka slow drift)
                val lights = listOf(
                    Blob(NeonRed, 0.10f, 0.36f, 1.05f, amp = 0.05f, phase = 0.0f),
                    Blob(RedDeep, 0.95f, 0.02f, 0.90f, amp = 0.04f, phase = 1.7f),
                    Blob(RedDeep, 0.95f, 0.38f, 0.75f, amp = 0.04f, phase = 2.4f),
                    Blob(RedSoft, 0.45f, 0.30f, 0.55f, alpha = 0.55f, amp = 0.05f, phase = 5.0f),
                    Blob(glow, 0.70f, 0.22f, 0.80f, alpha = 0.95f, amp = 0.06f, phase = 3.1f),
                    Blob(Blush, 0.05f, 0.03f, 0.70f, alpha = 0.90f, amp = 0.05f, phase = 4.4f),
                )
                // Neeche ke kaale blobs alag heights pe -> blend line uneven/organic
                val inks = listOf(
                    Blob(AmoledBlack, 0.10f, 0.84f, 1.10f),
                    Blob(AmoledBlack, 0.72f, 0.70f, 0.85f),
                    Blob(AmoledBlack, 1.00f, 0.76f, 0.70f),
                )

                val placement = position.coerceIn(0f, 1f)
                // Move every coloured blob down together, preserving its shape and slow drift.
                val lightCenters = lights.map { Offset(it.cx * w, (it.cy + 0.68f * placement) * h) }
                val topInkCenters = inks.map { Offset(it.cx * w, it.cy * h) }
                val bottomInkCenters = inks.map { Offset(it.cx * w, (1f - it.cy) * h) }

                fun brushOf(b: Blob, center: Offset) = Brush.radialGradient(
                    *softStops(b.color, b.alpha),
                    center = center,
                    radius = b.r * w,
                )

                val baseBrush = Brush.verticalGradient(*baseStops(RedDeep, placement), startY = 0f, endY = h)
                val lightBrushes = lights.mapIndexed { i, b -> brushOf(b, lightCenters[i]) }
                val inkFade = Brush.verticalGradient(*inkStops(deep, placement), startY = 0f, endY = h)
                val topInkBrushes = inks.mapIndexed { i, b -> brushOf(b, topInkCenters[i]) }
                val bottomInkBrushes = inks.mapIndexed { i, b -> brushOf(b, bottomInkCenters[i]) }
                val topInkAlpha = 1f - smoothstep(placement * 2f)
                val bottomInkAlpha = smoothstep((placement - 0.5f) * 2f)

                onDrawBehind {
                    val p = phase.value * 2f * PI.toFloat()

                    drawRect(baseBrush)

                    lights.forEachIndexed { i, b ->
                        val dx = sin(p + b.phase) * b.amp * w
                        val dy = cos(p + b.phase) * b.amp * w * 0.6f
                        translate(dx, dy) {
                            drawCircle(
                                brush = lightBrushes[i],
                                radius = b.r * w,
                                center = lightCenters[i],
                            )
                        }
                    }

                    drawRect(inkFade)

                    // The dark edge switches sides as the glow travels, staying soft throughout.
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
