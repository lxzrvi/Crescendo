@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.lxzrvi.crescendo.ui.home

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.styropyr0.prismal.PrismalBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal val ContentSidePadding = 20.dp

@Composable
internal fun HomeMusicPage(
    library: MusicLibraryState,
    featuredSongs: List<Song>,
    quickSongs: List<Song>,
    profileName: String,
    greetingSeed: Long,
    artwork: AlbumArtworkLoader,
    currentSong: Song?,
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    animateHero: Boolean,
    liveBlur: Boolean,
    onSongClick: (Song) -> Unit,
    onShuffle: () -> Unit,
    onVerticalScroll: (Float) -> Unit,
    listState: LazyListState,
    quickState: LazyListState,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val latestVerticalScroll by rememberUpdatedState(onVerticalScroll)
    val scrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (consumed.y != 0f) latestVerticalScroll(consumed.y)
                return Offset.Zero
            }
        }
    }
    var showAll by rememberSaveable { mutableStateOf(false) }
    val songs = (library as? MusicLibraryState.Ready)?.songs.orEmpty()
    val topSongs = featuredSongs
    val topIds = remember(topSongs) { topSongs.map { it.id } }
    var featuredIndex by remember(topIds) { androidx.compose.runtime.mutableIntStateOf(0) }
    val featured = topSongs.getOrNull(featuredIndex.coerceAtMost((topSongs.size - 1).coerceAtLeast(0)))
    val quickGroups = remember(quickSongs) { quickSongs.chunked(4) }
    val heroPixels = with(LocalDensity.current) { 300.dp.roundToPx() }

    fun nextQuickPicks() {
        if (quickGroups.size > 1) {
            val next = (quickState.firstVisibleItemIndex + 1) % quickGroups.size
            scope.launch { quickState.animateScrollToItem(next) }
        }
    }
    fun toggleAllSongs() { showAll = !showAll }

    // Prepare the next image and its edge colors before the timed transition.
    LaunchedEffect(topIds, featuredIndex, artwork, heroPixels) {
        if (topSongs.size > 1) {
            val next = topSongs[(featuredIndex + 1) % topSongs.size]
            artwork.load(next, heroPixels)?.let { artwork.edgePalette(next, it) }
        }
    }

    // Restart the slower timer after a manual swipe; never change an off-screen cover.
    LaunchedEffect(topIds, animateHero, featuredIndex) {
        if (animateHero && topIds.size > 1) {
            while (true) {
                delay(10_000)
                if (listState.layoutInfo.visibleItemsInfo.any { it.key == "home-hero" }) {
                    featuredIndex = (featuredIndex + 1) % topIds.size
                    break
                }
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val allRows = remember(songs) { songs.chunked(4) }
        val shownRows = if (showAll) allRows else allRows.take(3)
        val quickColumnWidth = (maxWidth - ContentSidePadding * 2 - 36.dp).coerceIn(190.dp, 360.dp)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().nestedScroll(scrollConnection),
            contentPadding = PaddingValues(bottom = bottomPadding),
        ) {
            item(key = "home-hero") {
                HomeHero(
                    song = featured,
                    artwork = artwork,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    profileName = profileName,
                    greetingSeed = greetingSeed,
                    shuffleEnabled = shuffleEnabled,
                    animate = animateHero,
                    liveBlur = liveBlur,
                    canSwipe = topSongs.size > 1,
                    onPlay = onSongClick,
                    onShuffle = onShuffle,
                    onSwipe = { direction ->
                        if (topSongs.size > 1) featuredIndex =
                            (featuredIndex + direction + topSongs.size) % topSongs.size
                    },
                    quickContent = {
                        if (library is MusicLibraryState.Ready && songs.isNotEmpty()) {
                            SectionHeading(
                                "Quick Picks",
                                moreDescription = "Next quick picks",
                                onMore = if (quickGroups.size > 1) ::nextQuickPicks else null,
                            )
                            LazyRow(
                                state = quickState,
                                contentPadding = PaddingValues(horizontal = ContentSidePadding),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(quickGroups, key = { it.first().id }) { group ->
                                    Column(modifier = Modifier.width(quickColumnWidth),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        group.forEach { song ->
                                            SongListRow(song = song, artwork = artwork,
                                                playing = isPlaying && currentSong?.id == song.id,
                                                showDuration = true, onClick = { onSongClick(song) })
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(26.dp))
                        }
                    },
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
                        item(key = "all-heading") {
                            SectionHeading(
                                "All Songs",
                                note = if (songs.size == 1) "1 song" else "${songs.size} songs",
                                onTitleClick = if (songs.size > 12) ::toggleAllSongs else null,
                            )
                        }
                        items(shownRows, key = { it.first().id }) { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(start = ContentSidePadding, end = ContentSidePadding, bottom = 7.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                row.forEach { song ->
                                    SongGridCard(
                                        song = song,
                                        artwork = artwork,
                                        playing = isPlaying && currentSong?.id == song.id,
                                        onClick = { onSongClick(song) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                        if (songs.size > 12) {
                            item(key = "all-toggle") {
                                Box(
                                    Modifier.fillMaxWidth().padding(horizontal = ContentSidePadding, vertical = 14.dp)
                                        .heightIn(min = 46.dp).clip(RoundedCornerShape(24.dp))
                                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f))
                                        .clickable(role = Role.Button) { showAll = !showAll },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(if (showAll) "Show Less" else "Show All",
                                        color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(
    title: String,
    note: String? = null,
    moreDescription: String = "More",
    onMore: (() -> Unit)? = null,
    onTitleClick: (() -> Unit)? = null,
) {
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
                modifier = Modifier.weight(1f).then(
                    if (onTitleClick != null) Modifier.clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClick = onTitleClick)
                        .semantics { contentDescription = "$title, expand or collapse" }
                    else Modifier,
                ),
            )
            if (onMore != null) {
                Row(
                    modifier = Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button, onClick = onMore)
                        .semantics { contentDescription = moreDescription }
                        .padding(start = 8.dp, end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("More", color = colors.onBackground.copy(alpha = 0.68f), fontSize = 14.sp)
                    Spacer(Modifier.width(5.dp))
                    Icon(painter = painterResource(R.drawable.ic_onboarding_chevron), contentDescription = null,
                        tint = colors.onBackground.copy(alpha = 0.68f),
                        modifier = Modifier.size(12.dp).graphicsLayer { scaleX = if (rtl) -1f else 1f })
                }
            }
        }
        if (note != null) Text(note, color = colors.onBackground.copy(alpha = 0.52f),
            fontSize = 12.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

/** Square artwork, with all metadata underneath. Covers deliberately have no border. */
@Composable
private fun SongGridCard(
    song: Song,
    artwork: AlbumArtworkLoader,
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = listOfNotNull(song.title, song.artist, formatSongDuration(song.durationMs)).joinToString(", ") },
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(9.dp))) {
            // Grid covers need no palette extraction; load only their sized bitmap.
            SongArtwork(song, artwork, Modifier.fillMaxSize(), 120.dp)
            if (playing) PlayingBadge(Modifier.align(Alignment.TopEnd).padding(4.dp))
        }
        Text(song.title, color = if (playing) colors.primary else colors.onBackground,
            fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        Text(song.artist, color = colors.onBackground.copy(alpha = 0.60f), fontSize = 11.sp,
            lineHeight = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        formatSongDuration(song.durationMs)?.let { duration ->
            Text(duration, color = colors.onBackground.copy(alpha = 0.43f), fontSize = 10.sp,
                lineHeight = 13.sp, maxLines = 1)
        }
    }
}

internal fun formatSongDuration(durationMs: Long): String? {
    if (durationMs <= 0L) return null
    val seconds = durationMs / 1000
    val hours = seconds / 3600
    val minutes = (seconds / 60) % 60
    val remainder = (seconds % 60).toString().padStart(2, '0')
    return if (hours > 0) "$hours:${minutes.toString().padStart(2, '0')}:$remainder"
    else "${seconds / 60}:$remainder"
}

@Composable
internal fun SongListRow(
    song: Song,
    artwork: AlbumArtworkLoader,
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDuration: Boolean = false,
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
                .semantics {
                    contentDescription = listOfNotNull(song.title, song.artist,
                        if (showDuration) formatSongDuration(song.durationMs) else null,
                        if (playing) "Playing" else null).joinToString(", ")
                },
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
                PlayingWave(colors.primary, Modifier.padding(start = 6.dp, end = 3.dp).size(17.dp))
            }
            if (showDuration) formatSongDuration(song.durationMs)?.let { duration ->
                Text(duration, color = colors.onBackground.copy(alpha = 0.52f), fontSize = 11.sp,
                    maxLines = 1, modifier = Modifier.padding(start = 8.dp, end = 3.dp))
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
        PlayingWave(MaterialTheme.colorScheme.onPrimary, Modifier.size(14.dp))
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
    settingsBackdrop: PrismalBackdrop,
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
                modifier = Modifier.padding(end = 56.dp, bottom = 18.dp),
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
                NavigationBlurSettings(appearance = appearance, onChange = onAppearanceChange, backdrop = settingsBackdrop)
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
