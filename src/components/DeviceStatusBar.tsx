import { invoke } from "@tauri-apps/api/core";
import { useEffect, useState } from "react";
import type { NavId } from "./Layout/types";

type DeviceStatusBarProps = {
  onNavigate: (page: NavId) => void;
};

type AdbDevice = {
  serial: string;
};

type SamsungProbe = {
  mode: string;
};

type EdlProbe = {
  programmer_loaded: boolean;
};

type StatusSnapshot = {
  adbCount: number;
  samsungLabel: string;
  edlLabel: string;
};

const INITIAL_STATUS: StatusSnapshot = {
  adbCount: 0,
  samsungLabel: "idle",
  edlLabel: "idle",
};

export function DeviceStatusBar({ onNavigate }: DeviceStatusBarProps) {
  const [status, setStatus] = useState<StatusSnapshot>(INITIAL_STATUS);

  useEffect(() => {
    let cancelled = false;

    const poll = async () => {
      const [adbResult, samsungResult, edlResult] = await Promise.allSettled([
        invoke<AdbDevice[]>("adb_list_devices"),
        invoke<SamsungProbe>("samsung_find_device_cmd"),
        invoke<EdlProbe>("edl_find_device"),
      ]);

      if (cancelled) {
        return;
      }

      const adbCount = adbResult.status === "fulfilled" ? adbResult.value.length : 0;
      const samsungLabel =
        samsungResult.status === "fulfilled"
          ? samsungResult.value.mode.toLowerCase().replace(/_/g, " ")
          : "idle";
      const edlLabel =
        edlResult.status === "fulfilled"
          ? edlResult.value.programmer_loaded
            ? "programmer ready"
            : "connected"
          : "idle";

      setStatus({ adbCount, samsungLabel, edlLabel });
    };

    void poll();
    const timer = window.setInterval(() => {
      void poll();
    }, 3000);

    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, []);

  return (
    <div className="device-status-bar" role="navigation" aria-label="Live device status">
      <button
        type="button"
        className="device-status-bar__item"
        onClick={() => onNavigate("adbtools")}
      >
        <span className="device-status-bar__emoji">🟢</span>
        <span>
          ADB: {status.adbCount > 0 ? `${status.adbCount} device${status.adbCount === 1 ? "" : "s"}` : "idle"}
        </span>
      </button>

      <button
        type="button"
        className="device-status-bar__item"
        onClick={() => onNavigate("samsung")}
      >
        <span className="device-status-bar__emoji">🔵</span>
        <span>Samsung: {status.samsungLabel}</span>
      </button>

      <button
        type="button"
        className="device-status-bar__item"
        onClick={() => onNavigate("guidedfrp")}
      >
        <span className="device-status-bar__emoji">🛡️</span>
        <span>Guided FRP</span>
      </button>

      <button
        type="button"
        className="device-status-bar__item"
        onClick={() => onNavigate("edl")}
      >
        <span className="device-status-bar__emoji">🟣</span>
        <span>EDL: {status.edlLabel}</span>
      </button>
    </div>
  );
}
