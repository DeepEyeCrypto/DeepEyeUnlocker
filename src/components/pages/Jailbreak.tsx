import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import Terminal from "../Terminal";

export default function JailbreakPage() {
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
      <h2 style={{ fontSize: 18, fontWeight: 700, marginBottom: 4 }}>⚡ Jailbreak Engine</h2>
      <p style={{ fontSize: 12, color: "#64748b", marginBottom: 20 }}>
        Gaster PWN · Checkra1n · Palera1n · Bootloader Exploits
      </p>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 16 }}>
        <div className="glass" style={{ padding: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 12, color: "#f87171" }}>PwnDFU State</div>
          <button className="btn danger" style={{ width: "100%", marginBottom: 10 }}
            onClick={() => run("ios_run_gaster_pwn")}>🔥 Run Gaster PWN</button>
          <button className="btn" style={{ width: "100%" }}
            onClick={() => run("ios_check_pwn_state")}>🔍 Check Pwned State</button>
        </div>

        <div className="glass" style={{ padding: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 12, color: "#a78bfa" }}>Integrated Exploits</div>
          <div style={{ display: "flex", gap: 8 }}>
            <button className="btn" style={{ flex: 1 }} onClick={() => run("ios_run_checkra1n")}>⚡ checkra1n</button>
            <button className="btn" style={{ flex: 1 }} onClick={() => run("ios_run_palera1n")}>⛓️ palera1n</button>
          </div>
          <p style={{ fontSize: 10, color: "#64748b", marginTop: 10 }}>Supports A7-A11 chips (iPhone 5s through X).</p>
        </div>
      </div>

      <div className="glass" style={{ padding: 16 }}>
        <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 12 }}>Advanced Primitives</div>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 10 }}>
          <button className="btn" style={{ fontSize: 11 }} onClick={() => run("ios_inject_surgical_patch")}>💉 Surgical Patch</button>
          <button className="btn" style={{ fontSize: 11 }} onClick={() => run("ios_poll_orchestrator")}>📡 Poll Orchestrator</button>
          <button className="btn" style={{ fontSize: 11 }} onClick={() => run("ios_boot_ramdisk")}>📦 Boot Ramdisk</button>
        </div>
      </div>

      <Terminal output={output} status={status} />
    </div>
  );
}
