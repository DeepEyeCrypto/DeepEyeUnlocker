use serde::{Deserialize, Serialize};
use tauri::{AppHandle, Emitter};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SshTunnel {
    pub local_port: u16,
    pub remote_port: u16,
    pub connected: bool,
    pub device_root: bool,
    pub mount_state: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MountResult {
    pub tunnel: SshTunnel,
    pub mnt1_writable: bool,
    pub mnt2_writable: bool,
    pub disk_info: String,
    pub stage_message: String,
}

fn path_env() -> String {
    let base = std::env::var("PATH").unwrap_or_else(|_| "/usr/bin:/bin".to_string());
    format!("/usr/local/bin:/opt/homebrew/bin:{base}")
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

fn ssh_cmd(port: u16, cmd: &str) -> (bool, String) {
    run(
        "sshpass",
        &[
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
        ],
    )
}

/// Start iproxy tunnel: forwards local_port -> device USB SSH port (44 or 22)
#[tauri::command]
pub async fn fs_start_tunnel(
    app: AppHandle,
    local_port: Option<u16>,
    device_port: Option<u16>,
) -> Result<SshTunnel, String> {
    macro_rules! slog {
        ($msg:expr) => { drop(app.emit("fs-log", $msg.to_string())) };
        ($fmt:literal, $($arg:tt)*) => { drop(app.emit("fs-log", format!($fmt, $($arg)*))) };
    }

    let lport = local_port.unwrap_or(2222);
    let dport = device_port.unwrap_or(44);

    slog!("╔══════════════════════════════════╗");
    slog!("║  FILESYSTEM — SSH TUNNEL         ║");
    slog!("╚══════════════════════════════════╝");
    slog!("");

    // Kill any existing iproxy on this port
    slog!("🔧 Cleaning up existing tunnels on port {}...", lport);
    let _ = run("pkill", &["-f", &format!("iproxy {lport}")]);
    std::thread::sleep(std::time::Duration::from_millis(500));

    // Start iproxy in background
    slog!("📡 Starting iproxy {} -> {} ...", lport, dport);
    let iproxy_result = std::process::Command::new("iproxy")
        .env("PATH", path_env())
        .args([&lport.to_string(), &dport.to_string()])
        .stdout(std::process::Stdio::null())
        .stderr(std::process::Stdio::null())
        .spawn();

    match iproxy_result {
        Ok(_child) => {
            slog!("   ✅ iproxy started (PID active)");
        }
        Err(e) => {
            // Try port 22 fallback
            slog!("   ⚠️ iproxy failed on port {}: {}", dport, e);
            if dport == 44 {
                slog!("   ↻ Retrying with port 22...");
                match std::process::Command::new("iproxy")
                    .env("PATH", path_env())
                    .args([&lport.to_string(), "22"])
                    .stdout(std::process::Stdio::null())
                    .stderr(std::process::Stdio::null())
                    .spawn()
                {
                    Ok(_) => slog!("   ✅ iproxy started on fallback port 22"),
                    Err(e2) => {
                        return Err(format!(
                            "❌ iproxy not found or failed: {}\n\
                             💡 Install: brew install libusbmuxd",
                            e2
                        ));
                    }
                }
            } else {
                return Err(format!("❌ iproxy failed: {e}"));
            }
        }
    }

    // Wait for tunnel to establish
    std::thread::sleep(std::time::Duration::from_secs(2));

    // Test SSH connection
    slog!("");
    slog!("🔑 Testing SSH connection (root@localhost:{})...", lport);

    let mut connected = false;
    let mut device_root = false;

    for attempt in 1..=3 {
        let (ok, out) = ssh_cmd(lport, "id");
        slog!(
            "   [{}/3] SSH: {} — {}",
            attempt,
            if ok { "✅" } else { "⚠️" },
            out.lines().next().unwrap_or("(none)")
        );

        if ok {
            connected = true;
            device_root = out.contains("uid=0") || out.contains("root");
            break;
        }
        std::thread::sleep(std::time::Duration::from_secs(2));
    }

    if !connected {
        return Err("❌ SSH connection failed after 3 attempts.\n\
             💡 Ensure device is jailbroken and SSH is running.\n\
             💡 Default credentials: root / alpine"
            .to_string());
    }

    slog!("   ✅ SSH connected (root={})", device_root);

    Ok(SshTunnel {
        local_port: lport,
        remote_port: dport,
        connected,
        device_root,
        mount_state: "tunnel_ready".to_string(),
    })
}

/// Mount device filesystem read-write via SSH
#[tauri::command]
pub async fn fs_mount_readwrite(
    app: AppHandle,
    ssh_port: Option<u16>,
) -> Result<MountResult, String> {
    macro_rules! slog {
        ($msg:expr) => { drop(app.emit("fs-log", $msg.to_string())) };
        ($fmt:literal, $($arg:tt)*) => { drop(app.emit("fs-log", format!($fmt, $($arg)*))) };
    }

    let port = ssh_port.unwrap_or(2222);

    slog!("╔══════════════════════════════════╗");
    slog!("║  FILESYSTEM — MOUNT R/W          ║");
    slog!("╚══════════════════════════════════╝");
    slog!("");

    // Check SSH connection
    let (ssh_ok, ssh_out) = ssh_cmd(port, "id");
    if !ssh_ok {
        return Err(format!(
            "❌ SSH not connected: {ssh_out}\n💡 Run fs_start_tunnel first."
        ));
    }
    let device_root = ssh_out.contains("uid=0");
    slog!("🔑 SSH OK (root={})", device_root);

    // Check current mount state
    slog!("");
    slog!("💾 Checking mount state...");

    let (_, mount_out) = ssh_cmd(port, "mount | grep -E '/mnt|/dev/disk'");
    slog!("   Current mounts:");
    for line in mount_out.lines().take(8) {
        slog!("   {}", line);
    }

    // Remount root filesystem read-write
    slog!("");
    slog!("🔓 Remounting / read-write...");
    let (rw_ok, rw_out) = ssh_cmd(port, "mount -o rw,union,update /");
    slog!(
        "   Result: {} — {}",
        if rw_ok { "✅" } else { "⚠️" },
        rw_out.lines().next().unwrap_or("done")
    );

    // Mount data partition
    slog!("🔓 Mounting /mnt1 (System)...");
    let (m1_ok, m1_out) = ssh_cmd(port, "mount -t hfs /dev/disk0s1s1 /mnt1 2>/dev/null || mount -t apfs /dev/disk0s1s1 /mnt1 2>/dev/null || echo 'already_mounted'");
    let mnt1_writable = m1_ok || m1_out.contains("already");
    slog!(
        "   /mnt1: {} — {}",
        if mnt1_writable { "✅" } else { "⚠️" },
        m1_out.lines().next().unwrap_or("done")
    );

    slog!("🔓 Mounting /mnt2 (Data)...");
    let (m2_ok, m2_out) = ssh_cmd(port, "mount -t hfs /dev/disk0s1s2 /mnt2 2>/dev/null || mount -t apfs /dev/disk0s1s2 /mnt2 2>/dev/null || echo 'already_mounted'");
    let mnt2_writable = m2_ok || m2_out.contains("already");
    slog!(
        "   /mnt2: {} — {}",
        if mnt2_writable { "✅" } else { "⚠️" },
        m2_out.lines().next().unwrap_or("done")
    );

    // Get disk info
    slog!("");
    slog!("📊 Disk info:");
    let (_, df_out) = ssh_cmd(port, "df -h / /mnt1 /mnt2 2>/dev/null | head -10");
    for line in df_out.lines() {
        slog!("   {}", line);
    }

    let mount_state = if mnt1_writable && mnt2_writable {
        "rw_full".to_string()
    } else if mnt1_writable || mnt2_writable {
        "rw_partial".to_string()
    } else {
        "read_only".to_string()
    };

    let msg = if mnt1_writable && mnt2_writable {
        "✅ Filesystem fully mounted read-write. Ready for patching."
    } else {
        "⚠️ Partial mount — some partitions may be read-only."
    };

    slog!("");
    slog!("{}", msg);

    Ok(MountResult {
        tunnel: SshTunnel {
            local_port: port,
            remote_port: 44,
            connected: true,
            device_root,
            mount_state,
        },
        mnt1_writable,
        mnt2_writable,
        disk_info: df_out,
        stage_message: msg.to_string(),
    })
}

/// List filesystem contents at a path
#[tauri::command]
pub async fn fs_list_path(
    app: AppHandle,
    ssh_port: Option<u16>,
    path: String,
) -> Result<String, String> {
    let port = ssh_port.unwrap_or(2222);
    let _ = app.emit("fs-log", format!("📂 Listing: {path}"));

    let (ok, out) = ssh_cmd(port, &format!("ls -la '{path}' 2>&1 | head -50"));
    if !ok && out.contains("not found") {
        return Err(format!("❌ SSH failed: {out}"));
    }
    Ok(out)
}

/// Read a file from device filesystem
#[tauri::command]
pub async fn fs_read_file(
    app: AppHandle,
    ssh_port: Option<u16>,
    remote_path: String,
) -> Result<String, String> {
    let port = ssh_port.unwrap_or(2222);
    let _ = app.emit("fs-log", format!("📖 Reading: {remote_path}"));

    let (ok, out) = ssh_cmd(port, &format!("cat '{remote_path}' 2>&1"));
    if !ok {
        return Err(format!("❌ Failed to read {remote_path}: {out}"));
    }
    Ok(out)
}

/// Write content to a file on device filesystem
#[tauri::command]
pub async fn fs_write_file(
    app: AppHandle,
    ssh_port: Option<u16>,
    remote_path: String,
    content: String,
) -> Result<String, String> {
    let port = ssh_port.unwrap_or(2222);
    let _ = app.emit("fs-log", format!("✏️ Writing: {remote_path}"));

    // Backup original first
    let _ = ssh_cmd(
        port,
        &format!("cp '{remote_path}' '{remote_path}.deepeye.bak' 2>/dev/null"),
    );

    // Write via heredoc
    let escaped = content.replace('\'', "'\\''");
    let (ok, out) = ssh_cmd(
        port,
        &format!("printf '%s' '{escaped}' > '{remote_path}' && echo 'OK'"),
    );

    if !ok || !out.contains("OK") {
        return Err(format!("❌ Write failed: {out}"));
    }

    let _ = app.emit("fs-log", format!("   ✅ Written {} bytes", content.len()));
    Ok(format!("✅ Written to {remote_path}"))
}

/// Push a local file to device via scp
#[tauri::command]
pub async fn fs_push_file(
    app: AppHandle,
    ssh_port: Option<u16>,
    local_path: String,
    remote_path: String,
) -> Result<String, String> {
    let port = ssh_port.unwrap_or(2222);
    let _ = app.emit(
        "fs-log",
        format!("📤 Pushing: {local_path} → {remote_path}"),
    );

    let (ok, out) = run(
        "sshpass",
        &[
            "-p",
            "alpine",
            "scp",
            "-o",
            "StrictHostKeyChecking=no",
            "-o",
            "UserKnownHostsFile=/dev/null",
            "-P",
            &port.to_string(),
            &local_path,
            &format!("root@localhost:{remote_path}"),
        ],
    );

    if !ok {
        return Err(format!("❌ SCP push failed: {out}"));
    }

    let _ = app.emit("fs-log", "   ✅ File pushed successfully");
    Ok(format!("✅ Pushed to {remote_path}"))
}

/// Pull a file from device to local
#[tauri::command]
pub async fn fs_pull_file(
    app: AppHandle,
    ssh_port: Option<u16>,
    remote_path: String,
    local_path: String,
) -> Result<String, String> {
    let port = ssh_port.unwrap_or(2222);
    let _ = app.emit(
        "fs-log",
        format!("📥 Pulling: {remote_path} → {local_path}"),
    );

    let (ok, out) = run(
        "sshpass",
        &[
            "-p",
            "alpine",
            "scp",
            "-o",
            "StrictHostKeyChecking=no",
            "-o",
            "UserKnownHostsFile=/dev/null",
            "-P",
            &port.to_string(),
            &format!("root@localhost:{remote_path}"),
            &local_path,
        ],
    );

    if !ok {
        return Err(format!("❌ SCP pull failed: {out}"));
    }

    let _ = app.emit("fs-log", "   ✅ File pulled successfully");
    Ok(format!("✅ Pulled to {local_path}"))
}
