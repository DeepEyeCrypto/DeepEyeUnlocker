package com.deepeye.otg.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.data.FeatureData
import com.deepeye.otg.data.FeatureItem
import com.deepeye.otg.UsbSessionState
import com.deepeye.otg.ui.components.GlassButton
import com.deepeye.otg.ui.components.GlassCard
import com.deepeye.otg.ui.components.GlassTabRow
import com.deepeye.otg.ui.components.GlassTierBadge
import com.deepeye.otg.ui.theme.GlassTokens
import com.deepeye.otg.viewmodel.UsbViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.hazeEffect

@Composable
fun MainScreen(viewModel: UsbViewModel) {
    val state by viewModel.usbState.collectAsState()
    val isConnected = state.state == com.deepeye.otg.ConnState.CONNECTED_READY
    val deviceName = state.deviceKey

    // RULE: HazeState precisely ONE per screen to prevent CPU/memory sink
    val hazeState = remember { HazeState() }
    val groups = FeatureData.groups
    val perfMode by viewModel.performanceMode.collectAsState()
    
    val selectedBrandIndex by viewModel.selectedBrand.collectAsState()
    val safeIndex = selectedBrandIndex.coerceIn(0, groups.size - 1)
    val activeGroup = groups[safeIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassTokens.backgroundBrush)
    ) {
        // Base Layer 0: Ambient Orbs manually managed safely
        if (!perfMode) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (size.width < 1f || size.height < 1f) return@Canvas
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

        // Layer 1: Content Scroller with Haze Source definition
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!perfMode) Modifier.hazeSource(hazeState) else Modifier
                ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)
        ) {
            // Spacer bridging floating top elements
            item { Spacer(modifier = Modifier.height(130.dp)) }

            // Device Status Box
            if (isConnected && deviceName != null) {
                item {
                    GlassCard(
                        hazeState = hazeState,
                        performanceMode = perfMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Connected: $deviceName",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Feature chunk renderer cleanly split by rows
            val rowChunks = activeGroup.features.chunked(2)
            
            items(items = rowChunks, key = { it.joinToString { item -> item.id } }) { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowItems.forEach { feature ->
                        GlassFeatureCard(
                            feature = feature,
                            hazeState = hazeState,
                            performanceMode = perfMode,
                            modifier = Modifier.weight(1f),
                            onRun = { viewModel.queueOperation(feature) }
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Layer 2: Fixed Overlay App Header UI
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dashboard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                GlassButton(
                    label = if (perfMode) "PERF: LOW" else "PERF: HIGH",
                    onClick = { viewModel.togglePerformance() },
                    modifier = Modifier.width(100.dp),
                    accent = !perfMode
                )
            }
            GlassTabRow(
                tabs = groups.map { it.title.substringBefore(":") },
                selectedIndex = safeIndex,
                onSelect = { viewModel.selectedBrand.value = it },
                hazeState = hazeState,
                performanceMode = perfMode
            )
        }
    }
}

@Composable
private fun GlassFeatureCard(
    feature: FeatureItem,
    hazeState: HazeState,
    performanceMode: Boolean,
    modifier: Modifier = Modifier,
    onRun: () -> Unit
) {
    GlassCard(
        hazeState = hazeState,
        performanceMode = performanceMode,
        modifier = modifier.height(160.dp),
        onClick = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = feature.icon, fontSize = 20.sp)
                }
                GlassTierBadge(tier = feature.tier)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = feature.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            GlassButton(
                label = "RUN",
                onClick = onRun,
                modifier = Modifier.fillMaxWidth(),
                accent = true
            )
        }
    }
}
