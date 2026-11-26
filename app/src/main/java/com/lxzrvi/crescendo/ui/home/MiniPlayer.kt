package com.lxzrvi.crescendo.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lxzrvi.crescendo.R
import com.lxzrvi.crescendo.data.AlbumArtworkLoader
import com.lxzrvi.crescendo.data.Song
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.PrismalGlass
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import com.styropyr0.prismal.shapes.PrismalRoundedRectangle
import com.styropyr0.prismal.sources.rememberPrismalCanvasSource
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

/** Floating controls sample the actual scrolling page, like the navigation bar. */
@Composable
internal fun MiniPlayer(
    song: Song,
    artwork: AlbumArtworkLoader,
    playing: Boolean,
    loading: Boolean,
    error: String?,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    pageBackdrop: PrismalBackdrop,
    pageInput: HazeInput.Sources,
    liveBlur: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val density = LocalDensity.current
    val shape = remember { PrismalRoundedRectangle(20.dp) }
    val solidColor = navigationSolidColor(dark)
    val solidDraw: DrawScope.() -> Unit = remember(solidColor) { { drawRect(solidColor) } }
    val solidBackdrop = rememberPrismalCanvasSource(solidDraw)
    val backdrop = if (liveBlur) pageBackdrop else solidBackdrop
    val tint = if (dark) Color(0xFF121212).copy(alpha = 0.40f) else Color(0xFFFAFAFA).copy(alpha = 0.40f)
    val borderBrush = remember(dark) {
        if (dark) Brush.linearGradient(listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.09f), Color.White.copy(alpha = 0.20f)))
        else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.90f), Color.Black.copy(alpha = 0.18f), Color.White.copy(alpha = 0.75f)))
    }
    val legacyStyle = remember {
        HazeBlurStyle {
            blurEnabled(true)
            blurRadius(10.dp)
            backgroundColor(Color.Transparent)
            noiseFactor(0f)
            colorEffects(emptyList())
            fallbackColorEffect(HazeColorEffect.tint(Color.Transparent))
        }
    }
    val glass = if (liveBlur && !PrismalGlass.supportsBackdropBlur) {
        // On Android 11 and below, Haze supplies the async live-blur fallback.
        Modifier
            .clip(shape)
            .hazeBlur(input = pageInput, style = legacyStyle, performanceMode = HazePerformanceMode.Performance)
            .background(tint)
    } else {
        Modifier
            .drawPrismalGlass(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    applyPrismalGlassEffects(
                        density = density,
                        adaptiveLuminance = false,
                        luminance = 0.5f,
                        blurRadiusPx = with(density) { 4.dp.toPx() },
                        refractionHeightPx = with(density) { 16.dp.toPx() },
                        refractionAmountPx = with(density) { 28.dp.toPx() },
                    )
                },
                onDrawSurface = { drawRect(tint) },
            )
            .clip(shape)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .then(glass)
            .border(0.8.dp, borderBrush, shape)
            .padding(start = 8.dp, end = 4.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SongArtwork(song, artwork, Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)), 50.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = colors.onBackground,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                error ?: song.artist,
                color = if (error != null) colors.primary else colors.onBackground.copy(alpha = 0.65f),
                fontSize = 12.sp,
                lineHeight = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onToggle, modifier = Modifier.size(42.dp)) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = colors.primary)
            } else {
                Icon(
                    painter = painterResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play),
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = colors.primary,
                    modifier = Modifier.size(23.dp),
                )
            }
        }
        IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_next),
                contentDescription = "Next song",
                tint = colors.onBackground,
                modifier = Modifier.size(23.dp),
            )
        }
    }
}
