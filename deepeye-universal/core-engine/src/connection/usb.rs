use crate::models::DevicePlatform;
use anyhow::Result;

#[derive(Debug, serde::Serialize, Clone)]
pub struct UsbDeviceInfo {
    pub vid: u16,
    pub pid: u16,
    pub name: String,
    pub platform: DevicePlatform,
    pub mode: String,
}

pub struct UsbManager;

impl UsbManager {
    pub fn new() -> Self {
        Self
    }

    pub fn auto_detect_platform() -> Result<DevicePlatform> {
        for dev in nusb::list_devices()? {
            let vid = dev.vendor_id();
            let pid = dev.product_id();

            if vid == 0x05C6 && pid == 0x9008 {
                return Ok(DevicePlatform::Qualcomm);
            }
            if vid == 0x0E8D && (pid == 0x0003 || pid == 0x2000) {
                return Ok(DevicePlatform::MTK);
            }
            if vid == 0x1782 && pid == 0x4D00 {
                return Ok(DevicePlatform::UniSoc);
            }
            if vid == 0x04E8 && pid == 0x685D {
                return Ok(DevicePlatform::Samsung);
            }

            // Fastboot (Generic)
            if pid == 0x4EE0 || pid == 0x0D02 || (vid == 0x18D1 && pid == 0x4EE0) {
                return Ok(DevicePlatform::Unknown); // Could be any platform in fastboot
            }
        }
        Ok(DevicePlatform::Unknown)
    }

    pub fn list_detailed_devices() -> Result<Vec<UsbDeviceInfo>> {
        let mut devices = Vec::new();
        for dev in nusb::list_devices()? {
            let vid = dev.vendor_id();
            let pid = dev.product_id();

            let info = match (vid, pid) {
                (0x05C6, 0x9008) => Some(("Qualcomm", DevicePlatform::Qualcomm, "EDL")),
                (0x0E8D, 0x0003) => Some(("MediaTek", DevicePlatform::MTK, "BROM")),
                (0x0E8D, 0x2000) => Some(("MediaTek", DevicePlatform::MTK, "Preloader")),
                (0x04E8, 0x685D) => Some(("Samsung", DevicePlatform::Samsung, "Download")),
                (0x1782, 0x4D00) => Some(("UniSoc", DevicePlatform::UniSoc, "SPD-Diagnostics")),
                (0x18D1, 0x4EE0) => Some(("Google/Generic", DevicePlatform::Unknown, "Fastboot")),
                (0x18D1, 0x4EE7) => Some(("Google/Generic", DevicePlatform::Unknown, "ADB")),
                (0x2717, 0xFF40) => Some(("Xiaomi", DevicePlatform::Qualcomm, "Fastboot")),
                (0x2717, 0xFF88) => Some(("Xiaomi", DevicePlatform::MTK, "Fastboot")),
                (0x1949, 0x0001) => Some(("Amazon", DevicePlatform::MTK, "Preloader")),
                _ => None,
            };

            if let Some((name, platform, mode)) = info {
                devices.push(UsbDeviceInfo {
                    vid,
                    pid,
                    name: name.to_string(),
                    platform,
                    mode: mode.to_string(),
                });
            }
        }
        Ok(devices)
    }

    pub fn scan_devices(&self) -> Result<Vec<String>> {
        let details = Self::list_detailed_devices()?;
        if details.is_empty() {
            return Ok(vec!["No DeepEye compatible devices found.".into()]);
        }

        Ok(details
            .into_iter()
            .map(|d| {
                format!(
                    "{} in {} Mode [{:04X}:{:04X}]",
                    d.name, d.mode, d.vid, d.pid
                )
            })
            .collect())
    }

    pub fn open_device(&self, vid: u16, pid: u16) -> Result<nusb::Device> {
        for dev_info in nusb::list_devices()? {
            if dev_info.vendor_id() == vid && dev_info.product_id() == pid {
                return Ok(dev_info.open()?);
            }
        }
        Err(anyhow::anyhow!("Device not found: {:04X}:{:04X}", vid, pid))
    }
}
