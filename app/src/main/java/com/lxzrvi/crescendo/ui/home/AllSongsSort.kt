package com.lxzrvi.crescendo.ui.home

/** Ordering choices available in the All Songs glass menu. */
internal enum class AllSongsSort(val label: String) {
    TitleAscending("Title A–Z"),
    TitleDescending("Title Z–A"),
    ArtistAscending("Artist A–Z"),
    ArtistDescending("Artist Z–A"),
    AlbumAscending("Album A–Z"),
    AlbumDescending("Album Z–A"),
    NewestAdded("Newest added"),
    OldestAdded("Oldest added"),
    Shortest("Shortest first"),
    Longest("Longest first"),
}

internal const val COLLAPSED_ALL_SONG_ROWS = 3

/** Keep row formation consistent when the user changes density. */
internal fun <T> allSongsRows(items: List<T>, columns: Int): List<List<T>> =
    items.chunked(columns.coerceIn(2, 5))

internal fun <T> visibleAllSongsRows(rows: List<List<T>>, expanded: Boolean): List<List<T>> =
    if (expanded) rows else rows.take(COLLAPSED_ALL_SONG_ROWS)

internal data class SortSongKeys(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val dateAdded: Long,
    val durationMs: Long,
)

/**
 * Cache keys once per song rather than recomputing them during every comparison.
 * Unknown dates/durations stay last in either direction; ties stay deterministic.
 * This pure function is run on a background dispatcher by the UI.
 */
internal fun <T> sortAllSongs(items: List<T>, order: AllSongsSort, keys: (T) -> SortSongKeys): List<T> {
    data class Entry<T>(val item: T, val key: SortSongKeys)
    fun text(a: String, b: String) = String.CASE_INSENSITIVE_ORDER.compare(a, b)
    fun number(a: Long, b: Long, descending: Boolean): Int = when {
        a <= 0L && b <= 0L -> 0
        a <= 0L -> 1
        b <= 0L -> -1
        descending -> b.compareTo(a)
        else -> a.compareTo(b)
    }
    val entries = items.map { Entry(it, keys(it)) }
    return entries.sortedWith { left, right ->
        val a = left.key
        val b = right.key
        val primary = when (order) {
            AllSongsSort.TitleAscending -> text(a.title, b.title)
            AllSongsSort.TitleDescending -> text(b.title, a.title)
            AllSongsSort.ArtistAscending -> text(a.artist, b.artist)
            AllSongsSort.ArtistDescending -> text(b.artist, a.artist)
            AllSongsSort.AlbumAscending -> text(a.album, b.album)
            AllSongsSort.AlbumDescending -> text(b.album, a.album)
            AllSongsSort.NewestAdded -> number(a.dateAdded, b.dateAdded, descending = true)
            AllSongsSort.OldestAdded -> number(a.dateAdded, b.dateAdded, descending = false)
            AllSongsSort.Shortest -> number(a.durationMs, b.durationMs, descending = false)
            AllSongsSort.Longest -> number(a.durationMs, b.durationMs, descending = true)
        }
        if (primary != 0) primary else {
            val title = text(a.title, b.title)
            if (title != 0) title else {
                val artist = text(a.artist, b.artist)
                if (artist != 0) artist else a.id.compareTo(b.id)
            }
        }
    }.map { it.item }
}
