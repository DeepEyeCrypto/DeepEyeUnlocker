package com.deepeye.otg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConnectedMtpOnlyScreen(
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111115))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📁", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "MTP Mode Detected",
                color = Color(0xFFF59E0B),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "The attached phone is in MTP/File Transfer mode.\n\n" +
                        "DeepEye needs a special boot mode (EDL/BROM/Download) to perform operations.\n\n" +
                        "Please check Developer Options and USB Debugging settings.",
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}
