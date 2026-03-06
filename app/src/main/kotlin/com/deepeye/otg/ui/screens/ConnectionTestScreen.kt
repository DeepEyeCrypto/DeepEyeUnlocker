package com.deepeye.otg.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.ui.components.*
import com.deepeye.otg.viewmodel.UsbViewModel
import com.deepeye.otg.viewmodel.UsbViewModel.DiagnosticStatus

@Composable
fun ConnectionTestScreen(viewModel: UsbViewModel) {
    val diagnosticSteps by viewModel.diagnosticSteps.collectAsState()
    val otgResult by viewModel.otgResult.collectAsState()

    val steps = listOf(
        1 to "Host Phone OTG Support",
        2 to "USB Cable Connection",
        3 to "Device Recognition",
        4 to "Mode Auto-Detection",
        5 to "USB Permission",
        6 to "Interface Claim",
        7 to "Endpoint Resolution",
        8 to "Communication Ping"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "OTG Connection Diagnostic",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = otgResult?.recommendation ?: "Checking OTG capability...",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        items(steps) { (stepNum, name) ->
            val status = diagnosticSteps[stepNum] ?: DiagnosticStatus.Idle
            DiagnosticStepCard(stepNum, name, status)
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            ConnectionGuideCard()
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            NoDriverNeededCard()
        }
    }
}

@Composable
fun DiagnosticStepCard(stepNum: Int, name: String, status: DiagnosticStatus) {
    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "#$stepNum",
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Column(modifier = Modifier.weight(1) ) {
                Text(name, color = Color.White, fontWeight = FontWeight.SemiBold)
                if (status is DiagnosticStatus.Pass) {
                    Text(status.msg, color = Color.Green.copy(alpha = 0.7f), fontSize = 12.sp)
                } else if (status is DiagnosticStatus.Fail) {
                    Text(status.msg, color = Color.Red.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }

            DiagnosticStatusIcon(status)
        }
    }
}

@Composable
fun DiagnosticStatusIcon(status: DiagnosticStatus) {
    when (status) {
        is DiagnosticStatus.Pass -> Icon(Icons.Default.CheckCircle, "Pass", tint = Color.Green)
        is DiagnosticStatus.Fail -> Icon(Icons.Default.Error, "Fail", tint = Color.Red)
        is DiagnosticStatus.Loading -> {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color.Cyan)
        }
        else -> Icon(Icons.Default.Pending, "Pending", tint = Color.White.copy(alpha = 0.3f))
    }
}

@Composable
fun ConnectionGuideCard() {
    GlassCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("How to Connect Target phone", fontWeight = FontWeight.Bold, color = Color.Cyan)
            Spacer(modifier = Modifier.height(8.dp))
            
            GuideStep("1", "Enable USB Debugging on target phone (for ADB mode)")
            GuideStep("2", "Connect OTG Adapter to Host phone (this phone)")
            GuideStep("3", "Connect Target phone via standard USB cable to OTG adapter")
            GuideStep("4", "Accept 'Allow USB Debugging' if it appears on target")
            GuideStep("5", "Accept 'DeepEye OTG Permission' on host phone")
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Note: EDL and BROM modes do NOT require USB Debugging.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun GuideStep(num: String, text: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("$num.", fontWeight = FontWeight.Bold, color = Color.Cyan, modifier = Modifier.width(24.dp))
        Text(text, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
    }
}

@Composable
fun NoDriverNeededCard() {
    GlassCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, "No Drivers", tint = Color.Yellow)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Zero Drivers Required", fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "DeepEye uses Android's native USB Host API to speak directly to device hardware. " +
                "Unlike a PC, no Qualcomm/MTK/Samsung drivers are needed. The app is the driver.",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
