export const HISTORY_COMMANDS = {
  ADD_ENTRY: 'history_add_entry',
  GET_ENTRIES: 'history_get_entries',
  CLEAR: 'history_clear',
  DELETE_ENTRY: 'history_delete_entry',
  EXPORT_JSON: 'history_export_json',
} as const;

export type DeviceHistoryEntry = {
  id: string;
  timestamp: string;
  model: string;
  serial: string;
  os_version: string;
  mode: string;
  action: string;
  result: string;
  platform: string;
};
