package com.deepeye.otg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
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
import com.deepeye.otg.usb.ProtocolFamily

// ═══════════════════════════════════════════════════════════════════
//  Executing-Operation screen — shows progress bar and status
// ═══════════════════════════════════════════════════════════════════

@Composable
fun ExecutingOperationScreen(
    op: DeepEyeOperation,
    protocol: ProtocolFamily,
    progress: Int,
    statusMsg: String
) {
    // Making it a full screen as requested by the filename
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        ExecutingOperationOverlay(op, protocol, progress, statusMsg)
    }
}

@Composable
fun ExecutingOperationOverlay(
    op: DeepEyeOperation,
    protocol: ProtocolFamily,
    progress: Int,
    statusMsg: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(DeepEyeColors.SurfaceDark)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = op.label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Engine: $protocol",
            color = DeepEyeColors.CyanAccent,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = DeepEyeColors.IndigoAccent,
            trackColor = DeepEyeColors.GlassBorder.copy(alpha = 0.5f),
        )

        Spacer(Modifier.height(8.dp))
        Text(
            text = statusMsg,
            color = DeepEyeColors.TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = "$progress%",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
