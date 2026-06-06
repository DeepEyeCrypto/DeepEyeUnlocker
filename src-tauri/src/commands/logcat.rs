use std::fs;
use std::path::Path;
use std::sync::{Mutex, OnceLock};

use serde::{Deserialize, Serialize};
use tauri::{AppHandle, Emitter};
use tauri_plugin_shell::process::{CommandChild, CommandEvent};
use tauri_plugin_shell::ShellExt;

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct LogcatEntry {
    pub timestamp: String,
    pub level: String,
    pub tag: String,
    pub pid: Option<u32>,
    pub tid: Option<u32>,
    pub message: String,
    pub raw: String,
}

#[derive(Debug, Serialize, Deserialize, Clone, Default)]
pub struct LogcatFilter {
    pub serial: Option<String>,
    pub level: Option<String>,
    pub tag: Option<String>,
    pub keyword: Option<String>,
    pub pid: Option<u32>,
}

#[derive(Debug, Serialize, Clone)]
#[serde(rename_all = "camelCase")]
struct LogcatStatusPayload {
    running: bool,
    serial: Option<String>,
    message: String,
}

#[derive(Debug, Serialize, Clone)]
struct LogcatErrorPayload {
    message: String,
}

#[derive(Debug, Serialize, Clone)]
#[serde(rename_all = "camelCase")]
struct LogcatLinePayload {
    line: String,
    raw: String,
    timestamp: Option<String>,
    level: Option<String>,
    tag: Option<String>,
    pid: Option<u32>,
}

#[derive(Default)]
struct ActiveLogcatStream {
    child: Option<CommandChild>,
    task: Option<LogcatTaskHandle>,
    pid: Option<u32>,
    serial: Option<String>,
}

type LogcatTaskHandle = tauri::async_runtime::JoinHandle<()>;
type DetachedLogcatStream = (
    Option<CommandChild>,
    Option<LogcatTaskHandle>,
    Option<String>,
);

static LOGCAT_STREAM: OnceLock<Mutex<ActiveLogcatStream>> = OnceLock::new();

fn stream_state() -> &'static Mutex<ActiveLogcatStream> {
    LOGCAT_STREAM.get_or_init(|| Mutex::new(ActiveLogcatStream::default()))
}

fn emit_status(app: &AppHandle, running: bool, serial: Option<String>, message: impl Into<String>) {
    let payload = LogcatStatusPayload {
        running,
        serial,
        message: message.into(),
    };

    let _ = app.emit("logcat-status", payload.clone());
    let _ = app.emit("logcat-state", payload);
}

fn emit_error(app: &AppHandle, message: impl Into<String>) {
    let payload = LogcatErrorPayload {
        message: message.into(),
    };

    let _ = app.emit("logcat-error", payload);
}

fn emit_entry(app: &AppHandle, entry: &LogcatEntry) {
    let line_payload = LogcatLinePayload {
        line: entry.raw.clone(),
        raw: entry.raw.clone(),
        timestamp: if entry.timestamp.is_empty() {
            None
        } else {
            Some(entry.timestamp.clone())
        },
        level: if entry.level.is_empty() {
            None
        } else {
            Some(entry.level.clone())
        },
        tag: if entry.tag.is_empty() {
            None
        } else {
            Some(entry.tag.clone())
        },
        pid: entry.pid,
    };

    let _ = app.emit("logcat-entry", entry.clone());
    let _ = app.emit("logcat-line", line_payload);
}

fn normalize_filter_text(value: &Option<String>) -> Option<String> {
    value.as_ref().and_then(|item| {
        let trimmed = item.trim();
        if trimmed.is_empty() {
            None
        } else {
            Some(trimmed.to_string())
        }
    })
}

impl LogcatFilter {
    fn matches(&self, entry: &LogcatEntry) -> bool {
        if let Some(level) = normalize_filter_text(&self.level) {
            if !entry.level.eq_ignore_ascii_case(&level) {
                return false;
            }
        }

        if let Some(tag) = normalize_filter_text(&self.tag) {
            if !entry.tag.to_lowercase().contains(&tag.to_lowercase()) {
                return false;
            }
        }

        if let Some(keyword) = normalize_filter_text(&self.keyword) {
            let search_space =
                format!("{} {} {}", entry.tag, entry.message, entry.raw).to_lowercase();
            if !search_space.contains(&keyword.to_lowercase()) {
                return false;
            }
        }

        if let Some(pid) = self.pid {
            if entry.pid != Some(pid) {
                return false;
            }
        }

        true
    }
}

fn parse_logcat_line(line: &str) -> LogcatEntry {
    let raw = line.trim_end_matches(&['\r', '\n'][..]).to_string();

    let mut fallback = LogcatEntry {
        timestamp: String::new(),
        level: String::new(),
        tag: String::new(),
        pid: None,
        tid: None,
        message: raw.clone(),
        raw,
    };

    let parts: Vec<&str> = fallback.raw.split_whitespace().collect();
    if parts.len() < 6 {
        return fallback;
    }

    let level = parts[4];
    if !matches!(level, "V" | "D" | "I" | "W" | "E" | "F" | "S") {
        return fallback;
    }

    let remainder = parts[5..].join(" ");
    let (tag, message) = match remainder.split_once(':') {
        Some((tag, message)) => (tag.trim().to_string(), message.trim_start().to_string()),
        None => (remainder.trim().to_string(), String::new()),
    };

    fallback.timestamp = format!("{} {}", parts[0], parts[1]);
    fallback.level = level.to_string();
    fallback.tag = tag;
    fallback.pid = parts[2].parse::<u32>().ok();
    fallback.tid = parts[3].parse::<u32>().ok();
    fallback.message = if message.is_empty() {
        fallback.raw.clone()
    } else {
        message
    };
    fallback
}

fn build_logcat_args(serial: &Option<String>, include_dump: bool) -> Vec<String> {
    let mut args = Vec::new();

    if let Some(serial_value) = normalize_filter_text(serial) {
        args.push("-s".to_string());
        args.push(serial_value);
    }

    args.push("logcat".to_string());

    if include_dump {
        args.push("-d".to_string());
    }

    args.push("-v".to_string());
    args.push("threadtime".to_string());
    args
}

async fn run_adb_output(app: &AppHandle, args: Vec<String>) -> Result<String, String> {
    let adb_path = crate::commands::adb::find_adb();
    let output = app
        .shell()
        .command(&adb_path)
        .args(args)
        .output()
        .await
        .map_err(|error| format!("adb spawn error: {error}"))?;

    let stdout = String::from_utf8_lossy(&output.stdout).to_string();
    let stderr = String::from_utf8_lossy(&output.stderr).to_string();

    if output.status.success() {
        Ok(stdout)
    } else {
        let stderr_text = stderr.trim();
        if stderr_text.is_empty() {
            Err(stdout.trim().to_string())
        } else {
            Err(stderr_text.to_string())
        }
    }
}

fn drain_lines(buffer: &mut String, chunk: &str) -> Vec<String> {
    buffer.push_str(chunk);
    let mut lines = Vec::new();

    while let Some(position) = buffer.find('\n') {
        let drained: String = buffer.drain(..=position).collect();
        let normalized = drained.trim_end_matches(&['\r', '\n'][..]).to_string();
        if !normalized.is_empty() {
            lines.push(normalized);
        }
    }

    lines
}

fn flush_line(buffer: &mut String) -> Option<String> {
    let normalized = buffer
        .trim_end_matches(&['\r', '\n'][..])
        .trim()
        .to_string();
    buffer.clear();

    if normalized.is_empty() {
        None
    } else {
        Some(normalized)
    }
}

fn is_disconnect_error(message: &str) -> bool {
    let normalized = message.to_lowercase();
    normalized.contains("device offline")
        || normalized.contains("device not found")
        || normalized.contains("no devices/emulators found")
        || normalized.contains("closed")
}

fn clear_stream_if_pid(pid: u32) -> Result<Option<String>, String> {
    let mut state = stream_state()
        .lock()
        .map_err(|_| "logcat state lock poisoned".to_string())?;

    if state.pid == Some(pid) {
        state.child = None;
        state.task = None;
        state.pid = None;
        Ok(state.serial.take())
    } else {
        Ok(None)
    }
}

fn take_stream() -> Result<DetachedLogcatStream, String> {
    let mut state = stream_state()
        .lock()
        .map_err(|_| "logcat state lock poisoned".to_string())?;

    let child = state.child.take();
    let task = state.task.take();
    let serial = state.serial.take();
    state.pid = None;
    Ok((child, task, serial))
}

async fn stop_logcat_stream_internal(app: &AppHandle, message: &str) -> Result<(), String> {
    let (child, task, serial) = take_stream()?;

    if let Some(join_handle) = task {
        join_handle.abort();
    }

    if let Some(command_child) = child {
        command_child
            .kill()
            .map_err(|error| format!("failed to terminate logcat stream: {error}"))?;
    }

    emit_status(app, false, serial, message.to_string());
    Ok(())
}

async fn dump_logcat_entries(
    app: &AppHandle,
    filter: &LogcatFilter,
) -> Result<Vec<LogcatEntry>, String> {
    let output = run_adb_output(app, build_logcat_args(&filter.serial, true)).await?;

    Ok(output
        .lines()
        .map(parse_logcat_line)
        .filter(|entry| filter.matches(entry))
        .collect())
}

fn format_entry_for_export(entry: &LogcatEntry) -> String {
    if !entry.raw.trim().is_empty() {
        return entry.raw.clone();
    }

    let pid = entry
        .pid
        .map_or_else(|| "-".to_string(), |value| value.to_string());
    let tid = entry
        .tid
        .map_or_else(|| "-".to_string(), |value| value.to_string());
    format!(
        "{} {:>6} {:>6} {} {}: {}",
        entry.timestamp, pid, tid, entry.level, entry.tag, entry.message
    )
}

#[tauri::command]
pub async fn start_logcat_stream(app: AppHandle, filter: LogcatFilter) -> Result<String, String> {
    let _ = stop_logcat_stream_internal(&app, "Preparing logcat stream").await;

    let args = build_logcat_args(&filter.serial, false);
    let adb_path = crate::commands::adb::find_adb();
    let (mut receiver, child) = app
        .shell()
        .command(&adb_path)
        .args(args)
        .spawn()
        .map_err(|error| format!("failed to spawn adb logcat: {error}"))?;

    let pid = child.pid();
    let serial = filter.serial.clone();
    let task_app = app.clone();
    let task_filter = filter.clone();
    let task_serial = serial.clone();

    let task = tauri::async_runtime::spawn(async move {
        let mut stdout_buffer = String::new();
        let mut stderr_buffer = String::new();
        let mut error_message: Option<String> = None;

        while let Some(event) = receiver.recv().await {
            match event {
                CommandEvent::Stdout(bytes) => {
                    let chunk = String::from_utf8_lossy(&bytes).to_string();
                    for line in drain_lines(&mut stdout_buffer, &chunk) {
                        let entry = parse_logcat_line(&line);
                        if task_filter.matches(&entry) {
                            emit_entry(&task_app, &entry);
                        }
                    }
                }
                CommandEvent::Stderr(bytes) => {
                    let chunk = String::from_utf8_lossy(&bytes).to_string();
                    for line in drain_lines(&mut stderr_buffer, &chunk) {
                        emit_error(&task_app, line.clone());
                        if is_disconnect_error(&line) {
                            error_message = Some(line);
                            break;
                        }
                    }

                    if error_message.is_some() {
                        break;
                    }
                }
                CommandEvent::Error(error) => {
                    error_message = Some(error);
                    break;
                }
                CommandEvent::Terminated(status) => {
                    if status.code.unwrap_or_default() != 0 {
                        error_message = Some(format!(
                            "adb logcat exited with code {}",
                            status.code.unwrap_or(-1)
                        ));
                    }
                    break;
                }
                _ => {}
            }
        }

        if let Some(line) = flush_line(&mut stdout_buffer) {
            let entry = parse_logcat_line(&line);
            if task_filter.matches(&entry) {
                emit_entry(&task_app, &entry);
            }
        }

        if let Some(line) = flush_line(&mut stderr_buffer) {
            emit_error(&task_app, line.clone());
            if error_message.is_none() {
                error_message = Some(line);
            }
        }

        let active_serial = clear_stream_if_pid(pid).ok().flatten().or(task_serial);

        if let Some(message) = error_message {
            emit_error(&task_app, message.clone());
            emit_status(
                &task_app,
                false,
                active_serial,
                format!("Logcat stopped: {message}"),
            );
        } else {
            emit_status(&task_app, false, active_serial, "Logcat stream stopped");
        }
    });

    {
        let mut state = stream_state()
            .lock()
            .map_err(|_| "logcat state lock poisoned".to_string())?;
        state.child = Some(child);
        state.task = Some(task);
        state.pid = Some(pid);
        state.serial = serial.clone();
    }

    let target_label = serial
        .clone()
        .filter(|value| !value.trim().is_empty())
        .unwrap_or_else(|| "default transport".to_string());

    emit_status(
        &app,
        true,
        serial,
        format!("Logcat started for {target_label}"),
    );

    Ok(format!("Logcat stream started for {target_label}"))
}

#[tauri::command]
pub async fn stop_logcat_stream(app: AppHandle) -> Result<(), String> {
    stop_logcat_stream_internal(&app, "Logcat stream stopped").await
}

#[tauri::command]
pub async fn clear_logcat_buffer(app: AppHandle, serial: Option<String>) -> Result<String, String> {
    let mut args = Vec::new();

    if let Some(serial_value) = normalize_filter_text(&serial) {
        args.push("-s".to_string());
        args.push(serial_value);
    }

    args.push("logcat".to_string());
    args.push("-c".to_string());

    run_adb_output(&app, args).await?;

    let label = serial
        .filter(|value| !value.trim().is_empty())
        .unwrap_or_else(|| "default transport".to_string());
    Ok(format!("Cleared logcat buffer for {label}"))
}

#[tauri::command]
pub async fn export_logcat_to_file(
    app: tauri::AppHandle,
    entries: Vec<LogcatEntry>,
    file_path: String,
) -> Result<String, String> {
    if file_path.contains("..") {
        return Err("Path traversal detected".to_string());
    }

    let export_path = Path::new(&file_path);

    use tauri::Manager;
    let path_resolver = app.path();
    let is_safe = [
        path_resolver.document_dir(),
        path_resolver.download_dir(),
        path_resolver.desktop_dir(),
        path_resolver.app_data_dir(),
    ]
    .into_iter()
    .flatten()
    .any(|safe_dir| export_path.starts_with(&safe_dir));

    if !is_safe {
        return Err("Security error: Export path must be within user Documents, Downloads, Desktop, or AppData directories".to_string());
    }

    if let Some(parent) = export_path.parent() {
        if !parent.as_os_str().is_empty() {
            fs::create_dir_all(parent)
                .map_err(|error| format!("failed to prepare export directory: {error}"))?;
        }
    }

    let payload = entries
        .iter()
        .map(format_entry_for_export)
        .collect::<Vec<_>>()
        .join("\n");

    fs::write(export_path, payload)
        .map_err(|error| format!("failed to write export file: {error}"))?;
    Ok(file_path)
}

#[tauri::command]
pub async fn adb_logcat_start(app: AppHandle, serial: Option<String>) -> Result<(), String> {
    start_logcat_stream(
        app,
        LogcatFilter {
            serial,
            ..LogcatFilter::default()
        },
    )
    .await
    .map(|_| ())
}

#[tauri::command]
pub async fn adb_logcat_stop(app: AppHandle) -> Result<(), String> {
    stop_logcat_stream(app).await
}

#[tauri::command]
pub async fn adb_logcat_clear(app: AppHandle, serial: Option<String>) -> Result<(), String> {
    clear_logcat_buffer(app, serial).await.map(|_| ())
}

#[tauri::command]
pub async fn adb_logcat_dump(
    app: AppHandle,
    serial: Option<String>,
    filter_spec: Option<String>,
) -> Result<Vec<LogcatEntry>, String> {
    dump_logcat_entries(
        &app,
        &LogcatFilter {
            serial,
            keyword: normalize_filter_text(&filter_spec),
            ..LogcatFilter::default()
        },
    )
    .await
}

#[tauri::command]
pub async fn adb_logcat_export(
    app: AppHandle,
    entries: Vec<LogcatEntry>,
    output_path: String,
) -> Result<String, String> {
    export_logcat_to_file(app, entries, output_path).await
}
