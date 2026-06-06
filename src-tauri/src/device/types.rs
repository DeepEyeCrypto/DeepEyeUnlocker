use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub enum DeviceConnectionState {
    Disconnected,
    Detecting,
    Connected,
    Unstable,
    Unauthorized,
    Error,
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub enum DeviceMode {
    Unknown,
    Normal,
    Recovery,
    Dfu,
    Fastboot,
    Adb,
    Sideload,
    Edl,
    Diagnostic,
    Purple,
    BootFiles,
    Unsupported,
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub enum DevicePlatform {
    Ios,
    Android,
    Qualcomm,
    Mtk,
    Unisoc,
    Unknown,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DeviceSnapshot {
    pub id: String,
    pub connection_state: DeviceConnectionState,
    pub platform: DevicePlatform,
    pub mode: DeviceMode,
    pub manufacturer: Option<String>,
    pub product_name: Option<String>,
    pub model: String,
    pub model_code: Option<String>,
    pub serial: String,
    pub os_version: Option<String>,
    pub chipset: Option<String>,
    pub is_supported: bool,
    pub support_reason: Option<String>,
    pub risk_flags: Vec<String>,
    pub capability_flags: Vec<String>,
    pub detected_at: u64,
    pub updated_at: u64,
}
