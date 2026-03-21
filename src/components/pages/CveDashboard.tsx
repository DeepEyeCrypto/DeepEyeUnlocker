import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import Terminal from "../Terminal";

export default function CveDashboard() {
  const [output, setOutput] = useState("");
  const [status, setStatus] = useState<"idle"|"running"|"success"|"error">("idle");
  const [version, setVersion] = useState("16.7.5");
  const [chip, setChip] = useState("A11 Bionic");

  const runScan = async () => {
    setStatus("running"); setOutput("Querying DeepEye CVE Database...");
    try {
      const res = await invoke<string>("query_cve_database", { version, chip });
      setOutput(res);
      setStatus("success");
    } catch (e: any) {
      setOutput(String(e));
      setStatus("error");
    }
  };

  return (
    <div>
      <h2 style={{ fontSize: 18, fontWeight: 700, marginBottom: 4 }}>🧠 CVE Intelligence</h2>
      <p style={{ fontSize: 12, color: "#64748b", marginBottom: 20 }}>
        Vulnerability Scanner · Exploit Surface Analysis · Security Audit
      </p>

      <div className="glass" style={{ padding: 16, marginBottom: 16 }}>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr auto", gap: 12, alignItems: "end" }}>
          <div>
            <div style={{ fontSize: 10, color: "#94a3b8", marginBottom: 4, fontWeight: 600 }}>OS Version</div>
            <input value={version} onChange={e => setVersion(e.target.value)}
              style={{
                width: "100%", padding: "8px 10px", borderRadius: 8,
                background: "rgba(255,255,255,0.05)",
                border: "1px solid rgba(255,255,255,0.1)", color: "#e2e8f0", fontSize: 13
              }} />
          </div>
          <div>
            <div style={{ fontSize: 10, color: "#94a3b8", marginBottom: 4, fontWeight: 600 }}>Processor / Chip</div>
            <input value={chip} onChange={e => setChip(e.target.value)}
              style={{
                width: "100%", padding: "8px 10px", borderRadius: 8,
                background: "rgba(255,255,255,0.05)",
                border: "1px solid rgba(255,255,255,0.1)", color: "#e2e8f0", fontSize: 13
              }} />
          </div>
          <button className="btn primary" onClick={runScan} disabled={status === "running"}>
            🔍 Scan CVEs
          </button>
        </div>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "2fr 1fr", gap: 12 }}>
        <div className="glass" style={{ padding: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 12, color: "#a78bfa" }}>Vulnerability Report</div>
          <Terminal output={output} status={status} />
        </div>

        <div className="glass" style={{ padding: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 12 }}>Security Score</div>
          <div style={{ textAlign: "center", padding: "10px 0" }}>
            <div style={{ fontSize: 32, fontWeight: 800, color: version.startsWith("15") ? "#f87171" : "#fbbf24" }}>
              {version.startsWith("15") ? "Critical" : "At Risk"}
            </div>
            <div style={{ fontSize: 11, color: "#64748b" }}>Exploit availability is high</div>
          </div>
          <button className="btn" style={{ width: "100%", marginTop: 10 }} 
            onClick={() => invoke("run_intelligence_scan", { ecid: "0x8020" })}>
            🛡️ Deep Scan
          </button>
        </div>
      </div>
    </div>
  );
}
