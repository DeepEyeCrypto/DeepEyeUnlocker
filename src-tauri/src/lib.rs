mod commands;

use commands::ios_backup::{ios_backup_info, ios_extract_hash, ios_extract_screentime, ios_run_crack};
use commands::adb::stream_adb_logs;
use commands::dfu_restore::{ios_detect_dfu_state, ios_enter_dfu, ios_restore_device, ios_download_ipsw};
use commands::activation::{ios_check_activation_state, ios_run_checkra1n, ios_patch_activation_record};
use commands::apple_id::{ios_apple_id_state, ios_remove_apple_id, ios_fmi_state};
use commands::screentime::{ios_extract_screentime_hash, ios_run_screentime_crack};
use commands::mdm::{ios_mdm_state, ios_list_profiles, ios_remove_mdm};
use commands::bypass::{ios_check_hello_state, ios_run_hello_bypass};
use commands::vault::{ios_create_deepvault};
use commands::ramdisk::{ios_check_pwn_state, ios_run_gaster_pwn, ios_boot_ramdisk};
use commands::bypass_advanced::{ios_activation_type_check, ios_temp_activation, ios_untethered_bypass, ios_activation_persistence_check};
use commands::identity::{ios_device_identity, ios_imei_state};
use commands::ticket::{ios_parse_activation_record, ios_activation_record_state, ios_scan_tickets};
use commands::orchestrator::{ios_poll_orchestrator, ios_inject_surgical_patch};
use commands::extraction::{ios_mount_ramdisk, ios_mass_extract};

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_sql::Builder::default().build())
        .invoke_handler(tauri::generate_handler![
            ios_backup_info,
            ios_extract_hash,
            ios_extract_screentime,
            ios_run_crack,
            stream_adb_logs,
            ios_detect_dfu_state,
            ios_enter_dfu,
            ios_restore_device,
            ios_download_ipsw,
            ios_check_activation_state,
            ios_run_checkra1n,
            ios_patch_activation_record,
            ios_apple_id_state,
            ios_remove_apple_id,
            ios_fmi_state,
            ios_extract_screentime_hash,
            ios_run_screentime_crack,
            ios_mdm_state,
            ios_list_profiles,
            ios_remove_mdm,
            ios_check_hello_state,
            ios_run_hello_bypass,
            ios_create_deepvault,
            ios_check_pwn_state,
            ios_run_gaster_pwn,
            ios_boot_ramdisk,
            ios_activation_type_check,
            ios_temp_activation,
            ios_untethered_bypass,
            ios_activation_persistence_check,
            ios_device_identity,
            ios_imei_state,
            ios_parse_activation_record,
            ios_activation_record_state,
            ios_scan_tickets,
            ios_poll_orchestrator,
            ios_inject_surgical_patch,
            ios_mount_ramdisk,
            ios_mass_extract
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
