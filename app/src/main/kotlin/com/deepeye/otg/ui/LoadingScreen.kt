package com.deepeye.otg.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Loading screen — shown immediately while native lib loads on IO thread.
 * Prevents black screen / ANR by giving the user visual feedback instantly.
 */
@Composable
fun DeepEyeLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF05050F), Color(0xFF0A0015))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Pulsing emoji icon
            val pulse = rememberInfiniteTransition(label = "pulse")
            val alpha by pulse.animateFloat(
                initialValue = 0.4f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    tween(900, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse
                ),
                label = "alpha"
            )
            Text(
                text = "🔓",
                fontSize = 56.sp,
                modifier = Modifier.graphicsLayer { this.alpha = alpha }
            )
            Text(
                text = "DeepEye Unlocker",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Initializing engine...",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 13.sp
            )
        }
    }
}
