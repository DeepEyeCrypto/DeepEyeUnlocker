import Foundation

struct IosDeviceInfo: Codable {
    var udid:        String
    var ecid:        String
    var chipId:      Int
    var chipName:    String
    var iosVersion:  String
    var productType: String
    var imei:        String?
    var serial:      String
    var isDfu:       Bool
    var isRecovery:  Bool
    var isCheckm8:   Bool
    var sessionId:   String
}

let checkm8Chips: [Int: String] = [
    0x8960: "A7 (iPhone 5S)",
    0x7000: "A8 (iPhone 6/6+)",
    0x7001: "A8X (iPad Air 2)",
    0x8000: "A9 (iPhone 6S/SE)",
    0x8003: "A9X (iPad Pro 9.7)",
    0x8010: "A10 (iPhone 7/7+)",
    0x8011: "A10X (iPad Pro 10.5)",
    0x8015: "A11 (iPhone 8/8+/X)",
]

struct DeviceDetector {
    let log: Logger

    func detect() -> IosDeviceInfo? {
        log.progress(5, "Scanning for iOS device...")

        // Check DFU via system_profiler
        let sp = ProcessRunner.run("/usr/sbin/system_profiler",
                                   args: ["SPUSBDataType"], timeout: 5)
        if sp.stdout.contains("0x1227") {
            log.emit("device_found", ["mode": "DFU"])
            return IosDeviceInfo(
                udid: "DFU_MODE", ecid: "", chipId: 0,
                chipName: "DFU Device", iosVersion: "",
                productType: "", serial: "",
                isDfu: true, isRecovery: false,
                isCheckm8: false,
                sessionId: log.sessionId,
            )
        }

        // Check normal mode via idevice_id
        let idList = ProcessRunner.run("idevice_id", args: ["-l"], timeout: 5)
        guard idList.success,
              let udid = idList.stdout.split(separator: "\n")
                               .map(String.init)
                               .first?.trimmingCharacters(in: .whitespaces),
              !udid.isEmpty
        else {
            log.error("No device found — connect iPhone via USB", layer: "DETECT")
            return nil
        }

        // Get full device info
        let info = ProcessRunner.run("ideviceinfo",
                                     args: ["-u", udid], timeout: 5)

        var props: [String: String] = [:]
        for line in info.stdout.split(separator: "\n") {
            let parts = line.split(separator: ":", maxSplits: 1)
            if parts.count == 2 {
                props[parts[0].trimmingCharacters(in: .whitespaces)] =
                    parts[1].trimmingCharacters(in: .whitespaces)
            }
        }

        let chipStr  = props["ChipID"] ?? "0"
        let chipId   = Int(chipStr.hasPrefix("0x") ?
            String(chipStr.dropFirst(2)) : chipStr, radix: 16) ?? 0

        let device = IosDeviceInfo(
            udid:        udid,
            ecid:        props["UniqueChipID"] ?? "",
            chipId:      chipId,
            chipName:    checkm8Chips[chipId] ?? "A12+ (0x\(String(chipId, radix:16)))",
            iosVersion:  props["ProductVersion"] ?? "",
            productType: props["ProductType"] ?? "",
            imei:        props["InternationalMobileEquipmentIdentity"],
            serial:      props["SerialNumber"] ?? "",
            isDfu:       false,
            isRecovery:  false,
            isCheckm8:   checkm8Chips[chipId] != nil,
            sessionId:   log.sessionId,
        )

        log.emit("device_found", [
            "mode":       "normal",
            "chip_id":    "0x\(String(chipId, radix: 16))",
            "chip_name":  device.chipName,
            "ios_version": device.iosVersion,
            "udid":       udid,
            "is_checkm8": device.isCheckm8,
        ])

        return device
    }
}
