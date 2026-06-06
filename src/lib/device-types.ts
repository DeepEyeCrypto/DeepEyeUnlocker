export type DeviceConnectionState =
  | 'disconnected'
  | 'detecting'
  | 'connected'
  | 'unstable'
  | 'unauthorized'
  | 'error';

export type DeviceMode =
  | 'unknown'
  | 'normal'
  | 'recovery'
  | 'dfu'
  | 'fastboot'
  | 'adb'
  | 'sideload'
  | 'edl'
  | 'diagnostic'
  | 'purple'
  | 'boot_files'
  | 'unsupported';

export type DevicePlatform = 'ios' | 'android' | 'qualcomm' | 'mtk' | 'unisoc' | 'unknown';

export type CapabilityFlags =
  | 'canReadInfo'
  | 'canEnterRecovery'
  | 'canExitRecovery'
  | 'canUseAdb'
  | 'canUseFastboot'
  | 'canStartSession'
  | 'canRunToolbox'
  | 'requiresTrust'
  | 'requiresDriver';

export type RiskFlags =
  | 'multipleDevicesConnected'
  | 'missingDriver'
  | 'unauthorizedHost'
  | 'unstableUsb'
  | 'unsupportedMode'
  | 'incompleteIdentity'
  | 'needsManualReconnect';

export interface DeviceSnapshot {
  id: string; // UDID, Serial, or stable USB bus ID
  connectionState: DeviceConnectionState;
  platform: DevicePlatform;
  mode: DeviceMode;
  manufacturer?: string;
  productName?: string;
  model: string;
  modelCode?: string;
  serial: string;
  osVersion?: string;
  chipset?: string;
  isSupported: boolean;
  supportReason?: string;
  riskFlags: RiskFlags[];
  capabilityFlags: CapabilityFlags[];
  detectedAt: number; // Unix timestamp ms
  updatedAt: number; // Unix timestamp ms
}
