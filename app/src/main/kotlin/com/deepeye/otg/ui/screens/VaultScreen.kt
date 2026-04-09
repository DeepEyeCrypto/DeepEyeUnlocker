package com.deepeye.otg.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deepeye.otg.data.db.entities.DeviceEntity
import com.deepeye.otg.data.db.entities.OperationLogEntity
import com.deepeye.otg.data.db.entities.SessionEntity
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.ui.theme.DeepEyeType
import com.deepeye.otg.viewmodel.ForensicVaultViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VaultScreen(
    viewModel: ForensicVaultViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val devices by viewModel.allDevices.collectAsStateWithLifecycle()
    val sessions by viewModel.deviceSessions.collectAsStateWithLifecycle()
    val logs by viewModel.sessionLogs.collectAsStateWithLifecycle()
    val selectedKey by viewModel.selectedDeviceKey.collectAsStateWithLifecycle()
    val selectedSessionId by viewModel.selectedSessionId.collectAsStateWithLifecycle()
    val exportStatus by viewModel.exportStatus.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF050505))
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Icon(
                    Icons.Default.Storage,
                    contentDescription = null,
                    tint = DeepEyeColors.NEON_PURPLE,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "FORENSIC VAULT",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }

            when {
                selectedSessionId != null -> {
                    LogList(
                        sessionId = selectedSessionId!!,
                        logs = logs,
                        onExport = { viewModel.exportAuditLog(it) },
                        onBack = { viewModel.selectSession(null) }
                    )
                }
                selectedKey != null -> {
                    SessionList(
                        deviceKey = selectedKey!!,
                        sessions = sessions,
                        onSelect = { viewModel.selectSession(it) },
                        onBack = { viewModel.selectDevice(null) }
                    )
                }
                else -> {
                    DeviceList(devices) { viewModel.selectDevice(it) }
                }
            }
        }

        // Export Status Snackbar
        AnimatedVisibility(
            visible = exportStatus != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
        ) {
            Surface(
                color = Color(0xFF111111),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, DeepEyeColors.NEON_PURPLE),
                shadowElevation = 12.dp
            ) {
                Text(
                    exportStatus ?: "",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    style = DeepEyeType.MONO.copy(fontSize = 12.sp),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun DeviceList(devices: List<DeviceEntity>, onSelect: (String) -> Unit) {
    Text(
        "REGISTERED DEVICES",
        color = Color.Gray,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )
    
    if (devices.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Vault is empty. Connect a device to register it.", color = Color.DarkGray)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(devices) { device ->
                DeviceCard(device) { onSelect(device.deviceKey) }
            }
        }
    }
}

@Composable
fun DeviceCard(device: DeviceEntity, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    
    Surface(
        onClick = onClick,
        color = Color(0xFF111111),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF222222)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Smartphone,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(device.model, color = Color.White, fontWeight = FontWeight.Bold)
                Text(device.manufacturer, color = Color.Gray, fontSize = 12.sp)
                Text(
                    "Last seen: ${dateFormat.format(Date(device.lastDetectedAt))}",
                    color = Color.DarkGray,
                    fontSize = 10.sp
                )
            }
            Text(
                device.status,
                color = DeepEyeColors.NEON_PURPLE,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .border(1.dp, DeepEyeColors.NEON_PURPLE, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun SessionList(deviceKey: String, sessions: List<SessionEntity>, onSelect: (Long) -> Unit, onBack: () -> Unit) {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "← REGISTERED DEVICES",
                color = DeepEyeColors.NEON_PURPLE,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onBack() }.padding(vertical = 8.dp)
            )
        }
        
        Spacer(Modifier.height(8.dp))
        Text(deviceKey, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text("SELECT SESSION TO VIEW AUDIT TRAIL", color = Color.Gray, fontSize = 10.sp)
        
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(sessions) { session ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0A0A0A))
                        .border(1.dp, Color(0xFF1A1A1A))
                        .clickable { onSelect(session.sessionId) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(session.connectionMode, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            dateFormat.format(Date(session.startTimestamp)),
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        session.resultStatus,
                        color = if (session.resultStatus == "ACTIVE") DeepEyeColors.NEON_GREEN else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun LogList(sessionId: Long, logs: List<OperationLogEntity>, onExport: (Long) -> Unit, onBack: () -> Unit) {
    val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "← DEVICE SESSIONS",
                color = DeepEyeColors.NEON_PURPLE,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onBack() }.padding(vertical = 8.dp)
            )
            
            Button(
                onClick = { onExport(sessionId) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.05f)),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, DeepEyeColors.NEON_PURPLE.copy(0.5f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Description, null, modifier = Modifier.size(16.dp), tint = DeepEyeColors.NEON_PURPLE)
                Spacer(Modifier.width(8.dp))
                Text("EXPORT AUDIT", fontSize = 10.sp, color = Color.White)
            }
        }
        
        Spacer(Modifier.height(8.dp))
        Text("AUDIT LOGS [ID: $sessionId]", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
        
        Spacer(Modifier.height(16.dp))

        if (logs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No operation logs recorded for this session.", color = Color.DarkGray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(logs) { log ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (log.result == "FAILED") Color(0x33FF0000) else Color.Transparent)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "[${dateFormat.format(Date(log.timestamp))}]",
                                color = Color.DarkGray,
                                style = DeepEyeType.MONO.copy(fontSize = 12.sp),
                                fontSize = 10.sp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                log.operationType,
                                color = if (log.result == "FAILED") Color.Red else DeepEyeColors.NEON_PURPLE,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            log.details,
                            color = Color.White,
                            style = DeepEyeType.MONO.copy(fontSize = 12.sp),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
