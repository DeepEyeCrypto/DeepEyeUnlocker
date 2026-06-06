package com.deepeye.otg.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.ui.theme.DeepEyeType
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
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val selectedKey by viewModel.selectedDeviceKey.collectAsStateWithLifecycle()
    val exposureReport by viewModel.exposureReport.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    
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
                    state = selectedSession,
                    sessionCount = sessions.size
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
                    state = selectedSession,
                    viewModel = viewModel,
                    hazeState = hazeState
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
            logs = logs.takeLast(6),
            hazeState = hazeState,
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}

@Composable
private fun MissionStatusRow(state: UsbLifecycleState, sessionCount: Int) {
    val isScanning = state is UsbLifecycleState.DeviceDetected ||
        state is UsbLifecycleState.Connecting ||
        state is UsbLifecycleState.PermissionPending
    val status = when (state) {
        is UsbLifecycleState.Connected -> "Device Connected"
        is UsbLifecycleState.DeviceDetected,
        is UsbLifecycleState.Connecting,
        is UsbLifecycleState.PermissionPending -> "Scanning OTG..."
        is UsbLifecycleState.PermissionDenied -> "USB Permission Needed"
        is UsbLifecycleState.Error -> "Connection Error"
        is UsbLifecycleState.Dead -> "Session Lost"
        is UsbLifecycleState.NoOtgSupport -> "OTG Not Supported"
        else -> "No Device Connected"
    }
    val subtitle = when (state) {
        is UsbLifecycleState.Connected -> "${state.brand} • ${state.chipset}"
        is UsbLifecycleState.DeviceDetected -> "${state.brand} detected • requesting secure access"
        is UsbLifecycleState.Connecting -> "Establishing ${state.protocolFamily.name.uppercase()} session"
        is UsbLifecycleState.PermissionPending -> "Allow USB access to continue"
        is UsbLifecycleState.PermissionDenied -> state.deviceName
        is UsbLifecycleState.Error -> state.message
        is UsbLifecycleState.Dead -> state.reason
        is UsbLifecycleState.NoOtgSupport -> "This phone cannot act as a USB host"
        else -> "Plug device → auto-detect"
    }
    val accent = when (state) {
        is UsbLifecycleState.Connected -> state.protocolFamily.getAccentColor()
        is UsbLifecycleState.DeviceDetected -> state.protocolFamily.getAccentColor()
        is UsbLifecycleState.Connecting -> state.protocolFamily.getAccentColor()
        is UsbLifecycleState.PermissionPending -> DeepEyeColors.NEON_YELLOW
        is UsbLifecycleState.PermissionDenied,
        is UsbLifecycleState.Error,
        is UsbLifecycleState.Dead,
        is UsbLifecycleState.NoOtgSupport -> DeepEyeColors.NEON_PINK
        else -> DeepEyeColors.WHITE_MED
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Connected Devices",
                style = DeepEyeType.CAPTION.copy(fontSize = 11.sp).copy(fontSize = 10.sp),
                color = DeepEyeColors.WHITE_MED
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulsingStatusDot(color = accent, active = isScanning || state is UsbLifecycleState.Connected)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = status,
                    style = DeepEyeType.SUBHEADER.copy(fontSize = 20.sp).copy(letterSpacing = 0.2.sp, fontSize = 18.sp),
                    color = if (state is UsbLifecycleState.Connected || isScanning) accent else DeepEyeColors.WHITE_HIGH
                )
            }
            Text(
                text = subtitle,
                style = DeepEyeType.BODY.copy(fontSize = 14.sp).copy(fontSize = 12.sp),
                color = DeepEyeColors.WHITE_MED,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        StatusPill(
            text = if (sessionCount == 0) "IDLE" else "$sessionCount LIVE"
        )
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
                style = DeepEyeType.CAPTION.copy(fontSize = 11.sp),
                color = DeepEyeColors.WHITE_MED
            )
            Text(
                text = status,
                style = DeepEyeType.SUBHEADER.copy(fontSize = 20.sp).copy(letterSpacing = 2.sp),
                color = if (status.startsWith("TARGET")) DeepEyeColors.NEON_GREEN else DeepEyeColors.WHITE_HIGH
            )
        }
        
        Badge(
            containerColor = DeepEyeColors.NEON_PURPLE.copy(alpha = 0.1f),
            contentColor = DeepEyeColors.NEON_PURPLE
        ) {
            Text(
                text = "$sessionCount ACTIVE SESSIONS",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = DeepEyeType.CAPTION.copy(fontSize = 11.sp)
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
                val protocolColor = when (state) {
                    is UsbLifecycleState.Connected,
                    is UsbLifecycleState.DeviceDetected,
                    is UsbLifecycleState.Connecting -> protocol.getAccentColor()
                    is UsbLifecycleState.PermissionPending -> DeepEyeColors.NEON_YELLOW
                    is UsbLifecycleState.PermissionDenied,
                    is UsbLifecycleState.Error,
                    is UsbLifecycleState.Dead,
                    is UsbLifecycleState.NoOtgSupport -> DeepEyeColors.NEON_PINK
                    else -> DeepEyeColors.WHITE_MED
                }
                val haloAlpha by rememberInfiniteTransition(label = "heroPulse").animateFloat(
                    initialValue = 0.35f,
                    targetValue = 0.85f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1400),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "heroAlpha"
                )
                
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(protocolColor.copy(alpha = 0.14f * haloAlpha))
                )
                Icon(
                    imageVector = when {
                        isConnected -> Icons.Default.PhoneAndroid
                        state is UsbLifecycleState.PermissionDenied || state is UsbLifecycleState.Error || state is UsbLifecycleState.Dead -> Icons.Default.UsbOff
                        else -> Icons.Default.Usb
                    },
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = if (isConnected || state is UsbLifecycleState.DeviceDetected || state is UsbLifecycleState.Connecting) {
                        protocolColor
                    } else {
                        DeepEyeColors.WHITE_MED
                    }
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
                        text = name,
                        style = DeepEyeType.HEADER.copy(fontSize = 32.sp).copy(fontSize = 22.sp, letterSpacing = 0.sp),
                        color = DeepEyeColors.WHITE_HIGH
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProtocolBadge(protocol)
                        Spacer(Modifier.width(12.dp))
                        ConfidenceBadge(confidence)
                    }
                    Text(
                        text = listOf(state.brand, state.chipset, "SB ${state.secureBootStatus}")
                            .filter { it.isNotBlank() }
                            .joinToString(" • "),
                        style = DeepEyeType.BODY.copy(fontSize = 14.sp).copy(fontSize = 12.sp),
                        color = DeepEyeColors.WHITE_MED,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else if (state is UsbLifecycleState.DeviceDetected) {
                    Text(
                        text = state.brand,
                        style = DeepEyeType.SUBHEADER.copy(fontSize = 20.sp).copy(fontSize = 20.sp),
                        color = DeepEyeColors.WHITE_HIGH
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProtocolBadge(state.protocolFamily)
                        Spacer(Modifier.width(12.dp))
                        ConfidenceBadge(state.confidence)
                    }
                    Text(
                        text = "${state.chipset} • waiting for permission",
                        style = DeepEyeType.BODY.copy(fontSize = 14.sp).copy(fontSize = 12.sp),
                        color = DeepEyeColors.WHITE_MED,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    Text(
                        text = when (state) {
                            is UsbLifecycleState.Connecting -> "Scanning OTG..."
                            is UsbLifecycleState.PermissionPending -> "USB Permission Needed"
                            is UsbLifecycleState.PermissionDenied -> "Permission Denied"
                            is UsbLifecycleState.Error -> "Connection Error"
                            is UsbLifecycleState.Dead -> "Session Lost"
                            is UsbLifecycleState.NoOtgSupport -> "OTG Not Supported"
                            else -> "No Device Connected"
                        },
                        style = DeepEyeType.SUBHEADER.copy(fontSize = 20.sp).copy(letterSpacing = 0.sp, fontSize = 20.sp),
                        color = DeepEyeColors.WHITE_HIGH
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when (state) {
                            is UsbLifecycleState.Connecting -> "Establishing secure transport"
                            is UsbLifecycleState.PermissionPending -> "Approve the Android USB dialog"
                            is UsbLifecycleState.PermissionDenied -> state.deviceName
                            is UsbLifecycleState.Error -> state.message
                            is UsbLifecycleState.Dead -> state.reason
                            is UsbLifecycleState.NoOtgSupport -> "Use a device with USB host capability"
                            else -> "Plug device → auto-detect"
                        },
                        style = DeepEyeType.BODY.copy(fontSize = 14.sp).copy(fontSize = 12.sp),
                        color = DeepEyeColors.WHITE_MED
                    )
                }
            }
        }
    }
}

@Composable
private fun TriageActionGrid(
    state: UsbLifecycleState,
    viewModel: UsbViewModel,
    hazeState: dev.chrisbanes.haze.HazeState
) {
    val isEnabled = state is UsbLifecycleState.Connected
    val protocol = when (state) {
        is UsbLifecycleState.Connected -> state.protocolFamily
        is UsbLifecycleState.DeviceDetected -> state.protocolFamily
        is UsbLifecycleState.Connecting -> state.protocolFamily
        else -> ProtocolFamily.UNKNOWN
    }
    val deviceLabel = when (state) {
        is UsbLifecycleState.Connected -> listOf(state.brand, state.deviceName, state.chipset)
        is UsbLifecycleState.DeviceDetected -> listOf(state.brand, state.chipset)
        is UsbLifecycleState.Connecting -> listOf("Linking", state.protocolFamily.name.uppercase())
        else -> emptyList()
    }.filter { it.isNotBlank() }.joinToString(" • ")
    val accent = when (state) {
        is UsbLifecycleState.Connected -> state.protocolFamily.getAccentColor()
        is UsbLifecycleState.DeviceDetected -> state.protocolFamily.getAccentColor()
        is UsbLifecycleState.Connecting -> state.protocolFamily.getAccentColor()
        else -> DeepEyeColors.NEON_BLUE
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        hazeState = hazeState,
        accentColor = accent
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionLabel(text = "OPERATION CONSOLE")
            Spacer(modifier = Modifier.height(12.dp))

            if (protocol != ProtocolFamily.UNKNOWN) {
                ProtocolBadge(protocol)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = if (deviceLabel.isBlank()) "Connected Devices" else deviceLabel,
                color = DeepEyeColors.WHITE_HIGH,
                style = DeepEyeType.BODY.copy(fontSize = 14.sp).copy(fontSize = 13.sp)
            )
            Text(
                text = if (isEnabled) {
                    "Live partition and recovery actions are ready."
                } else {
                    "Plug device → grant USB permission → actions unlock automatically."
                },
                color = DeepEyeColors.WHITE_MED,
                style = DeepEyeType.BODY.copy(fontSize = 14.sp).copy(fontSize = 11.sp),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionLabel(text = "PARTITION OPERATIONS")
            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RustActionButton(
                    text = "Read Info",
                    accentColor = DeepEyeColors.NEON_BLUE,
                    enabled = isEnabled,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.queueOperation(DeepEyeOperation.DEEP_DEVICE_INFO) }
                )
                RustActionButton(
                    text = "FRP Erase",
                    accentColor = DeepEyeColors.NEON_PINK,
                    enabled = isEnabled,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.queueOperation(DeepEyeOperation.ERASE_FRP) }
                )
                RustActionButton(
                    text = "Safe Wipe",
                    accentColor = DeepEyeColors.NEON_ORANGE,
                    enabled = isEnabled,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.queueOperation(DeepEyeOperation.SAFE_WIPE) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RustActionButton(
                    text = "Unlock BL",
                    accentColor = DeepEyeColors.NEON_GREEN,
                    enabled = isEnabled,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.queueOperation(DeepEyeOperation.UNLOCK_BOOTLOADER) }
                )
                RustActionButton(
                    text = "Read IMEI",
                    accentColor = DeepEyeColors.NEON_CYAN,
                    enabled = isEnabled,
                    modifier = Modifier.weight(1f),
                    onClick = viewModel::readImei
                )
            }
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
                    Icon(Icons.Default.DeveloperBoard, null, tint = DeepEyeColors.WHITE_MED, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("DEVICE DETAILS", style = DeepEyeType.CAPTION.copy(fontSize = 11.sp).copy(fontSize = 10.sp), color = DeepEyeColors.WHITE_MED)
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = DeepEyeColors.WHITE_MED
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
                    Text("No hardware context available", style = DeepEyeType.BODY.copy(fontSize = 14.sp), color = DeepEyeColors.WHITE_MED)
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
        Text(key, style = DeepEyeType.MONO.copy(fontSize = 12.sp).copy(fontSize = 11.sp), color = DeepEyeColors.WHITE_MED)
        Text(value, style = DeepEyeType.MONO.copy(fontSize = 12.sp).copy(fontSize = 11.sp), color = DeepEyeColors.WHITE_HIGH)
    }
}

@Composable
private fun TerminalMiniPreview(
    logs: List<com.deepeye.otg.ui.viewmodel.LogEntry>,
    hazeState: dev.chrisbanes.haze.HazeState,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        hazeState = hazeState,
        accentColor = Color.White.copy(alpha = 0.08f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            SectionLabel(text = "FRP LOG")
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DeepEyeColors.BG_VOID.copy(alpha = 0.96f))
                    .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (logs.isEmpty()) {
                        item {
                            Text(
                                text = "> Awaiting device telemetry...",
                                style = DeepEyeType.MONO.copy(fontSize = 12.sp).copy(fontSize = 10.sp),
                                color = DeepEyeColors.WHITE_MED
                            )
                        }
                    }

                    items(logs) { log ->
                        Text(
                            text = "> ${log.message}",
                            style = DeepEyeType.MONO.copy(fontSize = 12.sp).copy(fontSize = 10.sp, letterSpacing = 0.4.sp),
                            color = when (log.type) {
                                "ERROR" -> DeepEyeColors.NEON_PINK
                                "SUCCESS" -> DeepEyeColors.NEON_GREEN
                                else -> DeepEyeColors.WHITE_MED
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = DeepEyeColors.WHITE_MED,
        style = DeepEyeType.CAPTION.copy(fontSize = 11.sp).copy(fontSize = 9.sp, letterSpacing = 1.1.sp)
    )
}

@Composable
private fun StatusPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(DeepEyeColors.BG_SURFACE.copy(alpha = 0.9f))
            .border(1.dp, DeepEyeColors.WHITE_LOW.copy(0.3f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = DeepEyeType.CAPTION.copy(fontSize = 11.sp).copy(fontSize = 9.sp),
            color = DeepEyeColors.WHITE_MED
        )
    }
}

@Composable
private fun PulsingStatusDot(color: Color, active: Boolean) {
    val animatedAlpha by rememberInfiniteTransition(label = "statusDot").animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "statusDotAlpha"
    )
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = if (active) animatedAlpha else 0.8f))
    )
}

@Composable
private fun RustActionButton(
    text: String,
    accentColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) accentColor.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f))
            .border(
                1.dp,
                if (enabled) accentColor.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(10.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = DeepEyeType.CAPTION.copy(fontSize = 11.sp).copy(fontSize = 10.sp, letterSpacing = 0.6.sp),
            color = if (enabled) accentColor else DeepEyeColors.WHITE_MED
        )
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
                            style = DeepEyeType.CAPTION.copy(fontSize = 11.sp), 
                            color = DeepEyeColors.WHITE_MED
                        )
                        Text(
                            "RISK_LEVEL: ${report.overallRiskLevel}", 
                            style = DeepEyeType.SUBHEADER.copy(fontSize = 20.sp).copy(fontSize = 16.sp),
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
                            style = DeepEyeType.CAPTION.copy(fontSize = 11.sp).copy(fontWeight = FontWeight.Bold)
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
                    color = if (report.exposedCves.isNotEmpty()) riskColor else DeepEyeColors.WHITE_MED,
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
                        style = DeepEyeType.CAPTION.copy(fontSize = 11.sp).copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
        containerColor = DeepEyeColors.BG_VOID,
        title = {
            Text("EXPOSED_VULNERABILITIES", style = DeepEyeType.SUBHEADER.copy(fontSize = 20.sp), color = Color.White)
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
                            Text(cve.cveId, style = DeepEyeType.MONO.copy(fontSize = 12.sp), color = DeepEyeColors.NEON_PURPLE)
                        }
                        Text(cve.title, style = DeepEyeType.BODY.copy(fontSize = 14.sp), color = Color.LightGray)
                        Text(
                            text = "BUG_CLASS: ${cve.bugClass}",
                            style = DeepEyeType.CAPTION.copy(fontSize = 11.sp).copy(fontSize = 10.sp),
                            color = DeepEyeColors.WHITE_MED
                        )
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = DeepEyeColors.NEON_PURPLE)
            }
        }
    )
}

@Composable
private fun StatItem(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = DeepEyeType.CAPTION.copy(fontSize = 11.sp).copy(fontSize = 9.sp), color = DeepEyeColors.WHITE_MED)
        Text(value, style = DeepEyeType.MONO.copy(fontSize = 12.sp).copy(fontSize = 18.sp), color = color)
    }
}
