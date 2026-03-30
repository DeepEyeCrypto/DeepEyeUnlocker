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
    <div className="page">
      <h2 className="page-title">Activation</h2>
      <p className="page-subtitle">
        Official Hello Bypass · MDM Removal · Passcode Activation
      </p>

      <div className="panel row-between">
        <div>
          <div className="action-title">Detection Engine ({deviceState})</div>
          <div className="meta-text">Identify lock type before running exploit</div>
        </div>
        <button className="btn btn-primary btn-sm" onClick={checkState} disabled={status === "running"}>
          <span>S</span> <span>Check Device State</span>
        </button>
      </div>

      <div className="grid-auto">
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

function ActionCard({ title, desc, icon, btnText, onClick, disabled }: {
  title: string;
  desc: string;
  icon: string;
  btnText: string;
  onClick: () => void;
  disabled: boolean;
}) {
  return (
    <div className="action-card">
      <div className="action-row">
        <span>{icon}</span>
        <span className="action-title">{title}</span>
      </div>
      <p className="action-desc">{desc}</p>
      <button className="btn btn-secondary btn-sm" onClick={onClick} disabled={disabled}>
        {btnText}
      </button>
    </div>
  );
}
