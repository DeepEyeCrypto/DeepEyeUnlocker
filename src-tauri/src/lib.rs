mod afc;
mod backup;
mod commands;
mod crash_logs;
mod cve;
mod developer;
mod device;
mod diagnostics;
mod error;
mod frida;
mod identity;
mod ipsw_dl;
mod nonce;
mod purple;
mod restore;
mod shsh;
mod sideloader;
mod ssh_tunnel;
mod toolbox;
mod usb;
mod db;
mod config;
mod qualcomm;
mod unisoc;
mod vault;

use commands::activation::{
    ios_check_activation_state, ios_patch_activation_record, ios_run_checkra1n,
};
use commands::adb::{
    adb_check_root_access, adb_erase_frp_partition, adb_get_full_info, adb_install_apk,
    adb_list_devices, adb_pull_file, adb_push_file, adb_reboot_device, adb_shell_command,
    adb_sideload_zip, adb_test_binary, stream_adb_logs,
};
use commands::apple::{
    apple_check_activation, apple_device_info, apple_dns_activation, apple_enter_dfu,
    apple_exit_recovery, apple_icloud_bypass, apple_irecovery_cmd, apple_mdm_bypass,
    apple_restore_activation_record, apple_restore_ipsw,
};
use commands::apple_id::{ios_apple_id_state, ios_fmi_state, ios_remove_apple_id};
use commands::bruteforce::run_pin_bruteforce;
use commands::bypass::{ios_check_hello_state, ios_run_hello_bypass, run_bypass, run_otg_bypass};
use commands::bypass_advanced::{
    ios_activation_persistence_check, ios_activation_type_check, ios_temp_activation,
    ios_untethered_bypass,
};
use commands::checkm8::run_checkm8;
use commands::cloud_sync::cloud_sync_db;
use commands::connected_devices::get_connected_devices;
use commands::connected_devices::get_supported_brands;
use commands::device::{
    device_auto_connect, device_check_mode, device_get_protocol_name, device_scan_all,
    fastboot_detect, fastboot_flash_partition, fastboot_get_info, fastboot_lock_bootloader,
    fastboot_reboot, fastboot_reboot_bootloader, fastboot_reboot_recovery,
};
use commands::device_db::{
    db_auto_route, db_list_all, db_lookup_model, db_lookup_vid_pid, db_search_devices,
    frp_execute_protocol,
};
use db::history::{add_history_entry, get_history, clear_history, export_history_csv};
use config::settings::{load_settings, save_settings};
use commands::dfu_restore::{
    ios_detect_dfu_state, ios_download_ipsw, ios_enter_dfu, ios_restore_device,
};
use commands::diagnostics::diag_test_handshake;
use commands::edl::{
    edl_configure, edl_erase_partition, edl_find_device, edl_get_storage_info, edl_read_partition,
    edl_reboot, edl_sahara_handshake, edl_upload_programmer, edl_write_partition,
};
use commands::exploit::{bypass_icloud_activation, run_palera1n, verify_pwned_dfu};
use commands::extraction::{ios_mass_extract, ios_mount_ramdisk};
use commands::f3arrain::{f3arrain_run_bypass, f3arrain_send_iboot};
use commands::hydra::{hydra_detect_protocol, hydra_run_mtk_meta, hydra_samsung_frp_bypass};
use commands::identity::{ios_device_identity, ios_imei_state};
use commands::ios_backup::{
    ios_backup_info, ios_extract_hash, ios_extract_screentime, ios_run_crack,
};
use commands::ios_bypass::ios_bypass_full;
use commands::logcat::{
    adb_logcat_clear, adb_logcat_dump, adb_logcat_export, adb_logcat_start, adb_logcat_stop,
    clear_logcat_buffer, export_logcat_to_file, start_logcat_stream, stop_logcat_stream,
};
use commands::mdm::{ios_list_profiles, ios_mdm_state, ios_remove_mdm};
use commands::mtk::{
    mtk_device_info, mtk_erase_partition, mtk_read_partition, mtk_run_command,
    mtk_unlock_bootloader, mtk_write_partition,
};
use commands::mtk_brom::{
    mtk_bypass_sla, mtk_da_erase_partition, mtk_da_read_partition, mtk_da_write_partition,
    mtk_detect_auth_type, mtk_detect_device, mtk_dump_preloader, mtk_erase_frp,
    mtk_format_userdata, mtk_handshake_and_identify, mtk_jump_to_da, mtk_list_partitions,
    mtk_read_imei, mtk_reboot, mtk_upload_da, mtk_write_imei,
};
use commands::orchestrator::{ios_inject_surgical_patch, ios_poll_orchestrator};
use commands::ramdisk::{ios_boot_ramdisk, ios_check_pwn_state, ios_run_gaster_pwn};
use commands::reporter::reporter_generate_audit;
use commands::rom_flasher::{
    fastboot_erase_partition, fastboot_get_all_variables, fastboot_list_devices,
    fastboot_reboot_target, fastboot_unlock_bootloader, rom_flash_partition, rom_reboot_bootloader,
    rom_reboot_recovery, rom_sideload_zip, rom_wipe_data,
};
use commands::rom_manager::{
    rom_add_to_queue, rom_build_flash_plan, rom_clear_queue, rom_detect_type, rom_get_queue,
    rom_inspect_zip, rom_move_queue_item, rom_remove_from_queue, rom_select_file,
    rom_toggle_queue_partition, rom_validate_package,
};
use commands::samsung::{
    samsung_do_erase_frp_cmd, samsung_do_handshake_cmd, samsung_find_device_cmd,
    samsung_flash_part_cmd, samsung_get_pit_cmd, samsung_reboot_device_cmd,
};
use commands::wifi_adb::{
    pair_wifi_adb, connect_wifi_adb, disconnect_wifi_adb, enable_adb_wifi_mode
};
use unisoc::edl::run_unisoc_frp_bypass;
use qualcomm::programmer_db::{get_edl_programmers, load_edl_programmer};
use commands::rebuild::check_for_updates;
use commands::screentime::{ios_extract_screentime_hash, ios_run_screentime_crack};
use commands::ticket::{
    ios_activation_record_state, ios_parse_activation_record, ios_scan_tickets,
};
use commands::unisoc::unisoc_detect_device;
use commands::updater::{check_update, do_install_update};
use commands::usb_detector::start_usb_watcher;
use commands::usb_utils::usb_debug_list_devices;
use commands::vault::ios_create_deepvault;
use commands::signal_bypass::stage1::signal_stage1_detect;
use commands::signal_bypass::stage2::signal_stage2_activation;
use commands::signal_bypass::stage3::signal_stage3_baseband;
use commands::signal_bypass::stage4::signal_stage4_icloud;
use commands::signal_bypass::stage5::signal_stage5_mdm;
use commands::signal_bypass::stage6::signal_stage6_carrier;
use commands::signal_bypass::stage7::signal_stage7_imei;
use commands::signal_bypass::stage8::signal_stage8_baseband;
use commands::signal_bypass::stage9::signal_stage9_verify;
use commands::rebuild::{
    get_connected_device, run_mtk_brom_bypass, run_da_bypass, run_meta_bypass,
    run_frp_erase, run_adb_frp, run_deepeye_agent, run_pattern_bypass,
    run_screen_bypass, run_qcom_edl, run_qcom_frp_erase, run_sahara_handshake,
    run_full_bypass,
    
    // Apple Tools
    get_ios_device_info, check_activation_status, run_activation_bypass, 
    run_mdm_bypass, run_force_dfu, run_passcode_remove, run_shsh_save, run_ipsw_flash,
    
    // Samsung Tools
    run_samsung_frp, run_samsung_odin_info,
    
    // Database
    search_testpoints, get_all_testpoints,
    
    // Edge Cases & Diagnostics
    check_usb_permissions, restart_adb_server, check_samsung_download_mode, 
    run_tool_version_check,
};

// Server bypass URL (configure per deployment)
pub const BYPASS_SERVER_URL: &str = match option_env!("BYPASS_SERVER_URL") {
    Some(v) => v,
    None => "https://api.deepeye.bypass/v2",
};

use shsh::{
    check_signed_versions, futurerestore, futurerestore_no_baseband, get_board_config, get_ecid,
    list_saved_shsh, save_shsh_all_signed, save_shsh_specific, save_shsh_with_generator,
};

use diagnostics::{
    device_restart, device_shutdown, device_sleep, get_battery_stats, get_thermal_state,
    run_diagnostics,
};

use restore::{exit_recovery, get_recovery_info, restore_latest, restore_local_ipsw};

use purple::{enter_purple_mode, purple_read_all, purple_read_sn, purple_write_sn};

use toolbox::{toolbox_backup_device, toolbox_block_ota, toolbox_factory_reset, toolbox_get_logs};

use cve::{query_cve_database, run_intelligence_scan};

use vault::{list_cloud_vault, pull_from_cloud_vault, push_to_cloud_vault};

use identity::{check_imei_intel, get_full_identity};

use nonce::{
    clear_nonce, get_current_nonce, get_generator_from_blob, set_nonce_checkra1n,
    set_nonce_from_blob, set_nonce_generator,
};

use afc::{
    delete_path, get_file_info, list_directory, make_directory, mount_afc2, pull_file, push_file,
    read_file, write_file,
};

use backup::{
    backup_encrypted, change_backup_password, create_backup, delete_backup, extract_app_data,
    list_backups, restore_app_data, restore_backup,
};

use frida::{
    dump_app_memory, frida_attach, frida_inject, frida_kill_process, frida_list_exports, frida_ps,
    frida_run_script, frida_spawn, inject_dylib, ssl_kill_switch,
};

use sideloader::{
    get_app_info, install_ipa, list_installed_apps, reinstall_app, sign_and_install, uninstall_app,
};

use ssh_tunnel::{
    check_tunnel_status, install_sileo_pkg, run_ssh_command, run_su_command, ssh_download_file,
    ssh_upload_file, start_ssh_tunnel, stop_ssh_tunnel,
};

use crash_logs::{
    clear_crash_logs, list_crash_logs, pull_crash_logs, read_crash_log, symbolicate_log,
};

use ipsw_dl::{
    download_ipsw, get_all_firmwares, get_download_progress, get_signed_firmwares, verify_ipsw_sha1,
};

use developer::{
    check_dev_disk_mounted, get_screenshot, list_processes, mount_dev_disk_image,
    unmount_dev_disk_image,
};

// ── ADB and Samsung Commands are now imported from their modules directly ──

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    // v1.1.0 DB Initialization
    let _ = db::history::init_db();

    // [CONFIRMED] Release builds use panic=abort, so startup errors must be logged instead of panicking.
    let app_result = tauri::Builder::default()
        .setup(|app| {
            start_usb_watcher(app.handle().clone());
            Ok(())
        })
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_fs::init())
        .plugin(tauri_plugin_os::init())
        .plugin(tauri_plugin_sql::Builder::default().build())
        .plugin(tauri_plugin_updater::Builder::new().build())
        .invoke_handler(tauri::generate_handler![
            ios_backup_info,
            ios_extract_hash,
            ios_extract_screentime,
            ios_run_crack,
            stream_adb_logs,
            adb_list_devices,
            adb_get_full_info,
            adb_shell_command,
            adb_reboot_device,
            adb_install_apk,
            adb_push_file,
            adb_pull_file,
            adb_sideload_zip,
            adb_erase_frp_partition,
            adb_check_root_access,
            adb_test_binary,
            samsung_find_device_cmd,
            samsung_do_handshake_cmd,
            samsung_get_pit_cmd,
            samsung_flash_part_cmd,
            samsung_do_erase_frp_cmd,
            samsung_reboot_device_cmd,
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
            run_bypass,
            run_otg_bypass,
            ios_create_deepvault,
            ios_check_pwn_state,
            ios_run_gaster_pwn,
            ios_boot_ramdisk,
            ios_activation_type_check,
            ios_temp_activation,
            ios_untethered_bypass,
            ios_activation_persistence_check,
            get_connected_devices,
            get_supported_brands,
            ios_device_identity,
            ios_imei_state,
            ios_parse_activation_record,
            ios_activation_record_state,
            ios_scan_tickets,
            ios_poll_orchestrator,
            ios_inject_surgical_patch,
            ios_mount_ramdisk,
            ios_mass_extract,
            apple_device_info,
            apple_irecovery_cmd,
            apple_exit_recovery,
            apple_enter_dfu,
            apple_icloud_bypass,
            apple_restore_ipsw,
            apple_check_activation,
            apple_dns_activation,
            apple_mdm_bypass,
            apple_restore_activation_record,
            run_palera1n,
            verify_pwned_dfu,
            bypass_icloud_activation,
            f3arrain_send_iboot,
            f3arrain_run_bypass,
            hydra_detect_protocol,
            hydra_run_mtk_meta,
            hydra_samsung_frp_bypass,
            mtk_run_command,
            mtk_read_partition,
            mtk_write_partition,
            mtk_erase_partition,
            mtk_device_info,
            mtk_unlock_bootloader,
            mtk_detect_device,
            mtk_handshake_and_identify,
            mtk_detect_auth_type,
            mtk_bypass_sla,
            mtk_upload_da,
            mtk_jump_to_da,
            mtk_erase_frp,
            mtk_format_userdata,
            mtk_read_imei,
            mtk_write_imei,
            mtk_reboot,
            mtk_list_partitions,
            mtk_da_read_partition,
            mtk_da_write_partition,
            mtk_dump_preloader,
            mtk_da_erase_partition,
            usb_debug_list_devices,
            get_ecid,
            get_board_config,
            save_shsh_all_signed,
            save_shsh_specific,
            save_shsh_with_generator,
            list_saved_shsh,
            check_signed_versions,
            futurerestore,
            futurerestore_no_baseband,
            run_diagnostics,
            get_battery_stats,
            get_thermal_state,
            device_shutdown,
            device_restart,
            device_sleep,
            restore_local_ipsw,
            restore_latest,
            exit_recovery,
            get_recovery_info,
            enter_purple_mode,
            purple_read_sn,
            purple_write_sn,
            purple_read_all,
            toolbox_block_ota,
            toolbox_factory_reset,
            toolbox_get_logs,
            toolbox_backup_device,
            query_cve_database,
            run_intelligence_scan,
            push_to_cloud_vault,
            pull_from_cloud_vault,
            list_cloud_vault,
            check_imei_intel,
            get_full_identity,
            get_current_nonce,
            set_nonce_generator,
            set_nonce_from_blob,
            get_generator_from_blob,
            clear_nonce,
            set_nonce_checkra1n,
            mount_afc2,
            list_directory,
            get_file_info,
            read_file,
            write_file,
            delete_path,
            make_directory,
            pull_file,
            push_file,
            create_backup,
            backup_encrypted,
            restore_backup,
            list_backups,
            delete_backup,
            change_backup_password,
            extract_app_data,
            restore_app_data,
            frida_ps,
            frida_attach,
            frida_spawn,
            frida_run_script,
            frida_kill_process,
            frida_list_exports,
            inject_dylib,
            dump_app_memory,
            ssl_kill_switch,
            frida_inject,
            install_ipa,
            sign_and_install,
            list_installed_apps,
            uninstall_app,
            get_app_info,
            reinstall_app,
            start_ssh_tunnel,
            stop_ssh_tunnel,
            check_tunnel_status,
            run_ssh_command,
            run_su_command,
            ssh_upload_file,
            ssh_download_file,
            install_sileo_pkg,
            pull_crash_logs,
            list_crash_logs,
            read_crash_log,
            clear_crash_logs,
            symbolicate_log,
            get_signed_firmwares,
            get_all_firmwares,
            download_ipsw,
            get_download_progress,
            verify_ipsw_sha1,
            mount_dev_disk_image,
            unmount_dev_disk_image,
            check_dev_disk_mounted,
            list_processes,
            get_screenshot,
            run_checkm8,
            ios_bypass_full,
            run_pin_bruteforce,
            // Stage 25 — Auto-updater
            check_update,
            do_install_update,
            // Stage 26 — EDL mode
            edl_find_device,
            edl_sahara_handshake,
            edl_upload_programmer,
            edl_configure,
            edl_read_partition,
            edl_write_partition,
            edl_erase_partition,
            edl_get_storage_info,
            edl_reboot,
            unisoc_detect_device,
            // Stage 27 — Custom ROM flasher
            rom_sideload_zip,
            rom_flash_partition,
            fastboot_erase_partition,
            rom_wipe_data,
            rom_reboot_recovery,
            rom_reboot_bootloader,
            fastboot_list_devices,
            fastboot_get_all_variables,
            fastboot_unlock_bootloader,
            fastboot_reboot_target,
            // Stage 28 — v1.1.0 History & Config
            add_history_entry,
            get_history,
            clear_history,
            export_history_csv,
            load_settings,
            save_settings,
            // Stage 12 — Device Database
            db_search_devices,
            db_lookup_model,
            db_auto_route,
            db_lookup_vid_pid,
            db_list_all,
            frp_execute_protocol,
            // Stage 15 — Global Upgrade
            diag_test_handshake,
            start_logcat_stream,
            stop_logcat_stream,
            clear_logcat_buffer,
            export_logcat_to_file,
            adb_logcat_start,
            adb_logcat_stop,
            adb_logcat_clear,
            adb_logcat_dump,
            adb_logcat_export,
            reporter_generate_audit,
            rom_select_file,
            rom_detect_type,
            rom_inspect_zip,
            rom_build_flash_plan,
            rom_validate_package,
            rom_get_queue,
            rom_add_to_queue,
            rom_remove_from_queue,
            rom_clear_queue,
            rom_move_queue_item,
            rom_toggle_queue_partition,
            cloud_sync_db,
            // Device Protocol Integration
            device_scan_all,
            device_auto_connect,
            device_check_mode,
            device_get_protocol_name,
            fastboot_detect,
            fastboot_get_info,
            fastboot_flash_partition,
            fastboot_erase_partition,
            fastboot_reboot,
            fastboot_reboot_bootloader,
            fastboot_reboot_recovery,
            fastboot_unlock_bootloader,
            fastboot_lock_bootloader,
            get_connected_device,
            run_mtk_brom_bypass,
            run_da_bypass,
            run_meta_bypass,
            run_frp_erase,
            run_adb_frp,
            run_deepeye_agent,
            run_pattern_bypass,
            run_screen_bypass,
            run_qcom_edl,
            run_qcom_frp_erase,
            run_sahara_handshake,
            
            // Apple Tools
            get_ios_device_info,
            check_activation_status,
            run_activation_bypass,
            run_mdm_bypass,
            run_force_dfu,
            run_passcode_remove,
            run_shsh_save,
            run_ipsw_flash,
            
            // Samsung Tools
            run_samsung_frp,
            run_samsung_odin_info,
            
            // Database
            search_testpoints,
            get_all_testpoints,
            run_full_bypass,
            
            // Edge Cases & Diagnostics
            check_usb_permissions,
            restart_adb_server,
            check_samsung_download_mode,
            run_tool_version_check,
            
            // v1.2.0 New Commands
            pair_wifi_adb,
            connect_wifi_adb,
            disconnect_wifi_adb,
            enable_adb_wifi_mode,
            run_unisoc_frp_bypass,
            unisoc_detect_device,
            check_for_updates,
            get_edl_programmers,
            load_edl_programmer,
            // Signal Bypass Pipeline
            signal_stage1_detect,
            signal_stage2_activation,
            signal_stage3_baseband,
            signal_stage4_icloud,
            signal_stage5_mdm,
            signal_stage6_carrier,
            signal_stage7_imei,
            signal_stage8_baseband,
            signal_stage9_verify,
        ])
        .run(tauri::generate_context!());

    if let Err(error) = app_result {
        eprintln!("[startup] tauri application failed to initialize: {error}");
    }
}
