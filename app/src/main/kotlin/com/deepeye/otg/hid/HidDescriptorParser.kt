package com.deepeye.otg.hid

import javax.inject.Inject

import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

// ──────────────────────────────────────────────────────────────
// HID Descriptor Parser — Defensive Analysis
// DeepEye OTG — HID Research Module (Part 5)
//
// Parses USB HID Report Descriptors per USB HID spec 1.11.
// Focus: descriptor parsing, malformation detection, structure
// extraction — NOT exploitation or malicious descriptor delivery.
// ──────────────────────────────────────────────────────────────

private const val TAG = "HidDescriptorParser"

/**
 * HID item types per spec.
 */
enum class HidItemType(val code: Int) {
    MAIN(0),
    GLOBAL(1),
    LOCAL(2),
    RESERVED(3)
}

/**
 * HID main item tags.
 */
enum class HidMainTag(val code: Int) {
    INPUT(0x08),
    OUTPUT(0x09),
    FEATURE(0x0B),
    COLLECTION(0x0A),
    END_COLLECTION(0x0C),
    UNKNOWN(-1)
}

/**
 * HID global item tags.
 */
enum class HidGlobalTag(val code: Int) {
    USAGE_PAGE(0x00),
    LOGICAL_MINIMUM(0x01),
    LOGICAL_MAXIMUM(0x02),
    PHYSICAL_MINIMUM(0x03),
    PHYSICAL_MAXIMUM(0x04),
    UNIT_EXPONENT(0x05),
    UNIT(0x06),
    REPORT_SIZE(0x07),
    REPORT_ID(0x08),
    REPORT_COUNT(0x09),
    PUSH(0x0A),
    POP(0x0B),
    UNKNOWN(-1)
}

/**
 * HID local item tags.
 */
enum class HidLocalTag(val code: Int) {
    USAGE(0x00),
    USAGE_MINIMUM(0x01),
    USAGE_MAXIMUM(0x02),
    DESIGNATOR_INDEX(0x03),
    DESIGNATOR_MINIMUM(0x04),
    DESIGNATOR_MAXIMUM(0x05),
    STRING_INDEX(0x07),
    STRING_MINIMUM(0x08),
    STRING_MAXIMUM(0x09),
    DELIMITER(0x0A),
    UNKNOWN(-1)
}

/**
 * A parsed HID item from the descriptor.
 */
data class HidItem(
    val offset: Int,           // byte offset in descriptor
    val rawBytes: ByteArray,   // raw item bytes
    val type: HidItemType,
    val tag: Int,              // raw tag value
    val tagName: String,       // human-readable tag name
    val dataSize: Int,         // 0, 1, 2, or 4
    val dataValue: Long,       // parsed data value (sign-extended)
    val dataUnsigned: Long     // unsigned interpretation
) {
    override fun equals(other: Any?): Boolean = other is HidItem && offset == other.offset
    override fun hashCode(): Int = offset
}

/**
 * Collection nesting info.
 */
data class HidCollection(
    val type: Int,             // collection type (0=Physical, 1=Application, etc.)
    val typeName: String,
    val usagePage: Int,
    val usage: Int,
    val depth: Int,
    val items: MutableList<HidItem> = mutableListOf()
)

/**
 * Detected malformation in a descriptor.
 */
data class HidMalformation(
    val offset: Int,
    val severity: MalformationSeverity,
    val type: String,
    val description: String,
    val rawBytes: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean = other is HidMalformation && offset == other.offset && type == other.type
    override fun hashCode(): Int = 31 * offset + type.hashCode()
}

enum class MalformationSeverity {
    INFO,       // unusual but valid
    WARNING,    // likely unintended
    ERROR,      // violates spec
    CRITICAL    // known crash-inducing pattern
}

/**
 * Complete parse result for a HID descriptor.
 */
data class HidParseResult(
    val rawDescriptor: ByteArray,
    val items: List<HidItem>,
    val collections: List<HidCollection>,
    val malformations: List<HidMalformation>,
    val reportIds: Set<Int>,
    val usagePages: Set<Int>,
    val totalReportSizeBits: Int,
    val maxCollectionDepth: Int,
    val isWellFormed: Boolean,
    val parseErrors: List<String>,
    val summary: String
) {
    val hasCriticalMalformations: Boolean
        get() = malformations.any { it.severity == MalformationSeverity.CRITICAL }

    override fun equals(other: Any?) = other is HidParseResult && rawDescriptor.contentEquals(other.rawDescriptor)
    override fun hashCode() = rawDescriptor.contentHashCode()
}

/**
 * Parses USB HID Report Descriptors and detects malformations.
 *
 * Reference: USB HID Specification 1.11, Section 6.2.2
 *
 * This parser focuses on:
 * - Complete structure extraction
 * - Malformation detection (spec violations)
 * - Known crash-pattern identification
 * - Safe analysis without device interaction
 */
class HidDescriptorParser @Inject constructor() {

    companion object {
        // Known collection type names
        private val COLLECTION_TYPES = mapOf(
            0x00 to "Physical",
            0x01 to "Application",
            0x02 to "Logical",
            0x03 to "Report",
            0x04 to "Named Array",
            0x05 to "Usage Switch",
            0x06 to "Usage Modifier"
        )

        // Known usage page names
        val USAGE_PAGE_NAMES = mapOf(
            0x01 to "Generic Desktop",
            0x02 to "Simulation",
            0x03 to "VR",
            0x04 to "Sport",
            0x05 to "Game",
            0x06 to "Generic Device",
            0x07 to "Keyboard/Keypad",
            0x08 to "LED",
            0x09 to "Button",
            0x0A to "Ordinal",
            0x0B to "Telephony",
            0x0C to "Consumer",
            0x0D to "Digitizer",
            0x0F to "PID",
            0x10 to "Unicode",
            0x14 to "Alphanumeric Display",
            0x40 to "Medical",
            0x80 to "Monitor",
            0x84 to "Power",
            0xF1D0 to "FIDO Alliance",
            0xFF00 to "Vendor Defined"
        )

        // Known crash-inducing patterns (for detection only)
        private val KNOWN_CRASH_PATTERNS = listOf(
            KnownPattern(
                name = "excessive_nesting",
                description = "Collection nesting depth > 16 — known to crash some HID parsers",
                check = { _, collections, _ -> collections.any { it.depth > 16 } }
            ),
            KnownPattern(
                name = "huge_report_size",
                description = "Report size > 8192 bits — may cause buffer overflow in some drivers",
                check = { items, _, _ ->
                    items.filter { it.tagName == "REPORT_SIZE" }.any { it.dataUnsigned > 8192 }
                }
            ),
            KnownPattern(
                name = "huge_report_count",
                description = "Report count > 8192 — may cause resource exhaustion",
                check = { items, _, _ ->
                    items.filter { it.tagName == "REPORT_COUNT" }.any { it.dataUnsigned > 8192 }
                }
            ),
            KnownPattern(
                name = "mismatched_collections",
                description = "Collection open/close mismatch — undefined parser behavior",
                check = { _, _, result -> result.parseErrors.any { "collection" in it.lowercase() } }
            ),
            KnownPattern(
                name = "zero_report_size",
                description = "Report size = 0 with non-zero count — may cause division by zero",
                check = { items, _, _ ->
                    val sizes = items.filter { it.tagName == "REPORT_SIZE" }.map { it.dataUnsigned }
                    val counts = items.filter { it.tagName == "REPORT_COUNT" }.map { it.dataUnsigned }
                    sizes.any { it == 0L } && counts.any { it > 0 }
                }
            )
        )

        private data class KnownPattern(
            val name: String,
            val description: String,
            val check: (List<HidItem>, List<HidCollection>, HidParseResult) -> Boolean
        )
    }

    // ── Main Parse Entry Point ──────────────────────────────────

    /**
     * Parse a USB HID Report Descriptor.
     *
     * @param descriptor raw descriptor bytes
     * @return complete parse result with items, collections, malformations
     */
    fun parse(descriptor: ByteArray): HidParseResult {
        val items = mutableListOf<HidItem>()
        val collections = mutableListOf<HidCollection>()
        val malformations = mutableListOf<HidMalformation>()
        val parseErrors = mutableListOf<String>()
        val reportIds = mutableSetOf<Int>()
        val usagePages = mutableSetOf<Int>()

        var offset = 0
        var collectionDepth = 0
        var maxDepth = 0
        var currentUsagePage = 0
        var currentUsage = 0
        var totalReportBits = 0

        val collectionStack = mutableListOf<HidCollection>()

        while (offset < descriptor.size) {
            val startOffset = offset

            // Read item prefix byte
            val prefix = descriptor[offset].toInt() and 0xFF
            offset++

            // Check for long item (prefix = 0xFE)
            if (prefix == 0xFE) {
                if (offset + 2 > descriptor.size) {
                    parseErrors.add("Long item at offset $startOffset truncated")
                    malformations.add(HidMalformation(startOffset, MalformationSeverity.ERROR,
                        "truncated_long_item", "Long item extends beyond descriptor"))
                    break
                }
                val longSize = descriptor[offset].toInt() and 0xFF
                val longTag = descriptor[offset + 1].toInt() and 0xFF
                offset += 2 + longSize

                malformations.add(HidMalformation(startOffset, MalformationSeverity.WARNING,
                    "long_item", "Long item present (tag=$longTag, size=$longSize) — rare and poorly supported"))
                continue
            }

            // Short item: parse bSize, bType, bTag
            val bSize = when (prefix and 0x03) {
                0 -> 0; 1 -> 1; 2 -> 2; 3 -> 4; else -> 0
            }
            val bType = (prefix shr 2) and 0x03
            val bTag = (prefix shr 4) and 0x0F
            val itemType = HidItemType.entries.getOrNull(bType) ?: HidItemType.RESERVED

            // Read data bytes
            if (offset + bSize > descriptor.size) {
                parseErrors.add("Item at offset $startOffset truncated (needs $bSize bytes, ${descriptor.size - offset} available)")
                malformations.add(HidMalformation(startOffset, MalformationSeverity.ERROR,
                    "truncated_item", "Item data extends beyond descriptor"))
                break
            }

            val dataBytes = if (bSize > 0) descriptor.sliceArray(offset until offset + bSize) else ByteArray(0)
            val dataValue = parseSignedValue(dataBytes)
            val dataUnsigned = parseUnsignedValue(dataBytes)
            val rawItemBytes = descriptor.sliceArray(startOffset until offset + bSize)
            offset += bSize

            val tagName = resolveTagName(itemType, bTag)

            val item = HidItem(
                offset = startOffset,
                rawBytes = rawItemBytes,
                type = itemType,
                tag = bTag,
                tagName = tagName,
                dataSize = bSize,
                dataValue = dataValue,
                dataUnsigned = dataUnsigned
            )
            items.add(item)

            // Track global state
            when (itemType) {
                HidItemType.GLOBAL -> {
                    when (bTag) {
                        HidGlobalTag.USAGE_PAGE.code -> {
                            currentUsagePage = dataUnsigned.toInt()
                            usagePages.add(currentUsagePage)
                        }
                        HidGlobalTag.REPORT_ID.code -> reportIds.add(dataUnsigned.toInt())
                        HidGlobalTag.REPORT_SIZE.code -> {
                            // Track for total size calculation
                        }
                        HidGlobalTag.REPORT_COUNT.code -> {
                            // Track for total size calculation
                        }
                    }
                }
                HidItemType.LOCAL -> {
                    when (bTag) {
                        HidLocalTag.USAGE.code -> currentUsage = dataUnsigned.toInt()
                    }
                }
                HidItemType.MAIN -> {
                    when (bTag) {
                        HidMainTag.COLLECTION.code -> {
                            collectionDepth++
                            maxDepth = maxOf(maxDepth, collectionDepth)
                            val col = HidCollection(
                                type = dataUnsigned.toInt(),
                                typeName = COLLECTION_TYPES[dataUnsigned.toInt()] ?: "Reserved(0x${dataUnsigned.toString(16)})",
                                usagePage = currentUsagePage,
                                usage = currentUsage,
                                depth = collectionDepth
                            )
                            collectionStack.add(col)
                            collections.add(col)
                        }
                        HidMainTag.END_COLLECTION.code -> {
                            if (collectionDepth <= 0) {
                                malformations.add(HidMalformation(startOffset, MalformationSeverity.ERROR,
                                    "unmatched_end_collection", "END_COLLECTION without matching COLLECTION"))
                                parseErrors.add("Unmatched END_COLLECTION at offset $startOffset")
                            } else {
                                collectionDepth--
                                if (collectionStack.isNotEmpty()) collectionStack.removeAt(collectionStack.size - 1)
                            }
                        }
                        HidMainTag.INPUT.code, HidMainTag.OUTPUT.code, HidMainTag.FEATURE.code -> {
                            // Would track report size here
                        }
                    }
                }
                else -> { /* RESERVED */ }
            }

            // Add item to current collection
            collectionStack.lastOrNull()?.items?.add(item)
        }

        // Check for unclosed collections
        if (collectionDepth > 0) {
            malformations.add(HidMalformation(descriptor.size, MalformationSeverity.ERROR,
                "unclosed_collection", "$collectionDepth collection(s) not closed"))
            parseErrors.add("$collectionDepth unclosed collection(s)")
        }

        // Validate constraints
        validateConstraints(items, malformations)

        val result = HidParseResult(
            rawDescriptor = descriptor,
            items = items,
            collections = collections,
            malformations = malformations,
            reportIds = reportIds,
            usagePages = usagePages,
            totalReportSizeBits = totalReportBits,
            maxCollectionDepth = maxDepth,
            isWellFormed = malformations.none { it.severity >= MalformationSeverity.ERROR },
            parseErrors = parseErrors,
            summary = buildSummary(items, collections, malformations, reportIds, usagePages)
        )

        // Check known crash patterns
        for (pattern in KNOWN_CRASH_PATTERNS) {
            if (pattern.check(items, collections, result)) {
                malformations.add(HidMalformation(
                    0, MalformationSeverity.CRITICAL,
                    pattern.name, pattern.description
                ))
            }
        }

        Log.i(TAG, "Parsed descriptor: ${items.size} items, ${collections.size} collections, " +
                "${malformations.size} malformations")

        return result
    }

    // ── Value Parsing ───────────────────────────────────────────

    private fun parseSignedValue(data: ByteArray): Long {
        if (data.isEmpty()) return 0
        val buf = ByteBuffer.wrap(data.copyOf(8).also {
            // Sign extend
            val signBit = data.last().toInt() and 0x80
            if (signBit != 0) {
                for (i in data.size until 8) it[i] = 0xFF.toByte()
            }
        }).order(ByteOrder.LITTLE_ENDIAN)
        return buf.long
    }

    private fun parseUnsignedValue(data: ByteArray): Long {
        if (data.isEmpty()) return 0
        val buf = ByteBuffer.wrap(data.copyOf(8)).order(ByteOrder.LITTLE_ENDIAN)
        return buf.long and when (data.size) {
            1 -> 0xFFL
            2 -> 0xFFFFL
            4 -> 0xFFFFFFFFL
            else -> -1L // all bits set
        }
    }

    // ── Tag Resolution ──────────────────────────────────────────

    private fun resolveTagName(type: HidItemType, tag: Int): String = when (type) {
        HidItemType.MAIN -> HidMainTag.entries.find { it.code == tag }?.name ?: "MAIN_UNKNOWN($tag)"
        HidItemType.GLOBAL -> HidGlobalTag.entries.find { it.code == tag }?.name ?: "GLOBAL_UNKNOWN($tag)"
        HidItemType.LOCAL -> HidLocalTag.entries.find { it.code == tag }?.name ?: "LOCAL_UNKNOWN($tag)"
        HidItemType.RESERVED -> "RESERVED($tag)"
    }

    // ── Validation ──────────────────────────────────────────────

    private fun validateConstraints(items: List<HidItem>, malformations: MutableList<HidMalformation>) {
        // Check logical min > logical max
        var logMin: Long? = null
        var logMax: Long? = null

        for (item in items) {
            when (item.tagName) {
                "LOGICAL_MINIMUM" -> logMin = item.dataValue
                "LOGICAL_MAXIMUM" -> logMax = item.dataValue
                "INPUT", "OUTPUT", "FEATURE" -> {
                    if (logMin != null && logMax != null && logMin > logMax) {
                        malformations.add(HidMalformation(item.offset, MalformationSeverity.WARNING,
                            "logical_range_inverted", "Logical minimum ($logMin) > maximum ($logMax)"))
                    }
                }
            }
        }
    }

    // ── Summary ─────────────────────────────────────────────────

    private fun buildSummary(
        items: List<HidItem>,
        collections: List<HidCollection>,
        malformations: List<HidMalformation>,
        reportIds: Set<Int>,
        usagePages: Set<Int>
    ): String = buildString {
        appendLine("HID Descriptor Summary:")
        appendLine("  Items: ${items.size}")
        appendLine("  Collections: ${collections.size}")
        appendLine("  Report IDs: ${reportIds.ifEmpty { setOf(0) }}")
        appendLine("  Usage Pages: ${usagePages.joinToString { USAGE_PAGE_NAMES[it] ?: "0x${it.toString(16)}" }}")
        appendLine("  Malformations: ${malformations.size}")
        if (malformations.isNotEmpty()) {
            appendLine("  Severity breakdown:")
            malformations.groupBy { it.severity }.forEach { (sev, list) ->
                appendLine("    $sev: ${list.size}")
            }
        }
    }
}
