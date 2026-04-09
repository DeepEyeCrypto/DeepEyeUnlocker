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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.ui.theme.DeepEyeType

@Composable
fun GlassButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = "BtnPressAnim"
    )

    val opacity = if (enabled) 1f else 0.5f

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
                alpha = opacity
            }
            .shadow(if (accent) 8.dp else 2.dp, RoundedCornerShape(12.dp), spotColor = if (accent) com.deepeye.otg.ui.theme.DeepEyeColors.NEON_PURPLE.copy(alpha = 0.3f) else Color.Black)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (accent) Modifier.background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(com.deepeye.otg.ui.theme.DeepEyeColors.NEON_PURPLE, com.deepeye.otg.ui.theme.DeepEyeColors.NEON_BLUE)
                    )
                )
                else Modifier
                    .background(com.deepeye.otg.ui.theme.DeepEyeColors.BG_SURFACE.copy(0.6f))
                    .border(1.dp, com.deepeye.otg.ui.theme.DeepEyeColors.WHITE_LOW.copy(0.3f), RoundedCornerShape(12.dp))
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = if (accent) Color.White else com.deepeye.otg.ui.theme.DeepEyeColors.WHITE_HIGH,
            style = com.deepeye.otg.ui.theme.DeepEyeType.CAPTION.copy(fontSize = 11.sp),
            letterSpacing = 1.sp
        )
    }
}
