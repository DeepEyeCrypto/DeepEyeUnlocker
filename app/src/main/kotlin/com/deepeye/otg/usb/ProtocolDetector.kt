package com.deepeye.otg.usb

import android.hardware.usb.UsbConstants
import android.util.Log
import com.deepeye.otg.domain.models.DeviceMode
import com.deepeye.otg.domain.models.ProtocolFamily

data class DetectionResult(
    val deviceMode: DeviceMode,
    val protocolFamily: ProtocolFamily,
    val confidence: Int,
    val reason: String
) {
    fun toConnectionMode(): com.deepeye.otg.data.ConnectionMode = when (deviceMode) {
        DeviceMode.ADB -> com.deepeye.otg.data.ConnectionMode.ADB
        DeviceMode.FASTBOOT -> com.deepeye.otg.data.ConnectionMode.FASTBOOT
        DeviceMode.QC_EDL -> com.deepeye.otg.data.ConnectionMode.EDL
        DeviceMode.MTK_BROM -> com.deepeye.otg.data.ConnectionMode.BROM
        DeviceMode.MTK_PRELOADER -> com.deepeye.otg.data.ConnectionMode.PRELOADER
        DeviceMode.MTK_META -> com.deepeye.otg.data.ConnectionMode.META
        DeviceMode.QC_DIAG -> com.deepeye.otg.data.ConnectionMode.DIAG
        DeviceMode.MTP_ONLY -> com.deepeye.otg.data.ConnectionMode.MTP
        DeviceMode.SAMSUNG_ODIN -> com.deepeye.otg.data.ConnectionMode.ODIN
        DeviceMode.UNISOC_FDL -> com.deepeye.otg.data.ConnectionMode.FDL
        DeviceMode.TESTPOINT -> com.deepeye.otg.data.ConnectionMode.TESTPOINT
        DeviceMode.RECOVERY -> com.deepeye.otg.data.ConnectionMode.ADB
        DeviceMode.FASTBOOTD -> com.deepeye.otg.data.ConnectionMode.FASTBOOT
        DeviceMode.DISCONNECTED -> com.deepeye.otg.data.ConnectionMode.UNKNOWN
        DeviceMode.UNKNOWN -> com.deepeye.otg.data.ConnectionMode.UNKNOWN
        else -> com.deepeye.otg.data.ConnectionMode.UNKNOWN
    }
}

class ProtocolDetector {
    companion object {
        private const val TAG = "ProtocolDetector"
    }

    fun detect(snapshot: UsbDescriptorSnapshot): DetectionResult {
        logSnapshot(snapshot)

        // Strict precedence:
        // BROM / PRELOADER / EDL / ODIN / FASTBOOT / ADB / MTP / UNKNOWN
        val result = detectMtk(snapshot)
            ?: detectQualcomm(snapshot)
            ?: detectOdin(snapshot)
            ?: detectFastboot(snapshot)
            ?: detectAdb(snapshot)
            ?: detectMtp(snapshot)
            ?: DetectionResult(
            deviceMode = DeviceMode.UNKNOWN,
            protocolFamily = ProtocolFamily.UNKNOWN,
            confidence = 0,
            reason = "No explicit mode signature matched"
        )

        logResult(snapshot, result)
        return result
    }

    private fun detectMtk(snapshot: UsbDescriptorSnapshot): DetectionResult? {
        val vid = snapshot.vendorId
        val pid = snapshot.productId

        return when {
            vid == 0x0E8D && pid == 0x0003 -> DetectionResult(
                DeviceMode.MTK_BROM,
                ProtocolFamily.BROM,
                100,
                "Matched MTK BootROM VID/PID 0x0E8D:0x0003"
            )
            vid == 0x0E8D && pid == 0x2000 -> DetectionResult(
                DeviceMode.MTK_PRELOADER,
                ProtocolFamily.PRELOADER,
                100,
                "Matched MTK Preloader VID/PID 0x0E8D:0x2000"
            )
            vid == 0x0E8D && pid == 0x2001 -> DetectionResult(
                DeviceMode.MTK_META,
                ProtocolFamily.MTK,
                90,
                "Matched MTK Meta VID/PID 0x0E8D:0x2001"
            )
            else -> null
        }
    }

    private fun detectQualcomm(snapshot: UsbDescriptorSnapshot): DetectionResult? {
        val vid = snapshot.vendorId
        val pid = snapshot.productId

        return if (vid == 0x05C6 && pid == 0x9008) {
            DetectionResult(
                DeviceMode.QC_EDL,
                ProtocolFamily.EDL,
                100,
                "Matched Qualcomm EDL VID/PID 0x05C6:0x9008"
            )
        } else if (vid == 0x05C6 && (pid == 0x900E || pid == 0x901D)) {
            DetectionResult(
                DeviceMode.QC_DIAG,
                ProtocolFamily.DIAG,
                90,
                "Matched Qualcomm DIAG VID/PID 0x05C6:${"%04X".format(pid)}"
            )
        } else null
    }

    private fun detectOdin(snapshot: UsbDescriptorSnapshot): DetectionResult? {
        if (snapshot.vendorId == 0x04E8 && snapshot.productId == 0x685D) {
            return DetectionResult(
                DeviceMode.SAMSUNG_ODIN,
                ProtocolFamily.ODIN,
                100,
                "Matched Samsung Odin VID/PID 0x04E8:0x685D"
            )
        }

        val text = listOfNotNull(snapshot.manufacturerName, snapshot.productName)
            .joinToString(" ").lowercase()

        return if ("samsung" in text && ("download" in text || "odin" in text)) {
            DetectionResult(
                DeviceMode.SAMSUNG_ODIN,
                ProtocolFamily.ODIN,
                70,
                "Matched Samsung download-mode text heuristic"
            )
        } else null
    }

    private fun detectFastboot(snapshot: UsbDescriptorSnapshot): DetectionResult? {
        val isFastbootInterface = snapshot.interfaces.any {
            it.interfaceClass == 0xFF &&
                it.interfaceSubclass == 0x42 &&
                it.interfaceProtocol == 0x03
        }
        val text = listOfNotNull(snapshot.manufacturerName, snapshot.productName).joinToString(" ").lowercase()

        return if (isFastbootInterface) {
            DetectionResult(
                DeviceMode.FASTBOOT,
                ProtocolFamily.FASTBOOT,
                100,
                "Matched explicit fastboot interface signature (FF/42/03)"
            )
        } else if ("fastboot" in text) {
            DetectionResult(
                DeviceMode.FASTBOOT,
                ProtocolFamily.FASTBOOT,
                70,
                "Matched fastboot product/manufacturer text"
            )
        } else null
    }

    private fun detectAdb(snapshot: UsbDescriptorSnapshot): DetectionResult? {
        // STRICT ADB:
        // 1) Explicit class/subclass/protocol = FF/42/01
        // 2) Same interface must contain at least one BULK IN + one BULK OUT endpoint
        val adbInterface = snapshot.interfaces.find {
            it.interfaceClass == 0xFF &&
                it.interfaceSubclass == 0x42 &&
                it.interfaceProtocol == 0x01
        }

        if (adbInterface == null) return null

        val hasBulkIn = adbInterface.endpoints.any {
            it.type == UsbConstants.USB_ENDPOINT_XFER_BULK && it.direction == UsbConstants.USB_DIR_IN
        }
        val hasBulkOut = adbInterface.endpoints.any {
            it.type == UsbConstants.USB_ENDPOINT_XFER_BULK && it.direction == UsbConstants.USB_DIR_OUT
        }

        if (!hasBulkIn || !hasBulkOut) {
            Log.w(
                TAG,
                "[MODE] adb-signature-rejected intf=${adbInterface.id} reason=missing-bulk-pair hasIn=$hasBulkIn hasOut=$hasBulkOut"
            )
            return null
        }

        return DetectionResult(
            DeviceMode.ADB,
            ProtocolFamily.ADB,
            95,
            "Matched explicit ADB interface signature (FF/42/01, intf=${adbInterface.id}, bulkIn+bulkOut)"
        )
    }

    private fun detectMtp(snapshot: UsbDescriptorSnapshot): DetectionResult? {
        val mtpInterface = snapshot.interfaces.any {
            it.interfaceClass == UsbConstants.USB_CLASS_STILL_IMAGE ||
                it.interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE
        }

        return if (mtpInterface) {
            DetectionResult(
                DeviceMode.MTP_ONLY,
                ProtocolFamily.MTP,
                60,
                "Matched MTP/PTP/storage-like interface class"
            )
        } else null
    }

    private fun logSnapshot(s: UsbDescriptorSnapshot) {
        Log.i(
            TAG,
            "[MODE] attach vid=0x${"%04X".format(s.vendorId)} pid=0x${"%04X".format(s.productId)} " +
                "devClass=0x${"%02X".format(s.deviceClass)} devSubclass=0x${"%02X".format(s.deviceSubclass)} " +
                "proto=0x${"%02X".format(s.deviceProtocol)} intfCount=${s.interfaceCount}"
        )
        s.interfaces.forEachIndexed { idx, intf ->
            val epSummary = intf.endpoints.joinToString(",") { ep ->
                "addr=0x${"%02X".format(ep.address)}:type=${ep.type}:dir=${ep.direction}"
            }
            Log.i(
                TAG,
                "[MODE] intf[$idx] class=0x${"%02X".format(intf.interfaceClass)} " +
                    "subclass=0x${"%02X".format(intf.interfaceSubclass)} " +
                    "proto=0x${"%02X".format(intf.interfaceProtocol)} eps=${intf.endpointCount} [$epSummary]"
            )
        }
    }

    private fun logResult(snapshot: UsbDescriptorSnapshot, result: DetectionResult) {
        Log.i(
            TAG,
            "[MODE] classify mode=${result.deviceMode} family=${result.protocolFamily} confidence=${result.confidence} reason=\"${result.reason}\""
        )

        if (result.deviceMode == DeviceMode.UNKNOWN) {
            val summary = snapshot.interfaces.joinToString(" | ") {
                "${"%02X".format(it.interfaceClass)}/${"%02X".format(it.interfaceSubclass)}/${"%02X".format(it.interfaceProtocol)}"
            }
            Log.w(
                TAG,
                "[MODE] unknown-summary vid=0x${"%04X".format(snapshot.vendorId)} pid=0x${"%04X".format(snapshot.productId)} intfTuples=\"$summary\""
            )
        }
    }
}
