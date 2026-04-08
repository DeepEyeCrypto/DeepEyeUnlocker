import { invoke } from "@tauri-apps/api/core";

export type RomType =
  | "fastboot"
  | "recoveryZip"
  | "otaPackage"
  | "spFlashTool"
  | "odinPackage"
  | "qualcommEdl"
  | "unknown";

export type PlatformFamily =
  | "qualcomm"
  | "mediaTek"
  | "samsung"
  | "genericAndroid"
  | "unknown";

export type FlashMode =
  | "adb"
  | "fastboot"
  | "recoverySideload"
  | "edl"
  | "bromDownload"
  | "odinDownload"
  | "unknown";

export type CompatibilityState =
  | "compatible"
  | "likelyCompatible"
  | "incompatible"
  | "unknown";

export type FlashActionType =
  | "flashPartition"
  | "applyOta"
  | "flashPackage"
  | "erasePartition"
  | "programRaw"
  | "patchRaw"
  | "bootloaderStep";

export type RiskLevel = "low" | "medium" | "high" | "critical";

export type QueueStatus =
  | "pending"
  | "validating"
  | "ready"
  | "blocked"
  | "flashing"
  | "completed"
  | "failed";

export interface ConnectedDeviceSummary {
  id: string;
  model: string;
  serial: string;
  mode: string;
  source: string;
  bootloaderStatus: string;
  carrier: string | null;
}

export interface ArchiveEntryInfo {
  path: string;
  isDir: boolean;
  compressedSize: number;
  uncompressedSize: number;
}

export interface RomIndicators {
  hasImagesDir: boolean;
  hasFlashAllScript: boolean;
  hasAndroidInfo: boolean;
  hasPayloadBin: boolean;
  hasPayloadProperties: boolean;
  hasCareMap: boolean;
  hasMetaInf: boolean;
  hasScatter: boolean;
  hasPreloader: boolean;
  hasRawprogramXml: boolean;
  hasPatchXml: boolean;
  hasFirehose: boolean;
  hasOdinPackages: boolean;
  hasPit: boolean;
  hasTarMd5: boolean;
}

export interface PartitionCandidate {
  name: string;
  source: string;
  sourceFile: string;
  estimatedSize: number | null;
  actionType: FlashActionType;
}

export interface RomSummary {
  fileName: string;
  filePath: string;
  archiveEntryCount: number;
  totalCompressedSize: number;
  totalUncompressedSize: number;
  romType: RomType;
  detectedBrand: string | null;
  detectedPlatform: PlatformFamily;
  supportedFlashMode: FlashMode;
  topLevelFolders: string[];
  productHints: string[];
  codenameHints: string[];
  buildHints: string[];
}

export interface DeviceMatch {
  brand: string;
  model: string;
  codename: string;
  soc: string;
  socFamily: string;
  protocol: string;
}

export interface CompatibilityReport {
  state: CompatibilityState;
  score: number;
  reasons: string[];
  connectedDevice: ConnectedDeviceSummary | null;
  matchedDatabaseEntry: DeviceMatch | null;
}

export interface FlashEntry {
  partition: string;
  sourceFile: string;
  actionType: FlashActionType;
  estimatedSize: number | null;
  checksumAvailable: boolean;
  requiredProtocol: FlashMode;
  order: number;
  enabled: boolean;
  riskLevel: RiskLevel;
  notes: string[];
}

export interface FlashPlan {
  romSummary: RomSummary;
  detectedPlatform: PlatformFamily;
  detectedBrand: string | null;
  supportedFlashMode: FlashMode;
  requiredDeviceState: string;
  flashEntries: FlashEntry[];
  dataWipeImplied: boolean;
  bootloaderUnlockRequired: boolean;
  looksDangerousOrIncomplete: boolean;
  executionSupported: boolean;
  warnings: string[];
  blockers: string[];
}

export interface PackageValidation {
  valid: boolean;
  status: QueueStatus;
  warnings: string[];
  blockers: string[];
  executionSupported: boolean;
  dangerous: boolean;
}

export interface RomAnalysis {
  packageId: string;
  generatedAt: string;
  summary: RomSummary;
  archiveSha256: string;
  archiveEntries: ArchiveEntryInfo[];
  payloadFiles: string[];
  manifestFiles: string[];
  partitionCandidates: PartitionCandidate[];
  indicators: RomIndicators;
  compatibility: CompatibilityReport;
  validation: PackageValidation;
  flashPlan: FlashPlan;
}

export interface QueueItem {
  id: string;
  filePath: string;
  fileName: string;
  romType: RomType;
  flashMode: FlashMode;
  detectedBrand: string | null;
  detectedPlatform: PlatformFamily;
  status: QueueStatus;
  executionSupported: boolean;
  selectedPartitions: string[];
  warnings: string[];
  blockers: string[];
  addedAt: string;
  analysis: RomAnalysis;
}

export interface RomDetection {
  romType: RomType;
  detectedBrand: string | null;
  detectedPlatform: PlatformFamily;
  supportedFlashMode: FlashMode;
  warnings: string[];
  blockers: string[];
}

export async function selectRomFile(): Promise<string | null> {
  return invoke<string | null>("rom_select_file");
}

export async function detectRomType(filePath: string): Promise<RomDetection> {
  return invoke<RomDetection>("rom_detect_type", { filePath });
}

export async function inspectRomZip(filePath: string): Promise<RomAnalysis> {
  return invoke<RomAnalysis>("rom_inspect_zip", { filePath });
}

export async function buildRomFlashPlan(filePath: string): Promise<FlashPlan> {
  return invoke<FlashPlan>("rom_build_flash_plan", { filePath });
}

export async function validateRomPackage(filePath: string): Promise<PackageValidation> {
  return invoke<PackageValidation>("rom_validate_package", { filePath });
}

export async function getRomQueue(): Promise<QueueItem[]> {
  return invoke<QueueItem[]>("rom_get_queue");
}

export async function addRomToQueue(filePath: string): Promise<QueueItem> {
  return invoke<QueueItem>("rom_add_to_queue", { filePath });
}

export async function removeRomFromQueue(queueId: string): Promise<QueueItem[]> {
  return invoke<QueueItem[]>("rom_remove_from_queue", { queueId });
}

export async function clearRomQueue(): Promise<QueueItem[]> {
  return invoke<QueueItem[]>("rom_clear_queue");
}

export async function moveRomQueueItem(fromIndex: number, toIndex: number): Promise<QueueItem[]> {
  return invoke<QueueItem[]>("rom_move_queue_item", { fromIndex, toIndex });
}

export async function toggleRomQueuePartition(
  queueId: string,
  partition: string,
  enabled: boolean,
): Promise<QueueItem> {
  return invoke<QueueItem>("rom_toggle_queue_partition", {
    queueId,
    partition,
    enabled,
  });
}
