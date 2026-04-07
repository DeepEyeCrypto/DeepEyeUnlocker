use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use tauri::{AppHandle, Manager};
use tauri_plugin_shell::ShellExt;

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct VaultRequest {
    pub dir: String,
    pub meta: HashMap<String, serde_json::Value>,
    pub files: Vec<String>,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct VaultResult {
    pub success: bool,
    pub vault_path: Option<String>,
    pub error: Option<String>,
}

fn python_path(app: &AppHandle) -> std::path::PathBuf {
    app.path().resource_dir().unwrap().join("python")
}

#[tauri::command]
pub async fn ios_create_deepvault(
    app: AppHandle,
    request: VaultRequest,
) -> Result<VaultResult, String> {
    println!("[COMMAND] ios_create_deepvault dir={}", request.dir);

    let payload = serde_json::to_string(&request).map_err(|e| e.to_string())?;

    let output = app
        .shell()
        .command("python3")
        .args([
            python_path(&app)
                .join("ios_backup/cli.py")
                .to_str()
                .unwrap(),
            "create-vault",
            &payload,
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
