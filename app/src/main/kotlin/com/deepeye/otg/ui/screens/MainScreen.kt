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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun MainScreen(viewModel: UsbViewModel) {
    val state by viewModel.usbState.collectAsState()
    val isConnected = state.state == com.deepeye.otg.ConnState.CONNECTED_READY
    val deviceName = state.deviceKey

    val hazeState = remember { HazeState() }
    val perfMode by viewModel.performanceMode.collectAsState()
    
    val selectedBrandIndex by viewModel.selectedBrand.collectAsState()
    val brands = FeatureData.brands
    val safeBrandIndex = selectedBrandIndex.coerceIn(0, brands.size - 1)
    
    // For this UI version, groups are global categories shown for every brand
    val groups = FeatureData.groups

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
            contentPadding = PaddingValues(top = 130.dp, bottom = 32.dp)
        ) {
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
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Connected: $deviceName",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Groups and Features
            groups.forEach { group ->
                item {
                    GroupHeader(title = group.title)
                }

                val rowChunks = group.features.chunked(2)
                items(rowChunks) { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 5.dp), // 10dp gap between cards vertically
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                
                item { Spacer(modifier = Modifier.height(16.dp)) } // Gap between groups
            }
        }

        // Top Bar & Tabs
        Column(modifier = Modifier.fillMaxWidth()) {
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
            
            GlassTabRow(
                tabs = brands,
                selectedIndex = safeBrandIndex,
                onSelect = { viewModel.selectedBrand.value = it },
                hazeState = hazeState,
                performanceMode = perfMode
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
    modifier: Modifier = Modifier,
    onRun: () -> Unit
) {
    GlassCard(
        hazeState = hazeState,
        performanceMode = performanceMode,
        modifier = modifier.height(165.dp),
        onClick = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp) // Requested internal padding
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
                onClick = onRun,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                accent = true
            )
        }
    }
}
