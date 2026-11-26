/*
 * Adapted from PrismalAGSL v1.0.7, PrismalGlassBottomTabs and
 * PrismalGlassBottomTab. Copyright (c) 2026 Saurav Sajeev.
 * Licensed under the MIT License; see assets/licenses/PrismalAGSL-MIT.txt.
 *
 * The glass layers, effects, sizes and spring model follow the upstream
 * component. Crescendo keeps its own tab semantics and long-press-to-drag
 * selection instead of replacing its navigation with the demo's logic.
 */
package com.lxzrvi.crescendo.ui.home

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.PrismalGlass
import com.styropyr0.prismal.PrismalGlassPipeline
import com.styropyr0.prismal.depth.PrismalDepthInset
import com.styropyr0.prismal.depth.PrismalDepthShadow
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import com.styropyr0.prismal.effects.prismalLens
import com.styropyr0.prismal.interactive.PrismalPressRipple
import com.styropyr0.prismal.interactive.PrismalSpringMotion
import com.styropyr0.prismal.shapes.PrismalCapsule
import com.styropyr0.prismal.sources.prismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalMergedSource
import com.styropyr0.prismal.sources.rememberPrismalWrappedSource
import com.styropyr0.prismal.specular.PrismalSpecular
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/** The Prismal tab renderer, with Crescendo's existing selection and drag policy. */
@Composable
internal fun PrismalNavigationTabs(
    tabs: List<Destination>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    backdrop: PrismalBackdrop,
    isLightTheme: Boolean,
    controlsEnabled: Boolean,
    tabsAlpha: Float,
    indicatorAlpha: Float,
    connectionProgress: Float,
    onPressProgress: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabsCount = tabs.size
    val tabsBackdrop = rememberPrismalGlassLayer()
    tabsBackdrop.readSamplingState()
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val latestSelectedIndex by rememberUpdatedState(selectedIndex)
    val latestOnTabSelected by rememberUpdatedState(onTabSelected)
    val latestOnPressProgress by rememberUpdatedState(onPressProgress)
    var dragHighlightIndex by remember { mutableStateOf<Int?>(null) }
    var dragging by remember { mutableStateOf(false) }
    val containerColor = if (isLightTheme) Color(0xFFFAFAFA).copy(alpha = 0.4f)
    else Color(0xFF121212).copy(alpha = 0.4f)
    val dropletTint = MaterialTheme.colorScheme.primary
    // The Prismal catalog uses a lower press glow in dark mode.
    val pressGlow = if (isLightTheme) 0.7f else 0.25f

    BoxWithConstraints(modifier, contentAlignment = Alignment.CenterStart) {
        val tabWidth = ((constraints.maxWidth.toFloat() - with(density) { 8.dp.toPx() }) / tabsCount)
            .coerceAtLeast(1f)
        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density, constraints.maxWidth) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth.coerceAtLeast(1)).coerceIn(-1f, 1f)
                with(density) { 4.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction)) }
            }
        }
        // The upstream spring is retained; gesture events below drive it after a long press.
        val motion = remember(scope, tabsCount, tabWidth, isLtr) {
            PrismalSpringMotion(
                animationScope = scope,
                initialValue = selectedIndex.toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = {},
                onDragStopped = {},
                onDrag = { _, _ -> },
            )
        }
        LaunchedEffect(selectedIndex, motion) {
            if (!dragging && abs(motion.targetValue - selectedIndex) > 0.001f) {
                motion.animateToValue(selectedIndex.toFloat())
            }
            if (!dragging) dragHighlightIndex = null
        }
        // Let the neighboring Search surface join only as the raised droplet reaches it.
        LaunchedEffect(motion, controlsEnabled) {
            snapshotFlow {
                val proximity = ((motion.value - (tabsCount - 1.65f)) / 0.65f).coerceIn(0f, 1f)
                if (controlsEnabled) motion.pressProgress.coerceIn(0f, 1f) * proximity else 0f
            }.collect { latestOnPressProgress(it) }
        }
        DisposableEffect(motion) { onDispose { latestOnPressProgress(0f) } }
        LaunchedEffect(view) {
            snapshotFlow { dragHighlightIndex ?: latestSelectedIndex }
                .drop(1)
                .collect {
                    view.performHapticFeedback(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                            HapticFeedbackConstants.SEGMENT_TICK
                        else HapticFeedbackConstants.CLOCK_TICK,
                    )
                }
        }

        val lensAvailable = PrismalGlass.pipeline == PrismalGlassPipeline.Liquid
        val contentShrink = if (lensAvailable) 0.92f else 1f
        val shrunkTabsBackdrop = rememberPrismalWrappedSource(tabsBackdrop) { drawTabs ->
            val progress = if (lensAvailable) motion.pressProgress else 0f
            scale(
                scaleX = lerp(1f, 0.92f, progress),
                scaleY = lerp(1f, 0.92f, progress),
            ) { drawTabs() }
        }
        val mergedDropletBackdrop = rememberPrismalMergedSource(backdrop, shrunkTabsBackdrop)
        val dropletBackdrop = if (PrismalGlass.pipeline == PrismalGlassPipeline.Legacy) {
            shrunkTabsBackdrop
        } else mergedDropletBackdrop
        val interactiveSpecular = remember(scope, tabWidth, isLtr, pressGlow) {
            PrismalPressRipple(
                animationScope = scope,
                intensity = pressGlow,
                position = { size, _ ->
                    Offset(
                        if (isLtr) (motion.value + 0.5f) * tabWidth + panelOffset
                        else size.width - (motion.value + 0.5f) * tabWidth + panelOffset,
                        size.height / 2f,
                    )
                },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .selectableGroup()
                .pointerInput(controlsEnabled, isLtr, tabWidth, motion) {
                    if (controlsEnabled) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val inset = with(density) { 4.dp.toPx() }
                            val physical = ((down.position.x - inset) / tabWidth).toInt()
                                .coerceIn(0, tabs.lastIndex)
                            val index = if (isLtr) physical else tabs.lastIndex - physical
                            // Prismal lifts the selected droplet immediately on press,
                            // but Crescendo still waits for a long press before dragging.
                            val pressedDroplet = index == latestSelectedIndex
                            if (pressedDroplet) motion.press()
                            var started = false
                            var completed = false
                            try {
                                val held = awaitLongPressOrCancellation(down.id)
                                if (held == null) {
                                    val up = currentEvent.changes.firstOrNull { it.id == down.id }
                                    val tap = up != null && up.changedToUpIgnoreConsumed() && !up.isConsumed &&
                                        (up.position - down.position).getDistance() <= viewConfiguration.touchSlop &&
                                        up.position.x in 0f..size.width.toFloat() &&
                                        up.position.y in 0f..size.height.toFloat()
                                    if (tap) latestOnTabSelected(index)
                                } else {
                                    started = true
                                    dragging = true
                                    val direction = if (isLtr) 1f else -1f
                                    var raw = index.toFloat() + (held.position.x - down.position.x) * direction / tabWidth
                                    raw = raw.coerceIn(-2f, tabs.lastIndex + 2f)
                                    dragHighlightIndex = raw.roundToInt().coerceIn(0, tabs.lastIndex)
                                    if (!pressedDroplet) motion.press()
                                    motion.updateValue(raw)
                                    val released = drag(held.id) { change ->
                                        val dx = change.positionChange().x
                                        raw = (raw + dx * direction / tabWidth).coerceIn(-2f, tabs.lastIndex + 2f)
                                        motion.updateValue(raw)
                                        dragHighlightIndex = raw.roundToInt().coerceIn(0, tabs.lastIndex)
                                        scope.launch { offsetAnimation.snapTo(offsetAnimation.value + dx) }
                                        change.consume()
                                    }
                                    if (released) {
                                        currentEvent.changes.forEach { if (it.changedToUp()) it.consume() }
                                        val target = raw.roundToInt().coerceIn(0, tabs.lastIndex)
                                        motion.animateToValue(target.toFloat())
                                        latestOnTabSelected(target)
                                        completed = true
                                    }
                                }
                            } finally {
                                if (started) {
                                    if (!completed) motion.animateToValue(latestSelectedIndex.toFloat())
                                    dragging = false
                                    // Cleared immediately on cancel or when the selection already agrees;
                                    // otherwise the selectedIndex effect clears it when the parent updates.
                                    if (!completed || dragHighlightIndex == latestSelectedIndex) {
                                        dragHighlightIndex = null
                                    }
                                    scope.launch { offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f)) }
                                } else if (pressedDroplet) {
                                    motion.release()
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            // The visible 64 dp capsule is the upstream Prismal base layer.
            Row(
                Modifier
                    .graphicsLayer { translationX = panelOffset }
                    .drawPrismalGlass(
                        backdrop = backdrop,
                        shape = { PrismalCapsule() },
                        specular = { PrismalSpecular.Default.copy(alpha = 1f - connectionProgress * 0.75f) },
                        depthShadow = { PrismalDepthShadow.Default.copy(alpha = 1f - connectionProgress * 0.85f) },
                        effects = {
                            applyPrismalGlassEffects(
                                density = density,
                                adaptiveLuminance = false,
                                luminance = 0.5f,
                                blurRadiusPx = with(density) { 4.dp.toPx() },
                                refractionHeightPx = with(density) { 16.dp.toPx() },
                                refractionAmountPx = with(density) { 28.dp.toPx() },
                            )
                        },
                        layerBlock = {
                            val scale = lerp(1f, 1f + 16.dp.toPx() / size.width, motion.pressProgress)
                            scaleX = scale
                            scaleY = scale
                        },
                        onDrawSurface = { drawRect(containerColor) },
                    )
                    .height(64.dp)
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, destination ->
                    PrismalNavigationItem(
                        destination = destination,
                        selected = selectedIndex == index,
                        enabled = controlsEnabled,
                        captureOnly = false,
                        alpha = tabsAlpha,
                        scale = { lerp(1f, 1.05f, motion.pressProgress) },
                        isLightTheme = isLightTheme,
                        onSelect = { latestOnTabSelected(index) },
                    )
                }
            }

            // As in Prismal, the second (invisible) 56 dp row is the droplet's
            // live nested source, recolored with Crescendo's red accent.
            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .prismalGlassLayer(tabsBackdrop)
                    .graphicsLayer { translationX = panelOffset }
                    .drawPrismalGlass(
                        backdrop = backdrop,
                        shape = { PrismalCapsule() },
                        effects = {
                            applyPrismalGlassEffects(
                                density = density,
                                adaptiveLuminance = false,
                                luminance = 0.5f,
                                blurRadiusPx = with(density) { 4.dp.toPx() },
                                refractionHeightPx = with(density) { 16.dp.toPx() },
                                refractionAmountPx = with(density) { 28.dp.toPx() },
                            )
                        },
                        specular = { PrismalSpecular.Default.copy(alpha = motion.pressProgress) },
                        onDrawSurface = { drawRect(containerColor) },
                    )
                    .then(interactiveSpecular.modifier)
                    .height(56.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .drawWithContent {
                        val paint = Paint().apply { colorFilter = ColorFilter.tint(dropletTint) }
                        drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
                        drawContent()
                        drawContext.canvas.restore()
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, destination ->
                    PrismalNavigationItem(
                        destination = destination,
                        selected = selectedIndex == index,
                        enabled = false,
                        captureOnly = true,
                        alpha = tabsAlpha,
                        scale = { lerp(1f, 1.05f / contentShrink, motion.pressProgress) },
                        isLightTheme = isLightTheme,
                        onSelect = {},
                    )
                }
            }

            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .graphicsLayer {
                        // The child is already laid out at logical start (right in RTL).
                        translationX = if (isLtr) motion.value * tabWidth + panelOffset
                        else -motion.value * tabWidth + panelOffset
                        alpha = indicatorAlpha
                    }
                    .then(if (controlsEnabled) interactiveSpecular.gestureModifier else Modifier)
                    .height(56.dp)
                    .fillMaxWidth(1f / tabsCount)
                    .drawPrismalGlass(
                        backdrop = dropletBackdrop,
                        shape = { PrismalCapsule() },
                        effects = {
                            val progress = motion.pressProgress
                            prismalLens(
                                refractionHeight = with(density) { 14.dp.toPx() } * progress,
                                refractionAmount = with(density) { 14.dp.toPx() } * progress,
                                depthEffect = true,
                                chromaticAberration = 0.45f,
                            )
                        },
                        specular = { PrismalSpecular.Default.copy(alpha = motion.pressProgress) },
                        depthShadow = { PrismalDepthShadow(alpha = motion.pressProgress) },
                        depthInset = {
                            PrismalDepthInset(
                                radius = 8.dp * lerp(0.75f, 1f, motion.pressProgress),
                                alpha = lerp(0.5f, 1f, motion.pressProgress),
                            )
                        },
                        layerBlock = {
                            val velocity = motion.velocity / 10f
                            scaleX = motion.scaleX / (1f - (velocity * 0.75f).coerceIn(-0.2f, 0.2f))
                            scaleY = motion.scaleY * (1f - (velocity * 0.25f).coerceIn(-0.2f, 0.2f))
                        },
                        onDrawSurface = {
                            val progress = motion.pressProgress
                            drawRect(
                                if (isLightTheme) Color.Black.copy(alpha = 0.1f)
                                else Color.White.copy(alpha = 0.1f),
                                alpha = 1f - progress,
                            )
                            drawRect(Color.Black.copy(alpha = 0.03f * progress))
                            val glow = progress * pressGlow.coerceIn(0f, 1f)
                            if (glow > 0f) {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.30f * glow),
                                            Color.White.copy(alpha = 0.06f * glow),
                                        ),
                                    ),
                                    blendMode = BlendMode.Plus,
                                )
                            }
                        },
                    ),
            )
        }
    }
}

/** Prismal demo's 27 dp icon and 10 sp label, with Crescendo accessibility. */
@Composable
private fun RowScope.PrismalNavigationItem(
    destination: Destination,
    selected: Boolean,
    enabled: Boolean,
    captureOnly: Boolean,
    alpha: Float,
    scale: () -> Float,
    isLightTheme: Boolean,
    onSelect: () -> Unit,
) {
    val input = if (captureOnly) Modifier else Modifier
        .onKeyEvent { event ->
            if (enabled && event.type == KeyEventType.KeyUp &&
                (event.key == Key.Enter || event.key == Key.Spacebar || event.key == Key.DirectionCenter)
            ) {
                onSelect()
                true
            } else false
        }
        .focusable(enabled = enabled)
        .semantics {
            role = Role.Tab
            this.selected = selected
            contentDescription = destination.label
            if (enabled) onClick { onSelect(); true } else disabled()
        }
    val foreground = if (selected && !captureOnly) MaterialTheme.colorScheme.primary
    else if (isLightTheme) Color.Black else Color.White
    Column(
        Modifier
            .clip(PrismalCapsule())
            .then(input)
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                val s = scale()
                scaleX = s
                scaleY = s
                this.alpha = alpha
            },
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(destination.icon),
            contentDescription = null,
            modifier = Modifier.size(27.dp),
            tint = foreground,
        )
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = foreground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}
