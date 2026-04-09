package com.deepeye.otg.ui.components

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.ui.theme.DeepEyeType
import com.deepeye.otg.viewmodel.UsbViewModel
import com.deepeye.otg.usb.UsbLifecycleState
import com.deepeye.otg.usb.UsbDescriptorSnapshot

@Composable
fun DebugOverlayPanel(viewModel: UsbViewModel) {
    val showDebug by viewModel.showDebugPanel.collectAsStateWithLifecycle()
    val lifecycleState by viewModel.lifecycleState.collectAsStateWithLifecycle()

    AnimatedVisibility(
        visible = showDebug,
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 100.dp, start = 12.dp, end = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(0.85f))
                .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp))
                .padding(8.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Terminal, null, tint = DeepEyeColors.NEON_PURPLE, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "REAL-TIME USB DESCRIPTOR", 
                            style = DeepEyeType.CAPTION.copy(fontSize = 11.sp), 
                            color = DeepEyeColors.WHITE_HIGH
                        )
                    }
                    IconButton(onClick = { viewModel.toggleDebugPanel() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }

                HorizontalDivider(color = Color.White.copy(0.05f))

                Box(modifier = Modifier.heightIn(max = 200.dp)) {
                    LazyColumn(modifier = Modifier.padding(8.dp)) {
                        when (val state = lifecycleState) {
                            is UsbLifecycleState.Connected -> {
                                item { DebugRow("DEVICE", state.deviceName) }
                                item { DebugRow("VID", "0x${Integer.toHexString(state.vendorId).uppercase()}") }
                                item { DebugRow("PID", "0x${Integer.toHexString(state.productId).uppercase()}") }
                                // Snapshot detail rows
                                state.descriptorSnapshot.toDebugRows().forEach { (key, value) ->
                                    item { DebugRow(key, value) }
                                }
                            }
                            is UsbLifecycleState.DeviceDetected -> {
                                item { DebugRow("STATUS", "DETECTED (PENDING PERM)") }
                                item { DebugRow("BRAND", state.brand) }
                                item { DebugRow("MODE", state.detectedDeviceMode.name) }
                            }
                            else -> {
                                item { Text("WAITING FOR ATTACH...", style = DeepEyeType.MONO.copy(fontSize = 12.sp), color = Color.Gray, modifier = Modifier.padding(8.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun UsbDescriptorSnapshot.toDebugRows(): List<Pair<String, String>> {
    val rows = mutableListOf<Pair<String, String>>()
    rows += "Vendor" to "0x${vendorId.toString(16).uppercase()}"
    rows += "Product" to "0x${productId.toString(16).uppercase()}"
    rows += "Class" to "$deviceClass/$deviceSubclass/$deviceProtocol"
    rows += "Manufacturer" to (manufacturerName ?: "Unknown")
    rows += "Product Name" to (productName ?: "Unknown")
    rows += "Interfaces" to interfaceCount.toString()
    interfaces.forEachIndexed { index, intf ->
        rows += "IF#$index" to "id=${intf.id} cls=${intf.interfaceClass} sub=${intf.interfaceSubclass} proto=${intf.interfaceProtocol} eps=${intf.endpointCount}"
    }
    return rows
}

@Composable
private fun DebugRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label.uppercase(), style = DeepEyeType.CAPTION.copy(fontSize = 11.sp).copy(fontSize = 9.sp), color = DeepEyeColors.WHITE_MED)
        Text(value, style = DeepEyeType.MONO.copy(fontSize = 12.sp).copy(fontSize = 10.sp), color = DeepEyeColors.NEON_CYAN)
    }
}
