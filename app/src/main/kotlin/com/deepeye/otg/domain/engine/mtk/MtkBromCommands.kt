package com.deepeye.otg.domain.engine.mtk

object MtkBromCommands {
    const val CMD_READ32 = 0xD1.toByte()
    const val CMD_WRITE32 = 0xD4.toByte()
    const val CMD_GET_TARGET = 0xD8.toByte()
    const val CMD_SEND_DA = 0xD7.toByte()
    const val CMD_JUMP_DA = 0xD5.toByte()
    const val CMD_READ_EFUSE = 0xEA.toByte()

    const val RESP_ACK = 0x5A.toByte()
    const val RESP_NACK = 0xA5.toByte()

    // Handshake Sequence
    val HANDSHAKE_SEQ = listOf(
        0xA0.toByte() to 0x5F.toByte(),
        0x0A.toByte() to 0xF5.toByte(),
        0x50.toByte() to 0xAF.toByte(),
        0x05.toByte() to 0xFA.toByte()
    )

    // CDC-ACM SET_LINE_CODING data for 115200 8N1
    val CDC_LINE_CODING_115200 = byteArrayOf(
        0x00.toByte(), 0xC2.toByte(), 0x01.toByte(), 0x00.toByte(), // 115200 LE
        0x00.toByte(),                   // 1 stop bit
        0x00.toByte(),                   // parity none
        0x08.toByte()                    // 8 data bits
    )
}
