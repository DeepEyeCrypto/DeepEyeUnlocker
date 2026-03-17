import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import Terminal from "../Terminal";

export default function FmiPage() {
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
      <h2 style={{ fontSize: 18, fontWeight: 700, marginBottom: 4 }}>☁️ FMI / iCloud</h2>
      <p style={{ fontSize: 12, color: "#64748b", marginBottom: 20 }}>
        Find My iPhone Status · Apple ID Removal · Token Cleaning
      </p>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 12 }}>
        <div className="glass" style={{ padding: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 12, color: "#a78bfa" }}>FMI Intel</div>
          <button className="btn primary" style={{ width: "100%", marginBottom: 10 }}
            onClick={() => run("ios_fmi_state")}>🔍 Read FMI Status</button>
          <div style={{ fontSize: 11, color: "#64748b" }}>Checks if Find My is enabled via official Apple servers.</div>
        </div>

        <div className="glass" style={{ padding: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 12, color: "#a78bfa" }}>Account Logic</div>
          <button className="btn" style={{ width: "100%", marginBottom: 10 }}
            onClick={() => run("ios_apple_id_state")}>🔍 Check Apple ID</button>
          <button className="btn danger" style={{ width: "100%" }}
            onClick={() => run("ios_remove_apple_id")}>🛑 Remove Apple ID</button>
        </div>
      </div>

      <div className="glass" style={{ padding: 16, background: "rgba(248,113,113,0.05)", border: "1px solid rgba(248,113,113,0.1)" }}>
        <div style={{ fontSize: 12, fontWeight: 700, color: "#f87171", marginBottom: 8 }}>⚠️ PRO FORENSIC MODE</div>
        <p style={{ fontSize: 11, color: "#94a3b8", lineHeight: 1.5 }}>
          FMI OFF requires valid session tokens or an open menu state. If the menu is closed, use the <strong>MASS EXTRACTION</strong> tool to attempt token recovery before running removal logic.
        </p>
      </div>

      <Terminal output={output} status={status} />
    </div>
  );
}
