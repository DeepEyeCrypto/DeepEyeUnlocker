package com.deepeye.otg.usb

import android.hardware.usb.UsbDevice
import com.deepeye.otg.data.device.DeviceProtocol
import com.deepeye.otg.data.device.ProtocolRouter
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * IosSessionCoordinator — High-level session dispatcher.
 * Routes physical USB attachments to protocol-specific handlers.
 */
@Singleton
class IosSessionCoordinator @Inject constructor(
    private val sessionCoordinator: SessionCoordinator,
    private val hardwareManager: HardwareManager
) {
    /**
     * Entry point for USB attachment routing.
     * Called from UsbLifecycleManager after basic device detection.
     */
    fun onDeviceAttached(device: UsbDevice) {
        val result = ProtocolRouter.route(
            vid = device.vendorId,
            pid = device.productId,
        )
        
        Timber.d("[ROUTER] vid=0x${device.vendorId.toString(16)} " +
                 "protocol=${result.protocol} " +
                 "confidence=${result.confidence}")

        when (result.protocol) {
            DeviceProtocol.MTK_V6       -> startMtkV6Session(device)
            DeviceProtocol.MTK_BROM     -> startMtkBromSession(device)
            DeviceProtocol.QC_EDL       -> startQcEdlSession(device)
            DeviceProtocol.SAMSUNG_ODIN -> startSamsungSession(device)
            DeviceProtocol.MTK_OR_QC    -> askUserToSelect(device)
            DeviceProtocol.UNKNOWN      -> showUnknownDevice(device)
        }
    }

    private fun startMtkV6Session(device: UsbDevice) {
        Timber.i("[SESSION] Starting MTK V6 (Dimensity) session for ${device.productName}")
        // Future: hardwareManager.performMtkV6AutoStart(device)
    }

    private fun startMtkBromSession(device: UsbDevice) {
        Timber.i("[SESSION] Starting MTK Classic BROM session for ${device.productName}")
        // Future: hardwareManager.performMtkBromAutoStart(device)
    }

    private fun startQcEdlSession(device: UsbDevice) {
        Timber.i("[SESSION] Starting Qualcomm EDL (Sahara/Firehose) session for ${device.productName}")
        // Future: hardwareManager.performQcomAutoStart(device)
    }

    private fun startSamsungSession(device: UsbDevice) {
        Timber.i("[SESSION] Starting Samsung ODIN session for ${device.productName}")
        // Future: hardwareManager.performSamsungAutoStart(device)
    }

    private fun askUserToSelect(device: UsbDevice) {
        Timber.w("[SESSION] Ambiguous protocol (MTK/QC) for ${device.productName}. Awaiting User choice.")
        // Future: sessionCoordinator.transition(ConnectionState.AwaitingAmbiguousSelection)
    }

    private fun showUnknownDevice(device: UsbDevice) {
        Timber.w("[SESSION] Unknown device 0x${device.vendorId.toString(16)}. Showing generic support.")
        // Future: sessionCoordinator.transition(ConnectionState.UnsupportedDevice)
    }
}
