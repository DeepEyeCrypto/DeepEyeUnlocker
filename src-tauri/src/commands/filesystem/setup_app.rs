use serde::{Deserialize, Serialize};
use tauri::{AppHandle, Emitter};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SetupAppPatchResult {
    pub purplebuddy_patched: bool,
    pub setup_app_disabled: bool,
    pub language_set: bool,
    pub cloud_config_patched: bool,
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
            "-p", "alpine",
            "ssh",
            "-o", "StrictHostKeyChecking=no",
            "-o", "UserKnownHostsFile=/dev/null",
            "-o", "ConnectTimeout=5",
            "-p", &port.to_string(),
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

/// Patch Setup.app to skip activation screen on jailbroken device
#[tauri::command]
pub async fn fs_patch_setup_app(
    app: AppHandle,
    ssh_port: Option<u16>,
) -> Result<SetupAppPatchResult, String> {
    macro_rules! slog {
        ($msg:expr) => { drop(app.emit("fs-log", $msg.to_string())) };
        ($fmt:literal, $($arg:tt)*) => { drop(app.emit("fs-log", format!($fmt, $($arg)*))) };
    }

    let port = ssh_port.unwrap_or(2222);

    slog!("╔══════════════════════════════════╗");
    slog!("║  SETUP.APP — BYPASS PATCHES      ║");
    slog!("╚══════════════════════════════════╝");
    slog!("");

    // Verify SSH
    let (ssh_ok, _) = ssh_cmd(port, "id");
    if !ssh_ok {
        return Err("❌ SSH not connected. Run fs_start_tunnel first.".to_string());
    }

    // ── 1. Patch com.apple.purplebuddy.plist ────────
    slog!("📝 Step 1: Patching PurpleBuddy (skip Setup.app)...");

    // Find purplebuddy plist location
    let pb_paths = [
        "/mnt1/mobile/Library/Caches/com.apple.purplebuddy.plist",
        "/var/mobile/Library/Caches/com.apple.purplebuddy.plist",
        "/mnt2/mobile/Library/Caches/com.apple.purplebuddy.plist",
    ];

    let mut purplebuddy_patched = false;
    for pb_path in &pb_paths {
        slog!("   Trying: {}", pb_path);
        // Create parent directory if needed
        let dir = pb_path.rsplit_once('/').map(|(d, _)| d).unwrap_or("/tmp");
        let _ = ssh_cmd(port, &format!("mkdir -p '{dir}'"));

        // Write plist that marks setup as complete
        let plist_content = r#"<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>SetupDone</key>
    <true/>
    <key>SetupFinishedAllSteps</key>
    <true/>
    <key>SetupVersion</key>
    <integer>3</integer>
    <key>UserChoseLanguage</key>
    <true/>
    <key>UserHasBeenPromptedAboutLocationServices</key>
    <true/>
</dict>
</plist>"#;

        // Backup original
        let _ = ssh_cmd(port, &format!("cp '{pb_path}' '{pb_path}.deepeye.bak' 2>/dev/null"));

        let escaped = plist_content.replace('\'', "'\\''");
        let (ok, out) = ssh_cmd(port, &format!("printf '%s' '{escaped}' > '{pb_path}' && echo 'PATCHED'"));

        if ok && out.contains("PATCHED") {
            slog!("   ✅ PurpleBuddy patched at {}", pb_path);
            purplebuddy_patched = true;
            break;
        }
        slog!("   ⚠️ Failed: {}", out.lines().next().unwrap_or("unknown"));
    }

    // ── 2. Disable Setup.app bundle ────────────────
    slog!("");
    slog!("📝 Step 2: Disabling Setup.app...");

    let setup_paths = [
        "/mnt1/Applications/Setup.app",
        "/Applications/Setup.app",
    ];

    let mut setup_app_disabled = false;
    for setup_path in &setup_paths {
        let (exists, _) = ssh_cmd(port, &format!("test -d '{setup_path}' && echo 'EXISTS'"));
        if !exists {
            continue;
        }
        slog!("   Found: {}", setup_path);

        // Rename Setup.app to disable it (preserves for restore)
        let (ok, out) = ssh_cmd(port, &format!(
            "mv '{setup_path}' '{setup_path}.deepeye_disabled' 2>&1 && echo 'DISABLED'"
        ));

        if ok && out.contains("DISABLED") {
            slog!("   ✅ Setup.app disabled (renamed)");
            setup_app_disabled = true;
            break;
        }
        slog!("   ⚠️ Rename failed: {}", out.lines().next().unwrap_or("unknown"));

        // Alternative: patch Info.plist to skip
        slog!("   ↻ Trying Info.plist patch instead...");
        let (ok2, _) = ssh_cmd(port, &format!(
            "plutil -replace SBAppTags -json '[\"hidden\"]' '{setup_path}/Info.plist' 2>&1 && echo 'PATCHED'"
        ));
        if ok2 {
            slog!("   ✅ Setup.app hidden via Info.plist patch");
            setup_app_disabled = true;
            break;
        }
    }

    // ── 3. Set language/locale defaults ────────────
    slog!("");
    slog!("📝 Step 3: Setting locale defaults...");

    let (lang_ok, _) = ssh_cmd(port, 
        "defaults write /var/mobile/Library/Preferences/.GlobalPreferences AppleLanguages -array en 2>/dev/null && \
         defaults write /var/mobile/Library/Preferences/.GlobalPreferences AppleLocale en_US 2>/dev/null && \
         echo 'OK'"
    );
    let language_set = lang_ok;
    slog!("   Locale: {}", if language_set { "✅ Set to en_US" } else { "⚠️ Skipped" });

    // ── 4. Patch CloudConfigurationDetails ────────
    slog!("");
    slog!("📝 Step 4: Patching Cloud Configuration...");

    let cloud_paths = [
        "/mnt1/mobile/Library/ConfigurationProfiles/CloudConfigurationDetails.plist",
        "/var/mobile/Library/ConfigurationProfiles/CloudConfigurationDetails.plist",
    ];

    let cloud_plist = r#"<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CloudConfigurationUIComplete</key>
    <true/>
    <key>IsSupervised</key>
    <false/>
    <key>PostSetupProfileWasInstalled</key>
    <true/>
</dict>
</plist>"#;

    let mut cloud_config_patched = false;
    for cc_path in &cloud_paths {
        let dir = cc_path.rsplit_once('/').map(|(d, _)| d).unwrap_or("/tmp");
        let _ = ssh_cmd(port, &format!("mkdir -p '{dir}'"));
        let _ = ssh_cmd(port, &format!("cp '{cc_path}' '{cc_path}.deepeye.bak' 2>/dev/null"));

        let escaped = cloud_plist.replace('\'', "'\\''");
        let (ok, out) = ssh_cmd(port, &format!("printf '%s' '{escaped}' > '{cc_path}' && echo 'PATCHED'"));
        if ok && out.contains("PATCHED") {
            slog!("   ✅ CloudConfiguration patched at {}", cc_path);
            cloud_config_patched = true;
            break;
        }
    }
    if !cloud_config_patched {
        slog!("   ⚠️ CloudConfiguration patch skipped (path not writable)");
    }

    // ── 5. Clear setup caches ─────────────────────
    slog!("");
    slog!("🧹 Clearing setup caches...");
    let _ = ssh_cmd(port, "rm -f /var/mobile/Library/Caches/com.apple.setupassistant* 2>/dev/null");
    let _ = ssh_cmd(port, "rm -f /var/mobile/Library/Caches/Setup* 2>/dev/null");
    slog!("   ✅ Caches cleared");

    // ── Result ────────────────────────────────────
    let msg = if purplebuddy_patched && setup_app_disabled {
        "✅ Setup.app fully bypassed — device will skip activation screen on next boot."
    } else if purplebuddy_patched {
        "⚠️ PurpleBuddy patched but Setup.app rename failed. Partial bypass."
    } else {
        "❌ Setup.app bypass incomplete — check filesystem write permissions."
    };

    slog!("");
    slog!("{}", msg);

    Ok(SetupAppPatchResult {
        purplebuddy_patched,
        setup_app_disabled,
        language_set,
        cloud_config_patched,
        stage_message: msg.to_string(),
    })
}

/// Restore Setup.app to original state
#[tauri::command]
pub async fn fs_restore_setup_app(
    app: AppHandle,
    ssh_port: Option<u16>,
) -> Result<String, String> {
    let port = ssh_port.unwrap_or(2222);
    let _ = app.emit("fs-log", "🔄 Restoring Setup.app...");

    // Restore renamed Setup.app
    let paths = ["/mnt1/Applications/Setup.app", "/Applications/Setup.app"];
    for p in &paths {
        let _ = ssh_cmd(port, &format!("mv '{p}.deepeye_disabled' '{p}' 2>/dev/null"));
    }

    // Restore backed up plists
    let _ = ssh_cmd(port, "find /mnt1 /mnt2 /var -name '*.deepeye.bak' -exec sh -c 'mv \"$1\" \"${1%.deepeye.bak}\"' _ {} \\; 2>/dev/null");

    let _ = app.emit("fs-log", "✅ Setup.app restored to original state");
    Ok("✅ Setup.app restored".to_string())
}
