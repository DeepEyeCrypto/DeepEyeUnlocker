package com.deepeye.otg.security

// ──────────────────────────────────────────────────────────────
// Severity Scorer — Finding Risk Scoring
// DeepEye OTG — Detection + Hardening Module (Part 7)
// ──────────────────────────────────────────────────────────────

/**
 * Device-level security score.
 */
data class SecurityScore(
    val deviceId: String,
    val overallScore: Double,      // 0.0 (critical) – 10.0 (excellent)
    val grade: SecurityGrade,
    val criticalFindings: Int,
    val highFindings: Int,
    val mediumFindings: Int,
    val lowFindings: Int,
    val infoFindings: Int,
    val totalFindings: Int,
    val breakdown: Map<FindingCategory, CategoryScore>,
    val generatedAt: Long = System.currentTimeMillis()
)

enum class SecurityGrade {
    A_PLUS,  // 9.5–10.0  Excellent
    A,       // 9.0–9.4   Very Good
    B,       // 8.0–8.9   Good
    C,       // 7.0–7.9   Acceptable
    D,       // 5.0–6.9   Poor
    F        // 0.0–4.9   Failing
}

data class CategoryScore(
    val category: FindingCategory,
    val score: Double,         // 0.0–10.0
    val findingCount: Int,
    val maxSeverity: FindingSeverity
)

/**
 * Calculates device security scores from findings.
 *
 * Scoring approach:
 * - Start from 10.0 (perfect)
 * - Deduct points based on findings and severity
 * - Weight active exploitation and critical findings heavily
 * - Category breakdown for targeted remediation
 */
class SeverityScorer {

    companion object {
        // Severity deduction weights
        private val SEVERITY_WEIGHTS = mapOf(
            FindingSeverity.CRITICAL to 3.0,
            FindingSeverity.HIGH to 1.5,
            FindingSeverity.MEDIUM to 0.5,
            FindingSeverity.LOW to 0.2,
            FindingSeverity.INFO to 0.0
        )

        // Maximum deduction per category (prevent single category from destroying score)
        private const val MAX_CATEGORY_DEDUCTION = 4.0
    }

    /**
     * Calculate a security score from findings.
     *
     * @param findings list of detected security findings
     * @param deviceId device identifier
     * @return overall security score
     */
    fun score(findings: List<Finding>, deviceId: String = "unknown"): SecurityScore {
        var totalDeduction = 0.0

        // Group by category and calculate per-category scores
        val categorized = findings.groupBy { it.category }
        val categoryScores = mutableMapOf<FindingCategory, CategoryScore>()

        for ((category, catFindings) in categorized) {
            var catDeduction = 0.0
            for (finding in catFindings) {
                catDeduction += SEVERITY_WEIGHTS[finding.severity] ?: 0.0
            }
            catDeduction = catDeduction.coerceAtMost(MAX_CATEGORY_DEDUCTION)
            totalDeduction += catDeduction

            val maxSev = catFindings.minByOrNull { it.severity.ordinal }?.severity
                ?: FindingSeverity.INFO

            categoryScores[category] = CategoryScore(
                category = category,
                score = (10.0 - catDeduction).coerceIn(0.0, 10.0),
                findingCount = catFindings.size,
                maxSeverity = maxSev
            )
        }

        val overall = (10.0 - totalDeduction).coerceIn(0.0, 10.0)
        val grade = scoreToGrade(overall)

        return SecurityScore(
            deviceId = deviceId,
            overallScore = overall,
            grade = grade,
            criticalFindings = findings.count { it.severity == FindingSeverity.CRITICAL },
            highFindings = findings.count { it.severity == FindingSeverity.HIGH },
            mediumFindings = findings.count { it.severity == FindingSeverity.MEDIUM },
            lowFindings = findings.count { it.severity == FindingSeverity.LOW },
            infoFindings = findings.count { it.severity == FindingSeverity.INFO },
            totalFindings = findings.size,
            breakdown = categoryScores
        )
    }

    private fun scoreToGrade(score: Double): SecurityGrade = when {
        score >= 9.5 -> SecurityGrade.A_PLUS
        score >= 9.0 -> SecurityGrade.A
        score >= 8.0 -> SecurityGrade.B
        score >= 7.0 -> SecurityGrade.C
        score >= 5.0 -> SecurityGrade.D
        else -> SecurityGrade.F
    }
}
