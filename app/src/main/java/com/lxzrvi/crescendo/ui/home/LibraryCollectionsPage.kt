@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.lxzrvi.crescendo.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lxzrvi.crescendo.data.AlbumArtworkLoader
import com.lxzrvi.crescendo.data.MusicLibraryState
import com.lxzrvi.crescendo.data.Song
import kotlinx.coroutines.launch

private enum class CollectionKind(val label: String, val tint: Color, val symbol: String) {
    Liked("Liked songs", Color(0xFF9F475F), "♥"),
    Recent("Recently played", Color(0xFF7058A4), "↺"),
    MostPlayed("Most played", Color(0xFFAB743E), "✦"),
    Added("Recently added", Color(0xFF397B7C), "+"),
    All("All songs", Color(0xFF4F6DA4), "♪"),
    Albums("Albums", Color(0xFF99625A), "◉"),
    Artists("Artists", Color(0xFF586D93), "♫"),
    Long("Long listens", Color(0xFF637E57), "∞"),
}

private fun albumIdentity(song: Song): String =
    if (song.albumId >= 0) "${song.volumeName}:${song.albumId}"
    else "${song.artist.lowercase()}:${song.album.lowercase()}"

/** All collection cards are backed by the local library, likes and actual in-app history. */
@Composable
internal fun LibraryCollectionsPage(
    library: MusicLibraryState,
    playCounts: Map<String, Int>,
    recentTimes: Map<String, Long>,
    liked: Set<String>,
    artwork: AlbumArtworkLoader,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onLike: (Song) -> Unit,
    listState: LazyListState,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val offset = with(density) { (statusInset + 16.dp).roundToPx() }
    var category by rememberSaveable { mutableStateOf(CollectionKind.All) }
    val songs = (library as? MusicLibraryState.Ready)?.songs.orEmpty()
    val filtered = remember(songs, category, playCounts, recentTimes, liked) {
        when (category) {
            CollectionKind.Liked -> songs.filter { it.historyKey in liked }
            CollectionKind.Recent -> songs.filter { (recentTimes[it.historyKey] ?: 0L) > 0L }
                .sortedByDescending { recentTimes[it.historyKey] ?: 0L }
            CollectionKind.MostPlayed -> songs.filter { (playCounts[it.historyKey] ?: 0) > 0 }
                .sortedWith(compareByDescending<Song> { playCounts[it.historyKey] ?: 0 }.thenBy { it.title.lowercase() })
            CollectionKind.Added -> songs.sortedByDescending { it.dateAdded }
            CollectionKind.All -> songs
            CollectionKind.Albums -> songs.sortedWith(compareBy<Song> { it.album.lowercase() }.thenBy { it.title.lowercase() })
            CollectionKind.Artists -> songs.sortedWith(compareBy<Song> { it.artist.lowercase() }.thenBy { it.title.lowercase() })
            CollectionKind.Long -> songs.filter { it.durationMs >= 5 * 60 * 1000L }.sortedByDescending { it.durationMs }
        }
    }
    val counts = remember(songs, playCounts, recentTimes, liked) {
        mapOf(
            CollectionKind.Liked to songs.count { it.historyKey in liked },
            CollectionKind.Recent to songs.count { (recentTimes[it.historyKey] ?: 0L) > 0L },
            CollectionKind.MostPlayed to songs.count { (playCounts[it.historyKey] ?: 0) > 0 },
            CollectionKind.Added to songs.size,
            CollectionKind.All to songs.size,
            CollectionKind.Albums to songs.map(::albumIdentity).distinct().size,
            CollectionKind.Artists to songs.map { it.artist.lowercase() }.distinct().size,
            CollectionKind.Long to songs.count { it.durationMs >= 5 * 60 * 1000L },
        )
    }
    val grouped = remember(filtered, category) {
        if (category == CollectionKind.Albums) filtered.groupBy(::albumIdentity)
        else if (category == CollectionKind.Artists) filtered.groupBy { it.artist.lowercase() }
        else emptyMap()
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = ContentSidePadding, end = ContentSidePadding,
            top = statusInset + 22.dp, bottom = bottomPadding),
    ) {
        item(key = "library-heading") {
            Text("Library", color = colors.onBackground, fontSize = 32.sp, lineHeight = 36.sp,
                fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp, modifier = Modifier.padding(bottom = 16.dp))
        }
        item(key = "collections") {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                CollectionKind.entries.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { item ->
                            CollectionTile(
                                category = item,
                                count = counts[item] ?: 0,
                                selected = category == item,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    category = item
                                    scope.launch { listState.animateScrollToItem(2, -offset) }
                                },
                            )
                        }
                    }
                }
            }
        }
        item(key = "library-results-heading") {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 21.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(category.label, color = colors.onBackground, fontWeight = FontWeight.SemiBold,
                    fontSize = 21.sp, modifier = Modifier.weight(1f))
                Text("Collections ↑", color = colors.primary, fontSize = 13.sp,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { scope.launch { listState.animateScrollToItem(0) } }.padding(8.dp))
            }
        }
        when (library) {
            MusicLibraryState.Loading -> item(key = "library-loading") {
                Text("Loading your music…", color = colors.onBackground.copy(alpha = 0.65f), modifier = Modifier.padding(vertical = 18.dp))
            }
            is MusicLibraryState.Error -> item(key = "library-error") {
                Text(library.message, color = colors.onBackground.copy(alpha = 0.65f), modifier = Modifier.padding(vertical = 18.dp))
            }
            is MusicLibraryState.Ready -> {
                if (filtered.isEmpty()) {
                    item(key = "library-empty") {
                        Text(
                            text = when (category) {
                                CollectionKind.Liked -> "Tap a heart beside a song to add it here."
                                CollectionKind.Recent, CollectionKind.MostPlayed -> "Songs you play in Crescendo will appear here."
                                else -> "No songs in this collection yet."
                            },
                            color = colors.onBackground.copy(alpha = 0.62f), fontSize = 14.sp,
                            lineHeight = 20.sp, modifier = Modifier.padding(vertical = 18.dp),
                        )
                    }
                } else if (category == CollectionKind.Albums || category == CollectionKind.Artists) {
                    grouped.forEach { (identity, tracks) ->
                        item(key = "group:${category.name}:$identity") {
                            val label = if (category == CollectionKind.Albums) tracks.first().album else tracks.first().artist
                            Text(label, color = colors.onBackground.copy(alpha = 0.65f), fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
                        }
                        items(tracks, key = { it.id }) { song ->
                            SongListRow(song, artwork, isPlaying && currentSong?.id == song.id,
                                onClick = { onSongClick(song) }, liked = song.historyKey in liked,
                                onFavoriteToggle = { onLike(song) })
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { song ->
                        SongListRow(song, artwork, isPlaying && currentSong?.id == song.id,
                            onClick = { onSongClick(song) }, liked = song.historyKey in liked,
                            onFavoriteToggle = { onLike(song) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionTile(category: CollectionKind, count: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(17.dp)
    val description = when (category) {
        CollectionKind.Albums -> if (count == 1) "1 album" else "$count albums"
        CollectionKind.Artists -> if (count == 1) "1 artist" else "$count artists"
        else -> if (count == 1) "1 song" else "$count songs"
    }
    Box(
        modifier = modifier.height(92.dp).clip(shape)
            .background(Brush.linearGradient(listOf(category.tint, category.tint.copy(alpha = 0.70f), Color(0xFF292432))))
            .border(if (selected) 1.5.dp else 0.7.dp, Color.White.copy(alpha = if (selected) 0.8f else 0.22f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "${category.label}, $description"; this.selected = selected },
    ) {
        Box(Modifier.align(Alignment.TopEnd).padding(12.dp).size(34.dp).clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Text(category.symbol, color = Color.White.copy(alpha = 0.88f), fontSize = 19.sp, fontWeight = FontWeight.Medium)
        }
        Column(Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 8.dp, bottom = 11.dp)) {
            Text(category.label, color = Color.White, fontSize = 16.sp, lineHeight = 18.sp,
                fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(description, color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp, lineHeight = 13.sp)
        }
    }
}
