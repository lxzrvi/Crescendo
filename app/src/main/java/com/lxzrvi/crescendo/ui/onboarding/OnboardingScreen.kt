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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lxzrvi.crescendo.ui.theme.AmoledBlack
import com.lxzrvi.crescendo.ui.theme.BorelFamily
import com.lxzrvi.crescendo.ui.theme.NeonPurple
import com.lxzrvi.crescendo.ui.theme.TextPrimary
import com.lxzrvi.crescendo.ui.theme.TextSecondary

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    var pageIndex by remember { mutableStateOf(0) }
    val pageCount = PermissionPages.all.size
    val page = PermissionPages.all[pageIndex]

    fun advance() {
        if (pageIndex + 1 >= pageCount) onFinished() else pageIndex++
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

    // System back goes to previous permission without sliding the whole screen
    BackHandler(enabled = pageIndex > 0) { pageIndex-- }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        // Static top area: soft moving purple shapes + Borel wordmark
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AnimatedPurpleBlobs(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(380.dp)
            )

            Text(
                text = "Crescendo",
                fontFamily = BorelFamily,
                fontSize = 72.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Only this bottom content slides left/right between permissions
        AnimatedContent(
            targetState = pageIndex,
            modifier = Modifier.fillMaxWidth(),
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(animationSpec = tween(420)) { it } +
                        fadeIn(animationSpec = tween(420))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(420)) { -it } +
                            fadeOut(animationSpec = tween(420)))
                } else {
                    (slideInHorizontally(animationSpec = tween(420)) { -it } +
                        fadeIn(animationSpec = tween(420))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(420)) { it } +
                            fadeOut(animationSpec = tween(420)))
                }
            },
            label = "permissionContent",
        ) { index ->
            PermissionContent(
                page = PermissionPages.all[index],
                onOk = { request(PermissionPages.all[index]) },
            )
        }

        // Square indicator, small and low
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 20.dp, bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(pageCount) { i ->
                val active = i == pageIndex
                val dotSize by animateDpAsState(
                    targetValue = if (active) 10.dp else 7.dp,
                    animationSpec = tween(300),
                    label = "dotSize",
                )
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            color = if (active) NeonPurple else NeonPurple.copy(alpha = 0.25f)
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
            .padding(start = 28.dp, end = 28.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            textAlign = TextAlign.Start,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = page.detail,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Start,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onOk,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonPurple,
                contentColor = AmoledBlack,
            ),
        ) {
            Text(
                text = page.actionLabel,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                letterSpacing = 1.sp,
            )
        }
    }
}
