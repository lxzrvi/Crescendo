@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.lxzrvi.crescendo.ui.home

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lxzrvi.crescendo.R
import com.lxzrvi.crescendo.ui.onboarding.GradientBlobBackground
import com.styropyr0.prismal.sources.prismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeSourceSelection
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlin.math.roundToInt

internal const val PAGE_SOURCE_KEY = "page"

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

private val samplePalette = listOf(
    Color(0xFFF1746B), Color(0xFFAF86EC), Color(0xFF43BDB1), Color(0xFFF1AD55),
    Color(0xFF6D9EEF), Color(0xFFEE82AC), Color(0xFF91BF65), Color(0xFFE68758),
)

private fun sampleAccent(card: SampleCard): Color =
    samplePalette[(card.number - 1 + card.destination.ordinal * 3) % samplePalette.size]

/** Sample pages, captured as one live source beneath the independently floating controls. */
@Composable
fun HomeScreen(gradientPhase: State<Float>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
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
    val pageInput = remember(hazeState) {
        HazeInput.Sources(
            state = hazeState,
            selection = HazeSourceSelection.All.where { it.key == PAGE_SOURCE_KEY },
        )
    }
    // Both effects sample the same real scrolling content: Prismal for the
    // floating controls, and Haze for the progressive status-area backdrop.
    val pageBackdrop = rememberPrismalGlassLayer()
    val initialFade = remember { Animatable(0f) }

    // Each page keeps its own list state, including while the transition shows two pages at once.
    val homeState = rememberLazyListState()
    val playlistState = rememberLazyListState()
    val libraryState = rememberLazyListState()
    val settingsState = rememberLazyListState()
    val searchState = rememberLazyListState()
    fun stateFor(page: Destination): LazyListState = when (page) {
        Destination.Home -> homeState
        Destination.Playlist -> playlistState
        Destination.Library -> libraryState
        Destination.Settings -> settingsState
        Destination.Search -> searchState
    }
    LaunchedEffect(Unit) {
        initialFade.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(appearance) { prefs.save(appearance) }

    fun closeSearch() {
        keyboard?.hide()
        focusManager.clearFocus(force = true)
        destination = previousTab
    }

    BackHandler(enabled = destination != Destination.Home) {
        when {
            destination == Destination.Search && imeVisible -> {
                // Keeping focus until the IME finishes closing lets imePadding animate with it.
                keyboard?.hide()
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
            .background(colors.background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState, zIndex = 0f, key = PAGE_SOURCE_KEY)
                .then(if (appearance.liveBlur) Modifier.prismalGlassLayer(pageBackdrop) else Modifier)
                .background(colors.background),
        ) {
            GradientBlobBackground(
                phase = gradientPhase,
                modifier = Modifier.fillMaxSize(),
            )
            AnimatedContent(
                targetState = destination,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                    (fadeIn(tween(300, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { width ->
                            (width * 0.045f * direction).roundToInt()
                        }) togetherWith
                        (fadeOut(tween(200, easing = FastOutSlowInEasing)) +
                            slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { width ->
                                (-width * 0.025f * direction).roundToInt()
                            })
                },
                label = "pageTransition",
            ) { page ->
                PlaceholderPage(
                    destination = page,
                    query = query,
                    appearance = appearance,
                    onAppearanceChange = { appearance = it },
                    listState = stateFor(page),
                    fadeIn = initialFade,
                )
            }
        }

        StatusBarBackdrop(input = pageInput, liveBlur = appearance.liveBlur)

        FloatingNavigation(
            selected = destination,
            previousTab = previousTab,
            query = query,
            onQueryChange = { query = it.take(120) },
            onSelectTab = { tab ->
                if (imeVisible) {
                    keyboard?.hide()
                    focusManager.clearFocus(force = true)
                }
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
            onSearchDone = { keyboard?.hide() },
            imeVisible = imeVisible,
            pageBackdrop = pageBackdrop,
            liveBlur = appearance.liveBlur,
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
    fadeIn: Animatable<Float, *>,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
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
            .graphicsLayer { alpha = fadeIn.value },
        contentPadding = PaddingValues(
            start = 20.dp,
            // Padding the content, not the viewport, lets scrolling cards pass behind the status bar.
            top = statusInset + 28.dp,
            end = 20.dp,
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
    val accent = sampleAccent(card)
    val neighboringAccent = samplePalette[(card.number + card.destination.ordinal * 3 + 2) % samplePalette.size]
    val shape = RoundedCornerShape(18.dp)
    val surface = remember(accent, neighboringAccent, dark) {
        Brush.linearGradient(
            listOf(
                accent.copy(alpha = if (dark) 0.24f else 0.22f),
                neighboringAccent.copy(alpha = if (dark) 0.12f else 0.11f),
                (if (dark) Color.White else Color.Black).copy(alpha = if (dark) 0.05f else 0.025f),
            ),
        )
    }
    val artwork = remember(accent) {
        Brush.linearGradient(listOf(lerp(accent, Color.Black, 0.08f), lerp(accent, Color.Black, 0.40f)))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = (94 + (card.number % 3) * 8).dp)
            .clip(shape)
            .background(surface)
            .border(1.dp, accent.copy(alpha = if (dark) 0.25f else 0.22f), shape)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(artwork),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(card.destination.icon),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.94f),
                modifier = Modifier.size(23.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = card.title,
                color = colors.onBackground,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = card.destination.cardDetail,
                color = colors.onBackground.copy(alpha = 0.66f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
