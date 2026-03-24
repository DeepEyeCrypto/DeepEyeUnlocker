package com.deepeye.otg.ui.gsmg

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.deepeye.otg.data.gsmg.DevicePlatform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deepeye.otg.data.gsmg.BypassEvent
import com.deepeye.otg.data.gsmg.BypassFeature
import com.deepeye.otg.data.gsmg.DeviceState
import com.deepeye.otg.data.gsmg.ExecutionPlan
import com.deepeye.otg.data.gsmg.UnifiedBypassRegistry

private val screenBg = Color(0xFF050505)
private val panelBg = Color(0xFF121212)
private val outline = Color(0xFF2A2A2A)
private val accent = Color(0xFF2196F3)
private val success = Color(0xFF00E676)
private val warning = Color(0xFFFF9800)
private val danger = Color(0xFFFF1744)
private val textPrimary = Color(0xFFF0F0F0)
private val textSecondary = Color(0xFFB0B0B0)
private val textMuted = Color(0xFF707070)

@Composable
fun BypassScreen(viewModel: BypassViewModel = hiltViewModel()) {
    // High-assurance: lifecycle-aware state collection
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    
    val activePlan = uiState.activePlan
    val errorMessage = uiState.errorMessage
    val successMessage = uiState.successMessage

    // Optimization: derived state for heavy list counts
    val featureCountText by remember {
        derivedStateOf { "${uiState.displayedFeatures.size} / ${uiState.totalAvailable} features" }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "summary") {
                SummaryCard(
                    uiState = uiState,
                    onSelectPlatform = viewModel::onSelectPlatform,
                )
            }

            val connectedDevice = uiState.device
            if (connectedDevice != null) {
                item(key = "device_${connectedDevice.sessionId}") {
                    DeviceCard(device = connectedDevice)
                }
            }

            val recommendation = uiState.recommendation
            val bestRecommendation = recommendation?.best
            if (recommendation != null && bestRecommendation != null) {
                item(key = "recommendation_${bestRecommendation.id}") {
                    RecommendationCard(
                        feature = bestRecommendation,
                        reasoning = recommendation.reasoning,
                        wantSignal = uiState.filters.signalOnly,
                        wantFree = uiState.filters.freeOnly,
                        wantUntethered = uiState.filters.isUntethered,
                        onExecute = { viewModel.onRequestExecute(bestRecommendation) },
                        onToggleSignal = {
                            viewModel.onRefineRecommendation(
                                !uiState.filters.signalOnly,
                                uiState.filters.freeOnly,
                                uiState.filters.isUntethered,
                            )
                        },
                        onToggleFree = {
                            viewModel.onRefineRecommendation(
                                uiState.filters.signalOnly,
                                !uiState.filters.freeOnly,
                                uiState.filters.isUntethered,
                            )
                        },
                        onToggleUntethered = {
                            viewModel.onRefineRecommendation(
                                uiState.filters.signalOnly,
                                uiState.filters.freeOnly,
                                !uiState.filters.isUntethered,
                            )
                        },
                    )
                }
            }

            item(key = "filter") {
                FilterCard(
                    uiState = uiState,
                    onSearch = viewModel::onSearch,
                    onBrandFilter = viewModel::onBrandFilter,
                    onToggleFreeOnly = viewModel::onToggleFreeOnly,
                    onToggleSignalOnly = viewModel::onToggleSignalOnly,
                    onToggleUntethered = viewModel::onToggleUntethered,
                    onToggleNoDataLoss = viewModel::onToggleNoDataLoss,
                    onToggleNoJailbreak = viewModel::onToggleNoJailbreak,
                    onToggleOfflineOnly = viewModel::onToggleOfflineOnly,
                )
            }

            item(key = "count_header") {
                Text(
                    text = featureCountText,
                    color = textMuted,
                    fontSize = 11.sp,
                )
            }

            items(
                items = uiState.displayedFeatures,
                key = { it.id }
            ) { feature ->
                FeatureCard(
                    feature = feature,
                    isActive = uiState.activeFeatureId == feature.id,
                    onExecute = { viewModel.onRequestExecute(feature) },
                )
            }

            item {
                Spacer(modifier = Modifier.height(120.dp))
            }
        }

        if (uiState.isExecuting) {
            ExecutionCard(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                event = uiState.latestEvent,
                onCancel = viewModel::cancelExecution,
            )
        }

        if (uiState.showPlanDialog && activePlan != null) {
            PlanDialog(
                plan = activePlan,
                onConfirm = viewModel::onConfirmPlan,
                onDismiss = viewModel::onDismissPlan,
            )
        }

        if (errorMessage != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = panelBg,
                contentColor = textPrimary,
                action = {
                    TextButton(onClick = viewModel::onClearError) {
                        Text("DISMISS", color = danger, fontSize = 11.sp)
                    }
                },
            ) {
                Text(errorMessage, fontSize = 11.sp)
            }
        }

        if (successMessage != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = panelBg,
                contentColor = success,
                action = {
                    TextButton(onClick = viewModel::onClearSuccess) {
                        Text("OK", color = success, fontSize = 11.sp)
                    }
                },
            ) {
                Text(successMessage, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun SummaryCard(
    uiState: BypassUiState,
    onSelectPlatform: (DevicePlatform) -> Unit,
) {
    val platforms = listOf(
        DevicePlatform.UNKNOWN,
        DevicePlatform.IOS,
        DevicePlatform.ANDROID,
        DevicePlatform.MODEM_ROUTER,
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = panelBg),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DEEPEYE BYPASS",
                        color = textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Feature catalogue across supported tools",
                        color = textMuted,
                        fontSize = 10.sp,
                    )
                }
                Text(
                    text = uiState.totalAvailable.toString(),
                    color = accent,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Badge(text = "${uiState.freeCount} FREE", color = success)
                Badge(text = "${uiState.signalCount} SIGNAL", color = accent)
                Badge(text = "${UnifiedBypassRegistry.isUntetheredCount} UNTETHERED", color = warning)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (platform in platforms) {
                    PlatformBadge(
                        platform = platform,
                        selected = uiState.selectedPlatform == platform,
                        onClick = { onSelectPlatform(platform) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(device: DeviceState) {
    val details = ArrayList<String>()
    details.add(device.chipName)
    if (!device.androidModel.isNullOrBlank()) {
        details.add(device.androidModel)
    }
    if (device.iosVersion != "0") {
        details.add("iOS ${device.iosVersion}")
    }
    details.add(device.chipRange.displayName)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = panelBg),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = device.androidBrand ?: device.chipName,
                color = accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = details.joinToString(" · "),
                color = textSecondary,
                fontSize = 10.sp,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (device.canUseSignal) Badge(text = "IMEI OK", color = success)
                if (device.fmiEnabled) Badge(text = "FMI ON", color = warning)
                if (device.dfuMode) Badge(text = "DFU", color = accent)
                if (device.adbAvailable) Badge(text = "ADB", color = success)
                if (device.edlAvailable) Badge(text = "EDL", color = danger)
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    feature: BypassFeature,
    reasoning: String,
    wantSignal: Boolean,
    wantFree: Boolean,
    wantUntethered: Boolean,
    onExecute: () -> Unit,
    onToggleSignal: () -> Unit,
    onToggleFree: () -> Unit,
    onToggleUntethered: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = panelBg),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("RECOMMENDED", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(feature.displayName, color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(feature.description, color = textSecondary, fontSize = 12.sp)
            Text(reasoning, color = textMuted, fontSize = 10.sp)

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Badge(text = feature.connectionMode, color = connectionColor(feature.connectionMode))
                Badge(text = feature.riskLevel.name, color = riskColor(feature.riskLevel))
                if (feature.signalAfter) Badge(text = "SIGNAL", color = success)
                if (feature.isUntethered) Badge(text = "UNTETHERED", color = warning)
                if (feature.isFree) Badge(text = "FREE", color = success)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ToggleBadge(text = "Signal", active = wantSignal, onClick = onToggleSignal)
                ToggleBadge(text = "Free", active = wantFree, onClick = onToggleFree)
                ToggleBadge(text = "Untethered", active = wantUntethered, onClick = onToggleUntethered)
            }

            Button(
                onClick = onExecute,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
            ) {
                val label = if (feature.isFree) "EXECUTE FREE" else "EXECUTE ${feature.costCredits}¢"
                Text(label, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FilterCard(
    uiState: BypassUiState,
    onSearch: (String) -> Unit,
    onBrandFilter: (String) -> Unit,
    onToggleFreeOnly: () -> Unit,
    onToggleSignalOnly: () -> Unit,
    onToggleUntethered: () -> Unit,
    onToggleNoDataLoss: () -> Unit,
    onToggleNoJailbreak: () -> Unit,
    onToggleOfflineOnly: () -> Unit,
) {
    val showBrandFilter =
        uiState.selectedPlatform == DevicePlatform.ANDROID ||
            uiState.selectedPlatform == DevicePlatform.MODEM_ROUTER

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = panelBg),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.filters.searchQuery,
                    onValueChange = onSearch,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Search features", fontSize = 11.sp, color = textMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = outline,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                    ),
                )

                if (showBrandFilter) {
                    OutlinedTextField(
                        value = uiState.filters.brandFilter,
                        onValueChange = onBrandFilter,
                        modifier = Modifier.width(120.dp),
                        singleLine = true,
                        placeholder = { Text("Brand", fontSize = 11.sp, color = textMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = warning,
                            unfocusedBorderColor = outline,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                        ),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ToggleBadge(text = "Free", active = uiState.filters.freeOnly, onClick = onToggleFreeOnly)
                ToggleBadge(text = "Signal", active = uiState.filters.signalOnly, onClick = onToggleSignalOnly)
                ToggleBadge(text = "Untethered", active = uiState.filters.isUntethered, onClick = onToggleUntethered)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ToggleBadge(text = "No Data Loss", active = uiState.filters.noDataLoss, onClick = onToggleNoDataLoss)
                ToggleBadge(text = "No Jailbreak", active = uiState.filters.noJailbreak, onClick = onToggleNoJailbreak)
                ToggleBadge(text = "Offline", active = uiState.filters.offlineOnly, onClick = onToggleOfflineOnly)
            }
        }
    }
}

@Composable
private fun FeatureCard(
    feature: BypassFeature,
    isActive: Boolean,
    onExecute: () -> Unit,
) {
    val currentBorder = if (isActive) success else outline
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, currentBorder, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = panelBg),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = feature.displayName,
                    color = textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                val price = if (feature.isFree) "FREE" else "${feature.costCredits}¢"
                Text(price, color = warning, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Text(feature.description, color = textSecondary, fontSize = 10.sp)

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Badge(text = feature.connectionMode, color = connectionColor(feature.connectionMode))
                Badge(text = feature.chipRange.displayName, color = textMuted)
                if (feature.signalAfter) Badge(text = "SIGNAL", color = success)
                if (feature.isUntethered) Badge(text = "UNTETHERED", color = warning)
                if (feature.dataLoss) Badge(text = "DATA LOSS", color = danger)
            }

            Button(
                onClick = onExecute,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
            ) {
                Text("RUN", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ExecutionCard(
    modifier: Modifier,
    event: BypassEvent?,
    onCancel: () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = panelBg),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "EXECUTING",
                    color = success,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onCancel) {
                    Text("CANCEL", color = danger, fontSize = 10.sp)
                }
            }

            when (event) {
                is BypassEvent.StepBegin -> {
                    Text("Step ${event.step.stepNum}: ${event.step.title}", color = textPrimary, fontSize = 12.sp)
                    Text(event.step.instruction, color = textSecondary, fontSize = 11.sp)
                }

                is BypassEvent.ProgressUpdate -> {
                    Text(event.currentPhase, color = textPrimary, fontSize = 12.sp)
                    LinearProgressIndicator(progress = { event.pct / 100f }, modifier = Modifier.fillMaxWidth())
                    Text("${event.pct}%", color = accent, fontSize = 10.sp)
                }

                is BypassEvent.NeedUserAction -> {
                    Text("USER ACTION REQUIRED", color = warning, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(event.instruction, color = textPrimary, fontSize = 12.sp)
                }

                is BypassEvent.Completed -> {
                    Text("COMPLETED", color = success, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                is BypassEvent.Failed -> {
                    Text("FAILED", color = danger, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(event.reason, color = textPrimary, fontSize = 11.sp)
                }

                is BypassEvent.RetryingNow -> {
                    Text(
                        text = "Retrying ${event.attempt}/${event.maxAttempts} in ${event.backoffMs}ms",
                        color = warning,
                        fontSize = 11.sp,
                    )
                }

                else -> {
                    LinearProgressIndicator(progress = { 0.1f }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun PlanDialog(
    plan: ExecutionPlan,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = panelBg,
        title = {
            Text("Confirm Operation", color = textPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(plan.feature.displayName, color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(plan.feature.description, color = textSecondary, fontSize = 11.sp)

                for (warningMessage in plan.warnings) {
                    Text("• $warningMessage", color = warning, fontSize = 11.sp)
                }

                for (prerequisite in plan.prerequisites) {
                    val label = if (prerequisite.met) "✅ ${prerequisite.name}" else "❌ ${prerequisite.name}"
                    val labelColor = if (prerequisite.met) success else danger
                    Text(label, color = labelColor, fontSize = 11.sp)
                    if (!prerequisite.met) {
                        Text(prerequisite.fixHint, color = textSecondary, fontSize = 10.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = plan.canExecute,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
            ) {
                Text("EXECUTE", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = textSecondary)
            }
        },
    )
}

@Composable
private fun Badge(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 9.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun ToggleBadge(text: String, active: Boolean, onClick: () -> Unit) {
    val color = if (active) accent else textMuted
    Box(
        modifier = Modifier
            .background(if (active) accent.copy(alpha = 0.12f) else Color.Transparent, RoundedCornerShape(8.dp))
            .border(1.dp, if (active) accent else outline, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text, color = color, fontSize = 10.sp)
    }
}

@Composable
private fun PlatformBadge(
    platform: DevicePlatform,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val label = when (platform) {
        DevicePlatform.UNKNOWN -> "All"
        DevicePlatform.IOS -> "iOS"
        DevicePlatform.ANDROID -> "Android"
        DevicePlatform.MODEM_ROUTER -> "Modem"
        else -> "All"
    }
    val color = if (selected) accent else textMuted

    Box(
        modifier = Modifier
            .background(if (selected) accent.copy(alpha = 0.12f) else Color.Transparent, RoundedCornerShape(8.dp))
            .border(1.dp, if (selected) accent else outline, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(label, color = color, fontSize = 11.sp)
    }
}

private fun riskColor(riskLevel: com.deepeye.otg.data.gsmg.RiskLevel): Color = when (riskLevel) {
    com.deepeye.otg.data.gsmg.RiskLevel.LOW -> success
    com.deepeye.otg.data.gsmg.RiskLevel.MEDIUM -> warning
    com.deepeye.otg.data.gsmg.RiskLevel.HIGH -> danger
    com.deepeye.otg.data.gsmg.RiskLevel.EXTREME -> danger
}

private fun connectionColor(mode: String): Color = when (mode) {
    "DFU" -> accent
    "ADB" -> success
    "EDL" -> danger
    "BROM" -> warning
    "META" -> Color(0xFF9C27B0)
    "DIAG" -> Color(0xFF26C6DA)
    else -> textMuted
}