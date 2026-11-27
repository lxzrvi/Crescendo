@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.animation.ExperimentalSharedTransitionApi::class,
)

package com.lxzrvi.crescendo.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lxzrvi.crescendo.data.AlbumArtworkLoader
import com.lxzrvi.crescendo.data.MusicLibraryState
import com.lxzrvi.crescendo.data.Song

internal enum class CollectionKind(val label: String, val tint: Color, val symbol: String) {
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

/** Overview contains only large collections; a tapped tile travels into its real-data page. */
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
    category: CollectionKind?,
    onCategoryChange: (CollectionKind?) -> Unit,
    overviewState: LazyListState,
    detailState: LazyListState,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val songs = (library as? MusicLibraryState.Ready)?.songs.orEmpty()
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
    LaunchedEffect(category) { if (category != null) detailState.scrollToItem(0) }

    SharedTransitionLayout(modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = category,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = { fadeIn(tween(380)) togetherWith fadeOut(tween(260)) },
            label = "libraryCollectionTransition",
        ) { page ->
            val visibility = this
            if (page == null) {
                LazyColumn(
                    state = overviewState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = ContentSidePadding, end = ContentSidePadding,
                        top = statusInset + 22.dp, bottom = bottomPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "library-heading") {
                        Text("Library", color = colors.onBackground, fontSize = 32.sp, lineHeight = 36.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp,
                            modifier = Modifier.padding(end = 56.dp, bottom = 12.dp))
                    }
                    if (library is MusicLibraryState.Error) {
                        item(key = "library-error") {
                            Text(library.message, color = colors.onBackground.copy(alpha = 0.65f),
                                modifier = Modifier.padding(bottom = 8.dp))
                        }
                    }
                    items(CollectionKind.entries.chunked(2), key = { it.first().name }) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            row.forEach { tile ->
                                CollectionTile(
                                    category = tile,
                                    count = counts[tile] ?: 0,
                                    expanded = false,
                                    modifier = Modifier.weight(1f).sharedBounds(
                                        sharedContentState = rememberSharedContentState("collection-${tile.name}"),
                                        animatedVisibilityScope = visibility,
                                        boundsTransform = { _, _ -> tween(560, easing = FastOutSlowInEasing) },
                                    ),
                                    onOpen = { onCategoryChange(tile) },
                                )
                            }
                        }
                    }
                }
            } else {
                // Key this data to the rendered page, not the target category: the
                // outgoing list must stay populated while the tile animates back.
                val filtered = remember(songs, page, playCounts, recentTimes, liked) {
                    when (page) {
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
                val grouped = remember(filtered, page) {
                    if (page == CollectionKind.Albums) filtered.groupBy(::albumIdentity)
                    else if (page == CollectionKind.Artists) filtered.groupBy { it.artist.lowercase() }
                    else emptyMap()
                }
                val pageColor = lerp(colors.background, page.tint, if (dark) 0.29f else 0.18f)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
                    lerp(pageColor, page.tint, if (dark) 0.35f else 0.20f), pageColor,
                )))) {
                    LazyColumn(
                        state = detailState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = ContentSidePadding, end = ContentSidePadding,
                            top = statusInset + 16.dp, bottom = bottomPadding),
                    ) {
                        item(key = "collection-header") {
                            CollectionTile(
                                category = page,
                                count = counts[page] ?: 0,
                                expanded = true,
                                modifier = Modifier.fillMaxWidth().sharedBounds(
                                    sharedContentState = rememberSharedContentState("collection-${page.name}"),
                                    animatedVisibilityScope = visibility,
                                    boundsTransform = { _, _ -> tween(560, easing = FastOutSlowInEasing) },
                                ),
                                onBack = { onCategoryChange(null) },
                            )
                        }
                        item(key = "collection-songs-heading") {
                            Text("Songs", color = colors.onBackground, fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
                        }
                        when (library) {
                            MusicLibraryState.Loading -> item(key = "loading") {
                                Text("Loading your music…", color = colors.onBackground.copy(alpha = 0.65f),
                                    modifier = Modifier.padding(vertical = 18.dp))
                            }
                            is MusicLibraryState.Error -> item(key = "error") {
                                Text(library.message, color = colors.onBackground.copy(alpha = 0.65f),
                                    modifier = Modifier.padding(vertical = 18.dp))
                            }
                            is MusicLibraryState.Ready -> {
                                if (filtered.isEmpty()) {
                                    item(key = "empty") {
                                        Text(
                                            when (page) {
                                                CollectionKind.Liked -> "Tap a heart beside a song in another collection to add it here."
                                                CollectionKind.Recent, CollectionKind.MostPlayed -> "Songs you play in Crescendo will appear here."
                                                else -> "No songs in this collection yet."
                                            },
                                            color = colors.onBackground.copy(alpha = 0.65f), fontSize = 14.sp,
                                            lineHeight = 20.sp, modifier = Modifier.padding(vertical = 18.dp),
                                        )
                                    }
                                } else if (page == CollectionKind.Albums || page == CollectionKind.Artists) {
                                    grouped.forEach { (identity, tracks) ->
                                        item(key = "group:${page.name}:$identity") {
                                            val label = if (page == CollectionKind.Albums) tracks.first().album else tracks.first().artist
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
            }
        }
    }
}

@Composable
private fun CollectionTile(
    category: CollectionKind,
    count: Int,
    expanded: Boolean,
    modifier: Modifier,
    onOpen: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(if (expanded) 23.dp else 19.dp)
    val description = when (category) {
        CollectionKind.Albums -> if (count == 1) "1 album" else "$count albums"
        CollectionKind.Artists -> if (count == 1) "1 artist" else "$count artists"
        else -> if (count == 1) "1 song" else "$count songs"
    }
    Box(
        modifier = modifier.height(if (expanded) 205.dp else 135.dp).clip(shape)
            .background(Brush.linearGradient(listOf(category.tint, category.tint.copy(alpha = 0.83f), Color(0xFF292432))))
            .border(0.7.dp, Color.White.copy(alpha = 0.24f), shape)
            .then(if (onOpen != null) Modifier.clickable(role = Role.Button, onClick = onOpen) else Modifier)
            .semantics { contentDescription = "${category.label}, $description" },
    ) {
        if (onBack != null) {
            Box(Modifier.align(Alignment.TopStart).padding(14.dp).size(38.dp).clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.18f))
                .clickable(role = Role.Button, onClick = onBack)
                .semantics { contentDescription = "Back to Library collections" }, contentAlignment = Alignment.Center) {
                Text("‹", color = Color.White, fontSize = 31.sp, lineHeight = 32.sp)
            }
        }
        Box(Modifier.align(if (expanded) Alignment.BottomEnd else Alignment.TopEnd)
            .padding(if (expanded) 18.dp else 13.dp).size(if (expanded) 52.dp else 37.dp).clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Text(category.symbol, color = Color.White.copy(alpha = 0.90f), fontSize = if (expanded) 28.sp else 20.sp,
                fontWeight = FontWeight.Medium)
        }
        Column(Modifier.align(Alignment.BottomStart)
            .padding(start = if (expanded) 19.dp else 13.dp, end = if (expanded) 76.dp else 8.dp,
                bottom = if (expanded) 19.dp else 13.dp)) {
            Text(category.label, color = Color.White, fontSize = if (expanded) 28.sp else 18.sp,
                lineHeight = if (expanded) 30.sp else 21.sp,
                fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(if (expanded) 5.dp else 3.dp))
            Text(description, color = Color.White.copy(alpha = 0.78f), fontSize = if (expanded) 13.sp else 12.sp,
                lineHeight = 15.sp)
        }
    }
}
