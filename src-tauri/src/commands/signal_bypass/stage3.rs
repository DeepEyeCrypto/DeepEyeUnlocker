use serde::{Deserialize, Serialize};
use tauri::{AppHandle, Emitter};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Stage3Result {
    pub udid: String,

    // Baseband identity
    pub baseband_version: String,  // e.g. "2.01.00"
    pub baseband_serial: String,
    pub baseband_cert_id: String,  // hex cert ID
    pub baseband_chip_id: String,

    // SIM deep analysis
    pub sim_status: String,        // kCTSIMSupport...
    pub sim_status_label: String,  // human readable
    pub sim_tray_status: String,   // SIMTrayStatus
    pub iccid: String,
    pub imsi: String,              // carrier identity
    pub mcc_mnc: String,           // e.g. "310 260"
    pub carrier_name: String,
    pub carrier_bundle: String,
    pub carrier_roaming: bool,

    // Lock analysis
    pub sim_lock_type: String,     // None/Locked/Blocked
    pub is_carrier_locked: bool,
    pub is_sim_absent: bool,
    pub is_sim_blocked: bool,

    // Network
    pub current_mcc: String,
    pub current_mnc: String,
    pub phone_number: String,
    pub data_roaming: bool,

    // Bypass decision
    pub lock_analysis: String,
    pub bypass_method: String,
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

fn classify_sim_status(raw: &str) -> (&'static str, bool, bool, bool) {
    // Returns (label, is_locked, is_absent, is_blocked)
    match raw {
        s if s.contains("Ready") || s.contains("SIMStatusReady") => {
            ("Ready ✅", false, false, false)
        }
        s if s.contains("NotInserted") || s.contains("Absent") => {
            ("No SIM Inserted", false, true, false)
        }
        s if s.contains("Blocked") || s.contains("PUK") => {
            ("SIM Blocked (PUK needed) ⛔", false, false, true)
        }
        s if s.contains("CarrierLocked") => ("Carrier Locked 🔒", true, false, false),
        s if s.contains("Restricted") => ("Carrier Restricted ⚠️", true, false, false),
        s if s.contains("Invalid") => ("Invalid SIM ⚠️", true, false, false),
        s if s.contains("Locked") => ("SIM PIN Locked 🔒", true, false, false),
        "N/A" => ("Unknown / No SIM", false, true, false),
        _ => ("Unknown State", false, false, false),
    }
}

fn determine_lock_type(
    is_locked: bool,
    is_absent: bool,
    is_blocked: bool,
    carrier_name: &str,
    sim_status: &str,
    baseband_version: &str,
) -> (&'static str, &'static str) {
    // Returns (lock_type, bypass_method)
    if is_blocked {
        return (
            "SIM Blocked",
            "Replace SIM card — PUK unlock needed",
        );
    }
    if is_absent {
        return (
            "No SIM",
            "Insert physical SIM card, then Stage 6 will handle carrier unlock",
        );
    }
    if sim_status.contains("CarrierLocked") || sim_status.contains("Restricted") {
        return (
            "Carrier Locked",
            "Stage 6: Carrier restriction bypass via IMEI unlock server",
        );
    }
    if is_locked {
        return ("SIM PIN Locked", "Unlock SIM PIN on device, then re-run");
    }
    if (carrier_name == "N/A" || carrier_name.is_empty() || carrier_name == "No Carrier")
        && !is_absent
        && baseband_version != "N/A"
    {
        return (
            "No Carrier Signal",
            "Stage 6: Baseband carrier patch + Stage 8: signal restore",
        );
    }
    ("None", "No carrier lock — proceed to Stage 4")
}

#[tauri::command]
pub async fn signal_stage3_baseband(
    app: AppHandle,
    udid: String,
) -> Result<Stage3Result, String> {
    // ── Log helper ─────────────────────────────────
    macro_rules! slog {
        ($msg:expr) => {
            let _ = app.emit("s3-log", $msg.to_string());
        };
        ($fmt:literal, $($arg:tt)*) => {
            let _ = app.emit("s3-log", format!($fmt, $($arg)*));
        };
    }

    slog!("╔══════════════════════════════════╗");
    slog!("║  STAGE 3 — BASEBAND ANALYSIS     ║");
    slog!("╚══════════════════════════════════╝");
    slog!("");

    // ── 1. Baseband firmware info ──────────────────
    slog!("📡 Reading baseband firmware...");

    let baseband_version = iinfo(&udid, "BasebandVersion");
    let baseband_serial = iinfo(&udid, "BasebandSerialNumber");
    let baseband_cert_id = iinfo(&udid, "BasebandCertId");
    let baseband_chip_id = iinfo(&udid, "BasebandChipId");

    slog!("   Baseband FW:     {}", baseband_version);
    slog!("   Baseband Serial: {}", baseband_serial);
    slog!("   Cert ID:         {}", baseband_cert_id);
    slog!("   Chip ID:         {}", baseband_chip_id);

    // ── 2. Deep SIM analysis ───────────────────────
    slog!("");
    slog!("💳 Deep SIM card analysis...");

    let sim_status = iinfo(&udid, "SIMStatus");
    let sim_tray = iinfo(&udid, "SIMTrayStatus");
    let iccid = iinfo(&udid, "IntegratedCircuitCardIdentity");

    // IMSI via idevicediagnostics MobileGestalt
    let imsi = {
        let v = std::process::Command::new("idevicediagnostics")
            .env("PATH", path_env())
            .args(["-u", &udid, "MobileGestalt", "IMSI"])
            .output()
            .map(|o| String::from_utf8_lossy(&o.stdout).trim().to_string())
            .unwrap_or_default();
        // Extract value after "IMSI: " or just raw
        if v.contains("IMSI") {
            v.lines()
                .find(|l| l.contains("IMSI"))
                .and_then(|l| l.split(':').nth(1))
                .map(|s| s.trim().to_string())
                .unwrap_or_else(|| "N/A".to_string())
        } else if v.is_empty() {
            "N/A".to_string()
        } else {
            v
        }
    };

    let (sim_label, is_locked, is_absent, is_blocked) = classify_sim_status(&sim_status);

    slog!("   SIM Status:  {} ({})", sim_label, sim_status);
    slog!("   SIM Tray:    {}", sim_tray);
    slog!("   ICCID:       {}", iccid);
    slog!("   IMSI:        {}", imsi);

    // ── 3. Carrier identity ────────────────────────
    slog!("");
    slog!("📶 Reading carrier information...");

    let carrier_name = iinfo(&udid, "CarrierName");
    let carrier_bundle = iinfo(&udid, "CarrierBundleInfoURL");

    // MCC/MNC from IMSI (first 5-6 digits)
    let mcc_mnc = if imsi.len() >= 5 && imsi != "N/A" {
        let mcc = &imsi[..3];
        let mnc = &imsi[3..5];
        format!("{} {}", mcc, mnc)
    } else {
        let fallback = iinfo(&udid, "InternationalMobileSubscriberIdentity");
        let s: String = fallback.chars().take(6).collect();
        if s.len() >= 5 {
            format!("{} {}", &s[..3], &s[3..])
        } else {
            "N/A".to_string()
        }
    };

    let carrier_roaming_raw = iinfo(&udid, "CarrierRoaming");
    let carrier_roaming = carrier_roaming_raw.to_lowercase() == "true";

    slog!("   Carrier:  {}", carrier_name);
    slog!("   MCC/MNC:  {}", mcc_mnc);
    slog!("   Roaming:  {}", carrier_roaming);

    // ── 4. Current network ─────────────────────────
    slog!("");
    slog!("🌐 Network registration...");

    let current_mcc = iinfo(&udid, "CurrentMCC");
    let current_mnc = iinfo(&udid, "CurrentMNC");
    let phone_number = iinfo(&udid, "PhoneNumber");
    let data_roaming_raw = iinfo(&udid, "DataRoamingEnabled");
    let data_roaming = data_roaming_raw.to_lowercase() == "true";

    slog!("   Current MCC/MNC: {}/{}", current_mcc, current_mnc);
    slog!("   Phone number: {}", phone_number);

    // ── 5. Carrier lock detection ──────────────────
    slog!("");
    slog!("🔒 Analyzing carrier lock status...");

    let is_carrier_locked = is_locked
        || sim_status.contains("CarrierLocked")
        || sim_status.contains("Restricted")
        || (carrier_name == "N/A" && !is_absent && baseband_version != "N/A");

    let (lock_type, bypass_method) = determine_lock_type(
        is_carrier_locked,
        is_absent,
        is_blocked,
        &carrier_name,
        &sim_status,
        &baseband_version,
    );

    let lock_analysis = format!(
        "Baseband: {} | SIM: {} | Lock: {}",
        baseband_version, sim_label, lock_type,
    );

    slog!("   Carrier locked: {}", is_carrier_locked);
    slog!("   Lock type:      {}", lock_type);
    slog!("   Method:         {}", bypass_method);

    // ── 6. Stage pass/fail ─────────────────────────
    slog!("");
    let stage_passed = !is_blocked; // blocked = replace SIM
    let stage_message = if is_blocked {
        "SIM blocked (PUK). Replace SIM card. ⛔".to_string()
    } else if is_absent {
        "No SIM inserted — insert SIM, Stage 6 will unlock carrier. ⚠️ Continue.".to_string()
    } else if is_carrier_locked {
        format!(
            "Carrier locked detected ({}) — Stage 6 will bypass. ⚠️ Continue.",
            lock_type
        )
    } else {
        format!(
            "Baseband OK: {} | SIM: {} ✅ Stage 3 passed.",
            baseband_version, sim_label
        )
    };

    if stage_passed {
        slog!("╔══════════════════════════════════╗");
        slog!("║  ✅  STAGE 3 PASSED              ║");
        slog!("╚══════════════════════════════════╝");
    } else {
        slog!("╔══════════════════════════════════╗");
        slog!("║  ⛔  STAGE 3 BLOCKED             ║");
        slog!("╚══════════════════════════════════╝");
    }
    slog!("   {}", stage_message);

    Ok(Stage3Result {
        udid,
        baseband_version,
        baseband_serial,
        baseband_cert_id,
        baseband_chip_id,
        sim_status,
        sim_status_label: sim_label.to_string(),
        sim_tray_status: sim_tray,
        iccid,
        imsi,
        mcc_mnc,
        carrier_name,
        carrier_bundle,
        carrier_roaming,
        sim_lock_type: lock_type.to_string(),
        is_carrier_locked,
        is_sim_absent: is_absent,
        is_sim_blocked: is_blocked,
        current_mcc,
        current_mnc,
        phone_number,
        data_roaming,
        lock_analysis,
        bypass_method: bypass_method.to_string(),
        stage_passed,
        stage_message,
    })
}
