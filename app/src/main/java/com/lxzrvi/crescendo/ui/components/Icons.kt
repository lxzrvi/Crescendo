package com.lxzrvi.crescendo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.painterResource
import com.lxzrvi.crescendo.R

// Crescendo_main_icon.svg ka path (viewBox -450 -450 900 900)
private const val ICON_PATH =
    "M0 -435 A135 135 0 0 1 0 -165 A165 165 0 1 0 165 0 " +
        "A135 135 0 1 1 300 135 A165 165 0 0 0 135 300 " +
        "A135 135 0 0 1 -135 300 A165 165 0 0 0 -300 135 " +
        "A135 135 0 0 1 -300 -135 A165 165 0 0 0 -135 -300 " +
        "A135 135 0 0 1 0 -435 Z"

/** Crescendo logo mark — off-white icon (welcome header ke paas). */
@Composable
fun CrescendoMark(modifier: Modifier = Modifier, tint: Color = Color.White) {
    val iconPath = remember { PathParser().parsePathString(ICON_PATH).toPath() }
    Canvas(modifier) {
        val unit = size.minDimension / 900f
        translate(size.width / 2f, size.height / 2f) {
            scale(unit, unit, pivot = Offset.Zero) {
                drawPath(path = iconPath, color = tint)
            }
        }
    }
}

/** Each drawable is a scalable vector traced from the supplied icon reference. */
@Composable
private fun ReferenceIcon(resource: Int, modifier: Modifier, tint: Color) {
    Icon(
        painter = painterResource(resource),
        contentDescription = null, // Decorative: each icon sits beside its text label.
        modifier = modifier,
        tint = tint,
    )
}

@Composable
fun MusicNoteIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    ReferenceIcon(R.drawable.ic_onboarding_music, modifier, tint)
}

@Composable
fun BellIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    ReferenceIcon(R.drawable.ic_onboarding_bell, modifier, tint)
}

@Composable
fun LockIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    ReferenceIcon(R.drawable.ic_onboarding_lock, modifier, tint)
}

@Composable
fun GithubIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    ReferenceIcon(R.drawable.ic_onboarding_github, modifier, tint)
}

@Composable
fun WaveformIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    ReferenceIcon(R.drawable.ic_onboarding_waveform, modifier, tint)
}

@Composable
fun StarIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    ReferenceIcon(R.drawable.ic_onboarding_star, modifier, tint)
}

@Composable
fun ArrowRightIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    ReferenceIcon(R.drawable.ic_onboarding_chevron, modifier, tint)
}
