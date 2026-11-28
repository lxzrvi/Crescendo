package com.lxzrvi.crescendo.ui.home

import kotlin.random.Random

/** Shuffle once per opening, avoiding the same first pick on the next opening where possible. */
internal fun <T> shuffleForSession(
    items: List<T>,
    seed: Long,
    previousFirstId: String?,
    id: (T) -> String,
): List<T> {
    val shuffled = items.shuffled(Random(seed)).toMutableList()
    if (shuffled.size > 1 && id(shuffled.first()) == previousFirstId) {
        val replacement = 1 + Random(seed xor 0x6A09E667F3BCC909L).nextInt(shuffled.size - 1)
        val first = shuffled[0]
        shuffled[0] = shuffled[replacement]
        shuffled[replacement] = first
    }
    return shuffled
}
