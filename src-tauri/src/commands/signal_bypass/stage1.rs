use serde::{Deserialize, Serialize};
use tauri::{AppHandle, Emitter};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Stage1Result {
    // Identity
    pub udid: String,
    pub model_id: String,      // "iPhone14,2"
    pub model_name: String,    // "iPhone 13 Pro"
    pub ios_version: String,   // "17.4.1"
    pub build_version: String, // "21E236"

    // IMEI
    pub imei: String,
    pub imei2: Option<String>, // dual SIM
    pub meid: String,
    pub serial_number: String,
    pub ecid: String, // for SHSH2

    // Chip
    pub chip: String,      // "A15 Bionic"
    pub chip_id: String,   // raw chip identifier
    pub is_a12_plus: bool, // gating flag

    // SIM basic read (full analysis in Stage 3)
    pub iccid: String,
    pub sim_status_raw: String,
    pub carrier_raw: String,

    // Health
    pub battery_level: String,
    pub storage_total: String,
    pub wifi_mac: String,

    // Stage result
    pub stage_passed: bool,
    pub stage_message: String,
}

fn path_env() -> String {
    let base = std::env::var("PATH").unwrap_or_else(|_| "/usr/bin:/bin".to_string());
    format!("/usr/local/bin:/opt/homebrew/bin:{base}")
}

fn iinfo(udid: &str, key: &str) -> String {
    let raw = std::process::Command::new("ideviceinfo")
        .env("PATH", path_env())
        .args(["-u", udid, "-k", key])
        .output()
        .map(|o| String::from_utf8_lossy(&o.stdout).trim().to_string())
        .unwrap_or_default();

    if raw.is_empty() {
        "N/A".to_string()
    } else {
        raw
    }
}

fn model_to_name_chip(id: &str) -> (&'static str, &'static str, bool) {
    // Returns (name, chip, is_a12_plus)
    match id {
        // A11 — NOT supported
        "iPhone10,1" | "iPhone10,4" => ("iPhone 8", "A11 Bionic", false),
        "iPhone10,2" | "iPhone10,5" => ("iPhone 8 Plus", "A11 Bionic", false),
        "iPhone10,3" | "iPhone10,6" => ("iPhone X", "A11 Bionic", false),

        // A12 ✅
        "iPhone11,2" => ("iPhone XS", "A12 Bionic", true),
        "iPhone11,4" | "iPhone11,6" => ("iPhone XS Max", "A12 Bionic", true),
        "iPhone11,8" => ("iPhone XR", "A12 Bionic", true),

        // A13 ✅
        "iPhone12,1" => ("iPhone 11", "A13 Bionic", true),
        "iPhone12,3" => ("iPhone 11 Pro", "A13 Bionic", true),
        "iPhone12,5" => ("iPhone 11 Pro Max", "A13 Bionic", true),

        // A14 ✅
        "iPhone13,1" => ("iPhone 12 Mini", "A14 Bionic", true),
        "iPhone13,2" => ("iPhone 12", "A14 Bionic", true),
        "iPhone13,3" => ("iPhone 12 Pro", "A14 Bionic", true),
        "iPhone13,4" => ("iPhone 12 Pro Max", "A14 Bionic", true),

        // A15 ✅
        "iPhone14,4" => ("iPhone 13 Mini", "A15 Bionic", true),
        "iPhone14,5" => ("iPhone 13", "A15 Bionic", true),
        "iPhone14,2" => ("iPhone 13 Pro", "A15 Bionic", true),
        "iPhone14,3" => ("iPhone 13 Pro Max", "A15 Bionic", true),
        "iPhone14,7" => ("iPhone 14", "A15 Bionic", true),
        "iPhone14,8" => ("iPhone 14 Plus", "A15 Bionic", true),

        // A16 ✅
        "iPhone15,2" => ("iPhone 14 Pro", "A16 Bionic", true),
        "iPhone15,3" => ("iPhone 14 Pro Max", "A16 Bionic", true),
        "iPhone15,4" => ("iPhone 15", "A16 Bionic", true),
        "iPhone15,5" => ("iPhone 15 Plus", "A16 Bionic", true),

        // A17 Pro ✅
        "iPhone16,1" => ("iPhone 15 Pro", "A17 Pro", true),
        "iPhone16,2" => ("iPhone 15 Pro Max", "A17 Pro", true),

        // A18 ✅
        "iPhone17,1" => ("iPhone 16 Pro", "A18 Pro", true),
        "iPhone17,2" => ("iPhone 16 Pro Max", "A18 Pro", true),
        "iPhone17,3" => ("iPhone 16", "A18", true),
        "iPhone17,4" => ("iPhone 16 Plus", "A18", true),

        _ => ("Unknown Model", "Unknown", false),
    }
}

#[tauri::command]
pub async fn signal_stage1_detect(app: AppHandle) -> Result<Stage1Result, String> {
    // ── Log helper ─────────────────────────────────
    macro_rules! slog {
        ($msg:expr) => {
            let _ = app.emit("s1-log", $msg.to_string());
        };
        ($fmt:literal, $($arg:tt)*) => {
            let _ = app.emit("s1-log", format!($fmt, $($arg)*));
        };
    }

    slog!("╔══════════════════════════════════╗");
    slog!("║   STAGE 1 — DEVICE DETECTION     ║");
    slog!("╚══════════════════════════════════╝");
    slog!("");
    slog!("🔍 Searching for connected iPhone...");

    // ── 1. Detect UDID via idevice_id ─────────────
    let idevice_out = std::process::Command::new("idevice_id")
        .env("PATH", path_env())
        .arg("-l")
        .output()
        .map_err(|e| {
            format!(
                "❌ idevice_id not found: {e}\n\n\
                 💡 Fix: brew install libimobiledevice\n\
                 💡 Then reconnect iPhone"
            )
        })?;

    let stdout = String::from_utf8_lossy(&idevice_out.stdout);
    let udid = stdout
        .trim()
        .lines()
        .find(|l| l.len() >= 24)
        .ok_or_else(|| {
            "❌ No iPhone detected via USB.\n\n\
             📋 Checklist:\n\
             1️⃣  Connect iPhone with data cable\n\
             2️⃣  Unlock iPhone screen\n\
             3️⃣  Tap 'Trust This Computer' popup\n\
             4️⃣  iOS 16+: Settings → Privacy → Developer Mode → ON\n\
             5️⃣  Try different USB port or cable"
                .to_string()
        })?
        .trim()
        .to_string();

    slog!("✅ iPhone detected!");
    slog!("   UDID: {}...", &udid[..udid.len().min(12)]);

    // ── 2. Read model identity ─────────────────────
    slog!("");
    slog!("📱 Reading device identity...");

    let model_id = iinfo(&udid, "ProductType");
    let ios_version = iinfo(&udid, "ProductVersion");
    let build_version = iinfo(&udid, "BuildVersion");
    let serial_number = iinfo(&udid, "SerialNumber");

    let (model_name, chip, is_a12_plus) = model_to_name_chip(&model_id);

    slog!("   Model:   {} ({})", model_name, model_id);
    slog!("   iOS:     {} ({})", ios_version, build_version);
    slog!("   Chip:    {}", chip);
    slog!("   Serial:  {}", serial_number);

    // ── 3. Read IMEI / MEID / ECID ────────────────
    slog!("");
    slog!("📡 Reading IMEI and identifiers...");

    // Primary IMEI
    let imei = {
        let v = iinfo(&udid, "InternationalMobileEquipmentIdentity");
        if v == "N/A" {
            iinfo(&udid, "MobileEquipmentIdentifier")
        } else {
            v
        }
    };

    // IMEI2 (dual SIM models)
    let imei2 = {
        let v = iinfo(&udid, "InternationalMobileEquipmentIdentity2");
        if v == "N/A" || v.is_empty() {
            None
        } else {
            Some(v)
        }
    };

    let meid = iinfo(&udid, "MobileEquipmentIdentifier");
    let ecid = iinfo(&udid, "UniqueChipID");

    slog!("   IMEI:  {}", imei);
    if let Some(ref i2) = imei2 {
        slog!("   IMEI2: {}", i2);
    }
    slog!("   ECID:  {}", ecid);

    // ── 4. Quick SIM read (deep in Stage 3) ───────
    slog!("");
    slog!("📶 Quick SIM status read...");

    let iccid = iinfo(&udid, "IntegratedCircuitCardIdentity");
    let sim_status_raw = iinfo(&udid, "SIMStatus");
    let carrier_raw = iinfo(&udid, "CarrierName");

    slog!("   SIM:     {}", sim_status_raw);
    slog!("   ICCID:   {}", iccid);
    slog!("   Carrier: {}", carrier_raw);

    // ── 5. Device health ───────────────────────────
    slog!("");
    slog!("🔋 Reading device health...");

    let battery_raw = iinfo(&udid, "BatteryCurrentCapacity");
    let battery_level = if battery_raw == "N/A" {
        "N/A".to_string()
    } else {
        format!("{}%", battery_raw)
    };

    let storage_raw = iinfo(&udid, "TotalDiskCapacity");
    let storage_total = storage_raw
        .parse::<u64>()
        .map(|b| format!("{:.0} GB", b as f64 / 1_073_741_824.0))
        .unwrap_or(storage_raw);

    let wifi_mac = iinfo(&udid, "WiFiAddress");

    slog!("   Battery: {}", battery_level);
    slog!("   Storage: {}", storage_total);

    // ── 6. A12+ gate check ────────────────────────
    slog!("");
    let (stage_passed, stage_message) = if is_a12_plus {
        slog!("╔══════════════════════════════════╗");
        slog!("║  ✅  STAGE 1 PASSED              ║");
        slog!("║  Device supported for Signal+    ║");
        slog!("╚══════════════════════════════════╝");
        (
            true,
            format!("{model_name} with {chip} — Signal+ supported ✅"),
        )
    } else {
        slog!("╔══════════════════════════════════╗");
        slog!("║  ⛔  STAGE 1 FAILED              ║");
        slog!("║  Pre-A12 not supported           ║");
        slog!("╚══════════════════════════════════╝");
        (
            false,
            format!("{model_name} with {chip} — Requires A12 or newer ⛔"),
        )
    };

    Ok(Stage1Result {
        udid,
        model_id: model_id.clone(),
        model_name: model_name.to_string(),
        ios_version,
        build_version,
        imei,
        imei2,
        meid,
        serial_number,
        ecid,
        chip: chip.to_string(),
        chip_id: model_id,
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
