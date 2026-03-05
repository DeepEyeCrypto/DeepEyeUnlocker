package com.deepeye.otg.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.usb.DeepEyeOperation

@Composable
fun OperationCompleteScreen(
    op: DeepEyeOperation,
    success: Boolean,
    message: String,
    onDismiss: () -> Unit
) {
    DeepSpaceBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            OperationCompleteBanner(op, success, message, onDismiss)
        }
    }
}

@Composable
fun OperationCompleteBanner(
    op: DeepEyeOperation,
    success: Boolean,
    message: String,
    onDismiss: () -> Unit
) {
    val accent = if (success) DeepEyeColors.Tier1Green else DeepEyeColors.Tier3Red
    val icon = if (success) "✅" else "❌"

    GlassCard(cornerRadius = 28.dp) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            Text(icon, fontSize = 48.sp)

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (success) "Operation Complete" else "Operation Failed",
                color = accent,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = op.label,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = message,
                color = DeepEyeColors.TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(24.dp))

            // Gradient DONE button
            GradientRunButton(
                text = "DONE",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}
