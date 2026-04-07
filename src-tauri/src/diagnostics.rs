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

    Ok(format!(
        "{}\n{}",
        String::from_utf8_lossy(&output.stdout),
        String::from_utf8_lossy(&output.stderr)
    ))
}

/// Run diagnostics command on device
#[tauri::command]
pub async fn run_diagnostics(app: AppHandle, cmd: String) -> Result<String, String> {
    // Supported: iorep, gas, battery, display, sensors
    run_bash(&app, &format!("idevicediagnostics {cmd} 2>&1")).await
}

/// Get detailed battery health
#[tauri::command]
pub async fn get_battery_stats(app: AppHandle) -> Result<String, String> {
    run_bash(
        &app,
        "idevicediagnostics iorep -q com.apple.ioreport.BatteryUsage 2>&1",
    )
    .await
}

/// Get device thermal state
#[tauri::command]
pub async fn get_thermal_state(app: AppHandle) -> Result<String, String> {
    run_bash(
        &app,
        "idevicediagnostics iorep -q com.apple.ioreport.ThermalState 2>&1",
    )
    .await
}

/// Shutdown device diagnostic
#[tauri::command]
pub async fn device_shutdown(app: AppHandle) -> Result<String, String> {
    run_bash(&app, "idevicediagnostics shutdown 2>&1").await
}

/// Restart device diagnostic
#[tauri::command]
pub async fn device_restart(app: AppHandle) -> Result<String, String> {
    run_bash(&app, "idevicediagnostics restart 2>&1").await
}

/// Sleep device diagnostic
#[tauri::command]
pub async fn device_sleep(app: AppHandle) -> Result<String, String> {
    run_bash(&app, "idevicediagnostics sleep 2>&1").await
}
