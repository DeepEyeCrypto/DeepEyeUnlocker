package com.deepeye.otg.usb

import android.hardware.usb.UsbConstants
import com.deepeye.otg.domain.models.DeviceMode
import com.deepeye.otg.domain.models.ProtocolFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Unit tests for [ProtocolDetector].
 *
 * These tests focus on:
 * - T01/T02/T03: Explicit low-level and ADB/MTP mode detection.
 * - T04/T07: UNKNOWN handling and known-vendor / unknown-PID behavior.
 */
class ProtocolDetectorTest {

    private val detector = ProtocolDetector()

    // ────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────

    private fun emptySnapshot(
        vid: Int = 0x0000,
        pid: Int = 0x0000,
        deviceClass: Int = 0x00,
        deviceSubclass: Int = 0x00,
        deviceProtocol: Int = 0x00,
        manufacturer: String? = null,
        product: String? = null,
        interfaces: List<UsbInterfaceSnapshot> = emptyList()
    ): UsbDescriptorSnapshot =
        UsbDescriptorSnapshot(
            vendorId = vid,
            productId = pid,
            deviceClass = deviceClass,
            deviceSubclass = deviceSubclass,
            deviceProtocol = deviceProtocol,
            manufacturerName = manufacturer,
            productName = product,
            interfaceCount = interfaces.size,
            interfaces = interfaces
        )

    private fun adbInterface(
        hasBulkIn: Boolean = true,
        hasBulkOut: Boolean = true
    ): UsbInterfaceSnapshot {
        val endpoints = mutableListOf<UsbEndpointSnapshot>()
        if (hasBulkIn) {
            endpoints += UsbEndpointSnapshot(
                address = 0x81,
                type = UsbConstants.USB_ENDPOINT_XFER_BULK,
                direction = UsbConstants.USB_DIR_IN,
                maxPacketSize = 512
            )
        }
        if (hasBulkOut) {
            endpoints += UsbEndpointSnapshot(
                address = 0x02,
                type = UsbConstants.USB_ENDPOINT_XFER_BULK,
                direction = UsbConstants.USB_DIR_OUT,
                maxPacketSize = 512
            )
        }
        return UsbInterfaceSnapshot(
            id = 0,
            interfaceClass = 0xFF,
            interfaceSubclass = 0x42,
            interfaceProtocol = 0x01,
            endpointCount = endpoints.size,
            endpoints = endpoints
        )
    }

    private fun mtpInterface(): UsbInterfaceSnapshot =
        UsbInterfaceSnapshot(
            id = 0,
            interfaceClass = UsbConstants.USB_CLASS_MASS_STORAGE,
            interfaceSubclass = 0,
            interfaceProtocol = 0,
            endpointCount = 0,
            endpoints = emptyList()
        )

    // ────────────────────────────────────────────────────────────────
    // T01 — Low-level modes: Apple DFU / Recovery, BROM / EDL
    // ────────────────────────────────────────────────────────────────

    @Test
    fun `apple_dfu_pid_detected_as_APPLE_DFU`() {
        val snapshot = emptySnapshot(vid = 0x05AC, pid = 0x1227)
        val result = detector.detect(snapshot)
        assertEquals(DeviceMode.APPLE_DFU, result.deviceMode)
        assertEquals(ProtocolFamily.APPLE_DFU, result.protocolFamily)
        assertEquals(100, result.confidence)
    }

    @Test
    fun `apple_recovery_pid_detected_as_APPLE_RECOVERY`() {
        val snapshot = emptySnapshot(vid = 0x05AC, pid = 0x1281)
        val result = detector.detect(snapshot)
        assertEquals(DeviceMode.APPLE_RECOVERY, result.deviceMode)
        assertEquals(ProtocolFamily.APPLE_RECOVERY, result.protocolFamily)
        assertEquals(100, result.confidence)
    }

    @Test
    fun `unisoc_fdl_detected_as_UNISOC_FDL`() {
        val snapshot = emptySnapshot(vid = 0x1782, pid = 0x4D00)
        val result = detector.detect(snapshot)
        assertEquals(DeviceMode.UNISOC_FDL, result.deviceMode)
        assertEquals(ProtocolFamily.UNISOC, result.protocolFamily)
        assertEquals(100, result.confidence)
    }

    // ────────────────────────────────────────────────────────────────
    // T01 — Low-level modes: BROM / EDL
    // ────────────────────────────────────────────────────────────────

    @Test
    fun `mtk brom vid_pid detected as MTK_BROM`() {
        val snapshot = emptySnapshot(vid = 0x0E8D, pid = 0x0003)
        val result = detector.detect(snapshot)

        assertEquals(DeviceMode.MTK_BROM, result.deviceMode)
        assertEquals(ProtocolFamily.BROM, result.protocolFamily)
        assertEquals(100, result.confidence)
    }

    @Test
    fun `qualcomm edl vid_pid detected as QC_EDL`() {
        val snapshot = emptySnapshot(vid = 0x05C6, pid = 0x9008)
        val result = detector.detect(snapshot)

        assertEquals(DeviceMode.QC_EDL, result.deviceMode)
        assertEquals(ProtocolFamily.EDL, result.protocolFamily)
        assertEquals(100, result.confidence)
    }

    // ────────────────────────────────────────────────────────────────
    // T02 — Explicit ADB: FF/42/01 + bulk in+out
    // ────────────────────────────────────────────────────────────────

    @Test
    fun `explicit adb interface with bulk pair detected as ADB`() {
        val snapshot = emptySnapshot(
            vid = 0x18D1, // Google
            pid = 0x4EE7,
            interfaces = listOf(adbInterface(hasBulkIn = true, hasBulkOut = true))
        )

        val result = detector.detect(snapshot)
        assertEquals(DeviceMode.ADB, result.deviceMode)
        assertEquals(ProtocolFamily.ADB, result.protocolFamily)
    }

    @Test
    fun `adb signature without bulk pair does not classify as ADB`() {
        val snapshot = emptySnapshot(
            interfaces = listOf(adbInterface(hasBulkIn = true, hasBulkOut = false))
        )

        val result = detector.detect(snapshot)
        assertNotEquals("Should not be classified as ADB without bulk in+out", DeviceMode.ADB, result.deviceMode)
        assertNotEquals(ProtocolFamily.ADB, result.protocolFamily)
    }

    // ────────────────────────────────────────────────────────────────
    // T03 — MTP-only devices
    // ────────────────────────────────────────────────────────────────

    @Test
    fun `mtp_like_interface_class_detected_as_MTP_ONLY`() {
        val snapshot = emptySnapshot(
            interfaces = listOf(mtpInterface())
        )

        val result = detector.detect(snapshot)
        assertEquals(DeviceMode.MTP_ONLY, result.deviceMode)
        assertEquals(ProtocolFamily.MTP, result.protocolFamily)
    }

    // ────────────────────────────────────────────────────────────────
    // T04 — Unknown USB device → UNKNOWN (never ADB fallback)
    // ────────────────────────────────────────────────────────────────

    @Test
    fun `unknown_vid_pid_without_signatures_returns_UNKNOWN`() {
        val snapshot = emptySnapshot(
            vid = 0x1234,
            pid = 0x5678
        )

        val result = detector.detect(snapshot)
        assertEquals(DeviceMode.UNKNOWN, result.deviceMode)
        assertEquals(ProtocolFamily.UNKNOWN, result.protocolFamily)
        assertEquals(0, result.confidence)
    }

    // ────────────────────────────────────────────────────────────────
    // T07 — Known vendor VID, unknown PID → UNKNOWN + warning path
    // ────────────────────────────────────────────────────────────────

    @Test
    fun `mtk_known_vid_unknown_pid_returns_UNKNOWN`() {
        val snapshot = emptySnapshot(
            vid = 0x0E8D,
            pid = 0x1234
        )

        val result = detector.detect(snapshot)
        assertEquals(DeviceMode.UNKNOWN, result.deviceMode)
        assertEquals(ProtocolFamily.UNKNOWN, result.protocolFamily)
    }

    @Test
    fun `qualcomm_known_vid_unknown_pid_returns_UNKNOWN`() {
        val snapshot = emptySnapshot(
            vid = 0x05C6,
            pid = 0x1234
        )

        val result = detector.detect(snapshot)
        assertEquals(DeviceMode.UNKNOWN, result.deviceMode)
        assertEquals(ProtocolFamily.UNKNOWN, result.protocolFamily)
    }

    @Test
    fun `samsung_known_vid_unknown_pid_returns_UNKNOWN`() {
        val snapshot = emptySnapshot(
            vid = 0x04E8,
            pid = 0x1234,
            manufacturer = "Samsung",
            product = "Unknown mode"
        )

        val result = detector.detect(snapshot)
        assertEquals(DeviceMode.UNKNOWN, result.deviceMode)
        assertEquals(ProtocolFamily.UNKNOWN, result.protocolFamily)
    }

    // ────────────────────────────────────────────────────────────────
    // Odin / Fastboot heuristics (VID-gated, no pure string fallback)
    // ────────────────────────────────────────────────────────────────

    @Test
    fun `samsung_odin_strict_pid_detects_odin`() {
        val snapshot = emptySnapshot(
            vid = 0x04E8,
            pid = 0x685D,
            manufacturer = "Samsung",
            product = "Samsung Download Mode"
        )

        val result = detector.detect(snapshot)
        assertEquals(DeviceMode.SAMSUNG_ODIN, result.deviceMode)
        assertEquals(ProtocolFamily.ODIN, result.protocolFamily)
    }

    @Test
    fun `samsung_odin_text_heuristic_requires_samsung_vid`() {
        val samsungSnapshot = emptySnapshot(
            vid = 0x04E8,
            pid = 0x0000,
            manufacturer = "Samsung",
            product = "Odin Download Mode"
        )
        val otherSnapshot = emptySnapshot(
            vid = 0x1234,
            pid = 0x0000,
            manufacturer = "Samsung",
            product = "Odin Download Mode"
        )

        val samsungResult = detector.detect(samsungSnapshot)
        val otherResult = detector.detect(otherSnapshot)

        assertEquals(DeviceMode.SAMSUNG_ODIN, samsungResult.deviceMode)
        assertEquals(ProtocolFamily.ODIN, samsungResult.protocolFamily)

        // Non-Samsung VID must not classify as Odin purely from strings
        assertEquals(DeviceMode.UNKNOWN, otherResult.deviceMode)
        assertEquals(ProtocolFamily.UNKNOWN, otherResult.protocolFamily)
    }

    @Test
    fun `fastboot_text_heuristic_requires_vendor_specific_interface`() {
        val fastbootInterface = UsbInterfaceSnapshot(
            id = 0,
            interfaceClass = 0xFF,
            interfaceSubclass = 0x00,
            interfaceProtocol = 0x00,
            endpointCount = 0,
            endpoints = emptyList()
        )

        val withVendorSpecific = emptySnapshot(
            manufacturer = "TestVendor",
            product = "fastboot device",
            interfaces = listOf(fastbootInterface)
        )
        val withoutVendorSpecific = emptySnapshot(
            manufacturer = "TestVendor",
            product = "fastboot device",
            interfaces = emptyList()
        )

        val withResult = detector.detect(withVendorSpecific)
        val withoutResult = detector.detect(withoutVendorSpecific)

        assertEquals(DeviceMode.FASTBOOT, withResult.deviceMode)
        assertEquals(ProtocolFamily.FASTBOOT, withResult.protocolFamily)

        // No vendor-specific interface → must stay UNKNOWN, even if text says "fastboot"
        assertEquals(DeviceMode.UNKNOWN, withoutResult.deviceMode)
        assertEquals(ProtocolFamily.UNKNOWN, withoutResult.protocolFamily)
    }
}

