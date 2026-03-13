use serde::{Serialize, Deserialize};
use tauri::{AppHandle, Manager, Emitter};
use tauri_plugin_shell::ShellExt;
use tauri_plugin_shell::process::CommandEvent;

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct HashInfo {
    pub version: String,
    pub algorithm: String,
    pub iterations: u32,
    pub salt: String,
    pub hash: String,
    pub hashcat_mode: u32,
}

fn python_path(app: &AppHandle) -> std::path::PathBuf {
    app.path().resource_dir().unwrap().join("python")
}

#[tauri::command]
pub async fn ios_extract_screentime_hash(app: AppHandle, backup_path: String) -> Result<HashInfo, String> {
    println!("[COMMAND] ios_extract_screentime_hash path={}", backup_path);
    
    let output = app.shell()
        .command("python3")
        .args([
            python_path(&app).join("ios_backup/cli.py").to_str().unwrap(),
            "screentime-hash",
            &backup_path
        ])
        .output()
        .await
        .map_err(|e| e.to_string())?;

    if !output.status.success() {
        return Err(String::from_utf8_lossy(&output.stderr).to_string());
    }

    let json_str = String::from_utf8_lossy(&output.stdout);
    serde_json::from_str(&json_str).map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn ios_run_screentime_crack(
    app: AppHandle, 
    backup_path: String, 
    wordlist: String, 
    rules: String
) -> Result<(), String> {
    println!("[COMMAND] ios_run_screentime_crack path={}", backup_path);
    
    // Extract hash first
    let hash_info = ios_extract_screentime_hash(app.clone(), backup_path).await?;
    
    // Format for hashcat
    let hash_str = format!("{}:{}:{}", hash_info.hash, hash_info.salt, hash_info.iterations);
    
    let (mut rx, _child) = app.shell()
        .command("hashcat")
        .args([
            "-m", &hash_info.hashcat_mode.to_string(),
            "-a", "0",
            &hash_str,
            &wordlist,
            "-r", &rules
        ])
        .spawn()
        .map_err(|e| e.to_string())?;

    let app_handle = app.clone();
    tauri::async_runtime::spawn(async move {
        while let Some(event) = rx.recv().await {
            match event {
                CommandEvent::Stdout(bytes) => {
                    let line = String::from_utf8_lossy(&bytes).to_string();
                    // emit specific progress events
                    if line.contains("Progress") {
                        app_handle.emit("screentime-progress", line).unwrap();
                    }
                }
                CommandEvent::Terminated(payload) => {
                    app_handle.emit("screentime-done", payload.code).unwrap();
                    break;
                }
                _ => {}
            }
        }
    });

    Ok(())
}
