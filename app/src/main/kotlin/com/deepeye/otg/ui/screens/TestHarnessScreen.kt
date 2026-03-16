package com.deepeye.otg.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.ui.components.GlassButton
import com.deepeye.otg.ui.components.GlassCard
import com.deepeye.otg.usb.UsbLifecycleState
import com.deepeye.otg.usb.UsbLogger
import com.deepeye.otg.viewmodel.UsbViewModel
import dev.chrisbanes.haze.HazeState

@Composable
fun TestHarnessScreen(
    viewModel: UsbViewModel,
    modifier: Modifier = Modifier
) {
    val lifecycleState by viewModel.lifecycleState.collectAsState()
    val rawLogs by UsbLogger.logBuffer.collectAsState()
    val highLevelLogs by viewModel.logLines.collectAsState()
    val perfMode by viewModel.performanceMode.collectAsState()
    val fuzzActive by viewModel.fuzzingActive.collectAsState()
    val fuzzStats by viewModel.fuzzingStats.collectAsState()
    val exploitState by viewModel.exploitState.collectAsState()
    val hazeState = remember { HazeState() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STAGE T1: REAL DEVICE INTEGRATION",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(label = "Run Validation", onClick = { viewModel.runHardenValidation() }, modifier = Modifier.width(140.dp))
                GlassButton(label = "Clear Logs", onClick = { UsbLogger.clear() }, modifier = Modifier.width(100.dp))
                GlassButton(label = "Exit Harness", onClick = { viewModel.exitTestHarness() }, modifier = Modifier.width(120.dp), accent = true)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Device Info Card ──────────────────────────────────────
            GlassCard(
                hazeState = hazeState,
                performanceMode = perfMode,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("USB DEVICE STATUS", color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val deviceName = when(val s = lifecycleState) {
                        is UsbLifecycleState.Connected -> s.deviceName
                        is UsbLifecycleState.DeviceDetected -> s.device.productName ?: "Unknown"
                        is UsbLifecycleState.PermissionPending -> s.device.productName ?: "Pending"
                        else -> "No Device Detected"
                    }
                    
                    val vidPid = when(val s = lifecycleState) {
                        is UsbLifecycleState.DeviceDetected -> "0x${s.vendorId.toString(16)}:0x${s.productId.toString(16)}"
                        is UsbLifecycleState.Connected -> "0x${s.vendorId.toString(16)}:0x${s.productId.toString(16)}"
                        else -> "N/A"
                    }

                    val mode = when(val s = lifecycleState) {
                        is UsbLifecycleState.DeviceDetected -> s.detectedDeviceMode.name
                        is UsbLifecycleState.Connected -> s.detectedDeviceMode.name
                        else -> "IDLE"
                    }

                    val family = when (val s = lifecycleState) {
                        is UsbLifecycleState.DeviceDetected -> s.protocolFamily.name
                        is UsbLifecycleState.Connected -> s.protocolFamily.name
                        else -> "UNKNOWN"
                    }

                    val reason = when (val s = lifecycleState) {
                        is UsbLifecycleState.DeviceDetected -> s.detectionReason
                        is UsbLifecycleState.Connected -> s.detectionReason
                        else -> "N/A"
                    }

                    val key = when (val s = lifecycleState) {
                        is UsbLifecycleState.DeviceDetected -> s.deviceKey
                        is UsbLifecycleState.Connected -> s.deviceKey
                        else -> "N/A"
                    }

                    val confidence = when (val s = lifecycleState) {
                        is UsbLifecycleState.DeviceDetected -> "${s.confidence}%"
                        is UsbLifecycleState.Connected -> "${s.confidence}%"
                        else -> "N/A"
                    }

                    val interfaces = when (val s = lifecycleState) {
                        is UsbLifecycleState.DeviceDetected -> s.descriptorSnapshot.interfaces
                        is UsbLifecycleState.Connected -> s.descriptorSnapshot.interfaces
                        else -> emptyList()
                    }

                    val interfaceTuples = if (interfaces.isEmpty()) {
                        "N/A"
                    } else {
                        interfaces.joinToString(" | ") {
                            "${"%02X".format(it.interfaceClass)}/${"%02X".format(it.interfaceSubclass)}/${"%02X".format(it.interfaceProtocol)}"
                        }
                    }

                    InfoRow("Product", deviceName)
                    InfoRow("VID:PID", vidPid)
                    InfoRow("Mode", mode)
                    InfoRow("Family", family)
                    InfoRow("Confidence", confidence)
                    InfoRow("DeviceKey", key)
                    InfoRow("Reason", reason)
                    InfoRow("Ifaces", interfaceTuples)
                    
                    val handshake = if (lifecycleState is UsbLifecycleState.Connected) "PASS" else if (lifecycleState is UsbLifecycleState.Error) "FAIL" else "WAITING"
                    InfoRow("Handshake", handshake, color = if (handshake == "PASS") Color.Green else Color.Yellow)
                }
            }

            // ── Session Metrics ───────────────────────────────────────
            GlassCard(
                hazeState = hazeState,
                performanceMode = perfMode,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("LIFECYCLE EVENTS", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val currentStateName = lifecycleState::class.simpleName ?: "Unknown"
                    InfoRow("State", currentStateName)
                    InfoRow("Events", highLevelLogs.size.toString())
                    
                    val health = if (lifecycleState is UsbLifecycleState.Connected) "STABLE" else "OFFLINE"
                    InfoRow("Health", health, color = if (health == "STABLE") Color.Green else Color.Red)
                }
            }

            // ── HID Fuzzing Control ──────────────────────────────────
            GlassCard(
                hazeState = hazeState,
                performanceMode = perfMode,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("HID FUZZING (iOS 26.x RE)", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    val key = when (val s = lifecycleState) {
                        is UsbLifecycleState.DeviceDetected -> s.deviceKey
                        is UsbLifecycleState.Connected -> s.deviceKey
                        else -> null
                    }

                    InfoRow("Status", if (fuzzActive) "FUZZING..." else "IDLE", color = if (fuzzActive) Color.Red else Color.Gray)
                    InfoRow("Total Cases", fuzzStats.totalCases.toString())
                    InfoRow("Crashes Found", fuzzStats.crashesFound.toString(), color = if (fuzzStats.crashesFound > 0) Color.Red else Color.White)
                    InfoRow("Seed Index", fuzzStats.currentSeedIndex.toString())
                    
                    if (fuzzStats.lastCaseName.isNotEmpty()) {
                        Text(
                            text = "Last: ${fuzzStats.lastCaseName}",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (!fuzzActive) {
                        GlassButton(
                            label = "Start HID Fuzzer",
                            onClick = { key?.let { viewModel.startHidFuzzing(it) } },
                            enabled = key != null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        GlassButton(
                            label = "STOP FUZZER",
                            onClick = { viewModel.stopHidFuzzing() },
                            accent = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // ── Exploit Chain Control ──────────────────────────────────
            GlassCard(
                hazeState = hazeState,
                performanceMode = perfMode,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("EXPLOIT CHAIN (RESEARCH)", color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    val key = when (val s = lifecycleState) {
                        is UsbLifecycleState.DeviceDetected -> s.deviceKey
                        is UsbLifecycleState.Connected -> s.deviceKey
                        else -> null
                    }

                    val stateText = when(val s = exploitState) {
                        is com.deepeye.otg.exploit.UniversalExploitOrchestrator.ExploitState.Idle -> "READY"
                        is com.deepeye.otg.exploit.UniversalExploitOrchestrator.ExploitState.Analyzing -> "ANALYZING: ${s.brand}"
                        is com.deepeye.otg.exploit.UniversalExploitOrchestrator.ExploitState.Executing -> "EXECUTING: ${s.cveId} (${s.stage})"
                        is com.deepeye.otg.exploit.UniversalExploitOrchestrator.ExploitState.Success -> "SUCCESS: ${s.msg}"
                        is com.deepeye.otg.exploit.UniversalExploitOrchestrator.ExploitState.Failed -> "FAILED: ${s.reason}"
                    }

                    InfoRow("Chain State", stateText, color = if (exploitState is com.deepeye.otg.exploit.UniversalExploitOrchestrator.ExploitState.Failed) Color.Red else if (exploitState is com.deepeye.otg.exploit.UniversalExploitOrchestrator.ExploitState.Success) Color.Green else Color.White)

                    val isRunning = exploitState is com.deepeye.otg.exploit.UniversalExploitOrchestrator.ExploitState.Executing || 
                                    exploitState is com.deepeye.otg.exploit.UniversalExploitOrchestrator.ExploitState.Analyzing

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!isRunning) {
                        GlassButton(
                            label = "Run Exploit Chain",
                            onClick = { key?.let { viewModel.runExploitChain(it) } },
                            enabled = key != null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF8B5CF6),
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
            }


        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Raw Bulk Logs (Terminal Style) ──────────────────────────
        Text("RAW BULK TRANSFER LOGS (IO)", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            LazyColumn(
                reverseLayout = true,
                modifier = Modifier.fillMaxSize()
            ) {
                items(rawLogs.asReversed()) { line ->
                    Text(
                        text = line,
                        color = if (line.contains("E/")) Color.Red else if (line.contains("W/")) Color.Yellow else Color.Cyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, color: Color = Color.White) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
