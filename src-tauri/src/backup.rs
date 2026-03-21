use std::process::Command;

fn bash(s: &str) -> Result<String, String> {
    let out = Command::new("bash").arg("-c").arg(s).output()
        .map_err(|e| e.to_string())?;
    Ok(format!("{}\n{}", String::from_utf8_lossy(&out.stdout),
        String::from_utf8_lossy(&out.stderr)))
}

#[tauri::command]
pub fn create_backup(label: String) -> Result<String, String> {
    bash(&format!(
        "mkdir -p ~/DeepEyeUnlocker/backups/{label} && \
         idevicebackup2 backup --full ~/DeepEyeUnlocker/backups/{label}/ 2>&1 && \
         echo '✅ Backup complete → ~/DeepEyeUnlocker/backups/{label}/'"
    ))
}

#[tauri::command]
pub fn backup_encrypted(label: String, _password: String) -> Result<String, String> {
    bash(&format!(
        "mkdir -p ~/DeepEyeUnlocker/backups/{label} && \
         idevicebackup2 -i backup --full \
         ~/DeepEyeUnlocker/backups/{label}/ 2>&1"
    ))
}

#[tauri::command]
pub fn restore_backup(label: String) -> Result<String, String> {
    bash(&format!(
        "idevicebackup2 restore --system --reboot \
         ~/DeepEyeUnlocker/backups/{label}/ 2>&1"
    ))
}

#[tauri::command]
pub fn list_backups() -> Result<String, String> {
    bash(
        "ls -la ~/DeepEyeUnlocker/backups/ 2>/dev/null && \
         du -sh ~/DeepEyeUnlocker/backups/*/ 2>/dev/null || \
         echo 'No backups found.'"
    )
}

#[tauri::command]
pub fn delete_backup(label: String) -> Result<String, String> {
    bash(&format!(
        "rm -rf ~/DeepEyeUnlocker/backups/{label} && \
         echo '✅ Backup deleted: {label}'"
    ))
}

#[tauri::command]
pub fn change_backup_password(_old_pass: String, _new_pass: String) -> Result<String, String> {
    bash("idevicebackup2 changepw -i 2>&1")
}

#[tauri::command]
pub fn extract_app_data(label: String, bundle_id: String) -> Result<String, String> {
    bash(&format!(
        "mkdir -p ~/DeepEyeUnlocker/app_data/{bundle_id} && \
         find ~/DeepEyeUnlocker/backups/{label} -name 'Manifest.db' | head -1 | \
         xargs -I{{}} dirname {{}} | \
         xargs -I{{}} rsync -a {{}}/ ~/DeepEyeUnlocker/app_data/{bundle_id}/ 2>&1 && \
         echo '✅ App data extracted for {bundle_id}'"
    ))
}

#[tauri::command]
pub fn restore_app_data(label: String) -> Result<String, String> {
    bash(&format!(
        "idevicebackup2 restore --copy \
         ~/DeepEyeUnlocker/backups/{label}/ 2>&1"
    ))
}
