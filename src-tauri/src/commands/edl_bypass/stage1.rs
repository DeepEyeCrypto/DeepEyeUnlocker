use serde::{Deserialize, Serialize};
use tauri::{command, AppHandle, Emitter};

use crate::commands::edl_bypass::shared::*;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct QcomDevice {
    pub vid: String,
    pub pid: String,
    pub port: String,
    pub device_name: String,
    pub is_edl_mode: bool,
    pub is_fastboot: bool,
    pub is_adb: bool,
    pub chipset_hint: String,
    pub serial: String,
    pub brand_hint: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EdlStage1Result {
    pub devices: Vec<QcomDevice>,
    pub selected_serial: String,
    pub edl_count: usize,
    pub fastboot_count: usize,
    pub adb_count: usize,
    pub qdl_available: bool,
    pub edl_tool_available: bool,
    pub adb_available: bool,
    pub fastboot_available: bool,
    pub edl_prog_found: bool,
    pub firehose_path: String,
    pub how_to_edl: Vec<String>,
    pub stage_passed: bool,
    pub stage_message: String,
}

fn chip_hint(lo: &str) -> (&'static str, &'static str) {
    if lo.contains("sm8650") || lo.contains("8gen3") {
        ("SM8650 (8 Gen3)", "S24/OPlus12")
    } else if lo.contains("sm8550") || lo.contains("8gen2") {
        ("SM8550 (8 Gen2)", "S23/Zenfone10")
    } else if lo.contains("sm8450") || lo.contains("8gen1") {
        ("SM8450 (8 Gen1)", "S22/OPlus10")
    } else if lo.contains("sm8350") {
        ("SM8350 (888)", "S21/OPlus9/Mi11")
    } else if lo.contains("sm8250") {
        ("SM8250 (865)", "S20/OPlus8")
    } else if lo.contains("sm8150") {
        ("SM8150 (855)", "S10/OPlus7")
    } else if lo.contains("sm7550") {
        ("SM7550 (7 Gen2)", "A54/OPlus Nord3")
    } else if lo.contains("sm7325") {
        ("SM7325 (778G)", "A52s/Nord2T")
    } else if lo.contains("sm6375") {
        ("SM6375 (695)", "A33/Nord CE2")
    } else if lo.contains("sm6225") {
        ("SM6225 (680)", "A23/Redmi10C")
    } else {
        ("Qualcomm (generic)", "Unknown")
    }
}

fn parse_usb(raw: &str) -> Vec<QcomDevice> {
    let mut devices = Vec::new();

    for line in raw.lines() {
        let lo = line.to_lowercase();
        if !lo.contains("qualcomm")
            && !lo.contains("05c6")
            && !lo.contains("9008")
            && !lo.contains("9006")
            && !lo.contains("qdl")
        {
            continue;
        }

        let pid = if lo.contains("9008") {
            "9008"
        } else if lo.contains("9006") {
            "9006"
        } else {
            "unknown"
        };

        let (chipset_hint, brand_hint) = chip_hint(&lo);
        devices.push(QcomDevice {
            vid: "05c6".to_string(),
            pid: pid.to_string(),
            port: "usb".to_string(),
            device_name: line.trim().chars().take(50).collect(),
            is_edl_mode: pid == "9008",
            is_fastboot: pid == "9006",
            is_adb: false,
            chipset_hint: chipset_hint.to_string(),
            serial: String::new(),
            brand_hint: brand_hint.to_string(),
        });
    }

    devices
}

#[command]
pub async fn edl_stage1_detect(app: AppHandle) -> Result<EdlStage1Result, String> {
    macro_rules! log {
        ($msg:expr) => {
            let _ = app.emit("edl-s1", $msg.to_string());
        };
        ($fmt:literal, $($args:tt)*) => {
            let _ = app.emit("edl-s1", format!($fmt, $($args)*));
        };
    }

    log!("╔════════════════════════════════════╗");
    log!("║  EDL STAGE 1/20 — DEVICE DETECT    ║");
    log!("╚════════════════════════════════════╝");
    log!("");

    log!("🔧 Checking tools...");
    let (qdl_ok, _) = run_cmd("which", &["qdl"]);
    let (edl_ok, _) = run_cmd("which", &["edl"]);
    let (adb_ok, _) = run_cmd("which", &["adb"]);
    let (fb_ok, _) = run_cmd("which", &["fastboot"]);

    log!("   qdl:      {}", if qdl_ok { "✅" } else { "⚠️" });
    log!("   edl:      {}", if edl_ok { "✅" } else { "⚠️" });
    log!("   adb:      {}", if adb_ok { "✅" } else { "⚠️" });
    log!("   fastboot: {}", if fb_ok { "✅" } else { "⚠️" });

    let firehose = find_firehose();
    let firehose_found = firehose.is_some();
    let firehose_path = firehose.unwrap_or_default();
    log!(
        "   firehose: {}",
        if firehose_found {
            format!("✅ {firehose_path}")
        } else {
            "⚠️ not found".to_string()
        }
    );

    log!("");
    log!("🔍 Scanning USB bus...");
    let (_, sp) = run_cmd(
        "system_profiler",
        &["SPUSBDataType", "-detailLevel", "mini"],
    );
    let (_, io) = run_cmd("ioreg", &["-p", "IOUSB", "-l", "-w", "0"]);
    let mut devices = parse_usb(&format!("{sp}\n{io}"));

    let (_, adb_out) = run_cmd("adb", &["devices"]);
    let mut adb_count = 0usize;
    for line in adb_out.lines() {
        if line.contains('\t') && line.contains("device") {
            adb_count += 1;
            let serial = line.split('\t').next().unwrap_or("unknown");
            devices.push(QcomDevice {
                vid: "unknown".to_string(),
                pid: "adb".to_string(),
                port: serial.to_string(),
                device_name: format!("ADB: {serial}"),
                is_edl_mode: false,
                is_fastboot: false,
                is_adb: true,
                chipset_hint: "Android (ADB)".to_string(),
                serial: serial.to_string(),
                brand_hint: "Unknown".to_string(),
            });
            log!("   🔵 ADB: {serial}");
        }
    }

    let (_, fb_out) = run_cmd("fastboot", &["devices"]);
    let mut fastboot_count = 0usize;
    for line in fb_out.lines() {
        if line.is_empty() || line.starts_with('<') || line.contains("waiting") {
            continue;
        }

        fastboot_count += 1;
        let serial = line.split_whitespace().next().unwrap_or("unknown");
        devices.push(QcomDevice {
            vid: "unknown".to_string(),
            pid: "fastboot".to_string(),
            port: serial.to_string(),
            device_name: format!("Fastboot: {serial}"),
            is_edl_mode: false,
            is_fastboot: true,
            is_adb: false,
            chipset_hint: "Android (Fastboot)".to_string(),
            serial: serial.to_string(),
            brand_hint: "Unknown".to_string(),
        });
        log!("   🟡 Fastboot: {serial}");
    }

    if adb_count > 0 && devices.iter().all(|device| !device.is_edl_mode) {
        log!("");
        log!("📡 ADB found → rebooting to EDL...");
        let _ = run_cmd("adb", &["reboot", "edl"]);
        log!("   ⏳ Waiting 5s for 9008 mode...");
        wait_secs(5);
        let (_, sp_retry) = run_cmd(
            "system_profiler",
            &["SPUSBDataType", "-detailLevel", "mini"],
        );
        let new_devices = parse_usb(&sp_retry);
        if !new_devices.is_empty() {
            log!("   ✅ Device entered EDL 9008!");
        }
        devices.extend(new_devices);
    }

    if fastboot_count > 0 && devices.iter().all(|device| !device.is_edl_mode) {
        log!("");
        log!("⚡ Fastboot → EDL attempt...");
        let _ = run_cmd("fastboot", &["oem", "edl"]);
        wait_secs(3);
    }

    let edl_count = devices.iter().filter(|device| device.is_edl_mode).count();
    let fb_count = devices.iter().filter(|device| device.is_fastboot).count();
    let adb_found_count = devices.iter().filter(|device| device.is_adb).count();

    log!("");
    log!("   EDL 9008: {edl_count} device(s)");
    log!("   Fastboot: {fb_count} device(s)");
    log!("   ADB:      {adb_found_count} device(s)");

    for device in &devices {
        log!(
            "   → [{}] {} — {} ({})",
            device.pid,
            device.chipset_hint,
            device.brand_hint,
            device.serial
        );
    }

    let selected_serial = devices
        .iter()
        .find(|device| device.is_edl_mode)
        .or_else(|| devices.iter().find(|device| device.is_fastboot))
        .or_else(|| devices.iter().find(|device| device.is_adb))
        .or_else(|| devices.first())
        .map(|device| device.serial.clone())
        .unwrap_or_default();

    let stage_passed = edl_count > 0 || fb_count > 0 || adb_found_count > 0;
    let stage_message = if edl_count > 0 {
        format!("✅ {edl_count} device(s) in EDL 9008. → Stage 2: Sahara Handshake")
    } else if fb_count > 0 {
        "⚠️ Fastboot mode. Try: fastboot oem edl".to_string()
    } else if adb_found_count > 0 {
        "⚠️ ADB found. Sent reboot edl — reconnect in 9008 mode.".to_string()
    } else {
        "⚠️ No device. Enter EDL: Vol↓+Power or test point.".to_string()
    };

    let how_to_edl = vec![
        "1. adb reboot edl".to_string(),
        "2. fastboot oem edl".to_string(),
        "3. Vol↓ + Power (hold 10s)".to_string(),
        "4. EDL test point (PCB short)".to_string(),
        "5. Deep Flash USB cable".to_string(),
        "6. *#9090# → USB Settings (Samsung)".to_string(),
        "7. Emergency mode → EDL (Xiaomi)".to_string(),
    ];

    log!("{stage_message}");

    Ok(EdlStage1Result {
        devices,
        selected_serial,
        edl_count,
        fastboot_count: fb_count,
        adb_count: adb_found_count,
        qdl_available: qdl_ok || edl_ok,
        edl_tool_available: edl_ok,
        adb_available: adb_ok,
        fastboot_available: fb_ok,
        edl_prog_found: firehose_found,
        firehose_path,
        how_to_edl,
        stage_passed,
        stage_message,
    })
}
