package com.deepeye.otg.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.ui.components.*
import com.deepeye.otg.domain.models.*
import com.deepeye.otg.ui.theme.StitchTokens
import com.deepeye.otg.usb.UsbLifecycleState
import com.deepeye.otg.viewmodel.UsbViewModel
import com.deepeye.otg.intelligence.vulndb.RiskLevel
import com.deepeye.otg.ui.viewmodel.LogEntry

/**
 * V3.0 — Mission Dashboard (The Command Center)
 * Dedicated focal point for a single device target.
 */
@Composable
fun TargetDashboardScreen(viewModel: UsbViewModel, hazeState: dev.chrisbanes.haze.HazeState) {
    val sessions by viewModel.sessions.collectAsState()
    val selectedKey by viewModel.selectedDeviceKey.collectAsState()
    val exposureReport by viewModel.exposureReport.collectAsState()
    val logs by viewModel.logs.collectAsState()
    
    val selectedSession = selectedKey?.let { sessions[it] } ?: UsbLifecycleState.Idle

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 1. MISSION STATUS INDICATOR ────────────────────────────────────
            item {
                MissionStatusRow(
                    status = if (selectedSession is UsbLifecycleState.Connected) "TARGET_ACQUIRED" else "SEARCHING_TARGET"
                )
            }

            // ── 2. TARGET HERO CARD ───────────────────────────────────────────
            item {
                TargetHeroCard(state = selectedSession, hazeState = hazeState)
            }

            // ── 2.5 VULNERABILITY RISK REPORT ───────────────────────────────
            if (selectedSession is UsbLifecycleState.Connected && exposureReport != null) {
                item {
                    VulnerabilityRiskCard(
                        report = exposureReport!!, 
                        hazeState = hazeState,
                        onActivate = { viewModel.queueOperation("op_auto_exploit") }
                    )
                }
            }

            // ── 3. TRIAGE ACTION GRID (CLEAN/NON-INVASIVE) ────────────────────
            item {
                TriageActionGrid(
                    onReadInfo = { viewModel.queueOperation("READ_INFO") },
                    onSecurityScan = { viewModel.queueOperation("SECURITY_SCAN") },
                    isEnabled = selectedSession is UsbLifecycleState.Connected
                )
            }

            // ── 4. HARDWARE CONTEXT (TECHNICAL DETAILS) ──────────────────────
            item {
                HardwareContextCard(state = selectedSession, hazeState = hazeState)
            }
            
            // Spacer to push everything up if content is short
            item { Spacer(Modifier.height(8.dp)) }
        }

        // ── 5. GROUNDED ACTIVITY FEED ────────────────────────────────────
        TerminalMiniPreview(
            logs = logs.takeLast(5),
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}

@Composable
private fun MissionStatusRow(status: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "MISSION_TRACKER",
                style = StitchTokens.LabelSmall.copy(fontSize = 10.sp),
                color = StitchTokens.TextSecondary
            )
            Text(
                text = status,
                style = StitchTokens.TitleLarge.copy(letterSpacing = 1.sp, fontSize = 18.sp),
                color = if (status.startsWith("TARGET")) StitchTokens.AccentSuccess else StitchTokens.TextPrimary
            )
        }
    }
}

@Composable
private fun MissionHeader(sessionCount: Int, status: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "MISSION_STATUS",
                style = StitchTokens.LabelSmall,
                color = StitchTokens.TextSecondary
            )
            Text(
                text = status,
                style = StitchTokens.TitleLarge.copy(letterSpacing = 2.sp),
                color = if (status.startsWith("TARGET")) StitchTokens.AccentSuccess else StitchTokens.TextPrimary
            )
        }
        
        Badge(
            containerColor = StitchTokens.Primary.copy(alpha = 0.1f),
            contentColor = StitchTokens.Primary
        ) {
            Text(
                text = "$sessionCount ACTIVE SESSIONS",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = StitchTokens.LabelSmall
            )
        }
    }
}

@Composable
private fun TargetHeroCard(state: UsbLifecycleState, hazeState: dev.chrisbanes.haze.HazeState) {
    val isConnected = state is UsbLifecycleState.Connected
    
    ConnectionAuraCard(state = state, hazeState = hazeState) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Target Pulse Icon
            Box(contentAlignment = Alignment.Center) {
                val protocol = when (state) {
                    is UsbLifecycleState.Connected -> state.protocolFamily
                    is UsbLifecycleState.DeviceDetected -> state.protocolFamily
                    is UsbLifecycleState.Connecting -> state.protocolFamily
                    else -> ProtocolFamily.UNKNOWN
                }
                val protocolColor = if (state.isConnected || state is UsbLifecycleState.DeviceDetected) 
                    protocol.getAccentColor() 
                else StitchTokens.Primary
                
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(protocolColor.copy(alpha = 0.1f))
                )
                Icon(
                    imageVector = if (isConnected) Icons.Default.PhoneAndroid else Icons.Default.UsbOff,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (isConnected) protocolColor else StitchTokens.TextSecondary
                )
            }

            Spacer(Modifier.width(24.dp))

            Column {
                if (isConnected) {
                    val name = when (state) {
                        is UsbLifecycleState.Connected -> state.deviceName
                        is UsbLifecycleState.Operating -> state.deviceName
                        is UsbLifecycleState.Degraded -> state.deviceName
                        else -> "UNKNOWN_TARGET"
                    }
                    val protocol = when (state) {
                        is UsbLifecycleState.Connected -> state.protocolFamily
                        else -> ProtocolFamily.UNKNOWN
                    }
                    val confidence = when (state) {
                        is UsbLifecycleState.Connected -> state.confidence
                        else -> 100
                    }

                    Text(
                        text = name.uppercase(),
                        style = StitchTokens.DisplayLarge.copy(fontSize = 22.sp, letterSpacing = 1.sp),
                        color = StitchTokens.TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProtocolBadge(protocol)
                        Spacer(Modifier.width(12.dp))
                        ConfidenceBadge(confidence)
                    }
                } else {
                    Text(
                        text = "AWAITING_PHYSICAL_LINK",
                        style = StitchTokens.TitleLarge.copy(letterSpacing = 2.sp),
                        color = StitchTokens.TextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Connect device via OTG to start triage",
                        style = StitchTokens.BodyMedium,
                        color = StitchTokens.TextSecondary.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TriageActionGrid(
    onReadInfo: () -> Unit,
    onSecurityScan: () -> Unit,
    isEnabled: Boolean
) {
    Column {
        Text(
            text = "SAFE_TRIAGE_ACTIONS",
            style = StitchTokens.LabelSmall,
            color = StitchTokens.TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TriageActionCard(
                label = "READ INFO",
                icon = Icons.Default.Dns,
                onClick = onReadInfo,
                modifier = Modifier.weight(1f),
                isEnabled = isEnabled
            )
            TriageActionCard(
                label = "SECURITY SCAN",
                icon = Icons.Default.Security,
                onClick = onSecurityScan,
                modifier = Modifier.weight(1f),
                isEnabled = isEnabled
            )
        }
    }
}

@Composable
private fun TriageActionCard(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = if (isEnabled) 0.05f else 0.02f),
        border = BorderStroke(1.dp, StitchTokens.Semantic.ProtocolAdb.copy(alpha = if (isEnabled) 0.4f else 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon, null, 
                modifier = Modifier.size(18.dp), 
                tint = if (isEnabled) StitchTokens.AccentSuccess else StitchTokens.TextSecondary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = StitchTokens.LabelSmall,
                color = if (isEnabled) StitchTokens.TextPrimary else StitchTokens.TextSecondary
            )
        }
    }
}

@Composable
private fun HardwareContextCard(state: UsbLifecycleState, hazeState: dev.chrisbanes.haze.HazeState) {
    var isExpanded by remember { mutableStateOf(false) }
    
    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
        hazeState = hazeState,
        accentColor = Color.White.copy(0.1f)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeveloperBoard, null, tint = StitchTokens.TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("HARDWARE_DESCRIPTORS", style = StitchTokens.LabelSmall, color = StitchTokens.TextSecondary)
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = StitchTokens.TextSecondary
                )
            }
            
            if (isExpanded) {
                Spacer(Modifier.height(16.dp))
                if (state is UsbLifecycleState.Connected) {
                    DescriptorRow("VENDOR_ID", "0x${state.vendorId.toString(16).uppercase()}")
                    DescriptorRow("PRODUCT_ID", "0x${state.productId.toString(16).uppercase()}")
                    DescriptorRow("SERIAL", state.deviceKey.take(8).uppercase())
                    DescriptorRow("PROTOCOL", state.mode.name)
                } else {
                    Text("No hardware context available", style = StitchTokens.BodyMedium, color = StitchTokens.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun DescriptorRow(key: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(key, style = StitchTokens.MonoCode.copy(fontSize = 11.sp), color = StitchTokens.TextSecondary)
        Text(value, style = StitchTokens.MonoCode.copy(fontSize = 11.sp), color = StitchTokens.TextMono)
    }
}

@Composable
private fun TerminalMiniPreview(
    logs: List<com.deepeye.otg.ui.viewmodel.LogEntry>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "TELEMETRY_STREAM",
            style = StitchTokens.LabelSmall.copy(fontSize = 10.sp),
            color = StitchTokens.TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth().height(100.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.Black.copy(0.6f),
            border = BorderStroke(1.dp, Color.White.copy(0.05f))
        ) {
            LazyColumn(modifier = Modifier.padding(12.dp)) {
                items(logs) { log ->
                    Text(
                        text = "> ${log.message}",
                        style = StitchTokens.MonoCode.copy(fontSize = 10.sp, letterSpacing = 0.5.sp),
                        color = when (log.type) {
                            "ERROR" -> Color(0xFFF87171)
                            "SUCCESS" -> Color(0xFF4ADE80)
                            else -> com.deepeye.otg.ui.theme.StitchTokens.Semantic.TextTechnical.copy(alpha = 0.8f)
                        }
                    )
                }
            }
        }
    }
}
@Composable
private fun VulnerabilityRiskCard(
    report: com.deepeye.otg.intelligence.vulndb.DevicePatchReport,
    hazeState: dev.chrisbanes.haze.HazeState,
    onActivate: () -> Unit
) {
    val riskColor = when (report.overallRiskLevel) {
        RiskLevel.CRITICAL -> Color(0xFFFF1744)
        RiskLevel.HIGH -> Color(0xFFFF9100)
        RiskLevel.MEDIUM -> Color(0xFFFFD600)
        RiskLevel.LOW -> Color(0xFF4ADE80)
        else -> Color.Gray
    }

    val kveCount = report.exposedCves.count { it.cisaKev }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        hazeState = hazeState,
        accentColor = riskColor.copy(alpha = 0.2f)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = riskColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "VULN_INTELLIGENCE_REPORT", 
                            style = StitchTokens.LabelSmall, 
                            color = StitchTokens.TextSecondary
                        )
                        Text(
                            "RISK_LEVEL: ${report.overallRiskLevel}", 
                            style = StitchTokens.TitleLarge.copy(fontSize = 16.sp),
                            color = riskColor
                        )
                    }
                }
                
                if (kveCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFF1744).copy(alpha = 0.1f))
                            .border(1.dp, Color(0xFFFF1744).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "⚡ CISA KEV",
                            color = Color(0xFFFF1744),
                            style = StitchTokens.LabelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                var detailsVisible by remember { mutableStateOf(false) }
                
                StatItem(
                    label = "EXPOSED CVEs",
                    value = report.exposedCves.size.toString(),
                    color = if (report.exposedCves.isNotEmpty()) riskColor else StitchTokens.TextSecondary,
                    modifier = Modifier.weight(1f).clickable { if(report.exposedCves.isNotEmpty()) detailsVisible = true }
                )
                
                if (detailsVisible) {
                    CveExposuresDialog(
                        exposures = report.exposedCves,
                        onDismiss = { detailsVisible = false }
                    )
                }

                StatItem(
                    label = "PATCH STATE",
                    value = if (report.androidSplStatus == com.deepeye.otg.intelligence.vulndb.SplStatus.OUTDATED) "OUTDATED" else "CURRENT",
                    color = if (report.androidSplStatus == com.deepeye.otg.intelligence.vulndb.SplStatus.OUTDATED) Color(0xFFFF9100) else Color(0xFF4ADE80),
                    modifier = Modifier.weight(1f)
                )
            }

            if (report.overallRiskLevel == RiskLevel.CRITICAL || report.overallRiskLevel == RiskLevel.HIGH) {
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onActivate,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = riskColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.FlashOn, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "ACTIVATE COMPROMISE CHAIN", 
                        style = StitchTokens.LabelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CveExposuresDialog(
    exposures: List<com.deepeye.otg.intelligence.vulndb.CveEntry>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StitchTokens.BackgroundDark,
        title = {
            Text("EXPOSED_VULNERABILITIES", style = StitchTokens.TitleLarge, color = Color.White)
        },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(exposures) { cve ->
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (cve.exploitedInWild == true) Color.Red else Color.Gray, CircleShape)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(cve.cveId, style = StitchTokens.MonoCode, color = StitchTokens.Primary)
                        }
                        Text(cve.title, style = StitchTokens.BodyMedium, color = Color.LightGray)
                        Text(
                            text = "BUG_CLASS: ${cve.bugClass}",
                            style = StitchTokens.LabelSmall.copy(fontSize = 10.sp),
                            color = StitchTokens.TextSecondary
                        )
                    }
                    Divider(color = Color.White.copy(alpha = 0.1f))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = StitchTokens.Primary)
            }
        }
    )
}

@Composable
private fun StatItem(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = StitchTokens.LabelSmall.copy(fontSize = 9.sp), color = StitchTokens.TextSecondary)
        Text(value, style = StitchTokens.MonoCode.copy(fontSize = 18.sp), color = color)
    }
}
