package com.deepeye.otg.ui.screens.qualcomm

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.otg.ui.components.*
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.ui.theme.JetBrainsMonoFamily

@Composable
fun EdlScreen(
    onBack: () -> Unit,
    viewModel: EdlViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Detection", "Firehose", "FRP Erase")

    Column(
        Modifier
            .fillMaxSize()
            .background(DeepEyeColors.Background)
    ) {
        // ── Header ───────────────────────────────────
        GlassCard(
            hazeState = null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            cornerRadius = 14.dp
        ) {
            Row(
                Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // EDL status indicator
                Box(
                    Modifier
                        .size(12.dp)
                        .background(
                            if (state.isEdlDetected)
                                DeepEyeColors.Success
                            else DeepEyeColors.TextMuted,
                            CircleShape
                        )
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        if (state.isEdlDetected)
                            "EDL MODE DETECTED ✅"
                        else "Waiting for EDL Device…",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (state.isEdlDetected)
                            DeepEyeColors.Success
                        else DeepEyeColors.TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                    if (state.chipInfo.isNotBlank()) {
                        Text(state.chipInfo,
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepEyeColors.TextMuted)
                    }
                }
                Spacer(Modifier.weight(1f))
                if (state.isLoading)
                    CircularProgressIndicator(
                        Modifier.size(20.dp),
                        color = DeepEyeColors.GoldAccent,
                        strokeWidth = 2.dp
                    )
            }
        }

        // ── Header ─────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DeepEyeColors.TextPrimary)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "EDL EXPLOITATION HUB",
                style = MaterialTheme.typography.titleLarge,
                color = DeepEyeColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        // ── Tabs ─────────────────────────────────────
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor   = Color.Transparent,
            contentColor     = DeepEyeColors.GoldAccent,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(
                        tabPositions[selectedTab]
                    ),
                    color = DeepEyeColors.GoldAccent
                )
            }
        ) {
            tabs.forEachIndexed { i, title ->
                Tab(
                    selected = selectedTab == i,
                    onClick  = { selectedTab = i },
                    text = {
                        Text(title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selectedTab == i)
                                FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == i)
                                DeepEyeColors.GoldAccent
                            else DeepEyeColors.TextMuted)
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> DetectionTab(state, viewModel)
            1 -> FirehoseTab(state, viewModel)
            2 -> FrpEraseTab(state, viewModel)
        }
    }
}

// ── TAB 1: Detection ──────────────────────────────
@Composable
private fun DetectionTab(
    state: EdlUiState,
    viewModel: EdlViewModel
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // USB VID:PID input
        GlassCard(hazeState = null, modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
            Column(Modifier.padding(16.dp)) {
                Text("USB DETECTION",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepEyeColors.TextMuted)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.vidInput,
                        onValueChange = viewModel::onVidChanged,
                        label = { Text("VID (hex)") },
                        placeholder = { Text("05C6") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepEyeColors.GoldAccent,
                            unfocusedBorderColor = DeepEyeColors.BorderGlass
                        )
                    )
                    OutlinedTextField(
                        value = state.pidInput,
                        onValueChange = viewModel::onPidChanged,
                        label = { Text("PID (hex)") },
                        placeholder = { Text("9008") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepEyeColors.GoldAccent,
                            unfocusedBorderColor = DeepEyeColors.BorderGlass
                        )
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.chipInput,
                    onValueChange = viewModel::onChipChanged,
                    label = { Text("Chip Model") },
                    placeholder = { Text("SM8450") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepEyeColors.GoldAccent,
                        unfocusedBorderColor = DeepEyeColors.BorderGlass
                    )
                )
                Spacer(Modifier.height(10.dp))
                GoldCtaButton(
                    text = "DETECT EDL",
                    onClick = viewModel::detectEdl,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.vidInput.isNotBlank()
                        && state.pidInput.isNotBlank()
                        && !state.isLoading
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Detection Result
        AnimatedVisibility(state.detectionResult.isNotBlank()) {
            GlassCard(hazeState = null, modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                Column(Modifier.padding(16.dp)) {
                    Text("DETECTION RESULT",
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepEyeColors.TextMuted)
                    Spacer(Modifier.height(8.dp))
                    Text(state.detectionResult,
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepEyeColors.TextPrimary,
                        fontFamily = JetBrainsMonoFamily)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Programmer info
        AnimatedVisibility(state.programmerInfo.isNotBlank()) {
            GlassCard(hazeState = null, modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                Column(Modifier.padding(16.dp)) {
                    Text("FIREHOSE PROGRAMMER",
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepEyeColors.TextMuted)
                    Spacer(Modifier.height(8.dp))
                    Text(state.programmerInfo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeepEyeColors.GoldAccent,
                        fontWeight = FontWeight.Bold,
                        fontFamily = JetBrainsMonoFamily)
                }
            }
        }
    }
}

// ── TAB 2: Firehose ───────────────────────────────
@Composable
private fun FirehoseTab(
    state: EdlUiState,
    viewModel: EdlViewModel
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Storage selector
        GlassCard(hazeState = null, modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
            Column(Modifier.padding(16.dp)) {
                Text("FLASH CONFIGURATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepEyeColors.TextMuted)
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ufs","emmc","nvme").forEach { storage ->
                        FilterChip(
                            selected = state.storageType == storage,
                            onClick  = { viewModel.onStorageChanged(storage) },
                            label    = {
                                Text(storage.uppercase(),
                                    style = MaterialTheme.typography.labelSmall)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor =
                                    DeepEyeColors.GoldAccent.copy(0.2f),
                                selectedLabelColor =
                                    DeepEyeColors.GoldAccent
                            )
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                GoldCtaButton(
                    text = "BUILD FLASH SEQUENCE",
                    onClick = viewModel::buildFlashSequence,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.chipInput.isNotBlank()
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Flash steps list
        AnimatedVisibility(state.flashSteps.isNotEmpty()) {
            Column {
                Text("FLASH SEQUENCE (${state.flashSteps.size} steps)",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepEyeColors.TextMuted)
                Spacer(Modifier.height(8.dp))
                state.flashSteps.forEach { step ->
                    FlashStepRow(step = step)
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun FlashStepRow(step: FlashStep) {
    val phaseColor = when (step.phase) {
        "sahara"   -> Color(0xFF64B5F6)
        "firehose" -> DeepEyeColors.GoldAccent
        else       -> DeepEyeColors.TextMuted
    }
    GlassCard(hazeState = null, modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Step number
            Box(
                Modifier
                    .size(28.dp)
                    .background(phaseColor.copy(0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("${step.stepNum}",
                    style = MaterialTheme.typography.labelSmall,
                    color = phaseColor,
                    fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(step.action,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepEyeColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold)
                Text(step.note,
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepEyeColors.TextMuted)
                if (step.filename.isNotBlank()) {
                    Text(step.filename,
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepEyeColors.GoldAccent,
                        fontFamily = JetBrainsMonoFamily)
                }
            }
            Text(step.phase.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = phaseColor)
        }
    }
}

// ── TAB 3: FRP Erase ─────────────────────────────
@Composable
private fun FrpEraseTab(
    state: EdlUiState,
    viewModel: EdlViewModel
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Warning
        GlassCard(
            hazeState = null,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp
        ) {
            Row(
                Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.Warning, null,
                    tint = Color(0xFFFF8C00),
                    modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "EDL FRP erase permanently removes Factory Reset "
                    + "Protection. Device must be in EDL mode "
                    + "(VID:05C6 PID:9008). Requires Firehose programmer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFF8C00)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        GlassCard(hazeState = null, modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
            Column(Modifier.padding(16.dp)) {
                Text("FRP PARTITION ERASE",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepEyeColors.TextMuted)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ufs","emmc").forEach { s ->
                        FilterChip(
                            selected = state.frpStorage == s,
                            onClick  = { viewModel.onFrpStorageChanged(s) },
                            label    = {
                                Text(s.uppercase(),
                                    style = MaterialTheme.typography.labelSmall)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor =
                                    DeepEyeColors.GoldAccent.copy(0.2f),
                                selectedLabelColor =
                                    DeepEyeColors.GoldAccent
                            )
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                GoldCtaButton(
                    text = "GET FRP ERASE XML",
                    onClick = viewModel::getFrpInfo,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        AnimatedVisibility(state.frpXml.isNotBlank()) {
            GlassCard(hazeState = null, modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("FIREHOSE ERASE XML",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepEyeColors.TextMuted)
                        IconButton(
                            onClick = viewModel::copyFrpXml,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, "Copy",
                                tint = DeepEyeColors.GoldAccent,
                                modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(state.frpXml,
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepEyeColors.TextPrimary,
                        fontFamily = JetBrainsMonoFamily)
                }
            }
        }
    }
}

// ── Data Classes ──────────────────────────────────
data class FlashStep(
    val phase:    String,
    val stepNum:  Int,
    val action:   String,
    val note:     String,
    val filename: String = "",
    val xml:      String = ""
)
