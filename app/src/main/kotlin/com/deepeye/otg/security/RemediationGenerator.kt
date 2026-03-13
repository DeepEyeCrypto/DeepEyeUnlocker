package com.deepeye.otg.security

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ──────────────────────────────────────────────────────────────
// Remediation Generator — Guidance + Report Export
// DeepEye OTG — Detection + Hardening Module (Part 7)
// ──────────────────────────────────────────────────────────────

/**
 * Generates actionable remediation guidance from security findings
 * and exports as structured reports.
 */
class RemediationGenerator(private val outputDir: File) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US)

    init {
        outputDir.mkdirs()
    }

    /**
     * Remediation plan item.
     */
    data class RemediationItem(
        val priority: Int,              // 1 = highest
        val findingId: String,
        val title: String,
        val severity: FindingSeverity,
        val action: String,             // specific remediation step
        val effort: RemediationEffort,
        val category: FindingCategory,
        val relatedCves: List<String>,
        val estimatedImpact: String     // what this fix addresses
    )

    /**
     * Complete remediation plan.
     */
    data class RemediationPlan(
        val deviceId: String,
        val items: List<RemediationItem>,
        val score: SecurityScore,
        val quickWins: List<RemediationItem>,   // easy + high impact
        val generatedAt: Long = System.currentTimeMillis()
    )

    /**
     * Generate a prioritized remediation plan from findings.
     */
    fun generatePlan(
        findings: List<Finding>,
        score: SecurityScore,
        deviceId: String = "unknown"
    ): RemediationPlan {
        // Convert findings to remediation items, sorted by priority
        val items = findings
            .filter { it.severity != FindingSeverity.INFO }
            .sortedWith(
                compareBy<Finding> { it.severity.ordinal }
                    .thenBy { it.remediationEffort.ordinal }
            )
            .mapIndexed { idx, finding ->
                RemediationItem(
                    priority = idx + 1,
                    findingId = finding.id,
                    title = finding.title,
                    severity = finding.severity,
                    action = finding.remediation.ifBlank {
                        generateDefaultRemediation(finding)
                    },
                    effort = finding.remediationEffort,
                    category = finding.category,
                    relatedCves = finding.relatedCves,
                    estimatedImpact = estimateImpact(finding, score)
                )
            }

        // Quick wins: easy effort + at least medium severity
        val quickWins = items.filter {
            it.effort in listOf(RemediationEffort.TRIVIAL, RemediationEffort.EASY) &&
                    it.severity in listOf(FindingSeverity.CRITICAL, FindingSeverity.HIGH, FindingSeverity.MEDIUM)
        }

        return RemediationPlan(
            deviceId = deviceId,
            items = items,
            score = score,
            quickWins = quickWins
        )
    }

    /**
     * Export remediation plan as JSON.
     */
    fun exportJson(plan: RemediationPlan): File {
        val root = JSONObject().apply {
            put("format_version", "1.0")
            put("generator", "DeepEye OTG Security")
            put("generated_at", dateFormat.format(Date()))
            put("device_id", plan.deviceId)

            put("security_score", JSONObject().apply {
                put("overall", plan.score.overallScore)
                put("grade", plan.score.grade.name)
                put("critical", plan.score.criticalFindings)
                put("high", plan.score.highFindings)
                put("medium", plan.score.mediumFindings)
                put("low", plan.score.lowFindings)
                put("total", plan.score.totalFindings)
            })

            val itemsArray = JSONArray()
            plan.items.forEach { item ->
                itemsArray.put(JSONObject().apply {
                    put("priority", item.priority)
                    put("finding_id", item.findingId)
                    put("title", item.title)
                    put("severity", item.severity.name)
                    put("action", item.action)
                    put("effort", item.effort.name)
                    put("category", item.category.name)
                    put("impact", item.estimatedImpact)
                    if (item.relatedCves.isNotEmpty()) {
                        put("related_cves", JSONArray(item.relatedCves))
                    }
                })
            }
            put("remediation_items", itemsArray)

            val quickWinsArray = JSONArray()
            plan.quickWins.forEach { qw ->
                quickWinsArray.put(JSONObject().apply {
                    put("priority", qw.priority)
                    put("title", qw.title)
                    put("action", qw.action)
                })
            }
            put("quick_wins", quickWinsArray)
        }

        val outFile = File(outputDir, "remediation_${plan.deviceId}_${System.currentTimeMillis()}.json")
        FileWriter(outFile).use { it.write(root.toString(2)) }
        return outFile
    }

    /**
     * Export as human-readable text report.
     */
    fun exportText(plan: RemediationPlan): File {
        val text = buildString {
            appendLine("═══════════════════════════════════════════════════════════")
            appendLine("  DEEPEYE OTG — SECURITY REMEDIATION PLAN")
            appendLine("  Device: ${plan.deviceId}")
            appendLine("  Score: ${plan.score.overallScore}/10.0 (${plan.score.grade})")
            appendLine("  Generated: ${dateFormat.format(Date())}")
            appendLine("═══════════════════════════════════════════════════════════")
            appendLine()

            appendLine("─── EXECUTIVE SUMMARY ─────────────────────────────────────")
            appendLine("Findings: ${plan.score.totalFindings}")
            appendLine("  Critical: ${plan.score.criticalFindings}")
            appendLine("  High:     ${plan.score.highFindings}")
            appendLine("  Medium:   ${plan.score.mediumFindings}")
            appendLine("  Low:      ${plan.score.lowFindings}")
            appendLine()

            if (plan.quickWins.isNotEmpty()) {
                appendLine("─── QUICK WINS ─────────────────────────────────────────────")
                appendLine("These items have high impact and low effort:")
                appendLine()
                plan.quickWins.forEach { qw ->
                    appendLine("  ★ [${qw.severity}] ${qw.title}")
                    appendLine("    Action: ${qw.action}")
                    appendLine()
                }
            }

            appendLine("─── FULL REMEDIATION PLAN ──────────────────────────────────")
            appendLine()
            plan.items.forEach { item ->
                appendLine("#${item.priority} [${item.severity}] ${item.title}")
                appendLine("   Category: ${item.category}")
                appendLine("   Effort:   ${item.effort}")
                appendLine("   Action:   ${item.action}")
                appendLine("   Impact:   ${item.estimatedImpact}")
                if (item.relatedCves.isNotEmpty()) {
                    appendLine("   CVEs:     ${item.relatedCves.joinToString(", ")}")
                }
                appendLine()
            }

            appendLine("═══════════════════════════════════════════════════════════")
            appendLine("  END OF REMEDIATION PLAN")
            appendLine("═══════════════════════════════════════════════════════════")
        }

        val outFile = File(outputDir, "remediation_${plan.deviceId}_${System.currentTimeMillis()}.txt")
        outFile.writeText(text)
        return outFile
    }

    // ── Helpers ─────────────────────────────────────────────────

    private fun generateDefaultRemediation(finding: Finding): String = when (finding.category) {
        FindingCategory.OUTDATED_PATCH -> "Update the device to the latest OS version"
        FindingCategory.KNOWN_VULNERABILITY -> "Apply vendor patches for ${finding.relatedCves.joinToString(", ")}"
        FindingCategory.EXPOSED_SERVICE -> "Disable the exposed service '${finding.affectedComponent}' if not needed"
        FindingCategory.UNSAFE_TRUST -> "Review and revoke unauthorized trust relationships"
        FindingCategory.USB_ANOMALY -> "Investigate USB behavior and use known-good cables/accessories"
        FindingCategory.CONFIGURATION_WEAKNESS -> "Strengthen security settings for ${finding.affectedComponent}"
        FindingCategory.MISSING_PROTECTION -> "Enable ${finding.affectedComponent} protection"
        FindingCategory.SUSPICIOUS_ARTIFACT -> "Review and investigate the suspicious artifact"
        FindingCategory.NETWORK_EXPOSURE -> "Restrict network access or enable firewall rules"
        FindingCategory.GENERAL -> "Review and address the finding based on organizational policy"
    }

    private fun estimateImpact(finding: Finding, score: SecurityScore): String {
        val deduction = when (finding.severity) {
            FindingSeverity.CRITICAL -> 3.0
            FindingSeverity.HIGH -> 1.5
            FindingSeverity.MEDIUM -> 0.5
            FindingSeverity.LOW -> 0.2
            FindingSeverity.INFO -> 0.0
        }
        val potentialNewScore = (score.overallScore + deduction).coerceAtMost(10.0)
        return "Fixing this could raise score to ~${"%.1f".format(potentialNewScore)}/10.0"
    }
}
