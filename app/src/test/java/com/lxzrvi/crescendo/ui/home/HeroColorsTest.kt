package com.lxzrvi.crescendo.ui.home

import kotlin.random.Random
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeroColorsTest {
    @Test
    fun wordmarkAndDefaultTextRemainReadableForVariedArtInBothThemes() {
        val random = Random(2026)
        val white = 0xffffffff.toInt()
        val black = 0xff000000.toInt()
        repeat(2000) {
            val swatches = List(2) { (0xff000000L or random.nextInt(0x1000000).toLong()).toInt() }
            val edge = (0xff000000L or random.nextInt(0x1000000).toLong()).toInt()
            listOf(true, false).forEach { dark ->
                val palette = heroColors(swatches, edge, swatches.first(), dark)
                listOf(palette.main, palette.second, palette.lower).forEach { background ->
                    assertTrue("wordmark contrast", colorContrast(palette.title, background) >= 4.5)
                    assertTrue("default text contrast", colorContrast(if (dark) white else black, background) >= 4.5)
                }
                // A transition can temporarily combine the old title and new palette;
                // this conservative bound protects every such intermediate cover.
                val worstCase = if (dark) 0xff666666.toInt() else 0xffd2d2d2.toInt()
                assertTrue("transition contrast", colorContrast(palette.title, worstCase) >= 4.5)
            }
        }
    }

    @Test
    fun changingArtworkChangesTheTintedWordmark() {
        val red = 0xffc92732.toInt()
        val blue = 0xff254bd9.toInt()
        assertNotEquals(heroColors(listOf(red), null, red, true).title,
            heroColors(listOf(blue), null, blue, true).title)
        assertNotEquals(heroColors(listOf(red), null, red, false).title,
            heroColors(listOf(blue), null, blue, false).title)
    }
}
