package com.deepeye.otg.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.usb.DeepEyeOperation

// ═══════════════════════════════════════════════════════════════════
//  Waiting-for-Device v2 — Liquid Glass
//
//  Stitch screen4_waiting_v2 design:
//  - Animated purple/blue orbs in background
//  - Top bar: frosted ← button | "TIER 1 SAFE" pill | ⋮ button
//  - Centered frosted glass card (28px radius)
//  - Purple radial gradient behind cable icon
//  - Floating animation on icon container
//  - "Unlock Bootloader" in amber glass pill
//  - Full-width glass Cancel button
// ═══════════════════════════════════════════════════════════════════

@Composable
fun WaitingForDeviceScreen(
    queuedOp: DeepEyeOperation,
    onCancel: () -> Unit
) {
    // Pulse animation for cable icon (3s slow pulse)
    val pulse = rememberInfiniteTransition(label = "icon-pulse")
    val iconScale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )
    val iconAlpha by pulse.animateFloat(
        initialValue = 0.8f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )

    // Floating animation for orb
    val float = rememberInfiniteTransition(label = "float")
    val floatY by float.animateFloat(
        initialValue = 0f, targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "floatY"
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
        2 -> DeepEyeColors.Tier2Yellow
        3 -> DeepEyeColors.Tier3Red
        else -> DeepEyeColors.TextSecondary
    }

    DeepSpaceBackground {
        // Extra animated orb
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.Center)
                .offset(y = floatY.dp)
                .blur(120.dp)
                .background(DeepEyeColors.OrbPurple.copy(alpha = 0.30f), CircleShape)
        )

        // ── Top bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button — frosted circle
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onCancel() },
                shape = CircleShape,
                color = DeepEyeColors.GlassBg,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("←", color = Color.White, fontSize = 18.sp)
                }
            }

            // Tier pill
            GlassPill {
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(tierColor, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = tierLabel,
                    color = Color.White.copy(alpha = 0.80f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.width(12.dp))
            }

            // More button — frosted circle
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = DeepEyeColors.GlassBg,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("⋮", color = Color.White, fontSize = 18.sp)
                }
            }
        }

        // ── Centered frosted card ──
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 340.dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GlassCard(cornerRadius = 28.dp) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(16.dp))

                    // Purple radial icon container
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(iconScale)
                            .clip(CircleShape)
                            .background(
                                androidx.compose.ui.graphics.Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF8B5CF6).copy(alpha = 0.40f),
                                        Color(0xFF8B5CF6).copy(alpha = 0.10f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "🔌",
                            fontSize = 36.sp,
                            modifier = Modifier.alpha(iconAlpha)
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // Title
                    Text(
                        text = "Waiting for Device",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Plug in your device via USB\nOTG cable",
                        color = Color.White.copy(alpha = 0.60f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(Modifier.height(24.dp))

                    // "Ready to run" label
                    Text(
                        text = "READY TO RUN",
                        color = Color.White.copy(alpha = 0.40f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    // Operation name — glass pill with amber dot
                    GlassPill {
                        Spacer(Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(DeepEyeColors.Tier2Yellow, CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = queuedOp.label,
                            color = DeepEyeColors.Tier2Yellow,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(Modifier.width(12.dp))
                    }

                    Spacer(Modifier.height(32.dp))

                    // Cancel button — full-width glass
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable { onCancel() },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "CANCEL",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }
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
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.10f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("⏳", fontSize = 20.sp, modifier = Modifier.alpha(alpha))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                "MTK Mode Switch",
                color = DeepEyeColors.Tier2Yellow,
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
