package com.deepeye.otg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.UsbSessionState
import com.deepeye.otg.ConnState
import com.deepeye.otg.usb.DeepEyeOperation

@Composable
fun MainScreen(
    sessionState: UsbSessionState,
    queueState: SessionState,
    logs: List<OtgActivity.LogEntry> = emptyList(),
    onSelectModel: () -> Unit,
    onRemoteUnlock: () -> Unit,
    onOperationSelected: (DeepEyeOperation) -> Unit
) {
    var selectedBrand by remember { mutableStateOf("Xiaomi") }
    var showConsole by remember { mutableStateOf(false) }
    val brands = listOf("Xiaomi", "Samsung", "OPPO", "Vivo", "Realme", "Huawei", "OnePlus")

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepEyeColors.DarkBackground)
        ) {
            // --- HEADER ---
            MainHeader(
                state = sessionState.state,
                onRemoteClick = onRemoteUnlock,
                onLogoClick = { showConsole = !showConsole }
            )

            // --- BRAND TABS ---
            BrandTabs(
                brands = brands,
                selectedBrand = selectedBrand,
                onBrandSelected = { 
                    selectedBrand = it
                    onSelectModel() 
                }
            )

            // --- MODEL SELECTOR BAR ---
            ModelSelectorBar(
                selectedModel = "Auto-Detect",
                onSelectClick = onSelectModel
            )

            // --- FEATURE LIST ---
            FeatureListScreen(onOperationSelected = onOperationSelected)
        }
        
        // --- CONSOLE OVERLAY ---
        if (showConsole || queueState is SessionState.ExecutingOperation) {
            ConsoleOverlay(
                logs = logs,
                onClose = { showConsole = false }
            )
        }
    }
}

@Composable
fun ConsoleOverlay(
    logs: List<OtgActivity.LogEntry>,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable { onClose() }
            .padding(top = 100.dp)
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 0.dp)
                .clickable(enabled = false) {}
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("EXECUTION CONSOLE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                IconButton(onClick = onClose) {
                    Text("✕", color = DeepEyeColors.RestrictedRed, fontSize = 18.sp)
                }
            }
            
            Divider(color = DeepEyeColors.GlassBorder)
            
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                reverseLayout = false
            ) {
                items(logs) { log ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = "[${log.timestamp}] ",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        val color = when(log.type) {
                            "ERROR" -> DeepEyeColors.RestrictedRed
                            "SUCCESS" -> DeepEyeColors.SafeGreen
                            "WARNING" -> Color(0xFFF59E0B)
                            else -> DeepEyeColors.CyanAccent
                        }
                        Text(
                            text = log.message,
                            color = color,
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MainHeader(
    state: ConnState,
    onRemoteClick: () -> Unit,
    onLogoClick: () -> Unit
) {
    Surface(
        color = DeepEyeColors.SurfaceDark,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.clickable { onLogoClick() }) {
                Text("DEEP", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text("EYE", color = DeepEyeColors.CyanAccent, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            ConnectionBadge(state)
            
            Spacer(modifier = Modifier.width(12.dp))
            
            PrimaryIconButton(
                text = "REMOTE",
                onClick = onRemoteClick,
                containerColor = Color(0xFF6200EA)
            )
        }
    }
}

@Composable
private fun ConnectionBadge(state: ConnState) {
    val color = when (state) {
        ConnState.CONNECTED_READY -> DeepEyeColors.SafeGreen
        ConnState.DISCONNECTED, ConnState.ERROR -> DeepEyeColors.RestrictedRed
        else -> Color(0xFFF59E0B)
    }
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = state.name.replace("_", " "),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BrandTabs(
    brands: List<String>,
    selectedBrand: String,
    onBrandSelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1E))
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(brands) { brand ->
            val isSelected = brand == selectedBrand
            Surface(
                color = if (isSelected) DeepEyeColors.IndigoAccent else Color(0xFF2C2C30),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.clickable { onBrandSelected(brand) }
            ) {
                Text(
                    text = brand,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun ModelSelectorBar(
    selectedModel: String,
    onSelectClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF141417))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onSelectClick,
            modifier = Modifier.weight(1f).height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C30)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📱 $selectedModel", color = DeepEyeColors.CyanAccent, fontSize = 14.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text("▼", color = DeepEyeColors.CyanAccent, fontSize = 10.sp)
            }
        }
    }
}
