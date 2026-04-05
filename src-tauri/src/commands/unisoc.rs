use serde::Serialize;
#[cfg(target_os = "macos")]
use serde_json::Value;
use tauri::AppHandle;
#[cfg(target_os = "macos")]
use tauri_plugin_shell::process::CommandEvent;
#[cfg(target_os = "macos")]
use tauri_plugin_shell::ShellExt;

const UNISOC_VENDOR_ID: &str = "0x1782";
const UNISOC_PRODUCT_ID: &str = "0x4d00";
const UNISOC_TRANSPORT: &str = "Research Download";

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct UnisocDeviceInfo {
    pub detected: bool,
    pub vendor_id: String,
    pub product_id: String,
    pub mode: String,
    pub transport: String,
    pub product_name: String,
    pub location_id: String,
    pub service_guidance: String,
}

impl UnisocDeviceInfo {
    fn not_detected(mode: &str, service_guidance: &str) -> Self {
        Self {
            detected: false,
            vendor_id: UNISOC_VENDOR_ID.to_string(),
            product_id: UNISOC_PRODUCT_ID.to_string(),
            mode: mode.to_string(),
            transport: UNISOC_TRANSPORT.to_string(),
            product_name: String::new(),
            location_id: String::new(),
            service_guidance: service_guidance.to_string(),
        }
    }
}

#[cfg(target_os = "macos")]
fn contains_usb_id(value: &str, expected: &str) -> bool {
    value.to_ascii_lowercase().contains(expected)
}

#[cfg(target_os = "macos")]
fn get_object_string(entry: &Value, key: &str) -> String {
    entry
        .get(key)
        .and_then(Value::as_str)
        .map(str::trim)
        .unwrap_or_default()
        .to_string()
}

#[cfg(target_os = "macos")]
fn find_unisoc_usb_entry(value: &Value) -> Option<&Value> {
    match value {
        Value::Object(map) => {
            let vendor_id = map
                .get("vendor_id")
                .and_then(Value::as_str)
                .unwrap_or_default();
            let product_id = map
                .get("product_id")
                .and_then(Value::as_str)
                .unwrap_or_default();

            if contains_usb_id(vendor_id, UNISOC_VENDOR_ID)
                && contains_usb_id(product_id, UNISOC_PRODUCT_ID)
            {
                return Some(value);
            }

            map.values().find_map(find_unisoc_usb_entry)
        }
        Value::Array(items) => items.iter().find_map(find_unisoc_usb_entry),
        _ => None,
    }
}

#[cfg(target_os = "macos")]
async fn capture_system_profiler_usb(app: &AppHandle) -> Result<String, String> {
    // [INFERRED] macOS `system_profiler SPUSBDataType -json` exposes USB VID/PID metadata suitable for read-only transport detection.
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command("system_profiler")
        .args(["SPUSBDataType", "-json"])
        .spawn()
        .map_err(|e| format!("system_profiler spawn error: {e}"))?;

    let mut out = String::new();
    let mut err = String::new();

    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(bytes) => out.push_str(&String::from_utf8_lossy(&bytes)),
            CommandEvent::Stderr(bytes) => err.push_str(&String::from_utf8_lossy(&bytes)),
            CommandEvent::Error(error) => return Err(error),
            CommandEvent::Terminated(status) => {
                if status.code.unwrap_or(-1) != 0 {
                    return Err(format!(
                        "system_profiler exited {:?}\nstderr: {err}",
                        status.code
                    ));
                }
                break;
            }
            _ => {}
        }
    }

    Ok(out)
}

#[tauri::command]
pub async fn unisoc_detect_device(app: AppHandle) -> Result<UnisocDeviceInfo, String> {
    // [INFERRED] This command is intentionally read-only: it detects Unisoc Research Download presence and returns service guidance without auth or partition operations.
    #[cfg(target_os = "macos")]
    {
        let output = capture_system_profiler_usb(&app).await?;
        let parsed: Value = serde_json::from_str(&output)
            .map_err(|e| format!("system_profiler parse error: {e}"))?;

        if let Some(entry) = find_unisoc_usb_entry(&parsed) {
            let product_name = get_object_string(entry, "_name");
            let location_id = get_object_string(entry, "location_id");

            return Ok(UnisocDeviceInfo {
                detected: true,
                vendor_id: UNISOC_VENDOR_ID.to_string(),
                product_id: UNISOC_PRODUCT_ID.to_string(),
                mode: "research_download".to_string(),
                transport: UNISOC_TRANSPORT.to_string(),
                product_name: if product_name.is_empty() {
                    "Unisoc / Spreadtrum device".to_string()
                } else {
                    product_name
                },
                location_id,
                service_guidance:
                    "Read-only detection only. Auth, erase, and partition commands are intentionally not exposed in this build."
                        .to_string(),
            });
        }

        Ok(UnisocDeviceInfo::not_detected(
            "not_detected",
            "No Unisoc Research Download device with VID 0x1782 PID 0x4D00 was found on the host USB bus.",
        ))
    }

    #[cfg(not(target_os = "macos"))]
    {
        let _ = app;
        Ok(UnisocDeviceInfo::not_detected(
            "unsupported_host",
            "Read-only Unisoc USB detection is currently implemented for macOS hosts only.",
        ))
    }
}
