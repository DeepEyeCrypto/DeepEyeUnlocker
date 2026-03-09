package com.deepeye.otg.service

import android.content.Context
import android.util.Log
import com.deepeye.otg.NativeBridge
import com.deepeye.otg.domain.models.DeepEyeOperation
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Stage L — Reporting & Forensic Audit Engine.
 * Generates structured JSON and human-readable reports for every operation.
 */
object ReportManager {
    private const val TAG = "DeepEye-Report"

    data class AuditEntry(
        val timestamp: String,
        val operation: String,
        val result: String,
        val evidenceHash: String? = null,
        val evidencePath: String? = null
    )

    private val sessionLogs = mutableListOf<AuditEntry>()
    private var currentDevice: JSONObject? = null

    fun startSession(deviceInfoJson: String) {
        sessionLogs.clear()
        currentDevice = try { JSONObject(deviceInfoJson) } catch (e: Exception) { null }
        Log.i(TAG, "[REPORT] Session started for ${currentDevice?.optString("model", "Unknown")}")
    }

    fun logOperation(op: DeepEyeOperation, success: Boolean, message: String, filePath: String? = null) {
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val hash = if (filePath != null && File(filePath).exists()) {
            NativeBridge.calculateFileHash(filePath)
        } else null

        val entry = AuditEntry(
            timestamp = ts,
            operation = op.label,
            result = if (success) "SUCCESS" else "FAILED: $message",
            evidenceHash = hash,
            evidencePath = filePath
        )
        
        sessionLogs.add(entry)
        Log.d(TAG, "[REPORT] Added entry: ${entry.operation} | Hash=${hash?.take(8)}")
    }

    /**
     * Generates a final JSON audit report for the session.
     */
    fun generateFinalJson(context: Context): File? {
        val root = JSONObject().apply {
            put("version", "v2026.19")
            put("report_id", UUID.randomUUID().toString())
            put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
            put("device", currentDevice ?: JSONObject().put("status", "unknown"))
            
            val logsArray = JSONArray()
            sessionLogs.forEach { entry ->
                logsArray.put(JSONObject().apply {
                    put("ts", entry.timestamp)
                    put("op", entry.operation)
                    put("res", entry.result)
                    put("sha256", entry.evidenceHash ?: "N/A")
                    put("path", entry.evidencePath ?: "N/A")
                })
            }
            put("audit_trail", logsArray)
        }

        return try {
            val dir = File(context.filesDir, "reports")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "Report_${System.currentTimeMillis()}.json")
            file.writeText(root.toString(4))
            Log.i(TAG, "[REPORT] Final report saved: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "[REPORT] Failed to save report: ${e.message}")
            null
        }
    }
}
