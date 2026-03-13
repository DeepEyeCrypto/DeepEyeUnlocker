package com.deepeye.otg.hid

import javax.inject.Inject
import javax.inject.Named

import android.util.Log
import java.io.File
import java.security.SecureRandom

// ──────────────────────────────────────────────────────────────
// HID Corpus Generator — Descriptor Corpus for Fuzzing
// DeepEye OTG — HID Research Module (Part 5)
// ──────────────────────────────────────────────────────────────

private const val TAG = "HidCorpusGenerator"

/**
 * Generates USB HID Report Descriptor corpus entries for fuzzing.
 *
 * Strategies:
 * 1. Valid descriptors (baseline conformance)
 * 2. Edge-case descriptors (spec boundary conditions)
 * 3. Malformed descriptors (known crash patterns)
 * 4. Random descriptors (pure fuzzing)
 *
 * Does NOT deliver descriptors to devices — only generates
 * byte sequences for offline analysis or lab testing.
 */
class HidCorpusGenerator @Inject constructor(
    @Named("hidCorpusDir") private val outputDir: File,
    private val rng: SecureRandom = SecureRandom()
) {
    init {
        outputDir.mkdirs()
    }

    /**
     * Generate a complete seed corpus.
     *
     * @return list of generated files
     */
    fun generateFullCorpus(): List<File> {
        val files = mutableListOf<File>()
        files.addAll(generateValidDescriptors())
        files.addAll(generateEdgeCaseDescriptors())
        files.addAll(generateMalformedDescriptors())
        files.addAll(generateRandomDescriptors(count = 20))
        Log.i(TAG, "Generated ${files.size} corpus entries")
        return files
    }

    // ── Valid Descriptors ───────────────────────────────────────

    /**
     * Generate well-formed descriptors for various device types.
     */
    fun generateValidDescriptors(): List<File> {
        val files = mutableListOf<File>()

        // 1. Simple keyboard
        files.add(save("valid_keyboard.bin", buildDescriptor {
            usagePage(0x01) // Generic Desktop
            usage(0x06)     // Keyboard
            collection(0x01) { // Application
                usagePage(0x07) // Keyboard/Keypad
                usageMinimum(0xE0)
                usageMaximum(0xE7)
                logicalMinimum(0)
                logicalMaximum(1)
                reportSize(1)
                reportCount(8)
                input(0x02)     // Data, Variable, Absolute
                reportCount(1)
                reportSize(8)
                input(0x01)     // Constant (padding)
                usageMinimum(0x00)
                usageMaximum(0x65)
                logicalMinimum(0)
                logicalMaximum(101)
                reportSize(8)
                reportCount(6)
                input(0x00)     // Data, Array
            }
        }))

        // 2. Simple mouse
        files.add(save("valid_mouse.bin", buildDescriptor {
            usagePage(0x01)
            usage(0x02)     // Mouse
            collection(0x01) {
                usage(0x01) // Pointer
                collection(0x00) { // Physical
                    usagePage(0x09) // Button
                    usageMinimum(1)
                    usageMaximum(3)
                    logicalMinimum(0)
                    logicalMaximum(1)
                    reportSize(1)
                    reportCount(3)
                    input(0x02)
                    reportSize(5)
                    reportCount(1)
                    input(0x01) // padding
                    usagePage(0x01)
                    usage(0x30) // X
                    usage(0x31) // Y
                    logicalMinimum(-127)
                    logicalMaximum(127)
                    reportSize(8)
                    reportCount(2)
                    input(0x06)
                }
            }
        }))

        // 3. Gamepad
        files.add(save("valid_gamepad.bin", buildDescriptor {
            usagePage(0x01)
            usage(0x05)     // Gamepad
            collection(0x01) {
                usagePage(0x09)
                usageMinimum(1)
                usageMaximum(16)
                logicalMinimum(0)
                logicalMaximum(1)
                reportSize(1)
                reportCount(16)
                input(0x02)
                usagePage(0x01)
                usage(0x30) // X
                usage(0x31) // Y
                usage(0x32) // Z
                usage(0x35) // Rz
                logicalMinimum(0)
                logicalMaximum(255)
                reportSize(8)
                reportCount(4)
                input(0x02)
            }
        }))

        return files
    }

    // ── Edge Case Descriptors ───────────────────────────────────

    /**
     * Generate descriptors at spec boundary conditions.
     */
    fun generateEdgeCaseDescriptors(): List<File> {
        val files = mutableListOf<File>()

        // 1. Maximum report IDs (255)
        files.add(save("edge_max_report_ids.bin", buildDescriptor {
            usagePage(0x01)
            usage(0x00)
            collection(0x01) {
                for (id in 1..255) {
                    reportId(id)
                    logicalMinimum(0)
                    logicalMaximum(1)
                    reportSize(1)
                    reportCount(1)
                    input(0x02)
                }
            }
        }))

        // 2. Deeply nested collections (depth = 8, valid)
        files.add(save("edge_nested_collections.bin", buildDescriptor {
            usagePage(0x01)
            usage(0x00)
            var depth = 0
            repeat(8) {
                addRaw(0xA1); addRaw(0x01) // Collection Application
                depth++
            }
            logicalMinimum(0)
            logicalMaximum(1)
            reportSize(1)
            reportCount(1)
            input(0x02)
            repeat(depth) {
                addRaw(0xC0) // End Collection
            }
        }))

        // 3. Report size = 32 (maximum common)
        files.add(save("edge_large_report_size.bin", buildDescriptor {
            usagePage(0x01)
            usage(0x00)
            collection(0x01) {
                logicalMinimum(0)
                logicalMaximum(0x7FFFFFFF)
                reportSize(32)
                reportCount(1)
                input(0x02)
            }
        }))

        // 4. Empty descriptor (zero bytes)
        files.add(save("edge_empty.bin", ByteArray(0)))

        // 5. Single-byte descriptor
        files.add(save("edge_single_byte.bin", byteArrayOf(0x05)))

        return files
    }

    // ── Malformed Descriptors (for crash pattern detection) ─────

    /**
     * Generate known-malformed descriptors.
     * These are for OFFLINE ANALYSIS and lab testing only.
     */
    fun generateMalformedDescriptors(): List<File> {
        val files = mutableListOf<File>()

        // 1. Unclosed collection
        files.add(save("malformed_unclosed_collection.bin", buildDescriptor {
            usagePage(0x01)
            usage(0x06)
            addRaw(0xA1); addRaw(0x01) // Collection Application (not closed)
            logicalMinimum(0)
            logicalMaximum(1)
            reportSize(1)
            reportCount(1)
            input(0x02)
            // Missing End Collection
        }))

        // 2. Extra End Collection
        files.add(save("malformed_extra_end_collection.bin", buildDescriptor {
            usagePage(0x01)
            usage(0x06)
            collection(0x01) {
                input(0x02)
            }
            addRaw(0xC0) // Extra End Collection
        }))

        // 3. Report count = 0xFFFF (huge)
        files.add(save("malformed_huge_report_count.bin", buildDescriptor {
            usagePage(0x01)
            usage(0x00)
            collection(0x01) {
                logicalMinimum(0)
                logicalMaximum(1)
                reportSize(1)
                reportCount(0xFFFF)
                input(0x02)
            }
        }))

        // 4. Report size = 0 with count > 0
        files.add(save("malformed_zero_report_size.bin", buildDescriptor {
            usagePage(0x01)
            usage(0x00)
            collection(0x01) {
                logicalMinimum(0)
                logicalMaximum(1)
                reportSize(0)
                reportCount(8)
                input(0x02)
            }
        }))

        // 5. Truncated item (size says 2 bytes but only 1 left)
        files.add(save("malformed_truncated_item.bin", byteArrayOf(
            0x05, 0x01,       // Usage Page (Generic Desktop)
            0x09, 0x06,       // Usage (Keyboard)
            0x15              // Logical Minimum (truncated — needs 1 data byte)
        )))

        // 6. Long item marker
        files.add(save("malformed_long_item.bin", byteArrayOf(
            0x05, 0x01,             // Usage Page
            0xFE.toByte(),          // Long item marker
            0x04,                   // data size = 4
            0x01,                   // tag
            0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte() // data
        )))

        return files
    }

    // ── Random Descriptors ──────────────────────────────────────

    /**
     * Generate pure random descriptors for fuzzing.
     */
    fun generateRandomDescriptors(
        count: Int = 20,
        minSize: Int = 1,
        maxSize: Int = 512
    ): List<File> {
        return (0 until count).map { idx ->
            val size = minSize + rng.nextInt(maxSize - minSize + 1)
            val data = ByteArray(size)
            rng.nextBytes(data)
            save("random_$idx.bin", data)
        }
    }

    // ── Descriptor Builder DSL ──────────────────────────────────

    private class DescriptorBuilder {
        val bytes = mutableListOf<Byte>()

        fun addRaw(b: Int) { bytes.add(b.toByte()) }

        fun usagePage(page: Int) {
            if (page <= 0xFF) { addRaw(0x05); addRaw(page) }
            else { addRaw(0x06); addRaw(page and 0xFF); addRaw((page shr 8) and 0xFF) }
        }

        fun usage(u: Int) {
            if (u <= 0xFF) { addRaw(0x09); addRaw(u) }
            else { addRaw(0x0A); addRaw(u and 0xFF); addRaw((u shr 8) and 0xFF) }
        }

        fun usageMinimum(u: Int) { addRaw(0x19); addRaw(u) }
        fun usageMaximum(u: Int) { addRaw(0x29); addRaw(u) }

        fun logicalMinimum(v: Int) {
            if (v in -128..127) { addRaw(0x15); addRaw(v and 0xFF) }
            else { addRaw(0x16); addRaw(v and 0xFF); addRaw((v shr 8) and 0xFF) }
        }

        fun logicalMaximum(v: Int) {
            if (v in 0..255) { addRaw(0x25); addRaw(v and 0xFF) }
            else { addRaw(0x26); addRaw(v and 0xFF); addRaw((v shr 8) and 0xFF) }
        }

        fun reportSize(s: Int) { addRaw(0x75); addRaw(s) }
        fun reportCount(c: Int) {
            if (c <= 0xFF) { addRaw(0x95); addRaw(c) }
            else { addRaw(0x96); addRaw(c and 0xFF); addRaw((c shr 8) and 0xFF) }
        }

        fun reportId(id: Int) { addRaw(0x85); addRaw(id) }
        fun input(flags: Int) { addRaw(0x81); addRaw(flags) }
        fun output(flags: Int) { addRaw(0x91); addRaw(flags) }
        fun feature(flags: Int) { addRaw(0xB1.toInt()); addRaw(flags) }

        fun collection(type: Int, block: DescriptorBuilder.() -> Unit) {
            addRaw(0xA1); addRaw(type)
            block()
            addRaw(0xC0) // End Collection
        }

        fun build(): ByteArray = bytes.toByteArray()
    }

    private fun buildDescriptor(block: DescriptorBuilder.() -> Unit): ByteArray {
        return DescriptorBuilder().apply(block).build()
    }

    // ── File Management ─────────────────────────────────────────

    private fun save(name: String, data: ByteArray): File {
        val file = File(outputDir, name)
        file.writeBytes(data)
        return file
    }
}
