package com.lxzrvi.crescendo.ui.home

import kotlin.random.Random

internal const val QUICK_PICK_PAGES = 4
internal const val QUICK_PICK_ROWS = 4

/**
 * Four complete pages, in the existing shuffled order. A very small library
 * cycles its available songs rather than showing empty rows or extra pages.
 */
internal fun <T> quickPickPages(items: List<T>): List<List<T>> {
    if (items.isEmpty()) return emptyList()
    return List(QUICK_PICK_PAGES) { page ->
        List(QUICK_PICK_ROWS) { row -> items[(page * QUICK_PICK_ROWS + row) % items.size] }
    }
}

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
