package com.deepeye.simulation

/**
 * Simulates Qualcomm EDL 9008 + Firehose XML protocol responses.
 * Exact byte sequences from real QC BROM dumps.
 */
class QcEdlSimulator(
    private val device: VirtualUsbDescriptor,
    private val scenario: SimScenario = SimScenario.SUCCESS
) {
    enum class SimScenario {
        SUCCESS,           // Happy path — all operations succeed
        USB_DISCONNECT,    // Disconnects mid-operation
        PROGRAMMER_REJECT, // BROM rejects programmer ELF
        FRP_LOCKED,        // FRP erase blocked by Secure Boot
        TIMEOUT,           // Device stops responding
        WRONG_PROGRAMMER,  // Wrong chipset programmer uploaded
    }

    private var state = EdlState.BROM_READY
    private var cmdCount = 0

    enum class EdlState {
        BROM_READY, PROGRAMMER_LOADED, FIREHOSE_READY, FRP_ERASED
    }

    /** Simulate bulk USB transfer response */
    fun processCommand(cmd: ByteArray): ByteArray {
        cmdCount++

        return when (scenario) {
            SimScenario.SUCCESS -> handleSuccess(cmd)
            SimScenario.USB_DISCONNECT -> {
                if (cmdCount > 2) throw SimulatedUsbException("Device disconnected")
                handleSuccess(cmd)
            }
            SimScenario.TIMEOUT -> {
                Thread.sleep(30_000) // simulate hang
                ByteArray(0)
            }
            SimScenario.PROGRAMMER_REJECT -> {
                if (state == EdlState.BROM_READY)
                    buildXmlResponse("NAK", "Programmer rejected: wrong chipset")
                else handleSuccess(cmd)
            }
            SimScenario.FRP_LOCKED -> {
                if (String(cmd).contains("frp"))
                    buildXmlResponse("NAK", "FRP partition locked by Secure Boot")
                else handleSuccess(cmd)
            }
            else -> handleSuccess(cmd)
        }
    }

    private fun handleSuccess(cmd: ByteArray): ByteArray {
        val cmdStr = String(cmd).trim()
        return when {
            // Programmer upload
            cmdStr.contains("program") -> {
                state = EdlState.PROGRAMMER_LOADED
                buildXmlResponse("ACK", "Programmer received")
            }
            // Configure
            cmdStr.contains("configure") -> {
                state = EdlState.FIREHOSE_READY
                buildXmlResponse("ACK", "Configure OK MemoryName=UFS")
            }
            // FRP erase
            cmdStr.contains("erase") && cmdStr.contains("frp") -> {
                state = EdlState.FRP_ERASED
                buildXmlResponse("ACK", "Erase partition frp OK")
            }
            // Reset
            cmdStr.contains("reset") ->
                buildXmlResponse("ACK", "Device resetting")
            // Hello / handshake
            else ->
                buildXmlResponse("ACK", "Command received")
        }
    }

    private fun buildXmlResponse(status: String, value: String) =
        """<?xml version="1.0"?><data><response value="$status" rawmode="false">$value</response></data>"""
            .toByteArray()

    fun isSuccess() = state == EdlState.FRP_ERASED
    fun getState() = state
}

class SimulatedUsbException(message: String) : Exception(message)
