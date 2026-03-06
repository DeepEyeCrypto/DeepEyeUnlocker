package com.deepeye.otg.usb

import com.deepeye.otg.data.ConnectionMode

/**
 * Timeout values tuned per USB protocol.
 * Rule: NEVER use 0 (infinite block). NEVER use < 500 for data ops.
 * All values in milliseconds.
 */
object UsbTimeoutConstants {

    // ── Control / status pings ───────────────────────────────
    const val CONTROL_PING_MS       = 1_000   // GET_STATUS probe

    // ── ADB protocol ─────────────────────────────────────────
    const val ADB_HANDSHAKE_MS      = 3_000
    const val ADB_COMMAND_MS        = 10_000
    const val ADB_SHELL_STREAM_MS   = 30_000  // streaming shell

    // ── Fastboot protocol ─────────────────────────────────────
    const val FASTBOOT_COMMAND_MS   = 5_000
    const val FASTBOOT_FLASH_MS     = 15_000  // per 16KB chunk
    const val FASTBOOT_ERASE_MS     = 30_000  // partition erase

    // ── EDL / Sahara (Qualcomm 9008) ─────────────────────────
    const val EDL_HELLO_MS          = 3_000
    const val EDL_TRANSFER_MS       = 10_000
    const val EDL_PROG_FIREHOSE_MS  = 20_000

    // ── BROM / Preloader (MediaTek) ───────────────────────────
    const val BROM_SYNC_MS          = 2_000
    const val BROM_DA_SEND_MS       = 15_000
    const val BROM_FLASH_CHUNK_MS   = 10_000

    // ── DIAG (Qualcomm diagnostic) ────────────────────────────
    const val DIAG_COMMAND_MS       = 2_000
    const val DIAG_READ_MS          = 3_000

    // ── MTP ───────────────────────────────────────────────────
    const val MTP_COMMAND_MS        = 5_000
    const val MTP_DATA_MS           = 30_000

    /**
     * Get write timeout for current operation type.
     */
    fun writeTimeout(mode: ConnectionMode, isFlash: Boolean = false): Int =
        when (mode) {
            ConnectionMode.ADB       -> if (isFlash) ADB_SHELL_STREAM_MS else ADB_COMMAND_MS
            ConnectionMode.FASTBOOT  -> if (isFlash) FASTBOOT_FLASH_MS else FASTBOOT_COMMAND_MS
            ConnectionMode.EDL       -> if (isFlash) EDL_PROG_FIREHOSE_MS else EDL_TRANSFER_MS
            ConnectionMode.BROM,
            ConnectionMode.PRELOADER -> if (isFlash) BROM_DA_SEND_MS else BROM_SYNC_MS
            ConnectionMode.DIAG      -> DIAG_COMMAND_MS
            ConnectionMode.MTP       -> if (isFlash) MTP_DATA_MS else MTP_COMMAND_MS
            else                     -> 5_000
        }

    fun readTimeout(mode: ConnectionMode, isFlash: Boolean = false): Int =
        when (mode) {
            ConnectionMode.ADB       -> if (isFlash) ADB_SHELL_STREAM_MS else ADB_COMMAND_MS
            ConnectionMode.FASTBOOT  -> if (isFlash) FASTBOOT_FLASH_MS else FASTBOOT_COMMAND_MS
            ConnectionMode.EDL       -> EDL_TRANSFER_MS
            ConnectionMode.BROM,
            ConnectionMode.PRELOADER -> BROM_FLASH_CHUNK_MS
            ConnectionMode.DIAG      -> DIAG_READ_MS
            ConnectionMode.MTP       -> if (isFlash) MTP_DATA_MS else MTP_COMMAND_MS
            else                     -> 5_000
        }
}
