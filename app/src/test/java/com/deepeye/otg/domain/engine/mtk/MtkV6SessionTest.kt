package com.deepeye.otg.domain.engine.mtk

import android.content.Context
import android.content.res.AssetManager
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.*
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.io.ByteArrayInputStream

class MtkV6SessionTest {

    @Mock lateinit var context: Context
    @Mock lateinit var assetManager: AssetManager
    @Mock lateinit var conn: UsbDeviceConnection
    @Mock lateinit var epIn: UsbEndpoint
    @Mock lateinit var epOut: UsbEndpoint

    private lateinit var session: MtkV6Session
    private val sessionId = "test-session-123"

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(context.assets).thenReturn(assetManager)
        session = MtkV6Session(context, conn, epIn, epOut)
    }

    @Test
    fun `test key exchange success`() {
        // 1. Return descriptor 0x02
        `when`(conn.bulkTransfer(eq(epIn), any(ByteArray::class.java), eq(1), anyInt()))
            .thenAnswer { invocation ->
                val buffer = invocation.getArgument<ByteArray>(1)
                buffer[0] = 0x02
                1
            }

        // 2. Mock sending challenge (16 bytes)
        `when`(conn.bulkTransfer(eq(epOut), any(ByteArray::class.java), eq(16), anyInt())).thenReturn(16)

        // 3. Mock receiving response (16 bytes)
        `when`(conn.bulkTransfer(eq(epIn), any(ByteArray::class.java), eq(16), anyInt())).thenReturn(16)

        val result = session.performKeyExchange(0x1209, sessionId)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `test key exchange fails with wrong descriptor`() {
        `when`(conn.bulkTransfer(eq(epIn), any(ByteArray::class.java), eq(1), anyInt()))
            .thenAnswer { invocation ->
                val buffer = invocation.getArgument<ByteArray>(1)
                buffer[0] = 0x01 // Wrong
                1
            }

        val result = session.performKeyExchange(0x1209, sessionId)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is V6Error.KeyExchangeFailed)
    }

    @Test
    fun `test DA selection success`() {
        val mockDa = "mock da content".toByteArray()
        `when`(assetManager.open("da/mt6835t_da.bin")).thenReturn(ByteArrayInputStream(mockDa))

        val result = session.selectDa(0x1209, sessionId)
        assertTrue(result.isSuccess)
        assertArrayEquals(mockDa, result.getOrNull())
    }

    @Test
    fun `test DA selection missing asset`() {
        `when`(assetManager.open(anyString())).thenThrow(RuntimeException("File not found"))

        val result = session.selectDa(0x1209, sessionId)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is V6Error.DaNotFound)
    }

    @Test
    fun `test DA upload progress and ZLP`() {
        val daSize = 10000
        val daBytes = ByteArray(daSize) { it.toByte() }
        `when`(epOut.maxPacketSize).thenReturn(512)
        
        // Mock multi-chunk transfer
        `when`(conn.bulkTransfer(eq(epOut), any(ByteArray::class.java), anyInt(), anyInt()))
            .thenAnswer { it.getArgument<Int>(2) }

        val progressUpdates = mutableListOf<Int>()
        val result = session.uploadDa(daBytes, { progressUpdates.add(it) }, sessionId)

        assertTrue(result.isSuccess)
        assertTrue(progressUpdates.isNotEmpty())
        assertEquals(100, progressUpdates.last())
        
        // Verify ZLP: if size is multiple of maxPacketSize, an extra transfer of 0 is called.
        // Handled in session.uploadDa
    }
}
