package com.deepeye.otg.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PermissionDeniedScreen(
    onRetry: () -> Unit
) {
    DeepSpaceBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(cornerRadius = 28.dp) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(8.dp))

                    Text("🔒", fontSize = 48.sp)

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "USB Permission Denied",
                        color = DeepEyeColors.Tier2Yellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "DeepEye needs USB access to communicate with your device.\nPlease grant permission when prompted.",
                        color = DeepEyeColors.TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(Modifier.height(24.dp))

                    // Amber glass retry button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(50),
                        color = DeepEyeColors.Tier2Yellow.copy(alpha = 0.20f),
                        border = BorderStroke(1.dp, DeepEyeColors.Tier2Yellow.copy(alpha = 0.30f)),
                        onClick = onRetry
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "TRY AGAIN",
                                color = DeepEyeColors.Tier2Yellow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
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
