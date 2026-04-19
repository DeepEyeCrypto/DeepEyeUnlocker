use std::{
    fs,
    path::{Path, PathBuf},
};

use crate::tool_exec::run_tool;
use dirs::home_dir;
use reqwest::StatusCode;
use serde::Deserialize;
use serde::Serialize;
use tauri::AppHandle;

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ShshDeviceInfo {
    pub ecid: String,
    pub model: String,
    pub hardware_model: String,
    pub board_id: String,
    pub chip_id: String,
    pub product_version: String,
}

#[derive(Debug, Deserialize, Clone)]
struct IpswFirmwaresResponse {
    firmwares: Vec<IpswFirmware>,
}

#[derive(Debug, Deserialize, Clone)]
struct IpswFirmware {
    version: String,
    buildid: String,
    signed: bool,
}

fn validate_simple_input<F>(
    label: &str,
    raw_value: &str,
    allow_char: F,
    example: &str,
) -> Result<String, String>
where
    F: Fn(char) -> bool,
{
    let value = raw_value.trim();
    if value.is_empty() {
        return Err(format!("{label} is required"));
    }

    if !value.chars().all(allow_char) {
        return Err(format!(
            "Invalid {label}. Example: {example}",
        ));
    }

    Ok(value.to_string())
}

fn validate_model(model: &str) -> Result<String, String> {
    validate_simple_input(
        "device model",
        model,
        |c| c.is_ascii_alphanumeric() || c == ',' || c == '-' || c == '_',
        "iPhone11,8",
    )
}

fn validate_ecid(ecid: &str) -> Result<String, String> {
    let value = ecid.trim();
    if value.is_empty() {
        return Err("ECID is required".into());
    }

    let normalized = if value.starts_with("0x") || value.starts_with("0X") {
        &value[2..]
    } else {
        value
    };

    if normalized.is_empty() || !normalized.chars().all(|c| c.is_ascii_hexdigit()) {
        return Err("Invalid ECID. Use decimal or hexadecimal format".into());
    }

    Ok(value.to_string())
}

fn validate_ios_version(ios: &str) -> Result<String, String> {
    validate_simple_input(
        "iOS version",
        ios,
        |c| c.is_ascii_alphanumeric() || c == '.' || c == '-' || c == '_',
        "16.7.5",
    )
}

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

async fn fetch_signed_firmwares(model: &str) -> Result<Vec<IpswFirmware>, String> {
    let url = format!("https://api.ipsw.me/v4/device/{model}?type=ipsw");
    let response = reqwest::get(&url)
        .await
        .map_err(|e| format!("Signed firmware lookup failed: {e}"))?;

    if response.status() == StatusCode::NOT_FOUND {
        return Err(format!("Unknown Apple model: {model}"));
    }

    if !response.status().is_success() {
        return Err(format!(
            "Signed firmware lookup failed with status {}",
            response.status()
        ));
    }

    let payload = response
        .json::<IpswFirmwaresResponse>()
        .await
        .map_err(|e| format!("Cannot parse signed firmware response: {e}"))?;

    let mut signed = payload
        .firmwares
        .into_iter()
        .filter(|firmware| firmware.signed)
        .collect::<Vec<_>>();

    signed.sort_by(|left, right| {
        left.version
            .cmp(&right.version)
            .then(left.buildid.cmp(&right.buildid))
    });
    signed.dedup_by(|left, right| left.version == right.version && left.buildid == right.buildid);

    if signed.is_empty() {
        return Err(format!("No signed firmwares returned for {model}"));
    }

    Ok(signed)
}

async fn fetch_signed_versions_text(model: &str) -> Result<String, String> {
    let signed = fetch_signed_firmwares(model).await?;
    Ok(signed
        .iter()
        .map(|firmware| format!("iOS {} ({}) — signed", firmware.version, firmware.buildid))
        .collect::<Vec<_>>()
        .join("\n"))
}

fn shsh_root_dir() -> Result<PathBuf, String> {
    home_dir()
        .map(|dir| dir.join("DeepEyeUnlocker").join("shsh"))
        .ok_or_else(|| "Unable to resolve home directory".to_string())
}

fn ensure_shsh_output_dir(ecid: &str) -> Result<PathBuf, String> {
    let dir = shsh_root_dir()?.join(ecid);
    fs::create_dir_all(&dir).map_err(|e| format!("Cannot create SHSH directory: {e}"))?;
    Ok(dir)
}

fn validate_existing_file(path: &str, expected_extension: &str) -> Result<PathBuf, String> {
    let trimmed = path.trim();
    if trimmed.is_empty() {
        return Err(format!("{expected_extension} path is required"));
    }

    let parsed = PathBuf::from(trimmed);
    if !parsed.exists() {
        return Err(format!("File not found: {}", parsed.display()));
    }

    let extension = parsed
        .extension()
        .and_then(|value| value.to_str())
        .unwrap_or_default();
    if !extension.eq_ignore_ascii_case(expected_extension) {
        return Err(format!(
            "Expected a .{expected_extension} file: {}",
            parsed.display()
        ));
    }

    Ok(parsed)
}

async fn read_ideviceinfo_key(app: &AppHandle, key: &str) -> Result<String, String> {
    let output = run_tool(app, "ideviceinfo", vec!["-k".into(), key.into()]).await?;
    let trimmed = output.trim();

    if trimmed.is_empty() {
        return Err(format!("No value returned for ideviceinfo key {key}"));
    }

    Ok(trimmed.to_string())
}

async fn read_ideviceinfo_key_optional(app: &AppHandle, key: &str) -> String {
    read_ideviceinfo_key(app, key)
        .await
        .unwrap_or_else(|_| "Unknown".to_string())
}

fn collect_shsh_files(dir: &Path, files: &mut Vec<PathBuf>) -> Result<(), String> {
    if !dir.exists() {
        return Ok(());
    }

    for entry in fs::read_dir(dir).map_err(|e| format!("Cannot read {}: {e}", dir.display()))? {
        let entry = entry.map_err(|e| format!("Directory entry error: {e}"))?;
        let path = entry.path();

        if path.is_dir() {
            collect_shsh_files(&path, files)?;
            continue;
        }

        let is_shsh2 = path
            .extension()
            .and_then(|value| value.to_str())
            .map(|value| value.eq_ignore_ascii_case("shsh2"))
            .unwrap_or(false);

        if is_shsh2 {
            files.push(path);
        }
    }

    Ok(())
}

/// Get ECID from connected device
#[tauri::command]
pub async fn get_ecid(app: AppHandle) -> Result<String, String> {
    read_ideviceinfo_key(&app, "UniqueChipID").await
}

/// Get board config (needed for tsschecker)
#[tauri::command]
pub async fn get_board_config(app: AppHandle) -> Result<String, String> {
    let hardware_model = read_ideviceinfo_key_optional(&app, "HardwareModel").await;
    let board_id = read_ideviceinfo_key_optional(&app, "BoardId").await;
    let chip_id = read_ideviceinfo_key_optional(&app, "ChipID").await;

    Ok(format!(
        "HardwareModel: {hardware_model}\nBoardId: {board_id}\nChipID: {chip_id}"
    ))
}

#[tauri::command]
pub async fn get_shsh_device_info(app: AppHandle) -> Result<ShshDeviceInfo, String> {
    let model = validate_model(&read_ideviceinfo_key(&app, "ProductType").await?)?;
    let ecid = validate_ecid(&read_ideviceinfo_key(&app, "UniqueChipID").await?)?;

    Ok(ShshDeviceInfo {
        ecid,
        model,
        hardware_model: read_ideviceinfo_key_optional(&app, "HardwareModel").await,
        board_id: read_ideviceinfo_key_optional(&app, "BoardId").await,
        chip_id: read_ideviceinfo_key_optional(&app, "ChipID").await,
        product_version: read_ideviceinfo_key_optional(&app, "ProductVersion").await,
    })
}

/// Save SHSH blobs for currently connected device — all signed firmwares
#[tauri::command]
pub async fn save_shsh_all_signed(
    app: AppHandle,
    model: String,
    ecid: String,
) -> Result<String, String> {
    let model = validate_model(&model)?;
    let ecid = validate_ecid(&ecid)?;
    let output_dir = ensure_shsh_output_dir(&ecid)?;
    let signed_firmwares = fetch_signed_firmwares(&model).await?;
    let total = signed_firmwares.len();
    let mut saved = 0usize;
    let mut logs = Vec::with_capacity(total);

    for firmware in signed_firmwares {
        let version = firmware.version.clone();
        let buildid = firmware.buildid.clone();
        let result = run_tool(
            &app,
            "tsschecker",
            vec![
                "-d".into(),
                model.clone(),
                "-e".into(),
                ecid.clone(),
                "-i".into(),
                version.clone(),
                "-s".into(),
                "--save-path".into(),
                output_dir.to_string_lossy().to_string(),
            ],
        )
        .await;

        match result {
            Ok(output) => {
                saved += 1;
                logs.push(format!("[OK] iOS {version} ({buildid})\n{output}"));
            }
            Err(error) => logs.push(format!("[FAIL] iOS {version} ({buildid})\n{error}")),
        }
    }

    let summary = format!(
        "Saved {saved}/{total} signed SHSH blobs for {model} ({ecid})\nOutput directory: {}",
        output_dir.display()
    );

    if saved == 0 {
        return Err(format!("{summary}\n\n{}", logs.join("\n\n")));
    }

    Ok(format!("{summary}\n\n{}", logs.join("\n\n")))
}

/// Save SHSH blob for specific iOS version
#[tauri::command]
pub async fn save_shsh_specific(
    app: AppHandle,
    model: String,
    ecid: String,
    ios: String,
) -> Result<String, String> {
    let model = validate_model(&model)?;
    let ecid = validate_ecid(&ecid)?;
    let ios = validate_ios_version(&ios)?;
    let output_dir = ensure_shsh_output_dir(&ecid)?;
    let output = run_tool(
        &app,
        "tsschecker",
        vec![
            "-d".into(),
            model.clone(),
            "-e".into(),
            ecid.clone(),
            "-i".into(),
            ios.clone(),
            "-s".into(),
            "--save-path".into(),
            output_dir.to_string_lossy().to_string(),
        ],
    )
    .await?;

    Ok(format!(
        "Saved SHSH blob for {model} on iOS {ios}\nOutput directory: {}\n\n{output}",
        output_dir.display()
    ))
}

/// Save using generator (for nonce collision downgrade)
#[tauri::command]
pub async fn save_shsh_with_generator(
    app: AppHandle,
    model: String,
    ecid: String,
    ios: String,
    generator: String,
) -> Result<String, String> {
    let model = validate_model(&model)?;
    let ecid = validate_ecid(&ecid)?;
    let ios = validate_ios_version(&ios)?;
    let generator = validate_generator(&generator)?;
    let output_dir = ensure_shsh_output_dir(&ecid)?;
    let output = run_tool(
        &app,
        "tsschecker",
        vec![
            "-d".into(),
            model.clone(),
            "-e".into(),
            ecid.clone(),
            "-i".into(),
            ios.clone(),
            "-g".into(),
            generator.clone(),
            "-s".into(),
            "--save-path".into(),
            output_dir.to_string_lossy().to_string(),
        ],
    )
    .await?;

    Ok(format!(
        "Saved generator-based SHSH blob for {model} on iOS {ios}\nGenerator: {generator}\nOutput directory: {}\n\n{output}",
        output_dir.display()
    ))
}

/// List all saved SHSH blobs
#[tauri::command]
pub async fn list_saved_shsh(app: AppHandle) -> Result<String, String> {
    let root = shsh_root_dir()?;
    let mut files = Vec::new();
    collect_shsh_files(&root, &mut files)?;

    if files.is_empty() {
        return Ok(format!("No saved SHSH2 blobs found in {}", root.display()));
    }

    files.sort();
    let listing = files
        .iter()
        .map(|path| {
            path.strip_prefix(&root)
                .unwrap_or(path)
                .display()
                .to_string()
        })
        .collect::<Vec<_>>()
        .join("\n");

    let _ = app;
    Ok(format!("Saved SHSH2 blobs in {}\n\n{listing}", root.display()))
}

/// Check which iOS versions are currently signed by Apple
#[tauri::command]
pub async fn check_signed_versions(app: AppHandle, model: String) -> Result<String, String> {
    let model = validate_model(&model)?;
    match fetch_signed_versions_text(&model).await {
        Ok(output) => Ok(output),
        Err(api_error) => {
            let output = run_tool(
                &app,
                "tsschecker",
                vec!["-d".into(), model.clone(), "--list-ios".into()],
            )
            .await?;

            let filtered_lines = output
                .lines()
                .filter(|line| {
                    let lower = line.to_ascii_lowercase();
                    lower.contains("signed") || lower.contains("available") || lower.contains("version")
                })
                .collect::<Vec<_>>();

            if filtered_lines.is_empty() {
                return Ok(format!("Signed version API fallback triggered: {api_error}\n\n{output}"));
            }

            Ok(filtered_lines.join("\n"))
        }
    }
}

/// futurerestore — restore to unsigned firmware using SHSH blob
#[tauri::command]
pub async fn futurerestore(
    app: AppHandle,
    ipsw_path: String,
    shsh_path: String,
    _sep_manifest: String,
    _baseband: String,
) -> Result<String, String> {
    let ipsw = validate_existing_file(&ipsw_path, "ipsw")?;
    let shsh = validate_existing_file(&shsh_path, "shsh2")?;
    let output = run_tool(
        &app,
        "futurerestore",
        vec![
            "-t".into(),
            shsh.to_string_lossy().to_string(),
            "--latest-sep".into(),
            "--latest-baseband".into(),
            ipsw.to_string_lossy().to_string(),
        ],
    )
    .await?;

    Ok(format!(
        "futurerestore started\nIPSW: {}\nSHSH2: {}\n\n{output}",
        ipsw.display(),
        shsh.display()
    ))
}

/// futurerestore no baseband (WiFi iPad, iPod)
#[tauri::command]
pub async fn futurerestore_no_baseband(
    app: AppHandle,
    ipsw_path: String,
    shsh_path: String,
) -> Result<String, String> {
    let ipsw = validate_existing_file(&ipsw_path, "ipsw")?;
    let shsh = validate_existing_file(&shsh_path, "shsh2")?;
    let output = run_tool(
        &app,
        "futurerestore",
        vec![
            "-t".into(),
            shsh.to_string_lossy().to_string(),
            "--no-baseband".into(),
            ipsw.to_string_lossy().to_string(),
        ],
    )
    .await?;

    Ok(format!(
        "futurerestore (--no-baseband) started\nIPSW: {}\nSHSH2: {}\n\n{output}",
        ipsw.display(),
        shsh.display()
    ))
}
