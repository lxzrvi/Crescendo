package com.lxzrvi.crescendo.data

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/** A palette swatch and the number of sampled cover pixels it represents. */
internal data class CoverColorCandidate(val color: Int, val population: Int)

/**
 * Pick two independently prominent, perceptually different cover colors when possible.
 * Palette often produces several near-identical shades of its largest color; sorting by
 * population alone made both halves of the hero look the same. Very tiny color flecks
 * are not allowed to beat a genuinely prominent swatch merely for being different.
 *
 * A monochrome cover has only one truthful color, so callers may derive a second tone.
 */
internal fun selectProminentCoverColors(candidates: List<CoverColorCandidate>): List<Int> {
    val colors = candidates.filter { it.population > 0 }
        .distinctBy { it.color and 0x00ffffff }
    if (colors.isEmpty()) return emptyList()
    val maxPopulation = colors.maxOf { it.population }.toDouble()
    val totalPopulation = colors.sumOf { it.population.toLong() }.coerceAtLeast(1L).toDouble()
    fun score(candidate: CoverColorCandidate): Double {
        val r = ((candidate.color ushr 16) and 255) / 255.0
        val g = ((candidate.color ushr 8) and 255) / 255.0
        val b = (candidate.color and 255) / 255.0
        val high = max(r, max(g, b))
        val low = min(r, min(g, b))
        val saturation = if (high == 0.0) 0.0 else (high - low) / high
        // Population still matters; this modest boost prevents black or white margins
        // from burying the actual colorful subject of a cover.
        return sqrt(candidate.population / maxPopulation) *
            (0.35 + 0.65 * saturation) * (0.45 + 0.55 * high)
    }
    val first = colors.maxBy { score(it) }
    val remaining = colors.filter { it !== first }
    if (remaining.isEmpty()) return listOf(first.color)
    val firstLab = colorLab(first.color)
    val separated = remaining.filter {
        it.population / totalPopulation >= 0.025 && labDistance(firstLab, colorLab(it.color)) >= 18.0
    }
    val secondPool = separated.ifEmpty { remaining }
    val second = secondPool.maxBy {
        val difference = (labDistance(firstLab, colorLab(it.color)) / 55.0).coerceIn(0.0, 1.0)
        score(it) * (0.35 + 0.65 * difference)
    }
    return listOf(first.color, second.color)
}

private data class Lab(val l: Double, val a: Double, val b: Double)

private fun colorLab(color: Int): Lab {
    fun linear(channel: Int): Double {
        val value = channel / 255.0
        return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }
    val r = linear((color ushr 16) and 255)
    val g = linear((color ushr 8) and 255)
    val b = linear(color and 255)
    val x = (r * 0.4124564 + g * 0.3575761 + b * 0.1804375) / 0.95047
    val y = r * 0.2126729 + g * 0.7151522 + b * 0.0721750
    val z = (r * 0.0193339 + g * 0.1191920 + b * 0.9503041) / 1.08883
    fun curve(value: Double): Double = if (value > 0.008856451679) value.pow(1.0 / 3.0)
        else value * 7.787037037 + 16.0 / 116.0
    val fx = curve(x)
    val fy = curve(y)
    val fz = curve(z)
    return Lab(116.0 * fy - 16.0, 500.0 * (fx - fy), 200.0 * (fy - fz))
}

private fun labDistance(a: Lab, b: Lab): Double = sqrt(
    (a.l - b.l) * (a.l - b.l) + (a.a - b.a) * (a.a - b.a) + (a.b - b.b) * (a.b - b.b),
)
