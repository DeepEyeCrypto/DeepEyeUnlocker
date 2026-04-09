package com.deepeye.otg.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.deepeye.otg.ui.components.ForensicIntelPanel
import com.deepeye.otg.ui.components.GlassButton
import com.deepeye.otg.ui.components.GlassCard
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.ui.theme.DeepEyeType
import com.deepeye.otg.repair.NvBridge
import dev.chrisbanes.haze.HazeState

/**
 * Stage 12.1 — Magic Repair Center (Identity Restoration).
 * High-end tactical UI for restricted chipset operations.
 */
@Composable
fun ImeiRepairScreen(
    onRepair: (String, String) -> Unit,
    onRead: () -> Unit,
    currentImei1: String = "N/A",
    currentImei2: String = "N/A",
    hazeState: HazeState? = null,
    perfMode: Boolean = false,
    aiAnalysis: String = "",
    isAiProcessing: Boolean = false
) {
    var imei1 by remember { mutableStateOf("") }
    var imei2 by remember { mutableStateOf("") }
    
    val isImei1Valid = remember(imei1) { NvBridge.verifyImeiChecksum(imei1) }
    val isImei2Valid = remember(imei2) { NvBridge.verifyImeiChecksum(imei2) }
    
    var showSafetyConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepEyeColors.BG_VOID)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tactical Header
        TacticalHeader()

        // 1. AI Assistant Integration (Stage 15.1)
        ForensicIntelPanel(
            analysis = if (aiAnalysis.isEmpty()) {
                "Chipset identified as MT6765. NVRAM partition found. SecureBoot: ENABLED. suggested: READ IDENTITY first."
            } else aiAnalysis,
            confidence = 0.94f,
            isProcessing = isAiProcessing
        )

        // 2. Current Identity Observer
        SecurityCard(
            title = "CURRENT IDENTITY (NVRAM PEAK)",
            hazeState = hazeState,
            perfMode = perfMode
        ) {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    IdentityBlock("IMEI_01", currentImei1)
                    IdentityBlock("IMEI_02", currentImei2)
                }
                Spacer(Modifier.height(24.dp))
                GlassButton(
                    onClick = onRead, 
                    label = "SYNC FROM CHIPSET", 
                    modifier = Modifier.fillMaxWidth(),
                    accent = true
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 3. Restoration Engine
        SecurityCard(
            title = "IDENTITY RESTORATION ENGINE",
            hazeState = hazeState,
            perfMode = perfMode,
            accent = DeepEyeColors.NEON_PURPLE
        ) {
            Column {
                RestorationInput(
                    label = "RESTORE PRIMARY IMEI",
                    value = imei1,
                    onValueChange = { if(it.length <= 15) imei1 = it },
                    isValid = isImei1Valid,
                    placeholder = "Enter 15-digit IMEI"
                )
                
                Spacer(Modifier.height(16.dp))
                
                RestorationInput(
                    label = "RESTORE SECONDARY IMEI",
                    value = imei2,
                    onValueChange = { if(it.length <= 15) imei2 = it },
                    isValid = isImei2Valid,
                    placeholder = "Enter 15-digit IMEI"
                )

                Spacer(Modifier.height(32.dp))
                
                Button(
                    onClick = { showSafetyConfirm = true },
                    enabled = (isImei1Valid && imei1.isNotEmpty()) || (isImei2Valid && imei2.isNotEmpty()),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepEyeColors.NEON_PURPLE,
                        disabledContainerColor = Color.DarkGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(StitchTokens.RadiusDefault))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LockOpen, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "INITIATE RESTORATION CHAIN", 
                            style = DeepEyeType.SUBHEADER.copy(fontSize = 20.sp).copy(fontSize = 14.sp),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    if (showSafetyConfirm) {
        G7SafetyDialog(
            onCancel = { showSafetyConfirm = false },
            onConfirm = {
                showSafetyConfirm = false
                onRepair(imei1, imei2)
            }
        )
    }
}

@Composable
private fun TacticalHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "badgePulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )

        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = DeepEyeColors.NEON_PURPLE.copy(alpha = alpha),
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "MAGIC REPAIR CENTER",
            style = DeepEyeType.HEADER.copy(fontSize = 32.sp).copy(fontSize = 24.sp),
            color = DeepEyeColors.WHITE_HIGH
        )
        Text(
            "RESTRICTED IDENTITY RESTORATION PROTOCOL (G7)",
            style = DeepEyeType.CAPTION.copy(fontSize = 11.sp).copy(letterSpacing = 2.sp),
            color = DeepEyeColors.NEON_PURPLE
        )
    }
}

@Composable
private fun SecurityCard(
    title: String,
    hazeState: HazeState?,
    perfMode: Boolean,
    accent: Color = DeepEyeColors.WHITE_MED,
    content: @Composable () -> Unit
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title,
            style = DeepEyeType.CAPTION.copy(fontSize = 11.sp).copy(fontSize = 9.sp),
            color = accent,
            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
        )
        GlassCard(
            hazeState = hazeState,
            performanceMode = perfMode,
            modifier = Modifier.fillMaxWidth(),
            accentColor = accent.copy(alpha = 0.35f)
        ) {
            Box(Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun IdentityBlock(label: String, value: String) {
    Column {
        Text(label, style = DeepEyeType.MONO.copy(fontSize = 12.sp).copy(fontSize = 10.sp), color = DeepEyeColors.WHITE_MED)
        Text(
            text = value,
            style = DeepEyeType.SUBHEADER.copy(fontSize = 20.sp).copy(fontSize = 18.sp),
            color = DeepEyeColors.WHITE_HIGH,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RestorationInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isValid: Boolean,
    placeholder: String
) {
    Column {
        Text(
            text = label,
            style = DeepEyeType.CAPTION.copy(fontSize = 11.sp).copy(fontSize = 10.sp),
            color = DeepEyeColors.WHITE_MED,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
            textStyle = DeepEyeType.MONO.copy(fontSize = 12.sp).copy(fontSize = 16.sp),
            placeholder = { Text(placeholder, style = DeepEyeType.MONO.copy(fontSize = 12.sp), color = Color.Gray) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(0.05f),
                unfocusedContainerColor = Color.White.copy(0.02f),
                focusedIndicatorColor = if (isValid) DeepEyeColors.NEON_PURPLE else Color.Red,
                unfocusedIndicatorColor = if (value.isEmpty()) Color.Transparent else if (isValid) DeepEyeColors.NEON_PURPLE else Color.Red,
                cursorColor = DeepEyeColors.NEON_PURPLE
            ),
            trailingIcon = {
                if (value.length == 15) {
                    Icon(
                        imageVector = if (isValid) Icons.Default.Fingerprint else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isValid) DeepEyeColors.NEON_PURPLE else Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )
    }
}

@Composable
private fun G7SafetyDialog(onCancel: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        GlassCard(hazeState = null, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Warning, null, tint = DeepEyeColors.NEON_YELLOW, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    "CRITICAL SECURITY OVERRIDE",
                    style = DeepEyeType.SUBHEADER.copy(fontSize = 20.sp),
                    color = DeepEyeColors.WHITE_HIGH
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "You are about to modify low-level radio metadata (NVRAM). This operation will be logged in the Forensic Audit Trail. DeepEye will auto-dump a rollback image before commit.",
                    style = DeepEyeType.BODY.copy(fontSize = 14.sp),
                    color = DeepEyeColors.WHITE_MED,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(32.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassButton(onClick = onCancel, label = "ABORT", modifier = Modifier.weight(1f), accent = false)
                    GlassButton(onClick = onConfirm, label = "COMMIT", modifier = Modifier.weight(1f), accent = true)
                }
            }
        }
    }
}
