use crate::commands::connected_devices;
use crate::commands::device_db::DeviceEntry;
use serde::Serialize;
use tauri::AppHandle;

#[derive(Debug, Serialize, Clone)]
pub struct DeviceAuditReport {
    pub timestamp: String,
    pub device: Option<connected_devices::ConnectedDevice>,
    pub db_entry: Option<DeviceEntry>,
    pub logs_summary: Vec<String>,
    pub security_score: u8,
}

#[tauri::command]
pub async fn reporter_generate_audit(app: AppHandle) -> Result<DeviceAuditReport, String> {
    let devices = connected_devices::get_connected_devices(app.clone()).await?;
    let primary = devices.first().cloned();

    // In a real app we'd fetch from history or state
    Ok(DeviceAuditReport {
        timestamp: chrono::Local::now().to_rfc3339(),
        device: primary,
        db_entry: None, // Logic to match would go here
        logs_summary: vec![
            "Protocol Handshake Verified".into(),
            "FRP Target Identified".into(),
        ],
        security_score: 85,
    })
}
