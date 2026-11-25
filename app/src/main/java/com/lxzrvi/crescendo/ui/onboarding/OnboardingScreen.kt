package com.lxzrvi.crescendo.ui.onboarding

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lxzrvi.crescendo.ui.components.CrescendoLoader
import com.lxzrvi.crescendo.ui.theme.BorelFamily
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val ONBOARDING_ACTION_RADIUS_DP = 15f

private const val LOADER_SIZE_DP = 88f
private const val MARK_START_SCALE = WELCOME_MARK_SIZE_DP / LOADER_SIZE_DP

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> if (baseContext === this) null else baseContext.findActivity()
    else -> null
}

@Composable
fun OnboardingScreen(gradientPhase: State<Float>, onFinished: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = MaterialTheme.colorScheme
    val pageCount = PermissionPages.all.size
    var pageIndex by rememberSaveable { mutableStateOf(0) }
    var finishing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(readPermissionStatus(context)) }
    var musicRequested by rememberSaveable { mutableStateOf(false) }
    var notificationsRequested by rememberSaveable { mutableStateOf(false) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var markCenter by remember { mutableStateOf<Offset?>(null) }

    fun refreshPermissions() {
        status = readPermissionStatus(context)
        if (!status.musicGranted && pageIndex > 0) {
            pageIndex = 0
            finishing = false
        }
    }

    // Covers permission changes made in Settings as well as returning from the dialogs.
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(status.musicGranted, pageIndex) {
        if (!status.musicGranted && pageIndex > 0) {
            pageIndex = 0
            finishing = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshPermissions() }
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshPermissions() }

    fun appInfoIntent() = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:${context.packageName}"),
    )

    fun openAppInfo() {
        try {
            settingsLauncher.launch(appInfoIntent())
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Open Crescendo settings to change access.", Toast.LENGTH_LONG).show()
        } catch (_: SecurityException) {
            Toast.makeText(context, "Open Crescendo settings to change access.", Toast.LENGTH_LONG).show()
        }
    }

    fun openNotificationSettings() {
        try {
            settingsLauncher.launch(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            )
        } catch (_: ActivityNotFoundException) {
            openAppInfo()
        } catch (_: SecurityException) {
            openAppInfo()
        }
    }

    fun advanceFrom(kind: PermissionKind) {
        val current = readPermissionStatus(context)
        status = current
        if (!current.musicGranted) {
            pageIndex = 0
            return
        }
        if (kind == PermissionKind.MUSIC && pageIndex == 0) {
            pageIndex = 1
        } else if (kind == PermissionKind.NOTIFICATIONS && pageIndex == 1) {
            pageIndex = pageCount
        }
    }

    fun request(target: PermissionPage) {
        val current = readPermissionStatus(context)
        status = current
        if (target.kind == PermissionKind.NOTIFICATIONS && !current.musicGranted) {
            pageIndex = 0
            return
        }
        val alreadyGranted = when (target.kind) {
            PermissionKind.MUSIC -> current.musicGranted
            PermissionKind.NOTIFICATIONS -> current.notificationsGranted
        }
        if (alreadyGranted) {
            advanceFrom(target.kind)
            return
        }

        val permissions = PermissionPages.runtimePermissions(target.kind)
        val runtimeAlreadyGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (target.kind == PermissionKind.NOTIFICATIONS && runtimeAlreadyGranted) {
            // Below Android 13, or if app-wide notifications were disabled in Settings.
            openNotificationSettings()
            return
        }

        val requestedBefore = when (target.kind) {
            PermissionKind.MUSIC -> musicRequested
            PermissionKind.NOTIFICATIONS -> notificationsRequested
        }
        val activity = context.findActivity()
        if (requestedBefore && activity != null && permissions.none {
                ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
            }
        ) {
            // A permanently denied request needs an explicit Settings change.
            if (target.kind == PermissionKind.MUSIC) openAppInfo() else openNotificationSettings()
            return
        }
        when (target.kind) {
            PermissionKind.MUSIC -> musicRequested = true
            PermissionKind.NOTIFICATIONS -> notificationsRequested = true
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    fun requestOptionalBackground() {
        status = readPermissionStatus(context)
        if (status.backgroundGranted) {
            openAppInfo()
            return
        }
        try {
            settingsLauncher.launch(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            )
        } catch (_: ActivityNotFoundException) {
            openAppInfo()
        } catch (_: SecurityException) {
            openAppInfo()
        }
    }

    BackHandler(enabled = pageIndex > 0 && !finishing) { pageIndex-- }

    // The glow stays in place across both permissions, then glides down for welcome.
    val bgPosition = remember { Animatable(0f) }
    val showWelcome = pageIndex >= pageCount
    LaunchedEffect(showWelcome) {
        bgPosition.animateTo(
            targetValue = if (showWelcome) 1f else 0f,
            animationSpec = tween(1300, easing = FastOutSlowInEasing),
        )
    }

    val focusGlow = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }
    val loaderIn = remember { Animatable(0f) }
    val loaderPulse = remember { Animatable(1f) }
    val loaderDepart = remember { Animatable(0f) }
    val homeGlow = remember { Animatable(0f) }
    var revealingHomeGlow by remember { mutableStateOf(false) }

    fun glowTransform(): GradientLayerTransform {
        val w = rootSize.width.toFloat()
        val h = rootSize.height.toFloat()
        if (revealingHomeGlow) {
            return GradientLayerTransform(
                y = (homeGlow.value - 1f) * h,
                alpha = (homeGlow.value * 2f).coerceIn(0f, 1f),
            )
        }
        val source = Offset(w * 0.5f, h * (0.22f + 0.68f * bgPosition.value))
        val mark = markCenter ?: Offset(w * 0.8f, h * 0.16f)
        // The glow heads toward the mark as the mark travels to the loader's center.
        val target = Offset(
            mark.x + (w * 0.5f - mark.x) * loaderIn.value,
            mark.y + (h * 0.5f - mark.y) * loaderIn.value,
        )
        return GradientLayerTransform(
            x = (target.x - source.x) * focusGlow.value,
            y = (target.y - source.y) * focusGlow.value + exit.value * h * 1.8f,
            alpha = (1f - exit.value).coerceIn(0f, 1f),
        )
    }

    LaunchedEffect(finishing) {
        if (!finishing) {
            // Also reset if access is revoked while the finishing animation is running.
            focusGlow.snapTo(0f)
            exit.snapTo(0f)
            loaderIn.snapTo(0f)
            loaderPulse.snapTo(1f)
            loaderDepart.snapTo(0f)
            homeGlow.snapTo(0f)
            revealingHomeGlow = false
        } else {
            coroutineScope {
                launch { loaderIn.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }
                focusGlow.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
                exit.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
            }
            delay(1000) // A brief moment to see the comet loader.
            if (!hasMusicAccess(context)) {
                refreshPermissions()
                finishing = false
                return@LaunchedEffect
            }

            // The old glow is invisible; reset its shape before bringing it in from above.
            bgPosition.snapTo(0f)
            revealingHomeGlow = true
            coroutineScope {
                launch { homeGlow.animateTo(1f, tween(1300, easing = FastOutSlowInEasing)) }
                launch {
                    loaderPulse.animateTo(1.08f, tween(220, easing = FastOutSlowInEasing))
                    loaderDepart.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
                }
            }
            if (hasMusicAccess(context)) onFinished() else refreshPermissions()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .onSizeChanged { rootSize = it },
    ) {
        GradientBlobBackground(
            phase = gradientPhase,
            position = { bgPosition.value },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val transform = glowTransform()
                    translationX = transform.x
                    translationY = transform.y
                    alpha = transform.alpha
                },
        )

        AnimatedVisibility(
            visible = !finishing,
            exit = fadeOut(tween(300)),
            modifier = Modifier.fillMaxSize(),
        ) {
            AnimatedContent(
                targetState = showWelcome,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    if (targetState) {
                        (slideInVertically(tween(600)) { it } + fadeIn(tween(600))) togetherWith
                            (slideOutVertically(tween(450)) { -it } + fadeOut(tween(450)))
                    } else {
                        (slideInHorizontally(tween(420)) { -it } + fadeIn(tween(420))) togetherWith
                            (slideOutHorizontally(tween(420)) { it } + fadeOut(tween(420)))
                    }
                },
                label = "stage",
            ) { isWelcome ->
                if (isWelcome) {
                    WelcomeContent(
                        onGetStarted = {
                            refreshPermissions()
                            if (status.musicGranted && pageIndex >= pageCount) finishing = true
                        },
                        onMarkPositioned = { markCenter = it },
                        gradientPhase = gradientPhase,
                        gradientPosition = { bgPosition.value },
                        gradientSize = rootSize,
                        gradientTransform = { glowTransform() },
                    )
                } else {
                    PermissionStage(
                        pageIndex = pageIndex.coerceAtMost(pageCount - 1),
                        pageCount = pageCount,
                        status = status,
                        onOk = { request(it) },
                        onSkip = { advanceFrom(PermissionKind.NOTIFICATIONS) },
                        onOptionalBackground = { requestOptionalBackground() },
                    )
                }
            }
        }

        if (finishing) {
            val c = markCenter
            val start = if (c != null && rootSize != IntSize.Zero)
                Offset(c.x - rootSize.width / 2f, c.y - rootSize.height / 2f)
            else Offset.Zero
            val liftPx = with(LocalDensity.current) { 20.dp.toPx() }

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CrescendoLoader(
                    modifier = Modifier
                        .size(LOADER_SIZE_DP.dp)
                        .graphicsLayer {
                            val t = loaderIn.value
                            val depart = loaderDepart.value
                            translationX = start.x * (1f - t)
                            translationY = start.y * (1f - t) - liftPx * depart
                            val entryScale = MARK_START_SCALE + (1f - MARK_START_SCALE) * t
                            val settleScale = loaderPulse.value * (1f - 0.16f * depart)
                            scaleX = entryScale * settleScale
                            scaleY = entryScale * settleScale
                            alpha = (t * 5f).coerceIn(0f, 1f) * (1f - depart)
                        },
                    color = lerp(colors.onBackground, colors.primary, loaderIn.value),
                )
            }
        }
    }
}

/** Wordmark and dots stay still; both permission panels share one aligned slide area. */
@Composable
private fun PermissionStage(
    pageIndex: Int,
    pageCount: Int,
    status: PermissionStatus,
    onOk: (PermissionPage) -> Unit,
    onSkip: () -> Unit,
    onOptionalBackground: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        val titleStyle = MaterialTheme.typography.headlineMedium
        val detailStyle = MaterialTheme.typography.bodyMedium
        val textWidthPx = (with(density) { maxWidth.roundToPx() - 56.dp.roundToPx() }).coerceAtLeast(1)
        val titleWidthPx = (textWidthPx - with(density) { 36.dp.roundToPx() }).coerceAtLeast(1)
        val desiredSlotHeight = remember(textWidthPx, titleWidthPx, density, titleStyle, detailStyle, textMeasurer) {
            val titleHeight = PermissionPages.all.maxOf { page ->
                textMeasurer.measure(
                    text = AnnotatedString(page.title),
                    style = titleStyle,
                    constraints = Constraints(maxWidth = titleWidthPx),
                ).size.height
            }
            val detailHeight = PermissionPages.all.maxOf { page ->
                textMeasurer.measure(
                    text = AnnotatedString(page.detail),
                    style = detailStyle,
                    constraints = Constraints(maxWidth = textWidthPx),
                ).size.height
            }
            val iconHeight = with(density) { 26.dp.roundToPx() }
            with(density) { (maxOf(titleHeight, iconHeight) + detailHeight).toDp() } +
                10.dp + 10.dp + 52.dp + 8.dp
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.64f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Crescendo",
                    fontFamily = BorelFamily,
                    fontSize = 72.sp,
                    color = colors.onBackground,
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Bottom,
            ) {
                AnimatedContent(
                    targetState = pageIndex,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = desiredSlotHeight)
                        .weight(1f, fill = false),
                    transitionSpec = {
                        if (targetState < initialState) {
                            (slideInHorizontally(tween(420)) { -it } + fadeIn(tween(420))) togetherWith
                                (slideOutHorizontally(tween(420)) { it } + fadeOut(tween(420)))
                        } else {
                            (slideInHorizontally(tween(420)) { it } + fadeIn(tween(420))) togetherWith
                                (slideOutHorizontally(tween(420)) { -it } + fadeOut(tween(420)))
                        }
                    },
                    label = "permissionBottom",
                ) { index ->
                    val page = PermissionPages.all[index]
                    PermissionContent(
                        page = page,
                        status = status,
                        onOk = { onOk(page) },
                        onSkip = onSkip,
                        onOptionalBackground = onOptionalBackground,
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .navigationBarsPadding()
                        .padding(top = 4.dp, bottom = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(pageCount) { i ->
                        val active = i == pageIndex
                        val dotWidth by animateDpAsState(
                            targetValue = if (active) 24.dp else 8.dp,
                            animationSpec = tween(300),
                            label = "dotWidth",
                        )
                        Box(
                            modifier = Modifier
                                .width(dotWidth)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(if (active) colors.primary else colors.primary.copy(alpha = 0.25f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionContent(
    page: PermissionPage,
    status: PermissionStatus,
    onOk: () -> Unit,
    onSkip: () -> Unit,
    onOptionalBackground: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val music = page.kind == PermissionKind.MUSIC
    val granted = if (music) status.musicGranted else status.notificationsGranted
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 28.dp, end = 28.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        // The text shares a fixed top line and can scroll without shifting the actions.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PermissionStatusIcon(
                    kind = if (music) PermissionIconKind.MUSIC else PermissionIconKind.NOTIFICATIONS,
                    granted = granted,
                    tint = colors.primary,
                    badgeColor = colors.onBackground,
                    checkColor = colors.background,
                    badgeSize = 12.dp,
                    modifier = Modifier.size(26.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onBackground,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = page.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        PermissionActionRow(
            page = page,
            status = status,
            onPrimary = onOk,
            onSecondary = if (music) onOptionalBackground else onSkip,
        )
    }
}
