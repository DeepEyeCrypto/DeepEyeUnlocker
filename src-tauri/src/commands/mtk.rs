use tauri::{AppHandle, Emitter};
use tauri_plugin_shell::process::CommandEvent;
use tauri_plugin_shell::ShellExt;

async fn run_mtk_client(app: &AppHandle, args: &[&str]) -> Result<String, String> {
    let shell = app.shell();
    let (mut rx, _child) = shell
        .command("mtkclient")
        .args(args)
        .spawn()
        .map_err(|e| format!("mtkclient spawn error: {e}"))?;

    let mut out = String::new();
    let mut err = String::new();

    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(bytes) => {
                let line = String::from_utf8_lossy(&bytes);
                out.push_str(&line);
                let _ = app.emit("mtk://stdout", line.to_string());
            }
            CommandEvent::Stderr(bytes) => {
                let line = String::from_utf8_lossy(&bytes);
                err.push_str(&line);
                let _ = app.emit("mtk://stderr", line.to_string());
            }
            CommandEvent::Error(e) => {
                let _ = app.emit("mtk://error", e.clone());
                return Err(e);
            }
            CommandEvent::Terminated(status) => {
                let code = status.code.unwrap_or(-1);
                let _ = app.emit("mtk://exit", code);
                if code != 0 {
                    let msg = if err.trim().is_empty() {
                        format!("mtkclient exited with code {code}")
                    } else {
                        format!("mtkclient exited with code {code}: {}", err.trim())
                    };
                    return Err(msg);
                }
                break;
            }
            _ => {}
        }
    }

    Ok(out.trim().to_string())
}

#[tauri::command]
pub async fn mtk_run_command(app: AppHandle, args: Vec<String>) -> Result<String, String> {
    if args.is_empty() {
        return Err("mtk_run_command requires at least one argument".to_string());
    }
    let borrowed: Vec<&str> = args.iter().map(String::as_str).collect();
    run_mtk_client(&app, &borrowed).await
}

#[tauri::command]
pub async fn mtk_read_partition(
    app: AppHandle,
    partition: String,
    output_path: String,
) -> Result<String, String> {
    if partition.trim().is_empty() {
        return Err("partition must not be empty".to_string());
    }
    if output_path.trim().is_empty() {
        return Err("output_path must not be empty".to_string());
    }

    run_mtk_client(
        &app,
        &[
            "r",
            partition.trim(),
            output_path.trim(),
        ],
    )
    .await
}

#[tauri::command]
pub async fn mtk_write_partition(
    app: AppHandle,
    partition: String,
    input_path: String,
) -> Result<String, String> {
    if partition.trim().is_empty() {
        return Err("partition must not be empty".to_string());
    }
    if input_path.trim().is_empty() {
        return Err("input_path must not be empty".to_string());
    }

    run_mtk_client(
        &app,
        &[
            "w",
            partition.trim(),
            input_path.trim(),
        ],
    )
    .await
}

#[tauri::command]
pub async fn mtk_erase_partition(app: AppHandle, partition: String) -> Result<String, String> {
    if partition.trim().is_empty() {
        return Err("partition must not be empty".to_string());
    }

    run_mtk_client(&app, &["e", partition.trim()]).await
}

#[tauri::command]
pub async fn mtk_device_info(app: AppHandle) -> Result<String, String> {
    run_mtk_client(&app, &["printgpt"]).await
}

#[tauri::command]
pub async fn mtk_unlock_bootloader(app: AppHandle) -> Result<String, String> {
    run_mtk_client(&app, &["da", "seccfg", "unlock"]).await
}

