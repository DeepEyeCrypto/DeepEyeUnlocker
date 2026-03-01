use anyhow::Result;

pub struct UsbManager;

impl UsbManager {
    pub fn new() -> Self {
        Self
    }

    pub fn scan_devices(&self) -> Result<Vec<String>> {
        let mut devices = Vec::new();
        for dev in nusb::list_devices()? {
            let vid = dev.vendor_id();
            let pid = dev.product_id();
            tracing::info!(
                "Found Device: {:04x}:{:04x} {:?}",
                vid,
                pid,
                dev.manufacturer_string()
            );

            // Temporary simple check for Qualcomm EDL mode (05C6:9008)
            if vid == 0x05C6 && pid == 0x9008 {
                devices.push(format!(
                    "Qualcomm HS-USB QDLoader 9008 ({:04x}:{:04x})",
                    vid, pid
                ));
            }
        }
        Ok(devices)
    }
}
