import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";

type ConnectedDevice = {
  id: string;
  model: string;
  serial: string;
  os: string;
  mode: string;
  source: string;
};

type DeviceBarState = {
  name: string;
  os: string;
  mode: string;
  ecid: string;
  chip: string;
  cpid: string;
  udid?: string;
};

export default function DeviceBar() {
  const [device, setDevice] = useState<DeviceBarState | null>(null);
  const [isDetecting, setIsDetecting] = useState(false);

  const handleDetect = async () => {
    setIsDetecting(true);
    try {
      const devices = await invoke<ConnectedDevice[]>("get_connected_devices");
      const appleDevice = devices.find((entry) => entry.source === "apple") ?? null;

      if (!appleDevice) {
        setDevice(null);
        return;
      }

      setDevice({
        name: appleDevice.model,
        os: appleDevice.os,
        mode: appleDevice.mode,
        ecid: appleDevice.serial || "N/A",
        chip: appleDevice.source.toUpperCase(),
        cpid: appleDevice.id || "N/A",
        udid: appleDevice.id,
      });
    } catch (error) {
      console.error("[DeviceBar] Detection failed:", error);
    } finally {
      setIsDetecting(false);
    }
  };

  const handleSyncVault = async () => {
    if (!device) return;
    try {
      await invoke("push_to_cloud_vault", { udid: device.udid || "" });
      alert("Vault synchronized successfully.");
    } catch (error) {
      console.error("[DeviceBar] Sync failed:", error);
      alert("Failed to sync vault.");
    }
  };

  return (
    <div className="devicebar card">
      <div className="devicebar-main">
        <div className="devicebar-ident">
          <div className={`devicebar-icon ${device ? "active" : ""}`}>
            <span className="devicebar-icon-glyph">{device ? "D" : "U"}</span>
          </div>
          <div className="devicebar-ident-text">
            <div className={`devicebar-title ${device ? "active" : ""}`}>
              {device ? device.name : "No device connected"}
            </div>
            <div className="devicebar-subtitle">
              {device ? `${device.os} · ${device.mode}` : isDetecting ? "Scanning USB ports..." : "Waiting for device..."}
            </div>
          </div>
        </div>

        <div className="devicebar-divider" />

        <div className="devicebar-stats">
          <Stat label="ECID" value={device ? device.ecid : "N/A"} active={!!device} />
          <Stat label="CHIP" value={device ? device.chip : "N/A"} active={!!device} />
          <Stat label="CPID" value={device ? device.cpid : "N/A"} active={!!device} />
        </div>
      </div>

      <div className="devicebar-actions">
        <button className="btn btn-secondary btn-sm" onClick={handleDetect} disabled={isDetecting}>
          <span>{isDetecting ? "R" : "S"}</span>
          <span>{isDetecting ? "Detecting..." : "Detect"}</span>
        </button>
        <button className="btn btn-primary btn-sm" disabled={!device} onClick={handleSyncVault}>
          <span>V</span>
          <span>Sync Vault</span>
        </button>
      </div>
    </div>
  );
}

function Stat({ label, value, active }: { label: string; value: string; active?: boolean }) {
  return (
    <div className="devicebar-stat">
      <div className="devicebar-stat-label">{label}</div>
      <div className={`devicebar-stat-value ${active ? "active" : ""}`}>{value}</div>
    </div>
  );
}
