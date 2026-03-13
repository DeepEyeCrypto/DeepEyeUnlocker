package com.deepeye.otg.research.fuzz

import javax.inject.Inject
import javax.inject.Named

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ──────────────────────────────────────────────────────────────
// Repro Recorder — Crash Reproduction + Evidence Persistence
// DeepEye OTG — Research Module (Part 4)
// ──────────────────────────────────────────────────────────────

private const val TAG = "ReproRecorder"

/**
 * Records crash reproduction sessions and persists evidence.
 *
 * For each session, produces:
 * - session_meta.json   — session config and summary
 * - crashes/             — per-crash evidence
 *   - <crash_id>.json   — crash report
 *   - <crash_id>.bin    — reproduction input
 * - timeline.jsonl       — ordered event log
 * - summary.txt          — human-readable summary
 */
class ReproRecorder @Inject constructor(@Named("fuzzReproDir") private val outputDir: File) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)

    private var sessionDir: File? = null
    private var timelineWriter: FileWriter? = null
    private var crashCount = 0
    private var eventCount = 0L

    // ── Session Lifecycle ───────────────────────────────────────

    /**
     * Start a new recording session.
     */
    fun startSession(config: FuzzConfig) {
        val dirName = "repro_${dateFormat.format(Date())}_${config.sessionId}"
        sessionDir = File(outputDir, dirName).also { it.mkdirs() }
        File(sessionDir, "crashes").mkdirs()

        crashCount = 0
        eventCount = 0

        // Write session metadata
        val meta = JSONObject().apply {
            put("format_version", "1.0")
            put("session_id", config.sessionId)
            put("target_surface", config.targetSurface.name)
            put("max_iterations", config.maxIterations)
            put("max_duration_ms", config.maxDurationMs)
            put("max_input_size", config.maxInputSize)
            put("started_at", System.currentTimeMillis())
            put("notes", config.notes)
        }
        File(sessionDir, "session_meta.json").writeText(meta.toString(2))

        // Open timeline writer
        timelineWriter = FileWriter(File(sessionDir, "timeline.jsonl"))
        recordTimelineEvent("SESSION_START", "Fuzz session started")

        Log.i(TAG, "Recording session: ${sessionDir?.absolutePath}")
    }

    /**
     * End the current recording session and write summary.
     */
    fun endSession(finalStats: FuzzSessionStats) {
        recordTimelineEvent("SESSION_END", "Fuzz session ended")
        timelineWriter?.close()
        timelineWriter = null

        // Write summary
        val summary = buildString {
            appendLine("═══════════════════════════════════════════")
            appendLine("  DeepEye OTG — Fuzz Session Summary")
            appendLine("═══════════════════════════════════════════")
            appendLine()
            appendLine("Session:       ${finalStats.sessionId}")
            appendLine("Duration:      ${finalStats.elapsedMs / 1000}s")
            appendLine("Executions:    ${finalStats.totalExecutions}")
            appendLine("Exec/s:        ${"%.1f".format(finalStats.executionsPerSecond)}")
            appendLine("Total Crashes: ${finalStats.totalCrashes}")
            appendLine("Unique Crashes: ${finalStats.uniqueCrashes}")
            appendLine("Corpus Size:   ${finalStats.corpusSize}")
            appendLine()
            appendLine("═══════════════════════════════════════════")
        }
        sessionDir?.let { File(it, "summary.txt").writeText(summary) }

        // Update session meta with final stats
        sessionDir?.let { dir ->
            val metaFile = File(dir, "session_meta.json")
            if (metaFile.exists()) {
                val meta = JSONObject(metaFile.readText())
                meta.put("ended_at", System.currentTimeMillis())
                meta.put("total_executions", finalStats.totalExecutions)
                meta.put("total_crashes", finalStats.totalCrashes)
                meta.put("unique_crashes", finalStats.uniqueCrashes)
                meta.put("execs_per_second", finalStats.executionsPerSecond)
                metaFile.writeText(meta.toString(2))
            }
        }

        Log.i(TAG, "Session ended. Summary at ${sessionDir?.absolutePath}/summary.txt")
        sessionDir = null
    }

    // ── Crash Recording ─────────────────────────────────────────

    /**
     * Record a crash with full evidence.
     */
    fun recordCrash(
        testCase: FuzzTestCase,
        result: FuzzResult,
        classified: CrashClassifier.ClassifiedCrash
    ) {
        val dir = sessionDir ?: return
        crashCount++

        val crashDir = File(dir, "crashes")

        // Save reproduction input
        File(crashDir, "${testCase.id}.bin").writeBytes(testCase.inputData)

        // Save crash report as JSON
        val report = JSONObject().apply {
            put("crash_id", testCase.id)
            put("bucket", classified.bucket)
            put("signature", classified.signature)
            put("crash_type", classified.crashType.name)
            put("severity", classified.severity.name)
            put("component", classified.component)
            put("stack_trace_hash", classified.stackTraceHash)
            put("timestamp", classified.firstSeen)
            put("input_size", testCase.inputData.size)
            put("mutation_strategy", testCase.mutationStrategy)
            put("parent_id", testCase.parentId)
            put("generation", testCase.generation)
            put("duration_ms", result.durationMs)
            put("exit_code", result.exitCode)
            put("details", classified.details)

            if (result.stderr.isNotBlank()) {
                put("stderr", result.stderr.take(2000))
            }
            if (result.stdout.isNotBlank()) {
                put("stdout", result.stdout.take(2000))
            }
            if (testCase.metadata.isNotEmpty()) {
                put("metadata", JSONObject(testCase.metadata))
            }
        }
        File(crashDir, "${testCase.id}.json").writeText(report.toString(2))

        // Timeline event
        recordTimelineEvent("CRASH", "Crash #$crashCount: ${classified.bucket}", mapOf(
            "severity" to classified.severity.name,
            "input_size" to testCase.inputData.size.toString()
        ))

        Log.i(TAG, "Recorded crash #$crashCount: ${classified.bucket}")
    }

    // ── Replay Support ──────────────────────────────────────────

    /**
     * Load all crash inputs from a previous session for replay.
     *
     * @param sessionPath path to the session directory
     * @return list of test cases ready for replay
     */
    fun loadForReplay(sessionPath: File): List<FuzzTestCase> {
        val crashDir = File(sessionPath, "crashes")
        if (!crashDir.exists()) return emptyList()

        return crashDir.listFiles()?.filter { it.extension == "bin" }?.map { file ->
            FuzzTestCase(
                id = "replay_${file.nameWithoutExtension}",
                inputData = file.readBytes(),
                metadata = mapOf("source" to "replay", "original_session" to sessionPath.name)
            )
        } ?: emptyList()
    }

    /**
     * Load a specific crash report from a previous session.
     */
    fun loadCrashReport(sessionPath: File, crashId: String): JSONObject? {
        val reportFile = File(sessionPath, "crashes/$crashId.json")
        if (!reportFile.exists()) return null
        return JSONObject(reportFile.readText())
    }

    /**
     * List all sessions in the output directory.
     */
    fun listSessions(): List<SessionSummary> {
        return outputDir.listFiles()?.filter {
            it.isDirectory && it.name.startsWith("repro_")
        }?.mapNotNull { dir ->
            val metaFile = File(dir, "session_meta.json")
            if (!metaFile.exists()) return@mapNotNull null

            val meta = JSONObject(metaFile.readText())
            SessionSummary(
                path = dir,
                sessionId = meta.optString("session_id", "unknown"),
                targetSurface = meta.optString("target_surface", "unknown"),
                startedAt = meta.optLong("started_at", 0),
                endedAt = meta.optLong("ended_at", 0),
                totalExecutions = meta.optLong("total_executions", 0),
                uniqueCrashes = meta.optLong("unique_crashes", 0)
            )
        }?.sortedByDescending { it.startedAt } ?: emptyList()
    }

    data class SessionSummary(
        val path: File,
        val sessionId: String,
        val targetSurface: String,
        val startedAt: Long,
        val endedAt: Long,
        val totalExecutions: Long,
        val uniqueCrashes: Long
    )

    // ── Timeline ────────────────────────────────────────────────

    private fun recordTimelineEvent(
        type: String,
        message: String,
        data: Map<String, String> = emptyMap()
    ) {
        val event = JSONObject().apply {
            put("seq", eventCount++)
            put("ts", System.currentTimeMillis())
            put("type", type)
            put("msg", message)
            if (data.isNotEmpty()) {
                put("data", JSONObject(data))
            }
        }
        try {
            timelineWriter?.appendLine(event.toString())
            timelineWriter?.flush()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write timeline event", e)
        }
    }
}
