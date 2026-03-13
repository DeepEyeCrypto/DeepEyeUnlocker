package com.deepeye.otg.intelligence.vulndb

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

// ──────────────────────────────────────────────────────────────
// CVE Entry — Room Entity + Domain Model
// DeepEye OTG — CVE Intelligence Module (Part 1)
// ──────────────────────────────────────────────────────────────

/**
 * Exploitation status of a CVE in the wild.
 */
enum class ExploitationStatus {
    /** No known exploitation */
    NONE,
    /** Proof-of-concept exists (public or private) */
    POC_AVAILABLE,
    /** Actively exploited in the wild (per CISA KEV, vendor advisory, etc.) */
    ACTIVE_EXPLOITATION,
    /** Exploitation attempted but not confirmed successful */
    ATTEMPTED,
    /** Unknown / not yet assessed */
    UNKNOWN
}

/**
 * Confidence level for a CVE → component/version mapping.
 *
 * - [CONFIRMED]: vendor advisory explicitly lists the mapping
 * - [HIGH]: multiple credible sources agree
 * - [MEDIUM]: inferred from patch diffs or changelogs
 * - [LOW]: community speculation or single unverified source
 * - [UNVERIFIED]: no analyst review yet
 */
enum class ConfidenceLevel {
    CONFIRMED,
    HIGH,
    MEDIUM,
    LOW,
    UNVERIFIED
}

/**
 * Type of vulnerability (high-level bug class).
 */
enum class VulnerabilityType {
    MEMORY_CORRUPTION,
    USE_AFTER_FREE,
    TYPE_CONFUSION,
    BUFFER_OVERFLOW,
    INTEGER_OVERFLOW,
    OUT_OF_BOUNDS_READ,
    OUT_OF_BOUNDS_WRITE,
    RACE_CONDITION,
    LOGIC_ERROR,
    INFORMATION_DISCLOSURE,
    PRIVILEGE_ESCALATION,
    SANDBOX_ESCAPE,
    CODE_EXECUTION,
    DENIAL_OF_SERVICE,
    INPUT_VALIDATION,
    AUTHENTICATION_BYPASS,
    OTHER,
    UNKNOWN
}

/**
 * Source provenance for a CVE entry.
 * Tracks where the information came from and when it was last verified.
 */
data class SourceProvenance(
    val sourceUrl: String,
    val sourceName: String,          // e.g. "Apple Security Advisory HT214120"
    val retrievedAt: Long,            // epoch millis
    val analystVerified: Boolean = false,
    val analystNotes: String = ""
)

/**
 * Represents a single CVE entry with full metadata for iOS security research.
 *
 * Design:
 * - Room Entity for local persistence
 * - Separates OS-level version tracking from component-level build tracking
 * - Supports silent/background component updates (e.g. WebKit via App Store)
 * - Stores source provenance for audit trail
 * - JSON-list fields stored as delimited strings (Room TypeConverters)
 */
@Entity(tableName = "cve_entries")
@TypeConverters(CveTypeConverters::class)
data class CveEntry(
    /** CVE identifier, e.g. "CVE-2026-12345" */
    @PrimaryKey
    val cveId: String,

    /** Affected iOS component, e.g. "WebKit", "Kernel", "dyld", "IOKit" */
    val component: String,

    /** High-level bug class */
    val vulnerabilityType: VulnerabilityType = VulnerabilityType.UNKNOWN,

    /** iOS versions where this CVE is present, e.g. ["26.0", "26.1", "26.2"] */
    val affectedVersions: List<String> = emptyList(),

    /** iOS versions where this CVE is fixed, e.g. ["26.3"] */
    val fixedVersions: List<String> = emptyList(),

    /**
     * Component-level build string where the fix landed.
     * Useful for silent WebKit updates — the OS version may not change
     * but the component build does.
     * e.g. "618.1.15.10.5" for a Safari/WebKit build.
     */
    val fixedComponentBuild: String? = null,

    /** Exploitation status in the wild */
    val exploitationStatus: ExploitationStatus = ExploitationStatus.UNKNOWN,

    /** CVSS v3.1 base score, 0.0–10.0 */
    val cvssScore: Double? = null,

    /** Analyst-assigned confidence for this mapping */
    val confidence: ConfidenceLevel = ConfidenceLevel.UNVERIFIED,

    /** Source references (URLs, advisory IDs) — stored as JSON string */
    val sourceReferences: List<String> = emptyList(),

    /** Source provenance metadata — serialized */
    val provenance: SourceProvenance? = null,

    /** Related CVE IDs (e.g. same root cause, variant chain) */
    val relatedCves: List<String> = emptyList(),

    /** Free-form analyst notes */
    val notes: String = "",

    /** CWE ID if available, e.g. "CWE-416" */
    val cweId: String? = null,

    /** Short human-readable summary of the vulnerability */
    val summary: String = "",

    /** When this entry was first imported (epoch millis) */
    val importedAt: Long = System.currentTimeMillis(),

    /** When this entry was last updated (epoch millis) */
    val updatedAt: Long = System.currentTimeMillis(),

    /** Whether this entry has been manually reviewed by an analyst */
    val reviewed: Boolean = false
)

// ──────────────────────────────────────────────────────────────
// Room TypeConverters
// ──────────────────────────────────────────────────────────────

class CveTypeConverters {

    private val listDelimiter = "|||"

    // ── List<String> ↔ String ──

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        val sanitized = list
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        if (sanitized.isEmpty()) return ""

        // Persist sentinel delimiters at both ends so SQL lookups can match
        // whole items instead of partial substrings (e.g. 26.1 vs 26.10).
        return sanitized.joinToString(
            separator = listDelimiter,
            prefix = listDelimiter,
            postfix = listDelimiter
        )
    }

    @TypeConverter
    fun toStringList(data: String?): List<String> =
        if (data.isNullOrBlank()) emptyList()
        else data.split(listDelimiter)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    // ── VulnerabilityType ↔ String ──

    @TypeConverter
    fun fromVulnType(type: VulnerabilityType): String = type.name

    @TypeConverter
    fun toVulnType(name: String): VulnerabilityType =
        runCatching { VulnerabilityType.valueOf(name) }
            .getOrDefault(VulnerabilityType.UNKNOWN)

    // ── ExploitationStatus ↔ String ──

    @TypeConverter
    fun fromExploitStatus(status: ExploitationStatus): String = status.name

    @TypeConverter
    fun toExploitStatus(name: String): ExploitationStatus =
        runCatching { ExploitationStatus.valueOf(name) }
            .getOrDefault(ExploitationStatus.UNKNOWN)

    // ── ConfidenceLevel ↔ String ──

    @TypeConverter
    fun fromConfidence(level: ConfidenceLevel): String = level.name

    @TypeConverter
    fun toConfidence(name: String): ConfidenceLevel =
        runCatching { ConfidenceLevel.valueOf(name) }
            .getOrDefault(ConfidenceLevel.UNVERIFIED)

    // ── SourceProvenance ↔ String (simple pipe-delimited) ──

    @TypeConverter
    fun fromProvenance(p: SourceProvenance?): String? {
        if (p == null) return null
        return listOf(
            p.sourceUrl, p.sourceName, p.retrievedAt.toString(),
            p.analystVerified.toString(), p.analystNotes
        ).joinToString(listDelimiter)
    }

    @TypeConverter
    fun toProvenance(data: String?): SourceProvenance? {
        if (data.isNullOrBlank()) return null
        val parts = data.split(listDelimiter, limit = 5)
        if (parts.size < 5) return null
        return SourceProvenance(
            sourceUrl = parts[0],
            sourceName = parts[1],
            retrievedAt = parts[2].toLongOrNull() ?: 0L,
            analystVerified = parts[3].toBooleanStrictOrNull() ?: false,
            analystNotes = parts[4]
        )
    }
}
