use crate::device::coordinator::DeviceProbeCoordinator;
use crate::device::types::DeviceSnapshot;
use tauri::{AppHandle, State};

#[tauri::command]
pub fn get_current_device_snapshot(
    coordinator: State<'_, DeviceProbeCoordinator>,
) -> Option<DeviceSnapshot> {
    coordinator.get_snapshot()
}

#[tauri::command]
pub async fn refresh_device_detection(
    app: AppHandle,
    coordinator: State<'_, DeviceProbeCoordinator>,
) -> Result<Option<DeviceSnapshot>, String> {
    // Run an adb-check, apple-check, etc. if needed, or simply trigger an on-demand refresh of connected devices
    // For now, let's call the existing get_connected_devices logic to probe USBs and populate the coordinator
    match crate::commands::connected_devices::get_connected_devices(app.clone()).await {
        Ok(devices) => {
            // Transform the list of ConnectedDevice from connected_devices.rs into DetectedUsbDevice
            // and feed into the coordinator
            let mut detected = Vec::new();
            for dev in devices {
                let vid = if dev.source == "apple" {
                    0x05AC
                } else if dev.source == "mtk" {
                    0x0E8D
                } else if dev.source == "edl" {
                    0x05C6
                } else {
                    0x04E8
                };
                let mode = match dev.mode.as_str() {
                    "ADB" => crate::commands::usb_detector::DeviceMode::Adb,
                    "EDL 9008" => crate::commands::usb_detector::DeviceMode::Edl,
                    "BROM/DA" => crate::commands::usb_detector::DeviceMode::MtkBrom,
                    "DFU" => crate::commands::usb_detector::DeviceMode::AppleDfu,
                    "Recovery" => crate::commands::usb_detector::DeviceMode::AppleRecovery,
                    _ => crate::commands::usb_detector::DeviceMode::AppleNormal,
                };
                detected.push(crate::commands::usb_detector::DetectedUsbDevice {
                    vid,
                    pid: 0,
                    manufacturer: Some(dev.source.clone()),
                    product: Some(dev.model.clone()),
                    serial: Some(dev.serial.clone()),
                    mode,
                });
            }
            coordinator.handle_usb_change(detected, &app);
            Ok(coordinator.get_snapshot())
        }
        Err(e) => Err(e),
    }
}
