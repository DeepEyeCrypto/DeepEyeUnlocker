use serde::{Deserialize, Serialize};
use std::fs;
use std::path::PathBuf;
use tauri::AppHandle;
use tauri::Manager;

#[allow(dead_code)]
#[derive(Serialize, Deserialize, Clone)]
pub struct DeviceHistoryEntry {
    pub id: String,
    pub timestamp: String,
    pub model: String,
    pub serial: String,
    pub os_version: String,
    pub mode: String,
    pub action: String,
    pub result: String,
    pub platform: String,
}

#[allow(dead_code)]
#[derive(Serialize, Deserialize, Default)]
struct HistoryStore {
    entries: Vec<DeviceHistoryEntry>,
}

#[allow(dead_code)]
fn history_path(app: &AppHandle) -> Result<PathBuf, String> {
    let data_dir = app
        .path()
        .app_data_dir()
        .map_err(|e| format!("app data dir error: {e}"))?;
    fs::create_dir_all(&data_dir).map_err(|e| format!("create data dir error: {e}"))?;
    Ok(data_dir.join("device_history.json"))
}

#[allow(dead_code)]
fn read_store(app: &AppHandle) -> Result<HistoryStore, String> {
    let path = history_path(app)?;
    if !path.exists() {
        return Ok(HistoryStore::default());
    }
    let data = fs::read_to_string(&path).map_err(|e| format!("read history error: {e}"))?;
    serde_json::from_str(&data).map_err(|e| format!("parse history error: {e}"))
}

#[allow(dead_code)]
fn write_store(app: &AppHandle, store: &HistoryStore) -> Result<(), String> {
    let path = history_path(app)?;
    let json =
        serde_json::to_string_pretty(store).map_err(|e| format!("serialize history error: {e}"))?;
    fs::write(&path, json).map_err(|e| format!("write history error: {e}"))
}

#[tauri::command]
#[allow(clippy::too_many_arguments)]
#[allow(dead_code)]
pub async fn history_add_entry(
    app: AppHandle,
    model: String,
    serial: String,
    os_version: String,
    mode: String,
    action: String,
    result: String,
    platform: String,
) -> Result<DeviceHistoryEntry, String> {
    let mut store = read_store(&app)?;

    let entry = DeviceHistoryEntry {
        id: format!(
            "{}-{}",
            chrono::Utc::now().timestamp_millis(),
            store.entries.len()
        ),
        timestamp: chrono::Utc::now().to_rfc3339(),
        model,
        serial,
        os_version,
        mode,
        action,
        result,
        platform,
    };

    store.entries.push(entry.clone());

    // Keep max 500 entries
    if store.entries.len() > 500 {
        let drain_count = store.entries.len() - 500;
        store.entries.drain(..drain_count);
    }

    write_store(&app, &store)?;
    Ok(entry)
}

#[tauri::command]
#[allow(dead_code)]
pub async fn history_get_entries(
    app: AppHandle,
    limit: Option<usize>,
) -> Result<Vec<DeviceHistoryEntry>, String> {
    let store = read_store(&app)?;
    let limit = limit.unwrap_or(100).min(500);
    let entries: Vec<DeviceHistoryEntry> =
        store.entries.iter().rev().take(limit).cloned().collect();
    Ok(entries)
}

#[tauri::command]
#[allow(dead_code)]
pub async fn history_clear(app: AppHandle) -> Result<String, String> {
    let store = HistoryStore::default();
    write_store(&app, &store)?;
    Ok("History cleared".to_string())
}

#[tauri::command]
#[allow(dead_code)]
pub async fn history_delete_entry(app: AppHandle, entry_id: String) -> Result<String, String> {
    let mut store = read_store(&app)?;
    let before = store.entries.len();
    store.entries.retain(|e| e.id != entry_id);
    if store.entries.len() == before {
        return Err(format!("Entry '{}' not found", entry_id));
    }
    write_store(&app, &store)?;
    Ok(format!("Entry '{}' deleted", entry_id))
}

#[tauri::command]
#[allow(dead_code)]
pub async fn history_export_json(app: AppHandle) -> Result<String, String> {
    let store = read_store(&app)?;
    serde_json::to_string_pretty(&store.entries).map_err(|e| format!("export error: {e}"))
}
