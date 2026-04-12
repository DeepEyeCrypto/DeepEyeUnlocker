import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import Terminal from "../Terminal";
import { SpotlightFeatureCard } from "../ui/spotlight-feature-card";
import { Zap, Shield, Link, Bomb, Radio, Package } from "lucide-react";

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

      {/* PwnDFU State - Spotlight Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
        <SpotlightFeatureCard
          icon={<Bomb className="w-6 h-6 text-red-400" />}
          title="Run Gaster PWN"
          description="Exploit device to enter PwnDFU mode"
          glowColor="red"
          onClick={() => run("ios_run_gaster_pwn")}
        />
        
        <SpotlightFeatureCard
          icon={<Shield className="w-6 h-6 text-cyan-400" />}
          title="Check Pwned State"
          description="Verify if device is in PwnDFU mode"
          glowColor="blue"
          onClick={() => run("ios_check_pwn_state")}
        />
      </div>

      {/* Integrated Exploits - Spotlight Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
        <SpotlightFeatureCard
          icon={<Zap className="w-6 h-6 text-orange-400" />}
          title="checkra1n"
          description="Jailbreak for A5-A11 devices"
          glowColor="orange"
          onClick={() => run("ios_run_checkra1n")}
        />
        
        <SpotlightFeatureCard
          icon={<Link className="w-6 h-6 text-purple-400" />}
          title="palera1n"
          description="Rootful/rootless jailbreak for A8-A11"
          glowColor="purple"
          onClick={() => run("ios_run_palera1n")}
        />
      </div>

      {/* Advanced Primitives - Spotlight Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
        <SpotlightFeatureCard
          icon={<Zap className="w-5 h-5 text-green-400" />}
          title="Surgical Patch"
          description="Inject targeted memory patches"
          glowColor="green"
          onClick={() => run("ios_inject_surgical_patch")}
        />
        
        <SpotlightFeatureCard
          icon={<Radio className="w-5 h-5 text-cyan-400" />}
          title="Poll Orchestrator"
          description="Check exploit orchestrator status"
          glowColor="blue"
          onClick={() => run("ios_poll_orchestrator")}
        />
        
        <SpotlightFeatureCard
          icon={<Package className="w-5 h-5 text-orange-400" />}
          title="Boot Ramdisk"
          description="Load custom ramdisk environment"
          glowColor="orange"
          onClick={() => run("ios_boot_ramdisk")}
        />
      </div>

      <Terminal output={output} status={status} />
    </div>
  );
}
