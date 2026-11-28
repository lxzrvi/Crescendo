@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.lxzrvi.crescendo.ui.home

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
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
import com.lxzrvi.crescendo.data.ArtworkEdgePalette
import com.lxzrvi.crescendo.data.Song
import com.lxzrvi.crescendo.ui.theme.BorelFamily
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeSourceSelection
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

private const val HERO_SOURCE_KEY = "hero-scene"
private const val HERO_FADE_MS = 1350

private data class HeroCover(val song: Song, val bitmap: Bitmap?, val edges: ArtworkEdgePalette?)

/** The cover, its artwork-derived ambience and text change together without an empty placeholder. */
@Composable
internal fun HomeHero(
    song: Song?,
    artwork: AlbumArtworkLoader,
    currentSong: Song?,
    isPlaying: Boolean,
    profileName: String,
    greetingSeed: Long,
    shuffleEnabled: Boolean,
    animate: Boolean,
    liveBlur: Boolean,
    canSwipe: Boolean,
    onPlay: (Song) -> Unit,
    onShuffle: () -> Unit,
    onSwipe: (Int) -> Unit,
    quickContent: @Composable (HazeInput.Sources) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val status = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val heroPixels = with(LocalDensity.current) { 300.dp.roundToPx() }
    var cover by remember { mutableStateOf<HeroCover?>(null) }
    LaunchedEffect(song, artwork, heroPixels) {
        if (song == null) {
            cover = null
        } else {
            // Keep the previous cover, labels and color until all of the next one is ready.
            val bitmap = artwork.load(song, heroPixels)
            val edges = bitmap?.let { artwork.edgePalette(song, it) }
            cover = HeroCover(song, bitmap, edges)
        }
    }
    val edges = cover?.edges
    val mood = remember(edges, colors.primary, dark) {
        heroColors(edges?.swatches.orEmpty(), edges?.bottom, colors.primary.toArgb(), dark)
    }
    val colorSpec = tween<Color>(HERO_FADE_MS, easing = FastOutSlowInEasing)
    // Read background colors in drawing, and the wordmark color in its own
    // small composable, instead of recomposing the whole hero on every frame.
    val ambientMain = animateColorAsState(Color(mood.main), colorSpec, label = "heroCoverMain")
    val ambientSecond = animateColorAsState(Color(mood.second), colorSpec, label = "heroCoverSecond")
    val ambientLower = animateColorAsState(Color(mood.lower), colorSpec, label = "heroCoverLower")
    val titleColor = animateColorAsState(Color(mood.title), colorSpec, label = "heroCoverTitle")
    val phase = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animate) {
        if (!animate) phase.floatValue = 0f else while (true) {
            // This very slow drift moves less than a pixel per tick; sampling it
            // at 12.5 Hz avoids constantly recapturing the whole glass backdrop.
            phase.floatValue = (SystemClock.elapsedRealtime() % 24_000L) / 24_000f
            delay(80L)
        }
    }

    val heroHaze = rememberHazeState()
    val heroInput = remember(heroHaze) {
        HazeInput.Sources(state = heroHaze, selection = HazeSourceSelection.All.where { it.key == HERO_SOURCE_KEY })
    }
    val buttonBlurStyle = remember {
        HazeBlurStyle {
            blurEnabled(true)
            blurRadius(16.dp)
            backgroundColor(Color.Transparent)
            noiseFactor(0f)
            colorEffects(emptyList())
            fallbackColorEffect(HazeColorEffect.tint(Color.Transparent))
        }
    }
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
    val greeting = remember(hour, greetingSeed) {
        val choices = homeGreetings(hour)
        choices[Random(greetingSeed xor hour.toLong()).nextInt(choices.size)]
    }
    val displayedSong = cover?.song
    val playing = isPlaying && displayedSong?.id == currentSong?.id

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // A little larger and higher, with the right side bleeding to the screen edge.
        val coverSize = (maxWidth * 0.54f).coerceIn(148.dp, 242.dp)
        val textWidth = (maxWidth - coverSize - ContentSidePadding * 2 - 4.dp).coerceAtLeast(100.dp)
        val playWidth = (textWidth - 50.dp).coerceIn(68.dp, 112.dp)
        val heroHeight = status + maxOf(330.dp, coverSize + 138.dp)
        Box(Modifier.fillMaxWidth()) {
            // Only the background and cover are captured. The buttons never sample themselves.
            Box(
                Modifier.matchParentSize()
                    .then(if (liveBlur) Modifier.hazeSource(state = heroHaze, zIndex = 0f, key = HERO_SOURCE_KEY) else Modifier)
                    .ambientCoverGlow(ambientMain, ambientSecond, ambientLower, phase),
            ) {
                Box(
                    Modifier.align(Alignment.TopEnd)
                        .padding(top = status + 112.dp)
                        .size(coverSize)
                        .semantics {
                            contentDescription = displayedSong?.let {
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
                        targetState = cover,
                        transitionSpec = {
                            fadeIn(tween(HERO_FADE_MS, easing = FastOutSlowInEasing)) togetherWith
                                fadeOut(tween(HERO_FADE_MS, easing = FastOutSlowInEasing))
                        },
                        modifier = Modifier.fillMaxSize(), label = "featuredArtwork",
                    ) { visual ->
                        if (visual != null) {
                            ArtworkImage(visual.bitmap, Modifier.fillMaxSize().featheredArtwork(visual.edges))
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(heroHeight)) {
                    HeroWordmark(
                        color = titleColor,
                        modifier = Modifier.align(Alignment.TopStart)
                            .padding(start = ContentSidePadding, top = status + 6.dp, end = 58.dp),
                    )
                    Column(
                        Modifier.align(Alignment.TopStart)
                            .padding(start = ContentSidePadding, top = status + 57.dp, end = 58.dp),
                    ) {
                        Text("${greeting.first}, $profileName", color = colors.onBackground,
                            fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(greeting.second, color = colors.onBackground.copy(alpha = 0.62f),
                            fontSize = 12.sp, lineHeight = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box(
                        Modifier.align(Alignment.TopStart)
                            .padding(start = ContentSidePadding, top = status + 139.dp)
                            .width(textWidth).height(126.dp),
                    ) {
                        AnimatedContent(
                            targetState = cover,
                            transitionSpec = {
                                fadeIn(tween(HERO_FADE_MS, easing = FastOutSlowInEasing)) togetherWith
                                    fadeOut(tween(HERO_FADE_MS, easing = FastOutSlowInEasing))
                            },
                            label = "featuredDetails",
                        ) { visual ->
                            visual?.song?.let { featured ->
                                Column {
                                    Text("TOP PICK", color = titleColor.value, fontSize = 10.sp,
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
                    if (displayedSong != null) {
                        Row(
                            Modifier.align(Alignment.TopStart)
                                .padding(start = ContentSidePadding, top = status + 274.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            HeroBlurButton(
                                onClick = { onPlay(displayedSong) },
                                description = if (playing) "Pause ${displayedSong.title}" else "Play ${displayedSong.title}",
                                input = heroInput, style = buttonBlurStyle, liveBlur = liveBlur, dark = dark,
                                modifier = Modifier.width(playWidth).height(42.dp),
                            ) {
                                Icon(painterResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play),
                                    contentDescription = null, tint = colors.onBackground, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(if (playing) "Pause" else "Play", color = colors.onBackground,
                                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                            HeroBlurButton(
                                onClick = onShuffle,
                                description = if (shuffleEnabled) "Turn shuffle off" else "Shuffle songs",
                                input = heroInput, style = buttonBlurStyle, liveBlur = liveBlur, dark = dark,
                                modifier = Modifier.size(42.dp), highlight = if (shuffleEnabled) colors.primary else null,
                            ) {
                                Icon(painterResource(R.drawable.ic_shuffle), contentDescription = null,
                                    tint = colors.onBackground, modifier = Modifier.size(19.dp))
                            }
                        }
                    }
                }
                quickContent(heroInput)
            }
        }
    }
}

@Composable
private fun HeroWordmark(color: State<Color>, modifier: Modifier = Modifier) {
    Text(
        "Crescendo", fontFamily = BorelFamily, fontStyle = FontStyle.Normal,
        color = color.value, fontSize = 27.sp, lineHeight = 39.sp,
        maxLines = 1, modifier = modifier,
    )
}

/** Blurred live pixels with a subtle tint: no lens, refraction, specular effect or border. */
@Composable
private fun HeroBlurButton(
    onClick: () -> Unit,
    description: String,
    input: HazeInput.Sources,
    style: HazeBlurStyle,
    liveBlur: Boolean,
    dark: Boolean,
    modifier: Modifier = Modifier,
    highlight: Color? = null,
    content: @Composable () -> Unit,
) {
    val tint = highlight?.copy(alpha = if (dark) 0.24f else 0.20f)
        ?: if (dark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.50f)
    Box(
        modifier = modifier.clip(RoundedCornerShape(50))
            .then(if (liveBlur) Modifier.hazeBlur(input = input, style = style,
                performanceMode = HazePerformanceMode.Quality) else Modifier)
            .background(tint)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { content() }
    }
}

internal fun homeGreetings(hour: Int): List<Pair<String, String>> {
    val anytime = listOf(
        "Hello" to "A little music goes a long way.",
        "What's up" to "Find the sound that fits your day.",
        "Hey there" to "Something good is waiting to play.",
        "Welcome back" to "Your music is right where you left it.",
        "Good to see you" to "Let's find your next favorite.",
        "Ready to listen" to "Pick a song and settle in.",
        "Here for the music" to "There's always a song for this moment.",
        "Nice to see you" to "Let's make this moment sound better.",
        "Take a breath" to "Let the music do the rest.",
        "Find your rhythm" to "Your soundtrack starts here.",
        "Let's press play" to "A good song can change everything.",
        "How's it going" to "There's a song for every mood.",
    )
    val timely = when (hour) {
        in 5..11 -> listOf("Good morning" to "A fresh start, one song at a time.",
            "Rise and shine" to "Start the day on a good note.")
        in 12..16 -> listOf("Good afternoon" to "Find a little rhythm in your day.",
            "Taking a break" to "Make a little time for music.")
        in 17..21 -> listOf("Good evening" to "Unwind to something you love.",
            "Winding down" to "Find a softer soundtrack.")
        else -> listOf("Working late" to "Here's something to keep you company.",
            "Still up" to "Let the night have its own soundtrack.",
            "Good night" to "A softer soundtrack for tonight.")
    }
    return anytime + timely
}

/** Wide, eased fades make the cover emerge from the surrounding color without a hard start. */
private fun Modifier.featheredArtwork(edges: ArtworkEdgePalette?): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithCache {
        // Multiple stops approximate smoothstep rather than a conspicuous linear frame.
        val fade = arrayOf(
            0f to Color.Transparent, 0.04f to Color.Black.copy(alpha = 0.015f),
            0.10f to Color.Black.copy(alpha = 0.10f), 0.18f to Color.Black.copy(alpha = 0.34f),
            0.28f to Color.Black.copy(alpha = 0.78f), 0.38f to Color.Black,
            0.62f to Color.Black, 0.72f to Color.Black.copy(alpha = 0.78f),
            0.82f to Color.Black.copy(alpha = 0.34f), 0.90f to Color.Black.copy(alpha = 0.10f),
            0.96f to Color.Black.copy(alpha = 0.015f), 1f to Color.Transparent,
        )
        val horizontalMask = Brush.horizontalGradient(colorStops = fade)
        val verticalMask = Brush.verticalGradient(colorStops = fade)
        val horizontalTint = edges?.let {
            Brush.horizontalGradient(0f to Color(it.left).copy(alpha = 0.12f),
                0.32f to Color.Transparent, 0.68f to Color.Transparent, 1f to Color(it.right).copy(alpha = 0.12f))
        }
        val verticalTint = edges?.let {
            Brush.verticalGradient(0f to Color(it.top).copy(alpha = 0.09f),
                0.32f to Color.Transparent, 0.68f to Color.Transparent, 1f to Color(it.bottom).copy(alpha = 0.09f))
        }
        onDrawWithContent {
            drawContent()
            if (horizontalTint != null) drawRect(horizontalTint)
            if (verticalTint != null) drawRect(verticalTint)
            drawRect(horizontalMask, blendMode = BlendMode.DstIn)
            drawRect(verticalMask, blendMode = BlendMode.DstIn)
        }
    }

/** Rich at the top, then fully transparent by the All Songs heading's bottom. */
private fun Modifier.ambientCoverGlow(
    main: State<Color>,
    second: State<Color>,
    lower: State<Color>,
    phase: State<Float>,
): Modifier = drawWithCache {
    val first = main.value
    val next = second.value
    val bottom = lower.value
    // The underlying app background (including a user photo) is revealed naturally.
    // At the bottom every field is exactly transparent, so the next lazy item cannot
    // introduce a horizontal color cut.
    val solidToClear = Brush.verticalGradient(
        0f to first, 0.31f to first, 0.55f to first.copy(alpha = 0.82f),
        0.78f to first.copy(alpha = 0.36f), 1f to first.copy(alpha = 0f),
        endY = size.height.coerceAtLeast(1f),
    )
    fun glow(color: Color, alpha: Float, center: Offset, radius: Float): Brush = Brush.radialGradient(
        0f to color.copy(alpha = alpha),
        0.30f to color.copy(alpha = alpha * 0.84f),
        0.68f to color.copy(alpha = alpha * 0.30f),
        1f to color.copy(alpha = 0f),
        center = center, radius = radius.coerceAtLeast(1f),
    )
    val secondGlow = glow(next, 0.98f, Offset(size.width * 0.88f, size.height * 0.12f), size.height * 0.70f)
    val lowerGlow = glow(bottom, 0.62f, Offset(size.width * 0.26f, size.height * 0.43f), size.height * 0.48f)
    val driftSpan = minOf(size.width, size.height)
    // The translated gradients must overdraw their old bounds. Otherwise their
    // movement leaves a narrow, straight strip at one edge of the screen.
    val overscan = driftSpan * 0.025f
    val glowOrigin = Offset(-overscan, -overscan)
    val glowSize = Size(size.width + overscan * 2f, size.height + overscan * 2f)
    onDrawBehind {
        drawRect(solidToClear)
        val drift = sin(phase.value * 2f * PI.toFloat())
        translate(left = drift * driftSpan * 0.018f, top = drift * driftSpan * 0.012f) {
            drawRect(secondGlow, topLeft = glowOrigin, size = glowSize)
        }
        translate(left = -drift * driftSpan * 0.018f, top = -drift * driftSpan * 0.012f) {
            drawRect(lowerGlow, topLeft = glowOrigin, size = glowSize)
        }
    }
}
