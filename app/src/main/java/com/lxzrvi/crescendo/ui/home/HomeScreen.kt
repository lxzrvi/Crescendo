@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lxzrvi.crescendo.R
import com.lxzrvi.crescendo.ui.onboarding.GradientBlobBackground
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay

internal enum class Destination(
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
        "Preference", "More settings will appear here.", R.drawable.ic_nav_settings,
    ),
    Search(
        "Search", "Search the sample cards for now.",
        "Search result", "Your search results will appear here.", R.drawable.ic_nav_search,
    ),
}

internal data class SampleCard(val destination: Destination, val number: Int) {
    val title: String get() = "${destination.cardLabel} ${number.toString().padStart(2, '0')}"
    val key: String get() = "sample:${destination.name}:$number"
}

private val searchableSamples = listOf(Destination.Home, Destination.Playlist, Destination.Library)
    .flatMap { destination -> (1..36).map { SampleCard(destination, it) } }

internal fun placeholderSurfaceColor(dark: Boolean, number: Int): Color =
    if (dark) Color.White.copy(alpha = if (number % 2 == 0) 0.09f else 0.065f)
    else Color.Black.copy(alpha = if (number % 2 == 0) 0.055f else 0.035f)

/** Text-only sample pages with an independently animated nav and search control. */
@Composable
fun HomeScreen(gradientPhase: State<Float>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val imeVisible = WindowInsets.isImeVisible
    val prefs = remember(context) { NavigationAppearancePrefs(context) }
    var appearance by remember(prefs) { mutableStateOf(prefs.load()) }
    var destination by rememberSaveable { mutableStateOf(Destination.Home) }
    var previousTab by rememberSaveable { mutableStateOf(Destination.Home) }
    var query by rememberSaveable { mutableStateOf("") }
    val hazeState = rememberHazeState()
    val fadeIn = remember { Animatable(0f) }
    val rootSize = remember { mutableStateOf(IntSize.Zero) }
    val listOrigin = remember { mutableStateOf(Offset.Zero) }

    // Remember all five states independently, so returning to a page restores its scroll.
    val homeState = rememberLazyListState()
    val playlistState = rememberLazyListState()
    val libraryState = rememberLazyListState()
    val settingsState = rememberLazyListState()
    val searchState = rememberLazyListState()
    val listState = when (destination) {
        Destination.Home -> homeState
        Destination.Playlist -> playlistState
        Destination.Library -> libraryState
        Destination.Settings -> settingsState
        Destination.Search -> searchState
    }
    val blurStyle = rememberNavigationBlurStyle(appearance, colors.background)
    val borderOverlay = if (appearance.liveBlur && appearance.radius > 0f) {
        navigationTint(dark, appearance.type)
    } else navigationFallbackTint(dark)

    LaunchedEffect(Unit) {
        fadeIn.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
    }
    // Debounce slider writes while the visual result still updates on every drag frame.
    LaunchedEffect(appearance) {
        delay(250)
        prefs.save(appearance)
    }

    fun closeSearch() {
        keyboard?.hide()
        focusManager.clearFocus(force = true)
        destination = previousTab
    }

    BackHandler(enabled = destination != Destination.Home) {
        when {
            destination == Destination.Search && imeVisible -> {
                keyboard?.hide()
                focusManager.clearFocus(force = true)
            }
            destination == Destination.Search -> closeSearch()
            else -> {
                destination = Destination.Home
                previousTab = Destination.Home
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .onSizeChanged { rootSize.value = it },
    ) {
        // Both floating controls sample this actual full-screen source, including live scroll.
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
            key(destination) {
                PlaceholderPage(
                    destination = destination,
                    query = query,
                    appearance = appearance,
                    onAppearanceChange = { appearance = it },
                    listState = listState,
                    listOrigin = listOrigin,
                    fadeIn = fadeIn,
                )
            }
        }

        FloatingNavigation(
            selected = destination,
            previousTab = previousTab,
            query = query,
            onQueryChange = { query = it.take(120) },
            onSelectTab = { tab ->
                keyboard?.hide()
                focusManager.clearFocus(force = true)
                previousTab = tab
                destination = tab
            },
            onOpenSearch = {
                if (destination != Destination.Search) {
                    previousTab = destination
                    destination = Destination.Search
                }
            },
            onReturnToTab = { closeSearch() },
            onSearchDone = {
                keyboard?.hide()
                focusManager.clearFocus(force = true)
            },
            imeVisible = imeVisible,
            hazeState = hazeState,
            blurStyle = blurStyle,
            borderOverlay = borderOverlay,
            gradientPhase = gradientPhase,
            rootSize = rootSize,
            listOrigin = listOrigin,
            listState = listState,
        )
    }
}

@Composable
private fun PlaceholderPage(
    destination: Destination,
    query: String,
    appearance: NavigationAppearance,
    onAppearanceChange: (NavigationAppearance) -> Unit,
    listState: LazyListState,
    listOrigin: androidx.compose.runtime.MutableState<Offset>,
    fadeIn: Animatable<Float, *>,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imeInset = if (destination == Destination.Search) WindowInsets.ime.asPaddingValues().calculateBottomPadding() else 0.dp
    val searchResults = remember(query) {
        val needle = query.trim()
        if (needle.isEmpty()) {
            (1..36).map { SampleCard(Destination.Search, it) }
        } else {
            searchableSamples.filter { card ->
                card.title.contains(needle, ignoreCase = true) ||
                    card.destination.cardDetail.contains(needle, ignoreCase = true)
            }
        }
    }
    val cards = if (destination == Destination.Search) searchResults
    else remember(destination) { (1..36).map { SampleCard(destination, it) } }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .onGloballyPositioned { listOrigin.value = it.positionInRoot() }
            .graphicsLayer { alpha = fadeIn.value },
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 28.dp,
            end = 20.dp,
            // The final result stays reachable above the search bar and keyboard.
            bottom = maxOf(bottomInset, imeInset) + 104.dp,
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
                    text = if (destination == Destination.Search && query.isNotBlank()) {
                        "${cards.size} matching sample cards"
                    } else destination.subtitle,
                    color = colors.onBackground.copy(alpha = 0.68f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (destination == Destination.Settings) {
            item(key = "blur-settings") {
                NavigationBlurSettings(appearance = appearance, onChange = onAppearanceChange)
            }
        }
        if (destination == Destination.Search && cards.isEmpty()) {
            item(key = "empty-search") {
                Text(
                    text = "No matching sample cards. Search currently filters placeholders only.",
                    color = colors.onBackground.copy(alpha = 0.68f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
        items(items = cards, key = { it.key }) { card ->
            PlaceholderCard(card = card, dark = dark)
        }
    }
}

@Composable
private fun PlaceholderCard(card: SampleCard, dark: Boolean) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val cardBorder = colors.onBackground.copy(alpha = if (dark) 0.08f else 0.06f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 94.dp)
            .clip(shape)
            .background(placeholderSurfaceColor(dark, card.number))
            .border(1.dp, cardBorder, shape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = card.title,
            color = colors.onBackground,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = card.destination.cardDetail,
            color = colors.onBackground.copy(alpha = 0.64f),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
