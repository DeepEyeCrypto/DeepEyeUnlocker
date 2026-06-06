// Zero unwrap() — explicit Result everywhere
use serde::Serialize;
use serde_json::Value;
use tauri::{AppHandle, Manager};
use tauri_plugin_shell::ShellExt;

#[derive(Debug, Serialize)]
pub struct F3arError {
    pub layer: String,
    pub reason: String,
    pub retryable: bool,
}

fn swift(app: &AppHandle) -> String {
    app.path()
        .resource_dir()
        .unwrap_or_default()
        .join("deepeye-core")
        .to_string_lossy()
        .to_string()
}

async fn run_cmd(app: &AppHandle, cmd: &str, args: &[&str]) -> Result<Vec<Value>, F3arError> {
    let bin = swift(app);
    let mut all_args = vec![cmd];
    all_args.extend_from_slice(args);

    let out = app
        .shell()
        .command(&bin)
        .args(&all_args)
        .output()
        .await
        .map_err(|e| F3arError {
            layer: "SPAWN".into(),
            reason: e.to_string(),
            retryable: false,
        })?;

    let stdout = String::from_utf8_lossy(&out.stdout);
    let events: Vec<Value> = stdout
        .lines()
        .filter(|l| l.starts_with('{'))
        .filter_map(|l| serde_json::from_str(l).ok())
        .collect();

    Ok(events)
}

/// Full F3arRa1n chain: detect → DFU → checkm8 → ramdisk → activation
#[tauri::command]
pub async fn f3arrain_full(app: AppHandle, session_id: String) -> Result<Vec<Value>, F3arError> {
    run_cmd(&app, "f3arrain", &[&session_id]).await
}

/// Quick device detect only
#[tauri::command]
pub async fn f3arrain_detect(app: AppHandle, session_id: String) -> Result<Value, F3arError> {
    let bin = swift(&app);
    let out = app
        .shell()
        .command(&bin)
        .args(["f3arrain-detect", &session_id])
        .output()
        .await
        .map_err(|e| F3arError {
            layer: "SPAWN".into(),
            reason: e.to_string(),
            retryable: false,
        })?;

    let stdout = String::from_utf8_lossy(&out.stdout).to_string();
    stdout
        .lines()
        .rfind(|l| l.starts_with('{'))
        .ok_or_else(|| F3arError {
            layer: "PARSE".into(),
            reason: "No output".into(),
            retryable: false,
        })
        .and_then(|l| {
            serde_json::from_str::<Value>(l).map_err(|e| F3arError {
                layer: "PARSE".into(),
                reason: e.to_string(),
                retryable: false,
            })
        })
}

/// checkm8 only (for testing)
#[tauri::command]
pub async fn f3arrain_checkm8(app: AppHandle, session_id: String) -> Result<Vec<Value>, F3arError> {
    run_cmd(&app, "f3arrain-checkm8", &[&session_id]).await
}
