package com.deepeye.otg

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.util.Log

enum class DetectedProtocol {
    UNKNOWN,
    QUALCOMM_EDL,
    MTK_BROM,
    MTK_PRELOADER,
    FASTBOOT,
    SAMSUNG_ODIN,
    ADB,
    MTP_ONLY
}

data class ProtocolDetectionResult(
    val protocol: DetectedProtocol,
    val ifaceDump: String,
    val claimInterfaceIndex: Int? = null
)

/**
 * Protocol Detector for USB OTG connected devices.
 *
 * Classification priority:
 * 1. VID/PID lookup (known Qualcomm EDL 9008, MTK BROM, Samsung Download, ADB, Fastboot)
 * 2. USB interface class/subclass/protocol heuristics
 * 3. Active probing (Fastboot ASCII, MTK sync, Sahara hello)
 * 4. Fallback: MTP_ONLY if all interfaces are Still-Image/MSC class, else UNKNOWN
 *
 * Supported protocol fingerprints:
 * - Qualcomm EDL 9008:  VID=05C6 PID=9008, class=0xFF, bulk IN+OUT
 * - MTK BROM/Preloader: VID=0E8D PID=0003/2000, class=0xFF or class=0x02 (CDC)
 * - Samsung Download:   VID=04E8 PID=6601, class=0xFF or class=0x0A
 * - Fastboot:           class=0xFF sub=0x42 proto=0x03 (Google ADB interface)
 * - ADB:                class=0xFF sub=0x42 proto=0x01
 * - MTP:                class=0x06 (Still Image) or class=0x08 (Mass Storage)
 */
class ProtocolProbe(private val connection: UsbDeviceConnection, private val device: UsbDevice) {

    private val TAG = "DeepEye-Probe"
    private var bulkIn: UsbEndpoint? = null
    private var bulkOut: UsbEndpoint? = null
    private var probeInterfaceIndex: Int? = null

    // Known VID/PID pairs for fast classification
    companion object {
        // Qualcomm EDL 9008
        private val QCOM_EDL_VIDS = setOf(0x05C6, 0x18D1)
        private const val QCOM_EDL_PID = 0x9008

        // MTK BROM / Preloader
        private const val MTK_VID = 0x0E8D
        private val MTK_BROM_PIDS = setOf(0x0003, 0x2000, 0x2001)

        // Samsung Download Mode
        private const val SAMSUNG_VID = 0x04E8
        private val SAMSUNG_DL_PIDS = setOf(0x6601, 0x685D)

        // Google/Android ADB & Fastboot
        private const val GOOGLE_VID = 0x18D1
        private const val ADB_PID = 0x4EE7
        private val FASTBOOT_PIDS = setOf(0xD00D, 0x4EE0)

        // Xiaomi special VIDs for diag/ADB
        private const val XIAOMI_VID = 0x2717
    }

    fun detect(): ProtocolDetectionResult {
        Log.i(TAG, "[PROTO] Starting protocol probe for VID=0x${"%04X".format(device.vendorId)} PID=0x${"%04X".format(device.productId)}")

        val ifaceDescriptions = mutableListOf<String>()
        var allIfacesMtp = true
        var hasVendorSpecIface = false
        var hasAdbIface = false
        var hasFastbootIface = false

        // Phase 1: Enumerate and classify all interfaces
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            val desc = "[PROTO] iface[$i]: class=0x${"%02X".format(iface.interfaceClass)},sub=0x${"%02X".format(iface.interfaceSubclass)},proto=0x${"%02X".format(iface.interfaceProtocol)}, eps=${iface.endpointCount}"
            ifaceDescriptions.add(desc)

            var hasBulkIn = false
            var hasBulkOut = false
            var tmpBulkIn: UsbEndpoint? = null
            var tmpBulkOut: UsbEndpoint? = null

            for (e in 0 until iface.endpointCount) {
                val ep = iface.getEndpoint(e)
                val epDesc = "  ep[$e]: ${dir(ep)}_${type(ep)} maxPkt=${ep.maxPacketSize}"
                ifaceDescriptions.add(epDesc)
                if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (ep.direction == UsbConstants.USB_DIR_IN) { hasBulkIn = true; tmpBulkIn = ep }
                    else { hasBulkOut = true; tmpBulkOut = ep }
                }
            }

            // Track interface types
            if (!isMtpClass(iface)) allIfacesMtp = false
            if (iface.interfaceClass == UsbConstants.USB_CLASS_VENDOR_SPEC) hasVendorSpecIface = true

            // Detect ADB interface signature: class=0xFF, sub=0x42, proto=0x01
            if (iface.interfaceClass == 0xFF && iface.interfaceSubclass == 0x42 && iface.interfaceProtocol == 0x01) {
                hasAdbIface = true
            }

            // Detect Fastboot interface signature: class=0xFF, sub=0x42, proto=0x03
            if (iface.interfaceClass == 0xFF && iface.interfaceSubclass == 0x42 && iface.interfaceProtocol == 0x03) {
                hasFastbootIface = true
            }

            // Select first vendor-spec or non-MTP interface with bulk pair for probing
            if (bulkIn == null && hasBulkIn && hasBulkOut && !isMtpClass(iface)) {
                bulkIn = tmpBulkIn
                bulkOut = tmpBulkOut
                probeInterfaceIndex = i
            }
            // Fallback: if we have no probe interface, take any bulk pair
            if (bulkIn == null && hasBulkIn && hasBulkOut) {
                bulkIn = tmpBulkIn
                bulkOut = tmpBulkOut
                probeInterfaceIndex = i
            }
        }

        val ifaceDump = ifaceDescriptions.joinToString(separator = "\n")
        Log.i(TAG, "[PROTO] Interface dump:\n$ifaceDump")

        // Phase 2: VID/PID fast-path classification
        val vidPidResult = classifyByVidPid()
        if (vidPidResult != null) {
            Log.i(TAG, "[PROTO] VID/PID match → $vidPidResult")
            return ProtocolDetectionResult(vidPidResult, ifaceDump, probeInterfaceIndex)
        }

        // Phase 3: Interface descriptor heuristics
        if (hasFastbootIface) {
            Log.i(TAG, "[PROTO] Fastboot interface descriptor matched")
            return ProtocolDetectionResult(DetectedProtocol.FASTBOOT, ifaceDump, probeInterfaceIndex)
        }
        if (hasAdbIface) {
            Log.i(TAG, "[PROTO] ADB interface descriptor matched")
            return ProtocolDetectionResult(DetectedProtocol.ADB, ifaceDump, probeInterfaceIndex)
        }

        // Phase 4: No bulk endpoints available → classify by interface types alone
        if (bulkIn == null || bulkOut == null) {
            val fallback = if (allIfacesMtp) DetectedProtocol.MTP_ONLY else DetectedProtocol.UNKNOWN
            Log.i(TAG, "[PROTO] No bulk endpoints for probing → classified as $fallback")
            return ProtocolDetectionResult(fallback, ifaceDump, probeInterfaceIndex)
        }

        // Phase 5: Active probing (with increased timeouts for slow USB controllers)
        // Claim the probe interface temporarily for active probing
        val probeIface = probeInterfaceIndex?.let { device.getInterface(it) }
        if (probeIface != null) {
            try { connection.claimInterface(probeIface, true) } catch (_: Exception) {}
        }

        try {
            // 5a. Fastboot probe (safe ASCII exchange)
            if (probeFastboot()) return ProtocolDetectionResult(DetectedProtocol.FASTBOOT, ifaceDump, probeInterfaceIndex)

            // 5b. MTK BROM probe (sync bytes)
            if (probeMtkBrom()) return ProtocolDetectionResult(DetectedProtocol.MTK_BROM, ifaceDump, probeInterfaceIndex)

            // 5c. Qualcomm Sahara hello (passive read)
            if (probeQualcommSahara()) return ProtocolDetectionResult(DetectedProtocol.QUALCOMM_EDL, ifaceDump, probeInterfaceIndex)
        } finally {
            // Release probe claim; the caller will re-claim after decision
            if (probeIface != null) {
                try { connection.releaseInterface(probeIface) } catch (_: Exception) {}
            }
        }

        // Phase 6: Fallback
        val fallback = when {
            allIfacesMtp -> DetectedProtocol.MTP_ONLY
            hasVendorSpecIface -> {
                // Has vendor-specific interfaces but probes failed → likely needs different USB mode
                Log.i(TAG, "[PROTO] Vendor-specific interfaces present but all probes failed → UNKNOWN (needs mode switch)")
                DetectedProtocol.UNKNOWN
            }
            else -> DetectedProtocol.UNKNOWN
        }
        Log.i(TAG, "[PROTO] All probes exhausted → classified as $fallback")
        return ProtocolDetectionResult(fallback, ifaceDump, probeInterfaceIndex)
    }

    /**
     * Fast VID/PID lookup. Returns non-null if the device matches a known protocol signature.
     */
    private fun classifyByVidPid(): DetectedProtocol? {
        val vid = device.vendorId
        val pid = device.productId

        // Qualcomm EDL 9008
        if (vid in QCOM_EDL_VIDS && pid == QCOM_EDL_PID) return DetectedProtocol.QUALCOMM_EDL

        // MTK BROM / Preloader
        if (vid == MTK_VID && pid in MTK_BROM_PIDS) return DetectedProtocol.MTK_BROM

        // Samsung Download
        if (vid == SAMSUNG_VID && pid in SAMSUNG_DL_PIDS) return DetectedProtocol.SAMSUNG_ODIN

        // Google Fastboot
        if (vid == GOOGLE_VID && pid in FASTBOOT_PIDS) return DetectedProtocol.FASTBOOT

        // Google ADB
        if (vid == GOOGLE_VID && pid == ADB_PID) return DetectedProtocol.ADB

        return null
    }

    private fun isMtpClass(iface: UsbInterface): Boolean {
        return iface.interfaceClass == UsbConstants.USB_CLASS_STILL_IMAGE ||
               iface.interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE
    }

    private fun dir(ep: UsbEndpoint): String = if (ep.direction == UsbConstants.USB_DIR_IN) "IN" else "OUT"
    private fun type(ep: UsbEndpoint): String = when (ep.type) {
        UsbConstants.USB_ENDPOINT_XFER_BULK -> "BULK"
        UsbConstants.USB_ENDPOINT_XFER_INT -> "INT"
        UsbConstants.USB_ENDPOINT_XFER_ISOC -> "ISO"
        else -> "CTRL"
    }

    private fun probeFastboot(): Boolean {
        try {
            val cmd = "getvar:version".toByteArray()
            val buffer = ByteArray(64)
            
            connection.bulkTransfer(bulkOut, cmd, cmd.size, 500)
            val len = connection.bulkTransfer(bulkIn, buffer, buffer.size, 500)
            if (len > 0) {
                val response = String(buffer, 0, len)
                Log.d(TAG, "[PROTO] Fastboot probe response: $response")
                if (response.startsWith("OKAY") || response.startsWith("FAIL")) return true
            }
        } catch (e: Exception) {
            Log.d(TAG, "[PROTO] Fastboot probe failed: ${e.message}")
        }
        return false
    }

    private fun probeMtkBrom(): Boolean {
        try {
            // MTK Start Cmd: A0 0A 50 05
            val cmd = byteArrayOf(0xA0.toByte(), 0x0A, 0x50, 0x05)
            val buffer = ByteArray(64)
            
            connection.bulkTransfer(bulkOut, cmd, cmd.size, 500)
            val len = connection.bulkTransfer(bulkIn, buffer, buffer.size, 500)
            
            if (len > 0) {
                Log.d(TAG, "[PROTO] MTK probe response len=$len first=0x${"%02X".format(buffer[0])}")
                if (buffer[0] == 0x5F.toByte() || buffer[0] == 0x00.toByte()) return true
            }
        } catch (e: Exception) {
            Log.d(TAG, "[PROTO] MTK probe failed: ${e.message}")
        }
        return false
    }

    private fun probeQualcommSahara(): Boolean {
        try {
            val buffer = ByteArray(1024)
            val len = connection.bulkTransfer(bulkIn, buffer, buffer.size, 500)
            if (len > 0) {
                 if (buffer[0] == 0x01.toByte()) {
                     Log.d(TAG, "[PROTO] Qualcomm Sahara probe detected HELLO (len=$len)")
                     return true
                 }
            }
        } catch (e: Exception) {
             Log.d(TAG, "[PROTO] Qualcomm probe failed: ${e.message}")
        }
        return false
    }
}
