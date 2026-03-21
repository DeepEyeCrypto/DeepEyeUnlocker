use std::process::Command;

fn bash(s: &str) -> Result<String, String> {
    let out = Command::new("bash").arg("-c").arg(s).output()
        .map_err(|e| e.to_string())?;
    Ok(format!("{}\n{}", String::from_utf8_lossy(&out.stdout),
        String::from_utf8_lossy(&out.stderr)))
}

#[tauri::command]
pub fn mount_dev_disk_image() -> Result<String, String> {
    bash(
        "IOS_VER=$(ideviceinfo -k ProductVersion | cut -d. -f1,2) && \
         DMG_PATH=$(find /Applications/Xcode.app -name DeveloperDiskImage.dmg \
                    -path \"*$IOS_VER*\" 2>/dev/null | head -1) && \
         SIG_PATH=\"$DMG_PATH.signature\" && \
         if [ -z \"$DMG_PATH\" ]; then \
           echo \"❌ DeveloperDiskImage not found for iOS $IOS_VER\"; \
           echo 'Download from: https://github.com/mspvirajpatel/Xcode_Developer_Disk_Images'; \
         else \
           ideviceimagemounter \"$DMG_PATH\" \"$SIG_PATH\" 2>&1 && \
           echo \"✅ DeveloperDiskImage mounted for iOS $IOS_VER\"; \
         fi"
    )
}

#[tauri::command]
pub fn unmount_dev_disk_image() -> Result<String, String> {
    bash("ideviceimagemounter -u 2>&1 && echo '✅ DeveloperDiskImage unmounted'")
}

#[tauri::command]
pub fn check_dev_disk_mounted() -> Result<String, String> {
    bash(
        "ideviceimagemounter -l 2>&1 | grep -i 'image\\|developer' && \
         echo 'Status: MOUNTED' || echo 'Status: NOT mounted'"
    )
}

#[tauri::command]
pub fn list_processes() -> Result<String, String> {
    bash(
        "frida-ps -U 2>&1 | head -50 || \
         idevicediagnostics diagnostics All 2>&1 | python3 -c \
         'import json,sys; d=json.load(sys.stdin); \
          [print(p) for p in d.get(\"Processes\", {}).keys()]'"
    )
}

#[tauri::command]
pub fn get_screenshot() -> Result<String, String> {
    bash(
        "TS=$(date +%Y%m%d_%H%M%S) && \
         mkdir -p ~/DeepEyeUnlocker/screenshots && \
         idevicescreenshot ~/DeepEyeUnlocker/screenshots/shot_$TS.png 2>&1 && \
         echo \"✅ Screenshot saved: ~/DeepEyeUnlocker/screenshots/shot_$TS.png\""
    )
}
