package com.deepeye.otg.protocol.apple.iphone15

import com.deepeye.otg.usb.UsbDescriptorSnapshot

object Iphone15Session {

    fun buildProfile(
        snapshot: UsbDescriptorSnapshot,
        iosVersion: String,
        buildNumber: String = "Unknown",
        productHint: String? = null
    ): Iphone15Profile {
        val model = productHint ?: snapshot.productName ?: "Apple Device"
        val chip = detectChip(model)

        val usbSurface = UsbCAnalyzer.analyze(snapshot, model)
        val cves = IphoneCveDatabase.getCvesForDevice(chip, iosVersion)
        val chains = IphoneCveDatabase.getExploitChains(chip, iosVersion)

        return Iphone15Profile(
            isIphone15Family = chip != IphoneChip.UNKNOWN,
            modelName = model,
            chip = chip,
            ecid = null,
            boardConfig = boardConfigForModel(model),
            storageGb = null,
            iosVersion = iosVersion,
            buildNumber = buildNumber,
            basebandVersion = "Unknown",
            bootchainVersion = "Unknown",
            usbRestrictedMode = null,
            activationLocked = null,
            secureBootState = SecureBootState.FULL,
            pacEnabled = chip != IphoneChip.UNKNOWN,
            mteEnabled = chip != IphoneChip.UNKNOWN,
            pplEnabled = chip != IphoneChip.UNKNOWN,
            stolenDeviceProtection = null,
            usbSurface = usbSurface,
            applicableCves = cves,
            exploitChains = chains,
            safeCapabilities = listOf(
                "DFU/Recovery session detection",
                "USB descriptor and endpoint intelligence",
                "Version-targeted public CVE matching",
                "SHSH/nonce workflow readiness (metadata level)",
                "Activation/FMI status placeholders for paired workflows"
            ),
            knownLimitations = listOf(
                "No checkm8-class USB BROM exploit support on A12+",
                "No USB-only root/jailbreak path for A16/A17 in public domain",
                "iOS 17.5+ has no public full chain for A16/A17 as of current database",
                "PPL/SEP bypass sections are research references, not active exploit implementation"
            )
        )
    }

    private fun detectChip(model: String): IphoneChip {
        val lower = model.lowercase()
        return when {
            lower.contains("iphone 15 pro") || lower.contains("a17") -> IphoneChip.A17_PRO
            lower.contains("iphone 15") || lower.contains("a16") -> IphoneChip.A16_BIONIC
            else -> IphoneChip.UNKNOWN
        }
    }

    private fun boardConfigForModel(model: String): String {
        val lower = model.lowercase()
        return when {
            lower.contains("iphone 15 pro max") -> "D94AP"
            lower.contains("iphone 15 pro") -> "D84AP"
            lower.contains("iphone 15 plus") -> "D93AP"
            lower.contains("iphone 15") -> "D74AP"
            else -> "Unknown"
        }
    }
}
