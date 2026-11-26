package com.lxzrvi.crescendo.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

/** One live glass surface morphs from a 64dp player into a circular artwork bubble. */
@Composable
internal fun MiniPlayer(
    song: Song,
    artwork: AlbumArtworkLoader,
    playing: Boolean,
    loading: Boolean,
    error: String?,
    progress: Float,
    compact: Float,
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
    val fraction = compact.coerceIn(0f, 1f)
    val radius = 20.dp + 12.dp * fraction
    val shape = remember(radius) { PrismalRoundedRectangle(radius) }
    val solidColor = navigationSolidColor(dark)
    val solidDraw: DrawScope.() -> Unit = remember(solidColor) { { drawRect(solidColor) } }
    val solidBackdrop = rememberPrismalCanvasSource(solidDraw)
    val backdrop = if (liveBlur) pageBackdrop else solidBackdrop
    val tint = if (dark) Color(0xFF121212).copy(alpha = 0.40f) else Color(0xFFFAFAFA).copy(alpha = 0.40f)
    val borderBrush = remember(dark) {
        if (dark) Brush.linearGradient(listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.20f)))
        else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.9f), Color.Black.copy(alpha = 0.16f), Color.White.copy(alpha = 0.75f)))
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
        Modifier.clip(shape)
            .hazeBlur(input = pageInput, style = legacyStyle, performanceMode = HazePerformanceMode.Performance)
            .background(tint)
    } else {
        Modifier.drawPrismalGlass(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                applyPrismalGlassEffects(
                    density = density, adaptiveLuminance = false, luminance = 0.5f,
                    blurRadiusPx = with(density) { 4.dp.toPx() },
                    refractionHeightPx = with(density) { 16.dp.toPx() },
                    refractionAmountPx = with(density) { 28.dp.toPx() },
                )
            },
            onDrawSurface = { drawRect(tint) },
        ).clip(shape)
    }
    val trackProgress = progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier.fillMaxWidth().height(64.dp).then(glass)
            .border(0.8.dp, borderBrush, shape)
            .drawWithContent {
                drawContent()
                // The playback progress hugs the upper edge of the expanded glass.
                if (fraction < 0.8f && trackProgress > 0f) {
                    val inset = radius.toPx()
                    val available = (size.width - inset * 2f).coerceAtLeast(0f)
                    drawLine(
                        color = colors.primary.copy(alpha = (1f - fraction).coerceIn(0f, 1f)),
                        start = Offset(inset, 2.dp.toPx()),
                        end = Offset(inset + available * trackProgress, 2.dp.toPx()),
                        strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round,
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val expandedAlpha = (1f - fraction * 1.8f).coerceIn(0f, 1f)
        if (expandedAlpha > 0.01f) {
            Row(
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = expandedAlpha }
                    .padding(start = 5.dp, end = 3.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SongArtwork(song, artwork, Modifier.size(50.dp).clip(CircleShape), 50.dp)
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Text(song.title, color = colors.onBackground, fontSize = 14.sp, lineHeight = 17.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(error ?: song.artist, color = if (error != null) colors.primary else colors.onBackground.copy(alpha = 0.65f),
                        fontSize = 12.sp, lineHeight = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onToggle)
                    .semantics { contentDescription = if (playing) "Pause" else "Play" }, contentAlignment = Alignment.Center) {
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = colors.primary)
                    else Icon(painterResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play),
                        contentDescription = null, tint = colors.primary, modifier = Modifier.size(23.dp))
                }
                Box(Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onNext)
                    .semantics { contentDescription = "Next song" }, contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_next), contentDescription = null,
                        tint = colors.onBackground, modifier = Modifier.size(23.dp))
                }
            }
        }
        val bubbleAlpha = ((fraction - 0.35f) / 0.65f).coerceIn(0f, 1f)
        if (bubbleAlpha > 0.01f) {
            Box(
                modifier = Modifier.size(56.dp).graphicsLayer { alpha = bubbleAlpha }
                    .clip(CircleShape).clickable(role = Role.Button, onClick = onToggle)
                    .semantics { contentDescription = "${song.title}. ${if (playing) "Pause" else "Play"}" },
                contentAlignment = Alignment.Center,
            ) {
                SongArtwork(song, artwork, Modifier.size(45.dp).clip(CircleShape), 45.dp)
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 2.dp.toPx()
                    val inset = stroke / 2f + 1.dp.toPx()
                    val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
                    drawArc(colors.onBackground.copy(alpha = 0.14f), -90f, 360f, false,
                        topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke))
                    if (trackProgress > 0f) drawArc(colors.primary, -90f, trackProgress * 360f, false,
                        topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
        }
    }
}
