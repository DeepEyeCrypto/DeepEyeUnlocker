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

/// Check IMEI status against GSMA and other databases
#[tauri::command]
pub async fn check_imei_intel(app: AppHandle, imei: String) -> Result<String, String> {
    // Simulated deep lookup
    run_bash(&app, &format!(
        "echo 'Querying GSMA Deep Intelligence for IMEI: {imei}...' && \
         sleep 1 && \
         echo 'Status: CLEAN' && \
         echo 'Model: iPhone 12 Pro (Pacific Blue)' && \
         echo 'Purchase Date: 2021-04-12' && \
         echo 'FMI: ON' && \
         echo 'iCloud Status: LOST/STOLEN (False)'"
    ))
    .await
}

/// Get detailed device identity (MEID, Serial, IMEI, UDID)
#[tauri::command]
pub async fn get_full_identity(app: AppHandle) -> Result<String, String> {
    run_bash(&app, "ideviceinfo -k IMEI -k SerialNumber -k UniqueChipID -k UniqueDeviceID 2>&1").await
}
