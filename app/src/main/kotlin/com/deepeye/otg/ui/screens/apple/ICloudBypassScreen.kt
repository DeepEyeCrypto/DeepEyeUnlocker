package com.deepeye.otg.ui.screens.apple

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.otg.ui.components.*
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.ui.theme.JetBrainsMonoFamily

@Composable
fun ICloudBypassScreen(
    viewModel: ICloudBypassViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Bypass", "DNS Setup", "Apple ID", "PLIST")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepEyeColors.Background)
    ) {
        // ── Tab Row ─────────────────────────────────
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor   = Color.Transparent,
            contentColor     = DeepEyeColors.GoldAccent,
            edgePadding      = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[selectedTab]),
                    color = DeepEyeColors.GoldAccent
                )
            }
        ) {
            tabs.forEachIndexed { i, title ->
                Tab(
                    selected  = selectedTab == i,
                    onClick   = { selectedTab = i },
                    text = {
                        Text(
                            title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selectedTab == i)
                                FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == i)
                                DeepEyeColors.GoldAccent
                            else DeepEyeColors.TextMuted
                        )
                    }
                )
            }
        }

        // ── Tab Content ─────────────────────────────
        Box(Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> BypassTab(state, viewModel)
                1 -> DnsSetupTab(state, viewModel)
                2 -> AppleIdTab(state, viewModel)
                3 -> PlistTab(state, viewModel)
            }
        }
    }
}

// ── TAB 1: Main Bypass ─────────────────────────────
@Composable
private fun BypassTab(
    state: ICloudBypassUiState,
    viewModel: ICloudBypassViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Device info card
        GlassCard(hazeState = null, modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
            Column(Modifier.padding(16.dp)) {
                Text("DEVICE INFO",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepEyeColors.TextMuted)
                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.model,
                        onValueChange = viewModel::onModelChanged,
                        label = { Text("Model") },
                        placeholder = { Text("iPhone14,2") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepEyeColors.GoldAccent,
                            unfocusedBorderColor = DeepEyeColors.BorderGlass
                        )
                    )
                    OutlinedTextField(
                        value = state.chip,
                        onValueChange = viewModel::onChipChanged,
                        label = { Text("Chip") },
                        placeholder = { Text("A15") },
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
                    value = state.iosVersion,
                    onValueChange = viewModel::onIosVersionChanged,
                    label = { Text("iOS Version") },
                    placeholder = { Text("17.2") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepEyeColors.GoldAccent,
                        unfocusedBorderColor = DeepEyeColors.BorderGlass
                    )
                )
                Spacer(Modifier.height(8.dp))

                // Find My toggle
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Find My iPhone ON",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeepEyeColors.TextPrimary)
                    Switch(
                        checked = state.findMyEnabled,
                        onCheckedChange = viewModel::onFindMyChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepEyeColors.Error,
                            checkedTrackColor = DeepEyeColors.Error.copy(0.3f)
                        )
                    )
                }
                Spacer(Modifier.height(12.dp))
                GoldCtaButton(
                    text = "ANALYZE DEVICE",
                    onClick = viewModel::analyzeDevice,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.chip.isNotBlank()
                        && state.iosVersion.isNotBlank()
                        && !state.isLoading
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Bypass Score Card
        AnimatedVisibility(state.bypassScore != null) {
            state.bypassScore?.let { score ->
                BypassScoreCard(score)
                Spacer(Modifier.height(14.dp))
            }
        }

        // Method Cards
        AnimatedVisibility(state.bypassMethods.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("AVAILABLE METHODS",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepEyeColors.TextMuted)
                state.bypassMethods.forEachIndexed { i, method ->
                    BypassMethodCard(
                        method  = method,
                        isFirst = i == 0,
                        onSelect = { viewModel.onMethodSelected(method) }
                    )
                }
            }
        }
    }
}

// ── Bypass Score Widget ───────────────────────────
@Composable
private fun BypassScoreCard(score: BypassScore) {
    val color = when {
        score.scoreValue <= 3 -> DeepEyeColors.Success
        score.scoreValue <= 6 -> DeepEyeColors.GoldAccent
        score.scoreValue <= 8 -> Color(0xFFFF8C00)
        else                   -> DeepEyeColors.Error
    }
    GlassCard(hazeState = null, modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Score circle
            Box(
                Modifier
                    .size(56.dp)
                    .background(color.copy(0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${score.scoreValue}/10",
                    style = MaterialTheme.typography.titleMedium,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Bypass Difficulty",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepEyeColors.TextMuted)
                Text(score.difficulty,
                    style = MaterialTheme.typography.titleMedium,
                    color = color,
                    fontWeight = FontWeight.Bold)
                Text("${score.chip}  -   iOS ${score.iosMajor}",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepEyeColors.TextMuted)
            }
        }
    }
}

// ── Method Card ───────────────────────────────────
@Composable
private fun BypassMethodCard(
    method: BypassMethodEntry,
    isFirst: Boolean,
    onSelect: () -> Unit
) {
    GlassCard(
        hazeState = null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        cornerRadius = 14.dp
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isFirst) {
                        Text("⭐ ",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        method.methodName.replace("_"," ").uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isFirst) DeepEyeColors.GoldAccent
                                else DeepEyeColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text("${method.successRate}% success  -   ${method.time}",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepEyeColors.TextMuted)
                if (method.limitation.isNotBlank()) {
                    Text("⚠ ${method.limitation}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF8C00))
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = DeepEyeColors.TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── TAB 2: DNS Setup ─────────────────────────────
@Composable
private fun DnsSetupTab(
    state: ICloudBypassUiState,
    viewModel: ICloudBypassViewModel
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        GlassCard(hazeState = null, modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
            Column(Modifier.padding(16.dp)) {
                Text("DNS BYPASS SETUP",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepEyeColors.TextMuted)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.wifiSsid,
                    onValueChange = viewModel::onSsidChanged,
                    label = { Text("WiFi Network Name (SSID)") },
                    placeholder = { Text("MyWiFi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepEyeColors.GoldAccent,
                        unfocusedBorderColor = DeepEyeColors.BorderGlass
                    )
                )
                Spacer(Modifier.height(10.dp))
                GoldCtaButton(
                    text = "GENERATE DNS CONFIG",
                    onClick = viewModel::generateDnsConfig,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.wifiSsid.isNotBlank()
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        AnimatedVisibility(state.dnsConfig != null) {
            state.dnsConfig?.let { dns ->
                GlassCard(hazeState = null, modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                    Column(Modifier.padding(16.dp)) {
                        Text("DNS SERVERS",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepEyeColors.TextMuted)
                        Spacer(Modifier.height(8.dp))
                        listOf(
                            "Primary DNS"   to dns.primary,
                            "Secondary DNS" to dns.secondary,
                            "DeepEye DNS"   to dns.deepeyeDns
                        ).forEach { (label, value) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement =
                                    Arrangement.SpaceBetween
                            ) {
                                Text(label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DeepEyeColors.TextMuted)
                                Text(value,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DeepEyeColors.GoldAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = JetBrainsMonoFamily)
                            }
                            HorizontalDivider(color = DeepEyeColors.BorderGlass)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("STEPS",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepEyeColors.TextMuted)
                        Spacer(Modifier.height(8.dp))
                        dns.instructions.forEach { step ->
                            Row(
                                Modifier.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("›",
                                    color = DeepEyeColors.GoldAccent,
                                    modifier = Modifier.width(20.dp))
                                Text(step,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DeepEyeColors.TextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── TAB 3: Apple ID ──────────────────────────────
@Composable
private fun AppleIdTab(
    state: ICloudBypassUiState,
    viewModel: ICloudBypassViewModel
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        GlassCard(hazeState = null, modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
            Column(Modifier.padding(16.dp)) {
                Text("APPLE ID REMOVAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepEyeColors.TextMuted)
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Have purchase receipt?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeepEyeColors.TextPrimary)
                    Switch(
                        checked = state.hasReceipt,
                        onCheckedChange = viewModel::onReceiptChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepEyeColors.GoldAccent,
                            checkedTrackColor = DeepEyeColors.GoldAccent
                                .copy(0.3f)
                        )
                    )
                }
                Spacer(Modifier.height(8.dp))
                GoldCtaButton(
                    text = "GET REMOVAL PLAN",
                    onClick = viewModel::getAppleIdPlan,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.chip.isNotBlank()
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        AnimatedVisibility(state.appleIdPlan.isNotBlank()) {
            GlassCard(hazeState = null, modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                Column(Modifier.padding(16.dp)) {
                    Text("REMOVAL PLAN",
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepEyeColors.TextMuted)
                    Spacer(Modifier.height(8.dp))
                    Text(state.appleIdPlan,
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepEyeColors.TextPrimary,
                        fontFamily = JetBrainsMonoFamily)
                }
            }
        }
    }
}

// ── TAB 4: PLIST ──────────────────────────────────
@Composable
private fun PlistTab(
    state: ICloudBypassUiState,
    viewModel: ICloudBypassViewModel
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // PLIST paste + parse
        GlassCard(hazeState = null, modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null,
                        tint = DeepEyeColors.GoldAccent,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ACTIVATION PLIST",
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepEyeColors.TextMuted)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.plistInput,
                    onValueChange = viewModel::onPlistChanged,
                    label = { Text("Paste device activation PLIST") },
                    placeholder = { Text("<?xml version...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepEyeColors.GoldAccent,
                        unfocusedBorderColor = DeepEyeColors.BorderGlass
                    )
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = viewModel::parsePlist,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, DeepEyeColors.GoldAccent)
                    ) {
                        Text("PARSE", color = DeepEyeColors.GoldAccent)
                    }
                    GoldCtaButton(
                        text = "GENERATE",
                        onClick = viewModel::generatePlist,
                        modifier = Modifier.weight(1f),
                        enabled = state.udid.isNotBlank()
                            && state.imei.isNotBlank()
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Generated / Parsed PLIST result
        AnimatedVisibility(state.plistOutput.isNotBlank()) {
            GlassCard(hazeState = null, modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("PLIST OUTPUT",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepEyeColors.TextMuted)
                        // Copy button
                        IconButton(onClick = viewModel::copyPlistToClipboard,
                            modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.ContentCopy, "Copy",
                                tint = DeepEyeColors.GoldAccent,
                                modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        state.plistOutput.take(800) +
                            if (state.plistOutput.length > 800)
                                "\n..." else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepEyeColors.TextPrimary,
                        fontFamily = JetBrainsMonoFamily
                    )
                }
            }
        }
    }
}
