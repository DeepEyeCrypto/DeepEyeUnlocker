import { safeInvoke } from "./tauri-utils";

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
  return safeInvoke<ConnectedDevice[]>("get_connected_devices");
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

import devicesDb from '../assets/supported_devices.json';

export type BromSupport = 'full' | 'partial' | 'edl_only' | 'none';

export interface DeviceEntry {
  brand: string;
  model: string;
  series: string;
  year: number;
  type: string;
  chipset: string;
  brom_support: BromSupport;
}

export const allDevices: DeviceEntry[] = (devicesDb as any).devices;

export const getBrands = (): string[] =>
  [...new Set(allDevices.map(d => d.brand))].sort();

export const getModelsByBrand = (brand: string): DeviceEntry[] =>
  allDevices.filter(d => d.brand === brand)
    .sort((a, b) => b.year - a.year);

export const detectSupportLevel = (
  brand: string, model: string
): BromSupport =>
  allDevices.find(
    d => d.brand === brand &&
    d.model.toLowerCase().includes(model.toLowerCase())
  )?.brom_support ?? 'none';

export const getSupportBadge = (level: BromSupport) => ({
  full:     { label: 'Full BROM',  color: '#10b981' },
  partial:  { label: 'Partial',    color: '#f59e0b' },
  edl_only: { label: 'EDL Only',   color: '#3b82f6' },
  none:     { label: 'No Support', color: '#6b7280' },
}[level]);
