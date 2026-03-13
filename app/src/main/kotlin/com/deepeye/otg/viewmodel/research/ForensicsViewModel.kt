package com.deepeye.otg.viewmodel.research

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.feature.forensics.*
import com.deepeye.otg.ui.state.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// ──────────────────────────────────────────────────────────────
// Forensics ViewModel
// DeepEye OTG — ViewModels Module (Part 9)
// ──────────────────────────────────────────────────────────────

@HiltViewModel
class ForensicsViewModel @Inject constructor(
    private val indexer: ArtifactIndexer,
    private val timelineBuilder: TimelineBuilder,
    private val hashVerifier: HashVerifier,
    private val exporter: ReportExporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForensicsState())
    val uiState: StateFlow<ForensicsState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    // Hold raw model in memory so we can re-filter independently of serialization
    private var rawIndexResult: IndexResult? = null
    private var rawTimeline: ForensicTimeline? = null
    private var rawChainOfCustody: ChainOfCustodyRecord? = null

    fun onAction(action: ForensicsAction) {
        when (action) {
            is ForensicsAction.SelectTab -> _uiState.value = _uiState.value.copy(selectedTab = action.tab)
            
            is ForensicsAction.StartIndex -> {
                val file = File(action.rootPath)
                if (!file.exists() || !file.canRead()) {
                    viewModelScope.launch { _uiEvents.emit(UiEvent.ShowSnackbar("Directory unreadable: ${action.rootPath}", true)) }
                    return
                }
                
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val result = indexer.index(file, action.computeHashes)
                        rawIndexResult = result
                        
                        val display = IndexResultDisplay(
                            totalFiles = result.totalFiles,
                            totalSizeFormatted = "${result.totalSizeBytes / 1024 / 1024} MB",
                            byType = result.byType.mapKeys { it.key.name },
                            errorCount = result.errors.size,
                            durationMs = result.duration
                        )
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            indexResult = display
                        )
                        _uiEvents.emit(UiEvent.ShowToast("Indexed ${result.totalFiles} files"))
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(
isLoading = false, error = "Index failed: ${e.message}"
)
                    }
                }
            }

            is ForensicsAction.BuildTimeline -> {
                val idx = rawIndexResult ?: return
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                viewModelScope.launch(Dispatchers.Default) {
                    rawTimeline = timelineBuilder.buildFromIndex(idx)
                    updateTimelineDisplay()
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }

            is ForensicsAction.FilterTimeline -> {
                _uiState.value = _uiState.value.copy(timelineFilter = action.filter)
                updateTimelineDisplay()
            }

            is ForensicsAction.VerifyHash -> {
                val file = File(action.filePath)
                viewModelScope.launch(Dispatchers.IO) {
                    val result = hashVerifier.verify(file, action.expectedHash)
                    _uiState.value = _uiState.value.copy(
                        verificationStatus = VerificationDisplay(
                            totalFiles = 1,
                            verified = 1,
                            mismatched = if (result.matches == true) 0 else 1,
                            errors = 0,
                            allPassed = result.matches == true
                        )
                    )
                }
            }

            is ForensicsAction.GenerateChainOfCustody -> {
                val idx = rawIndexResult ?: return
                rawChainOfCustody = hashVerifier.generateChainOfCustody(
                    idx.artifacts,
                    action.examinerName,
                    action.caseId,
                    "Generated via automated UI request"
                )
                
                _uiState.value = _uiState.value.copy(
chainOfCustody = ChainOfCustodyDisplay(
                        caseId = action.caseId,
                        examinerName = action.examinerName,
                        acquisitionTimeFormatted = "Just now",
                        totalArtifacts = idx.totalFiles,
                        totalSizeFormatted = "${idx.totalSizeBytes / 1024 / 1024} MB"
                    )
)
            }

            is ForensicsAction.ExportReport -> {
                val idx = rawIndexResult
                if (idx == null) {
                    viewModelScope.launch { _uiEvents.emit(UiEvent.ShowSnackbar("Cannot export empty index", true)) }
                    return
                }

                _uiState.value = _uiState.value.copy(isExporting = true)
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val file = if (action.format == "JSON") {
                             exporter.exportJson(idx, rawTimeline, null, rawChainOfCustody)
                        } else {
                             exporter.exportText(idx, rawTimeline, rawChainOfCustody)
                        }
                        _uiState.value = _uiState.value.copy(
                            isExporting = false,
                            lastExportPath = file.absolutePath
                        )
                        _uiEvents.emit(UiEvent.ShareFile(file.absolutePath, "application/octet-stream"))
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(
isExporting = false, error = "Export Failed: ${e.message}"
)
                    }
                }
            }

            is ForensicsAction.ClearIndex -> {
                rawIndexResult = null
                rawTimeline = null
                rawChainOfCustody = null
                _uiState.value = ForensicsState()
            }
        }
    }

    private fun updateTimelineDisplay() {
        val tl = rawTimeline ?: return
        val filter = _uiState.value.timelineFilter
        
        val catsToFilter = filter.selectedCategories.mapNotNull { 
            runCatching { EventCategory.valueOf(it) }.getOrNull() 
        }.toSet()

        val matching = timelineBuilder.filter(
            startTime = filter.startTime,
            endTime = filter.endTime,
            categories = catsToFilter.ifEmpty { null },
            searchQuery = filter.searchQuery.ifBlank { null }
        )

        val displays = matching.take(1000).map { 
            TimelineEventDisplay(
                timestampFormatted = it.timestamp.toString(), // Simplify for compose logic
                source = it.source,
                category = it.category.name,
                action = it.action,
                description = it.description,
                confidence = it.confidence.name
            )
        }
        _uiState.value = _uiState.value.copy(timelineEvents = displays)
    }
}
