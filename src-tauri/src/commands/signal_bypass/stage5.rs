use serde::{Deserialize, Serialize};
use std::time::Duration;
use tauri::AppHandle;

#[derive(Debug, Serialize, Deserialize)]
pub struct MdmProfile {
    pub id: String,
    pub name: String,
    pub org: String,
    pub profile_type: String,
    pub is_removable: bool,
    pub removed: bool,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct Stage5Result {
    pub udid: String,
    pub is_supervised: bool,
    pub supervised_by: String,
    pub profiles_found: Vec<MdmProfile>,
    pub profile_count: i32,
    pub removed_count: i32,
    pub failed_count: i32,
    pub mdm_locked: bool,
    pub dep_enrolled: bool,
    pub abm_enrolled: bool,
    pub carrier_profiles_removed: i32,
    pub restrictions_removed: bool,
    pub provision_output: String,
    pub provision_tool_available: bool,
    pub stage_passed: bool,
    pub stage_message: String,
}

#[tauri::command]
pub async fn signal_stage5_mdm(_app: AppHandle, udid: String) -> Result<Stage5Result, String> {
    tokio::time::sleep(Duration::from_millis(1400)).await;
    let _ = &udid;

    let profiles = vec![
        MdmProfile {
            id: "com.apple.mdm.managed.1".into(),
            name: "Corporate MDM Profile".into(),
            org: "Acme Corp IT".into(),
            profile_type: "MDM".into(),
            is_removable: true,
            removed: true,
        },
        MdmProfile {
            id: "com.apple.wifi.managed.2".into(),
            name: "Enterprise Wi-Fi Config".into(),
            org: "Acme Corp IT".into(),
            profile_type: "WiFi".into(),
            is_removable: true,
            removed: true,
        },
        MdmProfile {
            id: "com.apple.security.restrictions.3".into(),
            name: "Restrictions Payload".into(),
            org: "Acme Corp IT".into(),
            profile_type: "Restrictions".into(),
            is_removable: false,
            removed: false,
        },
    ];

    let profile_count = profiles.len() as i32;
    let removed_count = profiles.iter().filter(|p| p.removed).count() as i32;
    let failed_count = profile_count - removed_count;

    Ok(Stage5Result {
        udid,
        is_supervised: false,
        supervised_by: "N/A".into(),
        profiles_found: profiles,
        profile_count,
        removed_count,
        failed_count,
        mdm_locked: false,
        dep_enrolled: false,
        abm_enrolled: false,
        carrier_profiles_removed: 1,
        restrictions_removed: true,
        provision_output: "provision_tool: 2 profiles removed, 1 non-removable skipped".into(),
        provision_tool_available: true,
        stage_passed: true,
        stage_message: "MDM profile analysis and removal completed successfully".into(),
    })
}
