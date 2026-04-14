package com.deepeye.otg.ui.screens.apple

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.deepeye.otg.ui.theme.JetBrainsMonoFamily

@Composable
fun MdmCtaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DeepEyeColors.GoldAccent,
            contentColor = Color.Black,
            disabledContainerColor = DeepEyeColors.Surface2,
            disabledContentColor = DeepEyeColors.TextMuted
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun MdmRemovalScreen(
    viewModel: MdmViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ── PLIST Paste Card (reference: PLIST icon) ──
        GlassCard(hazeState=null, modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, null,
                        tint = DeepEyeColors.GoldAccent,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("MDM PROFILE PARSER",
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepEyeColors.TextMuted)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Paste MDM config profile PLIST or enter device info manually",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepEyeColors.TextMuted
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.plistInput,
                    onValueChange = { viewModel.onPlistChanged(it) },
                    label = { Text("MDM Profile PLIST") },
                    placeholder = { Text("<?xml version=\"1.0\"...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = DeepEyeColors.GoldAccent,
                        unfocusedBorderColor = DeepEyeColors.BorderGlass
                    )
                )
                Spacer(Modifier.height(8.dp))
                MdmCtaButton(
                    text = "PARSE MDM PROFILE",
                    onClick = { viewModel.parseMdmProfile() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.plistInput.isNotBlank()
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── Manual device entry ────────────────────
        GlassCard(hazeState=null, modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
            Column(Modifier.padding(16.dp)) {
                Text("MANUAL DEVICE INFO",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepEyeColors.TextMuted)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.model,
                        onValueChange = { viewModel.onModelChanged(it) },
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
                        onValueChange = { viewModel.onChipChanged(it) },
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Supervised Device",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeepEyeColors.TextPrimary)
                    Switch(
                        checked = state.isSupervised,
                        onCheckedChange = { viewModel.onSupervisedChanged(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor  = DeepEyeColors.GoldAccent,
                            checkedTrackColor  = DeepEyeColors.GoldAccent
                                .copy(alpha = 0.3f)
                        )
                    )
                }
                Spacer(Modifier.height(10.dp))
                MdmCtaButton(
                    text = "GENERATE BYPASS PLAN",
                    onClick = { viewModel.generateBypassPlan() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.model.isNotBlank()
                        && state.chip.isNotBlank()
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── MDM Parse Result ───────────────────────
        AnimatedVisibility(state.parsedMdm != null) {
            state.parsedMdm?.let { mdm ->
                GlassCard(hazeState=null, modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                    Column(Modifier.padding(16.dp)) {
                        Text("PARSED MDM INFO",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepEyeColors.TextMuted)
                        Spacer(Modifier.height(10.dp))
                        listOf(
                            "Org Name"    to mdm.orgName,
                            "MDM Type"    to mdm.mdmType.uppercase(),
                            "Server URL"  to mdm.serverUrl.take(40),
                            "Supervised"  to if (mdm.isSupervised)
                                               "⚠️ YES" else "No",
                            "Removable"   to if (mdm.removable)
                                               "✅ YES" else "❌ NO",
                        ).forEach { (k, v) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement =
                                    Arrangement.SpaceBetween
                            ) {
                                Text(k,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DeepEyeColors.TextMuted)
                                Text(v,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DeepEyeColors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── Bypass Report ──────────────────────────
        AnimatedVisibility(state.bypassReport.isNotBlank()) {
            GlassCard(hazeState=null, modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                Column(Modifier.padding(16.dp)) {
                    Text("BYPASS PLAN",
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepEyeColors.TextMuted)
                    Spacer(Modifier.height(10.dp))
                    Text(state.bypassReport,
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepEyeColors.TextPrimary,
                        fontFamily = JetBrainsMonoFamily)
                }
            }
        }
    }
}

data class ParsedMdmInfo(
    val orgName: String,
    val mdmType: String,
    val serverUrl: String,
    val isSupervised: Boolean,
    val removable: Boolean
)
