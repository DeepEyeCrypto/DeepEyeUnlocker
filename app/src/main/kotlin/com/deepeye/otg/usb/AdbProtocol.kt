package com.deepeye.otg.usb

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Pure Kotlin implementation of the ADB (Android Debug Bridge) Wire Protocol.
 * Ref: https://android.googlesource.com/platform/system/core/+/master/adb/PROTOCOL.TXT
 */
object AdbProtocol {
    const val A_SYNC = 0x434e5953
    const val A_CNXN = 0x4e584e43
    const val A_OPEN = 0x4e45504f
    const val A_OKAY = 0x59414b4f
    const val A_CLSE = 0x45534c43
    const val A_WRTE = 0x45545257
    const val A_AUTH = 0x48545541

    const val AUTH_TOKEN = 1
    const val AUTH_SIGNATURE = 2
    const val AUTH_RSAPUBLICKEY = 3

    const val CONNECT_VERSION = 0x01000000
    const val CONNECT_MAXDATA = 4096

    fun generateChecksum(data: ByteArray?): Int {
        if (data == null) return 0
        var checksum = 0
        for (b in data) {
            checksum += b.toInt() and 0xFF
        }
        return checksum
    }

    fun generateMagic(command: Int): Int {
        return command.inv()
    }
}

data class AdbMessage(
    val command: Int,
    val arg0: Int,
    val arg1: Int,
    val data: ByteArray? = null
) {
    fun serialize(): ByteArray {
        val dataLength = data?.size ?: 0
        val checksum = AdbProtocol.generateChecksum(data)
        val magic = AdbProtocol.generateMagic(command)

        val buffer = ByteBuffer.allocate(24 + dataLength).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(command)
        buffer.putInt(arg0)
        buffer.putInt(arg1)
        buffer.putInt(dataLength)
        buffer.putInt(checksum)
        buffer.putInt(magic)
        if (data != null) {
            buffer.put(data)
        }
        return buffer.array()
    }

    companion object {
        fun parse(header: ByteArray, data: ByteArray?): AdbMessage {
            val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            val command = buffer.getInt()
            val arg0 = buffer.getInt()
            val arg1 = buffer.getInt()
            val dataLength = buffer.getInt()
            val checksum = buffer.getInt()
            val magic = buffer.getInt()

            // Verification logic would go here (checksum, magic)
            return AdbMessage(command, arg0, arg1, data)
        }
    }
}
