package com.lxzrvi.crescendo.ui.home

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.OpticalSizeValue
import dev.chrisbanes.haze.glass.hazeGlass
import dev.chrisbanes.haze.hazeSource
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sign

private val navTabs = listOf(Destination.Home, Destination.Playlist, Destination.Library, Destination.Settings)

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
    pageInput: HazeInput.Sources,
    selectorInput: HazeInput.Sources,
    blurStyle: HazeBlurStyle,
    liveBlur: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val searching = selected == Destination.Search
    val activeTab = if (searching) previousTab else selected
    val activeIndex = navTabs.indexOf(activeTab).coerceAtLeast(0)
    val latestOnSelect by rememberUpdatedState(onSelectTab)
    val latestActiveIndex by rememberUpdatedState(activeIndex)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val density = LocalDensity.current
    var searchFocused by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }
    val interactions = remember { navTabs.map { MutableInteractionSource() } }
    LaunchedEffect(searching) {
        if (!searching) searchFocused = false
    }

    val morph by animateFloatAsState(
        targetValue = if (searching) 1f else 0f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "navigationSearchMorph",
    )
    val keyboardGrowth by animateFloatAsState(
        targetValue = if (searching && searchFocused && imeVisible) 1f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "searchKeyboardWidth",
    )
    val liftAnimation by animateFloatAsState(
        targetValue = if (dragging) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "selectorLift",
    )
    val lift = liftAnimation.coerceIn(0f, 1.08f)
    val boundedDrag = dragPosition.coerceIn(0f, navTabs.lastIndex.toFloat())
    val rawOverscroll = dragPosition - boundedDrag
    val rubber = sign(rawOverscroll) * 0.40f * (1f - exp(-abs(rawOverscroll) * 1.3f))
    val elasticPull by animateFloatAsState(
        targetValue = if (dragging) rubber else 0f,
        animationSpec = if (dragging) tween(55) else spring(
            dampingRatio = 0.66f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "navigationElasticPull",
    )
    val selectorIndex by animateFloatAsState(
        targetValue = if (dragging) boundedDrag + rubber else activeIndex.toFloat(),
        animationSpec = if (dragging) tween(45) else spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "selectorPosition",
    )
    val visibleTab = if (dragging) boundedDrag.roundToInt() else activeIndex
    val glassStyle = remember(colors.primary) {
        GlassStyle.clear.then {
            shape(CircleShape)
            tint(colors.primary.copy(alpha = 0.16f))
            optics { copy(blurRadius = OpticalSizeValue.Fixed(5.dp)) }
            specularIntensity(0.72f)
            ambientResponse(0.24f)
        }
    }
    val glassFallbackBlur = remember(colors.background, dark) {
        HazeBlurStyle {
            blurEnabled(true)
            blurRadius(5.dp)
            noiseFactor(0f)
            backgroundColor(colors.background)
            colorEffects(emptyList())
            fallbackColorEffect(HazeColorEffect.tint(navigationSolidColor(dark)))
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val edge = 18.dp
        val gap = 10.dp
        val barHeight = 58.dp
        val availableWidth = (maxWidth - edge * 2).coerceAtLeast(barHeight * 2 + gap)
        val wideWidth = (availableWidth - barHeight - gap).coerceAtLeast(barHeight)
        val navWidth = wideWidth * (1f - morph) + barHeight * morph
        val searchAtRest = barHeight * (1f - morph) + wideWidth * morph
        val searchWidth = searchAtRest * (1f - keyboardGrowth) + availableWidth * keyboardGrowth
        val slotWidth = navWidth / navTabs.size
        val pull = elasticPull.coerceIn(-0.42f, 0.42f)
        val extension = slotWidth * (abs(pull) * 0.65f)
        val shellShift = if (pull < 0f) -extension else 0.dp
        val shellWidth = navWidth + extension
        val shellSlotWidth = shellWidth / navTabs.size
        // At rest, every side of the selector has the same 4 dp inset.
        val indicatorWidth = (slotWidth - 8.dp).coerceAtLeast(20.dp) + 24.dp * lift + slotWidth * (abs(pull) * 0.12f)
        val indicatorHeight = barHeight - 8.dp + 18.dp * lift
        val indicatorLeft = shellShift + shellSlotWidth * (selectorIndex + 0.5f) - indicatorWidth / 2f
        val indicatorAlpha = (1f - morph * 1.55f).coerceIn(0f, 1f)
        val tabsAlpha = (1f - morph * 1.35f).coerceIn(0f, 1f)
        val returnAlpha = (morph * 1.35f - 0.25f).coerceIn(0f, 1f)
        val controlsEnabled = !searching && morph < 0.25f

        // Capture only the navbar shell as a second source. Its own blur samples pageInput, never
        // itself. The lifted selector is a sibling and samples both this shell and the page.
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = edge, bottom = 14.dp)
                .offset(x = shellShift)
                .width(shellWidth)
                .height(barHeight)
                .hazeSource(state = hazeState, zIndex = 1f, key = NAV_SOURCE_KEY)
                .clip(CircleShape)
                .then(
                    if (liveBlur) Modifier.hazeBlur(input = pageInput, style = blurStyle)
                    else Modifier.background(navigationSolidColor(dark)),
                ),
        ) {
            LiveGlassBorder(
                input = pageInput,
                liveBlur = liveBlur,
                background = colors.background,
                dark = dark,
                modifier = Modifier.matchParentSize(),
            )
        }

        if (indicatorAlpha > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = edge, bottom = 14.dp)
                    .offset(x = indicatorLeft, y = (indicatorHeight - barHeight) / 2f)
                    .width(indicatorWidth)
                    .height(indicatorHeight)
                    .graphicsLayer { alpha = indicatorAlpha }
                    .clip(CircleShape)
                    .background(
                        if (liveBlur) colors.primary.copy(alpha = 0.20f)
                        else lerp(navigationSolidColor(dark), colors.primary, 0.28f),
                    ),
            ) {
                if (liveBlur && lift > 0.001f) {
                    // Glass falls back to lighting on API 32 and below; add a real source blur
                    // beneath that fallback so both the bar and page are still sampled there.
                    val olderAndroidBlur = if (Build.VERSION.SDK_INT < 33) {
                        Modifier.hazeBlur(input = selectorInput, style = glassFallbackBlur)
                    } else Modifier
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer { alpha = lift.coerceIn(0f, 1f) }
                            .clip(CircleShape)
                            .then(olderAndroidBlur)
                            .hazeGlass(input = selectorInput, style = glassStyle),
                    )
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .border(0.8.dp, colors.primary.copy(alpha = if (dragging) 0.40f else 0.22f), CircleShape),
                )
            }
        }

        if (tabsAlpha > 0f) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = edge, bottom = 14.dp)
                    .offset(x = shellShift)
                    .width(shellWidth)
                    .height(barHeight)
                    .graphicsLayer { alpha = tabsAlpha }
                    .clip(CircleShape)
                    .selectableGroup()
                    .pointerInput(controlsEnabled, isRtl) {
                        if (controlsEnabled) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val slotPixels = size.width.toFloat() / navTabs.size
                                if (slotPixels <= 0f) return@awaitEachGesture
                                val physical = (down.position.x / slotPixels).toInt().coerceIn(0, navTabs.lastIndex)
                                val index = if (isRtl) navTabs.lastIndex - physical else physical
                                val source = interactions[index]
                                // The ripple is a clean circular indication clipped to the icon target.
                                val press = if (index != latestActiveIndex) {
                                    PressInteraction.Press(Offset(with(density) { 24.dp.toPx() }, with(density) { 24.dp.toPx() }))
                                } else null
                                var pressActive = press != null
                                if (press != null) source.tryEmit(press)
                                try {
                                    val held = awaitLongPressOrCancellation(down.id)
                                    if (held == null) {
                                        val up = currentEvent.changes.firstOrNull { it.id == down.id }
                                        val tap = up != null && up.changedToUpIgnoreConsumed() && !up.isConsumed &&
                                            (up.position - down.position).getDistance() <= viewConfiguration.touchSlop &&
                                            up.position.x in 0f..size.width.toFloat() &&
                                            up.position.y in 0f..size.height.toFloat()
                                        if (press != null) {
                                            source.tryEmit(if (tap) PressInteraction.Release(press) else PressInteraction.Cancel(press))
                                            pressActive = false
                                        }
                                        if (tap) latestOnSelect(navTabs[index])
                                    } else {
                                        if (press != null) {
                                            source.tryEmit(PressInteraction.Cancel(press))
                                            pressActive = false
                                        }
                                        val initialDelta = (held.position.x - down.position.x) * (if (isRtl) -1f else 1f)
                                        var raw = index.toFloat() + initialDelta / slotPixels
                                        dragPosition = raw
                                        dragging = true
                                        val released = drag(held.id) { change ->
                                            val delta = change.positionChange().x * (if (isRtl) -1f else 1f)
                                            raw = (raw + delta / slotPixels).coerceIn(-2f, navTabs.lastIndex + 2f)
                                            dragPosition = raw
                                            change.consume()
                                        }
                                        if (released) {
                                            currentEvent.changes.forEach { if (it.changedToUp()) it.consume() }
                                            latestOnSelect(navTabs[raw.roundToInt().coerceIn(0, navTabs.lastIndex)])
                                        }
                                    }
                                } finally {
                                    if (pressActive && press != null) source.tryEmit(PressInteraction.Cancel(press))
                                    dragging = false
                                }
                            }
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                navTabs.forEachIndexed { index, tab ->
                    NavigationTab(
                        destination = tab,
                        selected = activeIndex == index,
                        visuallyActive = visibleTab == index,
                        enabled = controlsEnabled,
                        interactionSource = interactions[index],
                        onSelect = { onSelectTab(tab) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (returnAlpha > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = edge, bottom = 14.dp)
                    .width(navWidth)
                    .height(barHeight)
                    .graphicsLayer { alpha = returnAlpha }
                    .clip(CircleShape)
                    .clickable(enabled = searching, role = Role.Button, onClick = onReturnToTab)
                    .semantics { contentDescription = "Return to ${previousTab.label}" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(previousTab.icon),
                    contentDescription = null,
                    tint = colors.onBackground,
                    modifier = Modifier.size(23.dp),
                )
            }
        }

        // No IME modifier is attached to the resting Search circle. Only a focused Search field
        // follows its own keyboard; the previous-tab circle always remains at the bottom.
        val searchImeModifier = if (searching && searchFocused) Modifier.imePadding() else Modifier
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .then(searchImeModifier)
                .navigationBarsPadding()
                .padding(end = edge, bottom = 14.dp)
                .width(searchWidth)
                .height(barHeight)
                .clip(CircleShape)
                .then(
                    if (liveBlur) Modifier.hazeBlur(input = pageInput, style = blurStyle)
                    else Modifier.background(navigationSolidColor(dark)),
                ),
            contentAlignment = Alignment.Center,
        ) {
            LiveGlassBorder(
                input = pageInput,
                liveBlur = liveBlur,
                background = colors.background,
                dark = dark,
                modifier = Modifier.matchParentSize(),
            )
            val searchIconAlpha = (1f - morph * 1.4f).coerceIn(0f, 1f)
            if (searchIconAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = searchIconAlpha }
                        .clip(CircleShape)
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
            val fieldAlpha = ((morph - 0.12f) / 0.70f).coerceIn(0f, 1f)
            if (morph > 0.06f) {
                SearchInput(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearchDone = onSearchDone,
                    onFocusChanged = { searchFocused = it },
                    enabled = searching,
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = fieldAlpha },
                )
            }
        }
    }
}

@Composable
private fun NavigationTab(
    destination: Destination,
    selected: Boolean,
    visuallyActive: Boolean,
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val tint by animateColorAsState(
        targetValue = if (visuallyActive) colors.onBackground else colors.onBackground.copy(alpha = 0.67f),
        animationSpec = tween(180),
        label = "tabIconTint",
    )
    val scale by animateFloatAsState(
        targetValue = if (visuallyActive) 1f else 0.94f,
        animationSpec = tween(180),
        label = "tabIconScale",
    )
    Box(modifier = modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .indication(interactionSource, ripple(bounded = true, radius = 24.dp, color = colors.onBackground))
                .semantics {
                    role = Role.Tab
                    this.selected = selected
                    contentDescription = destination.label
                    if (enabled) onClick { onSelect(); true } else disabled()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(destination.icon),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp).graphicsLayer { scaleX = scale; scaleY = scale },
            )
        }
    }
}

@Composable
private fun SearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchDone: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
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
                .onFocusChanged { onFocusChanged(it.isFocused) }
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
