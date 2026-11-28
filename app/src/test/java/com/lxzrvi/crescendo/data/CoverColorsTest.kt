package com.lxzrvi.crescendo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverColorsTest {
    private val red = 0xffda2929.toInt()
    private val darkerRed = 0xffc32325.toInt()
    private val blue = 0xff2869db.toInt()
    private val black = 0xff101010.toInt()

    @Test
    fun twoProminentDifferentColorsBeatNearDuplicateShades() {
        val picked = selectProminentCoverColors(listOf(
            CoverColorCandidate(black, 900),
            CoverColorCandidate(red, 650),
            CoverColorCandidate(darkerRed, 450),
            CoverColorCandidate(blue, 220),
        ))
        assertEquals(2, picked.size)
        assertTrue(picked.contains(red))
        assertTrue(picked.contains(blue))
    }

    @Test
    fun tinyFlecksDoNotOverrideProminentShades() {
        val picked = selectProminentCoverColors(listOf(
            CoverColorCandidate(red, 1000),
            CoverColorCandidate(darkerRed, 700),
            CoverColorCandidate(blue, 1),
        ))
        assertEquals(listOf(red, darkerRed), picked)
    }

    @Test
    fun missingAndMonochromeCoversStaySafe() {
        assertTrue(selectProminentCoverColors(emptyList()).isEmpty())
        assertEquals(listOf(red), selectProminentCoverColors(listOf(CoverColorCandidate(red, 200))))
        assertTrue(selectProminentCoverColors(listOf(CoverColorCandidate(red, 0))).isEmpty())
    }
}
