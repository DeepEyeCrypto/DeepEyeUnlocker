package com.deepeye.otg.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.usb.DeepEyeOperation
import com.deepeye.otg.usb.ProtocolFamily
import com.deepeye.otg.usb.SessionState

// ═══════════════════════════════════════════════════════════════════
//  Waiting-for-Device screen — shown when an operation is queued
//  and the USB cable is not yet plugged in.
// ═══════════════════════════════════════════════════════════════════

@Composable
fun WaitingForDeviceScreen(
    queuedOp: DeepEyeOperation,
    onCancel: () -> Unit
) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val alpha by pulse.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Pulsing USB icon
        Text("🔌", fontSize = 64.sp, modifier = Modifier.alpha(alpha))

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Plug in your device",
            style = MaterialTheme.typography.headlineSmall,
            color = DeepEyeColors.IndigoAccent,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        Text("Waiting to run:", color = DeepEyeColors.TextSecondary, fontSize = 14.sp)

        Spacer(Modifier.height(4.dp))

        Text(
            text = queuedOp.label,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(4.dp))

        OperationTierBadge(queuedOp.tier)

        Spacer(Modifier.height(40.dp))

        WaitingDots()

        Spacer(Modifier.height(40.dp))

        OutlinedButton(
            onClick = onCancel,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepEyeColors.TextSecondary)
        ) {
            Text("Cancel")
        }
    }
}


@Composable
private fun WaitingDots() {
    val transition = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0, 200, 400).forEach { delayMs ->
            val scale by transition.animateFloat(
                initialValue = 0.3f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = delayMs),
                    repeatMode = RepeatMode.Reverse
                ), label = "dot$delayMs"
            )
            Text("●", color = DeepEyeColors.IndigoAccent.copy(alpha = scale), fontSize = 20.sp)
        }
    }
}

@Composable
fun ReenumerationWaitBanner() {
    val pulse = rememberInfiniteTransition(label = "reenum")
    val alpha by pulse.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "reenum-alpha"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1A2E))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("⏳", fontSize = 20.sp, modifier = Modifier.alpha(alpha))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                "MTK Mode Switch",
                color = Color(0xFFFFB300),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                "Re-requesting permission...",
                color = DeepEyeColors.TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
