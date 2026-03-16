package com.deepeye.otg.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.BuildConfig
import com.deepeye.otg.ui.theme.StitchTokens
import com.deepeye.otg.viewmodel.UsbViewModel

@Composable
fun SettingsScreen(viewModel: UsbViewModel) {
    val scrollState = rememberScrollState()
    
    // Collecting states from VM
    val perfMode by viewModel.performanceMode.collectAsState()
    val adbSig by viewModel.adbSignatureRequired.collectAsState()
    val debounce by viewModel.debounceAttach.collectAsState()
    val showDebug by viewModel.showDebugPanel.collectAsState()
    val showReason by viewModel.showDetectionReason.collectAsState()
    val forceReclass by viewModel.forceReclassify.collectAsState()
    val logToFile by viewModel.logUsbToFile.collectAsState()
    
    val licenseStatus by viewModel.licenseStatus.collectAsState()
    val activeLicense by viewModel.currentLicense.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchTokens.BackgroundDark)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        // Section header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.setNav(NavTarget.DASHBOARD) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = StitchTokens.TextPrimary)
            }
            Spacer(Modifier.width(8.dp))
            Text("ENGINE SETTINGS", style = StitchTokens.DisplayLarge.copy(fontSize = 24.sp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 1. Detection Preferences
        SettingsGroup(title = "DETECTION PREFERENCES") {
            ToggleRow(
                icon = Icons.Default.GppGood,
                title = "Require Explicit ADB Signature",
                subtitle = "Refuse ADB if vendor public key is missing.",
                checked = adbSig,
                onToggle = { viewModel.toggleAdbSignature() }
            )
            DividerLine()
            ToggleRow(
                icon = Icons.Default.Deblur,
                title = "Debounce Attach Events",
                subtitle = "Filter flaky USB cables (200ms grace).",
                checked = debounce,
                onToggle = { viewModel.toggleDebounceAttach() }
            )
            DividerLine()
            ToggleRow(
                icon = Icons.Default.Speed,
                title = "Performance Mode",
                subtitle = "Enables advanced GPU blurs and Haze effects.",
                checked = !perfMode,
                onToggle = { viewModel.togglePerformance() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Display & Feedback
        SettingsGroup(title = "DISPLAY & FEEDBACK") {
            ToggleRow(
                icon = Icons.Default.BugReport,
                title = "Show Debug Panel",
                subtitle = "Floating overlay with real-time descriptor data.",
                checked = showDebug,
                onToggle = { viewModel.toggleDebugPanel() }
            )
            DividerLine()
            ToggleRow(
                icon = Icons.Default.Info,
                title = "Show Detection Reason",
                subtitle = "Expose heuristic details on identification.",
                checked = showReason,
                onToggle = { viewModel.toggleShowDetectionReason() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Application Engine
        SettingsGroup(title = "APPLICATION ENGINE") {
            ToggleRow(
                icon = Icons.Default.Refresh,
                title = "Force Reclassify on Wake",
                subtitle = "Always re-probe device when app enters foreground.",
                checked = forceReclass,
                onToggle = { viewModel.toggleForceReclassify() }
            )
            DividerLine()
            ToggleRow(
                icon = Icons.Default.Save,
                title = "Log USB Packets to File",
                subtitle = "Internal forensics log (Pcap format compatible).",
                checked = logToFile,
                onToggle = { viewModel.toggleLogUsbToFile() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. Licensing & About
        SettingsGroup(title = "IDENTITY & LICENSING") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(StitchTokens.Primary.copy(0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.VerifiedUser, null, tint = StitchTokens.Primary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Current License Plan", style = StitchTokens.BodyMedium, fontWeight = FontWeight.Bold, color = StitchTokens.TextPrimary)
                    Text(licenseStatus.name, style = StitchTokens.LabelSmall, color = if (licenseStatus == com.deepeye.otg.domain.models.LicenseStatus.ACTIVE) Color(0xFF4ADE80) else Color.Gray)
                }
                if (licenseStatus != com.deepeye.otg.domain.models.LicenseStatus.ACTIVE) {
                    Text(
                        "UPGRADE",
                        style = StitchTokens.LabelSmall,
                        color = StitchTokens.Primary,
                        modifier = Modifier.clickable { viewModel.setActivationVisibility(true) }
                    )
                }
            }
        }

        Spacer(Modifier.height(40.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("DeepEye OTG Universal v${BuildConfig.VERSION_NAME}", style = StitchTokens.LabelSmall, color = StitchTokens.TextSecondary)
            Text("Engine: NativeCore x64/arm64 2.5.0", style = StitchTokens.LabelSmall, color = StitchTokens.TextSecondary.copy(0.5f))
        }
        
        Spacer(modifier = Modifier.height(100.dp)) // Nav bar space
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, style = StitchTokens.LabelSmall, color = StitchTokens.Primary, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(StitchTokens.SurfaceDark)
                .border(1.dp, StitchTokens.GlassBorder, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(24.dp), tint = StitchTokens.TextSecondary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = StitchTokens.BodyMedium, fontWeight = FontWeight.Bold, color = StitchTokens.TextPrimary)
            Text(subtitle, style = StitchTokens.BodyMedium.copy(fontSize = 10.sp), color = StitchTokens.TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = StitchTokens.Primary,
                checkedTrackColor = StitchTokens.Primary.copy(0.2f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.Transparent,
                uncheckedBorderColor = Color.White.copy(0.2f)
            )
        )
    }
}

@Composable
private fun DividerLine() {
    HorizontalDivider(color = Color.White.copy(0.05f), modifier = Modifier.padding(vertical = 4.dp))
}
