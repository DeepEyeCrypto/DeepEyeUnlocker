use std::process::Command;

fn run_bash(s: &str) -> Result<String, String> {
    let output = Command::new("bash").arg("-c").arg(s).output()
        .map_err(|e| e.to_string())?;
    Ok(format!("{}\n{}", String::from_utf8_lossy(&output.stdout),
        String::from_utf8_lossy(&output.stderr)))
}

/// Enter Purple Mode (for supported devices)
#[tauri::command]
pub fn enter_purple_mode() -> Result<String, String> {
    run_bash("gaster pwn && gaster boot purple 2>&1")
}

/// Read Serial Number from Purple Mode
#[tauri::command]
pub fn purple_read_sn() -> Result<String, String> {
    run_bash("ideviceserial -r SN 2>&1")
}

/// Write Serial Number in Purple Mode
#[tauri::command]
pub fn purple_write_sn(sn: String) -> Result<String, String> {
    run_bash(&format!("ideviceserial -w SN '{sn}' 2>&1"))
}

/// Read all SysCfg data
#[tauri::command]
pub fn purple_read_all() -> Result<String, String> {
    run_bash("ideviceserial -a 2>&1")
}
