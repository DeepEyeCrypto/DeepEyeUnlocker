package com.deepeye.simulation

class SamsungMtpSimulator(
    private val scenario: SimScenario = SimScenario.SUCCESS
) {
    enum class SimScenario {
        SUCCESS,
        MODEM_NOT_FOUND,     // ADB shell can't find modem interface
        FRP_ALREADY_CLEAR,   // FRP not set — nothing to erase
        ANDROID_VERSION_OLD, // Below Android 12 — method unsupported
    }

    private var state = MtpState.CONNECTED

    enum class MtpState {
        CONNECTED, MTP_ACTIVE, MODEM_ACTIVE, FRP_ERASED
    }

    fun switchToMtpMode(): Boolean {
        if (scenario == SimScenario.MODEM_NOT_FOUND) return false
        state = MtpState.MTP_ACTIVE
        return true
    }

    fun switchToModemMode(): Boolean {
        if (scenario == SimScenario.MODEM_NOT_FOUND) {
            println("[SIM_SAMSUNG] Modem interface not found — " +
                    "device may not support Modem FRP method")
            return false
        }
        state = MtpState.MODEM_ACTIVE
        return true
    }

    fun eraseFrp(): SimResult {
        return when (scenario) {
            SimScenario.SUCCESS -> {
                state = MtpState.FRP_ERASED
                SimResult.Success("FRP erased via Modem mode")
            }
            SimScenario.FRP_ALREADY_CLEAR ->
                SimResult.Success("FRP already clear — nothing to do")
            SimScenario.ANDROID_VERSION_OLD ->
                SimResult.Failure("Android version below 12 — method unsupported")
            else ->
                SimResult.Failure("Unknown failure")
        }
    }
}

sealed class SimResult {
    data class Success(val message: String) : SimResult()
    data class Failure(val reason: String) : SimResult()
}
