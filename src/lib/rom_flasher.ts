export const ROM_FLASHER_COMMANDS = {
  SIDELOAD_ZIP: 'rom_sideload_zip',
  FLASH_PARTITION: 'rom_flash_partition',
  WIPE_DATA: 'rom_wipe_data',
  REBOOT_RECOVERY: 'rom_reboot_recovery',
  REBOOT_BOOTLOADER: 'rom_reboot_bootloader',
} as const;

export type FlashResult = {
  success: boolean;
  partition: string;
  message: string;
};
