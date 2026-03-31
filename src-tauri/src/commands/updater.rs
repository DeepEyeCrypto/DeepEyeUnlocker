use serde::Serialize;
use tauri::AppHandle;
use tauri_plugin_updater::UpdaterExt;

#[derive(Serialize)]
pub struct UpdateInfo {
    pub available: bool,
    pub version: String,
    pub body: String,
    pub date: String,
}

#[tauri::command]
pub async fn check_for_update(app: AppHandle) -> Result<UpdateInfo, String> {
    let updater = app.updater().map_err(|e| format!("updater init error: {e}"))?;

    match updater.check().await {
        Ok(Some(update)) => Ok(UpdateInfo {
            available: true,
            version: update.version.clone(),
            body: update.body.clone().unwrap_or_default(),
            date: update.date.map(|d| d.to_string()).unwrap_or_default(),
        }),
        Ok(None) => Ok(UpdateInfo {
            available: false,
            version: String::new(),
            body: String::new(),
            date: String::new(),
        }),
        Err(e) => Err(format!("update check failed: {e}")),
    }
}

#[tauri::command]
pub async fn install_update(app: AppHandle) -> Result<String, String> {
    let updater = app.updater().map_err(|e| format!("updater init error: {e}"))?;

    let update = updater
        .check()
        .await
        .map_err(|e| format!("update check failed: {e}"))?
        .ok_or("no update available")?;

    let version = update.version.clone();

    update
        .download_and_install(|_, _| {}, || {})
        .await
        .map_err(|e| format!("install failed: {e}"))?;

    Ok(format!("Update v{version} installed — restart to apply"))
}
