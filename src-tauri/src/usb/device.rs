use rusb::{Context, UsbContext};
use serde::Serialize;
use std::time::Duration;

#[derive(Debug, Clone, Serialize)]
pub struct UsbDeviceInfo {
    pub vendor_id: u16,
    pub product_id: u16,
    pub serial: Option<String>,
    pub name: String,
}

/// List all USB devices connected to the system
pub fn list_usb_devices() -> Result<Vec<UsbDeviceInfo>, String> {
    let ctx = Context::new().map_err(|e| e.to_string())?;
    let devices = ctx.devices().map_err(|e| e.to_string())?;
    let mut result = Vec::new();

    for device in devices.iter() {
        if let Ok(desc) = device.device_descriptor() {
            let name = format!(
                "VID:{:04x} PID:{:04x}",
                desc.vendor_id(),
                desc.product_id()
            );

            // Try to get serial number if available
            let serial = if let Ok(handle) = device.open() {
                handle.read_serial_number_string_ascii(&desc).ok()
            } else {
                None
            };

            result.push(UsbDeviceInfo {
                vendor_id: desc.vendor_id(),
                product_id: desc.product_id(),
                serial,
                name,
            });
        }
    }

    Ok(result)
}

/// Send vendor-specific bypass command to USB device
pub fn send_bypass_command(vendor_id: u16, product_id: u16) -> Result<bool, String> {
    let ctx = Context::new().map_err(|e| e.to_string())?;
    let devices = ctx.devices().map_err(|e| e.to_string())?;

    for device in devices.iter() {
        let desc = match device.device_descriptor() {
            Ok(d) => d,
            Err(_) => continue,
        };

        // Find matching device by VID/PID
        if desc.vendor_id() == vendor_id && desc.product_id() == product_id {
            println!(
                "[USB] Found device VID:{:04x} PID:{:04x}, attempting to open...",
                vendor_id, product_id
            );

            // Open device
            let handle = device.open().map_err(|e| {
                format!(
                    "Cannot open device: {}. Try: sudo chmod 666 /dev/bus/usb/...",
                    e
                )
            })?;

            // Detach kernel driver if active (Linux/macOS)
            #[cfg(any(target_os = "linux", target_os = "macos"))]
            {
                if handle.kernel_driver_active(0).unwrap_or(false) {
                    println!("[USB] Detaching kernel driver...");
                    handle
                        .detach_kernel_driver(0)
                        .map_err(|e| e.to_string())?;
                }
            }

            // Claim interface 0
            handle.claim_interface(0).map_err(|e| {
                format!("Failed to claim interface: {}", e)
            })?;

            println!("[USB] Interface claimed, sending bypass command...");

            // Build vendor-specific command based on VID
            let cmd = match vendor_id {
                0x04E8 => vec![0x00u8, 0x01, 0x00, 0x00], // Samsung
                0x0E8D => vec![0x01u8, 0x00, 0x00, 0x00], // MediaTek
                0x18D1 => vec![0x00u8, 0x00, 0x00, 0x00], // Google
                _ => vec![0x00u8, 0x01, 0x00, 0x00],      // Default
            };

            // Send vendor control transfer
            let result = handle.write_control(
                0x40, // bmRequestType: vendor, device, host-to-device
                0x01, // bRequest
                0x00, // wValue
                0x00, // wIndex
                &cmd,
                Duration::from_secs(5),
            );

            // Release interface
            handle.release_interface(0).ok();

            return match result {
                Ok(bytes_sent) => {
                    println!("[USB] ✓ Bypass command sent ({} bytes)", bytes_sent);
                    Ok(true)
                }
                Err(e) => {
                    println!("[USB] ✗ Control transfer failed: {}", e);
                    Err(format!("Control transfer failed: {}", e))
                }
            };
        }
    }

    Err("Device not found in USB bus".to_string())
}
