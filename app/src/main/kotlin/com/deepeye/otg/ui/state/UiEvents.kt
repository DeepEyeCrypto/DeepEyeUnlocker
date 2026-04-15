package com.deepeye.otg.ui.state

// ──────────────────────────────────────────────────────────────
// UI Event Models — User Actions + Navigation
// DeepEye OTG — UI Module (Part 8)
// ──────────────────────────────────────────────────────────────

/**
 * One-shot UI events that should not be re-emitted on recomposition.
 */
sealed class UiEvent {
    data class ShowSnackbar(val message: String, val isError: Boolean = false) : UiEvent()
    data class Navigate(val route: String) : UiEvent()
    data class ShowDialog(val dialogType: DialogType) : UiEvent()
    object DismissDialog : UiEvent()
    data class ShareFile(val filePath: String, val mimeType: String = "application/json") : UiEvent()
    data class ShowToast(val message: String) : UiEvent()
}

enum class DialogType {
    CONFIRM_ANALYSIS,
    ANALYSIS_COMPLETE,
    FUZZ_CONFIG,
    FUZZ_STOP_CONFIRM,
    IMPORT_CVE,
    EXPORT_REPORT,
    CRASH_DETAIL,
    HID_DETAIL,
    FILTER_SETTINGS,
    REMEDIATION_PLAN,
    CHAIN_OF_CUSTODY,
    ERROR_DETAIL
}

// ── CVE Dashboard Actions ───────────────────────────────────

sealed class CveDashboardAction {
    data class Search(val query: String) : CveDashboardAction()
    data class UpdateFilter(val filter: CveFilter) : CveDashboardAction()
    data class SelectEntry(val cveId: String) : CveDashboardAction()
    object ClearSelection : CveDashboardAction()
    data class SortBy(val field: CveSortField, val ascending: Boolean) : CveDashboardAction()
    object ImportSeedData : CveDashboardAction()
    object RefreshAll : CveDashboardAction()
    object ClearFilters : CveDashboardAction()
    data class ToggleExploitFilter(val active: Boolean) : CveDashboardAction()
    data class FilterByComponent(val component: String) : CveDashboardAction()
}

// ── Device Analysis Actions ─────────────────────────────────

sealed class DeviceAnalysisAction {
    object StartEnumeration : DeviceAnalysisAction()
    object StartAnalysis : DeviceAnalysisAction()
    object StopAnalysis : DeviceAnalysisAction()
    data class SelectTab(val tab: DeviceTab) : DeviceAnalysisAction()
    object RefreshProfile : DeviceAnalysisAction()
    object ExportReport : DeviceAnalysisAction()
    data class AcknowledgeFinding(val findingId: String) : DeviceAnalysisAction()
    object ViewRemediationPlan : DeviceAnalysisAction()
}

// ── Fuzzing Actions ─────────────────────────────────────────

sealed class FuzzAction {
    data class UpdateConfig(val config: FuzzConfigState) : FuzzAction()
    object StartFuzzing : FuzzAction()
    object StopFuzzing : FuzzAction()
    object PauseFuzzing : FuzzAction()
    object ResumeFuzzing : FuzzAction()
    data class SelectCrash(val testCaseId: String) : FuzzAction()
    object ReplayCrashes : FuzzAction()
    data class LoadSession(val sessionId: String) : FuzzAction()
    object GenerateCorpus : FuzzAction()
}

// ── HID Research Actions ────────────────────────────────────

sealed class HidAction {
    data class ParseDescriptor(val data: ByteArray) : HidAction() {
        override fun equals(other: Any?) = other is ParseDescriptor && data.contentEquals(other.data)
        override fun hashCode() = data.contentHashCode()
    }
    data class SelectTab(val tab: HidTab) : HidAction()
    object GenerateCorpus : HidAction()
    data class AddVariant(
        val id: String,
        val name: String,
        val descriptor: ByteArray,
        val driverFamily: String
    ) : HidAction() {
        override fun equals(other: Any?) = other is AddVariant && id == other.id
        override fun hashCode() = id.hashCode()
    }
    data class CompareVariants(val idA: String, val idB: String) : HidAction()
    data class SelectVariant(val id: String) : HidAction()
}

// ── Forensics Actions ───────────────────────────────────────

sealed class ForensicsAction {
    data class StartIndex(val rootPath: String, val computeHashes: Boolean = true) : ForensicsAction()
    data class SelectTab(val tab: ForensicsTab) : ForensicsAction()
    object BuildTimeline : ForensicsAction()
    data class FilterTimeline(val filter: TimelineFilterState) : ForensicsAction()
    data class VerifyHash(val filePath: String, val expectedHash: String) : ForensicsAction()
    data class ExportReport(val format: String) : ForensicsAction()
    data class GenerateChainOfCustody(val caseId: String, val examinerName: String) : ForensicsAction()
    object StartThreatScan : ForensicsAction()
    data class FetchModelIntel(val modelName: String) : ForensicsAction()
    object ClearIndex : ForensicsAction()
}

// ── Navigation Routes ───────────────────────────────────

object Routes {
    const val CVE_DASHBOARD = "cve_dashboard"
    const val DEVICE_ANALYSIS = "device_analysis"
    const val FUZZ_DASHBOARD = "fuzz_dashboard"
    const val HID_RESEARCH = "hid_research"
    const val FORENSICS = "forensics"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
}
