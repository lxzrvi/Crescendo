package com.lxzrvi.crescendo.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val CrescendoColors = darkColorScheme(
    primary = NeonPurple,
    onPrimary = AmoledBlack,
    secondary = VioletSoft,
    onSecondary = AmoledBlack,
    background = AmoledBlack,
    onBackground = TextPrimary,
    surface = InkBlack,
    onSurface = TextPrimary,
    surfaceVariant = InkBlack,
    onSurfaceVariant = TextSecondary,
)

@Composable
fun CrescendoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CrescendoColors,
        typography = AppTypography,
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides DefaultAppTextStyle,
            content = content,
        )
    }
}
