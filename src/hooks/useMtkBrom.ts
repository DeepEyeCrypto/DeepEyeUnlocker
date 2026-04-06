import { invoke } from "@tauri-apps/api/core";
import { useCallback, useState } from "react";

export type MtkMode = "Brom" | "Preloader";
export type AuthType = "None" | "Sla" | "Daa" | "SlaDaa";

export interface MtkDevice {
  mode: MtkMode;
  vid: number;
  pid: number;
  bus: number;
  address: number;
}

export interface ChipInfo {
  hw_code: number;
  chip_name: string;
  hw_sub_code: number;
  hw_ver: number;
  sw_ver: number;
}

export interface DaUploadResult {
  bytes_uploaded: number;
  checksum: number;
}

export interface DaJumpInfo {
  status: number;
  version: number;
  capability: number;
}

export interface ImeiInfo {
  imei1: string;
  imei2: string | null;
}

export interface PartitionEntry {
  name: string;
  start: number;
  size: number;
  partition_type: number;
}

export type BromStatus =
  | "idle"
  | "detecting"
  | "handshaking"
  | "identifying"
  | "identified"
  | "detecting-auth"
  | "auth-detected"
  | "bypassing-sla"
  | "sla-bypassed"
  | "uploading-da"
  | "da-uploaded"
  | "jumping-da"
  | "da-ready"
  | "erasing-frp"
  | "frp-erased"
  | "formatting"
  | "formatted"
  | "reading-imei"
  | "imei-read"
  | "writing-imei"
  | "imei-written"
  | "rebooting"
  | "rebooted"
  | "listing-partitions"
  | "partitions-listed"
  | "reading-partition"
  | "partition-read"
  | "writing-partition"
  | "partition-written"
  | "dumping-preloader"
  | "preloader-dumped"
  | "erasing-partition"
  | "partition-erased"
  | "error";

async function runCommand<T>(cmd: string, args: Record<string, unknown> = {}): Promise<T> {
  try {
    return await invoke<T>(cmd, args);
  } catch (error) {
    throw typeof error === "string" ? new Error(error) : error;
  }
}

export function useMtkBrom() {
  const [chipInfo, setChipInfo] = useState<ChipInfo | null>(null);
  const [authType, setAuthType] = useState<AuthType | null>(null);
  const [daUploadResult, setDaUploadResult] = useState<DaUploadResult | null>(null);
  const [daJumpInfo, setDaJumpInfo] = useState<DaJumpInfo | null>(null);
  const [imeiInfo, setImeiInfo] = useState<ImeiInfo | null>(null);
  const [partitions, setPartitions] = useState<PartitionEntry[]>([]);
  const [bromStatus, setBromStatus] = useState<BromStatus>("idle");
  const [error, setError] = useState<string | null>(null);

  const detect = useCallback(async () => {
    setError(null);
    setChipInfo(null);
    setAuthType(null);
    setDaUploadResult(null);
    setDaJumpInfo(null);
    setImeiInfo(null);
    setPartitions([]);
    setBromStatus("detecting");

    try {
      await runCommand<MtkDevice>("mtk_detect_device");
      setBromStatus("handshaking");

      setBromStatus("identifying");
      const info = await runCommand<ChipInfo>("mtk_handshake_and_identify");
      setChipInfo(info);

      setBromStatus("detecting-auth");
      const detectedAuthType = await runCommand<AuthType>("mtk_detect_auth_type");
      setAuthType(detectedAuthType);
      setBromStatus(detectedAuthType === "None" ? "identified" : "auth-detected");

      return { chipInfo: info, authType: detectedAuthType };
    } catch (detectError) {
      const message = detectError instanceof Error ? detectError.message : String(detectError);
      setError(message);
      setBromStatus("error");
      throw detectError;
    }
  }, []);

  const detectAuth = useCallback(async () => {
    setError(null);
    setBromStatus("detecting-auth");
    try {
      const detectedAuthType = await runCommand<AuthType>("mtk_detect_auth_type");
      setAuthType(detectedAuthType);
      setBromStatus(detectedAuthType === "None" ? "identified" : "auth-detected");
      return detectedAuthType;
    } catch (authError) {
      const message = authError instanceof Error ? authError.message : String(authError);
      setError(message);
      setBromStatus("error");
      throw authError;
    }
  }, []);

  const bypassSla = useCallback(async () => {
    setError(null);
    setBromStatus("bypassing-sla");
    try {
      await runCommand<void>("mtk_bypass_sla");
      setBromStatus("sla-bypassed");
    } catch (bypassError) {
      const message = bypassError instanceof Error ? bypassError.message : String(bypassError);
      setError(message);
      setBromStatus("error");
      throw bypassError;
    }
  }, []);

  const uploadDa = useCallback(async (daPath: string) => {
    if (!daPath.trim()) {
      const pathError = new Error("DA path cannot be empty");
      setError(pathError.message);
      setBromStatus("error");
      throw pathError;
    }
    setError(null);
    setDaUploadResult(null);
    setDaJumpInfo(null);
    setBromStatus("uploading-da");
    try {
      const result = await runCommand<DaUploadResult>("mtk_upload_da", { daPath });
      setDaUploadResult(result);
      setBromStatus("da-uploaded");
      return result;
    } catch (uploadError) {
      const message = uploadError instanceof Error ? uploadError.message : String(uploadError);
      setError(message);
      setBromStatus("error");
      throw uploadError;
    }
  }, []);

  const jumpToDa = useCallback(async () => {
    setError(null);
    setDaJumpInfo(null);
    setBromStatus("jumping-da");
    try {
      const result = await runCommand<DaJumpInfo>("mtk_jump_to_da");
      setDaJumpInfo(result);
      setBromStatus("da-ready");
      return result;
    } catch (jumpError) {
      const message = jumpError instanceof Error ? jumpError.message : String(jumpError);
      setError(message);
      setBromStatus("error");
      throw jumpError;
    }
  }, []);

  // ── Day 3: DA-level operations ──────────────────────────────────────────

  const eraseFrp = useCallback(async () => {
    setError(null);
    setBromStatus("erasing-frp");
    try {
      await runCommand<void>("mtk_erase_frp");
      setBromStatus("frp-erased");
    } catch (frpError) {
      const message = frpError instanceof Error ? frpError.message : String(frpError);
      setError(message);
      setBromStatus("error");
      throw frpError;
    }
  }, []);

  const formatUserdata = useCallback(async () => {
    setError(null);
    setBromStatus("formatting");
    try {
      await runCommand<void>("mtk_format_userdata");
      setBromStatus("formatted");
    } catch (formatError) {
      const message = formatError instanceof Error ? formatError.message : String(formatError);
      setError(message);
      setBromStatus("error");
      throw formatError;
    }
  }, []);

  const readImei = useCallback(async () => {
    setError(null);
    setImeiInfo(null);
    setBromStatus("reading-imei");
    try {
      const result = await runCommand<ImeiInfo>("mtk_read_imei");
      setImeiInfo(result);
      setBromStatus("imei-read");
      return result;
    } catch (imeiError) {
      const message = imeiError instanceof Error ? imeiError.message : String(imeiError);
      setError(message);
      setBromStatus("error");
      throw imeiError;
    }
  }, []);

  const writeImei = useCallback(async (imei1: string, imei2?: string) => {
    setError(null);
    setBromStatus("writing-imei");
    try {
      await runCommand<void>("mtk_write_imei", { imei1, imei2: imei2 ?? null });
      setBromStatus("imei-written");
    } catch (writeError) {
      const message = writeError instanceof Error ? writeError.message : String(writeError);
      setError(message);
      setBromStatus("error");
      throw writeError;
    }
  }, []);

  const reboot = useCallback(async (mode: "normal" | "recovery" | "fastboot") => {
    const modeMap: Record<string, number> = { normal: 0, recovery: 1, fastboot: 2 };
    setError(null);
    setBromStatus("rebooting");
    try {
      await runCommand<void>("mtk_reboot", { mode: modeMap[mode] ?? 0 });
      setBromStatus("rebooted");
    } catch (rebootError) {
      const message = rebootError instanceof Error ? rebootError.message : String(rebootError);
      setError(message);
      setBromStatus("error");
      throw rebootError;
    }
  }, []);

  // ── Day 4: Partition operations ─────────────────────────────────────────

  const listPartitions = useCallback(async () => {
    setError(null);
    setPartitions([]);
    setBromStatus("listing-partitions");
    try {
      const result = await runCommand<PartitionEntry[]>("mtk_list_partitions");
      setPartitions(result);
      setBromStatus("partitions-listed");
      return result;
    } catch (listError) {
      const message = listError instanceof Error ? listError.message : String(listError);
      setError(message);
      setBromStatus("error");
      throw listError;
    }
  }, []);

  const readPartition = useCallback(
    async (name: string, offset: number, length: number, outPath: string) => {
      setError(null);
      setBromStatus("reading-partition");
      try {
        const bytes = await runCommand<number>("mtk_da_read_partition", {
          name, offset, length, outPath,
        });
        setBromStatus("partition-read");
        return bytes;
      } catch (readError) {
        const message = readError instanceof Error ? readError.message : String(readError);
        setError(message);
        setBromStatus("error");
        throw readError;
      }
    },
    [],
  );

  const writePartition = useCallback(
    async (name: string, offset: number, dataPath: string) => {
      setError(null);
      setBromStatus("writing-partition");
      try {
        await runCommand<void>("mtk_da_write_partition", { name, offset, dataPath });
        setBromStatus("partition-written");
      } catch (writeError) {
        const message = writeError instanceof Error ? writeError.message : String(writeError);
        setError(message);
        setBromStatus("error");
        throw writeError;
      }
    },
    [],
  );

  const dumpPreloader = useCallback(async (outPath: string) => {
    setError(null);
    setBromStatus("dumping-preloader");
    try {
      const bytes = await runCommand<number>("mtk_dump_preloader", { outPath });
      setBromStatus("preloader-dumped");
      return bytes;
    } catch (dumpError) {
      const message = dumpError instanceof Error ? dumpError.message : String(dumpError);
      setError(message);
      setBromStatus("error");
      throw dumpError;
    }
  }, []);

  const erasePartition = useCallback(async (name: string) => {
    setError(null);
    setBromStatus("erasing-partition");
    try {
      await runCommand<void>("mtk_da_erase_partition", { name });
      setBromStatus("partition-erased");
    } catch (eraseError) {
      const message = eraseError instanceof Error ? eraseError.message : String(eraseError);
      setError(message);
      setBromStatus("error");
      throw eraseError;
    }
  }, []);

  return {
    chipInfo,
    authType,
    daUploadResult,
    daJumpInfo,
    imeiInfo,
    partitions,
    bromStatus,
    error,
    detect,
    detectAuth,
    bypassSla,
    uploadDa,
    jumpToDa,
    eraseFrp,
    formatUserdata,
    readImei,
    writeImei,
    reboot,
    listPartitions,
    readPartition,
    writePartition,
    dumpPreloader,
    erasePartition,
  };
}
