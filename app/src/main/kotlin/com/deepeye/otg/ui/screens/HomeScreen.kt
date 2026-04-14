package com.deepeye.otg.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.deepeye.otg.ui.components.ExploitMethodCard
import com.deepeye.otg.ui.components.ExploitMethodModel
import com.deepeye.otg.ui.components.ExploitRisk
import com.deepeye.otg.ui.components.GlassCard
import com.deepeye.otg.ui.components.LogConsole
import com.deepeye.otg.ui.components.NeonButton
import com.deepeye.otg.ui.components.RadarAnimation
import com.deepeye.otg.ui.components.SectionHeader
import com.deepeye.otg.ui.components.StatusIndicator
import com.deepeye.otg.ui.components.toConsoleEntries
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.ui.viewmodel.LogEntry
import com.deepeye.otg.usb.UsbLifecycleState

@Composable
fun HomeScreen(
    selectedSession: UsbLifecycleState,
    recentLogs: List<LogEntry>,
    connectedCount: Int,
    modifier: Modifier = Modifier,
    onNavigateDevices: () -> Unit,
    onNavigateApple: () -> Unit,
    onNavigateLogs: () -> Unit,
    onNavigateMtk: () -> Unit,
    onNavigateFrp: () -> Unit,
) {
    val session = sessionPresentation(selectedSession)
    val quickActions = listOf(
        ExploitMethodModel(
            id = "mtk",
            icon = Icons.Default.Usb,
            name = "MTK Bypass",
            description = "Boot ROM, DA, SLA and FRP maintenance workflows.",
            risk = ExploitRisk.MED,
            accentColor = DeepEyeColors.Success,
        ),
        ExploitMethodModel(
            id = "qc",
            icon = Icons.Default.DeveloperBoard,
            name = "QC EDL",
            description = "9008 access, Firehose staging, and low-level diagnostics.",
            risk = ExploitRisk.HIGH,
            accentColor = DeepEyeColors.Warning,
        ),
        ExploitMethodModel(
            id = "apple",
            icon = Icons.Default.PhoneIphone,
            name = "Apple ProTools",
            description = "Recovery, DFU, activation checks, and iRecovery console.",
            risk = ExploitRisk.LOW,
            accentColor = DeepEyeColors.PurpleDim,
        ),
        ExploitMethodModel(
            id = "frp",
            icon = Icons.Default.LockOpen,
            name = "FRP Toolkit",
            description = "Queue Google lock workflows and guided maintenance paths.",
            risk = ExploitRisk.MED,
            accentColor = DeepEyeColors.PrimaryCyan,
        ),
    )

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        SectionHeader(title = "DeepEye Dashboard", count = "$connectedCount live")

        GlassCard(hazeState = null, accentColor = session.accent) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = session.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = DeepEyeColors.TextPrimary,
                        )
                        StatusIndicator(state = session.status, label = session.subtitle)
                    }

                    RadarAnimation(
                        modifier = Modifier.size(120.dp),
                        accentColor = session.accent,
                        active = session.status != com.deepeye.otg.ui.components.StatusIndicatorState.ERROR,
                    )
                }

                NeonButton(
                    text = "Open Device Tools",
                    onClick = onNavigateDevices,
                )
            }
        }

        SectionHeader(title = "Quick Actions", count = quickActions.size.toString())
        quickActions.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { action ->
                    ExploitMethodCard(
                        method = action,
                        selected = false,
                        onClick = when (action.id) {
                            "mtk" -> onNavigateMtk
                            "apple" -> onNavigateApple
                            "frp" -> onNavigateFrp
                            else -> onNavigateDevices
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        SectionHeader(title = "Recent Activity", count = recentLogs.takeLast(6).size.toString())
        LogConsole(
            entries = recentLogs.takeLast(6).toConsoleEntries(),
            title = "Recent Session Activity",
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
        )

        NeonButton(
            text = "Open Full Log Viewer",
            onClick = onNavigateLogs,
            style = com.deepeye.otg.ui.components.NeonButtonStyle.SECONDARY,
        )
        Spacer(modifier = Modifier.height(84.dp))
    }
}
