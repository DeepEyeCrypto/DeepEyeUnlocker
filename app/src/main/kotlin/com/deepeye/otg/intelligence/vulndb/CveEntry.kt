package com.deepeye.otg.intelligence.vulndb

import androidx.room.*

// ──────────────────────────────────────────────────────────────
// CVE Intelligence Models
// DeepEye OTG — Universal Security Research
// ──────────────────────────────────────────────────────────────

enum class PatchState { PATCHED, UNPATCHED, PARTIAL, UNKNOWN }
enum class ConfidenceLevel { CONFIRMED, HIGH, MEDIUM, LOW, UNKNOWN }
enum class RiskLevel { CRITICAL, HIGH, MEDIUM, LOW, SAFE }
enum class SplStatus { VULNERABLE, PROTECTED, OUTDATED, UNKNOWN }

enum class BugClass {
    UAF, HeapOverflow, IntegerOverflow, OOBRead, OOBWrite,
    TypeConfusion, LogicFlaw, RaceCondition, NullDeref,
    PathTraversal, ImproperAccessControl, BootromVuln,
    UNKNOWN
}

/**
 * Represents a single CVE entry with actionable intelligence metadata.
 */
@Entity(tableName = "cve_entries")
@TypeConverters(CveTypeConverters::class)
data class CveEntry(
    @PrimaryKey
    val cveId: String,
    val title: String,
    val bugClass: BugClass,
    val component: String,
    val affectedVersions: List<String>,
    val patchedInSpl: String?,       // "2024-11-01"
    val patchState: PatchState = PatchState.UNKNOWN,
    val silentPatch: Boolean = false,
    val cvssScore: Double?,
    val cwe: String,
    val exploitedInWild: Boolean? = null,
    val cisaKev: Boolean = false,
    val primitive: String = "",
    val detectionMethod: String = "",
    val mitigation: String = "",
    val confidence: ConfidenceLevel = ConfidenceLevel.UNKNOWN,
    val sources: List<String> = emptyList(),
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Result of a CVE import operation.
 */
data class ImportResult(
    val success: Boolean,
    val totalProcessed: Int = 0,
    val addedCount: Int = 0,
    val error: String? = null
)

/**
 * Observed device state for vulnerability analysis.
 */
data class DeviceObservation(
    val brand: String,
    val model: String,
    val androidSpl: String = "",
    val qtiSpl: String? = null,
    val mtkSpl: String? = null,
    val kernelVersion: String = "",
    val iosVersion: String = "",
    val deviceId: String = "",
    val iosBuildId: String? = null,
    val observedComponents: List<ObservedComponentVersion> = emptyList()
)

data class ObservedComponentVersion(
    val component: String,
    val buildVersion: String?
)

/**
 * Direct alias or wrapper for DevicePatchReport for consistency.
 */
typealias ExposureReport = DevicePatchReport

// ── Chain & Analysis Models ───────────────────────────────────

data class ChainStage(
    val stageNum: Int,
    val cveId: String,
    val component: String,
    val bugClass: BugClass,
    val primitive: String,
    val patchedInSpl: String,
    val detectableSignals: List<String>,
    val notes: String
)

data class ExploitChain(
    val chainId: String,
    val target: String,
    val stages: List<ChainStage>,
    val entryPoint: String,
    val preConditions: List<String>,
    val limitations: List<String>,
    val detectionSurface: List<String>,
    val patchStatus: String,
    val confidence: ConfidenceLevel
)

data class DevicePatchReport(
    val deviceModel: String,
    val brand: String,
    val androidSpl: String,
    val vendorQtiSpl: String?,
    val vendorMtkSpl: String?,
    val kernelVersion: String,
    val exposedCves: List<CveEntry>,
    val patchedCves: List<CveEntry>,
    val unknownCves: List<CveEntry>,
    val reportTimestamp: Long,
    val overallRiskLevel: RiskLevel,
    val androidSplStatus: SplStatus = SplStatus.UNKNOWN
)

/**
 * Map of version -> component for iOS vulnerability mapping.
 */
data class VersionComponentMapping(
    val iosVersion: String,
    val component: String,
    val expectedBuild: String,
    val confidence: ConfidenceLevel
)

// ── Room TypeConverters ───────────────────────────────────────

class CveTypeConverters {
    private val listDelimiter = "|||"

    @TypeConverter
    fun fromStringList(list: List<String>?): String = list?.joinToString(listDelimiter) ?: ""

    @TypeConverter
    fun toStringList(data: String?): List<String> = data?.split(listDelimiter)?.filter { it.isNotBlank() } ?: emptyList()

    @TypeConverter
    fun fromBugClass(v: BugClass): String = v.name

    @TypeConverter
    fun toBugClass(v: String): BugClass = runCatching { BugClass.valueOf(v) }.getOrDefault(BugClass.UNKNOWN)

    @TypeConverter
    fun fromPatchState(v: PatchState): String = v.name

    @TypeConverter
    fun toPatchState(v: String): PatchState = runCatching { PatchState.valueOf(v) }.getOrDefault(PatchState.UNKNOWN)

    @TypeConverter
    fun fromConfidence(v: ConfidenceLevel): String = v.name

    @TypeConverter
    fun toConfidence(v: String): ConfidenceLevel = runCatching { ConfidenceLevel.valueOf(v) }.getOrDefault(ConfidenceLevel.UNKNOWN)

    @TypeConverter
    fun fromRiskLevel(v: RiskLevel): String = v.name

    @TypeConverter
    fun toRiskLevel(v: String): RiskLevel = runCatching { RiskLevel.valueOf(v) }.getOrDefault(RiskLevel.LOW)
}
