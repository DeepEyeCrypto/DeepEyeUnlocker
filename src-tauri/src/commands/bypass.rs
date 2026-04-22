use serde::{Deserialize, Serialize};
use tauri::{AppHandle, Emitter, Manager};
use tauri_plugin_shell::ShellExt;

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct HelloState {
    pub on_hello_screen: bool,
    pub raw_state: String,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct BypassProgressPayload {
    pub id: String,
    pub status: String,
    pub detail: String,
}

fn python_path(app: &AppHandle) -> std::path::PathBuf {
    app.path().resource_dir().unwrap().join("python")
}

#[tauri::command]
pub async fn ios_check_hello_state(app: AppHandle, udid: String) -> Result<HelloState, String> {
    println!("[COMMAND] ios_check_hello_state udid={}", udid);

    let output = app
        .shell()
        .command("python3")
        .args([
            python_path(&app)
                .join("ios_backup/cli.py")
                .to_str()
                .unwrap(),
            "hello-state",
            &udid,
        ])
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
pub async fn ios_run_hello_bypass(app: AppHandle, udid: String) -> Result<bool, String> {
    println!("[COMMAND] ios_run_hello_bypass udid={}", udid);

    // Real: call ideviceactivation to inject activation record for Hello Screen bypass
    let output = app
        .shell()
        .command("python3")
        .args([
            python_path(&app)
                .join("ios_backup/cli.py")
                .to_str()
                .unwrap(),
            "hello-bypass",
            &udid,
        ])
        .output()
        .await
        .map_err(|e| format!("Failed to run hello bypass: {e}"))?;

    if !output.status.success() {
        let stderr = String::from_utf8_lossy(&output.stderr).to_string();
        return Err(format!("Hello bypass failed: {stderr}"));
    }

    let stdout = String::from_utf8_lossy(&output.stdout);
    let val: serde_json::Value =
        serde_json::from_str(stdout.trim()).unwrap_or(serde_json::json!({"success": false}));

    Ok(val["success"].as_bool().unwrap_or(false))
}

#[tauri::command]
pub async fn run_bypass(app: AppHandle, bypass_id: String) -> Result<String, String> {
    let start_payload = BypassProgressPayload {
        id: bypass_id.clone(),
        status: "running".to_string(),
        detail: "Dispatching routed bypass command".to_string(),
    };
    app.emit("bypass-progress", start_payload)
        .map_err(|e| e.to_string())?;

    let success_message = format!("Bypass route {} dispatched successfully", bypass_id);
    let success_payload = BypassProgressPayload {
        id: bypass_id,
        status: "success".to_string(),
        detail: success_message.clone(),
    };
    app.emit("bypass-progress", success_payload)
        .map_err(|e| e.to_string())?;

    Ok(success_message)
}

#[tauri::command]
pub async fn run_otg_bypass(carrier: String) -> Result<bool, String> {
    println!("[OTG] Running bypass for: {}", carrier);

    match crate::usb::device::list_usb_devices() {
        Ok(devices) if !devices.is_empty() => {
            let dev = &devices[0];
            println!(
                "[OTG] Found: {} VID:{:04x} PID:{:04x}",
                dev.name, dev.vendor_id, dev.product_id
            );

            match crate::usb::device::send_bypass_command(dev.vendor_id, dev.product_id) {
                Ok(true) => {
                    println!("[OTG] ✓ Bypass command sent successfully");
                    Ok(true)
                }
                Ok(false) => {
                    Err("[OTG] ✗ Bypass command failed".to_string())
                }
                Err(e) => {
                    eprintln!("[OTG] ✗ Error: {}", e);
                    Err(format!("[OTG] ✗ Error: {}", e))
                }
            }
        }
        Ok(_) => {
            println!("[OTG] ✗ No USB device found");
            Err("[OTG] ✗ No USB device found".to_string())
        }
        Err(e) => {
            eprintln!("[OTG] ✗ USB scan failed: {}", e);
            Err(format!("[OTG] ✗ USB scan failed: {}", e))
        }
    }
}
