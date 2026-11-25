package com.lxzrvi.crescendo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lxzrvi.crescendo.data.OnboardingPrefs
import com.lxzrvi.crescendo.ui.home.HomeScreen
import com.lxzrvi.crescendo.ui.onboarding.OnboardingScreen
import com.lxzrvi.crescendo.ui.onboarding.hasMusicAccess
import com.lxzrvi.crescendo.ui.onboarding.rememberGradientPhase
import com.lxzrvi.crescendo.ui.theme.CrescendoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = OnboardingPrefs(this)
        setContent {
            CrescendoTheme {
                var onboardingComplete by remember {
                    mutableStateOf(prefs.isOnboardingComplete() && hasMusicAccess(this@MainActivity))
                }
                val lifecycleOwner = LocalLifecycleOwner.current
                val gradientPhase = rememberGradientPhase()

                // If library access is revoked in Settings, Home cannot bypass the music gate.
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME && !hasMusicAccess(this@MainActivity)) {
                            onboardingComplete = false
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                if (onboardingComplete) {
                    HomeScreen(gradientPhase = gradientPhase)
                } else {
                    OnboardingScreen(
                        gradientPhase = gradientPhase,
                        onFinished = {
                            if (hasMusicAccess(this@MainActivity)) {
                                prefs.setOnboardingComplete(true)
                                onboardingComplete = true
                            }
                        },
                    )
                }
            }
        }
    }
}
