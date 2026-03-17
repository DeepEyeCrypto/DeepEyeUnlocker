use std::process::Command;

fn run_bash(s: &str) -> Result<String, String> {
    let output = Command::new("bash").arg("-c").arg(s).output()
        .map_err(|e| e.to_string())?;
    Ok(format!("{}\n{}", String::from_utf8_lossy(&output.stdout),
        String::from_utf8_lossy(&output.stderr)))
}

/// Block OTA updates by installing a TVOS profile or patching plist
#[tauri::command]
pub fn toolbox_block_ota() -> Result<String, String> {
    run_bash("ideviceinstaller -i /path/to/tvos_profile.mobileconfig && idevicedebug -e com.apple.softwareupdated block 2>&1")
}

/// Force factory reset (Erase all content and settings)
#[tauri::command]
pub fn toolbox_factory_reset() -> Result<String, String> {
    run_bash("idevicediagnostics factory_reset 2>&1")
}

/// Get logs from device in real-time (first 100 lines)
#[tauri::command]
pub fn toolbox_get_logs() -> Result<String, String> {
    run_bash("idevicesyslog -n 100 2>&1")
}

/// Backup device via libimobiledevice
#[tauri::command]
pub fn toolbox_backup_device(path: String) -> Result<String, String> {
    run_bash(&format!("idevicebackup2 backup '{path}' 2>&1"))
}
