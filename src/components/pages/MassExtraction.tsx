import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import Terminal from "../Terminal";

export default function MassExtraction() {
  const [output, setOutput] = useState("");
  const [status, setStatus] = useState<"idle"|"running"|"success"|"error">("idle");
  const [progress, setProgress] = useState(0);

  const targets = [
    { id: "photos", label: "Photos & Videos", icon: "🖼️" },
    { id: "messages", label: "Messages (SMS/WA)", icon: "💬" },
    { id: "contacts", label: "Contacts & Calls", icon: "👤" },
    { id: "keychain", label: "Keychain & Passwords", icon: "🔑" },
    { id: "apps", label: "Application Data", icon: "📱" },
    { id: "system", label: "System Root FS", icon: "⚙️" },
  ];

  const startExtraction = async () => {
    setStatus("running"); setOutput("Initializing DeepEye Extraction Engine...\nMounting Data Partition...");
    setProgress(10);
    
    // Simulate multi-stage extraction
    setTimeout(() => { setOutput(o => o + "\n[1/6] Extracting Photos..."); setProgress(30); }, 1000);
    setTimeout(() => { setOutput(o => o + "\n[2/6] Extracting Messages..."); setProgress(50); }, 2000);
    setTimeout(() => { setOutput(o => o + "\n[3/6] Decrypting Keychain..."); setProgress(70); }, 3000);
    
    try {
      const res = await invoke<string>("ios_mass_extract");
      setOutput(o => o + "\n" + res);
      setProgress(100);
      setStatus("success");
    } catch (e: any) {
      setOutput(o => o + "\nError: " + String(e));
      setStatus("error");
    }
  };

  return (
    <div>
      <h2 style={{ fontSize: 18, fontWeight: 700, marginBottom: 4 }}>📂 Mass Extraction / Forensics</h2>
      <p style={{ fontSize: 12, color: "#64748b", marginBottom: 20 }}>
        High-speed forensic artifact retrieval · Bit-level image extraction
      </p>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 300px", gap: 12, marginBottom: 12 }}>
        <div className="glass" style={{ padding: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 16 }}>Select Targets</div>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
            {targets.map(t => (
              <div key={t.id} style={{
                display: "flex", alignItems: "center", gap: 10, padding: 10,
                borderRadius: 10, background: "rgba(255,255,255,0.03)",
                border: "1px solid rgba(255,255,255,0.05)"
              }}>
                <input type="checkbox" defaultChecked style={{ accentColor: "#a78bfa" }} />
                <span style={{ fontSize: 18 }}>{t.icon}</span>
                <span style={{ fontSize: 12, fontWeight: 500 }}>{t.label}</span>
              </div>
            ))}
          </div>
          <button className="btn primary" style={{ width: "100%", marginTop: 20, padding: "12px 0" }} 
            onClick={startExtraction} disabled={status === "running"}>
            🚀 Start Mass Extraction
          </button>
        </div>

        <div className="glass" style={{ padding: 16, display: "flex", flexDirection: "column", gap: 15 }}>
          <div style={{ fontSize: 13, fontWeight: 700 }}>Extraction Progress</div>
          <div style={{ flex: 1, display: "flex", flexDirection: "column", justifyContent: "center", alignItems: "center", gap: 10 }}>
            <div style={{
              width: 120, height: 120, borderRadius: "50%",
              border: "4px solid rgba(255,255,255,0.05)",
              display: "flex", alignItems: "center", justifyContent: "center",
              position: "relative"
            }}>
              <div style={{ fontSize: 24, fontWeight: 800, color: "#a78bfa" }}>{progress}%</div>
              {/* Simple CSS progress circle simulation */}
              <div style={{
                position: "absolute", inset: -4, borderRadius: "50%",
                border: "4px solid #a78bfa",
                clipPath: `polygon(50% 50%, -50% -50%, ${progress > 25 ? '150% -50%' : '50% -50%'}, ${progress > 50 ? '150% 150%' : progress > 25 ? '150% -50%' : '50% -50%'}, ${progress > 75 ? '-50% 150%' : progress > 50 ? '150% 150%' : '50% -50%'}, -50% -50%)`,
                transition: "all 0.3s"
              }} />
            </div>
            <div style={{ fontSize: 11, color: "#64748b" }}>Speed: 45 MB/s</div>
          </div>
          <div style={{ fontSize: 10, color: "#475569", textAlign: "center" }}>
            Destination: ~/DeepEyeUnlocker/Forensics/
          </div>
        </div>
      </div>

      <Terminal output={output} status={status} />
    </div>
  );
}
