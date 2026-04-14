package com.deepeye.otg.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepeye.otg.ui.theme.DeepEyeColors

enum class NeonButtonStyle {
    PRIMARY,
    SECONDARY,
    DANGER,
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: NeonButtonStyle = NeonButtonStyle.PRIMARY,
    loading: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = Icons.Default.ArrowForward,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !loading) 0.96f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "neonButtonScale",
    )

    val containerColor = when (style) {
        NeonButtonStyle.PRIMARY -> DeepEyeColors.PrimaryCyan
        NeonButtonStyle.SECONDARY -> Color.Transparent
        NeonButtonStyle.DANGER -> DeepEyeColors.Error
    }
    val contentColor = when (style) {
        NeonButtonStyle.PRIMARY -> Color.Black
        NeonButtonStyle.SECONDARY -> DeepEyeColors.PrimaryCyan
        NeonButtonStyle.DANGER -> DeepEyeColors.TextPrimary
    }
    val border = when (style) {
        NeonButtonStyle.SECONDARY -> BorderStroke(1.dp, DeepEyeColors.PrimaryCyan)
        NeonButtonStyle.DANGER -> BorderStroke(1.dp, DeepEyeColors.Error.copy(alpha = 0.55f))
        NeonButtonStyle.PRIMARY -> null
    }

    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        interactionSource = interactionSource,
        border = border,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = DeepEyeColors.Surface2,
            disabledContentColor = DeepEyeColors.TextSecondary,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = if (style == NeonButtonStyle.DANGER) DeepEyeColors.TextPrimary else DeepEyeColors.PrimaryCyan,
                strokeWidth = 2.dp,
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null)
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
