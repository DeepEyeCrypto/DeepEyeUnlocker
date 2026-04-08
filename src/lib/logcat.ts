import { invoke } from "@tauri-apps/api/core";

export interface LogcatEntry {
  timestamp: string;
  level: string;
  tag: string;
  pid: number | null;
  tid: number | null;
  message: string;
  raw: string;
}

export interface LogcatFilter {
  serial?: string | null;
  level?: string | null;
  tag?: string | null;
  keyword?: string | null;
  pid?: number | null;
}

export interface LogcatStatus {
  running: boolean;
  serial?: string | null;
  message: string;
}

export interface LogcatError {
  message: string;
}

function normalizeFilter(serial?: string, filter?: Omit<LogcatFilter, "serial">): LogcatFilter {
  return {
    serial: serial ?? null,
    level: filter?.level ?? null,
    tag: filter?.tag ?? null,
    keyword: filter?.keyword ?? null,
    pid: filter?.pid ?? null,
  };
}

export async function startLogcat(
  serial?: string,
  filter?: Omit<LogcatFilter, "serial">,
): Promise<void> {
  await invoke<string>("start_logcat_stream", {
    filter: normalizeFilter(serial, filter),
  });
}

export async function stopLogcat(): Promise<void> {
  await invoke<void>("stop_logcat_stream");
}

export async function clearLogcat(serial?: string): Promise<void> {
  await invoke<string>("clear_logcat_buffer", {
    serial: serial ?? null,
  });
}

export async function dumpLogcat(
  serial?: string,
  filterSpec?: string,
): Promise<LogcatEntry[]> {
  return invoke<LogcatEntry[]>("adb_logcat_dump", {
    serial: serial ?? null,
    filter_spec: filterSpec ?? null,
  });
}

export async function exportLogcat(entries: LogcatEntry[], outputPath: string): Promise<string> {
  return invoke<string>("export_logcat_to_file", {
    entries,
    file_path: outputPath,
  });
}
