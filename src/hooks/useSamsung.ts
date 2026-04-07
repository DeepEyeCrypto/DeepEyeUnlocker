import { invoke } from "@tauri-apps/api/core";
import { useState, useCallback } from "react";

export type SamsungStatus =
  | "idle"
  | "detecting"
  | "detected"
  | "handshaking"
  | "reading_pit"
  | "flashing"
  | "erasing_frp"
  | "rebooting"
  | "error";

export interface SamsungDevice {
  vid: number;
  pid: number;
  model: string;
  mode: string;
}

export interface OdinInfo {
  protocol_version: string;
  pit_size: number;
}

export interface PitEntry {
  partition_name: string;
  flash_filename: string;
  partition_id: number;
  partition_type: number;
  device_type: number;
  offset: number;
  size: number;
}

export function useSamsung() {
  const [device, setDevice] = useState<SamsungDevice | null>(null);
  const [odinInfo, setOdinInfo] = useState<OdinInfo | null>(null);
  const [pitEntries, setPitEntries] = useState<PitEntry[]>([]);
  const [samsungStatus, setSamsungStatus] = useState<SamsungStatus>("idle");
  const [flashProgress, setFlashProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [log, setLog] = useState<string[]>([]);

  const addLog = (msg: string) =>
    setLog((prev) => [...prev, `[${new Date().toLocaleTimeString()}] ${msg}`]);

  const wrap = async <T>(
    status: SamsungStatus,
    fn: () => Promise<T>,
    logMsg?: string
  ): Promise<T> => {
    setSamsungStatus(status);
    setError(null);
    if (logMsg) addLog(logMsg);
    try {
      const result = await fn();
      setSamsungStatus("detected");
      return result;
    } catch (e: any) {
      const msg = e?.toString() ?? "Unknown error";
      setError(msg);
      addLog(`ERROR: ${msg}`);
      setSamsungStatus("error");
      throw e;
    }
  };

  const detectDevice = useCallback(async () => {
    const dev = await wrap(
      "detecting",
      () => invoke<SamsungDevice>("samsung_find_device_cmd"),
      "Scanning for Samsung device (VID 0x04e8)..."
    );
    setDevice(dev);
    addLog(`Found: PID 0x${dev.pid.toString(16)} — ${dev.mode}`);
  }, []);

  const doHandshake = useCallback(async () => {
    const info = await wrap(
      "handshaking",
      () => invoke<OdinInfo>("samsung_do_handshake_cmd"),
      "Odin handshake → ODIN/LOKE..."
    );
    setOdinInfo(info);
    addLog(`Protocol: ${info.protocol_version} | PIT size: ${info.pit_size}B`);
  }, []);

  const readPit = useCallback(async () => {
    const entries = await wrap(
      "reading_pit",
      () => invoke<PitEntry[]>("samsung_get_pit_cmd"),
      "Reading PIT table..."
    );
    setPitEntries(entries);
    addLog(`PIT read: ${entries.length} partitions`);
  }, []);

  const flashPartition = useCallback(
    async (name: string, filePath: string) => {
      setFlashProgress(0);
      addLog(`Flashing ${name} ← ${filePath}`);
      await wrap("flashing", () =>
        invoke("samsung_flash_part_cmd", { name, filePath })
      );
      setFlashProgress(100);
      addLog(`Flash complete: ${name}`);
    },
    []
  );

  const eraseFrp = useCallback(async () => {
    await wrap(
      "erasing_frp",
      () => invoke("samsung_do_erase_frp_cmd"),
      "Erasing FRP partition..."
    );
    addLog("FRP erased ✅");
  }, []);

  const reboot = useCallback(async (mode: number) => {
    const labels = ["Normal", "Download", "", "Recovery"];
    await wrap(
      "rebooting",
      () => invoke("samsung_reboot_device_cmd", { mode }),
      `Rebooting → ${labels[mode] ?? mode}`
    );
  }, []);

  return {
    device,
    odinInfo,
    pitEntries,
    samsungStatus,
    flashProgress,
    error,
    log,
    detectDevice,
    doHandshake,
    readPit,
    flashPartition,
    eraseFrp,
    reboot,
  };
}
