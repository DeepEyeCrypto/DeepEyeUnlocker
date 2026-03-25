package com.deepeye.simulation

class MtkBromSimulator(
    private val device: VirtualUsbDescriptor,
    private val scenario: SimScenario = SimScenario.SUCCESS
) {
    enum class SimScenario {
        SUCCESS,
        DA_REJECTED,       // BROM rejects DA (wrong chip)
        PAYLOAD_FAIL,      // kamakiri exploit fails
        AUTH_REQUIRED,     // SLA/DAA authentication needed
        WRONG_DA_SIZE,     // DA file < 1MB (payload uploaded by mistake)
        V5_CHIP_V6_DA,     // V5 chip but V6 DA uploaded
    }

    private var state = BromState.CONNECTED
    private var daSize = 0

    enum class BromState {
        CONNECTED, PAYLOAD_SENT, DA_LOADED, FRP_ERASED
    }

    // MTK BROM magic bytes
    private val BROM_HELLO = byteArrayOf(0x00)
    private val BROM_ACK   = byteArrayOf(0x5A.toByte())
    private val BROM_NACK  = byteArrayOf(0xA5.toByte())

    fun handshake(): ByteArray {
        return BROM_HELLO + byteArrayOf(
            device.hwCode.shr(8).toByte(),
            device.hwCode.and(0xFF).toByte()
        )
    }

    fun uploadPayload(payload: ByteArray): ByteArray {
        if (scenario == SimScenario.PAYLOAD_FAIL)
            return BROM_NACK

        if (payload.size > 1000) { // payload should be ~612B
            return BROM_NACK.also {
                println("[SIM] WARN: Payload too large ${payload.size}B — " +
                        "DA uploaded instead of payload?")
            }
        }

        state = BromState.PAYLOAD_SENT
        return BROM_ACK
    }

    fun uploadDa(da: ByteArray): ByteArray {
        daSize = da.size

        when (scenario) {
            SimScenario.DA_REJECTED -> {
                // V5 DA on V6 chip
                if (device.hwCode == 0x1209 && da.size < 7_000_000)
                    return BROM_NACK.also {
                        println("[SIM] DA rejected — V6 chip needs V6 DA > 7MB")
                    }
            }
            SimScenario.WRONG_DA_SIZE -> {
                // Payload uploaded instead of DA
                if (da.size < 1_000_000)
                    return BROM_NACK.also {
                        println("[SIM] CRITICAL: file ${da.size}B is a payload, not DA!")
                    }
            }
            SimScenario.AUTH_REQUIRED -> {
                return BROM_NACK.also {
                    println("[SIM] Auth required — SLA/DAA enabled on this device")
                }
            }
            else -> {}
        }

        state = BromState.DA_LOADED
        return BROM_ACK
    }

    fun eraseFrp(): ByteArray {
        if (state != BromState.DA_LOADED)
            return BROM_NACK.also {
                println("[SIM] Cannot erase FRP — DA not loaded. State=$state")
            }

        state = BromState.FRP_ERASED
        return BROM_ACK
    }

    fun isSuccess() = state == BromState.FRP_ERASED
    fun getState() = state
    fun getDaSize() = daSize
}
