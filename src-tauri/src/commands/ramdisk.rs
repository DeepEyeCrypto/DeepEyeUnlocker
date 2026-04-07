use serde::{Deserialize, Serialize};
use tauri::{AppHandle, Emitter, Manager};
use tauri_plugin_shell::process::CommandEvent;
use tauri_plugin_shell::ShellExt;

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct PwnState {
    pub pwned: bool,
    pub cpid: String,
    pub model: String,
}

fn python_path(app: &AppHandle) -> std::path::PathBuf {
    app.path().resource_dir().unwrap().join("python")
}

#[tauri::command]
pub async fn ios_check_pwn_state(app: AppHandle, udid: Option<String>) -> Result<PwnState, String> {
    println!("[COMMAND] ios_check_pwn_state");

    let mut args = vec![
        python_path(&app)
            .join("ios_backup/cli.py")
            .to_str()
            .unwrap()
            .to_string(),
        "pwn-state".to_string(),
    ];
    if let Some(u) = udid {
        args.push(u);
    }

    let output = app
        .shell()
        .command("python3")
        .args(args)
        .output()
        .await
        .map_err(|e| e.to_string())?;

    if !output.status.success() {
        return Err(String::from_utf8_lossy(&output.stderr).to_string());
    }

    let json_str = String::from_utf8_lossy(&output.stdout);
    serde_json::from_str(&json_str).map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn ios_run_gaster_pwn(app: AppHandle) -> Result<(), String> {
    println!("[COMMAND] ios_run_gaster_pwn");

    let (mut rx, _child) = app
        .shell()
        .command("python3")
        .args([
            python_path(&app)
                .join("ios_backup/cli.py")
                .to_str()
                .unwrap(),
            "gaster-pwn",
        ])
        .spawn()
        .map_err(|e| e.to_string())?;

    let app_handle = app.clone();
    tauri::async_runtime::spawn(async move {
        while let Some(event) = rx.recv().await {
            match event {
                CommandEvent::Stdout(bytes) => {
                    let line = String::from_utf8_lossy(&bytes).to_string();
                    app_handle.emit("pwn-progress", line).unwrap();
                }
                CommandEvent::Terminated(payload) => {
                    app_handle.emit("pwn-complete", payload.code).unwrap();
                    break;
                }
                _ => {}
            }
        }
    });

    Ok(())
}

#[tauri::command]
pub async fn ios_boot_ramdisk(_app: AppHandle, ramdisk_path: String) -> Result<(), String> {
    println!("[COMMAND] ios_boot_ramdisk path={}", ramdisk_path);
    // Placeholder for booting ramdisk via gaster or irecovery
    Ok(())
}
