package com.deepeye.otg.device

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class AdbDeviceInfo(
    val model:          String = "",
    val brand:          String = "",
    val android:        String = "",
    val sdk:            String = "",
    val chipset:        String = "",
    val serialNo:       String = "",
    val imei:           String = "",
    val frpStatus:      String = "",
    val securityPatch:  String = "",
    val bootloaderStatus: String = "",
    val abPartition:    String = "",
)

object AdbEngine {

    // Run ADB command via Runtime.exec (requires ADB binary or via adb server)
    suspend fun shell(serial: String, cmd: String): Result<String> =
        withContext(Dispatchers.IO) { runCatching {
            val proc = Runtime.getRuntime().exec(
                arrayOf("adb", "-s", serial, "shell", cmd)
            )
            proc.inputStream.bufferedReader().readText().trim()
        }}

    // Get all device props at once
    suspend fun getFullInfo(serial: String): Result<AdbDeviceInfo> =
        withContext(Dispatchers.IO) { runCatching {
            fun prop(p: String) = runCatching {
                Runtime.getRuntime()
                    .exec(arrayOf("adb", "-s", serial, "shell", "getprop $p"))
                    .inputStream.bufferedReader().readText().trim()
            }.getOrDefault("")

            AdbDeviceInfo(
                model         = prop("ro.product.model"),
                brand         = prop("ro.product.brand"),
                android       = prop("ro.build.version.release"),
                sdk           = prop("ro.build.version.sdk"),
                chipset       = prop("ro.board.platform"),
                serialNo      = prop("ro.serialno"),
                securityPatch = prop("ro.build.version.security_patch"),
                frpStatus     = prop("ro.setupwizard.mode"),
                bootloaderStatus = prop("ro.boot.flash.locked"),
                abPartition   = prop("ro.boot.slot_suffix"),
                imei          = runCatching {
                    Runtime.getRuntime()
                        .exec(arrayOf("adb", "-s", serial, "shell",
                            "service call iphonesubinfo 1 | cut -c 53-68 | tr -d '.[:space:]'"))
                        .inputStream.bufferedReader().readText().trim()
                }.getOrDefault(""),
            )
        }}

    // Reboot device to specific mode
    suspend fun reboot(serial: String, mode: String): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching {
            val args = when (mode) {
                "bootloader" -> arrayOf("adb", "-s", serial, "reboot", "bootloader")
                "recovery"   -> arrayOf("adb", "-s", serial, "reboot", "recovery")
                "edl"        -> arrayOf("adb", "-s", serial, "reboot", "edl")
                "sideload"   -> arrayOf("adb", "-s", serial, "reboot", "sideload")
                else         -> arrayOf("adb", "-s", serial, "reboot")
            }
            Runtime.getRuntime().exec(args).waitFor()
            Unit
        }}

    // Push file to device
    suspend fun push(serial: String, localPath: String, remotePath: String): Result<String> =
        withContext(Dispatchers.IO) { runCatching {
            val proc = Runtime.getRuntime()
                .exec(arrayOf("adb", "-s", serial, "push", localPath, remotePath))
            proc.inputStream.bufferedReader().readText().trim()
        }}

    // List connected ADB devices
    suspend fun devices(): Result<List<String>> =
        withContext(Dispatchers.IO) { runCatching {
            val proc = Runtime.getRuntime().exec(arrayOf("adb", "devices", "-l"))
            proc.inputStream.bufferedReader().readLines()
                .drop(1)
                .filter { it.isNotBlank() && !it.startsWith("*") }
                .mapNotNull { it.split("\\s+".toRegex()).firstOrNull() }
        }}
}

object FastbootEngine {

    suspend fun devices(): Result<List<String>> =
        withContext(Dispatchers.IO) { runCatching {
            val proc = Runtime.getRuntime().exec(arrayOf("fastboot", "devices"))
            proc.inputStream.bufferedReader().readLines()
                .filter { it.contains("fastboot") }
                .mapNotNull { it.split("\t").firstOrNull() }
        }}

    suspend fun getVar(serial: String, variable: String): Result<String> =
        withContext(Dispatchers.IO) { runCatching {
            val proc = Runtime.getRuntime()
                .exec(arrayOf("fastboot", "-s", serial, "getvar", variable))
            (proc.inputStream.bufferedReader().readText() +
             proc.errorStream.bufferedReader().readText()).trim()
        }}

    suspend fun flash(serial: String, partition: String, imagePath: String): Result<String> =
        withContext(Dispatchers.IO) { runCatching {
            val proc = Runtime.getRuntime()
                .exec(arrayOf("fastboot", "-s", serial, "flash", partition, imagePath))
            proc.errorStream.bufferedReader().readText().trim()
        }}

    suspend fun erase(serial: String, partition: String): Result<String> =
        withContext(Dispatchers.IO) { runCatching {
            val proc = Runtime.getRuntime()
                .exec(arrayOf("fastboot", "-s", serial, "erase", partition))
            proc.errorStream.bufferedReader().readText().trim()
        }}

    suspend fun oemUnlock(serial: String): Result<String> =
        withContext(Dispatchers.IO) { runCatching {
            val proc = Runtime.getRuntime()
                .exec(arrayOf("fastboot", "-s", serial, "oem", "unlock"))
            proc.errorStream.bufferedReader().readText().trim()
        }}

    suspend fun reboot(serial: String): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching {
            Runtime.getRuntime().exec(arrayOf("fastboot", "-s", serial, "reboot")).waitFor()
            Unit
        }}

    suspend fun getFullInfo(serial: String): Result<Map<String, String>> =
        withContext(Dispatchers.IO) { runCatching {
            val vars = listOf("product","version","serialno","unlocked",
                              "anti","current-slot","slot-count","secure")
            vars.associate { v -> v to (getVar(serial, v).getOrDefault("unknown")) }
        }}
}
