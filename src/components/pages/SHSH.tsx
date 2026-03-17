import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { open } from "@tauri-apps/plugin-dialog";
import Terminal from "../Terminal";

export default function SHSHPage() {
  const [output, setOutput] = useState("");
  const [status, setStatus] = useState<"idle"|"running"|"success"|"error">("idle");
  const [model, setModel] = useState("");
  const [ecid, setEcid] = useState("");
  const [ios, setIos] = useState("");
  const [generator, setGenerator] = useState("0x1111111111111111");
  const [ipsw, setIpsw] = useState("");
  const [shsh, setShsh] = useState("");

  const run = async (id: string, args: Record<string, string> = {}) => {
    setStatus("running"); setOutput("");
    try { setOutput(await invoke<string>(id, args)); setStatus("success"); }
    catch (e: any) { setOutput(String(e)); setStatus("error"); }
  };

  const autoFill = async () => {
    setStatus("running"); setOutput("Detecting device info...");
    try {
      const [ecidRes, boardRes] = await Promise.all([
        invoke<string>("get_ecid"),
        invoke<string>("get_board_config"),
      ]);
      setEcid(ecidRes.trim());
      setOutput(`ECID: ${ecidRes.trim()}\n${boardRes}`);
      setStatus("success");
    } catch (e: any) { setOutput(String(e)); setStatus("error"); }
  };

  return (
    <div>
      <h2 style={{ fontSize: 18, fontWeight: 700, marginBottom: 4 }}>💾 SHSH Blobs</h2>
      <p style={{ fontSize: 12, color: "#64748b", marginBottom: 20 }}>
        tsschecker + futurerestore — Save · Manage · Downgrade iOS
      </p>

      {/* Device fields */}
      <div className="glass" style={{ padding: 16, marginBottom: 12 }}>
        <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 12 }}>Device Info</div>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr auto", gap: 10, alignItems: "end" }}>
          {[
            { label: "Model (e.g. iPhone11,8)", val: model, set: setModel },
            { label: "ECID (decimal/hex)", val: ecid, set: setEcid },
            { label: "iOS Version (e.g. 16.7.5)", val: ios, set: setIos },
          ].map(f => (
            <div key={f.label}>
              <div style={{ fontSize: 10, color: "#64748b", marginBottom: 4 }}>{f.label}</div>
              <input value={f.val} onChange={e => f.set(e.target.value)}
                style={{
                  width: "100%", padding: "8px 10px", borderRadius: 8,
                  background: "rgba(255,255,255,0.05)",
                  border: "1px solid rgba(255,255,255,0.1)", color: "#e2e8f0", fontSize: 12
                }} />
            </div>
          ))}
          <button className="btn primary" onClick={autoFill}>⚡ Auto-Fill</button>
        </div>
      </div>

      {/* Action cards */}
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10, marginBottom: 12 }}>
        <div className="glass" style={{ padding: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 4 }}>Save All Signed</div>
          <div style={{ fontSize: 11, color: "#64748b", marginBottom: 10 }}>Save blobs for ALL Apple-signed firmwares now</div>
          <button className="btn success" style={{ width: "100%" }}
            onClick={() => run("save_shsh_all_signed", { model, ecid })}
            disabled={!model || !ecid || status === "running"}>
            ▶ Save All Signed
          </button>
        </div>

        <div className="glass" style={{ padding: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 4 }}>Save Specific Version</div>
          <div style={{ fontSize: 11, color: "#64748b", marginBottom: 10 }}>Save blob for one iOS version</div>
          <button className="btn" style={{ width: "100%" }}
            onClick={() => run("save_shsh_specific", { model, ecid, ios })}
            disabled={!model || !ecid || !ios || status === "running"}>
            ▶ Save Specific
          </button>
        </div>

        <div className="glass" style={{ padding: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 4 }}>Save with Generator</div>
          <div style={{ fontSize: 11, color: "#64748b", marginBottom: 8 }}>Nonce collision blob — for downgrade</div>
          <input value={generator} onChange={e => setGenerator(e.target.value)}
            style={{
              width: "100%", padding: "6px 10px", borderRadius: 6, marginBottom: 8,
              background: "rgba(255,255,255,0.05)", border: "1px solid rgba(255,255,255,0.1)",
              color: "#e2e8f0", fontSize: 11
            }} />
          <button className="btn warn" style={{ width: "100%" }}
            onClick={() => run("save_shsh_with_generator", { model, ecid, ios, generator })}
            disabled={!model || !ecid || !ios || status === "running"}>
            ▶ Save with Generator
          </button>
        </div>

        <div className="glass" style={{ padding: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 4 }}>Check Signed Versions</div>
          <div style={{ fontSize: 11, color: "#64748b", marginBottom: 10 }}>Which iOS versions Apple is signing right now</div>
          <button className="btn" style={{ width: "100%" }}
            onClick={() => run("check_signed_versions", { model })}
            disabled={!model || status === "running"}>
            ▶ Check Signed
          </button>
        </div>

        <div className="glass" style={{ padding: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 4 }}>List Saved Blobs</div>
          <div style={{ fontSize: 11, color: "#64748b", marginBottom: 10 }}>Show all .shsh2 files in DeepEyeUnlocker/shsh/</div>
          <button className="btn" style={{ width: "100%" }}
            onClick={() => run("list_saved_shsh")} disabled={status === "running"}>
            ▶ List Blobs
          </button>
        </div>

        <div className="glass" style={{ padding: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 8 }}>futurerestore</div>
          <button className="btn primary" style={{ width: "100%", marginBottom: 6 }}
            onClick={async () => {
              const i = await open({ filters: [{ name: "IPSW", extensions: ["ipsw"] }] });
              if (i) setIpsw(String(i));
            }}>
            📁 Pick IPSW
          </button>
          <button className="btn" style={{ width: "100%", marginBottom: 8, fontSize: 11 }}
            onClick={async () => {
              const s = await open({ filters: [{ name: "SHSH2", extensions: ["shsh2"] }] });
              if (s) setShsh(String(s));
            }}>
            📁 Pick SHSH2
          </button>
          <div style={{ fontSize: 10, color: "#64748b", marginBottom: 6 }}>
            {ipsw ? `IPSW: ...${ipsw.slice(-30)}` : "No IPSW selected"}
            <br/>{shsh ? `SHSH: ...${shsh.slice(-30)}` : "No SHSH selected"}
          </div>
          <button className="btn danger" style={{ width: "100%" }}
            onClick={() => run("futurerestore_no_baseband", { ipsw_path: ipsw, shsh_path: shsh })}
            disabled={!ipsw || !shsh || status === "running"}>
            ⚡ futurerestore
          </button>
        </div>
      </div>

      <Terminal output={output} status={status} />
    </div>
  );
}
