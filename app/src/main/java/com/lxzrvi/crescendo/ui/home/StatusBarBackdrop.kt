package com.lxzrvi.crescendo.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

/** A live status-area blur, held at full strength over the icons and faded out below them. */
@Composable
internal fun StatusBarBackdrop(input: HazeInput.Sources, liveBlur: Boolean) {
    val background = MaterialTheme.colorScheme.background
    val statusHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // Continue beneath the icons and gently fade into the real content.
    val totalHeight = statusHeight + 32.dp
    val fadeStart = (statusHeight / totalHeight).coerceIn(0.20f, 0.80f)
    val fadeMask = remember(fadeStart) {
        Brush.verticalGradient(
            0f to Color.Black,
            fadeStart to Color.Black,
            1f to Color.Transparent,
        )
    }
    val style = remember(fadeMask) {
        HazeBlurStyle {
            blurEnabled(true)
            blurRadius(18.dp)
            noiseFactor(0f)
            // Blur the sampled page itself, not a separately colored scrim.
            backgroundColor(Color.Transparent)
            colorEffects(emptyList())
            fallbackColorEffect(HazeColorEffect.tint(Color.Transparent))
            progressive(HazeProgressive.Brush(fadeMask))
            mask(fadeMask)
        }
    }
    val solidFade = remember(background, fadeStart) {
        Brush.verticalGradient(
            0f to background.copy(alpha = 0.92f),
            fadeStart to background.copy(alpha = 0.72f),
            1f to Color.Transparent,
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(totalHeight)
            .then(
                if (liveBlur) Modifier.hazeBlur(
                    input = input,
                    style = style,
                    performanceMode = HazePerformanceMode.Quality,
                ) else Modifier.background(solidFade),
            ),
    )
}
