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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lxzrvi.crescendo.R
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.components.PrismalGlassButton
import com.styropyr0.prismal.drawPlainPrismalGlass
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import com.styropyr0.prismal.sources.rememberPrismalCanvasSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin

/** Pinned avatar and staggered 2x2 liquid menu, sampling the actual page behind it. */
@Composable
internal fun ProfileMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    pageBackdrop: PrismalBackdrop,
    liveBlur: Boolean,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { context.getSharedPreferences("profile", android.content.Context.MODE_PRIVATE) }
    val store = remember(context) { BackgroundPhotoStore(context, "crescendo_profile_photo") }
    var photoVersion by remember(prefs) { mutableLongStateOf(prefs.getLong("photo_version", 0L)) }
    var editing by remember { mutableStateOf(false) }
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
    val surface = if (dark) Color(0xFF242427).copy(alpha = 0.52f) else Color.White.copy(alpha = 0.56f)

    val transition = updateTransition(expanded, label = "profileMenu")
    val scrim by transition.animateFloat(
        transitionSpec = { tween(if (targetState) 260 else 200) }, label = "profileScrim",
    ) { if (it) 1f else 0f }
    val edit by transition.animateFloat(
        transitionSpec = { tween(if (targetState) 380 else 210, delayMillis = if (targetState) 15 else 30, easing = FastOutSlowInEasing) },
        label = "profileEdit",
    ) { if (it) 1f else 0f }
    val feedback by transition.animateFloat(
        transitionSpec = { tween(if (targetState) 400 else 210, delayMillis = if (targetState) 70 else 15, easing = FastOutSlowInEasing) },
        label = "profileFeedback",
    ) { if (it) 1f else 0f }
    val settings by transition.animateFloat(
        transitionSpec = { tween(if (targetState) 420 else 210, delayMillis = if (targetState) 125 else 0, easing = FastOutSlowInEasing) },
        label = "profileSettings",
    ) { if (it) 1f else 0f }
    val visible = expanded || scrim > 0.001f || edit > 0.001f || settings > 0.001f || feedback > 0.001f
    BackHandler(enabled = expanded) { onExpandedChange(false) }

    Box(Modifier.fillMaxSize()) {
        if (visible) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.54f * scrim))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null,
                        enabled = expanded, role = Role.Button) { onExpandedChange(false) }
                    .semantics { contentDescription = "Close profile menu" },
            )
        }
        Box(
            Modifier.align(Alignment.TopEnd).statusBarsPadding()
                .padding(end = ContentSidePadding, top = 8.dp).size(150.dp),
        ) {
            if (visible) {
                // The changing necks are also real Prismal-sampled glass, not a solid imitation.
                Box(Modifier.fillMaxSize().drawPlainPrismalGlass(
                    backdrop = backdrop,
                    shape = { ProfileLinks(edit, settings, feedback) },
                    effects = { applyPrismalGlassEffects(density = density, adaptiveLuminance = false, luminance = 0.5f,
                        blurRadiusPx = with(density) { 6.dp.toPx() }, refractionHeightPx = 0f, refractionAmountPx = 0f) },
                    onDrawSurface = { drawRect(surface) },
                ))
                ProfileAction("Edit", R.drawable.ic_edit_profile, edit, -82f, 0f, expanded, backdrop, surface) {
                    onExpandedChange(false); editing = true
                }
                ProfileAction("Settings", R.drawable.ic_nav_settings, settings, -82f, 82f, expanded, backdrop, surface) {
                    onExpandedChange(false); onSettings()
                }
                ProfileAction("Feedback", R.drawable.ic_feedback, feedback, 0f, 82f, expanded, backdrop, surface) {
                    onExpandedChange(false)
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/lxzrvi/Crescendo/issues/new")))
                    } catch (_: Exception) {
                        error = "No browser is available. Visit github.com/lxzrvi/Crescendo/issues to share feedback."
                    }
                }
            }
            PrismalGlassButton(
                onClick = { onExpandedChange(!expanded) }, backdrop = backdrop,
                modifier = Modifier.align(Alignment.TopEnd).size(48.dp).semantics {
                    contentDescription = "Profile"
                    stateDescription = if (expanded) "Menu open" else "Menu closed"
                },
                height = 48.dp, contentPadding = PaddingValues(0.dp), surfaceColor = surface,
                blurRadius = 9.dp, refractionHeight = 12.dp, refractionAmount = 22.dp,
            ) {
                if (photo != null) {
                    Image(bitmap = photo!!.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(40.dp).clip(CircleShape))
                } else {
                    Icon(painterResource(R.drawable.ic_profile), contentDescription = null,
                        tint = colors.onBackground, modifier = Modifier.size(24.dp))
                }
            }
        }
    }

    if (editing) AlertDialog(
        onDismissRequest = { editing = false },
        title = { Text("Edit profile") },
        text = { Text("Alex\nChoose a profile photo. It stays private on this device.") },
        confirmButton = { TextButton(onClick = {
            editing = false
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }) { Text(if (photoVersion > 0L) "Change photo" else "Choose photo") } },
        dismissButton = {
            androidx.compose.foundation.layout.Row {
                if (photoVersion > 0L) TextButton(onClick = {
                    store.remove(); photoVersion = 0L
                    prefs.edit().remove("photo_version").apply(); editing = false
                }) { Text("Remove") }
                TextButton(onClick = { editing = false }) { Text("Cancel") }
            }
        },
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
    enabled: Boolean,
    backdrop: PrismalBackdrop,
    surface: Color,
    onClick: () -> Unit,
) {
    if (progress <= 0.001f) return
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val travelX = with(density) { targetX.dp.toPx() }
    val travelY = with(density) { targetY.dp.toPx() }
    PrismalGlassButton(
        onClick = { if (enabled && progress > 0.45f) onClick() }, backdrop = backdrop,
        modifier = Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-10).dp).size(68.dp)
            .graphicsLayer {
                translationX = travelX * progress
                translationY = travelY * progress
                val s = 0.58f + 0.42f * progress
                scaleX = s; scaleY = s
                alpha = progress.coerceIn(0f, 1f)
            }.semantics { contentDescription = label },
        height = 68.dp, contentPadding = PaddingValues(0.dp), surfaceColor = surface,
        blurRadius = 9.dp, refractionHeight = 13.dp, refractionAmount = 24.dp,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painterResource(icon), contentDescription = null, tint = colors.onBackground, modifier = Modifier.size(22.dp))
            Text(label, color = colors.onBackground, fontSize = 10.sp, lineHeight = 12.sp,
                fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

/** Concave necks stretch out from the avatar, then disappear as droplets separate. */
private data class ProfileLinks(val edit: Float, val settings: Float, val feedback: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val unit = density.density
        val origin = Offset(size.width - 24f * unit, 24f * unit)
        val path = Path()
        fun addLink(dx: Float, dy: Float, progress: Float) {
            val p = progress.coerceIn(0f, 1f)
            val strength = sin(p * PI.toFloat()).coerceAtLeast(0f)
            if (strength < 0.01f) return
            val vx = dx * unit * p
            val vy = dy * unit * p
            val distance = hypot(vx, vy)
            if (distance < 1f) return
            val ux = vx / distance; val uy = vy / distance
            val px = -uy; val py = ux
            val end = Offset(origin.x + vx, origin.y + vy)
            val outer = 19f * unit * strength
            val neck = (3f + 7f * strength) * unit * strength
            path.moveTo(origin.x + px * outer, origin.y + py * outer)
            path.cubicTo(origin.x + vx * 0.33f + px * neck, origin.y + vy * 0.33f + py * neck,
                origin.x + vx * 0.67f + px * neck, origin.y + vy * 0.67f + py * neck,
                end.x + px * outer, end.y + py * outer)
            path.lineTo(end.x - px * outer, end.y - py * outer)
            path.cubicTo(origin.x + vx * 0.67f - px * neck, origin.y + vy * 0.67f - py * neck,
                origin.x + vx * 0.33f - px * neck, origin.y + vy * 0.33f - py * neck,
                origin.x - px * outer, origin.y - py * outer)
            path.close()
        }
        addLink(-82f, 0f, edit)
        addLink(-82f, 82f, settings)
        addLink(0f, 82f, feedback)
        return Outline.Generic(path)
    }
}
