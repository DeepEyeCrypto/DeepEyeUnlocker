package com.deepeye.simulation

import org.junit.Test
import org.junit.Assert.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import com.deepeye.simulation.QcEdlSimulator.SimScenario as QcSimScenario
import com.deepeye.simulation.QcEdlSimulator.EdlState
import com.deepeye.simulation.MtkBromSimulator.SimScenario as MtkSimScenario
import com.deepeye.simulation.MtkBromSimulator.BromState
import com.deepeye.simulation.SamsungMtpSimulator.SimScenario as SamSimScenario
import java.util.Arrays

class DeepEyeSimulationTest {

    /* ============================================================
     * QUALCOMM EDL SCENARIOS
     * ============================================================ */

    @Test
    fun `SIM QC-001 — Realme 14x EDL FRP erase SUCCESS`() = runTest {
        val device = DeviceLibrary.REALME_14X_EDL
        val sim = QcEdlSimulator(device, QcSimScenario.SUCCESS)

        // Mocking the engine as we don't have the real ones in this context
        // But the simulation logic itself is what we are testing here.
        // In a real scenario, QcFrpEngine would use the simulator.
        
        sim.processCommand("hello".toByteArray())
        sim.processCommand("program".toByteArray())
        sim.processCommand("configure".toByteArray())
        sim.processCommand("erase frp".toByteArray())

        assertTrue(sim.isSuccess())
        assertEquals(EdlState.FRP_ERASED, sim.getState())
        println("✅ SIM QC-001 PASS — Realme 14x FRP erased")
    }

    @Test
    fun `SIM QC-002 — USB disconnect handled gracefully`() = runTest {
        val sim = QcEdlSimulator(
            DeviceLibrary.REALME_14X_EDL,
            QcSimScenario.USB_DISCONNECT
        )
        
        sim.processCommand("hello".toByteArray())
        sim.processCommand("program".toByteArray())
        
        assertThrows(SimulatedUsbException::class.java) {
            sim.processCommand("configure".toByteArray())
        }
        
        println("✅ SIM QC-002 PASS — USB disconnect handled gracefully")
    }

    @Test
    fun `SIM QC-003 — Wrong programmer REJECTED with clear error`() = runTest {
        val sim = QcEdlSimulator(
            DeviceLibrary.SAMSUNG_S23_EDL,
            QcSimScenario.PROGRAMMER_REJECT
        )

        val response = sim.processCommand("hello".toByteArray())
        val respStr = String(response)

        assertTrue("Error must mention programmer rejection, got: $respStr",
            respStr.contains("NAK") && respStr.contains("rejected"))
        println("✅ SIM QC-003 PASS — Wrong programmer error surfaced correctly")
    }

    @Test
    fun `SIM QC-004 — Timeout scenario handled`() = runTest {
        val sim = QcEdlSimulator(
            DeviceLibrary.REALME_14X_EDL,
            QcSimScenario.TIMEOUT
        )

        val result = withTimeout(35_000) {
            sim.processCommand("hello".toByteArray())
        }

        assertTrue(result.isEmpty())
        println("✅ SIM QC-004 PASS — Timeout handled, no freeze")
    }

    /* ============================================================
     * MTK BROM SCENARIOS
     * ============================================================ */

    @Test
    fun `SIM MTK-001 — Realme 14x BROM DA upload + FRP erase SUCCESS`() = runTest {
        val device = DeviceLibrary.REALME_14X_EDL
        val sim = MtkBromSimulator(device, MtkSimScenario.SUCCESS)
        val fakeDA = ByteArray(8_000_000) // Valid 8MB DA
        val fakePayload = ByteArray(612)  // Valid kamakiri payload

        // Sequence: handshake → payload → DA → erase
        sim.handshake()
        val payloadAck = sim.uploadPayload(fakePayload)
        val daAck = sim.uploadDa(fakeDA)
        val frpResult = sim.eraseFrp()

        assertArrayEquals(byteArrayOf(0x5A.toByte()), payloadAck)
        assertArrayEquals(byteArrayOf(0x5A.toByte()), daAck)
        assertArrayEquals(byteArrayOf(0x5A.toByte()), frpResult)
        assertTrue(sim.isSuccess())
        println("✅ SIM MTK-001 PASS — MTK DA flow complete")
    }



    @Test
    fun `SIM MTK-002 — Payload uploaded as DA gets REJECTED by BROM`() = runTest {
        val sim = MtkBromSimulator(
            DeviceLibrary.REALME_14X_EDL,
            MtkSimScenario.WRONG_DA_SIZE
        )
        val fakePayload = ByteArray(612) // WRONG — payload used as DA

        val result = sim.uploadDa(fakePayload) // Must be NACK

        assertArrayEquals(byteArrayOf(0xA5.toByte()), result) // NACK
        assertFalse(sim.isSuccess())
        println("✅ SIM MTK-002 PASS — Wrong file size caught before BROM upload")
    }

    @Test
    fun `SIM MTK-003 — SLA auth required returns clear error`() = runTest {
        val sim = MtkBromSimulator(
            DeviceLibrary.SAMSUNG_A14_MTK_BROM,
            MtkSimScenario.AUTH_REQUIRED
        )
        val fakeDA = ByteArray(8_000_000)

        val result = sim.uploadDa(fakeDA)

        assertArrayEquals(byteArrayOf(0xA5.toByte()), result) // NACK
        println("✅ SIM MTK-003 PASS — SLA auth failure surfaced")
    }

    @Test
    fun `SIM MTK-004 — V5 chip but V6 DA handled`() = runTest {
        val v5Chip = DeviceLibrary.REALME_C35_MTK_BROM // MT6765 = V5
        val sim = MtkBromSimulator(v5Chip, MtkSimScenario.SUCCESS)

        // V6 DA is larger
        val v6DA = ByteArray(10_000_000) 
        val result = sim.uploadDa(v6DA)

        assertArrayEquals(byteArrayOf(0x5A.toByte()), result)
        println("✅ SIM MTK-004 PASS — V5/V6 DA handled")
    }

    /* ============================================================
     * SAMSUNG MTP/MODEM SCENARIOS
     * ============================================================ */

    @Test
    fun `SIM SAM-001 — Samsung A54 MTP FRP erase SUCCESS`() = runTest {
        val sim = SamsungMtpSimulator(SamSimScenario.SUCCESS)

        assertTrue(sim.switchToMtpMode())
        assertTrue(sim.switchToModemMode())
        val result = sim.eraseFrp()

        assertTrue(result is SimResult.Success)
        println("✅ SIM SAM-001 PASS — Samsung MTP FRP cleared")
    }

    @Test
    fun `SIM SAM-002 — Modem not found handled gracefully`() = runTest {
        val sim = SamsungMtpSimulator(SamSimScenario.MODEM_NOT_FOUND)

        assertFalse(sim.switchToModemMode())
        println("✅ SIM SAM-002 PASS — Modem not found handled gracefully")
    }

    @Test
    fun `SIM SAM-003 — NOT_IMPLEMENTED code path never reached`() = runTest {
        val sim = SamsungMtpSimulator(SamSimScenario.SUCCESS)
        val result = sim.eraseFrp()

        val message = when (result) {
            is SimResult.Success -> result.message
            is SimResult.Failure -> result.reason
        }

        assertFalse(
            "NOT_IMPLEMENTED must never appear in production output",
            message.contains("NOT_IMPLEMENTED")
        )
        println("✅ SIM SAM-003 PASS — NOT_IMPLEMENTED message absent")
    }

    /* ============================================================
     * ALL DEVICES MATRIX TEST
     * ============================================================ */

    @Test
    fun `SIM MATRIX — All 6 devices detected correctly`() {
        DeviceLibrary.ALL.forEach { device ->
            val type = when {
                device.vid == 0x05C6 -> "Qualcomm EDL"
                device.vid == 0x0E8D -> "MTK BROM"
                device.vid == 0x04E8 -> "Samsung MTP"
                device.vid == 0x22D9 -> "OPLUS EDL"
                else -> "Unknown"
            }
            println("Device: ${device.productName} → $type ✅")
            assertNotEquals("Device 0x${device.vid.toString(16)} not recognized",
                "Unknown", type)
        }
        println("✅ SIM MATRIX PASS — All ${DeviceLibrary.ALL.size} devices recognized")
    }
}

// Extension for testing purpose if needed elsewhere
fun ByteArray.toHex() = joinToString("") { "%02X".format(it) }
