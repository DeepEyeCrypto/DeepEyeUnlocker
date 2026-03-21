import { useState } from "react";

export default function DeviceBar() {
  const [device, _setDevice] = useState<any>(null);
  const [isDetecting, setIsDetecting] = useState(false);

  const handleDetect = () => {
    setIsDetecting(true);
    // Placeholder logic for device detection hook
    setTimeout(() => {
      setIsDetecting(false);
      // setDevice({ name: "iPhone 12 Pro", os: "iOS 16.7.5", mode: "DFU Mode", ecid: "0x8020", chip: "A14 Bionic", cpid: "0x8101" });
    }, 1000);
  };

  return (
    <div className="glass" style={{
      padding: "12px 20px", borderRadius: 16,
      display: "flex", alignItems: "center", justifyContent: "space-between",
      border: "1px solid rgba(255,255,255,0.08)"
    }}>
      <div style={{ display: "flex", alignItems: "center", gap: 20 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <div style={{
            width: 40, height: 40, borderRadius: 10,
            background: device ? "linear-gradient(135deg, #a78bfa, #7c3aed)" : "rgba(255,255,255,0.05)",
            display: "flex", alignItems: "center", justifyContent: "center",
            fontSize: device ? 20 : 16,
            opacity: device ? 1 : 0.5
          }}>
            {device ? "📲" : "🔌"}
          </div>
          <div>
            <div style={{ fontSize: 14, fontWeight: 700, color: device ? "#e2e8f0" : "#94a3b8" }}>
              {device ? device.name : "No Device Connected"}
            </div>
            <div style={{ fontSize: 11, color: "#64748b" }}>
              {device ? `${device.os} · ${device.mode}` : isDetecting ? "Scanning USB ports..." : "Awaiting connection..."}
            </div>
          </div>
        </div>

        <div style={{ height: 24, width: 1, background: "rgba(255,255,255,0.1)" }} />

        <div style={{ display: "flex", gap: 15 }}>
          <Stat label="ECID" value={device ? device.ecid : "N/A"} active={!!device} />
          <Stat label="CHIP" value={device ? device.chip : "N/A"} active={!!device} />
          <Stat label="CPID" value={device ? device.cpid : "N/A"} active={!!device} />
        </div>
      </div>

      <div style={{ display: "flex", gap: 10 }}>
        <button 
          className="btn" 
          style={{ fontSize: 11, display: "flex", alignItems: "center", gap: 6 }} 
          onClick={handleDetect}
          disabled={isDetecting}
        >
          <span>{isDetecting ? "🔄" : "🔍"}</span> 
          <span>{isDetecting ? "Detecting..." : "Detect"}</span>
        </button>
        <button className="btn primary" style={{ fontSize: 11, display: "flex", alignItems: "center", gap: 6 }} disabled={!device}>
          <span>☁️</span> 
          <span>Sync Vault</span>
        </button>
      </div>
    </div>
  );
}

function Stat({ label, value, active }: { label: string; value: string; active?: boolean }) {
  return (
    <div>
      <div style={{ fontSize: 10, color: "#475569", fontWeight: 600 }}>{label}</div>
      <div style={{ fontSize: 12, color: active ? "#94a3b8" : "#475569", fontWeight: 700 }}>{value}</div>
    </div>
  );
}

