use crate::device::detector::{scan_usb_devices, DetectedDevice, DeviceMode};
use serde::Serialize;

/// Protocol session types
#[derive(Debug, Serialize, Clone)]
pub enum ProtocolType {
    MtkBrom,
    MtkPreloader,
    QualcommEdl,
    Fastboot,
    SamsungOdin,
    UnisocFdl,
    Adb,
    Mtp,
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
    /// Auto-detect connected device
    pub fn auto_detect() -> Result<DetectedDevice, String> {
        let devices = scan_usb_devices()?;

        if devices.is_empty() {
            return Err("No supported devices found".into());
        }

        // Return first detected device (prioritize by mode)
        let priority_order = [
            DeviceMode::Brom,
            DeviceMode::Edl,
            DeviceMode::Fastboot,
            DeviceMode::SamsungOdin,
            DeviceMode::PreLoader,
            DeviceMode::UnisocFdl,
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
            DeviceMode::Edl => ProtocolType::QualcommEdl,
            DeviceMode::Fastboot => ProtocolType::Fastboot,
            DeviceMode::SamsungOdin => ProtocolType::SamsungOdin,
            DeviceMode::UnisocFdl => ProtocolType::UnisocFdl,
            DeviceMode::Adb => ProtocolType::Adb,
            DeviceMode::Mtp => ProtocolType::Mtp,
            DeviceMode::Recovery => ProtocolType::Fastboot, // Recovery often supports fastboot
            DeviceMode::Unknown(_) => ProtocolType::Unknown,
        }
    }

    /// Get human-readable device description
    pub fn device_description(device: &DetectedDevice) -> String {
        let mode_str = match &device.mode {
            DeviceMode::Brom => "MediaTek BROM",
            DeviceMode::PreLoader => "MediaTek PreLoader",
            DeviceMode::Edl => "Qualcomm EDL",
            DeviceMode::Fastboot => "Fastboot",
            DeviceMode::Adb => "Android ADB",
            DeviceMode::Mtp => "MTP Device",
            DeviceMode::Recovery => "Recovery Mode",
            DeviceMode::SamsungOdin => "Samsung Odin",
            DeviceMode::UnisocFdl => "UniSoc FDL",
            DeviceMode::Unknown(pid) => return format!("Unknown device (PID: {:04X})", pid),
        };

        let serial_info = device
            .serial
            .as_ref()
            .map(|s| format!(" [{}]", s))
            .unwrap_or_default();

        format!("{}{}", mode_str, serial_info)
    }

    /// Get chipset info based on device characteristics
    pub fn identify_chipset(device: &DetectedDevice) -> Option<String> {
        // MTK chips can be identified from VID/PID patterns
        if device.vid == 0x0E8D {
            match device.pid {
                0x0003 => Some("MediaTek BootROM".to_string()),
                0x2000 => Some("MediaTek PreLoader".to_string()),
                _ => None,
            }
        } else if device.vid == 0x05C6 {
            match device.pid {
                0x9008 => Some("Qualcomm EDL 9008".to_string()),
                0x900E => Some("Qualcomm EDL 900E".to_string()),
                _ => None,
            }
        } else {
            None
        }
    }
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
