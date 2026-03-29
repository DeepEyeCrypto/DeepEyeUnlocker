use tauri::AppHandle;
use tauri_plugin_shell::ShellExt;

async fn run_bash(app: &AppHandle, s: &str) -> Result<String, String> {
    let output = app
        .shell()
        .command("bash")
        .args(["-c", s])
        .output()
        .await
        .map_err(|e| e.to_string())?;

    Ok(format!("{}\n{}", 
        String::from_utf8_lossy(&output.stdout),
        String::from_utf8_lossy(&output.stderr)))
}

/// Restore device using local IPSW
#[tauri::command]
pub async fn restore_local_ipsw(app: AppHandle, path: String) -> Result<String, String> {
    run_bash(&app, &format!("idevicerestore -l -p '{path}' 2>&1")).await
}

/// Restore to latest signed firmware
#[tauri::command]
pub async fn restore_latest(app: AppHandle) -> Result<String, String> {
    run_bash(&app, "idevicerestore -l 2>&1").await
}

/// Exit recovery mode
#[tauri::command]
pub async fn exit_recovery(app: AppHandle) -> Result<String, String> {
    run_bash(&app, "irecovery -n 2>&1").await
}

/// Get recovery mode info
#[tauri::command]
pub async fn get_recovery_info(app: AppHandle) -> Result<String, String> {
    run_bash(&app, "irecovery -v 2>&1").await
}
