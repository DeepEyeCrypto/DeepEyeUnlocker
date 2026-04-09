package com.deepeye.otg.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
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
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.ui.theme.DeepEyeType
import dev.chrisbanes.haze.HazeState

@Composable
fun CompleteScreen(
    op: DeepEyeOperation?,
    success: Boolean,
    message: String,
    onDismiss: () -> Unit,
    onViewAudit: () -> Unit
) {
    val hazeState = remember { HazeState() }
    val accentColor = if (success) Color(0xFF4ADE80) else DeepEyeColors.NEON_PURPLE
    val icon = if (success) Icons.Default.CheckCircle else Icons.Default.Error

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Success Halo
            Box(contentAlignment = Alignment.Center) {
                val infiniteTransition = rememberInfiniteTransition(label = "finishPulse")
                val alpha by infiniteTransition.animateFloat(0.1f, 0.4f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "alpha")
                
                Box(Modifier.size(120.dp).background(accentColor.copy(alpha), CircleShape))
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = accentColor
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = if (success) "EXECUTION COMPLETE" else "EXECUTION FAILED",
                style = DeepEyeType.CAPTION.copy(fontSize = 11.sp),
                color = accentColor,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = DeepEyeType.SUBHEADER.copy(fontSize = 20.sp),
                color = DeepEyeColors.WHITE_HIGH,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (op != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "ID: ${op.id.uppercase()}",
                    style = DeepEyeType.MONO.copy(fontSize = 12.sp).copy(fontSize = 11.sp),
                    color = DeepEyeColors.WHITE_MED
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            // Action Cluster
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassButton(
                    label = "DISMISS",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    accent = !success
                )
                
                GlassButton(
                    label = "SHARE AUDIT REPORT",
                    onClick = onViewAudit,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    accent = success
                )
            }
        }
    }
}
