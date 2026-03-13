use serde::{Serialize, Deserialize};
use tauri::{AppHandle, Manager, Emitter};
use tauri_plugin_shell::ShellExt;
use tauri_plugin_shell::process::CommandEvent;

#[derive(Debug, Serialize, Deserialize, Clone)]
pub enum DeviceMode {
    Normal,
    Recovery,
    DFU,
    Restore,
    Unknown,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct DfuState {
    pub mode: DeviceMode,
    pub ecid: Option<String>,
    pub chip_id: Option<u32>,
    pub board_id: Option<u32>,
}

fn python_path(app: &AppHandle) -> std::path::PathBuf {
    app.path().resource_dir().unwrap().join("python")
}

#[tauri::command]
pub async fn ios_detect_dfu_state(app: AppHandle) -> Result<DfuState, String> {
    println!("[COMMAND] ios_detect_dfu_state");
    
    // In a real implementation, we'd use irecovery -v or similar
    // For now, call our python helper
    let output = app.shell()
        .command("python3")
        .args([
            python_path(&app).join("ios_backup/dfu.py").to_str().unwrap(),
            "dfu-state"
        ])
        .output()
        .await
        .map_err(|e| e.to_string())?;

    if !output.status.success() {
        return Err(String::from_utf8_lossy(&output.stderr).to_string());
    }

    let json_str = String::from_utf8_lossy(&output.stdout);
    let val: serde_json::Value = serde_json::from_str(&json_str).map_err(|e| e.to_string())?;
    
    let mode_str = val["mode"].as_str().unwrap_or("unknown");
    let mode = match mode_str {
        "normal" => DeviceMode::Normal,
        "recovery" => DeviceMode::Recovery,
        "dfu" => DeviceMode::DFU,
        "restore" => DeviceMode::Restore,
        _ => DeviceMode::Unknown,
    };

    Ok(DfuState {
        mode,
        ecid: None, // TODO: Extract from irecovery output
        chip_id: None,
        board_id: None,
    })
}

#[tauri::command]
pub async fn ios_enter_dfu(app: AppHandle, udid: String) -> Result<(), String> {
    println!("[COMMAND] ios_enter_dfu udid={}", udid);
    
    let output = app.shell()
        .command("ideviceenterrecovery")
        .args([&udid])
        .output()
        .await
        .map_err(|e| e.to_string())?;

    if output.status.success() {
        app.emit("dfu-step", "Device entering recovery. Manual DFU steps needed next.").unwrap();
        Ok(())
    } else {
        Err(String::from_utf8_lossy(&output.stderr).to_string())
    }
}

#[tauri::command]
pub async fn ios_restore_device(app: AppHandle, udid: String, ipsw_path: String) -> Result<(), String> {
    println!("[COMMAND] ios_restore_device udid={} ipsw={}", udid, ipsw_path);
    
    let (mut rx, _child) = app.shell()
        .command("idevicerestore")
        .args(["--erase", &ipsw_path])
        .spawn()
        .map_err(|e| e.to_string())?;

    let app_handle = app.clone();
    tauri::async_runtime::spawn(async move {
        while let Some(event) = rx.recv().await {
            match event {
                CommandEvent::Stdout(bytes) => {
                    let line = String::from_utf8_lossy(&bytes).to_string();
                    app_handle.emit("dfu-progress", line).unwrap();
                }
                CommandEvent::Stderr(bytes) => {
                    let line = String::from_utf8_lossy(&bytes).to_string();
                    app_handle.emit("dfu-error", line).unwrap();
                }
                CommandEvent::Terminated(payload) => {
                    app_handle.emit("dfu-complete", payload.code).unwrap();
                    break;
                }
                _ => {}
            }
        }
    });

    Ok(())
}

#[tauri::command]
pub async fn ios_download_ipsw(app: AppHandle, model: String, ios_version: String) -> Result<String, String> {
    println!("[COMMAND] ios_download_ipsw model={} version={}", model, ios_version);
    
    // PSEUDO: IPSW download logic
    // In production, this would use reqwest or curl to download from ipsw.me
    let dest = format!("/tmp/{}_{}_Restore.ipsw", model, ios_version);
    app.emit("dfu-progress", format!("Starting download for {}...", model)).unwrap();
    
    // Simulate progress
    app.emit("dfu-progress", "Downloading: 10%").unwrap();
    app.emit("dfu-progress", "Downloading: 50%").unwrap();
    app.emit("dfu-progress", "Downloading: 100%").unwrap();
    
    Ok(dest)
}
