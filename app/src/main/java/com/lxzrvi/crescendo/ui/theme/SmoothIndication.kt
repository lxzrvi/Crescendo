package com.lxzrvi.crescendo.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** A smooth, noise-free touch glow; unlike the platform ripple it has no sparkly dots. */
internal data class SmoothIndication(private val color: Color) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = SmoothPressNode(interactionSource, color)
}

private class SmoothPressNode(
    private val interactions: InteractionSource,
    private val color: Color,
) : Modifier.Node(), DrawModifierNode {
    private val opacity = Animatable(0f)
    private var origin = Offset.Zero
    private var animation: Job? = null

    override fun onAttach() {
        coroutineScope.launch {
            interactions.interactions.collect { event ->
                when (event) {
                    is PressInteraction.Press -> {
                        origin = event.pressPosition
                        invalidateDraw()
                        animation?.cancel()
                        animation = launch { opacity.animateTo(1f, tween(140, easing = FastOutSlowInEasing)) }
                    }
                    is PressInteraction.Release, is PressInteraction.Cancel -> {
                        animation?.cancel()
                        animation = launch { opacity.animateTo(0f, tween(340, easing = FastOutSlowInEasing)) }
                    }
                    else -> Unit
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val strength = opacity.value
        if (strength > 0f) {
            val radius = size.maxDimension.coerceAtLeast(1f) * 0.9f
            drawRect(
                Brush.radialGradient(
                    0f to color.copy(alpha = 0.16f * strength),
                    0.58f to color.copy(alpha = 0.07f * strength),
                    1f to Color.Transparent,
                    center = origin,
                    radius = radius,
                ),
            )
        }
    }
}
