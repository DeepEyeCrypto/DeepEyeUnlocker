import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { open } from "@tauri-apps/plugin-dialog";
import Terminal from "../Terminal";
import { SpotlightFeatureCard } from "../ui/spotlight-feature-card";
import { Download, Save, Settings, Upload, FileCheck } from "lucide-react";

export default function SHSHPage() {
  const [output, setOutput] = useState("");
  const [status, setStatus] = useState<"idle" | "running" | "success" | "error">("idle");
  const [model, setModel] = useState("");
  const [ecid, setEcid] = useState("");
  const [ios, setIos] = useState("");
  const [generator] = useState("0x1111111111111111");
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

      {/* SHSH Operations - Spotlight Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-6">
        <SpotlightFeatureCard
          icon={<Save className="w-6 h-6 text-green-400" />}
          title="Save All Signed"
          description="Save blobs for all Apple-signed firmwares now"
          glowColor="green"
          onClick={() => run("save_shsh_all_signed", { model, ecid })}
        />
        
        <SpotlightFeatureCard
          icon={<Download className="w-6 h-6 text-cyan-400" />}
          title="Save Specific Version"
          description="Save blob for one iOS version"
          glowColor="blue"
          onClick={() => run("save_shsh_specific", { model, ecid, ios })}
        />
        
        <SpotlightFeatureCard
          icon={<Settings className="w-6 h-6 text-purple-400" />}
          title="Save with Generator"
          description="Nonce collision blob for downgrade workflow"
          glowColor="purple"
          onClick={() => run("save_shsh_with_generator", { model, ecid, ios, generator })}
        />
        
        <SpotlightFeatureCard
          icon={<FileCheck className="w-6 h-6 text-orange-400" />}
          title="Check Signed Versions"
          description="Show versions currently signed by Apple"
          glowColor="orange"
          onClick={() => run("check_signed_versions", { model })}
        />
        
        <SpotlightFeatureCard
          icon={<Save className="w-6 h-6 text-blue-400" />}
          title="List Saved Blobs"
          description="Show all .shsh2 files in DeepEyeUnlocker/shsh"
          glowColor="blue"
          onClick={() => run("list_saved_shsh")}
        />
      </div>

      {/* FutureRestore - Spotlight Card */}
      <div className="mb-6">
        <SpotlightFeatureCard
          icon={<Upload className="w-6 h-6 text-red-400" />}
          title="futurerestore"
          description={!ipsw ? "Select IPSW and SHSH2 files to downgrade" : `IPSW: ...${ipsw.slice(-30)}`}
          glowColor="red"
          onClick={() => run("futurerestore_no_baseband", { ipsw_path: ipsw, shsh_path: shsh })}
          badge={!ipsw || !shsh ? "Select files" : "Ready"}
        />
        
        {/* File Selection Buttons */}
        <div className="grid grid-cols-2 gap-3 mt-3">
          <button
            className="btn btn-primary btn-sm"
            onClick={async () => {
              const selected = await open({ filters: [{ name: "IPSW", extensions: ["ipsw"] }] });
              if (selected) setIpsw(String(selected));
            }}
          >
            Pick IPSW
          </button>
          <button
            className="btn btn-secondary btn-sm"
            onClick={async () => {
              const selected = await open({ filters: [{ name: "SHSH2", extensions: ["shsh2"] }] });
              if (selected) setShsh(String(selected));
            }}
          >
            Pick SHSH2
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

