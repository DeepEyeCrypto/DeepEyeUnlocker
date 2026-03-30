import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { open } from "@tauri-apps/plugin-dialog";
import Terminal from "../Terminal";

export default function SHSHPage() {
  const [output, setOutput] = useState("");
  const [status, setStatus] = useState<"idle" | "running" | "success" | "error">("idle");
  const [model, setModel] = useState("");
  const [ecid, setEcid] = useState("");
  const [ios, setIos] = useState("");
  const [generator, setGenerator] = useState("0x1111111111111111");
  const [ipsw, setIpsw] = useState("");
  const [shsh, setShsh] = useState("");

  const run = async (id: string, args: Record<string, string> = {}) => {
    setStatus("running");
    setOutput("");
    try {
      setOutput(await invoke<string>(id, args));
      setStatus("success");
    } catch (e: unknown) {
      setOutput(String(e));
      setStatus("error");
    }
  };

  const autoFill = async () => {
    setStatus("running");
    setOutput("Detecting device info...");
    try {
      const [ecidRes, boardRes] = await Promise.all([
        invoke<string>("get_ecid"),
        invoke<string>("get_board_config"),
      ]);
      setEcid(ecidRes.trim());
      setOutput(`ECID: ${ecidRes.trim()}\n${boardRes}`);
      setStatus("success");
    } catch (e: unknown) {
      setOutput(String(e));
      setStatus("error");
    }
  };

  return (
    <div className="page">
      <h2 className="page-title">SHSH Blobs</h2>
      <p className="page-subtitle">tsschecker + futurerestore — Save · Manage · Downgrade iOS</p>

      <div className="panel stack-sm">
        <div className="action-title">Device Info</div>
        <div className="field-grid">
          <Field label="Model (e.g. iPhone11,8)">
            <input className="field-input" value={model} onChange={(e) => setModel(e.target.value)} />
          </Field>
          <Field label="ECID (decimal/hex)">
            <input className="field-input" value={ecid} onChange={(e) => setEcid(e.target.value)} />
          </Field>
          <Field label="iOS Version (e.g. 16.7.5)">
            <input className="field-input" value={ios} onChange={(e) => setIos(e.target.value)} />
          </Field>
          <button className="btn btn-primary btn-sm" onClick={autoFill}>Auto-Fill</button>
        </div>
      </div>

      <div className="grid-two">
        <div className="action-card">
          <div className="action-title">Save All Signed</div>
          <div className="action-desc">Save blobs for all Apple-signed firmwares now.</div>
          <button
            className="btn btn-success btn-sm btn-block"
            onClick={() => run("save_shsh_all_signed", { model, ecid })}
            disabled={!model || !ecid || status === "running"}
          >
            Save All Signed
          </button>
        </div>

        <div className="action-card">
          <div className="action-title">Save Specific Version</div>
          <div className="action-desc">Save blob for one iOS version.</div>
          <button
            className="btn btn-secondary btn-sm btn-block"
            onClick={() => run("save_shsh_specific", { model, ecid, ios })}
            disabled={!model || !ecid || !ios || status === "running"}
          >
            Save Specific
          </button>
        </div>

        <div className="action-card">
          <div className="action-title">Save with Generator</div>
          <div className="action-desc">Nonce collision blob for downgrade workflow.</div>
          <input className="field-input" value={generator} onChange={(e) => setGenerator(e.target.value)} />
          <button
            className="btn btn-secondary btn-sm btn-block"
            onClick={() => run("save_shsh_with_generator", { model, ecid, ios, generator })}
            disabled={!model || !ecid || !ios || status === "running"}
          >
            Save with Generator
          </button>
        </div>

        <div className="action-card">
          <div className="action-title">Check Signed Versions</div>
          <div className="action-desc">Show versions currently signed by Apple.</div>
          <button
            className="btn btn-secondary btn-sm btn-block"
            onClick={() => run("check_signed_versions", { model })}
            disabled={!model || status === "running"}
          >
            Check Signed
          </button>
        </div>

        <div className="action-card">
          <div className="action-title">List Saved Blobs</div>
          <div className="action-desc">Show all .shsh2 files in DeepEyeUnlocker/shsh.</div>
          <button className="btn btn-secondary btn-sm btn-block" onClick={() => run("list_saved_shsh")} disabled={status === "running"}>
            List Blobs
          </button>
        </div>

        <div className="action-card">
          <div className="action-title">futurerestore</div>
          <button
            className="btn btn-primary btn-sm btn-block"
            onClick={async () => {
              const selected = await open({ filters: [{ name: "IPSW", extensions: ["ipsw"] }] });
              if (selected) setIpsw(String(selected));
            }}
          >
            Pick IPSW
          </button>
          <button
            className="btn btn-secondary btn-sm btn-block"
            onClick={async () => {
              const selected = await open({ filters: [{ name: "SHSH2", extensions: ["shsh2"] }] });
              if (selected) setShsh(String(selected));
            }}
          >
            Pick SHSH2
          </button>
          <div className="meta-text">{ipsw ? `IPSW: ...${ipsw.slice(-30)}` : "No IPSW selected"}</div>
          <div className="meta-text">{shsh ? `SHSH: ...${shsh.slice(-30)}` : "No SHSH selected"}</div>
          <button
            className="btn btn-danger btn-sm btn-block"
            onClick={() => run("futurerestore_no_baseband", { ipsw_path: ipsw, shsh_path: shsh })}
            disabled={!ipsw || !shsh || status === "running"}
          >
            futurerestore
          </button>
        </div>
      </div>

      <Terminal output={output} status={status} />
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <div className="field-label">{label}</div>
      {children}
    </div>
  );
}

