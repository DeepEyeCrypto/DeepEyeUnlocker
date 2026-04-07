use serde_json::Value;
use std::fs;
use std::path::PathBuf;
use tauri::Emitter;
#[allow(unused_imports)]
use tauri::Manager;
use tauri_plugin_shell::process::CommandEvent;
use tauri_plugin_shell::ShellExt;

/**
 * Layer 1 — python_module_root() path utility
 * Determines the location of our Python research modules.
 */
pub fn python_module_root(_app: &tauri::AppHandle) -> Result<PathBuf, String> {
    #[cfg(dev)]
    {
        // In development, point to the source-controlled python directory
        Ok(PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("python"))
    }
    #[cfg(not(dev))]
    {
        // [INFERRED] Tauri mobile packaging can surface resources from different extracted locations.
        // Prefer the bundled resource directory when it exists, otherwise fall back to app data.
        if let Ok(resource_dir) = _app.path().resource_dir() {
            let bundled_python = resource_dir.join("python");
            if bundled_python.exists() {
                return Ok(bundled_python);
            }
        }

        let app_data_dir = _app
            .path()
            .app_data_dir()
            .map_err(|e| format!("app data dir error: {e}"))?;
        let python_dir = app_data_dir.join("python");
        fs::create_dir_all(&python_dir).map_err(|e| format!("create python dir error: {e}"))?;
        Ok(python_dir)
    }
}

pub fn python_script_path(
    app: &tauri::AppHandle,
    relative_script_path: &str,
) -> Result<PathBuf, String> {
    let python_root = python_module_root(app)?;
    let script_path = python_root.join(relative_script_path);

    if let Some(parent) = script_path.parent() {
        fs::create_dir_all(parent).map_err(|e| format!("create python script dir error: {e}"))?;
    }

    if !script_path.exists() {
        return Err(format!(
            "bundled python script missing: {}",
            script_path.display()
        ));
    }

    Ok(script_path)
}

/**
 * Layer 2 — ios_backup Tauri Commands implementation (v2)
 */

#[tauri::command]
pub async fn ios_backup_info(app: tauri::AppHandle, backup_path: String) -> Result<Value, String> {
    let python_root = python_module_root(&app)?;

    let output = app
        .shell()
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
    let python_root = python_module_root(&app)?;

    let output = app
        .shell()
        .command("python3")
        .args([
            "-m",
            "ios_backup.cli",
            "hash",
            &backup_path,
            "--output",
            &output_path,
        ])
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
    let python_root = python_module_root(&app)?;

    let output = app
        .shell()
        .command("python3")
        .args([
            "-m",
            "ios_backup.cli",
            "screentime",
            &backup_path,
            "--password",
            &password,
        ])
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
    let python_root = python_module_root(&app)?;

    let (mut rx, _child) = app
        .shell()
        .command("python3")
        .args([
            "-m",
            "ios_backup.cli",
            "crack",
            &backup_path,
            "--wordlist",
            &wordlist,
        ])
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
                    app.emit(
                        "ios-crack-error",
                        format!("Process terminated with code {:?}", status.code),
                    )
                    .ok();
                }
                break;
            }
            _ => {}
        }
    }

    Ok(None) // Result is delivered via channel events
}
