package com.deepeye.otg.ui.apple

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.apple.AppleCategory
import com.deepeye.apple.AppleTool
import com.deepeye.apple.AppleToolsRegistry
import com.deepeye.apple.RiskLevel
import com.deepeye.otg.ui.components.GlassCard
import com.deepeye.otg.ui.components.NeonButton
import com.deepeye.otg.ui.components.NeonButtonStyle
import com.deepeye.otg.ui.components.SectionHeader
import com.deepeye.otg.ui.theme.DeepEyeColors

/**
 * Enhanced Apple Pro Tools Screen with complete tool registry
 * Organizes all Apple/iOS features into categorized tabs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleProToolsEnhancedScreen(
    viewModel: AppleDeviceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf<AppleCategory?>(null) }
    var selectedTool by remember { mutableStateOf<AppleTool?>(null) }
    var showToolDetails by remember { mutableStateOf(false) }
    
    val categories = AppleCategory.values().toList()
    val tools = selectedCategory?.let { 
        AppleToolsRegistry.getToolsByCategory(it) 
    } ?: AppleToolsRegistry.ALL_TOOLS
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        SectionHeader(
            title = "Apple Pro Tools",
            count = "${tools.size} Tools",
            accentColor = DeepEyeColors.PurpleDim
        )
        
        // Device Status Card
        AppleDeviceStatusCard(state, viewModel)
        
        // Category Filter Chips
        CategoryFilterChips(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it }
        )
        
        // Tools Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.heightIn(max = 600.dp)
        ) {
            items(tools) { tool ->
                AppleToolCard(
                    tool = tool,
                    isSelected = selectedTool?.id == tool.id,
                    onClick = { 
                        selectedTool = tool
                        showToolDetails = true
                    }
                )
            }
        }
        
        // Selected Tool Details
        if (showToolDetails && selectedTool != null) {
            AppleToolDetailsCard(
                tool = selectedTool!!,
                viewModel = viewModel,
                onClose = { showToolDetails = false }
            )
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }
}

/**
 * Device status card showing current Apple device state
 */
@Composable
private fun AppleDeviceStatusCard(
    state: AppleDeviceUiState,
    viewModel: AppleDeviceViewModel
) {
    val modeLabel = when (state.detectedMode) {
        com.deepeye.otg.usb.DeviceMatrix.AppleMode.DFU -> "DFU"
        com.deepeye.otg.usb.DeviceMatrix.AppleMode.RECOVERY -> "Recovery"
        com.deepeye.otg.usb.DeviceMatrix.AppleMode.NORMAL -> "Normal"
        com.deepeye.otg.usb.DeviceMatrix.AppleMode.WTF -> "WTF"
        com.deepeye.otg.usb.DeviceMatrix.AppleMode.PWNED_DFU -> "Pwned DFU"
        else -> "Idle"
    }
    
    val subtitle = when (val appleState = state.appleDeviceState) {
        is com.deepeye.otg.data.repository.AppleDeviceState.Detected -> 
            "${appleState.device.deviceName} • ${appleState.mode}"
        is com.deepeye.otg.data.repository.AppleDeviceState.Error -> 
            appleState.reason
        else -> 
            "Connect Apple device in Normal, Recovery, or DFU mode"
    }
    
    GlassCard(
        hazeState = null,
        accentColor = DeepEyeColors.PurpleDim,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneIphone,
                    contentDescription = null,
                    tint = DeepEyeColors.PurpleDim,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = "Apple Device: $modeLabel",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepEyeColors.TextPrimary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepEyeColors.TextSecondary
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                NeonButton(
                    text = "Refresh",
                    onClick = { viewModel.refreshAppleDevice() },
                    style = NeonButtonStyle.SECONDARY,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Category filter chips
 */
@Composable
private fun CategoryFilterChips(
    categories: List<AppleCategory>,
    selectedCategory: AppleCategory?,
    onCategorySelected: (AppleCategory?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedCategory == null,
            onClick = { onCategorySelected(null) },
            label = { Text("All") },
            leadingIcon = if (selectedCategory == null) {
                { Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(18.dp)) }
            } else null
        )
        
        categories.take(4).forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category.displayName) },
                leadingIcon = if (selectedCategory == category) {
                    { Icon(categoryIcon(category), contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null
            )
        }
    }
}

/**
 * Individual tool card
 */
@Composable
private fun AppleToolCard(
    tool: AppleTool,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val riskColor = when (tool.riskLevel) {
        RiskLevel.LOW -> DeepEyeColors.Success
        RiskLevel.MEDIUM -> Color(0xFFFFA500) // Orange
        RiskLevel.HIGH -> Color(0xFFFF4444) // Red
        RiskLevel.CRITICAL -> Color(0xFFFF0000) // Bright red
    }
    
    GlassCard(
        hazeState = null,
        accentColor = if (isSelected) DeepEyeColors.PurpleDim else DeepEyeColors.BorderGlass,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = toolIcon(tool),
                    contentDescription = null,
                    tint = DeepEyeColors.PurpleDim,
                    modifier = Modifier.size(24.dp)
                )
                
                Box(
                    modifier = Modifier
                        .background(
                            color = riskColor.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tool.riskLevel.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = riskColor
                    )
                }
            }
            
            Text(
                text = tool.name,
                style = MaterialTheme.typography.titleSmall,
                color = DeepEyeColors.TextPrimary,
                maxLines = 2
            )
            
            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodySmall,
                color = DeepEyeColors.TextSecondary,
                maxLines = 3
            )
            
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = tool.supportedVersions,
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepEyeColors.TextMuted
                )
                
                if (tool.requiresJailbreak) {
                    Text(
                        text = "JB Required",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFA500)
                    )
                }
            }
        }
    }
}

/**
 * Tool details card with action button
 */
@Composable
private fun AppleToolDetailsCard(
    tool: AppleTool,
    viewModel: AppleDeviceViewModel,
    onClose: () -> Unit
) {
    GlassCard(
        hazeState = null,
        accentColor = DeepEyeColors.PurpleDim,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = tool.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = DeepEyeColors.TextPrimary
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodyMedium,
                color = DeepEyeColors.TextSecondary
            )
            
            // Tool metadata
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                InfoRow(label = "Category", value = tool.category.displayName)
                InfoRow(label = "iOS Versions", value = tool.supportedVersions)
                InfoRow(label = "Risk Level", value = tool.riskLevel.name)
                InfoRow(label = "Est. Time", value = tool.estimatedTime)
                InfoRow(
                    label = "Jailbreak", 
                    value = if (tool.requiresJailbreak) "Required" else "Not Required"
                )
            }
            
            // Execute button
            NeonButton(
                text = "Execute ${tool.name}",
                onClick = {
                    // Map tool to appropriate viewModel action
                    executeAppleTool(tool, viewModel)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = DeepEyeColors.TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = DeepEyeColors.TextPrimary
        )
    }
}

/**
 * Execute the selected Apple tool
 */
private fun executeAppleTool(tool: AppleTool, viewModel: AppleDeviceViewModel) {
    when (tool.id) {
        "activation_check", "refresh_mode", "device_info" -> 
            viewModel.refreshAppleDevice()
        "getenv_snapshot", "mode_probe" -> 
            viewModel.sendIrecoveryCommand("getenv")
        "dfu_mode" -> 
            viewModel.enterDfu()
        "recovery_mode" -> 
            viewModel.exitRecovery()
        // Add more tool mappings as needed
        else -> {
            // For tools not yet implemented, show a message
            // This would typically trigger a navigation or show a toast
        }
    }
}

/**
 * Get icon for category
 */
private fun categoryIcon(category: AppleCategory): ImageVector {
    return when (category) {
        AppleCategory.ACTIVATION_BYPASS -> Icons.Default.LockOpen
        AppleCategory.MDM_BYPASS -> Icons.Default.Shield
        AppleCategory.PASSCODE_BYPASS -> Icons.Default.Lock
        AppleCategory.FIRMWARE_TOOLS -> Icons.Default.Download
        AppleCategory.CHECKM8_EXPLOIT -> Icons.Default.BugReport
        AppleCategory.ICLOUD_TOOLS -> Icons.Default.Cloud
        AppleCategory.DIAGNOSTICS -> Icons.Default.Info
        AppleCategory.NETWORK_UNLOCK -> Icons.Default.SignalCellularAlt
    }
}

/**
 * Get icon for tool
 */
private fun toolIcon(tool: AppleTool): ImageVector {
    return when (tool.category) {
        AppleCategory.ACTIVATION_BYPASS -> Icons.Default.LockOpen
        AppleCategory.MDM_BYPASS -> Icons.Default.Shield
        AppleCategory.PASSCODE_BYPASS -> Icons.Default.Lock
        AppleCategory.FIRMWARE_TOOLS -> Icons.Default.Download
        AppleCategory.CHECKM8_EXPLOIT -> Icons.Default.BugReport
        AppleCategory.ICLOUD_TOOLS -> Icons.Default.Cloud
        AppleCategory.DIAGNOSTICS -> Icons.Default.Info
        AppleCategory.NETWORK_UNLOCK -> Icons.Default.SignalCellularAlt
    }
}
