import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { WaitingQueuePanel } from "../WaitingQueuePanel";
import Terminal from "../Terminal";
import { useWaitQueue } from "../../hooks/useWaitQueue";
import { SpotlightFeatureCard } from "../ui/spotlight-feature-card";
import { Smartphone, Shield, Lock, Key, Unlock } from "lucide-react";

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

      {/* Detection Engine - Spotlight Card */}
      <div className="mb-6">
        <SpotlightFeatureCard
          icon={<Smartphone className="w-6 h-6 text-cyan-400" />}
          title="Detection Engine"
          description={`Identify lock type before running exploit (${deviceState})`}
          glowColor="blue"
          onClick={() => void queueActivationCommand("Check Device State", "ios_check_activation_state")}
          badge={deviceState !== "unknown" ? deviceState : undefined}
        />
      </div>

      {/* Activation Methods - Spotlight Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <SpotlightFeatureCard
          icon={<Shield className="w-6 h-6 text-green-400" />}
          title="Hello Bypass"
          description="Full untethered bypass for Hello screen (iOS 12-16)"
          glowColor="green"
          onClick={() => void queueActivationCommand("Hello Bypass", "ios_run_hello_bypass")}
        />
        
        <SpotlightFeatureCard
          icon={<Lock className="w-6 h-6 text-purple-400" />}
          title="MDM Bypass"
          description="Remove Mobile Device Management profiles instantly"
          glowColor="purple"
          onClick={() => void queueActivationCommand("MDM Bypass", "ios_remove_mdm")}
        />
        
        <SpotlightFeatureCard
          icon={<Key className="w-6 h-6 text-orange-400" />}
          title="Passcode / Disabled"
          description="Extract tickets and activate without data loss"
          glowColor="orange"
          onClick={() => void queueActivationCommand("Passcode Fix", "ios_patch_activation_record")}
        />
        
        <SpotlightFeatureCard
          icon={<Unlock className="w-6 h-6 text-red-400" />}
          title="Checkra1n Jailbreak"
          description="Run integrated jailbreak for A11 and below"
          glowColor="red"
          onClick={() => void queueActivationCommand("Checkra1n", "ios_run_checkra1n")}
        />
      </div>

      <Terminal output={output} status={status} />
    </div>
  );
}
