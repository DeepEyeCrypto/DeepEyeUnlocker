package com.deepeye.otg.feature.forensics

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
// Report Exporter — Forensic Report Generation
// DeepEye OTG — Forensics Module (Part 6)
// ──────────────────────────────────────────────────────────────

private const val TAG = "ReportExporter"

/**
 * Forensic report format.
 */
enum class ReportFormat {
    JSON,       // Structured JSON
    TEXT,       // Human-readable text
    CSV,        // Spreadsheet-friendly
    HTML        // Browser viewable
}

/**
 * Exports forensic analysis results in various formats.
 *
 * Generates reports from:
 * - Artifact indexes
 * - Timelines
 * - Hash verification results
 * - Chain-of-custody records
 *
 * All reports include provenance metadata.
 */
class ReportExporter @Inject constructor(@Named("forensicsReportsDir") private val outputDir: File) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US)
    private val filenameDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)

    init {
        outputDir.mkdirs()
    }

    // ── JSON Export ──────────────────────────────────────────────

    /**
     * Export a complete forensic report as JSON.
     */
    fun exportJson(
        indexResult: IndexResult? = null,
        timeline: ForensicTimeline? = null,
        verificationResult: BatchVerificationResult? = null,
        chainOfCustody: ChainOfCustodyRecord? = null,
        caseId: String = "unknown",
        examinerName: String = "DeepEye OTG"
    ): File {
        val root = JSONObject().apply {
            put("format_version", "1.0")
            put("generator", "DeepEye OTG Forensics")
            put("generated_at", dateFormat.format(Date()))
            put("case_id", caseId)
            put("examiner", examinerName)

            // Index results
            indexResult?.let { idx ->
                put("artifact_index", JSONObject().apply {
                    put("session_id", idx.sessionId)
                    put("root_path", idx.rootPath)
                    put("total_files", idx.totalFiles)
                    put("total_size_bytes", idx.totalSizeBytes)
                    put("indexed_at", idx.indexedAt)
                    put("duration_ms", idx.duration)

                    val typeSummary = JSONObject()
                    idx.byType.forEach { (type, count) -> typeSummary.put(type.name, count) }
                    put("type_summary", typeSummary)

                    val arts = JSONArray()
                    idx.artifacts.forEach { artifact ->
                        arts.put(JSONObject().apply {
                            put("id", artifact.id)
                            put("path", artifact.path)
                            put("filename", artifact.filename)
                            put("type", artifact.artifactType.name)
                            put("size_bytes", artifact.sizeBytes)
                            artifact.md5?.let { put("md5", it) }
                            artifact.sha256?.let { put("sha256", it) }
                            put("last_modified", artifact.lastModified)
                            put("extension", artifact.extension)
                        })
                    }
                    put("artifacts", arts)
                })
            }

            // Timeline
            timeline?.let { tl ->
                put("timeline", JSONObject().apply {
                    put("id", tl.id)
                    put("event_count", tl.eventCount)
                    put("earliest", tl.earliestEvent)
                    put("latest", tl.latestEvent)

                    val events = JSONArray()
                    tl.events.forEach { event ->
                        events.put(JSONObject().apply {
                            put("timestamp", event.timestamp)
                            put("source", event.source)
                            put("category", event.category.name)
                            put("action", event.action)
                            put("description", event.description)
                            put("confidence", event.confidence.name)
                            event.artifactPath?.let { put("artifact_path", it) }
                        })
                    }
                    put("events", events)
                })
            }

            // Verification
            verificationResult?.let { vr ->
                put("hash_verification", JSONObject().apply {
                    put("total_files", vr.totalFiles)
                    put("verified", vr.verified)
                    put("mismatched", vr.mismatched)
                    put("errors", vr.errors)
                    put("duration_ms", vr.durationMs)

                    val results = JSONArray()
                    vr.results.forEach { r ->
                        results.put(JSONObject().apply {
                            put("file", r.filePath)
                            put("algorithm", r.algorithm)
                            put("computed", r.computedHash)
                            put("expected", r.expectedHash)
                            put("matches", r.matches)
                        })
                    }
                    put("results", results)
                })
            }

            // Chain of custody
            chainOfCustody?.let { coc ->
                put("chain_of_custody", JSONObject().apply {
                    put("case_id", coc.caseId)
                    put("examiner", coc.examinerName)
                    put("acquisition_time", coc.acquisitionTime)
                    put("total_artifacts", coc.totalArtifacts)
                    put("total_bytes", coc.totalBytes)
                    put("notes", coc.notes)

                    val hashes = JSONArray()
                    coc.artifactHashes.forEach { ah ->
                        hashes.put(JSONObject().apply {
                            put("path", ah.path)
                            put("sha256", ah.sha256)
                            ah.md5?.let { put("md5", it) }
                            put("size", ah.sizeBytes)
                        })
                    }
                    put("artifact_hashes", hashes)
                })
            }
        }

        val filename = "forensic_report_${filenameDateFormat.format(Date())}.json"
        val outFile = File(outputDir, filename)
        FileWriter(outFile).use { it.write(root.toString(2)) }

        Log.i(TAG, "JSON report exported: ${outFile.absolutePath}")
        return outFile
    }

    // ── Text Export ──────────────────────────────────────────────

    /**
     * Export a human-readable text report.
     */
    fun exportText(
        indexResult: IndexResult? = null,
        timeline: ForensicTimeline? = null,
        chainOfCustody: ChainOfCustodyRecord? = null,
        caseId: String = "unknown"
    ): File {
        val text = buildString {
            appendLine("═══════════════════════════════════════════════════════════")
            appendLine("  DEEPEYE OTG — FORENSIC ANALYSIS REPORT")
            appendLine("  Case ID: $caseId")
            appendLine("  Generated: ${dateFormat.format(Date())}")
            appendLine("═══════════════════════════════════════════════════════════")
            appendLine()

            indexResult?.let { idx ->
                appendLine("─── ARTIFACT INDEX ────────────────────────────────────────")
                appendLine("Root: ${idx.rootPath}")
                appendLine("Files: ${idx.totalFiles}")
                appendLine("Size: ${formatSize(idx.totalSizeBytes)}")
                appendLine("Duration: ${idx.duration}ms")
                appendLine()
                appendLine("By Type:")
                idx.byType.entries.sortedByDescending { it.value }.forEach { (type, count) ->
                    appendLine("  %-25s %d".format(type.name, count))
                }
                appendLine()

                if (idx.errors.isNotEmpty()) {
                    appendLine("Errors (${idx.errors.size}):")
                    idx.errors.take(20).forEach { appendLine("  • $it") }
                    appendLine()
                }
            }

            timeline?.let { tl ->
                appendLine("─── TIMELINE ──────────────────────────────────────────────")
                appendLine("Events: ${tl.eventCount}")
                tl.earliestEvent?.let { appendLine("From: ${dateFormat.format(Date(it))}") }
                tl.latestEvent?.let { appendLine("To:   ${dateFormat.format(Date(it))}") }
                appendLine()

                tl.events.take(100).forEach { event ->
                    appendLine("[${dateFormat.format(Date(event.timestamp))}] " +
                            "[${event.category}] ${event.action}")
                    appendLine("  ${event.description}")
                }
                if (tl.events.size > 100) {
                    appendLine("  ... and ${tl.events.size - 100} more events")
                }
                appendLine()
            }

            chainOfCustody?.let { coc ->
                appendLine("─── CHAIN OF CUSTODY ──────────────────────────────────────")
                appendLine("Case: ${coc.caseId}")
                appendLine("Examiner: ${coc.examinerName}")
                appendLine("Acquired: ${dateFormat.format(Date(coc.acquisitionTime))}")
                appendLine("Artifacts: ${coc.totalArtifacts}")
                appendLine("Total Size: ${formatSize(coc.totalBytes)}")
                if (coc.notes.isNotBlank()) appendLine("Notes: ${coc.notes}")
                appendLine()
            }

            appendLine("═══════════════════════════════════════════════════════════")
            appendLine("  END OF REPORT")
            appendLine("═══════════════════════════════════════════════════════════")
        }

        val filename = "forensic_report_${filenameDateFormat.format(Date())}.txt"
        val outFile = File(outputDir, filename)
        outFile.writeText(text)

        Log.i(TAG, "Text report exported: ${outFile.absolutePath}")
        return outFile
    }

    // ── CSV Export (artifacts only) ──────────────────────────────

    /**
     * Export artifact index as CSV.
     */
    fun exportCsv(indexResult: IndexResult): File {
        val filename = "artifacts_${filenameDateFormat.format(Date())}.csv"
        val outFile = File(outputDir, filename)

        FileWriter(outFile).use { writer ->
            writer.appendLine("id,path,filename,type,size_bytes,md5,sha256,last_modified,extension")
            indexResult.artifacts.forEach { a ->
                writer.appendLine(
                    "${csvEscape(a.id)},${csvEscape(a.path)},${csvEscape(a.filename)}," +
                            "${a.artifactType},${a.sizeBytes},${a.md5 ?: ""},${a.sha256 ?: ""}," +
                            "${a.lastModified},${a.extension}"
                )
            }
        }

        Log.i(TAG, "CSV exported: ${outFile.absolutePath}")
        return outFile
    }

    private fun csvEscape(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else value
    }

    private fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)}KB"
        bytes < 1024L * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024))}MB"
        else -> "${"%.2f".format(bytes / (1024.0 * 1024 * 1024))}GB"
    }
}
