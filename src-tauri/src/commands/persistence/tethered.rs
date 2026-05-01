use serde::{Deserialize, Serialize};
use tauri::{AppHandle, Emitter};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TetheredState {
    pub bypass_active: bool,
    pub reboot_safe: bool,
    pub requires_re_exploit: bool,
    pub ssh_accessible: bool,
    pub current_patches: Vec<String>,
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

fn run(bin: &str, args: &[&str]) -> (bool, String) {
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

/// Check tethered bypass state — whether patches survive or need re-application
#[tauri::command]
pub async fn persist_check_tethered(
    app: AppHandle,
    ssh_port: Option<u16>,
) -> Result<TetheredState, String> {
    macro_rules! slog {
        ($msg:expr) => { drop(app.emit("persist-log", $msg.to_string())) };
        ($fmt:literal, $($arg:tt)*) => { drop(app.emit("persist-log", format!($fmt, $($arg)*))) };
    }

    let port = ssh_port.unwrap_or(2222);

    slog!("╔══════════════════════════════════╗");
    slog!("║  PERSISTENCE — TETHERED CHECK    ║");
    slog!("╚══════════════════════════════════╝");
    slog!("");

    // Check SSH
    let (ssh_ok, _) = ssh_cmd(port, "id");
    let ssh_accessible = ssh_ok;

    if !ssh_ok {
        // Try without SSH — check via ideviceinfo
        slog!("⚠️ SSH not accessible — checking via USB...");
        let (usb_ok, act_state) = run("ideviceinfo", &["-k", "ActivationState"]);
        let bypass_active = usb_ok && (act_state.contains("Activated") || act_state.contains("FactoryActivated"));

        return Ok(TetheredState {
            bypass_active,
            reboot_safe: false,
            requires_re_exploit: true,
            ssh_accessible: false,
            current_patches: vec![],
            stage_message: if bypass_active {
                "✅ Bypass active but SSH lost — re-exploit needed after reboot.".to_string()
            } else {
                "❌ Bypass not active and SSH lost — full re-exploit required.".to_string()
            },
        });
    }

    slog!("🔑 SSH connected — checking patch state...");

    let mut patches = Vec::new();

    // Check Setup.app state
    let (_, setup_out) = ssh_cmd(port, "ls -d /Applications/Setup.app /mnt1/Applications/Setup.app 2>/dev/null | head -1");
    let (_, setup_dis) = ssh_cmd(port, "ls -d /Applications/Setup.app.deepeye_disabled /mnt1/Applications/Setup.app.deepeye_disabled 2>/dev/null | head -1");
    if !setup_dis.is_empty() {
        patches.push("Setup.app disabled ✅".to_string());
        slog!("   ✅ Setup.app: Disabled");
    } else if !setup_out.is_empty() {
        slog!("   ⚠️ Setup.app: Active (not patched)");
    }

    // Check PurpleBuddy
    let (_, pb_out) = ssh_cmd(port, "cat /var/mobile/Library/Caches/com.apple.purplebuddy.plist 2>/dev/null | grep -c SetupDone");
    if pb_out.trim() != "0" && !pb_out.is_empty() {
        patches.push("PurpleBuddy patched ✅".to_string());
        slog!("   ✅ PurpleBuddy: Patched");
    } else {
        slog!("   ⚠️ PurpleBuddy: Not patched");
    }

    // Check hosts file
    let (_, hosts_out) = ssh_cmd(port, "grep -c 'DeepEye Bypass' /etc/hosts 2>/dev/null");
    if hosts_out.trim() != "0" && !hosts_out.is_empty() {
        patches.push("Hosts blocked ✅".to_string());
        slog!("   ✅ /etc/hosts: Apple servers blocked");
    } else {
        slog!("   ⚠️ /etc/hosts: Not patched");
    }

    // Check fstab
    let (_, fstab_out) = ssh_cmd(port, "grep -c ' rw ' /etc/fstab 2>/dev/null");
    if fstab_out.trim() != "0" && !fstab_out.is_empty() {
        patches.push("fstab R/W ✅".to_string());
        slog!("   ✅ fstab: R/W mode");
    }

    // Check activation state
    let (_, act_out) = ssh_cmd(port, "defaults read /var/root/Library/Lockdown/data_ark '-ActivationState' 2>/dev/null");
    if act_out.contains("Activated") {
        patches.push("Activation: Activated ✅".to_string());
        slog!("   ✅ Activation: Activated");
    }

    let bypass_active = patches.len() >= 2;

    slog!("");
    slog!("📊 Patch summary: {}/{} applied", patches.len(), 5);
    slog!("   Bypass: {}", if bypass_active { "✅ Active" } else { "⚠️ Incomplete" });
    slog!("   ⚠️ Tethered: Patches lost on reboot — re-exploit required");

    Ok(TetheredState {
        bypass_active,
        reboot_safe: false,
        requires_re_exploit: true,
        ssh_accessible,
        current_patches: patches,
        stage_message: if bypass_active {
            "✅ Tethered bypass active. WARNING: Reboot will require re-exploit via checkm8.".to_string()
        } else {
            "⚠️ Tethered bypass incomplete — some patches missing.".to_string()
        },
    })
}

/// Re-apply tethered bypass after reboot (requires device in DFU + checkm8)
#[tauri::command]
pub async fn persist_reapply_tethered(
    app: AppHandle,
) -> Result<String, String> {
    let _ = app.emit("persist-log", "🔄 Re-applying tethered bypass...");
    let _ = app.emit("persist-log", "");
    let _ = app.emit("persist-log", "Steps required after reboot:");
    let _ = app.emit("persist-log", "  1. Enter DFU mode (Power + Home/Volume Down)");
    let _ = app.emit("persist-log", "  2. Run checkm8 exploit (gaster pwn)");
    let _ = app.emit("persist-log", "  3. Boot ramdisk (irecovery -f ramdisk.dmg)");
    let _ = app.emit("persist-log", "  4. Start SSH tunnel (iproxy 2222 44)");
    let _ = app.emit("persist-log", "  5. Re-apply patches (fs_patch_setup_app, fs_patch_activation, fs_patch_lockdown)");
    let _ = app.emit("persist-log", "");
    let _ = app.emit("persist-log", "💡 Use the full bypass pipeline to automate this sequence.");

    Ok("ℹ️ Tethered re-apply requires DFU + checkm8 exploit sequence. See logs for steps.".to_string())
}
