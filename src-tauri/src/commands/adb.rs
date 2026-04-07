use serde::Serialize;
use std::process::Command;
use tauri_plugin_shell::ShellExt;
use tauri_plugin_shell::process::CommandEvent;
use tauri::Emitter;

#[derive(Debug, Serialize, Clone)]
pub struct AdbDevice {
    pub serial: String,
    pub state: String,
    pub model: String,
    pub android_version: String,
    pub sdk_version: String,
}

#[derive(Debug, Serialize, Clone)]
pub struct DeviceFullInfo {
    pub serial: String,
    pub model: String,
    pub brand: String,
    pub android_version: String,
    pub sdk_int: String,
    pub build_id: String,
    pub security_patch: String,
    pub bootloader_status: String,
    pub root_status: bool,
    pub frp_status: String,
    pub battery_level: String,
    pub imei: String,
}

#[allow(dead_code)]
#[derive(Debug, thiserror::Error, Serialize)]
pub enum AdbError {
    #[error("ADB binary not found")]
    AdbNotFound,
    #[error("Device not found: {0}")]
    DeviceNotFound(String),
    #[error("ADB command failed: {0}")]
    CommandFailed(String),
    #[error("Permission denied — check ADB authorization")]
    PermissionDenied,
    #[error("Device offline")]
    DeviceOffline,
}

fn adb_bin() -> Result<String, AdbError> {
    let candidates = ["adb", "/usr/bin/adb", "/usr/local/bin/adb"];
    for bin in candidates {
        if Command::new(bin).arg("version").output().is_ok() {
            return Ok(bin.to_string());
        }
    }
    Err(AdbError::AdbNotFound)
}

fn run_adb(args: &[&str]) -> Result<String, AdbError> {
    let bin = adb_bin()?;
    let out = Command::new(&bin)
        .args(args)
        .output()
        .map_err(|e| AdbError::CommandFailed(e.to_string()))?;
    let stdout = String::from_utf8_lossy(&out.stdout).trim().to_string();
    let stderr = String::from_utf8_lossy(&out.stderr).trim().to_string();
    if !out.status.success() && !stderr.is_empty() {
        if stderr.contains("Permission denied") {
            return Err(AdbError::PermissionDenied);
        }
        return Err(AdbError::CommandFailed(stderr));
    }
    Ok(stdout)
}

pub fn adb_devices() -> Result<Vec<AdbDevice>, AdbError> {
    let output = run_adb(&["devices", "-l"])?;
    let mut devices = Vec::new();

    for line in output.lines().skip(1) {
        let line = line.trim();
        if line.is_empty() || line.starts_with('*') {
            continue;
        }
        let parts: Vec<&str> = line.splitn(2, char::is_whitespace).collect();
        if parts.len() < 2 {
            continue;
        }
        let serial = parts[0].to_string();
        let rest = parts[1].trim();
        let state = rest.split_whitespace().next().unwrap_or("unknown").to_string();

        let model = rest
            .split_whitespace()
            .find(|s| s.starts_with("model:"))
            .map(|s| s.trim_start_matches("model:").to_string())
            .unwrap_or_default();

        let android_version = adb_get_prop(&serial, "ro.build.version.release")
            .unwrap_or_default();
        let sdk_version = adb_get_prop(&serial, "ro.build.version.sdk")
            .unwrap_or_default();

        devices.push(AdbDevice {
            serial,
            state,
            model,
            android_version,
            sdk_version,
        });
    }
    Ok(devices)
}

pub fn adb_shell(serial: &str, cmd: &str) -> Result<String, AdbError> {
    run_adb(&["-s", serial, "shell", cmd])
}

pub fn adb_get_prop(serial: &str, prop: &str) -> Result<String, AdbError> {
    let cmd = format!("getprop {}", prop);
    run_adb(&["-s", serial, "shell", &cmd])
}

pub fn adb_reboot(serial: &str, mode: &str) -> Result<(), AdbError> {
    match mode {
        "system" => run_adb(&["-s", serial, "reboot"])?,
        _ => run_adb(&["-s", serial, "reboot", mode])?,
    };
    Ok(())
}

pub fn adb_install(serial: &str, apk_path: &str) -> Result<String, AdbError> {
    run_adb(&["-s", serial, "install", "-r", apk_path])
}

pub fn adb_push(serial: &str, local: &str, remote: &str) -> Result<(), AdbError> {
    run_adb(&["-s", serial, "push", local, remote])?;
    Ok(())
}

pub fn adb_pull(serial: &str, remote: &str, local: &str) -> Result<(), AdbError> {
    run_adb(&["-s", serial, "pull", remote, local])?;
    Ok(())
}

pub fn adb_sideload(serial: &str, zip_path: &str) -> Result<(), AdbError> {
    run_adb(&["-s", serial, "sideload", zip_path])?;
    Ok(())
}

pub fn adb_erase_frp(serial: &str) -> Result<(), AdbError> {
    // get block size first
    let size_str = adb_shell(
        serial,
        "blockdev --getsize64 /dev/block/by-name/frp 2>/dev/null || echo 524288",
    )?;
    let size: u64 = size_str.trim().parse().unwrap_or(524288);
    let cmd = format!(
        "dd if=/dev/zero of=/dev/block/by-name/frp bs=4096 count={}",
        size.div_ceil(4096)
    );
    adb_shell(serial, &cmd)?;
    Ok(())
}

pub fn adb_check_root(serial: &str) -> Result<bool, AdbError> {
    let out = adb_shell(serial, "id")?;
    Ok(out.contains("uid=0"))
}

pub fn adb_get_device_info(serial: &str) -> Result<DeviceFullInfo, AdbError> {
    let model = adb_get_prop(serial, "ro.product.model").unwrap_or_default();
    let brand = adb_get_prop(serial, "ro.product.brand").unwrap_or_default();
    let android_version = adb_get_prop(serial, "ro.build.version.release").unwrap_or_default();
    let sdk_int = adb_get_prop(serial, "ro.build.version.sdk").unwrap_or_default();
    let build_id = adb_get_prop(serial, "ro.build.id").unwrap_or_default();
    let security_patch = adb_get_prop(serial, "ro.build.version.security_patch").unwrap_or_default();

    let bl_raw = adb_get_prop(serial, "ro.boot.verifiedbootstate").unwrap_or_default();
    let bootloader_status = if bl_raw.contains("green") {
        "Locked".to_string()
    } else if bl_raw.contains("orange") {
        "Unlocked".to_string()
    } else {
        bl_raw
    };

    let root_status = adb_check_root(serial).unwrap_or(false);

    let frp_raw = adb_shell(
        serial,
        "getprop ro.frp.pst 2>/dev/null || echo unknown",
    )
    .unwrap_or_default();
    let frp_status = if frp_raw.trim().is_empty() || frp_raw.contains("unknown") {
        "Unknown".to_string()
    } else {
        frp_raw.trim().to_string()
    };

    let battery_raw = adb_shell(
        serial,
        "dumpsys battery | grep level | awk '{print $2}'",
    )
    .unwrap_or_default();
    let battery_level = format!("{}%", battery_raw.trim());

    let imei = adb_shell(
        serial,
        "service call iphonesubinfo 1 | awk -F\"'\" '{print $2}' | tr -d '.' | tr -d '\\n'",
    )
    .unwrap_or("Unavailable".to_string());

    Ok(DeviceFullInfo {
        serial: serial.to_string(),
        model,
        brand,
        android_version,
        sdk_int,
        build_id,
        security_patch,
        bootloader_status,
        root_status,
        frp_status,
        battery_level,
        imei,
    })
}

// ── Tauri Commands ──────────────────────────────────────────────

#[tauri::command]
pub fn adb_list_devices() -> Result<Vec<AdbDevice>, String> {
    adb_devices().map_err(|e| e.to_string())
}

#[tauri::command]
pub fn adb_get_full_info(serial: String) -> Result<DeviceFullInfo, String> {
    adb_get_device_info(&serial).map_err(|e| e.to_string())
}

#[tauri::command]
pub fn adb_shell_command(serial: String, cmd: String) -> Result<String, String> {
    adb_shell(&serial, &cmd).map_err(|e| e.to_string())
}

#[tauri::command]
pub fn adb_reboot_device(serial: String, mode: String) -> Result<(), String> {
    adb_reboot(&serial, &mode).map_err(|e| e.to_string())
}

#[tauri::command]
pub fn adb_install_apk(serial: String, apk_path: String) -> Result<String, String> {
    adb_install(&serial, &apk_path).map_err(|e| e.to_string())
}

#[tauri::command]
pub fn adb_push_file(serial: String, local: String, remote: String) -> Result<(), String> {
    adb_push(&serial, &local, &remote).map_err(|e| e.to_string())
}

#[tauri::command]
pub fn adb_pull_file(serial: String, remote: String, local: String) -> Result<(), String> {
    adb_pull(&serial, &remote, &local).map_err(|e| e.to_string())
}

#[tauri::command]
pub fn adb_sideload_zip(serial: String, zip_path: String) -> Result<(), String> {
    adb_sideload(&serial, &zip_path).map_err(|e| e.to_string())
}

#[tauri::command]
pub fn adb_erase_frp_partition(serial: String) -> Result<(), String> {
    adb_erase_frp(&serial).map_err(|e| e.to_string())
}

#[tauri::command]
pub fn adb_check_root_access(serial: String) -> Result<bool, String> {
    adb_check_root(&serial).map_err(|e| e.to_string())
}

#[tauri::command]
pub fn adb_test_binary(path: String) -> Result<String, String> {
    let binary = if path.trim().is_empty() { "adb" } else { path.trim() };
    let output = Command::new(binary)
        .arg("version")
        .output()
        .map_err(|e| format!("failed to execute {binary}: {e}"))?;

    let stdout = String::from_utf8_lossy(&output.stdout).trim().to_string();
    let stderr = String::from_utf8_lossy(&output.stderr).trim().to_string();

    if output.status.success() {
        if stdout.is_empty() {
            Ok(stderr)
        } else {
            Ok(stdout)
        }
    } else if stderr.is_empty() {
        Err(stdout)
    } else {
        Err(stderr)
    }
}


#[tauri::command]
pub async fn stream_adb_logs(
    app: tauri::AppHandle,
    device_serial: Option<String>,
) -> Result<(), String> {
    let mut args = vec!["logcat", "-v", "time"];
    
    // If a serial is provided, target that device
    let serial_str;
    if let Some(s) = device_serial {
        serial_str = s;
        args.insert(0, "-s");
        args.insert(1, &serial_str);
    }

    let (mut rx, _child) = app.shell()
        .command("adb")
        .args(args)
        .spawn()
        .map_err(|e| e.to_string())?;

    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(line_bytes) => {
                let line = String::from_utf8_lossy(&line_bytes).to_string();
                app.emit("adb-log-line", line).ok();
            }
            CommandEvent::Stderr(error_bytes) => {
                let error = String::from_utf8_lossy(&error_bytes).to_string();
                app.emit("adb-log-error", error).ok();
            }
            CommandEvent::Terminated(status) => {
                app.emit("adb-log-terminated", format!("Exit code: {:?}", status.code)).ok();
                break;
            }
            _ => {}
        }
    }

    Ok(())
}
