import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import Terminal from "../Terminal";
import { Card } from "../ui/Card";

export default function MtkToolsPage() {
  const [status, setStatus] = useState<"idle" | "running" | "success" | "error">("idle");
  const [output, setOutput] = useState("");
  const [unlockAcknowledged, setUnlockAcknowledged] = useState(false);

  const run = async (command: string) => {
    setStatus("running");
    setOutput("");

    try {
      const result = await invoke<string>(command);
      setOutput(result);
      setStatus("success");
    } catch (error: unknown) {
      setOutput(String(error));
      setStatus("error");
    }
  };

  const unlockBootloader = async () => {
    // [INFERRED] MTK `seccfg unlock` modifies `seccfg` and many devices factory-reset `userdata` when the lock state changes.
    const confirmed = window.confirm(
      "Operation: MTK bootloader unlock\nRisk: MEDIUM\nAffected partitions: seccfg, userdata\n\nProceed only if the device owner has approved a bootloader unlock.",
    );

    if (!confirmed) {
      return;
    }

    await run("mtk_unlock_bootloader");
  };

  return (
    <div className="page">
      <div>
        <h2 className="page-title">MTK Tools</h2>
        <p className="page-subtitle">MediaTek device discovery and bootloader-state operations. Qualcomm deep access remains available in Qualcomm EDL.</p>
      </div>

      <Card title="Read-Only Discovery">
        <div className="action-row">
          <button className="btn btn-primary btn-sm" disabled={status === "running"} onClick={() => void run("mtk_device_info")}>
            Read Device Info
          </button>
        </div>
      </Card>

      <Card title="Bootloader Actions">
        <div className="stack-sm">
          <div className="panel">MEDIUM RISK — unlocking a MediaTek bootloader can change `seccfg` and trigger a userdata reset on many devices.</div>
          <label className="field-label" style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <input
              type="checkbox"
              checked={unlockAcknowledged}
              onChange={(event) => setUnlockAcknowledged(event.target.checked)}
            />
            I understand that bootloader unlock can modify `seccfg` and wipe `userdata`.
          </label>
          <div className="action-row">
            <button
              className="btn btn-danger btn-sm"
              disabled={!unlockAcknowledged || status === "running"}
              onClick={() => void unlockBootloader()}
              title={!unlockAcknowledged ? "Acknowledge the unlock risk first" : "Unlock MediaTek bootloader"}
            >
              Unlock Bootloader
            </button>
          </div>
        </div>
      </Card>

      <Terminal output={output} status={status} />
    </div>
  );
}
