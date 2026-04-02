use serde::{Serialize, Deserialize};
use tauri::AppHandle;
use tauri_plugin_shell::ShellExt;
use crate::commands::ios_backup::python_script_path;

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct DeviceMode {
    pub mode: String,
}

#[tauri::command]
pub async fn ios_poll_orchestrator(app: AppHandle) -> Result<DeviceMode, String> {
    // [INFERRED] Android runtime must not attempt to spawn the desktop Apple Python toolchain.
    if cfg!(target_os = "android") {
        return Err("ios_poll_orchestrator is unavailable on Android runtime".to_string());
    }

    let orchestrator_script = python_script_path(&app, "ios_backup/orchestrator.py")?;
    let output = app.shell()
        .command("python3")
        .args([
            orchestrator_script
                .to_str()
                .ok_or_else(|| "invalid orchestrator script path".to_string())?,
            "poll"
        ])
        .output()
        .await
        .map_err(|e| e.to_string())?;

    if !output.status.success() {
        return Err(String::from_utf8_lossy(&output.stderr).to_string());
    }

    let json_str = String::from_utf8_lossy(&output.stdout);
    serde_json::from_str(&json_str).map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn ios_inject_surgical_patch(_app: AppHandle, patch_id: String) -> Result<bool, String> {
    println!("[ORCHESTRATOR] Injecting surgical patch: {}", patch_id);
    // Simulation: would write to mounted ramdisk
    tokio::time::sleep(tokio::time::Duration::from_millis(1500)).await;
    Ok(true)
}
