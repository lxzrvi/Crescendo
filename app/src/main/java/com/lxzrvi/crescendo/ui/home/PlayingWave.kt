package com.lxzrvi.crescendo.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.PI
import kotlin.math.sin

/** Lightweight animated equalizer for the track that is actually playing. */
@Composable
internal fun PlayingWave(color: Color, modifier: Modifier = Modifier) {
    val phase = rememberInfiniteTransition(label = "playingWave").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "wavePhase",
    )
    Canvas(modifier.semantics { contentDescription = "Playing" }) {
        val t = phase.value * 2f * PI.toFloat()
        val stroke = size.width / 8f
        repeat(4) { index ->
            val height = size.height * (0.25f + 0.70f * (0.5f + 0.5f * sin(t + index * 1.35f)))
            val x = size.width * (0.14f + index * 0.24f)
            drawLine(color, Offset(x, (size.height - height) / 2f),
                Offset(x, (size.height + height) / 2f), strokeWidth = stroke, cap = StrokeCap.Round)
        }
    }
}
