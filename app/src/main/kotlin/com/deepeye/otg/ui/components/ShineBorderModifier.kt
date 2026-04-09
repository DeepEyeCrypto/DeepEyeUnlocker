package com.deepeye.otg.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.shimmerBorder(
    colors: List<Color> = listOf(
        Color(0xFFFF007F),
        Color(0xFF39FF14),
        Color(0xFF00FFFF),
    ),
    borderWidth: Dp = 1.dp,
    cornerRadius: Dp = 18.dp,
): Modifier = composed {
    val safeColors = when {
        colors.isEmpty() -> listOf(Color.White, Color.White)
        colors.size == 1 -> listOf(colors.first(), colors.first())
        else -> colors
    }

    val angle = rememberInfiniteTransition(label = "shine-border").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
        ),
        label = "shine-angle",
    )

    drawWithContent {
        drawContent()

        val strokeWidth = borderWidth.toPx()
        val inset = strokeWidth / 2f
        rotate(degrees = angle.value) {
            drawRoundRect(
                brush = Brush.sweepGradient(
                    colors = safeColors + safeColors.first(),
                    center = Offset(size.width / 2f, size.height / 2f),
                ),
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(
                    width = size.width - strokeWidth,
                    height = size.height - strokeWidth,
                ),
                cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                style = Stroke(width = strokeWidth),
            )
        }
    }
}
