@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.lxzrvi.crescendo.ui.home

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lxzrvi.crescendo.R
import com.lxzrvi.crescendo.data.AlbumArtworkLoader
import com.lxzrvi.crescendo.data.MusicLibraryState
import com.lxzrvi.crescendo.data.Song
import com.lxzrvi.crescendo.ui.theme.BorelFamily
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeSourceSelection
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

internal val ContentSidePadding = 10.dp
private const val ALL_SONGS_HEADER_INDEX = 6

@Composable
internal fun HomeMusicPage(
    library: MusicLibraryState,
    playCounts: Map<String, Int>,
    artwork: AlbumArtworkLoader,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onProfileClick: () -> Unit,
    listState: LazyListState,
    topState: LazyListState,
    quickState: LazyListState,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val songs = (library as? MusicLibraryState.Ready)?.songs.orEmpty()
    val topSongs = remember(songs, playCounts) {
        songs.sortedWith(
            compareByDescending<Song> { playCounts[it.historyKey] ?: 0 }
                .thenByDescending { it.dateAdded }
                .thenBy { it.title.lowercase() },
        ).take(5)
    }
    val hasHistory = remember(songs, playCounts) { songs.any { (playCounts[it.historyKey] ?: 0) > 0 } }
    val quickGroups = remember(songs) {
        // Four columns of four distinct local songs, or fewer if the library is smaller.
        songs.shuffled(Random(songs.size.toLong() + 319L)).take(16).chunked(4)
    }
    val carouselItems = remember(topSongs) {
        if (topSongs.size > 1) List(topSongs.size * 3) { topSongs[it % topSongs.size] } else topSongs
    }
    val headerOffset = with(density) { (statusInset + 24.dp).roundToPx() }

    LaunchedEffect(topSongs.map { it.id }) {
        val count = topSongs.size
        if (count > 1) {
            // Three copies permit a seamless wrap without creating billions of fake accessibility items.
            topState.scrollToItem(count + topState.firstVisibleItemIndex % count)
            while (true) {
                delay(5_000)
                val visible = listState.layoutInfo.visibleItemsInfo.any { it.key == "top-carousel" }
                if (visible && !topState.isScrollInProgress) {
                    val layout = topState.layoutInfo
                    val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
                    val centered = layout.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }
                        ?.index ?: topState.firstVisibleItemIndex
                    val normalized = if (centered < count || centered >= count * 2)
                        count + centered % count else centered
                    // A user drag can cancel this child without stopping the auto-scroll loop.
                    coroutineScope {
                        launch {
                            if (normalized != centered) topState.scrollToItem(normalized)
                            topState.animateScrollToItem(normalized + 1)
                        }.join()
                    }
                }
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val allRows = remember(songs) { songs.chunked(4) }
        val topCardWidth = (maxWidth - ContentSidePadding * 2).coerceAtLeast(100.dp)
        val quickColumnWidth = (maxWidth - ContentSidePadding * 2 - 32.dp).coerceIn(210.dp, 360.dp)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = statusInset + 24.dp, bottom = bottomPadding),
        ) {
            item(key = "home-header") { HomeHeader(onProfileClick = onProfileClick) }
            item(key = "home-title") {
                Text(
                    text = "Home",
                    modifier = Modifier.padding(start = ContentSidePadding, end = ContentSidePadding, top = 8.dp, bottom = 4.dp),
                    color = colors.onBackground,
                    fontSize = 32.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.6).sp,
                )
            }
            when (library) {
                MusicLibraryState.Loading -> item(key = "loading") { LoadingLibrary() }
                is MusicLibraryState.Error -> item(key = "error") { LibraryMessage(library.message) }
                is MusicLibraryState.Ready -> {
                    if (songs.isEmpty()) {
                        item(key = "empty") {
                            LibraryMessage("No music found yet. Add songs to your device and reopen Crescendo.")
                        }
                    } else {
                        item(key = "top-heading") {
                            SectionHeading(
                                title = "Top Songs",
                                note = if (hasHistory) null else "Start listening to build your top 5",
                            )
                        }
                        item(key = "top-carousel") {
                            LazyRow(
                                state = topState,
                                flingBehavior = rememberSnapFlingBehavior(lazyListState = topState),
                                contentPadding = PaddingValues(horizontal = ContentSidePadding),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(carouselItems.size, key = { it }) { index ->
                                    val song = carouselItems[index]
                                    TopSongCard(
                                        song = song,
                                        artwork = artwork,
                                        playing = isPlaying && currentSong?.id == song.id,
                                        onClick = { onSongClick(song) },
                                        modifier = Modifier.width(topCardWidth),
                                    )
                                }
                            }
                        }
                        item(key = "quick-heading") {
                            SectionHeading("Quick Picks", onMore = {
                                // Header is index 6 while a non-empty library is rendered below.
                                scope.launch { listState.animateScrollToItem(ALL_SONGS_HEADER_INDEX, -headerOffset) }
                            })
                        }
                        item(key = "quick-carousel") {
                            LazyRow(
                                state = quickState,
                                contentPadding = PaddingValues(horizontal = ContentSidePadding),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(quickGroups, key = { it.first().id }) { group ->
                                    Column(
                                        modifier = Modifier.width(quickColumnWidth),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        group.forEach { song ->
                                            SongListRow(
                                                song = song,
                                                artwork = artwork,
                                                playing = isPlaying && currentSong?.id == song.id,
                                                onClick = { onSongClick(song) },
                                                showMore = true,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item(key = "all-heading") { SectionHeading("All Songs") }
                        items(allRows, key = { it.first().id }) { row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = ContentSidePadding, end = ContentSidePadding, bottom = 8.dp)
                                    .height(IntrinsicSize.Max),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                row.forEach { song ->
                                    SongGridCard(
                                        song = song,
                                        artwork = artwork,
                                        playing = isPlaying && currentSong?.id == song.id,
                                        onClick = { onSongClick(song) },
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                    )
                                }
                                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(onProfileClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = ContentSidePadding, end = ContentSidePadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Crescendo",
            fontFamily = BorelFamily,
            fontSize = 28.sp,
            lineHeight = 42.sp,
            maxLines = 1,
            color = colors.onBackground,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onProfileClick,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.onBackground.copy(alpha = 0.06f)),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_profile),
                contentDescription = "Profile",
                tint = colors.onBackground,
                modifier = Modifier.size(25.dp),
            )
        }
    }
}

@Composable
private fun SectionHeading(title: String, note: String? = null, onMore: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Column(modifier = Modifier.padding(start = ContentSidePadding, end = ContentSidePadding, top = 20.dp, bottom = 10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = colors.onBackground,
                fontSize = 21.sp,
                lineHeight = 25.sp,
                letterSpacing = (-0.25).sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (onMore != null) {
                Row(
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button, onClick = onMore)
                        .semantics { contentDescription = "Show all songs" }
                        .padding(start = 8.dp, end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("More", color = colors.onBackground.copy(alpha = 0.68f), fontSize = 14.sp)
                    Spacer(Modifier.width(5.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_onboarding_chevron),
                        contentDescription = null,
                        tint = colors.onBackground.copy(alpha = 0.68f),
                        modifier = Modifier.size(12.dp).graphicsLayer { scaleX = if (rtl) -1f else 1f },
                    )
                }
            }
        }
        if (note != null) {
            Text(
                text = note,
                color = colors.onBackground.copy(alpha = 0.58f),
                fontSize = 12.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun TopSongCard(
    song: Song,
    artwork: AlbumArtworkLoader,
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val visual by rememberArtworkVisual(song, artwork, 208.dp)
    val dominant = visual.dominant?.let { Color(it) } ?: colors.surfaceVariant
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val coverMask = remember(rtl) {
        if (rtl) Brush.horizontalGradient(0f to Color.Black, 0.58f to Color.Black, 1f to Color.Transparent)
        else Brush.horizontalGradient(0f to Color.Transparent, 0.42f to Color.Black, 1f to Color.Black)
    }
    val legibility = remember(rtl) {
        if (rtl) Brush.horizontalGradient(
            0f to Color.Transparent, 0.42f to Color.Transparent,
            0.72f to Color.Black.copy(alpha = 0.32f), 1f to Color.Black.copy(alpha = 0.58f),
        ) else Brush.horizontalGradient(
            0f to Color.Black.copy(alpha = 0.58f), 0.28f to Color.Black.copy(alpha = 0.32f),
            0.58f to Color.Transparent, 1f to Color.Transparent,
        )
    }
    val haze = rememberHazeState()
    val input = remember(haze) { HazeInput.Sources(state = haze, selection = HazeSourceSelection.All) }
    val fade = remember {
        Brush.verticalGradient(
            0f to Color.Transparent,
            0.45f to Color.Black.copy(alpha = 0.3f),
            1f to Color.Black.copy(alpha = 0.75f),
        )
    }
    val blurStyle = remember(fade) {
        HazeBlurStyle {
            blurEnabled(true)
            blurRadius(16.dp)
            noiseFactor(0f)
            backgroundColor(Color.Transparent)
            colorEffects(emptyList())
            fallbackColorEffect(HazeColorEffect.tint(Color.Transparent))
            progressive(HazeProgressive.Brush(fade))
            mask(fade)
        }
    }
    Box(
        modifier = modifier
            .height(208.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "${song.title}, ${song.artist}" },
    ) {
        // The actual cover occupies the rightmost 70%; its leading edge fades
        // into the dominant color extracted from that very image.
        Box(Modifier.matchParentSize().hazeSource(state = haze)) {
            Box(Modifier.matchParentSize().background(dominant))
            ArtworkImage(
                bitmap = visual.bitmap,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(0.70f)
                    .fillMaxHeight()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(brush = coverMask, blendMode = BlendMode.DstIn)
                    },
            )
        }
        // Retain the previous soft, gradually fading blur behind the song labels.
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(86.dp).hazeBlur(
                input = input,
                style = blurStyle,
                performanceMode = HazePerformanceMode.Quality,
            ),
        )
        Box(Modifier.matchParentSize().background(legibility))
        Column(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(0.61f)
                .padding(start = 15.dp, end = 8.dp, bottom = 15.dp),
        ) {
            Text(
                song.title,
                color = Color.White,
                fontSize = 18.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                song.artist,
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 13.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (playing) PlayingBadge(Modifier.align(Alignment.TopEnd).padding(10.dp))
    }
}

@Composable
private fun SongGridCard(
    song: Song,
    artwork: AlbumArtworkLoader,
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val visual by rememberArtworkVisual(song, artwork, 96.dp)
    val dark = colors.background.luminance() < 0.5f
    val cardColor = visual.dominant?.let { rgb ->
        lerp(colors.background, Color(rgb), if (dark) 0.34f else 0.18f)
    } ?: colors.surfaceVariant
    val shape = RoundedCornerShape(15.dp)
    val border = remember(dark) {
        if (dark) Brush.linearGradient(listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.20f)))
        else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.90f), Color.Black.copy(alpha = 0.20f), Color.White.copy(alpha = 0.70f)))
    }
    Column(
        modifier = modifier
            .heightIn(min = 154.dp)
            .clip(shape)
            .background(cardColor)
            .border(0.8.dp, border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "${song.title}, ${song.artist}" }
            .padding(5.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp))) {
            ArtworkImage(visual.bitmap, Modifier.fillMaxSize())
            if (playing) PlayingBadge(Modifier.align(Alignment.TopEnd).padding(4.dp))
        }
        Spacer(Modifier.height(6.dp))
        // Natural-height text wraps instead of chopping long song or artist names.
        Text(
            song.title,
            color = colors.onBackground,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            song.artist,
            color = colors.onBackground.copy(alpha = 0.68f),
            fontSize = 11.sp,
            lineHeight = 13.sp,
        )
        Spacer(Modifier.height(3.dp))
    }
}

@Composable
internal fun SongListRow(
    song: Song,
    artwork: AlbumArtworkLoader,
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showMore: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth().height(62.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = "${song.title}, ${song.artist}" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(54.dp).clip(RoundedCornerShape(10.dp))) {
                SongArtwork(song, artwork, Modifier.fillMaxSize(), 54.dp)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    color = if (playing) colors.primary else colors.onBackground,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    song.artist,
                    color = colors.onBackground.copy(alpha = 0.59f),
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (playing) {
                Icon(
                    painter = painterResource(R.drawable.ic_onboarding_music),
                    contentDescription = "Playing",
                    tint = colors.primary,
                    modifier = Modifier.padding(start = 6.dp, end = 3.dp).size(17.dp),
                )
            }
        }
        if (showMore) {
            // Outside the song click target: this placeholder intentionally does nothing.
            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = null,
                    tint = colors.onBackground.copy(alpha = 0.55f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private data class ArtworkVisual(val bitmap: Bitmap? = null, val dominant: Int? = null)

@Composable
private fun rememberArtworkVisual(song: Song, artwork: AlbumArtworkLoader, requestedSize: Dp): State<ArtworkVisual> {
    val requestedPx = with(LocalDensity.current) { requestedSize.roundToPx() }
    return produceState(ArtworkVisual(), song.id, song.dateModified, requestedPx, artwork) {
        value = ArtworkVisual()
        val bitmap = artwork.load(song, requestedPx)
        value = ArtworkVisual(bitmap = bitmap)
        if (bitmap != null) value = ArtworkVisual(bitmap, artwork.dominantColor(song, bitmap))
    }
}

@Composable
internal fun SongArtwork(song: Song, artwork: AlbumArtworkLoader, modifier: Modifier, requestedSize: Dp) {
    val requestedPx = with(LocalDensity.current) { requestedSize.roundToPx() }
    val bitmap by produceState<Bitmap?>(null, song.id, song.dateModified, requestedPx, artwork) {
        value = null
        value = artwork.load(song, requestedPx)
    }
    ArtworkImage(bitmap, modifier)
}

@Composable
private fun ArtworkImage(bitmap: Bitmap?, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(modifier = modifier.background(colors.surfaceVariant), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_onboarding_music),
                contentDescription = null,
                tint = colors.onBackground.copy(alpha = 0.30f),
                modifier = Modifier.fillMaxSize(0.24f),
            )
        }
    }
}

@Composable
private fun PlayingBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_onboarding_music),
            contentDescription = "Playing",
            tint = Color.White,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
internal fun MusicListPage(
    destination: Destination,
    library: MusicLibraryState,
    query: String,
    artwork: AlbumArtworkLoader,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    appearance: NavigationAppearance,
    onAppearanceChange: (NavigationAppearance) -> Unit,
    listState: LazyListState,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val songs = (library as? MusicLibraryState.Ready)?.songs.orEmpty()
    val filtered = remember(songs, query, destination) {
        val needle = query.trim()
        if (destination != Destination.Search || needle.isEmpty()) songs else songs.filter { song ->
            song.title.contains(needle, ignoreCase = true) ||
                song.artist.contains(needle, ignoreCase = true) ||
                song.album.contains(needle, ignoreCase = true)
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = ContentSidePadding,
            end = ContentSidePadding,
            top = statusInset + 24.dp,
            bottom = bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        item(key = "heading") {
            Text(
                destination.label,
                color = colors.onBackground,
                fontSize = 32.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.6).sp,
                modifier = Modifier.padding(bottom = 18.dp),
            )
        }
        when (destination) {
            Destination.Library, Destination.Search -> {
                when (library) {
                    MusicLibraryState.Loading -> item(key = "loading") { LoadingLibrary() }
                    is MusicLibraryState.Error -> item(key = "error") { LibraryMessage(library.message, false) }
                    is MusicLibraryState.Ready -> {
                        if (filtered.isEmpty()) {
                            item(key = "empty") {
                                LibraryMessage(
                                    if (songs.isEmpty()) "No music found on this device."
                                    else "No songs match your search.",
                                    false,
                                )
                            }
                        } else {
                            items(filtered, key = { it.id }) { song ->
                                SongListRow(
                                    song = song,
                                    artwork = artwork,
                                    playing = isPlaying && currentSong?.id == song.id,
                                    onClick = { onSongClick(song) },
                                )
                            }
                        }
                    }
                }
            }
            Destination.Playlist -> item(key = "playlist-empty") {
                LibraryMessage("Your playlists will appear here.", false)
            }
            Destination.Settings -> item(key = "appearance") {
                NavigationBlurSettings(appearance = appearance, onChange = onAppearanceChange)
            }
            Destination.Home -> Unit
        }
    }
}

@Composable
private fun LoadingLibrary() {
    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(25.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun LibraryMessage(message: String, inset: Boolean = true) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
        fontSize = 14.sp,
        lineHeight = 20.sp,
        modifier = Modifier.padding(start = if (inset) ContentSidePadding else 0.dp, end = if (inset) ContentSidePadding else 0.dp, top = 24.dp),
    )
}
