package com.lxzrvi.crescendo.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickPicksTest {
    @Test
    fun fourPagesOfFourRowsForEveryNonemptyLibrary() {
        assertTrue(quickPickPages(emptyList<String>()).isEmpty())
        for (count in 1..40) {
            val available = (1..count).map { "song-$it" }
            val pages = quickPickPages(available)
            assertEquals(4, pages.size)
            assertTrue(pages.all { it.size == 4 })
            val expected = List(16) { available[it % available.size] }
            assertEquals(expected, pages.flatten())
        }
    }
}
