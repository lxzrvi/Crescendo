package com.lxzrvi.crescendo.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur

@Composable
internal fun FloatingNavigation(
    selected: Destination,
    previousTab: Destination,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelectTab: (Destination) -> Unit,
    onOpenSearch: () -> Unit,
    onReturnToTab: () -> Unit,
    onSearchDone: () -> Unit,
    imeVisible: Boolean,
    hazeState: HazeState,
    blurStyle: HazeBlurStyle,
    borderOverlay: Color,
    gradientPhase: State<Float>,
    rootSize: State<IntSize>,
    listOrigin: State<Offset>,
    listState: LazyListState,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val searching = selected == Destination.Search
    val activeTab = if (searching) previousTab else selected
    val progress by animateFloatAsState(
        targetValue = if (searching) 1f else 0f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "navigationSearchMorph",
    )
    val keyboardGrowth by animateFloatAsState(
        targetValue = if (searching && imeVisible) 1f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "searchKeyboardWidth",
    )
    val input = remember(hazeState) { HazeInput.Sources(hazeState) }

    // Full-screen container: the two controls are siblings, so only search consumes IME insets.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val edge = 18.dp
        val gap = 10.dp
        val barHeight = 58.dp
        val availableWidth = (maxWidth - edge * 2).coerceAtLeast(barHeight * 2 + gap)
        val wideWidth = (availableWidth - barHeight - gap).coerceAtLeast(barHeight)
        val navWidth = wideWidth * (1f - progress) + barHeight * progress
        val searchAtRest = barHeight * (1f - progress) + wideWidth * progress
        val searchWidth = searchAtRest * (1f - keyboardGrowth) + availableWidth * keyboardGrowth

        // This remains at its original bottom position even while the keyboard covers it.
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = edge, bottom = 14.dp)
                .width(navWidth)
                .height(barHeight)
                .clip(CircleShape)
                .hazeBlur(input = input, style = blurStyle)
                .contextAwareBorder(
                    gradientPhase = gradientPhase,
                    rootSize = rootSize,
                    listOrigin = listOrigin,
                    listState = listState,
                    background = colors.background,
                    overlay = borderOverlay,
                    dark = dark,
                ),
            contentAlignment = Alignment.Center,
        ) {
            val tabsAlpha = (1f - progress * 3.4f).coerceIn(0f, 1f)
            if (progress < 0.30f) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = tabsAlpha }
                        .selectableGroup(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf(Destination.Home, Destination.Playlist, Destination.Library, Destination.Settings)
                        .forEach { tab ->
                            LabeledNavItem(
                                destination = tab,
                                selected = activeTab == tab,
                                enabled = !searching,
                                onClick = { onSelectTab(tab) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                }
            }
            val collapsedAlpha = ((progress - 0.38f) / 0.45f).coerceIn(0f, 1f)
            if (progress > 0.38f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = collapsedAlpha }
                        .clickable(enabled = searching, role = Role.Button, onClick = onReturnToTab),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.semantics { contentDescription = "Return to ${previousTab.label}" },
                    ) {
                        Icon(
                            painter = painterResource(previousTab.icon),
                            contentDescription = null,
                            tint = colors.onBackground,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = previousTab.label,
                            color = colors.onBackground,
                            fontSize = 10.sp,
                            lineHeight = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }

        // Only this sibling uses imePadding. When typing, it also grows to the full available
        // width; the previous-tab circle stays in place underneath the keyboard.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .imePadding()
                .navigationBarsPadding()
                .padding(end = edge, bottom = 14.dp)
                .width(searchWidth)
                .height(barHeight)
                .clip(CircleShape)
                .hazeBlur(input = input, style = blurStyle)
                .contextAwareBorder(
                    gradientPhase = gradientPhase,
                    rootSize = rootSize,
                    listOrigin = listOrigin,
                    listState = listState,
                    background = colors.background,
                    overlay = borderOverlay,
                    dark = dark,
                ),
            contentAlignment = Alignment.Center,
        ) {
            val searchIconAlpha = (1f - progress * 3f).coerceIn(0f, 1f)
            if (progress < 0.34f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = searchIconAlpha }
                        .clickable(enabled = !searching, role = Role.Button, onClick = onOpenSearch),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Destination.Search.icon),
                        contentDescription = "Open search",
                        tint = colors.onBackground,
                        modifier = Modifier.size(23.dp),
                    )
                }
            }
            val fieldAlpha = ((progress - 0.30f) / 0.48f).coerceIn(0f, 1f)
            if (progress > 0.30f) {
                SearchInput(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearchDone = onSearchDone,
                    enabled = searching,
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = fieldAlpha },
                )
            }
        }
    }
}

@Composable
private fun LabeledNavItem(
    destination: Destination,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    Box(
        modifier = modifier
            .fillMaxHeight()
            .selectable(selected = selected, enabled = enabled, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                // The active capsule has a visible gap inside the outer pill.
                .padding(horizontal = 3.dp, vertical = 7.dp)
                .clip(CircleShape)
                .background(if (selected) ink.copy(alpha = 0.12f) else Color.Transparent),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(destination.icon),
                contentDescription = null,
                tint = if (selected) ink else ink.copy(alpha = 0.66f),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = destination.label,
                color = if (selected) ink else ink.copy(alpha = 0.72f),
                fontSize = 10.sp,
                lineHeight = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchDone: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier.padding(start = 18.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(Destination.Search.icon),
            contentDescription = null,
            tint = colors.onBackground,
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onBackground),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchDone() }),
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = "Search sample cards" },
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search sample cards",
                            color = colors.onBackground.copy(alpha = 0.55f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(enabled = enabled, role = Role.Button) { onQueryChange("") }
                    .semantics { contentDescription = "Clear search" },
                contentAlignment = Alignment.Center,
            ) {
                Text("×", color = colors.onBackground.copy(alpha = 0.75f), fontSize = 23.sp)
            }
        }
    }
}
