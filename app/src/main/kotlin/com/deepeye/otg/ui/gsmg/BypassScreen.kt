package com.deepeye.otg.ui.gsmg

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
            userScrollEnabled = true,
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

            if (uiState.displayedFeatures.isEmpty()) {
                item(
                    key = "empty_state",
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    EmptyState(query = uiState.filters.searchQuery)
                }
            } else {
                items(
                    items = uiState.displayedFeatures,
                    key = { feature -> feature.id },
                    contentType = { "bypass_feature" },
                ) { feature ->
                    FeatureCard(
                        feature = feature,
                        status = resolveFeatureRunStatus(
                            featureId = feature.id,
                            activeFeatureId = uiState.activeFeatureId,
                            isExecuting = uiState.isExecuting,
                            latestEvent = uiState.latestEvent,
                        ),
                        onExecute = { viewModel.onRequestExecute(feature) },
                    )
                }
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
    val animatedValue by animateIntAsState(
        targetValue = value,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "overview_count_$label",
    )

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
                maxLines = 1
            )
            Text(
                text = animatedValue.toString(),
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum",
                maxLines = 1
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
    status: FeatureRunStatus,
    onExecute: () -> Unit,
) {
    var expanded by remember(feature.id) { mutableStateOf(false) }
    val isRunning = status == FeatureRunStatus.RUNNING
    val connectionTint = connectionColor(feature.connectionMode)
    val framePrimary = when {
        status == FeatureRunStatus.SUCCESS -> success
        status == FeatureRunStatus.ERROR -> danger
        feature.signalAfter -> success
        feature.dataLoss -> danger
        else -> Color(0xFF7C4DFF)
    }
    val frameSecondary = when {
        status == FeatureRunStatus.SUCCESS -> Color(0xFF86EFAC)
        status == FeatureRunStatus.ERROR -> Color(0xFFFF6B9A)
        feature.dataLoss -> warning
        else -> connectionTint
    }
    val headerLabel = feature.supportedBrands.firstOrNull()?.uppercase() ?: feature.chipRange.displayName.uppercase()
    val priceLabel = if (feature.isFree) "FREE" else "${feature.costCredits}¢"
    val priceColor = if (feature.isFree) success else warning
    val targetLabel = if (feature.isUntethered) "UNTH" else feature.chipRange.displayName
    val priorityScore = featurePriorityScore(feature)
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 0f else 180f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "feature_expand_${feature.id}",
    )
    val borderTransition = rememberInfiniteTransition(label = "feature-border-${feature.id}")
    val borderAngle by borderTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
        ),
        label = "feature-border-angle-${feature.id}",
    )
    val shape = RoundedCornerShape(18.dp)
    val strokeWidth = if (isRunning) 1.6.dp else 1.1.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 168.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { expanded = !expanded }
            .clip(shape)
            .drawWithCache {
                val strokePx = strokeWidth.toPx()
                val inset = strokePx / 2f
                val cornerPx = 18.dp.toPx()
                val center = Offset(size.width / 2f, size.height / 2f)
                val glowCenter = Offset(x = size.width * 0.18f, y = size.height * 0.14f)
                val borderBrush = Brush.sweepGradient(
                    colors = listOf(
                        framePrimary.copy(alpha = 0.04f),
                        framePrimary.copy(alpha = if (isRunning) 0.92f else 0.74f),
                        frameSecondary.copy(alpha = 0.52f),
                        framePrimary.copy(alpha = 0.04f),
                        Color.Transparent,
                    ),
                    center = center,
                )

                onDrawWithContent {
                    drawRoundRect(
                        color = Color(0xFF080810),
                        size = size,
                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(framePrimary.copy(alpha = 0.07f), Color.Transparent),
                            center = glowCenter,
                            radius = size.width * 0.7f,
                        ),
                        center = glowCenter,
                        radius = size.width * 0.7f,
                    )
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.04f), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.3f,
                        ),
                        size = size,
                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                    )
                    drawContent()

                    rotate(degrees = borderAngle, pivot = center) {
                        drawRoundRect(
                            brush = borderBrush,
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - strokePx, size.height - strokePx),
                            cornerRadius = CornerRadius(cornerPx, cornerPx),
                            style = Stroke(
                                width = strokePx,
                                pathEffect = PathEffect.cornerPathEffect(cornerPx),
                            ),
                        )
                    }

                    drawRoundRect(
                        color = if (isRunning) framePrimary.copy(alpha = 0.16f) else outline.copy(alpha = 0.82f),
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - strokePx, size.height - strokePx),
                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                        style = Stroke(width = 0.8.dp.toPx()),
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 9.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = headerLabel,
                        color = textMuted,
                        fontSize = 7.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = feature.displayName,
                        color = textPrimary,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    PriorityBadge(score = priorityScore)
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(999.dp))
                            .border(0.6.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(999.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "⌃",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.graphicsLayer { rotationZ = chevronRotation },
                        )
                    }
                }
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
                SignalStatusChip(hasSignal = feature.signalAfter)
                CompactFeatureBadge(
                    text = feature.connectionMode,
                    color = connectionTint,
                    background = connectionTint.copy(alpha = 0.12f),
                )
                CompactFeatureBadge(
                    text = priceLabel,
                    color = priceColor,
                    background = priceColor.copy(alpha = 0.12f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CompactFeatureBadge(
                    text = feature.mechanism.displayName,
                    color = textSecondary,
                    background = Color.White.copy(alpha = 0.05f),
                    borderColor = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f),
                )
                CompactFeatureBadge(
                    text = targetLabel,
                    color = if (feature.isUntethered) warning else textMuted,
                    background = if (feature.isUntethered) warning.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.05f),
                )
                CompactFeatureBadge(
                    text = if (feature.dataLoss) "WIPE" else "SAFE",
                    color = if (feature.dataLoss) danger else success,
                    background = if (feature.dataLoss) danger.copy(alpha = 0.12f) else success.copy(alpha = 0.12f),
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(150)),
                exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(120)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 3.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .padding(5.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    InfoRow(
                        label = "SOURCE",
                        value = feature.source.displayName,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                    InfoRow(
                        label = "CHIP",
                        value = feature.supportedChipsets.firstOrNull() ?: feature.chipRange.displayName,
                        color = Color(0xFF60A5FA),
                    )
                    InfoRow(
                        label = "WINDOW",
                        value = "${feature.estimatedMinutes.first}-${feature.estimatedMinutes.last} min",
                        color = accent,
                    )
                    InfoRow(
                        label = "MODE",
                        value = when {
                            feature.requiresInternet -> "ONLINE REQUIRED"
                            feature.isUntethered -> "UNTETHERED"
                            feature.requiresDfu -> "DFU REQUIRED"
                            else -> "STANDARD"
                        },
                        color = when {
                            feature.requiresInternet -> warning
                            feature.isUntethered -> success
                            feature.requiresDfu -> accent
                            else -> Color.White.copy(alpha = 0.45f)
                        },
                    )
                    Text(
                        text = feature.detailedDescription.ifBlank { feature.description },
                        color = Color.White.copy(alpha = 0.32f),
                        fontSize = 6.sp,
                        lineHeight = 8.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            EnhancedRunButton(
                status = status,
                onClick = onExecute,
            )
        }
    }
}

@Composable
private fun SignalStatusChip(hasSignal: Boolean) {
    val signalColor = if (hasSignal) success else textMuted

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (hasSignal) success.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.05f))
            .border(0.6.dp, signalColor.copy(alpha = 0.32f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            PulsingDot(color = signalColor, isActive = hasSignal)
            Text(
                text = if (hasSignal) "SIGNAL+" else "NO SIGNAL",
                color = signalColor,
                fontSize = 6.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PulsingDot(color: Color, isActive: Boolean) {
    if (!isActive) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(color.copy(alpha = 0.5f), RoundedCornerShape(999.dp)),
        )
        return
    }

    val dotTransition = rememberInfiniteTransition(label = "signal_dot")
    val scale by dotTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "signal_dot_scale",
    )

    Box(
        modifier = Modifier
            .size(4.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(color, RoundedCornerShape(999.dp)),
    )
}

@Composable
private fun CompactFeatureBadge(
    text: String,
    color: Color,
    background: Color,
    modifier: Modifier = Modifier,
    borderColor: Color = color.copy(alpha = 0.32f),
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .border(0.6.dp, borderColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 6.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.2f),
            fontSize = 6.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = value,
            color = color,
            fontSize = 6.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PriorityBadge(score: Int) {
    val glowAlpha = if (score >= 90) {
        val glowTransition = rememberInfiniteTransition(label = "priority_glow_$score")
        val animatedAlpha by glowTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "priority_glow_alpha_$score",
        )
        animatedAlpha
    } else {
        0f
    }
    val (pillBg, pillBorder, pillText) = when {
        score >= 90 -> Triple(
            Color(0xFFEAB308).copy(alpha = 0.18f),
            Color(0xFFEAB308).copy(alpha = 0.55f),
            Color(0xFFFEF08A),
        )
        score >= 75 -> Triple(
            Color(0xFF22C55E).copy(alpha = 0.18f),
            Color(0xFF22C55E).copy(alpha = 0.45f),
            Color(0xFF86EFAC),
        )
        else -> Triple(
            Color.White.copy(alpha = 0.06f),
            Color.White.copy(alpha = 0.12f),
            Color.White.copy(alpha = 0.4f),
        )
    }

    Box(
        modifier = Modifier
            .size(18.dp)
            .drawBehind {
                if (score >= 90) {
                    drawCircle(
                        color = Color(0xFFEAB308).copy(alpha = glowAlpha * 0.4f),
                        radius = size.width * 0.8f,
                        blendMode = BlendMode.Screen,
                    )
                }
            }
            .background(pillBg, RoundedCornerShape(999.dp))
            .border(0.6.dp, pillBorder, RoundedCornerShape(999.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = score.toString(),
            color = pillText,
            fontSize = 6.sp,
            fontWeight = FontWeight.Black,
            fontFeatureSettings = "tnum",
            maxLines = 1
        )
    }
}

@Composable
private fun EnhancedRunButton(
    status: FeatureRunStatus,
    onClick: () -> Unit,
) {
    val style = when (status) {
        FeatureRunStatus.IDLE -> RunButtonStyle(
            background = Color(0xFF7C3AED).copy(alpha = 0.12f),
            border = Color(0xFF7C3AED).copy(alpha = 0.34f),
            text = Color(0xFFC4B5FD),
            label = "▶ RUN",
        )
        FeatureRunStatus.RUNNING -> RunButtonStyle(
            background = Color(0xFF00FFFF).copy(alpha = 0.08f),
            border = Color(0xFF00FFFF).copy(alpha = 0.44f),
            text = Color(0xFF00FFFF),
            label = "● RUNNING",
        )
        FeatureRunStatus.SUCCESS -> RunButtonStyle(
            background = Color(0xFF39FF14).copy(alpha = 0.12f),
            border = Color(0xFF39FF14).copy(alpha = 0.46f),
            text = Color(0xFF39FF14),
            label = "✓ DONE",
        )
        FeatureRunStatus.ERROR -> RunButtonStyle(
            background = Color(0xFFFF007F).copy(alpha = 0.12f),
            border = Color(0xFFFF007F).copy(alpha = 0.46f),
            text = Color(0xFFFF007F),
            label = "✗ FAILED",
        )
    }
    val scale = if (status == FeatureRunStatus.RUNNING) {
        val pulseTransition = rememberInfiniteTransition(label = "feature-run-pulse")
        val animatedScale by pulseTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.03f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 650),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "feature-run-scale",
        )
        animatedScale
    } else {
        1f
    }
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(style.background)
            .border(0.5.dp, style.border, shape)
            .clickable(
                enabled = status == FeatureRunStatus.IDLE,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (status == FeatureRunStatus.SUCCESS) {
            val rippleTransition = rememberInfiniteTransition(label = "feature-success-ripple")
            val rippleScale by rippleTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 800),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "feature-success-ripple-scale",
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        drawCircle(
                            color = Color(0xFF39FF14).copy(alpha = (1f - rippleScale) * 0.3f),
                            radius = size.width * rippleScale * 0.5f,
                        )
                    },
            )
        }

        Text(
            text = style.label,
            color = style.text,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun resolveFeatureRunStatus(
    featureId: String,
    activeFeatureId: String?,
    isExecuting: Boolean,
    latestEvent: BypassEvent?,
): FeatureRunStatus {
    if (isExecuting && activeFeatureId == featureId) {
        return FeatureRunStatus.RUNNING
    }

    return when (latestEvent) {
        is BypassEvent.Completed -> if (latestEvent.featureId == featureId) FeatureRunStatus.SUCCESS else FeatureRunStatus.IDLE
        is BypassEvent.Failed -> if (latestEvent.featureId == featureId) FeatureRunStatus.ERROR else FeatureRunStatus.IDLE
        is BypassEvent.Started -> if (latestEvent.featureId == featureId) FeatureRunStatus.RUNNING else FeatureRunStatus.IDLE
        is BypassEvent.StepBegin -> if (latestEvent.featureId == featureId) FeatureRunStatus.RUNNING else FeatureRunStatus.IDLE
        is BypassEvent.StepDone -> if (latestEvent.featureId == featureId) FeatureRunStatus.RUNNING else FeatureRunStatus.IDLE
        is BypassEvent.ProgressUpdate -> if (latestEvent.featureId == featureId) FeatureRunStatus.RUNNING else FeatureRunStatus.IDLE
        is BypassEvent.NeedUserAction -> if (latestEvent.featureId == featureId) FeatureRunStatus.RUNNING else FeatureRunStatus.IDLE
        is BypassEvent.RetryingNow -> if (latestEvent.featureId == featureId) FeatureRunStatus.RUNNING else FeatureRunStatus.IDLE
        is BypassEvent.WarningIssued -> if (latestEvent.featureId == featureId) FeatureRunStatus.RUNNING else FeatureRunStatus.IDLE
        else -> FeatureRunStatus.IDLE
    }
}

private data class RunButtonStyle(
    val background: Color,
    val border: Color,
    val text: Color,
    val label: String,
)

private enum class FeatureRunStatus {
    IDLE,
    RUNNING,
    SUCCESS,
    ERROR,
}

@Composable
private fun EmptyState(query: String) {
    val floatTransition = rememberInfiniteTransition(label = "empty_state_float")
    val floatAnim by floatTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "empty_state_y",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "◎",
            fontSize = 28.sp,
            color = Color.White.copy(alpha = 0.15f),
            modifier = Modifier.graphicsLayer { translationY = floatAnim },
        )
        Text(
            text = if (query.isBlank()) "No bypasses loaded" else "No match for \"$query\"",
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = if (query.isBlank()) "Check your data source" else "Try a different carrier or model",
            color = Color.White.copy(alpha = 0.15f),
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
        )
    }
}

private fun featurePriorityScore(feature: BypassFeature): Int {
    var score = 48
    if (feature.signalAfter) score += 16
    if (feature.isUntethered) score += 14
    if (feature.isFree) score += 10
    if (!feature.dataLoss) score += 8

    score += when (feature.riskLevel) {
        com.deepeye.otg.data.gsmg.RiskLevel.LOW -> 14
        com.deepeye.otg.data.gsmg.RiskLevel.MEDIUM -> 8
        com.deepeye.otg.data.gsmg.RiskLevel.HIGH -> 4
        com.deepeye.otg.data.gsmg.RiskLevel.EXTREME -> 0
    }

    score += when (feature.confidence) {
        com.deepeye.otg.data.gsmg.ConfidenceLevel.CONFIRMED -> 12
        com.deepeye.otg.data.gsmg.ConfidenceLevel.INFERRED -> 6
        com.deepeye.otg.data.gsmg.ConfidenceLevel.HYPOTHESIS -> 0
    }

    return score.coerceIn(0, 99)
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
