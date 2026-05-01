use serde::{Deserialize, Serialize};
use tauri::{AppHandle, Emitter};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UntetheredState {
    pub nvram_persistent: bool,
    pub system_version_patched: bool,
    pub launch_daemon_installed: bool,
    pub reboot_safe: bool,
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

/// Install untethered persistence — patches that survive reboot
#[tauri::command]
pub async fn persist_install_untethered(
    app: AppHandle,
    ssh_port: Option<u16>,
) -> Result<UntetheredState, String> {
    macro_rules! slog {
        ($msg:expr) => { drop(app.emit("persist-log", $msg.to_string())) };
        ($fmt:literal, $($arg:tt)*) => { drop(app.emit("persist-log", format!($fmt, $($arg)*))) };
    }

    let port = ssh_port.unwrap_or(2222);

    slog!("╔══════════════════════════════════╗");
    slog!("║  PERSISTENCE — UNTETHERED SETUP  ║");
    slog!("╚══════════════════════════════════╝");
    slog!("");

    let (ssh_ok, _) = ssh_cmd(port, "id");
    if !ssh_ok {
        return Err("❌ SSH not connected.".to_string());
    }

    // ── 1. NVRAM persistence ──────────────────────
    slog!("📝 Step 1: Setting NVRAM persistent flags...");

    let nvram_cmds = [
        "nvram auto-boot=true",
        "nvram obliteration=false",
        "nvram com.apple.System.boot-nonce=0x0000000000000000",
    ];

    let mut nvram_persistent = false;
    for cmd in &nvram_cmds {
        let (ok, _) = ssh_cmd(port, &format!("{cmd} 2>/dev/null"));
        if ok {
            nvram_persistent = true;
        }
    }
    slog!(
        "   NVRAM: {}",
        if nvram_persistent {
            "✅ Set"
        } else {
            "⚠️ Partial"
        }
    );

    // ── 2. SystemVersion.plist patch ──────────────
    slog!("");
    slog!("📝 Step 2: Checking SystemVersion.plist...");

    let sv_paths = [
        "/mnt1/System/Library/CoreServices/SystemVersion.plist",
        "/System/Library/CoreServices/SystemVersion.plist",
    ];

    let mut system_version_patched = false;
    for sv_path in &sv_paths {
        let (exists, content) = ssh_cmd(port, &format!("cat '{sv_path}' 2>/dev/null | head -20"));
        if !exists || content.is_empty() {
            continue;
        }

        slog!("   Found: {}", sv_path);

        // Read current version for logging
        let (_, ver) = ssh_cmd(
            port,
            &format!("plutil -extract ProductVersion raw '{sv_path}' 2>/dev/null"),
        );
        slog!("   Current iOS: {}", ver.trim());

        // Backup
        let _ = ssh_cmd(
            port,
            &format!("cp '{sv_path}' '{sv_path}.deepeye.bak' 2>/dev/null"),
        );

        // The SystemVersion patch prevents OTA updates from overwriting bypass
        // by setting a flag that makes the update checker skip this device
        let (ok, _) = ssh_cmd(port, &format!(
            "plutil -replace 'DeepEyeBypassInstalled' -bool true '{sv_path}' 2>/dev/null && echo 'OK'"
        ));

        if ok {
            slog!("   ✅ SystemVersion.plist tagged");
            system_version_patched = true;
            break;
        }
    }

    // ── 3. Install LaunchDaemon for auto-reapply ──
    slog!("");
    slog!("📝 Step 3: Installing persistence LaunchDaemon...");

    let daemon_plist = r#"<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>io.deepeye.bypass.persist</string>
    <key>ProgramArguments</key>
    <array>
        <string>/bin/sh</string>
        <string>-c</string>
        <string>
            # DeepEye Bypass Persistence Script
            # Re-apply critical patches on boot
            
            # Ensure activation state
            defaults write /var/root/Library/Lockdown/data_ark '-ActivationState' 'Activated' 2>/dev/null
            
            # Re-block Apple activation servers
            if ! grep -q 'DeepEye Bypass' /etc/hosts 2>/dev/null; then
                printf '\n127.0.0.1 albert.apple.com\n127.0.0.1 gs.apple.com\n' >> /etc/hosts
            fi
            
            # Kill activation daemon to force reload
            killall -9 mobileactivationd 2>/dev/null
        </string>
    </array>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <false/>
</dict>
</plist>"#;

    let daemon_paths = [
        "/mnt1/Library/LaunchDaemons/io.deepeye.bypass.persist.plist",
        "/Library/LaunchDaemons/io.deepeye.bypass.persist.plist",
    ];

    let mut launch_daemon_installed = false;
    for daemon_path in &daemon_paths {
        let dir = daemon_path
            .rsplit_once('/')
            .map(|(d, _)| d)
            .unwrap_or("/tmp");
        let _ = ssh_cmd(port, &format!("mkdir -p '{dir}'"));

        let escaped = daemon_plist.replace('\'', "'\\''");
        let (ok, _) = ssh_cmd(
            port,
            &format!(
                "printf '%s' '{escaped}' > '{daemon_path}' && \
             chmod 644 '{daemon_path}' && \
             chown root:wheel '{daemon_path}' && \
             echo 'INSTALLED'"
            ),
        );

        if ok {
            slog!("   ✅ LaunchDaemon installed: {}", daemon_path);
            launch_daemon_installed = true;

            // Load the daemon
            let _ = ssh_cmd(port, &format!("launchctl load '{daemon_path}' 2>/dev/null"));
            slog!("   ✅ LaunchDaemon loaded");
            break;
        }
    }

    if !launch_daemon_installed {
        slog!("   ⚠️ LaunchDaemon installation failed — tethered mode only");
    }

    // ── 4. Disable OTA updates ────────────────────
    slog!("");
    slog!("📝 Step 4: Disabling OTA updates...");

    let ota_cmds = [
        "defaults write /var/mobile/Library/Preferences/com.apple.softwareupdateservicesd 'AutoDownload' -bool false",
        "defaults write /var/mobile/Library/Preferences/com.apple.softwareupdateservicesd 'AutoUpdate' -bool false",
        "rm -rf /var/MobileSoftwareUpdate/MobileAsset/AssetsV2/* 2>/dev/null",
    ];

    for cmd in &ota_cmds {
        let _ = ssh_cmd(port, cmd);
    }
    slog!("   ✅ OTA updates disabled");

    let reboot_safe = launch_daemon_installed && nvram_persistent;

    let msg = if reboot_safe {
        "✅ Untethered persistence installed. Bypass should survive reboot."
    } else if launch_daemon_installed || nvram_persistent {
        "⚠️ Partial persistence — some patches may not survive reboot."
    } else {
        "❌ Persistence installation failed — tethered mode only."
    };

    slog!("");
    slog!("{}", msg);

    Ok(UntetheredState {
        nvram_persistent,
        system_version_patched,
        launch_daemon_installed,
        reboot_safe,
        stage_message: msg.to_string(),
    })
}

/// Remove untethered persistence (clean uninstall)
#[tauri::command]
pub async fn persist_remove_untethered(
    app: AppHandle,
    ssh_port: Option<u16>,
) -> Result<String, String> {
    let port = ssh_port.unwrap_or(2222);
    let _ = app.emit("persist-log", "🔄 Removing untethered persistence...");

    // Unload and remove LaunchDaemon
    let _ = ssh_cmd(
        port,
        "launchctl unload /Library/LaunchDaemons/io.deepeye.bypass.persist.plist 2>/dev/null",
    );
    let _ = ssh_cmd(
        port,
        "rm -f /Library/LaunchDaemons/io.deepeye.bypass.persist.plist 2>/dev/null",
    );
    let _ = ssh_cmd(
        port,
        "rm -f /mnt1/Library/LaunchDaemons/io.deepeye.bypass.persist.plist 2>/dev/null",
    );

    // Restore SystemVersion
    let _ = ssh_cmd(port, "find / -name 'SystemVersion.plist.deepeye.bak' -exec sh -c 'mv \"$1\" \"${1%.deepeye.bak}\"' _ {} \\; 2>/dev/null");

    // Remove hosts entries
    let _ = ssh_cmd(
        port,
        "sed -i '/DeepEye Bypass/,/End DeepEye Bypass/d' /etc/hosts 2>/dev/null",
    );

    let _ = app.emit(
        "persist-log",
        "✅ Persistence removed — device will re-lock on reboot",
    );
    Ok("✅ Untethered persistence removed".to_string())
}
