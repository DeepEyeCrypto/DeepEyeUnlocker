package com.deepeye.otg.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.ui.theme.GlassTokens

@Composable
fun GlassBadge(
    label: String,
    fillColor: Color,
    borderColor: Color,
    textColor: Color,
    showDot: Boolean = false,
    dotAnimated: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(fillColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        if (showDot) {
            val infiniteTransition = rememberInfiniteTransition(label = "DotAnim")
            val alphaAnim by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse
                ), label = "DotAlpha"
            )
            
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
                    .then(
                        if (dotAnimated) Modifier.graphicsLayer { alpha = alphaAnim }
                        else Modifier
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GlassTierBadge(tier: Int) {
    val tokens = GlassTokens.tierColors(tier)
    GlassBadge(
        label = "TIER $tier",
        fillColor = tokens.fill,
        borderColor = tokens.border,
        textColor = tokens.text
    )
}

@Composable
fun GlassRemoteBadge() {
    GlassBadge(
        label = "REMOTE",
        fillColor = Color(0xFFF3E8FF),
        borderColor = Color(0xFFD8B4FE),
        textColor = Color(0xFF7E22CE),
        showDot = true,
        dotAnimated = true
    )
}
