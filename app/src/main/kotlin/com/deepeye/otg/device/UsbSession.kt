package com.deepeye.otg.device

import android.content.Context
import android.hardware.usb.*

open class UsbSession(
    protected val context:    Context,
    protected val usbDevice:  UsbDevice,
    private   val epInAddr:   Int = 0x81,
    private   val epOutAddr:  Int = 0x01,
    private   val timeoutMs:  Int = 10_000,
) {
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    protected lateinit var connection: UsbDeviceConnection
    protected lateinit var epIn:       UsbEndpoint
    protected lateinit var epOut:      UsbEndpoint

    fun open(): Result<Unit> = runCatching {
        val iface = usbDevice.getInterface(0)
        connection = usbManager.openDevice(usbDevice)
            ?: error("Cannot open USB device — grant permission first")
        connection.claimInterface(iface, true)

        // Find bulk endpoints
        for (i in 0 until iface.endpointCount) {
            val ep = iface.getEndpoint(i)
            if (ep.direction == UsbConstants.USB_DIR_IN  && ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) epIn  = ep
            if (ep.direction == UsbConstants.USB_DIR_OUT && ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) epOut = ep
        }
    }

    fun close() {
        if (::connection.isInitialized) connection.close()
    }

    protected fun write(data: ByteArray): Int {
        return connection.bulkTransfer(epOut, data, data.size, timeoutMs)
    }

    protected fun read(length: Int): ByteArray {
        val buf = ByteArray(length)
        val n   = connection.bulkTransfer(epIn, buf, length, timeoutMs)
        return if (n > 0) buf.copyOf(n) else ByteArray(0)
    }

    protected fun readExact(length: Int): ByteArray {
        val buf = mutableListOf<Byte>()
        var remaining = length
        while (remaining > 0) {
            val chunk = read(remaining)
            if (chunk.isEmpty()) break
            buf.addAll(chunk.toList())
            remaining -= chunk.size
        }
        return buf.toByteArray()
    }
}
