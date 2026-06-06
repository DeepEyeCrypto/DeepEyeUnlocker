#[macro_export]
macro_rules! register_handlers {
    () => {{
        use $crate::commands::activation::{
            ios_check_activation_state, ios_patch_activation_record, ios_run_checkra1n,
        };
        use $crate::commands::adb::{
            adb_check_root_access, adb_erase_frp_partition, adb_get_full_info, adb_install_apk,
            adb_list_devices, adb_pull_file, adb_push_file, adb_reboot_device, adb_shell_command,
            adb_sideload_zip, adb_test_binary, stream_adb_logs,
        };
        use $crate::commands::apple::{
            apple_check_activation, apple_device_info, apple_dns_activation, apple_enter_dfu,
            apple_exit_recovery, apple_icloud_bypass, apple_irecovery_cmd, apple_mdm_bypass,
            apple_restore_activation_record, apple_restore_ipsw,
        };
        use $crate::commands::apple_id::{ios_apple_id_state, ios_fmi_state, ios_remove_apple_id};
        use $crate::commands::bruteforce::run_pin_bruteforce;
        use $crate::commands::bypass::{
            ios_check_hello_state, ios_run_hello_bypass, run_bypass, run_otg_bypass,
        };
        use $crate::commands::bypass_advanced::{
            ios_activation_persistence_check, ios_activation_type_check, ios_temp_activation,
            ios_untethered_bypass,
        };
        use $crate::commands::checkm8::run_checkm8;
        use $crate::commands::cloud_sync::cloud_sync_db;
        use $crate::commands::connected_devices::get_connected_devices;
        use $crate::commands::connected_devices::get_supported_brands;
        use $crate::commands::device::{
            device_auto_connect, device_check_mode, device_get_protocol_name, device_scan_all,
            fastboot_detect, fastboot_flash_partition, fastboot_get_info, fastboot_lock_bootloader,
            fastboot_reboot, fastboot_reboot_bootloader, fastboot_reboot_recovery,
        };
        use $crate::commands::device_db::{
            db_auto_route, db_list_all, db_lookup_model, db_lookup_vid_pid, db_search_devices,
            frp_execute_protocol,
        };
        use $crate::commands::device_status::{
            get_current_device_snapshot, refresh_device_detection,
        };
        use $crate::commands::dfu_restore::{
            ios_detect_dfu_state, ios_download_ipsw, ios_enter_dfu, ios_restore_device,
        };
        use $crate::commands::diagnostics::diag_test_handshake;
        use $crate::commands::edl::{
            edl_configure, edl_erase_partition, edl_find_device, edl_get_storage_info,
            edl_read_partition, edl_reboot, edl_sahara_handshake, edl_upload_programmer,
            edl_write_partition,
        };
        use $crate::commands::edl_bypass::stage1::edl_stage1_detect;
        use $crate::commands::edl_bypass::stage10::edl_stage10_frp_erase;
        use $crate::commands::edl_bypass::stage11::edl_stage11_userdata_plan;
        use $crate::commands::edl_bypass::stage12::edl_stage12_userdata_format;
        use $crate::commands::edl_bypass::stage13::edl_stage13_persist_backup;
        use $crate::commands::edl_bypass::stage14::edl_stage14_modem_backup;
        use $crate::commands::edl_bypass::stage15::edl_stage15_partition_read;
        use $crate::commands::edl_bypass::stage16::edl_stage16_partition_write;
        use $crate::commands::edl_bypass::stage17::edl_stage17_xml_console;
        use $crate::commands::edl_bypass::stage18::edl_stage18_power_control;
        use $crate::commands::edl_bypass::stage19::edl_stage19_verify;
        use $crate::commands::edl_bypass::stage2::edl_stage2_sahara;
        use $crate::commands::edl_bypass::stage20::edl_stage20_complete;
        use $crate::commands::edl_bypass::stage3::edl_stage3_programmer;
        use $crate::commands::edl_bypass::stage4::edl_stage4_firehose_upload;
        use $crate::commands::edl_bypass::stage5::edl_stage5_firehose_config;
        use $crate::commands::edl_bypass::stage6::edl_stage6_storage_probe;
        use $crate::commands::edl_bypass::stage7::edl_stage7_gpt;
        use $crate::commands::edl_bypass::stage8::edl_stage8_partition_map;
        use $crate::commands::edl_bypass::stage9::edl_stage9_frp_plan;
        use $crate::commands::exploit::{bypass_icloud_activation, run_palera1n, verify_pwned_dfu};
        use $crate::commands::extraction::{ios_mass_extract, ios_mount_ramdisk};
        use $crate::commands::f3arrain::{f3arrain_checkm8, f3arrain_detect, f3arrain_full};
        use $crate::commands::filesystem::activation::fs_patch_activation;
        use $crate::commands::filesystem::lockdown::{fs_patch_lockdown, fs_restore_lockdown};
        use $crate::commands::filesystem::mount::{
            fs_list_path, fs_mount_readwrite, fs_pull_file, fs_push_file, fs_read_file,
            fs_start_tunnel, fs_write_file,
        };
        use $crate::commands::filesystem::setup_app::{fs_patch_setup_app, fs_restore_setup_app};
        use $crate::commands::hello_bypass::{hello_bypass_detect, hello_bypass_run};
        use $crate::commands::hydra::{
            hydra_detect_protocol, hydra_run_mtk_meta, hydra_samsung_frp_bypass,
        };
        use $crate::commands::identity::{ios_device_identity, ios_imei_state};
        use $crate::commands::ios_backup::{
            ios_backup_info, ios_extract_hash, ios_extract_screentime, ios_run_crack,
        };
        use $crate::commands::ios_bypass::ios_bypass_full;
        use $crate::commands::ios_chain::{
            ios_detect_device, run_fake_erase, run_full_signal_bypass, run_hello_bypass,
        };
        use $crate::commands::iremoval_bypass::{
            iremoval_detect, iremoval_iservices, iremoval_run,
        };
        use $crate::commands::logcat::{
            adb_logcat_clear, adb_logcat_dump, adb_logcat_export, adb_logcat_start,
            adb_logcat_stop, clear_logcat_buffer, export_logcat_to_file, start_logcat_stream,
            stop_logcat_stream,
        };
        use $crate::commands::mdm::{ios_list_profiles, ios_mdm_state, ios_remove_mdm};
        use $crate::commands::mtk::{
            mtk_device_info, mtk_erase_partition, mtk_read_partition, mtk_run_command,
            mtk_unlock_bootloader, mtk_write_partition,
        };
        use $crate::commands::mtk_brom::{
            mtk_bypass_sla, mtk_da_erase_partition, mtk_da_read_partition, mtk_da_write_partition,
            mtk_detect_auth_type, mtk_detect_device, mtk_dump_preloader, mtk_erase_frp,
            mtk_format_userdata, mtk_handshake_and_identify, mtk_jump_to_da, mtk_list_partitions,
            mtk_read_imei, mtk_reboot, mtk_upload_da, mtk_write_imei,
        };
        use $crate::commands::orchestrator::{ios_inject_surgical_patch, ios_poll_orchestrator};
        use $crate::commands::persistence::tethered::{
            persist_check_tethered, persist_reapply_tethered,
        };
        use $crate::commands::persistence::untethered::{
            persist_install_untethered, persist_remove_untethered,
        };
        use $crate::commands::ramdisk::{
            ios_boot_ramdisk, ios_check_pwn_state, ios_run_gaster_pwn,
        };
        use $crate::commands::rebuild::check_for_updates;
        use $crate::commands::rebuild::{
            check_activation_status, check_samsung_download_mode, check_usb_permissions,
            get_all_testpoints, get_connected_device, get_ios_device_info, restart_adb_server,
            run_activation_bypass, run_adb_frp, run_da_bypass, run_deepeye_agent, run_force_dfu,
            run_frp_erase, run_full_bypass, run_ipsw_flash, run_mdm_bypass, run_meta_bypass,
            run_mtk_brom_bypass, run_passcode_remove, run_pattern_bypass, run_qcom_edl,
            run_qcom_frp_erase, run_sahara_handshake, run_samsung_frp, run_samsung_odin_info,
            run_screen_bypass, run_shsh_save, run_tool_version_check, search_testpoints,
        };
        use $crate::commands::reporter::reporter_generate_audit;
        use $crate::commands::rom_flasher::{
            fastboot_erase_partition, fastboot_get_all_variables, fastboot_list_devices,
            fastboot_reboot_target, fastboot_unlock_bootloader, rom_flash_partition,
            rom_reboot_bootloader, rom_reboot_recovery, rom_sideload_zip, rom_wipe_data,
        };
        use $crate::commands::rom_manager::{
            rom_add_to_queue, rom_build_flash_plan, rom_clear_queue, rom_detect_type,
            rom_get_queue, rom_inspect_zip, rom_move_queue_item, rom_remove_from_queue,
            rom_select_file, rom_toggle_queue_partition, rom_validate_package,
        };
        use $crate::commands::samsung::{
            samsung_do_erase_frp_cmd, samsung_do_handshake_cmd, samsung_find_device_cmd,
            samsung_flash_part_cmd, samsung_get_pit_cmd, samsung_reboot_device_cmd,
        };
        use $crate::commands::screentime::{ios_extract_screentime_hash, ios_run_screentime_crack};
        use $crate::commands::session_manager::{
            cancel_active_session, clear_active_session, get_active_session,
            start_operation_session,
        };
        use $crate::commands::ticket::{
            ios_activation_record_state, ios_parse_activation_record, ios_scan_tickets,
        };
        use $crate::commands::unisoc::unisoc_detect_device;
        use $crate::commands::updater::{check_update, do_install_update};
        use $crate::commands::usb_utils::usb_debug_list_devices;
        use $crate::commands::vault::ios_create_deepvault;
        use $crate::commands::wifi_adb::{
            connect_wifi_adb, disconnect_wifi_adb, enable_adb_wifi_mode, pair_wifi_adb,
        };
        use $crate::config::settings::{load_settings, save_settings};
        use $crate::db::history::{
            add_history_entry, clear_history, export_history_csv, get_history,
        };
        use $crate::qualcomm::programmer_db::{get_edl_programmers, load_edl_programmer};
        use $crate::unisoc::edl::run_unisoc_frp_bypass;

        use $crate::shsh::{
            check_signed_versions, futurerestore, futurerestore_no_baseband, get_board_config,
            get_ecid, get_shsh_device_info, list_saved_shsh, save_shsh_all_signed,
            save_shsh_specific, save_shsh_with_generator,
        };

        use $crate::diagnostics::{
            device_restart, device_shutdown, device_sleep, get_battery_stats, get_thermal_state,
            run_diagnostics,
        };

        use $crate::restore::{
            exit_recovery, get_recovery_info, restore_latest, restore_local_ipsw,
        };

        use $crate::purple::{enter_purple_mode, purple_read_all, purple_read_sn, purple_write_sn};

        use $crate::toolbox::{
            toolbox_backup_device, toolbox_block_ota, toolbox_factory_reset, toolbox_get_logs,
        };

        use $crate::cve::{query_cve_database, run_intelligence_scan};

        use $crate::vault::{list_cloud_vault, pull_from_cloud_vault, push_to_cloud_vault};

        use $crate::identity::{check_imei_intel, get_full_identity};

        use $crate::nonce::{
            clear_nonce, get_current_nonce, get_generator_from_blob, set_nonce_checkra1n,
            set_nonce_from_blob, set_nonce_generator,
        };

        use $crate::afc::{
            delete_path, get_file_info, list_directory, make_directory, mount_afc2, pull_file,
            push_file, read_file, write_file,
        };

        use $crate::backup::{
            backup_encrypted, change_backup_password, create_backup, delete_backup,
            extract_app_data, list_backups, restore_app_data, restore_backup,
        };

        use $crate::frida::{
            dump_app_memory, frida_attach, frida_inject, frida_kill_process, frida_list_exports,
            frida_ps, frida_run_script, frida_spawn, inject_dylib, ssl_kill_switch,
        };

        use $crate::sideloader::{
            get_app_info, install_ipa, list_installed_apps, reinstall_app, sign_and_install,
            uninstall_app,
        };

        use $crate::ssh_tunnel::{
            check_tunnel_status, install_sileo_pkg, run_ssh_command, run_su_command,
            ssh_download_file, ssh_upload_file, start_ssh_tunnel, stop_ssh_tunnel,
        };

        use $crate::crash_logs::{
            clear_crash_logs, list_crash_logs, pull_crash_logs, read_crash_log, symbolicate_log,
        };

        use $crate::ipsw_dl::{
            download_ipsw, get_all_firmwares, get_download_progress, get_signed_firmwares,
            verify_ipsw_sha1,
        };

        use $crate::developer::{
            check_dev_disk_mounted, get_screenshot, list_processes, mount_dev_disk_image,
            unmount_dev_disk_image,
        };

        tauri::generate_handler![
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
            get_current_device_snapshot,
            refresh_device_detection,
            start_operation_session,
            get_active_session,
            cancel_active_session,
            clear_active_session,
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
            f3arrain_full,
            f3arrain_detect,
            f3arrain_checkm8,
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
            get_shsh_device_info,
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
            check_update,
            do_install_update,
            edl_find_device,
            edl_sahara_handshake,
            edl_upload_programmer,
            edl_configure,
            edl_read_partition,
            edl_write_partition,
            edl_erase_partition,
            edl_get_storage_info,
            edl_reboot,
            edl_stage1_detect,
            edl_stage2_sahara,
            edl_stage3_programmer,
            edl_stage4_firehose_upload,
            edl_stage5_firehose_config,
            edl_stage6_storage_probe,
            edl_stage7_gpt,
            edl_stage8_partition_map,
            edl_stage9_frp_plan,
            edl_stage10_frp_erase,
            edl_stage11_userdata_plan,
            edl_stage12_userdata_format,
            edl_stage13_persist_backup,
            edl_stage14_modem_backup,
            edl_stage15_partition_read,
            edl_stage16_partition_write,
            edl_stage17_xml_console,
            edl_stage18_power_control,
            edl_stage19_verify,
            edl_stage20_complete,
            unisoc_detect_device,
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
            add_history_entry,
            get_history,
            clear_history,
            export_history_csv,
            load_settings,
            save_settings,
            db_search_devices,
            db_lookup_model,
            db_auto_route,
            db_lookup_vid_pid,
            db_list_all,
            frp_execute_protocol,
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
            get_ios_device_info,
            check_activation_status,
            run_activation_bypass,
            run_mdm_bypass,
            run_force_dfu,
            run_passcode_remove,
            run_shsh_save,
            run_ipsw_flash,
            run_samsung_frp,
            run_samsung_odin_info,
            search_testpoints,
            get_all_testpoints,
            run_full_bypass,
            check_usb_permissions,
            restart_adb_server,
            check_samsung_download_mode,
            run_tool_version_check,
            pair_wifi_adb,
            connect_wifi_adb,
            disconnect_wifi_adb,
            enable_adb_wifi_mode,
            run_unisoc_frp_bypass,
            check_for_updates,
            get_edl_programmers,
            load_edl_programmer,
            hello_bypass_detect,
            hello_bypass_run,
            iremoval_detect,
            iremoval_run,
            iremoval_iservices,
            ios_detect_device,
            run_hello_bypass,
            run_full_signal_bypass,
            run_fake_erase,
            fs_start_tunnel,
            fs_mount_readwrite,
            fs_list_path,
            fs_read_file,
            fs_write_file,
            fs_push_file,
            fs_pull_file,
            fs_patch_setup_app,
            fs_restore_setup_app,
            fs_patch_activation,
            fs_patch_lockdown,
            fs_restore_lockdown,
            persist_check_tethered,
            persist_reapply_tethered,
            persist_install_untethered,
            persist_remove_untethered,
        ]
    }};
}
