package com.lxzrvi.crescendo.ui.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.lxzrvi.crescendo.ui.theme.AmoledBlack
import com.lxzrvi.crescendo.ui.theme.Blush
import com.lxzrvi.crescendo.ui.theme.NeonRed
import com.lxzrvi.crescendo.ui.theme.RedDeep
import com.lxzrvi.crescendo.ui.theme.RedSoft

/** Ek soft "blurred" blob: andar solid, bahar smoothly transparent — yahi blur ka kaam karta hai. */
private fun DrawScope.blob(color: Color, center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            0.00f to color,
            0.45f to color.copy(alpha = 0.92f),
            0.75f to color.copy(alpha = 0.35f),
            1.00f to Color.Transparent,
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

/**
 * Liquid-glass background: red + white shapes drawn on canvas,
 * heavily soft-blurred and blended into the AMOLED black screen.
 * Slow drift taaki background "zinda" lage, plus neeche black fade.
 */
@Composable
fun GradientBlobBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "blobs")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 9000, easing = LinearEasing),
            RepeatMode.Reverse,
        ),
        label = "drift",
    )

    Canvas(modifier.background(AmoledBlack)) {
        val w = size.width
        val h = size.height
        val d = (drift - 0.5f) * w * 0.08f

        // Shapes peeche se aage — bade radius, soft blurred look
        blob(NeonRed, Offset(w * 0.30f - d, h * 0.30f), w * 0.95f)
        blob(RedDeep, Offset(w * 0.88f + d, h * 0.40f), w * 0.70f)
        blob(RedSoft, Offset(w * 0.72f + d, h * 0.22f), w * 0.80f)
        blob(Blush, Offset(w * 0.05f - d, h * 0.02f), w * 0.65f)
        blob(NeonRed.copy(alpha = 0.80f), Offset(w * 0.85f, h * 0.00f), w * 0.50f)

        // White accent mix, halka
        blob(Color.White.copy(alpha = 0.50f), Offset(w * 0.50f + d * 1.4f, h * 0.10f), w * 0.35f)

        // Neeche black fade — content pe smooth landing
        drawRect(
            brush = Brush.verticalGradient(
                0.00f to Color.Transparent,
                0.42f to Color.Transparent,
                0.56f to AmoledBlack.copy(alpha = 0.85f),
                0.66f to AmoledBlack,
                1.00f to AmoledBlack
            )
        )
    }
}
