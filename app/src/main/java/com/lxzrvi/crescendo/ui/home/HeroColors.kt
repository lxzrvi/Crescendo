package com.lxzrvi.crescendo.ui.home

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/** Opaque, cover-derived hero tones and a readable cover-derived wordmark color. */
internal data class HeroColors(val main: Int, val second: Int, val lower: Int, val title: Int)

/**
 * Preserve the cover hues while keeping the dark hero dark enough for light text,
 * and the light hero light enough for dark text. Neutral art stays neutral.
 */
internal fun heroColors(swatches: List<Int>, lowerEdge: Int?, fallback: Int, dark: Boolean): HeroColors {
    val first = swatches.firstOrNull() ?: fallback
    val second = swatches.getOrNull(1) ?: first
    val mainTone = richHeroTone(first, dark)
    val secondTone = richHeroTone(second, dark)
    val lowerTone = richHeroTone(lowerEdge ?: second, dark)
    val hsl = toHsl(first)
    val titleSaturation = if (hsl.saturation < 0.10) hsl.saturation
        else max(hsl.saturation * 0.72, 0.38).coerceAtMost(0.85)
    val titleCandidate = fromHsl(hsl.hue, titleSaturation, if (dark) 0.84 else 0.16)
    // Include a conservative bound for other covers encountered mid-crossfade, so
    // the animated wordmark stays readable between palette endpoints as well.
    val worstCase = if (dark) 0xff666666.toInt() else 0xffd2d2d2.toInt()
    val title = ensureTitleContrast(titleCandidate, listOf(mainTone, secondTone, lowerTone, worstCase), dark)
    return HeroColors(mainTone, secondTone, lowerTone, title)
}

private fun richHeroTone(color: Int, dark: Boolean): Int {
    val hsl = toHsl(color)
    val saturation = when {
        hsl.saturation < 0.10 -> hsl.saturation
        dark -> max(hsl.saturation, 0.70).coerceAtMost(0.98)
        else -> max(hsl.saturation, 0.46).coerceAtMost(0.92)
    }
    // Give cover-derived hues a little more presence while the luminance limits
    // below continue to protect the light/dark foreground contrast.
    val lightness = if (dark) 0.20 + 0.15 * hsl.lightness else 0.77 + 0.15 * hsl.lightness
    val tone = fromHsl(hsl.hue, saturation, lightness)
    // These limits also protect the unchanged white/black greeting and status icons.
    return if (dark) constrainLuminance(tone, 0xff000000.toInt()) { it <= 0.13 }
        else constrainLuminance(tone, 0xffffffff.toInt()) { it >= 0.65 }
}

private fun constrainLuminance(color: Int, endpoint: Int, acceptable: (Double) -> Boolean): Int {
    if (acceptable(relativeLuminance(color))) return color
    var low = 0.0
    var high = 1.0
    repeat(24) {
        val middle = (low + high) / 2.0
        if (acceptable(relativeLuminance(mixColors(color, endpoint, middle)))) high = middle else low = middle
    }
    return mixColors(color, endpoint, high)
}

private fun ensureTitleContrast(color: Int, backgrounds: List<Int>, dark: Boolean): Int {
    fun safe(value: Int) = backgrounds.all { colorContrast(value, it) >= 4.8 }
    if (safe(color)) return color
    val endpoint = if (dark) 0xffffffff.toInt() else 0xff000000.toInt()
    var low = 0.0
    var high = 1.0
    repeat(24) {
        val middle = (low + high) / 2.0
        if (safe(mixColors(color, endpoint, middle))) high = middle else low = middle
    }
    return mixColors(color, endpoint, high)
}

internal fun colorContrast(a: Int, b: Int): Double {
    val l1 = relativeLuminance(a)
    val l2 = relativeLuminance(b)
    return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
}

private fun relativeLuminance(color: Int): Double {
    fun linear(channel: Int): Double {
        val v = channel / 255.0
        return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * linear((color ushr 16) and 255) +
        0.7152 * linear((color ushr 8) and 255) + 0.0722 * linear(color and 255)
}

private data class Hsl(val hue: Double, val saturation: Double, val lightness: Double)

private fun toHsl(color: Int): Hsl {
    val r = ((color ushr 16) and 255) / 255.0
    val g = ((color ushr 8) and 255) / 255.0
    val b = (color and 255) / 255.0
    val high = max(r, max(g, b))
    val low = min(r, min(g, b))
    val lightness = (high + low) / 2.0
    val delta = high - low
    if (delta < 0.000001) return Hsl(0.0, 0.0, lightness)
    val saturation = delta / (1.0 - kotlin.math.abs(2.0 * lightness - 1.0))
    val hue = when (high) {
        r -> ((g - b) / delta + if (g < b) 6.0 else 0.0) / 6.0
        g -> ((b - r) / delta + 2.0) / 6.0
        else -> ((r - g) / delta + 4.0) / 6.0
    }
    return Hsl(hue, saturation, lightness)
}

private fun fromHsl(hue: Double, saturation: Double, lightness: Double): Int {
    if (saturation <= 0.0) {
        val gray = (lightness * 255.0).roundToInt().coerceIn(0, 255)
        return (0xff shl 24) or (gray shl 16) or (gray shl 8) or gray
    }
    val q = if (lightness < 0.5) lightness * (1.0 + saturation)
        else lightness + saturation - lightness * saturation
    val p = 2.0 * lightness - q
    fun channel(offset: Double): Int {
        var t = hue + offset
        if (t < 0.0) t += 1.0
        if (t > 1.0) t -= 1.0
        val value = when {
            t < 1.0 / 6.0 -> p + (q - p) * 6.0 * t
            t < 1.0 / 2.0 -> q
            t < 2.0 / 3.0 -> p + (q - p) * (2.0 / 3.0 - t) * 6.0
            else -> p
        }
        return (value * 255.0).roundToInt().coerceIn(0, 255)
    }
    return (0xff shl 24) or (channel(1.0 / 3.0) shl 16) or (channel(0.0) shl 8) or channel(-1.0 / 3.0)
}

private fun mixColors(from: Int, to: Int, fraction: Double): Int {
    fun channel(shift: Int): Int {
        val a = (from ushr shift) and 255
        val b = (to ushr shift) and 255
        return (a + (b - a) * fraction).roundToInt().coerceIn(0, 255)
    }
    return (0xff shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}
