use tauri::AppHandle;
use tauri_plugin_shell::ShellExt;

async fn bash(app: &AppHandle, s: &str) -> Result<String, String> {
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

#[tauri::command]
pub async fn pull_crash_logs(app: AppHandle) -> Result<String, String> {
    bash(
        &app,
        "mkdir -p ~/DeepEyeUnlocker/crash_logs && \
         idevicecrashreport -k -e ~/DeepEyeUnlocker/crash_logs/ 2>&1 && \
         echo '✅ Crash logs saved → ~/DeepEyeUnlocker/crash_logs/' && \
         ls ~/DeepEyeUnlocker/crash_logs/ | tail -20",
    )
    .await
}

#[tauri::command]
pub async fn list_crash_logs(app: AppHandle) -> Result<String, String> {
    bash(
        &app,
        "ls -lt ~/DeepEyeUnlocker/crash_logs/*.ips \
              ~/DeepEyeUnlocker/crash_logs/*.crash 2>/dev/null | \
         awk '{print $6, $7, $8, $9}' | head -50 || \
         echo 'No crash logs found. Pull first.'",
    )
    .await
}

#[tauri::command]
pub async fn read_crash_log(app: AppHandle, filename: String) -> Result<String, String> {
    bash(
        &app,
        &format!(
            "cat ~/DeepEyeUnlocker/crash_logs/{filename} 2>&1",
            filename = filename
        ),
    )
    .await
}

#[tauri::command]
pub async fn clear_crash_logs(app: AppHandle) -> Result<String, String> {
    bash(
        &app,
        "idevicecrashreport -k -e /tmp/crash_clear/ 2>&1 && \
         rm -rf /tmp/crash_clear && \
         echo '✅ Device crash logs cleared'",
    )
    .await
}

#[tauri::command]
pub async fn symbolicate_log(
    app: AppHandle,
    log_path: String,
    dsym_path: String,
) -> Result<String, String> {
    bash(
        &app,
        &format!(
        "SCRIPT=$(find /Applications/Xcode.app -name symbolicatecrash 2>/dev/null | head -1) && \
         if [ -n \"$SCRIPT\" ]; then \
             $SCRIPT {log_path} {dsym_path} 2>&1 | head -30; \
         else \
             echo 'symbolicatecrash not found. Install Xcode command line tools.'; \
         fi",
        log_path = log_path,
        dsym_path = dsym_path
    ),
    )
    .await
}
