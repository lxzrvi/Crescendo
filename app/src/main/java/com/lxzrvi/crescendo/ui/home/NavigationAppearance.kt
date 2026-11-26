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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import kotlin.math.roundToInt

internal enum class BlurType(val label: String) {
    Soft("Soft"),
    Frosted("Frosted"),
    Gradient("Gradient"),
}

internal data class NavigationAppearance(
    val liveBlur: Boolean = true,
    val radius: Float = 9f,
    val type: BlurType = BlurType.Soft,
)

/** These preferences are separate from onboarding and survive process restarts. */
internal class NavigationAppearancePrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("navigation_appearance", Context.MODE_PRIVATE)

    fun load(): NavigationAppearance {
        val storedRadius = prefs.getFloat("blur_radius", 9f)
        return NavigationAppearance(
            liveBlur = prefs.getBoolean("live_blur", true),
            radius = if (storedRadius.isFinite()) storedRadius.coerceIn(0f, 24f) else 9f,
            type = BlurType.entries.firstOrNull { it.name == prefs.getString("blur_type", null) } ?: BlurType.Soft,
        )
    }

    fun save(appearance: NavigationAppearance) {
        prefs.edit()
            .putBoolean("live_blur", appearance.liveBlur)
            .putFloat("blur_radius", appearance.radius.coerceIn(0f, 24f))
            .putString("blur_type", appearance.type.name)
            .apply()
    }
}

/** Tint used over the live source; passed to the adaptive border as well. */
internal fun navigationTint(dark: Boolean, type: BlurType): Color {
    val alpha = when (type) {
        BlurType.Soft -> if (dark) 0.36f else 0.40f
        BlurType.Frosted -> if (dark) 0.57f else 0.62f
        BlurType.Gradient -> if (dark) 0.34f else 0.38f
    }
    return (if (dark) Color(0xFF1D1D20) else Color(0xFFFAFAFA)).copy(alpha = alpha)
}

internal fun navigationFallbackTint(dark: Boolean): Color =
    (if (dark) Color(0xFF222225) else Color(0xFFF5F5F5)).copy(alpha = 0.67f)

@Composable
internal fun rememberNavigationBlurStyle(appearance: NavigationAppearance, background: Color): HazeBlurStyle {
    val dark = background.luminance() < 0.5f
    val tint = navigationTint(dark, appearance.type)
    val fallback = navigationFallbackTint(dark)
    return remember(appearance, background, tint, fallback) {
        HazeBlurStyle {
            // Enabled explicitly on Android 11 and below as well: Haze uses its experimental,
            // asynchronous RenderScript renderer there. Switching it off is a true scrim fallback.
            blurEnabled(appearance.liveBlur && appearance.radius > 0f)
            blurRadius(appearance.radius.dp)
            noiseFactor(if (appearance.type == BlurType.Frosted) 0.035f else 0f)
            backgroundColor(background)
            colorEffects(listOf(HazeColorEffect.tint(tint)))
            fallbackColorEffect(HazeColorEffect.tint(fallback))
            progressive(
                if (appearance.type == BlurType.Gradient) {
                    HazeProgressive.verticalGradient(startIntensity = 0.15f, endIntensity = 1f)
                } else null
            )
        }
    }
}

internal fun settingsSurfaceColor(dark: Boolean): Color =
    if (dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.045f)

/** Live changes immediately affect both floating surfaces; the parent persists them. */
@Composable
internal fun NavigationBlurSettings(
    appearance: NavigationAppearance,
    onChange: (NavigationAppearance) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
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
            text = "Changes apply to both floating controls.",
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
                    text = if (appearance.liveBlur) "Blurs the content behind the bar" else "Off · translucent surface only",
                    color = colors.onBackground.copy(alpha = 0.64f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = appearance.liveBlur,
                onCheckedChange = { onChange(appearance.copy(liveBlur = it)) },
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Blur intensity", color = colors.onBackground, fontWeight = FontWeight.Medium)
            Text(
                "${appearance.radius.roundToInt()} dp",
                color = colors.onBackground.copy(alpha = 0.68f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Slider(
            value = appearance.radius,
            onValueChange = { onChange(appearance.copy(radius = it)) },
            valueRange = 0f..24f,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Blur style", color = colors.onBackground, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BlurType.entries.forEach { type ->
                val chosen = type == appearance.type
                Text(
                    text = type.label,
                    color = if (chosen) colors.primary else colors.onBackground.copy(alpha = 0.78f),
                    fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (chosen) colors.primary.copy(alpha = 0.16f) else colors.onBackground.copy(alpha = 0.07f))
                        .selectable(
                            selected = chosen,
                            role = Role.RadioButton,
                            onClick = { onChange(appearance.copy(type = type)) },
                        )
                        .padding(horizontal = 2.dp, vertical = 10.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "On older Android versions live blur can update a little more slowly.",
            color = colors.onBackground.copy(alpha = 0.55f),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
