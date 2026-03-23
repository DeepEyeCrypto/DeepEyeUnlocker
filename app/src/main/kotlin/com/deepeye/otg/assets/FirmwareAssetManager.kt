package com.deepeye.otg.assets

import android.content.Context
import com.deepeye.otg.data.MtkChipDatabase
import com.deepeye.otg.data.ProtocolFamily
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// =============================================================================
// FirmwareAssetManager.kt
// Centralized loader for DA binaries and Firehose programmers.
//
// DA fallback chain (per-chip → AllInOne V6 → AllInOne Classic):
//   1. da/${chip}_da.bin       - chip-specific (e.g. mt6835t_da.bin)
//   2. da/MTK_AllInOne_DA_V6.bin  - ALL Dimensity chips (MTK_V6 protocol)
//   3. da/MTK_AllInOne_DA.bin     - ALL Helio/Classic chips
//
// Programmer lookup:
//   prog/${chipset}_${storage}_firehose.elf  (bkerler/Loaders naming)
//   e.g. prog/sm8550_ufs_firehose.elf
// =============================================================================

@Singleton
class FirmwareAssetManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    // ── DA Loader ────────────────────────────────────────────────────────────

    /**
     * Load DA binary for a given MTK hw_code.
     * Fallback chain: chip-specific → AllInOne V6 → AllInOne Classic
     */
    fun loadDa(hwCode: Int): ByteArray? {
        val entry = MtkChipDatabase.find(hwCode)

        // Try 1: chip-specific DA (e.g. da/mt6835t_da.bin)
        val specificPath = entry?.daAsset
        if (specificPath != null) {
            val bytes = tryOpenAsset(specificPath)
            if (bytes != null) {
                Timber.d("[ASSET] da_loaded chip-specific: $specificPath " +
                         "hw_code=0x${hwCode.toString(16)} size=${bytes.size}")
                return bytes
            }
            Timber.d("[ASSET] da_miss chip-specific: $specificPath — trying AllInOne")
        }

        // Try 2: AllInOne DA — V6 for Dimensity, Classic for Helio
        val allInOnePath = when (entry?.protocol) {
            ProtocolFamily.MTK_V6 -> ALLINONE_V6
            else                  -> ALLINONE_CLASSIC
        }
        val bytes = tryOpenAsset(allInOnePath)
        if (bytes != null) {
            Timber.d("[ASSET] da_loaded AllInOne: $allInOnePath " +
                     "hw_code=0x${hwCode.toString(16)} size=${bytes.size}")
            return bytes
        }

        // Try 3: Opposite AllInOne as last resort
        val fallbackPath = if (allInOnePath == ALLINONE_V6) ALLINONE_CLASSIC else ALLINONE_V6
        val fallback = tryOpenAsset(fallbackPath)
        if (fallback != null) {
            Timber.w("[ASSET] da_loaded AllInOne fallback: $fallbackPath " +
                     "hw_code=0x${hwCode.toString(16)} (may not work — wrong chip family)")
            return fallback
        }

        Timber.e("[ASSET] da_MISSING hw_code=0x${hwCode.toString(16)} " +
                 "chip=${entry?.chipName ?: "Unknown"}\n" +
                 "  Add ONE of these to app/src/main/assets/da/:\n" +
                 "  1. MTK_AllInOne_DA_V6.bin  ← V6/Dimensity chips\n" +
                 "     → from SP Flash Tool V6: spflashtools.com\n" +
                 "  2. MTK_AllInOne_DA.bin     ← Helio/Classic chips\n" +
                 "     → from SP Flash Tool V5: spflashtools.com\n" +
                 "  3. ${specificPath?.substringAfterLast("/") ?: "mt${hwCode.toString(16)}_da.bin"}" +
                 "     ← chip-specific (extract from OFP)")
        return null
    }

    // ── Programmer Loader ────────────────────────────────────────────────────

    /**
     * Load Firehose programmer ELF for a Qualcomm chipset.
     * Tries exact filename match, then storage-agnostic search.
     *
     * @param assetPath e.g. "prog/sm8550_ufs_firehose.elf"
     */
    fun loadProgrammer(assetPath: String): ByteArray? {
        // Try 1: exact path (e.g. prog/sm8550_ufs_firehose.elf)
        val bytes = tryOpenAsset(assetPath)
        if (bytes != null) {
            Timber.d("[ASSET] programmer_loaded: $assetPath size=${bytes.size}")
            return bytes
        }

        // Try 2: swap storage type (ufs ↔ emmc) in case only one variant is present
        val swapped = when {
            "_ufs_"  in assetPath -> assetPath.replace("_ufs_",  "_emmc_")
            "_emmc_" in assetPath -> assetPath.replace("_emmc_", "_ufs_")
            else -> null
        }
        if (swapped != null) {
            val swappedBytes = tryOpenAsset(swapped)
            if (swappedBytes != null) {
                Timber.w("[ASSET] programmer_loaded (storage swap): $swapped " +
                         "(original $assetPath not found)")
                return swappedBytes
            }
        }

        Timber.e("[ASSET] programmer_MISSING: $assetPath\n" +
                 "  Run: bash scripts/copy_programmers.sh\n" +
                 "  Or manually: git clone https://github.com/bkerler/Loaders /tmp/qc-loaders && git lfs pull")
        return null
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun tryOpenAsset(path: String): ByteArray? = runCatching {
        context.assets.open(path).use { it.readBytes() }
    }.getOrNull()

    // ── Asset path constants ─────────────────────────────────────────────────

    companion object {
        /** Covers ALL Dimensity chips: MT6833–MT6991 (V6 protocol) */
        const val ALLINONE_V6      = "da/MTK_AllInOne_DA_V6.bin"
        /** Covers ALL Helio / Classic chips: MT6572–MT6785 (BROM Classic) */
        const val ALLINONE_CLASSIC = "da/MTK_AllInOne_DA.bin"
    }
}
