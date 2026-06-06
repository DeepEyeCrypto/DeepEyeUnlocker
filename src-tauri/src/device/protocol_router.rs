use crate::device::detector::{scan_usb_devices, DetectedDevice, DeviceMode};
use serde::Serialize;

/// Protocol session types
#[derive(Debug, Serialize, Clone)]
pub enum ProtocolType {
    MtkBrom,
    MtkPreloader,
    MtkDa,
    QualcommEdl,
    Fastboot,
    SamsungOdin,
    UnisocFdl,
    Adb,
    Mtp,
    /// Android Recovery — supports ADB sideload, NOT fastboot
    /// (only Pixel/AOSP have fastbootd in recovery)
    Recovery,
    Unknown,
}

/// Device connection status
#[derive(Debug, Serialize, Clone)]
pub struct DeviceConnectionStatus {
    pub connected: bool,
    pub device: Option<DetectedDevice>,
    pub protocol: ProtocolType,
    pub message: String,
}

/// Protocol router for auto-detection and connection management
pub struct ProtocolRouter;

impl ProtocolRouter {
    /// Auto-detect connected device.
    /// Priority: BROM > EDL > SamsungOdin > Fastboot > PreLoader > UnisocFdl > Recovery > Adb
    pub fn auto_detect() -> Result<DetectedDevice, String> {
        let devices = scan_usb_devices()?;

        if devices.is_empty() {
            return Err("No supported devices found".into());
        }

        // Priority: low-level flash modes first, then higher-level modes
        let priority_order = [
            DeviceMode::Brom,
            DeviceMode::MtkDa,
            DeviceMode::Edl,
            DeviceMode::SamsungOdin,
            DeviceMode::Fastboot,
            DeviceMode::PreLoader,
            DeviceMode::UnisocFdl,
            DeviceMode::Recovery,
            DeviceMode::Adb,
        ];

        for mode in &priority_order {
            if let Some(device) = devices.iter().find(|d| &d.mode == mode) {
                return Ok(device.clone());
            }
        }

        // Fallback to first device
        Ok(devices.into_iter().next().unwrap())
    }

    /// Get protocol type from device mode
    pub fn mode_to_protocol(mode: &DeviceMode) -> ProtocolType {
        match mode {
            DeviceMode::Brom => ProtocolType::MtkBrom,
            DeviceMode::PreLoader => ProtocolType::MtkPreloader,
            DeviceMode::MtkDa => ProtocolType::MtkDa,
            DeviceMode::Edl => ProtocolType::QualcommEdl,
            DeviceMode::Fastboot => ProtocolType::Fastboot,
            DeviceMode::SamsungOdin => ProtocolType::SamsungOdin,
            DeviceMode::UnisocFdl => ProtocolType::UnisocFdl,
            DeviceMode::Adb => ProtocolType::Adb,
            DeviceMode::Mtp => ProtocolType::Mtp,
            // Recovery uses ADB sideload protocol, NOT fastboot.
            // Only Pixel/AOSP devices have fastbootd in recovery;
            // Samsung/Xiaomi/OPPO recovery = ADB only.
            DeviceMode::Recovery => ProtocolType::Recovery,
            DeviceMode::Unknown(_) => ProtocolType::Unknown,
        }
    }

    /// Get human-readable device description
    pub fn device_description(device: &DetectedDevice) -> String {
        let mode_str = match &device.mode {
            DeviceMode::Brom => "MediaTek BROM",
            DeviceMode::PreLoader => "MediaTek PreLoader",
            DeviceMode::MtkDa => "MediaTek DA",
            DeviceMode::Edl => "Qualcomm EDL",
            DeviceMode::Fastboot => "Fastboot",
            DeviceMode::Adb => "Android ADB",
            DeviceMode::Mtp => "MTP Device",
            DeviceMode::Recovery => "Recovery Mode",
            DeviceMode::SamsungOdin => "Samsung Odin",
            DeviceMode::UnisocFdl => "UniSoc FDL",
            DeviceMode::Unknown(pid) => return format!("Unknown device (PID: {:04X})", pid),
        };

        // Build description: mode + product name or serial
        let extra = device
            .product
            .as_ref()
            .map(|p| format!(" — {}", p))
            .or_else(|| device.serial.as_ref().map(|s| format!(" [{}]", s)))
            .unwrap_or_default();

        format!("{}{}", mode_str, extra)
    }

    /// Get chipset info based on device characteristics.
    /// Uses VID/PID + product string for more accurate identification.
    pub fn identify_chipset(device: &DetectedDevice) -> Option<String> {
        match device.vid {
            // MediaTek — VID/PID based
            0x0E8D => match device.pid {
                0x0003 => {
                    // BROM: try to extract SoC from product string
                    device
                        .product
                        .as_ref()
                        .and_then(|p| extract_mtk_soc(p))
                        .or_else(|| Some("MediaTek BootROM".to_string()))
                }
                0x2000 | 0x0006 => Some("MediaTek PreLoader".to_string()),
                0x2001 => Some("MediaTek DA".to_string()),
                0x0C01 => Some("MediaTek Fastboot".to_string()),
                _ => Some(format!("MediaTek (PID:{:04X})", device.pid)),
            },

            // Qualcomm — VID/PID based
            0x05C6 => match device.pid {
                0x9008 => Some("Qualcomm EDL 9008".to_string()),
                0x900E => Some("Qualcomm EDL 900E".to_string()),
                _ => Some(format!("Qualcomm (PID:{:04X})", device.pid)),
            },

            // Samsung — infer from product string
            0x04E8 => device
                .product
                .as_ref()
                .map(|p| format!("Samsung — {}", p))
                .or_else(|| Some("Samsung".to_string())),

            // Xiaomi
            0x2717 => device
                .product
                .as_ref()
                .map(|p| format!("Xiaomi — {}", p))
                .or_else(|| Some("Xiaomi".to_string())),

            // Huawei
            0x12D1 => Some("Huawei/Honor".to_string()),

            // OPPO/Realme
            0x22D9 => Some("OPPO/Realme".to_string()),

            // OnePlus
            0x2A70 => Some("OnePlus".to_string()),

            _ => None,
        }
    }
}

/// Try to extract MTK SoC name from USB product string.
/// BROM product strings often contain chip identifiers like "MT6765" or "MT6833".
fn extract_mtk_soc(product: &str) -> Option<String> {
    // Look for MT#### pattern
    let upper = product.to_uppercase();
    if let Some(pos) = upper.find("MT") {
        let soc: String = upper[pos..]
            .chars()
            .take_while(|c| c.is_ascii_alphanumeric())
            .collect();
        if soc.len() >= 6 {
            return Some(soc);
        }
    }
    None
}

/// Scan and return all connected devices with status
pub fn scan_all_devices() -> Result<Vec<DeviceConnectionStatus>, String> {
    let devices = scan_usb_devices()?;

    Ok(devices
        .iter()
        .map(|device| DeviceConnectionStatus {
            connected: true,
            device: Some(device.clone()),
            protocol: ProtocolRouter::mode_to_protocol(&device.mode),
            message: ProtocolRouter::device_description(device),
        })
        .collect())
}
