package com.lxzrvi.crescendo.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSessionTest {
    private val songs = (1..25).map { "song-$it" }

    @Test
    fun shuffleIsStableWithinASessionAndKeepsEverySong() {
        val first = shuffleForSession(songs, 1234L, null) { it }
        val again = shuffleForSession(songs, 1234L, null) { it }
        assertEquals(first, again)
        assertEquals(songs.toSet(), first.toSet())
        assertEquals(songs.size, first.size)
    }

    @Test
    fun newOpeningAvoidsRepeatingTheFirstPick() {
        val previous = shuffleForSession(songs, 77L, null) { it }.first()
        // The same seed deliberately exercises the collision/replacement path.
        val next = shuffleForSession(songs, 77L, previous) { it }
        assertNotEquals(previous, next.first())
        assertEquals(songs.toSet(), next.toSet())
    }

    @Test
    fun differentSessionSeedsGiveVarietyAndTinyLibrariesStaySafe() {
        val orders = (1L..8L).map { shuffleForSession(songs, it, null) { id -> id } }.toSet()
        assertTrue(orders.size > 1)
        assertEquals(emptyList<String>(), shuffleForSession(emptyList<String>(), 1L, null) { it })
        assertEquals(listOf("only"), shuffleForSession(listOf("only"), 1L, "only") { it })
    }

    @Test
    fun durationsHandleUnknownMinutesAndHours() {
        assertNull(formatSongDuration(0L))
        assertNull(formatSongDuration(-1L))
        assertEquals("0:05", formatSongDuration(5_000L))
        assertEquals("3:07", formatSongDuration(187_000L))
        assertEquals("1:02:03", formatSongDuration(3_723_000L))
    }

    @Test
    fun greetingsHaveVarietyAndALateNightChoice() {
        assertTrue(homeGreetings(12).size >= 10)
        assertTrue(homeGreetings(12).any { it.first == "Hello" })
        assertTrue(homeGreetings(12).any { it.first == "What's up" })
        assertTrue(homeGreetings(2).any { it.first == "Working late" })
    }
}
