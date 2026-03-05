package com.deepeye.otg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepEyeColors.DarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        OperationCompleteBanner(op, success, message, onDismiss)
    }
}

@Composable
fun OperationCompleteBanner(
    op: DeepEyeOperation,
    success: Boolean,
    message: String,
    onDismiss: () -> Unit
) {
    val bg = if (success) DeepEyeColors.Tier1Bg else DeepEyeColors.Tier3Bg
    val accent = if (success) DeepEyeColors.SafeGreen else DeepEyeColors.RestrictedRed
    val borderColor = if (success) Color(0xFF166534) else Color(0xFF7F1D1D)
    val icon = if (success) "✅" else "❌"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
            color = DeepEyeColors.TextPrimary,
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

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent)
        ) {
            Text("DONE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}
