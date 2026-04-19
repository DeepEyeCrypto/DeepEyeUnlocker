import { ReactNode, useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { open } from "@tauri-apps/plugin-dialog";
import Terminal from "../Terminal";
import { SpotlightFeatureCard } from "../ui/spotlight-feature-card";
import { Download, Save, Settings, Upload, FileCheck } from "lucide-react";

type ShshDeviceInfo = {
  ecid: string;
  model: string;
  hardwareModel: string;
  boardId: string;
  chipId: string;
  productVersion: string;
};

type RestoreMode = "noBaseband" | "latestBaseband";

const GENERATOR_PATTERN = /^0x[a-fA-F0-9]{16}$/;

function normalizePathSelection(selected: string | string[] | null): string {
  if (Array.isArray(selected)) {
    return selected[0] ?? "";
  }

  return selected ?? "";
}

function fileLabel(path: string): string {
  if (!path) {
    return "No file selected";
  }

  const normalized = path.split(/[\\/]/);
  return normalized[normalized.length - 1] || path;
}

export default function SHSHPage() {
  const [output, setOutput] = useState("");
  const [status, setStatus] = useState<"idle" | "running" | "success" | "error">("idle");
  const [model, setModel] = useState("");
  const [ecid, setEcid] = useState("");
  const [ios, setIos] = useState("");
  const [generator, setGenerator] = useState("0x1111111111111111");
  const [ipsw, setIpsw] = useState("");
  const [shsh, setShsh] = useState("");
  const [restoreMode, setRestoreMode] = useState<RestoreMode>("noBaseband");

  const trimmedModel = model.trim();
  const trimmedEcid = ecid.trim();
  const trimmedIos = ios.trim();
  const trimmedGenerator = generator.trim();
  const trimmedIpsw = ipsw.trim();
  const trimmedShsh = shsh.trim();

  const hasModel = trimmedModel.length > 0;
  const hasEcid = trimmedEcid.length > 0;
  const hasIos = trimmedIos.length > 0;
  const hasGenerator = GENERATOR_PATTERN.test(trimmedGenerator);
  const hasRestoreFiles = trimmedIpsw.length > 0 && trimmedShsh.length > 0;

  const run = async (id: string, args: Record<string, string> = {}) => {
    setStatus("running");
    setOutput("");
    try {
      const normalizedArgs = Object.fromEntries(
        Object.entries(args).map(([key, value]) => [key, value.trim()]),
      ) as Record<string, string>;
      setOutput(await invoke<string>(id, normalizedArgs));
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
      const info = await invoke<ShshDeviceInfo>("get_shsh_device_info");
      setModel(info.model);
      setEcid(info.ecid);
      setOutput(
        [
          `Model: ${info.model}`,
          `ECID: ${info.ecid}`,
          `HardwareModel: ${info.hardwareModel}`,
          `BoardId: ${info.boardId}`,
          `ChipID: ${info.chipId}`,
          `iOS: ${info.productVersion}`,
        ].join("\n"),
      );
      setStatus("success");
    } catch (e: unknown) {
      setOutput(String(e));
      setStatus("error");
    }
  };

  const runRestore = async () => {
    if (!hasRestoreFiles) {
      return;
    }

    if (restoreMode === "noBaseband") {
      await run("futurerestore_no_baseband", { ipsw_path: trimmedIpsw, shsh_path: trimmedShsh });
      return;
    }

    await run("futurerestore", {
      ipsw_path: trimmedIpsw,
      shsh_path: trimmedShsh,
      sep_manifest: "",
      baseband: "",
    });
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
          <Field label="Generator (optional for collision blobs)">
            <input className="field-input" value={generator} onChange={(e) => setGenerator(e.target.value)} />
          </Field>
          <button className="btn btn-primary btn-sm" onClick={autoFill}>Auto-Fill</button>
        </div>
        <div className="mt-3 text-xs text-white/65">
          Save with Generator requires a valid nonce generator in the format <code>0x1111111111111111</code>.
        </div>
      </div>

      {/* SHSH Operations - Spotlight Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-6">
        <SpotlightFeatureCard
          icon={<Save className="w-6 h-6 text-green-400" />}
          title="Save All Signed"
          description="Save blobs for all Apple-signed firmwares now"
          glowColor="green"
          onClick={() => run("save_shsh_all_signed", { model: trimmedModel, ecid: trimmedEcid })}
          disabled={!hasModel || !hasEcid}
          badge={hasModel && hasEcid ? "Ready" : "Model + ECID"}
        />
        
        <SpotlightFeatureCard
          icon={<Download className="w-6 h-6 text-cyan-400" />}
          title="Save Specific Version"
          description="Save blob for one iOS version"
          glowColor="blue"
          onClick={() => run("save_shsh_specific", { model: trimmedModel, ecid: trimmedEcid, ios: trimmedIos })}
          disabled={!hasModel || !hasEcid || !hasIos}
          badge={hasModel && hasEcid && hasIos ? "Ready" : "Model + ECID + iOS"}
        />
        
        <SpotlightFeatureCard
          icon={<Settings className="w-6 h-6 text-purple-400" />}
          title="Save with Generator"
          description="Nonce collision blob for downgrade workflow"
          glowColor="purple"
          onClick={() => run("save_shsh_with_generator", {
            model: trimmedModel,
            ecid: trimmedEcid,
            ios: trimmedIos,
            generator: trimmedGenerator,
          })}
          disabled={!hasModel || !hasEcid || !hasIos || !hasGenerator}
          badge={hasModel && hasEcid && hasIos && hasGenerator ? "Ready" : "Needs valid generator"}
        />
        
        <SpotlightFeatureCard
          icon={<FileCheck className="w-6 h-6 text-orange-400" />}
          title="Check Signed Versions"
          description="Show versions currently signed by Apple"
          glowColor="orange"
          onClick={() => run("check_signed_versions", { model: trimmedModel })}
          disabled={!hasModel}
          badge={hasModel ? "Ready" : "Model required"}
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
          title={restoreMode === "noBaseband" ? "futurerestore (--no-baseband)" : "futurerestore"}
          description={!trimmedIpsw ? "Select IPSW and SHSH2 files to downgrade" : `IPSW: ...${trimmedIpsw.slice(-40)}`}
          glowColor="red"
          onClick={runRestore}
          disabled={!hasRestoreFiles}
          badge={!hasRestoreFiles ? "Select files" : restoreMode === "noBaseband" ? "No baseband" : "Latest baseband"}
        />
        
        {/* File Selection Buttons */}
        <div className="grid grid-cols-2 gap-3 mt-3">
          <button
            className="btn btn-primary btn-sm"
            onClick={async () => {
              const selected = await open({ filters: [{ name: "IPSW", extensions: ["ipsw"] }] });
              const path = normalizePathSelection(selected);
              if (path) setIpsw(path);
            }}
          >
            Pick IPSW
          </button>
          <button
            className="btn btn-secondary btn-sm"
            onClick={async () => {
              const selected = await open({ filters: [{ name: "SHSH2", extensions: ["shsh2"] }] });
              const path = normalizePathSelection(selected);
              if (path) setShsh(path);
            }}
          >
            Pick SHSH2
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mt-3 text-xs text-white/70">
          <div className="panel">
            <div className="field-label">Selected IPSW</div>
            <div>{fileLabel(trimmedIpsw)}</div>
          </div>
          <div className="panel">
            <div className="field-label">Selected SHSH2</div>
            <div>{fileLabel(trimmedShsh)}</div>
          </div>
        </div>

        <div className="flex gap-3 mt-3">
          <button
            type="button"
            className={`btn btn-sm ${restoreMode === "noBaseband" ? "btn-primary" : "btn-secondary"}`}
            onClick={() => setRestoreMode("noBaseband")}
          >
            No Baseband
          </button>
          <button
            type="button"
            className={`btn btn-sm ${restoreMode === "latestBaseband" ? "btn-primary" : "btn-secondary"}`}
            onClick={() => setRestoreMode("latestBaseband")}
          >
            Latest Baseband
          </button>
        </div>
      </div>

      <Terminal output={output} status={status} />
    </div>
  );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div>
      <div className="field-label">{label}</div>
      {children}
    </div>
  );
}
