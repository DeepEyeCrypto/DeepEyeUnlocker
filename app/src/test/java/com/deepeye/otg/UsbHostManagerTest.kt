package com.deepeye.otg

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for DeepEye OTG USB connection layer.
 *
 * These tests verify:
 * 1. State machine transition rules (no illegal jumps)
 * 2. Permission flow contracts
 * 3. MTP-only classification behavior
 * 4. Re-enumeration handling
 * 5. Error throttling
 *
 * NOTE: Full integration tests require Android instrumentation with mocked UsbManager.
 * These tests validate the state machine logic and protocol classification independently.
 */
class UsbHostManagerTest {

    // ═══════════════════════════════════════════════════════════════
    // 1. PERMISSION GRANTED FLOW
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun permission_granted_flow_transitions_correctly() {
        // Simulate: DISCONNECTED → DEVICE_FOUND → PERMISSION_PENDING → USB_OPEN → CONNECTED_PROTOCOL_DETECT → CONNECTED
        val states = listOf(
            ConnectionState.DISCONNECTED to ConnectionState.DEVICE_FOUND,
            ConnectionState.DEVICE_FOUND to ConnectionState.PERMISSION_PENDING,
            ConnectionState.PERMISSION_PENDING to ConnectionState.USB_OPEN,
            ConnectionState.USB_OPEN to ConnectionState.CONNECTED_PROTOCOL_DETECT,
            ConnectionState.CONNECTED_PROTOCOL_DETECT to ConnectionState.NATIVE_INITIALIZING,
            ConnectionState.NATIVE_INITIALIZING to ConnectionState.CONNECTED
        )
        for ((from, to) in states) {
            assertTrue(
                "Transition $from → $to should be allowed",
                from.canTransitionTo(to)
            )
        }
    }

    @Test
    fun no_transition_back_to_permission_pending_after_open() {
        // KEY FIX VALIDATION: After USB_OPEN, CONNECTED, or NATIVE_INITIALIZING,
        // we must NEVER go back to PERMISSION_PENDING (root cause of the bug)
        val postOpenStates = listOf(
            ConnectionState.USB_OPEN,
            ConnectionState.CONNECTED_PROTOCOL_DETECT,
            ConnectionState.NATIVE_INITIALIZING,
            ConnectionState.CONNECTED,
            ConnectionState.CONNECTED_MTP_ONLY
        )
        for (state in postOpenStates) {
            assertFalse(
                "Transition $state → PERMISSION_PENDING must be BLOCKED",
                state.canTransitionTo(ConnectionState.PERMISSION_PENDING)
            )
        }
    }

    @Test
    fun permission_already_granted_skips_pending() {
        // When hasPermission() is true, we go directly from DEVICE_FOUND to USB_OPEN
        assertTrue(
            ConnectionState.DEVICE_FOUND.canTransitionTo(ConnectionState.USB_OPEN)
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. PERMISSION DENIED FLOW
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun permission_denied_flow_transitions_correctly() {
        assertTrue(
            ConnectionState.PERMISSION_PENDING.canTransitionTo(ConnectionState.PERMISSION_DENIED)
        )
    }

    @Test
    fun permission_denied_does_not_allow_usb_open() {
        assertFalse(
            "PERMISSION_DENIED → USB_OPEN must be blocked (no opening without permission!)",
            ConnectionState.PERMISSION_DENIED.canTransitionTo(ConnectionState.USB_OPEN)
        )
    }

    @Test
    fun permission_denied_allows_retry_on_replug() {
        // Re-plug triggers DEVICE_FOUND which can then re-request permission
        assertTrue(
            ConnectionState.PERMISSION_DENIED.canTransitionTo(ConnectionState.DEVICE_FOUND)
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. MTP-ONLY DEVICE
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun mtp_only_state_does_not_allow_operations() {
        assertFalse(
            "MTP_ONLY state must NOT allow flash/FRP/IMEI operations",
            ConnectionState.CONNECTED_MTP_ONLY.canExecuteOperations()
        )
    }

    @Test
    fun mtp_only_transitions_from_protocol_detect() {
        assertTrue(
            ConnectionState.CONNECTED_PROTOCOL_DETECT.canTransitionTo(ConnectionState.CONNECTED_MTP_ONLY)
        )
    }

    @Test
    fun mtp_only_does_not_re_request_permission() {
        // MTP_ONLY should not bounce back to PERMISSION_PENDING (the original bug)
        assertFalse(
            ConnectionState.CONNECTED_MTP_ONLY.canTransitionTo(ConnectionState.PERMISSION_PENDING)
        )
    }

    @Test
    fun only_connected_state_allows_operations() {
        for (state in ConnectionState.values()) {
            if (state == ConnectionState.CONNECTED) {
                assertTrue("CONNECTED must allow operations", state.canExecuteOperations())
            } else {
                assertFalse("$state must NOT allow operations", state.canExecuteOperations())
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. RE-ENUMERATION
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun re_enumeration_transitions_from_connected_to_device_found() {
        // When device re-enumerates (VID/PID same, deviceId changed),
        // we close the old connection and restart from DEVICE_FOUND
        assertTrue(
            ConnectionState.CONNECTED.canTransitionTo(ConnectionState.DEVICE_FOUND)
        )
        assertTrue(
            ConnectionState.CONNECTED_MTP_ONLY.canTransitionTo(ConnectionState.DEVICE_FOUND)
        )
    }

    @Test
    fun any_state_can_go_to_disconnected() {
        // Physical detach must always work
        for (state in ConnectionState.values()) {
            assertTrue(
                "$state must allow transition to DISCONNECTED",
                state.canTransitionTo(ConnectionState.DISCONNECTED)
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 5. ERROR HANDLING
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun any_state_can_go_to_error() {
        for (state in ConnectionState.values()) {
            assertTrue(
                "$state must allow transition to ERROR",
                state.canTransitionTo(ConnectionState.ERROR)
            )
        }
    }

    @Test
    fun error_allows_retry_on_replug() {
        assertTrue(
            ConnectionState.ERROR.canTransitionTo(ConnectionState.DEVICE_FOUND)
        )
    }

    @Test
    fun error_does_not_allow_skip_to_connected() {
        assertFalse(
            "ERROR → CONNECTED must be blocked (must go through full flow)",
            ConnectionState.ERROR.canTransitionTo(ConnectionState.CONNECTED)
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // Badge & Display
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun all_states_have_badge_text() {
        for (state in ConnectionState.values()) {
            val badge = state.getBadgeText()
            assertTrue("$state must have non-empty badge text", badge.isNotEmpty())
            assertTrue("Badge text must start with ●", badge.startsWith("●"))
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Protocol Detection Enum
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun protocol_enum_includes_all_expected_types() {
        val expected = setOf(
            "UNKNOWN", "QUALCOMM_EDL", "MTK_BROM", "MTK_PRELOADER",
            "FASTBOOT", "SAMSUNG_ODIN", "UNISOC_FDL", "ADB", "MTP_ONLY"
        )
        val actual = DetectedProtocol.values().map { it.name }.toSet()
        assertEquals("DetectedProtocol enum must contain all expected protocols", expected, actual)
    }
}
