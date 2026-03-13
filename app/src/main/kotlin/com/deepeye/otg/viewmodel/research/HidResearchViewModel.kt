package com.deepeye.otg.viewmodel.research

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.hid.*
import com.deepeye.otg.ui.state.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// ──────────────────────────────────────────────────────────────
// HID Research ViewModel
// DeepEye OTG — ViewModels Module (Part 9)
// ──────────────────────────────────────────────────────────────

@HiltViewModel
class HidResearchViewModel @Inject constructor(
    private val parser: HidDescriptorParser,
    private val generator: HidCorpusGenerator,
    private val variantTracker: HidVariantTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(HidResearchState())
    val uiState: StateFlow<HidResearchState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    init {
        // Keep UI synchronized with the VariantTracker DB
        viewModelScope.launch {
            variantTracker.trackedVariants.collect { dbVariants ->
                val displayItems = dbVariants.map { variant ->
                    VariantListItem(
                        id = variant.id,
                        name = variant.name,
                        driverFamily = variant.driverFamily,
                        category = variant.category.name,
                        descriptorSize = variant.descriptor.size,
                        effectCount = variant.observedEffects.size,
                        hasCrash = variant.observedEffects.any { it.effect == HidVariantTracker.EffectType.CRASH_USERSPACE || it.effect == HidVariantTracker.EffectType.CRASH_KERNEL }
                    )
                }
                _uiState.value = _uiState.value.copy(trackedVariants = displayItems)
            }
        }
    }

    fun onAction(action: HidAction) {
        when (action) {
            is HidAction.SelectTab -> {
                _uiState.value = _uiState.value.copy(selectedTab = action.tab)
            }
            is HidAction.ParseDescriptor -> {
                parseInput(action.data)
            }
            is HidAction.GenerateCorpus -> {
                generateReferenceCorpus()
            }
            is HidAction.AddVariant -> {
                viewModelScope.launch {
                    variantTracker.addVariant(
                        id = action.id,
                        name = action.name,
                        descriptor = action.descriptor,
                        driverFamily = action.driverFamily,
                        category = HidVariantTracker.VariantCategory.UNKNOWN
                    )
                    _uiEvents.emit(UiEvent.ShowToast("Variant added to tracker"))
                }
            }
            is HidAction.CompareVariants -> {
                compare(action.idA, action.idB)
            }
            is HidAction.SelectVariant -> {
               // Detail view handling
            }
        }
    }

    private fun parseInput(data: ByteArray) {
        try {
            val result = parser.parse(data)
            
            // Map strictly correctly to UI display schemas
            val summary = HidDescriptorSummary(
                totalItems = result.items.size,
                totalCollections = result.collections.size,
                reportIds = result.reportIds,
                usagePages = result.usagePages.map { HidDescriptorParser.USAGE_PAGE_NAMES[it] ?: "0x${it.toString(16)}" },
                malformationCount = result.malformations.size,
                isWellFormed = result.isWellFormed,
                hasCriticalIssues = result.hasCriticalMalformations,
                rawSize = data.size
            )

            val displayItems = result.items.map {
                HidItemDisplay(
                    offset = it.offset,
                    tagName = it.tagName,
                    type = it.type.name,
                    dataValue = it.dataValue,
                    rawHex = it.rawBytes.joinToString(" ") { b -> "%02X".format(b) }
                )
            }

            val malforms = result.malformations.map {
                HidMalformationDisplay(
                    offset = it.offset,
                    severity = it.severity.name,
                    type = it.type,
                    description = it.description
                )
            }

            val displayCollections = result.collections.map {
                HidCollectionDisplay(
                    typeName = it.typeName,
                    depth = it.depth,
                    usagePage = HidDescriptorParser.USAGE_PAGE_NAMES[it.usagePage] ?: "0x${it.usagePage.toString(16)}",
                    usage = it.usage,
                    itemCount = it.items.size
                )
            }

            _uiState.value = _uiState.value.copy(
                currentDescriptor = summary,
                parsedItems = displayItems,
                malformations = malforms,
                collections = displayCollections,
                error = null
            )

        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
error = "Parse Error: ${e.message}"
)
        }
    }

    private fun generateReferenceCorpus() {
        val corpus = generator.generateFullCorpus()
        _uiState.value = _uiState.value.copy(
            corpusGenerated = true,
            corpusFileCount = corpus.size
        )
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowToast("Generated ${corpus.size} reference HID payloads"))
        }
    }

    private fun compare(idA: String, idB: String) {
        val result = variantTracker.compareVariants(idA, idB)
        if (result != null) {
            val structurallyIdentical = result.itemCountA == result.commonItems && result.itemCountB == result.commonItems
            _uiState.value = _uiState.value.copy(
                comparisonResult = ComparisonDisplay(
                    variantA = idA,
                    variantB = idB,
                    diffCount = result.diffBytes.size,
                    summary = if (structurallyIdentical) "Structurally Identical" else "Structural differences found"
                )
            )
        } else {
            viewModelScope.launch { _uiEvents.emit(UiEvent.ShowSnackbar("Comparison failed (variant missing)", isError = true)) }
        }
    }
}
