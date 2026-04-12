import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import Terminal from "../Terminal";
import { SpotlightFeatureCard } from "../ui/spotlight-feature-card";
import { MapPin, User, UserX } from "lucide-react";

export default function FmiPage() {
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
    <div className="page">
      <h2 className="page-title">FMI / iCloud</h2>
      <p className="page-subtitle">
        Find My iPhone Status · Apple ID Removal · Token Cleaning
      </p>

      {/* FMI Operations - Spotlight Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <SpotlightFeatureCard
          icon={<MapPin className="w-6 h-6 text-cyan-400" />}
          title="Read FMI Status"
          description="Checks if Find My is enabled via official Apple servers"
          glowColor="blue"
          onClick={() => run("ios_fmi_state")}
        />
        
        <SpotlightFeatureCard
          icon={<User className="w-6 h-6 text-purple-400" />}
          title="Check Apple ID"
          description="View current Apple ID account state"
          glowColor="purple"
          onClick={() => run("ios_apple_id_state")}
        />
        
        <SpotlightFeatureCard
          icon={<UserX className="w-6 h-6 text-red-400" />}
          title="Remove Apple ID"
          description="Disconnect Apple ID from device"
          glowColor="red"
          onClick={() => run("ios_remove_apple_id")}
        />
      </div>

      <div className="danger-note">
        <div className="danger-title">PRO FORENSIC MODE</div>
        <p className="meta-text">
          FMI OFF requires valid session tokens or an open menu state. If the menu is closed, use the <strong>MASS EXTRACTION</strong> tool to attempt token recovery before running removal logic.
        </p>
      </div>

      <Terminal output={output} status={status} />
    </div>
  );
}
