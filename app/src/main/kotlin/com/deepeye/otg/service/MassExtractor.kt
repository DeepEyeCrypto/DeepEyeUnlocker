package com.deepeye.otg.service

import android.content.Context
import android.util.Log
import com.deepeye.otg.NativeBridge
import com.deepeye.otg.usb.UsbLifecycleManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Stage 50.3 — Mass Forensic Data Extraction Service.
 * Orchestrates multi-device pulls of critical evidence to central storage.
 */
class MassExtractor(
    private val context: Context,
    private val lifecycleManager: UsbLifecycleManager
) {
    private val TAG = "DeepEye-MassExtract"

    data class ExtractionProgress(
        val deviceKey: String,
        val currentFile: String,
        val progressPercent: Int,
        val status: String
    )

    private val _fleetProgress = MutableStateFlow<Map<String, ExtractionProgress>>(emptyMap())
    val fleetProgress = _fleetProgress.asStateFlow()

    /**
     * Extracts target paths from all selected devices in parallel.
     */
    suspend fun extractFromFleet(
        deviceKeys: Set<String>,
        srcPaths: List<String>,
        onLog: (String, String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val rootDir = File(context.getExternalFilesDir(null), "evidence/mass_${System.currentTimeMillis()}")
        if (!rootDir.exists()) rootDir.mkdirs()

        onLog("EXTRACT", "Starting Mass Extraction for ${deviceKeys.size} devices in $rootDir")

        deviceKeys.forEach { key ->
            val conn = lifecycleManager.getActiveConnection(key)
            val dev = lifecycleManager.getActiveDevice(key)

            if (conn == null || dev == null) {
                onLog(key, "ERROR: Device not connected")
                return@forEach
            }

            val deviceEvidenceDir = File(rootDir, "device_${key.replace(":", "_")}")
            if (!deviceEvidenceDir.exists()) deviceEvidenceDir.mkdirs()

            val handle = NativeBridge.initCore(conn.fileDescriptor, dev.vendorId, dev.productId)
            if (handle != 0L) {
                try {
                    // 1. Ensure MTK Decryption is active for this node (Stage 300.1)
                    // (Assuming keys are already extracted as part of normal init)
                    
                    srcPaths.forEach { src ->
                        onLog(key, "Pulling evidence: $src")
                        val dest = File(deviceEvidenceDir, src.replace("/", "_")).absolutePath
                        
                        NativeBridge.fsExtractDirectory(handle, "userdata", src, dest) { pct, file ->
                            _fleetProgress.value = _fleetProgress.value.toMutableMap().apply {
                                put(key, ExtractionProgress(key, file, pct, "PULLING $src"))
                            }
                        }
                    }
                    onLog(key, "SUCCESS: Extraction completed")
                    ReportManager.logOperation(key, com.deepeye.otg.domain.models.DeepEyeOperation.SAFE_DUMP, true, "Mass Pull Complete: $rootDir")
                } catch (e: Exception) {
                    onLog(key, "CRITICAL: Extraction failed - ${e.message}")
                } finally {
                    NativeBridge.closeCore(handle)
                }
            } else {
                onLog(key, "ERROR: Could not open forensic transport handle")
            }
        }
    }
}
