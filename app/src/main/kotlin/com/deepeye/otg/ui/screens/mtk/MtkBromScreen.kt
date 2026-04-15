package com.deepeye.otg.ui.screens.mtk

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.otg.ui.components.GlassCard
import com.deepeye.otg.ui.components.GoldCtaButton
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.ui.theme.JetBrainsMonoFamily

@Composable
fun MtkBromScreen(
    viewModel: MtkBromViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("USB + Chip", "Scatter File", "SP Flash XML")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepEyeColors.Background)
    ) {
        GlassCard(
            hazeState = null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            cornerRadius = 14.dp
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            if (state.isBromDetected) DeepEyeColors.Success else DeepEyeColors.TextMuted,
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (state.isBromDetected) {
                            "MTK BROM DETECTED ✅"
                        } else {
                            "MTK BROM Tool (Read-Only)"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (state.isBromDetected) DeepEyeColors.Success else DeepEyeColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    if (state.chipLabel.isNotBlank()) {
                        Text(
                            text = state.chipLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepEyeColors.TextMuted
                        )
                    }
                }
                if (state.isLoading) {
                    Spacer(modifier = Modifier.weight(1f))
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = DeepEyeColors.GoldAccent,
                        strokeWidth = 2.dp
                    )
                }
            }
        }

        AnimatedVisibility(visible = !state.error.isNullOrBlank()) {
            GlassCard(
                hazeState = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                cornerRadius = 12.dp,
                accentColor = DeepEyeColors.Error
            ) {
                Text(
                    text = state.error.orEmpty(),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepEyeColors.TextPrimary
                )
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = DeepEyeColors.GoldAccent,
            edgePadding = 16.dp,
            indicator = { positions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(positions[selectedTab]),
                    color = DeepEyeColors.GoldAccent
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) DeepEyeColors.GoldAccent else DeepEyeColors.TextMuted
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> UsbChipTab(state = state, viewModel = viewModel)
            1 -> ScatterTab(state = state, viewModel = viewModel)
            else -> SpFlashTab(state = state, viewModel = viewModel)
        }
    }
}

@Composable
private fun UsbChipTab(
    state: MtkBromUiState,
    viewModel: MtkBromViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        GlassCard(
            hazeState = null,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "USB VID:PID INPUT",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepEyeColors.TextMuted
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.vidInput,
                    onValueChange = viewModel::onVidChanged,
                    label = { Text("VID (hex, e.g. 0E8D)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepEyeColors.GoldAccent,
                        unfocusedBorderColor = DeepEyeColors.BorderGlass
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.pidInput,
                    onValueChange = viewModel::onPidChanged,
                    label = { Text("PID (hex, e.g. 0003)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepEyeColors.GoldAccent,
                        unfocusedBorderColor = DeepEyeColors.BorderGlass
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.chipIdInput,
                    onValueChange = viewModel::onChipIdChanged,
                    label = { Text("Chip ID (hex, e.g. 0x6785)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepEyeColors.GoldAccent,
                        unfocusedBorderColor = DeepEyeColors.BorderGlass
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                GoldCtaButton(
                    text = "IDENTIFY DEVICE",
                    onClick = viewModel::identifyDevice,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.vidInput.isNotBlank() && state.pidInput.isNotBlank() && !state.isLoading
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        AnimatedVisibility(visible = state.deviceReport.isNotBlank()) {
            GlassCard(
                hazeState = null,
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "IDENTIFICATION RESULT",
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepEyeColors.TextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.deviceReport,
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepEyeColors.TextPrimary,
                        fontFamily = JetBrainsMonoFamily
                    )
                }
            }
        }
    }
}

@Composable
private fun ScatterTab(
    state: MtkBromUiState,
    viewModel: MtkBromViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        GlassCard(
            hazeState = null,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SCATTER FILE PARSER",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepEyeColors.TextMuted
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.scatterInput,
                    onValueChange = viewModel::onScatterChanged,
                    label = { Text("Paste scatter.txt content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepEyeColors.GoldAccent,
                        unfocusedBorderColor = DeepEyeColors.BorderGlass
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                GoldCtaButton(
                    text = "PARSE SCATTER",
                    onClick = viewModel::parseScatter,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.scatterInput.isNotBlank() && !state.isLoading
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        AnimatedVisibility(visible = state.scatterPartitions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "${state.scatterPartitions.size} PARTITIONS (READ-ONLY)",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepEyeColors.TextMuted
                )
                state.scatterPartitions.forEach { part ->
                    GlassCard(
                        hazeState = null,
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 10.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = part.name.padEnd(16),
                                style = MaterialTheme.typography.labelSmall,
                                color = DeepEyeColors.GoldAccent,
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(120.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = part.startAddr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepEyeColors.TextMuted,
                                    fontFamily = JetBrainsMonoFamily
                                )
                                if (part.sizeMb > 0f) {
                                    Text(
                                        text = "${part.sizeMb} MB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DeepEyeColors.TextFaint
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpFlashTab(
    state: MtkBromUiState,
    viewModel: MtkBromViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        GlassCard(
            hazeState = null,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SP FLASH TOOL XML VALIDATOR",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepEyeColors.TextMuted
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.spFlashXmlInput,
                    onValueChange = viewModel::onSpFlashXmlChanged,
                    label = { Text("Paste download.xml content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepEyeColors.GoldAccent,
                        unfocusedBorderColor = DeepEyeColors.BorderGlass
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                GoldCtaButton(
                    text = "VALIDATE XML",
                    onClick = viewModel::validateSpFlashXml,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.spFlashXmlInput.isNotBlank() && !state.isLoading
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        AnimatedVisibility(visible = state.spFlashResult.isNotBlank()) {
            GlassCard(
                hazeState = null,
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (state.spFlashValid) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (state.spFlashValid) DeepEyeColors.Success else DeepEyeColors.Error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.spFlashValid) "VALID SP Flash XML" else "INVALID XML",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.spFlashValid) DeepEyeColors.Success else DeepEyeColors.Error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.spFlashResult,
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepEyeColors.TextPrimary,
                        fontFamily = JetBrainsMonoFamily
                    )
                }
            }
        }
    }
}

@Immutable
data class ScatterPartition(
    val name: String,
    val startAddr: String,
    val sizeMb: Float,
    val type: String,
    val file: String
)
