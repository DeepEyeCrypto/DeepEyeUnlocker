package com.deepeye.otg.device

import kotlinx.serialization.Serializable

@Serializable
data class TestpointGuide(
    val model:        String,
    val chipset:      String,
    val method:       String,
    val steps:        List<String>,
    val warning:      String?       = null,
    val imagePath:    String?       = null,
    val bromVid:      Int           = 0x0E8D,
    val bromPid:      Int           = 0x0003,
    val difficulty:   Int           = 3,         // 1-5
)

object TestpointDb {

    fun getGuide(model: String, chipset: String): TestpointGuide {
        val chip = chipset.uppercase()
        return when {
            chip.contains("MT67") || chip.contains("MT6833") ||
            chip.contains("MT6853") || chip.contains("MT6873") -> mtkModernGuide(model, chipset)

            chip.contains("MT65") || chip.contains("MT6580") -> mtkLegacyGuide(model, chipset)

            chip.contains("SDM") || chip.contains("SM6") ||
            chip.contains("SM7") || chip.contains("SM8") -> qcomGuide(model, chipset)

            chip.contains("EXYNOS") || model.uppercase().contains("SAMSUNG") -> samsungGuide(model)

            chip.contains("KIRIN") || model.uppercase().contains("HUAWEI") -> kirinGuide(model)

            else -> genericGuide(model, chipset)
        }
    }

    private fun mtkModernGuide(model: String, chipset: String) = TestpointGuide(
        model     = model,
        chipset   = chipset,
        method    = "ShortToGround",
        difficulty = 4,
        steps     = listOf(
            "1. Power OFF device — hold power 10s if needed",
            "2. Remove SIM tray with ejector pin",
            "3. Carefully open back panel (heat edges 60°C)",
            "4. Locate testpoint near CPU (see diagram)",
            "5. Connect USB-C to PC (do NOT connect to device yet)",
            "6. Short testpoint to GND pad with metal tweezers",
            "7. While shorting — plug USB to device",
            "8. Wait 2-3 seconds — release short",
            "9. Check PC: 'MTK USB Port' should appear",
            "10. DeepEye BROM detected! ✓",
        ),
        imagePath = "testpoints/${chipset.lowercase()}.png",
        warning   = "⚠️ Shorting wrong pad may permanently brick device. Use magnifier.",
        bromVid   = 0x0E8D,
        bromPid   = 0x0003,
    )

    private fun mtkLegacyGuide(model: String, chipset: String) = TestpointGuide(
        model     = model,
        chipset   = chipset,
        method    = "ShortToGround",
        difficulty = 2,
        steps     = listOf(
            "1. Power OFF — remove battery if removable",
            "2. Locate testpoint on motherboard edge",
            "3. Short testpoint to GND (battery negative terminal)",
            "4. Insert battery while holding short",
            "5. Connect USB to PC",
            "6. Release after 1 second",
            "7. BROM device should appear in Device Manager",
        ),
        imagePath = "testpoints/mtk_legacy.png",
        warning   = "For removable battery devices only",
        bromVid   = 0x0E8D,
        bromPid   = 0x0003,
    )

    private fun qcomGuide(model: String, chipset: String) = TestpointGuide(
        model     = model,
        chipset   = chipset,
        method    = "ShortToGround_OR_SoftEDL",
        difficulty = 3,
        steps     = listOf(
            "METHOD A — Software EDL (easier):",
            "  1. Enable ADB on device",
            "  2. Run: adb reboot edl",
            "  3. Device appears as 'Qualcomm HS-USB QDL 9008'",
            "",
            "METHOD B — Testpoint (hardware):",
            "  1. Power OFF device",
            "  2. Open device and locate EDL pad (D+ to GND)",
            "  3. Short EDL pad before connecting USB",
            "  4. Connect USB — device should enumerate as 9008",
            "",
            "VERIFY: Device Manager → 'Qualcomm HS-USB QDL 9008'",
        ),
        imagePath = "testpoints/qualcomm_${chipset.lowercase()}.png",
        warning   = "⚠️ Signed EDL programmer required for newer Qualcomm devices",
        bromVid   = 0x05C6,
        bromPid   = 0x9008,
    )

    private fun samsungGuide(model: String) = TestpointGuide(
        model     = model,
        chipset   = "Exynos/Snapdragon",
        method    = "ButtonCombo_Download",
        difficulty = 1,
        steps     = listOf(
            "1. Power OFF device completely",
            "2. Hold Vol- + Bixby/Home + Power (3-key combo)",
            "   OR: Vol- only on newer S-series",
            "3. Connect USB while holding",
            "4. Press Vol+ to confirm Download Mode warning",
            "5. Device enters Odin/Download mode",
            "6. Use Odin or DeepEye for flashing",
        ),
        warning   = "Samsung FRP also removable via Find My Mobile or EDL on some models",
        bromVid   = 0x04E8,
        bromPid   = 0x685D,
    )

    private fun kirinGuide(model: String) = TestpointGuide(
        model     = model,
        chipset   = "Kirin",
        method    = "ButtonCombo_OR_TestPoint",
        difficulty = 4,
        steps     = listOf(
            "1. Power OFF device",
            "2. Hold Vol- then connect USB",
            "   OR: Use HiSuite for unlock",
            "3. HUAWEI devices after 2018 require bootloader unlock code from official portal",
            "4. Testpoint method: short TP to GND — enters HiSilicon BROM",
        ),
        warning   = "⚠️ Huawei bootloader unlock officially discontinued for most models",
        bromVid   = 0x12D1,
        bromPid   = 0x1052,
    )

    private fun genericGuide(model: String, chipset: String) = TestpointGuide(
        model     = model,
        chipset   = chipset,
        method    = "ButtonCombo",
        difficulty = 2,
        steps     = listOf(
            "1. Power OFF device",
            "2. Hold Vol+ + Vol- + Power simultaneously",
            "3. Connect USB cable while holding buttons",
            "4. Check Device Manager for BROM/EDL device",
            "5. Try different button combos if no response",
        ),
        bromVid   = 0x0E8D,
        bromPid   = 0x0003,
    )
}
