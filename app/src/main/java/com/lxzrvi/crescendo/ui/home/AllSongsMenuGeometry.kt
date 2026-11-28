package com.lxzrvi.crescendo.ui.home

/** Pixel-space placement shared by the popup and the host window. */
internal data class AllSongsMenuSpace(val opensAbove: Boolean, val heightPx: Int)

/** Keep the trigger fixed and use the larger side when a full menu will not fit below. */
internal fun allSongsMenuSpace(
    anchorTop: Int,
    anchorBottom: Int,
    windowHeight: Int,
    topInset: Int,
    bottomInset: Int,
    desiredHeight: Int,
): AllSongsMenuSpace {
    val anchorHeight = (anchorBottom - anchorTop).coerceAtLeast(1)
    val below = (windowHeight - bottomInset - anchorTop).coerceAtLeast(0)
    val above = (anchorBottom - topInset).coerceAtLeast(0)
    val opensAbove = below < desiredHeight && above > below
    val available = if (opensAbove) above else below
    return AllSongsMenuSpace(opensAbove, minOf(desiredHeight, maxOf(anchorHeight, available)))
}

internal data class AllSongsMenuOffset(val x: Int, val y: Int)

/** Grow toward the middle of the screen, aligned with the original pill in LTR and RTL. */
internal fun allSongsMenuOffset(
    anchorLeft: Int,
    anchorTop: Int,
    anchorRight: Int,
    anchorBottom: Int,
    windowWidth: Int,
    windowHeight: Int,
    menuWidth: Int,
    menuHeight: Int,
    opensAbove: Boolean,
    rtl: Boolean,
): AllSongsMenuOffset {
    val x = if (rtl) anchorLeft else anchorRight - menuWidth
    val y = if (opensAbove) anchorBottom - menuHeight else anchorTop
    return AllSongsMenuOffset(
        x.coerceIn(0, (windowWidth - menuWidth).coerceAtLeast(0)),
        y.coerceIn(0, (windowHeight - menuHeight).coerceAtLeast(0)),
    )
}
