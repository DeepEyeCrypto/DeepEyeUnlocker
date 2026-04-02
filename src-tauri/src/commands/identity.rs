use serde::{Serialize, Deserialize};
use tauri::AppHandle;
use tauri_plugin_shell::ShellExt;
use crate::commands::ios_backup::python_script_path;

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

#[tauri::command]
pub async fn ios_device_identity(app: AppHandle, udid: String) -> Result<DeviceIdentity, String> {
    // [INFERRED] Android runtime must not invoke the desktop-only Apple Python bridge during startup.
    if cfg!(target_os = "android") {
        return Err("ios_device_identity is unavailable on Android runtime".to_string());
    }

    let cli_script = python_script_path(&app, "ios_backup/cli.py")?;
    let output = app.shell()
        .command("python3")
        .args([
            cli_script
                .to_str()
                .ok_or_else(|| "invalid ios_backup cli path".to_string())?,
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
    if cfg!(target_os = "android") {
        return Err("ios_imei_state is unavailable on Android runtime".to_string());
    }

    let cli_script = python_script_path(&app, "ios_backup/cli.py")?;
    let output = app.shell()
        .command("python3")
        .args([
            cli_script
                .to_str()
                .ok_or_else(|| "invalid ios_backup cli path".to_string())?,
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
        gsm_signal_eligible: matrix["eligible_types"].as_array().is_some_and(|a| a.iter().any(|v| v == "GsmSignal")),
        icloud_lock_inferred: None,
    })
}
