use serde::{Deserialize, Serialize};
use tauri::{AppHandle, Emitter};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Stage2Result {
    pub udid: String,
    pub activation_status: String,   // raw string
    pub activation_enum: String,     // enum label
    pub is_icloud_locked: bool,
    pub is_activated: bool,
    pub apple_id_linked: String,     // email if available
    pub find_my_enabled: bool,
    pub supervision_enabled: bool,
    pub supervised_by: String,
    pub escrow_bag: String,          // activation token
    pub activation_blob: String,     // raw blob hash
    pub bypass_possible: bool,
    pub recommended_action: String,
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

fn classify_activation(raw: &str) -> (&'static str, bool, bool) {
    // Returns (label, is_activated, is_icloud_locked)
    match raw {
        "Activated" => ("Activated", true, false),
        "NotActivated" => ("Not Activated", false, false),
        "MismatchedIMEI" => ("Mismatched IMEI", false, false),
        "WildcardActivated" => ("Wildcard Activated", true, false),
        s if s.contains("iCloud") => ("iCloud Locked", false, true),
        s if s.contains("Partial") => ("Partially Activated", false, false),
        _ => ("Unknown", false, false),
    }
}

fn determine_bypass_action(
    is_activated: bool,
    is_icloud_locked: bool,
    find_my: bool,
    supervised: bool,
    raw: &str,
) -> (bool, String) {
    if is_icloud_locked || find_my {
        return (
            false,
            "iCloud Lock: requires original Apple ID to remove".into(),
        );
    }
    if raw == "NotActivated" {
        return (
            true,
            "Run ideviceactivation to activate, then proceed to Stage 3".into(),
        );
    }
    if raw == "MismatchedIMEI" {
        return (
            true,
            "IMEI mismatch: Stage 7 will re-register correct IMEI with carrier".into(),
        );
    }
    if supervised {
        return (
            true,
            "MDM supervised: Stage 5 will remove all profiles before signal restore".into(),
        );
    }
    if is_activated {
        return (
            true,
            "Activated normally. Signal issue is at baseband/carrier level → continue to Stage 3"
                .into(),
        );
    }
    (
        true,
        "Partial activation — Stage 3 will analyze baseband".into(),
    )
}

#[tauri::command]
pub async fn signal_stage2_activation(
    app: AppHandle,
    udid: String,
) -> Result<Stage2Result, String> {
    // ── Log helper ─────────────────────────────────
    macro_rules! slog {
        ($msg:expr) => {
            let _ = app.emit("s2-log", $msg.to_string());
        };
        ($fmt:literal, $($arg:tt)*) => {
            let _ = app.emit("s2-log", format!($fmt, $($arg)*));
        };
    }

    slog!("╔══════════════════════════════════╗");
    slog!("║   STAGE 2 — ACTIVATION CHECK     ║");
    slog!("╚══════════════════════════════════╝");
    slog!("");

    // ── 1. Primary activation state ───────────────
    slog!("🔍 Reading activation state...");
    let activation_status = iinfo(&udid, "ActivationState");
    let (label, is_activated, is_icloud_locked) = classify_activation(&activation_status);

    slog!("   Raw state:  {}", activation_status);
    slog!("   Classified: {}", label);

    // ── 2. iCloud / Find My status ────────────────
    slog!("");
    slog!("☁️  Checking iCloud Lock...");

    // Check activation lock via ideviceactivation
    let act_check = std::process::Command::new("ideviceactivation")
        .env("PATH", path_env())
        .args(["state", "-u", &udid])
        .output()
        .map(|o| String::from_utf8_lossy(&o.stdout).trim().to_string())
        .unwrap_or_else(|_| "tool not found".into());

    slog!("   ideviceactivation: {}", act_check);

    // Find My detection from activation state
    let find_my_enabled = is_icloud_locked
        || act_check.to_lowercase().contains("activation lock")
        || act_check.to_lowercase().contains("find my");

    if find_my_enabled {
        slog!("   ⚠️  Find My / iCloud Lock DETECTED");
    } else {
        slog!("   ✅ No iCloud Lock detected");
    }

    // ── 3. Apple ID linked ────────────────────────
    slog!("");
    slog!("🍎 Checking Apple ID...");

    let apple_id_raw = iinfo(&udid, "AppleID");
    let apple_id = if apple_id_raw == "N/A" {
        // Try alternate key
        iinfo(&udid, "AccountInfo")
    } else {
        apple_id_raw
    };

    slog!("   Apple ID: {}", apple_id);

    // ── 4. Supervision check ──────────────────────
    slog!("");
    slog!("🏢 Checking supervision (MDM)...");

    let supervised_raw = iinfo(&udid, "IsSupervised");
    let supervision_enabled = supervised_raw.to_lowercase() == "true";

    let supervised_by = if supervision_enabled {
        let org = iinfo(&udid, "OrganizationName");
        slog!("   ⚠️  Supervised by: {}", org);
        org
    } else {
        slog!("   ✅ Not supervised");
        "N/A".to_string()
    };

    // ── 5. Activation token / blob ────────────────
    slog!("");
    slog!("🔑 Reading activation tokens...");

    let escrow_bag = iinfo(&udid, "EscrowBag");
    let activation_blob = iinfo(&udid, "ActivationInfoXML");

    // Truncate blob for display
    let blob_preview = if activation_blob.len() > 32 {
        format!(
            "{}...[{}b]",
            &activation_blob[..32],
            activation_blob.len()
        )
    } else {
        activation_blob.clone()
    };
    slog!("   Blob: {}", blob_preview);

    // ── 6. Determine bypass possibility ───────────
    slog!("");
    slog!("🔐 Analyzing bypass path...");

    let (bypass_possible, recommended_action) = determine_bypass_action(
        is_activated,
        is_icloud_locked,
        find_my_enabled,
        supervision_enabled,
        &activation_status,
    );

    slog!("   Bypass possible: {}", bypass_possible);
    slog!("   Recommended: {}", recommended_action);

    // ── 7. Stage pass/fail ─────────────────────────
    slog!("");
    let (stage_passed, stage_message) = if bypass_possible {
        slog!("╔══════════════════════════════════╗");
        slog!("║  ✅  STAGE 2 PASSED              ║");
        slog!("║  Bypass path available           ║");
        slog!("╚══════════════════════════════════╝");
        (
            true,
            format!("Activation: {} — bypass path available ✅", label),
        )
    } else if is_icloud_locked {
        slog!("╔══════════════════════════════════╗");
        slog!("║  ⛔  STAGE 2 BLOCKED             ║");
        slog!("║  iCloud Lock requires Apple ID   ║");
        slog!("╚══════════════════════════════════╝");
        (
            false,
            "iCloud Activation Lock detected. Remove via original Apple ID. ⛔".to_string(),
        )
    } else {
        slog!("╔══════════════════════════════════╗");
        slog!("║  ⚠️   STAGE 2 WARNING            ║");
        slog!("║  Activated — signal issue likely ║");
        slog!("║  from baseband/carrier           ║");
        slog!("╚══════════════════════════════════╝");
        (
            true,
            "Device activated. Signal issue is baseband/carrier level → continue ✅".to_string(),
        )
    };

    Ok(Stage2Result {
        udid,
        activation_status,
        activation_enum: label.to_string(),
        is_icloud_locked,
        is_activated,
        apple_id_linked: apple_id,
        find_my_enabled,
        supervision_enabled,
        supervised_by,
        escrow_bag,
        activation_blob: blob_preview,
        bypass_possible,
        recommended_action,
        stage_passed,
        stage_message,
    })
}
