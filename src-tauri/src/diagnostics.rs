use std::process::Command;

fn run_bash(s: &str) -> Result<String, String> {
    let output = Command::new("bash").arg("-c").arg(s).output()
        .map_err(|e| e.to_string())?;
    Ok(format!("{}\n{}", String::from_utf8_lossy(&output.stdout),
        String::from_utf8_lossy(&output.stderr)))
}

/// Run diagnostics command on device
#[tauri::command]
pub fn run_diagnostics(cmd: String) -> Result<String, String> {
    // Supported: iorep, gas, battery, display, sensors
    run_bash(&format!("idevicediagnostics {cmd} 2>&1"))
}

/// Get detailed battery health
#[tauri::command]
pub fn get_battery_stats() -> Result<String, String> {
    run_bash("idevicediagnostics iorep -q com.apple.ioreport.BatteryUsage 2>&1")
}

/// Get device thermal state
#[tauri::command]
pub fn get_thermal_state() -> Result<String, String> {
    run_bash("idevicediagnostics iorep -q com.apple.ioreport.ThermalState 2>&1")
}

/// Shutdown device diagnostic
#[tauri::command]
pub fn device_shutdown() -> Result<String, String> {
    run_bash("idevicediagnostics shutdown 2>&1")
}

/// Restart device diagnostic
#[tauri::command]
pub fn device_restart() -> Result<String, String> {
    run_bash("idevicediagnostics restart 2>&1")
}

/// Sleep device diagnostic
#[tauri::command]
pub fn device_sleep() -> Result<String, String> {
    run_bash("idevicediagnostics sleep 2>&1")
}
