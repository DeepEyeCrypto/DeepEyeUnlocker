package com.deepeye.otg.usb

import android.hardware.usb.UsbDevice
import android.util.Log

object UsbSnapshotFactory {
    private const val TAG = "UsbSnapshotFactory"

    fun from(device: UsbDevice): UsbDescriptorSnapshot {
        val safeInterfaceCount = runCatching { device.interfaceCount }
            .getOrDefault(0)
            .coerceAtLeast(0)

        val interfaces = (0 until safeInterfaceCount).mapNotNull { idx ->
            val intf = runCatching { device.getInterface(idx) }
                .getOrElse {
                    Log.w(TAG, "Skipping interface[$idx] due to exception: ${it.message}")
                    null
                } ?: return@mapNotNull null

            val endpoints = (0 until intf.endpointCount).mapNotNull { epIdx ->
                val ep = runCatching { intf.getEndpoint(epIdx) }
                    .getOrElse {
                        Log.w(TAG, "Skipping endpoint[$epIdx] on interface[$idx]: ${it.message}")
                        null
                    } ?: return@mapNotNull null

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
                endpointCount = endpoints.size,
                endpoints = endpoints
            )
        }

        return UsbDescriptorSnapshot(
            vendorId = device.vendorId,
            productId = device.productId,
            deviceClass = device.deviceClass,
            deviceSubclass = device.deviceSubclass,
            deviceProtocol = device.deviceProtocol,
            manufacturerName = sanitizeNullableText(device.manufacturerName),
            productName = sanitizeNullableText(device.productName),
            interfaceCount = interfaces.size,
            interfaces = interfaces
        )
    }

    private fun sanitizeNullableText(value: String?): String? {
        if (value == null) return null
        return runCatching {
            value.filter { !it.isISOControl() || it == '\n' || it == '\t' }
                .takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
