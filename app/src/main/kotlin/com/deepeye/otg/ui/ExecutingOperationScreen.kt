package com.deepeye.otg.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.usb.DeepEyeOperation
import com.deepeye.otg.usb.ProtocolFamily

// ═══════════════════════════════════════════════════════════════════
//  Executing Operation — matches Stitch Screen 2 (bottom half)
//
//  Layout: full-screen
//  - Back arrow + "System Flash" title + RUNNING badge + ⋮
//  - "Operation Progress" label + percentage
//  - Progress bar (primary purple fill)
//  - "Execution Log" section with terminal-style log
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
    // Pulse animation for the latest log line
    val pulse = rememberInfiniteTransition(label = "log-pulse")
    val logPulseAlpha by pulse.animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "log-alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepEyeColors.DarkBackground)
    ) {
        // ── Top bar: ← System Flash [RUNNING] ⋮ ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("←", color = Color(0xFF64748B), fontSize = 20.sp)

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = op.label,
                    color = DeepEyeColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                // RUNNING badge (green pill)
                Surface(
                    color = DeepEyeColors.SafeGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.border(
                        1.dp, Color(0xFF166534), RoundedCornerShape(50)
                    )
                ) {
                    Text(
                        text = "RUNNING",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = DeepEyeColors.SafeGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Text("⋮", color = Color(0xFF64748B), fontSize = 20.sp)
        }

        // ── Progress section ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Header row: "Operation Progress" + "47%"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Operation Progress",
                    color = DeepEyeColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$progress%",
                    color = DeepEyeColors.Primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar — Stitch: rounded-full bg-slate-800 h-3
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF1E293B))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progress / 100f)
                        .clip(RoundedCornerShape(50))
                        .background(DeepEyeColors.Primary)
                ) {
                    // Shimmer overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.2f))
                    )
                }
            }
        }

        // ── Execution Log section ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = "Execution Log",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Terminal box — rounded-xl, dark surface, border
            Surface(
                color = DeepEyeColors.SurfaceDark,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, DeepEyeColors.SurfaceVariant, RoundedCornerShape(12.dp))
            ) {
                // Generate log entries from statusMsg
                val logEntries = remember(statusMsg, progress) {
                    buildLogEntries(op, statusMsg, progress)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logEntries.size) { index ->
                        val entry = logEntries[index]
                        val isLast = index == logEntries.lastIndex
                        Row(
                            modifier = if (isLast) Modifier.alpha(logPulseAlpha) else Modifier,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = entry.first,
                                color = Color(0xFF64748B),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(72.dp)
                            )
                            Text(
                                text = entry.second,
                                color = DeepEyeColors.TerminalGreen,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Helper — generate mock log entries for visual display
// ═══════════════════════════════════════════════════════════════════

private fun buildLogEntries(
    op: DeepEyeOperation,
    statusMsg: String,
    progress: Int
): List<Pair<String, String>> {
    val entries = mutableListOf<Pair<String, String>>()
    val baseTime = "12:00"

    entries.add("${baseTime}:01" to "Initializing device interface...")
    
    if (progress >= 5) {
        entries.add("${baseTime}:02" to "Device connected (usb-1/3)")
    }
    if (progress >= 10) {
        entries.add("${baseTime}:03" to "Requesting ${op.label.lowercase()}...")
    }
    if (progress >= 20) {
        entries.add("${baseTime}:05" to "Starting data transfer...")
    }
    if (progress >= 30) {
        entries.add("${baseTime}:06" to "Processing partition data...")
    }
    if (progress > 0 && statusMsg.isNotEmpty()) {
        entries.add("${baseTime}:15" to "$statusMsg ($progress%)...")
    }

    return entries
}
