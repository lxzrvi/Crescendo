package com.lxzrvi.crescendo.ui.home

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lxzrvi.crescendo.R
import com.lxzrvi.crescendo.ui.onboarding.GradientBlobBackground
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

private enum class Destination(
    val label: String,
    val subtitle: String,
    val cardLabel: String,
    val cardDetail: String,
    @param:DrawableRes val icon: Int,
) {
    Home(
        "Home", "Your music, all in one place.",
        "For you", "A little preview of what is coming.", R.drawable.ic_nav_home,
    ),
    Playlist(
        "Playlist", "A space for your collections.",
        "Playlist", "Your playlists will appear here.", R.drawable.ic_nav_playlist,
    ),
    Library(
        "Library", "Everything you love to listen to.",
        "Library item", "Your music library will appear here.", R.drawable.ic_nav_library,
    ),
    Settings(
        "Settings", "Make Crescendo feel like yours.",
        "Preference", "Settings will appear here.", R.drawable.ic_nav_settings,
    ),
    Search(
        "Search", "Find your next favorite sound.",
        "Search result", "Search results will appear here.", R.drawable.ic_nav_search,
    ),
}

/** Temporary, text-only pages with a shared floating navigation bar. */
@Composable
fun HomeScreen(gradientPhase: State<Float>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val hazeState = rememberHazeState()
    val pageState = rememberSaveableStateHolder()
    var destination by rememberSaveable { mutableStateOf(Destination.Home) }
    val fadeIn = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        fadeIn.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
    }
    BackHandler(enabled = destination != Destination.Home) {
        destination = Destination.Home
    }

    val tint = if (dark) Color(0xFF1D1D20).copy(alpha = 0.56f)
    else Color(0xFFF9F9F9).copy(alpha = 0.62f)
    val fallback = if (dark) Color(0xFF19191B).copy(alpha = 0.94f)
    else Color(0xFFF4F4F4).copy(alpha = 0.94f)
    val glassBorder = if (dark) Color.White.copy(alpha = 0.15f)
    else Color.Black.copy(alpha = 0.10f)
    val blurStyle = remember(tint, fallback, colors.background) {
        HazeBlurStyle {
            // Also opt in to Haze's experimental RenderScript path on Android 11 and below.
            blurEnabled(true)
            blurRadius(20.dp)
            noiseFactor(0f)
            backgroundColor(colors.background)
            colorEffects(listOf(HazeColorEffect.tint(tint)))
            fallbackColorEffect(HazeColorEffect.tint(fallback))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        // The source covers the full viewport, including the area beneath both controls.
        // Scrolling cards and the moving gradient are captured together, not a static image.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .background(colors.background),
        ) {
            GradientBlobBackground(
                phase = gradientPhase,
                modifier = Modifier.fillMaxSize(),
            )
            pageState.SaveableStateProvider(destination.name) {
                PlaceholderPage(
                    destination = destination,
                    fadeIn = fadeIn,
                )
            }
        }

        FloatingNavigation(
            selected = destination,
            hazeState = hazeState,
            blurStyle = blurStyle,
            glassBorder = glassBorder,
            onSelect = { destination = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun PlaceholderPage(destination: Destination, fadeIn: Animatable<Float, *>) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val listState = rememberLazyListState()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Bottom padding makes the last card reachable above the bar while allowing every card
    // to scroll behind it. Each destination retains its own position when switching tabs.
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .graphicsLayer { alpha = fadeIn.value },
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 28.dp,
            end = 20.dp,
            bottom = bottomInset + 104.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "heading") {
            Column(modifier = Modifier.padding(bottom = 18.dp)) {
                Text(
                    text = "CRESCENDO",
                    color = colors.primary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = destination.label,
                    color = colors.onBackground,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = destination.subtitle,
                    color = colors.onBackground.copy(alpha = 0.68f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        items(items = (1..36).toList(), key = { it }) { number ->
            PlaceholderCard(destination = destination, number = number, dark = dark)
        }
    }
}

@Composable
private fun PlaceholderCard(destination: Destination, number: Int, dark: Boolean) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val cardColor = if (dark) Color.White.copy(alpha = if (number % 2 == 0) 0.09f else 0.065f)
    else Color.Black.copy(alpha = if (number % 2 == 0) 0.055f else 0.035f)
    val cardBorder = colors.onBackground.copy(alpha = if (dark) 0.08f else 0.06f)
    val numberLabel = number.toString().padStart(2, '0')

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 94.dp)
            .clip(shape)
            .background(cardColor)
            .border(1.dp, cardBorder, shape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "${destination.cardLabel} $numberLabel",
            color = colors.onBackground,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = destination.cardDetail,
            color = colors.onBackground.copy(alpha = 0.64f),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun FloatingNavigation(
    selected: Destination,
    hazeState: HazeState,
    blurStyle: HazeBlurStyle,
    glassBorder: Color,
    onSelect: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val input = remember(hazeState) { HazeInput.Sources(hazeState) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
                .clip(CircleShape)
                .hazeBlur(input = input, style = blurStyle)
                .border(1.dp, glassBorder, CircleShape)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(Destination.Home, Destination.Playlist, Destination.Library, Destination.Settings)
                .forEach { item ->
                    NavigationItem(
                        destination = item,
                        selected = item == selected,
                        onSelect = { onSelect(item) },
                        modifier = Modifier.weight(1f),
                    )
                }
        }

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .hazeBlur(input = input, style = blurStyle)
                .border(1.dp, glassBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            NavigationItem(
                destination = Destination.Search,
                selected = selected == Destination.Search,
                onSelect = { onSelect(Destination.Search) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun NavigationItem(
    destination: Destination,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    Box(
        modifier = modifier
            .fillMaxHeight()
            .selectable(selected = selected, onClick = onSelect, role = Role.Tab),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (selected) ink.copy(alpha = 0.10f) else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(destination.icon),
                contentDescription = destination.label,
                tint = if (selected) ink else ink.copy(alpha = 0.60f),
                modifier = Modifier.size(26.dp),
            )
        }
    }
}
