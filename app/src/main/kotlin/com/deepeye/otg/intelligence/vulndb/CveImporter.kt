package com.deepeye.otg.intelligence.vulndb

// Avoid android.util.Log for JVM tests; use LogSafe.

// ──────────────────────────────────────────────────────────────
// CVE Importer — JSON Import Pipeline
// DeepEye OTG — CVE Intelligence Module (Part 1)
// ──────────────────────────────────────────────────────────────

private const val TAG = "CveImporter"

/**
 * Import result for a batch CVE import operation.
 */
data class ImportResult(
    val totalParsed: Int,
    val inserted: Int,
    val updated: Int,
    val errors: Int,
    val errorMessages: List<String> = emptyList(),
    val importedAt: Long = System.currentTimeMillis()
)

/**
 * Imports CVE data into the local database.
 *
 * Supports:
 * - Manual entry construction
 * - Batch upsert with conflict resolution
 * - Validation before insert
 * - Import statistics
 *
 * In production, extend with JSON file parsing using org.json or
 * kotlinx.serialization.
 */
class CveImporter(private val cveDao: CveDao) {

    /**
     * Import a list of CVE entries with validation.
     */
    suspend fun importEntries(entries: List<CveEntry>): ImportResult {
        LogSafe.i(TAG, "Importing ${entries.size} CVE entries")

        val errors = mutableListOf<String>()
        val validated = mutableListOf<CveEntry>()

        for (entry in entries) {
            val validationError = validate(entry)
            if (validationError != null) {
                errors.add("${entry.cveId}: $validationError")
                LogSafe.w(TAG, "Validation failed for ${entry.cveId}: $validationError")
            } else {
                validated.add(entry.copy(updatedAt = System.currentTimeMillis()))
            }
        }

        // Check which entries already exist
        val existing = validated.mapNotNull { cveDao.getById(it.cveId) }
        val existingIds = existing.map { it.cveId }.toSet()

        val newEntries = validated.filter { it.cveId !in existingIds }
        val updatedEntries = validated.filter { it.cveId in existingIds }

        // Upsert all
        cveDao.upsertAll(validated)

        val result = ImportResult(
            totalParsed = entries.size,
            inserted = newEntries.size,
            updated = updatedEntries.size,
            errors = errors.size,
            errorMessages = errors
        )

        LogSafe.i(TAG, "Import complete: ${result.inserted} new, ${result.updated} updated, ${result.errors} errors")
        return result
    }

    /**
     * Parse a JSON array string and import entries.
     * Uses org.json for zero-dependency parsing in Android.
     */
    suspend fun importFromJson(jsonString: String): ImportResult {
        val entries = mutableListOf<CveEntry>()
        val parseErrors = mutableListOf<String>()

        try {
            val arr = org.json.JSONArray(jsonString)
            for (i in 0 until arr.length()) {
                try {
                    val obj = arr.getJSONObject(i)
                    entries.add(parseEntry(obj))
                } catch (e: Exception) {
                    parseErrors.add("Parse error at index $i: ${e.message}")
                }
            }
        } catch (e: Exception) {
            return ImportResult(0, 0, 0, 1, listOf("Root parse error: ${e.message}"))
        }

        val result = importEntries(entries)
        return result.copy(
            errors = result.errors + parseErrors.size,
            errorMessages = result.errorMessages + parseErrors
        )
    }

    /**
     * Parse a single CveEntry from a JSONObject.
     */
    private fun parseEntry(obj: org.json.JSONObject): CveEntry {
        return CveEntry(
            cveId = obj.getString("cveId"),
            component = obj.getString("component"),
            vulnerabilityType = VulnerabilityType.valueOf(obj.optString("vulnerabilityType", "UNKNOWN")),
            affectedVersions = jsonArrayToList(obj.optJSONArray("affectedVersions")),
            fixedVersions = jsonArrayToList(obj.optJSONArray("fixedVersions")),
            fixedComponentBuild = obj.optString("fixedComponentBuild").takeIf { it.isNotBlank() },
            exploitationStatus = ExploitationStatus.valueOf(obj.optString("exploitationStatus", "UNKNOWN")),
            cvssScore = if (obj.has("cvssScore")) obj.getDouble("cvssScore") else null,
            confidence = ConfidenceLevel.valueOf(obj.optString("confidence", "UNVERIFIED")),
            sourceReferences = jsonArrayToList(obj.optJSONArray("sourceReferences")),
            summary = obj.optString("summary"),
            cweId = obj.optString("cweId").takeIf { it.isNotBlank() },
            notes = obj.optString("notes"),
            reviewed = obj.optBoolean("reviewed", false)
        )
    }

    private fun jsonArrayToList(arr: org.json.JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            list.add(arr.getString(i))
        }
        return list
    }

    /**
     * Import the built-in iOS 26.x seed data for initial research.
     */
    suspend fun importSeedData(): ImportResult {
        val seedEntries = buildSeedEntries()
        return importEntries(seedEntries)
    }

    /**
     * Validate a CVE entry before import.
     *
     * @return error message if invalid, null if valid
     */
    private fun validate(entry: CveEntry): String? {
        if (!entry.cveId.matches(Regex("^CVE-\\d{4}-\\d{4,}$"))) {
            return "Invalid CVE ID format (expected CVE-YYYY-NNNNN)"
        }
        if (entry.component.isBlank()) {
            return "Component must not be blank"
        }
        if (entry.cvssScore != null && (entry.cvssScore < 0.0 || entry.cvssScore > 10.0)) {
            return "CVSS score must be between 0.0 and 10.0"
        }
        return null
    }

    // ── Seed Data for iOS 26.x Research ─────────────────────────

    /**
     * Build representative seed CVE entries for iOS 26.0–26.3.
     *
     * These are SYNTHETIC entries for testing the analysis pipeline.
     * In production, replace with real CVE data from:
     * - Apple Security Advisories
     * - NVD / NIST
     * - CISA KEV
     * - Project Zero
     */
    private fun buildSeedEntries(): List<CveEntry> = listOf(
        // ── WebKit CVEs ──
        CveEntry(
            cveId = "CVE-2026-10001",
            component = "WebKit",
            vulnerabilityType = VulnerabilityType.USE_AFTER_FREE,
            affectedVersions = listOf("26.0", "26.1", "26.2"),
            fixedVersions = listOf("26.3"),
            fixedComponentBuild = "618.1.15",
            exploitationStatus = ExploitationStatus.ACTIVE_EXPLOITATION,
            cvssScore = 8.8,
            confidence = ConfidenceLevel.CONFIRMED,
            sourceReferences = listOf(
                "https://support.apple.com/en-us/HT214XXX",
                "https://nvd.nist.gov/vuln/detail/CVE-2026-10001"
            ),
            summary = "WebKit UAF in DOM tree mutation observer — processing maliciously crafted web content may lead to arbitrary code execution",
            cweId = "CWE-416",
            notes = "Seed data for pipeline testing. Simulates a WebKit UAF patched in 26.3.",
            reviewed = true
        ),

        CveEntry(
            cveId = "CVE-2026-10002",
            component = "WebKit",
            vulnerabilityType = VulnerabilityType.TYPE_CONFUSION,
            affectedVersions = listOf("26.0", "26.1"),
            fixedVersions = listOf("26.2"),
            fixedComponentBuild = "618.1.10",
            exploitationStatus = ExploitationStatus.POC_AVAILABLE,
            cvssScore = 7.5,
            confidence = ConfidenceLevel.HIGH,
            sourceReferences = listOf(
                "https://support.apple.com/en-us/HT214YYY"
            ),
            summary = "WebKit type confusion in JIT compilation — may allow sandbox escape via crafted JavaScript",
            cweId = "CWE-843",
            notes = "Seed data. Simulates a JIT type confusion fixed in 26.2."
        ),

        // ── Kernel CVEs ──
        CveEntry(
            cveId = "CVE-2026-10003",
            component = "Kernel",
            vulnerabilityType = VulnerabilityType.PRIVILEGE_ESCALATION,
            affectedVersions = listOf("26.0", "26.1", "26.2"),
            fixedVersions = listOf("26.3"),
            fixedComponentBuild = "10000.30.1",
            exploitationStatus = ExploitationStatus.ACTIVE_EXPLOITATION,
            cvssScore = 9.3,
            confidence = ConfidenceLevel.CONFIRMED,
            sourceReferences = listOf(
                "https://support.apple.com/en-us/HT214ZZZ",
                "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
            ),
            summary = "Kernel LPE via IOKit driver race condition — an application may gain root privileges",
            cweId = "CWE-362",
            notes = "Seed data. Simulates a kernel LPE in CISA KEV, fixed in 26.3.",
            reviewed = true
        ),

        CveEntry(
            cveId = "CVE-2026-10004",
            component = "Kernel",
            vulnerabilityType = VulnerabilityType.INFORMATION_DISCLOSURE,
            affectedVersions = listOf("26.0"),
            fixedVersions = listOf("26.1"),
            fixedComponentBuild = "10000.10.1",
            exploitationStatus = ExploitationStatus.NONE,
            cvssScore = 5.5,
            confidence = ConfidenceLevel.CONFIRMED,
            summary = "Kernel info leak via mach port — application may read restricted process memory",
            cweId = "CWE-200",
            notes = "Seed data. Low-severity info leak fixed early."
        ),

        // ── dyld CVEs ──
        CveEntry(
            cveId = "CVE-2026-10005",
            component = "dyld",
            vulnerabilityType = VulnerabilityType.USE_AFTER_FREE,
            affectedVersions = listOf("26.0", "26.1"),
            fixedVersions = listOf("26.2"),
            fixedComponentBuild = "1200.1.0",
            exploitationStatus = ExploitationStatus.POC_AVAILABLE,
            cvssScore = 7.8,
            confidence = ConfidenceLevel.HIGH,
            summary = "dyld UAF in shared cache loading — may allow code execution prior to AMFI enforcement",
            cweId = "CWE-416",
            notes = "Seed data. dyld UAF useful for chain stage 2."
        ),

        // ── IOKit CVEs ──
        CveEntry(
            cveId = "CVE-2026-10006",
            component = "IOKit",
            vulnerabilityType = VulnerabilityType.BUFFER_OVERFLOW,
            affectedVersions = listOf("26.0", "26.1", "26.2", "26.3"),
            fixedVersions = emptyList(), // Not yet patched
            exploitationStatus = ExploitationStatus.UNKNOWN,
            cvssScore = 6.7,
            confidence = ConfidenceLevel.MEDIUM,
            summary = "IOKit USB HID driver heap overflow — processing a malformed HID descriptor may corrupt kernel heap",
            cweId = "CWE-122",
            notes = "Seed data. Simulates an unpatched IOKit bug across all 26.x versions."
        ),

        // ── Safari CVEs ──
        CveEntry(
            cveId = "CVE-2026-10007",
            component = "Safari",
            vulnerabilityType = VulnerabilityType.SANDBOX_ESCAPE,
            affectedVersions = listOf("26.0", "26.1"),
            fixedVersions = listOf("26.2"),
            exploitationStatus = ExploitationStatus.NONE,
            cvssScore = 6.5,
            confidence = ConfidenceLevel.MEDIUM,
            summary = "Safari sandbox escape via IPC message confusion — may allow file system access outside sandbox",
            cweId = "CWE-269",
            notes = "Seed data. Sandbox escape fixed in 26.2."
        ),

        // ── AppleMobileFileIntegrity CVEs ──
        CveEntry(
            cveId = "CVE-2026-10008",
            component = "AMFI",
            vulnerabilityType = VulnerabilityType.LOGIC_ERROR,
            affectedVersions = listOf("26.0"),
            fixedVersions = listOf("26.1"),
            fixedComponentBuild = null,
            exploitationStatus = ExploitationStatus.NONE,
            cvssScore = 7.0,
            confidence = ConfidenceLevel.HIGH,
            summary = "AMFI symlink validation bypass — may allow loading unsigned code in specific conditions",
            cweId = "CWE-59",
            notes = "Seed data. AMFI logic error fixed early in 26.1."
        )
    )
}
