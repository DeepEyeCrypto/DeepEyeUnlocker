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

private val DeepEyePurple = Color(0xFF6C3EF4)
private val DeepEyeCyan   = Color(0xFF00F2FF)
private val SurfaceDark   = Color(0xFF111115)
private val MutedGray     = Color(0xFF9CA3AF)

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
            color = DeepEyePurple,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        Text("Waiting to run:", color = MutedGray, fontSize = 14.sp)

        Spacer(Modifier.height(4.dp))

        Text(
            text = queuedOp.label,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(4.dp))

        TierBadge(queuedOp.tier)

        Spacer(Modifier.height(40.dp))

        WaitingDots()

        Spacer(Modifier.height(40.dp))

        OutlinedButton(
            onClick = onCancel,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedGray)
        ) {
            Text("Cancel")
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Executing-Operation overlay — progress bar + status
// ═══════════════════════════════════════════════════════════════════

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
            .background(Color(0xFF1A1A1F))
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
            color = DeepEyeCyan,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = DeepEyePurple,
            trackColor = Color(0xFF2A2A30),
        )

        Spacer(Modifier.height(8.dp))
        Text(
            text = statusMsg,
            color = MutedGray,
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

// ═══════════════════════════════════════════════════════════════════
//  Operation-Complete banner
// ═══════════════════════════════════════════════════════════════════

@Composable
fun OperationCompleteBanner(
    op: DeepEyeOperation,
    success: Boolean,
    message: String,
    onDismiss: () -> Unit
) {
    val bg = if (success) Color(0xFF0D2818) else Color(0xFF2D0A0A)
    val accent = if (success) Color(0xFF22C55E) else Color(0xFFEF4444)
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

// ═══════════════════════════════════════════════════════════════════
//  Re-enumeration wait indicator
// ═══════════════════════════════════════════════════════════════════

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
                color = MutedGray,
                fontSize = 12.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Shared components
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun TierBadge(tier: Int) {
    val (label, color) = when (tier) {
        1 -> "SAFE" to Color(0xFF22C55E)
        2 -> "POLICY" to Color(0xFFF59E0B)
        3 -> "RESTRICTED" to Color(0xFFEF4444)
        else -> "N/A" to Color(0xFF6B7280)
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = "TIER $tier · $label",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
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
            Text("●", color = DeepEyePurple.copy(alpha = scale), fontSize = 20.sp)
        }
    }
}
