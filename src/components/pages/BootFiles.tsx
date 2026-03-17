import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import Terminal from "../Terminal";

export default function BootFilesPage() {
  const [output, setOutput] = useState("");
  const [status, setStatus] = useState<"idle"|"running"|"success"|"error">("idle");

  const run = async (id: string, args: Record<string, any> = {}) => {
    setStatus("running"); setOutput("");
    try {
      const res = await invoke<string>(id, args);
      setOutput(res);
      setStatus("success");
    } catch (e: any) {
      setOutput(String(e));
      setStatus("error");
    }
  };

  return (
    <div>
      <h2 style={{ fontSize: 18, fontWeight: 700, marginBottom: 4 }}>📦 Boot Files / Ramdisk</h2>
      <p style={{ fontSize: 12, color: "#64748b", marginBottom: 20 }}>
        Custom SSH Ramdisk · Token Extraction · Filesystem Mounting
      </p>

      <div className="glass" style={{ padding: 16, marginBottom: 16 }}>
        <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 12, color: "#a78bfa" }}>Ramdisk Orchestration</div>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 10 }}>
          <button className="btn primary" onClick={() => run("ios_boot_ramdisk")}>🚀 Boot SSH Ramdisk</button>
          <button className="btn" onClick={() => run("ios_mount_ramdisk")}>📂 Mount Filesystem</button>
          <button className="btn danger" onClick={() => run("ios_mass_extract")}>⚡ Mass Extract</button>
        </div>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "2fr 1fr", gap: 12 }}>
        <div className="glass" style={{ padding: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 12 }}>Extraction Targets</div>
          <div style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
            {["Activation Records", "Accounts", "Keychains", "Media", "System Logs"].map(t => (
              <div key={t} style={{
                padding: "6px 12px", borderRadius: 20, background: "rgba(255,255,255,0.05)",
                border: "1px solid rgba(255,255,255,0.1)", fontSize: 11, color: "#94a3b8"
              }}>{t}</div>
            ))}
          </div>
        </div>

        <div className="glass" style={{ padding: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 4 }}>Total Extracted</div>
          <div style={{ fontSize: 24, fontWeight: 800, color: "#4ade80" }}>1.2 GB</div>
          <div style={{ fontSize: 10, color: "#64748b" }}>Vault sync enabled</div>
        </div>
      </div>

      <Terminal output={output} status={status} />
    </div>
  );
}
