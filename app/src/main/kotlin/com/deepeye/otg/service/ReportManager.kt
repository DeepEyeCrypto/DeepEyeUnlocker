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

    private val fleetLogs = mutableMapOf<String, MutableList<AuditEntry>>()
    private val fleetDevices = mutableMapOf<String, JSONObject>()

    fun initDevice(deviceKey: String, infoJson: String) {
        fleetDevices[deviceKey] = try { JSONObject(infoJson) } catch (e: Exception) { JSONObject() }
        if (!fleetLogs.containsKey(deviceKey)) {
            fleetLogs[deviceKey] = mutableListOf()
        }
        Log.i(TAG, "[FLEET] Audit initialized for device: $deviceKey")
    }

    fun logOperation(deviceKey: String?, op: DeepEyeOperation, success: Boolean, message: String, filePath: String? = null) {
        val key = deviceKey ?: "GLOBAL"
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
        
        fleetLogs.getOrPut(key) { mutableListOf() }.add(entry)
        Log.d(TAG, "[FLEET] Added entry for $key: ${entry.operation} | Result=${if(success) "OK" else "FAIL"}")
    }

    /**
     * Generates a consolidated JSON report for ALL active and recently seen devices.
     */
    fun generateFleetReport(context: Context): File? {
        val root = JSONObject().apply {
            put("report_type", "FORENSIC_FLEET_AUDIT")
            put("report_id", UUID.randomUUID().toString())
            put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
            put("engine_version", "v2026.19-MULTI")
            
            val nodesArray = JSONArray()
            fleetLogs.forEach { (deviceKey, logs) ->
                val nodeObj = JSONObject().apply {
                    put("device_key", deviceKey)
                    put("device_info", fleetDevices[deviceKey] ?: JSONObject().put("status", "unknown"))
                    
                    val logsArray = JSONArray()
                    logs.forEach { entry ->
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
                nodesArray.put(nodeObj)
            }
            put("forensic_nodes", nodesArray)
        }

        return try {
            val dir = File(context.filesDir, "reports")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "FleetReport_${System.currentTimeMillis()}.json")
            file.writeText(root.toString(4))
            Log.i(TAG, "[FLEET] Consolidated report saved: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "[FLEET] Failed to save report: ${e.message}")
            null
        }
    }

    fun clearFleet() {
        fleetLogs.clear()
        fleetDevices.clear()
    }
}
