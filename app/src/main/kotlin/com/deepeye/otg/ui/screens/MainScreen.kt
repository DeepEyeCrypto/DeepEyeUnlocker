package com.deepeye.otg.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.domain.models.*
import com.deepeye.otg.domain.engine.AvailabilityEngine
import com.deepeye.otg.ui.components.GlassButton
import com.deepeye.otg.ui.components.GlassCard
import com.deepeye.otg.ui.components.GlassPolicyBadge
import com.deepeye.otg.ui.theme.GlassTokens
import com.deepeye.otg.viewmodel.UsbViewModel
import com.deepeye.otg.data.BrandData
import com.deepeye.otg.ui.components.BrandSelectorBar
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

@Composable
fun MainScreen(viewModel: UsbViewModel) {
    val uiState by viewModel.usbUiState.collectAsState()
    val lifecycleState by viewModel.lifecycleState.collectAsState()
    val domainSessionState by viewModel.domainSessionState.collectAsState()
    
    val hazeState = remember { HazeState() }
    val perfMode by viewModel.performanceMode.collectAsState()
    
    val selectedBrandIndex by viewModel.selectedBrand.collectAsState()
    val brands = BrandData.brands
    val safeBrandIndex = selectedBrandIndex.coerceIn(0, brands.size - 1)
    val selectedBrandString = brands.getOrNull(safeBrandIndex)
    
    val statusMsg by viewModel.statusMsg.collectAsState()
    val otgResult by viewModel.otgResult.collectAsState()
    val diagnosticSteps by viewModel.diagnosticSteps.collectAsState()
    
    // Convert to target Domain SessionState to feed the engine
    // In future versions, UsbViewModel will natively maintain domainSessionState
    val connected = lifecycleState is com.deepeye.otg.usb.UsbLifecycleState.Connected
    val activeDeviceMode = when (viewModel.selectedMode.value) {
        com.deepeye.otg.data.ConnectionMode.ADB -> DeviceMode.ADB
        com.deepeye.otg.data.ConnectionMode.FASTBOOT -> DeviceMode.FASTBOOT
        com.deepeye.otg.data.ConnectionMode.EDL -> DeviceMode.QC_EDL
        com.deepeye.otg.data.ConnectionMode.BROM -> DeviceMode.MTK_BROM
        com.deepeye.otg.data.ConnectionMode.PRELOADER -> DeviceMode.MTK_PRELOADER
        com.deepeye.otg.data.ConnectionMode.META -> DeviceMode.MTK_META
        com.deepeye.otg.data.ConnectionMode.DIAG -> DeviceMode.QC_DIAG
        com.deepeye.otg.data.ConnectionMode.MTP -> DeviceMode.MTP_ONLY
        else -> DeviceMode.UNKNOWN
    }
    
    val virtualSessionState = SessionState(
        connected = connected,
        selectedBrand = selectedBrandString?.name,
        selectedModel = selectedBrandString?.name, // Fallback to brand simulating a selected target
        deviceMode = activeDeviceMode
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassTokens.backgroundBrush)
    ) {
        // Base Layer: Ambient Orbs
        if (!perfMode) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = Color(0xFF6750A4).copy(alpha = 0.08f), radius = 400.dp.toPx(), center = Offset(0f, 0f))
                drawCircle(color = Color(0xFFEADDFF).copy(alpha = 0.12f), radius = 350.dp.toPx(), center = Offset(size.width, size.height * 0.8f))
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(if (!perfMode) Modifier.hazeSource(hazeState) else Modifier),
            contentPadding = PaddingValues(top = 185.dp, bottom = 32.dp)
        ) {
            item {
                com.deepeye.otg.ui.components.OemWarningBanner()
            }
            
            // Device Status Box -> ModesPanel -> FeatureGroupsSection
            when (val ls = lifecycleState) {
                is com.deepeye.otg.usb.UsbLifecycleState.Connected -> {
                    item {
                        GlassCard(
                            hazeState = hazeState, performanceMode = perfMode,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                val pulse = rememberInfiniteTransition(label = "dot")
                                val dotAlpha by pulse.animateFloat(0.4f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "dotAlpha")
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(uiState.statusColor).copy(alpha = dotAlpha)))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = ls.deviceName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(text = uiState.statusLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                                }
                            }
                        }
                    }
                }
                is com.deepeye.otg.usb.UsbLifecycleState.Idle -> {
                    item {
                        com.deepeye.otg.ui.screens.ConnectionTestScreen(
                            otgResult = otgResult,
                            diagnosticSteps = diagnosticSteps
                        )
                    }
                }
                is com.deepeye.otg.usb.UsbLifecycleState.Error -> {
                    item {
                        GlassCard(hazeState = hazeState, performanceMode = perfMode, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                             Text(text = ls.message, modifier = Modifier.padding(16.dp), color = Color(0xFFFCA5A5))
                        }
                    }
                }
                else -> {
                    item {
                        GlassCard(hazeState = hazeState, performanceMode = perfMode, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(uiState.statusLabel, color = Color(uiState.statusColor))
                            }
                        }
                    }
                }
            }

            // Always Visible Mode Catalog Panel
            item {
                GroupHeader(title = "DETECTED & SUPPORTED MODES")
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(DeepEyeCatalogs.MODE_CATALOG, key = { it.id }) { modeSpec ->
                        val isDetected = virtualSessionState.deviceMode == modeSpec.relatedDeviceMode
                        GlassModeCard(modeSpec = modeSpec, detected = isDetected, hazeState = hazeState, performanceMode = perfMode)
                    }
                }
            }

            // Unconditional Feature Groups Definition
            DeepEyeCatalogs.FEATURE_GROUPS.forEach { group ->
                item(key = group.id) {
                    GroupHeader(title = group.title)
                }

                val rowChunks = group.operations.chunked(2)
                items(rowChunks, key = { it.joinToString { f -> f.id } }) { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { op ->
                            // Evaluate strictly via Policy Engine
                            val availability = AvailabilityEngine.availabilityFor(
                                operation = op,
                                sessionState = virtualSessionState,
                                userRole = PolicyTier.POLICY // Assuming current user operates at POLICY level
                            )
                            
                            GlassFeatureCard(
                                operation = op,
                                availability = availability,
                                hazeState = hazeState,
                                performanceMode = perfMode,
                                modifier = Modifier.weight(1f),
                                onRun = { if (availability.enabled) viewModel.queueOperation(op.id) }
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }

        // Top Bar & Tabs (Layered above content)
        Column(modifier = Modifier.fillMaxWidth().background(
            if (perfMode) MaterialTheme.colorScheme.background.copy(alpha = 0.95f) else Color.Transparent
        )) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("DeepEyeUnlocker", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)

                if (lifecycleState is com.deepeye.otg.usb.UsbLifecycleState.Connected) {
                    val healthBaseColor = Color(uiState.statusColor)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(healthBaseColor.copy(alpha = 0.12f))
                            .border(1.dp, healthBaseColor.copy(0.40f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        val pulse = rememberInfiniteTransition(label = "dot")
                        val dotAlpha by pulse.animateFloat(0.5f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "dotAlpha")
                        Box(Modifier.size(6.dp).clip(CircleShape).background(healthBaseColor.copy(dotAlpha)))
                        Text((lifecycleState as com.deepeye.otg.usb.UsbLifecycleState.Connected).deviceName, fontSize = 10.sp, color = healthBaseColor, fontWeight = FontWeight.Bold)
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RemoteBadge()
                    Spacer(modifier = Modifier.width(8.dp))
                    GlassButton(label = "TEST", onClick = { viewModel.enterTestHarness() }, modifier = Modifier.width(70.dp), accent = true)
                    Spacer(modifier = Modifier.width(8.dp))
                    GlassButton(label = if (perfMode) "PERF: LOW" else "PERF: HIGH", onClick = { viewModel.togglePerformance() }, modifier = Modifier.width(90.dp), accent = !perfMode)
                }
            }
            BrandSelectorBar(
                selectedIndex = safeBrandIndex,
                onBrandSelected = { viewModel.onBrandSelected(it) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun GroupHeader(title: String) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFEDE9FE))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6D28D9), letterSpacing = 0.5.sp)
    }
}

@Composable
private fun RemoteBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse), label = "alpha")
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFEADDFF))
            .border(1.dp, Color(0xFF6750A4).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF6750A4).copy(alpha = alpha)))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "REMOTE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
        }
    }
}

@Composable
private fun GlassModeCard(
    modeSpec: ModeCardSpec,
    detected: Boolean,
    hazeState: HazeState,
    performanceMode: Boolean
) {
    val bgColor = if (detected) Color(0xFF34D399).copy(alpha = 0.15f) else Color.Transparent
    val borderColor = if (detected) Color(0xFF10B981) else Color.White.copy(alpha = 0.2f)

    GlassCard(
        hazeState = hazeState,
        performanceMode = performanceMode,
        modifier = Modifier.width(160.dp).height(100.dp),
        onClick = null
    ) {
        Column(
            modifier = Modifier.fillMaxSize().background(bgColor).border(1.dp, borderColor, RoundedCornerShape(16.dp)).padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = modeSpec.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(text = modeSpec.requirementsSummary, style = MaterialTheme.typography.bodySmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            if (detected) {
                Text("DETECTED", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF059669))
            } else {
                Text("NOT DETECTED", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun GlassFeatureCard(
    operation: DeepEyeOperation,
    availability: OperationAvailability,
    hazeState: HazeState,
    performanceMode: Boolean,
    modifier: Modifier = Modifier,
    onRun: () -> Unit
) {
    GlassCard(
        hazeState = hazeState,
        performanceMode = performanceMode,
        modifier = modifier
            .height(180.dp)
            .graphicsLayer { alpha = if (availability.enabled) 1f else 0.5f },
        onClick = null
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Tier Badge
                GlassPolicyBadge(tier = operation.tier)
                
                // Dangerous indicator
                if (operation.dangerous) {
                    Text("⚠️", fontSize = 16.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = operation.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Availability Reason or Description
            Text(
                text = if (!availability.enabled && availability.reason != null) availability.reason else operation.description,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = if (!availability.enabled) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            GlassButton(
                label = if (availability.enabled) "EXECUTE" else "UNAVAILABLE",
                onClick = if (availability.enabled) onRun else ({}),
                modifier = Modifier.fillMaxWidth().height(36.dp),
                accent = availability.enabled
            )
        }
    }
}
