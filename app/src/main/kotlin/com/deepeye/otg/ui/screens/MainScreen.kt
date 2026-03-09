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
import com.deepeye.otg.usb.UsbLifecycleState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun MainScreen(
    viewModel: UsbViewModel,
    onRemoteShare: () -> Unit
) {
    val lifecycleState by viewModel.lifecycleState.collectAsState()
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
        // Debug Overlay (Top Layer)
        DebugOverlayPanel(viewModel)

        // Dynamic Layer: Changes between Disconnected and Active modes
        AnimatedContent(
            targetState = currentNav,
            transitionSpec = {
                (fadeIn(tween(400)) + scaleIn(initialScale = 0.95f)).togetherWith(fadeOut(tween(300)) + scaleOut(targetScale = 1.05f))
            },
            label = "NavTransition"
        ) { target ->
            when (target) {
                NavTarget.SETTINGS -> SettingsScreen(viewModel)
                else -> {
                    // Logic for Home / Devices (Active / Idle)
                    AnimatedContent(
                        targetState = lifecycleState,
                        transitionSpec = {
                            (fadeIn(tween(400)) + slideInVertically { it / 2 }).togetherWith(fadeOut(tween(300)))
                        },
                        label = "MainStateTransition"
                    ) { state ->
                        when (state) {
                            is UsbLifecycleState.Idle -> {
                                DisconnectedView(hazeState)
                            }
                            is UsbLifecycleState.DeviceDetected -> {
                                // Waiting for permission / connection – show a focused waiting overlay.
                                WaitingScreen(op = sessionState.queuedOperation) {
                                    viewModel.resetToIdle()
                                }
                            }
                            is UsbLifecycleState.PermissionPending -> {
                                WaitingScreen(op = sessionState.queuedOperation) {
                                    viewModel.resetToIdle()
                                }
                            }
                            is UsbLifecycleState.NoOtgSupport -> {
                                // Dedicated OTG unsupported UI
                                ConnectionTestScreen(
                                    otgResult = null,
                                    diagnosticSteps = emptyMap()
                                )
                            }
                            is UsbLifecycleState.Connected -> {
                                // Special handling for MTP-only connections
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
                            is UsbLifecycleState.Degraded -> {
                                ErrorScreen(
                                    message = "Connection degraded: missed ${state.missedPings}/${state.maxPings} health checks.",
                                    onRetry = { viewModel.resetToIdle() }
                                )
                            }
                            is UsbLifecycleState.Dead -> {
                                ErrorScreen(
                                    message = "Connection lost: ${state.reason}",
                                    onRetry = { viewModel.resetToIdle() }
                                )
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(hazeState),
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
        
        // 3. Log Tail (Terminal Overlay feel at the bottom)
        item {
            val logs by viewModel.logs.collectAsState()
            LogTailView(logs = logs.takeLast(5))
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
        Row {
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
