@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import kotlin.random.Random

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
        songs.shuffled(Random(songs.size.toLong() + 319L)).take(100).chunked(5)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = if (maxWidth >= 600.dp) 3 else 2
        val allRows = remember(songs, columns) { songs.chunked(columns) }
        val quickColumnWidth = (maxWidth - 62.dp).coerceIn(230.dp, 350.dp)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = statusInset + 24.dp, bottom = bottomPadding),
        ) {
            item(key = "home-header") {
                HomeHeader(onProfileClick = onProfileClick)
            }
            item(key = "home-title") {
                Text(
                    text = "Home",
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
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
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(topSongs, key = { it.id }) { song ->
                                    TopSongCard(
                                        song = song,
                                        artwork = artwork,
                                        playing = isPlaying && currentSong?.id == song.id,
                                        onClick = { onSongClick(song) },
                                    )
                                }
                            }
                        }
                        item(key = "quick-heading") { SectionHeading("Quick Picks") }
                        item(key = "quick-carousel") {
                            LazyRow(
                                state = quickState,
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
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
                                    .padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
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
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp),
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
private fun SectionHeading(title: String, note: String? = null) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 11.dp)) {
        Text(
            text = title,
            color = colors.onBackground,
            fontSize = 21.sp,
            lineHeight = 25.sp,
            letterSpacing = (-0.25).sp,
            fontWeight = FontWeight.SemiBold,
        )
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
private fun TopSongCard(song: Song, artwork: AlbumArtworkLoader, playing: Boolean, onClick: () -> Unit) {
    val haze = rememberHazeState()
    val input = remember(haze) { HazeInput.Sources(state = haze, selection = HazeSourceSelection.All) }
    val fade = remember {
        Brush.verticalGradient(
            0f to Color.Transparent,
            0.28f to Color.Black.copy(alpha = 0.18f),
            0.70f to Color.Black.copy(alpha = 0.75f),
            1f to Color.Black,
        )
    }
    val style = remember(fade) {
        HazeBlurStyle {
            blurEnabled(true)
            blurRadius(20.dp)
            noiseFactor(0f)
            backgroundColor(Color.Transparent)
            colorEffects(emptyList())
            fallbackColorEffect(HazeColorEffect.tint(Color.Transparent))
            progressive(HazeProgressive.Brush(fade))
            mask(fade)
        }
    }
    val legibility = remember {
        Brush.verticalGradient(
            0f to Color.Transparent,
            0.55f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.62f),
        )
    }
    Box(
        modifier = Modifier
            .size(208.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "${song.title}, ${song.artist}" },
    ) {
        SongArtwork(song, artwork, Modifier.fillMaxSize().hazeSource(state = haze), 208.dp)
        // A progressively stronger blur samples the actual cover, not a flat colored strip.
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(120.dp).hazeBlur(
                input = input,
                style = style,
                performanceMode = HazePerformanceMode.Quality,
            ),
        )
        Box(Modifier.matchParentSize().background(legibility))
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 15.dp, vertical = 14.dp)) {
            Text(
                song.title,
                color = Color.White,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                song.artist,
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 12.sp,
                lineHeight = 15.sp,
                maxLines = 1,
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
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "${song.title}, ${song.artist}" },
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp))) {
            SongArtwork(song, artwork, Modifier.fillMaxSize(), 180.dp)
            if (playing) PlayingBadge(Modifier.align(Alignment.TopEnd).padding(8.dp))
        }
        Text(
            song.title,
            color = colors.onBackground,
            fontSize = 14.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 7.dp),
        )
        Text(
            song.artist,
            color = colors.onBackground.copy(alpha = 0.62f),
            fontSize = 12.sp,
            lineHeight = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun SongListRow(
    song: Song,
    artwork: AlbumArtworkLoader,
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "${song.title}, ${song.artist}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(10.dp))) {
            SongArtwork(song, artwork, Modifier.fillMaxSize(), 54.dp)
        }
        Spacer(Modifier.width(11.dp))
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
                modifier = Modifier.padding(start = 8.dp, end = 3.dp).size(17.dp),
            )
        }
    }
}

@Composable
internal fun SongArtwork(song: Song, artwork: AlbumArtworkLoader, modifier: Modifier, requestedSize: Dp) {
    val colors = MaterialTheme.colorScheme
    val requestedPx = with(LocalDensity.current) { requestedSize.roundToPx() }
    val bitmap by produceState<Bitmap?>(null, song.id, song.dateModified, requestedPx, artwork) {
        value = null
        value = artwork.load(song, requestedPx)
    }
    Box(
        modifier = modifier.background(colors.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
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
            start = 20.dp,
            end = 20.dp,
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
        modifier = Modifier.padding(start = if (inset) 20.dp else 0.dp, end = if (inset) 20.dp else 0.dp, top = 24.dp),
    )
}

@Composable
internal fun MiniPlayer(
    song: Song,
    artwork: AlbumArtworkLoader,
    playing: Boolean,
    loading: Boolean,
    error: String?,
    onToggle: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val surface = if (dark) Color(0xFF242226) else Color(0xFFF5EFED)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 66.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(surface)
            .border(0.7.dp, colors.onBackground.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(start = 9.dp, end = 4.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SongArtwork(song, artwork, Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)), 50.dp)
        Spacer(Modifier.width(11.dp))
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
                color = if (error != null) colors.primary else colors.onBackground.copy(alpha = 0.60f),
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
        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = "Close player",
                tint = colors.onBackground.copy(alpha = 0.60f),
                modifier = Modifier.size(17.dp),
            )
        }
    }
}
