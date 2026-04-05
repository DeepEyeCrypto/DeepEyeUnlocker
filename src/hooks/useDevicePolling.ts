import { useCallback, useEffect, useRef, useState } from "react";
import {
  fetchConnectedDevices,
  type ConnectedDevice,
  type DeviceConnectionState,
} from "../lib/devices";

export function useDevicePolling(pollIntervalMs = 2000) {
  const [devices, setDevices] = useState<ConnectedDevice[]>([]);
  const [state, setState] = useState<DeviceConnectionState>("idle");
  const [error, setError] = useState("");
  const [logs, setLogs] = useState<string[]>(["[info] Waiting for connected devices"]);
  const previousPrimaryIdRef = useRef<string | null>(null);

  const appendLog = useCallback((line: string) => {
    setLogs((previous) => {
      if (previous[previous.length - 1] === line) {
        return previous;
      }
      return [...previous, line];
    });
  }, []);

  const refresh = useCallback(
    async (silent = false) => {
      if (!silent) {
        setState("scanning");
      }

      try {
        const nextDevices = await fetchConnectedDevices();
        setDevices(nextDevices);
        setError("");

        const nextPrimary = nextDevices[0] ?? null;
        const previousPrimaryId = previousPrimaryIdRef.current;

        if (nextPrimary) {
          setState("connected");
          if (previousPrimaryId !== nextPrimary.id) {
            appendLog(
              `[info] Device detected: ${nextPrimary.model} · ${nextPrimary.mode} · ${nextPrimary.source}`,
            );
          }
          previousPrimaryIdRef.current = nextPrimary.id;
          return;
        }

        setState("idle");
        if (previousPrimaryId !== null) {
          appendLog("[info] All devices disconnected");
        }
        previousPrimaryIdRef.current = null;
      } catch (pollError: unknown) {
        setState("error");
        setDevices([]);
        const message = String(pollError);
        setError(message);
        appendLog(`[error] ${message}`);
      }
    },
    [appendLog],
  );

  useEffect(() => {
    void refresh();

    const timer = window.setInterval(() => {
      void refresh(true);
    }, pollIntervalMs);

    return () => window.clearInterval(timer);
  }, [pollIntervalMs, refresh]);

  return {
    devices,
    primaryDevice: devices[0] ?? null,
    state,
    error,
    logs,
    refresh,
    appendLog,
  };
}

