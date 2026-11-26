package com.lxzrvi.crescendo.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private val DarkCrescendoColors = darkColorScheme(
    primary = Accent,
    onPrimary = AmoledBlack,
    secondary = RedSoft,
    onSecondary = AmoledBlack,
    background = AmoledBlack,
    onBackground = TextPrimary,
    surface = InkBlack,
    onSurface = TextPrimary,
    surfaceVariant = DarkSecondarySurface,
    onSurfaceVariant = TextSecondary,
)

private val LightCrescendoColors = lightColorScheme(
    primary = LightAccent,
    onPrimary = Color.White,
    secondary = LightAccent,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightPrimaryText,
    surface = LightSurface,
    onSurface = LightPrimaryText,
    surfaceVariant = LightSecondarySurface,
    onSurfaceVariant = LightSecondaryText,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrescendoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: Color? = null,
    content: @Composable () -> Unit,
) {
    val base = if (darkTheme) DarkCrescendoColors else LightCrescendoColors
    val colors = if (accent == null) base else base.copy(
        primary = accent,
        onPrimary = if (accent.luminance() > 0.50f) Color.Black else Color.White,
        secondary = accent,
    )
    val smoothIndication = remember(colors.onBackground) { SmoothIndication(colors.onBackground) }
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides DefaultAppTextStyle.copy(color = colors.onBackground),
            LocalIndication provides smoothIndication,
            // Avoid Android 12's patterned, grainy platform ripples on Material controls.
            LocalRippleConfiguration provides null,
            content = content,
        )
    }
}
