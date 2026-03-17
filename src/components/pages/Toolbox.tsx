import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import Terminal from "../Terminal";

export default function ToolboxPage() {
  const [output, setOutput] = useState("");
  const [status, setStatus] = useState<"idle"|"running"|"success"|"error">("idle");

  const run = async (id: string, args: Record<string, string> = {}) => {
    setStatus("running"); setOutput("");
    try { setOutput(await invoke<string>(id, args)); setStatus("success"); }
    catch (e: any) { setOutput(String(e)); setStatus("error"); }
  };

  return (
    <div>
      <h2 style={{ fontSize: 18, fontWeight: 700, marginBottom: 4 }}>🛠️ Toolbox</h2>
      <p style={{ fontSize: 12, color: "#64748b", marginBottom: 20 }}>
        OTA Block · Factory Reset · System Logs · Deep Backups
      </p>

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(220px, 1fr))", gap: 12, marginBottom: 20 }}>
        <ActionCard
          title="OTA Blocker"
          desc="Prevent Apple from auto-updating your device"
          icon="🚫"
          btnText="Block Updates"
          onClick={() => run("toolbox_block_ota")}
          disabled={status === "running"}
        />
        <ActionCard
          title="Factory Reset"
          desc="Complete wipe of all user data and settings"
          icon="🧹"
          btnText="Erase Device"
          danger
          onClick={() => run("toolbox_factory_reset")}
          disabled={status === "running"}
        />
        <ActionCard
          title="System Logs"
          desc="Stream live device logs (idevicesyslog)"
          icon="📝"
          btnText="Fetch Logs"
          onClick={() => run("toolbox_get_logs")}
          disabled={status === "running"}
        />
        <ActionCard
          title="Deep Backup"
          desc="Full local backup via idevicebackup2"
          icon="📂"
          btnText="Run Backup"
          onClick={() => run("toolbox_backup_device", { path: "~/DeepEyeUnlocker/Backups" })}
          disabled={status === "running"}
        />
      </div>

      <Terminal output={output} status={status} />
    </div>
  );
}

function ActionCard({ title, desc, icon, btnText, onClick, disabled, danger }: any) {
  return (
    <div className="glass" style={{ padding: 16, display: "flex", flexDirection: "column", gap: 10 }}>
      <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
        <span style={{ fontSize: 20 }}>{icon}</span>
        <span style={{ fontSize: 13, fontWeight: 700 }}>{title}</span>
      </div>
      <p style={{ fontSize: 11, color: "#64748b", flex: 1 }}>{desc}</p>
      <button
        className={`btn ${danger ? 'danger' : ''}`}
        style={{ width: "100%", fontSize: 11 }}
        onClick={onClick}
        disabled={disabled}
      >
        {btnText}
      </button>
    </div>
  );
}
