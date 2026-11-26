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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.lxzrvi.crescendo.data.ArtworkPalette
import com.lxzrvi.crescendo.data.MusicLibraryState
import com.lxzrvi.crescendo.data.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

internal val ContentSidePadding = 10.dp

@Composable
internal fun HomeMusicPage(
    library: MusicLibraryState,
    playCounts: Map<String, Int>,
    artwork: AlbumArtworkLoader,
    currentSong: Song?,
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    animateHero: Boolean,
    photoBehind: Boolean,
    onSongClick: (Song) -> Unit,
    onShuffle: () -> Unit,
    onProfileClick: () -> Unit,
    listState: LazyListState,
    quickState: LazyListState,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
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
    val topIds = remember(topSongs) { topSongs.map { it.id } }
    var featuredIndex by remember(topIds) { androidx.compose.runtime.mutableIntStateOf(0) }
    val featured = topSongs.getOrNull(featuredIndex.coerceAtMost((topSongs.size - 1).coerceAtLeast(0)))
    val quickGroups = remember(songs) {
        songs.shuffled(Random(songs.size.toLong() + 319L)).take(16).chunked(4)
    }
    val headerOffset = with(density) { (statusInset + 18.dp).roundToPx() }

    LaunchedEffect(topIds, animateHero) {
        if (animateHero && topIds.size > 1) {
            while (true) {
                delay(6_500)
                if (listState.layoutInfo.visibleItemsInfo.any { it.key == "home-hero" }) {
                    featuredIndex = (featuredIndex + 1) % topIds.size
                }
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val allRows = remember(songs) { songs.chunked(4) }
        val quickColumnWidth = (maxWidth - ContentSidePadding * 2 - 32.dp).coerceIn(210.dp, 360.dp)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomPadding),
        ) {
            item(key = "home-hero") {
                HomeHero(
                    song = featured,
                    artwork = artwork,
                    playing = isPlaying && featured?.id == currentSong?.id,
                    shuffleEnabled = shuffleEnabled,
                    animate = animateHero,
                    photoBehind = photoBehind,
                    onPlay = { featured?.let(onSongClick) },
                    onShuffle = onShuffle,
                    onProfile = onProfileClick,
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
                        item(key = "quick-heading") {
                            SectionHeading("Quick Picks", onMore = {
                                // Hero, Quick Picks heading, Quick Picks carousel, then All Songs.
                                scope.launch { listState.animateScrollToItem(3, -headerOffset) }
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
                                modifier = Modifier.fillMaxWidth()
                                    .padding(start = ContentSidePadding, end = ContentSidePadding, bottom = 7.dp)
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
        lerp(colors.background, Color(rgb), if (dark) 0.42f else 0.21f)
    } ?: colors.surfaceVariant
    val shape = RoundedCornerShape(15.dp)
    val border = remember(dark) {
        if (dark) Brush.linearGradient(listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.20f)))
        else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.90f), Color.Black.copy(alpha = 0.20f), Color.White.copy(alpha = 0.70f)))
    }
    Column(
        modifier = modifier
            .clip(shape)
            .background(cardColor)
            .border(0.8.dp, border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "${song.title}, ${song.artist}" }
            .padding(3.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp))) {
            ArtworkImage(visual.bitmap, Modifier.fillMaxSize())
            if (playing) PlayingBadge(Modifier.align(Alignment.TopEnd).padding(4.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            song.title,
            color = colors.onBackground,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(1.dp))
        Text(
            song.artist,
            color = colors.onBackground.copy(alpha = 0.68f),
            fontSize = 11.sp,
            lineHeight = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
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
    liked: Boolean = false,
    onFavoriteToggle: (() -> Unit)? = null,
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
        if (onFavoriteToggle != null) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onFavoriteToggle)
                    .semantics { contentDescription = if (liked) "Remove ${song.title} from liked songs" else "Like ${song.title}" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(if (liked) R.drawable.ic_heart_filled else R.drawable.ic_heart),
                    contentDescription = null,
                    tint = if (liked) colors.primary else colors.onBackground.copy(alpha = 0.55f),
                    modifier = Modifier.size(20.dp),
                )
            }
        } else if (showMore) {
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

internal data class ArtworkVisual(val bitmap: Bitmap? = null, val palette: ArtworkPalette? = null) {
    val dominant: Int? get() = palette?.dominant
}

@Composable
internal fun rememberArtworkVisual(song: Song, artwork: AlbumArtworkLoader, requestedSize: Dp): State<ArtworkVisual> {
    val requestedPx = with(LocalDensity.current) { requestedSize.roundToPx() }
    return produceState(ArtworkVisual(), song.id, song.dateModified, requestedPx, artwork) {
        value = ArtworkVisual()
        val bitmap = artwork.load(song, requestedPx)
        value = ArtworkVisual(bitmap = bitmap)
        if (bitmap != null) value = ArtworkVisual(bitmap, artwork.palette(song, bitmap))
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
internal fun ArtworkImage(bitmap: Bitmap?, modifier: Modifier) {
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
