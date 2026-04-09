package com.deepeye.otg.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.otg.domain.models.DeepEyeOperation
import com.deepeye.otg.domain.models.ProtocolFamily
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.ui.theme.DeepEyeType
import com.deepeye.otg.viewmodel.UsbViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdlConsole(
    mainViewModel: UsbViewModel,
    onBack: () -> Unit
) {
    val lifecycleState by mainViewModel.lifecycleState.collectAsStateWithLifecycle()
    val logs by mainViewModel.logs.collectAsStateWithLifecycle()
    val isConnected = lifecycleState is com.deepeye.otg.usb.UsbLifecycleState.Connected &&
                     (lifecycleState as com.deepeye.otg.usb.UsbLifecycleState.Connected).protocolFamily == ProtocolFamily.EDL

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Terminal, null, tint = DeepEyeColors.NEON_PURPLE, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("EDL CONSOLE", style = DeepEyeType.CAPTION.copy(fontSize = 11.sp), color = DeepEyeColors.WHITE_HIGH)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DeepEyeColors.BG_VOID.copy(alpha = 0.9f)
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isConnected) DeepEyeColors.NEON_GREEN.copy(alpha = 0.1f) else DeepEyeColors.NEON_PINK.copy(alpha = 0.1f)
                ),
                border = BorderStroke(1.dp, if (isConnected) DeepEyeColors.NEON_GREEN.copy(alpha = 0.3f) else DeepEyeColors.NEON_PINK.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isConnected) "EDL MODE ACTIVE" else "NOT IN EDL MODE",
                            style = DeepEyeType.SUBHEADER.copy(fontSize = 20.sp),
                            color = if (isConnected) DeepEyeColors.NEON_GREEN else DeepEyeColors.NEON_PINK
                        )
                        Text(
                            text = when (lifecycleState) {
                                is com.deepeye.otg.usb.UsbLifecycleState.Connected -> "Protocol: ${(lifecycleState as com.deepeye.otg.usb.UsbLifecycleState.Connected).protocolFamily}"
                                else -> "Connect device in EDL (Sahara) mode"
                            },
                            style = DeepEyeType.BODY.copy(fontSize = 14.sp),
                            color = DeepEyeColors.WHITE_MED
                        )
                    }
                    if (isConnected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = DeepEyeColors.NEON_GREEN,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Quick Actions
            Text(
                text = "EDL OPERATIONS",
                style = DeepEyeType.CAPTION.copy(fontSize = 11.sp),
                color = DeepEyeColors.WHITE_MED,
                letterSpacing = 2.sp
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ActionButton(
                        title = "SAHARA HANDSHAKE",
                        description = "Initialize Sahara protocol handshake",
                        icon = Icons.Default.Handshake,
                        isEnabled = isConnected,
                        onClick = { mainViewModel.performQcomHandshake() }
                    )
                }

                item {
                    ActionButton(
                        title = "FIREHOSE DIAGNOSTICS",
                        description = "Read partition table and device info",
                        icon = Icons.Default.Storage,
                        isEnabled = isConnected,
                        onClick = { mainViewModel.queueOperation(DeepEyeOperation.DEEP_DEVICE_INFO) }
                    )
                }

                item {
                    ActionButton(
                        title = "BACKUP EFS / SECURITY",
                        description = "Secure backup of NVRAM, persist, EFS",
                        icon = Icons.Default.Backup,
                        isEnabled = isConnected,
                        onClick = { mainViewModel.queueOperation(DeepEyeOperation.BACKUP_EFS) }
                    )
                }

                item {
                    ActionButton(
                        title = "READ FIRMWARE",
                        description = "Dump full ROM partitions",
                        icon = Icons.Default.Download,
                        isEnabled = isConnected,
                        onClick = { mainViewModel.queueOperation(DeepEyeOperation.READ_FIRMWARE) }
                    )
                }

                item {
                    ActionButton(
                        title = "FACTORY RESET",
                        description = "Erase userdata and cache via Firehose",
                        icon = Icons.Default.Delete,
                        isEnabled = isConnected,
                        onClick = { mainViewModel.queueOperation(DeepEyeOperation.FACTORY_RESET) },
                        accentColor = DeepEyeColors.NEON_YELLOW
                    )
                }

                item {
                    ActionButton(
                        title = "ERASE FRP",
                        description = "Remove Factory Reset Protection",
                        icon = Icons.Default.LockOpen,
                        isEnabled = isConnected,
                        onClick = { mainViewModel.queueOperation(DeepEyeOperation.ERASE_FRP) },
                        accentColor = DeepEyeColors.NEON_PINK
                    )
                }

                item {
                    ActionButton(
                        title = "PARTITION MANAGER",
                        description = "View and modify GPT partition table",
                        icon = Icons.Default.Edit,
                        isEnabled = isConnected,
                        onClick = { mainViewModel.queueOperation(DeepEyeOperation.PARTITION_MANAGER) }
                    )
                }

                item {
                    ActionButton(
                        title = "SAFE DUMP (FORENSIC)",
                        description = "Bit-stream acquisition with hash verification",
                        icon = Icons.Default.BrokenImage,
                        isEnabled = isConnected,
                        onClick = { mainViewModel.queueOperation(DeepEyeOperation.SAFE_DUMP) },
                        accentColor = DeepEyeColors.NEON_PURPLE
                    )
                }
            }

            Divider(color = DeepEyeColors.WHITE_LOW.copy(0.3f).copy(alpha = 0.3f))

            // Recent Logs Preview
            Text(
                text = "RECENT LOGS",
                style = DeepEyeType.CAPTION.copy(fontSize = 11.sp),
                color = DeepEyeColors.WHITE_MED,
                letterSpacing = 2.sp
            )

            LazyColumn(
                modifier = Modifier.height(200.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs.takeLast(10).reversed()) { log ->
                    Text(
                        text = "[${log.timestamp}] ${log.type}: ${log.message}",
                        style = DeepEyeType.MONO.copy(fontSize = 12.sp),
                        color = when (log.type.uppercase()) {
                            "ERROR" -> DeepEyeColors.NEON_PINK
                            "SUCCESS" -> DeepEyeColors.NEON_GREEN
                            else -> DeepEyeColors.WHITE_MED
                        },
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isEnabled: Boolean,
    onClick: () -> Unit,
    accentColor: Color = DeepEyeColors.NEON_PURPLE
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DeepEyeColors.BG_SURFACE.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, if (isEnabled) accentColor.copy(alpha = 0.3f) else DeepEyeColors.WHITE_LOW.copy(0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isEnabled) accentColor.copy(alpha = 0.15f) else DeepEyeColors.WHITE_MED.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isEnabled) accentColor else DeepEyeColors.WHITE_MED,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = DeepEyeType.SUBHEADER.copy(fontSize = 20.sp),
                    color = if (isEnabled) DeepEyeColors.WHITE_HIGH else DeepEyeColors.WHITE_MED
                )
                Text(
                    text = description,
                    style = DeepEyeType.BODY.copy(fontSize = 14.sp),
                    color = DeepEyeColors.WHITE_MED
                )
            }

            Button(
                onClick = onClick,
                enabled = isEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEnabled) accentColor else DeepEyeColors.WHITE_MED,
                    contentColor = if (isEnabled) Color.White else DeepEyeColors.WHITE_MED
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("RUN")
            }
        }
    }
}
