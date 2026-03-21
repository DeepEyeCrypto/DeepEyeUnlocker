import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { open } from "@tauri-apps/plugin-dialog";
import Terminal from "../Terminal";

type Status = "idle" | "running" | "success" | "error";

function useRunner() {
  const [out, setOut] = useState("");
  const [st, setSt] = useState<Status>("idle");
  const run = async (cmd: string, args: Record<string, any> = {}) => {
    setSt("running"); setOut("");
    try { 
      const res = await invoke<string>(cmd, args);
      setOut(res); 
      setSt("success"); 
    }
    catch (e: any) { 
      setOut(String(e)); 
      setSt("error"); 
    }
  };
  return { out, st, run };
}

// ── NONCE PANEL ──────────────────────────────────
function NoncePanel() {
  const { out, st, run } = useRunner();
  const [gen, setGen] = useState("0x1111111111111111");
  const [blobPath, setBlobPath] = useState("");

  return (
    <section>
      <h3 style={{ fontSize: 15, fontWeight: 700, marginBottom: 12, color: "#a78bfa" }}>
        🔢 Nonce Generator Engine
      </h3>
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10, marginBottom: 12 }}>
        <div className="glass" style={{ padding: 14 }}>
          <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 8 }}>Current Nonce</div>
          <button className="btn" style={{ width: "100%" }}
            onClick={() => run("get_current_nonce")} disabled={st === "running"}>
            ▶ Read Nonce
          </button>
        </div>
        <div className="glass" style={{ padding: 14 }}>
          <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 8 }}>Set Generator</div>
          <input value={gen} onChange={e => setGen(e.target.value)} placeholder="0x1111111111111111"
            style={{ width: "100%", padding: "6px 10px", borderRadius: 6, marginBottom: 8,
              background: "rgba(255,255,255,0.05)", border: "1px solid rgba(255,255,255,0.1)",
              color: "#e2e8f0", fontSize: 11 }} />
          <div style={{ display: "flex", gap: 6 }}>
            <button className="btn warn" style={{ flex: 1 }}
              onClick={() => run("set_nonce_generator", { generator: gen })}
              disabled={st === "running"}>irecovery</button>
            <button className="btn warn" style={{ flex: 1 }}
              onClick={() => run("set_nonce_checkra1n", { generator: gen })}
              disabled={st === "running"}>checkra1n</button>
          </div>
        </div>
        <div className="glass" style={{ padding: 14 }}>
          <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 8 }}>Set from SHSH2 Blob</div>
          <button className="btn" style={{ width: "100%", marginBottom: 8 }}
            onClick={async () => {
              const f = await open({ filters: [{ name: "SHSH2", extensions: ["shsh2"] }] });
              if (f) setBlobPath(String(f));
            }}>📁 Pick .shsh2</button>
          <div style={{ fontSize: 10, color: "#64748b", marginBottom: 6 }}>
            {blobPath ? `...${blobPath.slice(-35)}` : "No blob selected"}
          </div>
          <div style={{ display: "flex", gap: 6 }}>
            <button className="btn" style={{ flex: 1 }}
              onClick={() => run("get_generator_from_blob", { blob_path: blobPath })}
              disabled={!blobPath || st === "running"}>Read Gen</button>
            <button className="btn primary" style={{ flex: 1 }}
              onClick={() => run("set_nonce_from_blob", { blob_path: blobPath })}
              disabled={!blobPath || st === "running"}>Set Gen</button>
          </div>
        </div>
        <div className="glass" style={{ padding: 14 }}>
          <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 8 }}>Clear Nonce</div>
          <div style={{ fontSize: 11, color: "#64748b", marginBottom: 10 }}>Reset to random on next boot</div>
          <button className="btn danger" style={{ width: "100%" }}
            onClick={() => run("clear_nonce")} disabled={st === "running"}>
            ▶ Clear Nonce
          </button>
        </div>
      </div>
      <Terminal output={out} status={st} />
    </section>
  );
}

// ── SSH TUNNEL PANEL ──────────────────────────────
function SSHPanel() {
  const { out, st, run } = useRunner();
  const [cmd, setCmd] = useState("uname -a");
  const [remotePath, setRemotePath] = useState("/var/mobile/");
  const [localPath, setLocalPath] = useState("");
  const [pkg, setPkg] = useState("");

  return (
    <section style={{ marginTop: 24 }}>
      <h3 style={{ fontSize: 15, fontWeight: 700, marginBottom: 12, color: "#38bdf8" }}>
        🔐 SSH Tunnel Manager
      </h3>
      <div style={{ display: "flex", gap: 8, marginBottom: 12 }}>
        {[
          { label: "▶ Start Tunnel", cmd: "start_ssh_tunnel", color: "success" },
          { label: "■ Stop Tunnel",  cmd: "stop_ssh_tunnel",  color: "danger" },
          { label: "◉ Status",       cmd: "check_tunnel_status", color: "" },
        ].map(b => (
          <button key={b.cmd} className={`btn ${b.color}`} style={{ flex: 1 }}
            onClick={() => run(b.cmd)} disabled={st === "running"}>{b.label}</button>
        ))}
      </div>

      <div className="glass" style={{ padding: 14, marginBottom: 10 }}>
        <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 8 }}>Run SSH Command</div>
        <div style={{ display: "flex", gap: 8 }}>
          <input value={cmd} onChange={e => setCmd(e.target.value)}
            placeholder="e.g. cat /etc/hosts"
            style={{ flex: 1, padding: "8px 10px", borderRadius: 8,
              background: "rgba(255,255,255,0.05)", border: "1px solid rgba(255,255,255,0.1)",
              color: "#e2e8f0", fontSize: 12, fontFamily: "monospace" }} />
          <button className="btn primary" onClick={() => run("run_ssh_command", { cmd })}
            disabled={st === "running"}>▶ Run</button>
          <button className="btn warn" onClick={() => run("run_su_command", { cmd })}
            disabled={st === "running"}>▶ sudo</button>
        </div>
      </div>

      <div className="glass" style={{ padding: 14, marginBottom: 10 }}>
        <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 8 }}>File Transfer (SCP)</div>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8, marginBottom: 8 }}>
          <div>
            <div style={{ fontSize: 10, color: "#64748b", marginBottom: 4 }}>Local Path</div>
            <div style={{ display: "flex", gap: 6 }}>
              <input value={localPath} onChange={e => setLocalPath(e.target.value)}
                style={{ flex: 1, padding: "6px 10px", borderRadius: 6,
                  background: "rgba(255,255,255,0.05)", border: "1px solid rgba(255,255,255,0.1)",
                  color: "#e2e8f0", fontSize: 11 }} />
              <button className="btn" onClick={async () => {
                const f = await open(); if (f) setLocalPath(String(f));
              }}>📁</button>
            </div>
          </div>
          <div>
            <div style={{ fontSize: 10, color: "#64748b", marginBottom: 4 }}>Remote Path</div>
            <input value={remotePath} onChange={e => setRemotePath(e.target.value)}
              style={{ width: "100%", padding: "6px 10px", borderRadius: 6,
                background: "rgba(255,255,255,0.05)", border: "1px solid rgba(255,255,255,0.1)",
                color: "#e2e8f0", fontSize: 11, fontFamily: "monospace" }} />
          </div>
        </div>
        <div style={{ display: "flex", gap: 8 }}>
          <button className="btn" style={{ flex: 1 }}
            onClick={() => run("ssh_upload_file", { local_path: localPath, remote_path: remotePath })}
            disabled={!localPath || st === "running"}>⬆ Upload to Device</button>
          <button className="btn" style={{ flex: 1 }}
            onClick={() => run("ssh_download_file", { remote_path: remotePath, local_path: localPath || "~/DeepEyeUnlocker/downloads/" })}
            disabled={st === "running"}>⬇ Download from Device</button>
        </div>
      </div>

      <div className="glass" style={{ padding: 14 }}>
        <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 8 }}>Sileo / apt-get Package Installer</div>
        <div style={{ display: "flex", gap: 8 }}>
          <input value={pkg} onChange={e => setPkg(e.target.value)}
            placeholder="e.g. openssh, substrate, cydia"
            style={{ flex: 1, padding: "8px 10px", borderRadius: 8,
              background: "rgba(255,255,255,0.05)", border: "1px solid rgba(255,255,255,0.1)",
              color: "#e2e8f0", fontSize: 12 }} />
          <button className="btn success" onClick={() => run("install_sileo_pkg", { package_name: pkg })}
            disabled={!pkg || st === "running"}>📦 Install</button>
        </div>
      </div>

      <Terminal output={out} status={st} />
    </section>
  );
}

// ── FRIDA PANEL ───────────────────────────────────
function FridaPanel() {
  const { out, st, run } = useRunner();
  const [processTarget, setProcessTarget] = useState("");
  const [script, setScript] = useState(
`// Custom Frida script
ObjC.schedule(ObjC.mainQueue, function() {
  var UIDevice = ObjC.classes.UIDevice;
  var dev = UIDevice.currentDevice();
  console.log('[+] Name: ' + dev.name().toString());
  console.log('[+] iOS: ' + dev.systemVersion().toString());
});`
  );
  const [bundleId, setBundleId] = useState("");
  const [dylibPath, setDylibPath] = useState("");

  return (
    <section style={{ marginTop: 24 }}>
      <h3 style={{ fontSize: 15, fontWeight: 700, marginBottom: 12, color: "#4ade80" }}>
        🔬 Frida Instrumentation Engine
      </h3>

      <div style={{ display: "flex", gap: 8, marginBottom: 12, flexWrap: "wrap" }}>
        <button className="btn success" onClick={() => run("frida_ps")}
          disabled={st === "running"}>📋 List Processes</button>
        <button className="btn" onClick={() => run("mount_dev_disk_image")}
          disabled={st === "running"}>💿 Mount Dev Disk</button>
        <button className="btn" onClick={() => run("check_dev_disk_mounted")}
          disabled={st === "running"}>◉ Dev Disk Status</button>
        <button className="btn" onClick={() => run("get_screenshot")}
          disabled={st === "running"}>📸 Screenshot</button>
      </div>

      <div className="glass" style={{ padding: 14, marginBottom: 10 }}>
        <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 8 }}>Target Process / Bundle ID</div>
        <div style={{ display: "flex", gap: 8, marginBottom: 8 }}>
          <input value={processTarget} onChange={e => setProcessTarget(e.target.value)}
            placeholder="e.g. SpringBoard or com.apple.AppStore"
            style={{ flex: 1, padding: "8px 10px", borderRadius: 8,
              background: "rgba(255,255,255,0.05)", border: "1px solid rgba(255,255,255,0.1)",
              color: "#e2e8f0", fontSize: 12 }} />
        </div>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          {[
            { label: "Attach",        cmd: "frida_attach",       arg: { process_name: processTarget } },
            { label: "List Exports",  cmd: "frida_list_exports",  arg: { process: processTarget } },
            { label: "Kill Process",  cmd: "frida_kill_process",  arg: { pid: processTarget } },
            { label: "🔓 SSL KillSwitch", cmd: "ssl_kill_switch", arg: { process: processTarget } },
          ].map(b => (
            <button key={b.label} className="btn" onClick={() => run(b.cmd, b.arg)}
              disabled={!processTarget || st === "running"}>{b.label}</button>
          ))}
        </div>
      </div>

      <div className="glass" style={{ padding: 14, marginBottom: 10 }}>
        <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 8 }}>Custom Frida Script</div>
        <textarea value={script} onChange={e => setScript(e.target.value)}
          rows={8}
          style={{
            width: "100%", padding: "10px 12px", borderRadius: 8, marginBottom: 8,
            background: "rgba(0,0,0,0.4)", border: "1px solid rgba(255,255,255,0.1)",
            color: "#a3e635", fontSize: 11, fontFamily: "monospace", resize: "vertical"
          }} />
        <button className="btn primary" style={{ width: "100%" }}
          onClick={() => run("frida_run_script", { process: processTarget, script })}
          disabled={!processTarget || st === "running"}>
          ▶ Run Script on {processTarget || "[process]"}
        </button>
      </div>

      <div className="glass" style={{ padding: 14 }}>
        <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 8 }}>Inject .dylib</div>
        <div style={{ display: "flex", gap: 8, marginBottom: 8 }}>
          <input value={bundleId} onChange={e => setBundleId(e.target.value)}
            placeholder="Bundle ID" style={{ flex: 1, padding: "7px 10px", borderRadius: 6,
              background: "rgba(255,255,255,0.05)", border: "1px solid rgba(255,255,255,0.1)",
              color: "#e2e8f0", fontSize: 11 }} />
          <button className="btn" onClick={async () => {
            const f = await open({ filters: [{ name: "dylib", extensions: ["dylib"] }] });
            if (f) setDylibPath(String(f));
          }}>📁 Pick .dylib</button>
        </div>
        <div style={{ fontSize: 10, color: "#64748b", marginBottom: 8 }}>
          {dylibPath ? `Selected: ...${dylibPath.slice(-40)}` : "No dylib selected"}
        </div>
        <button className="btn warn" style={{ width: "100%" }}
          onClick={() => run("inject_dylib", { bundle_id: bundleId, dylib_path: dylibPath })}
          disabled={!bundleId || !dylibPath || st === "running"}>
          ⚡ Inject .dylib
        </button>
      </div>

      <Terminal output={out} status={st} />
    </section>
  );
}

// ── IPSW DOWNLOADER ──────────────────────────────
function IPSWPanel() {
  const { out, st, run } = useRunner();
  const [identifier, setIdentifier] = useState("iPhone11,8");
  const [buildId, setBuildId] = useState("");
  const [ipswPath, setIpswPath] = useState("");

  return (
    <section style={{ marginTop: 24 }}>
      <h3 style={{ fontSize: 15, fontWeight: 700, marginBottom: 12, color: "#fbbf24" }}>
        ⬇️ IPSW Downloader
      </h3>
      <div className="glass" style={{ padding: 14, marginBottom: 10 }}>
        <div style={{ display: "flex", gap: 8, marginBottom: 10 }}>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 10, color: "#64748b", marginBottom: 4 }}>Device Identifier</div>
            <input value={identifier} onChange={e => setIdentifier(e.target.value)}
              placeholder="iPhone11,8"
              style={{ width: "100%", padding: "7px 10px", borderRadius: 6,
                background: "rgba(255,255,255,0.05)", border: "1px solid rgba(255,255,255,0.1)",
                color: "#e2e8f0", fontSize: 12 }} />
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 10, color: "#64748b", marginBottom: 4 }}>Build ID (for download)</div>
            <input value={buildId} onChange={e => setBuildId(e.target.value)}
              placeholder="e.g. 22D82"
              style={{ width: "100%", padding: "7px 10px", borderRadius: 6,
                background: "rgba(255,255,255,0.05)", border: "1px solid rgba(255,255,255,0.1)",
                color: "#e2e8f0", fontSize: 12 }} />
          </div>
        </div>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button className="btn success" onClick={() => run("get_signed_firmwares", { identifier })}
            disabled={!identifier || st === "running"}>✅ Signed Firmwares</button>
          <button className="btn" onClick={() => run("get_all_firmwares", { identifier })}
            disabled={!identifier || st === "running"}>📋 All Firmwares</button>
          <button className="btn primary" onClick={() => run("download_ipsw", { identifier, build_id: buildId })}
            disabled={!identifier || !buildId || st === "running"}>⬇ Download IPSW</button>
          <button className="btn" onClick={() => run("get_download_progress")}
            disabled={st === "running"}>📊 Progress</button>
        </div>
      </div>
      <div className="glass" style={{ padding: 14 }}>
        <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 8 }}>Verify IPSW SHA1</div>
        <button className="btn" style={{ marginBottom: 8 }} onClick={async () => {
          const f = await open({ filters: [{ name: "IPSW", extensions: ["ipsw"] }] });
          if (f) setIpswPath(String(f));
        }}>📁 Pick IPSW File</button>
        <div style={{ fontSize: 10, color: "#64748b", marginBottom: 8 }}>
          {ipswPath ? `...${ipswPath.slice(-40)}` : "No file selected"}
        </div>
        <button className="btn warn" style={{ width: "100%" }}
          onClick={() => run("verify_ipsw_sha1", { ipsw_path: ipswPath, identifier, build_id: buildId })}
          disabled={!ipswPath || !buildId || st === "running"}>
          🔍 Verify SHA1
        </button>
      </div>
      <Terminal output={out} status={st} />
    </section>
  );
}

// ── CRASH LOGS ───────────────────────────────────
function CrashPanel() {
  const { out, st, run } = useRunner();
  const [file, setFile] = useState("");
  const [dsym, setDsym] = useState("");

  return (
    <section style={{ marginTop: 24 }}>
      <h3 style={{ fontSize: 15, fontWeight: 700, marginBottom: 12, color: "#f87171" }}>
        💥 Crash Log Engine
      </h3>
      <div style={{ display: "flex", gap: 8, marginBottom: 12, flexWrap: "wrap" }}>
        {[
          { label: "⬇ Pull Logs", cmd: "pull_crash_logs" },
          { label: "📋 List Logs", cmd: "list_crash_logs" },
          { label: "🗑 Clear Logs", cmd: "clear_crash_logs" },
        ].map(b => (
          <button key={b.cmd} className="btn" onClick={() => run(b.cmd)}
            disabled={st === "running"}>{b.label}</button>
        ))}
      </div>
      <div className="glass" style={{ padding: 14, marginBottom: 10 }}>
        <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 8 }}>Read / Symbolicate Log</div>
        <div style={{ display: "flex", gap: 8, marginBottom: 8 }}>
          <input value={file} onChange={e => setFile(e.target.value)}
            placeholder="filename.ips or .crash"
            style={{ flex: 1, padding: "7px 10px", borderRadius: 6,
              background: "rgba(255,255,255,0.05)", border: "1px solid rgba(255,255,255,0.1)",
              color: "#e2e8f0", fontSize: 11 }} />
          <button className="btn" onClick={() => run("read_crash_log", { filename: file })}
            disabled={!file || st === "running"}>Read</button>
        </div>
        <button className="btn" style={{ marginBottom: 8 }} onClick={async () => {
          const f = await open({ filters: [{ name: "dSYM", extensions: ["dSYM", "dsym"] }] });
          if (f) setDsym(String(f));
        }}>📁 Pick dSYM</button>
        <div style={{ fontSize: 10, color: "#64748b", marginBottom: 8 }}>
          {dsym ? `dSYM: ...${dsym.slice(-35)}` : "No dSYM selected"}
        </div>
        <button className="btn warn" style={{ width: "100%" }}
          onClick={() => run("symbolicate_log", {
            log_path: `~/DeepEyeUnlocker/crash_logs/${file}`,
            dsym_path: dsym
          })}
          disabled={!file || !dsym || st === "running"}>
          ⚡ Symbolicate
        </button>
      </div>
      <Terminal output={out} status={st} />
    </section>
  );
}

// ── AFC2 FILESYSTEM PANEL ─────────────────────────
function AFCPanel() {
  const { out, st, run } = useRunner();
  const [path, setPath] = useState("/");
  const [content, setContent] = useState("");

  return (
    <section style={{ marginTop: 24 }}>
      <h3 style={{ fontSize: 15, fontWeight: 700, marginBottom: 12, color: "#6366f1" }}>
        📂 AFC2 Filesystem Browser
      </h3>
      <div style={{ display: "flex", gap: 8, marginBottom: 12 }}>
        <button className="btn success" onClick={() => run("mount_afc2")}
          disabled={st === "running"}>📦 Mount AFC2</button>
      </div>

      <div className="glass" style={{ padding: 14, marginBottom: 10 }}>
        <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 8 }}>Path Explorer</div>
        <div style={{ display: "flex", gap: 8, marginBottom: 8 }}>
          <input value={path} onChange={e => setPath(e.target.value)}
            style={{ flex: 1, padding: "8px 10px", borderRadius: 8,
              background: "rgba(255,255,255,0.05)", border: "1px solid rgba(255,255,255,0.1)",
              color: "#e2e8f0", fontSize: 12, fontFamily: "monospace" }} />
          <button className="btn primary" onClick={() => run("list_directory", { path })}
            disabled={st === "running"}>List</button>
          <button className="btn" onClick={() => run("get_file_info", { path })}
            disabled={st === "running"}>Info</button>
        </div>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10, marginBottom: 10 }}>
        <div className="glass" style={{ padding: 14 }}>
          <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 8 }}>File Content</div>
          <button className="btn" style={{ width: "100%", marginBottom: 8 }}
            onClick={() => run("read_file", { path })} disabled={st === "running"}>Read File</button>
          <textarea value={content} onChange={e => setContent(e.target.value)}
            placeholder="Content to write..."
            rows={4} style={{ width: "100%", padding: 8, borderRadius: 6, marginBottom: 8,
              background: "rgba(0,0,0,0.2)", border: "1px solid rgba(255,255,255,0.1)",
              color: "#e2e8f0", fontSize: 11 }} />
          <button className="btn warn" style={{ width: "100%" }}
            onClick={() => run("write_file", { path, content })} disabled={st === "running"}>Write File</button>
        </div>
        <div className="glass" style={{ padding: 14 }}>
          <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 8 }}>Operations</div>
          <button className="btn danger" style={{ width: "100%", marginBottom: 8 }}
            onClick={() => run("delete_path", { path })} disabled={st === "running"}>Delete Path</button>
          <button className="btn" style={{ width: "100%", marginBottom: 8 }}
            onClick={() => run("make_directory", { path })} disabled={st === "running"}>MkDir</button>
          <div style={{ height: 1, background: "rgba(255,255,255,0.05)", margin: "8px 0" }} />
          <div style={{ fontSize: 10, color: "#64748b", marginBottom: 4 }}>Pull to Local</div>
          <button className="btn" style={{ width: "100%" }}
            onClick={async () => {
              const f = await open(); if (f) run("pull_file", { remote_path: path, local_path: String(f) });
            }}>⬇ Pull File</button>
        </div>
      </div>
      <Terminal output={out} status={st} />
    </section>
  );
}

// ── BACKUP PANEL ─────────────────────────────────
function BackupPanel() {
  const { out, st, run } = useRunner();
  const [label, setLabel] = useState(`Backup_${new Date().toISOString().split('T')[0]}`);
  const [pass, setPass] = useState("");
  const [appId, setAppId] = useState("");

  return (
    <section style={{ marginTop: 24 }}>
      <h3 style={{ fontSize: 15, fontWeight: 700, marginBottom: 12, color: "#ec4899" }}>
        💾 Full Backup & Restore Manager
      </h3>
      <div className="glass" style={{ padding: 14, marginBottom: 10 }}>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10, marginBottom: 12 }}>
          <div>
            <div style={{ fontSize: 10, color: "#64748b", marginBottom: 4 }}>Backup Label</div>
            <input value={label} onChange={e => setLabel(e.target.value)}
              style={{ width: "100%", padding: "7px 10px", borderRadius: 6,
                background: "rgba(255,255,255,0.05)", border: "1px solid rgba(255,255,255,0.1)",
                color: "#e2e8f0", fontSize: 11 }} />
          </div>
          <div>
            <div style={{ fontSize: 10, color: "#64748b", marginBottom: 4 }}>Encryption Password</div>
            <input type="password" value={pass} onChange={e => setPass(e.target.value)}
              style={{ width: "100%", padding: "7px 10px", borderRadius: 6,
                background: "rgba(255,255,255,0.05)", border: "1px solid rgba(255,255,255,0.1)",
                color: "#e2e8f0", fontSize: 11 }} />
          </div>
        </div>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button className="btn success" onClick={() => run("create_backup", { label })}
            disabled={st === "running"}>▶ Full Backup</button>
          <button className="btn warn" onClick={() => run("backup_encrypted", { label, password: pass })}
            disabled={!pass || st === "running"}>🔐 Encrypted Backup</button>
          <button className="btn danger" onClick={() => run("restore_backup", { label })}
            disabled={st === "running"}>🔄 Restore Backup</button>
          <button className="btn" onClick={() => run("list_backups")}
            disabled={st === "running"}>📋 List Backups</button>
        </div>
      </div>

      <div className="glass" style={{ padding: 14, marginBottom: 10 }}>
        <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 8 }}>Granular App Data Extraction</div>
        <div style={{ fontSize: 11, color: "#64748b", marginBottom: 8 }}>Target specific App Store / System bundle IDs</div>
        <div style={{ display: "flex", gap: 8 }}>
          <input value={appId} onChange={e => setAppId(e.target.value)} placeholder="e.g. com.apple.mobilesafari"
            style={{ flex: 1, padding: "8px 10px", borderRadius: 8,
              background: "rgba(255,255,255,0.05)", border: "1px solid rgba(255,255,255,0.1)",
              color: "#e2e8f0", fontSize: 11 }} />
          <button className="btn primary" onClick={() => run("extract_app_data", { bundle_id: appId })}
            disabled={!appId || st === "running"}>⬇ Extract</button>
          <button className="btn" onClick={() => run("restore_app_data", { bundle_id: appId })}
            disabled={!appId || st === "running"}>⬆ Restore</button>
        </div>
      </div>
      <Terminal output={out} status={st} />
    </section>
  );
}

// ── SIDELOADER PANEL ─────────────────────────────
function SideloaderPanel() {
  const { out, st, run } = useRunner();
  const [ipa, setIpa] = useState("");

  return (
    <section style={{ marginTop: 24 }}>
      <h3 style={{ fontSize: 15, fontWeight: 700, marginBottom: 12, color: "#22d3ee" }}>
        📦 IPA Sideloader & App Manager
      </h3>
      <div className="glass" style={{ padding: 14, marginBottom: 10 }}>
        <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 8 }}>Install Application</div>
        <div style={{ display: "flex", gap: 8, marginBottom: 10 }}>
          <input value={ipa} readOnly placeholder="Select .ipa file"
            style={{ flex: 1, padding: "7px 10px", borderRadius: 6,
              background: "rgba(255,255,255,0.05)", border: "1px solid rgba(255,255,255,0.1)",
              color: "#94a3b8", fontSize: 11 }} />
          <button className="btn" onClick={async () => {
            const f = await open({ filters: [{ name: "IPA", extensions: ["ipa"] }] });
            if (f) setIpa(String(f));
          }}>📁 Browse</button>
        </div>
        <div style={{ display: "flex", gap: 8 }}>
          <button className="btn primary" style={{ flex: 1 }}
            onClick={() => run("install_ipa", { path: ipa })}
            disabled={!ipa || st === "running"}>📦 Install IPA</button>
          <button className="btn warn" style={{ flex: 1 }}
            onClick={() => run("sign_and_install", { path: ipa })}
            disabled={!ipa || st === "running"}>✍️ Sign & Install</button>
        </div>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
        <div className="glass" style={{ padding: 14 }}>
          <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 8 }}>Inventory</div>
          <button className="btn" style={{ width: "100%", marginBottom: 8 }}
            onClick={() => run("list_installed_apps")} disabled={st === "running"}>📋 List All Apps</button>
          <button className="btn" style={{ width: "100%" }}
            onClick={() => run("uninstall_app", { bundle_id: "com.example.app" })}
            disabled={st === "running"}>🗑 Uninstall Demo</button>
        </div>
        <div className="glass" style={{ padding: 14 }}>
          <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 8 }}>Diagnostics</div>
          <button className="btn" style={{ width: "100%", marginBottom: 8 }}
            onClick={() => run("get_app_info", { bundle_id: "com.apple.Preferences" })}
            disabled={st === "running"}>ℹ️ App Info (Settings)</button>
          <button className="btn" style={{ width: "100%" }}
            onClick={() => run("reinstall_app", { bundle_id: "com.apple.mobilesafari" })}
            disabled={st === "running"}>🔄 Reinstall Safari</button>
        </div>
      </div>
      <Terminal output={out} status={st} />
    </section>
  );
}

// ── DEVELOPER PANEL ──────────────────────────────
function DeveloperPanel() {
  const { out, st, run } = useRunner();

  return (
    <section style={{ marginTop: 24 }}>
      <h3 style={{ fontSize: 15, fontWeight: 700, marginBottom: 12, color: "#818cf8" }}>
        🛠️ Developer Instruments & Mounts
      </h3>
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10, marginBottom: 12 }}>
        <div className="glass" style={{ padding: 14 }}>
          <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 10 }}>Disk Management</div>
          <button className="btn success" style={{ width: "100%", marginBottom: 8 }}
            onClick={() => run("mount_dev_disk_image")} disabled={st === "running"}>💿 Mount Dev Disk</button>
          <button className="btn danger" style={{ width: "100%", marginBottom: 8 }}
            onClick={() => run("unmount_dev_disk_image")} disabled={st === "running"}>⏏ Unmount Disk</button>
          <button className="btn" style={{ width: "100%" }}
            onClick={() => run("check_dev_disk_mounted")} disabled={st === "running"}>◉ Check Status</button>
        </div>
        <div className="glass" style={{ padding: 14 }}>
          <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 10 }}>Audit & Capture</div>
          <button className="btn primary" style={{ width: "100%", marginBottom: 8 }}
            onClick={() => run("list_processes")} disabled={st === "running"}>📋 Audit Processes</button>
          <button className="btn" style={{ width: "100%", marginBottom: 8 }}
            onClick={() => run("get_screenshot")} disabled={st === "running"}>📸 Take Screenshot</button>
          <div style={{ fontSize: 10, color: "#64748b", textAlign: "center" }}>
            Stored in: ~/DeepEyeUnlocker/media/
          </div>
        </div>
      </div>
      <Terminal output={out} status={st} />
    </section>
  );
}

// ── MASTER EXPORT ─────────────────────────────────
export default function AdvancedPage() {
  const [tab, setTab] = useState("nonce");

  const TABS = [
    { id: "nonce",  label: "🔢 Nonce"    },
    { id: "afc",    label: "📂 AFC2"     },
    { id: "backup", label: "💾 Backup"   },
    { id: "frida",  label: "🔬 Frida"    },
    { id: "ssh",    label: "🔐 SSH"      },
    { id: "ipa",    label: "📦 Side"     },
    { id: "ipsw",   label: "⬇️ IPSW"    },
    { id: "crash",  label: "💥 Crashes"  },
    { id: "dev",    label: "🛠️ Dev"     },
  ];

  return (
    <div>
      <h2 style={{ fontSize: 18, fontWeight: 700, marginBottom: 4 }}>⚡ Advanced Power Tools</h2>
      <p style={{ fontSize: 12, color: "#64748b", marginBottom: 16 }}>
        Nonce · AFC2 · Backup · Frida · SSH · Sideload · IPSW · Crashes · Instruments
      </p>

      {/* Tab bar */}
      <div style={{ display: "flex", gap: 4, marginBottom: 20,
        background: "rgba(0,0,0,0.3)", padding: 4, borderRadius: 12, flexWrap: "wrap" }}>
        {TABS.map(t => (
          <button key={t.id} onClick={() => setTab(t.id)} style={{
            flex: 1, minWidth: 60, padding: "7px 4px", borderRadius: 8, border: "none",
            background: tab === t.id
              ? "linear-gradient(135deg, rgba(124,58,237,0.5), rgba(37,99,235,0.4))"
              : "transparent",
            color: tab === t.id ? "#e2e8f0" : "#64748b",
            cursor: "pointer", fontSize: 11, fontWeight: tab === t.id ? 600 : 400,
            transition: "all 0.2s",
          }}>{t.label}</button>
        ))}
      </div>

      <div style={{ minHeight: "calc(100vh - 250px)" }}>
        {tab === "nonce"  && <NoncePanel />}
        {tab === "afc"    && <AFCPanel />}
        {tab === "backup" && <BackupPanel />}
        {tab === "frida"  && <FridaPanel />}
        {tab === "ssh"    && <SSHPanel />}
        {tab === "ipa"    && <SideloaderPanel />}
        {tab === "ipsw"   && <IPSWPanel />}
        {tab === "crash"  && <CrashPanel />}
        {tab === "dev"    && <DeveloperPanel />}
      </div>
    </div>
  );
}
