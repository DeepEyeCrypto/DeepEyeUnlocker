use serde::Serialize;
use tauri::AppHandle;
use tauri_plugin_shell::process::CommandEvent;
use tauri_plugin_shell::ShellExt;

use crate::commands::activation::ios_check_activation_state;
use crate::commands::edl::edl_find_device;
use crate::commands::identity::ios_device_identity;
use crate::commands::mtk::mtk_device_info;
use crate::commands::orchestrator::ios_poll_orchestrator;
use crate::commands::unisoc::unisoc_detect_device;

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectedDevice {
    pub id: String,
    pub model: String,
    pub serial: String,
    pub os: String,
    pub mode: String,
    pub bootloader_status: String,
    pub carrier: Option<String>,
    pub source: String,
}

// [INFERRED] Uses host-side command discovery and existing protocol probes to expose only physically detected devices.
async fn collect_command_output(
    app: &AppHandle,
    command: &str,
    args: Vec<String>,
) -> Result<String, String> {
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command(command)
        .args(args)
        .spawn()
        .map_err(|e| format!("spawn error: {e}"))?;

    let mut out = String::new();
    let mut err = String::new();

    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(bytes) => out.push_str(&String::from_utf8_lossy(&bytes)),
            CommandEvent::Stderr(bytes) => err.push_str(&String::from_utf8_lossy(&bytes)),
            CommandEvent::Error(error) => return Err(error),
            CommandEvent::Terminated(status) => {
                if status.code.unwrap_or(-1) != 0 {
                    return Err(format!("exit {:?}\nstderr: {err}", status.code));
                }
                break;
            }
            _ => {}
        }
    }

    Ok(out.trim().to_string())
}

fn extract_adb_field(line: &str, prefix: &str) -> Option<String> {
    line.split_whitespace()
        .find_map(|token| token.strip_prefix(prefix).map(|value| value.replace('_', " ")))
}

async fn adb_getprop(app: &AppHandle, serial: &str, property: &str) -> Option<String> {
    let output = collect_command_output(
        app,
        "adb",
        vec![
            "-s".to_string(),
            serial.to_string(),
            "shell".to_string(),
            "getprop".to_string(),
            property.to_string(),
        ],
    )
    .await
    .ok()?;

    let value = output.trim();
    if value.is_empty() {
        None
    } else {
        Some(value.to_string())
    }
}

fn normalize_bootloader_state(
    vbmeta_state: Option<String>,
    flash_locked: Option<String>,
) -> String {
    if let Some(state) = vbmeta_state {
        let normalized = state.trim().to_lowercase();
        if !normalized.is_empty() {
            return normalized;
        }
    }

    match flash_locked.as_deref().map(str::trim) {
        Some("0") => "unlocked".to_string(),
        Some("1") => "locked".to_string(),
        _ => "Unknown".to_string(),
    }
}

// [INFERRED] `adb devices -l` is the canonical non-destructive probe for attached Android devices on host machines.
async fn detect_adb_devices(app: &AppHandle) -> Vec<ConnectedDevice> {
    let output = match collect_command_output(app, "adb", vec!["devices".to_string(), "-l".to_string()]).await {
        Ok(output) => output,
        Err(_) => return Vec::new(),
    };

    let mut devices = Vec::new();

    for line in output.lines().skip(1) {
        let trimmed = line.trim();
        if trimmed.is_empty() || trimmed.starts_with('*') {
            continue;
        }

        let mut parts = trimmed.split_whitespace();
        let serial = match parts.next() {
            Some(value) => value.to_string(),
            None => continue,
        };
        let state = parts.next().unwrap_or_default().to_string();
        if state.is_empty() {
            continue;
        }

        let model = extract_adb_field(trimmed, "model:")
            .or_else(|| extract_adb_field(trimmed, "device:"))
            .or_else(|| Some("Unknown Android Device".to_string()))
            .unwrap_or_else(|| "Unknown Android Device".to_string());

        let os = if state == "device" {
            adb_getprop(app, &serial, "ro.build.version.release")
                .await
                .map(|value| format!("Android {value}"))
                .unwrap_or_else(|| "Android".to_string())
        } else {
            "Android".to_string()
        };

        let bootloader_status = if state == "device" {
            normalize_bootloader_state(
                adb_getprop(app, &serial, "ro.boot.vbmeta.device_state").await,
                adb_getprop(app, &serial, "ro.boot.flash.locked").await,
            )
        } else {
            "Unknown".to_string()
        };

        let carrier = if state == "device" {
            adb_getprop(app, &serial, "gsm.operator.alpha").await
        } else {
            None
        };

        devices.push(ConnectedDevice {
            id: serial.clone(),
            model,
            serial,
            os,
            mode: if state == "device" {
                "ADB".to_string()
            } else {
                format!("ADB {state}")
            },
            bootloader_status,
            carrier,
            source: "adb".to_string(),
        });
    }

    devices
}

// [INFERRED] This aggregates existing Apple, Qualcomm, MediaTek, and Unisoc probes so the frontend never needs placeholder device data.
#[tauri::command]
pub async fn get_connected_devices(app: AppHandle) -> Result<Vec<ConnectedDevice>, String> {
    let mut devices = detect_adb_devices(&app).await;

    if let Ok(mode) = ios_poll_orchestrator(app.clone()).await {
        let normalized_mode = mode.mode.to_lowercase();
        if !normalized_mode.contains("unknown") && !normalized_mode.contains("unsupported") {
            let activation = ios_check_activation_state(app.clone(), String::new()).await.ok();
            let identity = ios_device_identity(app.clone(), String::new()).await.ok();

            let id = identity
                .as_ref()
                .map(|entry| entry.udid.clone())
                .filter(|value| !value.is_empty())
                .or_else(|| {
                    identity
                        .as_ref()
                        .and_then(|entry| entry.serial.clone())
                })
                .unwrap_or_else(|| "apple-device".to_string());

            let serial = identity
                .as_ref()
                .and_then(|entry| entry.serial.clone())
                .filter(|value| !value.is_empty())
                .unwrap_or_else(|| id.clone());

            let model = activation
                .as_ref()
                .map(|entry| entry.model.clone())
                .filter(|value| !value.is_empty())
                .unwrap_or_else(|| "Apple Device".to_string());

            devices.push(ConnectedDevice {
                id,
                model,
                serial,
                os: "iOS".to_string(),
                mode: mode.mode,
                bootloader_status: "Unknown".to_string(),
                carrier: None,
                source: "apple".to_string(),
            });
        }
    }

    if let Ok(edl) = edl_find_device().await {
        let serial_str = edl.serial.clone().unwrap_or_else(|| "N/A".to_string());
        devices.push(ConnectedDevice {
            id: if serial_str.is_empty() || serial_str == "N/A" {
                "edl-device".to_string()
            } else {
                serial_str.clone()
            },
            model: "Qualcomm Device".to_string(), // In pure EDL we don't know the exact chipset until programmer speaks XML
            serial: serial_str,
            os: "Android".to_string(),
            mode: if edl.programmer_loaded { "EDL Programmable".to_string() } else { "EDL 9008".to_string() },
            bootloader_status: "Unknown".to_string(),
            carrier: None,
            source: "edl".to_string(),
        });
    }

    if let Ok(mtk_info) = mtk_device_info(app.clone()).await {
        let summary = mtk_info
            .lines()
            .find(|line| !line.trim().is_empty())
            .unwrap_or("MediaTek Device")
            .trim()
            .to_string();

        if !summary.is_empty() {
            devices.push(ConnectedDevice {
                id: format!("mtk-{}", summary.replace(' ', "-").to_lowercase()),
                model: summary,
                serial: "N/A".to_string(),
                os: "Android".to_string(),
                mode: "BROM/DA".to_string(),
                bootloader_status: "Unknown".to_string(),
                carrier: None,
                source: "mtk".to_string(),
            });
        }
    }

    if let Ok(unisoc) = unisoc_detect_device(app).await {
        if unisoc.detected {
            let serial = if unisoc.location_id.is_empty() {
                "N/A".to_string()
            } else {
                unisoc.location_id.clone()
            };

            devices.push(ConnectedDevice {
                id: serial.clone(),
                model: if unisoc.product_name.is_empty() {
                    "Unisoc / Spreadtrum device".to_string()
                } else {
                    unisoc.product_name.clone()
                },
                serial,
                os: "Android".to_string(),
                mode: unisoc.mode,
                bootloader_status: "Unknown".to_string(),
                carrier: None,
                source: "unisoc".to_string(),
            });
        }
    }

    Ok(devices)
}

#[derive(serde::Deserialize)]
struct SupportedDevicesDoc {
    devices: Vec<SupportedDeviceNode>,
}

#[derive(serde::Deserialize)]
struct SupportedDeviceNode {
    brand: String,
}

#[tauri::command]
pub fn get_supported_brands() -> Vec<String> {
    let json_content = include_str!("../../../src/assets/supported_devices.json");
    if let Ok(doc) = serde_json::from_str::<SupportedDevicesDoc>(json_content) {
        let mut brands: Vec<String> = doc.devices.into_iter().map(|d| d.brand).collect();
        brands.sort();
        brands.dedup();
        brands
    } else {
        vec![]
    }
}
