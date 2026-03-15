package com.deepeye.otg.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.ui.theme.StitchTokens
import com.deepeye.otg.usb.UsbLifecycleState
import com.deepeye.otg.viewmodel.UsbViewModel

/**
 * Stage 500.1 — Centralized Forensic Dashboard.
 * Displays a grid of all active sessions for massive-scale forensics.
 */
@Composable
fun ForensicDashboardScreen(viewModel: UsbViewModel) {
    val sessions by viewModel.sessions.collectAsState()
    val selectedKey by viewModel.selectedDeviceKey.collectAsState()
    val batchSelectedKeys by viewModel.batchSelectedKeys.collectAsState()
    val fuzzFindings by viewModel.fuzzFindings.collectAsState()
    val extractedFiles by viewModel.exploitExtractedFiles.collectAsState()
    
    var showActionConfirm by remember { mutableStateOf<String?>(null) }

    if (showActionConfirm != null) {
        AlertDialog(
            onDismissRequest = { showActionConfirm = null },
            containerColor = Color(0xFF1A1A1A),
            titleContentColor = Color.White,
            textContentColor = StitchTokens.TextSecondary,
            title = { Text("Confirm Batch Action", style = StitchTokens.TitleLarge) },
            text = { 
                Text(
                    "You are about to perform '${showActionConfirm}' on ${batchSelectedKeys.size} devices. Continue?",
                    style = StitchTokens.BodyMedium
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.performBatchOperation(showActionConfirm!!)
                        showActionConfirm = null
                    }
                ) {
                    Text("EXECUTE", color = if (showActionConfirm == "EXTRACT") Color.Red else StitchTokens.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showActionConfirm = null }) {
                    Text("CANCEL", color = StitchTokens.TextSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Forensic Multi-Device Dashboard",
                    style = StitchTokens.TitleLarge,
                    color = StitchTokens.TextPrimary
                )
                Text(
                    text = "${sessions.size} Active Sessions | ${batchSelectedKeys.size} Selected | ${fuzzFindings.size} Findings",
                    style = StitchTokens.LabelSmall,
                    color = StitchTokens.Primary
                )
                if (sessions.isNotEmpty()) {
                    Text(
                        text = if (batchSelectedKeys.size == sessions.size) "DESELECT ALL" else "SELECT ALL",
                        modifier = Modifier.padding(top = 4.dp).clickable { 
                            if(batchSelectedKeys.size == sessions.size) viewModel.clearBatchSelection() else viewModel.selectAllBatch()
                        },
                        style = StitchTokens.LabelSmall.copy(fontWeight = FontWeight.Bold),
                        color = StitchTokens.Primary
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (sessions.isNotEmpty()) {
                    androidx.compose.material3.Button(
                        onClick = { viewModel.generateForensicPdf() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StitchTokens.Primary.copy(alpha = 0.1f),
                            contentColor = StitchTokens.Primary
                        ),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(Icons.Default.Description, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("FLEET AUDIT", style = StitchTokens.LabelSmall)
                    }

                    val tunnelStatus by viewModel.tunnelStatus.collectAsState()
                    IconButton(
                        onClick = { viewModel.startRemoteTunnel() },
                        modifier = Modifier.clip(CircleShape).background(
                            if(tunnelStatus == com.deepeye.otg.service.TunnelManager.TunnelStatus.ACTIVE) 
                                Color.Green.copy(0.1f) else Color.White.copy(0.05f)
                        )
                    ) {
                        Icon(
                            Icons.Default.CloudSync, 
                            "Share", 
                            tint = if(tunnelStatus == com.deepeye.otg.service.TunnelManager.TunnelStatus.ACTIVE) 
                                Color.Green else StitchTokens.TextSecondary
                        )
                    }
                }
                
                // The CLEAR button is now part of BatchActionBar
            }
        }
        
        val aiAnalysis by viewModel.aiAnalysis.collectAsState()
        val aiIsProcessing by viewModel.aiIsProcessing.collectAsState()

        if (aiAnalysis.isNotEmpty() || fuzzFindings.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = if (fuzzFindings.isNotEmpty()) Color(0xFFEF4444).copy(alpha = 0.05f) else StitchTokens.Primary.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, if (fuzzFindings.isNotEmpty()) Color(0xFFEF4444).copy(alpha = 0.2f) else StitchTokens.Primary.copy(alpha = 0.2f))
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (fuzzFindings.isNotEmpty()) Icons.Default.Security else Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (fuzzFindings.isNotEmpty()) Color(0xFFEF4444) else StitchTokens.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    val displayText = when {
                        fuzzFindings.isNotEmpty() -> {
                            val lastFinding = fuzzFindings.last()
                            val detail = if (lastFinding.crashSignature.contains("ASLR_SLIDE")) {
                                " [RECON: ${lastFinding.crashSignature}]"
                            } else ""
                            "SECURITY FINDING: ${fuzzFindings.size} crashes logged.$detail Potential CVE-2025-43424 hits detected."
                        }
                        aiIsProcessing -> "DeepEye AI analyzing fleet context..."
                        else -> "INSIGHT: $aiAnalysis"
                    }
                    Text(
                        text = displayText,
                        style = StitchTokens.BodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = StitchTokens.TextPrimary
                    )
                }
            }
        }

        // ── Extracted Data Gallery (Stage 11.5) ────────────────────
        if (extractedFiles.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.Green.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, Color.Green.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Upload, null, tint = Color.Green, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "SENSITIVE DATA EXFILTRATED",
                            style = StitchTokens.TitleLarge.copy(fontSize = 16.sp),
                            color = Color.Green
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    
                    extractedFiles.keys.forEach { path ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Description, null, tint = Color.White.copy(0.4f), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    path,
                                    style = StitchTokens.BodyMedium.copy(fontSize = 12.sp),
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            Text(
                                "DECRYPTED",
                                style = StitchTokens.LabelSmall,
                                color = Color.Green.copy(0.6f)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))

        Box(modifier = Modifier.weight(1f)) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sessions.toList()) { (key, state) ->
                    DeviceDashboardCard(
                        deviceKey = key,
                        state = state,
                        isSelected = key == selectedKey,
                        isBatchSelected = batchSelectedKeys.contains(key),
                        onClick = { 
                            if (batchSelectedKeys.isNotEmpty()) {
                                viewModel.toggleBatchSelection(key)
                            } else {
                                viewModel.selectDevice(key)
                                viewModel.setNav(NavTarget.HOME)
                            }
                        },
                        onLongClick = { viewModel.toggleBatchSelection(key) }
                    )
                }
            }

            if (batchSelectedKeys.isNotEmpty()) {
                BatchActionBar(
                    count = batchSelectedKeys.size,
                    onAction = { showActionConfirm = it },
                    onClear = { viewModel.clearBatchSelection() },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceDashboardCard(
    deviceKey: String,
    state: UsbLifecycleState,
    isSelected: Boolean,
    isBatchSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val deviceName = when (state) {
        is UsbLifecycleState.Connected -> state.deviceName
        is UsbLifecycleState.DeviceDetected -> "Detecting..."
        else -> "Unknown"
    }

    val chipset = if (state is UsbLifecycleState.Connected) state.chipset else "Unknown"
    val mode = if (state is UsbLifecycleState.Connected) state.mode.name else "PENDING"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .clip(RoundedCornerShape(StitchTokens.RadiusDefault))
            .background(if (isBatchSelected) StitchTokens.Primary.copy(alpha = 0.1f) else StitchTokens.GlassSurface)
            .border(
                width = 1.dp,
                color = when {
                    isBatchSelected -> StitchTokens.Primary
                    isSelected -> StitchTokens.Primary.copy(alpha = 0.5f)
                    else -> StitchTokens.GlassBorder
                },
                shape = RoundedCornerShape(StitchTokens.RadiusDefault)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Usb,
                    contentDescription = null,
                    tint = if (isBatchSelected || isSelected) StitchTokens.Primary else StitchTokens.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                
                if (isBatchSelected) {
                    androidx.compose.material3.Checkbox(
                        checked = true,
                        onCheckedChange = null,
                        colors = androidx.compose.material3.CheckboxDefaults.colors(
                            checkedColor = StitchTokens.Primary,
                            uncheckedColor = Color.Transparent
                        )
                    )
                } else if (isSelected) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(StitchTokens.Primary.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "ACTIVE",
                            style = StitchTokens.LabelSmall,
                            color = StitchTokens.Primary
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = deviceName,
                style = StitchTokens.BodyMedium,
                fontWeight = FontWeight.Bold,
                color = StitchTokens.TextPrimary,
                maxLines = 1
            )
            
            Text(
                text = chipset,
                style = StitchTokens.LabelSmall,
                color = StitchTokens.TextSecondary
            )

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(if (state is UsbLifecycleState.Connected) Color(0xFF00E676) else Color(0xFFFFAB40))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = mode,
                    style = StitchTokens.MonoCode.copy(fontSize = 10.sp),
                    color = StitchTokens.TextMono,
                    modifier = Modifier.weight(1f)
                )

                // Stage 600.1 — Physical Integrity Indicator
                if (state is UsbLifecycleState.Connected) {
                    val integrityIcon = if (state.chipset.contains("Qualcomm")) Icons.Default.Shield else Icons.Default.Verified
                    val integrityColor = if (state.deviceName.contains("SEC-")) Color(0xFF00E676) else StitchTokens.Primary // Simplified mock
                    Icon(
                        imageVector = integrityIcon,
                        contentDescription = "HW Integrity",
                        tint = integrityColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BatchActionBar(
    count: Int,
    onAction: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp)),
        color = StitchTokens.GlassSurface,
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, StitchTokens.Primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BatchActionButton(text = "Identify ($count)", icon = Icons.Default.Info) { onAction("IDENTIFY") }
            Spacer(Modifier.width(16.dp))
            BatchActionButton(text = "Sahara Handshake", icon = Icons.Default.Usb) { onAction("SAHARA") }
            Spacer(Modifier.width(16.dp))
            BatchActionButton(text = "Mass Extract Media", icon = Icons.Default.Folder) { onAction("EXTRACT") }
            Spacer(Modifier.width(16.dp))
            BatchActionButton(text = "Clear", icon = Icons.Default.Close) { onClear() }
        }
    }
}

@Composable
private fun BatchActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = StitchTokens.Primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = StitchTokens.LabelSmall, color = StitchTokens.TextPrimary)
    }
}
