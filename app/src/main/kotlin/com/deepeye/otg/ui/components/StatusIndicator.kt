package com.deepeye.otg.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.deepeye.otg.ui.theme.DeepEyeColors

enum class StatusIndicatorState {
    CONNECTED,
    SCANNING,
    ERROR,
    DISCONNECTED,
}

@Composable
fun StatusIndicator(
    state: StatusIndicatorState,
    label: String? = null,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "statusIndicator")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "statusPulseScale",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "statusPulseAlpha",
    )
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "statusBlinkAlpha",
    )
    val shakeX = remember { Animatable(0f) }

    LaunchedEffect(state) {
        if (state == StatusIndicatorState.ERROR) {
            shakeX.snapTo(0f)
            shakeX.animateTo(-8f, animationSpec = tween(80))
            shakeX.animateTo(8f, animationSpec = tween(80))
            shakeX.animateTo(-4f, animationSpec = tween(80))
            shakeX.animateTo(0f, animationSpec = tween(160))
        } else {
            shakeX.snapTo(0f)
        }
    }

    val color = when (state) {
        StatusIndicatorState.CONNECTED -> DeepEyeColors.Success
        StatusIndicatorState.SCANNING -> DeepEyeColors.PrimaryCyan
        StatusIndicatorState.ERROR -> DeepEyeColors.Error
        StatusIndicatorState.DISCONNECTED -> DeepEyeColors.TextFaint
    }
    val activeAlpha = when (state) {
        StatusIndicatorState.SCANNING -> blinkAlpha
        StatusIndicatorState.DISCONNECTED -> 0.45f
        else -> 1f
    }

    Row(
        modifier = modifier.graphicsLayer(translationX = shakeX.value),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer(
                        scaleX = if (state == StatusIndicatorState.DISCONNECTED) 1f else pulseScale,
                        scaleY = if (state == StatusIndicatorState.DISCONNECTED) 1f else pulseScale
                    )
                    .alpha(if (state == StatusIndicatorState.DISCONNECTED) 0.14f else pulseAlpha)
                    .background(color, CircleShape),
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .alpha(activeAlpha)
                    .background(color, CircleShape),
            )
        }

        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (state == StatusIndicatorState.DISCONNECTED) {
                    DeepEyeColors.TextSecondary
                } else {
                    DeepEyeColors.TextPrimary
                },
            )
        }
    }
}
