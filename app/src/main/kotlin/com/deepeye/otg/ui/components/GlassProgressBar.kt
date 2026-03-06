package com.deepeye.otg.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.deepeye.otg.ui.theme.GlassTokens

@Composable
fun GlassProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    // CRASHFIX: Minimum width guard to prevent Canvas native crashes
    val safeProgress = progress.coerceIn(0.02f, 1.0f)
    
    val animatedProgress by animateFloatAsState(
        targetValue = safeProgress,
        animationSpec = tween(durationMillis = 300), label = "ProgAnim"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50))
            .background(GlassTokens.GlassBorderDark)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(GlassTokens.accentBtnBrush)
        )
    }
}
