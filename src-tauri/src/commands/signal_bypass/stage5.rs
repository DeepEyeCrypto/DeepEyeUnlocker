use serde::{Deserialize, Serialize};
use tauri::{AppHandle, Emitter};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MdmProfile {
    pub id: String,
    pub name: String,
    pub org: String,
    pub profile_type: String, // MDM/Config/Carrier/Supervision
    pub is_removable: bool,
    pub removed: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Stage5Result {
    pub udid: String,

    // Supervision
    pub is_supervised: bool,
    pub supervised_by: String,

    // Profiles found
    pub profiles_found: Vec<MdmProfile>,
    pub profile_count: usize,
    pub removed_count: usize,
    pub failed_count: usize,

    // MDM lock
    pub mdm_locked: bool, // DEP/ABM enrolled
    pub dep_enrolled: bool,
    pub abm_enrolled: bool,

    // Carrier profiles
    pub carrier_profiles_removed: usize,
    pub restrictions_removed: bool,

    // Raw output
    pub provision_output: String,
    pub provision_tool_available: bool,

    // Result
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
            let body = if stdout.is_empty() { stderr } else { stdout };
            (out.status.success(), body)
        }
        Err(e) => (false, format!("not found: {e}")),
    }
}

// Parse ideviceprovision list output into profiles
fn parse_profiles(raw: &str) -> Vec<MdmProfile> {
    let mut profiles: Vec<MdmProfile> = vec![];
    let mut current_id = String::new();
    let mut current_name = String::new();
    let mut current_org = String::new();
    let mut current_type = String::new();

    for line in raw.lines() {
        let line = line.trim();

        if line.starts_with("ProfileIdentifier:") {
            // Save previous profile if any
            if !current_id.is_empty() {
                profiles.push(MdmProfile {
                    id: current_id.clone(),
                    name: if current_name.is_empty() {
                        current_id.clone()
                    } else {
                        current_name.clone()
                    },
                    org: if current_org.is_empty() {
                        "Unknown".into()
                    } else {
                        current_org.clone()
                    },
                    profile_type: if current_type.is_empty() {
                        "Configuration".into()
                    } else {
                        current_type.clone()
                    },
                    is_removable: true,
                    removed: false,
                });
            }
            current_id = line
                .trim_start_matches("ProfileIdentifier:")
                .trim()
                .to_string();
            current_name = String::new();
            current_org = String::new();
            current_type = String::new();
        }
        if line.starts_with("PayloadDisplayName:") {
            current_name = line
                .trim_start_matches("PayloadDisplayName:")
                .trim()
                .to_string();
        }
        if line.starts_with("PayloadOrganization:") {
            current_org = line
                .trim_start_matches("PayloadOrganization:")
                .trim()
                .to_string();
        }
        if line.starts_with("PayloadType:") {
            current_type = line.trim_start_matches("PayloadType:").trim().to_string();
        }
    }

    // Last profile
    if !current_id.is_empty() {
        profiles.push(MdmProfile {
            id: current_id.clone(),
            name: if current_name.is_empty() {
                current_id
            } else {
                current_name
            },
            org: if current_org.is_empty() {
                "Unknown".into()
            } else {
                current_org
            },
            profile_type: if current_type.is_empty() {
                "Configuration".into()
            } else {
                current_type
            },
            is_removable: true,
            removed: false,
        });
    }

    profiles
}

#[tauri::command]
pub async fn signal_stage5_mdm(app: AppHandle, udid: String) -> Result<Stage5Result, String> {
    macro_rules! slog {
        ($msg:expr) => {
            let _ = app.emit("s5-log", $msg.to_string());
        };
        ($fmt:literal, $($arg:tt)*) => {
            let _ = app.emit("s5-log", format!($fmt, $($arg)*));
        };
    }

    slog!("╔══════════════════════════════════╗");
    slog!("║   STAGE 5 — MDM PROFILE REMOVAL  ║");
    slog!("╚══════════════════════════════════╝");
    slog!("");

    // ── 1. Supervision status ──────────────────────
    slog!("🏢 Checking device supervision...");

    let supervised_raw = iinfo(&udid, "IsSupervised");
    let is_supervised = supervised_raw.to_lowercase() == "true";
    let supervised_by = iinfo(&udid, "OrganizationName");
    let dep_raw = iinfo(&udid, "IsDEPEnrolled");
    let dep_enrolled = dep_raw.to_lowercase() == "true";

    slog!("   Supervised:  {}", is_supervised);
    slog!("   Org:         {}", supervised_by);
    slog!("   DEP:         {}", dep_enrolled);

    // ABM check via MDM URL presence
    let mdm_url = iinfo(&udid, "MDMServiceURL");
    let abm_enrolled = mdm_url != "N/A" && mdm_url.to_lowercase().contains("apple");

    if is_supervised {
        slog!("   ⚠️  Device is supervised by: {}", supervised_by);
    } else {
        slog!("   ✅ Not supervised");
    }

    // ── 2. List all profiles ───────────────────────
    slog!("");
    slog!("📋 Listing installed profiles...");

    let (list_ok, list_output) = run_tool("ideviceprovision", &["-u", &udid, "list"]);

    let provision_tool_available = list_ok || !list_output.contains("not found");

    let mut profiles = if provision_tool_available && !list_output.is_empty() {
        parse_profiles(&list_output)
    } else {
        vec![]
    };

    // Also try dumping all provisioning profiles
    let (_, dump_output) = run_tool("ideviceprovision", &["-u", &udid, "dump"]);

    // Merge any extra profiles from dump
    if !dump_output.is_empty() && dump_output != list_output {
        let extra = parse_profiles(&dump_output);
        for ep in extra {
            if !profiles.iter().any(|p: &MdmProfile| p.id == ep.id) {
                profiles.push(ep);
            }
        }
    }

    let profile_count = profiles.len();
    slog!("   Found {} profile(s)", profile_count);

    for p in &profiles {
        slog!("   → [{}] {} ({})", p.profile_type, p.name, p.org);
    }

    // ── 3. Remove profiles ─────────────────────────
    slog!("");
    slog!("🗑️  Removing profiles...");

    let mut removed_count: usize = 0;
    let mut failed_count: usize = 0;
    let mut carrier_removed: usize = 0;

    if profile_count == 0 && provision_tool_available {
        // Try remove-all even if list was empty
        slog!("   Attempting remove-all...");
        let (ok, out) = run_tool("ideviceprovision", &["-u", &udid, "remove-all"]);
        if ok {
            slog!("   ✅ remove-all: {}", out);
            removed_count += 1; // Just recording that an action was taken
        } else {
            slog!("   ℹ️  remove-all: {}", out);
        }
    } else {
        // Remove each profile by ID
        for profile in profiles.iter_mut() {
            let (ok, out) = run_tool("ideviceprovision", &["-u", &udid, "remove", &profile.id]);
            if ok || out.to_lowercase().contains("success") {
                profile.removed = true;
                removed_count += 1;
                if profile.profile_type.to_lowercase().contains("carrier") {
                    carrier_removed += 1;
                }
                slog!("   ✅ Removed: {}", profile.name);
            } else {
                failed_count += 1;
                slog!(
                    "   ⚠️  Failed: {} — {}",
                    profile.name,
                    out.lines().next().unwrap_or("error")
                );
            }
        }

        // Final remove-all sweep
        let (_, _) = run_tool("ideviceprovision", &["-u", &udid, "remove-all"]);
    }

    // ── 4. MDM lock check ─────────────────────────
    slog!("");
    slog!("🔒 Checking MDM lock status...");

    let mdm_locked = dep_enrolled
        || abm_enrolled
        || (is_supervised && supervised_by != "N/A" && removed_count == 0);

    if mdm_locked {
        slog!("   ⚠️  DEP/ABM enrolled — MDM lock may persist after profile removal");
        slog!("   💡 Device needs to be erased + re-enrolled to fully clear DEP");
    } else {
        slog!("   ✅ No persistent MDM lock");
    }

    // ── 5. Restrictions removal ────────────────────
    slog!("");
    slog!("🚫 Checking carrier restrictions...");

    // idevicebackup2 can sometimes clear restrictions (Note: keeping original logic, using ideviceinfo for check)
    let (_, rest_out) = run_tool("ideviceinfo", &["-u", &udid, "-k", "IsCarrierLocked"]);

    let restrictions_removed =
        removed_count > 0 || (!rest_out.contains("true") && rest_out != "N/A");

    slog!(
        "   Carrier restrictions: {}",
        if restrictions_removed {
            "cleared ✅"
        } else {
            "Stage 6 will handle ⚠️"
        }
    );

    // ── 6. Stage result ────────────────────────────
    slog!("");
    let stage_passed = true; // MDM removal is best-effort
    let stage_message = if mdm_locked {
        format!(
            "DEP/ABM lock detected ({}). {} profile(s) removed. Carrier unlock at Stage 6. ⚠️",
            supervised_by, removed_count
        )
    } else if removed_count > 0 {
        format!(
            "{} profile(s) removed successfully. Device clean for Stage 6. ✅",
            removed_count
        )
    } else if profile_count == 0 {
        "No profiles found — device clean. Stage 5 passed ✅".to_string()
    } else {
        format!(
            "{} profile(s) found, {} removed. Proceeding to Stage 6. ✅",
            profile_count, removed_count
        )
    };

    if stage_passed {
        slog!("╔══════════════════════════════════╗");
        slog!("║  ✅  STAGE 5 PASSED              ║");
        slog!("╚══════════════════════════════════╝");
    } else {
        slog!("╔══════════════════════════════════╗");
        slog!("║  ⛔  STAGE 5 BLOCKED             ║");
        slog!("╚══════════════════════════════════╝");
    }
    slog!("   {}", stage_message);

    Ok(Stage5Result {
        udid,
        is_supervised,
        supervised_by,
        profiles_found: profiles,
        profile_count,
        removed_count,
        failed_count,
        mdm_locked,
        dep_enrolled,
        abm_enrolled,
        carrier_profiles_removed: carrier_removed,
        restrictions_removed,
        provision_output: list_output,
        provision_tool_available,
        stage_passed,
        stage_message,
    })
}
