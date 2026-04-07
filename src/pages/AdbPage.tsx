import { useState } from "react";
import { open } from "@tauri-apps/plugin-dialog";
import { LiquidMetalButton } from "../components/ui/liquid-metal-button";
import { useAdb } from "../hooks/useAdb";
import "../styles/adb.css";

const STATUS_LABEL: Record<string, string> = {
  idle: "Idle",
  scanning: "Scanning...",
  connected: "Connected",
  executing: "Executing...",
  installing: "Installing...",
  pushing: "Pushing...",
  pulling: "Pulling...",
  rebooting: "Rebooting...",
  sideloading: "Sideloading...",
  erasing_frp: "Erasing FRP...",
  error: "Error",
};

export default function AdbPage() {
  const {
    devices,
    selectedSerial,
    deviceInfo,
    adbStatus,
    error,
    scanDevices,
    selectDevice,
    getDeviceInfo,
    shellCommand,
    rebootDevice,
    installApk,
    pushFile,
    sideloadZip,
    eraseFrp,
  } = useAdb();

  const [shellInput, setShellInput] = useState("");
  const [shellOutput, setShellOutput] = useState<string[]>([]);

  const handleShell = async () => {
    if (!shellInput.trim()) return;
    try {
      const out = await shellCommand(shellInput);
      setShellOutput((prev) => [
        ...prev,
        `$ ${shellInput}`,
        out || "(no output)",
      ]);
    } catch (e: any) {
      setShellOutput((prev) => [...prev, `$ ${shellInput}`, `ERROR: ${e}`]);
    }
    setShellInput("");
  };

  const handleInstall = async () => {
    const file = await open({ filters: [{ name: "APK", extensions: ["apk"] }] });
    if (file) await installApk(file as string);
  };

  const handlePush = async () => {
    const file = await open();
    if (file) await pushFile(file as string, `/sdcard/${(file as string).split("/").pop()}`);
  };

  const handleSideload = async () => {
    const file = await open({ filters: [{ name: "ZIP", extensions: ["zip"] }] });
    if (file) await sideloadZip(file as string);
  };

  return (
    <div className="adb-page">
      <div className="adb-header">
        <div className="adb-title">
          <span className="adb-icon">📱</span>
          <h1>ADB DEVICES</h1>
        </div>
        <span className={`adb-status adb-status--${adbStatus}`}>
          {STATUS_LABEL[adbStatus] ?? adbStatus}
        </span>
      </div>

      <div className="adb-scan-row">
        <LiquidMetalButton label="Scan Devices" onClick={scanDevices} />
        {selectedSerial && (
          <button className="adb-btn adb-btn--secondary" onClick={getDeviceInfo}>
            Get Full Info
          </button>
        )}
      </div>

      {error && <div className="adb-error">⚠ {error}</div>}

      {/* Device List */}
      {devices.length > 0 && (
        <div className="adb-card">
          <table className="adb-table">
            <thead>
              <tr>
                <th>Serial</th>
                <th>Model</th>
                <th>State</th>
                <th>Android</th>
              </tr>
            </thead>
            <tbody>
              {devices.map((d) => (
                <tr
                  key={d.serial}
                  className={
                    selectedSerial === d.serial ? "adb-row--selected" : ""
                  }
                  onClick={() => selectDevice(d.serial)}
                >
                  <td className="adb-serial">{d.serial}</td>
                  <td>{d.model || "—"}</td>
                  <td>
                    <span className={`adb-state adb-state--${d.state}`}>
                      {d.state}
                    </span>
                  </td>
                  <td>{d.android_version || "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Device Info */}
      {deviceInfo && (
        <div className="adb-card adb-device-info">
          <h2>Device Info</h2>
          <div className="adb-info-grid">
            <div><span>Brand</span><strong>{deviceInfo.brand}</strong></div>
            <div><span>Model</span><strong>{deviceInfo.model}</strong></div>
            <div><span>Android</span><strong>{deviceInfo.android_version}</strong></div>
            <div><span>SDK</span><strong>{deviceInfo.sdk_int}</strong></div>
            <div><span>Build</span><strong>{deviceInfo.build_id}</strong></div>
            <div><span>Security Patch</span><strong>{deviceInfo.security_patch}</strong></div>
            <div>
              <span>Root</span>
              <strong>{deviceInfo.root_status ? "✅ Rooted" : "❌ No Root"}</strong>
            </div>
            <div><span>Bootloader</span><strong>{deviceInfo.bootloader_status}</strong></div>
            <div><span>FRP</span><strong>{deviceInfo.frp_status}</strong></div>
            <div><span>Battery</span><strong>{deviceInfo.battery_level}</strong></div>
            <div><span>IMEI</span><strong>{deviceInfo.imei ? "***" + deviceInfo.imei.slice(-4) : "N/A"}</strong></div>
          </div>
        </div>
      )}

      {/* Operations */}
      {selectedSerial && (
        <div className="adb-card">
          <h2>Operations</h2>
          <div className="adb-ops-grid">
            <button className="adb-btn" onClick={() => rebootDevice("system")}>
              Reboot System
            </button>
            <button className="adb-btn" onClick={() => rebootDevice("recovery")}>
              Recovery
            </button>
            <button className="adb-btn" onClick={() => rebootDevice("bootloader")}>
              Bootloader
            </button>
            <button className="adb-btn" onClick={() => rebootDevice("edl")}>
              EDL Mode
            </button>
            <button className="adb-btn" onClick={handleInstall}>
              Install APK
            </button>
            <button className="adb-btn" onClick={handlePush}>
              Push File
            </button>
            <button className="adb-btn" onClick={handleSideload}>
              Sideload ZIP
            </button>
            <button
              className="adb-btn adb-btn--danger"
              onClick={eraseFrp}
            >
              Erase FRP ⚠
            </button>
          </div>
        </div>
      )}

      {/* Shell */}
      {selectedSerial && (
        <div className="adb-card adb-shell-card">
          <h2>ADB Shell</h2>
          <div className="adb-shell-input-row">
            <span className="adb-prompt">$</span>
            <input
              className="adb-shell-input"
              value={shellInput}
              onChange={(e) => setShellInput(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && handleShell()}
              placeholder="shell command..."
            />
            <button className="adb-btn adb-btn--primary" onClick={handleShell}>
              Run
            </button>
          </div>
          <div className="adb-shell-output">
            {shellOutput.map((line, i) => (
              <div
                key={i}
                className={line.startsWith("$") ? "adb-cmd-line" : "adb-out-line"}
              >
                {line}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
