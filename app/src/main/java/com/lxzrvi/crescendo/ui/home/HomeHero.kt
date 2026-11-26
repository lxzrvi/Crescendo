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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
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
import kotlin.math.cos
import kotlin.math.sin

/** Full-bleed artwork colors reach behind the wordmark and fade into the page. */
@Composable
internal fun HomeHero(
    song: Song?,
    artwork: AlbumArtworkLoader,
    playing: Boolean,
    shuffleEnabled: Boolean,
    animate: Boolean,
    photoBehind: Boolean,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onProfile: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val status = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val visual = if (song != null) rememberArtworkVisual(song, artwork, 300.dp).value else ArtworkVisual()
    val swatches = visual.palette?.swatches.orEmpty()
    val fallback = colors.surfaceVariant
    val first by animateColorAsState(swatches.getOrNull(0)?.let { Color(it) } ?: fallback, tween(950), label = "coverDominant")
    val second by animateColorAsState(swatches.getOrNull(1)?.let { Color(it) } ?: first, tween(950), label = "coverSecondary")
    val third by animateColorAsState(swatches.getOrNull(2)?.let { Color(it) } ?: first, tween(950), label = "coverThird")
    val phase: State<Float> = if (animate) {
        rememberInfiniteTransition(label = "coverBlobs").animateFloat(
            0f, 1f,
            animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
            label = "drift",
        )
    } else remember { mutableFloatStateOf(0f) }
    val dark = colors.background.luminance() < 0.5f
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val coverSize = (maxWidth * 0.60f).coerceIn(165.dp, 270.dp)
        val sceneHeight = status + maxOf(320.dp, coverSize + 95.dp)
        val leftWidth = (maxWidth - coverSize + 18.dp).coerceAtLeast(120.dp)
        Box(
            modifier = Modifier.fillMaxWidth().height(sceneHeight)
                .coverColorBlobs(first, second, third, dark, photoBehind, phase),
        ) {
            Row(
                modifier = Modifier.align(Alignment.TopStart)
                    .padding(start = ContentSidePadding, top = status + 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Crescendo", fontFamily = BorelFamily, fontSize = 28.sp, lineHeight = 42.sp,
                    color = colors.onBackground, maxLines = 1)
                Spacer(Modifier.width(7.dp))
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(colors.onBackground.copy(alpha = 0.08f))
                        .clickable(role = Role.Button, onClick = onProfile)
                        .semantics { contentDescription = "Profile" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_profile), contentDescription = null,
                        tint = colors.onBackground, modifier = Modifier.size(23.dp))
                }
            }

            val mask = remember(rtl) {
                if (rtl) Brush.horizontalGradient(0f to Color.Black, 0.78f to Color.Black, 1f to Color.Transparent)
                else Brush.horizontalGradient(0f to Color.Transparent, 0.22f to Color.Black, 1f to Color.Black)
            }
            Box(
                Modifier.align(Alignment.TopEnd).padding(top = status + 70.dp, end = ContentSidePadding)
                    .size(coverSize),
            ) {
                AnimatedContent(
                    targetState = song,
                    transitionSpec = {
                        (fadeIn(tween(760)) + scaleIn(tween(760), initialScale = 0.93f)) togetherWith
                            (fadeOut(tween(760)) + scaleOut(tween(760), targetScale = 1.035f))
                    },
                    modifier = Modifier.fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent { drawContent(); drawRect(mask, blendMode = BlendMode.DstIn) }
                        .clip(RoundedCornerShape(26.dp)),
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
                    Text(song.title, color = colors.onBackground, fontSize = 20.sp, lineHeight = 23.sp,
                        fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(song.artist, color = colors.onBackground.copy(alpha = 0.72f), fontSize = 13.sp,
                        lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(49.dp).clip(CircleShape)
                                .background(colors.onBackground.copy(alpha = if (dark) 0.91f else 0.86f))
                                .clickable(role = Role.Button, onClick = onPlay)
                                .semantics { contentDescription = if (playing) "Pause ${song.title}" else "Play ${song.title}" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(painterResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play),
                                contentDescription = null, tint = colors.background, modifier = Modifier.size(22.dp))
                        }
                        Box(
                            modifier = Modifier.size(43.dp).clip(CircleShape)
                                .background(if (shuffleEnabled) colors.primary else colors.onBackground.copy(alpha = 0.12f))
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

private fun Modifier.coverColorBlobs(
    first: Color,
    second: Color,
    third: Color,
    dark: Boolean,
    photoBehind: Boolean,
    phase: State<Float>,
): Modifier = drawWithCache {
    val alpha = if (dark) 0.62f else 0.30f
    val radiusA = size.width * 0.84f
    val radiusB = size.width * 0.66f
    val radiusC = size.width * 0.74f
    fun blob(color: Color, radius: Float, opacity: Float) = Brush.radialGradient(
        0f to color.copy(alpha = opacity), 0.38f to color.copy(alpha = opacity * 0.52f),
        1f to Color.Transparent, center = Offset.Zero, radius = radius.coerceAtLeast(1f),
    )
    val a = blob(first, radiusA, alpha)
    val b = blob(second, radiusB, alpha * 0.78f)
    val c = blob(third, radiusC, alpha * 0.64f)
    val bottomFade = Brush.verticalGradient(
        0f to Color.Transparent,
        0.72f to Color.Transparent,
        1f to (if (dark) Color.Black else Color.White).copy(alpha = if (photoBehind) 0.12f else 0.34f),
    )
    onDrawBehind {
        val t = phase.value * 2f * PI.toFloat()
        translate(size.width * (0.14f + 0.035f * sin(t)), size.height * (0.21f + 0.04f * cos(t))) {
            drawCircle(a, radiusA, center = Offset.Zero)
        }
        translate(size.width * (0.70f + 0.04f * cos(t)), size.height * (0.06f + 0.04f * sin(t))) {
            drawCircle(b, radiusB, center = Offset.Zero)
        }
        translate(size.width * (0.40f + 0.04f * sin(t + 1.5f)), size.height * 0.55f) {
            drawCircle(c, radiusC, center = Offset.Zero)
        }
        drawRect(bottomFade)
    }
}
