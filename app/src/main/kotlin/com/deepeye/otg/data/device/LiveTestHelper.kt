package com.deepeye.otg.data.device

import android.hardware.usb.UsbDevice
import timber.log.Timber

/**
 * LiveTestHelper — verifies ProtocolRouter on real USB attach
 * Call from IosSessionCoordinator.onDeviceAttached()
 */
object LiveTestHelper {

    fun onDeviceAttached(device: UsbDevice): RoutingResult {
        val vid = device.vendorId
        val pid = device.productId

        Timber.d("[LIVE_TEST] ════════════════════════════════")
        Timber.d("[LIVE_TEST] USB DEVICE ATTACHED")
        Timber.d("[LIVE_TEST] VID=0x${vid.toString(16).uppercase()} " +
                 "(${vid}) PID=0x${pid.toString(16).uppercase()} (${pid})")
        Timber.d("[LIVE_TEST] name=${device.deviceName}")
        Timber.d("[LIVE_TEST] manufacturer=${device.manufacturerName}")
        Timber.d("[LIVE_TEST] product=${device.productName}")
        Timber.d("[LIVE_TEST] interfaces=${device.interfaceCount}")

        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            Timber.d("[LIVE_TEST] IF#$i cls=${iface.interfaceClass} " +
                     "sub=${iface.interfaceSubclass} " +
                     "proto=${iface.interfaceProtocol} " +
                     "eps=${iface.endpointCount}")
        }

        val result = ProtocolRouter.route(
            vid   = vid,
            pid   = pid,
            brand = device.manufacturerName,
            model = device.productName,
        )

        Timber.d("[LIVE_TEST] ────────────────────────────────")
        Timber.d("[LIVE_TEST] PROTOCOL  → ${result.protocol}")
        Timber.d("[LIVE_TEST] CONFIDENCE→ ${result.confidence}")
        Timber.d("[LIVE_TEST] REASON    → ${result.reason}")
        Timber.d("[LIVE_TEST] ════════════════════════════════")

        return result
    }

    /** Adb logcat filter command for live testing **/
    const val LOGCAT_FILTER = "adb logcat -s LIVE_TEST:D ROUTER:D MTK_V6:D QC_EDL:D SAMSUNG:D"
}

typealias RoutingResult = ProtocolRouter.RoutingResult
