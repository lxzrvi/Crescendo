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
import androidx.compose.ui.graphics.lerp

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
    // Material's unspecified container colors default to purple. Derive all
    // accent-bearing slots from the user's existing Crescendo accent instead.
    val effectiveAccent = accent ?: base.primary
    val mutedAccent = lerp(base.surface, effectiveAccent, if (darkTheme) 0.32f else 0.18f)
    val colors = base.copy(
        primary = effectiveAccent,
        onPrimary = if (accent == null) base.onPrimary else if (effectiveAccent.luminance() > 0.50f) Color.Black else Color.White,
        primaryContainer = mutedAccent,
        onPrimaryContainer = base.onSurface,
        secondary = effectiveAccent,
        onSecondary = if (effectiveAccent.luminance() > 0.50f) Color.Black else Color.White,
        secondaryContainer = mutedAccent,
        onSecondaryContainer = base.onSurface,
        tertiary = effectiveAccent,
        onTertiary = if (effectiveAccent.luminance() > 0.50f) Color.Black else Color.White,
        tertiaryContainer = mutedAccent,
        onTertiaryContainer = base.onSurface,
        surfaceTint = effectiveAccent,
        surfaceContainerLowest = base.background,
        surfaceContainerLow = base.surface,
        surfaceContainer = lerp(base.surface, base.onSurface, 0.06f),
        surfaceContainerHigh = lerp(base.surface, base.onSurface, 0.10f),
        surfaceContainerHighest = lerp(base.surface, base.onSurface, 0.14f),
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
