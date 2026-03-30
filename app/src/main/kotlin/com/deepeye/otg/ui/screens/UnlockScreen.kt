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
import androidx.compose.ui.text.font.FontWeight
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
fun UnlockScreen(
    mainViewModel: UsbViewModel,
    onBack: () -> Unit
) {
    val lifecycleState by mainViewModel.lifecycleState.collectAsStateWithLifecycle()
    val logs by mainViewModel.logs.collectAsStateWithLifecycle()
    val selectedDeviceKey by mainViewModel.selectedDeviceKey.collectAsStateWithLifecycle()

    val isConnected = lifecycleState is com.deepeye.otg.usb.UsbLifecycleState.Connected
    val deviceName = when (lifecycleState) {
        is com.deepeye.otg.usb.UsbLifecycleState.Connected -> (lifecycleState as com.deepeye.otg.usb.UsbLifecycleState.Connected).deviceName
        else -> "Unknown Device"
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LockOpen, null, tint = StitchTokens.Primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("UNLOCK & BYPASS", style = StitchTokens.LabelSmall, color = StitchTokens.TextPrimary)
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
            // Device Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isConnected) StitchTokens.AccentSuccess.copy(alpha = 0.1f) else StitchTokens.AccentWarning.copy(alpha = 0.1f)
                ),
                border = BorderStroke(1.dp, if (isConnected) StitchTokens.AccentSuccess.copy(alpha = 0.3f) else StitchTokens.AccentWarning.copy(alpha = 0.3f))
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
                            text = if (isConnected) "DEVICE CONNECTED" else "NO DEVICE",
                            style = StitchTokens.TitleLarge,
                            color = if (isConnected) StitchTokens.AccentSuccess else StitchTokens.AccentWarning
                        )
                        Text(
                            text = deviceName,
                            style = StitchTokens.BodyMedium,
                            color = StitchTokens.TextPrimary
                        )
                        if (selectedDeviceKey != null) {
                            Text(
                                text = "Key: $selectedDeviceKey",
                                style = StitchTokens.LabelSmall,
                                color = StitchTokens.TextSecondary
                            )
                        }
                    }

                    // Status Badge (simple)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isConnected) StitchTokens.AccentSuccess.copy(alpha = 0.15f) else StitchTokens.AccentWarning.copy(alpha = 0.15f))
                            .border(1.dp, if (isConnected) StitchTokens.AccentSuccess.copy(alpha = 0.3f) else StitchTokens.AccentWarning.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isConnected) "READY" else "WAITING",
                            style = StitchTokens.LabelSmall,
                            color = if (isConnected) StitchTokens.AccentSuccess else StitchTokens.AccentWarning
                        )
                    }
                }
            }

            // Warning Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = StitchTokens.AccentError.copy(alpha = 0.1f)
                ),
                border = BorderStroke(1.dp, StitchTokens.AccentError.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = StitchTokens.AccentError,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "LEGAL COMPLIANCE NOTICE",
                            style = StitchTokens.LabelSmall,
                            color = StitchTokens.AccentError,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Only perform on devices you own. Bypassing security features may violate laws in your jurisdiction.",
                            style = StitchTokens.BodyMedium,
                            color = StitchTokens.TextSecondary
                        )
                    }
                }
            }

            // Unlock Operations
            Text(
                text = "UNLOCK OPERATIONS",
                style = StitchTokens.LabelSmall,
                color = StitchTokens.TextSecondary,
                letterSpacing = 2.sp
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // FRP Bypass (Google)
                item {
                    ActionButton(
                        title = "GOOGLE FRP BYPASS",
                        description = "Remove Factory Reset Protection (Google Account)",
                        icon = Icons.Default.AccountCircle,
                        isEnabled = isConnected,
                        onClick = { mainViewModel.queueOperation(DeepEyeOperation.ERASE_FRP) },
                        accentColor = StitchTokens.AccentError,
                        requires = "EDL / Fastboot / ADB"
                    )
                }

                // Samsung FRP
                item {
                    ActionButton(
                        title = "SAMSUNG FRP / CLOUD",
                        description = "Remove Samsung account binding and Google FRP",
                        icon = Icons.Default.CloudOff,
                        isEnabled = isConnected,
                        onClick = { mainViewModel.queueOperation(DeepEyeOperation.MTK_METAMODE_FRP) },
                        accentColor = StitchTokens.AccentError,
                        requires = "EDL / Download Mode"
                    )
                }

                // Mi Cloud Removal
                item {
                    ActionButton(
                        title = "MI CLOUD REMOVAL",
                        description = "Remove Mi Cloud activation lock",
                        icon = Icons.Default.CloudQueue,
                        isEnabled = isConnected,
                        onClick = { mainViewModel.queueOperation(DeepEyeOperation.REMOVE_MI_CLOUD) },
                        accentColor = StitchTokens.AccentWarning,
                        requires = "MTK EDL"
                    )
                }

                // Bootloader Unlock
                item {
                    ActionButton(
                        title = "BOOTLOADER UNLOCK",
                        description = "Unlock bootloader for custom ROMs",
                        icon = Icons.Default.FlashAuto,
                        isEnabled = isConnected,
                        onClick = { mainViewModel.queueOperation(DeepEyeOperation.UNLOCK_BOOTLOADER) },
                        accentColor = StitchTokens.Primary,
                        requires = "Fastboot / EDL"
                    )
                }

                // Screen Lock Removal
                item {
                    ActionButton(
                        title = "SCREEN LOCK REMOVAL",
                        description = "Remove PIN, Pattern, Password locks",
                        icon = Icons.Default.LockOpen,
                        isEnabled = isConnected,
                        onClick = { mainViewModel.queueOperation(DeepEyeOperation.REMOVE_SCREEN_LOCK) },
                        accentColor = StitchTokens.AccentWarning,
                        requires = "EDL (userdata access)"
                    )
                }

                // Enterprise MDM Removal
                item {
                    ActionButton(
                        title = "ENTERPRISE MDM HOOK",
                        description = "Remove device owner and MDM profiles",
                        icon = Icons.Default.AdminPanelSettings,
                        isEnabled = isConnected,
                        onClick = { mainViewModel.queueOperation(DeepEyeOperation.EFRP_MDM_HOOK) },
                        accentColor = StitchTokens.AccentError,
                        requires = "EDL / ADB"
                    )
                }

                // Demo to Retail
                item {
                    ActionButton(
                        title = "DEMO → RETAIL",
                        description = "Convert demo units to retail configuration",
                        icon = Icons.Default.Shop,
                        isEnabled = isConnected,
                        onClick = { mainViewModel.queueOperation(DeepEyeOperation.DEMO_UNLOCK) },
                        accentColor = StitchTokens.Primary,
                        requires = "EDL / Fastboot"
                    )
                }
            }

            Divider(color = StitchTokens.GlassBorder.copy(alpha = 0.3f))

            // Operation Status
            Text(
                text = "OPERATION STATUS",
                style = StitchTokens.LabelSmall,
                color = StitchTokens.TextSecondary,
                letterSpacing = 2.sp
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = StitchTokens.Semantic.SurfaceCard.copy(alpha = 0.6f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Current Operation:",
                            style = StitchTokens.BodyMedium,
                            color = StitchTokens.TextSecondary
                        )
                        Text(
                            text = "IDLE", // TODO: Connect to actual queue status
                            style = StitchTokens.LabelSmall,
                            color = StitchTokens.Primary
                        )
                    }

                    // Progress indicator if executing
                    LinearProgressIndicator(
                        progress = { 0f },
                        modifier = Modifier.fillMaxWidth(),
                        color = StitchTokens.Primary,
                        trackColor = StitchTokens.GlassBorder
                    )
                    Text(
                        text = "0% - No active operation",
                        style = StitchTokens.BodyMedium,
                        color = StitchTokens.TextSecondary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Operation control note
                    Text(
                        text = "Operations execute automatically. Monitor logs for progress.",
                        style = StitchTokens.BodyMedium,
                        color = StitchTokens.TextSecondary,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            // Recent Logs Preview
            Text(
                text = "RECENT ACTIVITY",
                style = StitchTokens.LabelSmall,
                color = StitchTokens.TextSecondary,
                letterSpacing = 2.sp
            )

            LazyColumn(
                modifier = Modifier.height(180.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs.takeLast(8).reversed()) { log ->
                    Text(
                        text = "[${log.timestamp}] ${log.type}: ${log.message}",
                        style = StitchTokens.MonoCode,
                        color = when (log.type.uppercase()) {
                            "ERROR" -> StitchTokens.AccentError
                            "SUCCESS" -> StitchTokens.AccentSuccess
                            "INFO" -> StitchTokens.Primary
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
    accentColor: Color,
    requires: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = StitchTokens.Semantic.SurfaceCard.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, if (isEnabled) accentColor.copy(alpha = 0.3f) else StitchTokens.GlassBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    Text(
                        text = "Requires: $requires",
                        style = StitchTokens.LabelSmall,
                        color = StitchTokens.TextSecondary,
                        fontSize = 10.sp
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
}
