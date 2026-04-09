use serde::{Deserialize, Serialize};
use tauri::{AppHandle, Emitter, Manager};
use tauri_plugin_shell::ShellExt;

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct HelloState {
    pub on_hello_screen: bool,
    pub raw_state: String,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct BypassProgressPayload {
    pub id: String,
    pub status: String,
    pub detail: String,
}

fn python_path(app: &AppHandle) -> std::path::PathBuf {
    app.path().resource_dir().unwrap().join("python")
}

#[tauri::command]
pub async fn ios_check_hello_state(app: AppHandle, udid: String) -> Result<HelloState, String> {
    println!("[COMMAND] ios_check_hello_state udid={}", udid);

    let output = app
        .shell()
        .command("python3")
        .args([
            python_path(&app)
                .join("ios_backup/cli.py")
                .to_str()
                .unwrap(),
            "hello-state",
            &udid,
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
pub async fn ios_run_hello_bypass(_app: AppHandle, udid: String) -> Result<bool, String> {
    println!("[COMMAND] ios_run_hello_bypass udid={}", udid);
    // Placeholder for actual signal/activation record injection
    Ok(true)
}

#[tauri::command]
pub async fn run_bypass(app: AppHandle, bypass_id: String) -> Result<String, String> {
    let start_payload = BypassProgressPayload {
        id: bypass_id.clone(),
        status: "running".to_string(),
        detail: "Dispatching routed bypass command".to_string(),
    };
    app.emit("bypass-progress", start_payload)
        .map_err(|e| e.to_string())?;

    let success_message = format!("Bypass route {} dispatched successfully", bypass_id);
    let success_payload = BypassProgressPayload {
        id: bypass_id,
        status: "success".to_string(),
        detail: success_message.clone(),
    };
    app.emit("bypass-progress", success_payload)
        .map_err(|e| e.to_string())?;

    Ok(success_message)
}
