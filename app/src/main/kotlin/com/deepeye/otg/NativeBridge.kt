package com.deepeye.otg

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * JNI bridge to deepeye_core (C++ via NDK).
 *
 * All methods run on Dispatchers.IO — callers must ensure this.
 * The `handle` parameter is a pointer returned by [initCore].
 *
 * ⚠ loadLibrary is async — call [loadAsync] on IO thread at startup.
 *   Never call System.loadLibrary on the main thread.
 */
object NativeBridge {
    private const val TAG = "NativeBridge"
    private val loadMutex = Mutex()

    @Volatile var loaded = false
        private set

    // ── Async library load — MUST be called on Dispatchers.IO ───
    suspend fun loadAsync() {
        if (loaded) return

        loadMutex.withLock {
            if (loaded) return

            try {
                withContext(Dispatchers.IO) {
                    System.loadLibrary("deepeye_core")
                }
                loaded = true
                Log.i(TAG, "deepeye_core loaded OK")
            } catch (e: UnsatisfiedLinkError) {
                loaded = false
                Log.e(TAG, "deepeye_core FAILED: ${e.message}")
            }
        }
    }

    fun isLoaded() = loaded

    // ── Lifecycle ────────────────────────────────────────────────
    /** Open transport on the given USB FD. Returns handle (>0) or 0 on failure. */
    external fun initCore(fd: Int, vid: Int, pid: Int): Long

    /** Perform protocol handshake and identify device type string. */
    external fun identifyDevice(handle: Long): String

    /** Close transport and free handle. Must be called in finally block. */
    external fun closeCore(handle: Long)

    // ── Partition Operations ─────────────────────────────────────
    /** Read GPT/PMT/PIT partition table. Returns ["name (size MB)", ...] */
    external fun getPartitions(handle: Long): Array<String>

    /** Dump a single partition to a file on disk. Returns true on success. */
    external fun readPartition(handle: Long, name: String, outPath: String): Boolean

    /** Write (flash) a file to a named partition. Returns true on success. */
    external fun writePartition(handle: Long, name: String, inPath: String): Boolean

    /** Erase a named partition. Returns true on success. */
    external fun erasePartition(handle: Long, name: String): Boolean

    // ── MTK-Specific ─────────────────────────────────────────────
    /** Inject Download Agent (DA) binary. Required before most MTK operations. */
    external fun injectDa(handle: Long, daData: ByteArray): Boolean

    /** Read MTK NVRAM data (IMEI, calibration). Returns raw bytes or empty. */
    external fun readNvram(handle: Long, item: Int): ByteArray

    /** Write MTK NVRAM item. Returns true on success. */
    external fun writeNvram(handle: Long, item: Int, data: ByteArray): Boolean

    /** Enter MTK MetaMode (for FRP bypass flows). Returns true if mode-switch succeeded. */
    external fun enterMetaMode(handle: Long): Boolean

    /** Read MTK seccfg partition (lock state flags). Returns raw bytes. */
    external fun readSeccfg(handle: Long): ByteArray

    /** Write MTK seccfg (bootloader unlock/relock). Returns true on success. */
    external fun writeSeccfg(handle: Long, data: ByteArray): Boolean

    // ── Qualcomm-Specific ────────────────────────────────────────
    /** Perform Sahara handshake and load Firehose programmer. */
    external fun saharaHandshake(handle: Long, programmerPath: String): Boolean

    /** Send Firehose XML command. Returns response XML string. */
    external fun firehoseCommand(handle: Long, xmlCommand: String): String

    /** Read QC NV item (e.g., NV#550 for IMEI). Returns raw bytes. */
    external fun readQcNv(handle: Long, nvItem: Int): ByteArray

    /** Write QC NV item. Returns true on success. */
    external fun writeQcNv(handle: Long, nvItem: Int, data: ByteArray): Boolean

    /** Send diag command bytes. Returns response bytes. */
    external fun diagCommand(handle: Long, cmd: ByteArray): ByteArray

    // ── Samsung-Specific ─────────────────────────────────────────
    /** Perform Odin handshake. Returns true if session established. */
    external fun odinHandshake(handle: Long): Boolean

    /** Read Samsung PIT (Partition Information Table). Returns PIT data bytes. */
    external fun readPit(handle: Long): ByteArray

    /** Flash a TAR.MD5 or raw image via Odin protocol. Returns true on success. */
    external fun odinFlash(handle: Long, partName: String, imagePath: String): Boolean

    /** Read Samsung EFS (nv_data.bin). Returns raw bytes. */
    external fun readEfs(handle: Long): ByteArray

    /** Write Samsung EFS. Returns true on success. */
    external fun writeEfs(handle: Long, data: ByteArray): Boolean

    // ── UniSoc-Specific ──────────────────────────────────────────
    /** Perform FDL handshake. Returns true on success. */
    external fun fdlHandshake(handle: Long): Boolean

    /** Flash PAC firmware package via FDL. Returns true on success. */
    external fun fdlFlash(handle: Long, pacPath: String): Boolean

    /** Read UniSoc NV data. Returns raw bytes. */
    external fun readUnisocNv(handle: Long, nvId: Int): ByteArray

    /** Write UniSoc NV data. Returns true on success. */
    external fun writeUnisocNv(handle: Long, nvId: Int, data: ByteArray): Boolean

    // ── Fastboot-Specific ────────────────────────────────────────
    /** Send generic fastboot command. Returns response or starts with FAIL: */
    external fun fastbootCommand(handle: Long, command: String): String

    /** Download and flash data via fastboot. */
    external fun fastbootFlash(handle: Long, partition: String, data: ByteArray): Boolean

    /** Perform OEM unlock via fastboot. */
    external fun fastbootUnlock(handle: Long): Boolean

    /** Reboot device via fastboot. Target can be "bootloader", "recovery", etc. */
    external fun fastbootReboot(handle: Long, target: String): Boolean

    // ── Forensics Layer ──────────────────────────────────────────
    /** 
     * Perform a bit-level safe dump of a partition with hash verification.
     * Returns true on successful acquisition.
     */
    external fun safeDump(handle: Long, partition: String, outPath: String): Boolean

    /**
     * Carve deleted files from a raw partition.
     * Returns a JSON list of identified file fragments.
     */
    external fun carveDeletedData(handle: Long, partition: String, types: Array<String>): String

    /** Perform forensic acquisition of encrypted userdata (Physical dumping). */
    external fun acquireForensicImage(handle: Long, partition: String, outDir: String): String

    /** Patch the Android locksettings database to remove user passwords. */
    external fun removeScreenLock(handle: Long, dbPath: String): Boolean

    // ── Device Info ──────────────────────────────────────────────
    /** Get a JSON string containing all device info (SoC, security, FRP, etc.) */
    external fun getDeviceInfo(handle: Long): String
}
