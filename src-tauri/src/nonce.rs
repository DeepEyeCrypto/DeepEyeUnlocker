use std::process::Command;
use std::fs;

fn irecovery_cmd(args: &[&str]) -> Result<String, String> {
    let out = Command::new("irecovery").args(args).output()
        .map_err(|e| format!("irecovery not found: {e}"))?;
    Ok(format!("{}\n{}", 
        String::from_utf8_lossy(&out.stdout),
        String::from_utf8_lossy(&out.stderr)))
}

fn run_bash(s: &str) -> Result<String, String> {
    let out = Command::new("bash").arg("-c").arg(s).output()
        .map_err(|e| e.to_string())?;
    Ok(format!("{}\n{}", String::from_utf8_lossy(&out.stdout),
        String::from_utf8_lossy(&out.stderr)))
}

#[tauri::command]
pub fn get_current_nonce() -> Result<String, String> {
    let res = run_bash(
        "ideviceinfo -k BootNonce 2>/dev/null || \
         irecovery -q 2>/dev/null | grep -i 'boot-nonce\\|ApNonce\\|generator' || \
         nvram boot-nonce 2>/dev/null || \
         echo 'Device must be in DFU/Recovery for NVRAM read'"
    )?;
    Ok(res)
}

#[tauri::command]
pub fn set_nonce_generator(generator: String) -> Result<String, String> {
    if !generator.starts_with("0x") || generator.len() != 18 {
        return Err("Generator must be 0x followed by 16 hex chars (e.g. 0x1111111111111111)".into());
    }
    irecovery_cmd(&["-s", "-c", &format!("setenv com.apple.System.boot-nonce {generator}")])
        .and_then(|_| irecovery_cmd(&["-s", "-c", "saveenv"]))
        .and_then(|_| irecovery_cmd(&["-s", "-c", "reset"]))
        .map(|o| format!("✅ Nonce set to {generator}\nDevice rebooting...\n{o}"))
}

#[tauri::command]
pub fn set_nonce_from_blob(blob_path: String) -> Result<String, String> {
    let blob = fs::read_to_string(&blob_path)
        .map_err(|e| format!("Cannot read blob: {e}"))?;
    let gen = extract_generator_from_plist(&blob)?;
    set_nonce_generator(gen)
}

fn extract_generator_from_plist(plist: &str) -> Result<String, String> {
    let lines: Vec<&str> = plist.lines().collect();
    for (i, line) in lines.iter().enumerate() {
        if line.contains("generator") {
            if let Some(next) = lines.get(i + 1) {
                let val = next.trim()
                    .replace("<string>", "")
                    .replace("</string>", "");
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
    let blob = fs::read_to_string(&blob_path)
        .map_err(|e| format!("Cannot read blob: {e}"))?;
    extract_generator_from_plist(&blob)
        .map(|g| format!("Generator: {g}"))
}

#[tauri::command]
pub fn clear_nonce() -> Result<String, String> {
    irecovery_cmd(&["-s", "-c", "setenv com.apple.System.boot-nonce"])
        .and_then(|_| irecovery_cmd(&["-s", "-c", "saveenv"]))
        .map(|o| format!("✅ Nonce cleared (randomized on next boot)\n{o}"))
}

#[tauri::command]
pub fn set_nonce_checkra1n(generator: String) -> Result<String, String> {
    if !generator.starts_with("0x") || generator.len() != 18 {
        return Err("Invalid generator format".into());
    }
    run_bash(&format!(
        "checkra1n -c --set-nonce {generator} 2>&1 && \
         echo '✅ Nonce set via checkra1n pwned DFU'"
    ))
}
