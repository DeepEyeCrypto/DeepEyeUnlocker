package com.deepeye.otg.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun UsbTypeCIcon(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    tint: Color = Color(0xFF6750A4),
    animated: Boolean = false
) {
    val scaleAnim = if (animated) {
        val infiniteTransition = rememberInfiniteTransition(label = "UsbIconAnim")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ), label = "UsbIconScale"
        )
        scale
    } else {
        1f
    }

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
    ) {
        // CRASHFIX: Zero size guard before any Canvas drawing
        if (this.size.width < 1f || this.size.height < 1f) return@Canvas

        val strokeWidth = 2.dp.toPx()
        val portWidth = this.size.width * 0.8f
        val portHeight = this.size.height * 0.45f
        val xOffset = (this.size.width - portWidth) / 2f
        val yOffset = (this.size.height - portHeight) / 2f

        // Draw outer shell (Type-C oval)
        drawRoundRect(
            color = tint,
            topLeft = Offset(xOffset, yOffset),
            size = Size(portWidth, portHeight),
            cornerRadius = CornerRadius(portHeight / 2f, portHeight / 2f),
            style = Stroke(width = strokeWidth)
        )

        // Draw inner pins (2 rows of 4)
        val pinWidth = portWidth * 0.08f
        val pinHeight = portHeight * 0.15f
        val startX = xOffset + portWidth * 0.25f
        val gapX = (portWidth * 0.5f) / 3f

        for (i in 0..3) {
            val px = startX + (i * gapX) - (pinWidth / 2f)
            // Top row
            drawRect(
                color = tint,
                topLeft = Offset(px, yOffset + portHeight * 0.25f),
                size = Size(pinWidth, pinHeight)
            )
            // Bottom row
            drawRect(
                color = tint,
                topLeft = Offset(px, yOffset + portHeight * 0.60f),
                size = Size(pinWidth, pinHeight)
            )
        }
    }
}
