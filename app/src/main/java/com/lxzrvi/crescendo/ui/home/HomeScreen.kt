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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lxzrvi.crescendo.R
import com.lxzrvi.crescendo.data.AlbumArtworkLoader
import com.lxzrvi.crescendo.data.ForegroundPlayback
import com.lxzrvi.crescendo.data.ListeningHistory
import com.lxzrvi.crescendo.data.MusicLibraryState
import com.lxzrvi.crescendo.data.rememberMusicLibrary
import com.styropyr0.prismal.sources.prismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeSourceSelection
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlin.math.roundToInt

internal const val PAGE_SOURCE_KEY = "page"

internal enum class Destination(val label: String, @param:DrawableRes val icon: Int) {
    Home("Home", R.drawable.ic_nav_home),
    Playlist("Playlist", R.drawable.ic_nav_playlist),
    Library("Library", R.drawable.ic_nav_library),
    Settings("Settings", R.drawable.ic_nav_settings),
    Search("Search", R.drawable.ic_nav_search),
}

/** Real local-library pages, captured beneath both the status blur and floating navigation. */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val imeVisible = WindowInsets.isImeVisible
    val prefs = remember(context) { NavigationAppearancePrefs(context) }
    var appearance by remember(prefs) { mutableStateOf(prefs.load()) }
    var destination by rememberSaveable { mutableStateOf(Destination.Home) }
    var previousTab by rememberSaveable { mutableStateOf(Destination.Home) }
    var query by rememberSaveable { mutableStateOf("") }
    var showProfile by rememberSaveable { mutableStateOf(false) }
    val library = rememberMusicLibrary()
    val artwork = remember(context) { AlbumArtworkLoader(context) }
    val history = remember(context) { ListeningHistory(context) }
    var playCounts by remember(history) { mutableStateOf(history.counts()) }
    val playback = remember(context, history) {
        ForegroundPlayback(context) { song -> playCounts = history.record(song) }
    }

    // A foreground player has no persistent media notification or service yet.
    DisposableEffect(lifecycle, playback) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) playback.pauseForBackground()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    DisposableEffect(playback) { onDispose { playback.release() } }

    val hazeState = rememberHazeState()
    val pageInput = remember(hazeState) {
        HazeInput.Sources(
            state = hazeState,
            selection = HazeSourceSelection.All.where { it.key == PAGE_SOURCE_KEY },
        )
    }
    val pageBackdrop = rememberPrismalGlassLayer()
    val initialFade = remember { Animatable(0f) }

    // These are owned above AnimatedContent, so each page/carousel keeps its scroll position.
    val homeState = rememberLazyListState()
    val topState = rememberLazyListState()
    val quickState = rememberLazyListState()
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
    LaunchedEffect(Unit) { initialFade.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
    LaunchedEffect(appearance) { prefs.save(appearance) }
    LaunchedEffect(query) { searchState.scrollToItem(0) }

    fun closeSearch() {
        keyboard?.hide()
        focusManager.clearFocus(force = true)
        destination = previousTab
    }

    BackHandler(enabled = destination != Destination.Home) {
        when {
            destination == Destination.Search && imeVisible -> keyboard?.hide()
            destination == Destination.Search -> closeSearch()
            else -> {
                destination = Destination.Home
                previousTab = Destination.Home
            }
        }
    }

    val showMiniPlayer = playback.currentSong != null && !(destination == Destination.Search && imeVisible)
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imeInset = if (destination == Destination.Search) WindowInsets.ime.asPaddingValues().calculateBottomPadding() else 0.dp
    val pageBottomPadding = maxOf(bottomInset, imeInset) + if (showMiniPlayer) 184.dp else 108.dp

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState, zIndex = 0f, key = PAGE_SOURCE_KEY)
                .then(if (appearance.liveBlur) Modifier.prismalGlassLayer(pageBackdrop) else Modifier)
                .background(colors.background),
        ) {
            // The onboarding glow deliberately ends on the welcome page. The app is solid.
            AnimatedContent(
                targetState = destination,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                    (fadeIn(tween(380, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(tween(420, easing = FastOutSlowInEasing)) { width ->
                            (width * 0.025f * direction).roundToInt()
                        }) togetherWith
                        (fadeOut(tween(300, easing = FastOutSlowInEasing)) +
                            slideOutHorizontally(tween(380, easing = FastOutSlowInEasing)) { width ->
                                (-width * 0.018f * direction).roundToInt()
                            })
                },
                label = "pageTransition",
            ) { page ->
                val fade = Modifier.graphicsLayer { alpha = initialFade.value }
                if (page == Destination.Home) {
                    HomeMusicPage(
                        library = library,
                        playCounts = playCounts,
                        artwork = artwork,
                        currentSong = playback.currentSong,
                        isPlaying = playback.isPlaying,
                        onSongClick = playback::select,
                        onProfileClick = { showProfile = true },
                        listState = homeState,
                        topState = topState,
                        quickState = quickState,
                        bottomPadding = pageBottomPadding,
                        modifier = fade,
                    )
                } else {
                    MusicListPage(
                        destination = page,
                        library = library,
                        query = query,
                        artwork = artwork,
                        currentSong = playback.currentSong,
                        isPlaying = playback.isPlaying,
                        onSongClick = playback::select,
                        appearance = appearance,
                        onAppearanceChange = { appearance = it },
                        listState = stateFor(page),
                        bottomPadding = pageBottomPadding,
                        modifier = fade,
                    )
                }
            }
        }

        StatusBarBackdrop(input = pageInput, liveBlur = appearance.liveBlur)

        val activeSong = playback.currentSong
        if (activeSong != null && showMiniPlayer) {
            MiniPlayer(
                song = activeSong,
                artwork = artwork,
                playing = playback.isPlaying,
                loading = playback.isLoading,
                error = playback.errorMessage,
                onToggle = playback::toggle,
                onClose = playback::close,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, bottom = 88.dp),
            )
        }

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
            pageInput = pageInput,
            liveBlur = appearance.liveBlur,
        )
    }

    if (showProfile) {
        val count = (library as? MusicLibraryState.Ready)?.songs?.size ?: 0
        AlertDialog(
            onDismissRequest = { showProfile = false },
            title = { Text("Profile") },
            text = { Text("Your music stays on this device. Your local library has $count ${if (count == 1) "song" else "songs"}.") },
            confirmButton = { TextButton(onClick = { showProfile = false }) { Text("Done") } },
        )
    }
}
