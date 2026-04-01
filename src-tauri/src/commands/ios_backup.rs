use tauri_plugin_shell::ShellExt;
use tauri_plugin_shell::process::CommandEvent;
use serde_json::Value;
use std::path::PathBuf;
use tauri::Emitter;
use tauri::Manager;

/**
 * Layer 1 — python_module_root() path utility
 * Determines the location of our Python research modules.
 */
pub fn python_module_root(_app: &tauri::AppHandle) -> PathBuf {
    #[cfg(dev)]
    {
        // In development, point to the source-controlled python directory
        PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("python")
    }
    #[cfg(not(dev))]
    {
        // In production, point to the bundled resource directory
        _app.path().resource_dir().unwrap().join("python")
    }
}

/**
 * Layer 2 — ios_backup Tauri Commands implementation (v2)
 */

#[tauri::command]
pub async fn ios_backup_info(
    app: tauri::AppHandle,
    backup_path: String,
) -> Result<Value, String> {
    let python_root = python_module_root(&app);
    
    let output = app.shell()
        .command("python3")
        .args(["-m", "ios_backup.cli", "info", &backup_path])
        .env("PYTHONPATH", python_root)
        .output()
        .await
        .map_err(|e| e.to_string())?;

    if !output.status.success() {
        return Err(String::from_utf8_lossy(&output.stderr).to_string());
    }

    serde_json::from_slice(&output.stdout).map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn ios_extract_hash(
    app: tauri::AppHandle,
    backup_path: String,
    output_path: String,
) -> Result<u32, String> {
    let python_root = python_module_root(&app);
    
    let output = app.shell()
        .command("python3")
        .args(["-m", "ios_backup.cli", "hash", &backup_path, "--output", &output_path])
        .env("PYTHONPATH", python_root)
        .output()
        .await
        .map_err(|e| e.to_string())?;

    if !output.status.success() {
        return Err(String::from_utf8_lossy(&output.stderr).to_string());
    }

    // Expecting plain text integer on stdout
    String::from_utf8_lossy(&output.stdout)
        .trim()
        .parse::<u32>()
        .map_err(|e| format!("Invalid hash extraction result: {}", e))
}

#[tauri::command]
pub async fn ios_extract_screentime(
    app: tauri::AppHandle,
    backup_path: String,
    password: String,
) -> Result<Value, String> {
    let python_root = python_module_root(&app);
    
    let output = app.shell()
        .command("python3")
        .args(["-m", "ios_backup.cli", "screentime", &backup_path, "--password", &password])
        .env("PYTHONPATH", python_root)
        .output()
        .await
        .map_err(|e| e.to_string())?;

    if !output.status.success() {
        return Err(String::from_utf8_lossy(&output.stderr).to_string());
    }

    serde_json::from_slice(&output.stdout).map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn ios_run_crack(
    app: tauri::AppHandle,
    backup_path: String,
    wordlist: String,
) -> Result<Option<String>, String> {
    let python_root = python_module_root(&app);
    
    let (mut rx, _child) = app.shell()
        .command("python3")
        .args(["-m", "ios_backup.cli", "crack", &backup_path, "--wordlist", &wordlist])
        .env("PYTHONPATH", python_root)
        .spawn()
        .map_err(|e| e.to_string())?;

    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(line_bytes) => {
                let line = String::from_utf8_lossy(&line_bytes);
                
                // Logic for streaming progress
                if line.contains("H/s") || line.contains("STATUS") {
                    app.emit("ios-crack-progress", line.to_string()).ok();
                }
                
                // Logic for terminal password found
                if line.contains("PASSWORD FOUND") {
                    app.emit("ios-crack-found", line.to_string()).ok();
                    // In a real scenario, we might want to return the password here or just via event
                    // The prompt says "break loop", return Ok(None)
                    break;
                }
            }
            CommandEvent::Stderr(error_bytes) => {
                let error = String::from_utf8_lossy(&error_bytes);
                app.emit("ios-crack-error", error.to_string()).ok();
            }
            CommandEvent::Terminated(status) => {
                if status.code != Some(0) {
                    app.emit("ios-crack-error", format!("Process terminated with code {:?}", status.code)).ok();
                }
                break;
            }
            _ => {}
        }
    }

    Ok(None) // Result is delivered via channel events
}
