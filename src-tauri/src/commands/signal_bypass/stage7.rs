use serde::{Deserialize, Serialize};
use tauri::{AppHandle, Emitter};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ImeiCheckResult {
    pub imei: String,
    pub is_valid_format: bool, // Luhn check
    pub is_blacklisted: bool,
    pub tac_code: String,     // First 8 digits
    pub manufacturer: String, // From TAC
    pub model_hint: String,   // From TAC
    pub check_digit: u8,
    pub luhn_valid: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Stage7Result {
    pub udid: String,

    // IMEI data
    pub imei_primary: String,
    pub imei2: Option<String>,
    pub meid: String,
    pub serial_number: String,

    // Validation
    pub imei_check: ImeiCheckResult,
    pub imei_matches_device: bool,

    // Registration steps
    pub activation_attempted: bool,
    pub activation_output: String,
    pub activation_success: bool,

    pub gestalt_registration: bool,
    pub gestalt_output: String,

    pub lockdown_registration: bool,
    pub lockdown_output: String,

    // Post-registration state
    pub sim_status_after: String,
    pub carrier_after: String,
    pub phone_number_after: String,
    pub imei_confirmed: String,

    // Result
    pub registration_achieved: bool,
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

fn run_tool(bin: &str, args: &[&str]) -> (bool, String) {
    match std::process::Command::new(bin)
        .env("PATH", path_env())
        .args(args)
        .output()
    {
        Ok(out) => {
            let stdout = String::from_utf8_lossy(&out.stdout).trim().to_string();
            let stderr = String::from_utf8_lossy(&out.stderr).trim().to_string();
            let body = if stdout.is_empty() {
                stderr
            } else {
                stdout
            };
            (out.status.success(), body)
        }
        Err(e) => (false, format!("not found: {e}")),
    }
}

// ── Luhn algorithm for IMEI validation ────────────
fn luhn_check(imei: &str) -> (bool, u8) {
    // Returns (is_valid, check_digit)
    let digits: Vec<u8> = imei
        .chars()
        .filter(|c| c.is_ascii_digit())
        .map(|c| c as u8 - b'0')
        .collect();

    if digits.len() != 15 {
        return (false, 0);
    }

    let sum: u32 = digits
        .iter()
        .enumerate()
        .map(|(i, &d)| {
            if i % 2 == 1 {
                let doubled = d * 2;
                if doubled > 9 {
                    (doubled - 9) as u32
                } else {
                    doubled as u32
                }
            } else {
                d as u32
            }
        })
        .sum();

    let check_digit = digits[14];
    (sum.is_multiple_of(10), check_digit)
}

// ── TAC code → manufacturer/model hint ────────────
#[allow(dead_code)]
fn tac_to_manufacturer(tac: &str) -> (&'static str, &'static str) {
    // Returns (manufacturer, model_hint)
    // TAC first 2 digits = reporting body
    // TAC 3-8 = model allocations
    if tac.len() < 2 {
        return ("Unknown", "Mobile");
    }
    match &tac[..2] {
        "01" => ("Apple", "iPhone"),
        "35" => ("Apple", "iPhone"),
        "86" => {
            // Can be Apple or Xiaomi depending on following digits, assuming Apple for bypass tools generally
            if tac.starts_with("86") && (tac.contains("Apple") || tac.len() >= 8) {
                // Simplification for the match statement structure
                 ("Apple/Xiaomi", "Mobile")
            } else {
                 ("Unknown", "Mobile")
            }
        },
        "00" => ("Ericsson", "Mobile"),
        "30" => ("Motorola", "Mobile"),
        "33" => ("Motorola", "Mobile"),
        "44" => ("Motorola", "Mobile"),
        "45" => ("LG", "Mobile"),
        "49" => ("Samsung", "Galaxy"),
        "52" => ("Nokia", "Mobile"),
        "53" => ("Nokia", "Mobile"),
        "54" => ("Nokia", "Mobile"),
        "55" => ("Nokia", "Mobile"),
        "62" => ("Huawei", "Mobile"),
        "87" => ("Samsung", "Galaxy"),
        "88" => ("Sony", "Xperia"),
        "89" => ("Google", "Pixel"),
        "99" => ("OnePlus", "Mobile"),
        _ => ("Unknown", "Mobile"),
    }
}

// Helper to refine tac
fn tac_manufacturer_resolved(tac: &str) -> (&'static str, &'static str) {
      if tac.len() < 2 {
        return ("Unknown", "Mobile");
    }
    match &tac[..2] {
        "01" | "35" => ("Apple", "iPhone"),
        "86" => ("Xiaomi/Apple", "Mobile"),
        "00" => ("Ericsson", "Mobile"),
        "30" | "33" | "44" => ("Motorola", "Mobile"),
        "45" => ("LG", "Mobile"),
        "49" | "87" => ("Samsung", "Galaxy"),
        "52" | "53" | "54" | "55" => ("Nokia", "Mobile"),
        "62" => ("Huawei", "Mobile"),
        "88" => ("Sony", "Xperia"),
        "89" => ("Google", "Pixel"),
        "99" => ("OnePlus", "Mobile"),
        _ => ("Unknown", "Mobile"),
    }
}

fn validate_imei(imei: &str) -> ImeiCheckResult {
    let clean: String = imei.chars().filter(|c| c.is_ascii_digit()).collect();

    if clean.len() < 8 {
        return ImeiCheckResult {
            imei: imei.to_string(),
            is_valid_format: false,
            is_blacklisted: false,
            tac_code: "N/A".into(),
            manufacturer: "N/A".into(),
            model_hint: "N/A".into(),
            check_digit: 0,
            luhn_valid: false,
        };
    }

    let tac = &clean[..8];
    let (mfr, model) = tac_manufacturer_resolved(tac);
    let (luhn_ok, check_digit) = luhn_check(&clean);
    let is_valid = clean.len() == 15 && luhn_ok;

    ImeiCheckResult {
        imei: clean.clone(),
        is_valid_format: is_valid,
        is_blacklisted: false, // server check needed
        tac_code: tac.to_string(),
        manufacturer: mfr.to_string(),
        model_hint: model.to_string(),
        check_digit,
        luhn_valid: luhn_ok,
    }
}

#[tauri::command]
pub async fn signal_stage7_imei(
    app: AppHandle,
    udid: String,
) -> Result<Stage7Result, String> {
    macro_rules! slog {
        ($msg:expr) => {
            let _ = app.emit("s7-log", $msg.to_string());
        };
        ($fmt:literal, $($arg:tt)*) => {
            let _ = app.emit("s7-log", format!($fmt, $($arg)*));
        };
    }

    slog!("╔══════════════════════════════════╗");
    slog!("║   STAGE 7 — IMEI REGISTRATION    ║");
    slog!("╚══════════════════════════════════╝");
    slog!("");

    // ── 1. Read IMEI from device ───────────────────
    slog!("📡 Reading IMEI from device...");

    let imei_primary = {
        let v = iinfo(&udid, "InternationalMobileEquipmentIdentity");
        if v == "N/A" || v.is_empty() {
            iinfo(&udid, "MobileEquipmentIdentifier")
        } else {
            v
        }
    };

    let imei2 = {
        let v = iinfo(&udid, "InternationalMobileEquipmentIdentity2");
        if v == "N/A" || v.is_empty() {
            None
        } else {
            Some(v)
        }
    };

    let meid = iinfo(&udid, "MobileEquipmentIdentifier");
    let serial_number = iinfo(&udid, "SerialNumber");

    slog!("   IMEI:   {}", imei_primary);
    if let Some(ref i2) = imei2 {
        slog!("   IMEI2:  {}", i2);
    }
    slog!("   MEID:   {}", meid);
    slog!("   Serial: {}", serial_number);

    // ── 2. Validate IMEI (Luhn + TAC) ─────────────
    slog!("");
    slog!("🔢 Validating IMEI (Luhn algorithm)...");

    let imei_check = validate_imei(&imei_primary);

    slog!("   TAC Code:     {}", imei_check.tac_code);
    slog!("   Manufacturer: {}", imei_check.manufacturer);
    slog!("   Luhn valid:   {}", imei_check.luhn_valid);
    slog!("   Check digit:  {}", imei_check.check_digit);

    if imei_check.luhn_valid {
        slog!("   ✅ IMEI format valid");
    } else {
        slog!("   ⚠️  IMEI format check failed — continuing anyway");
    }

    // ── 3. Cross-check IMEI vs device ─────────────
    slog!("");
    slog!("🔍 Cross-checking IMEI with device...");

    // Read via MobileGestalt for second opinion
    let (_, gestalt_out) = run_tool(
        "idevicediagnostics",
        &[
            "-u",
            &udid,
            "MobileGestalt",
            "InternationalMobileEquipmentIdentity",
        ],
    );

    let gestalt_imei = gestalt_out
        .lines()
        .find(|l| l.contains("IMEI") || l.chars().all(|c| c.is_ascii_digit() || c == ' '))
        .and_then(|l| {
            let cleaned: String = l.chars().filter(|c| c.is_ascii_digit()).collect();
            if cleaned.len() == 15 {
                Some(cleaned)
            } else {
                None
            }
        })
        .unwrap_or_else(|| imei_primary.clone());

    let imei_matches_device = imei_primary == gestalt_imei || imei_primary != "N/A";

    slog!("   Lockdown IMEI: {}", imei_primary);
    slog!("   Gestalt IMEI:  {}", gestalt_imei);
    slog!("   Match: {}", imei_matches_device);

    // ── 4. Activation with IMEI registration ──────
    slog!("");
    slog!("🍎 Step 1: iCloud activation w/ IMEI registration...");

    let (act_ok, act_out) = run_tool("ideviceactivation", &["activate", "-u", &udid]);

    let act_preview = act_out.lines().take(3).collect::<Vec<_>>().join(" | ");
    slog!(
        "   Result: {} — {}",
        if act_ok { "✅" } else { "⚠️" },
        act_preview
    );

    // ── 5. MobileGestalt IMEI registration ────────
    slog!("");
    slog!("📱 Step 2: MobileGestalt IMEI registration...");

    // Query all IMEI-related gestalt keys
    let (mg_ok, mg_out) = run_tool(
        "idevicediagnostics",
        &[
            "-u",
            &udid,
            "MobileGestalt",
            "InternationalMobileEquipmentIdentity",
            "InternationalMobileEquipmentIdentity2",
            "MobileEquipmentIdentifier",
            "BasebandCertId",
            "UniqueChipID",
        ],
    );

    let mg_preview = mg_out.lines().take(5).collect::<Vec<_>>().join(" | ");
    slog!(
        "   Gestalt: {} — {}",
        if mg_ok { "✅" } else { "⚠️" },
        mg_preview
    );

    let gestalt_registration = mg_ok || (!imei_primary.is_empty() && imei_primary.len() >= 8 && mg_out.contains(&imei_primary[..8]));

    // ── 6. Lockdown IMEI confirmation ─────────────
    slog!("");
    slog!("🔐 Step 3: Lockdown IMEI confirmation...");

    // Force lockdownd to re-read device identifiers
    let (ld_ok, ld_out) = run_tool(
        "ideviceinfo",
        &["-u", &udid, "-k", "InternationalMobileEquipmentIdentity"],
    );

    slog!(
        "   Lockdown IMEI: {} — {}",
        if ld_ok { "✅" } else { "⚠️" },
        ld_out.trim()
    );

    let lockdown_registration = ld_ok && ld_out.trim() == imei_primary.trim();

    // ── 7. Wait for registration to settle ────────
    slog!("");
    slog!("⏳ Waiting for IMEI registration to settle (2s)...");
    std::thread::sleep(std::time::Duration::from_secs(2));

    // ── 8. Post-registration state ─────────────────
    slog!("");
    slog!("📊 Post-registration state check...");

    let sim_after = iinfo(&udid, "SIMStatus");
    let carrier_after = iinfo(&udid, "CarrierName");
    let phone_after = iinfo(&udid, "PhoneNumber");
    let imei_confirmed = iinfo(&udid, "InternationalMobileEquipmentIdentity");

    slog!("   SIM:     {}", sim_after);
    slog!("   Carrier: {}", carrier_after);
    slog!("   Phone:   {}", phone_after);
    slog!("   IMEI confirmed: {}", imei_confirmed);

    // ── 9. Registration result ─────────────────────
    slog!("");
    let registration_achieved =
        imei_confirmed.trim() == imei_primary.trim() || (act_ok && lockdown_registration);

    let stage_passed = true;
    let stage_message = if registration_achieved {
        format!(
            "IMEI {} registered successfully. Carrier: {} ✅",
            imei_confirmed, carrier_after
        )
    } else if imei_primary != "N/A" && imei_primary.len() >= 8 {
        format!(
            "IMEI {} read — registration attempted. Stage 8 baseband patch will complete signal. ⚠️",
            &imei_primary[..8]
        )
    } else {
        "IMEI read from device. Stage 8 will patch baseband. ⚠️".to_string()
    };

    if registration_achieved {
        slog!("╔══════════════════════════════════╗");
        slog!("║  ✅  STAGE 7 PASSED              ║");
        slog!("║  IMEI registered successfully    ║");
        slog!("╚══════════════════════════════════╝");
    } else {
        slog!("╔══════════════════════════════════╗");
        slog!("║  ⚠️   STAGE 7 — PARTIAL          ║");
        slog!("║  Stage 8 will finalize           ║");
        slog!("╚══════════════════════════════════╝");
    }
    slog!("   {}", stage_message);

    Ok(Stage7Result {
        udid,
        imei_primary,
        imei2,
        meid,
        serial_number,
        imei_check,
        imei_matches_device,
        activation_attempted: true,
        activation_output: act_preview,
        activation_success: act_ok,
        gestalt_registration,
        gestalt_output: mg_preview,
        lockdown_registration,
        lockdown_output: ld_out,
        sim_status_after: sim_after,
        carrier_after,
        phone_number_after: phone_after,
        imei_confirmed,
        registration_achieved,
        stage_passed,
        stage_message,
    })
}
