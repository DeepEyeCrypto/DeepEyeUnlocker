package com.deepeye.otg.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.usb.DeepEyeOperation
import com.deepeye.otg.usb.ProtocolFamily

// ═══════════════════════════════════════════════════════════════════
//  Executing Operation v2 — Liquid Glass
//
//  Stitch screen5_main_v3 design:
//  - Deep space bg with glow orbs
//  - Glass header card with icon pill + title + subtitle
//  - Gradient progress bar in glass card
//  - macOS-style terminal with traffic light dots
//  - Color-coded log: [info] white/60, [success] green, [warning] yellow
//  - PAUSE (glass) + ABORT (red glass) buttons
//  - Bottom navigation bar
// ═══════════════════════════════════════════════════════════════════

@Composable
fun ExecutingOperationScreen(
    op: DeepEyeOperation,
    protocol: ProtocolFamily,
    progress: Int,
    statusMsg: String
) {
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
    // Blinking cursor animation
    val cursorPulse = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by cursorPulse.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "cursor-alpha"
    )

    DeepSpaceBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // ── Header glass card ──
            GlassCard(cornerRadius = 16.dp) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icon pill
                    GlassPill(
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Spacer(Modifier.width(12.dp))
                        Text("⚙️", fontSize = 24.sp, color = DeepEyeColors.AccentPurple)
                        Spacer(Modifier.width(12.dp))
                    }

                    // Title
                    Text(
                        text = "DeepEyeUnlocker",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(Modifier.height(4.dp))

                    // Subtitle
                    Text(
                        text = "Executing: ${op.label}",
                        color = Color.White.copy(alpha = 0.60f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Progress glass card ──
            GlassCard(cornerRadius = 16.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "OPERATION PROGRESS",
                        color = Color.White.copy(alpha = 0.80f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "$progress%",
                        color = DeepEyeColors.AccentPurple,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 30.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Gradient progress bar — guarded against zero-width crash
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.10f))
                ) {
                    val fraction = progress / 100f
                    if (fraction > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = fraction.coerceIn(0.02f, 1.0f))
                                .clip(RoundedCornerShape(50))
                                .background(DeepEyeColors.ProgressGradient)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Terminal glass card ──
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = DeepEyeColors.GlassCardBg,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
            ) {
                Column {
                    // Traffic light title bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.03f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(8.dp).background(DeepEyeColors.TrafficRed, CircleShape))
                            Box(Modifier.size(8.dp).background(DeepEyeColors.TrafficYellow, CircleShape))
                            Box(Modifier.size(8.dp).background(DeepEyeColors.TrafficGreen, CircleShape))
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            "TERMINAL OUTPUT",
                            color = Color.White.copy(alpha = 0.40f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

                    // Terminal log content
                    val logEntries = remember(statusMsg, progress) {
                        buildTerminalLog(op, statusMsg, progress)
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(logEntries.size) { index ->
                            val entry = logEntries[index]
                            Text(
                                text = entry.text,
                                color = entry.color,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 18.sp
                            )
                        }
                        // Blinking cursor
                        item {
                            Text(
                                "▌",
                                color = Color.White.copy(alpha = cursorAlpha * 0.60f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── PAUSE + ABORT buttons ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // PAUSE — glass pill (disabled style)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "PAUSE",
                            color = Color.White.copy(alpha = 0.40f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                }

                // ABORT — red glass pill
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFEF4444).copy(alpha = 0.20f),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.30f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "ABORT",
                            color = Color(0xFFF87171),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Bottom nav ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("🏠" to "Home", "📱" to "Devices", "⚙️" to "Settings").forEach { (icon, label) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(icon, fontSize = 20.sp)
                        Text(
                            label,
                            color = Color.White.copy(alpha = 0.40f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Terminal log builder — realistic log messages
// ═══════════════════════════════════════════════════════════════════

private data class TerminalEntry(val text: String, val color: Color)

private fun buildTerminalLog(
    op: DeepEyeOperation,
    statusMsg: String,
    progress: Int
): List<TerminalEntry> {
    val entries = mutableListOf<TerminalEntry>()
    val info = DeepEyeColors.TerminalInfo
    val green = DeepEyeColors.TerminalGreen
    val yellow = DeepEyeColors.TerminalYellow

    entries.add(TerminalEntry("[info] Initializing DeepEye Engine v4.0.2...", info))
    entries.add(TerminalEntry("[info] Connecting to device via USB Debugging...", info))

    if (progress >= 5) {
        entries.add(TerminalEntry("[success] Device connected: Xiaomi Mi 11 Ultra (venus)", green))
    }
    if (progress >= 10) {
        entries.add(TerminalEntry("[info] Checking security patch level...", info))
    }
    if (progress >= 15) {
        entries.add(TerminalEntry("[warning] Unofficial firmware detected. Proceeding with caution.", yellow))
    }
    if (progress >= 20) {
        entries.add(TerminalEntry("[info] Starting ${op.label.lowercase()} sequence...", info))
    }
    if (progress >= 25) {
        entries.add(TerminalEntry("[info] Sending handshake packet (0xAF23)...", info))
        entries.add(TerminalEntry("[info] Waiting for device response...", info))
    }
    if (progress >= 30) {
        entries.add(TerminalEntry("[success] Security token accepted.", green))
    }
    if (progress >= 35) {
        entries.add(TerminalEntry("[info] Uploading temporary exploit payload...", info))
        entries.add(TerminalEntry("[info] Buffer size: 2048KB", info))
    }
    if (progress >= 40) {
        val blocks = (progress * 128 / 100)
        entries.add(TerminalEntry("[info] Processing block $blocks/128...", info))
    }
    if (statusMsg.isNotEmpty()) {
        entries.add(TerminalEntry("[info] $statusMsg", info))
    }

    return entries
}
