package com.deepeye.otg.data

enum class SignalType {
    FULL, NO_SIGNAL, PARTIAL
}

enum class BypassMethod {
    OTG, ADB, CYBER, EDL, FORCE
}

data class BypassItem(
    val id: String,
    val carrier: String,
    val signalType: SignalType,
    val model: String,
    val android: String,
    val method: BypassMethod,
    val priority: Int,
    val isForce: Boolean = false
)

object BypassRepository {

    val allBypasses: List<BypassItem> = listOf(

        // ── SAMSUNG ──────────────────────────────────────────────
        BypassItem(id="sam_001", carrier="Samsung",   signalType=SignalType.FULL,
            model="All Models", android="8-14", method=BypassMethod.OTG,  priority=98),
        BypassItem(id="sam_002", carrier="Samsung",   signalType=SignalType.NO_SIGNAL,
            model="S/Note/A",   android="11-14", method=BypassMethod.ADB,  priority=85),
        BypassItem(id="sam_003", carrier="Samsung",   signalType=SignalType.FULL,
            model="A-Series",   android="13-14", method=BypassMethod.CYBER,priority=90),

        // ── XIAOMI / MIUI ────────────────────────────────────────
        BypassItem(id="xia_001", carrier="Xiaomi",    signalType=SignalType.FULL,
            model="MIUI 12+",   android="10-14", method=BypassMethod.OTG,  priority=95),
        BypassItem(id="xia_002", carrier="Redmi",     signalType=SignalType.NO_SIGNAL,
            model="All",        android="9-13",  method=BypassMethod.ADB,  priority=80),
        BypassItem(id="xia_003", carrier="POCO",      signalType=SignalType.PARTIAL,
            model="F/X/M",      android="11-13", method=BypassMethod.CYBER,priority=75),

        // ── OPPO / REALME / OnePlus ──────────────────────────────
        BypassItem(id="opp_001", carrier="OPPO",      signalType=SignalType.FULL,
            model="ColorOS 12+",android="12-14", method=BypassMethod.OTG,  priority=92),
        BypassItem(id="opp_002", carrier="Realme",    signalType=SignalType.FULL,
            model="All",        android="10-14", method=BypassMethod.OTG,  priority=88),
        BypassItem(id="opp_003", carrier="OnePlus",   signalType=SignalType.NO_SIGNAL,
            model="OxygenOS",   android="11-14", method=BypassMethod.ADB,  priority=82),

        // ── VIVO ─────────────────────────────────────────────────
        BypassItem(id="viv_001", carrier="Vivo",      signalType=SignalType.FULL,
            model="FunTouch 12",android="12-14", method=BypassMethod.OTG,  priority=90),
        BypassItem(id="viv_002", carrier="iQOO",      signalType=SignalType.PARTIAL,
            model="All",        android="11-14", method=BypassMethod.CYBER,priority=78),

        // ── MOTOROLA ─────────────────────────────────────────────
        BypassItem(id="mot_001", carrier="Motorola",  signalType=SignalType.FULL,
            model="All",        android="9-14",  method=BypassMethod.OTG,  priority=94),
        BypassItem(id="mot_002", carrier="Moto G/E",  signalType=SignalType.NO_SIGNAL,
            model="G/E Series", android="10-13", method=BypassMethod.ADB,  priority=79),

        // ── NOKIA ────────────────────────────────────────────────
        BypassItem(id="nok_001", carrier="Nokia",     signalType=SignalType.FULL,
            model="HMD Android", android="10-13", method=BypassMethod.OTG, priority=91),

        // ── TECNO / INFINIX / ITEL ───────────────────────────────
        BypassItem(id="tec_001", carrier="Tecno",     signalType=SignalType.FULL,
            model="All",        android="10-13", method=BypassMethod.OTG,  priority=89),
        BypassItem(id="inf_001", carrier="Infinix",   signalType=SignalType.FULL,
            model="Hot/Note",   android="10-13", method=BypassMethod.OTG,  priority=87),
        BypassItem(id="ite_001", carrier="Itel",      signalType=SignalType.NO_SIGNAL,
            model="A/P Series", android="10-12", method=BypassMethod.ADB,  priority=72),

        // ── HUAWEI / HONOR ───────────────────────────────────────
        BypassItem(id="hua_001", carrier="Huawei",    signalType=SignalType.FULL,
            model="EMUI 9+",    android="9-12",  method=BypassMethod.OTG,  priority=93),
        BypassItem(id="hua_002", carrier="Honor",     signalType=SignalType.PARTIAL,
            model="All",        android="10-13", method=BypassMethod.CYBER,priority=77),

        // ── MTK GENERIC ──────────────────────────────────────────
        BypassItem(id="mtk_001", carrier="MTK Bypass",signalType=SignalType.NO_SIGNAL,
            model="MTK Chipset",android="8-14",  method=BypassMethod.EDL,  priority=70,
            isForce=true),
        BypassItem(id="mtk_002", carrier="MTK BROM",  signalType=SignalType.NO_SIGNAL,
            model="All MTK",    android="8-14",  method=BypassMethod.FORCE,priority=65,
            isForce=true),

        // ── QCOM GENERIC ─────────────────────────────────────────
        BypassItem(id="qcm_001", carrier="Qualcomm",  signalType=SignalType.NO_SIGNAL,
            model="EDL Mode",   android="8-14",  method=BypassMethod.EDL,  priority=68,
            isForce=true),

        // ── ATT / CARRIER LOCKED ─────────────────────────────────
        BypassItem(id="att_001", carrier="AT&T Bypass",signalType=SignalType.NO_SIGNAL,
            model="Locked",    android="10-13", method=BypassMethod.ADB,  priority=66,
            isForce=true),
        BypassItem(id="att_002", carrier="T-Mobile",  signalType=SignalType.PARTIAL,
            model="Locked",    android="10-13", method=BypassMethod.CYBER,priority=63),

        // ── GENERIC MULTI ────────────────────────────────────────
        BypassItem(id="gen_001", carrier="Universal",  signalType=SignalType.FULL,
            model="Android 10+",android="10-14", method=BypassMethod.OTG, priority=60),
        BypassItem(id="gen_002", carrier="ADB Method", signalType=SignalType.NO_SIGNAL,
            model="Debuggable", android="8-14",  method=BypassMethod.ADB, priority=55),
    )

    // Filter helpers
    fun byBrand(brand: String) = allBypasses.filter {
        it.carrier.contains(brand, ignoreCase = true)
    }

    fun recommended(brand: String, androidVer: String) = allBypasses
        .filter {
            it.carrier.contains(brand, ignoreCase = true) ||
            brand.contains(it.carrier, ignoreCase = true)
        }
        .sortedByDescending { it.priority }
        .take(3)

    fun topBySignal(signal: SignalType) = allBypasses
        .filter { it.signalType == signal }
        .sortedByDescending { it.priority }
}
