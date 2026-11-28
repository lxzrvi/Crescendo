package com.lxzrvi.crescendo.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AllSongsMenuGeometryTest {
    @Test
    fun opensDownWhenItFitsAndUpWhenTheBottomIsTight() {
        val atTop = allSongsMenuSpace(100, 148, 800, 24, 24, 250)
        assertFalse(atTop.opensAbove)
        assertEquals(250, atTop.heightPx)
        val atBottom = allSongsMenuSpace(700, 748, 800, 24, 24, 420)
        assertTrue(atBottom.opensAbove)
        assertEquals(420, atBottom.heightPx)
    }

    @Test
    fun shortWindowsConstrainTheScrollableMenu() {
        val space = allSongsMenuSpace(155, 203, 300, 24, 24, 420)
        assertTrue(space.opensAbove)
        assertEquals(179, space.heightPx)
        assertTrue(space.heightPx >= 48)
    }

    @Test
    fun growingMenuKeepsItsTriggerAnchoredInBothDirections() {
        for (width in listOf(72, 120, 220)) for (height in listOf(48, 100, 240)) {
            val ltr = allSongsMenuOffset(300, 200, 372, 248, 400, 800, width, height, false, false)
            assertEquals(372, ltr.x + width)
            assertEquals(200, ltr.y)
            val rtl = allSongsMenuOffset(28, 500, 100, 548, 400, 800, width, height, true, true)
            assertEquals(28, rtl.x)
            assertEquals(548, rtl.y + height)
        }
    }

    @Test
    fun positionIsClampedAtWindowEdges() {
        val result = allSongsMenuOffset(5, 5, 55, 53, 320, 480, 220, 240, true, false)
        assertEquals(0, result.x)
        assertEquals(0, result.y)
    }
}
