use std::process::Command;

fn bash(s: &str) -> Result<String, String> {
    let out = Command::new("bash").arg("-c").arg(s).output()
        .map_err(|e| e.to_string())?;
    Ok(format!("{}\n{}", String::from_utf8_lossy(&out.stdout),
        String::from_utf8_lossy(&out.stderr)))
}

#[tauri::command]
pub fn pull_crash_logs() -> Result<String, String> {
    bash(
        "mkdir -p ~/DeepEyeUnlocker/crash_logs && \
         idevicecrashreport -k -e ~/DeepEyeUnlocker/crash_logs/ 2>&1 && \
         echo '✅ Crash logs saved → ~/DeepEyeUnlocker/crash_logs/' && \
         ls ~/DeepEyeUnlocker/crash_logs/ | tail -20"
    )
}

#[tauri::command]
pub fn list_crash_logs() -> Result<String, String> {
    bash(
        "ls -lt ~/DeepEyeUnlocker/crash_logs/*.ips \
              ~/DeepEyeUnlocker/crash_logs/*.crash 2>/dev/null | \
         awk '{print $6, $7, $8, $9}' | head -50 || \
         echo 'No crash logs found. Pull first.'"
    )
}

#[tauri::command]
pub fn read_crash_log(filename: String) -> Result<String, String> {
    bash(&format!(
        "cat ~/DeepEyeUnlocker/crash_logs/{filename} 2>&1"
    ))
}

#[tauri::command]
pub fn clear_crash_logs() -> Result<String, String> {
    bash(
        "idevicecrashreport -k -e /tmp/crash_clear/ 2>&1 && \
         rm -rf /tmp/crash_clear && \
         echo '✅ Device crash logs cleared'"
    )
}

#[tauri::command]
pub fn symbolicate_log(log_path: String, dsym_path: String) -> Result<String, String> {
    bash(&format!(
        "SCRIPT=$(find /Applications/Xcode.app -name symbolicatecrash 2>/dev/null | head -1) && \
         if [ -n \"$SCRIPT\" ]; then \
           DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
           \"$SCRIPT\" '{log_path}' '{dsym_path}' 2>&1; \
         else \
           echo 'Xcode not found. Basic atos symbolication:' && \
           atos -o '{dsym_path}' -arch arm64 2>&1; \
         fi"
    ))
}
