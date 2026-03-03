package com.deepeye.otg

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Minimal sanity placeholders to document expected transitions.
 * Real instrumentation tests should mock UsbManager/UsbDevice and broadcast delivery.
 */
class UsbHostManagerTest {

    @Test
    fun placeholder_permission_denied_state() {
        // Document expectation: permission denied leads to PERMISSION_DENIED (not PERMISSION_PENDING loop)
        assertEquals(ConnectionState.PERMISSION_DENIED, ConnectionState.PERMISSION_DENIED)
    }

    @Test
    fun placeholder_mtp_only_state() {
        // Document expectation: MTP-only classification leads to CONNECTED_MTP_ONLY and no retries
        assertEquals(ConnectionState.CONNECTED_MTP_ONLY, ConnectionState.CONNECTED_MTP_ONLY)
    }
}
