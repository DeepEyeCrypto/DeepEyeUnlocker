package com.deepeye.simulation

/**
 * Simulates a physical USB device at hardware level.
 * Mimics exact USB descriptor responses of real devices.
 */
data class VirtualUsbDescriptor(
    val hwCode: Int,
    val vid: Int,
    val pid: Int,
    val vendor: String,
    val productName: String,
    val manufacturer: String,
    val deviceClass: String = "2/0/0",
    val interfaces: Int = 2
)

object DeviceLibrary {

    /* -------- QUALCOMM EDL DEVICES -------- */
    val REALME_14X_EDL = VirtualUsbDescriptor(
        hwCode      = 0x1209,
        vid         = 0x22D9,
        pid         = 0x0006,
        vendor      = "0x22D9",
        manufacturer = "OPLUS",
        productName = "hw_code:0x1209;feature:V6;key:02;sn:LZN7EERSZPS4VSUG",
        deviceClass = "2/0/0",
        interfaces  = 2
    )

    val SAMSUNG_S23_EDL = VirtualUsbDescriptor(
        hwCode      = 0x8550,
        vid         = 0x05C6,
        pid         = 0x9008,
        vendor      = "0x05C6",
        manufacturer = "Qualcomm",
        productName = "QUSB_BULK_CID:0x0001",
        interfaces  = 1
    )

    val REDMI_NOTE_10_EDL = VirtualUsbDescriptor(
        hwCode      = 0x6115,
        vid         = 0x05C6,
        pid         = 0x9008,
        vendor      = "0x05C6",
        manufacturer = "Qualcomm",
        productName = "QUSB_BULK_CID:0x0003",
        interfaces  = 1
    )

    /* -------- MTK BROM DEVICES -------- */
    val SAMSUNG_A14_MTK_BROM = VirtualUsbDescriptor(
        hwCode      = 0x6769,
        vid         = 0x0E8D,
        pid         = 0x0003,
        vendor      = "0x0E8D",
        manufacturer = "MediaTek",
        productName = "MT6769 BROM",
        interfaces  = 1
    )

    val REALME_C35_MTK_BROM = VirtualUsbDescriptor(
        hwCode      = 0x6765,
        vid         = 0x0E8D,
        pid         = 0x0003,
        vendor      = "0x0E8D",
        manufacturer = "MediaTek",
        productName = "MT6765 BROM",
        interfaces  = 1
    )

    /* -------- SAMSUNG MTP/MODEM -------- */
    val SAMSUNG_A54_MTP = VirtualUsbDescriptor(
        hwCode      = 0x0000,
        vid         = 0x04E8,
        pid         = 0x6860,
        vendor      = "0x04E8",
        manufacturer = "Samsung",
        productName = "SAMSUNG_Android",
        interfaces  = 3
    )

    /* -------- ALL DEVICES LIST -------- */
    val ALL = listOf(
        REALME_14X_EDL,
        SAMSUNG_S23_EDL,
        REDMI_NOTE_10_EDL,
        SAMSUNG_A14_MTK_BROM,
        REALME_C35_MTK_BROM,
        SAMSUNG_A54_MTP
    )
}
