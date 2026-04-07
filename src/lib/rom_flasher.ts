export const ROM_FLASHER_COMMANDS = {
  SIDELOAD_ZIP: "rom_sideload_zip",
  FLASH_PARTITION: "rom_flash_partition",
  ERASE_PARTITION: "fastboot_erase_partition",
  WIPE_DATA: "rom_wipe_data",
  REBOOT_RECOVERY: "rom_reboot_recovery",
  REBOOT_BOOTLOADER: "rom_reboot_bootloader",
  LIST_DEVICES: "fastboot_list_devices",
  GET_ALL_VARIABLES: "fastboot_get_all_variables",
  UNLOCK_BOOTLOADER: "fastboot_unlock_bootloader",
  REBOOT_TARGET: "fastboot_reboot_target",
} as const;

export type FlashResult = {
  success: boolean;
  partition: string;
  message: string;
};

export type FastbootDevice = {
  serial: string;
  state: string;
};

export type FastbootVariable = {
  key: string;
  value: string;
};

export type FastbootQueryResult = {
  serial: string | null;
  variables: FastbootVariable[];
  raw_output: string;
};
