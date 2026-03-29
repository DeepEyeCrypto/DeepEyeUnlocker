use tauri::AppHandle;
use tauri_plugin_shell::ShellExt;
use tauri_plugin_shell::process::CommandEvent;

fn parse_shell_events(event: CommandEvent, out: &mut String, err: &mut String) -> Option<Result<(), String>> {
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

// Get device info (Normal mode only)
#[tauri::command]
pub async fn apple_device_info(app: AppHandle) -> Result<String, String> {
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command("ideviceinfo")
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
#[tauri              ::command]
pub async fn apple_irecovery_cmd(
    app: AppHandle,
    command: String
) -> Result<String, String> {
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command("irecovery")
        .args(["-c", &command])
        .spawn()
        .map_err(|e| format!("irecovery spawn error: {e}"))?;

    let mut out = String::new();
    let mut err = String::new();
    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(b) => out.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Stderr(b) => err.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Error(e)  => return Err(e),
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
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command("irecovery")
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
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command("irecovery")
        .args(["-c", "setenv auto-boot false"])
        .spawn()
        .map_err(|e| format!("DFU prep error: {e}"))?;

    while let Some(event) = rx.recv().await {
        if let CommandEvent::Terminated(_) = event { break; }
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

    let shell = app.shell();
    let (mut rx, _child) = shell
        .command(&tool)
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
    let shell = app.shell();
    let mut args = vec!["-t".to_string(), ipsw_path];
    if erase {
        args.insert(0, "-e".to_string());
    }

    let (mut rx, _child) = shell
        .command("idevicerestore")
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
