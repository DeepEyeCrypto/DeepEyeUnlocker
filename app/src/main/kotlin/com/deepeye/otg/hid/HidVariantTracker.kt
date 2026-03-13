package com.deepeye.otg.hid

import javax.inject.Inject

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ──────────────────────────────────────────────────────────────
// HID Variant Tracker — Driver-Family Variant Tracking
// DeepEye OTG — HID Research Module (Part 5)
// ──────────────────────────────────────────────────────────────

private const val TAG = "HidVariantTracker"

/**
 * Tracks HID descriptor variants and their effects across
 * different driver families and OS versions.
 *
 * Purpose:
 * - Catalog descriptor variations by driver family
 * - Track which variants trigger crashes/anomalies
 * - Map variants to CVEs where applicable
 * - Support A/B comparison of descriptors
 */
class HidVariantTracker @Inject constructor() {

    /**
     * A tracked HID descriptor variant.
     */
    data class HidVariant(
        val id: String,
        val name: String,
        val descriptor: ByteArray,
        val driverFamily: String,         // e.g. "IOHIDFamily", "AppleHIDKeyboard"
        val category: VariantCategory,
        val parseResult: HidParseResult?,
        val observedEffects: MutableList<ObservedEffect> = mutableListOf(),
        val relatedCves: List<String> = emptyList(),
        val notes: String = "",
        val createdAt: Long = System.currentTimeMillis(),
        val updatedAt: Long = System.currentTimeMillis()
    ) {
        override fun equals(other: Any?) = other is HidVariant && id == other.id
        override fun hashCode() = id.hashCode()
    }

    /**
     * Variant classification.
     */
    enum class VariantCategory {
        BASELINE,       // Known-good reference descriptor
        EDGE_CASE,      // At spec boundaries
        MALFORMED,      // Spec violations
        CRASH_TRIGGER,  // Confirmed to trigger crash
        ANOMALOUS,      // Unexpected behavior, not crash
        PATCHED,        // Previously crashed, now handled
        UNKNOWN
    }

    /**
     * Observed effect when a variant is processed.
     */
    data class ObservedEffect(
        val iosVersion: String,
        val driverVersion: String? = null,
        val effect: EffectType,
        val description: String,
        val timestamp: Long = System.currentTimeMillis(),
        val reproConfirmed: Boolean = false,
        val metadata: Map<String, String> = emptyMap()
    )

    enum class EffectType {
        NO_EFFECT,          // Processed normally
        REJECTED,           // Descriptor rejected by driver
        PARTIAL_PARSE,      // Partially parsed, some fields ignored
        CRASH_KERNEL,       // Kernel panic
        CRASH_USERSPACE,    // User-space crash
        HANG,               // Device hang/freeze
        RESOURCE_EXHAUSTION,// Memory/CPU exhaustion
        ENUMERATION_FAIL,   // USB enumeration failure
        UNEXPECTED_BEHAVIOR,// Something weird but not crash
        UNKNOWN
    }

    /**
     * Comparison result between two variants.
     */
    data class VariantComparison(
        val variantA: String,
        val variantB: String,
        val sizeA: Int,
        val sizeB: Int,
        val itemCountA: Int,
        val itemCountB: Int,
        val malformationsA: Int,
        val malformationsB: Int,
        val commonItems: Int,
        val uniqueToA: List<HidItem>,
        val uniqueToB: List<HidItem>,
        val diffBytes: List<ByteDiff>,
        val summary: String
    )

    data class ByteDiff(val offset: Int, val byteA: Byte, val byteB: Byte)

    // ── Storage ─────────────────────────────────────────────────

    private val variants = mutableMapOf<String, HidVariant>()
    private val parser = HidDescriptorParser()

    private val _trackedVariants = MutableStateFlow<List<HidVariant>>(emptyList())
    val trackedVariants: StateFlow<List<HidVariant>> = _trackedVariants.asStateFlow()

    // ── CRUD Operations ─────────────────────────────────────────

    /**
     * Add a new variant to track.
     *
     * @param id unique identifier
     * @param name human-readable name
     * @param descriptor raw descriptor bytes
     * @param driverFamily target driver family
     * @param category initial classification
     * @return the created variant
     */
    fun addVariant(
        id: String,
        name: String,
        descriptor: ByteArray,
        driverFamily: String,
        category: VariantCategory = VariantCategory.UNKNOWN,
        relatedCves: List<String> = emptyList(),
        notes: String = ""
    ): HidVariant {
        val parseResult = parser.parse(descriptor)

        val variant = HidVariant(
            id = id,
            name = name,
            descriptor = descriptor,
            driverFamily = driverFamily,
            category = category,
            parseResult = parseResult,
            relatedCves = relatedCves,
            notes = notes
        )

        variants[id] = variant
        refreshFlow()

        Log.i(TAG, "Added variant: $id ($name) — ${parseResult.items.size} items, " +
                "${parseResult.malformations.size} malformations")

        return variant
    }

    /**
     * Record an observed effect for a variant.
     */
    fun recordEffect(
        variantId: String,
        iosVersion: String,
        effect: EffectType,
        description: String,
        driverVersion: String? = null,
        reproConfirmed: Boolean = false
    ) {
        val variant = variants[variantId] ?: run {
            Log.w(TAG, "Unknown variant: $variantId")
            return
        }

        variant.observedEffects.add(ObservedEffect(
            iosVersion = iosVersion,
            driverVersion = driverVersion,
            effect = effect,
            description = description,
            reproConfirmed = reproConfirmed
        ))

        // Auto-upgrade category if crash observed
        if (effect in listOf(EffectType.CRASH_KERNEL, EffectType.CRASH_USERSPACE)) {
            variants[variantId] = variant.copy(category = VariantCategory.CRASH_TRIGGER)
        }

        refreshFlow()
        Log.i(TAG, "Effect recorded for $variantId: $effect on iOS $iosVersion")
    }

    /**
     * Get a specific variant.
     */
    fun getVariant(id: String): HidVariant? = variants[id]

    /**
     * Get all variants for a specific driver family.
     */
    fun getByDriverFamily(family: String): List<HidVariant> =
        variants.values.filter { it.driverFamily == family }

    /**
     * Get all crash-triggering variants.
     */
    fun getCrashTriggers(): List<HidVariant> =
        variants.values.filter { it.category == VariantCategory.CRASH_TRIGGER }

    /**
     * Get variants related to a specific CVE.
     */
    fun getByCve(cveId: String): List<HidVariant> =
        variants.values.filter { cveId in it.relatedCves }

    /**
     * Get all tracked driver families.
     */
    fun getDriverFamilies(): Set<String> =
        variants.values.map { it.driverFamily }.toSet()

    // ── Comparison ──────────────────────────────────────────────

    /**
     * Compare two variants byte-by-byte and structurally.
     */
    fun compareVariants(idA: String, idB: String): VariantComparison? {
        val a = variants[idA] ?: return null
        val b = variants[idB] ?: return null

        val parsedA = a.parseResult ?: parser.parse(a.descriptor)
        val parsedB = b.parseResult ?: parser.parse(b.descriptor)

        // Byte-level diff
        val maxLen = maxOf(a.descriptor.size, b.descriptor.size)
        val diffs = mutableListOf<ByteDiff>()
        for (i in 0 until maxLen) {
            val byteA = a.descriptor.getOrElse(i) { 0 }
            val byteB = b.descriptor.getOrElse(i) { 0 }
            if (byteA != byteB) {
                diffs.add(ByteDiff(i, byteA, byteB))
            }
        }

        // Item-level diff
        val tagsA = parsedA.items.map { it.tagName to it.dataUnsigned }
        val tagsB = parsedB.items.map { it.tagName to it.dataUnsigned }
        val commonTags = tagsA.intersect(tagsB.toSet())

        val summary = buildString {
            appendLine("Variant Comparison: $idA vs $idB")
            appendLine("Size: ${a.descriptor.size} vs ${b.descriptor.size} bytes")
            appendLine("Items: ${parsedA.items.size} vs ${parsedB.items.size}")
            appendLine("Byte diffs: ${diffs.size}")
            appendLine("Common items: ${commonTags.size}")
        }

        return VariantComparison(
            variantA = idA,
            variantB = idB,
            sizeA = a.descriptor.size,
            sizeB = b.descriptor.size,
            itemCountA = parsedA.items.size,
            itemCountB = parsedB.items.size,
            malformationsA = parsedA.malformations.size,
            malformationsB = parsedB.malformations.size,
            commonItems = commonTags.size,
            uniqueToA = parsedA.items.filter { item -> tagsB.none { it.first == item.tagName && it.second == item.dataUnsigned } },
            uniqueToB = parsedB.items.filter { item -> tagsA.none { it.first == item.tagName && it.second == item.dataUnsigned } },
            diffBytes = diffs,
            summary = summary
        )
    }

    // ── Statistics ──────────────────────────────────────────────

    data class TrackerStats(
        val totalVariants: Int,
        val byCategory: Map<VariantCategory, Int>,
        val byDriverFamily: Map<String, Int>,
        val totalEffects: Int,
        val crashTriggerCount: Int,
        val relatedCveCount: Int
    )

    fun getStats(): TrackerStats {
        val allVariants = variants.values.toList()
        return TrackerStats(
            totalVariants = allVariants.size,
            byCategory = allVariants.groupBy { it.category }.mapValues { it.value.size },
            byDriverFamily = allVariants.groupBy { it.driverFamily }.mapValues { it.value.size },
            totalEffects = allVariants.sumOf { it.observedEffects.size },
            crashTriggerCount = allVariants.count { it.category == VariantCategory.CRASH_TRIGGER },
            relatedCveCount = allVariants.flatMap { it.relatedCves }.distinct().size
        )
    }

    private fun refreshFlow() {
        _trackedVariants.value = variants.values.toList()
    }
}
