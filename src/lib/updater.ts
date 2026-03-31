export const UPDATER_COMMANDS = {
  CHECK: 'check_for_update',
  INSTALL: 'install_update',
} as const;

export type UpdateInfo = {
  available: boolean;
  version: string;
  body: string;
  date: string;
};
