// Auto-indexed Tauri command registry
// ALL invoke() calls must use these constants

export const COMMANDS = {
  // Apple iOS
  ONE_CLICK_BYPASS: "one_click_bypass",
  IOS_DEVICE_INFO: "ios_device_info",
  SHSH_SAVE: "shsh_save",
  CHECK_APPLE_TOOLS: "check_apple_tools",
  IOS_RUN_CHECKRA1N: "ios_run_checkra1n",
  IOS_UNTETHERED_BYPASS: "ios_untethered_bypass",
  IOS_RESTORE_DEVICE: "ios_restore_device",
  IOS_ENTER_DFU: "ios_enter_dfu",
  IOS_INJECT_SURGICAL_PATCH: "ios_inject_surgical_patch",
  IOS_RUN_GASTER_PWN: "ios_run_gaster_pwn",
  IOS_BOOT_RAMDISK: "ios_boot_ramdisk",
  IOS_RUN_SCREENTIME_CRACK: "ios_run_screentime_crack",

  // Signal Bypass stages 1-10
  SIGNAL_STAGE_1: "signal_bypass_stage1",
  SIGNAL_STAGE_2: "signal_bypass_stage2",
  SIGNAL_STAGE_3: "signal_bypass_stage3",
  SIGNAL_STAGE_4: "signal_bypass_stage4",
  SIGNAL_STAGE_5: "signal_bypass_stage5",
  SIGNAL_STAGE_6: "signal_bypass_stage6",
  SIGNAL_STAGE_7: "signal_bypass_stage7",
  SIGNAL_STAGE_8: "signal_bypass_stage8",
  SIGNAL_STAGE_9: "signal_bypass_stage9",
  SIGNAL_STAGE_10: "signal_bypass_stage10",

  // EDL stages 1-20
  EDL_STAGE_1: "edl_stage1_detect",
  EDL_STAGE_2: "edl_stage2_handshake",
  EDL_STAGE_3: "edl_stage3_auth",
  EDL_STAGE_4: "edl_stage4_sahara",
  EDL_STAGE_5: "edl_stage5_firehose",
  EDL_STAGE_6: "edl_stage6_config",
  EDL_STAGE_7: "edl_stage7_gpt",
  EDL_STAGE_8: "edl_stage8_read_part",
  EDL_STAGE_9: "edl_stage9_write_part",
  EDL_STAGE_10: "edl_stage10_erase_part",
  EDL_STAGE_11: "edl_stage11_patch_part",
  EDL_STAGE_12: "edl_stage12_verify",
  EDL_STAGE_13: "edl_stage13_boot_patch",
  EDL_STAGE_14: "edl_stage14_frp_wipe",
  EDL_STAGE_15: "edl_stage15_mdm_wipe",
  EDL_STAGE_16: "edl_stage16_oem_unlock",
  EDL_STAGE_17: "edl_stage17_persist_wipe",
  EDL_STAGE_18: "edl_stage18_modem_reset",
  EDL_STAGE_19: "edl_stage19_reboot",
  EDL_STAGE_20: "edl_stage20_finish",

  // EDL Programmer
  LOAD_EDL_PROGRAMMER: "load_edl_programmer",

  // Samsung
  SAMSUNG_FRP: "samsung_frp_bypass",
  SAMSUNG_ODIN: "samsung_odin_flash",
  SAMSUNG_FLASH_PART_CMD: "samsung_flash_part_cmd",
  SAMSUNG_REBOOT_DEVICE_CMD: "samsung_reboot_device_cmd",

  // ADB
  ADB_DEVICES: "adb_list_devices",
  ADB_SHELL: "adb_run_shell",
  ADB_REBOOT_DEVICE: "adb_reboot_device",
  ADB_INSTALL_APK: "adb_install_apk",
  ADB_PUSH_FILE: "adb_push_file",
  ADB_PULL_FILE: "adb_pull_file",
  ADB_SIDELOAD_ZIP: "adb_sideload_zip",
  ADB_ERASE_FRP_PARTITION: "adb_erase_frp_partition",
  STREAM_ADB_LOGS: "stream_adb_logs",
  PAIR_WIFI_ADB: "pair_wifi_adb",
  CONNECT_WIFI_ADB: "connect_wifi_adb",
  DISCONNECT_WIFI_ADB: "disconnect_wifi_adb",

  // MTK
  MTK_BROM_DETECT: "mtk_brom_detect",
  MTK_DA_LOAD: "mtk_da_load",

  // Misc
  PUSH_TO_CLOUD_VAULT: "push_to_cloud_vault",
  EXPORT_HISTORY_CSV: "export_history_csv",
  RUN_INTELLIGENCE_SCAN: "run_intelligence_scan",
  FRP_EXECUTE_PROTOCOL: "frp_execute_protocol",

} as const;
