package com.deepeye.otg.ui.screens.samsung

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.otg.ui.components.GlassCard
import com.deepeye.otg.ui.theme.DeepEyeColors

@Composable
fun SamsungToolsScreen(
    onBack: () -> Unit,
    viewModel: SamsungToolsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf("USB Detection", "PIT Table", "Odin Validator")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepEyeColors.Background),
    ) {
        GlassCard(
            hazeState = null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            cornerRadius = 18.dp,
            accentColor = if (state.usbDetection?.isDownload == true) DeepEyeColors.Success else Color.Transparent,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = DeepEyeColors.TextPrimary,
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Samsung Read-Only Tools",
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepEyeColors.GoldAccent,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = state.usbDetection?.action ?: "USB identification, PIT reference viewing, and Odin package validation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepEyeColors.TextMuted,
                    )
                }
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = DeepEyeColors.GoldAccent,
                        strokeWidth = 2.dp,
                    )
                }
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = DeepEyeColors.GoldAccent,
            edgePadding = 16.dp,
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selectedTab == index) DeepEyeColors.GoldAccent else DeepEyeColors.TextMuted,
                        )
                    },
                )
            }
        }

        when (selectedTab) {
            0 -> UsbDetectionTab(state = state, viewModel = viewModel, modifier = Modifier.weight(1f))
            1 -> PitTableTab(state = state, viewModel = viewModel, modifier = Modifier.weight(1f))
            else -> OdinValidatorTab(state = state, viewModel = viewModel, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun UsbDetectionTab(
    state: SamsungToolsUiState,
    viewModel: SamsungToolsViewModel,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            GlassCard(hazeState = null, modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "USB VID:PID Detection",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepEyeColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Enter hexadecimal USB identifiers to classify Samsung download-mode connectivity.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepEyeColors.TextMuted,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = state.vidInput,
                            onValueChange = viewModel::onVidChanged,
                            label = { Text("VID") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = state.pidInput,
                            onValueChange = viewModel::onPidChanged,
                            label = { Text("PID") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    PrimaryActionButton(
                        text = "Analyze USB Pair",
                        onClick = viewModel::detectUsb,
                        enabled = !state.isLoading,
                    )
                }
            }
        }

        state.errorMessage?.let { message ->
            item { ErrorCard(message = message) }
        }

        state.usbDetection?.let { detection ->
            item {
                GlassCard(
                    hazeState = null,
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    accentColor = if (detection.isDownload) DeepEyeColors.Success else Color.Transparent,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = if (detection.isSamsung) DeepEyeColors.Success else DeepEyeColors.TextMuted,
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = if (detection.isSamsung) "Samsung USB Match" else "Unknown USB Pair",
                                style = MaterialTheme.typography.titleMedium,
                                color = DeepEyeColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        KeyValueRow(label = "Vendor", value = detection.vendor)
                        KeyValueRow(label = "VID", value = detection.vid)
                        KeyValueRow(label = "PID", value = detection.pid)
                        KeyValueRow(label = "Mode", value = detection.mode)
                        KeyValueRow(
                            label = "Download Ready",
                            value = if (detection.isDownload) "YES" else "NO",
                            valueColor = if (detection.isDownload) DeepEyeColors.Success else DeepEyeColors.TextPrimary,
                        )
                        HorizontalDivider(color = DeepEyeColors.GlassBorder)
                        Text(
                            text = detection.action,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeepEyeColors.TextMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PitTableTab(
    state: SamsungToolsUiState,
    viewModel: SamsungToolsViewModel,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            GlassCard(hazeState = null, modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Reference PIT Table",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepEyeColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Load a non-destructive reference layout for Samsung Odin partition planning.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepEyeColors.TextMuted,
                    )
                    OutlinedTextField(
                        value = state.modelInput,
                        onValueChange = viewModel::onModelChanged,
                        label = { Text("Samsung Model") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StorageOptionButton(
                            text = "UFS",
                            selected = state.storage == "ufs",
                            onClick = { viewModel.onStorageChanged("ufs") },
                            modifier = Modifier.weight(1f),
                        )
                        StorageOptionButton(
                            text = "eMMC",
                            selected = state.storage == "emmc",
                            onClick = { viewModel.onStorageChanged("emmc") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    PrimaryActionButton(
                        text = "Load Reference PIT",
                        onClick = viewModel::loadPitTable,
                        enabled = !state.isLoading,
                    )
                }
            }
        }

        state.errorMessage?.let { message ->
            item { ErrorCard(message = message) }
        }

        if (state.pitEntries.isNotEmpty()) {
            item {
                Text(
                    text = "${state.pitEntries.size} partitions loaded",
                    style = MaterialTheme.typography.labelMedium,
                    color = DeepEyeColors.TextMuted,
                )
            }
        }

        items(state.pitEntries) { entry ->
            GlassCard(
                hazeState = null,
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp,
                accentColor = if (entry.name.equals("SUPER", ignoreCase = true)) DeepEyeColors.GoldAccent.copy(alpha = 0.25f) else Color.Transparent,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = DeepEyeColors.GoldAccent,
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = entry.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = DeepEyeColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    KeyValueRow(label = "Image", value = entry.filename)
                    KeyValueRow(label = "Type", value = entry.type.uppercase())
                    KeyValueRow(label = "Storage", value = entry.storage.uppercase())
                    KeyValueRow(label = "Partition ID", value = entry.id.toString())
                    KeyValueRow(
                        label = "Reference Size",
                        value = if (entry.sizeMb > 0f) "%.2f MB".format(entry.sizeMb) else "Dynamic / variable",
                    )
                }
            }
        }
    }
}

@Composable
private fun OdinValidatorTab(
    state: SamsungToolsUiState,
    viewModel: SamsungToolsViewModel,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            GlassCard(hazeState = null, modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Odin Package Validator",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepEyeColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Paste filenames from a .tar/.tar.md5 package to validate slot coverage without flashing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepEyeColors.TextMuted,
                    )
                    OutlinedTextField(
                        value = state.odinFilesInput,
                        onValueChange = viewModel::onOdinFilesChanged,
                        label = { Text("Package files") },
                        placeholder = { Text("BL_sboot.bin\nAP_super.img\nCP_modem.bin\nHOME_CSC_OMC.img") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 6,
                    )
                    PrimaryActionButton(
                        text = "Validate Odin Package",
                        onClick = viewModel::validateOdinPackage,
                        enabled = !state.isLoading,
                    )
                }
            }
        }

        state.errorMessage?.let { message ->
            item { ErrorCard(message = message) }
        }

        state.odinValidation?.let { validation ->
            item {
                GlassCard(
                    hazeState = null,
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    accentColor = if (validation.valid) DeepEyeColors.Success else DeepEyeColors.Error,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = if (validation.valid) "Package validation passed" else "Package validation incomplete",
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepEyeColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        KeyValueRow(
                            label = "Status",
                            value = if (validation.valid) "VALID" else "CHECK REQUIRED",
                            valueColor = if (validation.valid) DeepEyeColors.Success else DeepEyeColors.Error,
                        )
                        KeyValueRow(label = "Flash Type", value = validation.flashType)
                        KeyValueRow(label = "Files", value = validation.totalFiles.toString())
                        KeyValueRow(label = "PIT Included", value = if (validation.hasPit) "YES" else "NO")

                        if (validation.filledSlots.isNotEmpty()) {
                            HorizontalDivider(color = DeepEyeColors.GlassBorder)
                            Text(
                                text = "Detected slots",
                                style = MaterialTheme.typography.labelLarge,
                                color = DeepEyeColors.GoldAccent,
                            )
                            validation.filledSlots.forEach { (slot, files) ->
                                Text(
                                    text = "$slot: ${files.joinToString()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DeepEyeColors.TextPrimary,
                                )
                            }
                        }

                        if (validation.missingSlots.isNotEmpty()) {
                            HorizontalDivider(color = DeepEyeColors.GlassBorder)
                            Text(
                                text = "Missing slots: ${validation.missingSlots.joinToString()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = DeepEyeColors.Warning,
                            )
                        }

                        if (validation.unrecognized.isNotEmpty()) {
                            Text(
                                text = "Unrecognized files: ${validation.unrecognized.joinToString()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = DeepEyeColors.TextMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageOptionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) DeepEyeColors.GoldAccent.copy(alpha = 0.14f) else Color.Transparent,
            contentColor = if (selected) DeepEyeColors.GoldAccent else DeepEyeColors.TextPrimary,
        ),
    ) {
        Text(text = text)
    }
}

@Composable
private fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = DeepEyeColors.GoldAccent,
            contentColor = DeepEyeColors.Background,
        ),
    ) {
        Text(text = text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ErrorCard(message: String) {
    GlassCard(
        hazeState = null,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        accentColor = DeepEyeColors.Error,
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodySmall,
            color = DeepEyeColors.TextPrimary,
        )
    }
}

@Composable
private fun KeyValueRow(
    label: String,
    value: String,
    valueColor: Color = DeepEyeColors.TextPrimary,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = DeepEyeColors.TextMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
            fontWeight = FontWeight.Medium,
        )
    }
}
