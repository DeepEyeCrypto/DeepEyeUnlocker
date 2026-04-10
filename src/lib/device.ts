/**
 * Device Protocol Integration - TypeScript Types and API
 * 
 * Provides type-safe access to device detection and protocol operations
 * for MTK BROM, Qualcomm EDL, Fastboot, Samsung Odin, and more.
 */

import { invoke } from '@tauri-apps/api/core';

// ─────────────────────────────────────────────────────────────────────────────
// Enums
// ─────────────────────────────────────────────────────────────────────────────

export enum DeviceMode {
  Brom = 'Brom',
  PreLoader = 'PreLoader',
  Edl = 'Edl',
  Fastboot = 'Fastboot',
  Adb = 'Adb',
  Mtp = 'Mtp',
  Recovery = 'Recovery',
  SamsungOdin = 'SamsungOdin',
  UnisocFdl = 'UnisocFdl',
  Unknown = 'Unknown'
}

export enum ProtocolType {
  MtkBrom = 'MtkBrom',
  MtkPreloader = 'MtkPreloader',
  QualcommEdl = 'QualcommEdl',
  Fastboot = 'Fastboot',
  SamsungOdin = 'SamsungOdin',
  UnisocFdl = 'UnisocFdl',
  Adb = 'Adb',
  Mtp = 'Mtp',
  Unknown = 'Unknown'
}

// ─────────────────────────────────────────────────────────────────────────────
// Interfaces
// ─────────────────────────────────────────────────────────────────────────────

export interface DetectedDevice {
  mode: DeviceMode;
  vid: number;
  pid: number;
  serial: string | null;
  manufacturer: string | null;
  product: string | null;
  bus: number;
  address: number;
  chipset: string | null;
  detectedAt: number;
}

export interface DeviceConnectionStatus {
  connected: boolean;
  device: DetectedDevice | null;
  protocol: ProtocolType;
  message: string;
}

export interface DeviceScanResult {
  devices: DeviceConnectionStatus[];
  count: number;
  hasSupported: boolean;
}

export interface FastbootDeviceInfo {
  serial: string;
  product: string;
  variant: string;
  bootloaderVersion: string;
  basebandVersion: string;
  secureBoot: boolean;
  unlocked: boolean;
}

export interface FlashProgress {
  written: number;
  total: number;
  percent: number;
  partition: string;
}

// ─────────────────────────────────────────────────────────────────────────────
// Device Detection API
// ─────────────────────────────────────────────────────────────────────────────

async function invokeCommand<T>(command: string, args?: Record<string, unknown>): Promise<T> {
  return await invoke<T>(command, args);
}

/**
 * Scan all USB devices and return detected devices
 */
export async function scanDevices(): Promise<DeviceScanResult> {
  return await invokeCommand<DeviceScanResult>('device_scan_all');
}

/**
 * Auto-detect and connect to the best available device
 */
export async function autoConnectDevice(): Promise<DeviceConnectionStatus> {
  return await invokeCommand<DeviceConnectionStatus>('device_auto_connect');
}

/**
 * Check if specific device mode is available
 */
export async function checkDeviceMode(mode: string): Promise<boolean> {
  return await invokeCommand<boolean>('device_check_mode', { mode });
}

/**
 * Get protocol type name for display
 */
export async function getProtocolName(protocol: string): Promise<string> {
  return await invokeCommand<string>('device_get_protocol_name', { protocol });
}

// ─────────────────────────────────────────────────────────────────────────────
// Fastboot API
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Detect if fastboot device is connected
 */
export async function fastbootDetect(): Promise<boolean> {
  return await invokeCommand<boolean>('fastboot_detect');
}

/**
 * Get fastboot device information
 */
export async function fastbootGetInfo(): Promise<FastbootDeviceInfo> {
  return await invokeCommand<FastbootDeviceInfo>('fastboot_get_info');
}

/**
 * Flash partition from file
 */
export async function fastbootFlashPartition(
  partition: string,
  filePath: string
): Promise<void> {
  return await invokeCommand<void>('fastboot_flash_partition', {
    partition,
    filePath
  });
}

/**
 * Erase partition
 */
export async function fastbootErasePartition(partition: string): Promise<void> {
  return await invokeCommand<void>('fastboot_erase_partition', { partition });
}

/**
 * Reboot device
 */
export async function fastbootReboot(): Promise<void> {
  return await invokeCommand<void>('fastboot_reboot');
}

/**
 * Reboot to bootloader
 */
export async function fastbootRebootBootloader(): Promise<void> {
  return await invokeCommand<void>('fastboot_reboot_bootloader');
}

/**
 * Reboot to recovery
 */
export async function fastbootRebootRecovery(): Promise<void> {
  return await invokeCommand<void>('fastboot_reboot_recovery');
}

/**
 * Unlock bootloader
 */
export async function fastbootUnlockBootloader(): Promise<void> {
  return await invokeCommand<void>('fastboot_unlock_bootloader');
}

/**
 * Lock bootloader
 */
export async function fastbootLockBootloader(): Promise<void> {
  return await invokeCommand<void>('fastboot_lock_bootloader');
}

// ─────────────────────────────────────────────────────────────────────────────
// Utility Functions
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Get human-readable device mode name
 */
export function getDeviceModeName(mode: DeviceMode): string {
  const names: Record<DeviceMode, string> = {
    [DeviceMode.Brom]: 'MediaTek BROM',
    [DeviceMode.PreLoader]: 'MediaTek PreLoader',
    [DeviceMode.Edl]: 'Qualcomm EDL',
    [DeviceMode.Fastboot]: 'Fastboot',
    [DeviceMode.Adb]: 'Android ADB',
    [DeviceMode.Mtp]: 'MTP Device',
    [DeviceMode.Recovery]: 'Recovery Mode',
    [DeviceMode.SamsungOdin]: 'Samsung Odin',
    [DeviceMode.UnisocFdl]: 'UniSoc FDL',
    [DeviceMode.Unknown]: 'Unknown Device'
  };
  return names[mode];
}

/**
 * Get device mode color for UI
 */
export function getDeviceModeColor(mode: DeviceMode): string {
  const colors: Record<DeviceMode, string> = {
    [DeviceMode.Brom]: '#009688',
    [DeviceMode.PreLoader]: '#00BCD4',
    [DeviceMode.Edl]: '#9C27B0',
    [DeviceMode.Fastboot]: '#FF9800',
    [DeviceMode.Adb]: '#4CAF50',
    [DeviceMode.Mtp]: '#2196F3',
    [DeviceMode.Recovery]: '#FF5722',
    [DeviceMode.SamsungOdin]: '#1976D2',
    [DeviceMode.UnisocFdl]: '#795548',
    [DeviceMode.Unknown]: '#555555'
  };
  return colors[mode];
}

/**
 * Check if device is in download/flash mode
 */
export function isFlashMode(mode: DeviceMode): boolean {
  return [
    DeviceMode.Brom,
    DeviceMode.PreLoader,
    DeviceMode.Edl,
    DeviceMode.Fastboot,
    DeviceMode.SamsungOdin,
    DeviceMode.UnisocFdl
  ].includes(mode);
}

/**
 * Check if device is MTK-based
 */
export function isMtkDevice(mode: DeviceMode): boolean {
  return [DeviceMode.Brom, DeviceMode.PreLoader].includes(mode);
}

/**
 * Check if device is Qualcomm-based
 */
export function isQualcommDevice(mode: DeviceMode): boolean {
  return mode === DeviceMode.Edl;
}
