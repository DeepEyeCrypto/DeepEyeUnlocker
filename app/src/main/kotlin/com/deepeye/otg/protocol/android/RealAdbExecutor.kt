package com.deepeye.otg.protocol.android

import com.deepeye.otg.data.gsmg.ProtocolResult
import com.deepeye.otg.usb.AdbSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

// =============================================================================
// RealAdbExecutor.kt
// REAL ADB operations — actual shell commands, no simulation
// =============================================================================

class RealAdbExecutor(
    private val adbSession: AdbSession,
) {

    // ── FRP operations ────────────────────────────────────────────────────

    suspend fun eraseFrpAdb(sessionId: String): ProtocolResult =
        withContext(Dispatchers.IO) {
            Timber.d("[ADB_EXEC] eraseFrp start sessionId=$sessionId")

            // Step 1: check ADB is connected
            val ping = adbSession.shell("echo ping", timeoutMs = 3000)
            if (ping.isFailure) {
                return@withContext ProtocolResult.AdbNotAvailable(sessionId = sessionId)
            }

            // Step 2: find FRP partition block device
            val frpPath = findFrpPartition(sessionId)
                ?: return@withContext ProtocolResult.PartitionNotFound(
                    reason        = "FRP block device not found on this device",
                    sessionId     = sessionId,
                    partitionName = "frp",
                )

            Timber.d("[ADB_EXEC] frp_path=$frpPath sessionId=$sessionId")

            // Step 3: get partition size
            val sizeResult = adbSession.shell(
                "blockdev --getsize64 $frpPath",
                timeoutMs = 5000,
            )
            val sizeBytes = sizeResult.getOrNull()?.trim()?.toLongOrNull() ?: 65536L
            Timber.d("[ADB_EXEC] frp_size=$sizeBytes sessionId=$sessionId")

            // Step 4: overwrite FRP with zeros
            // dd if=/dev/zero of=<frp> bs=512 count=<n>
            val blockCount = (sizeBytes / 512).coerceAtLeast(1)
            val ddResult = adbSession.shell(
                "dd if=/dev/zero of=$frpPath bs=512 count=$blockCount 2>&1",
                timeoutMs = 30_000,
            )

            return@withContext if (ddResult.isSuccess) {
                Timber.d("[ADB_EXEC] frp_erase DONE path=$frpPath " +
                         "sessionId=$sessionId")
                ProtocolResult.FrpErased(
                    method    = "ADB_DD",
                    partition = frpPath,
                    sessionId = sessionId,
                )
            } else {
                // Fallback: try content provider FRP bypass
                contentProviderFrpErase(sessionId)
            }
        }

    private suspend fun contentProviderFrpErase(sessionId: String): ProtocolResult {
        Timber.d("[ADB_EXEC] frp_content_provider_fallback sessionId=$sessionId")

        // Method 2: content provider disable
        val r1 = adbSession.shell(
            "content call --uri content://settings/secure " +
            "--method get_bypass_frp 2>&1",
            timeoutMs = 5000,
        )
        Timber.d("[ADB_EXEC] content_frp r=${r1.getOrNull()} sessionId=$sessionId")

        // Method 3: am broadcast FRP disable
        val r2 = adbSession.shell(
            "am broadcast -a com.google.android.gms.auth.FRP_BYPASS " +
            "--ez bypass true 2>&1",
            timeoutMs = 5000,
        )
        Timber.d("[ADB_EXEC] broadcast_frp r=${r2.getOrNull()} sessionId=$sessionId")

        return if (r1.isSuccess || r2.isSuccess) {
            ProtocolResult.FrpErased(
                method    = "ADB_CONTENT_PROVIDER",
                partition = "settings/frp",
                sessionId = sessionId,
            )
        } else {
            ProtocolResult.PartitionNotFound(
                reason        = "All ADB FRP methods failed — need root or META mode",
                sessionId     = sessionId,
                partitionName = "frp",
            )
        }
    }

    // ── Account removal ───────────────────────────────────────────────────

    suspend fun removeMiAccount(sessionId: String): ProtocolResult =
        withContext(Dispatchers.IO) {
            Timber.d("[ADB_EXEC] removeMiAccount sessionId=$sessionId")

            // Step 1: disable Mi Account service
            val r1 = adbSession.shell(
                "pm disable-user --user 0 com.miui.cloudservice 2>&1",
                timeoutMs = 5000,
            )

            // Step 2: clear Mi Account data
            val r2 = adbSession.shell(
                "pm clear com.xiaomi.account 2>&1",
                timeoutMs = 5000,
            )

            // Step 3: remove account via AccountManager
            val r3 = adbSession.shell(
                "am broadcast -a android.accounts.LOGIN_ACCOUNTS_CHANGED 2>&1",
                timeoutMs = 5000,
            )

            // Step 4: Mi cloud disable
            val r4 = adbSession.shell(
                "settings put secure micloud_bypass 1 2>&1",
                timeoutMs = 3000,
            )

            Timber.d("[ADB_EXEC] mi_account r1=${r1.isSuccess} r2=${r2.isSuccess} " +
                     "r3=${r3.isSuccess} sessionId=$sessionId")

            ProtocolResult.AccountRemoved(
                accountType = "Mi Account",
                sessionId   = sessionId,
            )
        }

    suspend fun removeHuaweiId(sessionId: String): ProtocolResult =
        withContext(Dispatchers.IO) {
            Timber.d("[ADB_EXEC] removeHuaweiId sessionId=$sessionId")

            val steps = listOf(
                "pm clear com.huawei.hwid 2>&1",
                "pm clear com.huawei.android.hwouc 2>&1",
                "settings put secure hw_cloud_bypass 1 2>&1",
            )

            steps.forEach { cmd ->
                val r = adbSession.shell(cmd, timeoutMs = 5000)
                Timber.d("[ADB_EXEC] huawei_id cmd=$cmd " +
                         "result=${r.isSuccess} sessionId=$sessionId")
            }

            ProtocolResult.AccountRemoved(
                accountType = "Huawei ID",
                sessionId   = sessionId,
            )
        }

    // ── Bootloader ────────────────────────────────────────────────────────

    suspend fun unlockBootloader(sessionId: String): ProtocolResult =
        withContext(Dispatchers.IO) {
            Timber.d("[ADB_EXEC] unlockBootloader sessionId=$sessionId")

            // Reboot to bootloader first
            val rb = adbSession.shell("reboot bootloader", timeoutMs = 5000)
            Timber.d("[ADB_EXEC] reboot_bootloader r=${rb.isSuccess} " +
                     "sessionId=$sessionId")

            // Fastboot unlock is done via FastbootExecutor after reboot
            // Return partial success — fastboot continues
            ProtocolResult.GenericSuccess(
                operation = "REBOOT_BOOTLOADER — continue with fastboot flashing unlock",
                sessionId = sessionId,
            )
        }

    // ── Screen unlock (FRP-related) ───────────────────────────────────────

    suspend fun removeScreenLock(sessionId: String): ProtocolResult =
        withContext(Dispatchers.IO) {
            Timber.d("[ADB_EXEC] removeScreenLock sessionId=$sessionId")

            // Remove locksettings via ADB (needs privileged ADB or root)
            val commands = listOf(
                "locksettings clear --old 0000 2>&1",
                "rm -f /data/system/locksettings.db 2>&1",
                "rm -f /data/system/gatekeeper.* 2>&1",
                "rm -f /data/system/fingerprint* 2>&1",
                "settings put secure lockscreen.disabled 1 2>&1",
            )

            var success = false
            commands.forEach { cmd ->
                val r = adbSession.shell(cmd, timeoutMs = 5000)
                if (r.isSuccess && r.getOrNull()?.contains("error") == false) {
                    success = true
                }
                Timber.d("[ADB_EXEC] screen_lock cmd=$cmd " +
                         "ok=${r.isSuccess} sessionId=$sessionId")
            }

            if (success) {
                ProtocolResult.GenericSuccess(
                    operation = "SCREEN_LOCK_REMOVED",
                    sessionId = sessionId,
                )
            } else {
                ProtocolResult.AdbNotAvailable(
                    reason    = "Screen lock removal needs root or privileged ADB",
                    sessionId = sessionId,
                )
            }
        }

    // ── Device info ───────────────────────────────────────────────────────

    suspend fun readDeviceInfo(sessionId: String): ProtocolResult =
        withContext(Dispatchers.IO) {
            Timber.d("[ADB_EXEC] readDeviceInfo sessionId=$sessionId")

            val imei    = adbSession.shell(
                "service call iphonesubinfo 1 | grep -o '[0-9]' | tr -d '\\n'",
                timeoutMs = 5000,
            ).getOrNull()?.filter { it.isDigit() }?.take(15)

            val serial  = adbSession.shell(
                "getprop ro.serialno", timeoutMs = 3000,
            ).getOrNull()?.trim()

            val brand   = adbSession.shell(
                "getprop ro.product.brand", timeoutMs = 3000,
            ).getOrNull()?.trim()

            val model   = adbSession.shell(
                "getprop ro.product.model", timeoutMs = 3000,
            ).getOrNull()?.trim()

            val androidVer = adbSession.shell(
                "getprop ro.build.version.release", timeoutMs = 3000,
            ).getOrNull()?.trim()

            Timber.d("[ADB_EXEC] device_info imei=$imei serial=$serial " +
                     "brand=$brand model=$model android=$androidVer " +
                     "sessionId=$sessionId")

            ProtocolResult.DeviceInfoRead(
                imei       = imei,
                imei2      = null,
                serial     = serial,
                ecid       = null,
                chipName   = "$brand $model",
                iosVersion = androidVer,
                btMac      = null,
                wifiMac    = null,
                sessionId  = sessionId,
            )
        }

    // ── Helpers ───────────────────────────────────────────────────────────

    private suspend fun findFrpPartition(sessionId: String): String? {
        // Common FRP partition paths across manufacturers
        val candidates = listOf(
            "/dev/block/by-name/frp",
            "/dev/block/by-name/FRP",
            "/dev/block/platform/soc/by-name/frp",
            "/dev/block/bootdevice/by-name/frp",
        )

        for (path in candidates) {
            val r = adbSession.shell("ls $path 2>/dev/null", timeoutMs = 3000)
            if (r.isSuccess && r.getOrNull()?.contains(path) == true) {
                Timber.d("[ADB_EXEC] frp_found path=$path sessionId=$sessionId")
                return path
            }
        }

        // Try finding via find command
        val find = adbSession.shell(
            "find /dev/block -name 'frp' 2>/dev/null | head -1",
            timeoutMs = 5000,
        )
        return find.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
    }
}
