use std::process::Command;

fn run_bash(s: &str) -> Result<String, String> {
    let output = Command::new("bash").arg("-c").arg(s).output()
        .map_err(|e| e.to_string())?;
    Ok(format!("{}\n{}", String::from_utf8_lossy(&output.stdout),
        String::from_utf8_lossy(&output.stderr)))
}

/// Get ECID from connected device
#[tauri::command]
pub fn get_ecid() -> Result<String, String> {
    run_bash("ideviceinfo -k UniqueChipID 2>&1")
}

/// Get board config (needed for tsschecker)
#[tauri::command]
pub fn get_board_config() -> Result<String, String> {
    run_bash("ideviceinfo -k BoardId && ideviceinfo -k ChipID 2>&1")
}

/// Save SHSH blobs for currently connected device — all signed firmwares
#[tauri::command]
pub fn save_shsh_all_signed(model: String, ecid: String) -> Result<String, String> {
    run_bash(&format!(
        "mkdir -p ~/DeepEyeUnlocker/shsh/{ecid} && \
         tsschecker -d '{model}' -e '{ecid}' -s -a \
         --save-path ~/DeepEyeUnlocker/shsh/{ecid}/ 2>&1"
    ))
}

/// Save SHSH blob for specific iOS version
#[tauri::command]
pub fn save_shsh_specific(model: String, ecid: String, ios: String) -> Result<String, String> {
    run_bash(&format!(
        "mkdir -p ~/DeepEyeUnlocker/shsh/{ecid} && \
         tsschecker -d '{model}' -e '{ecid}' -i '{ios}' -s \
         --save-path ~/DeepEyeUnlocker/shsh/{ecid}/ 2>&1"
    ))
}

/// Save using generator (for nonce collision downgrade)
#[tauri::command]
pub fn save_shsh_with_generator(model: String, ecid: String, ios: String, generator: String) -> Result<String, String> {
    run_bash(&format!(
        "mkdir -p ~/DeepEyeUnlocker/shsh/{ecid} && \
         tsschecker -d '{model}' -e '{ecid}' -i '{ios}' -g '{generator}' -s \
         --save-path ~/DeepEyeUnlocker/shsh/{ecid}/ 2>&1"
    ))
}

/// List all saved SHSH blobs
#[tauri::command]
pub fn list_saved_shsh() -> Result<String, String> {
    run_bash(
        "find ~/DeepEyeUnlocker/shsh -name '*.shsh2' 2>/dev/null | \
         while read f; do echo \"$(basename $f)\"; done | sort"
    )
}

/// Check which iOS versions are currently signed by Apple
#[tauri::command]
pub fn check_signed_versions(model: String) -> Result<String, String> {
    run_bash(&format!(
        "tsschecker -d '{model}' --list-ios 2>&1 | grep -i 'signed\\|available'"
    ))
}

/// futurerestore — restore to unsigned firmware using SHSH blob
#[tauri::command]
pub fn futurerestore(
    ipsw_path: String,
    shsh_path: String,
    sep_manifest: String,
    baseband: String
) -> Result<String, String> {
    run_bash(&format!(
        "futurerestore -t '{shsh_path}' \
         --latest-sep --latest-baseband \
         '{ipsw_path}' 2>&1"
    ))
}

/// futurerestore no baseband (WiFi iPad, iPod)
#[tauri::command]
pub fn futurerestore_no_baseband(ipsw_path: String, shsh_path: String) -> Result<String, String> {
    run_bash(&format!(
        "futurerestore -t '{shsh_path}' --no-baseband '{ipsw_path}' 2>&1"
    ))
}
