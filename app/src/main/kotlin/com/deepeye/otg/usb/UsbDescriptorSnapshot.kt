package com.deepeye.otg.usb

import androidx.compose.runtime.Immutable

@Immutable
data class UsbEndpointSnapshot(
    val address: Int,
    val type: Int,
    val direction: Int,
    val maxPacketSize: Int
) {
    val isBulkIn: Boolean get() = type == 2 && direction == 128
    val isBulkOut: Boolean get() = type == 2 && direction == 0
    val isInterruptIn: Boolean get() = type == 3 && direction == 128
}

@Immutable
data class UsbInterfaceSnapshot(
    val id: Int,
    val interfaceClass: Int,
    val interfaceSubclass: Int,
    val interfaceProtocol: Int,
    val endpointCount: Int,
    val endpoints: List<UsbEndpointSnapshot>
) {
    val isExplicitAdb: Boolean
        get() = interfaceClass == 0xFF && interfaceSubclass == 0x42 && interfaceProtocol == 0x01

    val hasBulkBidirectional: Boolean
        get() = endpoints.any { it.isBulkIn } && endpoints.any { it.isBulkOut }

    val isAppleDfu: Boolean
        get() = interfaceClass == 0xFE && interfaceSubclass == 0x01 && interfaceProtocol == 0x01

    val isFastboot: Boolean
        get() = interfaceClass == 0xFF && interfaceSubclass == 0x42 && interfaceProtocol == 0x03

    val isCdcSerial: Boolean
        get() = interfaceClass == 0x02 && interfaceSubclass == 0x02

    fun classTriple(): String = "class=0x%02X sub=0x%02X proto=0x%02X".format(interfaceClass, interfaceSubclass, interfaceProtocol)
}

@Immutable
data class UsbDescriptorSnapshot(
    val vendorId: Int,
    val productId: Int,
    val deviceClass: Int,
    val deviceSubclass: Int,
    val deviceProtocol: Int,
    val manufacturerName: String?,
    val productName: String?,
    val interfaceCount: Int,
    val interfaces: List<UsbInterfaceSnapshot>
) {
    fun vidPidHex(): String = 
        "0x%04X:0x%04X".format(vendorId, productId)

    fun shortDump(): String = 
        "VID=0x${"%04X".format(vendorId)} PID=0x${"%04X".format(productId)} Intf=$interfaceCount Class=$deviceClass NAME=$productName"

    /** 
     * Degenerate devices (like some MTK BROM or Apple DFU in specific states)
     * may report 0 interfaces initially or require a specific handshake.
     */
    fun isDegenerate(): Boolean = interfaceCount == 0 && vendorId == 0

    /** Heuristic: Most security modes (EDL, BROM, FDL) use Vendor Specific class. */
    fun isVendorSpecific(): Boolean = deviceClass == 0xFF || interfaces.any { it.interfaceClass == 0xFF }
}
