use serde::{Serialize, Deserialize};
use tauri::{AppHandle, Manager, Emitter};
use tauri_plugin_shell::ShellExt;
use tauri_plugin_shell::process::CommandEvent;

#[derive(Debug, Serialize, Deserialize, Clone)]
pub enum ActivationRemovalPath {
    None,
    DfuRestore,
    DirectFmiOff,
    Checkra1n,
    A12Ramdisk,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct ActivationState {
    pub locked: bool,
    pub fmi_enabled: bool,
    pub apple_id_bound: Option<String>,
    pub removal_path: ActivationRemovalPath,
    pub model: String,
    pub chip: String,
}

fn python_path(app: &AppHandle) -> std::path::PathBuf {
    app.path().resource_dir().unwrap().join("python")
}

#[tauri::command]
pub async fn ios_check_activation_state(app: AppHandle, udid: String) -> Result<ActivationState, String> {
    println!("[COMMAND] ios_check_activation_state udid={}", udid);
    
    let output = app.shell()
        .command("python3")
        .args([
            python_path(&app).join("ios_backup/cli.py").to_str().unwrap(),
            "activation-state",
            &udid
        ])
        .output()
        .await
        .map_err(|e| e.to_string())?;

    if !output.status.success() {
        return Err(String::from_utf8_lossy(&output.stderr).to_string());
    }

    let json_str = String::from_utf8_lossy(&output.stdout);
    let val: serde_json::Value = serde_json::from_str(&json_str).map_err(|e| e.to_string())?;
    
    if val.get("error").is_some() {
        return Err(val["error"].as_str().unwrap().to_string());
    }

    let fmi_enabled = val["fmi_enabled"].as_bool().unwrap_or(false);
    let locked = val["locked"].as_bool().unwrap_or(false);
    let chip = val["chip"].as_str().unwrap_or("Unknown").to_string();

    // Logic to determine removal path
    let removal_path = if !fmi_enabled {
        ActivationRemovalPath::None
    } else if chip.contains("arm64") || chip.contains("A7") || chip.contains("A8") || chip.contains("A9") || chip.contains("A10") || chip.contains("A11") {
        ActivationRemovalPath::Checkra1n
    } else {
        ActivationRemovalPath::DfuRestore
    };

    Ok(ActivationState {
        locked,
        fmi_enabled,
        apple_id_bound: None, // TODO: Extract email prefix if possible
        removal_path,
        model: val["model"].as_str().unwrap_or("Unknown").to_string(),
        chip,
    })
}

#[tauri::command]
pub async fn ios_run_checkra1n(app: AppHandle, udid: String) -> Result<(), String> {
    println!("[COMMAND] ios_run_checkra1n udid={}", udid);
    
    let (mut rx, _child) = app.shell()
        .command("checkra1n")
        .args(["--cli", "--udid", &udid])
        .spawn()
        .map_err(|e| e.to_string())?;

    let app_handle = app.clone();
    tauri::async_runtime::spawn(async move {
        while let Some(event) = rx.recv().await {
            match event {
                CommandEvent::Stdout(bytes) => {
                    let line = String::from_utf8_lossy(&bytes).to_string();
                    app_handle.emit("checkra1n-progress", line).unwrap();
                }
                CommandEvent::Stderr(bytes) => {
                    let line = String::from_utf8_lossy(&bytes).to_string();
                    app_handle.emit("activation-error", line).unwrap();
                }
                CommandEvent::Terminated(payload) => {
                    app_handle.emit("activation-complete", payload.code).unwrap();
                    break;
                }
                _ => {}
            }
        }
    });

    Ok(())
}

#[tauri::command]
pub async fn ios_patch_activation_record(app: AppHandle, udid: String) -> Result<(), String> {
    println!("[COMMAND] ios_patch_activation_record udid={}", udid);
    // Placeholder for actual patch logic (usually involves SSH to jailbroken device)
    Ok(())
}
