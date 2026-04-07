use std::path::PathBuf;
use tauri::AppHandle;
use tauri::Manager;
use tauri_plugin_shell::process::CommandEvent;
use tauri_plugin_shell::ShellExt;

fn parse_shell_events(
    event: CommandEvent,
    out: &mut String,
    err: &mut String,
) -> Option<Result<(), String>> {
    match event {
        CommandEvent::Stdout(b) => {
            out.push_str(&String::from_utf8_lossy(&b));
            None
        }
        CommandEvent::Stderr(b) => {
            err.push_str(&String::from_utf8_lossy(&b));
            None
        }
        CommandEvent::Error(e) => Some(Err(e)),
        CommandEvent::Terminated(s) => {
            if s.code.unwrap_or(-1) != 0 {
                Some(Err(format!("exit {:?}\nstderr: {err}", s.code)))
            } else {
                Some(Ok(()))
            }
        }
        _ => None,
    }
}

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

// Get device info (Normal mode only)
#[tauri::command]
pub async fn apple_device_info(app: AppHandle) -> Result<String, String> {
    let ideviceinfo = get_tool_path(&app, "ideviceinfo")?;
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command(ideviceinfo.to_str().ok_or("invalid ideviceinfo path")?)
        .args(["-s"])
        .spawn()
        .map_err(|e| format!("ideviceinfo spawn error: {e}"))?;

    let mut out = String::new();
    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(b) => out.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Stderr(b) => {
                let err = String::from_utf8_lossy(&b);
                if err.contains("No device found") {
                    return Err("Device not in normal mode".into());
                }
            }
            CommandEvent::Terminated(s) => {
                if s.code.unwrap_or(-1) != 0 {
                    return Err("ideviceinfo failed".into());
                }
                break;
            }
            _ => {}
        }
    }
    Ok(out.trim().to_string())
}

// Send irecovery command (Recovery / DFU mode)
#[tauri::command]
pub async fn apple_irecovery_cmd(app: AppHandle, command: String) -> Result<String, String> {
    let irecovery = get_tool_path(&app, "irecovery")?;
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command(irecovery.to_str().ok_or("invalid irecovery path")?)
        .args(["-c", &command])
        .spawn()
        .map_err(|e| format!("irecovery spawn error: {e}"))?;

    let mut out = String::new();
    let mut err = String::new();
    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(b) => out.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Stderr(b) => err.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Error(e) => return Err(e),
            CommandEvent::Terminated(s) => {
                if s.code.unwrap_or(-1) != 0 {
                    return Err(format!("irecovery exit {:?}\nstderr: {err}", s.code));
                }
                break;
            }
            _ => {}
        }
    }
    Ok(out.trim().to_string())
}

// Exit Recovery Mode → Normal
#[tauri::command]
pub async fn apple_exit_recovery(app: AppHandle) -> Result<String, String> {
    let irecovery = get_tool_path(&app, "irecovery")?;
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command(irecovery.to_str().ok_or("invalid irecovery path")?)
        .args(["-n"])
        .spawn()
        .map_err(|e| format!("exit recovery spawn error: {e}"))?;

    let mut out = String::new();
    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(b) => out.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Terminated(_) => break,
            _ => {}
        }
    }
    Ok(out.trim().to_string())
}

// Enter DFU from Recovery (send exploit command)
#[tauri::command]
pub async fn apple_enter_dfu(app: AppHandle) -> Result<String, String> {
    // irecovery sequence: setenv → saveenv → reboot
    // Real DFU requires physical button combo — this only works
    // if device is already in partial DFU via recovery
    let irecovery = get_tool_path(&app, "irecovery")?;
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command(irecovery.to_str().ok_or("invalid irecovery path")?)
        .args(["-c", "setenv auto-boot false"])
        .spawn()
        .map_err(|e| format!("DFU prep error: {e}"))?;

    while let Some(event) = rx.recv().await {
        if let CommandEvent::Terminated(_) = event {
            break;
        }
    }
    Ok("DFU environment set — physical button combo required".to_string())
}

#[tauri::command]
pub async fn apple_icloud_bypass(
    app: AppHandle,
    tool: String,
    udid: String,
) -> Result<String, String> {
    let allowed_tools = ["palera1n", "checkra1n"];
    if !allowed_tools.contains(&tool.as_str()) {
        return Err(format!("Tool '{}' not allowed", tool));
    }

    let tool_path = get_tool_path(&app, &tool)?;
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command(tool_path.to_str().ok_or("invalid bypass tool path")?)
        .args(["--bypass-icloud", "--udid", &udid])
        .spawn()
        .map_err(|e| format!("{tool} spawn error: {e}"))?;

    let mut out = String::new();
    let mut err = String::new();

    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(b) => out.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Stderr(b) => err.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Error(e) => return Err(e),
            CommandEvent::Terminated(s) => {
                if s.code.unwrap_or(-1) != 0 {
                    return Err(format!("{tool} exit {:?}\nstderr: {err}", s.code));
                }
                break;
            }
            _ => {}
        }
    }

    Ok(out.trim().to_string())
}

#[tauri::command]
pub async fn apple_restore_ipsw(
    app: AppHandle,
    ipsw_path: String,
    erase: bool,
) -> Result<String, String> {
    let idevicerestore = get_tool_path(&app, "idevicerestore")?;
    let shell = app.shell();
    let mut args = vec!["-t".to_string(), ipsw_path];
    if erase {
        args.insert(0, "-e".to_string());
    }

    let (mut rx, _child) = shell
        .command(
            idevicerestore
                .to_str()
                .ok_or("invalid idevicerestore path")?,
        )
        .args(args)
        .spawn()
        .map_err(|e| format!("idevicerestore error: {e}"))?;

    let mut out = String::new();
    let mut err = String::new();

    while let Some(event) = rx.recv().await {
        if let Some(term) = parse_shell_events(event, &mut out, &mut err) {
            term.map_err(|e| format!("restore failed\n{e}"))?;
            break;
        }
    }

    Ok(out.trim().to_string())
}

#[tauri::command]
pub async fn apple_check_activation(app: AppHandle) -> Result<String, String> {
    let ideviceactivation = get_tool_path(&app, "ideviceactivation")?;
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command(
            ideviceactivation
                .to_str()
                .ok_or("invalid ideviceactivation path")?,
        )
        .args(["state"])
        .spawn()
        .map_err(|e| format!("activation check error: {e}"))?;

    let mut out = String::new();
    let mut err = String::new();
    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(b) => out.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Stderr(b) => err.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Error(e) => return Err(e),
            CommandEvent::Terminated(s) => {
                if s.code.unwrap_or(-1) != 0 {
                    return Err(format!("activation check failed\nstderr: {err}"));
                }
                break;
            }
            _ => {}
        }
    }
    Ok(out.trim().to_string())
}

#[tauri::command]
pub async fn apple_dns_activation(app: AppHandle, server_host: String) -> Result<String, String> {
    let ideviceactivation = get_tool_path(&app, "ideviceactivation")?;
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command(
            ideviceactivation
                .to_str()
                .ok_or("invalid ideviceactivation path")?,
        )
        .args(["activate", "-s", &server_host])
        .spawn()
        .map_err(|e| format!("DNS bypass error: {e}"))?;

    let mut out = String::new();
    let mut err = String::new();
    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(b) => out.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Stderr(b) => err.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Error(e) => return Err(e),
            CommandEvent::Terminated(s) => {
                if s.code.unwrap_or(-1) != 0 {
                    return Err(format!("DNS activation failed\nstderr: {err}"));
                }
                break;
            }
            _ => {}
        }
    }
    Ok(out.trim().to_string())
}

#[tauri::command]
pub async fn apple_mdm_bypass(app: AppHandle, profile_path: String) -> Result<String, String> {
    let ideviceinstaller = get_tool_path(&app, "ideviceinstaller")?;
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command(
            ideviceinstaller
                .to_str()
                .ok_or("invalid ideviceinstaller path")?,
        )
        .args(["--install", &profile_path])
        .spawn()
        .map_err(|e| format!("MDM profile install error: {e}"))?;

    let mut out = String::new();
    let mut err = String::new();
    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(b) => out.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Stderr(b) => err.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Error(e) => return Err(e),
            CommandEvent::Terminated(s) => {
                if s.code.unwrap_or(-1) != 0 {
                    return Err(format!("MDM bypass profile install failed\nstderr: {err}"));
                }
                break;
            }
            _ => {}
        }
    }
    Ok(format!("MDM bypass profile installed: {}", out.trim()))
}

#[tauri::command]
pub async fn apple_restore_activation_record(
    app: AppHandle,
    record_path: String,
) -> Result<String, String> {
    let ideviceactivation = get_tool_path(&app, "ideviceactivation")?;
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command(
            ideviceactivation
                .to_str()
                .ok_or("invalid ideviceactivation path")?,
        )
        .args(["activate", "-A", &record_path])
        .spawn()
        .map_err(|e| format!("activation restore error: {e}"))?;

    let mut out = String::new();
    let mut err = String::new();
    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(b) => out.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Stderr(b) => err.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Error(e) => return Err(e),
            CommandEvent::Terminated(s) => {
                if s.code.unwrap_or(-1) != 0 {
                    return Err(format!("restore failed\nstderr: {err}"));
                }
                break;
            }
            _ => {}
        }
    }

    Ok(out.trim().to_string())
}
