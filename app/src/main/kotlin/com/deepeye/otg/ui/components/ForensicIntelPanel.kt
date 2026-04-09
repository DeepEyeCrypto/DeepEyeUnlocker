package com.deepeye.otg.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.ui.theme.DeepEyeType

/**
 * Stage 15.1 — AI Forensic Intel Panel.
 * Provides real-time analysis of the current forensic session.
 */
@Composable
fun ForensicIntelPanel(
    analysis: String,
    confidence: Float,
    isProcessing: Boolean
) {
    GlassCard(
        hazeState = null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        accentColor = DeepEyeColors.NEON_PURPLE
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = DeepEyeColors.NEON_CYAN,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "AI FORENSIC INTEL",
                        style = DeepEyeType.CAPTION.copy(fontSize = 11.sp),
                        color = DeepEyeColors.WHITE_HIGH
                    )
                }
                
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = DeepEyeColors.NEON_PURPLE
                    )
                } else {
                    Text(
                        "CONFIDENCE: ${(confidence * 100).toInt()}%",
                        style = DeepEyeType.MONO.copy(fontSize = 12.sp).copy(fontSize = 10.sp),
                        color = DeepEyeColors.NEON_CYAN
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .border(1.dp, DeepEyeColors.WHITE_LOW.copy(0.3f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = if (isProcessing) "Analyzing chipset entropy and NVRAM parity..." else analysis,
                    style = DeepEyeType.MONO.copy(fontSize = 12.sp),
                    color = DeepEyeColors.WHITE_MED,
                    lineHeight = 18.sp
                )
            }
            
            if (!isProcessing && analysis.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        "SUGGESTED ACTION: RESTORE IDENTITY →",
                        style = DeepEyeType.CAPTION.copy(fontSize = 11.sp).copy(fontSize = 9.sp),
                        color = DeepEyeColors.NEON_PURPLE
                    )
                }
            }
        }
    }
}
