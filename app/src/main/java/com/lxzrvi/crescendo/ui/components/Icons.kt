package com.lxzrvi.crescendo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** Beamed music note — drawn vector, no emoji. */
@Composable
fun MusicNoteIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        // note head
        drawOval(
            color = tint,
            topLeft = Offset(w * 0.14f, h * 0.60f),
            size = Size(w * 0.32f, h * 0.26f),
        )
        // stem
        drawLine(
            color = tint,
            start = Offset(w * 0.46f, h * 0.70f),
            end = Offset(w * 0.46f, h * 0.14f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round,
        )
        // flag
        val flag = Path().apply {
            moveTo(w * 0.46f, h * 0.14f)
            cubicTo(
                w * 0.88f, h * 0.26f,
                w * 0.84f, h * 0.58f,
                w * 0.56f, h * 0.64f,
            )
        }
        drawPath(
            path = flag,
            color = tint,
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Round),
        )
    }
}

/** Padlock — privacy section ke liye. */
@Composable
fun LockIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        // body
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.18f, h * 0.42f),
            size = Size(w * 0.64f, h * 0.50f),
            cornerRadius = CornerRadius(w * 0.10f),
        )
        // shackle
        val shackle = Path().apply {
            moveTo(w * 0.32f, h * 0.44f)
            lineTo(w * 0.32f, h * 0.34f)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    center = Offset(w * 0.5f, h * 0.34f),
                    radius = androidx.compose.ui.geometry.Radius(w * 0.18f, h * 0.18f),
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
            style = Stroke(width = w * 0.10f, cap = StrokeCap.Round),
        )
    }
}

/** 5-point star — GitHub support ke liye. */
@Composable
fun StarIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val outer = size.minDimension * 0.46f
        val inner = outer * 0.42f
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

/** Right arrow — let's get started button ke liye. */
@Composable
fun ArrowRightIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.10f
        drawLine(
            color = tint,
            start = Offset(w * 0.12f, h * 0.5f),
            end = Offset(w * 0.78f, h * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.62f, h * 0.30f),
            end = Offset(w * 0.84f, h * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.62f, h * 0.70f),
            end = Offset(w * 0.84f, h * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
