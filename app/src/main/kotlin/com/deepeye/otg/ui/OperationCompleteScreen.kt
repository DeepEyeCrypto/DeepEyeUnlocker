package com.deepeye.otg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
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
        modifier = Modifier.fillMaxSize().padding(24.dp),
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
    val bg = if (success) Color(0xFF0D2818) else Color(0xFF2D0A0A)
    val accent = if (success) DeepEyeColors.SafeGreen else DeepEyeColors.RestrictedRed
    val icon = if (success) "✅" else "❌"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 36.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (success) "Success" else "Failed",
            color = accent,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = message,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = accent)
        ) {
            Text("OK", color = Color.White)
        }
    }
}
