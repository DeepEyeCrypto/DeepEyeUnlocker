use serde::{Deserialize, Serialize};
use tauri::{AppHandle, Emitter};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LockdownPatchResult {
    pub fstab_patched: bool,
    pub hosts_patched: bool,
    pub nvram_patched: bool,
    pub baseband_ticket_cleared: bool,
    pub lockdown_daemon_restarted: bool,
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

/// Disable lock checks: fstab, hosts redirection, NVRAM, and lockdown daemon
#[tauri::command]
pub async fn fs_patch_lockdown(
    app: AppHandle,
    ssh_port: Option<u16>,
) -> Result<LockdownPatchResult, String> {
    macro_rules! slog {
        ($msg:expr) => { drop(app.emit("fs-log", $msg.to_string())) };
        ($fmt:literal, $($arg:tt)*) => { drop(app.emit("fs-log", format!($fmt, $($arg)*))) };
    }

    let port = ssh_port.unwrap_or(2222);

    slog!("╔══════════════════════════════════╗");
    slog!("║  LOCKDOWN — DISABLE CHECKS       ║");
    slog!("╚══════════════════════════════════╝");
    slog!("");

    let (ssh_ok, _) = ssh_cmd(port, "id");
    if !ssh_ok {
        return Err("❌ SSH not connected.".to_string());
    }

    // ── 1. Patch fstab for R/W mount ──────────────
    slog!("📝 Step 1: Patching fstab for persistent R/W...");

    let fstab_paths = ["/mnt2/etc/fstab", "/etc/fstab", "/mnt1/etc/fstab"];
    let mut fstab_patched = false;

    for fstab in &fstab_paths {
        let (exists, content) = ssh_cmd(port, &format!("cat '{fstab}' 2>/dev/null"));
        if !exists || content.is_empty() {
            continue;
        }

        slog!("   Found: {}", fstab);
        slog!("   Current:");
        for line in content.lines().take(5) {
            slog!("     {}", line);
        }

        // Backup
        let _ = ssh_cmd(port, &format!("cp '{fstab}' '{fstab}.deepeye.bak'"));

        // Replace 'ro' with 'rw' in fstab
        let (ok, _) = ssh_cmd(port, &format!(
            "sed -i 's/ ro / rw /g; s/ ro,/ rw,/g' '{fstab}' && echo 'PATCHED'"
        ));

        if ok {
            slog!("   ✅ fstab patched (ro → rw)");
            fstab_patched = true;
            break;
        }
    }

    if !fstab_patched {
        slog!("   ⚠️ fstab not found or not writable");
    }

    // ── 2. Patch /etc/hosts to block activation checks ──
    slog!("");
    slog!("📝 Step 2: Patching /etc/hosts (block Apple activation servers)...");

    let hosts_entries = "\n\
# DeepEye Bypass — block activation checks\n\
127.0.0.1 albert.apple.com\n\
127.0.0.1 gs.apple.com\n\
127.0.0.1 mesu.apple.com\n\
127.0.0.1 appldnld.apple.com\n\
127.0.0.1 iprofiles.apple.com\n\
127.0.0.1 static.ips.apple.com\n\
# End DeepEye Bypass\n";

    let hosts_paths = ["/mnt2/etc/hosts", "/etc/hosts", "/mnt1/etc/hosts"];
    let mut hosts_patched = false;

    for hosts in &hosts_paths {
        let (exists, _) = ssh_cmd(port, &format!("test -f '{hosts}' && echo 'EXISTS'"));
        if !exists {
            continue;
        }

        // Check if already patched
        let (_, content) = ssh_cmd(port, &format!("cat '{hosts}'"));
        if content.contains("DeepEye Bypass") {
            slog!("   ✅ Already patched: {}", hosts);
            hosts_patched = true;
            break;
        }

        // Backup
        let _ = ssh_cmd(port, &format!("cp '{hosts}' '{hosts}.deepeye.bak'"));

        // Append blocking entries
        let escaped = hosts_entries.replace('\'', "'\\''");
        let (ok, _) = ssh_cmd(port, &format!("printf '%s' '{escaped}' >> '{hosts}' && echo 'PATCHED'"));

        if ok {
            slog!("   ✅ /etc/hosts patched at {}", hosts);
            hosts_patched = true;
            break;
        }
    }

    if !hosts_patched {
        slog!("   ⚠️ /etc/hosts not writable");
    }

    // ── 3. NVRAM patches ──────────────────────────
    slog!("");
    slog!("📝 Step 3: NVRAM obliteration patches...");

    let nvram_cmds = [
        ("auto-boot", "nvram auto-boot=true 2>/dev/null"),
        ("obliteration", "nvram obliteration=false 2>/dev/null"),
        ("boot-args verbose", "nvram boot-args='-v' 2>/dev/null"),
    ];

    let mut nvram_patched = false;
    for (label, cmd) in &nvram_cmds {
        let (ok, _) = ssh_cmd(port, cmd);
        if ok {
            slog!("   ✅ {}", label);
            nvram_patched = true;
        } else {
            slog!("   ⚠️ {} — skipped", label);
        }
    }

    // ── 4. Clear baseband ticket ──────────────────
    slog!("");
    slog!("📝 Step 4: Clearing baseband ticket cache...");

    let bb_paths = [
        "/var/wireless/Library/Caches/com.apple.BasebandManager/",
        "/mnt2/wireless/Library/Caches/com.apple.BasebandManager/",
    ];

    let mut baseband_ticket_cleared = false;
    for bb_path in &bb_paths {
        let (ok, _) = ssh_cmd(port, &format!("rm -rf '{bb_path}'* 2>/dev/null && echo 'CLEARED'"));
        if ok {
            slog!("   ✅ Baseband cache cleared: {}", bb_path);
            baseband_ticket_cleared = true;
            break;
        }
    }

    if !baseband_ticket_cleared {
        slog!("   ⚠️ Baseband cache not found or not writable");
    }

    // ── 5. Restart lockdownd ──────────────────────
    slog!("");
    slog!("📝 Step 5: Restarting lockdownd...");

    let (restart_ok, restart_out) = ssh_cmd(port,
        "killall -9 lockdownd 2>/dev/null; \
         killall -9 mobileactivationd 2>/dev/null; \
         killall -9 SpringBoard 2>/dev/null; \
         sleep 1 && echo 'RESTARTED'"
    );

    let lockdown_daemon_restarted = restart_ok && restart_out.contains("RESTARTED");
    slog!("   Daemons: {}", if lockdown_daemon_restarted { "✅ Restarted" } else { "⚠️ Partial" });

    let msg = if fstab_patched && hosts_patched {
        "✅ Lock checks fully disabled. Activation server blocked, filesystem R/W."
    } else if fstab_patched || hosts_patched || nvram_patched {
        "⚠️ Partial lockdown patches applied. Some checks may still trigger."
    } else {
        "❌ Lockdown patches failed — check filesystem access."
    };

    slog!("");
    slog!("{}", msg);

    Ok(LockdownPatchResult {
        fstab_patched,
        hosts_patched,
        nvram_patched,
        baseband_ticket_cleared,
        lockdown_daemon_restarted,
        stage_message: msg.to_string(),
    })
}

/// Restore all lockdown patches (undo bypass)
#[tauri::command]
pub async fn fs_restore_lockdown(
    app: AppHandle,
    ssh_port: Option<u16>,
) -> Result<String, String> {
    let port = ssh_port.unwrap_or(2222);
    let _ = app.emit("fs-log", "🔄 Restoring lockdown state...");

    // Restore fstab, hosts from backups
    let _ = ssh_cmd(port, "find /mnt1 /mnt2 /etc /var -name '*.deepeye.bak' -exec sh -c 'mv \"$1\" \"${1%.deepeye.bak}\"' _ {} \\; 2>/dev/null");

    // Remove hosts entries
    let _ = ssh_cmd(port, "sed -i '/DeepEye Bypass/,/End DeepEye Bypass/d' /etc/hosts 2>/dev/null");

    // Restart daemons
    let _ = ssh_cmd(port, "killall -9 lockdownd mobileactivationd 2>/dev/null");

    let _ = app.emit("fs-log", "✅ Lockdown state restored");
    Ok("✅ Lockdown restored".to_string())
}
