import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import Terminal from "../Terminal";

export default function PurplePage() {
  const [output, setOutput] = useState("");
  const [status, setStatus] = useState<"idle"|"running"|"success"|"error">("idle");
  const [newSn, setNewSn] = useState("");

  const run = async (id: string, args: Record<string, string> = {}) => {
    setStatus("running"); setOutput("");
    try { setOutput(await invoke<string>(id, args)); setStatus("success"); }
    catch (e: any) { setOutput(String(e)); setStatus("error"); }
  };

  return (
    <div>
      <h2 style={{ fontSize: 18, fontWeight: 700, marginBottom: 4 }}>🟣 Purple Mode</h2>
      <p style={{ fontSize: 12, color: "#64748b", marginBottom: 20 }}>
        Diagnostic Mode · Config Data · SN Change · SysCfg Repair
      </p>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 12 }}>
        <div className="glass" style={{ padding: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 12 }}>Mode Control</div>
          <button className="btn primary" style={{ width: "100%", marginBottom: 8 }}
            onClick={() => run("enter_purple_mode")} disabled={status === "running"}>
            🔮 Enter Purple Mode
          </button>
          <button className="btn" style={{ width: "100%" }}
            onClick={() => run("purple_read_all")} disabled={status === "running"}>
            📋 Read All SysCfg
          </button>
        </div>

        <div className="glass" style={{ padding: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 12 }}>Serial Number (SN)</div>
          <button className="btn" style={{ width: "100%", marginBottom: 12 }}
            onClick={() => run("purple_read_sn")} disabled={status === "running"}>
            🔍 Read Current SN
          </button>
          <div style={{ display: "flex", gap: 8 }}>
            <input
              value={newSn}
              onChange={(e) => setNewSn(e.target.value)}
              placeholder="New Serial Number"
              style={{
                flex: 1, padding: "8px 10px", borderRadius: 8,
                background: "rgba(255,255,255,0.05)",
                border: "1px solid rgba(255,255,255,0.1)", color: "#e2e8f0", fontSize: 12
              }}
            />
            <button className="btn success"
              disabled={!newSn || status === "running"}
              onClick={() => run("purple_write_sn", { sn: newSn })}>
              ✍️ Write
            </button>
          </div>
        </div>
      </div>

      <div className="glass" style={{ padding: 16, border: "1px solid rgba(167,139,250,0.1)" }}>
        <div style={{ fontSize: 11, color: "#94a3b8", lineHeight: 1.4 }}>
          <strong>Note:</strong> Purple Mode requires a DCSD cable or a compatible PWN DFU state (via gaster). Ensure your device is in a stable state before writing any SysCfg values.
        </div>
      </div>

      <Terminal output={output} status={status} />
    </div>
  );
}
