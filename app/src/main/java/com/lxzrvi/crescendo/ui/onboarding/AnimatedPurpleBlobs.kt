package com.lxzrvi.crescendo.ui.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lxzrvi.crescendo.ui.theme.Blush
import com.lxzrvi.crescendo.ui.theme.NeonRed
import com.lxzrvi.crescendo.ui.theme.RedDeep
import com.lxzrvi.crescendo.ui.theme.RedSoft
import kotlin.math.roundToInt
import kotlin.math.sin

private data class BlobSpec(
    val colors: List<Color>, // gradient mix for the shape
    val alpha: Float,
    val sizeDp: Float,
    val centerX: Float,  // fraction of width (spread across the whole top)
    val centerY: Float,  // fraction of height
    val ampXDp: Float,
    val ampYDp: Float,
    val phase: Float,
)

/**
 * Liquid-glass intro background: soft shapes spread across the top,
 * mixed from a red + white palette, heavily blurred and blended into
 * the black screen. Everything drifts extremely slowly.
 */
@Composable
fun AnimatedPurpleBlobs(modifier: Modifier = Modifier) {
    val specs = remember {
        listOf(
            BlobSpec(listOf(NeonRed, RedSoft), 0.9f, 320f, 0.14f, 0.22f, 36f, 28f, 0.0f),
            BlobSpec(listOf(RedDeep, NeonRed), 0.85f, 270f, 0.86f, 0.16f, 32f, 44f, 1.2f),
            BlobSpec(listOf(Blush, Color.White.copy(alpha = 0.75f)), 0.55f, 200f, 0.70f, 0.52f, 44f, 30f, 2.4f),
            BlobSpec(listOf(NeonRed, Blush), 0.6f, 260f, 0.20f, 0.55f, 26f, 40f, 3.5f),
            BlobSpec(listOf(RedSoft, Color.White.copy(alpha = 0.6f)), 0.5f, 180f, 0.52f, 0.08f, 52f, 22f, 4.3f),
            BlobSpec(listOf(RedDeep, RedSoft), 0.75f, 300f, 0.94f, 0.48f, 24f, 34f, 5.2f),
        )
    }

    val transition = rememberInfiniteTransition(label = "blobMotion")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "blobProgress",
    )

    BoxWithConstraints(modifier = modifier) {
        val maxW = constraints.maxWidth.toFloat()
        val maxH = constraints.maxHeight.toFloat()
        val density = LocalDensity.current

        specs.forEach { spec ->
            val angle = (progress * 2.0 * Math.PI + spec.phase).toFloat()
            val dx = sin(angle) * with(density) { spec.ampXDp.dp.toPx() }
            val dy = sin(angle * 2f) * with(density) { spec.ampYDp.dp.toPx() }

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (spec.centerX * maxW + dx).roundToInt(),
                            y = (spec.centerY * maxH + dy).roundToInt(),
                        )
                    }
                    .size(spec.sizeDp.dp)
                    .blur(radius = 72.dp)
                    .background(
                        brush = Brush.linearGradient(spec.colors),
                        shape = CircleShape,
                        alpha = spec.alpha,
                    )
            )
        }
    }
}
