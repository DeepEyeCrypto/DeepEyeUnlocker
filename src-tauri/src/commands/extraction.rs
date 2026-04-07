use crate::commands::ios_backup::python_module_root;
use serde::{Deserialize, Serialize};
use tauri::AppHandle;
use tauri_plugin_shell::ShellExt;

// ──────────────────────────────────────────────────────────────
// MODULE 15: MASS ARTIFACT EXTRACTION (DeepExtraction v2)
// ──────────────────────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize)]
pub struct ExtractionResult {
    pub name: String,
    pub remote: String,
    pub success: bool,
    pub local: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct MassExtractionReport {
    pub success: bool,
    pub results: Vec<ExtractionResult>,
    pub message: String,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct MountResult {
    pub success: bool,
    pub message: String,
}

#[tauri::command]
pub async fn ios_mount_ramdisk(app: AppHandle) -> Result<MountResult, String> {
    let python_root = python_module_root(&app)?;
    let output = app
        .shell()
        .command("python3")
        .args(["-m", "ios_backup.cli", "mount-ramdisk"])
        .env("PYTHONPATH", &python_root)
        .output()
        .await
        .map_err(|e| format!("Failed to start mount ramdisk: {}", e))?;

    if !output.status.success() {
        return Err(String::from_utf8_lossy(&output.stderr).to_string());
    }

    let result: MountResult = serde_json::from_slice(&output.stdout)
        .map_err(|e| format!("Failed to parse mount result: {}", e))?;

    Ok(result)
}

#[tauri::command]
pub async fn ios_mass_extract(
    app: AppHandle,
    save_path: String,
) -> Result<MassExtractionReport, String> {
    let python_root = python_module_root(&app)?;
    let output = app
        .shell()
        .command("python3")
        .args(["-m", "ios_backup.cli", "mass-extract", &save_path])
        .env("PYTHONPATH", &python_root)
        .output()
        .await
        .map_err(|e| format!("Failed to start mass extraction: {}", e))?;

    if !output.status.success() {
        return Err(String::from_utf8_lossy(&output.stderr).to_string());
    }

    let result: MassExtractionReport = serde_json::from_slice(&output.stdout)
        .map_err(|e| format!("Failed to parse extraction report: {}", e))?;

    Ok(result)
}
