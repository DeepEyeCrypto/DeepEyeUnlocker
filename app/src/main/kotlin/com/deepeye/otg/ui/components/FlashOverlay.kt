package com.deepeye.otg.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Build

@Composable
fun FlashProgressOverlay(percent: Int, step: String) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.85f))
            .let { 
                if (Build.VERSION.SDK_INT >= 31) it.blur(2.dp) else it 
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Circular ring
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = percent / 100f,
                    modifier = Modifier.size(90.dp),
                    color = when {
                        step.contains("MTK",  ignoreCase = true) -> Color(0xFFA78BFA)
                        step.contains("EDL",  ignoreCase = true) -> Color(0xFFFF007F)
                        step.contains("Fast", ignoreCase = true) -> Color(0xFFFB923C)
                        else -> Color(0xFF00FFFF)
                    },
                    strokeWidth = 5.dp,
                    trackColor = Color.White.copy(0.06f)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$percent",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text("%", color = Color.White.copy(0.4f), fontSize = 9.sp)
                }
            }

            Text(step.uppercase(), color = Color.White.copy(0.7f),
                fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            
            // Animated dots
            val infiniteTransition = rememberInfiniteTransition(label = "dots")
            val dotAlpha by infiniteTransition.animateFloat(
                0.2f, 1f,
                infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
                label = "dot"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { i ->
                    Box(
                        Modifier
                            .size(5.dp)
                            .alpha(if (i == (System.currentTimeMillis() / 300 % 3).toInt()) 1f else dotAlpha)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(0.4f))
                    )
                }
            }
        }
    }
}
