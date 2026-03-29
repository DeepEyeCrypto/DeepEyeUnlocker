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

/// Enter Purple Mode (for supported devices)
#[tauri::command]
pub async fn enter_purple_mode(app: AppHandle) -> Result<String, String> {
    run_bash(&app, "gaster pwn && gaster boot purple 2>&1").await
}

/// Read Serial Number from Purple Mode
#[tauri::command]
pub async fn purple_read_sn(app: AppHandle) -> Result<String, String> {
    run_bash(&app, "ideviceserial -r SN 2>&1").await
}

/// Write Serial Number in Purple Mode
#[tauri::command]
pub async fn purple_write_sn(app: AppHandle, sn: String) -> Result<String, String> {
    run_bash(&app, &format!("ideviceserial -w SN '{sn}' 2>&1")).await
}

/// Read all SysCfg data
#[tauri::command]
pub async fn purple_read_all(app: AppHandle) -> Result<String, String> {
    run_bash(&app, "ideviceserial -a 2>&1").await
}
