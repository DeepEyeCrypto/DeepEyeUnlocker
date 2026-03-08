package com.deepeye.otg.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.viewmodel.UsbViewModel.DiagnosticStatus
import com.deepeye.otg.usb.OtgCapabilityResult
import com.deepeye.otg.ui.components.GlassCard

@Composable
fun ConnectionTestScreen(
    otgResult: OtgCapabilityResult?,
    diagnosticSteps: Map<Int, DiagnosticStatus>
) {
    val steps = listOf(
        1 to "USB Host Hardware Check",
        2 to "OTG Cable/Hub Detection",
        3 to "Target Device Recognition",
        4 to "Connection Mode Verification",
        5 to "USB Permission Status",
        6 to "Kernel Driver Claim",
        7 to "I/O Endpoint Handshake"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text(
                "OTG Connection Diagnostic",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4C1D95), // Dark Violet
                modifier = Modifier.padding(bottom = 8.dp)
            )
            GlassCard(hazeState = null, performanceMode = false) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = otgResult?.recommendation ?: "Checking OTG capability...",
                        color = Color(0xFF1D1B20).copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        steps.forEach { step ->
            val (stepNum, name) = step
            val status = diagnosticSteps[stepNum] ?: DiagnosticStatus.Idle
            DiagnosticStepCard(stepNum, name, status)
        }

        Spacer(modifier = Modifier.height(4.dp))
        ConnectionGuideCard()

        Spacer(modifier = Modifier.height(4.dp))
        NoDriverNeededCard()
    }
}

@Composable
fun DiagnosticStepCard(stepNum: Int, name: String, status: DiagnosticStatus) {
    GlassCard(hazeState = null, performanceMode = false) {
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
                    color = Color(0xFF4C1D95).copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = Color(0xFF1D1B20), fontWeight = FontWeight.SemiBold)
                if (status is DiagnosticStatus.Pass) {
                    Text(status.msg, color = Color(0xFF059669), fontSize = 12.sp) // Darker Green
                } else if (status is DiagnosticStatus.Fail) {
                    Text(status.msg, color = Color(0xFFDC2626), fontSize = 12.sp) // Darker Red
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
        is DiagnosticStatus.Fail -> Icon(Icons.Default.Close, "Fail", tint = Color.Red)
        is DiagnosticStatus.Loading -> {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color.Cyan)
        }
        else -> Icon(Icons.Default.Refresh, "Pending", tint = Color.White.copy(alpha = 0.3f))
    }
}

@Composable
fun ConnectionGuideCard() {
    GlassCard(hazeState = null, performanceMode = false) {
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
                color = Color(0xFF1D1B20).copy(alpha = 0.6f)
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
    GlassCard(hazeState = null, performanceMode = false) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Build, "No Drivers", tint = Color.Yellow)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Zero Drivers Required", fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "DeepEye uses Android's native USB Host API to speak directly to device hardware. " +
                "Unlike a PC, no Qualcomm/MTK/Samsung drivers are needed. The app is the driver.",
                fontSize = 13.sp,
                color = Color(0xFF1D1B20).copy(alpha = 0.7f)
            )
        }
    }
}
