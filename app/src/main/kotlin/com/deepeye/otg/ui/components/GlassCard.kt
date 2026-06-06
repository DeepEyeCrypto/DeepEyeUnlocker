package com.deepeye.otg.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.deepeye.otg.ui.theme.DeepEyeColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

@Composable
fun GlassCard(
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    performanceMode: Boolean = false,
    accentColor: Color = Color.Transparent,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
        label = "glassCardScale",
    )
    val highlighted = accentColor != Color.Transparent
    val borderColor by animateColorAsState(
        targetValue = if (highlighted) accentColor.copy(alpha = 0.42f) else DeepEyeColors.GlassBorder,
        label = "glassCardBorder",
    )
    val hazeStyle = remember(accentColor) {
        HazeStyle(
            backgroundColor = DeepEyeColors.Surface,
            tint = HazeTint(
                if (highlighted) accentColor.copy(alpha = 0.06f) else DeepEyeColors.GlassWhite,
            ),
            blurRadius = 0.dp,  // DISABLED: Android blur causes artifacts and performance issues
            noiseFactor = 0.0f,  // DISABLED: No noise for clean look
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                compositingStrategy = CompositingStrategy.Offscreen
                // DISABLED: RenderEffect blur - causes triple blur issue
                // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !performanceMode) {
                //     renderEffect = RenderEffect
                //         .createBlurEffect(10f, 10f, Shader.TileMode.DECAL)
                //         .asComposeRenderEffect()
                // }
            }
            .shadow(
                elevation = 24.dp,
                shape = shape,
                ambientColor = DeepEyeColors.Shadow,
                spotColor = if (highlighted) accentColor.copy(alpha = 0.24f) else DeepEyeColors.Shadow,
            )
            .clip(shape)
            .then(
                if (hazeState != null && !performanceMode) {
                    Modifier.hazeChild(state = hazeState, style = hazeStyle)
                } else {
                    Modifier.background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                DeepEyeColors.Surface.copy(alpha = 0.06f),  // Reduced: 0.92f → 0.06f
                                DeepEyeColors.Surface2.copy(alpha = 0.04f), // Reduced: 0.84f → 0.04f
                            ),
                        ),
                    )
                }
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DeepEyeColors.GlassHighlight.copy(alpha = 0.5f), // Reduced opacity
                        DeepEyeColors.GlassWhite.copy(alpha = 0.03f),    // Reduced: 0.55f → 0.03f
                        Color.Transparent,
                    ),
                ),
            )
            .border(1.dp, borderColor, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(
                            color = if (highlighted) accentColor else DeepEyeColors.PrimaryCyan,
                        ),
                        onClick = onClick,
                    )
                } else {
                    Modifier
                }
            ),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DeepEyeColors.GlassHighlight,
                        Color.Transparent,
                    ),
                    endY = size.height * 0.35f,
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx()),
            )

            if (highlighted) {
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.18f),
                            Color.Transparent,
                        ),
                        radius = size.maxDimension * 0.85f,
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx()),
                )
            }
        }

        content()
    }
}
