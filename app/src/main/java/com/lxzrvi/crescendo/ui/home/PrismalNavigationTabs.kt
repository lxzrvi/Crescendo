package com.lxzrvi.crescendo.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.components.PrismalGlassBottomTab
import com.styropyr0.prismal.components.PrismalGlassBottomTabs

/**
 * Use PrismalAGSL's actual bottom-tabs component: its own spring, drag, lens,
 * haptics, and nested glass remain untouched. Crescendo supplies only its tabs,
 * current destination and accent, and observes touches for the separate bridge.
 */
@Composable
internal fun PrismalNavigationTabs(
    tabs: List<Destination>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    backdrop: PrismalBackdrop,
    isLightTheme: Boolean,
    controlsEnabled: Boolean,
    alpha: Float,
    onPressProgress: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val indexState = rememberUpdatedState(selectedIndex)
    val enabledState = rememberUpdatedState(controlsEnabled)
    val selectedCallback = rememberUpdatedState(onTabSelected)
    val pressCallback = rememberUpdatedState(onPressProgress)
    // A stable accessor lets upstream observe state changes instead of resetting its spring.
    val selectedAccessor = remember { { indexState.value } }
    val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val configuration = LocalConfiguration.current
    // Prismal follows isSystemInDarkTheme(); honor Crescendo's explicit theme preference too.
    val themedConfiguration = remember(configuration, isLightTheme) {
        Configuration(configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (isLightTheme) Configuration.UI_MODE_NIGHT_NO else Configuration.UI_MODE_NIGHT_YES
        }
    }
    val foreground = if (isLightTheme) Color.Black else Color.White
    val accent = MaterialTheme.colorScheme.primary

    BoxWithConstraints(
        modifier = modifier.graphicsLayer { this.alpha = alpha.coerceIn(0f, 1f) }
            .pointerInput(controlsEnabled, ltr, tabs.size) {
                // Observe without consuming: Prismal owns all taps and drag gestures.
                if (controlsEnabled) awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    fun report(x: Float) {
                        val logical = if (ltr) x / size.width.coerceAtLeast(1) else 1f - x / size.width.coerceAtLeast(1)
                        pressCallback.value(((logical - 0.70f) / 0.20f).coerceIn(0f, 1f))
                    }
                    report(down.position.x)
                    try {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            report(change.position.x)
                        }
                    } finally {
                        pressCallback.value(0f)
                    }
                }
            },
    ) {
        val showLabels = (maxWidth - 8.dp) / tabs.size.toFloat() >= 42.dp
        CompositionLocalProvider(LocalConfiguration provides themedConfiguration) {
            PrismalGlassBottomTabs(
                selectedTabIndex = selectedAccessor,
                onTabSelected = { if (enabledState.value) selectedCallback.value(it) },
                backdrop = backdrop,
                tabsCount = tabs.size,
                modifier = Modifier.fillMaxSize(),
                dropletContentTint = accent,
                pressGlow = if (isLightTheme) 0.7f else 0.25f,
                renderWithLowOverhead = true,
            ) {
                tabs.forEachIndexed { index, destination ->
                    PrismalGlassBottomTab(
                        onClick = { if (enabledState.value) selectedCallback.value(index) },
                        modifier = Modifier.semantics {
                            contentDescription = destination.label
                            selected = index == selectedIndex
                            if (!controlsEnabled) disabled()
                        },
                    ) {
                        Icon(painterResource(destination.icon), contentDescription = null,
                            modifier = Modifier.size(27.dp), tint = foreground)
                        if (showLabels) Text(
                            text = destination.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium,
                            ),
                            color = foreground, textAlign = TextAlign.Center,
                            maxLines = 1, softWrap = false, overflow = TextOverflow.Clip,
                        )
                    }
                }
            }
        }
    }
}
