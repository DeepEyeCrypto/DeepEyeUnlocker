package com.deepeye.otg.domain.engine.mtk

object MtkChipDatabase {
    private val chipMap = mapOf(
        0x1209 to "MT6789 / MT6833 (Helio G99 / Dimensity 700)",
        0x0707 to "MT6765 (Helio P35)",
        0x0766 to "MT6761 (Helio A22)",
        0x0335 to "MT6735",
        0x0321 to "MT6737",
        0x0279 to "MT6797 (Helio X20)",
        0x0633 to "MT6753",
        0x0813 to "MT6877 (Dimensity 900)",
        0x0951 to "MT6893 (Dimensity 1200)",
        0x6789 to "MT6789 (Helio G99)"
    )

    fun getChipName(hwCode: Int): String {
        return chipMap[hwCode] ?: "UNKNOWN MTK (hw_code: 0x${hwCode.toString(16)})"
    }

    /**
     * Common OPLUS MTK Chipsets
     */
    fun isOplusSupported(hwCode: Int): Boolean {
        return hwCode == 0x1209 || hwCode == 0x0707 || hwCode == 0x0813
    }
}
