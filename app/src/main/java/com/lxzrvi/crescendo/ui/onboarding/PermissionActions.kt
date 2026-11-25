package com.lxzrvi.crescendo.ui.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lxzrvi.crescendo.ui.components.BellIcon
import com.lxzrvi.crescendo.ui.components.MusicNoteIcon
import com.lxzrvi.crescendo.ui.components.WaveformIcon

internal enum class PermissionIconKind { MUSIC, NOTIFICATIONS, BACKGROUND }

/** Keeps the reference icon visible, with a small check only when the system reports a grant. */
@Composable
internal fun PermissionStatusIcon(
    kind: PermissionIconKind,
    granted: Boolean,
    tint: Color,
    badgeColor: Color,
    checkColor: Color,
    modifier: Modifier = Modifier,
    badgeSize: Dp = 10.dp,
) {
    Box(modifier) {
        when (kind) {
            PermissionIconKind.MUSIC -> MusicNoteIcon(Modifier.fillMaxSize(), tint)
            PermissionIconKind.NOTIFICATIONS -> BellIcon(Modifier.fillMaxSize(), tint)
            PermissionIconKind.BACKGROUND -> WaveformIcon(Modifier.fillMaxSize(), tint)
        }
        if (granted) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(badgeSize)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize().padding(badgeSize * 0.17f)) {
                    val tick = Path().apply {
                        moveTo(size.width * 0.14f, size.height * 0.52f)
                        lineTo(size.width * 0.39f, size.height * 0.76f)
                        lineTo(size.width * 0.87f, size.height * 0.23f)
                    }
                    drawPath(
                        path = tick,
                        color = checkColor,
                        style = Stroke(
                            width = size.minDimension * 0.20f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }
        }
    }
}

/** Both pages have the same bottom anchor, action height, and corner radius. */
@Composable
internal fun PermissionActionRow(
    page: PermissionPage,
    status: PermissionStatus,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val music = page.kind == PermissionKind.MUSIC
    val granted = if (music) status.musicGranted else status.notificationsGranted
    val kind = if (music) PermissionIconKind.MUSIC else PermissionIconKind.NOTIFICATIONS
    val label = if (granted) "Continue" else page.actionLabel

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onPrimary,
            modifier = Modifier
                .weight(1.35f)
                .height(52.dp)
                .semantics { stateDescription = if (granted) "Granted" else "Not granted" },
            shape = RoundedCornerShape(ONBOARDING_ACTION_RADIUS_DP.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
            ),
        ) {
            PermissionStatusIcon(
                kind = kind,
                granted = granted,
                tint = colors.onPrimary,
                badgeColor = colors.onPrimary,
                checkColor = colors.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                modifier = Modifier.weight(1f, fill = false),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                textAlign = TextAlign.Center,
            )
        }

        BorderlessAction(
            label = if (music) "Background Playback" else "Skip",
            icon = if (music) PermissionIconKind.BACKGROUND else null,
            granted = if (music) status.backgroundGranted else null,
            onClick = onSecondary,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Bounded tap ripple plus a soft light that follows a finger without consuming the click. */
@Composable
private fun BorderlessAction(
    label: String,
    icon: PermissionIconKind?,
    granted: Boolean?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(ONBOARDING_ACTION_RADIUS_DP.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val touchPoint = remember { mutableStateOf(Offset.Zero) }
    var touching by remember { mutableStateOf(false) }
    val touchAlpha = animateFloatAsState(
        targetValue = if (touching) 1f else 0f,
        animationSpec = tween(if (touching) 120 else 350),
        label = "touchGlow",
    )
    val indicatorColor = colors.primary

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(colors.surfaceVariant)
            .drawBehind {
                val strength = touchAlpha.value
                if (strength > 0f) {
                    val radius = size.width * 0.65f
                    val center = touchPoint.value
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                indicatorColor.copy(alpha = 0.25f * strength),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                        center = center,
                    )
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    touchPoint.value = down.position
                    touching = true
                    try {
                        while (true) {
                            val change = awaitPointerEvent(PointerEventPass.Initial).changes
                                .firstOrNull { it.id == down.id } ?: break
                            touchPoint.value = change.position
                            if (!change.pressed) break
                        }
                    } finally {
                        touching = false
                    }
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = indicatorColor),
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                if (granted != null) stateDescription = if (granted) "Granted" else "Not granted"
            }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                PermissionStatusIcon(
                    kind = icon,
                    granted = granted == true,
                    tint = colors.onBackground,
                    badgeColor = colors.primary,
                    checkColor = colors.onPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text = label,
                modifier = Modifier.weight(1f, fill = false),
                color = colors.onBackground,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                textAlign = TextAlign.Center,
            )
        }
    }
}
