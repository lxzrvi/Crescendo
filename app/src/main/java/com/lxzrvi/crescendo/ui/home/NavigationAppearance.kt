package com.lxzrvi.crescendo.ui.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect

/** The blur has one deliberately fixed look; only the on/off preference is user-configurable. */
internal data class NavigationAppearance(val liveBlur: Boolean = true)

internal class NavigationAppearancePrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("navigation_appearance", Context.MODE_PRIVATE)

    fun load(): NavigationAppearance = NavigationAppearance(
        liveBlur = prefs.getBoolean("live_blur", true),
    )

    fun save(appearance: NavigationAppearance) {
        prefs.edit()
            .putBoolean("live_blur", appearance.liveBlur)
            // Discard the old customization so an upgrade always uses Soft at 5 dp.
            .remove("blur_radius")
            .remove("blur_type")
            .apply()
    }
}

internal fun navigationTint(dark: Boolean): Color =
    (if (dark) Color(0xFF1D1D20) else Color(0xFFFAFAFA))
        .copy(alpha = if (dark) 0.36f else 0.40f)

/** Always opaque: it is drawn directly, rather than relying on a translucent Haze fallback. */
internal fun navigationSolidColor(dark: Boolean): Color =
    if (dark) Color(0xFF252529) else Color(0xFFF6F4F4)

@Composable
internal fun rememberNavigationBlurStyle(background: Color): HazeBlurStyle {
    val dark = background.luminance() < 0.5f
    return remember(background, dark) {
        HazeBlurStyle {
            // Explicitly enable Haze's experimental older-Android renderer as well.
            blurEnabled(true)
            blurRadius(5.dp)
            noiseFactor(0f)
            backgroundColor(background)
            colorEffects(listOf(HazeColorEffect.tint(navigationTint(dark))))
            fallbackColorEffect(HazeColorEffect.tint(navigationSolidColor(dark)))
        }
    }
}

internal fun settingsSurfaceColor(dark: Boolean): Color =
    if (dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.045f)

@Composable
internal fun NavigationBlurSettings(
    appearance: NavigationAppearance,
    onChange: (NavigationAppearance) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(settingsSurfaceColor(dark))
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Text(
            text = "Navigation appearance",
            color = colors.onBackground,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Controls the live backdrop, including the status-bar blur.",
            color = colors.onBackground.copy(alpha = 0.64f),
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Live blur", color = colors.onBackground, fontWeight = FontWeight.Medium)
                Text(
                    text = if (appearance.liveBlur) "Soft · 5 dp on floating controls" else "Off · solid floating controls",
                    color = colors.onBackground.copy(alpha = 0.64f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = appearance.liveBlur,
                onCheckedChange = { onChange(appearance.copy(liveBlur = it)) },
            )
        }
    }
}
