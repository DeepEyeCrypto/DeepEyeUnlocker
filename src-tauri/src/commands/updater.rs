use serde::Serialize;
use tauri::AppHandle;
use tauri_plugin_updater::UpdaterExt;

#[derive(Debug, Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct UpdateInfo {
    pub version: String,
    pub body: String,
    pub date: String,
    pub download_url: String,
}

pub async fn check_for_update(app: AppHandle) -> Result<Option<UpdateInfo>, String> {
    let updater = app.updater().map_err(|e| format!("updater init error: {e}"))?;

    match updater.check().await {
        Ok(Some(update)) => Ok(Some(UpdateInfo {
            version: update.version.clone(),
            body: update.body.clone().unwrap_or_default(),
            date: update.date.map(|d| d.to_string()).unwrap_or_default(),
            download_url: update.download_url.to_string(),
        })),
        Ok(None) => Ok(None),
        Err(e) => Err(format!("update check failed: {e}")),
    }
}

pub async fn install_update(app: AppHandle) -> Result<(), String> {
    let updater = app.updater().map_err(|e| format!("updater init error: {e}"))?;

    let update = updater
        .check()
        .await
        .map_err(|e| format!("update check failed: {e}"))?
        .ok_or("no update available")?;

    update
        .download_and_install(|_, _| {}, || {})
        .await
        .map_err(|e| format!("install failed: {e}"))?;

    app.restart();
}

#[tauri::command]
pub async fn check_update(app: AppHandle) -> Result<Option<UpdateInfo>, String> {
    check_for_update(app).await
}

#[tauri::command]
pub async fn do_install_update(app: AppHandle) -> Result<(), String> {
    install_update(app).await
}
