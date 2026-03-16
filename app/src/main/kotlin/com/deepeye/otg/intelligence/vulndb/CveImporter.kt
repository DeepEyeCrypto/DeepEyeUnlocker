package com.deepeye.otg.intelligence.vulndb

import org.json.JSONArray
import org.json.JSONObject

/**
 * Handles seeding and manual import of forensic CVE intelligence.
 */
class CveImporter(private val cveDao: CveDao) {

    suspend fun importSeedData() {
        val entries = buildSeedEntries()
        cveDao.upsertAll(entries)
    }

    private fun buildSeedEntries(): List<CveEntry> = listOf(
        CveEntry(
            cveId = "CVE-2024-43093",
            title = "Android Framework privilege escalation via path traversal",
            bugClass = BugClass.PathTraversal,
            component = "Android Framework (DocumentsUI)",
            affectedVersions = listOf("12", "12L", "13", "14", "15"),
            patchedInSpl = "2024-11-01",
            cvssScore = 7.8,
            cwe = "CWE-22",
            exploitedInWild = true,
            cisaKev = true,
            primitive = "Elevated read/write to Android/data, Android/obb, Android/sandbox subdirectories",
            detectionMethod = "Check SPL >= 2024-11-01 via ro.build.version.security_patch",
            mitigation = "Apply November 2024 Android security update",
            confidence = ConfidenceLevel.HIGH,
            sources = listOf("https://source.android.com/security/bulletin/2024-11-01"),
            notes = "Linked to commercial mobile spyware campaigns targeting journalists and activists."
        ),
        CveEntry(
            cveId = "CVE-2024-43047",
            title = "Qualcomm DSP kernel UAF enabling local privilege escalation",
            bugClass = BugClass.UAF,
            component = "Qualcomm DSP Kernel driver",
            affectedVersions = listOf("Android 12", "13", "14", "15"),
            patchedInSpl = "2024-11-01",
            cvssScore = 7.8,
            cwe = "CWE-416",
            exploitedInWild = true,
            cisaKev = true,
            primitive = "Local attacker (low priv) -> UAF in DSP kernel object -> arbitrary kernel read/write -> uid=0",
            detectionMethod = "Check ro.vendor.qti.security_patch >= 2024-10-01",
            mitigation = "Apply Nov 2024 SPL immediately",
            confidence = ConfidenceLevel.HIGH,
            sources = listOf("https://docs.qualcomm.com/product/publicresources/securitybulletin/october-2024-bulletin"),
            notes = "DeepEye relevance: HIGH. Direct interaction with Qualcomm DSP/modem partition area."
        ),
        CveEntry(
            cveId = "CVE-2024-53150",
            title = "Linux kernel USB/ALSA OOB read (Physical Access)",
            bugClass = BugClass.OOBRead,
            component = "Linux kernel — USB Audio / ALSA driver",
            affectedVersions = listOf("Linux kernel < 6.13", "Android 12, 13, 14"),
            patchedInSpl = "2025-03-01",
            cvssScore = 5.5,
            cwe = "CWE-125",
            exploitedInWild = true,
            cisaKev = true,
            primitive = "Physical USB attacker connects malicious USB audio device -> OOB read leaks kernel memory (KASLR defeat)",
            detectionMethod = "Check kernel version uname -r and March 2025 SPL",
            mitigation = "Apply March 2025 SPL; disable USB audio auto-enumeration",
            confidence = ConfidenceLevel.HIGH,
            sources = listOf("https://source.android.com/security/bulletin/2025-03-01"),
            notes = "Used by forensic extraction tooling against locked Android devices via USB."
        ),
        CveEntry(
            cveId = "CVE-2024-53197",
            title = "Linux kernel USB/ALSA OOB write enabling privilege escalation",
            bugClass = BugClass.OOBWrite,
            component = "Linux kernel — USB Audio / ALSA driver",
            affectedVersions = listOf("Linux kernel < 6.13", "Android 12, 13, 14"),
            patchedInSpl = "2025-03-01",
            cvssScore = 7.8,
            cwe = "CWE-787",
            exploitedInWild = true,
            cisaKev = true,
            primitive = "Physical USB connection -> OOB write achieves kernel code execution -> uid=0",
            detectionMethod = "Check SPL >= 2025-03-01",
            mitigation = "Apply March 2025 SPL",
            confidence = ConfidenceLevel.HIGH,
            sources = listOf("https://gridinsoft.com/blogs/two-android-zero-day-vulnerabilities"),
            notes = "Chain Stage 2 following CVE-2024-53150 KASLR defeat."
        ),
        CveEntry(
            cveId = "CVE-2025-21043",
            title = "Samsung Quram image codec heap overflow",
            bugClass = BugClass.HeapOverflow,
            component = "Samsung Quram Image Codec (libskia-quram.so)",
            affectedVersions = listOf("One UI 5.x, 6.x, 7.x"),
            patchedInSpl = "2025-09-01",
            cvssScore = 8.8,
            cwe = "CWE-122",
            exploitedInWild = true,
            cisaKev = false,
            primitive = "Crafted image file -> heap overflow in Quram codec -> RCE in media processing",
            detectionMethod = "Check Samsung SMR level >= 2025-09-01",
            mitigation = "Apply Samsung September 2025 SMR update",
            confidence = ConfidenceLevel.HIGH,
            sources = listOf("Samsung Security Bulletin September 2025"),
            notes = "Actively exploited targeting Samsung devices in specific markets."
        ),
        CveEntry(
            cveId = "CVE-2025-48572",
            title = "Android Framework privilege escalation (Zero-Day Chain)",
            bugClass = BugClass.ImproperAccessControl,
            component = "Android Framework (core services layer)",
            affectedVersions = listOf("Android 13, 14, 15, 16"),
            patchedInSpl = "2025-12-01",
            cvssScore = 8.8,
            cwe = "CWE-269",
            exploitedInWild = true,
            cisaKev = true,
            primitive = "No-interaction RCE chain -> escalate to system privileges",
            detectionMethod = "Check December 2025 SPL via ro.build.version.security_patch",
            mitigation = "Apply December 2025 SPL immediately",
            confidence = ConfidenceLevel.HIGH,
            sources = listOf("https://source.android.com/security/bulletin/2025-12-01"),
            notes = "Exploited in targeted espionage campaigns; combined with CVE-2025-48633."
        ),
        CveEntry(
            cveId = "MTK-JAN-2025",
            title = "MediaTek Chipset vulnerabilities (Jan 2025 Batch)",
            bugClass = BugClass.OOBWrite,
            component = "MediaTek Modem / DA Firmware / Preloader",
            affectedVersions = listOf("MT6739, MT6761, MT6768, MT6833, MT6893"),
            patchedInSpl = "2025-01-01",
            cvssScore = 9.8,
            cwe = "CWE-121",
            exploitedInWild = null,
            cisaKev = false,
            primitive = "DA firmware OOB -> preloader bypass; Modem stack overflow -> baseband code execution",
            detectionMethod = "Check ro.vendor.mediatek.security_patch vs Jan 2025 MTK bulletin",
            mitigation = "Apply OEM update containing Jan 2025 MTK fixes",
            confidence = ConfidenceLevel.MEDIUM,
            sources = listOf("https://corp.mediatek.com/product-security-bulletin/January-2025"),
            notes = "Critical for MTK Engine. Assume unpatched if vendor SPL < 2025-01-01."
        )
    )

    /**
     * Import CVE entries from a raw JSON string.
     */
    suspend fun importFromJson(json: String): ImportResult {
        return try {
            val array = JSONArray(json)
            val entries = mutableListOf<CveEntry>()
            
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                // Minimal mapping logic
                entries.add(CveEntry(
                    cveId = obj.getString("cve_id"),
                    title = obj.optString("title", ""),
                    bugClass = runCatching { BugClass.valueOf(obj.optString("bug_class", "UNKNOWN")) }.getOrDefault(BugClass.UNKNOWN),
                    component = obj.optString("component", "Unknown"),
                    affectedVersions = emptyList(), // Simplified for now
                    patchedInSpl = obj.optString("patched_in_spl"),
                    cvssScore = obj.optDouble("cvss_score", 0.0),
                    cwe = obj.optString("cwe", "UNKNOWN")
                ))
            }
            
            cveDao.upsertAll(entries)
            ImportResult(success = true, totalProcessed = array.length(), addedCount = entries.size)
        } catch (e: Exception) {
            ImportResult(success = false, error = e.message)
        }
    }
}
