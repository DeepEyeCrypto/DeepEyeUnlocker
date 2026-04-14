package com.deepeye.otg.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.deepeye.otg.ui.components.DeviceInfoCard
import com.deepeye.otg.ui.components.ExploitMethodCard
import com.deepeye.otg.ui.components.ExploitMethodModel
import com.deepeye.otg.ui.components.ExploitRisk
import com.deepeye.otg.ui.components.LogConsole
import com.deepeye.otg.ui.components.SectionHeader
import com.deepeye.otg.ui.components.toConsoleEntries
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.ui.viewmodel.LogEntry
import com.deepeye.otg.usb.UsbLifecycleState

@Composable
fun DeepEyeDevicesScreen(
    selectedSession: UsbLifecycleState,
    recentLogs: List<LogEntry>,
    modifier: Modifier = Modifier,
    onNavigateMtk: () -> Unit,
    onNavigateEdl: () -> Unit,
    onNavigateFrp: () -> Unit,
    onNavigateDiagnostics: () -> Unit,
) {
    val session = sessionPresentation(selectedSession)
    val toolCards = listOf(
        ExploitMethodModel(
            id = "mtk-tools",
            icon = Icons.Default.Memory,
            name = "MediaTek",
            description = "BROM, SLA, DA upload, and partition workflows.",
            risk = ExploitRisk.MED,
            accentColor = DeepEyeColors.Success,
        ),
        ExploitMethodModel(
            id = "qcom-edl",
            icon = Icons.Default.DeveloperBoard,
            name = "Qualcomm EDL",
            description = "Sahara / Firehose console and 9008 maintenance entry.",
            risk = ExploitRisk.HIGH,
            accentColor = DeepEyeColors.Warning,
        ),
        ExploitMethodModel(
            id = "frp-stack",
            icon = Icons.Default.LockReset,
            name = "FRP Stack",
            description = "Queue guided Android lock reset and account removal flows.",
            risk = ExploitRisk.MED,
            accentColor = DeepEyeColors.PrimaryCyan,
        ),
        ExploitMethodModel(
            id = "diag",
            icon = Icons.Default.BugReport,
            name = "Diagnostics",
            description = "Descriptor analysis, device support, and protocol inspection.",
            risk = ExploitRisk.LOW,
            accentColor = DeepEyeColors.PurpleDim,
        ),
    )

    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        SectionHeader(title = "Device Tools", count = session.badge)

        if (selectedSession is UsbLifecycleState.Connected || selectedSession is UsbLifecycleState.DeviceDetected) {
            DeviceInfoCard(
                title = session.title,
                brand = session.badge,
                subtitle = session.subtitle,
                fields = session.fields,
                thumbnail = Icons.Default.Memory,
                status = session.status,
                accentColor = session.accent,
                active = selectedSession is UsbLifecycleState.Connected,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            DeviceScanScreen(
                selectedSession = selectedSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp),
            )
        }

        Text(
            text = "Choose a tool suite to open protocol-specific workflows.",
            style = MaterialTheme.typography.bodySmall,
            color = DeepEyeColors.TextSecondary,
        )

        toolCards.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { card ->
                    ExploitMethodCard(
                        method = card,
                        selected = false,
                        onClick = when (card.id) {
                            "mtk-tools" -> onNavigateMtk
                            "qcom-edl" -> onNavigateEdl
                            "frp-stack" -> onNavigateFrp
                            else -> onNavigateDiagnostics
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        SectionHeader(title = "Live Tail", count = recentLogs.takeLast(5).size.toString())
        LogConsole(
            entries = recentLogs.takeLast(5).toConsoleEntries(),
            title = "Protocol Tail",
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
        )

        Spacer(modifier = Modifier.height(84.dp))
    }
}
