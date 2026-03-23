package com.deepeye.otg

import android.content.Context
import java.io.File
import java.io.FileOutputStream

/**
 * DaManager — caches DA binaries extracted from assets to internal storage.
 * Uses AllInOne strategy: two files cover all MTK chips.
 *
 * Required assets:
 *   assets/da/MTK_AllInOne_DA_V6.bin  → ALL Dimensity chips (MTK V6 protocol)
 *   assets/da/MTK_AllInOne_DA.bin     → ALL Helio / Classic chips (BROM Classic)
 */
class DaManager(private val context: Context) {

    private val cacheDir = File(context.filesDir, "mtk_da").also { it.mkdirs() }

    /** Load DA bytes for a V6/Dimensity chip. Returns null if asset missing. */
    fun getDaV6(): ByteArray? = loadFromAsset("da/MTK_AllInOne_DA_V6.bin")

    /** Load DA bytes for a Classic/Helio chip. Returns null if asset missing. */
    fun getDaClassic(): ByteArray? = loadFromAsset("da/MTK_AllInOne_DA.bin")

    /**
     * Load DA bytes for the given chipset name.
     * Tries chip-specific first (da/mt{chipset}_da.bin),
     * then falls back to the appropriate AllInOne.
     *
     * @param chipset  hex hw_code string e.g. "6835t" or "6765"
     * @param isV6     true for Dimensity / V6 protocol chips
     */
    fun getDaForChipset(chipset: String, isV6: Boolean = false): ByteArray? {
        // Try chip-specific DA
        val specific = loadFromAsset("da/mt${chipset}_da.bin")
        if (specific != null) return specific

        // Fallback: AllInOne
        return if (isV6) getDaV6() else getDaClassic()
    }

    /** List all DA files currently cached in internal storage. */
    fun listCachedDas(): List<String> = cacheDir.list()?.toList() ?: emptyList()

    // ── Internal ─────────────────────────────────────────────────────────

    private fun loadFromAsset(assetPath: String): ByteArray? = runCatching {
        context.assets.open(assetPath).use { it.readBytes() }
    }.getOrNull()
}
