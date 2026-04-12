import { invoke } from "@tauri-apps/api/core";
import { useMemo, useState } from "react";
import { WaitingQueuePanel } from "../components/WaitingQueuePanel";
import { useWaitQueue } from "../hooks/useWaitQueue";
import { TerminalLog } from "../components/ui/TerminalLog";
import { ProgressStep, type ProgressItem } from "../components/ui/ProgressStep";
import { SpotlightFeatureCard } from "../components/ui/spotlight-feature-card";
import { Shield, Smartphone, Settings, Zap } from "lucide-react";
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
  const [_activeOperation, setActiveOperation] = useState<string | null>(null);

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

      <div className="card glass glass-hover" style={{ padding: '1.25rem', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '1rem' }}>
        <div style={{ padding: '1rem', background: 'rgba(59, 130, 246, 0.1)', borderRadius: '25%', color: '#3b82f6' }}>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="4" y="4" width="16" height="16" rx="2" ry="2" />
            <rect x="9" y="9" width="6" height="6" />
            <line x1="9" y1="1" x2="9" y2="4" />
            <line x1="15" y1="1" x2="15" y2="4" />
            <line x1="9" y1="20" x2="9" y2="23" />
            <line x1="15" y1="20" x2="15" y2="23" />
            <line x1="20" y1="9" x2="23" y2="9" />
            <line x1="20" y1="14" x2="23" y2="14" />
            <line x1="1" y1="9" x2="4" y2="9" />
            <line x1="1" y1="14" x2="4" y2="14" />
          </svg>
        </div>
        <div>
          <h3 style={{ margin: 0, fontWeight: 600, fontSize: '1.125rem' }}>1,879 Supported Devices</h3>
          <p style={{ margin: '0.25rem 0 0 0', color: 'var(--text-2)', fontSize: '0.875rem' }}>
            483 Full BROM · 649 Partial · 412 EDL
          </p>
        </div>
      </div>

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

      {/* Spotlight Feature Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <SpotlightFeatureCard
          icon={<Shield className="w-6 h-6 text-cyan-400" />}
          title="Check FMI"
          description="Check Find My iPhone status and activation state"
          glowColor="blue"
          onClick={() => queueDashboardAction("Check FMI", DASHBOARD_COMMANDS.CHECK_FMI)}
        />
        
        <SpotlightFeatureCard
          icon={<Smartphone className="w-6 h-6 text-purple-400" />}
          title="Check SHSH"
          description="Verify signed iOS versions for your device"
          glowColor="purple"
          onClick={() => queueDashboardAction("Check SHSH", DASHBOARD_COMMANDS.CHECK_SHSH, {
            model: device?.model ?? "",
          })}
        />
        
        <SpotlightFeatureCard
          icon={<Settings className="w-6 h-6 text-green-400" />}
          title="Activation"
          description="Check device activation state and FMI status"
          glowColor="green"
          onClick={() => queueDashboardAction("Activation", DASHBOARD_COMMANDS.CHECK_ACTIVATION, {
            udid: device?.id ?? "",
          })}
        />
        
        <SpotlightFeatureCard
          icon={<Zap className="w-6 h-6 text-orange-400" />}
          title="Restore"
          description="Restore device from latest backup"
          glowColor="orange"
          onClick={() => queueDashboardAction("Restore", DASHBOARD_COMMANDS.RESTORE_LATEST)}
        />
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
