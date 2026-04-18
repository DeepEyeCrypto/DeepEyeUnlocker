package com.deepeye.otg.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.ui.theme.DeepEyeType
import com.deepeye.otg.viewmodel.research.CveDashboardViewModel
import com.deepeye.otg.intelligence.vulndb.RiskLevel
import com.deepeye.otg.intelligence.vulndb.SplStatus
import com.deepeye.otg.usb.UsbLifecycleState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.deepeye.otg.ui.device.DeviceSupportScreen
import com.deepeye.otg.ui.screens.CveDashboardScreen
import com.deepeye.otg.ui.screens.EdlConsole
import com.deepeye.otg.ui.screens.FuzzDashboardScreen
import com.deepeye.otg.ui.screens.HidResearchScreen
import com.deepeye.otg.ui.screens.Iphone15ResearchScreen
import com.deepeye.otg.ui.screens.LogScreen
import com.deepeye.otg.ui.screens.UnlockScreen
import com.deepeye.otg.ui.screens.XiaomiFlashScreen
import com.deepeye.otg.ui.screens.MtkUnlockScreen
import com.deepeye.otg.ui.screens.MtkExploitScreen
import com.deepeye.otg.ui.screens.XiaomiExploitScreen
import com.deepeye.otg.viewmodel.UsbViewModel

// Mapping from SpotlightNavDestination to NavTarget
private fun spotlightToNavTarget(dest: com.deepeye.otg.ui.components.SpotlightNavDestination): NavTarget = when (dest) {
    com.deepeye.otg.ui.components.SpotlightNavDestination.DASHBOARD -> NavTarget.DASHBOARD
    com.deepeye.otg.ui.components.SpotlightNavDestination.DEVICE -> NavTarget.DEVICES
    com.deepeye.otg.ui.components.SpotlightNavDestination.LAB -> NavTarget.LAB_HOME
    com.deepeye.otg.ui.components.SpotlightNavDestination.BYPASS -> NavTarget.MISSION_HUB
    com.deepeye.otg.ui.components.SpotlightNavDestination.TOOL -> NavTarget.LAB_HOME
    com.deepeye.otg.ui.components.SpotlightNavDestination.ARCHIVE -> NavTarget.BYPASS_HISTORY
    com.deepeye.otg.ui.components.SpotlightNavDestination.SHARE -> NavTarget.REMOTE_SHARE
    com.deepeye.otg.ui.components.SpotlightNavDestination.PROFILE -> NavTarget.SETTINGS
}

// Route mapping for GradientBottomBar
private fun spotlightToRoute(dest: com.deepeye.otg.ui.components.SpotlightNavDestination): String = when (dest) {
    com.deepeye.otg.ui.components.SpotlightNavDestination.DASHBOARD -> "home"
    com.deepeye.otg.ui.components.SpotlightNavDestination.DEVICE -> "devices"
    com.deepeye.otg.ui.components.SpotlightNavDestination.LAB -> "network"
    com.deepeye.otg.ui.components.SpotlightNavDestination.BYPASS -> "bypass"
    com.deepeye.otg.ui.components.SpotlightNavDestination.TOOL -> "logs"
    com.deepeye.otg.ui.components.SpotlightNavDestination.PROFILE -> "settings"
    else -> "home" // Fallback for removed items (ARCHIVE, SHARE)
}

private fun routeToSpotlight(route: String): com.deepeye.otg.ui.components.SpotlightNavDestination = when (route) {
    "home" -> com.deepeye.otg.ui.components.SpotlightNavDestination.DASHBOARD
    "devices" -> com.deepeye.otg.ui.components.SpotlightNavDestination.DEVICE
    "bypass" -> com.deepeye.otg.ui.components.SpotlightNavDestination.BYPASS
    "network" -> com.deepeye.otg.ui.components.SpotlightNavDestination.LAB
    "logs" -> com.deepeye.otg.ui.components.SpotlightNavDestination.TOOL
    "settings" -> com.deepeye.otg.ui.components.SpotlightNavDestination.PROFILE
    else -> com.deepeye.otg.ui.components.SpotlightNavDestination.DASHBOARD
}

@Composable
fun MainScreen(
    viewModel: UsbViewModel,
    cveViewModel: CveDashboardViewModel = hiltViewModel(),
    fuzzViewModel: com.deepeye.otg.viewmodel.research.FuzzDashboardViewModel = hiltViewModel(),
    hidViewModel: com.deepeye.otg.viewmodel.research.HidResearchViewModel = hiltViewModel(),
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val selectedKey by viewModel.selectedDeviceKey.collectAsStateWithLifecycle()
    val currentNav by viewModel.currentNav.collectAsStateWithLifecycle()
    val perfMode by viewModel.performanceMode.collectAsStateWithLifecycle()
    val hazeState = remember { dev.chrisbanes.haze.HazeState() }
    
    // Spotlight bottom bar state
    var spotlightDestination by remember { 
        mutableStateOf(
            when (currentNav) {
                NavTarget.DASHBOARD -> com.deepeye.otg.ui.components.SpotlightNavDestination.DASHBOARD
                NavTarget.DEVICES, NavTarget.DEVICE_SUPPORT, NavTarget.EDL_CONSOLE, NavTarget.XIAOMI_FLASH, NavTarget.MTK_UNLOCK, NavTarget.MTK_EXPLOIT, NavTarget.XIAOMI_EXPLOIT -> com.deepeye.otg.ui.components.SpotlightNavDestination.DEVICE
                NavTarget.LAB_HOME, NavTarget.IMEI_REPAIR, NavTarget.STORAGE -> com.deepeye.otg.ui.components.SpotlightNavDestination.LAB
                NavTarget.MISSION_HUB, NavTarget.UNLOCK_SCREEN -> com.deepeye.otg.ui.components.SpotlightNavDestination.BYPASS
                NavTarget.SETTINGS, NavTarget.TERMINAL, NavTarget.VAULT, NavTarget.LOG_SCREEN -> com.deepeye.otg.ui.components.SpotlightNavDestination.PROFILE
                NavTarget.REMOTE_SHARE -> com.deepeye.otg.ui.components.SpotlightNavDestination.SHARE
                else -> com.deepeye.otg.ui.components.SpotlightNavDestination.DASHBOARD
            }
        )
    }
    
    // Update spotlight destination when currentNav changes externally
    LaunchedEffect(currentNav) {
        spotlightDestination = when (currentNav) {
            NavTarget.DASHBOARD -> com.deepeye.otg.ui.components.SpotlightNavDestination.DASHBOARD
            NavTarget.DEVICES, NavTarget.DEVICE_SUPPORT, NavTarget.EDL_CONSOLE, NavTarget.XIAOMI_FLASH, NavTarget.MTK_UNLOCK, NavTarget.MTK_EXPLOIT, NavTarget.XIAOMI_EXPLOIT -> com.deepeye.otg.ui.components.SpotlightNavDestination.DEVICE
            NavTarget.LAB_HOME, NavTarget.IMEI_REPAIR, NavTarget.STORAGE, NavTarget.FILE_EXPLORER -> com.deepeye.otg.ui.components.SpotlightNavDestination.LAB
            NavTarget.MISSION_HUB, NavTarget.UNLOCK_SCREEN -> com.deepeye.otg.ui.components.SpotlightNavDestination.BYPASS
            NavTarget.SETTINGS, NavTarget.TERMINAL, NavTarget.VAULT, NavTarget.LOG_SCREEN, NavTarget.BYPASS_HISTORY -> com.deepeye.otg.ui.components.SpotlightNavDestination.PROFILE
            NavTarget.REMOTE_SHARE -> com.deepeye.otg.ui.components.SpotlightNavDestination.SHARE
            else -> com.deepeye.otg.ui.components.SpotlightNavDestination.DASHBOARD
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepEyeColors.BG_VOID, DeepEyeColors.BG_SURFACE)
                )
            )
    ) {
        val compactLayout = maxWidth < 700.dp
        val fabBottomPadding = if (compactLayout) 88.dp else 32.dp
        val fabSize = if (compactLayout) 56.dp else 64.dp

        if (compactLayout) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (currentNav != NavTarget.SETTINGS) {
                    MissionTopBar(
                        viewModel = viewModel,
                        sessions = sessions,
                        selectedKey = selectedKey,
                        onSelect = { viewModel.selectDevice(it) },
                        compactMode = true
                    )
                }

                DebugOverlayPanel(viewModel)

                MissionNavContent(
                    modifier = Modifier.weight(1f),
                    currentNav = currentNav,
                    viewModel = viewModel,
                    hazeState = hazeState,
                    perfMode = perfMode,
                    cveViewModel = cveViewModel,
                    fuzzViewModel = fuzzViewModel,
                    hidViewModel = hidViewModel
                )

                // Gradient Bottom Navigation Bar
                com.deepeye.otg.ui.components.GradientBottomBar(
                    currentRoute = spotlightToRoute(spotlightDestination),
                    onNavigate = { route ->
                        spotlightDestination = routeToSpotlight(route)
                        viewModel.setNav(spotlightToNavTarget(routeToSpotlight(route)))
                    }
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                MissionNavigationRail(viewModel)

                Column(modifier = Modifier.weight(1f)) {
                    if (currentNav != NavTarget.SETTINGS) {
                        MissionTopBar(
                            viewModel = viewModel,
                            sessions = sessions,
                            selectedKey = selectedKey,
                            onSelect = { viewModel.selectDevice(it) },
                            compactMode = false
                        )
                    }

                    DebugOverlayPanel(viewModel)

                    MissionNavContent(
                        modifier = Modifier.weight(1f),
                        currentNav = currentNav,
                        viewModel = viewModel,
                        hazeState = hazeState,
                        perfMode = perfMode,
                        cveViewModel = cveViewModel,
                        fuzzViewModel = fuzzViewModel,
                        hidViewModel = hidViewModel
                    )
                }
            }
        }

        // Global overlay — shown on top of EVERYTHING
        val deviceViewModel: com.deepeye.otg.viewmodel.DeviceViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        val flashPct by deviceViewModel.flashProgress.collectAsStateWithLifecycle()
        val flashStep by deviceViewModel.flashStep.collectAsStateWithLifecycle()
        
        androidx.compose.animation.AnimatedVisibility(
            visible = (flashPct?.percent ?: 0) in 1..99,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit  = fadeOut() + scaleOut(targetScale = 0.9f),
            modifier = Modifier.fillMaxSize()
        ) {
            com.deepeye.otg.ui.components.FlashProgressOverlay(percent = flashPct?.percent ?: 0, step = flashStep)
        }
    }
}

@Composable
private fun MissionNavContent(
    modifier: Modifier = Modifier,
    currentNav: NavTarget,
    viewModel: UsbViewModel,
    hazeState: HazeState,
    perfMode: Boolean,
    cveViewModel: CveDashboardViewModel,
    fuzzViewModel: com.deepeye.otg.viewmodel.research.FuzzDashboardViewModel,
    hidViewModel: com.deepeye.otg.viewmodel.research.HidResearchViewModel
) {
    AnimatedContent(
        targetState = currentNav,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(tween(400)) + scaleIn(initialScale = 0.95f)).togetherWith(
                fadeOut(tween(300)) + scaleOut(targetScale = 1.05f)
            )
        },
        label = "NavTransition"
    ) { target ->
        when (target) {
            NavTarget.DASHBOARD -> {
                TargetDashboardScreen(viewModel, hazeState)
            }
            NavTarget.DEVICES -> {
                com.deepeye.otg.ui.device.DeviceDashboardScreen(
                    onNavigateToXiaomiFlash = { viewModel.setNav(NavTarget.XIAOMI_FLASH) },
                    onNavigateToMtkUnlock = { viewModel.setNav(NavTarget.MTK_UNLOCK) }
                )
            }
            NavTarget.DEVICE_SUPPORT -> DeviceSupportScreen()
            NavTarget.EDL_CONSOLE -> EdlConsole(
                mainViewModel = viewModel,
                onBack = { viewModel.setNav(NavTarget.DASHBOARD) }
            )
            NavTarget.XIAOMI_FLASH -> XiaomiFlashScreen()
            NavTarget.MTK_UNLOCK -> MtkUnlockScreen()
            NavTarget.MTK_EXPLOIT -> MtkExploitScreen()
            NavTarget.XIAOMI_EXPLOIT -> XiaomiExploitScreen()
            NavTarget.LAB_HOME -> ForensicLabScreen(viewModel, hazeState, perfMode)
            NavTarget.VAULT -> VaultScreen(onBack = { viewModel.setNav(NavTarget.LAB_HOME) })
            NavTarget.FILE_EXPLORER -> FileExplorerScreen(viewModel)
            NavTarget.IPHONE_15_RESEARCH -> Iphone15ResearchScreen(viewModel)
            NavTarget.APPLE_PRO_TOOLS -> com.deepeye.otg.ui.apple.AppleProToolsEnhancedScreen()
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
            NavTarget.REMOTE_SHARE -> {
                val tunnelStatus by viewModel.tunnelStatus.collectAsStateWithLifecycle()
                val sessionCode by viewModel.tunnelCode.collectAsStateWithLifecycle()
                
                com.deepeye.otg.ui.RemoteShareScreen(
                    status = when (tunnelStatus) {
                        com.deepeye.otg.service.TunnelManager.TunnelStatus.ACTIVE -> "ACTIVE"
                        com.deepeye.otg.service.TunnelManager.TunnelStatus.CONNECTING -> "CONNECTING"
                        com.deepeye.otg.service.TunnelManager.TunnelStatus.FAILED -> "FAILED"
                        else -> "Ready"
                    },
                    subStatus = when (tunnelStatus) {
                        com.deepeye.otg.service.TunnelManager.TunnelStatus.ACTIVE -> "Relay active - sharing USB connection"
                        com.deepeye.otg.service.TunnelManager.TunnelStatus.CONNECTING -> "Establishing secure tunnel..."
                        com.deepeye.otg.service.TunnelManager.TunnelStatus.FAILED -> "Connection failed - retrying..."
                        else -> "Tap START RELAY to share your USB connection"
                    },
                    sessionCode = sessionCode,
                    isDeviceDetected = viewModel.connectionState.value is com.deepeye.otg.domain.models.ConnectionState.Open ||
                                       viewModel.connectionState.value is com.deepeye.otg.domain.models.ConnectionState.Ready,
                    onStartSharing = { viewModel.startFleetSharing() },
                    onConnectRemote = { code -> viewModel.joinRemoteSession(code) },
                    onBack = { 
                        viewModel.stopSharing()
                        viewModel.setNav(NavTarget.DASHBOARD) 
                    }
                )
            }
            NavTarget.SETTINGS -> SettingsScreen(viewModel)
            NavTarget.LOG_SCREEN -> LogScreen(
                mainViewModel = viewModel,
                onBack = { viewModel.setNav(NavTarget.DASHBOARD) }
            )
            NavTarget.UNLOCK_SCREEN -> UnlockScreen(
                mainViewModel = viewModel,
                onBack = { viewModel.setNav(NavTarget.DASHBOARD) }
            )
            NavTarget.BYPASS_HISTORY -> {
                com.deepeye.otg.ui.history.HistoryScreen()
            }
            NavTarget.IMEI_REPAIR -> {
                val aiAnalysis by viewModel.aiAnalysis.collectAsStateWithLifecycle()
                val aiIsProcessing by viewModel.aiIsProcessing.collectAsStateWithLifecycle()
                val ci1 by viewModel.currentImei1.collectAsStateWithLifecycle()
                val ci2 by viewModel.currentImei2.collectAsStateWithLifecycle()

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
                    .background(DeepEyeColors.NEON_PURPLE.copy(alpha = 0.15f * glowAlpha), CircleShape)
                    .border(1.dp, DeepEyeColors.NEON_PURPLE.copy(alpha = 0.2f), CircleShape)
            )
            Icon(
                imageVector = Icons.Default.Usb,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = DeepEyeColors.NEON_PURPLE.copy(alpha = 0.8f)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "Connect a device via OTG",
            style = DeepEyeType.SUBHEADER.copy(fontSize = 20.sp),
            color = DeepEyeColors.WHITE_HIGH
        )
        Text(
            text = "Supports MTK BROM • EDL • Fastboot • ADB • Odin",
            style = DeepEyeType.BODY.copy(fontSize = 14.sp),
            color = DeepEyeColors.WHITE_MED,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(60.dp))
        
        // Status Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusChip(Icons.Default.CheckCircle, "USB Host Mode ✓", Color(0xFF4ADE80))
            StatusChip(Icons.Default.Info, "Root Optional", DeepEyeColors.NEON_PURPLE)
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
    val userRole by viewModel.currentUserPolicyTier.collectAsStateWithLifecycle()
    val isSplitActive by viewModel.splitViewActive.collectAsStateWithLifecycle()

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
            Box(Modifier.weight(0.45f).background(DeepEyeColors.BG_VOID)) {
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
                        Text(state.deviceName, style = DeepEyeType.HEADER.copy(fontSize = 32.sp).copy(fontSize = 24.sp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "VID: ${state.vendorId} | PID: ${state.productId} | MODE: ${sessionState.deviceMode}",
                        style = DeepEyeType.MONO.copy(fontSize = 12.sp),
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
                    style = DeepEyeType.CAPTION.copy(fontSize = 11.sp),
                    color = DeepEyeColors.WHITE_MED,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            items(group.operations.chunked(2)) { pair ->
                Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    pair.forEach { op ->
                        val availability = com.deepeye.otg.domain.engine.AvailabilityEngine.availabilityFor(
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
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val aiAnalysis by viewModel.aiAnalysis.collectAsStateWithLifecycle()
    val exposureReport by viewModel.exposureReport.collectAsStateWithLifecycle()
    
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("FORENSIC WORKSPACE", style = DeepEyeType.CAPTION.copy(fontSize = 11.sp), color = accent)
            Text(
                text = "EXPORT REPORT",
                style = DeepEyeType.CAPTION.copy(fontSize = 11.sp),
                color = DeepEyeColors.NEON_PURPLE,
                modifier = Modifier.clickable { viewModel.generateForensicPdf() }
            )
        }
        Spacer(Modifier.height(12.dp))
        
        // AI Intel Snippet
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassCard(hazeState = null, modifier = Modifier.weight(1f).height(100.dp), accentColor = accent.copy(0.2f)) {
                Column(Modifier.padding(8.dp)) {
                    Text("AI INTEL", style = DeepEyeType.MONO.copy(fontSize = 12.sp).copy(fontSize = 9.sp), color = accent)
                    Text(
                        text = aiAnalysis.ifEmpty { "Waiting for session telemetry..." },
                        style = DeepEyeType.BODY.copy(fontSize = 14.sp).copy(fontSize = 11.sp),
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
                        Text("VULN_INTEL", style = DeepEyeType.MONO.copy(fontSize = 12.sp).copy(fontSize = 9.sp), color = riskColor)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = report.overallRiskLevel.name,
                            style = DeepEyeType.SUBHEADER.copy(fontSize = 20.sp).copy(fontSize = 14.sp),
                            color = riskColor
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Exposures: ${report.exposedCves.size}",
                            style = DeepEyeType.BODY.copy(fontSize = 14.sp).copy(fontSize = 10.sp),
                            color = Color.LightGray
                        )
                        if (report.exposedCves.any { it.cisaKev }) {
                            Text(
                                text = "⚠️ CISA KEV",
                                style = DeepEyeType.CAPTION.copy(fontSize = 11.sp).copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
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
                        style = DeepEyeType.MONO.copy(fontSize = 12.sp).copy(fontSize = 10.sp),
                        color = when (log.type) {
                            "ERROR" -> Color(0xFFF87171)
                            "SUCCESS" -> Color(0xFF4ADE80)
                            else -> DeepEyeColors.NEON_CYAN
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
                Text(op.label, style = DeepEyeType.SUBHEADER.copy(fontSize = 20.sp).copy(fontSize = 14.sp))
                Text(
                    text = if (availability.enabled) op.description else availability.reason ?: "Locked",
                    style = DeepEyeType.BODY.copy(fontSize = 14.sp).copy(fontSize = 10.sp),
                    color = if (availability.enabled) DeepEyeColors.WHITE_MED else Color(0xFFFCA5A5),
                    maxLines = 2
                )
            }
            if (availability.enabled) {
                Text(
                    text = "EXECUTE →",
                    style = DeepEyeType.CAPTION.copy(fontSize = 11.sp),
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
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = color)
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = DeepEyeType.CAPTION.copy(fontSize = 11.sp), color = DeepEyeColors.WHITE_MED)
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
                    style = DeepEyeType.MONO.copy(fontSize = 12.sp),
                    color = when (log.type) {
                        "ERROR" -> Color(0xFFF87171)
                        "SUCCESS" -> Color(0xFF4ADE80)
                        else -> DeepEyeColors.NEON_CYAN
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
    DeviceMode.MTK_BROM -> DeepEyeColors.NEON_GREEN
    DeviceMode.ADB -> DeepEyeColors.NEON_BLUE
    DeviceMode.QC_EDL -> DeepEyeColors.NEON_PURPLE
    DeviceMode.FASTBOOT -> DeepEyeColors.NEON_ORANGE
    DeviceMode.APPLE_DFU, DeviceMode.APPLE_RECOVERY, DeviceMode.APPLE_NORMAL -> DeepEyeColors.WHITE_HIGH
    else -> DeepEyeColors.NEON_PURPLE
}

@Composable
private fun MissionNavigationRail(viewModel: UsbViewModel) {
    val currentNav by viewModel.currentNav.collectAsStateWithLifecycle()
    
    NavigationRail(
        containerColor = Color.Transparent,
        header = {
            Icon(
                imageVector = Icons.Default.Cyclone,
                contentDescription = "DeepEye",
                tint = DeepEyeColors.NEON_PURPLE,
                modifier = Modifier.size(32.dp).padding(vertical = 16.dp)
            )
        },
        modifier = Modifier.padding(top = 24.dp)
    ) {
        MissionHub.entries.forEach { hub ->
            val isSelected = currentNav.hub == hub
            NavigationRailItem(
                selected = isSelected,
                onClick = { viewModel.setNav(defaultTargetForHub(hub)) },
                icon = {
                    Icon(
                        imageVector = hub.icon,
                        contentDescription = hub.label,
                        tint = if (isSelected) Color.White else DeepEyeColors.WHITE_MED
                    )
                },
                label = {
                    Text(
                        text = hub.label.uppercase(),
                        style = DeepEyeType.CAPTION.copy(fontSize = 11.sp).copy(fontSize = 9.sp),
                        color = if (isSelected) DeepEyeColors.NEON_PURPLE else DeepEyeColors.WHITE_MED
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = DeepEyeColors.NEON_PURPLE.copy(alpha = 0.1f)
                )
            )
        }
    }
}

@Composable
private fun MissionNavigationBar(viewModel: UsbViewModel) {
    val currentNav by viewModel.currentNav.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepEyeColors.BG_SURFACE.copy(alpha = 0.96f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavigationBar(
            modifier = Modifier.weight(1f),
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
        MissionHub.entries.forEach { hub ->
            val isSelected = currentNav.hub == hub

            NavigationBarItem(
                selected = isSelected,
                onClick = { viewModel.setNav(defaultTargetForHub(hub)) },
                icon = {
                    Icon(
                        imageVector = hub.icon,
                        contentDescription = hub.label
                    )
                },
                label = {
                    Text(
                        text = hub.label,
                        style = DeepEyeType.CAPTION.copy(fontSize = 11.sp).copy(fontSize = 9.sp)
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = DeepEyeColors.NEON_PURPLE,
                    indicatorColor = DeepEyeColors.NEON_PURPLE.copy(alpha = 0.12f),
                    unselectedIconColor = DeepEyeColors.WHITE_MED,
                    unselectedTextColor = DeepEyeColors.WHITE_MED
                )
            )
        }
        } // Close NavigationBar content
        
            Row(
                modifier = Modifier.padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.deepeye.otg.ui.components.BottomActionButtons(
                    compact = true,
                    onBug = { viewModel.toggleDebugPanel() },
                    onRemoteShare = { viewModel.setNav(com.deepeye.otg.ui.screens.NavTarget.REMOTE_SHARE) },
                    onSettings = { viewModel.setNav(com.deepeye.otg.ui.screens.NavTarget.SETTINGS) }
                )
            }
    }
}

private fun defaultTargetForHub(hub: MissionHub): NavTarget = when (hub) {
    MissionHub.COMMAND -> NavTarget.DASHBOARD
    MissionHub.LAB -> NavTarget.LAB_HOME
    MissionHub.BYPASS -> NavTarget.MISSION_HUB
    MissionHub.INTEL -> NavTarget.CVE_INTELLIGENCE
    MissionHub.ARCHIVE -> NavTarget.SETTINGS // Settings/Profile
}