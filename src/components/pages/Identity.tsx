import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import Terminal from "../Terminal";

export default function IdentityPage() {
  const [output, setOutput] = useState("");
  const [status, setStatus] = useState<"idle"|"running"|"success"|"error">("idle");
  const [imei, setImei] = useState("");

  const run = async (id: string, args: Record<string, string> = {}) => {
    setStatus("running"); setOutput("");
    try { setOutput(await invoke<string>(id, args)); setStatus("success"); }
    catch (e: any) { setOutput(String(e)); setStatus("error"); }
  };

  return (
    <div>
      <h2 style={{ fontSize: 18, fontWeight: 700, marginBottom: 4 }}>🆔 Identity Intelligence</h2>
      <p style={{ fontSize: 12, color: "#64748b", marginBottom: 20 }}>
        GSMA Lookup · IMEI Status · Device Forensics · Identity Verification
      </p>

      <div className="glass" style={{ padding: 16, marginBottom: 16 }}>
        <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 12 }}>Check IMEI / MEID</div>
        <div style={{ display: "flex", gap: 10 }}>
          <input
            value={imei}
            onChange={(e) => setImei(e.target.value)}
            placeholder="Enter 15-digit IMEI"
            style={{
              flex: 1, padding: "10px 14px", borderRadius: 10,
              background: "rgba(255,255,255,0.05)",
              border: "1px solid rgba(255,255,255,0.1)", color: "#e2e8f0", fontSize: 14
            }}
          />
          <button className="btn primary" style={{ padding: "0 20px" }}
            onClick={() => run("check_imei_intel", { imei })}
            disabled={!imei || status === "running"}>
            🔍 Check Intel
          </button>
        </div>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 12 }}>
        <div className="glass" style={{ padding: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 8 }}>Identity Report</div>
          <p style={{ fontSize: 11, color: "#64748b", marginBottom: 15 }}>Generate a full forensic report of all hardware identifiers.</p>
          <button className="btn" style={{ width: "100%" }} onClick={() => run("get_full_identity")}>
            📄 Generate Full Report
          </button>
        </div>

        <div className="glass" style={{ padding: 16, background: "rgba(37,99,235,0.05)" }}>
          <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 8 }}>Sync Status</div>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <div style={{ width: 10, height: 10, borderRadius: "50%", background: "#4ade80" }} />
            <span style={{ fontSize: 12, color: "#e2e8f0" }}>Connected to Intel Server</span>
          </div>
          <p style={{ fontSize: 11, color: "#64748b", marginTop: 10 }}>Database v2026.3.17 Alpha</p>
        </div>
      </div>

      <Terminal output={output} status={status} />
    </div>
  );
}
