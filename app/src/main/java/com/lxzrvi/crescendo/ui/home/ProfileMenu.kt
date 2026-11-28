package com.lxzrvi.crescendo.ui.home

import android.content.Intent
import android.graphics.Bitmap
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
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
import com.lxzrvi.crescendo.R
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.drawPlainPrismalGlass
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import com.styropyr0.prismal.sources.rememberPrismalCanvasSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot

/** A single connected C silhouette: Edit, avatar, Settings and Feedback. */
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
    // One uniform surface makes the links and four circular lobes a single shape.
    val surface = if (dark) Color(0xFF29292E).copy(alpha = 0.94f) else Color(0xFF111114).copy(alpha = 0.94f)

    val transition = updateTransition(expanded, label = "profileMenu")
    val scrim by transition.animateFloat(
        transitionSpec = { tween(if (targetState) 260 else 220) }, label = "profileScrim",
    ) { if (it) 1f else 0f }
    val edit by transition.animateFloat(
        transitionSpec = { tween(if (targetState) 390 else 240, delayMillis = if (targetState) 0 else 35, easing = FastOutSlowInEasing) },
        label = "profileEdit",
    ) { if (it) 1f else 0f }
    val settings by transition.animateFloat(
        transitionSpec = { tween(if (targetState) 430 else 240, delayMillis = if (targetState) 65 else 15, easing = FastOutSlowInEasing) },
        label = "profileSettings",
    ) { if (it) 1f else 0f }
    val feedback by transition.animateFloat(
        transitionSpec = { tween(if (targetState) 430 else 220, delayMillis = if (targetState) 115 else 0, easing = FastOutSlowInEasing) },
        label = "profileFeedback",
    ) { if (it) 1f else 0f }
    val visible = expanded || scrim > 0.001f || edit > 0.001f || settings > 0.001f || feedback > 0.001f
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
            Box(Modifier.fillMaxSize().drawPlainPrismalGlass(
                backdrop = backdrop,
                shape = { ProfileMenuShape(edit, settings, feedback, scrim, rtl) },
                effects = { applyPrismalGlassEffects(density = density, adaptiveLuminance = false, luminance = 0.5f,
                    blurRadiusPx = with(density) { 7.dp.toPx() }, refractionHeightPx = 0f, refractionAmountPx = 0f) },
                onDrawSurface = { drawRect(surface) },
            ))
            val dx = if (rtl) 82f else -82f
            ProfileAction("Edit profile", R.drawable.ic_edit_profile, edit, dx, 0f, expanded) {
                draftName = profileName
                onExpandedChange(false)
                editing = true
            }
            ProfileAction("Settings", R.drawable.ic_nav_settings, settings, dx, 82f, expanded) {
                onExpandedChange(false)
                onSettings()
            }
            ProfileAction("Feedback", R.drawable.ic_feedback, feedback, 0f, 82f, expanded) {
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
                val pictureSize = 40.dp + 16.dp * scrim
                Box(Modifier.size(pictureSize).clip(CircleShape).background(Color(0xFFBFC0C5)),
                    contentAlignment = Alignment.Center) {
                    if (photo != null) {
                        Image(bitmap = photo!!.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(painterResource(R.drawable.ic_profile_filled), contentDescription = null,
                            tint = Color.White, modifier = Modifier.fillMaxSize(0.78f))
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
    if (progress <= 0.001f) return
    val density = LocalDensity.current
    val travelX = with(density) { targetX.dp.toPx() }
    val travelY = with(density) { targetY.dp.toPx() }
    Box(
        modifier = Modifier.align(Alignment.TopEnd).size(68.dp).graphicsLayer {
            translationX = travelX * progress
            translationY = travelY * progress
            val scale = 0.58f + 0.42f * progress
            scaleX = scale; scaleY = scale
            alpha = progress.coerceIn(0f, 1f)
        }.clip(CircleShape)
            .clickable(enabled = expanded && progress > 0.75f, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(29.dp))
    }
}

/** The three permanent concave links follow the reference's C, leaving the right side open. */
private data class ProfileMenuShape(
    val edit: Float,
    val settings: Float,
    val feedback: Float,
    val open: Float,
    val rtl: Boolean,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val unit = density.density
        val direction = if (rtl) 1f else -1f
        val origin = Offset(if (rtl) 34f * unit else size.width - 34f * unit, 34f * unit)
        val e = Offset(origin.x + direction * 82f * unit * edit, origin.y)
        val s = Offset(origin.x + direction * 82f * unit * settings, origin.y + 82f * unit * settings)
        val f = Offset(origin.x, origin.y + 82f * unit * feedback)
        val path = Path()
        fun circle(center: Offset, radius: Float) {
            if (radius > 0.01f) path.addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
        }
        fun link(a: Offset, b: Offset, strength: Float) {
            val p = strength.coerceIn(0f, 1f)
            val vx = b.x - a.x
            val vy = b.y - a.y
            val distance = hypot(vx, vy)
            if (p < 0.01f || distance < 1f) return
            val nx = -vy / distance
            val ny = vx / distance
            val outer = 26f * unit * p
            val neck = 13f * unit * p
            // addOval winds counter-clockwise; use the same winding for each bridge.
            path.moveTo(a.x + nx * outer, a.y + ny * outer)
            path.cubicTo(a.x + vx * 0.32f + nx * neck, a.y + vy * 0.32f + ny * neck,
                a.x + vx * 0.68f + nx * neck, a.y + vy * 0.68f + ny * neck,
                b.x + nx * outer, b.y + ny * outer)
            path.lineTo(b.x - nx * outer, b.y - ny * outer)
            path.cubicTo(a.x + vx * 0.68f - nx * neck, a.y + vy * 0.68f - ny * neck,
                a.x + vx * 0.32f - nx * neck, a.y + vy * 0.32f - ny * neck,
                a.x - nx * outer, a.y - ny * outer)
            path.close()
        }
        circle(origin, (24f + 10f * open.coerceIn(0f, 1f)) * unit)
        circle(e, 34f * unit * edit.coerceIn(0f, 1f))
        circle(s, 34f * unit * settings.coerceIn(0f, 1f))
        circle(f, 34f * unit * feedback.coerceIn(0f, 1f))
        link(origin, e, edit)
        link(e, s, minOf(edit, settings))
        link(s, f, minOf(settings, feedback))
        return Outline.Generic(path)
    }
}
