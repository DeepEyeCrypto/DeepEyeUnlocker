package com.deepeye.otg.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.deepeye.otg.ui.theme.GlassTokens
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

@Composable
fun GlassCard(
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    performanceMode: Boolean = false,
    accentColor: Color = Color.Transparent, // Added for mode accents
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "cardScale"
    )

    // ── CRITICAL: backgroundColor MUST be specified ──────────
    val hazeStyle = remember {
        HazeStyle(
            backgroundColor = Color.White,   // ← fixes crash
            tint = HazeTint(Color.White.copy(alpha = 0.52f)),
            blurRadius = 24.dp,
            noiseFactor = 0.0f
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 4.dp,
                shape = shape,
                ambientColor = Color(0xFF6750A4).copy(alpha = 0.12f),
                spotColor = Color(0xFF6750A4).copy(alpha = 0.08f)
            )
            .clip(shape)
            .then(
                if (hazeState != null && !performanceMode) {
                    Modifier.hazeEffect(state = hazeState, style = hazeStyle)
                } else {
                    Modifier.background(GlassTokens.glassBrush)
                }
            )
            .border(
                width = 1.dp,
                color = if (accentColor != Color.Transparent) accentColor.copy(alpha = 0.4f) else GlassTokens.cardBorderColor,
                shape = shape
            )
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ) else Modifier
            )
    ) {
        // Accent glow at top edge
        if (accentColor != Color.Transparent) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .matchParentSize()
            ) {
                // Subtle top edge glow
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(accentColor.copy(alpha = 0.3f), Color.Transparent),
                        startY = 0f,
                        endY = 20f
                    ),
                    size = androidx.compose.ui.geometry.Size(size.width, 20f)
                )
            }
        }
        
        content()
    }
}
