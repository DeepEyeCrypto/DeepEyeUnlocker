package com.deepeye.otg.fuzz.hid

import java.util.Random

class HidMutator(private val seed: Long = System.currentTimeMillis()) {
    private val random = Random(seed)

    enum class Strategy {
        BIT_FLIP,
        BYTE_DELETE,
        BYTE_INSERT,
        MAXIMIZE_LENGTH_FIELD,
        ZERO_FILL,
        FF_FILL
    }

    fun mutate(data: ByteArray, strategy: Strategy): ByteArray {
        val result = data.copyOf()
        if (result.isEmpty()) return result

        when (strategy) {
            Strategy.BIT_FLIP -> {
                val idx = random.nextInt(result.size)
                val bit = random.nextInt(8)
                result[idx] = (result[idx].toInt() xor (1 shl bit)).toByte()
            }
            Strategy.BYTE_DELETE -> {
                if (result.size > 1) {
                    val idx = random.nextInt(result.size)
                    return result.filterIndexed { i, _ -> i != idx }.toByteArray()
                }
            }
            Strategy.BYTE_INSERT -> {
                val idx = random.nextInt(result.size + 1)
                val newByte = random.nextInt(256).toByte()
                val list = result.toMutableList()
                list.add(idx, newByte)
                return list.toByteArray()
            }
            Strategy.MAXIMIZE_LENGTH_FIELD -> {
                // Heuristic: search for common length-like bytes (e.g. following tag 0x95/0x75 in HID)
                for (i in 0 until result.size - 1) {
                    if (result[i].toInt() == 0x95 || result[i].toInt() == 0x75) {
                        result[i+1] = 0xFF.toByte()
                    }
                }
            }
            Strategy.ZERO_FILL -> result.fill(0)
            Strategy.FF_FILL -> result.fill(0xFF.toByte())
        }
        return result
    }
}
