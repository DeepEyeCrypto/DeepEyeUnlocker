import { GlowCard } from "@/components/ui/spotlight-card";
import { 
  Smartphone, 
  Shield, 
  Unlock, 
  Cpu, 
  Cloud, 
  Terminal,
  Settings,
  Zap
} from "lucide-react";

export function SpotlightDemo() {
  const features = [
    {
      icon: <Smartphone className="w-6 h-6 text-cyan-400" />,
      title: "Android Bypass",
      description: "FRP, MDM, and Knox bypass for all Android devices",
      color: "blue" as const,
    },
    {
      icon: <Shield className="w-6 h-6 text-purple-400" />,
      title: "iOS iCloud Bypass",
      description: "Bypass iCloud activation lock on supported devices",
      color: "purple" as const,
    },
    {
      icon: <Unlock className="w-6 h-6 text-green-400" />,
      title: "Network Unlock",
      description: "Carrier unlock for locked devices",
      color: "green" as const,
    },
    {
      icon: <Cpu className="w-6 h-6 text-orange-400" />,
      title: "EDL Mode",
      description: "Emergency Download Mode flash and repair",
      color: "orange" as const,
    },
    {
      icon: <Cloud className="w-6 h-6 text-blue-400" />,
      title: "Cloud Vault",
      description: "Secure cloud backup and forensic storage",
      color: "blue" as const,
    },
    {
      icon: <Terminal className="w-6 h-6 text-red-400" />,
      title: "ADB Shell",
      description: "Direct ADB access with custom commands",
      color: "red" as const,
    },
    {
      icon: <Settings className="w-6 h-6 text-cyan-400" />,
      title: "Device Settings",
      description: "Configure device-specific parameters",
      color: "blue" as const,
    },
    {
      icon: <Zap className="w-6 h-6 text-purple-400" />,
      title: "MTK BROM",
      description: "MediaTek BROM exploit and flash tools",
      color: "purple" as const,
    },
  ];

  return (
    <div className="min-h-screen bg-black p-8">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="text-center mb-12">
          <h1 className="text-5xl font-bold text-white mb-4">
            DeepEye Unlocker
          </h1>
          <p className="text-white/60 text-lg">
            Professional Device Unlocking & Forensic Tools
          </p>
        </div>

        {/* Feature Grid with Spotlight Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-12">
          {features.map((feature, index) => (
            <GlowCard 
              key={index}
              glowColor={feature.color}
              customSize={true}
              className="min-h-[200px] cursor-pointer hover:scale-[1.02] transition-transform"
            >
              <div className="flex flex-col gap-3">
                {/* Icon */}
                <div className="w-12 h-12 rounded-xl bg-white/10 backdrop-blur-sm flex items-center justify-center">
                  {feature.icon}
                </div>

                {/* Content */}
                <div className="flex-1">
                  <h3 className="text-white font-bold text-lg mb-2">
                    {feature.title}
                  </h3>
                  <p className="text-white/70 text-sm leading-relaxed">
                    {feature.description}
                  </p>
                </div>
              </div>
            </GlowCard>
          ))}
        </div>

        {/* Large Showcase Cards */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          <GlowCard 
            glowColor="blue"
            customSize={true}
            width="100%"
            height="300px"
            className="cursor-pointer hover:scale-[1.01] transition-transform"
          >
            <div className="flex items-center gap-6">
              <div className="w-20 h-20 rounded-2xl bg-cyan-500/20 flex items-center justify-center">
                <Smartphone className="w-10 h-10 text-cyan-400" />
              </div>
              <div className="flex-1">
                <h2 className="text-white font-bold text-2xl mb-2">
                  Android Forensic Acquisition
                </h2>
                <p className="text-white/70 leading-relaxed">
                  Complete bit-level acquisition with double-layer decryption. 
                  Support for EDL, ADB, and Fastboot modes.
                </p>
              </div>
            </div>
          </GlowCard>

          <GlowCard 
            glowColor="purple"
            customSize={true}
            width="100%"
            height="300px"
            className="cursor-pointer hover:scale-[1.01] transition-transform"
          >
            <div className="flex items-center gap-6">
              <div className="w-20 h-20 rounded-2xl bg-purple-500/20 flex items-center justify-center">
                <Shield className="w-10 h-10 text-purple-400" />
              </div>
              <div className="flex-1">
                <h2 className="text-white font-bold text-2xl mb-2">
                  iOS Bypass Engine
                </h2>
                <p className="text-white/70 leading-relaxed">
                  Advanced checkm8 exploit with palera1n integration. 
                  Support for A11-A17 chips with DFU mode operations.
                </p>
              </div>
            </div>
          </GlowCard>
        </div>
      </div>
    </div>
  );
}
