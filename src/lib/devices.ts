import { invoke } from "@tauri-apps/api/core";

export type ConnectedDevice = {
  id: string;
  model: string;
  serial: string;
  os: string;
  mode: string;
  bootloaderStatus: string;
  carrier?: string | null;
  source: string;
};

export type DeviceConnectionState = "idle" | "scanning" | "connected" | "error";

export async function fetchConnectedDevices(): Promise<ConnectedDevice[]> {
  return invoke<ConnectedDevice[]>("get_connected_devices");
}

export function createStableId(prefix: string): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return `${prefix}-${crypto.randomUUID()}`;
  }
  return `${prefix}-${Date.now()}`;
}

export function formatElapsed(ms: number): string {
  const totalSeconds = Math.max(0, Math.floor(ms / 1000));
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${seconds.toString().padStart(2, "0")}`;
}

