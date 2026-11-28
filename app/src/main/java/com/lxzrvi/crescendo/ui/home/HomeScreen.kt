@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.lxzrvi.crescendo.ui.home

import android.app.Activity
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lxzrvi.crescendo.R
import com.lxzrvi.crescendo.data.AlbumArtworkLoader
import com.lxzrvi.crescendo.data.ForegroundPlayback
import com.lxzrvi.crescendo.data.ListeningHistory
import com.lxzrvi.crescendo.data.MusicLibraryState
import com.lxzrvi.crescendo.data.Song
import com.lxzrvi.crescendo.data.rememberMusicLibrary
import com.lxzrvi.crescendo.ui.theme.CrescendoTheme
import com.styropyr0.prismal.sources.prismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeSourceSelection
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

internal const val PAGE_SOURCE_KEY = "page"

internal enum class Destination(val label: String, @param:DrawableRes val icon: Int) {
    Home("Home", R.drawable.ic_nav_home),
    Playlist("Playlist", R.drawable.ic_nav_playlist),
    Library("Library", R.drawable.ic_nav_library),
    Settings("Settings", R.drawable.ic_nav_settings),
    Search("Search", R.drawable.ic_nav_search),
}

/** User preferences apply to every tab without changing the onboarding theme. */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val view = LocalView.current
    val prefs = remember(context) { NavigationAppearancePrefs(context) }
    var appearance by remember(prefs) { mutableStateOf(prefs.load()) }
    val darkTheme = when (appearance.themeMode) {
        AppThemeMode.System -> isSystemInDarkTheme()
        AppThemeMode.Dark -> true
        AppThemeMode.Light -> false
    }
    LaunchedEffect(appearance) { prefs.save(appearance) }
    SideEffect {
        (context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    CrescendoTheme(
        darkTheme = darkTheme,
        accent = if (appearance.accent == AccentChoice.Crescendo) null else appearance.accent.color,
    ) {
        HomeScreenContent(appearance, onAppearanceChange = { appearance = it }, modifier = modifier)
    }
}

/** Real local-library pages, captured beneath both the status blur and floating navigation. */
@Composable
private fun HomeScreenContent(
    appearance: NavigationAppearance,
    onAppearanceChange: (NavigationAppearance) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val imeVisible = WindowInsets.isImeVisible
    var destination by rememberSaveable { mutableStateOf(Destination.Home) }
    var previousTab by rememberSaveable { mutableStateOf(Destination.Home) }
    var query by rememberSaveable { mutableStateOf("") }
    var libraryCategory by rememberSaveable { mutableStateOf<CollectionKind?>(null) }
    var showProfile by rememberSaveable { mutableStateOf(false) }
    val library = rememberMusicLibrary()
    val artwork = remember(context) { AlbumArtworkLoader(context) }
    val history = remember(context) { ListeningHistory(context) }
    var playCounts by remember(history) { mutableStateOf(history.counts()) }
    var recentTimes by remember(history) { mutableStateOf(history.recentTimes()) }
    var liked by remember(history) { mutableStateOf(history.liked()) }
    val playback = remember(context, history) {
        ForegroundPlayback(context) { song ->
            playCounts = history.record(song)
            recentTimes = history.recentTimes()
        }
    }
    val currentSong = playback.currentSong
    val progress by produceState(0f, currentSong?.id, playback.isPlaying) {
        if (currentSong == null) value = 0f else while (true) {
            value = playback.progressFraction()
            delay(if (playback.isPlaying) 350 else 1_000)
        }
    }

    // Show the branded startup while the real library and first covers warm in the background.
    var startedAt by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var startupCycle by remember { mutableIntStateOf(0) }
    var startupReady by remember { mutableStateOf(false) }
    // Avoid leaving the branded screen up forever if a device's media provider hangs.
    LaunchedEffect(startupCycle) {
        delay(5_000)
        startupReady = true
    }
    LaunchedEffect(library, startupCycle) {
        if (!startupReady && library !is MusicLibraryState.Loading) {
            if (library is MusicLibraryState.Ready) {
                val firstCovers = library.songs.sortedWith(
                    compareByDescending<Song> { playCounts[it.historyKey] ?: 0 }
                        .thenByDescending { it.dateAdded }
                        .thenBy { it.title.lowercase() },
                ).take(3)
                withTimeoutOrNull(2_500) {
                    coroutineScope {
                        firstCovers.map { song -> async {
                            artwork.load(song, 512)?.let { artwork.palette(song, it) }
                        } }.awaitAll()
                    }
                }
            }
            val remaining = 1_800L - (SystemClock.elapsedRealtime() - startedAt)
            if (remaining > 0) delay(remaining)
            startupReady = true
        }
    }

    // A foreground player has no persistent media notification or service yet.
    DisposableEffect(lifecycle, playback) {
        var wasStopped = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    playback.pauseForBackground()
                    wasStopped = true
                }
                Lifecycle.Event.ON_RESUME -> if (wasStopped) {
                    // Reopening also preloads underneath the same smooth transition.
                    startedAt = SystemClock.elapsedRealtime()
                    startupReady = false
                    startupCycle++
                    wasStopped = false
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    DisposableEffect(playback) { onDispose { playback.release() } }

    val hazeState = rememberHazeState()
    val pageInput = remember(hazeState) {
        HazeInput.Sources(state = hazeState, selection = HazeSourceSelection.All.where { it.key == PAGE_SOURCE_KEY })
    }
    val pageBackdrop = rememberPrismalGlassLayer()
    val wallpaperBackdrop = rememberPrismalGlassLayer()
    val initialFade = remember { Animatable(0f) }
    val homeState = rememberLazyListState()
    val quickState = rememberLazyListState()
    val moodsState = rememberLazyListState()
    val playlistState = rememberLazyListState()
    val libraryState = rememberLazyListState()
    val libraryDetailState = rememberLazyListState()
    val settingsState = rememberLazyListState()
    val searchState = rememberLazyListState()
    fun stateFor(page: Destination): LazyListState = when (page) {
        Destination.Home -> homeState
        Destination.Playlist -> playlistState
        Destination.Library -> if (libraryCategory == null) libraryState else libraryDetailState
        Destination.Settings -> settingsState
        Destination.Search -> searchState
    }
    LaunchedEffect(Unit) { initialFade.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
    LaunchedEffect(query) { searchState.scrollToItem(0) }
    // Match the reference's persistent dock: enough downward Home scroll collapses;
    // upward movement, reaching the top, tapping the disc, or leaving Home expands it.
    val density = LocalDensity.current
    val downThreshold = with(density) { 70.dp.toPx() }
    val deepThreshold = with(density) { 120.dp.toPx() }
    val topThreshold = with(density) { 80.dp.toPx() }
    val upwardThreshold = with(density) { 4.dp.toPx() }
    var downDistance by remember { mutableFloatStateOf(0f) }
    var upDistance by remember { mutableFloatStateOf(0f) }
    var miniDocked by remember { mutableStateOf(false) }
    LaunchedEffect(destination, currentSong?.id) {
        if (destination != Destination.Home || currentSong == null) {
            miniDocked = false
            downDistance = 0f
            upDistance = 0f
        }
    }
    fun onHomeScroll(consumedY: Float) {
        if (destination != Destination.Home || currentSong == null) return
        val firstItem = homeState.firstVisibleItemIndex == 0
        val offset = homeState.firstVisibleItemScrollOffset.toFloat()
        when {
            firstItem && offset < topThreshold -> { miniDocked = false; downDistance = 0f; upDistance = 0f }
            consumedY > 0f -> {
                // Accumulate small high-refresh deltas too, so a gentle upward scroll expands.
                upDistance += consumedY
                if (upDistance > upwardThreshold) { miniDocked = false; downDistance = 0f }
            }
            consumedY < 0f -> {
                upDistance = 0f
                downDistance = (downDistance - consumedY).coerceAtMost(deepThreshold * 2f)
                if (downDistance > downThreshold && (!firstItem || offset > deepThreshold)) miniDocked = true
            }
        }
    }
    val miniCollapsed = destination == Destination.Home && miniDocked
    val profileBlur by animateDpAsState(if (showProfile) 10.dp else 0.dp, tween(220), label = "profileBackdropBlur")

    fun closeSearch() {
        keyboard?.hide()
        focusManager.clearFocus(force = true)
        destination = previousTab
    }
    BackHandler(enabled = destination != Destination.Home) {
        when {
            destination == Destination.Search && imeVisible -> keyboard?.hide()
            destination == Destination.Search -> closeSearch()
            destination == Destination.Library && libraryCategory != null -> libraryCategory = null
            else -> { destination = Destination.Home; previousTab = Destination.Home }
        }
    }

    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imeInset = if (destination == Destination.Search) WindowInsets.ime.asPaddingValues().calculateBottomPadding() else 0.dp
    val pageBottomPadding = maxOf(bottomInset, imeInset) + if (currentSong != null) 184.dp else 108.dp

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        Box(Modifier.fillMaxSize().blur(profileBlur)
            .then(if (showProfile) Modifier.clearAndSetSemantics { } else Modifier)) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .hazeSource(state = hazeState, zIndex = 0f, key = PAGE_SOURCE_KEY)
                    .then(if (appearance.liveBlur) Modifier.prismalGlassLayer(pageBackdrop) else Modifier)
                    .background(colors.background),
            ) {
                // A separate source for in-page glass avoids sampling the page into itself.
                Box(Modifier.fillMaxSize().prismalGlassLayer(wallpaperBackdrop).background(colors.background)) {
                    AppPhotoBackground(appearance)
                }
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
                    when (page) {
                        Destination.Home -> HomeMusicPage(
                            library = library, playCounts = playCounts, artwork = artwork,
                            currentSong = currentSong, isPlaying = playback.isPlaying,
                            shuffleEnabled = playback.shuffleEnabled, animateHero = appearance.animateHero, liveBlur = appearance.liveBlur,
                            onSongClick = playback::select,
                            onShuffle = { playback.toggleShuffle((library as? MusicLibraryState.Ready)?.songs.orEmpty()) },
                            onVerticalScroll = ::onHomeScroll,
                            listState = homeState, quickState = quickState, moodsState = moodsState,
                            bottomPadding = pageBottomPadding, modifier = fade,
                        )
                        Destination.Library -> LibraryCollectionsPage(
                            library = library, playCounts = playCounts, recentTimes = recentTimes, liked = liked,
                            artwork = artwork, currentSong = currentSong, isPlaying = playback.isPlaying,
                            onSongClick = playback::select, onLike = { liked = history.toggleLiked(it) },
                            category = libraryCategory, onCategoryChange = { libraryCategory = it },
                            overviewState = libraryState, detailState = libraryDetailState,
                            bottomPadding = pageBottomPadding, modifier = fade,
                        )
                        else -> MusicListPage(
                            destination = page, library = library, query = query, artwork = artwork,
                            currentSong = currentSong, isPlaying = playback.isPlaying, onSongClick = playback::select,
                            appearance = appearance, onAppearanceChange = onAppearanceChange,
                            settingsBackdrop = wallpaperBackdrop,
                            listState = stateFor(page), bottomPadding = pageBottomPadding, modifier = fade,
                        )
                    }
                }
            }
            StatusBarBackdrop(input = pageInput, liveBlur = appearance.liveBlur)
            FloatingNavigation(
                selected = destination, previousTab = previousTab, query = query,
                onQueryChange = { query = it.take(120) },
                onSelectTab = { tab ->
                    if (imeVisible) { keyboard?.hide(); focusManager.clearFocus(force = true) }
                    previousTab = tab
                    destination = tab
                },
                onOpenSearch = { if (destination != Destination.Search) { previousTab = destination; destination = Destination.Search } },
                onReturnToTab = { closeSearch() }, onSearchDone = { keyboard?.hide() },
                imeVisible = imeVisible, pageBackdrop = pageBackdrop, pageInput = pageInput, liveBlur = appearance.liveBlur,
                miniSong = currentSong, artwork = artwork, miniPlaying = playback.isPlaying,
                miniLoading = playback.isLoading, miniError = playback.errorMessage, miniProgress = progress,
                miniCollapsed = miniCollapsed, onMiniToggle = playback::toggle,
                onMiniNext = { playback.next((library as? MusicLibraryState.Ready)?.songs.orEmpty()) },
                onMiniExpand = { miniDocked = false; downDistance = 0f; upDistance = 0f },
            )
        }
        ProfileMenu(
            expanded = showProfile, onExpandedChange = {
                if (it && imeVisible) { keyboard?.hide(); focusManager.clearFocus(force = true) }
                showProfile = it
            },
            pageBackdrop = pageBackdrop, liveBlur = appearance.liveBlur,
            onSettings = { previousTab = Destination.Settings; destination = Destination.Settings },
        )
        AnimatedVisibility(visible = !startupReady, enter = fadeIn(), exit = fadeOut(tween(360))) {
            StartupLoadingScreen()
        }
    }
}
