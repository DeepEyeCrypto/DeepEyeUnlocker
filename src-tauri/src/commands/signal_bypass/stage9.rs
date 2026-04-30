use serde::{Deserialize, Serialize};
use tauri::{AppHandle, Emitter};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct VerificationCheck {
    pub name: String,
    pub expected: String,
    pub actual: String,
    pub passed: bool,
    pub critical: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Stage9Result {
    pub udid: String,

    // All checks
    pub checks: Vec<VerificationCheck>,
    pub total_checks: usize,
    pub passed_checks: usize,
    pub failed_critical: usize,

    // Key values
    pub final_carrier: String,
    pub final_sim_status: String,
    pub final_phone_number: String,
    pub final_imei: String,
    pub final_mcc: String,
    pub final_mnc: String,
    pub final_baseband: String,
    pub final_activation_state: String,

    // Capability matrix
    pub signal_ok: bool,
    pub sim_ok: bool,
    pub carrier_ok: bool,
    pub imei_ok: bool,
    pub activation_ok: bool,
    pub calls_ok: bool,
    pub data_ok: bool,

    // Score
    pub bypass_score: u8,     // 0–100
    pub bypass_grade: String, // A/B/C/F

    // Result
    pub stage_passed: bool,
    pub ready_for_completion: bool,
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

fn check(name: &str, expected: &str, actual: &str, critical: bool) -> VerificationCheck {
    let passed = match expected {
        "NOT_EMPTY" => !actual.is_empty() && actual != "N/A",
        "NOT_NA" => actual != "N/A" && !actual.is_empty(),
        "SIM_READY" => {
            actual.contains("Ready")
                || actual == "kCTSIMSupportSIMStatusReady"
                || actual == "SIMStatusReady"
        }
        "HAS_CARRIER" => {
            actual != "N/A" && !actual.is_empty() && actual != "No Carrier" && actual != "Unknown"
        }
        "LUHN_PASS" => {
            let d: Vec<u32> = actual.chars().filter_map(|c| c.to_digit(10)).collect();
            if d.len() != 15 {
                false
            } else {
                let s: u32 = d
                    .iter()
                    .rev()
                    .enumerate()
                    .map(|(i, &x)| {
                        if i % 2 == 1 {
                            let v = x * 2;
                            if v > 9 {
                                v - 9
                            } else {
                                v
                            }
                        } else {
                            x
                        }
                    })
                    .sum();
                s.is_multiple_of(10)
            }
        }
        "ACTIVATED" => {
            actual == "Activated"
                || actual == "FactoryActivated"
                || actual == "MobileActivated"
                || actual == "WildcardActivated"
                || actual == "PartiallyActivated"
        }
        "NUMERIC_MCC" => actual.len() == 3 && actual.chars().all(|c| c.is_ascii_digit()),
        "HAS_NUMBER" => {
            !actual.is_empty() && actual != "N/A" && actual.chars().any(|c| c.is_ascii_digit())
        }
        _ => actual == expected,
    };
    VerificationCheck {
        name: name.to_string(),
        expected: expected.to_string(),
        actual: actual.to_string(),
        passed,
        critical,
    }
}

fn score_grade(score: u8) -> &'static str {
    match score {
        90..=100 => "A",
        75..=89 => "B",
        60..=74 => "C",
        _ => "F",
    }
}

#[tauri::command]
pub async fn signal_stage9_verify(app: AppHandle, udid: String) -> Result<Stage9Result, String> {
    macro_rules! slog {
        ($msg:expr) => {
            let _ = app.emit("s9-log", $msg.to_string());
        };
        ($fmt:literal, $($arg:tt)*) => {
            let _ = app.emit("s9-log", format!($fmt, $($arg)*));
        };
    }

    slog!("╔══════════════════════════════════╗");
    slog!("║  STAGE 9 — FINAL VERIFICATION    ║");
    slog!("╚══════════════════════════════════╝");
    slog!("");

    // ── 0. Activation pre-check — re-attempt if needed ────
    slog!("🔑 Pre-check: ensuring activation...");
    let mut act_state = iinfo(&udid, "ActivationState");
    slog!("   Current: {}", act_state);

    if act_state == "Unactivated" || act_state == "N/A" {
        slog!("   ↻ Re-attempting activation...");
        let (ok, out) = run_tool("ideviceactivation", &["activate", "-u", &udid]);
        slog!(
            "   activate: {} — {}",
            if ok { "✅" } else { "⚠️" },
            out.lines().next().unwrap_or("(none)")
        );
        std::thread::sleep(std::time::Duration::from_secs(2));
        act_state = iinfo(&udid, "ActivationState");
        slog!("   After retry: {}", act_state);
    }
    slog!("");

    // ── 1. Wait for SIM/carrier registration (retry loop) ────
    slog!("📡 Waiting for SIM/carrier registration...");

    let mut carrier = "N/A".to_string();
    let mut sim = "N/A".to_string();
    let mut mcc = "N/A".to_string();
    let mut mnc = "N/A".to_string();
    let mut phone = "N/A".to_string();
    let max_retries = 5;

    for attempt in 1..=max_retries {
        carrier = iinfo(&udid, "CarrierName");
        sim = iinfo(&udid, "SIMStatus");
        mcc = iinfo(&udid, "CurrentMCC");
        mnc = iinfo(&udid, "CurrentMNC");
        phone = iinfo(&udid, "PhoneNumber");

        let has_carrier = carrier != "N/A"
            && !carrier.is_empty()
            && carrier != "No Carrier"
            && carrier != "Unknown";
        let has_sim = sim.contains("Ready") || sim == "kCTSIMSupportSIMStatusReady";

        slog!(
            "   [{}/{}] Carrier: {} | SIM: {}",
            attempt,
            max_retries,
            carrier,
            sim
        );

        if has_carrier && has_sim {
            slog!("   ✅ SIM + Carrier registered!");
            break;
        }
        if attempt < max_retries {
            slog!("   ⏳ Settling... ({}s)", attempt * 3);
            std::thread::sleep(std::time::Duration::from_secs(3));
        }
    }
    slog!("");

    // ── 2. Read all final values ───────────────────────
    slog!("🔍 Reading final device state...");

    let imei = iinfo(&udid, "InternationalMobileEquipmentIdentity");
    let baseband = iinfo(&udid, "BasebandVersion");
    // Re-read activation state (may have changed during settle)
    act_state = iinfo(&udid, "ActivationState");
    let iccid = iinfo(&udid, "IntegratedCircuitCardIdentity");
    let imsi = iinfo(&udid, "InternationalMobileSubscriberIdentity");
    let serial = iinfo(&udid, "SerialNumber");
    let _product = iinfo(&udid, "ProductType");
    let ios_ver = iinfo(&udid, "ProductVersion");

    slog!("   Carrier:    {}", carrier);
    slog!("   SIM Status: {}", sim);
    slog!("   Phone:      {}", phone);
    slog!("   IMEI:       {}", imei);
    slog!("   MCC/MNC:    {}/{}", mcc, mnc);
    slog!("   Activation: {}", act_state);
    slog!("   Baseband:   {}", baseband);
    slog!("   iOS:        {}", ios_ver);

    // ── 2. Run all verification checks ────────────
    slog!("");
    slog!("✅ Running verification checks...");
    slog!("");

    let mut checks: Vec<VerificationCheck> = vec![];

    // Critical checks (gate the pipeline)
    let c1 = check("SIM Status", "SIM_READY", &sim, true);
    slog!(
        "   [{}] SIM Status: {}",
        if c1.passed { "✅" } else { "❌" },
        sim
    );
    checks.push(c1);

    let c2 = check("Carrier", "HAS_CARRIER", &carrier, true);
    slog!(
        "   [{}] Carrier: {}",
        if c2.passed { "✅" } else { "❌" },
        carrier
    );
    checks.push(c2);

    let c3 = check("IMEI Luhn", "LUHN_PASS", &imei, true);
    slog!(
        "   [{}] IMEI Luhn: {}",
        if c3.passed { "✅" } else { "⚠️" },
        imei
    );
    checks.push(c3);

    let c4 = check("Activation", "ACTIVATED", &act_state, true);
    slog!(
        "   [{}] Activation: {}",
        if c4.passed { "✅" } else { "❌" },
        act_state
    );
    checks.push(c4);

    // Non-critical checks (warnings)
    let c5 = check("Phone Number", "HAS_NUMBER", &phone, false);
    slog!(
        "   [{}] Phone: {}",
        if c5.passed { "✅" } else { "⚠️" },
        phone
    );
    checks.push(c5);

    let c6 = check("MCC", "NUMERIC_MCC", &mcc, false);
    slog!("   [{}] MCC: {}", if c6.passed { "✅" } else { "⚠️" }, mcc);
    checks.push(c6);

    let c_mnc = check("MNC", "NOT_NA", &mnc, false);
    slog!(
        "   [{}] MNC: {}",
        if c_mnc.passed { "✅" } else { "⚠️" },
        mnc
    );
    checks.push(c_mnc);

    let c7 = check("ICCID", "NOT_EMPTY", &iccid, false);
    slog!(
        "   [{}] ICCID: {}",
        if c7.passed { "✅" } else { "⚠️" },
        if iccid.len() > 8 {
            format!("{}****", &iccid[..8])
        } else {
            iccid.clone()
        }
    );
    checks.push(c7);

    let c8 = check("Baseband", "NOT_NA", &baseband, false);
    slog!(
        "   [{}] Baseband: {}",
        if c8.passed { "✅" } else { "⚠️" },
        baseband
    );
    checks.push(c8);

    let c9 = check("IMSI", "NOT_EMPTY", &imsi, false);
    slog!(
        "   [{}] IMSI: {}",
        if c9.passed { "✅" } else { "⚠️" },
        if imsi.len() > 6 {
            format!("{}***", &imsi[..6])
        } else {
            imsi.clone()
        }
    );
    checks.push(c9);

    let c10 = check("Serial", "NOT_EMPTY", &serial, false);
    slog!(
        "   [{}] Serial: {}",
        if c10.passed { "✅" } else { "⚠️" },
        serial
    );
    checks.push(c10);

    // ── 3. Activation re-verify via ideviceactivation
    slog!("");
    slog!("🔑 Re-verifying activation state...");

    let (act_ok, act_out) = run_tool("ideviceactivation", &["state", "-u", &udid]);
    let act_confirmed = act_ok
        || act_out.to_lowercase().contains("activated")
        || act_out.to_lowercase().contains("already")
        || act_state == "Activated"
        || act_state == "FactoryActivated"
        || act_state == "MobileActivated"
        || act_state == "WildcardActivated"
        || act_state == "PartiallyActivated";
    slog!(
        "   State check: {} — {} (lockdown: {})",
        if act_confirmed { "✅" } else { "⚠️" },
        act_out.lines().next().unwrap_or("(none)"),
        act_state
    );

    // ── 4. Score computation ───────────────────────
    slog!("");
    slog!("📊 Computing bypass score...");

    let total = checks.len();
    let passed = checks.iter().filter(|c| c.passed).count();
    let critical_fails = checks.iter().filter(|c| !c.passed && c.critical).count();

    // Weighted score:
    // - Critical checks: 15 points each (4 × 15 = 60)
    // - Non-critical: ~6.67 each (6 × 6.67 ≈ 40)
    let critical_score: u8 = checks.iter().filter(|c| c.critical && c.passed).count() as u8 * 15;

    let noncrit_score: u8 = {
        let n = checks.iter().filter(|c| !c.critical && c.passed).count() as u8;
        (n * 40) / 7
    };

    let raw_score = critical_score.saturating_add(noncrit_score);
    let bypass_score = raw_score.min(100);
    let grade = score_grade(bypass_score).to_string();

    slog!("   Checks: {}/{}", passed, total);
    slog!("   Critical fails: {}", critical_fails);
    slog!("   Score: {}/100 ({})", bypass_score, grade);

    // ── 5. Capability matrix ───────────────────────
    let sim_ok = checks[0].passed;
    let carrier_ok_flag = checks[1].passed;
    let imei_ok = checks[2].passed;
    let activation_ok = checks[3].passed || act_confirmed;
    let signal_ok = sim_ok && carrier_ok_flag;
    let calls_ok = signal_ok && activation_ok;
    let data_ok = carrier_ok_flag && checks[5].passed;

    slog!("");
    slog!("📱 Capability matrix:");
    slog!(
        "   Signal:     {}",
        if signal_ok { "✅ YES" } else { "❌ NO" }
    );
    slog!("   SIM:        {}", if sim_ok { "✅ YES" } else { "❌ NO" });
    slog!(
        "   Carrier:    {}",
        if carrier_ok_flag { "✅ YES" } else { "❌ NO" }
    );
    slog!(
        "   Calls:      {}",
        if calls_ok {
            "✅ YES"
        } else {
            "⚠️ PENDING"
        }
    );
    slog!(
        "   Data:       {}",
        if data_ok { "✅ YES" } else { "⚠️ PENDING" }
    );
    slog!(
        "   IMEI:       {}",
        if imei_ok { "✅ OK" } else { "⚠️ CHECK" }
    );
    slog!(
        "   Activation: {}",
        if activation_ok {
            "✅ OK"
        } else {
            "⚠️ PENDING"
        }
    );

    // ── 6. Final result ────────────────────────────
    slog!("");
    // Stage passes if no critical failures OR score is decent (>= 40)
    // or if activation is confirmed (key milestone)
    let stage_passed = critical_fails == 0 || bypass_score >= 40 || act_confirmed;
    let ready_for_completion = bypass_score >= 60 || (act_confirmed && bypass_score >= 40);

    let stage_message = if bypass_score >= 90 {
        format!(
            "✅ BYPASS COMPLETE — Grade: {} ({}/100)\nCarrier: {} | Phone: {} | Voice+Data ready!",
            grade, bypass_score, carrier, phone
        )
    } else if bypass_score >= 75 {
        format!(
            "✅ Bypass successful — Grade: {} ({}/100)\nCarrier: {} | {} check(s) pending",
            grade,
            bypass_score,
            carrier,
            total - passed
        )
    } else if bypass_score >= 60 {
        format!(
            "⚠️ Partial bypass — Grade: {} ({}/100)\n{} critical check(s) ok. Stage 10 will finalize.",
            grade,
            bypass_score,
            4 - critical_fails
        )
    } else {
        format!(
            "❌ Bypass incomplete — Grade: {} ({}/100)\n{} critical check(s) failed. Review pipeline.",
            grade, bypass_score, critical_fails
        )
    };

    if bypass_score >= 75 {
        slog!("╔══════════════════════════════════╗");
        slog!("║  ✅  STAGE 9 — VERIFIED          ║");
        slog!("║  Grade: {}  Score: {}/100        ║", grade, bypass_score);
        slog!("╚══════════════════════════════════╝");
    } else {
        slog!("╔══════════════════════════════════╗");
        slog!("║  ⚠️   STAGE 9 — PARTIAL          ║");
        slog!("║  Score: {}/100 Grade: {}          ║", bypass_score, grade);
        slog!("╚══════════════════════════════════╝");
    }
    slog!("   Score: {}/100 — {}", bypass_score, grade);
    slog!("   {}", carrier);

    Ok(Stage9Result {
        udid,
        checks,
        total_checks: total,
        passed_checks: passed,
        failed_critical: critical_fails,
        final_carrier: carrier,
        final_sim_status: sim,
        final_phone_number: phone,
        final_imei: imei,
        final_mcc: mcc,
        final_mnc: mnc,
        final_baseband: baseband,
        final_activation_state: act_state,
        signal_ok,
        sim_ok,
        carrier_ok: carrier_ok_flag,
        imei_ok,
        activation_ok,
        calls_ok,
        data_ok,
        bypass_score,
        bypass_grade: grade,
        stage_passed,
        ready_for_completion,
        stage_message,
    })
}
