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
pub async fn mount_dev_disk_image(app: AppHandle) -> Result<String, String> {
    bash(
        &app,
        "IOS_VER=$(ideviceinfo -k ProductVersion | cut -d. -f1,2) && DMG_PATH=$(find /Applications/Xcode.app -name DeveloperDiskImage.dmg -path \"*$IOS_VER*\" 2>/dev/null | head -1) && SIG_PATH=\"$DMG_PATH.signature\" && if [ -z \"$DMG_PATH\" ]; then echo \"❌ DeveloperDiskImage not found for iOS $IOS_VER\"; echo 'Download from: https://github.com/mspvirajpatel/Xcode_Developer_Disk_Images'; else ideviceimagemounter \"$DMG_PATH\" \"$SIG_PATH\" 2>&1 && echo \"✅ DeveloperDiskImage mounted for iOS $IOS_VER\"; fi",
    )
    .await
}

#[tauri::command]
pub async fn unmount_dev_disk_image(app: AppHandle) -> Result<String, String> {
    bash(&app, "ideviceimagemounter -u 2>&1 && echo '✅ DeveloperDiskImage unmounted'").await
}

#[tauri::command]
pub async fn check_dev_disk_mounted(app: AppHandle) -> Result<String, String> {
    bash(
        &app,
        "ideviceimagemounter -l 2>&1 | grep -Ei 'image|developer' && echo 'Status: MOUNTED' || echo 'Status: NOT mounted'",
    )
    .await
}

#[tauri::command]
pub async fn list_processes(app: AppHandle) -> Result<String, String> {
    bash(
        &app,
        "frida-ps -U 2>&1 | head -50 || idevicediagnostics diagnostics All 2>&1 | python3 -c 'import json,sys; d=json.load(sys.stdin); [print(p) for p in d.get(\"Processes\", {}).keys()]'",
    )
    .await
}

#[tauri::command]
pub async fn get_screenshot(app: AppHandle) -> Result<String, String> {
    bash(
        &app,
        "TS=$(date +%Y%m%d_%H%M%S) && mkdir -p ~/DeepEyeUnlocker/screenshots && idevicescreenshot ~/DeepEyeUnlocker/screenshots/shot_$TS.png 2>&1 && echo \"✅ Screenshot saved: ~/DeepEyeUnlocker/screenshots/shot_$TS.png\"",
    )
    .await
}
