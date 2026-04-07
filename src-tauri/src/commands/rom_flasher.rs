use serde::Serialize;
use std::path::PathBuf;
use tauri::AppHandle;
use tauri::Manager;
use tauri_plugin_shell::process::CommandEvent;
use tauri_plugin_shell::ShellExt;

fn get_tool_path(app: &AppHandle, tool: &str) -> Result<PathBuf, String> {
    #[cfg(target_os = "windows")]
    let (resource_subdir, executable_name) = {
        let exe_name = if tool.ends_with(".exe") {
            tool.to_string()
        } else {
            format!("{tool}.exe")
        };
        ("windows", exe_name)
    };

    #[cfg(target_os = "linux")]
    let (resource_subdir, executable_name) = ("linux", tool.to_string());

    #[cfg(all(not(target_os = "windows"), not(target_os = "linux")))]
    let (resource_subdir, executable_name) = ("macos", tool.to_string());

    let resource_path = app
        .path()
        .resource_dir()
        .map_err(|e| format!("resource dir error: {e}"))?
        .join(resource_subdir)
        .join(executable_name);

    let resolved = if resource_path.exists() {
        #[cfg(unix)]
        {
            use std::os::unix::fs::PermissionsExt;
            std::fs::set_permissions(&resource_path, std::fs::Permissions::from_mode(0o755))
                .map_err(|e| format!("chmod error: {e}"))?;
        }
        resource_path
    } else {
        PathBuf::from(tool)
    };

    Ok(resolved)
}

#[derive(Serialize)]
pub struct FlashResult {
    pub success: bool,
    pub partition: String,
    pub message: String,
}

/// Flash a custom ROM ZIP via TWRP sideload (device must be in TWRP sideload mode)
#[tauri::command]
pub async fn rom_sideload_zip(app: AppHandle, zip_path: String) -> Result<FlashResult, String> {
    if !zip_path.ends_with(".zip") {
        return Err("Only .zip files are allowed for sideload".into());
    }

    let adb = get_tool_path(&app, "adb")?;
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command(adb.to_str().ok_or("invalid adb path")?)
        .args(["sideload", &zip_path])
        .spawn()
        .map_err(|e| format!("adb sideload spawn error: {e}"))?;

    let mut out = String::new();
    let mut err = String::new();

    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(b) => out.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Stderr(b) => err.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Error(e) => return Err(e),
            CommandEvent::Terminated(s) => {
                if s.code.unwrap_or(-1) != 0 {
                    return Ok(FlashResult {
                        success: false,
                        partition: "sideload".into(),
                        message: format!("Sideload failed\nstderr: {err}"),
                    });
                }
                break;
            }
            _ => {}
        }
    }

    Ok(FlashResult {
        success: true,
        partition: "sideload".into(),
        message: format!("ZIP sideloaded successfully\n{}", out.trim()),
    })
}

/// Flash a partition image via fastboot
#[tauri::command]
pub async fn rom_flash_partition(
    app: AppHandle,
    partition: String,
    image_path: String,
) -> Result<FlashResult, String> {
    let allowed = [
        "boot",
        "recovery",
        "system",
        "vendor",
        "dtbo",
        "vbmeta",
        "cache",
        "userdata",
        "product",
        "system_ext",
    ];
    if !allowed.contains(&partition.as_str()) {
        return Err(format!(
            "Partition '{}' not in allowed flash list",
            partition
        ));
    }

    let fastboot = get_tool_path(&app, "fastboot")?;
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command(fastboot.to_str().ok_or("invalid fastboot path")?)
        .args(["flash", &partition, &image_path])
        .spawn()
        .map_err(|e| format!("fastboot flash spawn error: {e}"))?;

    let mut out = String::new();
    let mut err = String::new();

    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(b) => out.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Stderr(b) => err.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Error(e) => return Err(e),
            CommandEvent::Terminated(s) => {
                if s.code.unwrap_or(-1) != 0 {
                    return Ok(FlashResult {
                        success: false,
                        partition: partition.clone(),
                        message: format!("Flash failed\nstderr: {err}"),
                    });
                }
                break;
            }
            _ => {}
        }
    }

    Ok(FlashResult {
        success: true,
        partition,
        message: format!("Partition flashed\n{}", out.trim()),
    })
}

/// Wipe partitions via fastboot (for clean flash)
#[tauri::command]
pub async fn rom_wipe_data(app: AppHandle) -> Result<String, String> {
    let fastboot = get_tool_path(&app, "fastboot")?;
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command(fastboot.to_str().ok_or("invalid fastboot path")?)
        .args(["-w"])
        .spawn()
        .map_err(|e| format!("fastboot wipe spawn error: {e}"))?;

    let mut out = String::new();
    let mut err = String::new();

    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(b) => out.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Stderr(b) => err.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Error(e) => return Err(e),
            CommandEvent::Terminated(s) => {
                if s.code.unwrap_or(-1) != 0 {
                    return Err(format!("wipe failed\nstderr: {err}"));
                }
                break;
            }
            _ => {}
        }
    }

    Ok(format!("Data wiped\n{}", out.trim()))
}

/// Reboot to recovery (for TWRP sideload workflow)
#[tauri::command]
pub async fn rom_reboot_recovery(app: AppHandle) -> Result<String, String> {
    let adb = get_tool_path(&app, "adb")?;
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command(adb.to_str().ok_or("invalid adb path")?)
        .args(["reboot", "recovery"])
        .spawn()
        .map_err(|e| format!("adb reboot recovery error: {e}"))?;

    let mut out = String::new();
    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(b) => out.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Terminated(_) => break,
            _ => {}
        }
    }

    Ok("Rebooting to recovery".to_string())
}

/// Reboot to bootloader (for fastboot flash workflow)
#[tauri::command]
pub async fn rom_reboot_bootloader(app: AppHandle) -> Result<String, String> {
    let adb = get_tool_path(&app, "adb")?;
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command(adb.to_str().ok_or("invalid adb path")?)
        .args(["reboot", "bootloader"])
        .spawn()
        .map_err(|e| format!("adb reboot bootloader error: {e}"))?;

    let mut out = String::new();
    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(b) => out.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Terminated(_) => break,
            _ => {}
        }
    }

    Ok("Rebooting to bootloader".to_string())
}
