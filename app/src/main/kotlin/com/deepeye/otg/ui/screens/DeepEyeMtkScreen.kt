package com.deepeye.otg.ui.screens

import android.hardware.usb.UsbDevice
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.otg.ui.components.DeviceInfoCard
import com.deepeye.otg.ui.components.ExploitMethodCard
import com.deepeye.otg.ui.components.ExploitMethodModel
import com.deepeye.otg.ui.components.ExploitRisk
import com.deepeye.otg.ui.components.NeonButton
import com.deepeye.otg.ui.components.NeonButtonStyle
import com.deepeye.otg.ui.components.SectionHeader
import com.deepeye.otg.ui.components.toConsoleEntries
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.usb.MtkChip
import com.deepeye.otg.usb.MtkDeviceInfo
import com.deepeye.otg.usb.UsbLifecycleState
import com.deepeye.otg.viewmodel.MtkExploitViewModel
import com.deepeye.otg.viewmodel.UsbViewModel

private data class MtkActionItem(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val description: String,
    val risk: ExploitRisk,
    val accent: Color,
    val execute: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepEyeMtkScreen(
    mainViewModel: UsbViewModel,
    viewModel: MtkExploitViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sessions by mainViewModel.sessions.collectAsStateWithLifecycle()
    val selectedKey by mainViewModel.selectedDeviceKey.collectAsStateWithLifecycle()
    val selectedSession = selectedKey?.let { sessions[it] } ?: sessions.values.firstOrNull() ?: UsbLifecycleState.Idle
    val presentation = sessionPresentation(selectedSession)

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedActionId by rememberSaveable { mutableStateOf("") }
    var consoleExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(selectedSession) {
        val usbDevice: UsbDevice? = when (selectedSession) {
            is UsbLifecycleState.Connected -> selectedSession.device
            is UsbLifecycleState.DeviceDetected -> selectedSession.device
            else -> null
        }
        viewModel.setDevice(usbDevice, derive_mtk_info(selectedSession))
    }

    val tabTitles = listOf("BROM", "DA", "Exploit", "Flash")
    val actions = remember(selectedTab, viewModel) {
        when (selectedTab) {
            0 -> listOf(
                MtkActionItem(
                    id = "brom-glitch",
                    icon = Icons.Default.Bolt,
                    title = "Voltage Glitch",
                    description = "Boot ROM handshake timing attack with cyan trace instrumentation.",
                    risk = ExploitRisk.HIGH,
                    accent = DeepEyeColors.Warning,
                    execute = viewModel::runVoltageGlitch,
                ),
                MtkActionItem(
                    id = "sla-bypass",
                    icon = Icons.Default.Security,
                    title = "SLA Bypass",
                    description = "Dimensity / modern MTK auth path for secured BROM sessions.",
                    risk = ExploitRisk.HIGH,
                    accent = DeepEyeColors.Success,
                    execute = viewModel::runSlaBypass,
                ),
            )

            1 -> listOf(
                MtkActionItem(
                    id = "da-auth",
                    icon = Icons.Default.Memory,
                    title = "DA Auth Bypass",
                    description = "Upload patched Download Agent and negotiate auth-free session state.",
                    risk = ExploitRisk.MED,
                    accent = DeepEyeColors.PrimaryCyan,
                    execute = viewModel::runDaAuthBypass,
                ),
                MtkActionItem(
                    id = "meta-mode",
                    icon = Icons.Default.Build,
                    title = "META Mode",
                    description = "Engineering path for screen-lock and service operations.",
                    risk = ExploitRisk.MED,
                    accent = DeepEyeColors.PurpleDim,
                    execute = { viewModel.runScreenLockBypass(com.deepeye.otg.data.model.MtkScreenBypassMethod.META_MODE) },
                ),
            )

            2 -> listOf(
                MtkActionItem(
                    id = "frp-bypass",
                    icon = Icons.Default.FlashOn,
                    title = "FRP Bypass",
                    description = "Reset Google lock state using guided MediaTek maintenance flow.",
                    risk = ExploitRisk.MED,
                    accent = DeepEyeColors.PrimaryCyan,
                    execute = { viewModel.runScreenLockBypass(com.deepeye.otg.data.model.MtkScreenBypassMethod.FRP_BYPASS) },
                ),
                MtkActionItem(
                    id = "frida-hook",
                    icon = Icons.Default.Security,
                    title = "Frida Hook",
                    description = "Interactive keyguard instrumentation for live verification research.",
                    risk = ExploitRisk.HIGH,
                    accent = DeepEyeColors.Error,
                    execute = { viewModel.runScreenLockBypass(com.deepeye.otg.data.model.MtkScreenBypassMethod.FRIDA_HOOK) },
                ),
                MtkActionItem(
                    id = "adb-backup",
                    icon = Icons.Default.Build,
                    title = "ADB Backup",
                    description = "Legacy settings-navigation flow for older Android generations.",
                    risk = ExploitRisk.LOW,
                    accent = DeepEyeColors.Success,
                    execute = { viewModel.runScreenLockBypass(com.deepeye.otg.data.model.MtkScreenBypassMethod.ADB_BACKUP) },
                ),
                MtkActionItem(
                    id = "brom-wipe",
                    icon = Icons.Default.Memory,
                    title = "BROM Wipe",
                    description = "Erase lock database artefacts through Boot ROM service access.",
                    risk = ExploitRisk.HIGH,
                    accent = DeepEyeColors.Warning,
                    execute = { viewModel.runScreenLockBypass(com.deepeye.otg.data.model.MtkScreenBypassMethod.BROM_WIPE) },
                ),
            )

            else -> listOf(
                MtkActionItem(
                    id = "force-unlock",
                    icon = Icons.Default.FlashOn,
                    title = "Force BL Unlock",
                    description = "Full four-stage unlock routine with destructive flash side-effects.",
                    risk = ExploitRisk.HIGH,
                    accent = DeepEyeColors.Error,
                    execute = viewModel::runForceBlUnlock,
                ),
            )
        }
    }

    LaunchedEffect(selectedTab) {
        selectedActionId = actions.firstOrNull()?.id.orEmpty()
    }

    val selectedAction = actions.firstOrNull { it.id == selectedActionId } ?: actions.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        SectionHeader(title = "MediaTek Tools", count = presentation.badge)

        DeviceInfoCard(
            title = presentation.title,
            brand = presentation.badge,
            subtitle = presentation.subtitle,
            fields = if (presentation.fields.isEmpty()) {
                listOf(
                    com.deepeye.otg.ui.components.DeviceField("Status", "Waiting for MTK device"),
                    com.deepeye.otg.ui.components.DeviceField("Transport", "USB OTG"),
                )
            } else {
                presentation.fields
            },
            thumbnail = Icons.Default.Memory,
            status = presentation.status,
            accentColor = if (presentation.accent == Color.Transparent) DeepEyeColors.Success else presentation.accent,
            active = selectedSession is UsbLifecycleState.Connected,
            modifier = Modifier.fillMaxWidth(),
        )

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            edgePadding = 0.dp,
            contentColor = DeepEyeColors.PrimaryCyan,
        ) {
            tabTitles.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(tab) },
                )
            }
        }

        Text(
            text = "Select one method and execute it through the Deep Eye pipeline.",
            style = MaterialTheme.typography.bodySmall,
            color = DeepEyeColors.TextSecondary,
        )

        actions.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { action ->
                    ExploitMethodCard(
                        method = ExploitMethodModel(
                            id = action.id,
                            icon = action.icon,
                            name = action.title,
                            description = action.description,
                            risk = action.risk,
                            accentColor = action.accent,
                        ),
                        selected = action.id == selectedActionId,
                        onClick = { selectedActionId = action.id },
                        enabled = !state.isWorking,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(2 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (consoleExpanded) "Console expanded" else "Console collapsed",
                style = MaterialTheme.typography.labelSmall,
                color = DeepEyeColors.TextSecondary,
            )
            Text(
                text = if (consoleExpanded) "Tap below to minimize" else "Tap below to expand",
                style = MaterialTheme.typography.labelSmall,
                color = DeepEyeColors.PrimaryCyan,
            )
        }

        com.deepeye.otg.ui.components.GlassCard(
            hazeState = null,
            onClick = { consoleExpanded = consoleExpanded == false },
            modifier = Modifier.fillMaxWidth(),
            accentColor = DeepEyeColors.PrimaryCyan.copy(alpha = 0.5f),
        ) {
            com.deepeye.otg.ui.components.LogConsole(
                entries = state.logs.toConsoleEntries(),
                title = "MTK Execution Console",
                onClear = { viewModel.clearLogs() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (consoleExpanded) 280.dp else 160.dp),
            )
        }

        NeonButton(
            text = selectedAction?.let { "▶ Run ${it.title}" } ?: "Select a method",
            onClick = { selectedAction?.execute?.invoke() },
            enabled = !state.isWorking && selectedAction != null,
            loading = state.isWorking,
        )

        if (selectedAction != null) {
            Text(
                text = selectedAction.description,
                style = MaterialTheme.typography.bodySmall,
                color = DeepEyeColors.TextSecondary,
            )
        }

        Spacer(modifier = Modifier.height(84.dp))
    }
}

private fun derive_mtk_info(state: UsbLifecycleState): MtkDeviceInfo? = when (state) {
    is UsbLifecycleState.Connected -> MtkDeviceInfo(
        chip = detect_mtk_chip(state.chipset),
        chipId = "0x${state.productId.toString(16).uppercase()}",
        hwCode = state.productId,
        hwVersion = state.vendorId.toString(16).uppercase(),
        swVersion = state.confidence.toString(),
        isInBromMode = state.protocolFamily.name.contains("BROM"),
        isInPreloaderMode = state.protocolFamily.name.contains("PRELOADER"),
        supportsSla = true,
        supportsGlitch = true,
        daRequired = true,
    )

    is UsbLifecycleState.DeviceDetected -> MtkDeviceInfo(
        chip = detect_mtk_chip(state.chipset),
        chipId = "0x${state.productId.toString(16).uppercase()}",
        hwCode = state.productId,
        isInBromMode = state.protocolFamily.name.contains("BROM"),
        isInPreloaderMode = state.protocolFamily.name.contains("PRELOADER"),
        supportsSla = true,
        supportsGlitch = state.protocolFamily.name.contains("BROM"),
        daRequired = true,
    )

    else -> null
}

private fun detect_mtk_chip(chipset: String): MtkChip {
    val normalized = chipset.uppercase().replace("-", "").replace(" ", "")
    return enumValues<MtkChip>().firstOrNull { normalized.contains(it.name) } ?: MtkChip.UNKNOWN
}
