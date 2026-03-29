package com.deepeye.otg

// Helper class to guide users into the correct service mode based on Brand/SoC
object ModeHelper {

    data class ModeGuidance(
        val requiredMode: String,
        val steps: List<String>,
        val alternativeSteps: List<String> = emptyList(),
        val safetyNotes: List<String> = emptyList()
    )

    fun getGuidance(brand: String, model: String, socType: String): ModeGuidance {
        val brandNorm = brand.uppercase()
        val socNorm = socType.uppercase() // "QUALCOMM", "MTK", "SPD", "EXYNOS"

        return when (brandNorm) {
            "XIAOMI", "REDMI", "POCO" -> getXiaomiGuidance(socNorm)
            "OPPO", "REALME", "ONEPLUS" -> getOppoRealmeGuidance(socNorm)
            "VIVO" -> getVivoGuidance(socNorm)
            "SAMSUNG" -> getSamsungGuidance(socNorm)
            "HUAWEI", "HONOR" -> getHuaweiGuidance(socNorm)
            "MOTOROLA", "LENOVO" -> getMotorolaGuidance(socNorm)
            "GOOGLE", "PIXEL" -> getGoogleGuidance(socNorm)
            "ASUS" -> getAsusGuidance(socNorm)
            "LG" -> getLgGuidance(socNorm)
            "APPLE", "IPHONE", "IPAD" -> getAppleGuidance(socNorm)
            else -> getGenericGuidance(socNorm)
        }
    }

    private fun getXiaomiGuidance(soc: String): ModeGuidance {
        return if (soc.contains("MTK")) {
            ModeGuidance(
                requiredMode = "BROM / Preloader",
                steps = listOf(
                    "1. Power off the device completely.",
                    "2. Hold Volume Up (or Volume Down) button.",
                    "3. Connect USB cable while holding the button."
                ),
                alternativeSteps = listOf("Try holding both Volume buttons if single button fails."),
                safetyNotes = listOf("Some newer models require a test-point if BROM is disabled (SEC CTRL).")
            )
        } else { // Qualcomm
            ModeGuidance(
                requiredMode = "EDL (9008)",
                steps = listOf(
                    "1. Power off the device completely.",
                    "2. Hold BOTH Volume Up + Volume Down.",
                    "3. Connect USB cable while holding buttons.",
                    "4. Screen should remain black."
                ),
                alternativeSteps = listOf(
                    "If Fastboot is available: 'fastboot oem edl'",
                    "Use DeepEye EDL Cable if buttons fail."
                ),
                safetyNotes = listOf("Test-point required if software EDL features are blocked.")
            )
        }
    }

    private fun getOppoRealmeGuidance(soc: String): ModeGuidance {
        return if (soc.contains("MTK")) {
            ModeGuidance(
                requiredMode = "BROM (BootROM)",
                steps = listOf(
                    "1. Power off device.",
                    "2. Hold Volume Up + Volume Down.",
                    "3. Connect USB cable."
                ),
                alternativeSteps = listOf(
                    "Try holding only Volume Up.",
                    "Auth Bypass may be required for some models."
                )
            )
        } else {
            ModeGuidance(
                requiredMode = "EDL (9008)",
                steps = listOf(
                    "1. Power off device.",
                    "2. Hold Volume Up + Volume Down.",
                    "3. Connect USB cable."
                ),
                safetyNotes = listOf("Oppo/Realme often require test-points for EDL on newer security.")
            )
        }
    }

    private fun getVivoGuidance(soc: String): ModeGuidance {
        return if (soc.contains("MTK")) {
            ModeGuidance(
                requiredMode = "BROM / Preloader",
                steps = listOf(
                    "1. Power off.",
                    "2. Hold Volume Up.",
                    "3. Connect USB cable."
                ),
                alternativeSteps = listOf("Try Volume Up + Power."),
                safetyNotes = listOf("Vivo security is tight. Test-point often needed for MTK BROM.")
            )
        } else {
            ModeGuidance(
                requiredMode = "EDL (9008)",
                steps = listOf(
                    "1. Power off.",
                    "2. Hold Volume Up + Volume Down.",
                    "3. Connect USB.",
                    "4. If 'Fastboot' appears, retry with test-point."
                ),
                alternativeSteps = listOf("Fastboot 'fastboot oem edl' rarely works on Vivo."),
                safetyNotes = listOf("Test-point is the most reliable method for Vivo Qualcomm EDL.")
            )
        }
    }

    private fun getSamsungGuidance(soc: String): ModeGuidance {
        return if (soc.contains("MTK")) {
            ModeGuidance(
                requiredMode = "BROM (Testpoint)",
                steps = listOf(
                    "1. Samsung BROM almost ALWAYS requires Test-Point.",
                    "2. Disconnect battery.",
                    "3. Short test-points.",
                    "4. Connect USB."
                ),
                alternativeSteps = listOf("Sometimes works w/o testpoint on very old security (Vol+ & Vol-)."),
                safetyNotes = listOf("Opening back cover required. Advanced users only.")
            )
        } else {
            ModeGuidance(
                requiredMode = "Download Mode (Odin)",
                steps = listOf(
                    "1. Power off.",
                    "2. Hold Vol Up + Vol Down.",
                    "3. Connect USB.",
                    "4. Press Vol Up to continue when prompted."
                )
            )
        }
    }

    private fun getHuaweiGuidance(soc: String): ModeGuidance {
         // Kirin/HiSilicon logic
         return ModeGuidance(
             requiredMode = "USB COM 1.0 (Testpoint)",
             steps = listOf(
                 "1. Power off.",
                 "2. Short Test-Point to Ground.",
                 "3. Connect USB."
             ),
             alternativeSteps = listOf("HarmonyTP cable may be required."),
             safetyNotes = listOf("Requires specific drivers (USB SER).")
         )
    }

    private fun getMotorolaGuidance(soc: String): ModeGuidance {
        return if (soc.contains("MTK")) {
            ModeGuidance(
                requiredMode = "BROM / Preloader",
                steps = listOf(
                    "1. Power off device.",
                    "2. Hold Volume Down + Power.",
                    "3. Connect USB cable."
                ),
                alternativeSteps = listOf("Try Volume Up + Power for some models."),
                safetyNotes = listOf("Motorola MTK devices are rare; most are Qualcomm.")
            )
        } else {
            ModeGuidance(
                requiredMode = "Fastboot / EDL (9008)",
                steps = listOf(
                    "1. Power off device.",
                    "2. Hold Volume Down + Power.",
                    "3. Connect USB cable.",
                    "4. Select 'AP Fastboot Flash Mode' from boot menu."
                ),
                alternativeSteps = listOf(
                    "Use 'adb reboot bootloader' if ADB available.",
                    "For EDL: 'adb reboot edl' or test-point."
                ),
                safetyNotes = listOf("Motorola bootloader unlock required for many operations.")
            )
        }
    }

    private fun getGoogleGuidance(soc: String): ModeGuidance {
        // Google Pixel devices are Qualcomm (except Tensor which is Exynos-based)
        return ModeGuidance(
            requiredMode = "Fastboot / EDL (9008)",
            steps = listOf(
                "1. Power off device.",
                "2. Hold Volume Down + Power.",
                "3. Connect USB cable."
            ),
            alternativeSteps = listOf(
                "Use 'adb reboot bootloader' or 'adb reboot edl'.",
                "For Tensor devices: Use 'adb reboot fastboot'."
            ),
            safetyNotes = listOf("Pixel devices have strict bootloader locking; unlock via OEM unlocking in developer options.")
        )
    }

    private fun getAsusGuidance(soc: String): ModeGuidance {
        return if (soc.contains("MTK")) {
            ModeGuidance(
                requiredMode = "BROM / Preloader",
                steps = listOf(
                    "1. Power off device.",
                    "2. Hold Volume Up + Power.",
                    "3. Connect USB cable."
                ),
                alternativeSteps = listOf("Try Volume Down + Power for some models."),
                safetyNotes = listOf("Asus MTK devices may require test-point for BROM.")
            )
        } else {
            ModeGuidance(
                requiredMode = "Fastboot / Download Mode",
                steps = listOf(
                    "1. Power off device.",
                    "2. Hold Volume Up + Power.",
                    "3. Connect USB cable."
                ),
                alternativeSteps = listOf("Use 'adb reboot bootloader' if ADB available."),
                safetyNotes = listOf("Asus Qualcomm devices may have custom download mode.")
            )
        }
    }

    private fun getLgGuidance(soc: String): ModeGuidance {
        return if (soc.contains("MTK")) {
            ModeGuidance(
                requiredMode = "BROM / Preloader",
                steps = listOf(
                    "1. Power off device.",
                    "2. Hold Volume Up + Power.",
                    "3. Connect USB cable."
                ),
                alternativeSteps = listOf("Try Volume Down + Power for some models."),
                safetyNotes = listOf("LG MTK devices are uncommon.")
            )
        } else {
            ModeGuidance(
                requiredMode = "Download Mode (9008)",
                steps = listOf(
                    "1. Power off device.",
                    "2. Hold Volume Up + Power.",
                    "3. Connect USB cable.",
                    "4. When LG logo appears, release Power and press again."
                ),
                alternativeSteps = listOf("Use 'adb reboot download' if supported."),
                safetyNotes = listOf("LG devices may require specific drivers for download mode.")
            )
        }
    }

    private fun getAppleGuidance(soc: String): ModeGuidance {
        // iOS devices: DFU mode (checkm8) or Recovery mode
        return ModeGuidance(
            requiredMode = "DFU Mode (checkm8)",
            steps = listOf(
                "1. Connect device to USB while powered on.",
                "2. Press Volume Up quickly, then Volume Down quickly (iPhone 7 and later).",
                "3. Hold Side button until screen goes black.",
                "4. After 3 seconds, release Side button but keep holding Volume Down.",
                "5. Continue holding Volume Down for 5 seconds (iPhone 7/8) or 10 seconds (iPhone X and later)."
            ),
            alternativeSteps = listOf(
                "For older devices (iPhone 6s and earlier): Hold Home + Power.",
                "Use checkm8 exploit via ipwndfu."
            ),
            safetyNotes = listOf("DFU mode required for checkm8 exploit. Ensure USB host controller supports timing.")
        )
    }

    private fun getGenericGuidance(soc: String): ModeGuidance {
        return if (soc.contains("MTK")) {
            ModeGuidance(
                requiredMode = "BROM / Preloader",
                steps = listOf("Power Off > Hold Vol+ > Connect USB"),
                alternativeSteps = listOf("Try Vol- or Vol+ & Vol-")
            )
        } else {
             ModeGuidance(
                requiredMode = "EDL (9008) / Fastboot",
                steps = listOf("Power Off > Hold Vol+ & Vol- > Connect USB"),
                alternativeSteps = listOf("Use 'adb reboot edl' or 'adb reboot bootloader'")
            )
        }
    }
}
