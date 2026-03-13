use serde::{Serialize, Deserialize};
use tauri::{AppHandle, Manager};
use tauri_plugin_shell::ShellExt;

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct DeviceIdentity {
    pub udid: String,
    pub ecid: Option<String>,
    pub imei: Option<String>,
    pub imei2: Option<String>,
    pub meid: Option<String>,
    pub serial: Option<String>,
    pub board_id: Option<String>,
    pub chip_id: Option<String>,
    pub is_cdma: bool,
    pub imei_valid: bool,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct ImeiState {
    pub imei: String,
    pub valid: bool,
    pub cdma_meid: bool,
    pub gsm_signal_eligible: bool,
    pub icloud_lock_inferred: Option<bool>,
}

fn python_path(app: &AppHandle) -> std::path::PathBuf {
    app.path().resource_dir().unwrap().join("python")
}

#[tauri::command]
pub async fn ios_device_identity(app: AppHandle, udid: String) -> Result<DeviceIdentity, String> {
    let output = app.shell()
        .command("python3")
        .args([
            python_path(&app).join("ios_backup/cli.py").to_str().unwrap(),
            "device-identity",
            &udid
        ])
        .output()
        .await
        .map_err(|e| e.to_string())?;

    let json_str = String::from_utf8_lossy(&output.stdout);
    serde_json::from_str(&json_str).map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn ios_imei_state(app: AppHandle, udid: String) -> Result<ImeiState, String> {
    // Derived from matrix for simplicity in this bridge
    let output = app.shell()
        .command("python3")
        .args([
            python_path(&app).join("ios_backup/cli.py").to_str().unwrap(),
            "activation-matrix",
            &udid
        ])
        .output()
        .await
        .map_err(|e| e.to_string())?;

    let json_str = String::from_utf8_lossy(&output.stdout);
    let matrix: serde_json::Value = serde_json::from_str(&json_str).map_err(|e| e.to_string())?;
    
    Ok(ImeiState {
        imei: matrix["imei_present"].as_str().unwrap_or("N/A").to_string(),
        valid: matrix["imei_valid"].as_bool().unwrap_or(false),
        cdma_meid: matrix["is_meid_cdma"].as_bool().unwrap_or(false),
        gsm_signal_eligible: matrix["eligible_types"].as_array().map_or(false, |a| a.iter().any(|v| v == "GsmSignal")),
        icloud_lock_inferred: None,
    })
}
