import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import Terminal from "../Terminal";

export default function ActivationPage() {
  const [output, setOutput] = useState("");
  const [status, setStatus] = useState<"idle"|"running"|"success"|"error">("idle");
  const [deviceState, setDeviceState] = useState<string>("unknown");

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

  const checkState = async () => {
    setStatus("running"); setOutput("Analyzing activation state...");
    try {
      const res = await invoke<string>("ios_check_activation_state");
      setOutput(res);
      setStatus("success");
      // Mock logic for UI state update
      if (res.includes("Hello")) setDeviceState("hello");
      else if (res.includes("MDM")) setDeviceState("mdm");
      else setDeviceState("activated");
    } catch (e: any) { setOutput(String(e)); setStatus("error"); }
  };

  return (
    <div>
      <h2 style={{ fontSize: 18, fontWeight: 700, marginBottom: 4 }}>🔓 Activation</h2>
      <p style={{ fontSize: 12, color: "#64748b", marginBottom: 20 }}>
        Official Hello Bypass · MDM Removal · Passcode Activation
      </p>

      <div className="glass" style={{ padding: 16, marginBottom: 16, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <div>
          <div style={{ fontSize: 13, fontWeight: 700, color: "#e2e8f0" }}>Detection Engine ({deviceState})</div>
          <div style={{ fontSize: 11, color: "#94a3b8" }}>Identify lock type before running exploit</div>
        </div>
        <button className="btn primary" onClick={checkState} disabled={status === "running"}>
          🔍 Check Device State
        </button>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(240px, 1fr))", gap: 12, marginBottom: 12 }}>
        <ActionCard
          title="Hello Bypass"
          desc="Full untethered bypass for Hello screen (iOS 12-16)"
          icon="👋"
          btnText="Run Bypass"
          onClick={() => run("ios_run_hello_bypass")}
          disabled={status === "running"}
        />
        <ActionCard
          title="MDM Bypass"
          desc="Remove Mobile Device Management profiles instantly"
          icon="🏢"
          btnText="Remove MDM"
          onClick={() => run("ios_remove_mdm")}
          disabled={status === "running"}
        />
        <ActionCard
          title="Passcode / Disabled"
          desc="Extract tickets and activate without data loss"
          icon="🔢"
          btnText="Fix Passcode"
          onClick={() => run("ios_patch_activation_record")}
          disabled={status === "running"}
        />
        <ActionCard
          title="Checkra1n Jails"
          desc="Run integrated jailbreak for A11 and below"
          icon="⚡"
          btnText="Launch Checkra1n"
          onClick={() => run("ios_run_checkra1n")}
          disabled={status === "running"}
        />
      </div>

      <Terminal output={output} status={status} />
    </div>
  );
}

function ActionCard({ title, desc, icon, btnText, onClick, disabled }: any) {
  return (
    <div className="glass" style={{ padding: 16, display: "flex", flexDirection: "column", gap: 10 }}>
      <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
        <span style={{ fontSize: 20 }}>{icon}</span>
        <span style={{ fontSize: 13, fontWeight: 700 }}>{title}</span>
      </div>
      <p style={{ fontSize: 11, color: "#64748b", flex: 1 }}>{desc}</p>
      <button className="btn" style={{ width: "100%", fontSize: 11 }} onClick={onClick} disabled={disabled}>
        {btnText}
      </button>
    </div>
  );
}
