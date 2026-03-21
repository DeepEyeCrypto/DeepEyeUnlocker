package com.deepeye.otg.usb

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.deepeye.otg.usb.UsbLifecycleState.Error
import com.deepeye.otg.usb.UsbLifecycleState.PermissionDenied
import com.deepeye.otg.usb.UsbLifecycleState.PermissionPending
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`

/**
 * Unit tests for [UsbLifecycleManager] focusing on:
 *
 * - Permission timeout behavior (T10).
 * - Permission result handling and timeout cancellation (part of T06).
 *
 * These tests use a TestScope so that the 10s timeout can be advanced
 * without real wall-clock delays.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UsbLifecycleManagerTest {

    private fun mockUsbDevice(
        vid: Int = 0x18D1,
        pid: Int = 0x4EE7,
        deviceId: Int = 1
    ): UsbDevice {
        val device = Mockito.mock(UsbDevice::class.java)
        `when`(device.vendorId).thenReturn(vid)
        `when`(device.productId).thenReturn(pid)
        `when`(device.deviceId).thenReturn(deviceId)
        `when`(device.deviceClass).thenReturn(0)
        `when`(device.deviceSubclass).thenReturn(0)
        `when`(device.deviceProtocol).thenReturn(0)
        `when`(device.manufacturerName).thenReturn("TestVendor")
        `when`(device.productName).thenReturn("TestProduct")
        `when`(device.interfaceCount).thenReturn(0)
        return device
    }

    @Test
    fun permission_request_times_out_after_10_seconds() = runTest {
        val context = Mockito.mock(Context::class.java)
        val usbManager = Mockito.mock(UsbManager::class.java)
        val device = mockUsbDevice()

        `when`(usbManager.hasPermission(device)).thenReturn(false)

        val lifecycle = UsbLifecycleManager(
            context = context,
            usbManager = usbManager,
            scope = this,
            coordinator = SessionCoordinator()
        )

        lifecycle.onDeviceAttached(device)
        runCurrent()

        // We should now be pending permission
        val pendingState = lifecycle.state.value
        assertTrue(pendingState is PermissionPending)

        // Advance virtual time by 10 seconds to trigger timeout
        advanceTimeBy(10_000L)
        runCurrent()

        val stateAfterTimeout = lifecycle.state.value
        assertTrue(
            "State after 10s permission timeout must be Error",
            stateAfterTimeout is Error
        )
    }

    @Test
    fun permission_denied_cancels_timeout_and_sets_permission_denied_state() = runTest {
        val context = Mockito.mock(Context::class.java)
        val usbManager = Mockito.mock(UsbManager::class.java)
        val device = mockUsbDevice(deviceId = 2)

        `when`(usbManager.hasPermission(device)).thenReturn(false)

        val lifecycle = UsbLifecycleManager(
            context = context,
            usbManager = usbManager,
            scope = this,
            coordinator = SessionCoordinator()
        )

        lifecycle.onDeviceAttached(device)
        runCurrent()

        val pendingState = lifecycle.state.value
        assertTrue(pendingState is PermissionPending)

        // Simulate explicit permission denial before timeout fires
        lifecycle.onPermissionResult(device, granted = false)
        runCurrent()

        val deniedState = lifecycle.state.value
        assertTrue(deniedState is PermissionDenied)

        // Even after advancing time beyond the timeout, state must remain PermissionDenied
        advanceTimeBy(10_000L)
        runCurrent()

        val finalState = lifecycle.state.value
        assertTrue(finalState is PermissionDenied)
    }
}

