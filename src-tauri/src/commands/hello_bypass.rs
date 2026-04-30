use serde::{Deserialize, Serialize};
use tauri::{AppHandle, Emitter, Manager};
use tauri_plugin_shell::ShellExt;

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct HelloBypassDevice {
    pub chip_id: String,
    pub chip_name: String,
    pub udid: String,
    pub ios_version: String,
    pub ios_build: String,
    pub ios_major: u32,
    pub model: String,
    pub serial: String,
    pub exploit_method: String, // "checkm8" or "server_bypass"
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct HelloBypassProgress {
    pub session_id: String,
    pub event: String,
    pub message: String,
    #[serde(flatten)]
    pub extra: serde_json::Value,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct HelloBypassResult {
    pub success: bool,
    pub method: String,
    pub notes: Vec<String>,
}

fn python_path(app: &AppHandle) -> std::path::PathBuf {
    app.path().resource_dir().unwrap().join("python")
}

/// Detect a connected iOS device and determine the correct bypass route.
/// Runs hello_bypass.py with "detect" mode, parses streaming JSON events,
/// and emits "hello-bypass-progress" Tauri events for each line.
#[tauri::command]
pub async fn hello_bypass_detect(
    app: AppHandle,
    session_id: String,
) -> Result<HelloBypassDevice, String> {
    println!("[COMMAND] hello_bypass_detect session_id={}", session_id);

    let script_path = python_path(&app).join("ios_bypass/hello_bypass.py");

    let output = app
        .shell()
        .command("python3")
        .args([script_path.to_str().unwrap(), "detect", &session_id])
        .output()
        .await
        .map_err(|e| format!("Failed to run hello_bypass detect: {e}"))?;

    let stdout = String::from_utf8_lossy(&output.stdout);

    // Parse and emit each JSON line as a progress event
    let mut device: Option<HelloBypassDevice> = None;
    for line in stdout.lines() {
        let line = line.trim();
        if line.is_empty() {
            continue;
        }
        if let Ok(val) = serde_json::from_str::<serde_json::Value>(line) {
            let event_name = val
                .get("event")
                .and_then(|v| v.as_str())
                .unwrap_or("info")
                .to_string();
            let message = val
                .get("message")
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .to_string();

            let progress = HelloBypassProgress {
                session_id: session_id.clone(),
                event: event_name.clone(),
                message: message.clone(),
                extra: val.clone(),
            };
            let _ = app.emit("hello-bypass-progress", progress);

            // Extract device info if this is a device_found event
            if event_name == "device_found" {
                if let Ok(dev) = serde_json::from_value::<HelloBypassDevice>(val) {
                    device = Some(dev);
                }
            }
        }
    }

    if !output.status.success() {
        let stderr = String::from_utf8_lossy(&output.stderr).to_string();
        // Still try to return device if we parsed one before the error
        if let Some(dev) = device {
            return Ok(dev);
        }
        return Err(format!("hello_bypass detect failed: {stderr}"));
    }

    device.ok_or_else(|| "No device_found event received from hello_bypass.py".to_string())
}

/// Main bypass command. Runs hello_bypass.py, captures stdout line by line,
/// parses each JSON event, emits "hello-bypass-progress" Tauri events,
/// and returns a HelloBypassResult on completion.
#[tauri::command]
pub async fn hello_bypass_run(
    app: AppHandle,
    session_id: String,
) -> Result<HelloBypassResult, String> {
    println!("[COMMAND] hello_bypass_run session_id={}", session_id);

    let script_path = python_path(&app).join("ios_bypass/hello_bypass.py");

    // Emit a "starting" progress event immediately
    let start_progress = HelloBypassProgress {
        session_id: session_id.clone(),
        event: "starting".to_string(),
        message: "Initialising Hello Bypass pipeline…".to_string(),
        extra: serde_json::Value::Object(serde_json::Map::new()),
    };
    let _ = app.emit("hello-bypass-progress", start_progress);

    let output = app
        .shell()
        .command("python3")
        .args([script_path.to_str().unwrap(), "run", &session_id])
        .output()
        .await
        .map_err(|e| format!("Failed to spawn hello_bypass.py: {e}"))?;

    let stdout = String::from_utf8_lossy(&output.stdout);

    // Parse every JSON line and emit as progress events
    let mut result: Option<HelloBypassResult> = None;
    for line in stdout.lines() {
        let line = line.trim();
        if line.is_empty() {
            continue;
        }
        if let Ok(val) = serde_json::from_str::<serde_json::Value>(line) {
            let event_name = val
                .get("event")
                .and_then(|v| v.as_str())
                .unwrap_or("info")
                .to_string();
            let message = val
                .get("message")
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .to_string();

            let progress = HelloBypassProgress {
                session_id: session_id.clone(),
                event: event_name.clone(),
                message: message.clone(),
                extra: val.clone(),
            };
            let _ = app.emit("hello-bypass-progress", progress);

            // Capture the final "complete" event as the result
            if event_name == "complete" {
                let success = val
                    .get("success")
                    .and_then(|v| v.as_bool())
                    .unwrap_or(false);
                let method = val
                    .get("method")
                    .and_then(|v| v.as_str())
                    .unwrap_or("unknown")
                    .to_string();
                let notes: Vec<String> = val
                    .get("notes")
                    .and_then(|v| v.as_array())
                    .map(|arr| {
                        arr.iter()
                            .filter_map(|n| n.as_str().map(|s| s.to_string()))
                            .collect()
                    })
                    .unwrap_or_default();

                result = Some(HelloBypassResult {
                    success,
                    method,
                    notes,
                });
            }
        }
    }

    if !output.status.success() {
        let stderr = String::from_utf8_lossy(&output.stderr).to_string();

        // Emit an error progress event
        let err_progress = HelloBypassProgress {
            session_id: session_id.clone(),
            event: "error".to_string(),
            message: stderr.clone(),
            extra: serde_json::Value::Object(serde_json::Map::new()),
        };
        let _ = app.emit("hello-bypass-progress", err_progress);

        return Err(format!("hello_bypass.py exited with error: {stderr}"));
    }

    Ok(result.unwrap_or(HelloBypassResult {
        success: false,
        method: "unknown".to_string(),
        notes: vec!["No complete event received from hello_bypass.py".to_string()],
    }))
}
