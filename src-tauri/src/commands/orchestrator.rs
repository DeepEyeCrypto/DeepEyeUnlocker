use crate::commands::ios_backup::python_script_path;
use serde::{Deserialize, Serialize};
use tauri::AppHandle;
use tauri_plugin_shell::ShellExt;

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
    let output = app
        .shell()
        .command("python3")
        .args([
            orchestrator_script
                .to_str()
                .ok_or_else(|| "invalid orchestrator script path".to_string())?,
            "poll",
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
pub async fn ios_inject_surgical_patch(app: AppHandle, patch_id: String) -> Result<bool, String> {
    println!("[ORCHESTRATOR] Injecting surgical patch: {}", patch_id);

    // Determine the patch file from bundled resources
    let patch_script = python_script_path(&app, "ios_backup/cli.py")?;

    let output = app
        .shell()
        .command("python3")
        .args([
            patch_script
                .to_str()
                .ok_or_else(|| "invalid cli script path".to_string())?,
            "inject-patch",
            &patch_id,
        ])
        .output()
        .await
        .map_err(|e| format!("Failed to run patch injection: {e}"))?;

    if !output.status.success() {
        let stderr = String::from_utf8_lossy(&output.stderr).to_string();
        return Err(format!("Patch injection failed: {stderr}"));
    }

    let stdout = String::from_utf8_lossy(&output.stdout);
    // Python script returns JSON with {"success": true/false}
    let val: serde_json::Value =
        serde_json::from_str(&stdout).map_err(|e| format!("Parse error: {e}"))?;

    Ok(val["success"].as_bool().unwrap_or(false))
}
