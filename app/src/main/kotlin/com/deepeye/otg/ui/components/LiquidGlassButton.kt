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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "liquid-glass")
    val sweepOffset = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "liquid-sweep",
    )
    val borderAlpha = transition.animateFloat(
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
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = borderAlpha.value * 0.45f),
            Color(0xFF7C3AED).copy(alpha = borderAlpha.value),
            Color(0xFF22D3EE).copy(alpha = borderAlpha.value * 0.75f),
        ),
        start = Offset.Zero,
        end = Offset(320f * sweepOffset.value.coerceAtLeast(0.2f), 120f),
    )
    val glowBrush = Brush.radialGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.18f),
            Color.Transparent,
        ),
        center = Offset(120f + (100f * sweepOffset.value), 18f),
        radius = 180f,
    )

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.55f)
            .clip(shape)
            .background(baseBrush)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(glowBrush),
        )

        Row(
            modifier = Modifier.padding(contentPadding),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
