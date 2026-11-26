package com.lxzrvi.crescendo.ui.home

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal enum class AppThemeMode(val label: String) { System("System"), Dark("Dark"), Light("Light") }

internal enum class AccentChoice(val label: String, val color: Color) {
    Crescendo("Red", Color(0xFFF2293A)),
    Coral("Coral", Color(0xFFF36F66)),
    Violet("Violet", Color(0xFFAA76EF)),
    Ocean("Blue", Color(0xFF4B9FF5)),
    Mint("Mint", Color(0xFF45C9A1)),
}

internal data class NavigationAppearance(
    val liveBlur: Boolean = true,
    val themeMode: AppThemeMode = AppThemeMode.System,
    val accent: AccentChoice = AccentChoice.Crescendo,
    val backgroundPhotoVersion: Long = 0,
    val photoBlur: Float = 12f,
    val photoDim: Float = 0.55f,
    val animateHero: Boolean = true,
)

internal class NavigationAppearancePrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("navigation_appearance", Context.MODE_PRIVATE)

    fun load(): NavigationAppearance = NavigationAppearance(
        liveBlur = prefs.getBoolean("live_blur", true),
        themeMode = enumValues<AppThemeMode>().firstOrNull { it.name == prefs.getString("theme_mode", null) } ?: AppThemeMode.System,
        accent = enumValues<AccentChoice>().firstOrNull { it.name == prefs.getString("accent", null) } ?: AccentChoice.Crescendo,
        backgroundPhotoVersion = prefs.getLong("background_photo_version", 0),
        photoBlur = prefs.getFloat("photo_blur", 12f).coerceIn(0f, 24f),
        photoDim = prefs.getFloat("photo_dim", 0.55f).coerceIn(0.20f, 0.85f),
        animateHero = prefs.getBoolean("animate_hero", true),
    )

    fun save(appearance: NavigationAppearance) {
        prefs.edit()
            .putBoolean("live_blur", appearance.liveBlur)
            .putString("theme_mode", appearance.themeMode.name)
            .putString("accent", appearance.accent.name)
            .putLong("background_photo_version", appearance.backgroundPhotoVersion)
            .putFloat("photo_blur", appearance.photoBlur.coerceIn(0f, 24f))
            .putFloat("photo_dim", appearance.photoDim.coerceIn(0.20f, 0.85f))
            .putBoolean("animate_hero", appearance.animateHero)
            .remove("blur_radius")
            .remove("blur_type")
            .apply()
    }
}

internal fun navigationSolidColor(dark: Boolean): Color =
    if (dark) Color(0xFF252529) else Color(0xFFF6F4F4)

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
    val context = LocalContext.current
    val store = remember(context) { BackgroundPhotoStore(context) }
    val scope = rememberCoroutineScope()
    val latestAppearance by rememberUpdatedState(appearance)
    var importing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            importing = true
            error = null
            if (store.importPhoto(uri)) {
                onChange(latestAppearance.copy(backgroundPhotoVersion = System.currentTimeMillis().coerceAtLeast(1L)))
            } else {
                error = "Couldn't use this image. Please choose another photo."
            }
            importing = false
        }
    }
    val sectionShape = RoundedCornerShape(20.dp)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(sectionShape).background(settingsSurfaceColor(dark)).padding(15.dp),
        ) {
            SettingsTitle("Appearance", "Make Crescendo feel like yours")
            Spacer(Modifier.height(12.dp))
            Text("Theme", color = colors.onBackground, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                AppThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = appearance.themeMode == mode,
                        onClick = { onChange(appearance.copy(themeMode = mode)) },
                        label = { Text(mode.label, maxLines = 1) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Accent color", color = colors.onBackground, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(13.dp), verticalAlignment = Alignment.CenterVertically) {
                AccentChoice.entries.forEach { accent ->
                    val selected = appearance.accent == accent
                    Box(
                        modifier = Modifier.size(38.dp).clip(CircleShape)
                            .background(accent.color)
                            .border(if (selected) 3.dp else 0.dp, colors.onBackground, CircleShape)
                            .clickable(role = Role.RadioButton) { onChange(appearance.copy(accent = accent)) }
                            .semantics { contentDescription = accent.label; this.selected = selected },
                    )
                }
            }
            Spacer(Modifier.height(15.dp))
            SettingSwitch("Live glass blur", "Blur the navigation, player and status area", appearance.liveBlur) {
                onChange(appearance.copy(liveBlur = it))
            }
            SettingSwitch("Moving cover colors", "Let the featured artwork slowly change", appearance.animateHero) {
                onChange(appearance.copy(animateHero = it))
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().clip(sectionShape).background(settingsSurfaceColor(dark)).padding(15.dp),
        ) {
            SettingsTitle("Background photo", "Saved in the app’s private storage")
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(enabled = !importing, onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text(if (importing) "Adding…" else if (appearance.backgroundPhotoVersion > 0) "Replace photo" else "Choose photo") }
                if (appearance.backgroundPhotoVersion > 0) {
                    TextButton(onClick = {
                        store.remove()
                        onChange(appearance.copy(backgroundPhotoVersion = 0))
                    }) { Text("Remove") }
                }
            }
            if (error != null) Text(error!!, color = colors.error, fontSize = 12.sp)
            if (appearance.backgroundPhotoVersion > 0) {
                Text("Photo blur · ${appearance.photoBlur.roundToInt()}", color = colors.onBackground, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Slider(value = appearance.photoBlur, onValueChange = { onChange(appearance.copy(photoBlur = it)) }, valueRange = 0f..24f)
                Text("Photo dim · ${(appearance.photoDim * 100).roundToInt()}%", color = colors.onBackground, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Slider(value = appearance.photoDim, onValueChange = { onChange(appearance.copy(photoDim = it)) }, valueRange = 0.20f..0.85f)
            }
        }
    }
}

@Composable
private fun SettingsTitle(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
    Text(subtitle, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f), fontSize = 12.sp, lineHeight = 16.sp)
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.onBackground, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(subtitle, color = colors.onBackground.copy(alpha = 0.62f), fontSize = 12.sp, lineHeight = 15.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
