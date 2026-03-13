package com.deepeye.otg.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepeye.otg.protocol.apple.iphone15.Iphone15Session
import com.deepeye.otg.protocol.apple.iphone15.IphoneChip
import com.deepeye.otg.protocol.apple.iphone15.ResearchChainStatus
import com.deepeye.otg.ui.components.GlassCard
import com.deepeye.otg.ui.theme.StitchTokens
import com.deepeye.otg.viewmodel.UsbViewModel

@Composable
fun Iphone15ResearchScreen(viewModel: UsbViewModel) {
    val sessions by viewModel.sessions.collectAsState()
    val selectedKey by viewModel.selectedDeviceKey.collectAsState()

    val selected = selectedKey?.let { sessions[it] } ?: sessions.values.firstOrNull()
    val connected = selected as? com.deepeye.otg.usb.UsbLifecycleState.Connected

    val profile = connected?.let {
        Iphone15Session.buildProfile(
            snapshot = it.descriptorSnapshot,
            iosVersion = "17.1.2",
            buildNumber = "Unknown",
            productHint = it.deviceName
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(StitchTokens.BackgroundDark, StitchTokens.SurfaceDark)
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 110.dp,
                bottom = 32.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.setNav(NavTarget.HOME) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = StitchTokens.TextPrimary)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "iPhone 15 Research",
                        style = StitchTokens.TitleLarge,
                        color = StitchTokens.TextPrimary
                    )
                }
            }

            if (profile == null) {
                item {
                    GlassCard(hazeState = null, accentColor = StitchTokens.AccentApple) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = "No active Apple session",
                                style = StitchTokens.TitleLarge,
                                color = StitchTokens.TextPrimary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Connect an iPhone in DFU/Recovery/Normal mode to populate A16/A17 research intelligence.",
                                style = StitchTokens.BodyMedium,
                                color = StitchTokens.TextSecondary
                            )
                        }
                    }
                }
                return@LazyColumn
            }

            item {
                GlassCard(hazeState = null, accentColor = StitchTokens.AccentApple) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = profile.modelName,
                            style = StitchTokens.TitleLarge,
                            color = StitchTokens.TextPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "${chipLabel(profile.chip)} • ${profile.boardConfig} • iOS ${profile.iosVersion}",
                            style = StitchTokens.MonoCode,
                            color = StitchTokens.TextMono
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Security: PAC ✓  MTE ✓  PPL ✓",
                            style = StitchTokens.BodyMedium,
                            color = StitchTokens.TextSecondary
                        )
                    }
                }
            }

            item {
                GlassCard(hazeState = null, accentColor = StitchTokens.Primary) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cable, null, tint = StitchTokens.Primary)
                            Spacer(Modifier.width(8.dp))
                            Text("USB-C Surface", style = StitchTokens.TitleLarge, color = StitchTokens.TextPrimary)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "USB3: ${if (profile.usbSurface.usb3Capable) "Yes" else "No"}  |  Thunderbolt: ${if (profile.usbSurface.thunderboltCapable) "Possible" else "No"}",
                            style = StitchTokens.BodyMedium,
                            color = StitchTokens.TextSecondary
                        )
                        Text(
                            text = "PD: ${profile.usbSurface.usbPdVersion}  |  Alt Mode: ${profile.usbSurface.altMode}",
                            style = StitchTokens.BodyMedium,
                            color = StitchTokens.TextSecondary
                        )
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { profile.usbSurface.fuzzingCoverageHint / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = StitchTokens.Primary,
                            trackColor = Color.White.copy(alpha = 0.12f)
                        )
                    }
                }
            }

            item {
                GlassCard(hazeState = null, accentColor = StitchTokens.AccentWarning) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, null, tint = StitchTokens.AccentWarning)
                            Spacer(Modifier.width(8.dp))
                            Text("Exploit Chain Status", style = StitchTokens.TitleLarge, color = StitchTokens.TextPrimary)
                        }
                        Spacer(Modifier.height(8.dp))
                        profile.exploitChains.forEach { chain ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(chain.name, style = StitchTokens.BodyMedium, color = StitchTokens.TextSecondary)
                                Text(statusLabel(chain.status), style = StitchTokens.LabelSmall, color = statusColor(chain.status))
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }

            item {
                GlassCard(hazeState = null, accentColor = StitchTokens.AccentAdb) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Memory, null, tint = StitchTokens.AccentAdb)
                            Spacer(Modifier.width(8.dp))
                            Text("Safe Capabilities", style = StitchTokens.TitleLarge, color = StitchTokens.TextPrimary)
                        }
                        Spacer(Modifier.height(8.dp))
                        profile.safeCapabilities.forEach {
                            Text("• $it", style = StitchTokens.BodyMedium, color = StitchTokens.TextSecondary)
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }

            item {
                GlassCard(hazeState = null, accentColor = StitchTokens.AccentError) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, null, tint = StitchTokens.AccentError)
                            Spacer(Modifier.width(8.dp))
                            Text("Honest Limitations", style = StitchTokens.TitleLarge, color = StitchTokens.TextPrimary)
                        }
                        Spacer(Modifier.height(8.dp))
                        profile.knownLimitations.forEach {
                            Text("• $it", style = StitchTokens.BodyMedium, color = StitchTokens.TextSecondary)
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }

            item {
                GlassCard(hazeState = null, accentColor = StitchTokens.AccentFastboot) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Applicable Public CVEs (${profile.applicableCves.size})", style = StitchTokens.TitleLarge, color = StitchTokens.TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        profile.applicableCves.take(8).forEach { cve ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .height(8.dp)
                                        .width(8.dp)
                                        .background(Color(0xFF4ADE80), CircleShape)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("${cve.cve} • ${cve.component}", style = StitchTokens.BodyMedium, color = StitchTokens.TextSecondary)
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun chipLabel(chip: IphoneChip): String = when (chip) {
    IphoneChip.A16_BIONIC -> "A16 Bionic"
    IphoneChip.A17_PRO -> "A17 Pro"
    IphoneChip.UNKNOWN -> "Unknown chip"
}

private fun statusLabel(status: ResearchChainStatus): String = when (status) {
    ResearchChainStatus.APPLICABLE_PUBLIC_RESEARCH -> "Applicable"
    ResearchChainStatus.PATCHED_ON_DEVICE -> "Patched"
    ResearchChainStatus.LIMITED_PUBLIC_RESEARCH -> "Research"
    ResearchChainStatus.NO_PUBLIC_CHAIN -> "No public chain"
}

private fun statusColor(status: ResearchChainStatus): Color = when (status) {
    ResearchChainStatus.APPLICABLE_PUBLIC_RESEARCH -> Color(0xFF4ADE80)
    ResearchChainStatus.PATCHED_ON_DEVICE -> Color(0xFFF87171)
    ResearchChainStatus.LIMITED_PUBLIC_RESEARCH -> Color(0xFFFBBF24)
    ResearchChainStatus.NO_PUBLIC_CHAIN -> Color(0xFFA1A1AA)
}
