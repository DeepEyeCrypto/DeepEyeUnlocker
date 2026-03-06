package com.deepeye.otg.testing

import android.hardware.usb.*
import android.util.Log
import com.deepeye.otg.data.ConnectionMode
import com.deepeye.otg.data.UsbDeviceDatabase
import com.deepeye.otg.usb.UsbSessionManager
import com.deepeye.otg.usb.UsbLogger
import kotlinx.coroutines.*

/**
 * PRODUCTION OTG TESTING HARNESS
 * Used for stability stress tests and mode-switch verification.
 */
object OtgTestHelper {

    private const val TAG = "DeepEye-Test"

    /**
     * STRESS TEST: 10-cycle rapid plug/unplug simulation.
     * Ensures resource cleanup and re-attach stability.
     */
    suspend fun runStressTest(
        session: UsbSessionManager,
        mockDevice: UsbDevice
    ) = coroutineScope {
        UsbLogger.info(TAG, "Starting 10-Cycle USB Stress Test...")
        
        repeat(10) { iteration ->
            UsbLogger.debug(TAG, "Stress Cycle #$iteration - ATTACHING")
            session.onDeviceAttached(mockDevice)
            delay(150)
            
            UsbLogger.debug(TAG, "Stress Cycle #$iteration - DETACHING")
            session.onDeviceDetached(mockDevice)
            delay(150)

            // Verify clean state
            if (session.isConnected()) {
                UsbLogger.error(TAG, "Stability FAILURE: iteration $iteration leaked a connection!")
                return@coroutineScope
            }
        }
        
        UsbLogger.info(TAG, "Stress test COMPLETED. No resource leaks detected.")
        
        // Final connectivity check
        UsbLogger.info(TAG, "Performing Final Handshake Check...")
        session.onDeviceAttached(mockDevice)
        delay(200)
        
        if (session.isConnected()) {
            UsbLogger.info(TAG, "STABILITY STATUS: PASS")
        } else {
            UsbLogger.error(TAG, "STABILITY STATUS: FAIL (Final handshake failed after stress)")
        }
    }

    /**
     * Mode Matrix Validation:
     * Validates that VID:PID database correctly maps to the modes chosen by the pro tools.
     */
    fun validateModeMatrix() {
        val testCases = listOf(
            Triple(0x05C6, 0x9008, ConnectionMode.EDL),
            Triple(0x0E8D, 0x0003, ConnectionMode.BROM),
            Triple(0x2717, 0xFF48, ConnectionMode.ADB),
            Triple(0x04E8, 0x685E, ConnectionMode.FASTBOOT), // ODIN
            Triple(0x18D1, 0x4EE0, ConnectionMode.FASTBOOT)  // Google
        )

        testCases.forEach { (vid, pid, expected) ->
            val detected = UsbDeviceDatabase.detect(vid, pid)?.mode 
                ?: UsbDeviceDatabase.detectByVendor(vid)
            
            if (detected == expected) {
                UsbLogger.info(TAG, "Matrix Check: 0x${vid.toString(16)}:0x${pid.toString(16)} -> $expected [OK]")
            } else {
                UsbLogger.error(TAG, "Matrix FAILURE: 0x${vid.toString(16)}:0x${pid.toString(16)} -> Expected $expected but got $detected")
            }
        }
    }
}

