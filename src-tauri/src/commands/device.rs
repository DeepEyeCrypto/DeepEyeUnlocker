use crate::device::{
    detector::{has_device_mode, DeviceMode},
    protocol_router::{scan_all_devices, DeviceConnectionStatus, ProtocolRouter, ProtocolType},
};
use serde::Serialize;

/// Unified device scan result
#[derive(Debug, Serialize)]
pub struct DeviceScanResult {
    pub devices: Vec<DeviceConnectionStatus>,
    pub count: usize,
    pub has_supported: bool,
}

/// Scan all USB devices and return detected devices
#[tauri::command]
pub async fn device_scan_all() -> Result<DeviceScanResult, String> {
    log::info!("[Device] Scanning all USB devices...");

    let devices = scan_all_devices()?;
    let count = devices.len();
    let has_supported = devices.iter().any(|d| d.connected);

    log::info!(
        "[Device] Found {} device(s), {} supported",
        count,
        has_supported
    );

    Ok(DeviceScanResult {
        devices,
        count,
        has_supported,
    })
}

/// Auto-detect and connect to the best available device
#[tauri::command]
pub async fn device_auto_connect() -> Result<DeviceConnectionStatus, String> {
    log::info!("[Device] Auto-detecting device...");

    let device = ProtocolRouter::auto_detect()?;
    let protocol = ProtocolRouter::mode_to_protocol(&device.mode);
    let message = ProtocolRouter::device_description(&device);
    let chipset = ProtocolRouter::identify_chipset(&device);

    log::info!("[Device] Connected: {} (chipset: {:?})", message, chipset);

    Ok(DeviceConnectionStatus {
        connected: true,
        device: Some(device),
        protocol,
        message,
    })
}

/// Check if specific device mode is available
#[tauri::command]
pub async fn device_check_mode(mode: String) -> Result<bool, String> {
    let target_mode = match mode.to_lowercase().as_str() {
        "brom" => DeviceMode::Brom,
        "preloader" => DeviceMode::PreLoader,
        "edl" => DeviceMode::Edl,
        "fastboot" => DeviceMode::Fastboot,
        "adb" => DeviceMode::Adb,
        "samsung" | "odin" => DeviceMode::SamsungOdin,
        "unisoc" | "fdl" => DeviceMode::UnisocFdl,
        "recovery" => DeviceMode::Recovery,
        _ => return Err(format!("Unknown device mode: {}", mode)),
    };

    has_device_mode(target_mode)
}

/// Get protocol type name for display
#[tauri::command]
pub async fn device_get_protocol_name(protocol: String) -> Result<String, String> {
    let proto_type = match protocol.to_lowercase().as_str() {
        "mtkbrom" => ProtocolType::MtkBrom,
        "mtkpreloader" => ProtocolType::MtkPreloader,
        "edl" | "qualcommedl" => ProtocolType::QualcommEdl,
        "fastboot" => ProtocolType::Fastboot,
        "samsungodin" => ProtocolType::SamsungOdin,
        "unisocfdl" => ProtocolType::UnisocFdl,
        "adb" => ProtocolType::Adb,
        "mtp" => ProtocolType::Mtp,
        "recovery" => ProtocolType::Recovery,
        _ => ProtocolType::Unknown,
    };

    Ok(format!("{:?}", proto_type))
}

/// Fastboot: Detect if fastboot device is connected
#[tauri::command]
pub async fn fastboot_detect() -> Result<bool, String> {
    crate::commands::fastboot::fastboot_device_connected()
}

/// Fastboot: Get device information
#[tauri::command]
pub async fn fastboot_get_info() -> Result<crate::commands::fastboot::FastbootDeviceInfo, String> {
    let session = crate::commands::fastboot::FastbootSession::open()?;
    session.get_variables()
}

/// Fastboot: Flash partition from file
#[tauri::command]
pub async fn fastboot_flash_partition(partition: String, file_path: String) -> Result<(), String> {
    let data = std::fs::read(&file_path)
        .map_err(|e| format!("Failed to read file '{}': {}", file_path, e))?;

    let session = crate::commands::fastboot::FastbootSession::open()?;
    session.flash_partition(&partition, &data)
}

/// Fastboot: Reboot device
#[tauri::command]
pub async fn fastboot_reboot() -> Result<(), String> {
    let session = crate::commands::fastboot::FastbootSession::open()?;
    session.reboot()
}

/// Fastboot: Reboot to bootloader
#[tauri::command]
pub async fn fastboot_reboot_bootloader() -> Result<(), String> {
    let session = crate::commands::fastboot::FastbootSession::open()?;
    session.reboot_bootloader()
}

/// Fastboot: Reboot to recovery
#[tauri::command]
pub async fn fastboot_reboot_recovery() -> Result<(), String> {
    let session = crate::commands::fastboot::FastbootSession::open()?;
    session.reboot_recovery()
}

/// Fastboot: Lock bootloader
#[tauri::command]
pub async fn fastboot_lock_bootloader() -> Result<(), String> {
    let session = crate::commands::fastboot::FastbootSession::open()?;
    session.lock_bootloader()
}
