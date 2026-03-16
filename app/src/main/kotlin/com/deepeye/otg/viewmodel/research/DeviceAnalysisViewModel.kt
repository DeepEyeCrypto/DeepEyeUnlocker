package com.deepeye.otg.viewmodel.research

import android.hardware.usb.UsbDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.intelligence.vulndb.DeviceObservation
import com.deepeye.otg.intelligence.vulndb.ObservedComponentVersion
import com.deepeye.otg.intelligence.vulndb.PatchStateAnalyzer
import com.deepeye.otg.intelligence.vulndb.VersionMappingEngine
import com.deepeye.otg.protocol.apple.UsbAppleSession
import com.deepeye.otg.security.RemediationGenerator
import com.deepeye.otg.security.RuleContext
import com.deepeye.otg.security.RuleEngine
import com.deepeye.otg.security.SeverityScorer
import com.deepeye.otg.ui.state.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ──────────────────────────────────────────────────────────────
// Device Analysis ViewModel
// DeepEye OTG — ViewModels Module (Part 9)
// ──────────────────────────────────────────────────────────────

class DeviceAnalysisViewModel(
    private val appleSession: UsbAppleSession,
    private val patchStateAnalyzer: PatchStateAnalyzer,
    private val versionMappingEngine: VersionMappingEngine,
    private val ruleEngine: RuleEngine,
    private val severityScorer: SeverityScorer,
    private val remediationGenerator: RemediationGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceAnalysisState())
    val uiState: StateFlow<DeviceAnalysisState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    init {
        // Observe live USB profiles
        viewModelScope.launch {
            appleSession.profile.collect { profile ->
                _uiState.value = _uiState.value.copy(
                        deviceProfile = profile,
                        connectionStatus = if (profile != null) DeviceConnectionStatus.CONNECTED
                                           else DeviceConnectionStatus.DISCONNECTED
                    )
            }
        }
    }

    fun onAction(action: DeviceAnalysisAction) {
        when (action) {
            is DeviceAnalysisAction.StartEnumeration -> {
                // Should be passed an actual UsbDevice in real flow, or rely on a system listener
            }
            is DeviceAnalysisAction.StartAnalysis -> {
                runFullAnalysis()
            }
            is DeviceAnalysisAction.StopAnalysis -> {
                _uiState.value = _uiState.value.copy(analysisPhase = AnalysisPhase.IDLE, analysisProgress = 0f)
            }
            is DeviceAnalysisAction.SelectTab -> {
                _uiState.value = _uiState.value.copy(selectedTab = action.tab)
            }
            is DeviceAnalysisAction.RefreshProfile -> {
               // Request USB session to re-read descriptor/lockdown
            }
            is DeviceAnalysisAction.ExportReport -> {
                // Export logic using RemediationGenerator
                exportReport()
            }
            is DeviceAnalysisAction.AcknowledgeFinding -> {
                val updatedFindings = _uiState.value.findings.map {
                    if (it.id == action.findingId) it.copy(acknowledged = true) else it
                }
                _uiState.value = _uiState.value.copy(findings = updatedFindings)
            }
            is DeviceAnalysisAction.ViewRemediationPlan -> {
                viewModelScope.launch { _uiEvents.emit(UiEvent.ShowDialog(DialogType.REMEDIATION_PLAN)) }
            }
        }
    }

    private fun runFullAnalysis() {
        val profile = _uiState.value.deviceProfile
        if (profile == null) {
            _uiState.value = _uiState.value.copy(error = "No device active")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                analysisPhase = AnalysisPhase.CVE_ANALYSIS,
                analysisProgress = 0.2f,
                connectionStatus = DeviceConnectionStatus.ANALYZING,
                error = null
            )

            try {
                // 1. Build observation for intelligence module
                val observation = DeviceObservation(
                    brand = "Apple",
                    model = profile.marketingName ?: profile.modelIdentifier ?: "iPhone",
                    deviceId = profile.udid ?: profile.usbSerialNumber ?: "unknown",
                    iosVersion = profile.iosVersion ?: "UNKNOWN",
                    iosBuildId = profile.buildVersion,
                    observedComponents = profile.componentBuilds.map {
                        ObservedComponentVersion(it.component, it.buildVersion)
                    }
                )

                // 2. Perform iOS patch-state exposure assessment
                val exposureReport = patchStateAnalyzer.analyze(observation)

                _uiState.value = _uiState.value.copy(
                    analysisPhase = AnalysisPhase.SECURITY_SCAN,
                    analysisProgress = 0.6f,
                    exposureReport = exposureReport
                )

                // 3. Detect silent updates
                val silentUpdates = mutableListOf<SilentUpdateInfo>()
                if (observation.iosVersion != "UNKNOWN") {
                    for (comp in profile.componentBuilds) {
                        val expected = versionMappingEngine.getExpectedBuild(observation.iosVersion, comp.component)
                        if (expected != null && comp.buildVersion != null && comp.buildVersion != expected) {
                            silentUpdates.add(SilentUpdateInfo(
                                component = comp.component,
                                expectedBuild = expected,
                                observedBuild = comp.buildVersion,
                                isNewer = compareBuilds(comp.buildVersion, expected) > 0,
                                isOlder = compareBuilds(comp.buildVersion, expected) < 0
                            ))
                        }
                    }
                }

                // 4. Run Security Context evaluation (Rule Engine)
                val ruleContext = RuleContext(
                    deviceProfile = profile,
                    exposureReport = exposureReport,
                    deviceId = observation.deviceId
                )
                val findings = ruleEngine.evaluate(ruleContext)

                // 5. Final Severity Scoring
                val score = severityScorer.score(findings, observation.deviceId)

                _uiState.value = _uiState.value.copy(
                    analysisPhase = AnalysisPhase.COMPLETE,
                    analysisProgress = 1.0f,
                    connectionStatus = DeviceConnectionStatus.CONNECTED,
                    securityScore = score,
                    findings = findings,
                    silentUpdates = silentUpdates
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    analysisPhase = AnalysisPhase.ERROR,
                    error = "Analysis failed: ${e.message}"
                )
            }
        }
    }

    private fun compareBuilds(a: String, b: String): Int {
        val partsA = a.split(".").map { it.toIntOrNull() ?: 0 }
        val partsB = b.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(partsA.size, partsB.size)
        for (i in 0 until maxLen) {
            val segA = partsA.getOrElse(i) { 0 }
            val segB = partsB.getOrElse(i) { 0 }
            if (segA != segB) return segA - segB
        }
        return 0
    }

    private fun exportReport() {
        val state = _uiState.value
        if (state.findings.isEmpty() || state.securityScore == null) return

        viewModelScope.launch {
            try {
                val plan = remediationGenerator.generatePlan(
                    findings = state.findings,
                    score = state.securityScore,
                    deviceId = state.deviceProfile?.udid ?: "unknown"
                )
                
                val file = withContext(Dispatchers.IO) {
                    remediationGenerator.exportText(plan)
                }
                
                _uiEvents.emit(UiEvent.ShareFile(file.absolutePath, "text/plain"))
            } catch (e: Exception) {
                _uiEvents.emit(UiEvent.ShowSnackbar("Export failed: ${e.message}", isError = true))
            }
        }
    }
}
