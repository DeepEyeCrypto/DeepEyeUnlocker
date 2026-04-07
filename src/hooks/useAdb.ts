import { invoke } from "@tauri-apps/api/core";
import { useState, useCallback } from "react";

export type AdbStatus =
  | "idle"
  | "scanning"
  | "connected"
  | "executing"
  | "installing"
  | "pushing"
  | "pulling"
  | "rebooting"
  | "sideloading"
  | "erasing_frp"
  | "error";

export interface AdbDevice {
  serial: string;
  state: string;
  model: string;
  android_version: string;
  sdk_version: string;
}

export interface DeviceFullInfo {
  serial: string;
  model: string;
  brand: string;
  android_version: string;
  sdk_int: string;
  build_id: string;
  security_patch: string;
  bootloader_status: string;
  root_status: boolean;
  frp_status: string;
  battery_level: string;
  imei: string;
}

export function useAdb() {
  const [devices, setDevices] = useState<AdbDevice[]>([]);
  const [selectedSerial, setSelectedSerial] = useState<string | null>(null);
  const [deviceInfo, setDeviceInfo] = useState<DeviceFullInfo | null>(null);
  const [adbStatus, setAdbStatus] = useState<AdbStatus>("idle");
  const [error, setError] = useState<string | null>(null);

  const wrap = async <T>(
    status: AdbStatus,
    fn: () => Promise<T>
  ): Promise<T> => {
    setAdbStatus(status);
    setError(null);
    try {
      const result = await fn();
      setAdbStatus("connected");
      return result;
    } catch (e: any) {
      setError(e?.toString() ?? "Unknown error");
      setAdbStatus("error");
      throw e;
    }
  };

  const scanDevices = useCallback(async () => {
    const devs = await wrap("scanning", () =>
      invoke<AdbDevice[]>("adb_list_devices")
    );
    setDevices(devs);
    if (devs.length > 0 && !selectedSerial) {
      setSelectedSerial(devs[0].serial);
    }
  }, [selectedSerial]);

  const selectDevice = useCallback(
    (serial: string) => setSelectedSerial(serial),
    []
  );

  const getDeviceInfo = useCallback(async () => {
    if (!selectedSerial) return;
    const info = await wrap("executing", () =>
      invoke<DeviceFullInfo>("adb_get_full_info", { serial: selectedSerial })
    );
    setDeviceInfo(info);
  }, [selectedSerial]);

  const shellCommand = useCallback(
    async (cmd: string): Promise<string> => {
      if (!selectedSerial) throw new Error("No device selected");
      return wrap("executing", () =>
        invoke<string>("adb_shell_command", { serial: selectedSerial, cmd })
      );
    },
    [selectedSerial]
  );

  const rebootDevice = useCallback(
    async (mode: string) => {
      if (!selectedSerial) return;
      await wrap("rebooting", () =>
        invoke("adb_reboot_device", { serial: selectedSerial, mode })
      );
    },
    [selectedSerial]
  );

  const installApk = useCallback(
    async (apkPath: string) => {
      if (!selectedSerial) return;
      await wrap("installing", () =>
        invoke("adb_install_apk", { serial: selectedSerial, apkPath })
      );
    },
    [selectedSerial]
  );

  const pushFile = useCallback(
    async (local: string, remote: string) => {
      if (!selectedSerial) return;
      await wrap("pushing", () =>
        invoke("adb_push_file", { serial: selectedSerial, local, remote })
      );
    },
    [selectedSerial]
  );

  const pullFile = useCallback(
    async (remote: string, local: string) => {
      if (!selectedSerial) return;
      await wrap("pulling", () =>
        invoke("adb_pull_file", { serial: selectedSerial, remote, local })
      );
    },
    [selectedSerial]
  );

  const sideloadZip = useCallback(
    async (zipPath: string) => {
      if (!selectedSerial) return;
      await wrap("sideloading", () =>
        invoke("adb_sideload_zip", { serial: selectedSerial, zip_path: zipPath })
      );
    },
    [selectedSerial]
  );

  const eraseFrp = useCallback(async () => {
    if (!selectedSerial) return;
    await wrap("erasing_frp", () =>
      invoke("adb_erase_frp_partition", { serial: selectedSerial })
    );
  }, [selectedSerial]);

  const checkRoot = useCallback(async (): Promise<boolean> => {
    if (!selectedSerial) return false;
    return wrap("executing", () =>
      invoke<boolean>("adb_check_root_access", { serial: selectedSerial })
    );
  }, [selectedSerial]);

  return {
    devices,
    selectedSerial,
    deviceInfo,
    adbStatus,
    error,
    scanDevices,
    selectDevice,
    getDeviceInfo,
    shellCommand,
    rebootDevice,
    installApk,
    pushFile,
    pullFile,
    sideloadZip,
    eraseFrp,
    checkRoot,
  };
}
