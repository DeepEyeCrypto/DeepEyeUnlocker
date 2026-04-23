package com.deepeye.otg.ui.screens.apple

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.otg.ui.components.GlassCard
import com.deepeye.otg.ui.theme.DeepEyeColors

@Composable
fun FirmwareDownloadScreen(
    viewModel: FirmwareViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // ── Model input ────────────────────────────
        GlassCard(hazeState=null, modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
            Column(Modifier.padding(16.dp)) {
                Text("FIRMWARE CATALOG",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepEyeColors.TextMuted)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.model,
                    onValueChange = { viewModel.onModelChanged(it) },
                    label = { Text("iPhone Model") },
                    placeholder = { Text("iPhone14,2") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (state.isLoading)
                            CircularProgressIndicator(
                                Modifier.size(20.dp),
                                color = DeepEyeColors.GoldAccent,
                                strokeWidth = 2.dp
                            )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = DeepEyeColors.GoldAccent,
                        unfocusedBorderColor = DeepEyeColors.BorderGlass
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Firmware list ──────────────────────────
        if (state.firmwareList.isNotEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.firmwareList) { fw ->
                    FirmwareEntryCard(
                        firmware = fw,
                        onDownload = {
                            val targetDir = java.io.File(
                                android.os.Environment.getExternalStorageDirectory(),
                                "DeepEye/firmware"
                            )
                            targetDir.mkdirs()
                            val targetFile = java.io.File(targetDir, "${fw.buildId}_iOS${fw.iosVersion}.ipsw")
                            viewModel.onDownloadSelected(fw, targetFile)
                        }
                    )
                }
            }
        }

        // ── Download progress ──────────────────────
        AnimatedVisibility(state.isDownloading) {
            Spacer(Modifier.height(12.dp))
            GlassCard(hazeState=null, modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("DOWNLOADING",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepEyeColors.GoldAccent)
                        Text("${(state.downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepEyeColors.TextPrimary,
                            fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { state.downloadProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color    = DeepEyeColors.GoldAccent,
                        trackColor = DeepEyeColors.BorderGlass
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(state.downloadEta,
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepEyeColors.TextMuted)
                }
            }
        }
    }
}

@Composable
private fun FirmwareEntryCard(
    firmware: FirmwareEntry,
    onDownload: () -> Unit
) {
    GlassCard(
        hazeState=null,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Signed status dot
            Box(
                Modifier
                    .size(10.dp)
                    .background(
                        if (firmware.signed) DeepEyeColors.Success
                        else DeepEyeColors.Error,
                        androidx.compose.foundation.shape.CircleShape
                    )
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "iOS ${firmware.iosVersion}  (${firmware.buildId})",
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepEyeColors.TextPrimary
                )
                Text(
                    "${firmware.sizeGb} GB  -   " +
                    if (firmware.signed) "✅ Signed" else "❌ Not signed",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (firmware.signed) DeepEyeColors.Success
                            else DeepEyeColors.TextMuted
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDownload) {
                Icon(
                    Icons.Default.Download,
                    "Download",
                    tint = DeepEyeColors.GoldAccent
                )
            }
        }
    }
}

data class FirmwareEntry(
    val iosVersion: String,
    val buildId: String,
    val sizeGb: Double,
    val signed: Boolean,
    val modelName: String,
    val url: String
)
