package com.deepeye.otg.protocol.apple.iphone15

import com.deepeye.otg.usb.UsbDescriptorSnapshot

object UsbCAnalyzer {

    fun analyze(snapshot: UsbDescriptorSnapshot, profileHint: String? = null): UsbCSurfaceReport {
        val lower = (profileHint ?: snapshot.productName ?: "").lowercase()

        val thunderboltCapable =
            lower.contains("pro") ||
                lower.contains("thunderbolt") ||
                lower.contains("usb 3") ||
                lower.contains("10gb")

        val superSpeedEndpointCount = snapshot.interfaces
            .flatMap { it.endpoints }
            .count { it.maxPacketSize >= 1024 }

        val usb3Capable = thunderboltCapable || superSpeedEndpointCount > 0

        val notes = buildList {
            add("Enumeration-based USB-C surface snapshot generated")
            if (usb3Capable) {
                add("High-speed endpoint characteristics suggest USB 3.x capability")
            } else {
                add("No clear USB 3.x endpoint characteristics detected from current snapshot")
            }
            if (thunderboltCapable) {
                add("Profile hint indicates potential Pro-class USB-C feature set")
            }
            add("PD/Auth/Alt-Mode values are heuristic without dedicated controller telemetry")
        }

        return UsbCSurfaceReport(
            usb3Capable = usb3Capable,
            thunderboltCapable = thunderboltCapable,
            superSpeedEndpointCount = superSpeedEndpointCount,
            usbPdVersion = if (usb3Capable) "3.1 (heuristic)" else "Unknown",
            altMode = if (thunderboltCapable) "DisplayPort/Thunderbolt (heuristic)" else "Unknown",
            usbAuthPresent = usb3Capable,
            fuzzingCoverageHint = if (usb3Capable) 80 else 45,
            notes = notes
        )
    }
}
