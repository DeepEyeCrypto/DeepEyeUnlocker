package com.deepeye.otg.service

import android.util.Log
import com.deepeye.otg.NativeBridge
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stage 600.1 — Physical Integrity Analysis Service.
 * Analyzes USB signal metrics to detect hardware interposers or anomalies.
 */
@Singleton
class PhysicalIntegrityService @Inject constructor() {
    private const val TAG = "IntegrityService"

    enum class IntegrityStatus {
        VERIFIED,
        ANOMALY_DETECTED,
        CRITICAL_TAMPERING,
        UNKNOWN
    }

    data class IntegrityReport(
        val status: IntegrityStatus,
        val impedanceDelta: Double,
        val eyeDiagramScore: Int,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Conducts a deep signal scan via NativeBridge.
     */
    fun analyzeDevice(handle: Long): IntegrityReport {
        if (!NativeBridge.isLoaded()) {
            return IntegrityReport(IntegrityStatus.UNKNOWN, 0.0, 0)
        }

        return try {
            val jsonRaw = NativeBridge.examinePhysicalIntegrity(handle)
            val json = JSONObject(jsonRaw)
            
            val statusStr = json.optString("status", "UNKNOWN")
            val status = when (statusStr) {
                "VERIFIED" -> IntegrityStatus.VERIFIED
                "ANOMALY" -> IntegrityStatus.ANOMALY_DETECTED
                "TAMPERED" -> IntegrityStatus.CRITICAL_TAMPERING
                else -> IntegrityStatus.UNKNOWN
            }

            IntegrityReport(
                status = status,
                impedanceDelta = json.optDouble("impedance_delta", 0.0),
                eyeDiagramScore = json.optInt("eye_score", 0)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Analysis failed: ${e.message}")
            IntegrityReport(IntegrityStatus.UNKNOWN, 0.0, 0)
        }
    }
}
