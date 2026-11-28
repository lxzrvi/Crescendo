@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.lxzrvi.crescendo.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.lxzrvi.crescendo.R
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass
import kotlin.math.roundToInt

private enum class AllSongsMenu { Columns, Sort }
private data class GlassChoice<T>(val value: T, val label: String)
private val MenuPillHeight = 48.dp
private val MenuShape = RoundedCornerShape(24.dp)

/** Two independent pills, each of which grows into one continuous glass surface. */
@Composable
internal fun AllSongsHeading(
    songCount: Int,
    columns: Int,
    onColumnsChange: (Int) -> Unit,
    sort: AllSongsSort,
    onSortChange: (AllSongsSort) -> Unit,
    heroInput: HazeInput.Sources,
    pageInput: HazeInput.Sources,
    liveBlur: Boolean,
) {
    var openMenu by remember { mutableStateOf<AllSongsMenu?>(null) }
    val colors = MaterialTheme.colorScheme
    val columnChoices = remember { (2..5).map { GlassChoice(it, "$it columns") } }
    val sortChoices = remember { AllSongsSort.entries.map { GlassChoice(it, it.label) } }
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(start = ContentSidePadding, end = ContentSidePadding, top = 20.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text("All Songs", color = colors.onBackground, fontSize = 21.sp, lineHeight = 25.sp,
                letterSpacing = (-0.25).sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (songCount == 1) "1 song" else "$songCount songs",
                color = colors.onBackground.copy(alpha = 0.52f), fontSize = 12.sp,
                lineHeight = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp))
        }
        GlassDropdown(
            title = "Columns", description = "Song grid columns", icon = R.drawable.ic_home_grid,
            triggerWidth = 72.dp, expandedWidth = 194.dp, triggerValue = columns.toString(),
            choices = columnChoices, selected = columns, onSelect = onColumnsChange,
            expanded = openMenu == AllSongsMenu.Columns,
            onExpandedChange = { open ->
                if (open) openMenu = AllSongsMenu.Columns else if (openMenu == AllSongsMenu.Columns) openMenu = null
            },
            heroInput = heroInput, pageInput = pageInput, liveBlur = liveBlur,
        )
        Spacer(Modifier.width(7.dp))
        GlassDropdown(
            title = "Sort by", description = "Sort songs", icon = R.drawable.ic_home_sort,
            triggerWidth = 58.dp, expandedWidth = 224.dp, triggerValue = null,
            choices = sortChoices, selected = sort, onSelect = onSortChange,
            expanded = openMenu == AllSongsMenu.Sort,
            onExpandedChange = { open ->
                if (open) openMenu = AllSongsMenu.Sort else if (openMenu == AllSongsMenu.Sort) openMenu = null
            },
            heroInput = heroInput, pageInput = pageInput, liveBlur = liveBlur,
        )
    }
}

/**
 * The popup is a single rounded surface including its trigger, not a separate
 * rectangular menu below a pill. It is source-backed across the popup window;
 * the in-page pill instead samples only the hero background, avoiding a loop.
 */
@Composable
private fun <T> GlassDropdown(
    title: String,
    description: String,
    @DrawableRes icon: Int,
    triggerWidth: Dp,
    expandedWidth: Dp,
    triggerValue: String?,
    choices: List<GlassChoice<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    heroInput: HazeInput.Sources,
    pageInput: HazeInput.Sources,
    liveBlur: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val glassTint = if (dark) Color(0xFF252A34).copy(alpha = 0.78f) else Color(0xFFF8F8FC).copy(alpha = 0.78f)
    val glass = remember(glassTint) {
        GlassStyle.regular.then {
            shape(MenuShape)
            tint(glassTint)
            // Subtle optical light, without a drawn outline or rim.
            edgeShadow(Color.Transparent)
            specularIntensity(0.10f)
            ambientResponse(0.10f)
            edgeSoftness(1.dp)
        }
    }
    val sheen = remember(dark) {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = if (dark) 0.10f else 0.16f), Color.Transparent))
    }
    val selectedLabel = choices.firstOrNull { it.value == selected }?.label.orEmpty()
    val transition = updateTransition(expanded, label = "${title}GlassMenu")
    val progress by transition.animateFloat(
        transitionSpec = { tween(if (targetState) 310 else 240, easing = FastOutSlowInEasing) },
        label = "${title}GlassGrowth",
    ) { if (it) 1f else 0f }
    val popupShowing = expanded || transition.currentState || progress > 0f
    val density = LocalDensity.current
    val window = LocalWindowInfo.current.containerSize
    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var anchor by remember { mutableStateOf(Rect.Zero) }
    val desiredHeight = minOf(MenuPillHeight + 46.dp * choices.size + 10.dp, 440.dp)
    val desiredPx = with(density) { desiredHeight.roundToPx() }
    val placement = if (window.height > 0 && anchor.height > 0f) {
        allSongsMenuSpace(
            anchorTop = anchor.top.roundToInt(), anchorBottom = anchor.bottom.roundToInt(),
            windowHeight = window.height,
            topInset = with(density) { (statusInset + 8.dp).roundToPx() },
            bottomInset = with(density) { (navInset + 8.dp).roundToPx() },
            desiredHeight = desiredPx,
        )
    } else AllSongsMenuSpace(opensAbove = false, heightPx = desiredPx)
    val maxWidth = with(density) {
        if (window.width > 0) (window.width - 16.dp.roundToPx()).coerceAtLeast(1).toDp() else expandedWidth
    }
    val panelWidth = expandedWidth.coerceAtMost(maxWidth).coerceAtLeast(triggerWidth)
    val panelHeight = with(density) { placement.heightPx.toDp() }.coerceAtLeast(MenuPillHeight)
    val animatedWidth = triggerWidth + (panelWidth - triggerWidth) * progress
    val animatedHeight = MenuPillHeight + (panelHeight - MenuPillHeight) * progress
    val provider = remember(placement.opensAbove) { ConnectedMenuPositionProvider(placement.opensAbove) }

    Box(
        modifier = Modifier.width(triggerWidth).height(MenuPillHeight)
            .onGloballyPositioned { anchor = it.boundsInWindow(clipBounds = false) },
    ) {
        if (!popupShowing) {
            Box(
                modifier = Modifier.matchParentSize().glassSurface(heroInput, glass, glassTint, sheen, liveBlur)
                    .clickable(role = Role.Button) { onExpandedChange(true) }
                    .semantics {
                        contentDescription = description
                        stateDescription = "$selectedLabel, collapsed"
                    },
                contentAlignment = Alignment.Center,
            ) { GlassTriggerIcon(icon, triggerValue, opensAbove = false, expanded = false) }
        }
        if (popupShowing) {
            Popup(
                popupPositionProvider = provider,
                onDismissRequest = { if (expanded) onExpandedChange(false) },
                properties = PopupProperties(focusable = true, dismissOnBackPress = true,
                    dismissOnClickOutside = true, clippingEnabled = false),
            ) {
                Column(
                    Modifier.width(animatedWidth).height(animatedHeight)
                        .glassSurface(pageInput, glass, glassTint, sheen, liveBlur),
                ) {
                    if (placement.opensAbove) {
                        MenuChoices(choices, selected, expanded, progress, onSelect = { value ->
                            onSelect(value); onExpandedChange(false)
                        }, modifier = Modifier.weight(1f))
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().height(MenuPillHeight)
                            .clickable(role = Role.Button) { onExpandedChange(false) }
                            .semantics {
                                contentDescription = description
                                stateDescription = "$selectedLabel, expanded"
                            },
                    ) {
                        if (progress > 0.45f) {
                            Text(title, color = colors.onBackground.copy(alpha = 0.74f),
                                fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.align(Alignment.CenterStart)
                                    .padding(start = 16.dp, end = triggerWidth)
                                    .graphicsLayer { alpha = ((progress - 0.45f) / 0.35f).coerceIn(0f, 1f) })
                        }
                        Box(Modifier.align(Alignment.CenterEnd).width(triggerWidth).fillMaxHeight(),
                            contentAlignment = Alignment.Center) {
                            GlassTriggerIcon(icon, triggerValue, placement.opensAbove, expanded = true)
                        }
                    }
                    if (!placement.opensAbove) {
                        MenuChoices(choices, selected, expanded, progress, onSelect = { value ->
                            onSelect(value); onExpandedChange(false)
                        }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun Modifier.glassSurface(
    input: HazeInput.Sources,
    style: GlassStyle,
    fallback: Color,
    sheen: Brush,
    liveBlur: Boolean,
): Modifier = clip(MenuShape)
    .then(if (liveBlur) Modifier.hazeGlass(input = input, style = style) else Modifier.background(fallback))
    .background(sheen)

@Composable
private fun GlassTriggerIcon(@DrawableRes icon: Int, value: String?, opensAbove: Boolean, expanded: Boolean) {
    val foreground = MaterialTheme.colorScheme.onBackground
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Icon(painterResource(icon), contentDescription = null, tint = foreground, modifier = Modifier.size(18.dp))
        if (value != null) {
            Spacer(Modifier.width(4.dp))
            Text(value, color = foreground, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
        }
        Spacer(Modifier.width(4.dp))
        Icon(painterResource(R.drawable.ic_onboarding_chevron), contentDescription = null,
            tint = foreground.copy(alpha = 0.75f), modifier = Modifier.size(9.dp).graphicsLayer {
                rotationZ = if (expanded && opensAbove) -90f else 90f
            })
    }
}

@Composable
private fun <T> MenuChoices(
    choices: List<GlassChoice<T>>,
    selected: T,
    expanded: Boolean,
    progress: Float,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val scroll = rememberScrollState()
    Column(
        modifier = modifier.fillMaxWidth().clipToBounds()
            .graphicsLayer { alpha = ((progress - 0.20f) / 0.55f).coerceIn(0f, 1f) }
            .verticalScroll(scroll).selectableGroup().padding(horizontal = 6.dp, vertical = 5.dp),
    ) {
        choices.forEach { choice ->
            val checked = choice.value == selected
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(RoundedCornerShape(15.dp))
                    .then(if (checked) Modifier.background(colors.onBackground.copy(alpha = 0.10f)) else Modifier)
                    .selectable(selected = checked, enabled = expanded && progress > 0.70f,
                        role = Role.RadioButton, onClick = { onSelect(choice.value) })
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(choice.label, color = colors.onBackground, fontSize = 14.sp, lineHeight = 18.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (checked) {
                    Spacer(Modifier.width(8.dp))
                    Icon(painterResource(R.drawable.ic_home_check), contentDescription = null,
                        tint = colors.onBackground, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

private data class ConnectedMenuPositionProvider(val opensAbove: Boolean) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val result = allSongsMenuOffset(
            anchorLeft = anchorBounds.left, anchorTop = anchorBounds.top,
            anchorRight = anchorBounds.right, anchorBottom = anchorBounds.bottom,
            windowWidth = windowSize.width, windowHeight = windowSize.height,
            menuWidth = popupContentSize.width, menuHeight = popupContentSize.height,
            opensAbove = opensAbove, rtl = layoutDirection == LayoutDirection.Rtl,
        )
        return IntOffset(result.x, result.y)
    }
}
