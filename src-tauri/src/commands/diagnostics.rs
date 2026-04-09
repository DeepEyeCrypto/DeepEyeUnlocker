use crate::commands::adb;
use crate::commands::edl;
use crate::commands::mtk_brom;
use serde::Serialize;
use tauri::AppHandle;

#[derive(Debug, Serialize, Clone)]
pub struct HandshakeResult {
    pub protocol: String,
    pub status: String,
    pub chip_info: Option<String>,
    pub latency_ms: u64,
}

#[tauri::command]
pub async fn diag_test_handshake(
    app: AppHandle,
    protocol: String,
) -> Result<HandshakeResult, String> {
    let start = std::time::Instant::now();

    match protocol.as_str() {
        "MtkBrom" => {
            // Non-destructive probe
            match mtk_brom::find_mtk_device() {
                Some(device) => Ok(HandshakeResult {
                    protocol: "MediaTek BROM".into(),
                    status: "CONNECTED".into(),
                    chip_info: Some(format!("VID:{:04x} PID:{:04x}", device.vid, device.pid)),
                    latency_ms: start.elapsed().as_millis() as u64,
                }),
                _ => Err("MTK Device not found in BROM mode".into()),
            }
        }
        "Edl" => match edl::edl_find_device().await {
            Ok(info) => Ok(HandshakeResult {
                protocol: "Qualcomm EDL".into(),
                status: "CONNECTED".into(),
                chip_info: info.serial,
                latency_ms: start.elapsed().as_millis() as u64,
            }),
            Err(e) => Err(format!("EDL Handshake Failed: {}", e)),
        },
        "Adb" => {
            let devices = adb::adb_devices(&app)
                .await
                .map_err(|e| format!("ADB Failed: {}", e))?;

            let target_device = devices
                .iter()
                .find(|device| device.state == "device")
                .or_else(|| devices.first())
                .ok_or_else(|| "ADB device not found".to_string())?;

            match adb::adb_shell(&app, &target_device.serial, "getprop ro.product.model").await {
                Ok(model) => Ok(HandshakeResult {
                    protocol: "ADB".into(),
                    status: "AUTHORIZED".into(),
                    chip_info: Some(format!("{} ({})", model.trim(), target_device.serial)),
                    latency_ms: start.elapsed().as_millis() as u64,
                }),
                Err(e) => Err(format!("ADB Failed: {}", e)),
            }
        }
        _ => Err(format!(
            "Protocol {} not supported for diagnostics",
            protocol
        )),
    }
}
