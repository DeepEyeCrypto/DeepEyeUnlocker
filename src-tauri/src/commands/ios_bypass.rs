use tauri::AppHandle;
use tauri_plugin_shell::ShellExt;
use crate::commands::checkm8::{CHECKM8_CHIPS, python_module_root};

#[tauri::command]
pub async fn ios_bypass_full(
    app:        AppHandle,
    ecid:       String,
    chip_id:    u16,
    ios_version:String,
    session_id: String,
) -> Result<serde_json::Value, String> {

    // A7-A11: checkm8 path
    if CHECKM8_CHIPS.contains(&chip_id) {
        return run_checkm8_bypass(&app, chip_id, &session_id).await;
    }

    // A12+: server bypass
    return run_server_bypass(&app, &ecid, &ios_version, &session_id).await;
}

async fn run_checkm8_bypass(
    app:        &AppHandle,
    chip_id:    u16,
    session_id: &str,
) -> Result<serde_json::Value, String> {

    let python_root = python_module_root(app);

    let output = app
        .shell()
        .command("python3")
        .args([
            &format!("{}/ios_exploit/checkm8_runner.py", python_root),
            "--chip-id",    &format!("0x{:04X}", chip_id),
            "--operation",  "activation_bypass",
            "--session-id", session_id,
        ])
        .env("PYTHONPATH", &python_root)
        .output()
        .await
        .map_err(|e| e.to_string())?;

    // Parse JSON events from stdout
    let stdout = String::from_utf8_lossy(&output.stdout);
    let last_event: serde_json::Value = stdout
        .lines()
        .filter_map(|l| serde_json::from_str(l).ok())
        .next_back()
        .unwrap_or(serde_json::json!({"event": "unknown"}));

    Ok(last_event)
}

async fn run_server_bypass(
    app:        &AppHandle,
    ecid:       &str,
    ios_version:&str,
    session_id: &str,
) -> Result<serde_json::Value, String> {

    let python_root = python_module_root(app);

    let output = app
        .shell()
        .command("python3")
        .args([
            &format!("{}/ios_bypass/server_bypass.py", python_root),
            "--ecid",        ecid,
            "--ios-version", ios_version,
            "--session-id",  session_id,
        ])
        .env("PYTHONPATH", &python_root)
        .output()
        .await
        .map_err(|e| e.to_string())?;

    let stdout = String::from_utf8_lossy(&output.stdout).to_string();
    serde_json::from_str(&stdout).map_err(|e| e.to_string())
}
