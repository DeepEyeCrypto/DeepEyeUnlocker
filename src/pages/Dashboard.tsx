import { invoke } from "@tauri-apps/api/core";
import { useMemo, useState } from "react";
import { WaitingQueuePanel } from "../components/WaitingQueuePanel";
import { useWaitQueue } from "../hooks/useWaitQueue";
import { TerminalLog } from "../components/ui/TerminalLog";
import { ProgressStep, type ProgressItem } from "../components/ui/ProgressStep";
import {
  DASHBOARD_COMMANDS,
  type PlatformTab,
} from "../lib/dashboard";
import type {
  ConnectedDevice,
  DeviceConnectionState,
} from "../lib/devices";
import { isMobile, type Platform } from "../lib/platform";

type ActivationStateResponse = {
  locked?: boolean;
  fmi_enabled?: boolean;
  fmiEnabled?: boolean;
  model?: string;
  chip?: string;
  removal_path?: string;
  removalPath?: string;
};

type DashboardPageProps = {
  platform: Platform | null;
  connectionState: DeviceConnectionState;
  error: string;
  device: ConnectedDevice | null;
  onRefresh: (silent?: boolean) => Promise<void>;
};

type DashboardQueuedCommand = {
  command: string;
  args: Record<string, unknown>;
};

function formatActivationResult(result: ActivationStateResponse): string {
  const locked = result.locked ? "Locked" : "Unlocked";
  const fmiEnabled = result.fmiEnabled ?? result.fmi_enabled ?? false;
  const removalPath = result.removalPath ?? result.removal_path ?? "Unknown";
  return [
    `Activation state: ${locked}`,
    `FMI: ${fmiEnabled ? "enabled" : "disabled"}`,
    `Model: ${result.model ?? "Unknown"}`,
    `Chip: ${result.chip ?? "Unknown"}`,
    `Removal path: ${removalPath}`,
  ].join(" • ");
}

export default function DashboardPage({
  platform,
  connectionState,
  error,
  device,
  onRefresh,
}: DashboardPageProps) {
  const mobile = isMobile();
  const [tab, setTab] = useState<PlatformTab>(platform === "android" ? "android" : "apple");
  const [logLines, setLogLines] = useState<string[]>(["[info] Dashboard initialized"]);
  const [logCollapsed, setLogCollapsed] = useState(true);
  const [activeOperation, setActiveOperation] = useState<string | null>(null);

  const steps = useMemo<ProgressItem[]>(() => {
    return [
      { id: "usb", label: "USB Detect", status: connectionState === "idle" ? "idle" : "done" },
      {
        id: "scan",
        label: "Identity Scan",
        status:
          connectionState === "scanning"
            ? "active"
            : connectionState === "connected"
              ? "done"
              : connectionState === "error"
                ? "error"
                : "idle",
      },
      { id: "ready", label: "Ready", status: connectionState === "connected" ? "done" : "idle" },
    ];
  }, [connectionState]);

  const addLog = (line: string) => {
    setLogLines((prev) => [...prev, line]);
  };

  const {
    waitingOperation,
    waitingElapsedMs,
    enqueueOrRunOperation,
    cancelQueuedOperation,
  } = useWaitQueue<DashboardQueuedCommand>({
    isDeviceCompatible: (connectedDevice) => connectedDevice.source === "apple",
    onQueued: (operation) => {
      addLog(`[queue] Waiting for device... ${operation.name} queued.`);
    },
    onDeviceDetected: (connectedDevice, operation) => {
      addLog(`[queue] Device detected! Starting ${operation.name} on ${connectedDevice.model}...`);
    },
    onError: (message) => {
      addLog(`[error] ${message}`);
      setActiveOperation(null);
    },
    onExecute: async (connectedDevice, operation) => {
      setActiveOperation(operation.name);
      try {
        if (operation.params.command === DASHBOARD_COMMANDS.CHECK_FMI) {
          const enabled = await invoke<boolean>(operation.params.command, { udid: connectedDevice.id });
          addLog(`[info] Find My is ${enabled ? "enabled" : "disabled"}.`);
          return;
        }

        if (operation.params.command === DASHBOARD_COMMANDS.CHECK_ACTIVATION) {
          const result = await invoke<ActivationStateResponse>(operation.params.command, {
            udid: connectedDevice.id,
          });
          addLog(`[info] ${formatActivationResult(result)}`);
          return;
        }

        const args = {
          ...operation.params.args,
          ...(operation.params.command === DASHBOARD_COMMANDS.CHECK_SHSH && !operation.params.args.model
            ? { model: connectedDevice.model }
            : {}),
        };

        const result = await invoke<string>(operation.params.command, args);
        if (result.trim()) {
          addLog(result.trim());
          return;
        }
        addLog(`[info] ${operation.name} completed.`);
      } catch (executeError: unknown) {
        addLog(`[error] ${String(executeError)}`);
      } finally {
        setActiveOperation(null);
      }
    },
  });

  const queueDashboardAction = async (
    name: string,
    command: string,
    args: Record<string, unknown> = {},
  ) => {
    await enqueueOrRunOperation(name, { command, args });
  };

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

      {platform === "android" && (
        <div className="runtime-note">
          Android runtime detected. Desktop Apple probe commands are disabled on device.
        </div>
      )}
      {connectionState === "idle" && <div className="panel pulse-panel">No device connected</div>}
      {connectionState === "scanning" && <div className="panel pulse-panel">Scanning for connected devices...</div>}
      {connectionState === "error" && (
        <div className="danger-note row-between">
          <span>{error || "Connection error"}</span>
          <button className="action-btn" onClick={() => void onRefresh()}>Retry</button>
        </div>
      )}

      <WaitingQueuePanel
        operation={waitingOperation}
        elapsedMs={waitingElapsedMs}
        onCancel={cancelQueuedOperation}
      />

      <div className="quick-grid">
        <button
          className="action-btn"
          disabled={activeOperation !== null}
          title="Check FMI state"
          onClick={() =>
            void queueDashboardAction("Check FMI", DASHBOARD_COMMANDS.CHECK_FMI)
          }
        >
          <span className="action-title">Check FMI</span>
        </button>
        <button
          className="action-btn"
          disabled={activeOperation !== null}
          title="Check SHSH signed versions"
          onClick={() =>
            void queueDashboardAction("Check SHSH", DASHBOARD_COMMANDS.CHECK_SHSH, {
              model: device?.model ?? "",
            })
          }
        >
          <span className="action-title">Check SHSH</span>
        </button>
        <button
          className="action-btn"
          disabled={activeOperation !== null}
          title="Check activation state"
          onClick={() =>
            void queueDashboardAction("Activation", DASHBOARD_COMMANDS.CHECK_ACTIVATION, {
              udid: device?.id ?? "",
            })
          }
        >
          <span className="action-title">Activation</span>
        </button>
        <button
          className="action-btn"
          disabled={activeOperation !== null}
          title="Run restore latest"
          onClick={() =>
            void queueDashboardAction("Restore", DASHBOARD_COMMANDS.RESTORE_LATEST)
          }
        >
          <span className="action-title">Restore</span>
        </button>
      </div>

      <div className="panel">
        <div className="row-between">
          <button
            className="action-btn logs"
            type="button"
            onClick={() => setLogCollapsed((value) => !value)}
          >
            Live Logs
          </button>
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
