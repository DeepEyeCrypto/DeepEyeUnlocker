package com.deepeye.otg.usb

import android.hardware.usb.UsbDevice

object UsbSnapshotFactory {
    fun from(device: UsbDevice): UsbDescriptorSnapshot {
        val interfaces = (0 until device.interfaceCount).map { idx ->
            val intf = device.getInterface(idx)
            val endpoints = (0 until intf.endpointCount).map { epIdx ->
                val ep = intf.getEndpoint(epIdx)
                UsbEndpointSnapshot(
                    address = ep.address,
                    type = ep.type,
                    direction = ep.direction,
                    maxPacketSize = ep.maxPacketSize
                )
            }
            UsbInterfaceSnapshot(
                id = intf.id,
                interfaceClass = intf.interfaceClass,
                interfaceSubclass = intf.interfaceSubclass,
                interfaceProtocol = intf.interfaceProtocol,
                endpointCount = intf.endpointCount,
                endpoints = endpoints
            )
        }

        return UsbDescriptorSnapshot(
            vendorId = device.vendorId,
            productId = device.productId,
            deviceClass = device.deviceClass,
            deviceSubclass = device.deviceSubclass,
            deviceProtocol = device.deviceProtocol,
            manufacturerName = device.manufacturerName,
            productName = device.productName,
            interfaceCount = device.interfaceCount,
            interfaces = interfaces
        )
    }
}
