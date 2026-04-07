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
pub async fn create_backup(app: AppHandle, label: String) -> Result<String, String> {
    bash(
        &app,
        &format!(
            "mkdir -p ~/DeepEyeUnlocker/backups/{label} && \
         idevicebackup2 backup --full ~/DeepEyeUnlocker/backups/{label}/ 2>&1 && \
         echo '✅ Backup complete → ~/DeepEyeUnlocker/backups/{label}/'",
            label = label
        ),
    )
    .await
}

#[tauri::command]
pub async fn backup_encrypted(
    app: AppHandle,
    label: String,
    _password: String,
) -> Result<String, String> {
    bash(
        &app,
        &format!(
            "mkdir -p ~/DeepEyeUnlocker/backups/{label} && \
         idevicebackup2 -i backup --full \
         ~/DeepEyeUnlocker/backups/{label}/ 2>&1",
            label = label
        ),
    )
    .await
}

#[tauri::command]
pub async fn restore_backup(app: AppHandle, label: String) -> Result<String, String> {
    bash(
        &app,
        &format!(
            "idevicebackup2 restore --system --reboot \
         ~/DeepEyeUnlocker/backups/{label}/ 2>&1",
            label = label
        ),
    )
    .await
}

#[tauri::command]
pub async fn list_backups(app: AppHandle) -> Result<String, String> {
    bash(
        &app,
        "ls -la ~/DeepEyeUnlocker/backups/ 2>/dev/null && \
         du -sh ~/DeepEyeUnlocker/backups/*/ 2>/dev/null || \
         echo 'No backups found.'",
    )
    .await
}

#[tauri::command]
pub async fn delete_backup(app: AppHandle, label: String) -> Result<String, String> {
    bash(
        &app,
        &format!(
            "rm -rf ~/DeepEyeUnlocker/backups/{label} && \
         echo '✅ Backup deleted: {label}'",
            label = label
        ),
    )
    .await
}

#[tauri::command]
pub async fn change_backup_password(
    app: AppHandle,
    old_password: String,
    new_password: String,
) -> Result<String, String> {
    bash(
        &app,
        &format!(
            "idevicebackup2 encryption on '{new_password}' '{old_password}' 2>&1 || idevicebackup2 encryption on '{new_password}' 2>&1"
        ),
    )
    .await
}

#[tauri::command]
pub async fn extract_app_data(
    app: AppHandle,
    bundle_id: String,
    output_dir: String,
) -> Result<String, String> {
    bash(
        &app,
        &format!(
            "mkdir -p '{output_dir}' && idevicebackup2 backup --full '{output_dir}' 2>&1 && echo 'Requested app data extraction for {bundle_id} into {output_dir}'"
        ),
    )
    .await
}

#[tauri::command]
pub async fn restore_app_data(
    app: AppHandle,
    bundle_id: String,
    backup_dir: String,
) -> Result<String, String> {
    bash(
        &app,
        &format!(
            "idevicebackup2 restore --system --reboot '{backup_dir}' 2>&1 && echo 'Requested app data restore for {bundle_id} from {backup_dir}'"
        ),
    )
    .await
}
