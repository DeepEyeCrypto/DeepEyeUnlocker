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

/// Get ECID from connected device
#[tauri::command]
pub async fn get_ecid(app: AppHandle) -> Result<String, String> {
    run_bash(&app, "ideviceinfo -k UniqueChipID 2>&1").await
}

/// Get board config (needed for tsschecker)
#[tauri::command]
pub async fn get_board_config(app: AppHandle) -> Result<String, String> {
    run_bash(&app, "ideviceinfo -k BoardId && ideviceinfo -k ChipID 2>&1").await
}

/// Save SHSH blobs for currently connected device — all signed firmwares
#[tauri::command]
pub async fn save_shsh_all_signed(
    app: AppHandle,
    model: String,
    ecid: String,
) -> Result<String, String> {
    run_bash(
        &app,
        &format!(
            "mkdir -p ~/DeepEyeUnlocker/shsh/{ecid} && \
         tsschecker -d '{model}' -e '{ecid}' -s -a \
         --save-path ~/DeepEyeUnlocker/shsh/{ecid}/ 2>&1"
        ),
    )
    .await
}

/// Save SHSH blob for specific iOS version
#[tauri::command]
pub async fn save_shsh_specific(
    app: AppHandle,
    model: String,
    ecid: String,
    ios: String,
) -> Result<String, String> {
    run_bash(
        &app,
        &format!(
            "mkdir -p ~/DeepEyeUnlocker/shsh/{ecid} && \
         tsschecker -d '{model}' -e '{ecid}' -i '{ios}' -s \
         --save-path ~/DeepEyeUnlocker/shsh/{ecid}/ 2>&1"
        ),
    )
    .await
}

/// Save using generator (for nonce collision downgrade)
#[tauri::command]
pub async fn save_shsh_with_generator(
    app: AppHandle,
    model: String,
    ecid: String,
    ios: String,
    generator: String,
) -> Result<String, String> {
    run_bash(
        &app,
        &format!(
            "mkdir -p ~/DeepEyeUnlocker/shsh/{ecid} && \
         tsschecker -d '{model}' -e '{ecid}' -i '{ios}' -g '{generator}' -s \
         --save-path ~/DeepEyeUnlocker/shsh/{ecid}/ 2>&1"
        ),
    )
    .await
}

/// List all saved SHSH blobs
#[tauri::command]
pub async fn list_saved_shsh(app: AppHandle) -> Result<String, String> {
    run_bash(
        &app,
        "find ~/DeepEyeUnlocker/shsh -name '*.shsh2' 2>/dev/null | \
         while read f; do echo \"$(basename $f)\"; done | sort",
    )
    .await
}

/// Check which iOS versions are currently signed by Apple
#[tauri::command]
pub async fn check_signed_versions(app: AppHandle, model: String) -> Result<String, String> {
    run_bash(
        &app,
        &format!("tsschecker -d '{model}' --list-ios 2>&1 | grep -i 'signed\\|available'"),
    )
    .await
}

/// futurerestore — restore to unsigned firmware using SHSH blob
#[tauri::command]
pub async fn futurerestore(
    app: AppHandle,
    ipsw_path: String,
    shsh_path: String,
    _sep_manifest: String,
    _baseband: String,
) -> Result<String, String> {
    run_bash(
        &app,
        &format!(
            "futurerestore -t '{shsh_path}' \
         --latest-sep --latest-baseband \
         '{ipsw_path}' 2>&1"
        ),
    )
    .await
}

/// futurerestore no baseband (WiFi iPad, iPod)
#[tauri::command]
pub async fn futurerestore_no_baseband(
    app: AppHandle,
    ipsw_path: String,
    shsh_path: String,
) -> Result<String, String> {
    run_bash(
        &app,
        &format!("futurerestore -t '{shsh_path}' --no-baseband '{ipsw_path}' 2>&1"),
    )
    .await
}
