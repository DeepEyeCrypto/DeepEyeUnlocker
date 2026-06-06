use crate::config::settings::AppSettings;
use serde_json::json;
use tauri::AppHandle;
use tauri_plugin_store::StoreExt;

const SETTINGS_STORE_FILE: &str = "deepeye.settings.json";
const SETTINGS_KEY: &str = "app_settings";

#[tauri::command]
pub async fn get_settings(app: AppHandle) -> Result<AppSettings, String> {
    if let Ok(store) = app.store(SETTINGS_STORE_FILE) {
        if let Some(val) = store.get(SETTINGS_KEY) {
            if let Ok(settings) = serde_json::from_value(val) {
                return Ok(settings);
            }
        }
    }
    Ok(AppSettings::default())
}

#[tauri::command]
pub async fn save_settings(settings: AppSettings, app: AppHandle) -> Result<(), String> {
    let store = app
        .store(SETTINGS_STORE_FILE)
        .map_err(|e| format!("Failed to open settings store: {}", e))?;

    store.set(SETTINGS_KEY, json!(settings));
    store
        .save()
        .map_err(|e| format!("Failed to save settings: {}", e))?;
    Ok(())
}

#[tauri::command]
pub async fn reset_settings(app: AppHandle) -> Result<AppSettings, String> {
    let defaults = AppSettings::default();
    let store = app
        .store(SETTINGS_STORE_FILE)
        .map_err(|e| format!("Failed to open settings store: {}", e))?;

    store.set(SETTINGS_KEY, json!(defaults));
    store
        .save()
        .map_err(|e| format!("Failed to save settings: {}", e))?;
    Ok(defaults)
}
