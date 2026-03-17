import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { open } from "@tauri-apps/plugin-dialog";
import Terminal from "../Terminal";

export default function RestorePage() {
  const [output, setOutput] = useState("");
  const [status, setStatus] = useState<"idle"|"running"|"success"|"error">("idle");
  const [ipswPath, setIpswPath] = useState("");

  const run = async (id: string, args: Record<string, string> = {}) => {
    setStatus("running"); setOutput("");
    try { setOutput(await invoke<string>(id, args)); setStatus("success"); }
    catch (e: any) { setOutput(String(e)); setStatus("error"); }
  };

  const pickIpsw = async () => {
    const selected = await open({
      filters: [{ name: "IPSW", extensions: ["ipsw"] }]
    });
    if (selected) setIpswPath(String(selected));
  };

  return (
    <div>
      <h2 style={{ fontSize: 18, fontWeight: 700, marginBottom: 4 }}>🔄 Restore / IPSW</h2>
      <p style={{ fontSize: 12, color: "#64748b", marginBottom: 20 }}>
        idevicerestore — Flash firmware · Exit recovery · Repair system
      </p>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 12 }}>
        <div className="glass" style={{ padding: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 12 }}>Standard Restore</div>
          <button className="btn primary" style={{ width: "100%", marginBottom: 8 }}
            onClick={pickIpsw}>📁 Pick Local IPSW</button>
          {ipswPath && <div style={{ fontSize: 10, color: "#64748b", marginBottom: 10 }}>Path: ...{ipswPath.slice(-40)}</div>}
          <button className="btn success" style={{ width: "100%" }}
            onClick={() => run("restore_local_ipsw", { path: ipswPath })}
            disabled={!ipswPath || status === "running"}>⚡ Flash IPSW</button>
        </div>

        <div className="glass" style={{ padding: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 12 }}>Quick Actions</div>
          <button className="btn" style={{ width: "100%", marginBottom: 8 }}
            onClick={() => run("restore_latest")} disabled={status === "running"}>
            ⬆️ Update to Latest Signed
          </button>
          <button className="btn warn" style={{ width: "100%", marginBottom: 8 }}
            onClick={() => run("exit_recovery")} disabled={status === "running"}>
            🏠 Exit Recovery Mode
          </button>
          <button className="btn" style={{ width: "100%" }}
            onClick={() => run("get_recovery_info")} disabled={status === "running"}>
            ℹ️ Get Recovery Info
          </button>
        </div>
      </div>

      <div className="glass" style={{ padding: 16, marginBottom: 12, border: "1px solid rgba(248,113,113,0.1)" }}>
        <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 8 }}>
          <span style={{ fontSize: 16 }}>⚠️</span>
          <span style={{ fontSize: 13, fontWeight: 700, color: "#f87171" }}>CRITICAL WARNING</span>
        </div>
        <p style={{ fontSize: 11, color: "#94a3b8", lineHeight: 1.4 }}>
          Restoring firmware will wipe all user data. Ensure you have backed up activation tokens and SHSH blobs before proceeding. DeepEye Unlocker is not responsible for data loss.
        </p>
      </div>

      <Terminal output={output} status={status} />
    </div>
  );
}
