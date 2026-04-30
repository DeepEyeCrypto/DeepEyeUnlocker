use serde::{Deserialize, Serialize};
use std::process::Command;
use tauri::{command, AppHandle, Manager};

#[derive(Debug, Serialize, Deserialize)]
pub struct SwiftDevice {
    pub udid: String,
    pub model: String,
    pub ios: String,
    pub ecid: String,
    pub mode: String,
    pub imei: Option<String>,
}

fn get_core_path(app: &AppHandle) -> Result<std::path::PathBuf, String> {
    app.path()
        .resource_dir()
        .map(|p| p.join("resources").join("deepeye-core"))
        .map_err(|e| format!("Resource dir error: {e}"))
}

#[command]
pub async fn ios_detect_device(app: AppHandle, session_id: String) -> Result<SwiftDevice, String> {
    let bin = get_core_path(&app)?;

    let output = Command::new(bin)
        .arg("detect")
        .arg(session_id)
        .output()
        .map_err(|e| format!("Swift core failed: {e}"))?;

    if !output.status.success() {
        let err = String::from_utf8_lossy(&output.stderr).to_string();
        return Err(if err.is_empty() {
            "Unknown error in Swift core".to_string()
        } else {
            err
        });
    }

    serde_json::from_slice(&output.stdout).map_err(|e| format!("JSON parse error: {e}"))
}

#[command]
pub async fn run_hello_bypass(app: AppHandle, session_id: String) -> Result<String, String> {
    let bin = get_core_path(&app)?;

    let status = Command::new(bin)
        .arg("hello-bypass")
        .arg(session_id)
        .status()
        .map_err(|e| format!("Failed to start hello-bypass: {e}"))?;

    if status.success() {
        Ok("Hello bypass completed successfully".to_string())
    } else {
        Err("Hello bypass failed (see logs)".to_string())
    }
}

#[command]
#[allow(clippy::too_many_arguments)]
pub async fn run_full_signal_bypass(
    app: AppHandle,
    ecid: String,
    imei: String,
    imei2: String,
    serial: String,
    ios: String,
    model: String,
    session_id: String,
) -> Result<String, String> {
    let bin = get_core_path(&app)?;

    let status = Command::new(bin)
        .arg("full-signal")
        .arg(ecid)
        .arg(imei)
        .arg(imei2)
        .arg(serial)
        .arg(ios)
        .arg(model)
        .arg(session_id)
        .status()
        .map_err(|e| format!("Full signal failed: {e}"))?;

    if status.success() {
        Ok("Full signal bypass successful".to_string())
    } else {
        Err("Full signal bypass failed".to_string())
    }
}

#[command]
pub async fn run_fake_erase(
    app: AppHandle,
    ecid: String,
    imei: String,
    serial: String,
    ios: String,
    model: String,
    session_id: String,
) -> Result<String, String> {
    let bin = get_core_path(&app)?;

    let status = Command::new(bin)
        .arg("fake-erase")
        .arg(ecid)
        .arg(imei)
        .arg(serial)
        .arg(ios)
        .arg(model)
        .arg(session_id)
        .status()
        .map_err(|e| format!("Fake erase failed: {e}"))?;

    if status.success() {
        Ok("Fake erase successful".to_string())
    } else {
        Err("Fake erase failed".to_string())
    }
}
