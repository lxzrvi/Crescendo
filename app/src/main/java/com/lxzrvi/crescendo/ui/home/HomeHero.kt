@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.lxzrvi.crescendo.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lxzrvi.crescendo.R
import com.lxzrvi.crescendo.data.AlbumArtworkLoader
import com.lxzrvi.crescendo.data.Song
import com.lxzrvi.crescendo.ui.theme.BorelFamily
import com.styropyr0.prismal.components.PrismalGlassButton
import com.styropyr0.prismal.sources.prismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalCanvasSource
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** A cover-colored ambient scene, with a feathered cover and real playback controls. */
@Composable
internal fun HomeHero(
    song: Song?,
    artwork: AlbumArtworkLoader,
    playing: Boolean,
    shuffleEnabled: Boolean,
    animate: Boolean,
    liveBlur: Boolean,
    canSwipe: Boolean,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onSwipe: (Int) -> Unit,
    quickContent: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val status = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val visual = if (song != null) rememberArtworkVisual(song, artwork, 300.dp).value else ArtworkVisual()
    val coverColor = visual.palette?.dominant?.let { Color(it) } ?: colors.primary
    val ambient by animateColorAsState(coverColor, tween(1100), label = "homeAmbientColor")
    // The moving gradient is drawn, not recomposed, each frame. The preference still turns it off.
    val phase: State<Float> = if (animate) {
        rememberInfiniteTransition(label = "homeAmbient").animateFloat(
            0f, 1f, infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
            label = "ambientDrift",
        )
    } else remember { mutableFloatStateOf(0f) }
    val sceneBackdrop = rememberPrismalGlassLayer()
    val solidColor = if (dark) Color(0xFF252529) else Color(0xFFF6F4F4)
    val solidDraw: DrawScope.() -> Unit = remember(solidColor) { { drawRect(solidColor) } }
    val solidBackdrop = rememberPrismalCanvasSource(solidDraw)
    val buttonBackdrop = if (liveBlur) sceneBackdrop else solidBackdrop
    val density = LocalDensity.current
    val threshold = with(density) { 40.dp.toPx() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val latestSwipe by rememberUpdatedState(onSwipe)
    var hour by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    LaunchedEffect(Unit) {
        while (true) {
            hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            delay(60_000)
        }
    }
    val greeting = when (hour) {
        in 5..11 -> "Good morning" to "A fresh start, one song at a time."
        in 12..16 -> "Good afternoon" to "Find a little rhythm in your day."
        in 17..20 -> "Good evening" to "Unwind to something you love."
        else -> "Good night" to "A softer soundtrack for tonight."
    }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val coverSize = (maxWidth * 0.50f).coerceIn(140.dp, 226.dp)
        val textWidth = (maxWidth - coverSize - ContentSidePadding * 2 - 4.dp).coerceAtLeast(100.dp)
        val playWidth = (textWidth - 50.dp).coerceIn(68.dp, 112.dp)
        val heroHeight = status + maxOf(330.dp, coverSize + 156.dp)
        Box(Modifier.fillMaxWidth()) {
            // The source is separate from its glass controls, so it never samples itself.
            Box(
                Modifier.matchParentSize().prismalGlassLayer(sceneBackdrop)
                    .ambientCoverGlow(ambient, colors.background, dark, phase),
            )
            Column(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(heroHeight)) {
                    Text(
                        "Crescendo", fontFamily = BorelFamily, fontStyle = FontStyle.Italic,
                        color = colors.onBackground, fontSize = 27.sp, lineHeight = 39.sp,
                        maxLines = 1, modifier = Modifier.align(Alignment.TopStart)
                            .padding(start = ContentSidePadding, top = status + 6.dp, end = 58.dp),
                    )
                    Column(
                        Modifier.align(Alignment.TopStart)
                            .padding(start = ContentSidePadding, top = status + 57.dp, end = 58.dp),
                    ) {
                        Text("${greeting.first}, Alex", color = colors.onBackground,
                            fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(greeting.second, color = colors.onBackground.copy(alpha = 0.62f),
                            fontSize = 12.sp, lineHeight = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box(
                        Modifier.align(Alignment.TopEnd)
                            .padding(top = status + 132.dp, end = ContentSidePadding - 4.dp)
                            .size(coverSize)
                            .semantics {
                                contentDescription = song?.let {
                                    "Featured cover: ${it.title}" + if (canSwipe) ". Swipe to change." else ""
                                } ?: "Featured cover"
                                if (canSwipe) customActions = listOf(
                                    CustomAccessibilityAction("Next featured song") { onSwipe(1); true },
                                    CustomAccessibilityAction("Previous featured song") { onSwipe(-1); true },
                                )
                            }
                            .pointerInput(canSwipe, rtl, threshold) {
                                var dragged = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = { dragged = 0f },
                                    onHorizontalDrag = { change, amount ->
                                        if (canSwipe) { dragged += amount; change.consume() }
                                    },
                                    onDragCancel = { dragged = 0f },
                                    onDragEnd = {
                                        if (canSwipe && abs(dragged) >= threshold) {
                                            val direction = if (dragged < 0f) 1 else -1
                                            latestSwipe(if (rtl) -direction else direction)
                                        }
                                        dragged = 0f
                                    },
                                )
                            },
                    ) {
                        AnimatedContent(
                            targetState = song,
                            transitionSpec = { fadeIn(tween(700)) togetherWith fadeOut(tween(700)) },
                            modifier = Modifier.fillMaxSize().softArtworkEdges(), label = "featuredArtwork",
                        ) { featured ->
                            if (featured != null) {
                                val image by rememberArtworkVisual(featured, artwork, 300.dp)
                                ArtworkImage(image.bitmap, Modifier.fillMaxSize())
                            }
                        }
                    }
                    Box(
                        Modifier.align(Alignment.TopStart)
                            .padding(start = ContentSidePadding, top = status + 139.dp)
                            .width(textWidth).height(126.dp),
                    ) {
                        AnimatedContent(
                            targetState = song,
                            transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(600)) },
                            label = "featuredDetails",
                        ) { featured ->
                            if (featured != null) {
                                Column {
                                    Text("TOP PICK", color = colors.primary, fontSize = 10.sp,
                                        letterSpacing = 1.3.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(7.dp))
                                    Text(featured.title, color = colors.onBackground, fontSize = 19.sp,
                                        lineHeight = 22.sp, fontWeight = FontWeight.Bold,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(4.dp))
                                    Text(featured.artist, color = colors.onBackground.copy(alpha = 0.72f),
                                        fontSize = 13.sp, lineHeight = 16.sp, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis)
                                    val meta = listOfNotNull(featured.genre, formatSongDuration(featured.durationMs)).joinToString(" · ")
                                    if (meta.isNotEmpty()) Text(meta, color = colors.onBackground.copy(alpha = 0.50f),
                                        fontSize = 11.sp, lineHeight = 15.sp, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
                                }
                            }
                        }
                    }
                    if (song != null) {
                        Row(
                            Modifier.align(Alignment.TopStart)
                                .padding(start = ContentSidePadding, top = status + 274.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PrismalGlassButton(
                                onClick = onPlay, backdrop = buttonBackdrop,
                                modifier = Modifier.width(playWidth).height(42.dp)
                                    .semantics { contentDescription = if (playing) "Pause ${song.title}" else "Play ${song.title}" },
                                height = 42.dp, contentPadding = PaddingValues(horizontal = 4.dp), blurRadius = 8.dp,
                                surfaceColor = if (dark) Color.White.copy(alpha = 0.19f) else Color.White.copy(alpha = 0.48f),
                            ) {
                                Icon(painterResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play),
                                    contentDescription = null, tint = colors.onBackground, modifier = Modifier.size(18.dp))
                                Text(if (playing) "Pause" else "Play", color = colors.onBackground,
                                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                            PrismalGlassButton(
                                onClick = onShuffle, backdrop = buttonBackdrop, modifier = Modifier.size(42.dp)
                                    .semantics { contentDescription = if (shuffleEnabled) "Turn shuffle off" else "Shuffle songs" },
                                height = 42.dp, contentPadding = PaddingValues(0.dp), blurRadius = 8.dp,
                                tint = if (shuffleEnabled) colors.primary else Color.Unspecified, tintAlpha = 0.38f,
                                surfaceColor = if (dark) Color.White.copy(alpha = 0.13f) else Color.White.copy(alpha = 0.38f),
                            ) {
                                Icon(painterResource(R.drawable.ic_shuffle), contentDescription = null,
                                    tint = colors.onBackground, modifier = Modifier.size(19.dp))
                            }
                        }
                    }
                }
                quickContent()
            }
        }
    }
}

/** Two destination-in masks fade all four edges without a cover border. */
private fun Modifier.softArtworkEdges(): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
        drawContent()
        drawRect(
            Brush.horizontalGradient(0f to Color.Transparent, 0.13f to Color.Black,
                0.87f to Color.Black, 1f to Color.Transparent),
            blendMode = BlendMode.DstIn,
        )
        drawRect(
            Brush.verticalGradient(0f to Color.Transparent, 0.13f to Color.Black,
                0.87f to Color.Black, 1f to Color.Transparent),
            blendMode = BlendMode.DstIn,
        )
    }

/** Radial gradients are already soft, avoiding a large animated blur layer. */
private fun Modifier.ambientCoverGlow(
    color: Color,
    base: Color,
    dark: Boolean,
    phase: State<Float>,
): Modifier = drawWithCache {
    val glow = Brush.radialGradient(
        0f to color.copy(alpha = if (dark) 0.34f else 0.23f),
        0.48f to color.copy(alpha = if (dark) 0.12f else 0.08f),
        1f to Color.Transparent,
        center = Offset(size.width * 0.82f, size.width * 0.50f),
        radius = (size.width * 0.90f).coerceAtLeast(1f),
    )
    onDrawBehind {
        drawRect(base.copy(alpha = 0.96f))
        val drift = sin(phase.value * 2f * PI.toFloat())
        translate(left = drift * size.width * 0.025f, top = drift * size.width * 0.018f) {
            drawRect(glow)
        }
    }
}
