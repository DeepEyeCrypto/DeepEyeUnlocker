use crate::commands::connected_devices;
use crate::commands::device_db;
use serde::Serialize;
use tauri::AppHandle;

#[derive(Debug, Serialize, Clone)]
pub struct DeviceAuditReport {
    pub timestamp: String,
    pub device: Option<connected_devices::ConnectedDevice>,
    pub db_entry: Option<device_db::DeviceEntry>,
    pub logs_summary: Vec<String>,
    pub security_score: u8,
}

#[tauri::command]
pub async fn reporter_generate_audit(app: AppHandle) -> Result<DeviceAuditReport, String> {
    let devices = connected_devices::get_connected_devices(app.clone()).await?;
    let primary = devices.first().cloned();

    // Try to match the connected device against the device database
    let db_entry = if let Some(ref dev) = primary {
        device_db::db_lookup_model(dev.model.clone()).ok().flatten()
    } else {
        None
    };

    // Pull real operation history from the database
    let history_entries = crate::db::history::get_history()
        .await
        .unwrap_or_default();

    let logs_summary: Vec<String> = history_entries
        .iter()
        .take(10)
        .map(|entry| {
            format!(
                "[{}] {} · {} — {}",
                entry.timestamp, entry.device_name, entry.tool_name, entry.result
            )
        })
        .collect();

    // Calculate security score based on actual device state
    let security_score = calculate_security_score(&primary, &db_entry, history_entries.len());

    Ok(DeviceAuditReport {
        timestamp: chrono::Local::now().to_rfc3339(),
        device: primary,
        db_entry,
        logs_summary,
        security_score,
    })
}

fn calculate_security_score(
    device: &Option<connected_devices::ConnectedDevice>,
    db_entry: &Option<device_db::DeviceEntry>,
    history_count: usize,
) -> u8 {
    let mut score: u8 = 50;

    // +15 if a device is connected and recognized
    if device.is_some() {
        score = score.saturating_add(15);
    }

    // +15 if matched in device database
    if db_entry.is_some() {
        score = score.saturating_add(15);
    }

    // +10 if we have operation history (tool is being actively used)
    if history_count > 0 {
        score = score.saturating_add(10);
    }

    // +10 for substantial history
    if history_count >= 5 {
        score = score.saturating_add(10);
    }

    score.min(100)
}
