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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.math.sin

private data class BlobSpec(
    val color: Color,
    val sizeDp: Float,
    val centerX: Float, // fraction of width
    val centerY: Float, // fraction of height
    val ampXDp: Float,
    val ampYDp: Float,
    val phase: Float,
)

/**
 * Liquid-glass intro background: 6 shapes in shades of purple,
 * drifting slowly and blurred into a soft glow.
 */
@Composable
fun AnimatedPurpleBlobs(modifier: Modifier = Modifier) {
    val specs = remember {
        listOf(
            BlobSpec(Color(0xFF7C3AED), 320f, 0.20f, 0.22f, 40f, 28f, 0.0f),
            BlobSpec(Color(0xFF8B5CF6), 260f, 0.84f, 0.18f, 34f, 46f, 1.1f),
            BlobSpec(Color(0xFFA855F7), 210f, 0.70f, 0.52f, 48f, 30f, 2.3f),
            BlobSpec(Color(0xFF6D28D9), 300f, 0.16f, 0.55f, 28f, 44f, 3.4f),
            BlobSpec(Color(0xFFC084FC), 170f, 0.50f, 0.10f, 56f, 22f, 4.2f),
            BlobSpec(Color(0xFF4C1D95), 340f, 0.92f, 0.60f, 26f, 38f, 5.1f),
        )
    }

    val transition = rememberInfiniteTransition(label = "blobMotion")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
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
                    .background(color = spec.color, shape = CircleShape)
            )
        }
    }
}
