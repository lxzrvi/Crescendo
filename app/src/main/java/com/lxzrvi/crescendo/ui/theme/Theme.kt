package com.lxzrvi.crescendo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

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

@Composable
fun CrescendoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkCrescendoColors else LightCrescendoColors
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides DefaultAppTextStyle.copy(color = colors.onBackground),
            content = content,
        )
    }
}
