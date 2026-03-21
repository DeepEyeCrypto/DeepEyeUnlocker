use std::process::Command;

fn afc(subcmd: &str) -> Result<String, String> {
    let out = Command::new("bash").arg("-c").arg(subcmd).output()
        .map_err(|e| e.to_string())?;
    let stdout = String::from_utf8_lossy(&out.stdout).to_string();
    let stderr = String::from_utf8_lossy(&out.stderr).to_string();
    if stdout.is_empty() && !stderr.is_empty() {
        Err(stderr)
    } else {
        Ok(format!("{stdout}{stderr}"))
    }
}

#[tauri::command]
pub fn mount_afc2() -> Result<String, String> {
    afc(
        "mkdir -p ~/DeepEyeUnlocker/mount/afc2 && \
         ifuse --afc2 ~/DeepEyeUnlocker/mount/afc2 2>&1 && \
         echo '✅ AFC2 mounted at ~/DeepEyeUnlocker/mount/afc2/' || \
         (ifuse ~/DeepEyeUnlocker/mount/afc2 2>&1 && \
          echo '✅ AFC (standard) mounted - jailbreak needed for full access')"
    )
}

#[tauri::command]
pub fn list_directory(path: String) -> Result<String, String> {
    afc(&format!(
        "ifuse ~/DeepEyeUnlocker/mount/afc2 2>/dev/null; \
         ls -la ~/DeepEyeUnlocker/mount/afc2/{path} 2>&1 || \
         idevicecrashreport -k ls {path} 2>&1"
    ))
}

#[tauri::command]
pub fn get_file_info(path: String) -> Result<String, String> {
    afc(&format!(
        "stat ~/DeepEyeUnlocker/mount/afc2/{path} 2>&1"
    ))
}

#[tauri::command]
pub fn read_file(path: String) -> Result<String, String> {
    afc(&format!(
        "cat ~/DeepEyeUnlocker/mount/afc2/{path} 2>&1"
    ))
}

#[tauri::command]
pub fn write_file(path: String, content: String) -> Result<String, String> {
    afc(&format!(
        "echo '{content}' > ~/DeepEyeUnlocker/mount/afc2/{path} 2>&1 && \
         echo '✅ Written to {path}'"
    ))
}

#[tauri::command]
pub fn delete_path(path: String) -> Result<String, String> {
    afc(&format!(
        "rm -rf ~/DeepEyeUnlocker/mount/afc2/{path} 2>&1 && \
         echo '✅ Deleted: {path}'"
    ))
}

#[tauri::command]
pub fn make_directory(path: String) -> Result<String, String> {
    afc(&format!(
        "mkdir -p ~/DeepEyeUnlocker/mount/afc2/{path} 2>&1 && \
         echo '✅ Created: {path}'"
    ))
}

#[tauri::command]
pub fn pull_file(device_path: String, local_path: String) -> Result<String, String> {
    afc(&format!(
        "cp ~/DeepEyeUnlocker/mount/afc2/{device_path} {local_path} 2>&1 && \
         echo '✅ Pulled to {local_path}'"
    ))
}

#[tauri::command]
pub fn push_file(local_path: String, device_path: String) -> Result<String, String> {
    afc(&format!(
        "cp {local_path} ~/DeepEyeUnlocker/mount/afc2/{device_path} 2>&1 && \
         echo '✅ Pushed to {device_path}'"
    ))
}
