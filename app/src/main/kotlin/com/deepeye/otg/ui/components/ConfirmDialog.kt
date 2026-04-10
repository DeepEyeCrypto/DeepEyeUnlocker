package com.deepeye.otg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DestructiveConfirmDialog(
    title:     String,
    message:   String,
    confirmText: String = "PROCEED",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Color(0xFF0F0F1A),
        shape            = RoundedCornerShape(16.dp),
        title = {
            Text("⚠️ $title", color = Color(0xFFFF4444),
                fontWeight = FontWeight.Black, fontSize = 14.sp)
        },
        text = {
            Text(message, color = Color.White.copy(0.7f), fontSize = 11.sp)
        },
        confirmButton = {
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFF4444).copy(0.2f))
                    .border(0.5.dp, Color(0xFFFF4444).copy(0.6f), RoundedCornerShape(8.dp))
                    .clickable(onClick = onConfirm)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(confirmText, color = Color(0xFFFF4444),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(0.05f))
                    .border(0.5.dp, Color.White.copy(0.1f), RoundedCornerShape(8.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("CANCEL", color = Color.White.copy(0.5f),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    )
}
