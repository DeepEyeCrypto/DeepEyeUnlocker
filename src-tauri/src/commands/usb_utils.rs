use rusb::{Device, DeviceHandle, GlobalContext};
use std::time::Duration;

pub const EP_OUT: u8 = 0x01;
pub const EP_IN: u8 = 0x81;
pub const TIMEOUT: Duration = Duration::from_millis(2000);

#[derive(Debug, Clone, Copy, Default)]
pub struct ClaimOptions {
    pub config_value: Option<u8>,
    pub interface_number: u8,
    pub alternate_setting: Option<u8>,
}

pub fn open_and_claim(
    device: &Device<GlobalContext>,
) -> Result<DeviceHandle<GlobalContext>, rusb::Error> {
    open_and_claim_with_options(device, ClaimOptions::default())
}

pub fn open_and_claim_with_options(
    device: &Device<GlobalContext>,
    options: ClaimOptions,
) -> Result<DeviceHandle<GlobalContext>, rusb::Error> {
    let handle = device.open()?;

    if let Some(config_value) = options.config_value {
        let needs_configuration = match handle.active_configuration() {
            Ok(active_configuration) => active_configuration != config_value,
            Err(_) => true,
        };

        if needs_configuration {
            match handle.set_active_configuration(config_value) {
                Ok(()) => {}
                Err(rusb::Error::Busy | rusb::Error::NotSupported) => {}
                Err(error) => return Err(error),
            }
        }
    }

    #[cfg(any(target_os = "macos", target_os = "linux", target_os = "android"))]
    {
        match handle.kernel_driver_active(options.interface_number) {
            Ok(true) => match handle.detach_kernel_driver(options.interface_number) {
                Ok(()) | Err(rusb::Error::NotFound | rusb::Error::NotSupported) => {}
                Err(error) => return Err(error),
            },
            Ok(false) | Err(rusb::Error::NotSupported) => {}
            Err(error) => eprintln!(
                "[usb] kernel_driver_active failed for interface {}: {error}",
                options.interface_number
            ),
        }
    }

    handle.claim_interface(options.interface_number)?;

    if let Some(alternate_setting) = options.alternate_setting {
        if alternate_setting != 0 {
            handle.set_alternate_setting(options.interface_number, alternate_setting)?;
        }
    }

    Ok(handle)
}

pub fn debug_list_usb_devices() {
    match rusb::devices() {
        Ok(list) => {
            for device in list.iter() {
                if let Ok(desc) = device.device_descriptor() {
                    println!(
                        "[usb_debug] Bus {:03} Device {:03} ID {:04x}:{:04x}",
                        device.bus_number(),
                        device.address(),
                        desc.vendor_id(),
                        desc.product_id()
                    );
                }
            }
        }
        Err(error) => eprintln!("[usb_debug] Cannot list devices: {error}"),
    }
}

#[cfg(target_os = "windows")]
pub fn check_winusb_installed(vid: u16, pid: u16) -> bool {
    rusb::open_device_with_vid_pid(vid, pid).is_some()
}

#[cfg(not(target_os = "windows"))]
pub fn check_winusb_installed(_vid: u16, _pid: u16) -> bool {
    true
}

#[derive(Debug, serde::Serialize, Clone, Copy)]
pub struct UsbDeviceEntry {
    pub bus: u8,
    pub address: u8,
    pub vid: u16,
    pub pid: u16,
}

#[tauri::command]
pub fn usb_debug_list_devices() -> Vec<UsbDeviceEntry> {
    let mut result = Vec::new();

    if let Ok(list) = rusb::devices() {
        for device in list.iter() {
            if let Ok(desc) = device.device_descriptor() {
                result.push(UsbDeviceEntry {
                    bus: device.bus_number(),
                    address: device.address(),
                    vid: desc.vendor_id(),
                    pid: desc.product_id(),
                });
            }
        }
    }

    result
}
