use super::types::LicenseStatus;
use serde_json::json;
use tauri::AppHandle;
use tauri_plugin_store::StoreExt;

const LICENSE_STORE_FILE: &str = "deepeye.license.bin"; // .bin indicates it's encrypted usually
const LICENSE_KEY: &str = "active_license_status";

// In a real app, you would pass password or use platform secure storage.
// tauri-plugin-store supports this via the save() / load() builder if configured.

pub fn load_stored_license(app: &AppHandle) -> Option<LicenseStatus> {
    if let Ok(store) = app.store(LICENSE_STORE_FILE) {
        if let Some(val) = store.get(LICENSE_KEY) {
            return serde_json::from_value(val).ok();
        }
    }
    None
}

pub fn save_stored_license(app: &AppHandle, status: &LicenseStatus) -> Result<(), String> {
    let store = app
        .store(LICENSE_STORE_FILE)
        .map_err(|e| format!("Failed to open license store: {}", e))?;

    store.set(LICENSE_KEY, json!(status));
    store
        .save()
        .map_err(|e| format!("Failed to save license store: {}", e))?;
    Ok(())
}

pub fn clear_stored_license(app: &AppHandle) -> Result<(), String> {
    let store = app
        .store(LICENSE_STORE_FILE)
        .map_err(|e| format!("Failed to open license store: {}", e))?;

    store.delete(LICENSE_KEY);
    store
        .save()
        .map_err(|e| format!("Failed to save license store: {}", e))?;
    Ok(())
}
