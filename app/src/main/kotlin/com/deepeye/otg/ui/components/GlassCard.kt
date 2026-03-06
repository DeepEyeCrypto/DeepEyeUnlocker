package com.deepeye.otg.ui.components

import androidx.compose.animation.core.Spring
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.deepeye.otg.ui.theme.GlassTokens
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

@Composable
fun GlassCard(
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 8.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = "CardPressAnim"
    )

    var rootModifier = modifier.graphicsLayer {
        scaleX = scaleAnim
        scaleY = scaleAnim
    }

    if (onClick != null) {
        rootModifier = rootModifier.clickable(
            interactionSource = interactionSource,
            indication = null, // Disable default ripple for zero-latency custom feel
            onClick = onClick
        )
    }

    Box(
        modifier = rootModifier
            .shadow(elevation, shape, spotColor = Color(0x146750A4))
            .clip(shape)
            .then(
                if (hazeState != null) Modifier.hazeChild(state = hazeState)
                else Modifier.background(GlassTokens.glassBrush)
            )
            .border(1.dp, GlassTokens.cardBorderColor, shape)
    ) {
        content()
    }
}
