package com.deepeye.otg.protocol.mtk

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import com.deepeye.otg.domain.engine.mtk.MtkCdcSession
import com.deepeye.otg.domain.engine.mtk.V6Error
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.nullable
import org.mockito.Mockito

class MtkV6CdcSetupTest {

    @Test
    fun `cdc setup failure prevents sync attempt`() = runTest {
        val fixture = createFixture(controlTransferResults = intArrayOf(-1))

        val result = fixture.session.initialize()

        assertTrue(result.exceptionOrNull() is V6Error.CdcSetupFailed)
        assertTrue(fixture.bulkWrites.none { it.size == 16 && it.all { byte -> byte == 0x55.toByte() } })
    }

    @Test
    fun `successful cdc setup sends sixteen sync bytes`() = runTest {
        val hello = byteArrayOf(0x11, 0x22, 0x33)
        val fixture = createFixture(controlTransferResults = intArrayOf(7, 0), helloPacket = hello)

        val setup = fixture.session.setupCdcAcm()
        val sync = fixture.session.sendV6Sync()

        assertTrue(setup.isSuccess)
        assertTrue(sync.isSuccess)
        assertTrue(fixture.bulkWrites.size == 1)
        assertTrue(fixture.bulkWrites.single().size == 16)
        assertTrue(fixture.bulkWrites.single().all { it == 0x55.toByte() })
        assertArrayEquals(hello, sync.getOrThrow().copyOf(hello.size))
    }

    @Test
    fun `control transfer minus one returns cdc setup failed`() = runTest {
        val fixture = createFixture(controlTransferResults = intArrayOf(7, -1))

        val result = fixture.session.setupCdcAcm()

        assertTrue(result.exceptionOrNull() is V6Error.CdcSetupFailed)
    }

    private fun createFixture(
        controlTransferResults: IntArray,
        helloPacket: ByteArray = byteArrayOf(0x5A.toByte())
    ): SessionFixture {
        val connection = Mockito.mock(UsbDeviceConnection::class.java)
        val device = Mockito.mock(UsbDevice::class.java)
        val controlInterface = Mockito.mock(UsbInterface::class.java)
        val dataInterface = Mockito.mock(UsbInterface::class.java)
        val bulkIn = Mockito.mock(UsbEndpoint::class.java)
        val bulkOut = Mockito.mock(UsbEndpoint::class.java)
        val writes = mutableListOf<ByteArray>()

        Mockito.`when`(device.vendorId).thenReturn(0x22D9)
        Mockito.`when`(device.productId).thenReturn(0x0006)
        Mockito.`when`(device.getInterface(0)).thenReturn(controlInterface)
        Mockito.`when`(device.getInterface(1)).thenReturn(dataInterface)

        Mockito.`when`(connection.claimInterface(controlInterface, true)).thenReturn(true)
        Mockito.`when`(connection.claimInterface(dataInterface, true)).thenReturn(true)

        Mockito.`when`(dataInterface.endpointCount).thenReturn(2)
        Mockito.`when`(dataInterface.getEndpoint(0)).thenReturn(bulkOut)
        Mockito.`when`(dataInterface.getEndpoint(1)).thenReturn(bulkIn)

        Mockito.`when`(bulkOut.type).thenReturn(UsbConstants.USB_ENDPOINT_XFER_BULK)
        Mockito.`when`(bulkOut.direction).thenReturn(UsbConstants.USB_DIR_OUT)
        Mockito.`when`(bulkIn.type).thenReturn(UsbConstants.USB_ENDPOINT_XFER_BULK)
        Mockito.`when`(bulkIn.direction).thenReturn(UsbConstants.USB_DIR_IN)

        var controlIndex = 0
        Mockito.`when`(
            connection.controlTransfer(
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt(),
                nullable(ByteArray::class.java),
                anyInt(),
                anyInt()
            )
        ).thenAnswer {
            val idx = controlIndex.coerceAtMost(controlTransferResults.lastIndex)
            controlIndex++
            controlTransferResults[idx]
        }

        Mockito.doAnswer { invocation ->
            val endpoint = invocation.arguments[0] as UsbEndpoint
            val buffer = invocation.arguments[1] as ByteArray
            val length = invocation.arguments[2] as Int
            if (endpoint === bulkOut) {
                writes += buffer.copyOf(length)
                length
            } else {
                helloPacket.copyInto(buffer, endIndex = minOf(helloPacket.size, length))
                minOf(helloPacket.size, length)
            }
        }.`when`(connection).bulkTransfer(any(UsbEndpoint::class.java), any(ByteArray::class.java), anyInt(), anyInt())

        return SessionFixture(
            session = MtkCdcSession(connection, device, "v6-test"),
            bulkWrites = writes
        )
    }

    private data class SessionFixture(
        val session: MtkCdcSession,
        val bulkWrites: MutableList<ByteArray>
    )
}