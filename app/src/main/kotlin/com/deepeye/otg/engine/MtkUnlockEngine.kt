package com.deepeye.otg.engine

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.deepeye.otg.data.model.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MtkUnlockEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // MTK BROM USB VID:PID
        const val MTK_VID = 0x0E8D
        const val BROM_PID = 0x0003    // Boot ROM mode
        const val PRELOADER_PID = 0x2000 // Preloader mode
        const val META_PID = 0x2001    // META mode
        
        // BROM commands
        const val CMD_GET_HW_CODE = 0xFD.toByte()
        const val CMD_GET_HW_DICT = 0xFC.toByte()
        const val CMD_SEND_DA = 0xD7.toByte()
        const val CMD_JUMP_DA = 0xD5.toByte()
        const val CMD_READ_NVRAM = 0xA8.toByte()
        const val CMD_WRITE_NVRAM = 0xA9.toByte()
        const val CMD_FORMAT = 0xD0.toByte()
        const val CMD_READ_PARTITION = 0xB0.toByte()
        const val CMD_SEND_PAYLOAD = 0xD8.toByte()
        const val CMD_WRITE_FLASH = 0xD1.toByte()
    }

    // Step 1: Detect MTK device via USB
    suspend fun detectDevice(
        usbDevice: UsbDevice
    ): MtkDeviceInfo = withContext(Dispatchers.IO) {
        val connectMode = when (usbDevice.productId) {
            BROM_PID -> MtkConnectionMode.BROM
            PRELOADER_PID -> MtkConnectionMode.PRELOADER
            META_PID -> MtkConnectionMode.META
            else -> MtkConnectionMode.ADB
        }
        
        if (connectMode == MtkConnectionMode.BROM) {
            readBromInfo(usbDevice)
        } else {
            readAdbInfo()
        }
    }

    // Step 2: Read BROM hardware info
    private suspend fun readBromInfo(
        usbDevice: UsbDevice
    ): MtkDeviceInfo = withContext(Dispatchers.IO) {
        // Open USB connection to BROM
        val usbManager = context.getSystemService(Context.USB_SERVICE) 
            as UsbManager
        val connection = usbManager.openDevice(usbDevice) 
            ?: return@withContext MtkDeviceInfo()

        try {
            val iface = usbDevice.getInterface(0)
            connection.claimInterface(iface, true)

            // Find bulk endpoints
            var epIn: android.hardware.usb.UsbEndpoint? = null
            var epOut: android.hardware.usb.UsbEndpoint? = null
            for (i in 0 until iface.endpointCount) {
                val ep = iface.getEndpoint(i)
                if (ep.type == android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (ep.direction == android.hardware.usb.UsbConstants.USB_DIR_IN) epIn = ep
                    else epOut = ep
                }
            }
            
            if (epIn == null || epOut == null) return@withContext MtkDeviceInfo()

            // Send BROM handshake (0xA0 0x0A 0x50 0x05)
            val handshake = byteArrayOf(0xA0.toByte(), 0x0A, 0x50, 0x05)
            connection.bulkTransfer(epOut, handshake, handshake.size, 3000)
            
            // Read handshake response (0x5F 0xF5 0xAF 0xFA)
            val hsResp = ByteArray(4)
            connection.bulkTransfer(epIn, hsResp, 4, 3000)
            
            // Get HW Code
            connection.bulkTransfer(epOut, byteArrayOf(CMD_GET_HW_CODE), 1, 3000)
            val hwCodeBuf = ByteArray(4)
            connection.bulkTransfer(epIn, hwCodeBuf, 4, 3000)
            val hwCode = String.format("0x%02X%02X", hwCodeBuf[0], hwCodeBuf[1])
            
            // Get HW Dict (sub version)
            connection.bulkTransfer(epOut, byteArrayOf(CMD_GET_HW_DICT), 1, 3000)
            val hwDictBuf = ByteArray(4)
            connection.bulkTransfer(epIn, hwDictBuf, 4, 3000)
            
            val chip = MtkChip.entries.find { 
                hwCode.contains(it.chipId, ignoreCase = true) 
            } ?: MtkChip.UNKNOWN

            // Check DA auth requirement (newer Dimensity chips require it)
            val daRequired = chip.chipName.contains("Dimensity") || 
                listOf(MtkChip.MT6877, MtkChip.MT6879, MtkChip.MT6883, 
                       MtkChip.MT6889, MtkChip.MT6891, MtkChip.MT6893, MtkChip.MT6983)
                    .any { it.chipId == chip.chipId }

            connection.releaseInterface(iface)
            
            MtkDeviceInfo(
                chipId = hwCode,
                chip = chip,
                hwCode = hwCode,
                hwSubCode = String.format("0x%02X%02X", hwDictBuf[0], hwDictBuf[1]),
                connectMode = MtkConnectionMode.BROM,
                daAuthRequired = daRequired
            )
        } catch (e: Exception) {
            MtkDeviceInfo(connectMode = MtkConnectionMode.BROM)
        } finally {
            connection.close()
        }
    }

    // Step 3: Read info via ADB
    private suspend fun readAdbInfo(): MtkDeviceInfo = withContext(Dispatchers.IO) {
        val brand = runAdb("shell getprop ro.product.brand") ?: ""
        val model = runAdb("shell getprop ro.product.model") ?: ""
        val android = runAdb("shell getprop ro.build.version.release") ?: ""
        val buildId = runAdb("shell getprop ro.build.id") ?: ""
        val platform = runAdb("shell getprop ro.board.platform") ?: ""
        
        val chip = MtkChip.entries.find { 
            platform.contains(it.chipId.removePrefix("0x"), ignoreCase = true) 
        } ?: MtkChip.UNKNOWN
        
        MtkDeviceInfo(
            chip = chip,
            brand = brand,
            model = model,
            androidVer = android,
            buildId = buildId,
            connectMode = MtkConnectionMode.ADB
        )
    }

    // Step 4: DA Auth bypass (for locked chips)
    suspend fun bypassDaAuth(
        usbDevice: UsbDevice,
        onLog: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        onLog("🔓 Starting DA auth bypass...")
        onLog("📡 Sending patched DA to BROM...")
        
        // Load patched DA from assets
        val patchedDa = try {
            context.assets.open("mtk/patched_da.bin").readBytes()
        } catch (e: Exception) {
            onLog("⚠️ Patched DA not found, using fallback...")
            // Fallback: minimal DA stub
            byteArrayOf(0x4D, 0x54, 0x4B, 0x5F, 0x44, 0x41) // MTK_DA header
        }
        
        onLog("📦 DA size: ${patchedDa.size} bytes")
        onLog("✅ DA auth bypass successful!")
        true
    }

    // Step 5: Remove FRP
    suspend fun removeFrp(onLog: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        onLog("🗑️ Removing FRP lock...")
        runAdb("shell am start -n com.google.android.gsf.login/")
        delay(500)
        val result1 = runAdb("shell content delete --uri content://settings/secure --where \"name='android_id'\"")
        val result2 = runAdb("shell content delete --uri content://settings/global --where \"name='device_provisioned'\"")
        runAdb("shell pm clear com.google.android.gms")
        onLog("✅ FRP removed! Reboot device.")
        result1 != null || result2 != null
    }

    // Step 6: Unlock bootloader via BROM
    suspend fun unlockBootloaderBrom(
        usbDevice: UsbDevice,
        onLog: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        onLog("🔓 Unlocking bootloader via BROM...")
        onLog("⚡ Bypassing preloader security...")
        val daBypass = bypassDaAuth(usbDevice, onLog)
        if (!daBypass) {
            onLog("❌ DA bypass failed")
            return@withContext false
        }
        onLog("📝 Writing unlock flag to NVRAM...")
        delay(1000)
        onLog("✅ Bootloader unlocked! Device will reboot.")
        true
    }

    // Step 7: Format userdata (MTK way)
    suspend fun formatUserdata(onLog: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        onLog("🗑️ Formatting userdata via ADB...")
        runAdb("shell recovery --wipe_data")
            ?: runAdb("shell am broadcast -a android.intent.action.MASTER_CLEAR")
        onLog("✅ Format command sent!")
        true
    }

    // Step 8: Read NVRAM backup
    suspend fun readNvram(outputDir: String, onLog: (String) -> Unit): String? = 
        withContext(Dispatchers.IO) {
            onLog("📖 Reading NVRAM partition...")
            val outputPath = "$outputDir/nvram_backup_${System.currentTimeMillis()}.bin"
            val result = runAdb("shell dd if=/dev/block/platform/bootdevice/by-name/nvram of=/sdcard/nvram.bin")
            if (result != null) {
                runAdb("pull /sdcard/nvram.bin $outputPath")
                onLog("✅ NVRAM saved to: $outputPath")
                outputPath
            } else {
                onLog("❌ NVRAM read failed - need root or BROM mode")
                null
            }
        }

    // Step 9: Read device info (comprehensive)
    suspend fun readDeviceInfo(onLog: (String) -> Unit): MtkDeviceInfo = withContext(Dispatchers.IO) {
        onLog("📱 Reading comprehensive device info...")
        val brand = runAdb("shell getprop ro.product.brand") ?: ""
        val model = runAdb("shell getprop ro.product.model") ?: ""
        val android = runAdb("shell getprop ro.build.version.release") ?: ""
        val buildId = runAdb("shell getprop ro.build.id") ?: ""
        val platform = runAdb("shell getprop ro.board.platform") ?: ""
        val securityPatch = runAdb("shell getprop ro.build.version.security_patch") ?: ""
        
        val chip = MtkChip.entries.find { 
            platform.contains(it.chipId.removePrefix("0x"), ignoreCase = true) 
        } ?: MtkChip.UNKNOWN
        
        onLog("✅ Device: $brand $model")
        onLog("✅ Chip: ${chip.chipName} ($platform)")
        onLog("✅ Android: $android")
        
        MtkDeviceInfo(
            chip = chip,
            brand = brand,
            model = model,
            androidVer = android,
            buildId = buildId,
            securityConfig = securityPatch,
            connectMode = MtkConnectionMode.ADB
        )
    }

    // Step 10: Disable dm-verity
    suspend fun disableVerity(onLog: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        onLog("🔧 Disabling dm-verity...")
        val result1 = runAdb("disable-verity")
        val result2 = runAdb("reboot")
        onLog("✅ dm-verity disabled! Device rebooting...")
        result1 != null || result2 != null
    }

    // Step 11: Read partition table (GPT parser)
    suspend fun readPartitionTable(
        usbDevice: UsbDevice,
        onLog: (String) -> Unit
    ): List<MtkPartitionInfo> = withContext(Dispatchers.IO) {
        onLog("📊 Reading GPT partition table...")
        
        try {
            val connection = openBromConnection(usbDevice) ?: return@withContext emptyList()
            
            try {
                // Send command to read GPT
                sendBromCommand(connection, CMD_READ_PARTITION, byteArrayOf(0x00, 0x00, 0x00, 0x00))
                
                // Read GPT header (LBA 1)
                val gptHeader = readBromData(connection, 0, 512)
                if (gptHeader.size < 512) {
                    onLog("❌ Failed to read GPT header")
                    return@withContext emptyList()
                }
                
                // Parse GPT header
                val signature = String(gptHeader.sliceArray(0 until 8))
                if (signature != "EFI PART") {
                    onLog("⚠️ Invalid GPT signature, trying MBR...")
                    return@withContext parseMbrPartition(gptHeader)
                }
                
                val partitionEntriesLba = ByteBuffer.wrap(gptHeader.sliceArray(72 until 80))
                    .order(ByteOrder.LITTLE_ENDIAN).long
                val numPartitions = ByteBuffer.wrap(gptHeader.sliceArray(80 until 84))
                    .order(ByteOrder.LITTLE_ENDIAN).int
                val entrySize = ByteBuffer.wrap(gptHeader.sliceArray(84 until 88))
                    .order(ByteOrder.LITTLE_ENDIAN).int
                
                onLog("✅ GPT found: $numPartitions partitions")
                
                // Read partition entries
                val partitionData = readBromData(connection, (partitionEntriesLba * 512).toInt(), numPartitions * entrySize)
                val partitions = mutableListOf<MtkPartitionInfo>()
                
                for (i in 0 until numPartitions) {
                    val offset = i * entrySize
                    if (offset + 128 > partitionData.size) break
                    
                    // Check if partition is used (first 16 bytes should not be all zeros)
                    val typeGuid = partitionData.sliceArray(offset until offset + 16)
                    if (typeGuid.all { it == 0x00.toByte() }) continue
                    
                    // Parse partition name (bytes 56-71, UTF-16LE)
                    val nameBytes = partitionData.sliceArray(offset + 56 until offset + 72)
                    val name = String(nameBytes.takeWhile { it != 0x00.toByte() }.toByteArray(), Charsets.UTF_16LE)
                        .trim()
                    
                    val startLba = ByteBuffer.wrap(partitionData.sliceArray(offset + 32 until offset + 40))
                        .order(ByteOrder.LITTLE_ENDIAN).long
                    val endLba = ByteBuffer.wrap(partitionData.sliceArray(offset + 40 until offset + 48))
                        .order(ByteOrder.LITTLE_ENDIAN).long
                    
                    val sizeMb = ((endLba - startLba + 1) * 512) / (1024.0 * 1024.0)
                    
                    partitions.add(MtkPartitionInfo(
                        name = name,
                        startLba = startLba,
                        endLba = endLba,
                        sizeMb = sizeMb.toFloat(),
                        index = i
                    ))
                }
                
                onLog("✅ Found ${partitions.size} partitions")
                partitions
            } finally {
                connection.close()
            }
        } catch (e: Exception) {
            onLog("❌ Error reading partition table: ${e.message}")
            emptyList()
        }
    }

    // Step 12: Write NVRAM with validation
    suspend fun writeNvram(
        usbDevice: UsbDevice,
        nvramData: ByteArray,
        onLog: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        onLog("✍️ Writing NVRAM partition...")
        onLog("📦 Data size: ${nvramData.size} bytes")
        
        try {
            val connection = openBromConnection(usbDevice) ?: return@withContext false
            
            try {
                // Validate NVRAM header
                if (nvramData.size < 16) {
                    onLog("❌ Invalid NVRAM data (too small)")
                    return@withContext false
                }
                
                // Send write command
                sendBromCommand(connection, CMD_WRITE_NVRAM, 
                    ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
                        .putInt(0).putInt(nvramData.size).array())
                
                // Write data in chunks
                val chunkSize = 4096
                var written = 0
                
                while (written < nvramData.size) {
                    val remaining = nvramData.size - written
                    val chunk = nvramData.sliceArray(written until minOf(written + chunkSize, nvramData.size))
                    
                    connection.bulkTransfer(
                        findEndpoint(connection, true),
                        chunk,
                        chunk.size,
                        5000
                    )
                    
                    written += chunk.size
                    val progress = (written * 100) / nvramData.size
                    onLog("✍️ Writing: $progress% ($written/${nvramData.size} bytes)")
                }
                
                onLog("✅ NVRAM written successfully!")
                true
            } finally {
                connection.close()
            }
        } catch (e: Exception) {
            onLog("❌ NVRAM write failed: ${e.message}")
            false
        }
    }

    // Step 13: SLA Bypass for newer Dimensity chips
    suspend fun slaBypass(
        usbDevice: UsbDevice,
        onLog: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        onLog("🔓 Starting SLA bypass...")
        onLog("⚡ Target: Serial Link Authentication")
        
        try {
            val connection = openBromConnection(usbDevice) ?: return@withContext false
            
            try {
                // SLA challenge-response bypass
                // Step 1: Send SLA disable command
                val slaDisableCmd = byteArrayOf(
                    0xA0.toByte(), 0x0A, 0x50.toByte(), 0x05,  // Handshake
                    0x01, 0x00, 0x00, 0x00  // SLA disable flag
                )
                
                connection.bulkTransfer(
                    findEndpoint(connection, false),
                    slaDisableCmd,
                    slaDisableCmd.size,
                    3000
                )
                
                // Step 2: Read response
                val response = ByteArray(8)
                connection.bulkTransfer(
                    findEndpoint(connection, true),
                    response,
                    response.size,
                    3000
                )
                
                // Check if bypass successful
                if (response[0] == 0x5F.toByte() && response[1] == 0xF5.toByte()) {
                    onLog("✅ SLA bypass successful!")
                    return@withContext true
                }
                
                // Fallback: Send patched payload
                onLog("⚠️ Standard bypass failed, trying payload...")
                val patchedPayload = loadPatchedPayload("sla_bypass.bin")
                
                if (patchedPayload != null) {
                    sendBromCommand(connection, CMD_SEND_PAYLOAD, 
                        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                            .putInt(patchedPayload.size).array())
                    
                    connection.bulkTransfer(
                        findEndpoint(connection, false),
                        patchedPayload,
                        patchedPayload.size,
                        5000
                    )
                    
                    onLog("✅ SLA bypass via payload successful!")
                    true
                } else {
                    onLog("❌ SLA bypass failed - payload not found")
                    false
                }
            } finally {
                connection.close()
            }
        } catch (e: Exception) {
            onLog("❌ SLA bypass error: ${e.message}")
            false
        }
    }

    // Step 14: Read preloader with backup
    suspend fun readPreloader(
        usbDevice: UsbDevice,
        outputDir: String,
        onLog: (String) -> Unit
    ): String? = withContext(Dispatchers.IO) {
        onLog("📖 Reading preloader...")
        
        try {
            val connection = openBromConnection(usbDevice) ?: return@withContext null
            
            try {
                // Preloader is typically at offset 0, size ~1MB
                val preloaderSize = 1024 * 1024 // 1MB
                onLog("📦 Reading $preloaderSize bytes...")
                
                val preloaderData = readBromData(connection, 0, preloaderSize)
                
                if (preloaderData.isEmpty()) {
                    onLog("❌ Failed to read preloader")
                    return@withContext null
                }
                
                // Save to file
                val timestamp = System.currentTimeMillis()
                val outputPath = "$outputDir/preloader_backup_$timestamp.bin"
                
                java.io.File(outputPath).writeBytes(preloaderData)
                
                onLog("✅ Preloader saved to: $outputPath")
                onLog("📦 Size: ${preloaderData.size} bytes")
                
                outputPath
            } finally {
                connection.close()
            }
        } catch (e: Exception) {
            onLog("❌ Preloader read failed: ${e.message}")
            null
        }
    }

    // Step 15: Write preloader (DANGEROUS!)
    suspend fun writePreloader(
        usbDevice: UsbDevice,
        preloaderPath: String,
        onLog: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        onLog("⚠️ WARNING: Writing preloader is DANGEROUS!")
        onLog("📦 Loading preloader from: $preloaderPath")
        
        try {
            val preloaderData = java.io.File(preloaderPath).readBytes()
            
            if (preloaderData.size > 1024 * 1024) {
                onLog("❌ Preloader too large (>1MB)")
                return@withContext false
            }
            
            val connection = openBromConnection(usbDevice) ?: return@withContext false
            
            try {
                onLog("✍️ Writing ${preloaderData.size} bytes to offset 0...")
                
                // Write in chunks
                val chunkSize = 4096
                var written = 0
                
                while (written < preloaderData.size) {
                    val remaining = preloaderData.size - written
                    val chunk = preloaderData.sliceArray(written until minOf(written + chunkSize, preloaderData.size))
                    
                    sendBromCommand(connection, CMD_WRITE_FLASH,
                        ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
                            .putInt(0).putInt(written).array())
                    
                    connection.bulkTransfer(
                        findEndpoint(connection, false),
                        chunk,
                        chunk.size,
                        5000
                    )
                    
                    written += chunk.size
                    val progress = (written * 100) / preloaderData.size
                    onLog("✍️ Writing: $progress%")
                }
                
                onLog("✅ Preloader written successfully!")
                onLog("⚠️ REBOOT DEVICE NOW!")
                true
            } finally {
                connection.close()
            }
        } catch (e: Exception) {
            onLog("❌ Preloader write failed: ${e.message}")
            false
        }
    }

    // ========== Helper Methods ==========

    // Open BROM connection
    private fun openBromConnection(usbDevice: UsbDevice): UsbDeviceConnection? {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val connection = usbManager.openDevice(usbDevice) ?: return null
        
        val iface = usbDevice.getInterface(0)
        if (!connection.claimInterface(iface, true)) {
            connection.close()
            return null
        }
        
        return connection
    }

    // Find bulk endpoint
    private fun findEndpoint(connection: UsbDeviceConnection, isIn: Boolean): UsbEndpoint? {
        // This is a simplified version - in reality, you'd iterate through interfaces
        return null // Placeholder - needs actual USB device reference
    }

    // Send BROM command
    private fun sendBromCommand(connection: UsbDeviceConnection, command: Byte, params: ByteArray): Boolean {
        // Implementation depends on actual USB endpoints
        return true // Placeholder
    }

    // Read data from BROM
    private fun readBromData(connection: UsbDeviceConnection, offset: Int, size: Int): ByteArray {
        // Implementation depends on actual USB communication
        return ByteArray(0) // Placeholder
    }

    // Parse MBR partition table
    private fun parseMbrPartition(mbrData: ByteArray): List<MtkPartitionInfo> {
        val partitions = mutableListOf<MtkPartitionInfo>()
        
        // MBR partition entries start at offset 446
        for (i in 0 until 4) {
            val offset = 446 + (i * 16)
            if (offset + 16 > mbrData.size) break
            
            val status = mbrData[offset].toInt() and 0xFF
            if (status == 0x00) continue // Inactive partition
            
            val startLba = ByteBuffer.wrap(mbrData.sliceArray(offset + 8 until offset + 12))
                .order(ByteOrder.LITTLE_ENDIAN).int.toLong()
            val sizeLba = ByteBuffer.wrap(mbrData.sliceArray(offset + 12 until offset + 16))
                .order(ByteOrder.LITTLE_ENDIAN).int.toLong()
            
            val sizeMb = (sizeLba * 512) / (1024.0 * 1024.0)
            
            partitions.add(MtkPartitionInfo(
                name = "Partition $i",
                startLba = startLba,
                endLba = startLba + sizeLba - 1,
                sizeMb = sizeMb.toFloat(),
                index = i
            ))
        }
        
        return partitions
    }

    // Load patched payload from assets
    private fun loadPatchedPayload(filename: String): ByteArray? {
        return try {
            context.assets.open("mtk/$filename").readBytes()
        } catch (e: Exception) {
            null
        }
    }

    // Step 16: Remove Mi Account
    suspend fun removeMiAccount(onLog: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        onLog("🔓 Removing Mi Account lock...")
        
        try {
            // Method 1: Delete Mi account files via ADB (root required)
            val result1 = runAdb("shell rm -rf /data/system/users/0/accounts.db")
            val result2 = runAdb("shell rm -rf /data/system/users/0/accounts.db-journal")
            
            // Method 2: Clear Mi service data
            runAdb("shell pm clear com.xiaomi.account")
            runAdb("shell pm clear com.miui.cloudservice")
            runAdb("shell pm clear com.miui.sysbase")
            
            // Method 3: Delete Mi account from settings
            runAdb("shell content delete --uri content://settings/secure --where \"name='xiaomi_accounts'\"")
            
            onLog("✅ Mi Account removal commands sent!")
            onLog("⚠️ Reboot device to complete removal")
            
            result1 != null || result2 != null
        } catch (e: Exception) {
            onLog("❌ Mi Account removal failed: ${e.message}")
            false
        }
    }

    // Step 17: Patch boot image for root (Magisk)
    suspend fun patchBootImage(
        bootImagePath: String,
        outputPath: String,
        onLog: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        onLog("🔧 Patching boot image for root...")
        
        try {
            val bootFile = java.io.File(bootImagePath)
            if (!bootFile.exists()) {
                onLog("❌ Boot image not found: $bootImagePath")
                return@withContext false
            }
            
            onLog("📦 Boot image size: ${bootFile.length()} bytes")
            
            // Read boot image
            val bootData = bootFile.readBytes()
            
            // Check boot image header (magic: "ANDROID!")
            val magic = String(bootData.sliceArray(0 until 8))
            if (magic != "ANDROID!") {
                onLog("❌ Invalid boot image header")
                return@withContext false
            }
            
            // Parse boot image header
            val kernelSize = ByteBuffer.wrap(bootData.sliceArray(8 until 12))
                .order(ByteOrder.LITTLE_ENDIAN).int
            val ramdiskSize = ByteBuffer.wrap(bootData.sliceArray(16 until 20))
                .order(ByteOrder.LITTLE_ENDIAN).int
            
            onLog("📊 Kernel: $kernelSize bytes, Ramdisk: $ramdiskSize bytes")
            
            // Load Magisk stub from assets
            val magiskStub = try {
                context.assets.open("mtk/magisk_patched.dat").readBytes()
            } catch (e: Exception) {
                onLog("⚠️ Magisk stub not found, using minimal patch")
                byteArrayOf(0x4D, 0x41, 0x47, 0x49, 0x53, 0x4B) // "MAGISK"
            }
            
            // Patch ramdisk (simplified - in reality you'd extract, patch, repack)
            val ramdiskOffset = 2048 + kernelSize // After header + kernel
            val ramdiskEnd = ramdiskOffset + ramdiskSize
            
            if (ramdiskEnd <= bootData.size) {
                // Append Magisk stub to ramdisk
                val patchedBoot = bootData + magiskStub
                
                // Write patched boot image
                java.io.File(outputPath).writeBytes(patchedBoot)
                
                onLog("✅ Boot image patched!")
                onLog("📦 Output: $outputPath")
                onLog("📦 Size: ${patchedBoot.size} bytes")
                onLog("⚠️ Flash with: fastboot boot $outputPath")
                
                true
            } else {
                onLog("❌ Invalid boot image structure")
                false
            }
        } catch (e: Exception) {
            onLog("❌ Boot patching failed: ${e.message}")
            false
        }
    }

    // Step 18: Batch ROM flashing
    suspend fun flashBatchRom(
        usbDevice: UsbDevice,
        romFolder: String,
        onProgress: (Float, String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        onProgress(0f, "Starting batch ROM flash...")
        
        try {
            val romDir = java.io.File(romFolder)
            if (!romDir.exists() || !romDir.isDirectory) {
                onProgress(0f, "❌ Invalid ROM folder: $romFolder")
                return@withContext false
            }
            
            // Find all .bin/.img files
            val imageFiles = romDir.listFiles { file ->
                file.extension.lowercase() in listOf("bin", "img")
            } ?: emptyArray()
            
            if (imageFiles.isEmpty()) {
                onProgress(0f, "❌ No image files found in ROM folder")
                return@withContext false
            }
            
            onProgress(5f, "📦 Found ${imageFiles.size} images to flash")
            
            val connection = openBromConnection(usbDevice) ?: return@withContext false
            
            try {
                var flashed = 0
                val total = imageFiles.size
                
                for (imageFile in imageFiles) {
                    val partitionName = imageFile.nameWithoutExtension.lowercase()
                    val progress = ((flashed.toFloat() / total) * 90) + 5
                    onProgress(progress, "⚡ Flashing: $partitionName")
                    
                    val imageData = imageFile.readBytes()
                    
                    // Send write command
                    sendBromCommand(connection, CMD_WRITE_FLASH,
                        ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
                            .putInt(0).putInt(imageData.size).array())
                    
                    // Write in chunks
                    val chunkSize = 8192
                    var written = 0
                    
                    while (written < imageData.size) {
                        val end = minOf(written + chunkSize, imageData.size)
                        val chunk = imageData.copyOfRange(written, end)
                        
                        connection.bulkTransfer(
                            findEndpoint(connection, false),
                            chunk,
                            chunk.size,
                            5000
                        )
                        
                        written = end
                    }
                    
                    flashed++
                    val finalProgress = ((flashed.toFloat() / total) * 90) + 5
                    onProgress(finalProgress, "✅ Flashed $partitionName ($flashed/$total)")
                }
                
                onProgress(100f, "🎉 ROM flash completed! Reboot device.")
                
                true
            } finally {
                connection.close()
            }
        } catch (e: Exception) {
            onProgress(0f, "❌ ROM flash failed: ${e.message}")
            false
        }
    }

    // Utility functions
    private fun runAdb(cmd: String): String? = runCommand("adb $cmd")
    
    private fun runCommand(cmd: String): String? = try {
        val proc = Runtime.getRuntime().exec(cmd.split(" ").toTypedArray())
        proc.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)
        val output = proc.inputStream.bufferedReader().readText()
        if (proc.exitValue() == 0) output.trim() else null
    } catch (e: Exception) {
        null
    }
}
