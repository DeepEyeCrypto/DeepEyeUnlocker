import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import Terminal from "../Terminal";

export default function DiagnosticsPage() {
  const [output, setOutput] = useState("");
  const [status, setStatus] = useState<"idle"|"running"|"success"|"error">("idle");

  const run = async (id: string, args: Record<string, string> = {}) => {
    setStatus("running"); setOutput("");
    try { setOutput(await invoke<string>(id, args)); setStatus("success"); }
    catch (e: any) { setOutput(String(e)); setStatus("error"); }
  };

  const DIAG_ACTIONS = [
    { title: "General Info", cmd: "gas", icon: "📊" },
    { title: "Battery Health", cmd: "battery", icon: "🔋", special: "get_battery_stats" },
    { title: "Thermal State", cmd: "thermal", icon: "🌡️", special: "get_thermal_state" },
    { title: "Display Info", cmd: "display", icon: "🖥️" },
    { title: "All Sensors", cmd: "sensors", icon: "📡" },
    { title: "Shutdown", cmd: "shutdown", icon: "🛑", danger: true, special: "device_shutdown" },
    { title: "Restart", cmd: "restart", icon: "🔄", danger: true, special: "device_restart" },
  ];

  return (
    <div>
      <h2 style={{ fontSize: 18, fontWeight: 700, marginBottom: 4 }}>🔬 Hardware Diagnostics</h2>
      <p style={{ fontSize: 12, color: "#64748b", marginBottom: 20 }}>
        Bit-level system health monitoring & hardware tests
      </p>

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))", gap: 12, marginBottom: 20 }}>
        {DIAG_ACTIONS.map(action => (
          <div key={action.title} className="glass" style={{
            padding: 16, display: "flex", flexDirection: "column", gap: 12,
            border: action.danger ? "1px solid rgba(248,113,113,0.1)" : "1px solid rgba(255,255,255,0.05)"
          }}>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <span style={{ fontSize: 20 }}>{action.icon}</span>
              <span style={{ fontSize: 13, fontWeight: 600 }}>{action.title}</span>
            </div>
            <button
              className={`btn ${action.danger ? 'danger' : action.special ? 'primary' : ''}`}
              style={{ width: "100%", fontSize: 11 }}
              disabled={status === "running"}
              onClick={() => {
                if (action.special) run(action.special);
                else run("run_diagnostics", { cmd: action.cmd });
              }}
            >
              Run Test
            </button>
          </div>
        ))}
      </div>

      <Terminal output={output} status={status} />
    </div>
  );
}
