use super::storage::{clear_stored_license, load_stored_license, save_stored_license};
use super::types::{LicenseFeatureSet, LicenseStatus, LicenseType};
use super::validator::validate_license;
use chrono::Utc;
use std::sync::Mutex;
use tauri::AppHandle;

pub struct LicenseManager {
    pub status: Mutex<LicenseStatus>,
}

impl Default for LicenseManager {
    fn default() -> Self {
        Self::new()
    }
}

impl LicenseManager {
    pub fn new() -> Self {
        let default_free = LicenseStatus {
            license_type: LicenseType::Free,
            is_valid: true,
            expires_at: None,
            days_remaining: None,
            seat_id: None,
            activated_at: None,
            last_validated_at: Utc::now().to_rfc3339(),
            features: LicenseFeatureSet::for_type(&LicenseType::Free),
        };

        Self {
            status: Mutex::new(default_free),
        }
    }

    pub fn load_from_store(&self, app: &AppHandle) {
        if let Some(mut stored) = load_stored_license(app) {
            // Re-validate if needed, or just accept the stored status
            // Update last_validated_at, etc.
            stored.last_validated_at = Utc::now().to_rfc3339();

            let mut guard = self.status.lock().unwrap();
            *guard = stored;
        }
    }

    pub fn activate(&self, key: &str, app: &AppHandle) -> Result<LicenseStatus, String> {
        let status = validate_license(key)?;
        save_stored_license(app, &status)?;

        let mut guard = self.status.lock().unwrap();
        *guard = status.clone();

        Ok(status)
    }

    pub fn deactivate(&self, app: &AppHandle) -> Result<(), String> {
        clear_stored_license(app)?;

        let default_free = LicenseStatus {
            license_type: LicenseType::Free,
            is_valid: true,
            expires_at: None,
            days_remaining: None,
            seat_id: None,
            activated_at: None,
            last_validated_at: Utc::now().to_rfc3339(),
            features: LicenseFeatureSet::for_type(&LicenseType::Free),
        };

        let mut guard = self.status.lock().unwrap();
        *guard = default_free;

        Ok(())
    }

    pub fn get_status(&self) -> LicenseStatus {
        let guard = self.status.lock().unwrap();
        guard.clone()
    }
}
