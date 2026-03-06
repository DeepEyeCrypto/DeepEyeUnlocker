package com.deepeye.otg.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.deepeye.otg.data.FeatureData
import com.deepeye.otg.data.FeatureItem
import com.deepeye.otg.ui.components.GlassButton
import com.deepeye.otg.ui.components.GlassCard
import com.deepeye.otg.ui.components.GlassTabRow
import com.deepeye.otg.ui.components.GlassTierBadge
import com.deepeye.otg.ui.theme.GlassTokens
import com.deepeye.otg.viewmodel.UsbViewModel
import com.deepeye.otg.data.ConnectionMode
import com.deepeye.otg.data.BrandData
import com.deepeye.otg.ui.components.BrandSelectorBar
import com.deepeye.otg.ui.components.ConnectionModeBar
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

@Composable
fun MainScreen(viewModel: UsbViewModel) {
    val state by viewModel.usbState.collectAsState()
    val isConnected = state.state == com.deepeye.otg.ConnState.CONNECTED_READY
    val deviceName = state.deviceKey

    val hazeState = remember { HazeState() }
    val perfMode by viewModel.performanceMode.collectAsState()
    
    val selectedBrandIndex by viewModel.selectedBrand.collectAsState()
    val brands = BrandData.brands
    val safeBrandIndex = selectedBrandIndex.coerceIn(0, brands.size - 1)
    
    val selectedMode by viewModel.selectedMode.collectAsState()
    val availableIds by viewModel.availableFeatureIds.collectAsState()
    val brandFeatures by viewModel.activeBrandFeatures.collectAsState()
    val activeUsbState by viewModel.activeUsbState.collectAsState()
    val statusMsg by viewModel.statusMsg.collectAsState()
    val health by viewModel.connectionHealth.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassTokens.backgroundBrush)
    ) {
        // Base Layer: Ambient Orbs
        if (!perfMode) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0xFF6750A4).copy(alpha = 0.08f),
                    radius = 400.dp.toPx(),
                    center = Offset(0f, 0f)
                )
                drawCircle(
                    color = Color(0xFFEADDFF).copy(alpha = 0.12f),
                    radius = 350.dp.toPx(),
                    center = Offset(size.width, size.height * 0.8f)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(if (!perfMode) Modifier.hazeSource(hazeState) else Modifier),
            contentPadding = PaddingValues(top = 185.dp, bottom = 32.dp) // Adjusted for dual selector bars
        ) {
            item {
                com.deepeye.otg.ui.components.OemWarningBanner()
            }
            
            // Device Status Box: Diagnostic or Connection
            when (val usb = activeUsbState) {
                is com.deepeye.otg.UsbSessionState.ConnectedReady -> {
                    item {
                        GlassCard(
                            hazeState = hazeState,
                            performanceMode = perfMode,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val pulse = rememberInfiniteTransition(label = "dot")
                                val dotAlpha by pulse.animateFloat(
                                    0.4f, 1f,
                                    infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                                    label = "dotAlpha"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E).copy(alpha = dotAlpha))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = usb.deviceName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = statusMsg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
                is com.deepeye.otg.UsbSessionState.Error -> {
                    item {
                        GlassCard(
                            hazeState = hazeState,
                            performanceMode = perfMode,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            containerColor = Color(0xFFEF4444).copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = usb.message,
                                modifier = Modifier.padding(16.dp),
                                color = Color(0xFFFCA5A5),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                is com.deepeye.otg.UsbSessionState.Idle -> {
                    item {
                        ConnectionTestScreen(viewModel = viewModel)
                    }
                }
                else -> Unit
            }

            // Connection Mode Bar
            item {
                ConnectionModeBar(
                    selectedMode = selectedMode,
                    onModeSelected = { viewModel.onModeSelected(it) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }

            // Groups and Features
            brandFeatures.groups.forEach { group ->
                item(key = group.id) {
                    GroupHeader(title = group.title)
                }

                val rowChunks = group.features.chunked(2)
                items(rowChunks, key = { it.joinToString { f -> f.id } }) { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 5.dp), // 10dp gap between cards vertically
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { feature ->
                            val isAvailable = availableIds.contains(feature.id)
                            GlassFeatureCard(
                                feature = feature,
                                hazeState = hazeState,
                                performanceMode = perfMode,
                                enabled = isAvailable,
                                modifier = Modifier.weight(1f),
                                onRun = { if (isAvailable) viewModel.queueOperation(feature) }
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) } // Gap between groups
            }
        }

        // Top Bar & Tabs (Layered above content)
        Column(modifier = Modifier.fillMaxWidth().background(
            if (perfMode) MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
            else Color.Transparent
        )) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DeepEyeUnlocker",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (activeUsbState is com.deepeye.otg.UsbSessionState.ConnectedReady) {
                    val healthBaseColor = when (health) {
                        com.deepeye.otg.usb.ConnectionHealth.DEGRADED -> Color(0xFFF59E0B) // Amber
                        com.deepeye.otg.usb.ConnectionHealth.DEAD -> Color(0xFFEF4444) // Red
                        com.deepeye.otg.usb.ConnectionHealth.PAUSED -> Color(0xFF6B7280) // Gray
                        else -> Color(0xFF059669) // Green
                    }
                    
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(healthBaseColor.copy(alpha = 0.12f))
                            .border(
                                1.dp, healthBaseColor.copy(0.40f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        val pulse = rememberInfiniteTransition(label = "dot")
                        val dotAlpha by pulse.animateFloat(
                            0.5f, 1f,
                            infiniteRepeatable(tween(800), RepeatMode.Reverse),
                            label = "dotAlpha"
                        )
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(healthBaseColor.copy(dotAlpha))
                        )
                        Text(
                            (activeUsbState as com.deepeye.otg.UsbSessionState.ConnectedReady).deviceName,
                            fontSize = 10.sp, color = healthBaseColor,
                            fontWeight = FontWeight.Bold
                        )
                        if (health == com.deepeye.otg.usb.ConnectionHealth.DEGRADED) {
                            Text("UNSTABLE", fontSize = 8.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Black)
                        }
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RemoteBadge()
                    Spacer(modifier = Modifier.width(12.dp))
                    GlassButton(
                        label = if (perfMode) "PERF: LOW" else "PERF: HIGH",
                        onClick = { viewModel.togglePerformance() },
                        modifier = Modifier.width(90.dp),
                        accent = !perfMode
                    )
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
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6D28D9),
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun RemoteBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFEADDFF))
            .border(1.dp, Color(0xFF6750A4).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6750A4).copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "REMOTE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6750A4)
            )
        }
    }
}

@Composable
private fun GlassFeatureCard(
    feature: FeatureItem,
    hazeState: HazeState,
    performanceMode: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onRun: () -> Unit
) {
    GlassCard(
        hazeState = hazeState,
        performanceMode = performanceMode,
        modifier = modifier
            .height(165.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f },
        onClick = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Emoji chip
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = feature.icon, fontSize = 18.sp)
                }
                
                GlassTierBadge(tier = feature.tier)
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = feature.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            GlassButton(
                label = "RUN",
                onClick = if (enabled) onRun else ({}),
                modifier = Modifier.fillMaxWidth().height(36.dp),
                accent = enabled
            )
        }
    }
}
