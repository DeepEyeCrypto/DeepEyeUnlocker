import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { WaitingQueuePanel } from "../WaitingQueuePanel";
import Terminal from "../Terminal";
import { useWaitQueue } from "../../hooks/useWaitQueue";

type ActivationStateResponse = {
  locked: boolean;
  fmi_enabled: boolean;
  apple_id_bound?: string | null;
  removal_path: string;
  model: string;
  chip: string;
};

type ActivationCommand = {
  command: string;
  args: Record<string, unknown>;
};

function formatActivationState(result: ActivationStateResponse): string {
  return [
    `Model: ${result.model}`,
    `Chip: ${result.chip}`,
    `Locked: ${result.locked ? "Yes" : "No"}`,
    `Find My: ${result.fmi_enabled ? "Enabled" : "Disabled"}`,
    `Removal Path: ${result.removal_path}`,
  ].join("\n");
}

export default function ActivationPage() {
  const [output, setOutput] = useState("");
  const [status, setStatus] = useState<"idle"|"running"|"success"|"error">("idle");
  const [deviceState, setDeviceState] = useState<string>("unknown");

  const {
    waitingOperation,
    waitingElapsedMs,
    enqueueOrRunOperation,
    cancelQueuedOperation,
  } = useWaitQueue<ActivationCommand>({
    isDeviceCompatible: (device) => device.source === "apple",
    onQueued: (operation) => {
      setStatus("idle");
      setOutput(`[queue] Waiting for device... ${operation.name} queued.`);
    },
    onDeviceDetected: (device, operation) => {
      setOutput(`[queue] Device detected! Starting ${operation.name} on ${device.model}...`);
    },
    onError: (message) => {
      setOutput(String(message));
      setStatus("error");
    },
    onExecute: async (device, operation) => {
      setStatus("running");
      setOutput(`${operation.name} started on ${device.model}...`);

      try {
        if (operation.params.command === "ios_check_activation_state") {
          const result = await invoke<ActivationStateResponse>(operation.params.command, {
            udid: device.id,
          });
          setDeviceState(result.removal_path.toLowerCase());
          setOutput(formatActivationState(result));
          setStatus("success");
          return;
        }

        const result = await invoke<string | void>(
          operation.params.command,
          {
            ...operation.params.args,
            udid: device.id,
          },
        );

        setOutput(
          typeof result === "string" && result.trim().length > 0
            ? result
            : `${operation.name} dispatched for ${device.model}.`,
        );
        setStatus("success");
      } catch (error: unknown) {
        setOutput(String(error));
        setStatus("error");
      }
    },
  });

  const queueActivationCommand = async (name: string, command: string, args: Record<string, unknown> = {}) => {
    await enqueueOrRunOperation(name, { command, args });
  };

  return (
    <div className="page">
      <h2 className="page-title">Activation</h2>
      <p className="page-subtitle">
        Official Hello Bypass · MDM Removal · Passcode Activation
      </p>

      <WaitingQueuePanel
        operation={waitingOperation}
        elapsedMs={waitingElapsedMs}
        onCancel={cancelQueuedOperation}
      />

      <div className="panel row-between">
        <div>
          <div className="action-title">Detection Engine ({deviceState})</div>
          <div className="meta-text">Identify lock type before running exploit</div>
        </div>
        <button
          className="action-btn"
          onClick={() => void queueActivationCommand("Check Device State", "ios_check_activation_state")}
          disabled={status === "running"}
        >
          <span>S</span> <span>Check Device State</span>
        </button>
      </div>

      <div className="grid-auto">
        <ActionCard
          title="Hello Bypass"
          desc="Full untethered bypass for Hello screen (iOS 12-16)"
          icon="HB"
          btnText="Run Bypass"
          onClick={() => void queueActivationCommand("Hello Bypass", "ios_run_hello_bypass")}
          disabled={status === "running"}
        />
        <ActionCard
          title="MDM Bypass"
          desc="Remove Mobile Device Management profiles instantly"
          icon="MDM"
          btnText="Remove MDM"
          onClick={() => void queueActivationCommand("MDM Bypass", "ios_remove_mdm")}
          disabled={status === "running"}
        />
        <ActionCard
          title="Passcode / Disabled"
          desc="Extract tickets and activate without data loss"
          icon="PC"
          btnText="Fix Passcode"
          onClick={() => void queueActivationCommand("Passcode Fix", "ios_patch_activation_record")}
          disabled={status === "running"}
        />
        <ActionCard
          title="Checkra1n Jails"
          desc="Run integrated jailbreak for A11 and below"
          icon="A11"
          btnText="Launch Checkra1n"
          onClick={() => void queueActivationCommand("Checkra1n", "ios_run_checkra1n")}
          disabled={status === "running"}
        />
      </div>

      <Terminal output={output} status={status} />
    </div>
  );
}

function ActionCard({ title, desc, icon, btnText, onClick, disabled }: {
  title: string;
  desc: string;
  icon: string;
  btnText: string;
  onClick: () => void;
  disabled: boolean;
}) {
  return (
    <div className="action-card">
      <div className="action-row">
        <span className="action-icon-chip">{icon}</span>
        <span className="action-title">{title}</span>
      </div>
      <p className="action-desc">{desc}</p>
      <button className="action-btn" onClick={onClick} disabled={disabled}>
        {btnText}
      </button>
    </div>
  );
}
