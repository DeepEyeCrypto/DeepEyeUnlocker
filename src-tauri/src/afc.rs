use tauri::AppHandle;
use tauri_plugin_shell::ShellExt;

async fn afc(app: &AppHandle, subcmd: &str) -> Result<String, String> {
    let output = app
        .shell()
        .command("bash")
        .args(["-c", subcmd])
        .output()
        .await
        .map_err(|e| e.to_string())?;

    let stdout = String::from_utf8_lossy(&output.stdout).to_string();
    let stderr = String::from_utf8_lossy(&output.stderr).to_string();

    if stdout.is_empty() && !stderr.is_empty() {
        Err(stderr)
    } else {
        Ok(format!("{stdout}{stderr}"))
    }
}

#[tauri::command]
pub async fn mount_afc2(app: AppHandle) -> Result<String, String> {
    afc(
        &app,
        "mkdir -p ~/DeepEyeUnlocker/mount/afc2 && \
         ifuse --afc2 ~/DeepEyeUnlocker/mount/afc2 2>&1 && \
         echo '✅ AFC2 mounted at ~/DeepEyeUnlocker/mount/afc2/' || \
         (ifuse ~/DeepEyeUnlocker/mount/afc2 2>&1 && \
          echo '✅ AFC (standard) mounted - jailbreak needed for full access')",
    )
    .await
}

#[tauri::command]
pub async fn list_directory(app: AppHandle, path: String) -> Result<String, String> {
    afc(
        &app,
        &format!(
            "ifuse ~/DeepEyeUnlocker/mount/afc2 2>/dev/null; \
         ls -la ~/DeepEyeUnlocker/mount/afc2/{path} 2>&1 || \
         idevicecrashreport -k ls {path} 2>&1",
            path = path
        ),
    )
    .await
}

#[tauri::command]
pub async fn get_file_info(app: AppHandle, path: String) -> Result<String, String> {
    afc(
        &app,
        &format!("stat ~/DeepEyeUnlocker/mount/afc2/{path} 2>&1", path = path),
    )
    .await
}

#[tauri::command]
pub async fn read_file(app: AppHandle, path: String) -> Result<String, String> {
    afc(
        &app,
        &format!("cat ~/DeepEyeUnlocker/mount/afc2/{path} 2>&1", path = path),
    )
    .await
}

#[tauri::command]
pub async fn write_file(app: AppHandle, path: String, content: String) -> Result<String, String> {
    afc(
        &app,
        &format!(
            "echo '{content}' > ~/DeepEyeUnlocker/mount/afc2/{path} 2>&1 && \
         echo '✅ Written to {path}'",
            content = content,
            path = path
        ),
    )
    .await
}

#[tauri::command]
pub async fn delete_path(app: AppHandle, path: String) -> Result<String, String> {
    afc(
        &app,
        &format!(
            "rm -rf ~/DeepEyeUnlocker/mount/afc2/{path} 2>&1 && \
         echo '✅ Deleted: {path}'",
            path = path
        ),
    )
    .await
}

#[tauri::command]
pub async fn make_directory(app: AppHandle, path: String) -> Result<String, String> {
    afc(
        &app,
        &format!(
            "mkdir -p ~/DeepEyeUnlocker/mount/afc2/{path} 2>&1 && \
         echo '✅ Created: {path}'",
            path = path
        ),
    )
    .await
}

#[tauri::command]
pub async fn pull_file(
    app: AppHandle,
    device_path: String,
    local_path: String,
) -> Result<String, String> {
    afc(
        &app,
        &format!(
            "cp ~/DeepEyeUnlocker/mount/afc2/{device_path} {local_path} 2>&1 && \
         echo '✅ Pulled to {local_path}'",
            device_path = device_path,
            local_path = local_path
        ),
    )
    .await
}

#[tauri::command]
pub async fn push_file(
    app: AppHandle,
    local_path: String,
    device_path: String,
) -> Result<String, String> {
    afc(
        &app,
        &format!(
            "cp {local_path} ~/DeepEyeUnlocker/mount/afc2/{device_path} 2>&1 && \
         echo '✅ Pushed to {device_path}'",
            local_path = local_path,
            device_path = device_path
        ),
    )
    .await
}
