package com.deepeye.otg.research.fuzz

import java.security.SecureRandom
import kotlin.math.min

// ──────────────────────────────────────────────────────────────
// Mutators — Input Mutation Strategies
// DeepEye OTG — Research Module (Part 4)
// ──────────────────────────────────────────────────────────────

/**
 * Interface for input mutation strategies.
 *
 * Each mutator takes a base input and produces a variant.
 * Mutators are stateless and composable.
 */
interface Mutator {
    val name: String
    val description: String

    /**
     * Mutate the input data.
     *
     * @param input base input bytes
     * @param maxSize maximum output size
     * @return mutated bytes
     */
    fun mutate(input: ByteArray, maxSize: Int = input.size * 2): ByteArray
}

/**
 * Flips random bits in the input.
 */
class BitFlipMutator(
    private val flipCount: Int = 1,
    private val rng: SecureRandom = SecureRandom()
) : Mutator {
    override val name = "bit_flip"
    override val description = "Flips $flipCount random bit(s)"

    override fun mutate(input: ByteArray, maxSize: Int): ByteArray {
        if (input.isEmpty()) return input
        val output = input.copyOf()
        repeat(flipCount) {
            val byteIdx = rng.nextInt(output.size)
            val bitIdx = rng.nextInt(8)
            output[byteIdx] = (output[byteIdx].toInt() xor (1 shl bitIdx)).toByte()
        }
        return output
    }
}

/**
 * Replaces random bytes with interesting values.
 * Targets boundary conditions in parsers.
 */
class InterestingValueMutator(
    private val rng: SecureRandom = SecureRandom()
) : Mutator {
    override val name = "interesting_values"
    override val description = "Inserts boundary/interesting values"

    companion object {
        val INTERESTING_BYTES = byteArrayOf(
            0x00, 0x01, 0x7F, 0x80.toByte(), 0xFF.toByte(),
            0xFE.toByte(), 0x7E, 0x02, 0x10, 0x20
        )
        val INTERESTING_SHORTS = shortArrayOf(
            0, 1, -1, 127, 128, 255, 256, 32767, -32768, -1
        )
        val INTERESTING_INTS = intArrayOf(
            0, 1, -1, 127, 128, 255, 256, 32767, 32768,
            65535, 65536, Int.MAX_VALUE, Int.MIN_VALUE,
            0x7FFFFFFE, 0x80000001.toInt()
        )
    }

    override fun mutate(input: ByteArray, maxSize: Int): ByteArray {
        if (input.isEmpty()) return input
        val output = input.copyOf()
        val pos = rng.nextInt(output.size)

        when (rng.nextInt(3)) {
            0 -> { // Replace single byte
                output[pos] = INTERESTING_BYTES[rng.nextInt(INTERESTING_BYTES.size)]
            }
            1 -> { // Replace 2 bytes (short)
                if (pos + 1 < output.size) {
                    val v = INTERESTING_SHORTS[rng.nextInt(INTERESTING_SHORTS.size)]
                    output[pos] = (v.toInt() shr 8).toByte()
                    output[pos + 1] = v.toByte()
                }
            }
            2 -> { // Replace 4 bytes (int)
                if (pos + 3 < output.size) {
                    val v = INTERESTING_INTS[rng.nextInt(INTERESTING_INTS.size)]
                    output[pos] = (v shr 24).toByte()
                    output[pos + 1] = (v shr 16).toByte()
                    output[pos + 2] = (v shr 8).toByte()
                    output[pos + 3] = v.toByte()
                }
            }
        }
        return output
    }
}

/**
 * Inserts, deletes, or repeats random byte sequences.
 */
class BlockMutator(
    private val rng: SecureRandom = SecureRandom()
) : Mutator {
    override val name = "block_ops"
    override val description = "Insert/delete/repeat byte blocks"

    override fun mutate(input: ByteArray, maxSize: Int): ByteArray {
        if (input.isEmpty()) return ByteArray(rng.nextInt(min(16, maxSize) + 1)).also { rng.nextBytes(it) }

        return when (rng.nextInt(4)) {
            0 -> insertBlock(input, maxSize)
            1 -> deleteBlock(input)
            2 -> repeatBlock(input, maxSize)
            3 -> shuffleBlock(input)
            else -> input.copyOf()
        }
    }

    private fun insertBlock(input: ByteArray, maxSize: Int): ByteArray {
        val insertSize = rng.nextInt(min(32, maxSize - input.size).coerceAtLeast(1)) + 1
        val insertPos = rng.nextInt(input.size + 1)
        val block = ByteArray(insertSize).also { rng.nextBytes(it) }

        val output = ByteArray(min(input.size + insertSize, maxSize))
        System.arraycopy(input, 0, output, 0, min(insertPos, output.size))
        if (insertPos < output.size) {
            val blockCopy = min(insertSize, output.size - insertPos)
            System.arraycopy(block, 0, output, insertPos, blockCopy)
            val remaining = min(input.size - insertPos, output.size - insertPos - blockCopy)
            if (remaining > 0) {
                System.arraycopy(input, insertPos, output, insertPos + blockCopy, remaining)
            }
        }
        return output
    }

    private fun deleteBlock(input: ByteArray): ByteArray {
        if (input.size <= 1) return input.copyOf()
        val deleteSize = rng.nextInt(min(32, input.size / 2).coerceAtLeast(1)) + 1
        val deletePos = rng.nextInt(input.size - deleteSize + 1)
        val output = ByteArray(input.size - deleteSize)
        System.arraycopy(input, 0, output, 0, deletePos)
        System.arraycopy(input, deletePos + deleteSize, output, deletePos, input.size - deletePos - deleteSize)
        return output
    }

    private fun repeatBlock(input: ByteArray, maxSize: Int): ByteArray {
        val blockSize = rng.nextInt(min(16, input.size).coerceAtLeast(1)) + 1
        val blockPos = rng.nextInt(input.size - blockSize + 1)
        val repeatCount = rng.nextInt(4) + 2

        val extra = blockSize * (repeatCount - 1)
        val output = ByteArray(min(input.size + extra, maxSize))
        System.arraycopy(input, 0, output, 0, min(blockPos + blockSize, output.size))
        var pos = blockPos + blockSize
        repeat(repeatCount - 1) {
            if (pos + blockSize <= output.size) {
                System.arraycopy(input, blockPos, output, pos, blockSize)
                pos += blockSize
            }
        }
        val remaining = min(input.size - blockPos - blockSize, output.size - pos)
        if (remaining > 0) {
            System.arraycopy(input, blockPos + blockSize, output, pos, remaining)
        }
        return output
    }

    private fun shuffleBlock(input: ByteArray): ByteArray {
        val output = input.copyOf()
        if (output.size < 4) return output
        val blockSize = rng.nextInt(min(16, output.size / 2).coerceAtLeast(1)) + 1
        val pos1 = rng.nextInt(output.size - blockSize)
        var pos2 = rng.nextInt(output.size - blockSize)
        if (pos2 == pos1) pos2 = (pos1 + blockSize) % (output.size - blockSize)

        // Swap blocks
        for (i in 0 until blockSize) {
            val temp = output[pos1 + i]
            output[pos1 + i] = output[pos2 + i]
            output[pos2 + i] = temp
        }
        return output
    }
}

/**
 * Arithmetic mutations — add/subtract small values from bytes.
 */
class ArithmeticMutator(
    private val rng: SecureRandom = SecureRandom()
) : Mutator {
    override val name = "arithmetic"
    override val description = "Add/subtract small values from random bytes"

    override fun mutate(input: ByteArray, maxSize: Int): ByteArray {
        if (input.isEmpty()) return input
        val output = input.copyOf()
        val pos = rng.nextInt(output.size)
        val delta = rng.nextInt(71) - 35 // -35 to +35
        output[pos] = (output[pos].toInt() + delta).toByte()
        return output
    }
}

/**
 * Cross-over mutator — splices two corpus entries together.
 */
class CrossOverMutator(
    private val corpusManager: CorpusManager,
    private val rng: SecureRandom = SecureRandom()
) : Mutator {
    override val name = "crossover"
    override val description = "Splice two corpus entries together"

    override fun mutate(input: ByteArray, maxSize: Int): ByteArray {
        val other = corpusManager.pickRandom()?.inputData ?: return input.copyOf()
        if (input.isEmpty() || other.isEmpty()) return input.copyOf()

        val splitA = rng.nextInt(input.size)
        val splitB = rng.nextInt(other.size)

        val outputSize = min(splitA + (other.size - splitB), maxSize)
        val output = ByteArray(outputSize)
        System.arraycopy(input, 0, output, 0, min(splitA, outputSize))
        val secondHalf = min(other.size - splitB, outputSize - min(splitA, outputSize))
        if (secondHalf > 0) {
            System.arraycopy(other, splitB, output, min(splitA, outputSize), secondHalf)
        }
        return output
    }
}

/**
 * Factory for creating the default mutator set.
 */
object MutatorFactory {
    fun createDefaultSet(corpusManager: CorpusManager): List<Mutator> = listOf(
        BitFlipMutator(flipCount = 1),
        BitFlipMutator(flipCount = 2),
        BitFlipMutator(flipCount = 4),
        InterestingValueMutator(),
        BlockMutator(),
        ArithmeticMutator(),
        CrossOverMutator(corpusManager)
    )
}
