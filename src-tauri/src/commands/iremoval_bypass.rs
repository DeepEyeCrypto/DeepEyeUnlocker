use serde::{Deserialize, Serialize};
use tauri::{AppHandle, Emitter, Manager};
use tauri_plugin_shell::ShellExt;

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct IRemovalDevice {
    pub chip_id: String,
    pub chip_name: String,
    pub udid: String,
    pub ecid: String,
    pub serial: String,
    pub imei: String,
    pub ios_version: String,
    pub ios_major: u32,
    pub model: String,
    pub exploit_method: String, // "checkm8" or "server_bypass"
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct IRemovalProgress {
    pub session_id: String,
    pub event: String,
    pub message: String, // NOTE: Python emits "message" key, NOT "msg"
    #[serde(flatten)]
    pub extra: serde_json::Value,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct IRemovalResult {
    pub success: bool,
    pub technique: String, // "A", "B", or "C"
    pub message: String,
}

fn python_path(app: &AppHandle) -> std::path::PathBuf {
    app.path().resource_dir().unwrap().join("python")
}

/// Detect a connected iOS device and determine the correct iRemoval bypass route.
/// Runs iremoval_bypass.py with "detect" mode, parses streaming JSON events,
/// and emits "iremoval-progress" Tauri events for each line.
#[tauri::command]
pub async fn iremoval_detect(app: AppHandle, session_id: String) -> Result<IRemovalDevice, String> {
    println!("[COMMAND] iremoval_detect session_id={}", session_id);

    let script_path = python_path(&app).join("ios_bypass/iremoval_bypass.py");

    let output = app
        .shell()
        .command("python3")
        .args([script_path.to_str().unwrap(), "detect", &session_id])
        .output()
        .await
        .map_err(|e| format!("Failed to run iremoval_bypass detect: {e}"))?;

    let stdout = String::from_utf8_lossy(&output.stdout);

    // Parse and emit each JSON line as a progress event
    let mut device: Option<IRemovalDevice> = None;
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

            let progress = IRemovalProgress {
                session_id: session_id.clone(),
                event: event_name.clone(),
                message: message.clone(),
                extra: val.clone(),
            };
            let _ = app.emit("iremoval-progress", progress);

            // Extract device info if this is a device_found event
            if event_name == "device_found" {
                if let Ok(dev) = serde_json::from_value::<IRemovalDevice>(val) {
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
        return Err(format!("iremoval_bypass detect failed: {stderr}"));
    }

    device.ok_or_else(|| "No device_found event received from iremoval_bypass.py".to_string())
}

/// Main iRemoval bypass command. Runs iremoval_bypass.py with "run" mode,
/// captures stdout line by line, parses each JSON event, emits "iremoval-progress"
/// Tauri events, and returns an IRemovalResult on completion.
#[tauri::command]
pub async fn iremoval_run(app: AppHandle, session_id: String) -> Result<IRemovalResult, String> {
    println!("[COMMAND] iremoval_run session_id={}", session_id);

    let script_path = python_path(&app).join("ios_bypass/iremoval_bypass.py");

    // Emit a "starting" progress event immediately
    let start_progress = IRemovalProgress {
        session_id: session_id.clone(),
        event: "starting".to_string(),
        message: "Initialising iRemoval bypass pipeline…".to_string(),
        extra: serde_json::Value::Object(serde_json::Map::new()),
    };
    let _ = app.emit("iremoval-progress", start_progress);

    let output = app
        .shell()
        .command("python3")
        .args([script_path.to_str().unwrap(), "run", &session_id])
        .output()
        .await
        .map_err(|e| format!("Failed to spawn iremoval_bypass.py: {e}"))?;

    let stdout = String::from_utf8_lossy(&output.stdout);

    // Parse every JSON line and emit as progress events
    let mut result: Option<IRemovalResult> = None;
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

            let progress = IRemovalProgress {
                session_id: session_id.clone(),
                event: event_name.clone(),
                message: message.clone(),
                extra: val.clone(),
            };
            let _ = app.emit("iremoval-progress", progress);

            // Capture the final "complete" or "failed" event as the result
            if event_name == "complete" || event_name == "failed" {
                let success = val
                    .get("success")
                    .and_then(|v| v.as_bool())
                    .unwrap_or(event_name == "complete");
                let technique = val
                    .get("technique")
                    .and_then(|v| v.as_str())
                    .unwrap_or("unknown")
                    .to_string();
                let msg = val
                    .get("message")
                    .and_then(|v| v.as_str())
                    .unwrap_or("")
                    .to_string();

                result = Some(IRemovalResult {
                    success,
                    technique,
                    message: msg,
                });
            }
        }
    }

    if !output.status.success() {
        let stderr = String::from_utf8_lossy(&output.stderr).to_string();

        // Emit an error progress event
        let err_progress = IRemovalProgress {
            session_id: session_id.clone(),
            event: "error".to_string(),
            message: stderr.clone(),
            extra: serde_json::Value::Object(serde_json::Map::new()),
        };
        let _ = app.emit("iremoval-progress", err_progress);

        return Err(format!("iremoval_bypass.py exited with error: {stderr}"));
    }

    Ok(result.unwrap_or(IRemovalResult {
        success: false,
        technique: "unknown".to_string(),
        message: "No complete event received from iremoval_bypass.py".to_string(),
    }))
}

/// iServices fix command. Runs iremoval_bypass.py with "iservices" mode (Technique C),
/// streams all JSON lines as "iremoval-progress" Tauri events,
/// and returns an IRemovalResult on completion.
#[tauri::command]
pub async fn iremoval_iservices(
    app: AppHandle,
    session_id: String,
) -> Result<IRemovalResult, String> {
    println!("[COMMAND] iremoval_iservices session_id={}", session_id);

    let script_path = python_path(&app).join("ios_bypass/iremoval_bypass.py");

    // Emit a "starting" progress event immediately
    let start_progress = IRemovalProgress {
        session_id: session_id.clone(),
        event: "starting".to_string(),
        message: "Initialising iServices fix pipeline…".to_string(),
        extra: serde_json::Value::Object(serde_json::Map::new()),
    };
    let _ = app.emit("iremoval-progress", start_progress);

    let output = app
        .shell()
        .command("python3")
        .args([script_path.to_str().unwrap(), "iservices", &session_id])
        .output()
        .await
        .map_err(|e| format!("Failed to spawn iremoval_bypass.py (iservices): {e}"))?;

    let stdout = String::from_utf8_lossy(&output.stdout);

    // Parse every JSON line and emit as progress events
    let mut result: Option<IRemovalResult> = None;
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

            let progress = IRemovalProgress {
                session_id: session_id.clone(),
                event: event_name.clone(),
                message: message.clone(),
                extra: val.clone(),
            };
            let _ = app.emit("iremoval-progress", progress);

            // Capture the final "complete" or "failed" event as the result
            if event_name == "complete" || event_name == "failed" {
                let success = val
                    .get("success")
                    .and_then(|v| v.as_bool())
                    .unwrap_or(event_name == "complete");
                let technique = val
                    .get("technique")
                    .and_then(|v| v.as_str())
                    .unwrap_or("C")
                    .to_string();
                let msg = val
                    .get("message")
                    .and_then(|v| v.as_str())
                    .unwrap_or("")
                    .to_string();

                result = Some(IRemovalResult {
                    success,
                    technique,
                    message: msg,
                });
            }
        }
    }

    if !output.status.success() {
        let stderr = String::from_utf8_lossy(&output.stderr).to_string();

        // Emit an error progress event
        let err_progress = IRemovalProgress {
            session_id: session_id.clone(),
            event: "error".to_string(),
            message: stderr.clone(),
            extra: serde_json::Value::Object(serde_json::Map::new()),
        };
        let _ = app.emit("iremoval-progress", err_progress);

        return Err(format!(
            "iremoval_bypass.py (iservices) exited with error: {stderr}"
        ));
    }

    Ok(result.unwrap_or(IRemovalResult {
        success: false,
        technique: "C".to_string(),
        message: "No complete event received from iremoval_bypass.py (iservices)".to_string(),
    }))
}
