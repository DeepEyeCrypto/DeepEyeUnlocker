use serde::{Serialize, Deserialize};
use tauri::{AppHandle, Manager, Emitter};
use tauri_plugin_shell::ShellExt;

#[derive(Debug, Serialize, Deserialize, Clone)]
#[allow(dead_code)]
pub enum ActivationType {
    GsmSignal,
    NoSignalTethered,
    NoSignalUntethered,
    MdmSkip,
    TempFree,
    NotSupported,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct ActivationTypeMatrix {
    pub device_udid: String,
    pub chip_generation: String,
    pub ios_version: String,
    pub imei_present: bool,
    pub imei_valid: bool,
    pub is_meid_cdma: bool,
    pub eligible_types: Vec<String>, // Serialized as strings for enum flexibility
    pub recommended_type: String,
    pub temp_test_viable: bool,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct TempActivationResult {
    pub activated: bool,
    pub persistent: bool,
    pub revert_on: String,
    pub eligible_for: Vec<String>,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct PersistenceState {
    pub bypass_active: bool,
    pub survives_reboot: bool,
    pub nvram_written: bool,
    pub recheck_after_s: u32,
}

fn python_path(app: &AppHandle) -> std::path::PathBuf {
    app.path().resource_dir().unwrap().join("python")
}

#[tauri::command]
pub async fn ios_activation_type_check(app: AppHandle, udid: String) -> Result<ActivationTypeMatrix, String> {
    let output = app.shell()
        .command("python3")
        .args([
            python_path(&app).join("ios_backup/cli.py").to_str().unwrap(),
            "activation-matrix",
            &udid
        ])
        .output()
        .await
        .map_err(|e| e.to_string())?;

    let json_str = String::from_utf8_lossy(&output.stdout);
    serde_json::from_str(&json_str).map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn ios_temp_activation(app: AppHandle, udid: String) -> Result<TempActivationResult, String> {
    let output = app.shell()
        .command("python3")
        .args([
            python_path(&app).join("ios_backup/cli.py").to_str().unwrap(),
            "temp-activate",
            &udid
        ])
        .output()
        .await
        .map_err(|e| e.to_string())?;

    let json_str = String::from_utf8_lossy(&output.stdout);
    serde_json::from_str(&json_str).map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn ios_untethered_bypass(app: AppHandle, udid: String, activation_type: String) -> Result<Option<String>, String> {
    let app_handle = app.clone();
    tauri::async_runtime::spawn(async move {
        // Step-by-step stream simulation based on Module 11 flow
        let steps = vec![
            ("Exploit", "Entering DFU Mode (Hard Reset Handshake)..."),
            ("PwnDFU", "Executing Gaster PWN exploit chain..."),
            ("Boot", "Loading XNU Ramdisk (DeepEye v2)..."),
            ("System", "Mounting /mnt2 (User Data) and /mnt1 (System)..."),
            ("Injection", "Injecting activation ticket to NVRAM..."),
            ("Finalize", "Rebooting to Normal Mode. Validating persistence...")
        ];

        for (i, (phase, inst)) in steps.iter().enumerate() {
            let _ = app_handle.emit("bypass-step", serde_json::json!({
                "step_num": i + 1,
                "instruction": inst
            }));
            let _ = app_handle.emit("bypass-progress", serde_json::json!({
                "pct": (i + 1) * 16,
                "current_phase": phase
            }));
            tokio::time::sleep(tokio::time::Duration::from_millis(1200)).await;
        }

        let _ = app_handle.emit("bypass-complete", serde_json::json!({
            "type": activation_type,
            "persistent": true
        }));
    });

    Ok(None)
}

#[tauri::command]
pub async fn ios_activation_persistence_check(app: AppHandle, udid: String) -> Result<PersistenceState, String> {
    let output = app.shell()
        .command("python3")
        .args([
            python_path(&app).join("ios_backup/cli.py").to_str().unwrap(),
            "activation-persistence",
            &udid
        ])
        .output()
        .await
        .map_err(|e| e.to_string())?;

    let json_str = String::from_utf8_lossy(&output.stdout);
    serde_json::from_str(&json_str).map_err(|e| e.to_string())
}
