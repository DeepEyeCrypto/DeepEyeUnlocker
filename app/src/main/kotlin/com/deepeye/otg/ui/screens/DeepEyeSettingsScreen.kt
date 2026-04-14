package com.deepeye.otg.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.otg.BuildConfig
import com.deepeye.otg.ui.components.GlassCard
import com.deepeye.otg.ui.components.NeonButton
import com.deepeye.otg.ui.components.NeonButtonStyle
import com.deepeye.otg.ui.components.SectionHeader
import com.deepeye.otg.ui.settings.ThemePreferences
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.ui.theme.ThemeMode
import com.deepeye.otg.viewmodel.UsbViewModel
import kotlinx.coroutines.launch

@Composable
fun DeepEyeSettingsScreen(viewModel: UsbViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeMode by ThemePreferences.getThemeModeFlow(context).collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val perfMode by viewModel.performanceMode.collectAsStateWithLifecycle()
    val adbSig by viewModel.adbSignatureRequired.collectAsStateWithLifecycle()
    val debounce by viewModel.debounceAttach.collectAsStateWithLifecycle()
    val showDebug by viewModel.showDebugPanel.collectAsStateWithLifecycle()
    val showReason by viewModel.showDetectionReason.collectAsStateWithLifecycle()
    val forceReclass by viewModel.forceReclassify.collectAsStateWithLifecycle()
    val logToFile by viewModel.logUsbToFile.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        SectionHeader(title = "Settings", count = BuildConfig.VERSION_NAME)

        GlassCard(hazeState = null, accentColor = DeepEyeColors.PrimaryCyan, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Deep Eye control plane",
                    style = MaterialTheme.typography.titleLarge,
                    color = DeepEyeColors.TextPrimary,
                )
                Text(
                    text = "Tune telemetry, protocol heuristics, and the visual theme used by the OTG workstation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DeepEyeColors.TextSecondary,
                )
            }
        }

        SectionHeader(title = "Theme", count = themeMode.name)
        ThemeMode.values().forEach { mode ->
            GlassCard(
                hazeState = null,
                modifier = Modifier.fillMaxWidth(),
                accentColor = if (themeMode == mode) DeepEyeColors.PrimaryCyan else Color.Transparent,
                onClick = {
                    scope.launch {
                        ThemePreferences.setThemeMode(context, mode)
                    }
                },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Brush, contentDescription = null, tint = if (themeMode == mode) DeepEyeColors.PrimaryCyan else DeepEyeColors.TextSecondary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(mode.name, style = MaterialTheme.typography.titleMedium, color = DeepEyeColors.TextPrimary)
                            Text(
                                text = when (mode) {
                                    ThemeMode.SYSTEM -> "Follow device dark/light mode"
                                    ThemeMode.DARK -> "Cyber glass interface"
                                    ThemeMode.LIGHT -> "Light lab surface"
                                    ThemeMode.MONET -> "Wallpaper-aware accent synthesis"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = DeepEyeColors.TextSecondary,
                            )
                        }
                    }
                    Text(
                        text = if (themeMode == mode) "ACTIVE" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepEyeColors.PrimaryCyan,
                    )
                }
            }
        }

        SectionHeader(title = "USB & Detection", count = "Live")
        SettingsToggleCard(
            icon = Icons.Default.Security,
            title = "Require explicit ADB signature",
            subtitle = "Refuse insecure ADB pairings when public-key state is missing.",
            checked = adbSig,
            onToggle = { viewModel.toggleAdbSignature() },
        )
        SettingsToggleCard(
            icon = Icons.Default.Memory,
            title = "Debounce attach events",
            subtitle = "Smooth noisy OTG cable reconnect storms.",
            checked = debounce,
            onToggle = { viewModel.toggleDebounceAttach() },
        )
        SettingsToggleCard(
            icon = Icons.Default.BugReport,
            title = "Show debug overlay",
            subtitle = "Expose descriptor tuples and session health diagnostics.",
            checked = showDebug,
            onToggle = { viewModel.toggleDebugPanel() },
        )
        SettingsToggleCard(
            icon = Icons.Default.BugReport,
            title = "Show detection reason",
            subtitle = "Display heuristic rationale alongside detected mode.",
            checked = showReason,
            onToggle = { viewModel.toggleShowDetectionReason() },
        )
        SettingsToggleCard(
            icon = Icons.Default.Memory,
            title = "Performance mode",
            subtitle = "Reduce expensive blur and animation load for thermal headroom.",
            checked = perfMode,
            onToggle = { viewModel.togglePerformance() },
        )
        SettingsToggleCard(
            icon = Icons.Default.Security,
            title = "Force reclassify on wake",
            subtitle = "Re-run device probing whenever the app returns to foreground.",
            checked = forceReclass,
            onToggle = { viewModel.toggleForceReclassify() },
        )
        SettingsToggleCard(
            icon = Icons.Default.Save,
            title = "Persist USB traces",
            subtitle = "Write raw packet telemetry to forensic cache for later review.",
            checked = logToFile,
            onToggle = { viewModel.toggleLogUsbToFile() },
        )

        SectionHeader(title = "Reports", count = "Actions")
        NeonButton(
            text = "Export Session Report",
            onClick = { viewModel.exportSessionReport() },
            style = NeonButtonStyle.SECONDARY,
        )
        NeonButton(
            text = "Generate Forensic PDF",
            onClick = { viewModel.generateForensicPdf() },
        )

        Spacer(modifier = Modifier.height(84.dp))
    }
}

@Composable
private fun SettingsToggleCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    GlassCard(
        hazeState = null,
        modifier = Modifier.fillMaxWidth(),
        accentColor = if (checked) DeepEyeColors.PrimaryCyan else Color.Transparent,
        onClick = onToggle,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = null, tint = if (checked) DeepEyeColors.PrimaryCyan else DeepEyeColors.TextSecondary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, color = DeepEyeColors.TextPrimary)
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = DeepEyeColors.TextSecondary)
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = DeepEyeColors.PrimaryCyan,
                    uncheckedThumbColor = DeepEyeColors.TextSecondary,
                    uncheckedTrackColor = DeepEyeColors.Surface2,
                ),
            )
        }
    }
}
