package com.deepeye.otg.usb

import android.content.res.AssetManager
import java.io.IOException

class MtkAuthHandler {

    fun requiresAuth(chip: DeviceMatrix.MtkChipFamily): Boolean = chip in listOf(
        DeviceMatrix.MtkChipFamily.MT6761,
        DeviceMatrix.MtkChipFamily.MT6763,
        DeviceMatrix.MtkChipFamily.MT6765,
        DeviceMatrix.MtkChipFamily.MT6768,
        DeviceMatrix.MtkChipFamily.MT6771,
        DeviceMatrix.MtkChipFamily.MT6785,
        DeviceMatrix.MtkChipFamily.MT6833,
        DeviceMatrix.MtkChipFamily.MT6877,
        DeviceMatrix.MtkChipFamily.MT6893,
        DeviceMatrix.MtkChipFamily.DIMENSITY_700,
        DeviceMatrix.MtkChipFamily.DIMENSITY_900,
        DeviceMatrix.MtkChipFamily.DIMENSITY_1000,
        DeviceMatrix.MtkChipFamily.DIMENSITY_8000
    )

    fun loadPatchedDa(
        chip: DeviceMatrix.MtkChipFamily,
        assetManager: AssetManager
    ): ByteArray? {
        val daFile = when (chip) {
            DeviceMatrix.MtkChipFamily.MT6761 -> "da/mt6761_patched.bin"
            DeviceMatrix.MtkChipFamily.MT6765 -> "da/mt6765_patched.bin"
            DeviceMatrix.MtkChipFamily.MT6768 -> "da/mt6768_patched.bin"
            DeviceMatrix.MtkChipFamily.MT6771 -> "da/mt6771_patched.bin"
            DeviceMatrix.MtkChipFamily.MT6785 -> "da/mt6785_patched.bin"
            DeviceMatrix.MtkChipFamily.MT6833 -> "da/mt6833_patched.bin"
            else -> return null
        }

        return try {
            assetManager.open(daFile).use { it.readBytes() }
        } catch (_: IOException) {
            null
        }
    }
}

