use serde::{Deserialize, Serialize};
use tauri::{AppHandle, Emitter, Manager};
use tauri_plugin_shell::process::CommandEvent;
use tauri_plugin_shell::ShellExt;

#[derive(Debug, Serialize, Deserialize, Clone)]
pub enum DeviceMode {
    Normal,
    Recovery,
    #[serde(rename = "DFU")]
    Dfu,
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

/// Parse a hex or decimal value like "0x8020" or "32800" from irecovery output
fn parse_hex_or_dec(s: &str) -> Option<u32> {
    let trimmed = s.trim();
    if let Some(hex) = trimmed.strip_prefix("0x").or_else(|| trimmed.strip_prefix("0X")) {
        u32::from_str_radix(hex, 16).ok()
    } else {
        trimmed.parse::<u32>().ok()
    }
}

#[tauri::command]
pub async fn ios_detect_dfu_state(app: AppHandle) -> Result<DfuState, String> {
    println!("[COMMAND] ios_detect_dfu_state");

    // Try irecovery -q first for real device state detection
    let irecovery_result = app
        .shell()
        .command("irecovery")
        .args(["-q"])
        .output()
        .await;

    if let Ok(output) = irecovery_result {
        if output.status.success() {
            let stdout = String::from_utf8_lossy(&output.stdout).to_string();

            // Parse fields from irecovery -q output
            let mut ecid: Option<String> = None;
            let mut chip_id: Option<u32> = None;
            let mut board_id: Option<u32> = None;
            let mut mode = DeviceMode::Unknown;

            for line in stdout.lines() {
                let line_lower = line.to_lowercase();
                if let Some(val) = line.split(':').nth(1) {
                    let val = val.trim();
                    if line_lower.contains("ecid") {
                        ecid = Some(val.to_string());
                    } else if line_lower.contains("cpid") || line_lower.contains("chip id") {
                        chip_id = parse_hex_or_dec(val);
                    } else if line_lower.contains("bdid") || line_lower.contains("board id") {
                        board_id = parse_hex_or_dec(val);
                    } else if line_lower.contains("mode") {
                        mode = match val.to_lowercase().as_str() {
                            "dfu" | "wtr dfu" => DeviceMode::Dfu,
                            "recovery" | "recovery mode" => DeviceMode::Recovery,
                            "normal" => DeviceMode::Normal,
                            "restore" => DeviceMode::Restore,
                            _ => DeviceMode::Unknown,
                        };
                    }
                }
            }

            return Ok(DfuState {
                mode,
                ecid,
                chip_id,
                board_id,
            });
        }
    }

    // Fallback to python helper if irecovery is not available
    let output = app
        .shell()
        .command("python3")
        .args([
            python_path(&app)
                .join("ios_backup/dfu.py")
                .to_str()
                .unwrap(),
            "dfu-state",
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
        "dfu" => DeviceMode::Dfu,
        "restore" => DeviceMode::Restore,
        _ => DeviceMode::Unknown,
    };

    Ok(DfuState {
        mode,
        ecid: val["ecid"].as_str().map(|s| s.to_string()),
        chip_id: val["chip_id"].as_u64().map(|v| v as u32),
        board_id: val["board_id"].as_u64().map(|v| v as u32),
    })
}

#[tauri::command]
pub async fn ios_enter_dfu(app: AppHandle, udid: String) -> Result<(), String> {
    println!("[COMMAND] ios_enter_dfu udid={}", udid);

    let output = app
        .shell()
        .command("ideviceenterrecovery")
        .args([&udid])
        .output()
        .await
        .map_err(|e| e.to_string())?;

    if output.status.success() {
        app.emit(
            "dfu-step",
            "Device entering recovery. Manual DFU steps needed next.",
        )
        .unwrap();
        Ok(())
    } else {
        Err(String::from_utf8_lossy(&output.stderr).to_string())
    }
}

#[tauri::command]
pub async fn ios_restore_device(
    app: AppHandle,
    udid: String,
    ipsw_path: String,
) -> Result<(), String> {
    println!(
        "[COMMAND] ios_restore_device udid={} ipsw={}",
        udid, ipsw_path
    );

    let (mut rx, _child) = app
        .shell()
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

/// IPSW download response from ipsw.me API
#[derive(Deserialize)]
#[allow(dead_code)]
struct IpswApiFirmware {
    url: Option<String>,
    version: Option<String>,
    buildid: Option<String>,
    sha1sum: Option<String>,
    signed: Option<bool>,
}

#[tauri::command]
pub async fn ios_download_ipsw(
    app: AppHandle,
    model: String,
    ios_version: String,
) -> Result<String, String> {
    println!(
        "[COMMAND] ios_download_ipsw model={} version={}",
        model, ios_version
    );

    let _ = app.emit(
        "dfu-progress",
        format!("Querying ipsw.me for {} {}...", model, ios_version),
    );

    // Step 1: Query ipsw.me API to find the download URL
    let api_url = format!(
        "https://api.ipsw.me/v4/device/{}?type=ipsw",
        model
    );

    let client = reqwest::Client::builder()
        .user_agent("DeepEyeUnlocker/2027")
        .timeout(std::time::Duration::from_secs(15))
        .build()
        .map_err(|e| format!("HTTP client error: {e}"))?;

    let resp = client
        .get(&api_url)
        .send()
        .await
        .map_err(|e| format!("ipsw.me API error: {e}"))?;

    if !resp.status().is_success() {
        return Err(format!(
            "ipsw.me returned HTTP {}",
            resp.status().as_u16()
        ));
    }

    let body: serde_json::Value = resp
        .json()
        .await
        .map_err(|e| format!("JSON parse error: {e}"))?;

    // Find the matching firmware entry
    let firmwares = body["firmwares"]
        .as_array()
        .ok_or_else(|| "No firmwares found in API response".to_string())?;

    let firmware = firmwares
        .iter()
        .find(|fw| {
            fw["version"].as_str().map(|v| v == ios_version).unwrap_or(false)
                && fw["signed"].as_bool().unwrap_or(false)
        })
        .or_else(|| {
            // Fallback: find any firmware matching the version even if unsigned
            firmwares
                .iter()
                .find(|fw| fw["version"].as_str().map(|v| v == ios_version).unwrap_or(false))
        })
        .ok_or_else(|| {
            format!(
                "No IPSW found for {} version {}",
                model, ios_version
            )
        })?;

    let download_url = firmware["url"]
        .as_str()
        .ok_or_else(|| "IPSW URL not found".to_string())?;

    let sha1sum = firmware["sha1sum"]
        .as_str()
        .unwrap_or("unknown")
        .to_string();

    let dest = format!("/tmp/{}_{}_Restore.ipsw", model, ios_version);

    let _ = app.emit(
        "dfu-progress",
        format!("Downloading IPSW from {}...", download_url),
    );

    // Step 2: Download with progress via streamed response
    let download_resp = client
        .get(download_url)
        .send()
        .await
        .map_err(|e| format!("Download error: {e}"))?;

    let total_size = download_resp.content_length().unwrap_or(0);
    let mut downloaded: u64 = 0;
    let mut last_pct: u64 = 0;

    let mut file = tokio::fs::File::create(&dest)
        .await
        .map_err(|e| format!("Cannot create {}: {e}", dest))?;

    use tokio::io::AsyncWriteExt;
    let mut stream = download_resp.bytes_stream();
    use futures_util::StreamExt;

    while let Some(chunk_result) = stream.next().await {
        let chunk = chunk_result.map_err(|e| format!("Download stream error: {e}"))?;
        file.write_all(&chunk)
            .await
            .map_err(|e| format!("Write error: {e}"))?;

        downloaded += chunk.len() as u64;

        // Emit progress every 5%
        if total_size > 0 {
            let pct = (downloaded * 100) / total_size;
            if pct >= last_pct + 5 {
                last_pct = pct;
                let _ = app.emit(
                    "dfu-progress",
                    format!("Downloading: {}% ({}/{} MB)", pct, downloaded / 1_048_576, total_size / 1_048_576),
                );
            }
        }
    }

    file.flush()
        .await
        .map_err(|e| format!("Flush error: {e}"))?;

    let _ = app.emit(
        "dfu-progress",
        format!(
            "Download complete: {} (SHA1: {})",
            dest, sha1sum
        ),
    );

    Ok(dest)
}
