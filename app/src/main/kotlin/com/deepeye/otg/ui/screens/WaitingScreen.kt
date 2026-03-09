package com.deepeye.otg.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.domain.models.DeepEyeOperation
import com.deepeye.otg.ui.components.GlassButton
import com.deepeye.otg.ui.components.GlassCard
import com.deepeye.otg.ui.theme.StitchTokens
import dev.chrisbanes.haze.HazeState

@Composable
fun WaitingScreen(
    op: DeepEyeOperation?,
    onCancel: () -> Unit
) {
    val hazeState = remember { HazeState() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Pulsing Animation Visual
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(0.95f, 1.05f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "scale")
            val alpha by infiniteTransition.animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "alpha")

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                // Secondary pulse ring
                Box(
                    modifier = Modifier
                        .size(180.dp * scale)
                        .border(1.dp, StitchTokens.Primary.copy(alpha = 0.2f * alpha), CircleShape)
                )
                
                // Content Card
                GlassCard(
                    hazeState = hazeState,
                    cornerRadius = 100.dp,
                    accentColor = StitchTokens.Primary,
                    modifier = Modifier.size(120.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassEmpty,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).align(Alignment.Center),
                        tint = StitchTokens.Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "AWAITING HARDWARE",
                style = StitchTokens.LabelSmall,
                color = StitchTokens.Primary,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Please connect the target device\nvia OTG to proceed.",
                style = StitchTokens.TitleLarge,
                color = StitchTokens.TextPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )

            if (op != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(StitchTokens.Primary.copy(0.1f))
                        .border(1.dp, StitchTokens.Primary.copy(0.2f), CircleShape)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "QUEUED: ${op.label}",
                        style = StitchTokens.LabelSmall,
                        color = StitchTokens.Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            GlassButton(
                label = "CANCEL REQUEST",
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                accent = false
            )
        }
    }
}
