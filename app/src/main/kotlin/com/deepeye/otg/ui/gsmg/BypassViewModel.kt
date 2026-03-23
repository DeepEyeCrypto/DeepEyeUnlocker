package com.deepeye.otg.ui.gsmg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.data.gsmg.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

// =============================================================================
// BypassViewModel.kt v4.0
// Supports iOS + Android + Modem/Router features across 7 tools
// =============================================================================

enum class DevicePlatform { IOS, ANDROID, MODEM_ROUTER, UNKNOWN }

data class BypassFilters(
    val category:       FeatureCategory? = null,
    val source:         FeatureSource?   = null,
    val platform:       DevicePlatform   = DevicePlatform.UNKNOWN,
    val freeOnly:       Boolean          = false,
    val signalOnly:     Boolean          = false,
    val untethered:     Boolean          = false,
    val noDataLoss:     Boolean          = false,
    val noJailbreak:    Boolean          = false,
    val offlineOnly:    Boolean          = false,
    val searchQuery:    String           = "",
    val brandFilter:    String           = "",
)

data class BypassUiState(
    val allFeatures:       List<BypassFeature>       = emptyList(),
    val displayedFeatures: List<BypassFeature>       = emptyList(),
    val filters:           BypassFilters             = BypassFilters(),
    val device:            DeviceState?              = null,
    val recommendation:    RecommendationResult?     = null,

    val wantSignal:        Boolean                   = false,
    val wantFree:          Boolean                   = false,
    val wantUntethered:    Boolean                   = false,

    val activePlan:        ExecutionPlan?            = null,
    val activeFeatureId:   String?                   = null,
    val activeEvents:      List<BypassEvent>         = emptyList(),
    val latestEvent:       BypassEvent?              = null,
    val isExecuting:       Boolean                   = false,
    val showPlanDialog:    Boolean                   = false,
    val detailFeatureId:   String?                   = null,

    val errorMessage:      String?                   = null,
    val successMessage:    String?                   = null,

    // Stats
    val totalAvailable:    Int                       = 0,
    val freeCount:         Int                       = 0,
    val signalCount:       Int                       = 0,
    val sourceBreakdown:   Map<FeatureSource, Int>   = emptyMap(),
    val categoryBreakdown: Map<FeatureCategory, Int> = emptyMap(),

    // Platform tabs
    val selectedPlatform:  DevicePlatform            = DevicePlatform.UNKNOWN,
)

@HiltViewModel
class BypassViewModel @Inject constructor(
    private val engine: BypassOperationEngine
) : ViewModel() {

    private val _state = MutableStateFlow(BypassUiState())
    val state: StateFlow<BypassUiState> = _state.asStateFlow()

    private var executeJob: Job? = null

    init {
        val all = UnifiedBypassRegistry.all
        val initialFilters = BypassFilters()
        val displayed = applyFilters(all, initialFilters)
        val current = _state.value
        _state.value = current.copy(
            allFeatures       = all,
            displayedFeatures = displayed,
            totalAvailable    = UnifiedBypassRegistry.total,
            freeCount         = UnifiedBypassRegistry.freeCount,
            signalCount       = UnifiedBypassRegistry.signalCount,
            sourceBreakdown   = UnifiedBypassRegistry.countBySource(),
            categoryBreakdown = UnifiedBypassRegistry.countByCategory(),
        )
        Timber.d("[BYPASS_VM] init total=${UnifiedBypassRegistry.total}")
    }

    // ── Device ────────────────────────────────────────────────────────────────

    fun onDeviceConnected(device: DeviceState) {
        Timber.d("[BYPASS_VM] device chip=${device.chipName} android=${device.androidBrand} " +
                 "sessionId=${device.sessionId}")

        val available = UnifiedBypassRegistry.forChip(device.chipRange)
        val rec = UnifiedBypassRegistry.recommend(
            device         = device,
            wantSignal     = _state.value.wantSignal,
            wantFree       = _state.value.wantFree,
            wantUntethered = _state.value.wantUntethered,
        )

        val platform = when {
            device.chipRange in listOf(
                ChipRange.A7_TO_A11, ChipRange.A8_TO_A11,
                ChipRange.A12_TO_A18, ChipRange.A7_TO_A18,
            ) -> DevicePlatform.IOS
            device.chipRange == ChipRange.MODEM_ALL -> DevicePlatform.MODEM_ROUTER
            else -> DevicePlatform.ANDROID
        }
        val updatedFilters = _state.value.filters.copy(platform = platform)
        val displayed = applyFilters(available, updatedFilters)
        val sourceBreakdown = UnifiedBypassRegistry.countBySource(available)
        val categoryBreakdown = UnifiedBypassRegistry.countByCategory(available)

        val current = _state.value
        _state.value = current.copy(
            device            = device,
            allFeatures       = available,
            displayedFeatures = displayed,
            recommendation    = rec,
            selectedPlatform  = platform,
            totalAvailable    = available.size,
            freeCount         = available.count { it.isFree },
            signalCount       = available.count { it.signalAfter },
            sourceBreakdown   = sourceBreakdown,
            categoryBreakdown = categoryBreakdown,
        )
    }

    fun onDeviceDisconnected() {
        cancelExecution()
        val all = UnifiedBypassRegistry.all
        val current = _state.value
        _state.value = current.copy(
            device            = null,
            recommendation    = null,
            allFeatures       = all,
            displayedFeatures = applyFilters(all, current.filters),
            totalAvailable    = UnifiedBypassRegistry.total,
            freeCount         = UnifiedBypassRegistry.freeCount,
            signalCount       = UnifiedBypassRegistry.signalCount,
            isExecuting       = false,
            activeFeatureId   = null,
            activeEvents      = emptyList(),
            latestEvent       = null,
        )
    }

    // ── Platform tab ──────────────────────────────────────────────────────────


    fun onSelectPlatform(platform: DevicePlatform) {
        val base = when (platform) {
            DevicePlatform.IOS          -> UnifiedBypassRegistry.forIos()
            DevicePlatform.ANDROID      -> UnifiedBypassRegistry.forAndroid()
            DevicePlatform.MODEM_ROUTER -> UnifiedBypassRegistry.modemRouter()
            DevicePlatform.UNKNOWN      -> UnifiedBypassRegistry.all
        }
        val sourceBreakdown = UnifiedBypassRegistry.countBySource(base)
        val categoryBreakdown = UnifiedBypassRegistry.countByCategory(base)
        val current = _state.value
        val newFilters = current.filters.copy(platform = platform)
        val displayed = applyFilters(base, newFilters)
        _state.value = current.copy(
            selectedPlatform  = platform,
            allFeatures       = base,
            filters           = newFilters,
            displayedFeatures = displayed,
            totalAvailable    = base.size,
            freeCount         = base.count { it.isFree },
            signalCount       = base.count { it.signalAfter },
            categoryBreakdown = categoryBreakdown,
            sourceBreakdown   = sourceBreakdown,
        )
    }

    // ── Filters ───────────────────────────────────────────────────────────────

    fun onCategoryFilter(cat: FeatureCategory?) {
        val currentFilters = _state.value.filters
        replaceFilters(currentFilters.copy(category = cat))
    }

    fun onSourceFilter(src: FeatureSource?) {
        val currentFilters = _state.value.filters
        replaceFilters(currentFilters.copy(source = src))
    }

    fun onToggleFreeOnly() {
        val currentFilters = _state.value.filters
        replaceFilters(currentFilters.copy(freeOnly = !currentFilters.freeOnly))
    }

    fun onToggleSignalOnly() {
        val currentFilters = _state.value.filters
        replaceFilters(currentFilters.copy(signalOnly = !currentFilters.signalOnly))
    }

    fun onToggleUntethered() {
        val currentFilters = _state.value.filters
        replaceFilters(currentFilters.copy(untethered = !currentFilters.untethered))
    }

    fun onToggleNoDataLoss() {
        val currentFilters = _state.value.filters
        replaceFilters(currentFilters.copy(noDataLoss = !currentFilters.noDataLoss))
    }

    fun onToggleNoJailbreak() {
        val currentFilters = _state.value.filters
        replaceFilters(currentFilters.copy(noJailbreak = !currentFilters.noJailbreak))
    }

    fun onToggleOfflineOnly() {
        val currentFilters = _state.value.filters
        replaceFilters(currentFilters.copy(offlineOnly = !currentFilters.offlineOnly))
    }

    fun onSearch(q: String) {
        val currentFilters = _state.value.filters
        replaceFilters(currentFilters.copy(searchQuery = q))
    }

    fun onBrandFilter(b: String) {
        val currentFilters = _state.value.filters
        replaceFilters(currentFilters.copy(brandFilter = b))
    }

    fun clearFilters() {
        val currentFilters = _state.value.filters
        replaceFilters(
            currentFilters.copy(
                category = null,
                source = null,
                freeOnly = false,
                signalOnly = false,
                untethered = false,
                noDataLoss = false,
                noJailbreak = false,
                offlineOnly = false,
                searchQuery = "",
                brandFilter = "",
            )
        )
    }

    private fun replaceFilters(filters: BypassFilters) {
        val current = _state.value
        _state.value = current.copy(
            filters = filters,
            displayedFeatures = applyFilters(current.allFeatures, filters),
        )
    }

    // ── Recommendation ────────────────────────────────────────────────────────

    fun onRefineRecommendation(signal: Boolean, free: Boolean, unt: Boolean) {
        val device = _state.value.device ?: return
        val rec = UnifiedBypassRegistry.recommend(device, signal, free, unt)
        val current = _state.value
        _state.value = current.copy(
            wantSignal = signal,
            wantFree = free,
            wantUntethered = unt,
            recommendation = rec,
        )
    }

    // ── Execution ─────────────────────────────────────────────────────────────

    fun onRequestExecute(feature: BypassFeature) {
        val device    = _state.value.device ?: fallbackDevice(feature)
        val sessionId = UUID.randomUUID().toString()
        val plan      = UnifiedBypassRegistry.buildPlan(feature, device, sessionId)

        if (feature.dataLoss || feature.riskLevel >= RiskLevel.HIGH) {
            val current = _state.value
            _state.value = current.copy(activePlan = plan, showPlanDialog = true)
        } else {
            val current = _state.value
            _state.value = current.copy(activePlan = plan)
            startExecution(feature, device, sessionId)
        }
    }

    fun onConfirmPlan() {
        val plan = _state.value.activePlan ?: return
        val current = _state.value
        _state.value = current.copy(showPlanDialog = false)
        startExecution(plan.feature, plan.device, plan.sessionId)
    }

    fun onDismissPlan() {
        val current = _state.value
        _state.value = current.copy(showPlanDialog = false, activePlan = null)
    }

    private fun startExecution(feature: BypassFeature, device: DeviceState, sessionId: String) {
        if (_state.value.isExecuting) return
        Timber.d("[BYPASS_VM] execute feature=${feature.id} sessionId=$sessionId")

        val current = _state.value
        _state.value = current.copy(
            activeFeatureId = feature.id, isExecuting = true,
            activeEvents = emptyList(), latestEvent = null,
            errorMessage = null, successMessage = null,
        )

        executeJob = viewModelScope.launch {
            engine.execute(feature, device, null, sessionId).collect { event ->
                val updatedEvents = ArrayList(_state.value.activeEvents)
                updatedEvents.add(event)
                val eventState = _state.value
                _state.value = eventState.copy(latestEvent = event, activeEvents = updatedEvents)
                when (event) {
                    is BypassEvent.Completed -> {
                        val completedState = _state.value
                        _state.value = completedState.copy(
                            isExecuting = false,
                            successMessage = buildSuccess(event, feature),
                        )
                    }
                    is BypassEvent.Failed -> {
                        val failedState = _state.value
                        _state.value = failedState.copy(
                            isExecuting = false,
                            errorMessage = "[${event.layer}] ${event.reason}",
                        )
                    }
                    else -> Unit
                }
            }
        }
    }

    fun cancelExecution() {
        executeJob?.cancel(); executeJob = null
        val current = _state.value
        if (current.isExecuting) {
            _state.value = current.copy(isExecuting = false)
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    fun onShowDetail(id: String?) {
        val current = _state.value
        _state.value = current.copy(detailFeatureId = id)
    }

    fun onDismissDetail() {
        val current = _state.value
        _state.value = current.copy(detailFeatureId = null)
    }

    fun onClearError() {
        val current = _state.value
        _state.value = current.copy(errorMessage = null)
    }

    fun onClearSuccess() {
        val current = _state.value
        _state.value = current.copy(successMessage = null)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun applyFilters(features: List<BypassFeature>, f: BypassFilters): List<BypassFeature> {
        val brandFilter = if (f.brandFilter.length >= 2) f.brandFilter.lowercase() else null
        val searchQuery = if (f.searchQuery.length >= 2) f.searchQuery.lowercase() else null
        val filtered = ArrayList<BypassFeature>(features.size)

        for (feature in features) {
            if (f.category != null && feature.category != f.category) continue
            if (f.source != null && feature.source != f.source) continue
            if (f.freeOnly && !feature.isFree) continue
            if (f.signalOnly && !feature.signalAfter) continue
            if (f.untethered && !feature.untethered) continue
            if (f.noDataLoss && feature.dataLoss) continue
            if (f.noJailbreak && feature.requiresJailbreak) continue
            if (f.offlineOnly && feature.requiresInternet) continue
            if (brandFilter != null && !matchesBrandFilter(feature, brandFilter)) continue
            if (searchQuery != null && !matchesSearchQuery(feature, searchQuery)) continue
            filtered.add(feature)
        }

        filtered.sortWith { left, right ->
            val categoryComparison = left.category.ordinal.compareTo(right.category.ordinal)
            if (categoryComparison != 0) {
                categoryComparison
            } else {
                left.costCredits.compareTo(right.costCredits)
            }
        }

        return filtered
    }

    private fun matchesBrandFilter(feature: BypassFeature, brandFilter: String): Boolean {
        if (feature.supportedBrands.isEmpty()) {
            return true
        }

        for (brand in feature.supportedBrands) {
            if (brand.lowercase().contains(brandFilter)) {
                return true
            }
        }

        for (tag in feature.tags) {
            if (tag.lowercase().contains(brandFilter)) {
                return true
            }
        }

        return false
    }

    private fun matchesSearchQuery(feature: BypassFeature, searchQuery: String): Boolean {
        if (feature.displayName.lowercase().contains(searchQuery)) return true
        if (feature.description.lowercase().contains(searchQuery)) return true

        for (tag in feature.tags) {
            if (tag.contains(searchQuery)) {
                return true
            }
        }

        for (brand in feature.supportedBrands) {
            if (brand.lowercase().contains(searchQuery)) {
                return true
            }
        }

        for (chipset in feature.supportedChipsets) {
            if (chipset.lowercase().contains(searchQuery)) {
                return true
            }
        }

        return false
    }

    private fun fallbackDevice(feature: BypassFeature): DeviceState = DeviceState(
        sessionId    = UUID.randomUUID().toString(),
        ecid         = null, imei = null, serial = null,
        chipName     = feature.chipRange.displayName,
        chipRange    = feature.chipRange,
        iosVersion   = "0",
        buildNumber  = null,
        isJailbroken = false,
        fmiEnabled   = false,
        imeiPresent  = false,
        imeiValid    = false,
        isCdmaMeid   = false,
        activated    = false,
        mdmEnrolled  = false,
        dfuMode      = false,
        adbAvailable = feature.connectionMode == "ADB",
        edlAvailable = feature.connectionMode == "EDL",
        metaAvailable= feature.connectionMode == "META",
    )

    private fun buildSuccess(event: BypassEvent.Completed, feature: BypassFeature): String =
        buildString {
            append("✅ ${feature.displayName} complete. ")
            if (event.signalEnabled) append("📶 Signal active. ")
            if (event.iServices)     append("💬 iMessage+FaceTime active. ")
            if (event.untethered)    append("🔄 Persists after reboot.")
            else                     append("⚠ Re-run after power cycle.")
        }

    override fun onCleared() { super.onCleared(); executeJob?.cancel() }
}
