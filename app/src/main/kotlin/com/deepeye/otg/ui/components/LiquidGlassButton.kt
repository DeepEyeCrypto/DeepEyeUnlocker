package com.deepeye.otg.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "liquid-glass")
    val sweepAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
        ),
        label = "liquid-sweep",
    )
    val glowOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "liquid-glow-offset",
    )
    val borderAlpha by transition.animateFloat(
        initialValue = 0.34f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "liquid-alpha",
    )
    val shape = RoundedCornerShape(18.dp)

    val baseBrush = Brush.linearGradient(
        colors = listOf(
            Color(0x331E40AF),
            Color(0x554C1D95),
            Color(0x3322D3EE),
        ),
    )

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.55f)
            .clip(shape)
            .background(baseBrush)
            .drawWithCache {
                val strokeWidth = 1.dp.toPx()
                val inset = strokeWidth / 2f
                val cornerRadiusPx = 18.dp.toPx()
                val sweepCenter = Offset(size.width / 2f, size.height / 2f)
                val borderBrush = Brush.sweepGradient(
                    colors = listOf(
                        Color.White.copy(alpha = borderAlpha * 0.2f),
                        Color(0xFF7C3AED).copy(alpha = borderAlpha),
                        Color(0xFF22D3EE).copy(alpha = borderAlpha * 0.78f),
                        Color.White.copy(alpha = borderAlpha * 0.2f),
                    ),
                    center = sweepCenter,
                )
                val glowBrush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color(0xFF22D3EE).copy(alpha = 0.08f),
                        Color.Transparent,
                    ),
                    center = Offset(
                        x = size.width * (0.24f + (0.52f * glowOffset)),
                        y = size.height * 0.18f,
                    ),
                    radius = max(size.width, size.height) * 0.95f,
                )

                onDrawWithContent {
                    drawRoundRect(
                        brush = glowBrush,
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                    )

                    drawContent()

                    rotate(degrees = sweepAngle, pivot = sweepCenter) {
                        drawRoundRect(
                            brush = borderBrush,
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - strokeWidth, size.height - strokeWidth),
                            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                            style = Stroke(
                                width = strokeWidth,
                                pathEffect = PathEffect.cornerPathEffect(cornerRadiusPx),
                            ),
                        )
                    }
                }
            }
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
