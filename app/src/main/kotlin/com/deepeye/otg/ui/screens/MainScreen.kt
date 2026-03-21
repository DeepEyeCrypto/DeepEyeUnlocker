package com.deepeye.otg.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.domain.engine.AvailabilityEngine
import com.deepeye.otg.domain.models.*
import com.deepeye.otg.ui.components.*
import com.deepeye.otg.ui.theme.StitchTokens
import com.deepeye.otg.viewmodel.research.CveDashboardViewModel
import com.deepeye.otg.intelligence.vulndb.RiskLevel
import com.deepeye.otg.intelligence.vulndb.SplStatus
import com.deepeye.otg.usb.UsbLifecycleState
import androidx.hilt.navigation.compose.hiltViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.deepeye.otg.ui.device.DeviceSupportScreen
import com.deepeye.otg.ui.screens.CveDashboardScreen
import com.deepeye.otg.ui.screens.FuzzDashboardScreen
import com.deepeye.otg.ui.screens.HidResearchScreen
import com.deepeye.otg.ui.screens.Iphone15ResearchScreen
import com.deepeye.otg.viewmodel.UsbViewModel

@Composable
fun MainScreen(
    viewModel: UsbViewModel,
    cveViewModel: CveDashboardViewModel = hiltViewModel(),
    fuzzViewModel: com.deepeye.otg.viewmodel.research.FuzzDashboardViewModel = hiltViewModel(),
    hidViewModel: com.deepeye.otg.viewmodel.research.HidResearchViewModel = hiltViewModel(),
    onRemoteShare: () -> Unit
) {
    val lifecycleState by viewModel.lifecycleState.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val selectedKey by viewModel.selectedDeviceKey.collectAsState()
    val sessionState by viewModel.domainSessionState.collectAsState()
    val currentNav by viewModel.currentNav.collectAsState()
    val perfMode by viewModel.performanceMode.collectAsState()
    val hazeState = remember { dev.chrisbanes.haze.HazeState() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(StitchTokens.Semantic.BackgroundBase, StitchTokens.Semantic.BackgroundElevated)
                )
            )
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // V3.0 Command Rail
            MissionNavigationRail(viewModel)

            Column(modifier = Modifier.weight(1f)) {
                // Consolidated Mission Top Bar (Stage 200.2)
                if (currentNav != NavTarget.SETTINGS) {
                    MissionTopBar(
                        viewModel = viewModel,
                        sessions = sessions,
                        selectedKey = selectedKey,
                        onSelect = { viewModel.selectDevice(it) },
                        onRemoteShare = onRemoteShare
                    )
                }

                // Debug Overlay (Top Layer)
                DebugOverlayPanel(viewModel)

                // Dynamic Layer: Changes between Disconnected and Active modes
                AnimatedContent(
                    targetState = currentNav,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        (fadeIn(tween(400)) + scaleIn(initialScale = 0.95f)).togetherWith(fadeOut(tween(300)) + scaleOut(targetScale = 1.05f))
                    },
                    label = "NavTransition"
                ) { target ->
                    when (target) {
                        NavTarget.DASHBOARD -> {
                            TargetDashboardScreen(viewModel, hazeState)
                        }
                        NavTarget.DEVICE_SUPPORT -> DeviceSupportScreen()
                        NavTarget.LAB_HOME -> ForensicLabScreen(viewModel, hazeState, perfMode)
                        NavTarget.VAULT -> VaultScreen(onBack = { viewModel.setNav(NavTarget.LAB_HOME) })
                        NavTarget.FILE_EXPLORER -> FileExplorerScreen(viewModel)
                        NavTarget.IPHONE_15_RESEARCH -> Iphone15ResearchScreen(viewModel)
                        NavTarget.TERMINAL -> TerminalScreen(viewModel)
                        NavTarget.CVE_INTELLIGENCE -> CveDashboardScreen(
                            viewModel = cveViewModel,
                            onNavigateBack = { viewModel.setNav(NavTarget.LAB_HOME) }
                        )
                        NavTarget.FUZZ_DASHBOARD -> FuzzDashboardScreen(
                            viewModel = fuzzViewModel,
                            onNavigateBack = { viewModel.setNav(NavTarget.LAB_HOME) }
                        )
                        NavTarget.HID_RESEARCH -> HidResearchScreen(
                            viewModel = hidViewModel,
                            onNavigateBack = { viewModel.setNav(NavTarget.LAB_HOME) }
                        )
                        NavTarget.STORAGE -> StorageScreen()
                        NavTarget.SETTINGS -> SettingsScreen(viewModel)
                        NavTarget.IMEI_REPAIR -> {
                            val aiAnalysis by viewModel.aiAnalysis.collectAsState()
                            val aiIsProcessing by viewModel.aiIsProcessing.collectAsState()
                            val ci1 by viewModel.currentImei1.collectAsState()
                            val ci2 by viewModel.currentImei2.collectAsState()
                            
                            ImeiRepairScreen(
                                onRepair = { i1, i2 -> viewModel.performImeiRepair(i1, i2) },
                                onRead = { viewModel.readImei() },
                                currentImei1 = ci1,
                                currentImei2 = ci2,
                                hazeState = hazeState,
                                perfMode = perfMode,
                                aiAnalysis = aiAnalysis,
                                isAiProcessing = aiIsProcessing
                            )
                        }
                        NavTarget.MISSION_HUB -> {
                            com.deepeye.otg.ui.gsmg.BypassScreen()
                        }
                        else -> {
                            DisconnectedView(hazeState)
                        }
                    }
                }
            }
        }

        // Remote Relay FAB
        FloatingActionButton(
            onClick = onRemoteShare,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 32.dp, end = 24.dp)
                .size(64.dp),
            containerColor = StitchTokens.Primary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Default.CloudSync, "Remote Share")
        }
    }
}

@Composable
private fun DisconnectedView(hazeState: HazeState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(hazeState),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glowing USB Icon (Pulse Logic)
        val infiniteTransition = rememberInfiniteTransition(label = "iconPulse")
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
            label = "glowScale"
        )

        Box(contentAlignment = Alignment.Center) {
            // Glow halo
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(StitchTokens.Primary.copy(alpha = 0.15f * glowAlpha), CircleShape)
                    .border(1.dp, StitchTokens.Primary.copy(alpha = 0.2f), CircleShape)
            )
            Icon(
                imageVector = Icons.Default.Usb,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = StitchTokens.Primary.copy(alpha = 0.8f)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "Connect a device via OTG",
            style = StitchTokens.TitleLarge,
            color = StitchTokens.TextPrimary
        )
        Text(
            text = "Supports MTK BROM • EDL • Fastboot • ADB • Odin",
            style = StitchTokens.BodyMedium,
            color = StitchTokens.TextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(60.dp))
        
        // Status Chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(Icons.Default.CheckCircle, "USB Host Mode ✓", Color(0xFF4ADE80))
            StatusChip(Icons.Default.Info, "Root Optional", StitchTokens.Primary)
            StatusChip(Icons.Default.Cable, "OTG Cable Required", Color(0xFFFBBF24))
        }
    }
}

@Composable
private fun ActiveSessionView(
    state: UsbLifecycleState.Connected,
    sessionState: SessionState,
    viewModel: UsbViewModel,
    hazeState: HazeState,
    perfMode: Boolean
) {
    val modeAccent = accentColorForMode(sessionState.deviceMode)
    val userRole by viewModel.currentUserPolicyTier.collectAsState()
    val isSplitActive by viewModel.splitViewActive.collectAsState()

    if (isSplitActive) {
        Column(Modifier.fillMaxSize()) {
            // Top Window: Operation Catalog
            Box(Modifier.weight(0.55f).hazeSource(hazeState)) {
                OperationCatalog(
                    state = state,
                    sessionState = sessionState,
                    viewModel = viewModel,
                    hazeState = hazeState,
                    perfMode = perfMode,
                    modeAccent = modeAccent,
                    userRole = userRole
                )
            }
            
            // Divider
            Box(Modifier.fillMaxWidth().height(1.dp).background(modeAccent.copy(alpha = 0.2f)))
            
            // Bottom Window: Forensic Intel & Logs
            Box(Modifier.weight(0.45f).background(StitchTokens.BackgroundDark)) {
                ForensicWorkspace(viewModel, modeAccent)
            }
        }
    } else {
        OperationCatalog(
            state = state,
            sessionState = sessionState,
            viewModel = viewModel,
            hazeState = hazeState,
            perfMode = perfMode,
            modeAccent = modeAccent,
            userRole = userRole
        )
    }
}

@Composable
private fun OperationCatalog(
    state: UsbLifecycleState.Connected,
    sessionState: SessionState,
    viewModel: UsbViewModel,
    hazeState: HazeState,
    perfMode: Boolean,
    modeAccent: Color,
    userRole: PolicyTier
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 160.dp, bottom = 120.dp)
    ) {
        // 1. Mode Status Header
        item {
            GlassCard(
                hazeState = hazeState,
                performanceMode = perfMode,
                accentColor = modeAccent,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(modeAccent, CircleShape))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(state.deviceName, style = StitchTokens.DisplayLarge.copy(fontSize = 24.sp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "VID: ${state.vendorId} | PID: ${state.productId} | MODE: ${sessionState.deviceMode}",
                        style = StitchTokens.MonoCode,
                        color = modeAccent
                    )
                }
            }
        }

        // 2. Feature Groups (Mode + Policy Sensitive)
        DeepEyeCatalogs.FEATURE_GROUPS.forEach { group ->
            item {
                Text(
                    text = group.title.uppercase(),
                    style = StitchTokens.LabelSmall,
                    color = StitchTokens.TextSecondary,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            items(group.operations.chunked(2)) { pair ->
                Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    pair.forEach { op ->
                        val availability = AvailabilityEngine.availabilityFor(
                            operation = op,
                            sessionState = sessionState,
                            userRole = userRole
                        )
                        FeatureActionCard(
                            op = op,
                            availability = availability,
                            accent = modeAccent,
                            hazeState = hazeState,
                            perfMode = perfMode,
                            modifier = Modifier.weight(1f),
                            onRun = { viewModel.queueOperation(op.id) }
                        )
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ForensicWorkspace(viewModel: UsbViewModel, accent: Color) {
    val logs by viewModel.logs.collectAsState()
    val aiAnalysis by viewModel.aiAnalysis.collectAsState()
    val exposureReport by viewModel.exposureReport.collectAsState()
    
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("FORENSIC WORKSPACE", style = StitchTokens.LabelSmall, color = accent)
            Text(
                text = "EXPORT REPORT",
                style = StitchTokens.LabelSmall,
                color = StitchTokens.Primary,
                modifier = Modifier.clickable { viewModel.generateForensicPdf() }
            )
        }
        Spacer(Modifier.height(12.dp))
        
        // AI Intel Snippet
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassCard(hazeState = null, modifier = Modifier.weight(1f).height(100.dp), accentColor = accent.copy(0.2f)) {
                Column(Modifier.padding(8.dp)) {
                    Text("AI INTEL", style = StitchTokens.MonoCode.copy(fontSize = 9.sp), color = accent)
                    Text(
                        text = aiAnalysis.ifEmpty { "Waiting for session telemetry..." },
                        style = StitchTokens.BodyMedium.copy(fontSize = 11.sp),
                        color = Color.LightGray,
                        maxLines = 4
                    )
                }
            }
            
            exposureReport?.let { report ->
                val riskColor = when (report.overallRiskLevel) {
                    RiskLevel.CRITICAL -> Color(0xFFFF1744)
                    RiskLevel.HIGH -> Color(0xFFFF9100)
                    RiskLevel.MEDIUM -> Color(0xFFFFD600)
                    RiskLevel.LOW -> Color(0xFF4ADE80)
                    else -> Color.Gray
                }
                
                GlassCard(hazeState = null, modifier = Modifier.width(140.dp).height(100.dp), accentColor = riskColor.copy(0.2f)) {
                    Column(Modifier.padding(8.dp)) {
                        Text("VULN_INTEL", style = StitchTokens.MonoCode.copy(fontSize = 9.sp), color = riskColor)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = report.overallRiskLevel.name,
                            style = StitchTokens.TitleLarge.copy(fontSize = 14.sp),
                            color = riskColor
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Exposures: ${report.exposedCves.size}",
                            style = StitchTokens.BodyMedium.copy(fontSize = 10.sp),
                            color = Color.LightGray
                        )
                        if (report.exposedCves.any { it.cisaKev }) {
                            Text(
                                text = "⚠️ CISA KEV",
                                style = StitchTokens.LabelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = Color(0xFFFF1744)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        // Terminal Logs
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(0.3f))) {
            LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
                items(logs) { log ->
                    Text(
                        text = "> ${log.message}",
                        style = StitchTokens.MonoCode.copy(fontSize = 10.sp),
                        color = when (log.type) {
                            "ERROR" -> Color(0xFFF87171)
                            "SUCCESS" -> Color(0xFF4ADE80)
                            else -> StitchTokens.TextMono
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureActionCard(
    op: DeepEyeOperation,
    availability: OperationAvailability,
    accent: Color,
    hazeState: HazeState,
    perfMode: Boolean,
    modifier: Modifier,
    onRun: () -> Unit
) {
    GlassCard(
        hazeState = hazeState,
        performanceMode = perfMode,
        modifier = modifier.height(140.dp).padding(horizontal = 4.dp),
        onClick = onRun
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(op.label, style = StitchTokens.TitleLarge.copy(fontSize = 14.sp))
                Text(
                    text = if (availability.enabled) op.description else availability.reason ?: "Locked",
                    style = StitchTokens.BodyMedium.copy(fontSize = 10.sp),
                    color = if (availability.enabled) StitchTokens.TextSecondary else Color(0xFFFCA5A5),
                    maxLines = 2
                )
            }
            if (availability.enabled) {
                Text(
                    text = "EXECUTE →",
                    style = StitchTokens.LabelSmall,
                    color = accent,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun StatusChip(icon: ImageVector, label: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(StitchTokens.RadiusFull))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(StitchTokens.RadiusFull))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = color)
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = StitchTokens.LabelSmall, color = StitchTokens.TextSecondary)
    }
}

@Composable
private fun LogTailView(logs: List<com.deepeye.otg.ui.viewmodel.LogEntry>) {
    GlassCard(
        hazeState = null, // No blur for log tail to keep it sharp
        modifier = Modifier.fillMaxWidth().padding(16.dp).height(120.dp),
        cornerRadius = 12.dp
    ) {
        LazyColumn(Modifier.padding(12.dp)) {
            items(logs) { log ->
                Text(
                    text = "> ${log.message}",
                    style = StitchTokens.MonoCode,
                    color = when (log.type) {
                        "ERROR" -> Color(0xFFF87171)
                        "SUCCESS" -> Color(0xFF4ADE80)
                        else -> StitchTokens.TextMono
                    },
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ErrorOverlay(message: String, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.8f)), contentAlignment = Alignment.Center) {
        GlassCard(hazeState = null, modifier = Modifier.width(300.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Error, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text(message, textAlign = TextAlign.Center, color = Color.White)
                Spacer(Modifier.height(24.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text("ACKNOWLEDGEMENT")
                }
            }
        }
    }
}

// Accent lookup
private fun accentColorForMode(mode: DeviceMode): Color = when (mode) {
    DeviceMode.MTK_BROM -> StitchTokens.AccentBrom
    DeviceMode.ADB -> StitchTokens.AccentAdb
    DeviceMode.QC_EDL -> StitchTokens.AccentEdl
    DeviceMode.FASTBOOT -> StitchTokens.AccentFastboot
    DeviceMode.APPLE_DFU, DeviceMode.APPLE_RECOVERY, DeviceMode.APPLE_NORMAL -> StitchTokens.AccentApple
    else -> StitchTokens.Primary
}

@Composable
private fun MissionNavigationRail(viewModel: UsbViewModel) {
    val currentNav by viewModel.currentNav.collectAsState()
    
    NavigationRail(
        containerColor = Color.Transparent,
        header = {
            Icon(
                imageVector = Icons.Default.Cyclone,
                contentDescription = "DeepEye",
                tint = StitchTokens.Primary,
                modifier = Modifier.size(32.dp).padding(vertical = 16.dp)
            )
        },
        modifier = Modifier.padding(top = 24.dp)
    ) {
        MissionHub.entries.forEach { hub ->
            val isSelected = currentNav.hub == hub
            NavigationRailItem(
                selected = isSelected,
                onClick = {
                    val target = when (hub) {
                        MissionHub.COMMAND -> NavTarget.DASHBOARD
                        MissionHub.LAB -> NavTarget.LAB_HOME
                        MissionHub.BYPASS -> NavTarget.MISSION_HUB
                        MissionHub.INTEL -> NavTarget.CVE_INTELLIGENCE
                        MissionHub.ARCHIVE -> NavTarget.SETTINGS
                    }
                    viewModel.setNav(target)
                },
                icon = {
                    Icon(
                        imageVector = hub.icon,
                        contentDescription = hub.label,
                        tint = if (isSelected) Color.White else StitchTokens.TextSecondary
                    )
                },
                label = {
                    Text(
                        text = hub.label.uppercase(),
                        style = StitchTokens.LabelSmall.copy(fontSize = 9.sp),
                        color = if (isSelected) StitchTokens.Primary else StitchTokens.TextSecondary
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = StitchTokens.Primary.copy(alpha = 0.1f)
                )
            )
        }
    }
}
