use tauri_plugin_shell::ShellExt;
use tauri_plugin_shell::process::CommandEvent;
use tauri::Emitter;

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
