package com.deepeye.otg.data.hardware

object IspDatabase {
    // ISP = In-System Programming (direct eMMC/UFS chip access)
    // DeepEye role: DETECT NEED + GUIDE USER — not implement ISP
    //
    // ISP not possible via Android OTG — requires dedicated hardware:
    //   UFI Box, Easy JTAG Plus, Medusa Pro, etc.

    data class IspEntry(
        val storageChip: String,
        val interface_: String,
        val pinout: String,
        val voltageVcc: String,
        val compatTools: List<String>,
        val notes: String,
    )

    val known: List<IspEntry> = listOf(
        IspEntry(
            storageChip = "Samsung KLUCG4J1ED",
            interface_ = "eMMC",
            pinout = "CMD, CLK, DAT0, VCC(3.3V), GND",
            voltageVcc = "3.3V",
            compatTools = listOf("UFI Box", "Easy JTAG Plus", "Medusa Pro"),
            notes = "Common in Realme/OPPO MT6765 devices",
        ),
        IspEntry(
            storageChip = "Micron MTFC4GACAJCN",
            interface_ = "eMMC",
            pinout = "CMD, CLK, DAT0, VCC(1.8V), GND",
            voltageVcc = "1.8V",
            compatTools = listOf("UFI Box", "EasyJTAG"),
            notes = "Use 1.8V adapter — 3.3V will damage chip",
        ),
    )

    fun shouldSuggestIsp(
        testPointAttempted: Boolean,
        testPointFailed: Boolean,
        cpuResponsive: Boolean,
    ): Boolean = testPointAttempted && testPointFailed && !cpuResponsive
}