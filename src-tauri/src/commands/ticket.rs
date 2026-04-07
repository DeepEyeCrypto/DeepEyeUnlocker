use serde::{Deserialize, Serialize};
use tauri::{AppHandle, Manager};
use tauri_plugin_shell::ShellExt;

#[derive(Debug, Serialize, Deserialize, Clone)]
#[allow(dead_code)]
pub enum TicketSource {
    Apple,
    Patched,
    Injected,
    Unknown,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct ActivationRecord {
    pub imei: Option<String>,
    pub meid: Option<String>,
    pub serial: Option<String>,
    pub unique_device_id: Option<String>,
    pub device_class: Option<String>,
    pub activation_state: String,
    pub ticket_present: bool,
    pub ticket_valid: bool,
    pub ticket_source: String, // String for ease of mapping from Python
    pub signed_fields: Vec<String>,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct ActivationRecordState {
    pub record: ActivationRecord,
    pub mismatch: bool,
    pub bypass_detected: bool,
}

fn python_path(app: &AppHandle) -> std::path::PathBuf {
    app.path().resource_dir().unwrap().join("python")
}

#[tauri::command]
pub async fn ios_parse_activation_record(
    app: AppHandle,
    backup_path: String,
) -> Result<ActivationRecord, String> {
    let output = app
        .shell()
        .command("python3")
        .args([
            python_path(&app)
                .join("ios_backup/cli.py")
                .to_str()
                .unwrap(),
            "activation-record",
            &backup_path,
        ])
        .output()
        .await
        .map_err(|e| e.to_string())?;

    let json_str = String::from_utf8_lossy(&output.stdout);
    serde_json::from_str(&json_str).map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn ios_activation_record_state(
    app: AppHandle,
    udid: String,
) -> Result<ActivationRecordState, String> {
    // For live device state
    let record = ios_parse_activation_record(app.clone(), udid).await?;
    Ok(ActivationRecordState {
        record,
        mismatch: false,
        bypass_detected: false,
    })
}

#[tauri::command]
pub async fn ios_scan_tickets(app: AppHandle, backup_path: String) -> Result<Vec<String>, String> {
    let output = app
        .shell()
        .command("python3")
        .args([
            python_path(&app)
                .join("ios_backup/cli.py")
                .to_str()
                .unwrap(),
            "scan-tickets",
            &backup_path,
        ])
        .output()
        .await
        .map_err(|e| e.to_string())?;

    let json_str = String::from_utf8_lossy(&output.stdout);
    serde_json::from_str(&json_str).map_err(|e| e.to_string())
}
