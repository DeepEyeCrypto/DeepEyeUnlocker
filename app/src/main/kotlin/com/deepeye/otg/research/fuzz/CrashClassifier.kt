package com.deepeye.otg.research.fuzz

import android.util.Log
import java.security.MessageDigest

// ──────────────────────────────────────────────────────────────
// Crash Classifier — Bucketing + Triage
// DeepEye OTG — Research Module (Part 4)
// ──────────────────────────────────────────────────────────────

private const val TAG = "CrashClassifier"

/**
 * Classifies and buckets crash results for deduplication and triage.
 *
 * Bucketing strategy:
 * 1. Extract crash signature from output (panic string, backtrace hash, signal)
 * 2. Determine affected component from crash context
 * 3. Assign severity based on crash type
 * 4. Group by bucket (component + crash_type + signature_prefix)
 */
class CrashClassifier {

    /**
     * Crash severity for triage prioritization.
     */
    enum class CrashSeverity {
        CRITICAL,  // Kernel panic, SEP fault, memory corruption with control
        HIGH,      // User-space code execution potential, sandbox escape indicators
        MEDIUM,    // Denial of service, driver crash, resource exhaustion
        LOW,       // Handled exception, assertion failure, graceful error
        INFO       // Interesting behavior but not a true crash
    }

    /**
     * High-level crash type classification.
     */
    enum class CrashType {
        KERNEL_PANIC,
        USERSPACE_CRASH,
        WATCHDOG_TIMEOUT,
        ASSERTION_FAILURE,
        SEGFAULT,
        BUS_ERROR,
        ABORT,
        RESOURCE_EXHAUSTION,
        HANG,
        PROTOCOL_ERROR,
        DRIVER_FAULT,
        UNKNOWN
    }

    /**
     * A classified crash with triage metadata.
     */
    data class ClassifiedCrash(
        val testCaseId: String,
        val signature: String,
        val bucket: String,
        val crashType: CrashType,
        val severity: CrashSeverity,
        val component: String,
        val details: String,
        val stackTraceHash: String?,
        val firstSeen: Long = System.currentTimeMillis(),
        var occurrences: Int = 1,
        val triageNotes: String = "",
        val reproConfirmed: Boolean = false
    )

    // Known crash patterns → component/type mapping
    private data class CrashPattern(
        val regex: Regex,
        val component: String,
        val crashType: CrashType,
        val severity: CrashSeverity
    )

    private val patterns = listOf(
        CrashPattern(
            Regex("panic.*kernel|kernel.*panic", RegexOption.IGNORE_CASE),
            "Kernel", CrashType.KERNEL_PANIC, CrashSeverity.CRITICAL
        ),
        CrashPattern(
            Regex("panic.*iokit|IOKit.*fault", RegexOption.IGNORE_CASE),
            "IOKit", CrashType.DRIVER_FAULT, CrashSeverity.HIGH
        ),
        CrashPattern(
            Regex("SIGBUS|bus error", RegexOption.IGNORE_CASE),
            "Unknown", CrashType.BUS_ERROR, CrashSeverity.HIGH
        ),
        CrashPattern(
            Regex("SIGSEGV|segmentation fault|EXC_BAD_ACCESS", RegexOption.IGNORE_CASE),
            "Unknown", CrashType.SEGFAULT, CrashSeverity.HIGH
        ),
        CrashPattern(
            Regex("SIGABRT|abort|Abort trap", RegexOption.IGNORE_CASE),
            "Unknown", CrashType.ABORT, CrashSeverity.MEDIUM
        ),
        CrashPattern(
            Regex("assertion.*fail|assert.*false|precondition", RegexOption.IGNORE_CASE),
            "Unknown", CrashType.ASSERTION_FAILURE, CrashSeverity.LOW
        ),
        CrashPattern(
            Regex("watchdog.*timeout|WDT|watchdog", RegexOption.IGNORE_CASE),
            "Unknown", CrashType.WATCHDOG_TIMEOUT, CrashSeverity.MEDIUM
        ),
        CrashPattern(
            Regex("out of memory|OOM|memory pressure|jetsam", RegexOption.IGNORE_CASE),
            "Unknown", CrashType.RESOURCE_EXHAUSTION, CrashSeverity.LOW
        ),
        CrashPattern(
            Regex("USB.*error|usb.*fault|endpoint.*stall", RegexOption.IGNORE_CASE),
            "USB", CrashType.PROTOCOL_ERROR, CrashSeverity.MEDIUM
        ),
        CrashPattern(
            Regex("HID.*crash|AppleHID|IOHIDFamily", RegexOption.IGNORE_CASE),
            "IOHIDFamily", CrashType.DRIVER_FAULT, CrashSeverity.HIGH
        ),
    )

    // ── Classification ──────────────────────────────────────────

    /**
     * Classify a crash result.
     *
     * @param testCase the input that caused the crash
     * @param result the execution result
     * @return classified crash with bucket and triage info
     */
    fun classify(testCase: FuzzTestCase, result: FuzzResult): ClassifiedCrash {
        val fullOutput = "${result.stdout}\n${result.stderr}\n${result.crashSignature ?: ""}"

        // Try pattern matching
        var matchedComponent = "Unknown"
        var matchedType = CrashType.UNKNOWN
        var matchedSeverity = CrashSeverity.MEDIUM

        for (pattern in patterns) {
            if (pattern.regex.containsMatchIn(fullOutput)) {
                matchedComponent = pattern.component
                matchedType = pattern.crashType
                matchedSeverity = pattern.severity
                break
            }
        }

        // Build signature
        val signature = buildSignature(result, matchedType)

        // Build bucket
        val bucket = "${matchedComponent}_${matchedType.name}_${signature.take(16)}"

        val classified = ClassifiedCrash(
            testCaseId = testCase.id,
            signature = signature,
            bucket = bucket,
            crashType = matchedType,
            severity = matchedSeverity,
            component = matchedComponent,
            details = buildDetails(testCase, result, matchedType),
            stackTraceHash = extractStackTraceHash(fullOutput)
        )

        Log.i(TAG, "Classified crash: bucket=$bucket severity=$matchedSeverity")
        return classified
    }

    /**
     * Re-classify a crash with additional analyst context.
     */
    fun reclassify(
        crash: ClassifiedCrash,
        component: String? = null,
        severity: CrashSeverity? = null,
        triageNotes: String? = null,
        reproConfirmed: Boolean? = null
    ): ClassifiedCrash {
        return crash.copy(
            component = component ?: crash.component,
            severity = severity ?: crash.severity,
            triageNotes = triageNotes ?: crash.triageNotes,
            reproConfirmed = reproConfirmed ?: crash.reproConfirmed
        )
    }

    // ── Signature Building ──────────────────────────────────────

    private fun buildSignature(result: FuzzResult, crashType: CrashType): String {
        val parts = mutableListOf<String>()
        parts.add(crashType.name)

        // Use crash signature if available
        result.crashSignature?.let { sig ->
            parts.add(sig.take(64))
        }

        // Use exit code
        result.exitCode?.let { code ->
            parts.add("exit=$code")
        }

        // Hash first significant line of stderr
        val firstLine = result.stderr.lines().firstOrNull { it.isNotBlank() }
        firstLine?.let {
            parts.add(hashString(it).take(8))
        }

        return parts.joinToString("_")
    }

    private fun buildDetails(
        testCase: FuzzTestCase,
        result: FuzzResult,
        crashType: CrashType
    ): String = buildString {
        appendLine("=== Crash Details ===")
        appendLine("Type: $crashType")
        appendLine("Test Case: ${testCase.id}")
        appendLine("Input Size: ${testCase.inputData.size} bytes")
        appendLine("Mutation: ${testCase.mutationStrategy ?: "N/A"}")
        appendLine("Generation: ${testCase.generation}")
        appendLine("Duration: ${result.durationMs}ms")
        appendLine("Exit Code: ${result.exitCode ?: "N/A"}")
        appendLine()
        if (result.crashSignature != null) {
            appendLine("=== Crash Signature ===")
            appendLine(result.crashSignature)
            appendLine()
        }
        if (result.stderr.isNotBlank()) {
            appendLine("=== stderr (first 500 chars) ===")
            appendLine(result.stderr.take(500))
            appendLine()
        }
        if (result.stdout.isNotBlank()) {
            appendLine("=== stdout (first 500 chars) ===")
            appendLine(result.stdout.take(500))
        }
    }

    private fun extractStackTraceHash(output: String): String? {
        // Look for common stack trace patterns
        val framePattern = Regex("(0x[0-9a-fA-F]+\\s+\\S+|at \\S+\\(.+\\))")
        val frames = framePattern.findAll(output).map { it.value }.toList()

        if (frames.isEmpty()) return null

        // Hash the top 5 frames for deduplication
        val topFrames = frames.take(5).joinToString("|")
        return hashString(topFrames)
    }

    private fun hashString(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    // ── Triage Checklist ────────────────────────────────────────

    /**
     * Validation checklist for confirming a crash is a real repro.
     */
    data class TriageChecklist(
        val reproduced: Boolean = false,
        val minimalInput: Boolean = false,
        val rootCauseIdentified: Boolean = false,
        val affectedVersionsConfirmed: Boolean = false,
        val componentConfirmed: Boolean = false,
        val severityConfirmed: Boolean = false,
        val notesAdded: Boolean = false
    ) {
        val isComplete: Boolean
            get() = reproduced && componentConfirmed && severityConfirmed
    }
}
