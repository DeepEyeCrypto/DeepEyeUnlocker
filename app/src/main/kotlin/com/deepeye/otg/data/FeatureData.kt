package com.deepeye.otg.data

import androidx.compose.runtime.Immutable

// ── Connection modes that unlock this feature ─────────────────
enum class SupportedMode { ADB, FASTBOOT, EDL, TESTPOINT,
                           BROM, PRELOADER, DIAG, MTP, ISP, META }

// ── Chipset families ──────────────────────────────────────────
enum class Chipset { QUALCOMM, MTK, EXYNOS, UNISOC, KIRIN, ALL }

// ── Risk level ────────────────────────────────────────────────
enum class RiskLevel {
    SAFE,      // No data loss, reversible
    MODERATE,  // Partial data loss possible
    HIGH,      // Data loss likely, irreversible
    CRITICAL   // Brick risk, auth required
}

@Immutable
data class FeatureItem(
    val id: String,
    val icon: String,
    val label: String,
    val tier: Int,                          // 1=safe 2=moderate 3=high
    val modes: List<SupportedMode>,         // which modes support it
    val chipsets: List<Chipset> = listOf(Chipset.ALL),
    val risk: RiskLevel = RiskLevel.SAFE,
    val requiresAuth: Boolean = false,      // needs server auth
    val description: String = "",
    val successLog: String = "",
    val warningMsg: String? = null
)

@Immutable
data class FeatureGroup(
    val id: String,
    val title: String,
    val features: List<FeatureItem>
)

@Immutable
data class BrandFeatureSet(
    val brand: String,
    val groups: List<FeatureGroup>
)

// ── Master database ───────────────────────────────────────────
object FeatureData {

    val brands = listOf("Xiaomi", "Samsung", "Oppo", "Vivo", "Realme", "OnePlus")

    // ═══════════════════════════════════════════════════════════
    // XIAOMI / REDMI / POCO
    // ═══════════════════════════════════════════════════════════
    val xiaomi = BrandFeatureSet(
        brand = "Xiaomi",
        groups = listOf(

            FeatureGroup("xiaomi_unlock", "UNLOCK OPERATIONS", listOf(
                FeatureItem(
                    id = "xi_unlock_bl",
                    icon = "🔓",
                    label = "Unlock Bootloader",
                    tier = 1,
                    modes = listOf(SupportedMode.FASTBOOT, SupportedMode.EDL),
                    description = "Official/unofficial BL unlock via fastboot oem unlock",
                    successLog = "Bootloader unlocked successfully",
                    warningMsg = "Wipes userdata partition"
                ),
                FeatureItem(
                    id = "xi_relock_bl",
                    icon = "🔒",
                    label = "Relock Bootloader",
                    tier = 1,
                    modes = listOf(SupportedMode.FASTBOOT),
                    description = "Relock BL via fastboot oem lock"
                ),
                FeatureItem(
                    id = "xi_micloud_remove",
                    icon = "☁️",
                    label = "Mi Cloud Remove",
                    tier = 2,
                    modes = listOf(SupportedMode.EDL, SupportedMode.TESTPOINT,
                                   SupportedMode.FASTBOOT),
                    chipsets = listOf(Chipset.QUALCOMM, Chipset.MTK),
                    risk = RiskLevel.MODERATE,
                    requiresAuth = true,
                    description = "Remove Mi Account lock via EDL firehose / fastboot flash",
                    successLog = "Mi Cloud account removed",
                    warningMsg = "Requires EDL or TestPoint on secured devices"
                ),
                FeatureItem(
                    id = "xi_micloud_check",
                    icon = "🔍",
                    label = "Mi Cloud Check",
                    tier = 1,
                    modes = listOf(SupportedMode.ADB, SupportedMode.FASTBOOT),
                    description = "Check Mi Account lock status ON/OFF/LOST via server"
                ),
                FeatureItem(
                    id = "xi_frp_erase",
                    icon = "🗑️",
                    label = "Erase FRP",
                    tier = 1,
                    modes = listOf(SupportedMode.ADB, SupportedMode.EDL,
                                   SupportedMode.FASTBOOT, SupportedMode.TESTPOINT),
                    description = "Wipe FRP partition — Google account bypass",
                    successLog = "FRP partition erased"
                ),
                FeatureItem(
                    id = "xi_auth_bypass",
                    icon = "⚡",
                    label = "Auth Bypass (SLAH)",
                    tier = 3,
                    modes = listOf(SupportedMode.EDL, SupportedMode.TESTPOINT),
                    chipsets = listOf(Chipset.QUALCOMM),
                    risk = RiskLevel.CRITICAL,
                    requiresAuth = true,
                    description = "Sahara/Firehose auth bypass for locked EDL devices",
                    warningMsg = "⚠️ High risk — wrong loader = softbrick"
                )
            )),

            FeatureGroup("xiaomi_firmware", "FIRMWARE & FLASH", listOf(
                FeatureItem(
                    id = "xi_write_fw",
                    icon = "💾",
                    label = "Write Firmware",
                    tier = 2,
                    modes = listOf(SupportedMode.FASTBOOT, SupportedMode.EDL,
                                   SupportedMode.TESTPOINT),
                    risk = RiskLevel.HIGH,
                    description = "Flash stock MIUI/HyperOS via fastboot or firehose"
                ),
                FeatureItem(
                    id = "xi_read_fw",
                    icon = "📖",
                    label = "Read / Backup FW",
                    tier = 1,
                    modes = listOf(SupportedMode.ADB, SupportedMode.EDL,
                                   SupportedMode.FASTBOOT),
                    description = "Read and backup firmware partitions"
                ),
                FeatureItem(
                    id = "xi_fastboot_edl",
                    icon = "⚠️",
                    label = "Force EDL Mode",
                    tier = 3,
                    modes = listOf(SupportedMode.FASTBOOT, SupportedMode.ADB),
                    chipsets = listOf(Chipset.QUALCOMM),
                    risk = RiskLevel.HIGH,
                    description = "Force reboot to EDL 9008 from fastboot/ADB"
                ),
                FeatureItem(
                    id = "xi_partition",
                    icon = "🗂️",
                    label = "Partition Manager",
                    tier = 3,
                    modes = listOf(SupportedMode.EDL, SupportedMode.FASTBOOT),
                    risk = RiskLevel.CRITICAL,
                    description = "Read/write/erase individual partitions"
                )
            )),

            FeatureGroup("xiaomi_repair", "REPAIR & DIAGNOSTICS", listOf(
                FeatureItem(
                    id = "xi_imei_repair",
                    icon = "📡",
                    label = "IMEI Repair",
                    tier = 2,
                    modes = listOf(SupportedMode.DIAG, SupportedMode.EDL,
                                   SupportedMode.ADB),
                    chipsets = listOf(Chipset.QUALCOMM, Chipset.MTK),
                    risk = RiskLevel.MODERATE,
                    requiresAuth = true,
                    description = "Read/write IMEI via QCN or NV items"
                ),
                FeatureItem(
                    id = "xi_nv_backup",
                    icon = "💿",
                    label = "NV Backup/Restore",
                    tier = 2,
                    modes = listOf(SupportedMode.DIAG, SupportedMode.EDL),
                    chipsets = listOf(Chipset.QUALCOMM),
                    description = "Read/write QCN NV file (IMEI, calibration data)"
                ),
                FeatureItem(
                    id = "xi_device_info",
                    icon = "📊",
                    label = "Device Info Dump",
                    tier = 1,
                    modes = listOf(SupportedMode.ADB, SupportedMode.FASTBOOT,
                                   SupportedMode.DIAG),
                    description = "Dump device info: model, SN, IMEI, BL status"
                ),
                FeatureItem(
                    id = "xi_demo_retail",
                    icon = "🏪",
                    label = "Demo to Retail",
                    tier = 2,
                    modes = listOf(SupportedMode.ADB, SupportedMode.FASTBOOT),
                    description = "Convert demo unit ROM to retail/global"
                ),
                FeatureItem(
                    id = "xi_diag_enable",
                    icon = "🩺",
                    label = "Enable DIAG Mode",
                    tier = 3,
                    modes = listOf(SupportedMode.ADB, SupportedMode.FASTBOOT),
                    chipsets = listOf(Chipset.QUALCOMM),
                    risk = RiskLevel.MODERATE,
                    description = "Enable Qualcomm diagnostic port for IMEI/NV ops"
                )
            )),

            FeatureGroup("xiaomi_screen", "SCREEN & ACCOUNT LOCK", listOf(
                FeatureItem(
                    id = "xi_screen_lock",
                    icon = "🔑",
                    label = "Remove Screen Lock",
                    tier = 2,
                    modes = listOf(SupportedMode.ADB, SupportedMode.EDL,
                                   SupportedMode.FASTBOOT),
                    risk = RiskLevel.MODERATE,
                    description = "Wipe lockscreen PIN/pattern via ADB or fastboot"
                ),
                FeatureItem(
                    id = "xi_factory_rst",
                    icon = "🏭",
                    label = "Factory Reset",
                    tier = 1,
                    modes = listOf(SupportedMode.ADB, SupportedMode.FASTBOOT,
                                   SupportedMode.EDL),
                    warningMsg = "Wipes all user data",
                    description = "Full factory reset via fastboot -w or ADB"
                )
            ))
        )
    )

    // ═══════════════════════════════════════════════════════════
    // SAMSUNG
    // ═══════════════════════════════════════════════════════════
    val samsung = BrandFeatureSet(
        brand = "Samsung",
        groups = listOf(

            FeatureGroup("sam_frp", "FRP & ACCOUNT", listOf(
                FeatureItem(
                    id = "sam_frp_adb",
                    icon = "🗑️",
                    label = "FRP Remove (ADB)",
                    tier = 1,
                    modes = listOf(SupportedMode.ADB),
                    description = "Remove FRP via ADB sideload or Settings exploit"
                ),
                FeatureItem(
                    id = "sam_frp_odin",
                    icon = "🔥",
                    label = "FRP Remove (Odin)",
                    tier = 2,
                    modes = listOf(SupportedMode.FASTBOOT),
                    chipsets = listOf(Chipset.EXYNOS, Chipset.QUALCOMM),
                    description = "Flash FRP-clear package via Odin/Heimdall protocol"
                ),
                FeatureItem(
                    id = "sam_frp_testpoint",
                    icon = "⚡",
                    label = "FRP (TestPoint)",
                    tier = 3,
                    modes = listOf(SupportedMode.TESTPOINT, SupportedMode.EDL),
                    risk = RiskLevel.HIGH,
                    requiresAuth = true,
                    description = "Hardware testpoint to bypass SLA auth + FRP erase"
                ),
                FeatureItem(
                    id = "sam_samsung_account",
                    icon = "👤",
                    label = "Samsung Account Remove",
                    tier = 2,
                    modes = listOf(SupportedMode.ADB, SupportedMode.FASTBOOT),
                    requiresAuth = true,
                    description = "Remove Samsung/Find My Mobile account lock"
                )
            )),

            FeatureGroup("sam_flash", "FLASH & FIRMWARE", listOf(
                FeatureItem(
                    id = "sam_odin_flash",
                    icon = "⚙️",
                    label = "Odin Flash",
                    tier = 2,
                    modes = listOf(SupportedMode.FASTBOOT),
                    description = "Flash via Samsung Odin/Heimdall download mode"
                ),
                FeatureItem(
                    id = "sam_write_fw",
                    icon = "💾",
                    label = "Write Firmware",
                    tier = 2,
                    modes = listOf(SupportedMode.FASTBOOT, SupportedMode.EDL),
                    risk = RiskLevel.HIGH,
                    description = "Flash full firmware package via Odin protocol"
                ),
                FeatureItem(
                    id = "sam_read_fw",
                    icon = "📖",
                    label = "Read Firmware",
                    tier = 1,
                    modes = listOf(SupportedMode.ADB, SupportedMode.FASTBOOT),
                    description = "Read firmware info and backup partitions"
                )
            )),

            FeatureGroup("sam_repair", "REPAIR & SECURITY", listOf(
                FeatureItem(
                    id = "sam_imei_repair",
                    icon = "📡",
                    label = "IMEI Repair",
                    tier = 2,
                    modes = listOf(SupportedMode.DIAG, SupportedMode.ADB),
                    requiresAuth = true,
                    description = "Repair/write IMEI via EFS partition or DIAG"
                ),
                FeatureItem(
                    id = "sam_efs_backup",
                    icon = "🛡️",
                    label = "EFS Backup/Restore",
                    tier = 2,
                    modes = listOf(SupportedMode.ADB, SupportedMode.DIAG,
                                   SupportedMode.ISP),
                    description = "Backup and restore EFS partition (IMEI storage)"
                ),
                FeatureItem(
                    id = "sam_screen_lock",
                    icon = "🔑",
                    label = "Remove Screen Lock",
                    tier = 2,
                    modes = listOf(SupportedMode.ADB, SupportedMode.FASTBOOT),
                    risk = RiskLevel.MODERATE,
                    description = "Wipe lock via ADB or Odin flash"
                ),
                FeatureItem(
                    id = "sam_network_unlock",
                    icon = "📶",
                    label = "Network Unlock",
                    tier = 2,
                    modes = listOf(SupportedMode.ADB, SupportedMode.DIAG),
                    requiresAuth = true,
                    description = "Remove carrier SIM lock via server code"
                ),
                FeatureItem(
                    id = "sam_factory_rst",
                    icon = "🏭",
                    label = "Factory Reset",
                    tier = 1,
                    modes = listOf(SupportedMode.ADB, SupportedMode.FASTBOOT),
                    warningMsg = "Wipes all user data",
                    description = "Full wipe via ADB or Odin"
                ),
                FeatureItem(
                    id = "sam_da_key",
                    icon = "🔐",
                    label = "DA Public Key Bypass",
                    tier = 3,
                    modes = listOf(SupportedMode.TESTPOINT, SupportedMode.BROM),
                    chipsets = listOf(Chipset.MTK),
                    risk = RiskLevel.CRITICAL,
                    requiresAuth = true,
                    description = "Bypass MTK DA Public Key check (Samsung MTK models)"
                )
            ))
        )
    )

    // ═══════════════════════════════════════════════════════════
    // OPPO / REALME / OnePlus (same codebase)
    // ═══════════════════════════════════════════════════════════
    val oppo = BrandFeatureSet(
        brand = "Oppo",
        groups = listOf(

            FeatureGroup("oppo_unlock", "UNLOCK OPERATIONS", listOf(
                FeatureItem(
                    id = "oppo_frp_adb",
                    icon = "🗑️",
                    label = "FRP Remove (ADB)",
                    tier = 1,
                    modes = listOf(SupportedMode.ADB),
                    description = "Remove FRP via ADB exploit or settings bypass"
                ),
                FeatureItem(
                    id = "oppo_frp_meta",
                    icon = "⚡",
                    label = "FRP Remove (META)",
                    tier = 2,
                    modes = listOf(SupportedMode.META, SupportedMode.PRELOADER),
                    chipsets = listOf(Chipset.MTK),
                    description = "MTK META mode factory reset to bypass FRP"
                ),
                FeatureItem(
                    id = "oppo_oppo_id",
                    icon = "👤",
                    label = "OPPO ID Remove",
                    tier = 2,
                    modes = listOf(SupportedMode.EDL, SupportedMode.TESTPOINT,
                                   SupportedMode.FASTBOOT),
                    requiresAuth = true,
                    risk = RiskLevel.MODERATE,
                    description = "Remove OPPO/Realme cloud account lock"
                ),
                FeatureItem(
                    id = "oppo_bl_unlock",
                    icon = "🔓",
                    label = "Bootloader Unlock",
                    tier = 2,
                    modes = listOf(SupportedMode.FASTBOOT, SupportedMode.EDL),
                    risk = RiskLevel.HIGH,
                    description = "Unlock BL via deep test mode or fastboot"
                ),
                FeatureItem(
                    id = "oppo_pattern",
                    icon = "🔑",
                    label = "Screen Lock Remove",
                    tier = 2,
                    modes = listOf(SupportedMode.ADB, SupportedMode.PRELOADER,
                                   SupportedMode.META),
                    risk = RiskLevel.MODERATE,
                    description = "Remove pattern/PIN via ADB or MTK META"
                ),
                FeatureItem(
                    id = "oppo_factory_rst",
                    icon = "🏭",
                    label = "Factory Reset",
                    tier = 1,
                    modes = listOf(SupportedMode.ADB, SupportedMode.META,
                                   SupportedMode.FASTBOOT),
                    warningMsg = "Wipes all user data"
                )
            )),

            FeatureGroup("oppo_firmware", "FIRMWARE & REPAIR", listOf(
                FeatureItem(
                    id = "oppo_write_fw",
                    icon = "💾",
                    label = "Write Firmware",
                    tier = 2,
                    modes = listOf(SupportedMode.FASTBOOT, SupportedMode.EDL,
                                   SupportedMode.BROM),
                    risk = RiskLevel.HIGH,
                    description = "Flash ColorOS firmware via ofp/fastboot/scatter"
                ),
                FeatureItem(
                    id = "oppo_read_fw",
                    icon = "📖",
                    label = "Read Backup",
                    tier = 1,
                    modes = listOf(SupportedMode.ADB, SupportedMode.FASTBOOT),
                    description = "Backup firmware and critical partitions"
                ),
                FeatureItem(
                    id = "oppo_imei_repair",
                    icon = "📡",
                    label = "IMEI Repair",
                    tier = 2,
                    modes = listOf(SupportedMode.DIAG, SupportedMode.META,
                                   SupportedMode.ADB),
                    requiresAuth = true,
                    description = "Repair IMEI via ADB AT commands or META mode"
                ),
                FeatureItem(
                    id = "oppo_sn8_edl",
                    icon = "⚠️",
                    label = "SD8Gen2 EDL Mode",
                    tier = 3,
                    modes = listOf(SupportedMode.EDL, SupportedMode.TESTPOINT),
                    chipsets = listOf(Chipset.QUALCOMM),
                    risk = RiskLevel.CRITICAL,
                    requiresAuth = true,
                    description = "Snapdragon 8 Gen 2 EDL support — latest OPPO/Realme"
                )
            ))
        )
    )

    // ═══════════════════════════════════════════════════════════
    // VIVO
    // ═══════════════════════════════════════════════════════════
    val vivo = BrandFeatureSet(
        brand = "Vivo",
        groups = listOf(
            FeatureGroup("vivo_ops", "FRP & UNLOCK", listOf(
                FeatureItem(
                    id = "vivo_frp",
                    icon = "🗑️",
                    label = "FRP Remove",
                    tier = 1,
                    modes = listOf(SupportedMode.ADB, SupportedMode.META,
                                   SupportedMode.FASTBOOT),
                    description = "Remove Google FRP via ADB or META mode"
                ),
                FeatureItem(
                    id = "vivo_factory_rst",
                    icon = "🏭",
                    label = "Factory Reset",
                    tier = 1,
                    modes = listOf(SupportedMode.ADB, SupportedMode.META,
                                   SupportedMode.FASTBOOT),
                    warningMsg = "Wipes all user data",
                    description = "Full wipe via META mode or ADB"
                ),
                FeatureItem(
                    id = "vivo_vivo_id",
                    icon = "👤",
                    label = "Vivo Account Remove",
                    tier = 2,
                    modes = listOf(SupportedMode.EDL, SupportedMode.TESTPOINT),
                    requiresAuth = true,
                    risk = RiskLevel.MODERATE,
                    description = "Remove Vivo account/cloud lock"
                ),
                FeatureItem(
                    id = "vivo_pattern",
                    icon = "🔑",
                    label = "Screen Lock Remove",
                    tier = 2,
                    modes = listOf(SupportedMode.ADB, SupportedMode.META),
                    risk = RiskLevel.MODERATE
                )
            )),
            FeatureGroup("vivo_firmware", "FIRMWARE & REPAIR", listOf(
                FeatureItem(
                    id = "vivo_write_fw",
                    icon = "💾",
                    label = "Write Firmware",
                    tier = 2,
                    modes = listOf(SupportedMode.FASTBOOT, SupportedMode.EDL,
                                   SupportedMode.BROM),
                    risk = RiskLevel.HIGH
                ),
                FeatureItem(
                    id = "vivo_imei",
                    icon = "📡",
                    label = "IMEI Repair",
                    tier = 2,
                    modes = listOf(SupportedMode.DIAG, SupportedMode.ADB),
                    requiresAuth = true
                )
            ))
        )
    )

    // ═══════════════════════════════════════════════════════════
    // REALME
    // ═══════════════════════════════════════════════════════════
    val realme = BrandFeatureSet(
        brand = "Realme",
        groups = listOf(
            FeatureGroup("realme_ops", "FRP & UNLOCK", listOf(
                FeatureItem(
                    id = "rm_frp_adb",
                    icon = "🗑️",
                    label = "FRP Remove (ADB)",
                    tier = 1,
                    modes = listOf(SupportedMode.ADB)
                ),
                FeatureItem(
                    id = "rm_frp_meta",
                    icon = "⚡",
                    label = "FRP Remove (META/Preloader)",
                    tier = 2,
                    modes = listOf(SupportedMode.PRELOADER, SupportedMode.META),
                    chipsets = listOf(Chipset.MTK),
                    description = "MTK Preloader V3 auth bypass for newer Realme"
                ),
                FeatureItem(
                    id = "rm_realme_id",
                    icon = "👤",
                    label = "Realme Account Remove",
                    tier = 2,
                    modes = listOf(SupportedMode.EDL, SupportedMode.TESTPOINT),
                    requiresAuth = true
                ),
                FeatureItem(
                    id = "rm_bl_unlock",
                    icon = "🔓",
                    label = "Bootloader Unlock",
                    tier = 2,
                    modes = listOf(SupportedMode.FASTBOOT, SupportedMode.EDL),
                    risk = RiskLevel.HIGH
                ),
                FeatureItem(
                    id = "rm_factory_rst",
                    icon = "🏭",
                    label = "Factory Reset",
                    tier = 1,
                    modes = listOf(SupportedMode.ADB, SupportedMode.META,
                                   SupportedMode.FASTBOOT),
                    warningMsg = "Wipes all user data"
                )
            )),
            FeatureGroup("realme_fw", "FIRMWARE", listOf(
                FeatureItem(
                    id = "rm_write_fw",
                    icon = "💾",
                    label = "Write Firmware",
                    tier = 2,
                    modes = listOf(SupportedMode.FASTBOOT, SupportedMode.BROM,
                                   SupportedMode.EDL),
                    risk = RiskLevel.HIGH
                ),
                FeatureItem(
                    id = "rm_imei",
                    icon = "📡",
                    label = "IMEI Repair",
                    tier = 2,
                    modes = listOf(SupportedMode.DIAG, SupportedMode.META),
                    requiresAuth = true
                ),
                FeatureItem(
                    id = "rm_sla_bypass",
                    icon = "⚠️",
                    label = "SLA Auth Bypass",
                    tier = 3,
                    modes = listOf(SupportedMode.BROM, SupportedMode.TESTPOINT),
                    chipsets = listOf(Chipset.MTK),
                    risk = RiskLevel.CRITICAL,
                    requiresAuth = true,
                    description = "SLA V3/V5 auth bypass for secured MTK Realme/Tecno"
                )
            ))
        )
    )

    // ═══════════════════════════════════════════════════════════
    // ONEPLUS
    // ═══════════════════════════════════════════════════════════
    val oneplus = BrandFeatureSet(
        brand = "OnePlus",
        groups = listOf(
            FeatureGroup("op_ops", "UNLOCK & FRP", listOf(
                FeatureItem(
                    id = "op_frp",
                    icon = "🗑️",
                    label = "FRP Remove",
                    tier = 1,
                    modes = listOf(SupportedMode.ADB, SupportedMode.FASTBOOT)
                ),
                FeatureItem(
                    id = "op_bl_unlock",
                    icon = "🔓",
                    label = "Bootloader Unlock",
                    tier = 1,
                    modes = listOf(SupportedMode.FASTBOOT),
                    description = "fastboot oem unlock — officially supported"
                ),
                FeatureItem(
                    id = "op_screen_lock",
                    icon = "🔑",
                    label = "Screen Lock Remove",
                    tier = 2,
                    modes = listOf(SupportedMode.ADB, SupportedMode.FASTBOOT),
                    risk = RiskLevel.MODERATE
                ),
                FeatureItem(
                    id = "op_factory_rst",
                    icon = "🏭",
                    label = "Factory Reset",
                    tier = 1,
                    modes = listOf(SupportedMode.ADB, SupportedMode.FASTBOOT),
                    warningMsg = "Wipes all user data"
                )
            )),
            FeatureGroup("op_fw", "FIRMWARE & REPAIR", listOf(
                FeatureItem(
                    id = "op_write_fw",
                    icon = "💾",
                    label = "Write Firmware",
                    tier = 2,
                    modes = listOf(SupportedMode.FASTBOOT, SupportedMode.EDL),
                    risk = RiskLevel.HIGH
                ),
                FeatureItem(
                    id = "op_imei",
                    icon = "📡",
                    label = "IMEI Repair",
                    tier = 2,
                    modes = listOf(SupportedMode.DIAG, SupportedMode.ADB),
                    requiresAuth = true
                ),
                FeatureItem(
                    id = "op_edl_force",
                    icon = "⚠️",
                    label = "Force EDL Mode",
                    tier = 3,
                    modes = listOf(SupportedMode.FASTBOOT, SupportedMode.ADB),
                    chipsets = listOf(Chipset.QUALCOMM),
                    risk = RiskLevel.HIGH,
                    description = "Force reboot into Qualcomm 9008 EDL port"
                )
            ))
        )
    )

    // ── Master index ──────────────────────────────────────────
    val allBrands: List<BrandFeatureSet> = listOf(
        xiaomi, samsung, oppo, vivo, realme, oneplus
    )

    fun forBrand(index: Int): BrandFeatureSet =
        allBrands.getOrElse(index) { xiaomi }

    fun forBrand(name: String): BrandFeatureSet =
        allBrands.firstOrNull {
            it.brand.equals(name, ignoreCase = true)
        } ?: xiaomi
}
