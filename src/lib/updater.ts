import { invoke } from "@tauri-apps/api/core";

export const UPDATER_COMMANDS = {
  CHECK: "check_update",
  INSTALL: "do_install_update",
} as const;

export type UpdateStatus =
  | "idle"
  | "checking"
  | "available"
  | "upToDate"
  | "installing"
  | "error";

export type UpdateInfo = {
  version: string;
  body: string;
  date: string;
  downloadUrl: string;
};

export async function checkForUpdate(): Promise<UpdateInfo | null> {
  return invoke<UpdateInfo | null>(UPDATER_COMMANDS.CHECK);
}

export async function installUpdate(): Promise<void> {
  await invoke<void>(UPDATER_COMMANDS.INSTALL);
}

export function summarizeChangelog(body: string, maxLines = 2): string {
  return body
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line.length > 0)
    .slice(0, maxLines)
    .join(" • ");
}
