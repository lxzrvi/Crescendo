package com.lxzrvi.crescendo.ui.home

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Register the whole floating surface in hit testing, including its decorative/empty areas.
 * Events still reach its own children normally; lower sibling song rows are not hit-tested.
 */
internal fun Modifier.blockTouchThrough(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) awaitPointerEvent(PointerEventPass.Final)
    }
}
