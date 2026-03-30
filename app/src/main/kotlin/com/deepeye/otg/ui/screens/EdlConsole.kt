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
import com.deepeye.otg.ui.theme.StitchTokens
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
                        Icon(Icons.Default.Terminal, null, tint = StitchTokens.Primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("EDL CONSOLE", style = StitchTokens.LabelSmall, color = StitchTokens.TextPrimary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = StitchTokens.BackgroundDark.copy(alpha = 0.9f)
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
                    containerColor = if (isConnected) StitchTokens.AccentSuccess.copy(alpha = 0.1f) else StitchTokens.AccentError.copy(alpha = 0.1f)
                ),
                border = BorderStroke(1.dp, if (isConnected) StitchTokens.AccentSuccess.copy(alpha = 0.3f) else StitchTokens.AccentError.copy(alpha = 0.3f))
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
                            style = StitchTokens.TitleLarge,
                            color = if (isConnected) StitchTokens.AccentSuccess else StitchTokens.AccentError
                        )
                        Text(
                            text = when (lifecycleState) {
                                is com.deepeye.otg.usb.UsbLifecycleState.Connected -> "Protocol: ${(lifecycleState as com.deepeye.otg.usb.UsbLifecycleState.Connected).protocolFamily}"
                                else -> "Connect device in EDL (Sahara) mode"
                            },
                            style = StitchTokens.BodyMedium,
                            color = StitchTokens.TextSecondary
                        )
                    }
                    if (isConnected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StitchTokens.AccentSuccess,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Quick Actions
            Text(
                text = "EDL OPERATIONS",
                style = StitchTokens.LabelSmall,
                color = StitchTokens.TextSecondary,
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
                        accentColor = StitchTokens.AccentWarning
                    )
                }

                item {
                    ActionButton(
                        title = "ERASE FRP",
                        description = "Remove Factory Reset Protection",
                        icon = Icons.Default.LockOpen,
                        isEnabled = isConnected,
                        onClick = { mainViewModel.queueOperation(DeepEyeOperation.ERASE_FRP) },
                        accentColor = StitchTokens.AccentError
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
                        accentColor = StitchTokens.Primary
                    )
                }
            }

            Divider(color = StitchTokens.GlassBorder.copy(alpha = 0.3f))

            // Recent Logs Preview
            Text(
                text = "RECENT LOGS",
                style = StitchTokens.LabelSmall,
                color = StitchTokens.TextSecondary,
                letterSpacing = 2.sp
            )

            LazyColumn(
                modifier = Modifier.height(200.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs.takeLast(10).reversed()) { log ->
                    Text(
                        text = "[${log.timestamp}] ${log.type}: ${log.message}",
                        style = StitchTokens.MonoCode,
                        color = when (log.type.uppercase()) {
                            "ERROR" -> StitchTokens.AccentError
                            "SUCCESS" -> StitchTokens.AccentSuccess
                            else -> StitchTokens.TextSecondary
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
    accentColor: Color = StitchTokens.Primary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = StitchTokens.Semantic.SurfaceCard.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, if (isEnabled) accentColor.copy(alpha = 0.3f) else StitchTokens.GlassBorder),
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
                    .background(if (isEnabled) accentColor.copy(alpha = 0.15f) else StitchTokens.TextSecondary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isEnabled) accentColor else StitchTokens.TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = StitchTokens.TitleLarge,
                    color = if (isEnabled) StitchTokens.TextPrimary else StitchTokens.TextSecondary
                )
                Text(
                    text = description,
                    style = StitchTokens.BodyMedium,
                    color = StitchTokens.TextSecondary
                )
            }

            Button(
                onClick = onClick,
                enabled = isEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEnabled) accentColor else StitchTokens.TextSecondary,
                    contentColor = if (isEnabled) Color.White else StitchTokens.TextSecondary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("RUN")
            }
        }
    }
}
