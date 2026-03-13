use serde::{Serialize, Deserialize};
use tauri::{AppHandle, Manager};
use tauri_plugin_shell::ShellExt;

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct AppleIdState {
    pub fmi_on: bool,
    pub apple_id_bound: bool,
    pub ios_version: String,
    pub model: String,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct RemovalResult {
    pub success: bool,
    pub message: Option<String>,
    pub error: Option<String>,
}

fn python_path(app: &AppHandle) -> std::path::PathBuf {
    app.path().resource_dir().unwrap().join("python")
}

#[tauri::command]
pub async fn ios_apple_id_state(app: AppHandle, udid: String) -> Result<AppleIdState, String> {
    println!("[COMMAND] ios_apple_id_state udid={}", udid);
    
    let output = app.shell()
        .command("python3")
        .args([
            python_path(&app).join("ios_backup/cli.py").to_str().unwrap(),
            "apple-id-state",
            &udid
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
pub async fn ios_remove_apple_id(app: AppHandle, udid: String) -> Result<RemovalResult, String> {
    println!("[COMMAND] ios_remove_apple_id udid={}", udid);
    
    let output = app.shell()
        .command("python3")
        .args([
            python_path(&app).join("ios_backup/cli.py").to_str().unwrap(),
            "remove-apple-id",
            &udid
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
pub async fn ios_fmi_state(app: AppHandle, udid: String) -> Result<bool, String> {
    println!("[COMMAND] ios_fmi_state udid={}", udid);
    let state = ios_apple_id_state(app, udid).await?;
    Ok(state.fmi_on)
}
