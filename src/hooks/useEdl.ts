import { useState, useCallback } from 'react';
import { invoke } from '@tauri-apps/api/core';

export type EdlStatus =
  | 'idle'
  | 'detecting'
  | 'detected'
  | 'sahara_handshake'
  | 'uploading_programmer'
  | 'programmer_ready'
  | 'configuring'
  | 'erasing'
  | 'reading'
  | 'writing'
  | 'rebooting'
  | 'error';

export interface EdlDeviceInfo {
  vid: number;
  pid: number;
  serial: string | null;
  programmer_loaded: boolean;
}

export interface SaharaInfo {
  version: number;
  mode: number;
  max_packet_size: number;
}

export interface StorageInfo {
  total_blocks: number;
  block_size: number;
  storage_type: string;
}

export interface UseEdlReturn {
  deviceInfo: EdlDeviceInfo | null;
  saharaInfo: SaharaInfo | null;
  storageInfo: StorageInfo | null;
  edlStatus: EdlStatus;
  error: string | null;
  detect: () => Promise<void>;
  saharaHandshake: () => Promise<void>;
  uploadProgrammer: (path: string) => Promise<void>;
  configure: (maxPayload: number, sectorSize: number) => Promise<void>;
  erasePartition: (name: string) => Promise<void>;
  readPartition: (name: string, sectors: number, outPath: string) => Promise<void>;
  writePartition: (name: string, dataPath: string) => Promise<void>;
  getStorageInfo: () => Promise<void>;
  reboot: (mode: string) => Promise<void>;
}

export function useEdl(): UseEdlReturn {
  const [deviceInfo, setDeviceInfo] = useState<EdlDeviceInfo | null>(null);
  const [saharaInfo, setSaharaInfo] = useState<SaharaInfo | null>(null);
  const [storageInfo, setStorageInfo] = useState<StorageInfo | null>(null);
  const [edlStatus, setEdlStatus] = useState<EdlStatus>('idle');
  const [error, setError] = useState<string | null>(null);

  const handleError = (e: unknown, defaultMsg: string) => {
    const errorMsg = e instanceof Error ? e.message : String(e);
    setError(`${defaultMsg}: ${errorMsg}`);
    setEdlStatus('error');
    throw new Error(errorMsg);
  };

  const detect = useCallback(async () => {
    try {
      setEdlStatus('detecting');
      setError(null);
      const info = await invoke<EdlDeviceInfo>('edl_find_device');
      setDeviceInfo(info);
      setEdlStatus('detected');
    } catch (e) {
      handleError(e, 'Failed to detect EDL device');
    }
  }, []);

  const saharaHandshake = useCallback(async () => {
    try {
      setEdlStatus('sahara_handshake');
      setError(null);
      const info = await invoke<SaharaInfo>('edl_sahara_handshake');
      setSaharaInfo(info);
      setEdlStatus('idle'); // Or 'waiting' depending on architecture
    } catch (e) {
      handleError(e, 'Sahara handshake failed');
    }
  }, []);

  const uploadProgrammer = useCallback(async (path: string) => {
    try {
      setEdlStatus('uploading_programmer');
      setError(null);
      await invoke<void>('edl_upload_programmer', { path });
      setEdlStatus('programmer_ready');
    } catch (e) {
      handleError(e, 'Programmer upload failed');
    }
  }, []);

  const configure = useCallback(async (maxPayload: number, sectorSize: number) => {
    try {
      setEdlStatus('configuring');
      setError(null);
      await invoke<void>('edl_configure', { maxPayload, sectorSize });
      setEdlStatus('programmer_ready');
    } catch (e) {
      handleError(e, 'Firehose configuration failed');
    }
  }, []);

  const erasePartition = useCallback(async (name: string) => {
    try {
      setEdlStatus('erasing');
      setError(null);
      await invoke<void>('edl_erase_partition', { name });
      setEdlStatus('programmer_ready');
    } catch (e) {
      handleError(e, `Failed to erase partition ${name}`);
    }
  }, []);

  const readPartition = useCallback(async (name: string, sectors: number, outPath: string) => {
    try {
      setEdlStatus('reading');
      setError(null);
      await invoke<void>('edl_read_partition', { name, sectors, outPath });
      setEdlStatus('programmer_ready');
    } catch (e) {
      handleError(e, `Failed to read partition ${name}`);
    }
  }, []);

  const writePartition = useCallback(async (name: string, dataPath: string) => {
    try {
      setEdlStatus('writing');
      setError(null);
      await invoke<void>('edl_write_partition', { name, dataPath });
      setEdlStatus('programmer_ready');
    } catch (e) {
      handleError(e, `Failed to write partition ${name}`);
    }
  }, []);

  const getStorageInfo = useCallback(async () => {
    try {
      setError(null);
      const info = await invoke<StorageInfo>('edl_get_storage_info');
      setStorageInfo(info);
    } catch (e) {
      handleError(e, 'Failed to get storage info');
    }
  }, []);

  const reboot = useCallback(async (mode: string) => {
    try {
      setEdlStatus('rebooting');
      setError(null);
      await invoke<void>('edl_reboot', { mode });
      setEdlStatus('idle');
    } catch (e) {
      handleError(e, 'Failed to reboot device');
    }
  }, []);

  return {
    deviceInfo,
    saharaInfo,
    storageInfo,
    edlStatus,
    error,
    detect,
    saharaHandshake,
    uploadProgrammer,
    configure,
    erasePartition,
    readPartition,
    writePartition,
    getStorageInfo,
    reboot,
  };
}
