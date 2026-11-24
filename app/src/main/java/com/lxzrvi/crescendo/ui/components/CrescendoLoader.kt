package com.lxzrvi.crescendo.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

// Crescendo_main_icon.svg ka path (viewBox -450 -450 900 900)
private const val ICON_PATH =
    "M0 -435 A135 135 0 0 1 0 -165 A165 165 0 1 0 165 0 " +
        "A135 135 0 1 1 300 135 A165 165 0 0 0 135 300 " +
        "A135 135 0 0 1 -135 300 A165 165 0 0 0 -300 135 " +
        "A135 135 0 0 1 -300 -135 A165 165 0 0 0 -135 -300 " +
        "A135 135 0 0 1 0 -435 Z"

@Composable
fun CrescendoLoader(
    modifier: Modifier = Modifier.size(72.dp),
    color: Color = Color.White,
    durationMillis: Int = 2200,   // ek poora chakkar
    strokeWidth: Float = 18f,     // icon units mein (900 = poori width)
) {
    val transition = rememberInfiniteTransition(label = "crescendoLoader")
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing)),
        label = "progress",
    )
    val breath = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1100, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "breath",
    )

    Box(
        modifier.drawWithCache {
            val unit = size.minDimension / 900f
            val iconPath = PathParser().parsePathString(ICON_PATH).toPath()
            val measure = PathMeasure().apply { setPath(iconPath, false) }
            val len = measure.length
            val seg = Path()

            val slices = 22
            val tailLen = len * 0.30f
            val sliceLen = tailLen / slices

            onDrawBehind {
                val p = progress.value
                val b = breath.value
                // Comet ki speed halki upar-neeche (kabhi ruke nahi): derivative 0.4x..1.6x
                val headFrac = p - 0.6f * sin(2f * PI.toFloat() * p) / (2f * PI.toFloat())
                val head = headFrac * len

                fun slice(startD: Float, endD: Float, alpha: Float, width: Float) {
                    val s = ((startD % len) + len) % len
                    val e = s + (endD - startD)
                    seg.rewind()
                    if (e <= len) {
                        measure.getSegment(s, e, seg, true)
                    } else {                       // end/start wrap hota hai
                        measure.getSegment(s, len, seg, true)
                        measure.getSegment(0f, e - len, seg, true)
                    }
                    drawPath(
                        path = seg,
                        color = color.copy(alpha = alpha),
                        style = Stroke(width = width, cap = StrokeCap.Round),
                    )
                }

                withTransform({
                    translate(size.width / 2f, size.height / 2f)
                    val s = unit * (0.96f + 0.04f * b)       // halka breathing
                    scale(s, s, pivot = Offset.Zero)
                }) {
                    // Soft fill
                    drawPath(iconPath, color.copy(alpha = 0.10f + 0.06f * b), style = Fill)

                    // Comet: head se peeche tak fade (glow + sharp line)
                    for (i in 0 until slices) {
                        val f = 1f - i / slices.toFloat()
                        val a = f * f
                        val endD = head - i * sliceLen
                        val startD = endD - sliceLen * 1.2f   // halka overlap, gap na dikhe
                        slice(startD, endD, a * 0.14f, strokeWidth * 3.2f)  // glow
                        slice(startD, endD, a, strokeWidth)                 // line
                    }
                }
            }
        }
    )
}
