package com.lxzrvi.crescendo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.cos
import kotlin.math.sin

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
    Canvas(modifier) {
        val unit = size.minDimension / 900f
        val iconPath = PathParser().parsePathString(ICON_PATH).toPath()
        translate(size.width / 2f, size.height / 2f) {
            scale(unit, unit, pivot = Offset.Zero) {
                drawPath(path = iconPath, color = tint)
            }
        }
    }
}

/** Beamed music note — rounded bold iOS-style vector icon. */
@Composable
fun MusicNoteIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        // note head
        drawOval(
            color = tint,
            topLeft = Offset(w * 0.10f, h * 0.58f),
            size = Size(w * 0.36f, h * 0.28f),
        )
        // stem
        drawLine(
            color = tint,
            start = Offset(w * 0.46f, h * 0.70f),
            end = Offset(w * 0.46f, h * 0.12f),
            strokeWidth = w * 0.13f,
            cap = StrokeCap.Round,
        )
        // flag
        val flag = Path().apply {
            moveTo(w * 0.46f, h * 0.12f)
            cubicTo(
                w * 0.90f, h * 0.24f,
                w * 0.86f, h * 0.58f,
                w * 0.54f, h * 0.64f,
            )
        }
        drawPath(
            path = flag,
            color = tint,
            style = Stroke(width = w * 0.13f, cap = StrokeCap.Round),
        )
    }
}

/** Padlock — privacy section ke liye (rounded, bold). */
@Composable
fun LockIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        // body
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.16f, h * 0.42f),
            size = Size(w * 0.68f, h * 0.52f),
            cornerRadius = CornerRadius(w * 0.16f),
        )
        // shackle
        val shackle = Path().apply {
            moveTo(w * 0.32f, h * 0.44f)
            lineTo(w * 0.32f, h * 0.34f)
            arcTo(
                rect = Rect(
                    left = w * 0.30f,
                    top = h * 0.34f - h * 0.18f,
                    right = w * 0.70f,
                    bottom = h * 0.34f + h * 0.18f,
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false,
            )
            lineTo(w * 0.68f, h * 0.44f)
        }
        drawPath(
            path = shackle,
            color = tint,
            style = Stroke(width = w * 0.13f, cap = StrokeCap.Round),
        )
    }
}

/** Chunky 5-point star — GitHub support ke liye. */
@Composable
fun StarIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val outer = size.minDimension * 0.46f
        val inner = outer * 0.38f
        val star = Path()
        for (i in 0 until 10) {
            val angle = -Math.PI / 2 + i * Math.PI / 5
            val r = if (i % 2 == 0) outer else inner
            val x = cx + r * cos(angle).toFloat()
            val y = cy + r * sin(angle).toFloat()
            if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
        }
        star.close()
        drawPath(path = star, color = tint)
    }
}

/** Right arrow — let's get started button ke liye (rounded, bold). */
@Composable
fun ArrowRightIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.14f
        drawLine(
            color = tint,
            start = Offset(w * 0.10f, h * 0.5f),
            end = Offset(w * 0.76f, h * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.58f, h * 0.28f),
            end = Offset(w * 0.84f, h * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.58f, h * 0.72f),
            end = Offset(w * 0.84f, h * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
