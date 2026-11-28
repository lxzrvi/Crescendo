package com.lxzrvi.crescendo.ui.home

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Path as AndroidPath
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.PathParser
import com.lxzrvi.crescendo.R
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.drawPlainPrismalGlass
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import com.styropyr0.prismal.sources.rememberPrismalCanvasSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

private const val PROFILE_STEP_DP = 82f
private val profileLobeDiameter = (270f * PROFILE_STEP_DP / (300f * sqrt(2f))).dp

/** A single connected launcher-mark silhouette: Edit, avatar, Settings and Feedback. */
@Composable
internal fun ProfileMenu(
    profileName: String,
    onNameChange: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    pageBackdrop: PrismalBackdrop,
    liveBlur: Boolean,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    // The resource is also used by ic_launcher_foreground.xml: no traced or
    // approximated geometry can drift away from the real app icon.
    val markPath = remember(context) {
        requireNotNull(PathParser.createPathFromPathData(context.getString(R.string.launcher_mark_path)))
    }
    val prefs = remember(context) { context.getSharedPreferences("profile", android.content.Context.MODE_PRIVATE) }
    val store = remember(context) { BackgroundPhotoStore(context, "crescendo_profile_photo") }
    var photoVersion by remember(prefs) { mutableLongStateOf(prefs.getLong("photo_version", 0L)) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var draftName by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val photo by produceState<Bitmap?>(null, photoVersion) {
        value = if (photoVersion > 0L) withContext(Dispatchers.IO) { store.load(maxSide = 256) } else null
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            if (store.importPhoto(uri)) {
                photoVersion = System.currentTimeMillis().coerceAtLeast(1L)
                prefs.edit().putLong("photo_version", photoVersion).apply()
            } else error = "Couldn't use that photo. Please choose another image."
        }
    }
    val solid = navigationSolidColor(dark)
    val solidDraw: DrawScope.() -> Unit = remember(solid) { { drawRect(solid) } }
    val solidBackdrop = rememberPrismalCanvasSource(solidDraw)
    val backdrop = if (liveBlur) pageBackdrop else solidBackdrop

    val transition = updateTransition(expanded, label = "profileMenu")
    val scrim by transition.animateFloat(
        transitionSpec = { tween(260) }, label = "profileScrim",
    ) { if (it) 1f else 0f }
    // The actual mark scales from the avatar anchor, so the three actions and
    // the concave liquid links move together and arrive at exact logo geometry.
    val morph by transition.animateFloat(
        transitionSpec = { tween(if (targetState) 460 else 340, easing = FastOutSlowInEasing) },
        label = "profileMarkMorph",
    ) { if (it) 1f else 0f }
    val visible = expanded || scrim > 0.001f || morph > 0.001f
    BackHandler(enabled = expanded) { onExpandedChange(false) }

    Box(Modifier.fillMaxSize()) {
        if (visible) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.54f * scrim))
                    // Remain a hit barrier during the closing animation as well.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null,
                        role = Role.Button) { onExpandedChange(false) }
                    .semantics { contentDescription = "Close profile menu" },
            )
        }
        Box(
            Modifier.align(Alignment.TopEnd).statusBarsPadding()
                .padding(end = 10.dp).size(150.dp),
        ) {
            if (morph > 0.001f) {
                Box(Modifier.fillMaxSize().drawPlainPrismalGlass(
                    backdrop = backdrop,
                    shape = { ProfileMenuShape(markPath, morph, rtl) },
                    effects = {
                        // The exact, concave logo is not a rounded rectangle. Prismal's
                        // lens only supports rounded shapes, so blur this real path and
                        // give the circular avatar its own safe lens below.
                        applyPrismalGlassEffects(density = density, adaptiveLuminance = false, luminance = 0.5f,
                            blurRadiusPx = with(density) { 7.dp.toPx() }, refractionHeightPx = 0f,
                            refractionAmountPx = 0f, useVibrancy = false, saturation = 1.15f)
                    },
                    onDrawSurface = { drawMenuGlass(dark, density, morph, rtl) },
                ))
            }
            val dx = if (rtl) PROFILE_STEP_DP else -PROFILE_STEP_DP
            ProfileAction("Edit profile", R.drawable.ic_edit_profile, morph, dx, 0f, expanded) {
                draftName = profileName
                onExpandedChange(false)
                editing = true
            }
            ProfileAction("Settings", R.drawable.ic_nav_settings, morph, dx, PROFILE_STEP_DP, expanded) {
                onExpandedChange(false)
                onSettings()
            }
            ProfileAction("Feedback", R.drawable.ic_feedback, morph, 0f, PROFILE_STEP_DP, expanded) {
                onExpandedChange(false)
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/lxzrvi/Crescendo/issues/new")))
                } catch (_: Exception) {
                    error = "No browser is available. Visit github.com/lxzrvi/Crescendo/issues to share feedback."
                }
            }
            Box(
                modifier = Modifier.align(Alignment.TopEnd).size(68.dp).clip(CircleShape)
                    .clickable(role = Role.Button) { onExpandedChange(!expanded) }
                    .semantics {
                        contentDescription = "Profile, $profileName"
                        stateDescription = if (expanded) "Menu open" else "Menu closed"
                    },
                contentAlignment = Alignment.Center,
            ) {
                val glassSize = 48.dp + (profileLobeDiameter - 48.dp) * morph
                Box(
                    modifier = Modifier.size(glassSize).drawPlainPrismalGlass(
                        backdrop = backdrop,
                        shape = { CircleShape },
                        effects = {
                            applyPrismalGlassEffects(density = density, adaptiveLuminance = false, luminance = 0.5f,
                                blurRadiusPx = with(density) { 6.dp.toPx() },
                                refractionHeightPx = with(density) { 7.dp.toPx() },
                                refractionAmountPx = with(density) { 9.dp.toPx() },
                                depthEffect = true, useVibrancy = false, saturation = 1.15f)
                        },
                        onDrawSurface = { drawAvatarGlass(dark) },
                    ).clip(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (photo != null) {
                        Image(bitmap = photo!!.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.size(32.dp + 7.dp * morph).clip(CircleShape))
                    } else {
                        Icon(painterResource(R.drawable.ic_profile_filled), contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(28.dp + 2.dp * morph))
                    }
                }
            }
        }
    }

    if (editing) AlertDialog(
        onDismissRequest = { editing = false },
        title = { Text("Edit profile") },
        text = {
            Column {
                OutlinedTextField(
                    value = draftName, onValueChange = { draftName = it.take(28) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Display name") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                Spacer(Modifier.height(10.dp))
                Text("Your name and photo stay on this device.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f), fontSize = 12.sp, lineHeight = 17.sp)
                Row {
                    TextButton(onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text(if (photoVersion > 0L) "Change photo" else "Choose photo") }
                    if (photoVersion > 0L) TextButton(onClick = {
                        store.remove()
                        photoVersion = 0L
                        prefs.edit().remove("photo_version").apply()
                    }) { Text("Remove photo") }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = draftName.isNotBlank(), onClick = {
                onNameChange(draftName)
                editing = false
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } },
    )
    if (error != null) AlertDialog(
        onDismissRequest = { error = null }, title = { Text("Profile") }, text = { Text(error.orEmpty()) },
        confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } },
    )
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.ProfileAction(
    label: String,
    icon: Int,
    progress: Float,
    targetX: Float,
    targetY: Float,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val reveal = ((progress - 0.28f) / 0.52f).coerceIn(0f, 1f)
    if (reveal <= 0.001f) return
    val density = LocalDensity.current
    val travelX = with(density) { targetX.dp.toPx() }
    val travelY = with(density) { targetY.dp.toPx() }
    Box(
        modifier = Modifier.align(Alignment.TopEnd).size(68.dp).graphicsLayer {
            translationX = travelX * progress
            translationY = travelY * progress
            val scale = 0.30f + 0.70f * progress
            scaleX = scale; scaleY = scale
            alpha = reveal
        }.clip(CircleShape)
            .clickable(enabled = expanded && progress > 0.75f, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(29.dp))
    }
}

/**
 * Rotate the exact launcher path 45 degrees to put its four lobes under the
 * buttons. At full expansion this is precisely the launcher mark, not a C made
 * from separately approximated circles and bridges. RTL mirrors the same mark.
 */
private data class ProfileMenuShape(
    val launcherPath: AndroidPath,
    val progress: Float,
    val rtl: Boolean,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val unit = density.density
        val p = progress.coerceIn(0f, 1f)
        val mirror = if (rtl) -1f else 1f
        val anchorX = if (rtl) 34f * unit else size.width - 34f * unit
        val anchorY = 34f * unit
        // Original lobe centers are 300 path units from the origin. After the
        // 45-degree rotation they are exactly 82dp apart on each screen axis.
        val coefficient = PROFILE_STEP_DP * unit * p / 600f
        val matrix = Matrix().apply {
            setValues(floatArrayOf(
                mirror * coefficient, -mirror * coefficient, anchorX - mirror * PROFILE_STEP_DP * unit * p / 2f,
                coefficient, coefficient, anchorY + PROFILE_STEP_DP * unit * p / 2f,
                0f, 0f, 1f,
            ))
        }
        val result = AndroidPath()
        launcherPath.transform(matrix, result)
        if (p < 0.9999f) {
            // Keep the avatar full-size while the mark emerges from it. A true
            // union avoids path winding holes where the two silhouettes overlap.
            val radius = with(density) { (24.dp + (profileLobeDiameter / 2 - 24.dp) * p).toPx() }
            val avatar = AndroidPath().apply { addCircle(anchorX, anchorY, radius, AndroidPath.Direction.CCW) }
            result.op(avatar, AndroidPath.Op.UNION)
        }
        return Outline.Generic(result.asComposePath())
    }
}

private fun profileTint(dark: Boolean): Color = Color(0xFF0B101A).copy(alpha = if (dark) 0.48f else 0.60f)

/** Soft internal reflections and shading; deliberately no stroke or border. */
private fun DrawScope.drawMenuGlass(dark: Boolean, density: Density, progress: Float, rtl: Boolean) {
    drawRect(profileTint(dark))
    drawRect(Brush.linearGradient(
        0f to Color.White.copy(alpha = 0.17f),
        0.48f to Color.White.copy(alpha = 0.025f),
        1f to Color.Black.copy(alpha = 0.16f),
        start = Offset.Zero, end = Offset(size.width, size.height),
    ))
    val unit = density.density
    val p = progress.coerceIn(0f, 1f)
    val anchor = Offset(if (rtl) 34f * unit else size.width - 34f * unit, 34f * unit)
    val step = PROFILE_STEP_DP * unit * p
    val dx = if (rtl) step else -step
    val centers = listOf(anchor, Offset(anchor.x + dx, anchor.y),
        Offset(anchor.x + dx, anchor.y + step), Offset(anchor.x, anchor.y + step))
    val lobeRadius = with(density) { profileLobeDiameter.toPx() / 2f }
    centers.forEachIndexed { index, center ->
        val radius = if (index == 0) (24f * unit + (lobeRadius - 24f * unit) * p) else lobeRadius * p
        if (radius > 1f) {
            val spot = Offset(center.x - radius * 0.27f, center.y - radius * 0.36f)
            val glowRadius = radius * 1.2f
            val highlight = Brush.radialGradient(
                0f to Color.White.copy(alpha = 0.24f),
                0.48f to Color.White.copy(alpha = 0.075f),
                1f to Color.Transparent,
                center = spot, radius = glowRadius,
            )
            drawCircle(brush = highlight, radius = glowRadius, center = spot)
        }
    }
}

/** The circular profile glass additionally uses Prismal's refractive lens where supported. */
private fun DrawScope.drawAvatarGlass(dark: Boolean) {
    drawRect(profileTint(dark))
    drawRect(Brush.verticalGradient(
        0f to Color.White.copy(alpha = 0.20f),
        0.45f to Color.White.copy(alpha = 0.025f),
        1f to Color.Black.copy(alpha = 0.18f),
        endY = size.height,
    ))
    val spot = Offset(size.width * 0.28f, size.height * 0.20f)
    val radius = size.minDimension * 0.65f
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color.White.copy(alpha = 0.25f),
            0.55f to Color.White.copy(alpha = 0.06f),
            1f to Color.Transparent,
            center = spot, radius = radius.coerceAtLeast(1f),
        ),
        center = spot, radius = radius,
    )
}
