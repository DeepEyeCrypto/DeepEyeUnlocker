use std::process::Command;

fn run_bash(s: &str) -> Result<String, String> {
    let output = Command::new("bash").arg("-c").arg(s).output()
        .map_err(|e| e.to_string())?;
    Ok(format!("{}\n{}", String::from_utf8_lossy(&output.stdout),
        String::from_utf8_lossy(&output.stderr)))
}

/// Restore device using local IPSW
#[tauri::command]
pub fn restore_local_ipsw(path: String) -> Result<String, String> {
    run_bash(&format!("idevicerestore -l -p '{path}' 2>&1"))
}

/// Restore to latest signed firmware
#[tauri::command]
pub fn restore_latest() -> Result<String, String> {
    run_bash("idevicerestore -l 2>&1")
}

/// Exit recovery mode
#[tauri::command]
pub fn exit_recovery() -> Result<String, String> {
    run_bash("irecovery -n 2>&1")
}

/// Get recovery mode info
#[tauri::command]
pub fn get_recovery_info() -> Result<String, String> {
    run_bash("irecovery -v 2>&1")
}
