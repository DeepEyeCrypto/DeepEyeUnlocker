package com.deepeye.otg.protocol.apple.model

// ──────────────────────────────────────────────────────────────
// Apple Device Profile — Observed Metadata Model
// DeepEye OTG — Protocol / Apple Module (Part 2 + 3)
// ──────────────────────────────────────────────────────────────

/**
 * Device connection mode observed via USB.
 */
enum class AppleDeviceMode {
    /** Normal mode — device is locked or unlocked, not in recovery */
    NORMAL,
    /** Recovery Mode — iBoot prompt, iTunes logo */
    RECOVERY,
    /** DFU Mode — no screen output, lowest-level USB interface */
    DFU,
    /** Restore Mode — mid-restore state */
    RESTORE,
    /** Diagnostics Mode */
    DIAGNOSTICS,
    /** Unknown / unclassified */
    UNKNOWN
}

/**
 * USB pairing trust state as observed externally.
 */
enum class PairingState {
    /** Device has an active trust relationship with this host */
    TRUSTED,
    /** Device has NOT been paired — trust dialog not accepted */
    UNTRUSTED,
    /** Pairing state cannot be determined */
    UNKNOWN,
    /** Device is in a mode where pairing doesn't apply (DFU/Recovery) */
    NOT_APPLICABLE
}

/**
 * Observed component-level build metadata from an Apple device.
 * Populated from lockdown/usbmux responses, firmware metadata,
 * or device info queries where legitimately available.
 */
data class ComponentBuildInfo(
    val component: String,          // e.g. "WebKit", "Kernel", "dyld", "Safari"
    val buildVersion: String?,      // e.g. "618.1.15.10.5"
    val sourceDescription: String = "device_query",
    val observedAt: Long = System.currentTimeMillis()
)

/**
 * Complete observed Apple device profile.
 *
 * Captures everything we can legitimately determine about a connected
 * Apple device via USB host enumeration and standard protocol queries.
 *
 * Design principles:
 * - All fields nullable where data may not be available
 * - Tracks how and when each piece of data was observed
 * - No bypass or exploit logic — pure metadata collection
 * - Serializable for JSON export and forensic logging
 */
data class AppleDeviceProfile(
    // ── USB Identification ──
    /** USB Vendor ID (Apple = 0x05AC) */
    val usbVendorId: Int = 0x05AC,

    /** USB Product ID — changes by device mode */
    val usbProductId: Int,

    /** USB serial number string (UDID or ECID in DFU) */
    val usbSerialNumber: String? = null,

    /** USB device descriptor iProduct string */
    val usbProductString: String? = null,

    /** USB device descriptor iManufacturer string */
    val usbManufacturerString: String? = null,

    // ── Device Identity ──
    /** Device UDID (40-char hex for pre-A12, 24+16 hyphenated for A12+) */
    val udid: String? = null,

    /** ECID — unique chip identifier (visible in DFU/Recovery via USB serial) */
    val ecid: String? = null,

    /** Device model identifier, e.g. "iPhone15,3" */
    val modelIdentifier: String? = null,

    /** Marketing name, e.g. "iPhone 15 Pro Max" */
    val marketingName: String? = null,

    /** Hardware model, e.g. "D84AP" */
    val hardwareModel: String? = null,

    /** SoC identifier, e.g. "A17 Pro" */
    val chipIdentifier: String? = null,

    /** Board configuration, e.g. "d84ap" */
    val boardConfig: String? = null,

    // ── Software State ──
    /** iOS version, e.g. "26.1.2" */
    val iosVersion: String? = null,

    /** Build ID, e.g. "24A345" */
    val buildVersion: String? = null,

    /** Baseband version */
    val basebandVersion: String? = null,

    /** Observed component-level builds */
    val componentBuilds: List<ComponentBuildInfo> = emptyList(),

    // ── Connection / Session State ──
    /** Current device mode */
    val deviceMode: AppleDeviceMode = AppleDeviceMode.UNKNOWN,

    /** Pairing trust state */
    val pairingState: PairingState = PairingState.UNKNOWN,

    /** Whether the device screen is locked */
    val isScreenLocked: Boolean? = null,

    /** Whether a passcode is set */
    val isPasscodeSet: Boolean? = null,

    /** Whether Activation Lock is enabled */
    val isActivationLocked: Boolean? = null,

    // ── Firmware / Security ──
    /** SEP (Secure Enclave Processor) version if available */
    val sepVersion: String? = null,

    /** iBoot version (visible in Recovery/DFU) */
    val iBootVersion: String? = null,

    /** Whether Secure Boot is enforced */
    val secureBootEnforced: Boolean? = null,

    /** Whether nonce is set for SHSH / APTicket */
    val apNonce: String? = null,

    /** Whether a Cryptex has been applied */
    val hasCryptex: Boolean? = null,

    // ── Metadata ──
    /** When this profile was first created */
    val firstSeen: Long = System.currentTimeMillis(),

    /** When this profile was last updated */
    val lastSeen: Long = System.currentTimeMillis(),

    /** Source of the observation (e.g. "usbmux_query", "dfu_serial_parse") */
    val observationSource: String = "usb_enumeration",

    /** Free-form analyst notes */
    val notes: String = ""
) {
    /**
     * Whether this is a known Apple device (VID check).
     */
    val isAppleDevice: Boolean
        get() = usbVendorId == APPLE_VID

    /**
     * Derive device mode from USB PID if not explicitly set.
     * Apple uses specific PIDs for each mode.
     */
    val inferredMode: AppleDeviceMode
        get() = when (usbProductId) {
            in DFU_PIDS -> AppleDeviceMode.DFU
            in RECOVERY_PIDS -> AppleDeviceMode.RECOVERY
            in NORMAL_PIDS -> AppleDeviceMode.NORMAL
            in RESTORE_PIDS -> AppleDeviceMode.RESTORE
            else -> AppleDeviceMode.UNKNOWN
        }

    /**
     * Short display string for UI.
     */
    val displayName: String
        get() = marketingName
            ?: modelIdentifier
            ?: usbProductString
            ?: "Apple Device (PID=0x${usbProductId.toString(16).uppercase()})"

    companion object {
        const val APPLE_VID = 0x05AC

        // ── Known Apple USB PIDs by mode ──
        // These are well-documented public values.

        val DFU_PIDS = setOf(
            0x1227,  // DFU Mode (all devices)
            0x1226,  // DFU (legacy)
        )

        val RECOVERY_PIDS = setOf(
            0x1281,  // Recovery Mode (modern)
            0x1280,  // Recovery Mode (legacy)
        )

        val NORMAL_PIDS = setOf(
            0x12A8,  // Normal mode (iPhone, iPod)
            0x12AB,  // Normal mode (iPad)
        )

        val RESTORE_PIDS = setOf(
            0x1282,  // Restore mode
        )

        /**
         * Parse ECID and other fields from DFU/Recovery serial string.
         *
         * DFU serial format typically: "CPID:XXXX CPRV:XX CPFM:XX SCEP:XX BDID:XX ECID:XXXXXXXXXX ..."
         */
        fun parseRecoverySerial(serialString: String): Map<String, String> {
            val fields = mutableMapOf<String, String>()
            val parts = serialString.split(" ")
            for (part in parts) {
                val kv = part.split(":", limit = 2)
                if (kv.size == 2) {
                    fields[kv[0].trim()] = kv[1].trim()
                }
            }
            return fields
        }

        /**
         * Create a profile from USB enumeration data only
         * (minimum available information).
         */
        fun fromUsbDescriptor(
            vendorId: Int,
            productId: Int,
            serialNumber: String?,
            productString: String?,
            manufacturerString: String?
        ): AppleDeviceProfile {
            val profile = AppleDeviceProfile(
                usbVendorId = vendorId,
                usbProductId = productId,
                usbSerialNumber = serialNumber,
                usbProductString = productString,
                usbManufacturerString = manufacturerString,
                observationSource = "usb_descriptor"
            )

            // If in DFU/Recovery, try to parse serial for ECID etc.
            if (profile.inferredMode == AppleDeviceMode.DFU ||
                profile.inferredMode == AppleDeviceMode.RECOVERY
            ) {
                if (serialNumber != null) {
                    val fields = parseRecoverySerial(serialNumber)
                    return profile.copy(
                        ecid = fields["ECID"],
                        chipIdentifier = fields["CPID"],
                        boardConfig = fields["BDID"],
                        deviceMode = profile.inferredMode
                    )
                }
            }

            return profile.copy(deviceMode = profile.inferredMode)
        }
    }
}
