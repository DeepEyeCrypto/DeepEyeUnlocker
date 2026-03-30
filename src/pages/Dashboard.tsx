import { invoke } from "@tauri-apps/api/core";
import { useEffect, useMemo, useState } from "react";
import { DeviceCard } from "../components/DeviceCard";
import { TerminalLog } from "../components/ui/TerminalLog";
import { ProgressStep, type ProgressItem } from "../components/ui/ProgressStep";
import {
  DASHBOARD_COMMANDS,
  DASHBOARD_CONFIG,
  type DashboardState,
  type DetectedDeviceInfo,
  type PlatformTab,
} from "../lib/dashboard";
import { isMobile } from "../lib/platform";

type IdentityResponse = {
  model?: string;
  version?: string;
  mode?: string;
  udid?: string;
};

type OrchestratorResponse = { mode: string };

export default function DashboardPage() {
  const mobile = isMobile();
  const [state, setState] = useState<DashboardState>("idle");
  const [error, setError] = useState<string>("");
  const [tab, setTab] = useState<PlatformTab>("apple");
  const [device, setDevice] = useState<DetectedDeviceInfo | null>(null);
  const [logLines, setLogLines] = useState<string[]>(["[info] Dashboard initialized"]);
  const [logCollapsed, setLogCollapsed] = useState(true);

  const steps = useMemo<ProgressItem[]>(() => {
    return [
      { id: "usb", label: "USB Detect", status: state === "idle" ? "idle" : "done" },
      { id: "scan", label: "Identity Scan", status: state === "scanning" ? "active" : state === "connected" ? "done" : state === "error" ? "error" : "idle" },
      { id: "ready", label: "Ready", status: state === "connected" ? "done" : state === "unsupported" ? "error" : "idle" },
    ];
  }, [state]);

  const addLog = (line: string) => {
    setLogLines((prev) => [...prev, line]);
  };

  const poll = async () => {
    try {
      setState("scanning");
      setError("");
      addLog("[info] Scanning USB...");

      const modeResult = await invoke<OrchestratorResponse>(DASHBOARD_COMMANDS.POLL_ORCHESTRATOR);
      const identity = await invoke<IdentityResponse>(DASHBOARD_COMMANDS.DEVICE_IDENTITY, { udid: "" });

      const mode = (modeResult.mode || identity.mode || "unknown").toLowerCase();
      if (mode.includes("unknown") || mode.includes("unsupported")) {
        setState("unsupported");
        setDevice(null);
        addLog("[info] Device unsupported");
        return;
      }

      setDevice({
        model: identity.model || "Unknown Device",
        serial: identity.udid || "N/A",
        os: identity.version || "Unknown OS",
        mode: modeResult.mode,
        bootloaderStatus: "Unknown",
        carrier: "N/A",
      });
      setState("connected");
      addLog("[info] Device connected");
    } catch (e: unknown) {
      setState("error");
      setError(String(e));
      setDevice(null);
      addLog(`[info] Detection error: ${String(e)}`);
    }
  };

  useEffect(() => {
    void poll();
    const timer = setInterval(() => {
      void poll();
    }, DASHBOARD_CONFIG.POLLING_INTERVAL_MS);
    return () => clearInterval(timer);
  }, []);

  const canRunActions = state === "connected";

  return (
    <div className="page">
      <div className="row-between">
        <h2 className="page-title">Dashboard</h2>
        <div className="tab-row" role="tablist" aria-label="Platform tabs">
          <button className={`btn btn-sm ${tab === "android" ? "btn-primary" : "btn-ghost"}`} onClick={() => setTab("android")}>Android</button>
          <button className={`btn btn-sm ${tab === "apple" ? "btn-primary" : "btn-ghost"}`} onClick={() => setTab("apple")}>Apple</button>
          <button className={`btn btn-sm ${tab === "tools" ? "btn-primary" : "btn-ghost"}`} onClick={() => setTab("tools")}>Tools</button>
        </div>
      </div>

      <ProgressStep steps={steps} />

      {state === "idle" && <div className="panel">Connect a device to begin.</div>}
      {state === "scanning" && <div className="panel pulse-panel">Scanning USB...</div>}
      {state === "error" && (
        <div className="danger-note row-between">
          <span>{error || "Connection error"}</span>
          <button className="btn btn-danger btn-sm" onClick={() => void poll()}>Retry</button>
        </div>
      )}
      {state === "unsupported" && <div className="panel">Device not supported yet.</div>}
      {state === "connected" && device && (
        <DeviceCard
          device={{
            status: "connected",
            model: device.model,
            serial: device.serial,
            os: device.os,
            mode: device.mode,
            bootloaderStatus: device.bootloaderStatus,
            carrier: device.carrier,
          }}
        />
      )}

      <div className="quick-grid">
        <button
          className="action-card"
          disabled={!canRunActions}
          title="Check FMI state"
          onClick={async () => addLog(await invoke<string>(DASHBOARD_COMMANDS.CHECK_FMI))}
        >
          <span className="action-title">Check FMI</span>
        </button>
        <button
          className="action-card"
          disabled={!canRunActions}
          title="Check SHSH signed versions"
          onClick={async () => addLog(await invoke<string>(DASHBOARD_COMMANDS.CHECK_SHSH, { model: device?.model || "" }))}
        >
          <span className="action-title">Check SHSH</span>
        </button>
        <button
          className="action-card"
          disabled={!canRunActions}
          title="Check activation state"
          onClick={async () => addLog(await invoke<string>(DASHBOARD_COMMANDS.CHECK_ACTIVATION))}
        >
          <span className="action-title">Activation</span>
        </button>
        <button
          className="action-card"
          disabled={!canRunActions}
          title="Run restore latest"
          onClick={async () => addLog(await invoke<string>(DASHBOARD_COMMANDS.RESTORE_LATEST))}
        >
          <span className="action-title">Restore</span>
        </button>
      </div>

      <div className="panel">
        <div className="row-between">
          <div className="action-title">Live Logs</div>
          <div className="row-between">
            <button className="btn btn-ghost btn-sm" onClick={() => setLogLines([])}>Clear</button>
            <button className="btn btn-ghost btn-sm" onClick={() => navigator.clipboard.writeText(logLines.join("\n"))}>Copy All</button>
            {mobile && (
              <button className="btn btn-ghost btn-sm" onClick={() => setLogCollapsed((v) => !v)}>
                {logCollapsed ? "Expand" : "Collapse"}
              </button>
            )}
          </div>
        </div>
        {(!mobile || !logCollapsed) && <TerminalLog lines={logLines} />}
      </div>
    </div>
  );
}

