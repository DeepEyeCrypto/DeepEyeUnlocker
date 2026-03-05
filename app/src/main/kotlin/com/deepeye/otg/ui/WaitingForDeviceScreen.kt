package com.deepeye.otg.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.usb.DeepEyeOperation

// ═══════════════════════════════════════════════════════════════════
//  Waiting-for-Device screen — matches Stitch Screen 2 (top half)
//
//  Layout: full-screen, centered vertically
//  - Back arrow + "Waiting for Device" title + more_vert
//  - Pulsing cable icon (900ms ease-in-out infinite)
//  - "Plug in your device" heading
//  - "Waiting for device connection..." subtitle
//  - Queued operation name + TIER badge
//  - Cancel button (outlined)
// ═══════════════════════════════════════════════════════════════════

@Composable
fun WaitingForDeviceScreen(
    queuedOp: DeepEyeOperation,
    onCancel: () -> Unit
) {
    // Stitch: pulseAlpha 900ms ease-in-out infinite (0.3 → 1.0)
    val pulse = rememberInfiniteTransition(label = "cable-pulse")
    val alpha by pulse.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse-alpha"
    )

    // Tier info
    val tierLabel = when (queuedOp.tier) {
        1 -> "TIER 1 SAFE"
        2 -> "TIER 2 POLICY"
        3 -> "TIER 3 RESTRICTED"
        else -> "TIER ?"
    }
    val tierColor = when (queuedOp.tier) {
        1 -> DeepEyeColors.Tier1Green
        2 -> DeepEyeColors.Tier2Amber
        3 -> DeepEyeColors.Tier3Red
        else -> DeepEyeColors.TextSecondary
    }
    val tierBorderColor = when (queuedOp.tier) {
        1 -> Color(0xFF166534)
        2 -> Color(0xFF713F12)
        3 -> Color(0xFF7F1D1D)
        else -> DeepEyeColors.SurfaceVariant
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepEyeColors.DarkBackground)
    ) {
        // ── Top bar: ← Waiting for Device ⋮ ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "←",
                color = Color(0xFF64748B),
                fontSize = 20.sp,
                modifier = Modifier.clickable(onClick = onCancel)
            )
            Text(
                text = "Waiting for Device",
                color = DeepEyeColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Text("⋮", color = Color(0xFF64748B), fontSize = 20.sp)
        }

        // ── Centered content ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Pulsing cable icon — 64sp matching Stitch font-size: 64px
            Text(
                "🔌",
                fontSize = 64.sp,
                modifier = Modifier.alpha(alpha)
            )

            Spacer(Modifier.height(32.dp))

            // "Plug in your device" — 20sp bold
            Text(
                text = "Plug in your device",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Waiting for device connection...",
                color = DeepEyeColors.TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // "Waiting to run:" label
            Text(
                text = "Waiting to run:",
                color = DeepEyeColors.TextSecondary,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(4.dp))

            // Operation name — 18sp bold white
            Text(
                text = queuedOp.label,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            // Tier badge — rounded-full with border (Stitch style)
            Surface(
                color = tierColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(50),
                modifier = Modifier.border(1.dp, tierBorderColor, RoundedCornerShape(50))
            ) {
                Text(
                    text = tierLabel,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = tierColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(32.dp))

            // Cancel button — outlined, rounded-lg
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.height(48.dp).widthIn(min = 160.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = DeepEyeColors.TextSecondary
                ),
                border = BorderStroke(1.dp, DeepEyeColors.TextSecondary)
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
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
                color = DeepEyeColors.Tier2Amber,
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

