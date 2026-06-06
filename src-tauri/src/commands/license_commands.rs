use crate::license::manager::LicenseManager;
use crate::license::types::LicenseStatus;
use tauri::{AppHandle, State};

#[tauri::command]
pub async fn activate_license(
    key: String,
    license_mgr: State<'_, LicenseManager>,
    app: AppHandle,
) -> Result<LicenseStatus, String> {
    license_mgr.activate(&key, &app)
}

#[tauri::command]
pub fn get_license_status(license_mgr: State<'_, LicenseManager>) -> LicenseStatus {
    license_mgr.get_status()
}

#[tauri::command]
pub async fn deactivate_license(
    license_mgr: State<'_, LicenseManager>,
    app: AppHandle,
) -> Result<(), String> {
    license_mgr.deactivate(&app)
}

#[tauri::command]
pub fn check_license_feature(feature: String, license_mgr: State<'_, LicenseManager>) -> bool {
    let status = license_mgr.get_status();
    let features = status.features;
    match feature.as_str() {
        "can_use_jailbreak_tools" => features.can_use_jailbreak_tools,
        "can_use_boot_files" => features.can_use_boot_files,
        "can_use_fmi_off" => features.can_use_fmi_off,
        "can_export_logs" => features.can_export_logs,
        "can_use_edl_pipeline" => features.can_use_edl_pipeline,
        "can_use_mtk_brom" => features.can_use_mtk_brom,
        _ => false,
    }
}
