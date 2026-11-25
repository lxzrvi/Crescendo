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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Keep the loader's starting size in sync with the larger welcome mark.
private const val MARK_START_SCALE = WELCOME_MARK_SIZE_DP / 88f

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
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

    // Permission pages keep the glow at the top; welcome moves it to the bottom.
    val bgTravel = remember { Animatable(1f) } // Permission-page forward transition.
    val bgPosition = remember { Animatable(0f) } // 0 = top, 1 = bottom.
    val prevIndex = remember { intArrayOf(0) }
    LaunchedEffect(pageIndex) {
        val previous = prevIndex[0]
        // Update before suspending so rapid taps/back gestures remain consistent.
        prevIndex[0] = pageIndex
        if (pageIndex >= pageCount) {
            launch { bgTravel.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }
            bgPosition.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
        } else {
            launch { bgPosition.animateTo(0f, tween(1000, easing = FastOutSlowInEasing)) }
            if (pageIndex > previous && previous < pageCount) {
                bgTravel.snapTo(0f)
            }
            bgTravel.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
        }
    }

    // ---- Let's get started: bg neeche slide + fade, icon center me aake loader ----
    val exit = remember { Animatable(0f) }        // bg vanish
    val loaderIn = remember { Animatable(0f) }    // icon -> center
    LaunchedEffect(finishing) {
        if (finishing) {
            launch { exit.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
            launch { loaderIn.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }
            delay(2400) // slide (0.8s) + loading animation (~1.6s)
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
            position = bgPosition.value,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val h = rootSize.height.toFloat()
                    // (1 - bgTravel): neeche se upar | exit: upar se neeche
                    translationY = ((1f - bgTravel.value) + exit.value) * h
                    alpha = 1f - exit.value
                }
        )

        AnimatedVisibility(
            visible = !finishing,
            exit = fadeOut(tween(300)),
            modifier = Modifier.fillMaxSize(),
        ) {
            // Outer: permission stage <-> welcome
            AnimatedContent(
                targetState = pageIndex >= pageCount,
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

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CrescendoLoader(
                    modifier = Modifier
                        .size(88.dp)
                        .graphicsLayer {
                            val t = loaderIn.value
                            translationX = start.x * (1f - t)
                            translationY = start.y * (1f - t)
                            val s = MARK_START_SCALE + (1f - MARK_START_SCALE) * t
                            scaleX = s
                            scaleY = s
                            alpha = (t * 5f).coerceIn(0f, 1f)
                        },
                    color = lerp(TextPrimary, Accent, loaderIn.value),
                )
            }
        }
    }
}

/** Crescendo wordmark + dots STATIC; sirf neeche ka content (title/detail/buttons) slide hota hai. */
@Composable
private fun PermissionStage(
    pageIndex: Int,
    pageCount: Int,
    onOk: (PermissionPage) -> Unit,
    onSkip: () -> Unit,
    onOptionalBackground: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // A fixed fraction of the screen keeps the wordmark still even when page text changes height.
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
            // Only the lower content slides; long text scrolls instead of moving the wordmark.
            AnimatedContent(
                targetState = pageIndex,
                modifier = Modifier
                    .fillMaxWidth()
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    PermissionContent(
                        page = page,
                        onOk = { onOk(page) },
                        onSkip = onSkip,
                        onOptionalBackground = onOptionalBackground,
                    )
                }
            }

            // Dots stay anchored; only the active dot's width animates.
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp, bottom = 36.dp),
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

@Composable
private fun PermissionContent(
    page: PermissionPage,
    onOk: () -> Unit,
    onSkip: () -> Unit,
    onOptionalBackground: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp, end = 28.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        // Label ke aage icon
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
        Spacer(modifier = Modifier.height(18.dp))

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
            shape = RoundedCornerShape(18.dp),
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
            shape = RoundedCornerShape(18.dp),
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
