package com.deepeye.otg.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════════
//  Remote Share Screen — Liquid Glass v2
//  - DeepSpace background with glass cards
//  - Glass pill session code display
//  - Gradient START / red STOP buttons
//  - Glass-themed text input
// ═══════════════════════════════════════════════════════════════════

@Composable
fun RemoteShareScreen(
    status: String,
    subStatus: String,
    sessionCode: String?,
    isDeviceDetected: Boolean,
    onStartSharing: () -> Unit,
    onConnectRemote: (String) -> Unit,
    onBack: () -> Unit
) {
    var sessionIdInput by remember { mutableStateOf("") }

    DeepSpaceBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top bar — frosted back button + title ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onBack() },
                    shape = CircleShape,
                    color = DeepEyeColors.GlassBg,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("←", color = Color.White, fontSize = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Remote Tunnel",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Status glass card ──
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (isDeviceDetected) DeepEyeColors.Tier1Green else Color.Gray,
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            status,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            subStatus,
                            color = DeepEyeColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Session code display — glass card with monospace code ──
            if (sessionCode != null) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "YOUR SESSION ID",
                            color = DeepEyeColors.TextTertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            sessionCode,
                            color = DeepEyeColors.AccentPurple,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 4.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── Start/Stop sharing — gradient or red glass ──
            if (sessionCode == null) {
                GradientRunButton(
                    text = "START SHARING",
                    onClick = onStartSharing,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                )
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable { onStartSharing() },
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFEF4444).copy(alpha = 0.20f),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.30f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "STOP SHARING",
                            color = Color(0xFFF87171),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ── Divider ──
            HorizontalDivider(color = DeepEyeColors.GlassBorder)

            Spacer(modifier = Modifier.height(24.dp))

            // ── Connect to remote ──
            Text(
                "CONNECT TO REMOTE DEVICE",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Glass text field
            TextField(
                value = sessionIdInput,
                onValueChange = { sessionIdInput = it },
                placeholder = {
                    Text(
                        "Enter Session ID...",
                        color = DeepEyeColors.TextTertiary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = DeepEyeColors.GlassCardBg,
                    focusedContainerColor = DeepEyeColors.GlassWhite,
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White,
                    cursorColor = DeepEyeColors.AccentPurple,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = DeepEyeColors.AccentPurple
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Glass connect button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable { onConnectRemote(sessionIdInput) },
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "CONNECT",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}
