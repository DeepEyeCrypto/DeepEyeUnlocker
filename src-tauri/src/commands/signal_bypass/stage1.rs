use serde::{Deserialize, Serialize};
use std::time::Duration;
use tauri::AppHandle;
use tauri_plugin_shell::ShellExt;
use tokio::time::sleep;

#[derive(Debug, Serialize, Deserialize)]
pub struct Stage1Result {
    pub udid: String,
    pub model_name: String,
    pub model_id: String,
    pub ios_version: String,
    pub build_version: String,
    pub imei: String,
    pub imei2: Option<String>,
    pub meid: String,
    pub serial_number: String,
    pub ecid: String,
    pub chip: String,
    pub is_a12_plus: bool,
    pub iccid: String,
    pub sim_status_raw: String,
    pub carrier_raw: String,
    pub battery_level: String,
    pub storage_total: String,
    pub wifi_mac: String,
    pub stage_passed: bool,
    pub stage_message: String,
}

/// Helper: run ideviceinfo -k <key> and return trimmed stdout, or fallback.
async fn ideviceinfo_key(app: &AppHandle, key: &str, fallback: &str) -> String {
    let shell = app.shell();
    match shell
        .command("ideviceinfo")
        .args(["-k", key])
        .output()
        .await
    {
        Ok(out) => {
            let val = String::from_utf8_lossy(&out.stdout).trim().to_string();
            if val.is_empty() {
                fallback.to_string()
            } else {
                val
            }
        }
        Err(_) => fallback.to_string(),
    }
}

#[tauri::command]
pub async fn signal_stage1_detect(app: AppHandle) -> Result<Stage1Result, String> {
    sleep(Duration::from_millis(400)).await;

    let shell = app.shell();

    // 1. Get UDID
    let udid = match shell.command("idevice_id").args(["-l"]).output().await {
        Ok(out) => {
            let stdout = String::from_utf8_lossy(&out.stdout).to_string();
            stdout.lines().next().unwrap_or("").trim().to_string()
        }
        Err(_) => String::new(),
    };

    if udid.is_empty() {
        return Err(
            "No Apple device detected via USB. Connect device and trust this computer.".to_string(),
        );
    }

    // 2. Pull device info via ideviceinfo
    let model_id = ideviceinfo_key(&app, "ProductType", "Unknown").await;
    let model_name = ideviceinfo_key(&app, "MarketingName", &model_id).await;
    let ios_version = ideviceinfo_key(&app, "ProductVersion", "Unknown").await;
    let build_version = ideviceinfo_key(&app, "BuildVersion", "Unknown").await;
    let serial_number = ideviceinfo_key(&app, "SerialNumber", "Unknown").await;
    let chip = ideviceinfo_key(&app, "CPUArchitecture", "Unknown").await;
    let imei = ideviceinfo_key(&app, "InternationalMobileEquipmentIdentity", "N/A").await;
    let imei2 = {
        let v = ideviceinfo_key(&app, "InternationalMobileEquipmentIdentity2", "").await;
        if v.is_empty() {
            None
        } else {
            Some(v)
        }
    };
    let meid = ideviceinfo_key(&app, "MobileEquipmentIdentifier", "N/A").await;
    let ecid = ideviceinfo_key(&app, "UniqueChipID", "N/A").await;
    let iccid = ideviceinfo_key(&app, "IntegratedCircuitCardIdentity", "N/A").await;
    let sim_status_raw = ideviceinfo_key(&app, "SIMStatus", "Unknown").await;
    let carrier_raw = ideviceinfo_key(&app, "CarrierBundleInfoArray", "Unknown").await;
    let battery_level = ideviceinfo_key(&app, "BatteryCurrentCapacity", "N/A").await;
    let storage_total = ideviceinfo_key(&app, "TotalDiskCapacity", "N/A").await;
    let wifi_mac = ideviceinfo_key(&app, "WiFiAddress", "N/A").await;

    // 3. Determine A12+ status from chip architecture
    let is_a12_plus = chip.contains("arm64e");

    let stage_passed = !udid.is_empty() && imei != "N/A";
    let stage_message = if stage_passed {
        format!("✅ Device detected: {} ({})", model_name, ios_version)
    } else {
        "⛔ Device detected but IMEI not readable. Check USB trust.".to_string()
    };

    Ok(Stage1Result {
        udid,
        model_name,
        model_id,
        ios_version,
        build_version,
        imei,
        imei2,
        meid,
        serial_number,
        ecid,
        chip,
        is_a12_plus,
        iccid,
        sim_status_raw,
        carrier_raw,
        battery_level,
        storage_total,
        wifi_mac,
        stage_passed,
        stage_message,
    })
}
