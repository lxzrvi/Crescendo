package com.lxzrvi.crescendo.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AllSongsSortTest {
    private val songs = listOf(
        SortSongKeys("3", "zebra", "Amy", "Third", 30L, 180_000L),
        SortSongKeys("1", "Alpha", "Zed", "First", 10L, 240_000L),
        SortSongKeys("2", "beta", "Ben", "Second", 20L, 0L),
    )

    private fun ids(order: AllSongsSort) = sortAllSongs(songs, order) { it }.map { it.id }

    @Test
    fun titleArtistAndAlbumCanBeSortedBothWays() {
        assertEquals(listOf("1", "2", "3"), ids(AllSongsSort.TitleAscending))
        assertEquals(listOf("3", "2", "1"), ids(AllSongsSort.TitleDescending))
        assertEquals(listOf("3", "2", "1"), ids(AllSongsSort.ArtistAscending))
        assertEquals(listOf("1", "2", "3"), ids(AllSongsSort.ArtistDescending))
        assertEquals(listOf("1", "2", "3"), ids(AllSongsSort.AlbumAscending))
        assertEquals(listOf("3", "2", "1"), ids(AllSongsSort.AlbumDescending))
    }

    @Test
    fun datesAndDurationsSortCorrectlyAndUnknownValuesStayLast() {
        assertEquals(listOf("3", "2", "1"), ids(AllSongsSort.NewestAdded))
        assertEquals(listOf("1", "2", "3"), ids(AllSongsSort.OldestAdded))
        assertEquals(listOf("3", "1", "2"), ids(AllSongsSort.Shortest))
        assertEquals(listOf("1", "3", "2"), ids(AllSongsSort.Longest))
        val unknown = songs[0].copy(dateAdded = 0L)
        listOf(AllSongsSort.NewestAdded, AllSongsSort.OldestAdded).forEach { order ->
            assertEquals(unknown, sortAllSongs(listOf(unknown, songs[1]), order) { it }.last())
        }
    }

    @Test
    fun everyColumnChoiceKeepsAllSongsInOrderAndLimitsTheCollapsedViewToThreeRows() {
        for (count in 0..37) for (columns in 2..5) {
            val input = (0 until count).toList()
            val rows = allSongsRows(input, columns)
            assertEquals(input, rows.flatten())
            assertTrue(rows.all { it.size in 1..columns })
            assertEquals(input.take(columns * 3), visibleAllSongsRows(rows, expanded = false).flatten())
            assertEquals(input, visibleAllSongsRows(rows, expanded = true).flatten())
        }
        assertEquals(listOf(listOf(1, 2), listOf(3)), allSongsRows(listOf(1, 2, 3), columns = 0))
    }

    @Test
    fun tiesAreDeterministicAndEmptyLibrariesStayEmpty() {
        val same = listOf(songs[0].copy(id = "b"), songs[0].copy(id = "a"))
        assertEquals(listOf("a", "b"), sortAllSongs(same, AllSongsSort.TitleAscending) { it }.map { it.id })
        assertTrue(sortAllSongs(emptyList<SortSongKeys>(), AllSongsSort.Shortest) { it }.isEmpty())
        assertEquals(10, AllSongsSort.entries.size)
    }
}
