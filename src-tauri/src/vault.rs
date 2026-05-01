use std::fs;
use std::path::PathBuf;
use tauri::AppHandle;
use tauri_plugin_shell::ShellExt;

/// Get the vault storage directory (~/.deepeye/vault/)
fn vault_dir() -> PathBuf {
    let home = std::env::var("HOME").unwrap_or_else(|_| "/tmp".to_string());
    let dir = PathBuf::from(home).join(".deepeye").join("vault");
    let _ = fs::create_dir_all(&dir);
    dir
}

async fn run_bash(app: &AppHandle, s: &str) -> Result<String, String> {
    let output = app
        .shell()
        .command("bash")
        .args(["-c", s])
        .output()
        .await
        .map_err(|e| e.to_string())?;

    Ok(format!(
        "{}{}",
        String::from_utf8_lossy(&output.stdout),
        String::from_utf8_lossy(&output.stderr)
    ))
}

/// Push local activation record to DeepEye local vault (encrypted via openssl)
#[tauri::command]
pub async fn push_to_cloud_vault(
    app: AppHandle,
    ecid: String,
    token_path: String,
) -> Result<String, String> {
    let vault = vault_dir();
    let dest = vault.join(format!("{}.enc", ecid));
    let dest_str = dest.to_string_lossy().to_string();

    // Check if source file exists
    if !std::path::Path::new(&token_path).exists() {
        return Err(format!(
            "❌ Token file not found: {}\n💡 Provide a valid activation record path.",
            token_path
        ));
    }

    // Encrypt and store using openssl (AES-256-CBC with ECID-derived key)
    let cmd = format!(
        "openssl enc -aes-256-cbc -salt -pbkdf2 -in '{}' -out '{}' -pass pass:'DEE-{}' 2>&1 && \
         echo '✅ Vault: Record encrypted and saved' && \
         echo 'ECID: {}' && \
         echo 'Path: {}' && \
         ls -lh '{}'",
        token_path, dest_str, ecid, ecid, dest_str, dest_str
    );

    run_bash(&app, &cmd).await
}

/// Pull activation record from local vault (decrypt)
#[tauri::command]
pub async fn pull_from_cloud_vault(app: AppHandle, ecid: String) -> Result<String, String> {
    let vault = vault_dir();
    let src = vault.join(format!("{}.enc", ecid));
    let src_str = src.to_string_lossy().to_string();

    if !src.exists() {
        return Err(format!(
            "❌ No vault record found for ECID: {}\n💡 Push a record first with push_to_cloud_vault.",
            ecid
        ));
    }

    let home = std::env::var("HOME").unwrap_or_else(|_| "/tmp".to_string());
    let restore_dir = format!("{}/DeepEyeUnlocker/Vault/{}", home, ecid);
    let restore_path = format!("{}/activation_record.plist", restore_dir);

    let cmd = format!(
        "mkdir -p '{}' && \
         openssl enc -aes-256-cbc -d -salt -pbkdf2 -in '{}' -out '{}' -pass pass:'DEE-{}' 2>&1 && \
         echo '✅ Vault: Record decrypted and restored' && \
         echo 'ECID: {}' && \
         echo 'Restored to: {}' && \
         ls -lh '{}'",
        restore_dir, src_str, restore_path, ecid, ecid, restore_path, restore_path
    );

    run_bash(&app, &cmd).await
}

/// List all records in the local vault
#[tauri::command]
pub fn list_cloud_vault() -> Result<String, String> {
    let vault = vault_dir();

    let entries: Vec<String> = fs::read_dir(&vault)
        .map_err(|e| format!("❌ Cannot read vault directory: {}", e))?
        .filter_map(|entry| {
            let entry = entry.ok()?;
            let name = entry.file_name().to_string_lossy().to_string();
            if name.ends_with(".enc") {
                let ecid = name.trim_end_matches(".enc");
                let meta = entry.metadata().ok()?;
                let size = meta.len();
                let modified = meta
                    .modified()
                    .ok()
                    .and_then(|t| {
                        t.duration_since(std::time::UNIX_EPOCH)
                            .ok()
                            .map(|d| {
                                let secs = d.as_secs();
                                // Simple date formatting
                                let days = secs / 86400;
                                let years = 1970 + days / 365;
                                let rem_days = days % 365;
                                let months = rem_days / 30 + 1;
                                let day = rem_days % 30 + 1;
                                format!("{}-{:02}-{:02}", years, months, day)
                            })
                    })
                    .unwrap_or_else(|| "unknown".to_string());
                Some(format!(
                    "ECID: {} | Size: {} bytes | Synced: {}",
                    ecid, size, modified
                ))
            } else {
                None
            }
        })
        .collect();

    if entries.is_empty() {
        Ok("No records in vault. Use push_to_cloud_vault to store activation records.".to_string())
    } else {
        Ok(format!(
            "📦 DeepEye Vault ({}):\n{}",
            vault.to_string_lossy(),
            entries.join("\n")
        ))
    }
}
