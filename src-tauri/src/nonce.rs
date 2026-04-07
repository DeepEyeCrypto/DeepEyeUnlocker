use std::fs;
use tauri::AppHandle;
use tauri_plugin_shell::ShellExt;

async fn run_bash(app: &AppHandle, s: &str) -> Result<String, String> {
    let output = app
        .shell()
        .command("bash")
        .args(["-c", s])
        .output()
        .await
        .map_err(|e| e.to_string())?;

    Ok(format!(
        "{}\n{}",
        String::from_utf8_lossy(&output.stdout),
        String::from_utf8_lossy(&output.stderr)
    ))
}

#[tauri::command]
pub async fn get_current_nonce(app: AppHandle) -> Result<String, String> {
    run_bash(
        &app,
        "ideviceinfo -k BootNonce 2>/dev/null || irecovery -q 2>/dev/null | grep -Ei 'boot-nonce|ApNonce|generator' || nvram boot-nonce 2>/dev/null || echo 'Device must be in DFU/Recovery for NVRAM read'",
    )
    .await
}

#[tauri::command]
pub async fn set_nonce_generator(app: AppHandle, generator: String) -> Result<String, String> {
    if !generator.starts_with("0x") || generator.len() != 18 {
        return Err(
            "Generator must be 0x followed by 16 hex chars (e.g. 0x1111111111111111)".into(),
        );
    }

    let cmd = format!(
        "irecovery -s -c \"setenv com.apple.System.boot-nonce {generator}\" && \
         irecovery -s -c \"saveenv\" && \
         irecovery -s -c \"reset\""
    );
    let out = run_bash(&app, &cmd).await?;
    Ok(format!(
        "✅ Nonce set to {generator}\nDevice rebooting...\n{out}"
    ))
}

#[tauri::command]
pub async fn set_nonce_from_blob(app: AppHandle, blob_path: String) -> Result<String, String> {
    let blob = fs::read_to_string(&blob_path).map_err(|e| format!("Cannot read blob: {e}"))?;
    let generator = extract_generator_from_plist(&blob)?;
    set_nonce_generator(app, generator).await
}

fn extract_generator_from_plist(plist: &str) -> Result<String, String> {
    let lines: Vec<&str> = plist.lines().collect();
    for (i, line) in lines.iter().enumerate() {
        if line.contains("generator") {
            if let Some(next) = lines.get(i + 1) {
                let val = next.trim().replace("<string>", "").replace("</string>", "");
                if val.starts_with("0x") {
                    return Ok(val);
                }
            }
        }
    }
    Err("Generator not found in blob. This blob may not have a generator set.".into())
}

#[tauri::command]
pub fn get_generator_from_blob(blob_path: String) -> Result<String, String> {
    let blob = fs::read_to_string(&blob_path).map_err(|e| format!("Cannot read blob: {e}"))?;
    extract_generator_from_plist(&blob).map(|g| format!("Generator: {g}"))
}

#[tauri::command]
pub async fn clear_nonce(app: AppHandle) -> Result<String, String> {
    let out = run_bash(
        &app,
        "irecovery -s -c \"setenv com.apple.System.boot-nonce\" && irecovery -s -c \"saveenv\"",
    )
    .await?;
    Ok(format!("✅ Nonce cleared (randomized on next boot)\n{out}"))
}

#[tauri::command]
pub async fn set_nonce_checkra1n(app: AppHandle, generator: String) -> Result<String, String> {
    if !generator.starts_with("0x") || generator.len() != 18 {
        return Err("Invalid generator format".into());
    }
    run_bash(
        &app,
        &format!(
            "checkra1n -c --set-nonce {generator} 2>&1 && \
             echo '✅ Nonce set via checkra1n pwned DFU'"
        ),
    )
    .await
}
