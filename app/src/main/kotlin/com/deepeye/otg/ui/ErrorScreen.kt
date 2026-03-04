package com.deepeye.otg.ui

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
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("\u274C", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Error", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(message, color = Color.LightGray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}
