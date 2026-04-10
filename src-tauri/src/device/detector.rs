use rusb::{Context, UsbContext};
use serde::{Deserialize, Serialize};

/// Device operational mode classification based on USB VID/PID
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub enum DeviceMode {
    /// MediaTek Boot ROM (0x0E8D:0x0003)
    Brom,
    /// MediaTek PreLoader (0x0E8D:0x2000)
    PreLoader,
    /// Qualcomm Emergency Download Mode (0x05C6:0x9008)
    Edl,
    /// Generic Fastboot (0x18D1:0xD00D)
    Fastboot,
    /// Android Debug Bridge
    Adb,
    /// Media Transfer Protocol
    Mtp,
    /// Android Recovery mode
    Recovery,
    /// Samsung Download/Odin mode
    SamsungOdin,
    /// UniSoc Flash Download Mode
    UnisocFdl,
    /// Unknown or unsupported device
    Unknown(u16),
}

/// Detected USB device with protocol classification
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DetectedDevice {
    pub mode: DeviceMode,
    pub vid: u16,
    pub pid: u16,
    pub serial: Option<String>,
    pub manufacturer: Option<String>,
    pub product: Option<String>,
    pub bus: u8,
    pub address: u8,
    pub chipset: Option<String>,
    pub detected_at: u64,
}

// Known USB VID/PID mappings (consolidated from Android Kotlin codebase)
const MTK_VID: u16 = 0x0E8D;
const QCOM_VID: u16 = 0x05C6;
const SAMSUNG_VID: u16 = 0x04E8;
const GOOGLE_VID: u16 = 0x18D1;
const UNISOC_VID: u16 = 0x1782;

/// Classify device mode from VID/PID pair
fn classify_device(vid: u16, pid: u16) -> DeviceMode {
    match (vid, pid) {
        // MediaTek BROM
        (MTK_VID, 0x0003) => DeviceMode::Brom,
        (MTK_VID, 0x2000) | (MTK_VID, 0x0006) => DeviceMode::PreLoader,

        // Qualcomm EDL
        (QCOM_VID, 0x9008) | (QCOM_VID, 0x900E) => DeviceMode::Edl,

        // Fastboot
        (GOOGLE_VID, 0xD00D) | (MTK_VID, 0x0C01) => DeviceMode::Fastboot,

        // ADB
        (GOOGLE_VID, 0x4EE7) => DeviceMode::Recovery,
        (GOOGLE_VID, 0x4EE2) | (GOOGLE_VID, 0x4EE1) => DeviceMode::Adb,

        // Samsung Odin/Download mode
        (SAMSUNG_VID, 0x685D) | (SAMSUNG_VID, 0x6860) | (SAMSUNG_VID, 0x6861) => {
            DeviceMode::SamsungOdin
        }

        // UniSoc FDL
        (UNISOC_VID, 0x4D00) => DeviceMode::UnisocFdl,

        // MTP devices (various vendors)
        (_, _) if is_mtp_device(vid, pid) => DeviceMode::Mtp,

        _ => DeviceMode::Unknown(pid),
    }
}

/// Check if device is MTP class
fn is_mtp_device(vid: u16, pid: u16) -> bool {
    matches!(
        (vid, pid),
        (MTK_VID, 0x201D)
            | (QCOM_VID, 0x9048)
            | (0x04E8, 0x6860)
            | (0x18D1, 0x4EE2)
    )
}

/// Scan all USB devices and return detected supported devices
pub fn scan_usb_devices() -> Result<Vec<DetectedDevice>, String> {
    let context = Context::new().map_err(|e| e.to_string())?;
    let mut devices = Vec::new();

    let device_list = context.devices().map_err(|e| e.to_string())?;

    for device in device_list.iter() {
        let desc = match device.device_descriptor() {
            Ok(d) => d,
            Err(_) => continue, // Skip devices we can't read
        };

        let vid = desc.vendor_id();
        let pid = desc.product_id();
        let mode = classify_device(vid, pid);

        // Skip unknown devices
        if matches!(mode, DeviceMode::Unknown(_)) {
            continue;
        }

        // Try to open device for string descriptors
        let handle = device.open().ok();
        let timeout = std::time::Duration::from_millis(200);
        let languages = handle
            .as_ref()
            .and_then(|h| h.read_languages(timeout).ok())
            .unwrap_or_default();
        let lang = languages.first().copied();

        // Extract serial number
        let serial = handle.as_ref().zip(lang).and_then(|(h, l)| {
            desc.serial_number_string_index()
                .and_then(|i| h.read_string_descriptor(l, i, timeout).ok())
        });

        // Extract manufacturer
        let manufacturer = handle.as_ref().zip(lang).and_then(|(h, l)| {
            desc.manufacturer_string_index()
                .and_then(|i| h.read_string_descriptor(l, i, timeout).ok())
        });

        // Extract product name
        let product = handle.as_ref().zip(lang).and_then(|(h, l)| {
            desc.product_string_index()
                .and_then(|i| h.read_string_descriptor(l, i, timeout).ok())
        });

        devices.push(DetectedDevice {
            mode,
            vid,
            pid,
            serial,
            manufacturer,
            product,
            bus: device.bus_number(),
            address: device.address(),
            chipset: None, // Populated by protocol-specific code
            detected_at: std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .unwrap_or_default()
                .as_millis() as u64,
        });
    }

    Ok(devices)
}

/// Check if any device of specific mode is connected
pub fn has_device_mode(mode: DeviceMode) -> Result<bool, String> {
    let devices = scan_usb_devices()?;
    Ok(devices.iter().any(|d| d.mode == mode))
}
