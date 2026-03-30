use tauri::AppHandle;
use tauri_plugin_shell::process::CommandEvent;
use tauri_plugin_shell::ShellExt;

#[tauri::command]
pub async fn f3arrain_send_iboot(app: AppHandle, iboot_path: String) -> Result<String, String> {
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command("irecovery")
        .args(["-f", &iboot_path])
        .spawn()
        .map_err(|e| format!("irecovery spawn error: {e}"))?;

    let mut out = String::new();
    let mut err = String::new();
    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(b) => out.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Stderr(b) => err.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Terminated(s) => {
                if s.code.unwrap_or(-1) != 0 {
                    return Err(format!("irecovery failed: {err}"));
                }
                break;
            }
            _ => {}
        }
    }
    Ok(out.trim().to_string())
}

#[tauri::command]
pub async fn f3arrain_run_bypass(app: AppHandle, bypass_type: String) -> Result<String, String> {
    let script_name = match bypass_type.as_str() {
        "icloud" => "bypass_icloud.sh",
        "mdm" => "bypass_mdm.sh",
        "passcode" => "bypass_passcode.sh",
        _ => return Err(format!("Unknown bypass type: {bypass_type}")),
    };

    let resource_path = app.path()
        .resource_dir()
        .map_err(|e| format!("resource dir error: {e}"))?
        .join("scripts")
        .join(script_name);

    let shell = app.shell();
    let (mut rx, _child) = shell
        .command("bash")
        .arg(resource_path.to_str().unwrap())
        .spawn()
        .map_err(|e| format!("spawn error: {e}"))?;

    let mut out = String::new();
    let mut err = String::new();
    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(b) => out.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Stderr(b) => err.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Error(e) => return Err(e),
            CommandEvent::Terminated(s) => {
                if s.code.unwrap_or(-1) != 0 {
                    return Err(format!("bypass failed\n{err}"));
                }
                break;
            }
            _ => {}
        }
    }
    Ok(out.trim().to_string())
}
