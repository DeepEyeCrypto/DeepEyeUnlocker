use tauri::AppHandle;
use tauri::Manager;
use tauri_plugin_shell::ShellExt;
use tauri_plugin_shell::process::CommandEvent;
use serde::Serialize;

#[derive(Serialize)]
pub struct EdlDeviceInfo {
    pub detected: bool,
    pub chipset: String,
    pub serial: String,
    pub mode: String,
}

fn get_edl_tool_path(app: &AppHandle, tool: &str) -> Result<std::path::PathBuf, String> {
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
            std::fs::set_permissions(
                &resource_path,
                std::fs::Permissions::from_mode(0o755),
            )
            .map_err(|e| format!("chmod error: {e}"))?;
        }
        resource_path
    } else {
        std::path::PathBuf::from(tool)
    };

    Ok(resolved)
}

#[tauri::command]
pub async fn edl_detect_device(app: AppHandle) -> Result<EdlDeviceInfo, String> {
    let tool = get_edl_tool_path(&app, "edl")?;
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command(tool.to_str().ok_or("invalid edl path")?)
        .args(["printgpt"])
        .spawn()
        .map_err(|e| format!("edl detect spawn error: {e}"))?;

    let mut out = String::new();
    let mut err = String::new();

    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(b) => out.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Stderr(b) => err.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Terminated(s) => {
                if s.code.unwrap_or(-1) != 0 {
                    return Ok(EdlDeviceInfo {
                        detected: false,
                        chipset: String::new(),
                        serial: String::new(),
                        mode: "not_detected".to_string(),
                    });
                }
                break;
            }
            _ => {}
        }
    }

    let chipset = out.lines()
        .find(|l| l.contains("Chipset"))
        .map(|l| l.split(':').nth(1).unwrap_or("").trim().to_string())
        .unwrap_or_else(|| "Qualcomm".to_string());

    let serial = out.lines()
        .find(|l| l.contains("Serial"))
        .map(|l| l.split(':').nth(1).unwrap_or("").trim().to_string())
        .unwrap_or_default();

    Ok(EdlDeviceInfo {
        detected: true,
        chipset,
        serial,
        mode: "edl_9008".to_string(),
    })
}

#[tauri::command]
pub async fn edl_read_partition(
    app: AppHandle,
    partition: String,
    output_path: String,
) -> Result<String, String> {
    let allowed = ["boot", "recovery", "system", "userdata", "modem", "fsg", "persist", "misc", "aboot", "sbl1", "tz", "rpm"];
    if !allowed.contains(&partition.as_str()) {
        return Err(format!("Partition '{}' not in allowed list", partition));
    }

    let tool = get_edl_tool_path(&app, "edl")?;
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command(tool.to_str().ok_or("invalid edl path")?)
        .args(["r", &partition, &output_path])
        .spawn()
        .map_err(|e| format!("edl read spawn error: {e}"))?;

    let mut out = String::new();
    let mut err = String::new();

    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(b) => out.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Stderr(b) => err.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Error(e) => return Err(e),
            CommandEvent::Terminated(s) => {
                if s.code.unwrap_or(-1) != 0 {
                    return Err(format!("edl read failed\nstderr: {err}"));
                }
                break;
            }
            _ => {}
        }
    }

    Ok(format!("Partition '{}' read to {}", partition, output_path))
}

#[tauri::command]
pub async fn edl_write_partition(
    app: AppHandle,
    partition: String,
    image_path: String,
) -> Result<String, String> {
    let allowed = ["boot", "recovery", "modem", "fsg", "persist", "aboot", "sbl1", "tz", "rpm"];
    if !allowed.contains(&partition.as_str()) {
        return Err(format!("Partition '{}' not in allowed write list", partition));
    }

    let tool = get_edl_tool_path(&app, "edl")?;
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command(tool.to_str().ok_or("invalid edl path")?)
        .args(["w", &partition, &image_path])
        .spawn()
        .map_err(|e| format!("edl write spawn error: {e}"))?;

    let mut out = String::new();
    let mut err = String::new();

    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(b) => out.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Stderr(b) => err.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Error(e) => return Err(e),
            CommandEvent::Terminated(s) => {
                if s.code.unwrap_or(-1) != 0 {
                    return Err(format!("edl write failed\nstderr: {err}"));
                }
                break;
            }
            _ => {}
        }
    }

    Ok(format!("Partition '{}' written from {}", partition, image_path))
}

#[tauri::command]
pub async fn edl_erase_partition(
    app: AppHandle,
    partition: String,
) -> Result<String, String> {
    let allowed = ["userdata", "cache", "modem", "fsg", "persist", "misc"];
    if !allowed.contains(&partition.as_str()) {
        return Err(format!("Partition '{}' not in allowed erase list", partition));
    }

    let tool = get_edl_tool_path(&app, "edl")?;
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command(tool.to_str().ok_or("invalid edl path")?)
        .args(["e", &partition])
        .spawn()
        .map_err(|e| format!("edl erase spawn error: {e}"))?;

    let mut out = String::new();
    let mut err = String::new();

    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(b) => out.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Stderr(b) => err.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Error(e) => return Err(e),
            CommandEvent::Terminated(s) => {
                if s.code.unwrap_or(-1) != 0 {
                    return Err(format!("edl erase failed\nstderr: {err}"));
                }
                break;
            }
            _ => {}
        }
    }

    Ok(format!("Partition '{}' erased", partition))
}

#[tauri::command]
pub async fn edl_reboot(app: AppHandle) -> Result<String, String> {
    let tool = get_edl_tool_path(&app, "edl")?;
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command(tool.to_str().ok_or("invalid edl path")?)
        .args(["reset"])
        .spawn()
        .map_err(|e| format!("edl reboot spawn error: {e}"))?;

    let mut out = String::new();
    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(b) => out.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Terminated(_) => break,
            _ => {}
        }
    }

    Ok("Device rebooting from EDL".to_string())
}

#[tauri::command]
pub async fn edl_get_gpt(app: AppHandle) -> Result<String, String> {
    let tool = get_edl_tool_path(&app, "edl")?;
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command(tool.to_str().ok_or("invalid edl path")?)
        .args(["printgpt"])
        .spawn()
        .map_err(|e| format!("edl gpt spawn error: {e}"))?;

    let mut out = String::new();
    let mut err = String::new();

    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(b) => out.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Stderr(b) => err.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Error(e) => return Err(e),
            CommandEvent::Terminated(s) => {
                if s.code.unwrap_or(-1) != 0 {
                    return Err(format!("edl printgpt failed\nstderr: {err}"));
                }
                break;
            }
            _ => {}
        }
    }

    Ok(out.trim().to_string())
}
