package com.lxzrvi.crescendo.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.PrismalGlass
import com.styropyr0.prismal.depth.PrismalDepthShadow
import com.styropyr0.prismal.drawPlainPrismalGlass
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import com.styropyr0.prismal.shapes.PrismalCapsule
import com.styropyr0.prismal.sources.rememberPrismalCanvasSource
import com.styropyr0.prismal.specular.PrismalSpecular
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

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
    pageBackdrop: PrismalBackdrop,
    pageInput: HazeInput.Sources,
    liveBlur: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val isLightTheme = colors.background.luminance() >= 0.5f
    val foreground = if (isLightTheme) Color.Black else Color.White
    val searching = selected == Destination.Search
    val activeTab = if (searching) previousTab else selected
    val activeIndex = navTabs.indexOf(activeTab).coerceAtLeast(0)
    val density = LocalDensity.current
    var searchFocused by remember { mutableStateOf(false) }
    var navPressProgress by remember { mutableStateOf(0f) }
    val searchInteraction = remember { MutableInteractionSource() }
    val searchPressed by searchInteraction.collectIsPressedAsState()
    LaunchedEffect(searching) {
        if (!searching) searchFocused = false
    }

    // When the preference is off, even Prismal's nested droplet samples an opaque
    // color instead of the page. The old preference therefore remains meaningful.
    val solidColor = navigationSolidColor(dark = !isLightTheme)
    val solidDraw: DrawScope.() -> Unit = remember(solidColor) { { drawRect(solidColor) } }
    val solidBackdrop = rememberPrismalCanvasSource(solidDraw)
    val backdrop = if (liveBlur) pageBackdrop else solidBackdrop
    val legacySearchStyle = remember {
        HazeBlurStyle {
            // Prismal's Android 11-and-earlier tier samples glass but cannot blur it.
            // Haze uses its slower asynchronous RenderScript path on those devices.
            blurEnabled(true)
            blurRadius(10.dp)
            backgroundColor(Color.Transparent)
            noiseFactor(0f)
            colorEffects(emptyList())
            fallbackColorEffect(HazeColorEffect.tint(Color.Transparent))
        }
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
    val touchConnection by animateFloatAsState(
        targetValue = maxOf(navPressProgress, if (searchPressed) 1f else 0f),
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
        label = "liquidTouchConnection",
    )
    // Joining, stretching and splitting is synchronized to the capsule morph.
    val transitionConnection = sin((morph.coerceIn(0f, 1f) * PI).toFloat()).coerceIn(0f, 1f)
    val connection = (maxOf(touchConnection, transitionConnection) * (1f - keyboardGrowth))
        .coerceIn(0f, 1f)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val edge = ContentSidePadding
        val gap = 10.dp
        val barHeight = 64.dp // Prismal's actual bottom-tabs capsule height.
        val availableWidth = (maxWidth - edge * 2).coerceAtLeast(barHeight * 2 + gap)
        val wideWidth = (availableWidth - barHeight - gap).coerceAtLeast(barHeight)
        val navWidth = wideWidth * (1f - morph) + barHeight * morph
        val searchAtRest = barHeight * (1f - morph) + wideWidth * morph
        val searchWidth = searchAtRest * (1f - keyboardGrowth) + availableWidth * keyboardGrowth
        val indicatorAlpha = (1f - morph * 1.55f).coerceIn(0f, 1f)
        val tabsAlpha = (1f - morph * 1.35f).coerceIn(0f, 1f)
        val returnAlpha = (morph * 1.35f - 0.25f).coerceIn(0f, 1f)
        val controlsEnabled = !searching && morph < 0.25f

        // A real sampled-glass bridge sits beneath the two capsules. Its concave
        // neck grows and shrinks like liquid; it has no touch target of its own.
        if (connection > 0.005f && !(searching && searchFocused && imeVisible)) {
            val overlap = 16.dp
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = edge + navWidth - overlap, bottom = 14.dp)
                    .width(gap + overlap * 2)
                    .height(barHeight)
                    .graphicsLayer { alpha = connection }
                    .liquidBridge(backdrop, density, isLightTheme, connection),
            )
        }

        PrismalNavigationTabs(
            tabs = navTabs,
            selectedIndex = activeIndex,
            onTabSelected = { index -> onSelectTab(navTabs[index.coerceIn(0, navTabs.lastIndex)]) },
            backdrop = backdrop,
            isLightTheme = isLightTheme,
            controlsEnabled = controlsEnabled,
            tabsAlpha = tabsAlpha,
            indicatorAlpha = indicatorAlpha,
            connectionProgress = connection,
            onPressProgress = { navPressProgress = it },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = edge, bottom = 14.dp)
                .width(navWidth)
                .height(barHeight),
        )

        if (returnAlpha > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = edge, bottom = 14.dp)
                    .width(navWidth)
                    .height(barHeight)
                    .graphicsLayer { alpha = returnAlpha }
                    .clip(PrismalCapsule())
                    .clickable(enabled = searching, role = Role.Button, onClick = onReturnToTab)
                    .semantics { contentDescription = "Return to ${previousTab.label}" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(previousTab.icon),
                    contentDescription = null,
                    tint = foreground,
                    modifier = Modifier.size(27.dp),
                )
            }
        }

        // Only a focused Search field follows its IME; the return circle stays at
        // the bottom, as in Crescendo's original Search transition.
        val searchImeModifier = if (searching && searchFocused) Modifier.imePadding() else Modifier
        val searchSurface = if (liveBlur && !PrismalGlass.supportsBackdropBlur) {
            Modifier
                .clip(PrismalCapsule())
                .hazeBlur(input = pageInput, style = legacySearchStyle, performanceMode = HazePerformanceMode.Performance)
                .background(if (isLightTheme) Color(0xFFFAFAFA).copy(alpha = 0.4f) else Color(0xFF121212).copy(alpha = 0.4f))
                .border(
                    0.5.dp,
                    (if (isLightTheme) Color.Black else Color.White).copy(alpha = 0.16f * (1f - connection * 0.75f)),
                    PrismalCapsule(),
                )
        } else {
            Modifier.prismalNavigationSurface(backdrop, density, isLightTheme, connection).clip(PrismalCapsule())
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .then(searchImeModifier)
                .navigationBarsPadding()
                .padding(end = edge, bottom = 14.dp)
                .width(searchWidth)
                .height(barHeight)
                .then(searchSurface),
            contentAlignment = Alignment.Center,
        ) {
            val searchIconAlpha = (1f - morph * 1.4f).coerceIn(0f, 1f)
            if (searchIconAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = searchIconAlpha }
                        .clip(PrismalCapsule())
                        .clickable(
                            interactionSource = searchInteraction,
                            indication = null,
                            enabled = !searching,
                            role = Role.Button,
                            onClick = onOpenSearch,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Destination.Search.icon),
                        contentDescription = "Open search",
                        tint = foreground,
                        modifier = Modifier.size(27.dp),
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
                    foreground = foreground,
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = fieldAlpha },
                )
            }
        }
    }
}

/** The same base capsule material used by Prismal's tab bar, for the separate Search control. */
private fun Modifier.prismalNavigationSurface(
    backdrop: PrismalBackdrop,
    density: Density,
    isLightTheme: Boolean,
    connection: Float,
): Modifier = drawPrismalGlass(
    backdrop = backdrop,
    shape = { PrismalCapsule() },
    specular = { PrismalSpecular.Default.copy(alpha = 1f - connection * 0.75f) },
    depthShadow = { PrismalDepthShadow.Default.copy(alpha = 1f - connection * 0.85f) },
    effects = {
        applyPrismalGlassEffects(
            density = density,
            adaptiveLuminance = false,
            luminance = 0.5f,
            // Search visibly blurs the live content directly beneath it.
            blurRadiusPx = with(density) { 10.dp.toPx() },
            refractionHeightPx = with(density) { 16.dp.toPx() },
            refractionAmountPx = with(density) { 28.dp.toPx() },
        )
    },
    onDrawSurface = {
        drawRect(
            if (isLightTheme) Color(0xFFFAFAFA).copy(alpha = 0.4f)
            else Color(0xFF121212).copy(alpha = 0.4f),
        )
    },
)

/** The gap is filled by a continuously curved, live-sampled liquid neck. */
private fun Modifier.liquidBridge(
    backdrop: PrismalBackdrop,
    density: Density,
    isLightTheme: Boolean,
    connection: Float,
): Modifier = drawPlainPrismalGlass(
    backdrop = backdrop,
    shape = { LiquidBridgeShape(connection) },
    effects = {
        // A generic curved shape cannot use Prismal's rounded-rect lens, but can
        // sample and blur the same page pixels as its two neighboring capsules.
        applyPrismalGlassEffects(
            density = density,
            adaptiveLuminance = false,
            luminance = 0.5f,
            blurRadiusPx = with(density) { 6.dp.toPx() },
            refractionHeightPx = 0f,
            refractionAmountPx = 0f,
        )
    },
    onDrawSurface = {
        drawRect(
            if (isLightTheme) Color(0xFFFAFAFA).copy(alpha = 0.4f)
            else Color(0xFF121212).copy(alpha = 0.4f),
        )
    },
    onDrawFront = {
        drawPath(
            path = LiquidBridgeShape(connection).path(size, density),
            color = if (isLightTheme) Color.Black.copy(alpha = 0.13f) else Color.White.copy(alpha = 0.28f),
            style = Stroke(width = with(density) { 1.dp.toPx() }),
        )
    },
)

private data class LiquidBridgeShape(val connection: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(path(size, density))

    fun path(size: Size, density: Density): Path {
        val width = size.width
        val height = size.height
        val radius = height / 2f
        val overlap = with(density) { 16.dp.toPx() }.coerceAtMost(width / 2f)
        val circleX = (radius - overlap).coerceAtLeast(0f)
        val circleY = sqrt((radius * radius - circleX * circleX).coerceAtLeast(0f))
        val edgeY = radius - circleY
        val tangent = if (circleY > 0f) circleX / circleY else 0f
        val lead = overlap * 0.24f
        val midX = width / 2f
        val halfNeck = radius * (0.04f + 0.57f * connection.coerceIn(0f, 1f))
        val topMiddle = radius - halfNeck
        val bottomMiddle = radius + halfNeck
        val path = Path().apply {
            moveTo(0f, edgeY)
            cubicTo(lead, edgeY + lead * tangent, midX * 0.55f, topMiddle, midX, topMiddle)
            cubicTo(midX + midX * 0.45f, topMiddle, width - lead, edgeY + lead * tangent, width, edgeY)
            lineTo(width, height - edgeY)
            cubicTo(width - lead, height - edgeY - lead * tangent, midX + midX * 0.45f, bottomMiddle, midX, bottomMiddle)
            cubicTo(midX * 0.55f, bottomMiddle, lead, height - edgeY - lead * tangent, 0f, height - edgeY)
            close()
        }
        return path
    }
}

@Composable
private fun SearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchDone: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    enabled: Boolean,
    foreground: Color,
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
            tint = foreground,
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = foreground),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchDone() }),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { onFocusChanged(it.isFocused) }
                .semantics { contentDescription = "Search songs, artists, or albums" },
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search your music",
                            color = foreground.copy(alpha = 0.55f),
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
                    .clip(PrismalCapsule())
                    .clickable(enabled = enabled, role = Role.Button) { onQueryChange("") }
                    .semantics { contentDescription = "Clear search" },
                contentAlignment = Alignment.Center,
            ) {
                Text("×", color = foreground.copy(alpha = 0.75f), fontSize = 23.sp)
            }
        }
    }
}
