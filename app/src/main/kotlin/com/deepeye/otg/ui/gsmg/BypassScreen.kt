package com.deepeye.otg.ui.gsmg

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deepeye.otg.data.gsmg.BypassEvent
import com.deepeye.otg.data.gsmg.BypassFeature
import com.deepeye.otg.data.gsmg.BypassMechanism
import com.deepeye.otg.data.gsmg.ChipRange
import com.deepeye.otg.data.gsmg.ConfidenceLevel
import com.deepeye.otg.data.gsmg.DeviceState
import com.deepeye.otg.data.gsmg.ExecutionPlan
import com.deepeye.otg.data.gsmg.FeatureCategory
import com.deepeye.otg.data.gsmg.FeatureSource
import com.deepeye.otg.data.gsmg.RiskLevel
import com.deepeye.otg.data.gsmg.UnifiedBypassRegistry

// =============================================================================
// BypassScreen.kt — Production Compose UI v3.0
// All imports present, no missing references, no Modifier conflicts
// =============================================================================

// ─── Colour palette ──────────────────────────────────────────────────────────

private val cBg      = Color(0xFF050505)
private val cSurface = Color(0xFF0E0E0E)
private val cCard    = Color(0xFF141414)
private val cBorder  = Color(0xFF252525)
private val cAccent  = Color(0xFF2196F3)
private val cGreen   = Color(0xFF00E676)
private val cOrange  = Color(0xFFFF9800)
private val cRed     = Color(0xFFFF1744)
private val cPurple  = Color(0xFFCE93D8)
private val cCyan    = Color(0xFF00BCD4)
private val cText    = Color(0xFFEEEEEE)
private val cText2   = Color(0xFFAAAAAA)
private val cSub     = Color(0xFF666666)

private fun sourceColor(src: FeatureSource): Color = when (src) {
    FeatureSource.GSMG          -> Color(0xFF9C27B0)
    FeatureSource.IREMOVAL      -> Color(0xFF2196F3)
    FeatureSource.F3ARRAIN      -> Color(0xFF00BCD4)
    FeatureSource.GSMG_IREMOVAL -> Color(0xFF7B1FA2)
    FeatureSource.ALL_TOOLS     -> Color(0xFF4CAF50)
}

private fun riskColor(r: RiskLevel): Color = when (r) {
    RiskLevel.LOW     -> cGreen
    RiskLevel.MEDIUM  -> cOrange
    RiskLevel.HIGH    -> cRed
    RiskLevel.EXTREME -> Color(0xFFF44336)
}

private fun categoryColor(c: FeatureCategory): Color = when (c) {
    FeatureCategory.ICLOUD_BYPASS     -> cAccent
    FeatureCategory.PASSCODE          -> cOrange
    FeatureCategory.DEVICE_MANAGEMENT -> cCyan
    FeatureCategory.FIRMWARE          -> Color(0xFFE040FB)
    FeatureCategory.CARRIER           -> cGreen
    FeatureCategory.MDM               -> Color(0xFFFF7043)
    FeatureCategory.SERVICES          -> Color(0xFF26C6DA)
    FeatureCategory.DEVICE_INFO       -> cSub
    FeatureCategory.EXPLOIT_ENGINE    -> cRed
    FeatureCategory.ANDROID           -> Color(0xFF8BC34A)
}

// ─── Root screen ─────────────────────────────────────────────────────────────

@Composable
fun BypassScreen(viewModel: BypassViewModel = hiltViewModel()) {
    val st by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cBg),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            ScreenHeader(st)

            // Device banner
            st.device?.let { DeviceBanner(device = it) }

            // Recommendation
            st.recommendation?.best?.let { best ->
                RecommendedCard(
                    feature   = best,
                    reasoning = st.recommendation!!.reasoning,
                    wantSig   = st.wantSignal,
                    wantFree  = st.wantFree,
                    wantUntet = st.wantUntethered,
                    onExecute = { viewModel.onRequestExecute(best) },
                    onDetails = { viewModel.onShowDetail(best.id) },
                    onRefine  = { s, f, u -> viewModel.onRefineRecommendation(s, f, u) },
                )
            }

            // Search + filters
            SearchAndFilters(st, viewModel)

            // Category tabs
            CategoryTabs(
                selected = st.filters.category,
                onSelect = { viewModel.onCategoryFilter(it) },
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Results count
            Text(
                text     = "${st.displayedFeatures.size} / ${st.totalAvailable} features",
                color    = cSub,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Feature list
            LazyColumn(
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier            = Modifier.weight(1f),
            ) {
                items(items = st.displayedFeatures, key = { it.id }) { feature ->
                    FeatureCard(
                        feature   = feature,
                        isActive  = st.activeFeatureId == feature.id,
                        onExecute = { viewModel.onRequestExecute(feature) },
                        onDetails = { viewModel.onShowDetail(feature.id) },
                    )
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }

        // Execution overlay
        AnimatedVisibility(
            visible  = st.isExecuting,
            enter    = slideInVertically(animationSpec = tween(300)) { it } + fadeIn(),
            exit     = slideOutVertically(animationSpec = tween(300)) { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            ExecutionOverlay(
                latestEvent = st.latestEvent,
                featureId   = st.activeFeatureId ?: "",
                onCancel    = { viewModel.cancelActiveExecution() },
            )
        }

        // Plan confirmation dialog
        if (st.showPlanDialog) {
            st.activePlan?.let { plan ->
                PlanConfirmDialog(
                    plan      = plan,
                    onConfirm = { viewModel.onConfirmPlan() },
                    onDismiss = { viewModel.onDismissPlan() },
                )
            }
        }

        // Error message
        st.errorMessage?.let { msg ->
            Snackbar(
                modifier       = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action         = {
                    TextButton(onClick = { viewModel.onClearError() }) {
                        Text("DISMISS", color = cRed, fontSize = 11.sp)
                    }
                },
                containerColor = cSurface,
                contentColor   = cText,
            ) { Text(msg, fontSize = 11.sp) }
        }

        // Success message
        st.successMessage?.let { msg ->
            Snackbar(
                modifier       = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action         = {
                    TextButton(onClick = { viewModel.onClearSuccess() }) {
                        Text("OK", color = cGreen, fontSize = 11.sp)
                    }
                },
                containerColor = cSurface,
                contentColor   = cGreen,
            ) { Text(msg, fontSize = 11.sp) }
        }
    }
}

// ─── Screen header ────────────────────────────────────────────────────────────

@Composable
private fun ScreenHeader(st: BypassUiState) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = "DEEPEYE BYPASS",
                    color      = cText,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
                Text(
                    text     = "GSMG · iRemoval · F3arRa1n · checkm8.info",
                    color    = cSub,
                    fontSize = 10.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text       = "${st.totalAvailable}",
                    color      = cAccent,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(text = "features", color = cSub, fontSize = 10.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Source pills
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(st.sourceBreakdown.entries.toList()) { (src, count) ->
                if (count > 0) SourcePill(src = src, count = count)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Stat chips
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MiniChip(label = "${st.freeCount} FREE",          color = cGreen)
            MiniChip(label = "${st.signalCount} 📶 Signal",   color = cAccent)
            MiniChip(
                label = "${UnifiedBypassRegistry.untetheredCount} Untethered",
                color = cPurple,
            )
        }
    }
}

// ─── Device banner ────────────────────────────────────────────────────────────

@Composable
private fun DeviceBanner(device: DeviceState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = cCard),
        border = BorderStroke(1.dp, cAccent.copy(alpha = 0.3f)),
        shape  = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = device.chipName,
                    color      = cAccent,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text     = "iOS ${device.iosVersion} · ${device.chipRange.displayName}",
                    color    = cText2,
                    fontSize = 11.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (device.canUseSignal)  Text("📶 IMEI Valid", color = cGreen,  fontSize = 10.sp)
                if (device.fmiEnabled)    Text("🔒 FMI ON",     color = cOrange, fontSize = 10.sp)
                if (device.dfuMode)       Text("⚡ DFU",        color = cCyan,   fontSize = 10.sp)
                if (device.isJailbroken)  Text("🔓 Jailbroken", color = cPurple, fontSize = 10.sp)
            }
        }
    }
}

// ─── Recommended card ────────────────────────────────────────────────────────

@Composable
private fun RecommendedCard(
    feature:   BypassFeature,
    reasoning: String,
    wantSig:   Boolean,
    wantFree:  Boolean,
    wantUntet: Boolean,
    onExecute: () -> Unit,
    onDetails: () -> Unit,
    onRefine:  (Boolean, Boolean, Boolean) -> Unit,
) {
    var sig   by remember { mutableStateOf(wantSig) }
    var free  by remember { mutableStateOf(wantFree) }
    var untet by remember { mutableStateOf(wantUntet) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = cCard),
        border = BorderStroke(1.dp, cAccent.copy(alpha = 0.5f)),
        shape  = RoundedCornerShape(10.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text          = "✨ RECOMMENDED",
                    color         = cAccent,
                    fontSize      = 9.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight    = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.weight(1f))
                SourceTag(feature.source)
                Spacer(modifier = Modifier.width(6.dp))
                RiskTag(feature.riskLevel)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(feature.displayName, color = cText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(feature.description, color = cText2, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(6.dp))

            CapabilityBadgeRow(feature)

            Spacer(modifier = Modifier.height(4.dp))
            Text(reasoning, color = cSub, fontSize = 10.sp)

            Spacer(modifier = Modifier.height(10.dp))

            // Refine toggles
            Text("REFINE:", color = cSub, fontSize = 9.sp, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ToggleChip("📶 Signal", sig) {
                    sig = !sig; onRefine(sig, free, untet)
                }
                ToggleChip("FREE", free) {
                    free = !free; onRefine(sig, free, untet)
                }
                ToggleChip("Untethered", untet) {
                    untet = !untet; onRefine(sig, free, untet)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick  = onDetails,
                    modifier = Modifier.weight(1f),
                    border   = BorderStroke(1.dp, cBorder),
                    shape    = RoundedCornerShape(8.dp),
                ) {
                    Text("DETAILS", color = cText2, fontSize = 11.sp)
                }
                Button(
                    onClick  = onExecute,
                    modifier = Modifier.weight(2f),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = cAccent,
                        contentColor   = Color.White,
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    val label = if (feature.isFree) "EXECUTE FREE →"
                                else "EXECUTE ${feature.costCredits}¢ →"
                    Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

// ─── Search + filters ────────────────────────────────────────────────────────

@Composable
private fun SearchAndFilters(st: BypassUiState, vm: BypassViewModel) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {

        OutlinedTextField(
            value         = st.filters.searchQuery,
            onValueChange = { vm.onSearch(it) },
            placeholder   = { Text("Search features, tags...", color = cSub, fontSize = 12.sp) },
            modifier      = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = cAccent,
                unfocusedBorderColor = cBorder,
                focusedTextColor     = cText,
                unfocusedTextColor   = cText,
                cursorColor          = cAccent,
            ),
            singleLine    = true,
            textStyle     = LocalTextStyle.current.copy(fontSize = 12.sp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item { ToggleChip("FREE",         st.filters.freeOnly)    { vm.onToggleFreeOnly() } }
            item { ToggleChip("📶 Signal",    st.filters.signalOnly)  { vm.onToggleSignalOnly() } }
            item { ToggleChip("Untethered",   st.filters.untethered)  { vm.onToggleUntethered() } }
            item { ToggleChip("No Data Loss", st.filters.noDataLoss)  { vm.onToggleNoDataLoss() } }
            item { ToggleChip("No Jailbreak", st.filters.noJailbreak) { vm.onToggleNoJailbreak() } }
        }
    }
}

// ─── Category tabs ────────────────────────────────────────────────────────────

@Composable
private fun CategoryTabs(
    selected: FeatureCategory?,
    onSelect: (FeatureCategory?) -> Unit,
) {
    LazyRow(
        contentPadding      = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            CategoryTab(label = "ALL", isSelected = selected == null, color = cAccent) {
                onSelect(null)
            }
        }
        items(FeatureCategory.entries) { cat ->
            CategoryTab(
                label      = "${cat.icon} ${cat.displayName}",
                isSelected = selected == cat,
                color      = categoryColor(cat),
            ) { onSelect(cat) }
        }
    }
}

@Composable
private fun CategoryTab(
    label:      String,
    isSelected: Boolean,
    color:      Color,
    onClick:    () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent,
                RoundedCornerShape(8.dp),
            )
            .border(1.dp, if (isSelected) color else cBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text  = label,
            color = if (isSelected) color else cSub,
            fontSize = 11.sp,
        )
    }
}

// ─── Feature card ─────────────────────────────────────────────────────────────

@Composable
private fun FeatureCard(
    feature:   BypassFeature,
    isActive:  Boolean,
    onExecute: () -> Unit,
    onDetails: () -> Unit,
) {
    val srcColor    = sourceColor(feature.source)
    val borderColor = if (isActive) cGreen else cBorder

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = cCard),
        shape  = RoundedCornerShape(8.dp),
    ) {
        Row(modifier = Modifier.height(intrinsicSize = androidx.compose.foundation.layout.IntrinsicSize.Min)) {

            // Source colour bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(srcColor),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = feature.displayName,
                        color      = cText,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier   = Modifier.weight(1f),
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (feature.isFree) {
                        FreeTag()
                    } else {
                        Text("${feature.costCredits}¢", color = cOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text     = feature.description,
                    color    = cText2,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(5.dp))

                // Badge row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    MechTag(feature.mechanism)
                    ChipTag(feature.chipRange)
                    if (feature.signalAfter)    IconDot("📶", cGreen)
                    if (feature.untethered)     IconDot("🔄", cPurple)
                    if (feature.iServicesAfter) IconDot("💬", cCyan)
                    if (feature.dataLoss)       IconDot("⚠", cRed)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text     = "${feature.estimatedMinutes.first}–${feature.estimatedMinutes.last}m",
                        color    = cSub,
                        fontSize = 9.sp,
                    )
                }
            }

            // Action column
            Column(
                modifier              = Modifier.padding(end = 8.dp),
                horizontalAlignment   = Alignment.CenterHorizontally,
                verticalArrangement   = Arrangement.Center,
            ) {
                TextButton(
                    onClick         = onDetails,
                    contentPadding  = PaddingValues(4.dp),
                ) {
                    Text("ℹ", color = cSub, fontSize = 14.sp)
                }
                TextButton(
                    onClick        = onExecute,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text("RUN →", color = cAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── Execution overlay ────────────────────────────────────────────────────────

@Composable
private fun ExecutionOverlay(
    latestEvent: BypassEvent?,
    featureId:   String,
    onCancel:    () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = cSurface),
        border = BorderStroke(1.dp, cGreen.copy(alpha = 0.4f)),
        shape  = RoundedCornerShape(10.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text          = "⚡ EXECUTING",
                    color         = cGreen,
                    fontSize      = 10.sp,
                    letterSpacing = 1.sp,
                    fontWeight    = FontWeight.Bold,
                    modifier      = Modifier.weight(1f),
                )
                TextButton(
                    onClick        = onCancel,
                    contentPadding = PaddingValues(4.dp),
                ) {
                    Text("CANCEL", color = cRed, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            when (val ev = latestEvent) {
                is BypassEvent.StepBegin -> {
                    Text(
                        text       = "Step ${ev.step.stepNum}: ${ev.step.title}",
                        color      = cText,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(text = ev.step.instruction, color = cText2, fontSize = 11.sp)
                }
                is BypassEvent.NeedUserAction -> {
                    Text(
                        text       = "👆 USER ACTION REQUIRED",
                        color      = cOrange,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = ev.instruction, color = cText, fontSize = 12.sp)
                }
                is BypassEvent.ProgressUpdate -> {
                    Text(text = ev.currentPhase, color = cText2, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(5.dp))
                    LinearProgressIndicator(
                        progress      = { ev.pct / 100f },
                        modifier      = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color         = cAccent,
                        trackColor    = cBorder,
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text     = "${ev.pct}%",
                        color    = cAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.End),
                    )
                }
                is BypassEvent.RetryingNow -> {
                    Text(
                        text     = "🔄 Retrying ${ev.attempt}/${ev.maxAttempts} in ${ev.backoffMs}ms...",
                        color    = cOrange,
                        fontSize = 11.sp,
                    )
                }
                is BypassEvent.Completed -> {
                    Text(
                        text       = "✅ COMPLETED",
                        color      = cGreen,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (ev.signalEnabled) MiniChip("📶 Signal",    cGreen)
                        if (ev.iServices)     MiniChip("💬 iServices", cCyan)
                        if (ev.untethered)    MiniChip("🔄 Untethered",cPurple)
                    }
                }
                else -> {
                    LinearProgressIndicator(
                        modifier   = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color      = cAccent,
                        trackColor = cBorder,
                    )
                }
            }
        }
    }
}

// ─── Plan confirm dialog ──────────────────────────────────────────────────────

@Composable
private fun PlanConfirmDialog(
    plan:      ExecutionPlan,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = cSurface,
        shape            = RoundedCornerShape(12.dp),
        title = {
            Text(
                text       = if (plan.feature.dataLoss) "⚠ DATA LOSS WARNING" else "Confirm Operation",
                color      = if (plan.feature.dataLoss) cRed else cText,
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

                Text(plan.feature.displayName, color = cText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(plan.feature.description, color = cText2, fontSize = 11.sp)

                Spacer(modifier = Modifier.height(6.dp))

                // Warnings
                if (plan.warnings.isNotEmpty()) {
                    plan.warnings.forEach { w ->
                        Text("• $w", color = cOrange, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Prerequisites
                Text("Prerequisites:", color = cSub, fontSize = 10.sp, letterSpacing = 1.sp)
                plan.prerequisites.forEach { p ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (p.met) "✅" else "❌", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text     = p.name,
                            color    = if (p.met) cGreen else cRed,
                            fontSize = 11.sp,
                        )
                    }
                    if (!p.met) {
                        Text(
                            text     = "   → ${p.fixHint}",
                            color    = cText2,
                            fontSize = 10.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text     = "${plan.feature.executionSteps.size} steps",
                        color    = cText2,
                        fontSize = 11.sp,
                    )
                    Text("·", color = cSub, fontSize = 11.sp)
                    Text(
                        text     = "${plan.feature.estimatedMinutes.first}–" +
                                   "${plan.feature.estimatedMinutes.last} min",
                        color    = cText2,
                        fontSize = 11.sp,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = onConfirm,
                enabled  = plan.canExecute,
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = if (plan.feature.dataLoss) cRed else cAccent,
                    disabledContainerColor = cBorder,
                ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("CONFIRM & EXECUTE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = cText2, fontSize = 12.sp)
            }
        },
    )
}

// ─── Reusable composables ─────────────────────────────────────────────────────

@Composable
private fun CapabilityBadgeRow(feature: BypassFeature) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        MechTag(feature.mechanism)
        ChipTag(feature.chipRange)
        if (feature.signalAfter)    CapBadge("📶 Signal",    cGreen)
        if (feature.untethered)     CapBadge("🔄 Untethered", cPurple)
        if (feature.iServicesAfter) CapBadge("💬 iServices",  cCyan)
        if (feature.dataLoss)       CapBadge("⚠ Data Loss",  cRed)
    }
}

@Composable
private fun MechTag(mech: BypassMechanism) {
    SmallTag(text = mech.displayName, color = cPurple)
}

@Composable
private fun ChipTag(chip: ChipRange) {
    SmallTag(text = chip.displayName, color = cSub)
}

@Composable
private fun CapBadge(text: String, color: Color) {
    SmallTag(text = text, color = color)
}

@Composable
private fun SmallTag(text: String, color: Color) {
    Text(
        text     = text,
        color    = color,
        fontSize = 9.sp,
        modifier = Modifier
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

@Composable
private fun IconDot(icon: String, color: Color) {
    Text(
        text     = icon,
        fontSize = 11.sp,
        modifier = Modifier
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(3.dp))
            .padding(2.dp),
    )
}

@Composable
private fun FreeTag() {
    Text(
        text       = "FREE",
        color      = cGreen,
        fontSize   = 9.sp,
        fontWeight = FontWeight.Bold,
        modifier   = Modifier
            .background(cGreen.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

@Composable
private fun SourceTag(src: FeatureSource) {
    val c = sourceColor(src)
    SmallTag(text = src.displayName, color = c)
}

@Composable
private fun RiskTag(risk: RiskLevel) {
    val c = riskColor(risk)
    Text(
        text       = risk.name,
        color      = c,
        fontSize   = 9.sp,
        fontWeight = FontWeight.Bold,
        modifier   = Modifier
            .background(c.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

@Composable
private fun SourcePill(src: FeatureSource, count: Int) {
    val c = sourceColor(src)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(c.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Box(modifier = Modifier.size(6.dp).background(c, RoundedCornerShape(3.dp)))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "${src.displayName} $count", color = c, fontSize = 9.sp)
    }
}

@Composable
private fun MiniChip(label: String, color: Color) {
    Text(
        text       = label,
        color      = color,
        fontSize   = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier   = Modifier
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun ToggleChip(
    label:   String,
    active:  Boolean,
    onClick: () -> Unit,
) {
    val bg     = if (active) cAccent.copy(alpha = 0.15f) else Color.Transparent
    val color  = if (active) cAccent else cSub
    val border = if (active) cAccent else cBorder

    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(8.dp))
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text = label, color = color, fontSize = 11.sp)
    }
}
