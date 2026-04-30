use chrono::Local;
use rusqlite::{params, Connection, Result};
use serde::{Deserialize, Serialize};
use std::fs;
use std::path::PathBuf;

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct HistoryEntry {
    pub id: i64,
    pub timestamp: String,
    pub device_name: String,
    pub chipset: String,
    pub tool_name: String,
    pub result: String,
}

fn get_db_path() -> PathBuf {
    let mut path = dirs::home_dir().unwrap_or_else(|| PathBuf::from("."));
    path.push(".deepeye");
    if !path.exists() {
        fs::create_dir_all(&path).ok();
    }
    path.push("history.db");
    path
}

pub fn init_db() -> Result<()> {
    let conn = Connection::open(get_db_path())?;
    conn.execute(
        "CREATE TABLE IF NOT EXISTS history (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            timestamp TEXT NOT NULL,
            device_name TEXT NOT NULL,
            chipset TEXT NOT NULL,
            tool_name TEXT NOT NULL,
            result TEXT NOT NULL
        )",
        [],
    )?;
    Ok(())
}

#[tauri::command]
pub async fn add_history_entry(
    device_name: String,
    chipset: String,
    tool_name: String,
    result: String,
) -> Result<(), String> {
    let conn = Connection::open(get_db_path()).map_err(|e| e.to_string())?;
    let timestamp = Local::now().format("%Y-%m-%d %H:%M:%S").to_string();

    conn.execute(
        "INSERT INTO history (timestamp, device_name, chipset, tool_name, result)
         VALUES (?1, ?2, ?3, ?4, ?5)",
        params![timestamp, device_name, chipset, tool_name, result],
    )
    .map_err(|e| e.to_string())?;

    Ok(())
}

#[tauri::command]
pub async fn get_history() -> Result<Vec<HistoryEntry>, String> {
    let conn = Connection::open(get_db_path()).map_err(|e| e.to_string())?;
    let mut stmt = conn
        .prepare("SELECT id, timestamp, device_name, chipset, tool_name, result FROM history ORDER BY id DESC LIMIT 100")
        .map_err(|e| e.to_string())?;

    let history_iter = stmt
        .query_map([], |row| {
            Ok(HistoryEntry {
                id: row.get(0)?,
                timestamp: row.get(1)?,
                device_name: row.get(2)?,
                chipset: row.get(3)?,
                tool_name: row.get(4)?,
                result: row.get(5)?,
            })
        })
        .map_err(|e| e.to_string())?;

    let mut results = Vec::new();
    for entry in history_iter {
        results.push(entry.map_err(|e| e.to_string())?);
    }

    Ok(results)
}

#[tauri::command]
pub async fn clear_history() -> Result<(), String> {
    let conn = Connection::open(get_db_path()).map_err(|e| e.to_string())?;
    conn.execute("DELETE FROM history", [])
        .map_err(|e| e.to_string())?;
    Ok(())
}

#[tauri::command]
pub async fn export_history_csv(path: String) -> Result<String, String> {
    let history = get_history().await?;
    let mut wtr = csv::Writer::from_path(&path).map_err(|e| e.to_string())?;

    for entry in history {
        wtr.serialize(entry).map_err(|e| e.to_string())?;
    }

    wtr.flush().map_err(|e| e.to_string())?;
    Ok(format!("Successfully exported to {}", path))
}
