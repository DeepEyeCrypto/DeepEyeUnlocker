use tauri::{AppHandle, Emitter};
use tauri_plugin_shell::ShellExt;
use tauri_plugin_shell::process::CommandEvent;

#[tauri::command]
pub async fn logcat_start_stream(app: AppHandle) -> Result<(), String> {
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command("adb")
        .args(["logcat", "*:V"])
        .spawn()
        .map_err(|e| e.to_string())?;

    tauri::async_runtime::spawn(async move {
        while let Some(event) = rx.recv().await {
            match event {
                CommandEvent::Stdout(line) => {
                    let text = String::from_utf8_lossy(&line).to_string();
                    app.emit("logcat-line", text).ok();
                }
                CommandEvent::Terminated(_) => break,
                _ => {}
            }
        }
    });

    Ok(())
}

#[tauri::command]
pub async fn logcat_clear(app: AppHandle) -> Result<(), String> {
    let shell = app.shell();
    shell.command("adb")
        .args(["logcat", "-c"])
        .spawn()
        .map_err(|e| e.to_string())?;
    Ok(())
}
