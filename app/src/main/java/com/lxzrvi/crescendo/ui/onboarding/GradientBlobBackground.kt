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

/** Top par halka rang, neeche transparent — blobs ke beech gaps na dikhein. */
private fun baseStops(color: Color, n: Int = 20): Array<Pair<Float, Color>> =
    Array(n + 1) { i ->
        val t = i / n.toFloat()
        t to color.copy(alpha = 0.9f * (1f - smoothstep((t - 0.30f) / 0.35f)))
    }

/** Eased fade: rang -> dark tone -> pure black (seedhi line nahi, premium transition). */
private fun inkStops(deep: Color, n: Int = 12): Array<Pair<Float, Color>> =
    Array(n + 1) { i ->
        val t = i / n.toFloat()
        val tone = lerp(deep, AmoledBlack, smoothstep((t - 0.30f) / 0.70f))
        t to tone.copy(alpha = smoothstep(t))
    }

@Composable
fun GradientBlobBackground(modifier: Modifier = Modifier) {
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

                fun brushOf(b: Blob) = Brush.radialGradient(
                    *softStops(b.color, b.alpha),
                    center = Offset(b.cx * w, b.cy * h),
                    radius = b.r * w,
                )

                val baseBrush = Brush.verticalGradient(*baseStops(RedDeep), startY = 0f, endY = h)
                val lightBrushes = lights.map { brushOf(it) }
                val inkFade = Brush.verticalGradient(*inkStops(deep), startY = 0.34f * h, endY = 0.82f * h)
                val inkBrushes = inks.map { brushOf(it) }

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
                                center = Offset(b.cx * w, b.cy * h),
                            )
                        }
                    }

                    drawRect(inkFade)

                    inks.forEachIndexed { i, b ->
                        drawCircle(
                            brush = inkBrushes[i],
                            radius = b.r * w,
                            center = Offset(b.cx * w, b.cy * h),
                        )
                    }
                }
            }
    )
}
