package com.deepeye.otg.data.hardware

data class TestPointEntry(
    val deviceModel: String,
    val chipset: String,
    val manufacturer: String,
    val targetVid: Int?,
    val targetPid: Int?,
    val location: String,
    val method: String,
    val timing: String,
    val riskLevel: String,
    val confidence: TestPointConfidence,
    val communitySource: String,
    val notes: String,
)

enum class TestPointConfidence {
    COMMUNITY,
    VERIFIED,
    CONFIRMED,
}

object TestPointDatabase {
    val entries: List<TestPointEntry> = listOf(
        TestPointEntry(
            deviceModel = "Realme 14x",
            chipset = "MT6835T",
            manufacturer = "Realme",
            targetVid = 0x0E8D,
            targetPid = 0x0003,
            location = "TP_BROM pad near USB controller IC",
            method = "Short TP_BROM to GND with metal tweezers",
            timing = "Apply short BEFORE connecting USB cable",
            riskLevel = "MEDIUM",
            confidence = TestPointConfidence.COMMUNITY,
            communitySource = "XDA Developers Realme 14x thread",
            notes = "Remove back cover first. TP is small — use fine-tip tool.",
        ),
        TestPointEntry(
            deviceModel = "Realme C35",
            chipset = "MT6765",
            manufacturer = "Realme",
            targetVid = 0x0E8D,
            targetPid = 0x0003,
            location = "CLK test point near eMMC chip",
            method = "Short CLK to GND",
            timing = "During power-on sequence",
            riskLevel = "HIGH",
            confidence = TestPointConfidence.COMMUNITY,
            communitySource = "GSMForum",
            notes = "High risk — wrong pad = dead eMMC",
        ),
        TestPointEntry(
            deviceModel = "OPPO A77",
            chipset = "MT6765",
            manufacturer = "OPPO",
            targetVid = 0x0E8D,
            targetPid = 0x0003,
            location = "Same as Realme C35 (shared PCB)",
            method = "Short CLK to GND",
            timing = "During power-on",
            riskLevel = "HIGH",
            confidence = TestPointConfidence.COMMUNITY,
            communitySource = "GSMForum",
            notes = "Verify PCB revision before attempting",
        ),
        TestPointEntry(
            deviceModel = "Vivo Y35",
            chipset = "MT6765",
            manufacturer = "Vivo",
            targetVid = 0x0E8D,
            targetPid = 0x0003,
            location = "TP near PMIC chip",
            method = "Short to GND",
            timing = "Before USB connect",
            riskLevel = "HIGH",
            confidence = TestPointConfidence.COMMUNITY,
            communitySource = "XDA",
            notes = "Locate PMIC — usually largest IC near battery connector",
        ),
        TestPointEntry(
            deviceModel = "Samsung Galaxy A32",
            chipset = "MT6769",
            manufacturer = "Samsung",
            targetVid = 0x0E8D,
            targetPid = 0x0003,
            location = "ISP eMMC direct connection pads",
            method = "Direct eMMC read (ISP)",
            timing = "N/A — direct chip access",
            riskLevel = "EXTREME",
            confidence = TestPointConfidence.COMMUNITY,
            communitySource = "GSMForum ISP pinout thread",
            notes = "Requires hot air station + ISP adapter. CPU bypass.",
        ),
        TestPointEntry(
            deviceModel = "Xiaomi Redmi 9",
            chipset = "MT6769",
            manufacturer = "Xiaomi",
            targetVid = 0x0E8D,
            targetPid = 0x0003,
            location = "CLK short point near USB controller",
            method = "Short CLK to GND",
            timing = "During boot, before logo appears",
            riskLevel = "HIGH",
            confidence = TestPointConfidence.COMMUNITY,
            communitySource = "Xiaomi.eu forum",
            notes = "Must be exact timing — too late = misses BROM window",
        ),
    )

    fun forDevice(model: String): List<TestPointEntry> =
        entries.filter { it.deviceModel.lowercase().contains(model.lowercase()) }

    fun forChipset(chipset: String): List<TestPointEntry> =
        entries.filter { it.chipset.lowercase().contains(chipset.lowercase()) }

    fun confirmedOnly(): List<TestPointEntry> =
        entries.filter { it.confidence == TestPointConfidence.CONFIRMED }
}