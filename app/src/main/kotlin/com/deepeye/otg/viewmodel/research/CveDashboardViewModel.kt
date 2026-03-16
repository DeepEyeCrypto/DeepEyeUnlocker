package com.deepeye.otg.viewmodel.research

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.intelligence.vulndb.*
import com.deepeye.otg.ui.state.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// ──────────────────────────────────────────────────────────────
// CVE Dashboard ViewModel (V3.0 — VulnIntel-AI)
// DeepEye OTG — ViewModels Module
// ──────────────────────────────────────────────────────────────

@HiltViewModel
class CveDashboardViewModel @Inject constructor(
    private val cveDao: CveDao,
    private val importer: CveImporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(CveDashboardState())
    val uiState: StateFlow<CveDashboardState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    private var filterJob: Job? = null

    init {
        viewModelScope.launch {
            cveDao.observeAll().collect { entries ->
                updateStats(entries)
                applyFiltersAndSort(entries)
            }
        }
    }

    fun onAction(action: CveDashboardAction) {
        when (action) {
            is CveDashboardAction.Search -> {
                _uiState.value = _uiState.value.copy(searchQuery = action.query)
                triggerFilter()
            }
            is CveDashboardAction.UpdateFilter -> {
                _uiState.value = _uiState.value.copy(filter = action.filter)
                triggerFilter()
            }
            is CveDashboardAction.SelectEntry -> {
                val entry = _uiState.value.allEntries.find { it.cveId == action.cveId }
                _uiState.value = _uiState.value.copy(selectedEntry = entry)
            }
            is CveDashboardAction.ClearSelection -> {
                _uiState.value = _uiState.value.copy(selectedEntry = null)
            }
            is CveDashboardAction.SortBy -> {
                _uiState.value = _uiState.value.copy(sortBy = action.field, sortAscending = action.ascending)
                triggerFilter()
            }
            is CveDashboardAction.ImportSeedData -> {
                importSeedData()
            }
            is CveDashboardAction.RefreshAll -> {
                importSeedData()
            }
            is CveDashboardAction.ClearFilters -> {
                _uiState.value = _uiState.value.copy(filter = CveFilter(), searchQuery = "")
                triggerFilter()
            }
            is CveDashboardAction.ToggleExploitFilter -> {
                val current = _uiState.value.filter
                _uiState.value = _uiState.value.copy(filter = current.copy(exploitedOnly = action.active))
                triggerFilter()
            }
            is CveDashboardAction.FilterByComponent -> {
                val current = _uiState.value.filter
                val newComponents = if (current.components.contains(action.component)) {
                    current.components - action.component
                } else {
                    current.components + action.component
                }
                _uiState.value = _uiState.value.copy(filter = current.copy(components = newComponents))
                triggerFilter()
            }
        }
    }

    private fun updateStats(entries: List<CveEntry>) {
        val componentStats = entries.groupBy { it.component }
            .map { (comp, list) ->
                ComponentStat(
                    component = comp,
                    cnt = list.size
                )
            }.sortedByDescending { it.cnt }

        val exploitableCount = entries.count { it.exploitedInWild == true }
        
        _uiState.value = _uiState.value.copy(
            allEntries = entries,
            totalCount = entries.size,
            componentStats = componentStats,
            lastSyncAt = System.currentTimeMillis()
        )
    }

    private fun triggerFilter() {
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            delay(150)
            applyFiltersAndSort(_uiState.value.allEntries)
        }
    }

    private fun applyFiltersAndSort(entries: List<CveEntry>) {
        val state = _uiState.value
        val query = state.searchQuery.lowercase().trim()
        val f = state.filter

        // 1. Filter
        var filtered = entries.filter { cve ->
            var matches = true

            if (query.isNotEmpty()) {
                val qMatch = cve.cveId.lowercase().contains(query) ||
                        cve.title.lowercase().contains(query) ||
                        cve.component.lowercase().contains(query)
                if (!qMatch) matches = false
            }

            if (matches && f.components.isNotEmpty() && !f.components.contains(cve.component)) {
                matches = false
            }

            if (matches && f.severityMin != null && (cve.cvssScore ?: 0.0) < f.severityMin) {
                matches = false
            }

            if (matches && f.exploitedOnly && cve.exploitedInWild != true) {
                matches = false
            }

            if (matches && f.cisaKevOnly && !cve.cisaKev) {
                matches = false
            }

            if (matches && f.confidenceLevels.isNotEmpty() && !f.confidenceLevels.contains(cve.confidence)) {
                matches = false
            }

            if (matches && f.bugClasses.isNotEmpty() && !f.bugClasses.contains(cve.bugClass)) {
                matches = false
            }

            matches
        }

        // 2. Sort
        filtered = when (state.sortBy) {
            CveSortField.CVE_ID -> filtered.sortedBy { it.cveId }
            CveSortField.COMPONENT -> filtered.sortedBy { it.component }
            CveSortField.CVSS_SCORE -> filtered.sortedBy { it.cvssScore ?: 0.0 }
            CveSortField.CONFIDENCE -> filtered.sortedBy { it.confidence.ordinal }
            CveSortField.UPDATED_AT -> filtered.sortedBy { it.updatedAt }
        }

        if (!state.sortAscending) {
            filtered = filtered.reversed()
        }

        _uiState.value = _uiState.value.copy(filteredEntries = filtered)
    }

    private fun importSeedData() {
        if (_uiState.value.importProgress != null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(importProgress = 0f, error = null)
            try {
                for (i in 1..5) {
                    delay(50)
                    _uiState.value = _uiState.value.copy(importProgress = i / 5f)
                }

                importer.importSeedData()
                _uiState.value = _uiState.value.copy(importProgress = null)
                _uiEvents.emit(UiEvent.ShowToast("VulnIntel-AI DB synchronized"))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Sync failed: ${e.message}", importProgress = null)
                _uiEvents.emit(UiEvent.ShowSnackbar("Sync Failed: ${e.message}", isError = true))
            }
        }
    }
}
