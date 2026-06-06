use serde::Serialize;
use std::path::{Path, PathBuf};
use tauri::AppHandle;
use tauri::Manager;
use tauri_plugin_shell::process::CommandEvent;
use tauri_plugin_shell::ShellExt;

fn get_tool_path(app: &AppHandle, tool: &str) -> Result<PathBuf, String> {
    #[cfg(target_os = "windows")]
    let (resource_subdir, executable_name) = {
        let exe_name = if tool.ends_with(".exe") {
            tool.to_string()
        } else {
            format!("{tool}.exe")
        };
        ("windows", exe_name)
    };

    #[cfg(target_os = "linux")]
    let (resource_subdir, executable_name) = ("linux", tool.to_string());

    #[cfg(all(not(target_os = "windows"), not(target_os = "linux")))]
    let (resource_subdir, executable_name) = ("macos", tool.to_string());

    let resource_path = app
        .path()
        .resource_dir()
        .map_err(|e| format!("resource dir error: {e}"))?
        .join(resource_subdir)
        .join(executable_name);

    let resolved = if resource_path.exists() {
        #[cfg(unix)]
        {
            use std::os::unix::fs::PermissionsExt;
            std::fs::set_permissions(&resource_path, std::fs::Permissions::from_mode(0o755))
                .map_err(|e| format!("chmod error: {e}"))?;
        }
        resource_path
    } else {
        PathBuf::from(tool)
    };

    Ok(resolved)
}

#[derive(Serialize)]
pub struct FlashResult {
    pub success: bool,
    pub partition: String,
    pub message: String,
}

#[derive(Serialize, Clone)]
pub struct FastbootDevice {
    pub serial: String,
    pub state: String,
}

#[derive(Serialize, Clone)]
pub struct FastbootVariable {
    pub key: String,
    pub value: String,
}

#[derive(Serialize, Clone)]
pub struct FastbootQueryResult {
    pub serial: Option<String>,
    pub variables: Vec<FastbootVariable>,
    pub raw_output: String,
}

struct CommandOutput {
    exit_code: i32,
    stdout: String,
    stderr: String,
}

impl CommandOutput {
    fn combined_output(&self) -> String {
        let mut sections = Vec::new();

        if !self.stdout.trim().is_empty() {
            sections.push(self.stdout.trim());
        }

        if !self.stderr.trim().is_empty() {
            sections.push(self.stderr.trim());
        }

        sections.join("\n")
    }
}

fn ensure_existing_file(path: &str, expected_extension: Option<&str>) -> Result<(), String> {
    let file_path = Path::new(path);
    if !file_path.is_file() {
        return Err(format!("File not found: {path}"));
    }

    if let Some(extension) = expected_extension {
        let expected = extension.trim_start_matches('.');
        let actual = file_path
            .extension()
            .and_then(|value| value.to_str())
            .unwrap_or_default();

        if !actual.eq_ignore_ascii_case(expected) {
            return Err(format!("Expected a .{expected} file: {path}"));
        }
    }

    Ok(())
}

fn validate_token(label: &str, value: &str) -> Result<(), String> {
    let trimmed = value.trim();
    if trimmed.is_empty() {
        return Err(format!("{label} cannot be empty"));
    }

    if trimmed.len() > 128 {
        return Err(format!("{label} is too long"));
    }

    let valid = trimmed.chars().all(|character| {
        character.is_ascii_alphanumeric() || matches!(character, '_' | '-' | '.' | ':')
    });

    if !valid {
        return Err(format!("Invalid {label}: {trimmed}"));
    }

    Ok(())
}

fn build_fastboot_args(serial: Option<String>, args: Vec<String>) -> Result<Vec<String>, String> {
    let mut resolved = Vec::new();

    if let Some(serial) = serial {
        let trimmed = serial.trim().to_string();
        if !trimmed.is_empty() {
            validate_token("fastboot serial", &trimmed)?;
            resolved.push("-s".to_string());
            resolved.push(trimmed);
        }
    }

    resolved.extend(args);
    Ok(resolved)
}

async fn run_tool_command(
    app: &AppHandle,
    tool: &str,
    args: Vec<String>,
) -> Result<CommandOutput, String> {
    let binary = get_tool_path(app, tool)?;
    let binary = binary
        .to_str()
        .ok_or_else(|| format!("invalid {tool} path"))?;

    let shell = app.shell();
    let (mut rx, _child) = shell
        .command(binary)
        .args(args)
        .spawn()
        .map_err(|error| format!("{tool} spawn error: {error}"))?;

    let mut stdout = String::new();
    let mut stderr = String::new();
    let mut exit_code = -1;

    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(bytes) => stdout.push_str(&String::from_utf8_lossy(&bytes)),
            CommandEvent::Stderr(bytes) => stderr.push_str(&String::from_utf8_lossy(&bytes)),
            CommandEvent::Error(error) => return Err(error),
            CommandEvent::Terminated(status) => {
                exit_code = status.code.unwrap_or(-1);
                break;
            }
            _ => {}
        }
    }

    Ok(CommandOutput {
        exit_code,
        stdout,
        stderr,
    })
}

fn format_command_message(prefix: &str, output: &CommandOutput) -> String {
    let combined = output.combined_output();
    if combined.is_empty() {
        prefix.to_string()
    } else {
        format!("{prefix}\n{combined}")
    }
}

fn parse_fastboot_devices(output: &str) -> Vec<FastbootDevice> {
    output
        .lines()
        .filter_map(|line| {
            let trimmed = line.trim();
            if trimmed.is_empty()
                || trimmed.starts_with("<")
                || trimmed.starts_with("waiting")
                || trimmed.starts_with("error:")
            {
                return None;
            }

            let mut parts = trimmed.split_whitespace();
            let serial = parts.next()?;
            let state = parts.next().unwrap_or("unknown");

            Some(FastbootDevice {
                serial: serial.to_string(),
                state: state.to_string(),
            })
        })
        .collect()
}

fn parse_fastboot_variables(output: &str) -> Vec<FastbootVariable> {
    let mut variables = Vec::new();

    for line in output.lines() {
        let mut trimmed = line.trim();
        if trimmed.is_empty() {
            continue;
        }

        if let Some(rest) = trimmed.strip_prefix("(bootloader)") {
            trimmed = rest.trim();
        }

        if let Some(rest) = trimmed.strip_prefix("INFO") {
            trimmed = rest.trim();
        }

        if trimmed.is_empty()
            || trimmed.starts_with("Finished")
            || trimmed.starts_with("total time:")
            || trimmed.starts_with("FAILED")
            || trimmed.starts_with("error:")
            || trimmed.starts_with("fastboot:")
            || trimmed.starts_with("<")
        {
            continue;
        }

        let pair = trimmed
            .rfind(": ")
            .map(|index| (&trimmed[..index], &trimmed[index + 2..]))
            .or_else(|| {
                trimmed
                    .rfind(':')
                    .map(|index| (&trimmed[..index], &trimmed[index + 1..]))
            });

        let Some((key, value)) = pair else {
            continue;
        };

        let key = key.trim();
        let value = value.trim();
        if key.is_empty() || value.is_empty() {
            continue;
        }

        variables.push(FastbootVariable {
            key: key.to_string(),
            value: value.to_string(),
        });
    }

    variables
}

async fn run_fastboot_action(
    app: &AppHandle,
    serial: Option<String>,
    args: Vec<String>,
) -> Result<CommandOutput, String> {
    let arguments = build_fastboot_args(serial, args)?;
    run_tool_command(app, "fastboot", arguments).await
}

async fn reboot_via_adb_or_fastboot(
    app: &AppHandle,
    adb_mode: &str,
    fastboot_target: &str,
) -> Result<String, String> {
    let adb_attempt =
        run_tool_command(app, "adb", vec!["reboot".to_string(), adb_mode.to_string()]).await;

    if let Ok(output) = adb_attempt {
        if output.exit_code == 0 {
            return Ok(format_command_message(
                &format!("Rebooting to {adb_mode}"),
                &output,
            ));
        }
    }

    let fastboot_args = match fastboot_target {
        "system" | "normal" | "" => vec!["reboot".to_string()],
        "bootloader" => vec!["reboot-bootloader".to_string()],
        "fastboot" | "fastbootd" => vec!["reboot".to_string(), "fastboot".to_string()],
        "recovery" => vec!["reboot".to_string(), "recovery".to_string()],
        _ => return Err(format!("Unsupported reboot target: {fastboot_target}")),
    };

    let output = run_fastboot_action(app, None, fastboot_args).await?;
    if output.exit_code != 0 {
        return Err(format_command_message(
            &format!("Reboot to {fastboot_target} failed"),
            &output,
        ));
    }

    Ok(format_command_message(
        &format!("Rebooting to {fastboot_target}"),
        &output,
    ))
}

/// Flash a custom ROM ZIP via TWRP sideload (device must be in TWRP sideload mode)
#[tauri::command]
pub async fn rom_sideload_zip(app: AppHandle, zip_path: String) -> Result<FlashResult, String> {
    if zip_path.contains("..") {
        return Err("Path traversal detected".to_string());
    }
    let path = Path::new(&zip_path);
    let path_resolver = app.path();
    let is_safe = [
        path_resolver.document_dir(),
        path_resolver.download_dir(),
        path_resolver.desktop_dir(),
    ]
    .into_iter()
    .flatten()
    .any(|allowed| path.starts_with(allowed));

    if !is_safe {
        return Err("ZIP file must be in Documents, Downloads, or Desktop".to_string());
    }

    ensure_existing_file(&zip_path, Some("zip"))?;

    let output = run_tool_command(&app, "adb", vec!["sideload".to_string(), zip_path]).await?;

    if output.exit_code != 0 {
        return Ok(FlashResult {
            success: false,
            partition: "sideload".into(),
            message: format_command_message("Sideload failed", &output),
        });
    }

    Ok(FlashResult {
        success: true,
        partition: "sideload".into(),
        message: format_command_message("ZIP sideloaded successfully", &output),
    })
}

/// Flash a partition image via fastboot
#[tauri::command]
pub async fn rom_flash_partition(
    app: AppHandle,
    partition: String,
    image_path: String,
    serial: Option<String>,
) -> Result<FlashResult, String> {
    if image_path.contains("..") {
        return Err("Path traversal detected".to_string());
    }
    let path = Path::new(&image_path);
    let path_resolver = app.path();
    let is_safe = [
        path_resolver.document_dir(),
        path_resolver.download_dir(),
        path_resolver.desktop_dir(),
    ]
    .into_iter()
    .flatten()
    .any(|allowed| path.starts_with(allowed));

    if !is_safe {
        return Err("Image file must be in Documents, Downloads, or Desktop".to_string());
    }

    validate_token("partition", &partition)?;
    ensure_existing_file(&image_path, None)?;

    let output = run_fastboot_action(
        &app,
        serial,
        vec!["flash".to_string(), partition.clone(), image_path],
    )
    .await?;

    Ok(FlashResult {
        success: output.exit_code == 0,
        partition,
        message: if output.exit_code == 0 {
            format_command_message("Partition flashed", &output)
        } else {
            format_command_message("Fastboot flash failed", &output)
        },
    })
}

/// Erase a specific partition via fastboot
#[tauri::command]
pub async fn fastboot_erase_partition(
    app: AppHandle,
    partition: String,
    serial: Option<String>,
) -> Result<FlashResult, String> {
    validate_token("partition", &partition)?;

    let output =
        run_fastboot_action(&app, serial, vec!["erase".to_string(), partition.clone()]).await?;

    Ok(FlashResult {
        success: output.exit_code == 0,
        partition,
        message: if output.exit_code == 0 {
            format_command_message("Partition erased", &output)
        } else {
            format_command_message("Fastboot erase failed", &output)
        },
    })
}

/// Wipe partitions via fastboot (for clean flash)
#[tauri::command]
pub async fn rom_wipe_data(app: AppHandle, serial: Option<String>) -> Result<String, String> {
    let direct = run_fastboot_action(&app, serial.clone(), vec!["-w".to_string()]).await?;
    if direct.exit_code == 0 {
        return Ok(format_command_message("Data wiped", &direct));
    }

    let userdata = run_fastboot_action(
        &app,
        serial.clone(),
        vec!["erase".to_string(), "userdata".to_string()],
    )
    .await?;
    if userdata.exit_code != 0 {
        return Err(format_command_message("Fastboot wipe failed", &userdata));
    }

    let cache =
        run_fastboot_action(&app, serial, vec!["erase".to_string(), "cache".to_string()]).await?;

    let combined = CommandOutput {
        exit_code: if cache.exit_code == 0 {
            0
        } else {
            cache.exit_code
        },
        stdout: [userdata.stdout, cache.stdout]
            .into_iter()
            .filter(|value| !value.trim().is_empty())
            .collect::<Vec<_>>()
            .join("\n"),
        stderr: [direct.stderr, userdata.stderr, cache.stderr]
            .into_iter()
            .filter(|value| !value.trim().is_empty())
            .collect::<Vec<_>>()
            .join("\n"),
    };

    Ok(format_command_message("Data wiped", &combined))
}

/// Reboot to recovery (for TWRP sideload workflow)
#[tauri::command]
pub async fn rom_reboot_recovery(app: AppHandle) -> Result<String, String> {
    reboot_via_adb_or_fastboot(&app, "recovery", "recovery").await
}

/// Reboot to bootloader (for fastboot flash workflow)
#[tauri::command]
pub async fn rom_reboot_bootloader(app: AppHandle) -> Result<String, String> {
    reboot_via_adb_or_fastboot(&app, "bootloader", "bootloader").await
}

/// Enumerate currently connected fastboot devices.
#[tauri::command]
pub async fn fastboot_list_devices(app: AppHandle) -> Result<Vec<FastbootDevice>, String> {
    let output = run_fastboot_action(&app, None, vec!["devices".to_string()]).await?;
    if output.exit_code != 0 {
        return Err(format!(
            "fastboot devices failed\n{}",
            output.combined_output()
        ));
    }

    Ok(parse_fastboot_devices(&output.combined_output()))
}

/// Query fastboot getvar:all and return parsed variables with raw output.
#[tauri::command]
pub async fn fastboot_get_all_variables(
    app: AppHandle,
    serial: Option<String>,
) -> Result<FastbootQueryResult, String> {
    let requested_serial = serial.clone().filter(|value| !value.trim().is_empty());
    let output =
        run_fastboot_action(&app, serial, vec!["getvar".to_string(), "all".to_string()]).await?;

    let raw_output = output.combined_output();
    let variables = parse_fastboot_variables(&raw_output);
    if output.exit_code != 0 && variables.is_empty() {
        return Err(format!("fastboot getvar all failed\n{raw_output}"));
    }

    Ok(FastbootQueryResult {
        serial: requested_serial,
        variables,
        raw_output,
    })
}

/// Send the standard fastboot unlock sequences.
#[tauri::command]
pub async fn fastboot_unlock_bootloader(
    app: AppHandle,
    serial: Option<String>,
) -> Result<FlashResult, String> {
    let attempts = [
        vec!["flashing".to_string(), "unlock".to_string()],
        vec!["oem".to_string(), "unlock".to_string()],
    ];

    let mut failures = Vec::new();

    for args in attempts {
        let command_name = args.join(" ");
        let output = run_fastboot_action(&app, serial.clone(), args).await?;
        let combined = output.combined_output();
        let already_unlocked = combined.to_lowercase().contains("already unlocked");

        if output.exit_code == 0 || already_unlocked {
            return Ok(FlashResult {
                success: true,
                partition: "bootloader".to_string(),
                message: format_command_message(
                    &format!("Bootloader unlock sequence accepted via '{command_name}'"),
                    &output,
                ),
            });
        }

        failures.push(format!("{command_name}: {combined}"));
    }

    Ok(FlashResult {
        success: false,
        partition: "bootloader".to_string(),
        message: format!("Fastboot unlock failed\n{}", failures.join("\n")),
    })
}

/// Reboot an active fastboot session to a requested target.
#[tauri::command]
pub async fn fastboot_reboot_target(
    app: AppHandle,
    target: Option<String>,
    serial: Option<String>,
) -> Result<String, String> {
    let normalized = target
        .unwrap_or_else(|| "system".to_string())
        .trim()
        .to_lowercase();

    let args = match normalized.as_str() {
        "" | "system" | "normal" => vec!["reboot".to_string()],
        "bootloader" => vec!["reboot-bootloader".to_string()],
        "fastboot" | "fastbootd" => vec!["reboot".to_string(), "fastboot".to_string()],
        "recovery" => vec!["reboot".to_string(), "recovery".to_string()],
        _ => return Err(format!("Unsupported fastboot reboot target: {normalized}")),
    };

    let output = run_fastboot_action(&app, serial, args).await?;
    if output.exit_code != 0 {
        return Err(format_command_message("Fastboot reboot failed", &output));
    }

    Ok(format_command_message(
        &format!("Rebooting to {normalized}"),
        &output,
    ))
}
