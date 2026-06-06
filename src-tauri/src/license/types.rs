use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "lowercase")]
pub enum LicenseType {
    Free,
    Trial,
    Pro,
    Expired,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LicenseStatus {
    pub license_type: LicenseType,
    pub is_valid: bool,
    pub expires_at: Option<String>,
    pub days_remaining: Option<i64>,
    pub seat_id: Option<String>,
    pub activated_at: Option<String>,
    pub last_validated_at: String,
    pub features: LicenseFeatureSet,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LicenseFeatureSet {
    pub max_devices_per_session: u32,
    pub can_use_jailbreak_tools: bool,
    pub can_use_boot_files: bool,
    pub can_use_fmi_off: bool,
    pub can_export_logs: bool,
    pub can_use_edl_pipeline: bool,
    pub can_use_mtk_brom: bool,
}
