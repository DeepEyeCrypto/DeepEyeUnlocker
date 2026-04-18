use serde::{Deserialize, Serialize};
use std::fs;
use std::path::PathBuf;

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct AppSettings {
    pub adb_path: String,
    pub fastboot_path: String,
    pub custom_tool_paths: Vec<String>,
    pub theme: String,
    pub log_level: String,
}

impl Default for AppSettings {
    fn default() -> Self {
        Self {
            adb_path: "adb".to_string(),
            fastboot_path: "fastboot".to_string(),
            custom_tool_paths: Vec::new(),
            theme: "dark".to_string(),
            log_level: "info".to_string(),
        }
    }
}

fn get_settings_path() -> PathBuf {
    let mut path = dirs::home_dir().unwrap_or_else(|| PathBuf::from("."));
    path.push(".deepeye");
    if !path.exists() {
        fs::create_dir_all(&path).ok();
    }
    path.push("settings.json");
    path
}

#[tauri::command]
pub fn load_settings() -> AppSettings {
    let path = get_settings_path();
    if path.exists() {
        if let Ok(content) = fs::read_to_string(path) {
            if let Ok(settings) = serde_json::from_str(&content) {
                return settings;
            }
        }
    }
    AppSettings::default()
}

#[tauri::command]
pub fn save_settings(settings: AppSettings) -> Result<(), String> {
    let path = get_settings_path();
    let content = serde_json::to_string_pretty(&settings).map_err(|e| e.to_string())?;
    fs::write(path, content).map_err(|e| e.to_string())?;
    Ok(())
}
