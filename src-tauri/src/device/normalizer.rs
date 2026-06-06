use super::types::{DeviceConnectionState, DeviceMode, DevicePlatform, DeviceSnapshot};
use crate::commands::usb_detector::DetectedUsbDevice;
use std::time::{SystemTime, UNIX_EPOCH};

pub struct SnapshotNormalizer;

impl SnapshotNormalizer {
    pub fn normalize_usb_device(usb: &DetectedUsbDevice) -> DeviceSnapshot {
        let now = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap_or_default()
            .as_millis() as u64;

        let platform = match usb.vid {
            0x05AC => DevicePlatform::Ios,
            0x0E8D => DevicePlatform::Mtk,
            0x05C6 | 0x22B8 => DevicePlatform::Qualcomm,
            0x04E8 => DevicePlatform::Android, // Samsung
            0x1782 => DevicePlatform::Unisoc,
            _ => DevicePlatform::Unknown,
        };

        // Map DeviceMode
        let mode = match usb.mode {
            crate::commands::usb_detector::DeviceMode::Adb => DeviceMode::Adb,
            crate::commands::usb_detector::DeviceMode::Fastboot => DeviceMode::Fastboot,
            crate::commands::usb_detector::DeviceMode::Edl => DeviceMode::Edl,
            crate::commands::usb_detector::DeviceMode::MtkBrom => DeviceMode::Purple, // BROM is diagnostic/purple-like
            crate::commands::usb_detector::DeviceMode::MtkPreloader => DeviceMode::Purple,
            crate::commands::usb_detector::DeviceMode::SamsungOdin => DeviceMode::Purple,
            crate::commands::usb_detector::DeviceMode::UnisocFdl => DeviceMode::Purple,
            crate::commands::usb_detector::DeviceMode::AppleNormal => DeviceMode::Normal,
            crate::commands::usb_detector::DeviceMode::AppleRecovery => DeviceMode::Recovery,
            crate::commands::usb_detector::DeviceMode::AppleDfu => DeviceMode::Dfu,
            _ => DeviceMode::Unknown,
        };

        let mut capability_flags = Vec::new();
        let risk_flags = Vec::new();

        // Default capabilities based on platform/mode
        match platform {
            DevicePlatform::Ios => {
                capability_flags.push("canReadInfo".to_string());
                match mode {
                    DeviceMode::Normal => {
                        capability_flags.push("canEnterRecovery".to_string());
                        capability_flags.push("canStartSession".to_string());
                        capability_flags.push("canRunToolbox".to_string());
                    }
                    DeviceMode::Recovery => {
                        capability_flags.push("canExitRecovery".to_string());
                        capability_flags.push("canStartSession".to_string());
                    }
                    DeviceMode::Dfu => {
                        capability_flags.push("canStartSession".to_string());
                    }
                    _ => {}
                }
            }
            DevicePlatform::Mtk | DevicePlatform::Qualcomm | DevicePlatform::Unisoc => {
                capability_flags.push("canReadInfo".to_string());
                capability_flags.push("canRunToolbox".to_string());
                if mode == DeviceMode::Adb {
                    capability_flags.push("canUseAdb".to_string());
                } else if mode == DeviceMode::Fastboot {
                    capability_flags.push("canUseFastboot".to_string());
                }
            }
            _ => {}
        }

        let serial = usb.serial.clone().unwrap_or_else(|| "N/A".to_string());
        let model = usb.product.clone().unwrap_or_else(|| match platform {
            DevicePlatform::Ios => "Apple Device".to_string(),
            DevicePlatform::Mtk => "MediaTek Device".to_string(),
            DevicePlatform::Qualcomm => "Qualcomm Device".to_string(),
            DevicePlatform::Unisoc => "Unisoc Device".to_string(),
            _ => "Unknown Device".to_string(),
        });

        DeviceSnapshot {
            id: if serial == "N/A" {
                format!("usb-{:04x}-{:04x}", usb.vid, usb.pid)
            } else {
                serial.clone()
            },
            connection_state: DeviceConnectionState::Connected,
            platform,
            mode,
            manufacturer: usb.manufacturer.clone(),
            product_name: usb.product.clone(),
            model,
            model_code: None,
            serial,
            os_version: None,
            chipset: None,
            is_supported: platform != DevicePlatform::Unknown,
            support_reason: None,
            risk_flags,
            capability_flags,
            detected_at: now,
            updated_at: now,
        }
    }
}
