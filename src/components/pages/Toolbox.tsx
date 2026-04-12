import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import Terminal from "../Terminal";
import { SpotlightFeatureCard } from "../ui/spotlight-feature-card";
import { Shield, Trash, FileText, Database } from "lucide-react";

export default function ToolboxPage() {
  const [output, setOutput] = useState("");
  const [status, setStatus] = useState<"idle"|"running"|"success"|"error">("idle");

  const run = async (id: string, args: Record<string, string> = {}) => {
    setStatus("running"); setOutput("");
    try { setOutput(await invoke<string>(id, args)); setStatus("success"); }
    catch (e: any) { setOutput(String(e)); setStatus("error"); }
  };

  return (
    <div>
      <h2 style={{ fontSize: 18, fontWeight: 700, marginBottom: 4 }}>🛠️ Toolbox</h2>
      <p style={{ fontSize: 12, color: "#64748b", marginBottom: 20 }}>
        OTA Block · Factory Reset · System Logs · Deep Backups
      </p>

      {/* Toolbox Operations - Spotlight Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <SpotlightFeatureCard
          icon={<Shield className="w-6 h-6 text-cyan-400" />}
          title="OTA Blocker"
          description="Prevent Apple from auto-updating your device"
          glowColor="blue"
          onClick={() => run("toolbox_block_ota")}
        />
        
        <SpotlightFeatureCard
          icon={<Trash className="w-6 h-6 text-red-400" />}
          title="Factory Reset"
          description="Complete wipe of all user data and settings"
          glowColor="red"
          onClick={() => run("toolbox_factory_reset")}
        />
        
        <SpotlightFeatureCard
          icon={<FileText className="w-6 h-6 text-green-400" />}
          title="System Logs"
          description="Stream live device logs (idevicesyslog)"
          glowColor="green"
          onClick={() => run("toolbox_get_logs")}
        />
        
        <SpotlightFeatureCard
          icon={<Database className="w-6 h-6 text-purple-400" />}
          title="Deep Backup"
          description="Full local backup via idevicebackup2"
          glowColor="purple"
          onClick={() => run("toolbox_backup_device", { path: "~/DeepEyeUnlocker/Backups" })}
        />
      </div>

      <Terminal output={output} status={status} />
    </div>
  );
}
