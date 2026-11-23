package com.lxzrvi.crescendo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lxzrvi.crescendo.data.OnboardingPrefs
import com.lxzrvi.crescendo.ui.home.HomeScreen
import com.lxzrvi.crescendo.ui.onboarding.OnboardingScreen
import com.lxzrvi.crescendo.ui.theme.CrescendoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = OnboardingPrefs(this)
        setContent {
            CrescendoTheme {
                var onboardingComplete by remember { mutableStateOf(prefs.isOnboardingComplete()) }

                if (onboardingComplete) {
                    HomeScreen()
                } else {
                    OnboardingScreen(
                        onFinished = {
                            prefs.setOnboardingComplete(true)
                            onboardingComplete = true
                        }
                    )
                }
            }
        }
    }
}
