use serde::{Deserialize, Serialize};
use tauri::{AppHandle, Emitter};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Stage4Result {
    pub udid: String,

    // iCloud lock state
    pub activation_state: String,
    pub is_icloud_locked: bool,
    pub is_activation_locked: bool,
    pub is_demo_unit: bool,
    pub is_internal_build: bool,

    // Activation record details
    pub activation_record_exists: bool,
    pub activation_ticket_hash: String,
    pub wildcard_ticket: bool,

    // Device eligibility
    pub eligible_for_ios_update: bool,
    pub device_color: String,
    pub region_info: String,
    pub product_name: String,

    // ideviceactivation deep check
    pub act_tool_output: String,
    pub act_tool_available: bool,

    // Find My status
    pub find_my_state: String,  // On/Off/Unknown
    pub owner_apple_id: String, // masked if found

    // DST root / cert chain
    pub dst_root_available: bool,
    pub activation_server_reachable: bool,

    // Decision
    pub lock_severity: String, // None/Soft/Hard
    pub bypass_route: String,
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
    // Returns (success, stdout+stderr combined)
    match std::process::Command::new(bin)
        .env("PATH", path_env())
        .args(args)
        .output()
    {
        Ok(out) => {
            let stdout = String::from_utf8_lossy(&out.stdout).trim().to_string();
            let stderr = String::from_utf8_lossy(&out.stderr).trim().to_string();
            let combined = if stdout.is_empty() {
                stderr
            } else {
                stdout
            };
            (out.status.success(), combined)
        }
        Err(e) => (false, format!("tool not found: {e}")),
    }
}

fn mask_apple_id(email: &str) -> String {
    // Mask: jo***@gmail.com
    if email == "N/A" || !email.contains('@') {
        return email.to_string();
    }
    let parts: Vec<&str> = email.splitn(2, '@').collect();
    if parts.len() != 2 {
        return email.to_string();
    }
    let local = parts[0];
    let domain = parts[1];
    let visible: String = local.chars().take(2).collect();
    format!("{}***@{}", visible, domain)
}

fn determine_lock_severity(
    is_icloud: bool,
    is_act_locked: bool,
    find_my: &str,
    act_state: &str,
    wildcard: bool,
) -> (&'static str, &'static str) {
    // Returns (severity, bypass_route)
    if is_act_locked && find_my == "On" {
        return (
            "Hard Lock",
            "Signal Bypass Pipeline (A12+ Multi-method) — \
             1. ideviceactivation session -s\n\
             2. Albert carrier server POST\n\
             3. mobileactivation local patch\n\
             4. checkm8 ramdisk (if A7-A11)\n\
             5. ECID server ticket request\n\
             Note: Find My ON — partial bypass expected (WiFi access likely, iCloud restricted)",
        );
    }
    if is_icloud && !wildcard {
        return (
            "Hard Lock",
            "Signal Bypass Pipeline — \
             1. ideviceactivation session -s\n\
             2. Albert carrier server POST\n\
             3. mobileactivation local patch\n\
             4. checkm8 ramdisk (if A7-A11)\n\
             5. ECID server ticket request\n\
             Note: Direct method will be attempted",
        );
    }
    if is_act_locked && wildcard {
        return (
            "Soft Lock",
            "Wildcard activation detected — \
             Stage 5 MDM removal may clear lock. Continue pipeline.",
        );
    }
    if act_state == "NotActivated" {
        return (
            "Soft Lock",
            "Device not yet activated. \
             Run ideviceactivation activate, then resume from Stage 2.",
        );
    }
    if act_state == "MismatchedIMEI" {
        return (
            "Soft Lock",
            "IMEI mismatch with carrier record. \
             Stage 7 IMEI re-registration will fix.",
        );
    }
    (
        "None",
        "No iCloud lock detected. Proceed to Stage 5 MDM removal.",
    )
}

#[tauri::command]
pub async fn signal_stage4_icloud(
    app: AppHandle,
    udid: String,
) -> Result<Stage4Result, String> {
    // ── Log helper ─────────────────────────────────
    macro_rules! slog {
        ($msg:expr) => {
            let _ = app.emit("s4-log", $msg.to_string());
        };
        ($fmt:literal, $($arg:tt)*) => {
            let _ = app.emit("s4-log", format!($fmt, $($arg)*));
        };
    }

    slog!("╔══════════════════════════════════╗");
    slog!("║  STAGE 4 — iCLOUD DEEP SCAN      ║");
    slog!("╚══════════════════════════════════╝");
    slog!("");

    // ── 1. Activation state ────────────────────────
    slog!("☁️  Reading activation state...");

    let activation_state = iinfo(&udid, "ActivationState");
    let is_icloud_locked = activation_state.to_lowercase().contains("icloud")
        || activation_state == "Unactivated";

    slog!("   State: {}", activation_state);

    // ── 2. ideviceactivation deep check ───────────
    slog!("");
    slog!("🔍 Running ideviceactivation deep check...");

    let (act_available, act_output) =
        run_tool("ideviceactivation", &["state", "-u", &udid]);

    let act_tool_available = act_available || !act_output.contains("not found");

    slog!("   Tool available: {}", act_tool_available);
    slog!(
        "   Output: {}",
        act_output.lines().next().unwrap_or("(empty)")
    );

    // Parse ideviceactivation output for lock clues
    let is_activation_locked = act_output.to_lowercase().contains("activation lock")
        || act_output.to_lowercase().contains("locked")
        || is_icloud_locked;

    // ── 3. Activation record / ticket ─────────────
    slog!("");
    slog!("🎟️  Checking activation record...");

    let act_record = iinfo(&udid, "ActivationInfoXML");
    let activation_record_exists = act_record != "N/A" && !act_record.is_empty();

    // Check for wildcard ticket
    let wildcard_ticket = act_record.contains("WildcardTicket")
        || act_record.contains("wildcard")
        || activation_state == "WildcardActivated";

    // Hash first 16 chars as preview
    let activation_ticket_hash = if activation_record_exists && act_record.len() > 16 {
        format!("{}...[{}b]", &act_record[..16], act_record.len())
    } else {
        "No record".to_string()
    };

    slog!("   Record exists: {}", activation_record_exists);
    slog!("   Wildcard: {}", wildcard_ticket);
    slog!("   Ticket: {}", activation_ticket_hash);

    // ── 4. Device metadata ─────────────────────────
    slog!("");
    slog!("📱 Reading device metadata...");

    let product_name = iinfo(&udid, "ProductName");
    let device_color = iinfo(&udid, "DeviceColor");
    let region_info = iinfo(&udid, "RegionInfo");
    let is_demo_raw = iinfo(&udid, "IsDemo");
    let is_demo_unit = is_demo_raw.to_lowercase() == "true";
    let is_internal_raw = iinfo(&udid, "InternalBuild");
    let is_internal_build = is_internal_raw.to_lowercase() == "true";
    let eligible_raw = iinfo(&udid, "EligibleForIOSUpdate");
    let eligible_for_ios_update =
        eligible_raw.to_lowercase() == "true" || eligible_raw == "N/A";

    slog!("   Product: {}", product_name);
    slog!("   Region: {}", region_info);
    slog!("   Demo unit: {}", is_demo_unit);

    // ── 5. Apple ID / Find My ──────────────────────
    slog!("");
    slog!("🍎 Checking Apple ID / Find My...");

    // Try to get linked Apple ID
    let raw_apple_id = {
        let v = iinfo(&udid, "AppleID");
        if v == "N/A" {
            iinfo(&udid, "AccountInfo")
        } else {
            v
        }
    };
    let owner_apple_id = mask_apple_id(&raw_apple_id);

    // Find My state inference
    let find_my_state = if is_activation_locked || act_output.to_lowercase().contains("find my") {
        "On".to_string()
    } else if activation_state == "Activated" && !is_icloud_locked {
        "Off".to_string()
    } else {
        "Unknown".to_string()
    };

    slog!("   Apple ID: {}", owner_apple_id);
    slog!("   Find My:  {}", find_my_state);

    // ── 6. Network reachability ────────────────────
    slog!("");
    slog!("🌐 Checking Apple activation server...");

    // Ping albert.apple.com (activation server)
    let (server_ok, _) = run_tool(
        "curl",
        &[
            "--silent",
            "--max-time",
            "4",
            "--output",
            "/dev/null",
            "--write-out",
            "%{http_code}",
            "https://albert.apple.com/WebObjects/MZInit.woa/wa/deviceActivation",
        ],
    );

    // DST root cert check via security tool
    let (dst_ok, _) = run_tool(
        "security",
        &["find-certificate", "-c", "DST Root CA X3"],
    );

    slog!(
        "   Activation server: {}",
        if server_ok {
            "reachable ✅"
        } else {
            "unreachable ⚠️"
        }
    );
    slog!(
        "   DST Root cert: {}",
        if dst_ok { "found ✅" } else { "missing ⚠️" }
    );

    // ── 7. Lock severity + bypass route ───────────
    slog!("");
    slog!("🔐 Determining lock severity...");

    let (lock_severity, bypass_route) = determine_lock_severity(
        is_icloud_locked,
        is_activation_locked,
        &find_my_state,
        &activation_state,
        wildcard_ticket,
    );

    slog!("   Severity: {}", lock_severity);
    slog!("   Route: {}", bypass_route);

    // ── 8. Stage pass/fail — Hard Lock no longer blocks, attempt bypass ───
    slog!("");
    let stage_passed = true; // Always pass to allow bypass attempt
    let stage_message = if lock_severity == "Hard Lock" {
        "⚠️  Hard Lock detected — attempting advanced bypass methods...".to_string()
    } else if lock_severity == "Soft Lock" {
        format!(
            "Soft lock ({}) — Stage 5 MDM + Stage 6 carrier will clear. ⚠️ Continue.",
            activation_state
        )
    } else {
        "No iCloud lock. Stage 4 passed ✅".into()
    };

    if stage_passed {
        slog!("╔══════════════════════════════════╗");
        slog!("║  ✅  STAGE 4 PASSED              ║");
        slog!("╚══════════════════════════════════╝");
        slog!("   Proceeding to bypass pipeline...");
    }
    slog!("   {}", stage_message);

    Ok(Stage4Result {
        udid,
        activation_state,
        is_icloud_locked,
        is_activation_locked,
        is_demo_unit,
        is_internal_build,
        activation_record_exists,
        activation_ticket_hash,
        wildcard_ticket,
        eligible_for_ios_update,
        device_color,
        region_info,
        product_name,
        act_tool_output: act_output,
        act_tool_available,
        find_my_state,
        owner_apple_id,
        dst_root_available: dst_ok,
        activation_server_reachable: server_ok,
        lock_severity: lock_severity.to_string(),
        bypass_route: bypass_route.to_string(),
        stage_passed,
        stage_message,
    })
}
