import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import Terminal from "../Terminal";

export default function VaultPage() {
  const [output, setOutput] = useState("");
  const [status, setStatus] = useState<"idle"|"running"|"success"|"error">("idle");
  const [vaultList, setVaultList] = useState<string[]>([]);
  const [ecid, setEcid] = useState("");
  const [tokenPath, setTokenPath] = useState("");

  const run = async (id: string, args: Record<string, string> = {}) => {
    setStatus("running"); setOutput("");
    try { setOutput(await invoke<string>(id, args)); setStatus("success"); }
    catch (e: any) { setOutput(String(e)); setStatus("error"); }
  };

  const refreshVault = async () => {
    setStatus("running");
    try {
      const res = await invoke<string>("list_cloud_vault");
      setVaultList(res.split("\n"));
      setStatus("success");
    } catch (e: any) { setOutput(String(e)); setStatus("error"); }
  };

  return (
    <div>
      <h2 style={{ fontSize: 18, fontWeight: 700, marginBottom: 4 }}>☁️ Cloud Vault</h2>
      <p style={{ fontSize: 12, color: "#64748b", marginBottom: 20 }}>
        End-to-End Encrypted Token Storage · Cross-Device Sync
      </p>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 12 }}>
        <div className="glass" style={{ padding: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 12, color: "#475569" }}>Local to Cloud</div>
          <p style={{ fontSize: 11, color: "#64748b", marginBottom: 12 }}>Secure your locally extracted tickets in the DeepEye vault.</p>
          <input
            value={ecid}
            onChange={(event) => setEcid(event.target.value)}
            placeholder="Enter actual ECID"
            className="field-input"
            style={{ marginBottom: 8 }}
          />
          <input
            value={tokenPath}
            onChange={(event) => setTokenPath(event.target.value)}
            placeholder="Enter extracted token path"
            className="field-input"
            style={{ marginBottom: 8 }}
          />
          <button className="btn primary" style={{ width: "100%" }}
            disabled={!ecid.trim() || !tokenPath.trim()}
            onClick={() => run("push_to_cloud_vault", { ecid: ecid.trim(), token_path: tokenPath.trim() })}>
            ⬆️ Push Record to Cloud
          </button>
        </div>

        <div className="glass" style={{ padding: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 12, color: "#475569" }}>Cloud to Local</div>
          <p style={{ fontSize: 11, color: "#64748b", marginBottom: 12 }}>Recover records for any previously synced device.</p>
          <input
            value={ecid}
            onChange={(event) => setEcid(event.target.value)}
            placeholder="Enter actual ECID"
            className="field-input"
            style={{ marginBottom: 8 }}
          />
          <button className="btn" style={{ width: "100%" }}
            disabled={!ecid.trim()}
            onClick={() => run("pull_from_cloud_vault", { ecid: ecid.trim() })}>
            ⬇️ Pull Record from Cloud
          </button>
        </div>
      </div>

      <div className="glass" style={{ padding: 16, marginBottom: 12 }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 12 }}>
          <div style={{ fontSize: 13, fontWeight: 700 }}>Vault Explorer</div>
          <button className="btn" style={{ padding: "4px 10px", fontSize: 10 }} onClick={refreshVault}>🔄 Refresh</button>
        </div>
        <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
          {vaultList.length > 0 ? vaultList.map((item, idx) => (
            <div key={idx} style={{
              padding: "10px 12px", borderRadius: 8, background: "rgba(255,255,255,0.03)",
              border: "1px solid rgba(255,255,255,0.05)", fontSize: 12, display: "flex", justifyContent: "space-between"
            }}>
              <span>{item}</span>
              <span style={{ color: "#4ade80", fontSize: 10 }}>● SECURE</span>
            </div>
          )) : (
            <div style={{ textAlign: "center", padding: 20, fontSize: 12, color: "#475569" }}>Vault is empty or not refreshed</div>
          )}
        </div>
      </div>

      <Terminal output={output} status={status} />
    </div>
  );
}
