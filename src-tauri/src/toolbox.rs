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

/// Block OTA updates by installing a TVOS profile or patching plist
#[tauri::command]
pub async fn toolbox_block_ota(app: AppHandle) -> Result<String, String> {
    run_bash(&app, "ideviceinstaller -i /path/to/tvos_profile.mobileconfig && idevicedebug -e com.apple.softwareupdated block 2>&1").await
}

/// Force factory reset (Erase all content and settings)
#[tauri::command]
pub async fn toolbox_factory_reset(app: AppHandle) -> Result<String, String> {
    run_bash(&app, "idevicediagnostics factory_reset 2>&1").await
}

/// Get logs from device in real-time (first 100 lines)
#[tauri::command]
pub async fn toolbox_get_logs(app: AppHandle) -> Result<String, String> {
    run_bash(&app, "idevicesyslog -n 100 2>&1").await
}

/// Backup device via libimobiledevice
#[tauri::command]
pub async fn toolbox_backup_device(app: AppHandle, path: String) -> Result<String, String> {
    run_bash(&app, &format!("idevicebackup2 backup '{path}' 2>&1")).await
}
