export const EDL_COMMANDS = {
  DETECT: 'edl_detect_device',
  READ_PARTITION: 'edl_read_partition',
  WRITE_PARTITION: 'edl_write_partition',
  ERASE_PARTITION: 'edl_erase_partition',
  REBOOT: 'edl_reboot',
  GET_GPT: 'edl_get_gpt',
} as const;

export type EdlDeviceInfo = {
  detected: boolean;
  chipset: string;
  serial: string;
  mode: string;
};
