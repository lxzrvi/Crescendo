package com.lxzrvi.crescendo.ui.home

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.components.PrismalGlassButton
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import com.styropyr0.prismal.shapes.PrismalCapsule
import com.styropyr0.prismal.shapes.PrismalRoundedRectangle
import com.styropyr0.prismal.sources.rememberPrismalCanvasSource
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal enum class AppThemeMode(val label: String) { System("System"), Dark("Dark"), Light("Light") }
internal enum class AccentChoice(val label: String, val color: Color) {
    Crescendo("Red", Color(0xFFF2293A)), Coral("Coral", Color(0xFFF36F66)),
    Violet("Violet", Color(0xFFAA76EF)), Ocean("Blue", Color(0xFF4B9FF5)), Mint("Mint", Color(0xFF45C9A1)),
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
        prefs.edit().putBoolean("live_blur", appearance.liveBlur).putString("theme_mode", appearance.themeMode.name)
            .putString("accent", appearance.accent.name).putLong("background_photo_version", appearance.backgroundPhotoVersion)
            .putFloat("photo_blur", appearance.photoBlur.coerceIn(0f, 24f))
            .putFloat("photo_dim", appearance.photoDim.coerceIn(0.20f, 0.85f))
            .putBoolean("animate_hero", appearance.animateHero).remove("blur_radius").remove("blur_type").apply()
    }
}
internal fun navigationSolidColor(dark: Boolean): Color = if (dark) Color(0xFF252529) else Color(0xFFF6F4F4)
internal fun settingsSurfaceColor(dark: Boolean): Color = if (dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.045f)

/** Prismal's grouped liquid-glass settings style, retaining Crescendo's existing preferences. */
@Composable
internal fun NavigationBlurSettings(
    appearance: NavigationAppearance,
    onChange: (NavigationAppearance) -> Unit,
    backdrop: PrismalBackdrop,
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
            importing = true; error = null
            if (store.importPhoto(uri)) {
                onChange(latestAppearance.copy(backgroundPhotoVersion = System.currentTimeMillis().coerceAtLeast(1L)))
            } else error = "Couldn't use this image. Please choose another photo."
            importing = false
        }
    }
    val solidColor = navigationSolidColor(dark)
    val solidDraw: DrawScope.() -> Unit = remember(solidColor) { { drawRect(solidColor) } }
    val solidBackdrop = rememberPrismalCanvasSource(solidDraw)
    val source = if (appearance.liveBlur) backdrop else solidBackdrop

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        SettingsHeader("Appearance")
        GlassSettingsGroup(source, dark) {
            Text("Theme", color = colors.onBackground, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(Modifier.height(9.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                AppThemeMode.entries.forEach { mode ->
                    val selectedMode = appearance.themeMode == mode
                    PrismalGlassButton(
                        onClick = { onChange(appearance.copy(themeMode = mode)) }, backdrop = source,
                        modifier = Modifier.weight(1f).semantics { selected = selectedMode; contentDescription = "${mode.label} theme" },
                        height = 40.dp, contentPadding = PaddingValues(horizontal = 2.dp),
                        tint = if (selectedMode) colors.primary else Color.Unspecified,
                        tintAlpha = 0.28f, surfaceColor = colors.onBackground.copy(alpha = 0.06f),
                    ) { Text(mode.label, maxLines = 1, color = colors.onBackground, fontSize = 12.sp) }
                }
            }
            GlassDivider()
            Text("Accent color", color = colors.onBackground, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AccentChoice.entries.forEach { accent ->
                    val selectedAccent = appearance.accent == accent
                    Box(Modifier.size(35.dp).clip(CircleShape).background(accent.color)
                        .border(if (selectedAccent) 3.dp else 0.dp, colors.onBackground, CircleShape)
                        .clickable(role = Role.RadioButton) { onChange(appearance.copy(accent = accent)) }
                        .semantics { contentDescription = accent.label; this.selected = selectedAccent })
                }
            }
            GlassDivider()
            SettingSwitch("Live glass blur", "Blur navigation, player and controls", appearance.liveBlur, source) {
                onChange(appearance.copy(liveBlur = it))
            }
            GlassDivider()
            SettingSwitch("Moving cover colors", "Animate covers and mood cards", appearance.animateHero, source) {
                onChange(appearance.copy(animateHero = it))
            }
        }
        Spacer(Modifier.height(6.dp))
        SettingsHeader("Background")
        GlassSettingsGroup(source, dark) {
            Text("Background photo", color = colors.onBackground, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text("Saved privately on this device", color = colors.onBackground.copy(alpha = 0.6f), fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(enabled = !importing, onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text(if (importing) "Adding…" else if (appearance.backgroundPhotoVersion > 0) "Replace photo" else "Choose photo") }
                if (appearance.backgroundPhotoVersion > 0) TextButton(onClick = {
                    store.remove(); onChange(appearance.copy(backgroundPhotoVersion = 0))
                }) { Text("Remove") }
            }
            if (error != null) Text(error!!, color = colors.error, fontSize = 12.sp)
            if (appearance.backgroundPhotoVersion > 0) {
                GlassDivider()
                Text("Photo blur · ${appearance.photoBlur.roundToInt()}", color = colors.onBackground, fontSize = 14.sp)
                Slider(value = appearance.photoBlur, onValueChange = { onChange(appearance.copy(photoBlur = it)) },
                    valueRange = 0f..24f, colors = settingSliderColors())
                Text("Photo dim · ${(appearance.photoDim * 100).roundToInt()}%", color = colors.onBackground, fontSize = 14.sp)
                Slider(value = appearance.photoDim, onValueChange = { onChange(appearance.copy(photoDim = it)) },
                    valueRange = 0.20f..0.85f, colors = settingSliderColors())
            }
        }
    }
}

@Composable
private fun SettingsHeader(title: String) {
    Text(title.uppercase(), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f),
        fontSize = 12.sp, letterSpacing = 0.7.sp, modifier = Modifier.padding(start = 12.dp, top = 4.dp))
}

@Composable
private fun GlassSettingsGroup(backdrop: PrismalBackdrop, dark: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val density = LocalDensity.current
    val shape = remember { PrismalRoundedRectangle(22.dp) }
    Column(Modifier.fillMaxWidth().drawPrismalGlass(
        backdrop = backdrop, shape = { shape },
        effects = { applyPrismalGlassEffects(density = density, adaptiveLuminance = false, luminance = 0.5f,
            blurRadiusPx = with(density) { 10.dp.toPx() }, refractionHeightPx = with(density) { 16.dp.toPx() },
            refractionAmountPx = with(density) { 25.dp.toPx() }) },
        onDrawSurface = { drawRect(if (dark) Color(0xFF171719).copy(alpha = 0.50f) else Color.White.copy(alpha = 0.56f)) },
    ).clip(RoundedCornerShape(22.dp)).padding(16.dp), content = content)
}

@Composable
private fun GlassDivider() {
    Box(Modifier.fillMaxWidth().padding(vertical = 14.dp).height(0.5.dp)
        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)))
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, backdrop: PrismalBackdrop, onCheckedChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val thumbX by animateDpAsState(if (checked) 24.dp else 2.dp, spring(stiffness = 450f), label = "glassToggle")
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, color = colors.onBackground, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(subtitle, color = colors.onBackground.copy(alpha = 0.62f), fontSize = 12.sp, lineHeight = 15.sp)
        }
        Box(Modifier.width(54.dp).height(32.dp).clip(PrismalCapsule())
            .background(if (checked) colors.primary else colors.onBackground.copy(alpha = 0.20f))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics { contentDescription = title }, contentAlignment = Alignment.CenterStart) {
            Box(Modifier.offset(x = thumbX).size(28.dp).drawPrismalGlass(
                backdrop = backdrop, shape = { PrismalCapsule() },
                effects = { applyPrismalGlassEffects(density = density, adaptiveLuminance = false, luminance = 0.5f,
                    blurRadiusPx = with(density) { 6.dp.toPx() }, refractionHeightPx = with(density) { 9.dp.toPx() },
                    refractionAmountPx = with(density) { 16.dp.toPx() }) },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.72f)) },
            ))
        }
    }
}

@Composable
private fun settingSliderColors() = SliderDefaults.colors(
    thumbColor = MaterialTheme.colorScheme.primary,
    activeTrackColor = MaterialTheme.colorScheme.primary,
    inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.16f),
)
