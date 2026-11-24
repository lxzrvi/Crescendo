package com.lxzrvi.crescendo.ui.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lxzrvi.crescendo.ui.components.CrescendoLoader
import com.lxzrvi.crescendo.ui.theme.Accent
import com.lxzrvi.crescendo.ui.theme.AmoledBlack
import com.lxzrvi.crescendo.ui.theme.BorelFamily
import com.lxzrvi.crescendo.ui.theme.TextPrimary
import com.lxzrvi.crescendo.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    var pageIndex by remember { mutableStateOf(0) }
    var finishing by remember { mutableStateOf(false) }
    val pageCount = PermissionPages.all.size
    val page = PermissionPages.all.getOrElse(pageIndex) { PermissionPages.all.last() }

    fun advance() {
        pageIndex++
    }

    // Runtime permission launcher (music / notifications)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.isEmpty() || result.values.any { it }
        if (granted) advance()
    }

    // Special-intent launcher (battery optimization exemption)
    val batteryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (pm?.isIgnoringBatteryOptimizations(context.packageName) == true) advance()
    }

    fun request(target: PermissionPage) {
        when (target.kind) {
            PermissionKind.MUSIC, PermissionKind.NOTIFICATIONS -> {
                val perms = PermissionPages.runtimePermissions(target.kind)
                if (perms.isEmpty()) advance() else permissionLauncher.launch(perms.toTypedArray())
            }
            PermissionKind.BACKGROUND -> {
                try {
                    batteryLauncher.launch(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                } catch (_: Exception) {
                    advance()
                }
            }
        }
    }

    // System back goes to previous permission
    BackHandler(enabled = pageIndex > 0 && !finishing) { pageIndex-- }

    // Background gradient top se travel karke apni jagah set hota hai
    val screenHPx = with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val bgTravel = remember { Animatable(1f) }
    LaunchedEffect(pageIndex) {
        if (pageIndex >= pageCount) {
            bgTravel.snapTo(0f)
            bgTravel.animateTo(1f, tween(durationMillis = 1000, easing = FastOutSlowInEasing))
        }
    }

    // Get Started: content fade out, icon center me travel, phir home
    LaunchedEffect(finishing) {
        if (finishing) {
            delay(1100)
            onFinished()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        GradientBlobBackground(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = (1f - bgTravel.value) * screenHPx * 0.6f
                }
        )

        // Main onboarding flow
        AnimatedVisibility(
            visible = !finishing,
            exit = fadeOut(tween(450)),
            modifier = Modifier.fillMaxSize(),
        ) {
            AnimatedContent(
                targetState = pageIndex,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    when {
                        // Last permission -> welcome: content upar slide karke bahar,
                        // welcome neeche se upar aata hai (screen ke top pe set)
                        targetState >= pageCount && targetState != initialState ->
                            (slideInVertically(animationSpec = tween(600)) { it } +
                                fadeIn(animationSpec = tween(600))) togetherWith
                                (slideOutVertically(animationSpec = tween(450)) { -it } +
                                    fadeOut(animationSpec = tween(450)))
                        targetState < initialState ->
                            (slideInHorizontally(animationSpec = tween(420)) { -it } +
                                fadeIn(animationSpec = tween(420))) togetherWith
                                (slideOutHorizontally(animationSpec = tween(420)) { it } +
                                    fadeOut(animationSpec = tween(420)))
                        else ->
                            (slideInHorizontally(animationSpec = tween(420)) { it } +
                                fadeIn(animationSpec = tween(420))) togetherWith
                                (slideOutHorizontally(animationSpec = tween(420)) { -it } +
                                    fadeOut(animationSpec = tween(420)))
                    }
                },
                label = "onboardingContent",
            ) { index ->
                if (index < pageCount) {
                    PermissionStep(
                        page = PermissionPages.all[index],
                        onOk = { request(PermissionPages.all[index]) },
                        pageIndex = index,
                        pageCount = pageCount,
                    )
                } else {
                    WelcomeContent(onGetStarted = { finishing = true })
                }
            }
        }

        // Finish transition: Crescendo icon center me
        AnimatedVisibility(
            visible = finishing,
            enter = fadeIn(tween(600)) + scaleIn(tween(600), initialScale = 0.5f),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CrescendoLoader(modifier = Modifier.size(88.dp))
            }
        }
    }
}

@Composable
private fun PermissionStep(
    page: PermissionPage,
    onOk: () -> Unit,
    pageIndex: Int,
    pageCount: Int,
) {
    Column(modifier = Modifier.fillMaxSize()) {

        // Center: Borel wordmark over the blobs
        Box(
            modifier = Modifier
                .height(300.dp)
                .fillMaxWidth(),
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

        PermissionContent(page = page, onOk = onOk)

        // Pehla style: round dots (active thoda chhoda pill)
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 20.dp, bottom = 36.dp),
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
                        .background(
                            color = if (active) Accent else Accent.copy(alpha = 0.25f)
                        )
                )
            }
        }
    }
}

@Composable
private fun PermissionContent(page: PermissionPage, onOk: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp, end = 28.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            textAlign = TextAlign.Start,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = page.detail,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Start,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onOk,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(18.dp), // iOS-style squircle
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = AmoledBlack,
            ),
        ) {
            Text(
                text = page.actionLabel,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                letterSpacing = 0.2.sp,
            )
        }
    }
}
