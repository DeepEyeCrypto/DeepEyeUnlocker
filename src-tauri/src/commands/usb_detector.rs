use rusb::{Context, UsbContext};
use serde::Serialize;
use std::time::Duration;
use tauri::{AppHandle, Emitter};

#[derive(Debug, Clone, Serialize, PartialEq)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum DeviceMode {
    Adb,
    Fastboot,
    Edl,
    MtkBrom,
    MtkPreloader,
    SamsungOdin,
    UnisocFdl,
    AppleNormal,
    AppleRecovery,
    AppleDfu,
    CdcSerial,
    Mtp,
    Unknown,
}

#[derive(Debug, Clone, Serialize)]
pub struct DetectedUsbDevice {
    pub vid: u16,
    pub pid: u16,
    pub manufacturer: Option<String>,
    pub product: Option<String>,
    pub serial: Option<String>,
    pub mode: DeviceMode,
}

pub fn detect_device_mode(vid: u16, pid: u16, product: &str) -> DeviceMode {
    let product_lc = product.to_lowercase();

    // MediaTek
    if vid == 0x0E8D {
        return match pid {
            0x0003 => DeviceMode::MtkBrom,
            0x2000 | 0x0023 | 0x200A | 0x200C | 0x200E => DeviceMode::MtkPreloader,
            _ => DeviceMode::Unknown,
        };
    }

    // Qualcomm EDL
    if vid == 0x05C6 && (pid == 0x9008 || pid == 0x900E) {
        return DeviceMode::Edl;
    }

    // Samsung Odin
    if vid == 0x04E8 && (pid == 0x685D || pid == 0x6860 || pid == 0x685E) {
        return DeviceMode::SamsungOdin;
    }
    if vid == 0x04E8
        && (product_lc.contains("samsung")
            && (product_lc.contains("odin") || product_lc.contains("download")))
    {
        return DeviceMode::SamsungOdin;
    }

    // Apple
    if vid == 0x05AC {
        return match pid {
            0x1227 => DeviceMode::AppleDfu,
            0x1281 | 0x1280 | 0x1282 | 0x12A0 | 0x1338 => DeviceMode::AppleRecovery,
            p if (0x12A0..=0x12FF).contains(&p) => DeviceMode::AppleNormal,
            _ => DeviceMode::Unknown,
        };
    }

    // UniSoc
    if vid == 0x1782 && (pid == 0x4D00 || pid == 0x4D01 || pid == 0x5400) {
        return DeviceMode::UnisocFdl;
    }

    // Fastboot Heuristics
    if product_lc.contains("fastboot") {
        return DeviceMode::Fastboot;
    }

    if product_lc.contains("adb") || product_lc.contains("android debug") {
        return DeviceMode::Adb;
    }

    if product_lc.contains("mtp") || product_lc.contains("media transfer") {
        return DeviceMode::Mtp;
    }

    if product_lc.contains("cdc") || product_lc.contains("serial") {
        return DeviceMode::CdcSerial;
    }

    DeviceMode::Unknown
}

pub fn start_usb_watcher(app: AppHandle) {
    std::thread::spawn(move || {
        let context = Context::new().expect("Failed to create libusb context");
        let mut last_devices: Vec<(u16, u16)> = Vec::new();

        loop {
            if let Ok(devices) = context.devices() {
                let mut current_devices = Vec::new();
                for device in devices.iter() {
                    if let Ok(desc) = device.device_descriptor() {
                        current_devices.push((desc.vendor_id(), desc.product_id()));
                    }
                }

                // Simple diff: if count changed or PIDs changed
                if current_devices != last_devices {
                    let mut detected = Vec::new();
                    for device in devices.iter() {
                        if let Ok(desc) = device.device_descriptor() {
                            let vid = desc.vendor_id();
                            let pid = desc.product_id();

                            let mut manufacturer = None;
                            let mut product = None;
                            let mut serial = None;

                            if let Ok(handle) = device.open() {
                                manufacturer = handle.read_manufacturer_string_ascii(&desc).ok();
                                product = handle.read_product_string_ascii(&desc).ok();
                                serial = handle.read_serial_number_string_ascii(&desc).ok();
                            }

                            let mode =
                                detect_device_mode(vid, pid, product.as_deref().unwrap_or(""));

                            detected.push(DetectedUsbDevice {
                                vid,
                                pid,
                                manufacturer,
                                product,
                                serial,
                                mode,
                            });
                        }
                    }

                    app.emit("usb-devices-changed", &detected).ok();
                    
                    // Feature 2: Auto-detect chipset/platform
                    for dev in &detected {
                        let platform = match dev.vid {
                            0x0E8D => Some("MTK"),
                            0x05AC => Some("APPLE"),
                            0x04E8 => Some("SAMSUNG"),
                            0x05C6 | 0x22B8 => Some("QUALCOMM"),
                            _ => None,
                        };

                        if let Some(p) = platform {
                            app.emit("device-profile-detected", p).ok();
                        }
                    }

                    last_devices = current_devices;
                }
            }
            std::thread::sleep(Duration::from_millis(1500));
        }
    });
}
