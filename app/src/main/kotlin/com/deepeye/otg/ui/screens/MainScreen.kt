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
import com.deepeye.otg.viewmodel.UsbViewModel
import com.deepeye.otg.viewmodel.research.CveDashboardViewModel
import com.deepeye.otg.usb.UsbLifecycleState
import androidx.hilt.navigation.compose.hiltViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

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
                    colors = listOf(StitchTokens.BackgroundDark, StitchTokens.SurfaceDark)
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Multi-Device Rail (Stage 200.1)
            if (sessions.size > 1) {
                MultiDeviceRail(
                    sessions = sessions,
                    selectedKey = selectedKey,
                    onSelect = { viewModel.selectDevice(it) }
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
                    NavTarget.SETTINGS -> SettingsScreen(viewModel)
                    NavTarget.DASHBOARD -> ForensicDashboardScreen(viewModel)
                    NavTarget.FILE_EXPLORER -> FileExplorerScreen(viewModel)
                    NavTarget.IPHONE_15_RESEARCH -> Iphone15ResearchScreen(viewModel)
                    NavTarget.TERMINAL -> TerminalScreen(viewModel)
                    NavTarget.VAULT -> VaultScreen(onBack = { viewModel.setNav(NavTarget.HOME) })
                    NavTarget.STORAGE -> StorageScreen()
                    NavTarget.CVE_INTELLIGENCE -> CveDashboardScreen(
                        viewModel = cveViewModel,
                        onNavigateBack = { viewModel.setNav(NavTarget.HOME) }
                    )
                    NavTarget.FUZZ_DASHBOARD -> FuzzDashboardScreen(
                        viewModel = fuzzViewModel,
                        onNavigateBack = { viewModel.setNav(NavTarget.HOME) }
                    )
                    NavTarget.HID_RESEARCH -> HidResearchScreen(
                        viewModel = hidViewModel,
                        onNavigateBack = { viewModel.setNav(NavTarget.HOME) }
                    )
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
                    else -> {
                        // Logic for Home / Devices (Active / Idle)
                        val effectiveState = if (selectedKey != null) sessions[selectedKey] ?: lifecycleState else lifecycleState
                        
                        AnimatedContent(
                            targetState = effectiveState,
                            transitionSpec = {
                                (fadeIn(tween(400)) + slideInVertically { it / 2 }).togetherWith(fadeOut(tween(300)))
                            },
                            label = "MainStateTransition"
                        ) { state ->
                            when (state) {
                                is UsbLifecycleState.Idle -> DisconnectedView(hazeState)
                                is UsbLifecycleState.DeviceDetected -> {
                                    WaitingScreen(op = sessionState.queuedOperation) { viewModel.resetToIdle() }
                                }
                                is UsbLifecycleState.PermissionPending -> {
                                    WaitingScreen(op = sessionState.queuedOperation) { viewModel.resetToIdle() }
                                }
                                is UsbLifecycleState.Connected -> {
                                    if (sessionState.deviceMode == DeviceMode.MTP_ONLY) {
                                        MtpOnlyScreen(onBack = { viewModel.resetToIdle() })
                                    } else {
                                        ActiveSessionView(
                                            state = state,
                                            sessionState = sessionState,
                                            viewModel = viewModel,
                                            hazeState = hazeState,
                                            perfMode = perfMode
                                        )
                                    }
                                }
                                is UsbLifecycleState.Dead -> {
                                    ErrorScreen(message = "Connection lost: ${state.reason}", onRetry = { viewModel.resetToIdle() })
                                }
                                is UsbLifecycleState.Error -> {
                                    ErrorOverlay(state.message) { viewModel.resetToIdle() }
                                }
                                else -> {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = StitchTokens.Primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Overlay Navigation / Controls (Top Bar always visible above Home, but hidden in Settings for clean exit)
        if (currentNav != NavTarget.SETTINGS) {
            MainTopBar(viewModel)
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
fun MultiDeviceRail(
    sessions: Map<String, UsbLifecycleState>,
    selectedKey: String?,
    onSelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(StitchTokens.SurfaceDark.copy(alpha = 0.5f))
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sessions.toList()) { (key, state) ->
            val isSelected = key == selectedKey
            val deviceName = when (state) {
                is UsbLifecycleState.Connected -> state.deviceName
                is UsbLifecycleState.DeviceDetected -> "Detecting..."
                else -> "Unknown"
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(key) }
                    .border(
                        width = 1.dp,
                        color = if (isSelected) StitchTokens.Primary else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    ),
                color = if (isSelected) StitchTokens.Primary.copy(alpha = 0.1f) else StitchTokens.GlassSurface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Usb,
                        contentDescription = null,
                        tint = if (isSelected) StitchTokens.Primary else StitchTokens.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = deviceName,
                        style = StitchTokens.LabelSmall,
                        color = if (isSelected) StitchTokens.TextPrimary else StitchTokens.TextSecondary
                    )
                }
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
        GlassCard(hazeState = null, modifier = Modifier.fillMaxWidth().height(80.dp), accentColor = accent.copy(0.2f)) {
            Column(Modifier.padding(8.dp)) {
                Text("AI INTEL", style = StitchTokens.MonoCode.copy(fontSize = 9.sp), color = accent)
                Text(
                    text = aiAnalysis.ifEmpty { "Waiting for session telemetry..." },
                    style = StitchTokens.BodyMedium,
                    color = Color.LightGray,
                    maxLines = 3
                )
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
        onClick = if (availability.enabled) onRun else null
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
private fun MainTopBar(viewModel: UsbViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("DEEPEYE OTG", style = StitchTokens.TitleLarge, color = StitchTokens.TextPrimary)
            Text("Pro Forensic Toolkit", style = StitchTokens.LabelSmall, color = StitchTokens.Primary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { viewModel.setNav(NavTarget.DASHBOARD) },
                modifier = Modifier.clip(CircleShape).background(Color.White.copy(0.05f))
            ) {
                val currentNav by viewModel.currentNav.collectAsState()
                Icon(
                    imageVector = Icons.Default.Dashboard,
                    contentDescription = "Dashboard",
                    tint = if (currentNav == NavTarget.DASHBOARD) StitchTokens.Primary else StitchTokens.TextSecondary
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.setNav(NavTarget.IPHONE_15_RESEARCH) },
                modifier = Modifier.clip(CircleShape).background(Color.White.copy(0.05f))
            ) {
                val currentNav by viewModel.currentNav.collectAsState()
                Icon(
                    imageVector = Icons.Default.PhoneIphone,
                    contentDescription = "iPhone 15 Research",
                    tint = if (currentNav == NavTarget.IPHONE_15_RESEARCH) StitchTokens.Primary else StitchTokens.TextSecondary
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.setNav(NavTarget.TERMINAL) },
                modifier = Modifier.clip(CircleShape).background(Color.White.copy(0.05f))
            ) {
                val currentNav by viewModel.currentNav.collectAsState()
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Forensic Console",
                    tint = if (currentNav == NavTarget.TERMINAL) StitchTokens.Primary else StitchTokens.TextSecondary
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.setNav(NavTarget.VAULT) },
                modifier = Modifier.clip(CircleShape).background(Color.White.copy(0.05f))
            ) {
                val currentNav by viewModel.currentNav.collectAsState()
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = "Forensic Vault",
                    tint = if (currentNav == NavTarget.VAULT) StitchTokens.Primary else StitchTokens.TextSecondary
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.setNav(NavTarget.CVE_INTELLIGENCE) },
                modifier = Modifier.clip(CircleShape).background(Color.White.copy(0.05f))
            ) {
                val currentNav by viewModel.currentNav.collectAsState()
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "CVE Intelligence",
                    tint = if (currentNav == NavTarget.CVE_INTELLIGENCE) StitchTokens.Primary else StitchTokens.TextSecondary
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.setNav(NavTarget.FUZZ_DASHBOARD) },
                modifier = Modifier.clip(CircleShape).background(Color.White.copy(0.05f))
            ) {
                val currentNav by viewModel.currentNav.collectAsState()
                Icon(
                    imageVector = Icons.Default.ElectricBolt,
                    contentDescription = "Fuzz Harness",
                    tint = if (currentNav == NavTarget.FUZZ_DASHBOARD) StitchTokens.Primary else StitchTokens.TextSecondary
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.setNav(NavTarget.HID_RESEARCH) },
                modifier = Modifier.clip(CircleShape).background(Color.White.copy(0.05f))
            ) {
                val currentNav by viewModel.currentNav.collectAsState()
                Icon(
                    imageVector = Icons.Default.Usb,
                    contentDescription = "HID Research",
                    tint = if (currentNav == NavTarget.HID_RESEARCH) StitchTokens.Primary else StitchTokens.TextSecondary
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.setNav(NavTarget.STORAGE) },
                modifier = Modifier.clip(CircleShape).background(Color.White.copy(0.05f))
            ) {
                val currentNav by viewModel.currentNav.collectAsState()
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = "Storage",
                    tint = if (currentNav == NavTarget.STORAGE) StitchTokens.Primary else StitchTokens.TextSecondary
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.toggleDebugPanel() },
                modifier = Modifier.clip(CircleShape).background(Color.White.copy(0.05f))
            ) {
                val showDebug by viewModel.showDebugPanel.collectAsState()
                Icon(
                    Icons.Default.BugReport, 
                    "Debug", 
                    tint = if (showDebug) StitchTokens.Primary else StitchTokens.TextSecondary
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.toggleSplitView() },
                modifier = Modifier.clip(CircleShape).background(Color.White.copy(0.05f))
            ) {
                val splitActive by viewModel.splitViewActive.collectAsState()
                Icon(
                    imageVector = if (splitActive) Icons.Default.VerticalSplit else Icons.Default.ViewAgenda,
                    contentDescription = "Split View",
                    tint = if (splitActive) StitchTokens.Primary else StitchTokens.TextSecondary
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.setNav(NavTarget.SETTINGS) },
                modifier = Modifier.clip(CircleShape).background(Color.White.copy(0.05f))
            ) {
                Icon(Icons.Default.Settings, "Settings", tint = StitchTokens.TextSecondary)
            }
        }
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
