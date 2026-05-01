use serde::{Deserialize, Serialize};
use tauri::{AppHandle, Emitter};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ActivationPatchResult {
    pub activation_record_injected: bool,
    pub data_ark_patched: bool,
    pub activation_state_set: bool,
    pub fmi_disabled: bool,
    pub stage_message: String,
}

fn path_env() -> String {
    let base = std::env::var("PATH").unwrap_or_else(|_| "/usr/bin:/bin".to_string());
    format!("/usr/local/bin:/opt/homebrew/bin:{base}")
}

fn ssh_cmd(port: u16, cmd: &str) -> (bool, String) {
    match std::process::Command::new("sshpass")
        .env("PATH", path_env())
        .args([
            "-p",
            "alpine",
            "ssh",
            "-o",
            "StrictHostKeyChecking=no",
            "-o",
            "UserKnownHostsFile=/dev/null",
            "-o",
            "ConnectTimeout=5",
            "-p",
            &port.to_string(),
            "root@localhost",
            cmd,
        ])
        .output()
    {
        Ok(out) => {
            let stdout = String::from_utf8_lossy(&out.stdout).trim().to_string();
            let stderr = String::from_utf8_lossy(&out.stderr).trim().to_string();
            let body = if stdout.is_empty() { stderr } else { stdout };
            (out.status.success(), body)
        }
        Err(e) => (false, format!("SSH failed: {e}")),
    }
}

/// Inject a crafted activation record and patch activation state
#[tauri::command]
pub async fn fs_patch_activation(
    app: AppHandle,
    ssh_port: Option<u16>,
    activation_record_path: Option<String>,
) -> Result<ActivationPatchResult, String> {
    macro_rules! slog {
        ($msg:expr) => { drop(app.emit("fs-log", $msg.to_string())) };
        ($fmt:literal, $($arg:tt)*) => { drop(app.emit("fs-log", format!($fmt, $($arg)*))) };
    }

    let port = ssh_port.unwrap_or(2222);

    slog!("╔══════════════════════════════════╗");
    slog!("║  ACTIVATION — FILE PATCHES       ║");
    slog!("╚══════════════════════════════════╝");
    slog!("");

    // Verify SSH
    let (ssh_ok, _) = ssh_cmd(port, "id");
    if !ssh_ok {
        return Err("❌ SSH not connected. Run fs_start_tunnel first.".to_string());
    }

    // ── 1. Find and patch activation_record.plist ──
    slog!("📝 Step 1: Locating activation records...");

    let (_, find_out) = ssh_cmd(port,
        "find /mnt1 /mnt2 /var -name 'activation_record*' -o -name 'ActivationRecord*' 2>/dev/null | head -10"
    );

    let found_paths: Vec<&str> = find_out.lines().filter(|l| !l.is_empty()).collect();
    slog!("   Found {} activation record(s)", found_paths.len());
    for p in &found_paths {
        slog!("   📄 {}", p);
    }

    // Also check the main mobileactivationd data container
    let activation_containers = [
        "/mnt2/containers/Data/System/*/Library/internal/data_ark.plist",
        "/var/containers/Data/System/*/Library/internal/data_ark.plist",
        "/mnt1/containers/Data/System/*/Library/internal/data_ark.plist",
    ];

    let mut activation_record_injected = false;

    // If user provided a custom activation record, push it
    if let Some(ref local_record) = activation_record_path {
        slog!("");
        slog!("📤 Injecting custom activation record...");
        slog!("   Source: {}", local_record);

        // Find the mobileactivationd container
        let (_, container_out) = ssh_cmd(port,
            "find /mnt2/containers/Data/System /var/containers/Data/System -name 'mobileactivationd' -type d 2>/dev/null | head -1"
        );

        let target_dir = if container_out.is_empty() {
            "/var/root".to_string()
        } else {
            container_out.trim().to_string()
        };

        let target = format!("{}/activation_record.plist", target_dir);
        slog!("   Target: {}", target);

        // Backup original
        let _ = ssh_cmd(
            port,
            &format!("cp '{target}' '{target}.deepeye.bak' 2>/dev/null"),
        );

        // Push via scp
        let (push_ok, push_out) = std::process::Command::new("sshpass")
            .env("PATH", path_env())
            .args([
                "-p",
                "alpine",
                "scp",
                "-o",
                "StrictHostKeyChecking=no",
                "-o",
                "UserKnownHostsFile=/dev/null",
                "-P",
                &port.to_string(),
                local_record,
                &format!("root@localhost:{target}"),
            ])
            .output()
            .map(|o| {
                let stdout = String::from_utf8_lossy(&o.stdout).trim().to_string();
                let stderr = String::from_utf8_lossy(&o.stderr).trim().to_string();
                (
                    o.status.success(),
                    if stdout.is_empty() { stderr } else { stdout },
                )
            })
            .unwrap_or((false, "scp not available".to_string()));

        if push_ok {
            slog!("   ✅ Activation record injected");
            activation_record_injected = true;
        } else {
            slog!("   ⚠️ Injection failed: {}", push_out);
        }
    } else {
        slog!("   ℹ️  No custom record provided — patching existing state");
    }

    // ── 2. Patch data_ark.plist (activation state) ──
    slog!("");
    slog!("📝 Step 2: Patching data_ark.plist...");

    let mut data_ark_patched = false;
    for glob_pattern in &activation_containers {
        let (_, ark_out) = ssh_cmd(port, &format!("ls {} 2>/dev/null | head -1", glob_pattern));
        if ark_out.is_empty() {
            continue;
        }

        let ark_path = ark_out.trim();
        slog!("   Found: {}", ark_path);

        // Backup
        let _ = ssh_cmd(
            port,
            &format!("cp '{ark_path}' '{ark_path}.deepeye.bak' 2>/dev/null"),
        );

        // Patch ActivationState to Activated
        let (ok1, _) = ssh_cmd(port, &format!(
            "plutil -replace '-ActivationState' -string 'Activated' '{ark_path}' 2>/dev/null && echo 'OK'"
        ));

        // Also try the non-prefixed key
        let (ok2, _) = ssh_cmd(port, &format!(
            "plutil -replace 'ActivationState' -string 'Activated' '{ark_path}' 2>/dev/null && echo 'OK'"
        ));

        if ok1 || ok2 {
            slog!("   ✅ ActivationState set to 'Activated'");
            data_ark_patched = true;
        } else {
            slog!("   ⚠️ plutil patch failed — trying defaults write...");
            let (ok3, _) = ssh_cmd(
                port,
                &format!(
                    "defaults write '{}' '-ActivationState' 'Activated' 2>/dev/null",
                    ark_path.trim_end_matches(".plist")
                ),
            );
            if ok3 {
                slog!("   ✅ ActivationState set via defaults");
                data_ark_patched = true;
            }
        }

        if data_ark_patched {
            break;
        }
    }

    // ── 3. Set activation state in lockdownd ────────
    slog!("");
    slog!("📝 Step 3: Setting lockdownd activation state...");

    let activation_state_cmds = [
        "defaults write /var/root/Library/Lockdown/data_ark '-ActivationState' 'Activated'",
        "defaults write /var/root/Library/Lockdown/data_ark 'ActivationState' 'Activated'",
    ];

    let mut activation_state_set = false;
    for cmd in &activation_state_cmds {
        let (ok, _) = ssh_cmd(port, &format!("{cmd} 2>/dev/null && echo 'OK'"));
        if ok {
            activation_state_set = true;
        }
    }
    slog!(
        "   Lockdownd state: {}",
        if activation_state_set {
            "✅ Set"
        } else {
            "⚠️ Skipped"
        }
    );

    // ── 4. Disable FMI check ────────────────────────
    slog!("");
    slog!("📝 Step 4: Disabling Find My iPhone check...");

    let fmi_cmds = [
        "defaults write /var/mobile/Library/Preferences/com.apple.icloud 'FindMyiPhoneEnabled' -bool false",
        "defaults write /var/mobile/Library/Preferences/com.apple.icloud 'FMIPEnabled' -bool false",
        "defaults write /var/mobile/Library/Preferences/com.apple.fmipd 'IsDeviceLocatorServiceEnabled' -bool false",
    ];

    let mut fmi_disabled = false;
    for cmd in &fmi_cmds {
        let (ok, _) = ssh_cmd(port, &format!("{cmd} 2>/dev/null"));
        if ok {
            fmi_disabled = true;
        }
    }
    slog!(
        "   FMI: {}",
        if fmi_disabled {
            "✅ Disabled"
        } else {
            "⚠️ Skipped"
        }
    );

    // ── 5. Clear activation caches ──────────────────
    slog!("");
    slog!("🧹 Clearing activation caches...");
    let _ = ssh_cmd(
        port,
        "rm -rf /var/mobile/Library/Caches/com.apple.mobileactivationd* 2>/dev/null",
    );
    let _ = ssh_cmd(
        port,
        "rm -f /var/mobile/Library/Caches/com.apple.activation* 2>/dev/null",
    );
    slog!("   ✅ Caches cleared");

    let msg = if activation_record_injected || data_ark_patched {
        "✅ Activation files patched. Device should show as Activated on next boot."
    } else if activation_state_set {
        "⚠️ Activation state set in lockdownd but filesystem patches incomplete."
    } else {
        "❌ Activation patching failed — check filesystem permissions and SSH access."
    };

    slog!("");
    slog!("{}", msg);

    Ok(ActivationPatchResult {
        activation_record_injected,
        data_ark_patched,
        activation_state_set,
        fmi_disabled,
        stage_message: msg.to_string(),
    })
}
