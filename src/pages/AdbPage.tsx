import { useState } from "react";
import { open } from "@tauri-apps/plugin-dialog";
import { SpotlightFeatureCard } from "../components/ui/spotlight-feature-card";
import { DeviceInfoDashboard } from "../components/device/DeviceInfoDashboard";
import { Terminal, Smartphone, Download, Upload, RotateCcw, Trash } from "lucide-react";
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
  const [lastScanned, setLastScanned] = useState<Date | null>(null);

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

  const handleScanDevices = async () => {
    await scanDevices();
    setLastScanned(new Date());
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
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap', justifyContent: 'flex-end' }}>
          {lastScanned && (
            <span style={{ fontSize: '0.7rem', color: '#64748b' }}>
              Last scan: {lastScanned.toLocaleTimeString()}
            </span>
          )}
          <span className={`adb-status adb-status--${adbStatus}`}>
            {STATUS_LABEL[adbStatus] ?? adbStatus}
          </span>
        </div>
      </div>

      {/* ADB Actions - Spotlight Cards */}
      <div className="tools-grid" style={{ position: 'relative', zIndex: 10, marginBottom: '1.5rem' }}>
        <SpotlightFeatureCard
          icon={<Smartphone className="w-5 h-5 text-cyan-400" />}
          title="Scan Devices"
          description="Detect connected Android devices"
          glowColor="blue"
          onClick={handleScanDevices}
        />
        
        {selectedSerial && (
          <SpotlightFeatureCard
            icon={<Terminal className="w-5 h-5 text-purple-400" />}
            title="Get Full Info"
            description="Read complete device information"
            glowColor="purple"
            onClick={getDeviceInfo}
          />
        )}
        
        {selectedSerial && (
          <>
            <SpotlightFeatureCard
              icon={<RotateCcw className="w-5 h-5 text-green-400" />}
              title="Reboot"
              description="Reboot to system"
              glowColor="green"
              onClick={() => rebootDevice("system")}
            />
            <SpotlightFeatureCard
              icon={<Download className="w-5 h-5 text-orange-400" />}
              title="Install APK"
              description="Install APK file to device"
              glowColor="orange"
              onClick={handleInstall}
            />
            <SpotlightFeatureCard
              icon={<Upload className="w-5 h-5 text-red-400" />}
              title="Push File"
              description="Push file to device storage"
              glowColor="red"
              onClick={handlePush}
            />
            <SpotlightFeatureCard
              icon={<Trash className="w-5 h-5 text-red-400" />}
              title="Erase FRP"
              description="Remove FRP lock via ADB"
              glowColor="red"
              onClick={eraseFrp}
            />
          </>
        )}
      </div>

      {error && <div className="adb-error">⚠ {error}</div>}

      {/* Device List */}
      {devices.length > 0 && (
        <div className="adb-card" style={{ marginTop: '1rem' }}>
          <h2 style={{ fontSize: '0.9rem', color: '#60a5fa', marginBottom: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.06em' }}>Connected Devices</h2>
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

      {/* Device Info - Enhanced Dashboard */}
      {deviceInfo && (
        <div className="adb-card adb-device-info" style={{ marginTop: '1rem', padding: '0' }}>
          <DeviceInfoDashboard info={deviceInfo} />
        </div>
      )}

      {/* Operations */}
      {selectedSerial && (
        <div className="adb-card" style={{ marginTop: '1rem' }}>
          <h2>Quick Operations</h2>
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
        <div className="adb-card adb-shell-card" style={{ marginTop: '1rem' }}>
          <h2>ADB Shell Terminal</h2>
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
