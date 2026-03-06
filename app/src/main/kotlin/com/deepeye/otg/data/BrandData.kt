package com.deepeye.otg.data

import androidx.compose.ui.graphics.Color

// Research: official brand colors used in their UI/logos
data class BrandInfo(
    val index: Int,
    val name: String,
    val shortName: String,
    val icon: String,           // emoji fallback
    val brandColor: Color,      // official brand color
    val chipsetFamily: String,  // QC / MTK / BOTH
    val popularChipsets: String // info shown in tooltip
)

object BrandData {
    val brands = listOf(
        BrandInfo(
            index = 0,
            name = "Xiaomi",
            shortName = "Mi",
            icon = "小",
            brandColor = Color(0xFFFF6900),    // Xiaomi official orange
            chipsetFamily = "BOTH",
            popularChipsets = "SD8Gen3, SD7s Gen3, D9300, G99"
        ),
        BrandInfo(
            index = 1,
            name = "Samsung",
            shortName = "Sam",
            icon = "S",
            brandColor = Color(0xFF1428A0),    // Samsung official blue
            chipsetFamily = "BOTH",
            popularChipsets = "SD8Gen2, Exynos2400, SD7 Gen1"
        ),
        BrandInfo(
            index = 2,
            name = "Oppo",
            shortName = "Op",
            icon = "O",
            brandColor = Color(0xFF1D6FA4),    // Oppo official blue
            chipsetFamily = "BOTH",
            popularChipsets = "SD8Gen3, D9200, SD7s Gen3"
        ),
        BrandInfo(
            index = 3,
            name = "Vivo",
            shortName = "Vv",
            icon = "V",
            brandColor = Color(0xFF415FFF),    // Vivo official blue-violet
            chipsetFamily = "BOTH",
            popularChipsets = "SD8Gen2, D9200, SD7 Gen1"
        ),
        BrandInfo(
            index = 4,
            name = "Realme",
            shortName = "Rm",
            icon = "R",
            brandColor = Color(0xFFFFD000),    // Realme official yellow
            chipsetFamily = "BOTH",
            popularChipsets = "SD8s Gen3, D9300, G99, SD6 Gen1"
        ),
        BrandInfo(
            index = 5,
            name = "OnePlus",
            shortName = "1+",
            icon = "1+",
            brandColor = Color(0xFFEB0029),    // OnePlus official red
            chipsetFamily = "QC",
            popularChipsets = "SD8Gen3, SD8Gen2, SD8+ Gen1"
        )
    )

    fun get(index: Int): BrandInfo =
        brands.getOrElse(index) { brands[0] }
}
