package com.deepeye.otg.protocol.qualcomm

import android.util.Log
import com.deepeye.otg.usb.TransferResult
import com.deepeye.otg.usb.UsbTransport

/**
 * Qualcomm Firehose XML Protocol implementation (Stage 5.2).
 */
object FirehoseProtocol {
    private const val TAG = "FirehoseProtocol"

    /**
     * Wrap XML string in a packet with proper padding for Firehose.
     */
    fun createPacket(xml: String): ByteArray {
        return xml.toByteArray(Charsets.UTF_8)
    }

    /**
     * Sends a configuration packet to Firehose.
     */
    suspend fun configure(transport: UsbTransport, maxPayloadSize: Int = 1048576): Boolean {
        val xml = """<?xml version="1.0" encoding="UTF-8" ?>
            |<data>
            |<configure MemoryName="emmc" MaxPayloadSizeToTargetInBytes="$maxPayloadSize" />
            |</data>""".trimMargin()
        
        Log.i(TAG, "Sending Firehose configuration...")
        return sendCommand(transport, xml).contains("ack")
    }

    /**
     * Reads a chunk of data from the device.
     */
    suspend fun read(transport: UsbTransport, sector: Long, numSectors: Int): ByteArray? {
        val xml = """<?xml version="1.0" encoding="UTF-8" ?>
            |<data>
            |<read SECTOR_SIZE_IN_BYTES="512" num_partition_sectors="$numSectors" start_sector="$sector" />
            |</data>""".trimMargin()
        
        if (transport.write(createPacket(xml)).isSuccess) {
            // Firehose usually returns an ACK XML followed by raw data
            val ack = receiveResponse(transport)
            if (ack.contains("ack")) {
                val dataRes = transport.read(numSectors * 512)
                if (dataRes is TransferResult.Success) return dataRes.data
            }
        }
        return null
    }

    /**
     * Sends XML command and returns the response.
     */
    suspend fun sendCommand(transport: UsbTransport, xml: String): String {
        val writeRes = transport.write(createPacket(xml))
        if (!writeRes.isSuccess) return "ERROR: WRITE_FAILED"
        
        return receiveResponse(transport)
    }

    private suspend fun receiveResponse(transport: UsbTransport): String {
        val res = transport.read(4096, timeoutMs = 2000)
        return if (res is TransferResult.Success && res.data != null) {
            String(res.data)
        } else ""
    }
}
