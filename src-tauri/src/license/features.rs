use super::types::{LicenseFeatureSet, LicenseType};

impl LicenseFeatureSet {
    pub fn for_type(license_type: &LicenseType) -> Self {
        match license_type {
            LicenseType::Pro => Self {
                max_devices_per_session: 100, // unlimited effectively
                can_use_jailbreak_tools: true,
                can_use_boot_files: true,
                can_use_fmi_off: true,
                can_export_logs: true,
                can_use_edl_pipeline: true,
                can_use_mtk_brom: true,
            },
            LicenseType::Trial => Self {
                max_devices_per_session: 2,
                can_use_jailbreak_tools: true,
                can_use_boot_files: true,
                can_use_fmi_off: false,
                can_export_logs: true,
                can_use_edl_pipeline: true,
                can_use_mtk_brom: true,
            },
            LicenseType::Free | LicenseType::Expired => Self {
                max_devices_per_session: 1,
                can_use_jailbreak_tools: false,
                can_use_boot_files: false,
                can_use_fmi_off: false,
                can_export_logs: false,
                can_use_edl_pipeline: false,
                can_use_mtk_brom: false,
            },
        }
    }
}
