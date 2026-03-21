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
// BypassViewModel.kt — Production v3.0
// Clean state management, proper Job cancellation, no bugs
// =============================================================================

data class BypassFilters(
    val category:     FeatureCategory? = null,
    val source:       FeatureSource?   = null,
    val freeOnly:     Boolean          = false,
    val signalOnly:   Boolean          = false,
    val untethered:   Boolean          = false,
    val noDataLoss:   Boolean          = false,
    val noJailbreak:  Boolean          = false,
    val searchQuery:  String           = "",
)

data class BypassUiState(
    val allFeatures:       List<BypassFeature>       = emptyList(),
    val displayedFeatures: List<BypassFeature>       = emptyList(),
    val filters:           BypassFilters             = BypassFilters(),
    val device:            DeviceState?              = null,
    val recommendation:    RecommendationResult?     = null,

    // Prefs for recommendation
    val wantSignal:        Boolean                   = false,
    val wantFree:          Boolean                   = false,
    val wantUntethered:    Boolean                   = false,

    // Execution
    val activePlan:        ExecutionPlan?            = null,
    val activeFeatureId:   String?                   = null,
    val activeEvents:      List<BypassEvent>         = emptyList(),
    val latestEvent:       BypassEvent?              = null,
    val isExecuting:       Boolean                   = false,
    val showPlanDialog:    Boolean                   = false,

    // Detail sheet
    val detailFeatureId:   String?                   = null,

    // Messages
    val errorMessage:      String?                   = null,
    val successMessage:    String?                   = null,

    // Stats
    val totalAvailable:    Int                       = 0,
    val freeCount:         Int                       = 0,
    val signalCount:       Int                       = 0,
    val sourceBreakdown:   Map<FeatureSource, Int>   = emptyMap(),
    val categoryBreakdown: Map<FeatureCategory, Int> = emptyMap(),
)

@HiltViewModel
class BypassViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(BypassUiState())
    val state: StateFlow<BypassUiState> = _state.asStateFlow()

    private var executeJob: Job? = null

    init {
        val all = UnifiedBypassRegistry.all
        _state.update { s -> s.copy(
            allFeatures       = all,
            displayedFeatures = applyFilters(all, s.filters),
            totalAvailable    = UnifiedBypassRegistry.total,
            freeCount         = UnifiedBypassRegistry.freeCount,
            signalCount       = UnifiedBypassRegistry.signalCount,
            sourceBreakdown   = UnifiedBypassRegistry.countBySource(),
            categoryBreakdown = UnifiedBypassRegistry.countByCategory(),
        )}
        Timber.d("[BYPASS_VM] init total=${UnifiedBypassRegistry.total}")
    }

    // ─── Device ───────────────────────────────────────────────────────────────

    fun onDeviceConnected(device: DeviceState) {
        Timber.d("[BYPASS_VM] device connected chip=${device.chipName} " +
                 "ios=${device.iosVersion} sessionId=${device.sessionId}")

        val available = UnifiedBypassRegistry.forChip(device.chipRange)
        val rec = UnifiedBypassRegistry.recommend(
            device         = device,
            wantSignal     = _state.value.wantSignal,
            wantFree       = _state.value.wantFree,
            wantUntethered = _state.value.wantUntethered,
        )

        _state.update { s -> s.copy(
            device            = device,
            allFeatures       = available,
            displayedFeatures = applyFilters(available, s.filters),
            recommendation    = rec,
            totalAvailable    = available.size,
            freeCount         = available.count { it.isFree },
            signalCount       = available.count { it.signalAfter },
            sourceBreakdown   = available.groupBy { it.source }.mapValues { it.value.size },
            categoryBreakdown = available.groupBy { it.category }.mapValues { it.value.size },
        )}
    }

    fun onDeviceDisconnected() {
        Timber.d("[BYPASS_VM] device disconnected")
        cancelActiveExecution()
        val all = UnifiedBypassRegistry.all
        _state.update { s -> s.copy(
            device            = null,
            recommendation    = null,
            allFeatures       = all,
            displayedFeatures = applyFilters(all, s.filters),
            totalAvailable    = UnifiedBypassRegistry.total,
            freeCount         = UnifiedBypassRegistry.freeCount,
            signalCount       = UnifiedBypassRegistry.signalCount,
            activeFeatureId   = null,
            activeEvents      = emptyList(),
            latestEvent       = null,
            isExecuting       = false,
        )}
    }

    // ─── Filters ──────────────────────────────────────────────────────────────

    fun onCategoryFilter(cat: FeatureCategory?) = updateFilters { copy(category = cat) }
    fun onSourceFilter(src: FeatureSource?)     = updateFilters { copy(source = src) }
    fun onToggleFreeOnly()    = updateFilters { copy(freeOnly    = !freeOnly) }
    fun onToggleSignalOnly()  = updateFilters { copy(signalOnly  = !signalOnly) }
    fun onToggleUntethered()  = updateFilters { copy(untethered  = !untethered) }
    fun onToggleNoDataLoss()  = updateFilters { copy(noDataLoss  = !noDataLoss) }
    fun onToggleNoJailbreak() = updateFilters { copy(noJailbreak = !noJailbreak) }

    fun onSearch(q: String) = updateFilters { copy(searchQuery = q) }

    fun clearAllFilters() = updateFilters { BypassFilters() }

    private fun updateFilters(transform: BypassFilters.() -> BypassFilters) {
        _state.update { s ->
            val newFilters = s.filters.transform()
            s.copy(
                filters           = newFilters,
                displayedFeatures = applyFilters(s.allFeatures, newFilters),
            )
        }
    }

    // ─── Recommendation ───────────────────────────────────────────────────────

    fun onRefineRecommendation(signal: Boolean, free: Boolean, untethered: Boolean) {
        val device = _state.value.device ?: return
        val rec = UnifiedBypassRegistry.recommend(
            device         = device,
            wantSignal     = signal,
            wantFree       = free,
            wantUntethered = untethered,
        )
        _state.update { it.copy(
            wantSignal     = signal,
            wantFree       = free,
            wantUntethered = untethered,
            recommendation = rec,
        )}
        Timber.d("[BYPASS_VM] recommendation refined → ${rec.best?.id}")
    }

    // ─── Plan dialog ──────────────────────────────────────────────────────────

    fun onRequestExecute(feature: BypassFeature) {
        val device    = _state.value.device ?: defaultDevice()
        val sessionId = UUID.randomUUID().toString()
        val plan      = UnifiedBypassRegistry.buildPlan(feature, device, sessionId)

        if (feature.dataLoss || feature.riskLevel >= RiskLevel.HIGH) {
            _state.update { it.copy(activePlan = plan, showPlanDialog = true) }
        } else {
            _state.update { it.copy(activePlan = plan) }
            startExecution(feature, device, sessionId)
        }
    }

    fun onConfirmPlan() {
        val plan = _state.value.activePlan ?: return
        _state.update { it.copy(showPlanDialog = false) }
        startExecution(plan.feature, plan.device, plan.sessionId)
    }

    fun onDismissPlan() {
        _state.update { it.copy(showPlanDialog = false, activePlan = null) }
    }

    // ─── Execution ────────────────────────────────────────────────────────────

    private fun startExecution(
        feature:   BypassFeature,
        device:    DeviceState,
        sessionId: String,
    ) {
        if (_state.value.isExecuting) {
            Timber.w("[BYPASS_VM] execution already in progress — ignored")
            return
        }

        Timber.d("[BYPASS_VM] start execution feature=${feature.id} " +
                 "sessionId=$sessionId")

        _state.update { it.copy(
            activeFeatureId = feature.id,
            isExecuting     = true,
            activeEvents    = emptyList(),
            latestEvent     = null,
            errorMessage    = null,
            successMessage  = null,
        )}

        executeJob = viewModelScope.launch {
            BypassOperationEngine.execute(feature, device, sessionId)
                .collect { event ->
                    _state.update { it.copy(
                        latestEvent  = event,
                        activeEvents = it.activeEvents + event,
                    )}

                    when (event) {
                        is BypassEvent.Completed -> {
                            Timber.d("[BYPASS_VM] completed feature=${feature.id} " +
                                     "signal=${event.signalEnabled}")
                            _state.update { it.copy(
                                isExecuting    = false,
                                successMessage = buildSuccessMsg(event, feature),
                            )}
                        }
                        is BypassEvent.Failed -> {
                            Timber.w("[BYPASS_VM] failed feature=${feature.id} " +
                                     "layer=${event.layer} reason=${event.reason}")
                            _state.update { it.copy(
                                isExecuting  = false,
                                errorMessage = "[${event.layer}] ${event.reason}",
                            )}
                        }
                        else -> Unit
                    }
                }
        }
    }

    fun cancelActiveExecution() {
        executeJob?.cancel()
        executeJob = null
        if (_state.value.isExecuting) {
            _state.update { it.copy(isExecuting = false) }
            Timber.d("[BYPASS_VM] execution cancelled by user")
        }
    }

    // ─── UI ───────────────────────────────────────────────────────────────────

    fun onShowDetail(featureId: String?)  { _state.update { it.copy(detailFeatureId = featureId) } }
    fun onDismissDetail()                 { _state.update { it.copy(detailFeatureId = null) } }
    fun onClearError()                    { _state.update { it.copy(errorMessage = null) } }
    fun onClearSuccess()                  { _state.update { it.copy(successMessage = null) } }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun applyFilters(
        features: List<BypassFeature>,
        filters:  BypassFilters,
    ): List<BypassFeature> {
        var list = features
        filters.category?.let  { cat -> list = list.filter { it.category == cat } }
        filters.source?.let    { src -> list = list.filter { it.source == src } }
        if (filters.freeOnly)    list = list.filter { it.isFree }
        if (filters.signalOnly)  list = list.filter { it.signalAfter }
        if (filters.untethered)  list = list.filter { it.untethered }
        if (filters.noDataLoss)  list = list.filter { !it.dataLoss }
        if (filters.noJailbreak) list = list.filter { !it.requiresJailbreak }
        if (filters.searchQuery.length >= 2) {
            val q = filters.searchQuery.lowercase().trim()
            list = list.filter {
                q in it.displayName.lowercase() ||
                q in it.description.lowercase() ||
                it.tags.any { tag -> q in tag }
            }
        }
        return list.sortedWith(compareBy({ it.category.ordinal }, { it.costCredits }))
    }

    private fun defaultDevice(): DeviceState = DeviceState(
        sessionId    = UUID.randomUUID().toString(),
        ecid         = null, imei = null, serial = null,
        chipName     = "Unknown Chip",
        chipRange    = ChipRange.A7_TO_A18,
        iosVersion   = "15.0",
        buildNumber  = null,
        isJailbroken = false,
        fmiEnabled   = true,
        imeiPresent  = false,
        imeiValid    = false,
        isCdmaMeid   = false,
        activated    = false,
        mdmEnrolled  = false,
        dfuMode      = false,
    )

    private fun buildSuccessMsg(
        event:   BypassEvent.Completed,
        feature: BypassFeature,
    ): String = buildString {
        append("✅ ${feature.displayName} completed. ")
        if (event.signalEnabled) append("📶 SIM/signal active. ")
        if (event.iServices)     append("💬 iMessage + FaceTime active. ")
        if (event.untethered)    append("🔄 Persists after reboot.")
        else                     append("⚠ Re-run after full power cycle.")
    }

    override fun onCleared() {
        super.onCleared()
        executeJob?.cancel()
    }
}
