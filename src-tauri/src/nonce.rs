use std::{
    fs,
    path::{Path, PathBuf},
};

use crate::tool_exec::run_tool;
use quick_xml::{events::Event, Reader};
use tauri::AppHandle;

fn validate_generator(generator: &str) -> Result<String, String> {
    let value = generator.trim();
    if value.len() != 18 || !value.starts_with("0x") || !value[2..].chars().all(|c| c.is_ascii_hexdigit()) {
        return Err(
            "Generator must be 0x followed by 16 hex chars (e.g. 0x1111111111111111)"
                .into(),
        );
    }

    Ok(value.to_string())
}

fn validate_blob_path(blob_path: &str) -> Result<PathBuf, String> {
    let path = PathBuf::from(blob_path.trim());
    if blob_path.trim().is_empty() {
        return Err("SHSH2 blob path is required".into());
    }
    if !path.exists() {
        return Err(format!("Blob not found: {}", path.display()));
    }
    let extension = path
        .extension()
        .and_then(|value| value.to_str())
        .unwrap_or_default();
    if !extension.eq_ignore_ascii_case("shsh2") {
        return Err(format!("Expected a .shsh2 file: {}", path.display()));
    }

    Ok(path)
}

fn read_blob_file(path: &Path) -> Result<String, String> {
    fs::read_to_string(path).map_err(|e| format!("Cannot read blob {}: {e}", path.display()))
}

fn relevant_nonce_lines(output: &str) -> Vec<String> {
    output
        .lines()
        .filter(|line| {
            let lower = line.to_ascii_lowercase();
            lower.contains("nonce")
                || lower.contains("apnonce")
                || lower.contains("generator")
                || line.contains("NONC")
                || line.contains("SNON")
        })
        .map(|line| line.trim().to_string())
        .filter(|line| !line.is_empty())
        .collect()
}

#[tauri::command]
pub async fn get_current_nonce(app: AppHandle) -> Result<String, String> {
    if let Ok(output) = run_tool(&app, "ideviceinfo", vec!["-k".into(), "BootNonce".into()]).await {
        let trimmed = output.trim();
        if !trimmed.is_empty() {
            return Ok(format!("BootNonce: {trimmed}"));
        }
    }

    let recovery_output = run_tool(&app, "irecovery", vec!["-q".into()]).await?;
    let relevant = relevant_nonce_lines(&recovery_output);

    if relevant.is_empty() {
        return Ok(recovery_output);
    }

    Ok(relevant.join("\n"))
}

#[tauri::command]
pub async fn set_nonce_generator(app: AppHandle, generator: String) -> Result<String, String> {
    let generator = validate_generator(&generator)?;
    let set_output = run_tool(
        &app,
        "irecovery",
        vec![
            "-s".into(),
            "-c".into(),
            format!("setenv com.apple.System.boot-nonce {generator}"),
        ],
    )
    .await?;
    let save_output = run_tool(
        &app,
        "irecovery",
        vec!["-s".into(), "-c".into(), "saveenv".into()],
    )
    .await?;
    let reset_output = run_tool(
        &app,
        "irecovery",
        vec!["-s".into(), "-c".into(), "reset".into()],
    )
    .await?;

    Ok(format!(
        "Nonce set to {generator}\nDevice rebooting...\n\n{set_output}\n{save_output}\n{reset_output}"
    ))
}

#[tauri::command]
pub async fn set_nonce_from_blob(app: AppHandle, blob_path: String) -> Result<String, String> {
    let blob_path = validate_blob_path(&blob_path)?;
    let blob = read_blob_file(&blob_path)?;
    let generator = extract_generator_from_plist(&blob)?;
    set_nonce_generator(app, generator).await
}

fn extract_generator_from_plist(plist: &str) -> Result<String, String> {
    let mut reader = Reader::from_str(plist);
    reader.config_mut().trim_text(true);

    let mut buffer = Vec::new();
    let mut current_key: Option<String> = None;
    let mut reading_key = false;
    let mut reading_string = false;

    loop {
        match reader.read_event_into(&mut buffer) {
            Ok(Event::Start(event)) => match event.name().as_ref() {
                b"key" => reading_key = true,
                b"string" => reading_string = true,
                _ => {}
            },
            Ok(Event::End(event)) => match event.name().as_ref() {
                b"key" => reading_key = false,
                b"string" => {
                    reading_string = false;
                    current_key = None;
                }
                _ => {}
            },
            Ok(Event::Text(text)) => {
                let value = String::from_utf8_lossy(text.as_ref()).trim().to_string();

                if reading_key {
                    current_key = Some(value);
                    reading_key = false;
                } else if reading_string {
                    if current_key.as_deref() == Some("generator")
                        || current_key.as_deref() == Some("com.apple.System.boot-nonce")
                    {
                        return validate_generator(&value);
                    }
                    reading_string = false;
                    current_key = None;
                }
            }
            Ok(Event::Eof) => break,
            Err(e) => return Err(format!("Invalid SHSH2 plist: {e}")),
            _ => {}
        }

        buffer.clear();
    }

    Err("Generator not found in blob. This SHSH2 file may not include one.".into())
}

#[tauri::command]
pub fn get_generator_from_blob(blob_path: String) -> Result<String, String> {
    let blob_path = validate_blob_path(&blob_path)?;
    let blob = read_blob_file(&blob_path)?;
    extract_generator_from_plist(&blob).map(|g| format!("Generator: {g}"))
}

#[tauri::command]
pub async fn clear_nonce(app: AppHandle) -> Result<String, String> {
    let clear_output = run_tool(
        &app,
        "irecovery",
        vec![
            "-s".into(),
            "-c".into(),
            "setenv com.apple.System.boot-nonce".into(),
        ],
    )
    .await?;
    let save_output = run_tool(
        &app,
        "irecovery",
        vec!["-s".into(), "-c".into(), "saveenv".into()],
    )
    .await?;
    let reset_output = run_tool(
        &app,
        "irecovery",
        vec!["-s".into(), "-c".into(), "reset".into()],
    )
    .await?;

    Ok(format!(
        "Nonce cleared. Device rebooting...\n\n{clear_output}\n{save_output}\n{reset_output}"
    ))
}

#[tauri::command]
pub async fn set_nonce_checkra1n(app: AppHandle, generator: String) -> Result<String, String> {
    let generator = validate_generator(&generator)?;
    let output = run_tool(
        &app,
        "checkra1n",
        vec!["-c".into(), "--set-nonce".into(), generator.clone()],
    )
    .await?;

    Ok(format!("Nonce set via checkra1n using {generator}\n\n{output}"))
}
