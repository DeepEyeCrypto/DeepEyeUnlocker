use rusb::{Context, DeviceHandle, UsbContext};
use serde::Serialize;
use std::time::Duration;

const FASTBOOT_VID: u16 = 0x18D1;
const FASTBOOT_PID_D00D: u16 = 0xD00D;
const MTK_FASTBOOT_PID: u16 = 0x0C01;
const FASTBOOT_TIMEOUT: Duration = Duration::from_secs(10);
const FASTBOOT_CHUNK_SIZE: usize = 0x10000; // 64KB chunks

/// Fastboot device information
#[derive(Debug, Serialize, Clone)]
pub struct FastbootDeviceInfo {
    pub serial: String,
    pub product: String,
    pub variant: String,
    pub bootloader_version: String,
    pub baseband_version: String,
    pub secure_boot: bool,
    pub unlocked: bool,
}

/// Fastboot protocol session
pub struct FastbootSession {
    handle: DeviceHandle<Context>,
}

impl FastbootSession {
    const EP_IN: u8 = 0x81;
    const EP_OUT: u8 = 0x01;

    /// Open fastboot device
    pub fn open() -> Result<Self, String> {
        let context = Context::new().map_err(|e| e.to_string())?;
        let devices = context.devices().map_err(|e| e.to_string())?;

        for device in devices.iter() {
            let desc = match device.device_descriptor() {
                Ok(d) => d,
                Err(_) => continue,
            };

            let pid = desc.product_id();
            if desc.vendor_id() == FASTBOOT_VID
                && (pid == FASTBOOT_PID_D00D || pid == MTK_FASTBOOT_PID)
            {
                let handle = device.open().map_err(|e| e.to_string())?;
                handle.claim_interface(0).map_err(|e| e.to_string())?;
                return Ok(Self { handle });
            }
        }

        Err("No fastboot device found".into())
    }

    /// Get device variables (serial, product, etc.)
    pub fn get_variables(&self) -> Result<FastbootDeviceInfo, String> {
        let serial = self.get_var("serialno")?;
        let product = self.get_var("product")?;
        let variant = self.get_var("variant")?;
        let bootloader_version = self.get_var("version-bootloader")?;
        let baseband_version = self.get_var("version-baseband")?;
        let secure_boot = self.get_var("secure")? == "yes";
        let unlocked = self.get_var("unlocked")? == "yes";

        Ok(FastbootDeviceInfo {
            serial,
            product,
            variant,
            bootloader_version,
            baseband_version,
            secure_boot,
            unlocked,
        })
    }

    /// Flash partition with data
    pub fn flash_partition(&self, partition: &str, data: &[u8]) -> Result<(), String> {
        log::info!(
            "[Fastboot] Flashing '{}' with {} bytes",
            partition,
            data.len()
        );

        // Send download command
        let download_cmd = format!("download:{:08x}", data.len());
        self.send_command(&download_cmd)?;

        // Wait for DATA response
        let response = self.read_response()?;
        if !response.starts_with("DATA") {
            return Err(format!("Expected DATA response, got: {}", response));
        }

        // Send data in chunks
        for chunk in data.chunks(FASTBOOT_CHUNK_SIZE) {
            self.write(chunk)?;
        }

        // Wait for OKAY after download
        let status = self.read_response()?;
        if !status.starts_with("OKAY") {
            return Err(format!("Download failed: {}", status));
        }

        // Send actual flash command
        self.send_command(&format!("flash:{}", partition))?;

        // Wait for OKAY after flash
        let flash_status = self.read_response()?;
        if !flash_status.starts_with("OKAY") {
            return Err(format!("Flash failed: {}", flash_status));
        }

        log::info!("[Fastboot] Successfully flashed '{}'", partition);
        Ok(())
    }

    /// Reboot device
    pub fn reboot(&self) -> Result<(), String> {
        log::info!("[Fastboot] Rebooting device");
        self.send_command("reboot")?;
        Ok(())
    }

    /// Reboot to bootloader
    pub fn reboot_bootloader(&self) -> Result<(), String> {
        log::info!("[Fastboot] Rebooting to bootloader");
        self.send_command("reboot-bootloader")?;
        Ok(())
    }

    /// Reboot to recovery
    pub fn reboot_recovery(&self) -> Result<(), String> {
        log::info!("[Fastboot] Rebooting to recovery");
        self.send_command("reboot-recovery")?;
        Ok(())
    }

    /// Lock bootloader
    pub fn lock_bootloader(&self) -> Result<(), String> {
        log::warn!("[Fastboot] Locking bootloader");
        self.send_command("flashing lock")?;
        let status = self.read_response()?;
        if status != "OKAY" {
            return Err(format!("Lock failed: {}", status));
        }
        Ok(())
    }

    /// Get single variable
    fn get_var(&self, name: &str) -> Result<String, String> {
        self.send_command(&format!("getvar:{}", name))?;
        let response = self.read_response()?;

        // Fastboot protocol returns "OKAY<value>"
        if let Some(stripped) = response.strip_prefix("OKAY") {
            Ok(stripped.trim().to_string())
        } else {
            Ok(String::new())
        }
    }

    /// Send fastboot command
    fn send_command(&self, cmd: &str) -> Result<(), String> {
        let data = cmd.as_bytes();
        self.write(data)
    }

    /// Write data to USB
    fn write(&self, data: &[u8]) -> Result<(), String> {
        let written = self
            .handle
            .write_bulk(Self::EP_OUT, data, FASTBOOT_TIMEOUT)
            .map_err(|e| e.to_string())?;

        if written != data.len() {
            return Err(format!(
                "Short write: expected {} bytes, wrote {}",
                data.len(),
                written
            ));
        }

        Ok(())
    }

    /// Read response from device
    fn read_response(&self) -> Result<String, String> {
        let mut buf = vec![0u8; 64];
        let read = self
            .handle
            .read_bulk(Self::EP_IN, &mut buf, FASTBOOT_TIMEOUT)
            .map_err(|e| e.to_string())?;

        let response = String::from_utf8_lossy(&buf[..read]);
        log::debug!("[Fastboot] Response: {}", response.trim());
        Ok(response.trim().to_string())
    }
}

/// Detect if fastboot device is connected
pub fn fastboot_device_connected() -> Result<bool, String> {
    let context = Context::new().map_err(|e| e.to_string())?;
    let devices = context.devices().map_err(|e| e.to_string())?;

    for device in devices.iter() {
        let desc = match device.device_descriptor() {
            Ok(d) => d,
            Err(_) => continue,
        };

        if desc.vendor_id() == FASTBOOT_VID {
            let pid = desc.product_id();
            if pid == FASTBOOT_PID_D00D || pid == MTK_FASTBOOT_PID {
                return Ok(true);
            }
        }
    }

    Ok(false)
}
