package com.lxzrvi.crescendo.ui.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.lxzrvi.crescendo.ui.components.BellIcon
import com.lxzrvi.crescendo.ui.components.CrescendoLoader
import com.lxzrvi.crescendo.ui.components.MusicNoteIcon
import com.lxzrvi.crescendo.ui.theme.Accent
import com.lxzrvi.crescendo.ui.theme.AmoledBlack
import com.lxzrvi.crescendo.ui.theme.BorelFamily
import com.lxzrvi.crescendo.ui.theme.TextPrimary
import com.lxzrvi.crescendo.ui.theme.TextSecondary
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val ONBOARDING_ACTION_RADIUS_DP = 15f

// Keep the loader's starting size in sync with the larger welcome mark.
private const val LOADER_SIZE_DP = 88f
private const val MARK_START_SCALE = WELCOME_MARK_SIZE_DP / LOADER_SIZE_DP

@Composable
fun OnboardingScreen(gradientPhase: State<Float>, onFinished: () -> Unit) {
    val context = LocalContext.current
    var pageIndex by remember { mutableStateOf(0) }
    var finishing by remember { mutableStateOf(false) }
    val pageCount = PermissionPages.all.size

    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var markCenter by remember { mutableStateOf<Offset?>(null) } // welcome icon ki position

    fun advance() {
        pageIndex++
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.isEmpty() || result.values.any { it }
        if (granted) advance()
    }

    val batteryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val audioPermission =
            if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
            else Manifest.permission.READ_EXTERNAL_STORAGE
        val audioGranted = ContextCompat.checkSelfPermission(context, audioPermission) ==
            PackageManager.PERMISSION_GRANTED
        if (audioGranted) advance()
    }

    fun request(target: PermissionPage) {
        val perms = PermissionPages.runtimePermissions(target.kind)
        if (perms.isEmpty()) advance() else permissionLauncher.launch(perms.toTypedArray())
    }

    fun requestOptionalBackground() {
        try {
            batteryLauncher.launch(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            )
        } catch (_: Exception) {
        }
    }

    BackHandler(enabled = pageIndex > 0 && !finishing) { pageIndex-- }

    // The glow stays in place across both permissions, then glides down for welcome.
    val bgPosition = remember { Animatable(0f) } // 0 = top, 1 = bottom.
    val showWelcome = pageIndex >= pageCount
    LaunchedEffect(showWelcome) {
        bgPosition.animateTo(
            targetValue = if (showWelcome) 1f else 0f,
            animationSpec = tween(1300, easing = FastOutSlowInEasing),
        )
    }

    // Welcome glow exits down as the mark enters; Home glow later arrives from above.
    val exit = remember { Animatable(0f) }
    val loaderIn = remember { Animatable(0f) }
    val loaderPulse = remember { Animatable(1f) }
    val loaderDepart = remember { Animatable(0f) }
    val homeGlow = remember { Animatable(0f) }
    var revealingHomeGlow by remember { mutableStateOf(false) }
    LaunchedEffect(finishing) {
        if (finishing) {
            launch { exit.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
            launch { loaderIn.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }
            delay(2400) // Entry plus a moment to see the comet loader.

            // The old glow is invisible; reset its shape before bringing it in from the top.
            bgPosition.snapTo(0f)
            revealingHomeGlow = true
            coroutineScope {
                launch { homeGlow.animateTo(1f, tween(1300, easing = FastOutSlowInEasing)) }
                launch {
                    loaderPulse.animateTo(1.08f, tween(220, easing = FastOutSlowInEasing))
                    loaderDepart.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
                }
            }
            onFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .onSizeChanged { rootSize = it },
    ) {

        GradientBlobBackground(
            phase = gradientPhase,
            position = { bgPosition.value },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val h = rootSize.height.toFloat()
                    if (revealingHomeGlow) {
                        translationY = (homeGlow.value - 1f) * h
                        alpha = (homeGlow.value * 2f).coerceIn(0f, 1f)
                    } else {
                        translationY = exit.value * h
                        alpha = 1f - exit.value
                    }
                }
        )

        AnimatedVisibility(
            visible = !finishing,
            exit = fadeOut(tween(300)),
            modifier = Modifier.fillMaxSize(),
        ) {
            // Outer: permission stage <-> welcome
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
                        onGetStarted = { finishing = true },
                        onMarkPositioned = { markCenter = it },
                    )
                } else {
                    PermissionStage(
                        pageIndex = pageIndex.coerceAtMost(pageCount - 1),
                        pageCount = pageCount,
                        onOk = { request(it) },
                        onSkip = { advance() },
                        onOptionalBackground = { requestOptionalBackground() },
                    )
                }
            }
        }

        // Welcome ka icon right se center me slide hoke loader ban jata hai
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
                    color = lerp(TextPrimary, Accent, loaderIn.value),
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
    onOk: (PermissionPage) -> Unit,
    onSkip: () -> Unit,
    onOptionalBackground: () -> Unit,
) {
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
            // The wordmark's position does not depend on the page or description height.
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
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Bottom,
            ) {
                // Measure the longest copy once so labels align, buttons stay fixed,
                // and neither page has an unnecessarily tall empty area.
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
                        onOk = { onOk(page) },
                        onSkip = onSkip,
                        onOptionalBackground = onOptionalBackground,
                    )
                }

                // Five dp above the bottom safe area, without overlapping system navigation.
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
                                .background(if (active) Accent else Accent.copy(alpha = 0.25f))
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
    onOk: () -> Unit,
    onSkip: () -> Unit,
    onOptionalBackground: () -> Unit,
) {
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
                when (page.kind) {
                    PermissionKind.MUSIC -> MusicNoteIcon(Modifier.size(26.dp), tint = Accent)
                    PermissionKind.NOTIFICATIONS -> BellIcon(Modifier.size(26.dp), tint = Accent)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = page.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Same bottom anchor and height on both pages, regardless of description length.
        when (page.kind) {
            PermissionKind.MUSIC -> ActionRow(
                primaryLabel = page.actionLabel,
                onPrimary = onOk,
                secondaryLabel = "Background Playback",
                onSecondary = onOptionalBackground,
            )
            PermissionKind.NOTIFICATIONS -> ActionRow(
                primaryLabel = page.actionLabel,
                onPrimary = onOk,
                secondaryLabel = "Skip",
                onSecondary = onSkip,
            )
        }
    }
}

/** Ek hi row: primary (filled) + secondary (outlined). Dono pages me same height. */
@Composable
private fun ActionRow(
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onPrimary,
            modifier = Modifier
                .weight(1.25f)
                .height(52.dp),
            shape = RoundedCornerShape(ONBOARDING_ACTION_RADIUS_DP.dp),
            contentPadding = PaddingValues(horizontal = 10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = AmoledBlack,
            ),
        ) {
            Text(
                text = primaryLabel,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }

        OutlinedButton(
            onClick = onSecondary,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(ONBOARDING_ACTION_RADIUS_DP.dp),
            contentPadding = PaddingValues(horizontal = 10.dp),
            border = BorderStroke(1.dp, Accent.copy(alpha = 0.6f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
        ) {
            Text(
                text = secondaryLabel,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                textAlign = TextAlign.Center,
            )
        }
    }
}
