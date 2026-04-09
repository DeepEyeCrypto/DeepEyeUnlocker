package com.deepeye.otg.ui.gsmg

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deepeye.otg.data.gsmg.BypassEvent
import com.deepeye.otg.data.gsmg.BypassFeature
import com.deepeye.otg.data.gsmg.DeviceState
import com.deepeye.otg.data.gsmg.ExecutionPlan
import com.deepeye.otg.data.gsmg.UnifiedBypassRegistry
import com.deepeye.otg.ui.components.LiquidGlassButton
import com.deepeye.otg.ui.components.SignalBadge
import com.deepeye.otg.ui.components.shimmerBorder

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
    val gridColumns = if (LocalConfiguration.current.screenWidthDp >= 520) 4 else 3
    
    val activePlan = uiState.activePlan
    val errorMessage = uiState.errorMessage
    val successMessage = uiState.successMessage
    val displayedCount by remember(uiState.displayedFeatures.size) {
        derivedStateOf { uiState.displayedFeatures.size }
    }
    val hasActiveExecution by remember(uiState.isExecuting, uiState.activeFeatureId) {
        derivedStateOf { uiState.isExecuting && uiState.activeFeatureId != null }
    }

    // Optimization: derived state for heavy list counts
    val featureCountText by remember(displayedCount, uiState.totalAvailable) {
        derivedStateOf { "$displayedCount / ${uiState.totalAvailable} features" }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(
                key = "summary",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                SummaryCard(
                    uiState = uiState,
                    onSelectPlatform = viewModel::onSelectPlatform,
                )
            }

            val connectedDevice = uiState.device
            if (connectedDevice != null) {
                item(
                    key = "device_${connectedDevice.sessionId}",
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    DeviceCard(device = connectedDevice)
                }
            }

            val recommendation = uiState.recommendation
            val bestRecommendation = recommendation?.best
            if (recommendation != null && bestRecommendation != null) {
                item(
                    key = "recommendation_${bestRecommendation.id}",
                    span = { GridItemSpan(maxLineSpan) },
                ) {
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

            item(
                key = "filter",
                span = { GridItemSpan(maxLineSpan) },
            ) {
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

            item(
                key = "count_header",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                Text(
                    text = featureCountText,
                    color = textMuted,
                    fontSize = 11.sp,
                )
            }

            item(
                key = "compact_stats",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                CompactOverviewStrip(
                    freeCount = uiState.freeCount,
                    signalCount = uiState.signalCount,
                    untetheredCount = UnifiedBypassRegistry.isUntetheredCount,
                    displayedCount = displayedCount,
                )
            }

            items(
                items = uiState.displayedFeatures,
                key = { feature -> feature.id },
            ) { feature ->
                FeatureCard(
                    feature = feature,
                    isActive = uiState.activeFeatureId == feature.id,
                    onExecute = { viewModel.onRequestExecute(feature) },
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(120.dp))
            }
        }

        if (hasActiveExecution) {
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
private fun CompactOverviewStrip(
    freeCount: Int,
    signalCount: Int,
    untetheredCount: Int,
    displayedCount: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CompactOverviewChip(
            modifier = Modifier.weight(1f),
            label = "FREE",
            value = freeCount,
            color = success,
        )
        CompactOverviewChip(
            modifier = Modifier.weight(1f),
            label = "SIGNAL",
            value = signalCount,
            color = accent,
        )
        CompactOverviewChip(
            modifier = Modifier.weight(1f),
            label = "UNTETH",
            value = untetheredCount,
            color = warning,
        )
        CompactOverviewChip(
            modifier = Modifier.weight(1f),
            label = "LIVE",
            value = displayedCount,
            color = textPrimary,
        )
    }
}

@Composable
private fun CompactOverviewChip(
    modifier: Modifier = Modifier,
    label: String,
    value: Int,
    color: Color,
) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                color = color.copy(alpha = 0.75f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Text(
                text = value.toString(),
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
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
    val currentBorder = if (isActive) accent.copy(alpha = 0.85f) else outline.copy(alpha = 0.72f)
    val frameColors = when {
        feature.signalAfter -> listOf(success, accent, Color(0xFF26C6DA))
        feature.dataLoss -> listOf(danger, warning, accent)
        else -> listOf(Color(0xFF7C4DFF), accent, success)
    }
    val headerLabel = feature.supportedBrands.firstOrNull()?.uppercase() ?: feature.chipRange.displayName.uppercase()
    val actionLabel = when {
        isActive -> "RUNNING"
        feature.isFree -> "RUN"
        else -> "RUN ${feature.costCredits}¢"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp)
            .shimmerBorder(
                colors = frameColors,
                borderWidth = if (isActive) 1.6.dp else 1.dp,
                cornerRadius = 18.dp,
            )
            .border(1.dp, currentBorder, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = panelBg.copy(alpha = 0.96f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = headerLabel,
                            color = textMuted,
                            fontSize = 8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = feature.displayName,
                            color = textPrimary,
                            fontSize = 11.sp,
                            lineHeight = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    val price = if (feature.isFree) "FREE" else "${feature.costCredits}¢"
                    Text(
                        text = price,
                        color = warning,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Text(
                    text = feature.description,
                    color = textSecondary,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CompactStatBadge(
                        text = feature.connectionMode,
                        color = connectionColor(feature.connectionMode),
                    )
                    SignalBadge(hasSignal = feature.signalAfter)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CompactStatBadge(
                        text = if (feature.isUntethered) "UNTH" else feature.chipRange.displayName,
                        color = if (feature.isUntethered) warning else textMuted,
                    )
                    CompactStatBadge(
                        text = if (feature.dataLoss) "WIPE" else "SAFE",
                        color = if (feature.dataLoss) danger else success,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .background(success, RoundedCornerShape(999.dp))
                                .height(6.dp)
                                .width(6.dp),
                        )
                    }
                    Text(
                        text = feature.mechanism.displayName,
                        color = if (isActive) success else textMuted,
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                LiquidGlassButton(
                    onClick = onExecute,
                    enabled = !isActive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp),
                ) {
                    Text(
                        text = actionLabel,
                        color = if (isActive) success else textPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactStatBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .border(1.dp, color.copy(alpha = 0.24f), RoundedCornerShape(999.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
