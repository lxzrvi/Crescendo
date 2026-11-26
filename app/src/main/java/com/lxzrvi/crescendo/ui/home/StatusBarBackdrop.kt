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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

/** A live status-area blur, held at full strength over the icons and faded out below them. */
@Composable
internal fun StatusBarBackdrop(input: HazeInput.Sources, liveBlur: Boolean) {
    val background = MaterialTheme.colorScheme.background
    val dark = background.luminance() < 0.5f
    val statusHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val totalHeight = statusHeight + 24.dp
    val fadeStart = (statusHeight / totalHeight).coerceIn(0.20f, 0.80f)
    val fadeMask = remember(fadeStart) {
        Brush.verticalGradient(
            0f to Color.Black,
            fadeStart to Color.Black,
            1f to Color.Transparent,
        )
    }
    val style = remember(background, dark, fadeMask) {
        HazeBlurStyle {
            blurEnabled(true)
            blurRadius(12.dp)
            noiseFactor(0f)
            backgroundColor(background)
            colorEffects(
                listOf(HazeColorEffect.tint(navigationSolidColor(dark).copy(alpha = 0.22f))),
            )
            fallbackColorEffect(HazeColorEffect.tint(navigationSolidColor(dark)))
            progressive(HazeProgressive.Brush(fadeMask))
            mask(fadeMask)
        }
    }
    val solidFade = remember(dark, fadeStart) {
        Brush.verticalGradient(
            0f to navigationSolidColor(dark),
            fadeStart to navigationSolidColor(dark),
            1f to Color.Transparent,
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(totalHeight)
            .then(if (liveBlur) Modifier.hazeBlur(input = input, style = style) else Modifier.background(solidFade)),
    )
}
