package com.deepeye.otg.usb

data class UsbEndpointSnapshot(
    val address: Int,
    val type: Int,
    val direction: Int,
    val maxPacketSize: Int
)

data class UsbInterfaceSnapshot(
    val id: Int,
    val interfaceClass: Int,
    val interfaceSubclass: Int,
    val interfaceProtocol: Int,
    val endpointCount: Int,
    val endpoints: List<UsbEndpointSnapshot>
)

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
)
