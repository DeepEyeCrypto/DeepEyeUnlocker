mod commands;
mod shsh;
mod diagnostics;
mod restore;
mod purple;
mod toolbox;
mod cve;
mod vault;
mod identity;
mod nonce;
mod afc;
mod backup;
mod frida;
mod sideloader;
mod ssh_tunnel;
mod crash_logs;
mod ipsw_dl;
mod developer;

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
use commands::checkm8::run_checkm8;
use commands::ios_bypass::ios_bypass_full;
use commands::apple::{
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
};
use commands::exploit::{run_palera1n, verify_pwned_dfu, bypass_icloud_activation};
use commands::f3arrain::{f3arrain_send_iboot, f3arrain_run_bypass};
use commands::hydra::{hydra_detect_protocol, hydra_run_mtk_meta, hydra_samsung_frp_bypass};
use commands::mtk::{
    mtk_run_command,
    mtk_read_partition,
    mtk_write_partition,
    mtk_erase_partition,
    mtk_device_info,
    mtk_unlock_bootloader,
};
use commands::bruteforce::run_pin_bruteforce;
use commands::updater::{check_for_update, install_update};
use commands::edl::{edl_detect_device, edl_read_partition, edl_write_partition, edl_erase_partition, edl_reboot, edl_get_gpt};
use commands::rom_flasher::{rom_sideload_zip, rom_flash_partition, rom_wipe_data, rom_reboot_recovery, rom_reboot_bootloader};
use commands::device_history::{history_add_entry, history_get_entries, history_clear, history_delete_entry, history_export_json};

// Server bypass URL (configure per deployment)
pub const BYPASS_SERVER_URL: &str = match option_env!("BYPASS_SERVER_URL") {
    Some(v) => v,
    None => "https://api.deepeye.bypass/v2",
};

use shsh::{
    get_ecid, get_board_config, save_shsh_all_signed, save_shsh_specific,
    save_shsh_with_generator, list_saved_shsh, check_signed_versions,
    futurerestore, futurerestore_no_baseband
};

use diagnostics::{
    run_diagnostics, get_battery_stats, get_thermal_state,
    device_shutdown, device_restart, device_sleep
};

use restore::{
    restore_local_ipsw, restore_latest, exit_recovery, get_recovery_info
};

use purple::{
    enter_purple_mode, purple_read_sn, purple_write_sn, purple_read_all
};

use toolbox::{
    toolbox_block_ota, toolbox_factory_reset, toolbox_get_logs, toolbox_backup_device
};

use cve::{
    query_cve_database, run_intelligence_scan
};

use vault::{
    push_to_cloud_vault, pull_from_cloud_vault, list_cloud_vault
};

use identity::{
    check_imei_intel, get_full_identity
};

use nonce::{
    get_current_nonce, set_nonce_generator, set_nonce_from_blob,
    get_generator_from_blob, clear_nonce, set_nonce_checkra1n
};

use afc::{
    mount_afc2, list_directory, get_file_info, read_file,
    write_file, delete_path, make_directory, pull_file, push_file
};

use backup::{
    create_backup, backup_encrypted, restore_backup, list_backups,
    delete_backup, change_backup_password, extract_app_data, restore_app_data
};

use frida::{
    frida_ps, frida_attach, frida_spawn, frida_run_script,
    frida_kill_process, frida_list_exports, inject_dylib,
    dump_app_memory, ssl_kill_switch, frida_inject
};

use sideloader::{
    install_ipa, sign_and_install, list_installed_apps,
    uninstall_app, get_app_info, reinstall_app
};

use ssh_tunnel::{
    start_ssh_tunnel, stop_ssh_tunnel, check_tunnel_status,
    run_ssh_command, run_su_command, ssh_upload_file,
    ssh_download_file, install_sileo_pkg
};

use crash_logs::{
    pull_crash_logs, list_crash_logs, read_crash_log,
    clear_crash_logs, symbolicate_log
};

use ipsw_dl::{
    get_signed_firmwares, get_all_firmwares, download_ipsw,
    get_download_progress, verify_ipsw_sha1
};

use developer::{
    mount_dev_disk_image, unmount_dev_disk_image, check_dev_disk_mounted,
    list_processes, get_screenshot
};

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_fs::init())
        .plugin(tauri_plugin_sql::Builder::default().build())
        .plugin(tauri_plugin_updater::Builder::new().build())
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
            check_for_update,
            install_update,
            // Stage 26 — EDL mode
            edl_detect_device,
            edl_read_partition,
            edl_write_partition,
            edl_erase_partition,
            edl_reboot,
            edl_get_gpt,
            // Stage 27 — Custom ROM flasher
            rom_sideload_zip,
            rom_flash_partition,
            rom_wipe_data,
            rom_reboot_recovery,
            rom_reboot_bootloader,
            // Stage 28 — Device history
            history_add_entry,
            history_get_entries,
            history_clear,
            history_delete_entry,
            history_export_json
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
