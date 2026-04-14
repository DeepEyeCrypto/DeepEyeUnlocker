package com.deepeye.otg.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.deepeye.otg.ui.theme.DeepEyeColors

@Composable
fun RadarAnimation(
    modifier: Modifier = Modifier,
    accentColor: Color = DeepEyeColors.PrimaryCyan,
    active: Boolean = true,
    centerIcon: ImageVector = Icons.Default.Usb,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val sweepRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "radarSweepRotation",
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "radarPulse",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 2.dp.toPx()
            val minSize = size.minDimension
            val baseRadius = minSize * 0.16f

            repeat(4) { index ->
                drawCircle(
                    color = accentColor.copy(alpha = 0.12f + index * 0.05f),
                    radius = baseRadius * (index + 1) * pulse,
                    style = Stroke(width = if (index == 0) stroke else stroke * 0.6f),
                )
            }

            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        accentColor.copy(alpha = 0.10f),
                        accentColor.copy(alpha = 0.80f),
                        Color.Transparent,
                    ),
                ),
                startAngle = sweepRotation,
                sweepAngle = 64f,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
            )

            if (active) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.22f),
                            Color.Transparent,
                        ),
                    ),
                    radius = minSize * 0.22f,
                )
            }
        }

        Icon(
            imageVector = centerIcon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(38.dp),
        )
    }
}
