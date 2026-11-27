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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lxzrvi.crescendo.R
import com.lxzrvi.crescendo.data.AlbumArtworkLoader
import com.lxzrvi.crescendo.data.Song
import com.lxzrvi.crescendo.ui.theme.BorelFamily
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** The cover, opaque palette scene and soft lower fade form one continuous hero. */
@Composable
internal fun HomeHero(
    song: Song?,
    artwork: AlbumArtworkLoader,
    playing: Boolean,
    shuffleEnabled: Boolean,
    animate: Boolean,
    canSwipe: Boolean,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onSwipe: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val status = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val visual = if (song != null) rememberArtworkVisual(song, artwork, 300.dp).value else ArtworkVisual()
    val swatches = visual.palette?.swatches.orEmpty().map { Color(it) }
    val fallback = colors.surfaceVariant
    val first by animateColorAsState(swatches.getOrNull(0) ?: fallback, tween(1500), label = "coverDominant")
    val second by animateColorAsState(swatches.getOrNull(1) ?: first, tween(1500), label = "coverSecond")
    val third by animateColorAsState(swatches.getOrNull(2) ?: first, tween(1500), label = "coverThird")
    val fourth by animateColorAsState(swatches.getOrNull(3) ?: second, tween(1500), label = "coverFourth")
    val fifth by animateColorAsState(swatches.getOrNull(4) ?: third, tween(1500), label = "coverFifth")
    val palette = listOf(first, second, third, fourth, fifth)
    val phase: State<Float> = if (animate) {
        rememberInfiniteTransition(label = "coverBlobs").animateFloat(
            0f, 1f, animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Restart),
            label = "drift",
        )
    } else remember { mutableFloatStateOf(0f) }
    val dark = colors.background.luminance() < 0.5f
    // The photo/background is completely covered until the final, gradual bottom fade.
    val base = lerp(colors.background, first, if (dark) 0.30f else 0.20f).copy(alpha = 1f)
    val titleSource = swatches.maxByOrNull { maxOf(it.red, it.green, it.blue) - minOf(it.red, it.green, it.blue) } ?: first
    val wordmark by animateColorAsState(visiblePaletteInk(titleSource, base, dark), tween(1500), label = "wordmarkColor")
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 40.dp.toPx() }
    val latestSwipe by rememberUpdatedState(onSwipe)
    val seeds = remember(song?.id) { makeBlobSpecs(song?.id.orEmpty()) }
    val heroFade = remember {
        Brush.verticalGradient(0f to Color.Black, 0.76f to Color.Black,
            0.90f to Color.Black.copy(alpha = 0.58f), 1f to Color.Transparent)
    }
    val horizontalCoverMask = remember {
        Brush.horizontalGradient(0f to Color.Transparent, 0.15f to Color.Black,
            0.85f to Color.Black, 1f to Color.Transparent)
    }
    val verticalCoverMask = remember {
        Brush.verticalGradient(0f to Color.Transparent, 0.15f to Color.Black,
            0.84f to Color.Black, 1f to Color.Transparent)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val coverSize = (maxWidth * 0.60f).coerceIn(165.dp, 270.dp)
        val sceneHeight = status + maxOf(360.dp, coverSize + 150.dp)
        val leftWidth = (maxWidth - coverSize + 18.dp).coerceAtLeast(112.dp)
        Box(
            modifier = Modifier.fillMaxWidth().height(sceneHeight)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(heroFade, blendMode = BlendMode.DstIn)
                },
        ) {
            // Keep a solid cover-derived color behind the blended shapes, not the app photo.
            Box(Modifier.fillMaxSize().background(base))
            Box(
                Modifier.fillMaxSize()
                    .blur(34.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .paletteBlobs(palette, seeds, dark, phase),
            )
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(
                0f to base.copy(alpha = 0.62f), 0.37f to base.copy(alpha = 0.32f), 0.72f to Color.Transparent,
            )))

            Text(
                "Crescendo", fontFamily = BorelFamily, fontSize = 28.sp, lineHeight = 42.sp,
                color = wordmark, maxLines = 1,
                modifier = Modifier.align(Alignment.TopStart)
                    .padding(start = ContentSidePadding, top = status + 10.dp, end = 58.dp),
            )

            Box(
                Modifier.align(Alignment.TopEnd).padding(top = status + 70.dp, end = ContentSidePadding)
                    .size(coverSize)
                    .semantics {
                        contentDescription = song?.let {
                            "Featured cover: ${it.title}" + if (canSwipe) ". Swipe to change." else ""
                        } ?: "Featured cover"
                        if (canSwipe) customActions = listOf(
                            CustomAccessibilityAction("Next featured cover") { onSwipe(1); true },
                            CustomAccessibilityAction("Previous featured cover") { onSwipe(-1); true },
                        )
                    }
                    .pointerInput(canSwipe, rtl, swipeThreshold) {
                        var dragged = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { dragged = 0f },
                            onHorizontalDrag = { change, amount ->
                                if (canSwipe) { dragged += amount; change.consume() }
                            },
                            onDragCancel = { dragged = 0f },
                            onDragEnd = {
                                if (canSwipe && abs(dragged) >= swipeThreshold) {
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
                    transitionSpec = {
                        (fadeIn(tween(1500)) + scaleIn(tween(1500), initialScale = 0.96f)) togetherWith
                            (fadeOut(tween(1500)) + scaleOut(tween(1500), targetScale = 1.025f))
                    },
                    modifier = Modifier.fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            // Two masks feather all four sides of the artwork into its own colors.
                            drawRect(horizontalCoverMask, blendMode = BlendMode.DstIn)
                            drawRect(verticalCoverMask, blendMode = BlendMode.DstIn)
                        },
                    label = "featuredCoverMorph",
                ) { featured ->
                    if (featured != null) {
                        val image by rememberArtworkVisual(featured, artwork, 300.dp)
                        ArtworkImage(image.bitmap, Modifier.fillMaxSize())
                    }
                }
            }
            if (song != null) {
                Column(
                    modifier = Modifier.align(Alignment.TopStart)
                        .padding(start = ContentSidePadding, top = status + 93.dp)
                        .width(leftWidth),
                ) {
                    // A fixed text slot keeps Play and Shuffle anchored despite long titles.
                    Column(Modifier.height(84.dp)) {
                        Text(song.title, color = colors.onBackground, fontSize = 20.sp, lineHeight = 23.sp,
                            fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Text(song.artist, color = colors.onBackground.copy(alpha = 0.74f), fontSize = 13.sp,
                            lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(9.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(49.dp).clip(CircleShape)
                                .background(colors.onBackground.copy(alpha = if (dark) 0.92f else 0.86f))
                                .clickable(role = Role.Button, onClick = onPlay)
                                .semantics { contentDescription = if (playing) "Pause ${song.title}" else "Play ${song.title}" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(painterResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play),
                                contentDescription = null, tint = colors.background, modifier = Modifier.size(22.dp))
                        }
                        Box(
                            modifier = Modifier.size(43.dp).clip(CircleShape)
                                .background(if (shuffleEnabled) colors.primary else colors.onBackground.copy(alpha = 0.14f))
                                .clickable(role = Role.Button, onClick = onShuffle)
                                .semantics { contentDescription = if (shuffleEnabled) "Turn shuffle off" else "Shuffle songs" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(painterResource(R.drawable.ic_shuffle), contentDescription = null,
                                tint = if (shuffleEnabled) colors.onPrimary else colors.onBackground,
                                modifier = Modifier.size(21.dp))
                        }
                    }
                }
            }
        }
    }
}

/** A solid, readable ink with the current cover's hue rather than a fixed theme color. */
private fun visiblePaletteInk(source: Color, background: Color, dark: Boolean): Color {
    val ink = if (dark) Color.White else Color.Black
    var candidate = lerp(source, ink, 0.48f)
    repeat(8) {
        val a = candidate.luminance()
        val b = background.luminance()
        if ((maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f) >= 4.5f) return candidate
        candidate = lerp(candidate, ink, 0.22f)
    }
    return candidate
}

private data class BlobSpec(val x: Float, val y: Float, val rx: Float, val ry: Float, val skew: Float)

/** New organic outlines per song; stable during recomposition and animation. */
private fun makeBlobSpecs(seed: String): List<BlobSpec> {
    val random = Random(seed.hashCode())
    val anchors = listOf(0.11f to 0.22f, 0.69f to 0.10f, 0.39f to 0.52f, 0.89f to 0.58f, 0.35f to 0.85f)
    return anchors.map { (x, y) ->
        BlobSpec(x + random.nextFloat() * 0.12f - 0.06f, y + random.nextFloat() * 0.12f - 0.06f,
            0.32f + random.nextFloat() * 0.20f, 0.27f + random.nextFloat() * 0.20f,
            0.42f + random.nextFloat() * 0.38f)
    }
}

private fun Modifier.paletteBlobs(
    colors: List<Color>,
    specs: List<BlobSpec>,
    dark: Boolean,
    phase: State<Float>,
): Modifier = drawWithCache {
    val outlines = specs.mapIndexed { index, spec ->
        val x = size.width * spec.x
        val y = size.height * spec.y
        val rx = size.width * spec.rx
        val ry = size.height * spec.ry
        val k = spec.skew
        val path = Path().apply {
            moveTo(x + rx, y)
            cubicTo(x + rx, y + ry * k, x + rx * k, y + ry, x, y + ry)
            cubicTo(x - rx * (1f - k * 0.25f), y + ry, x - rx, y + ry * k, x - rx, y)
            cubicTo(x - rx, y - ry * (1f - k * 0.2f), x - rx * k, y - ry, x, y - ry)
            cubicTo(x + rx * (1f - k * 0.2f), y - ry, x + rx, y - ry * k, x + rx, y)
            close()
        }
        val color = colors[index % colors.size]
        val opacity = if (dark) 0.82f else 0.50f
        val brush = Brush.radialGradient(
            0f to color.copy(alpha = opacity),
            0.56f to color.copy(alpha = opacity * 0.50f),
            1f to Color.Transparent,
            center = Offset(x, y), radius = maxOf(rx, ry).coerceAtLeast(1f) * 1.05f,
        )
        path to brush
    }
    onDrawBehind {
        val t = phase.value * 2f * PI.toFloat()
        outlines.forEachIndexed { index, (path, brush) ->
            translate(size.width * 0.025f * sin(t + index), size.height * 0.025f * cos(t + index)) {
                drawPath(path, brush)
            }
        }
    }
}
