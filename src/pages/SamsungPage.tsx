import { useState } from "react";
import { open } from "@tauri-apps/plugin-dialog";
import { useSamsung } from "../hooks/useSamsung";
import "../styles/samsung.css";

const fmt = (bytes: number) => {
  if (bytes >= 1073741824) return `${(bytes / 1073741824).toFixed(1)} GB`;
  if (bytes >= 1048576) return `${(bytes / 1048576).toFixed(1)} MB`;
  if (bytes >= 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${bytes} B`;
};

export default function SamsungPage() {
  const {
    device,
    odinInfo,
    pitEntries,
    samsungStatus,
    flashProgress,
    error,
    log,
    detectDevice,
    doHandshake,
    readPit,
    flashPartition,
    eraseFrp,
    reboot,
  } = useSamsung();

  const [flashPart, setFlashPart] = useState("");
  const [flashFile, setFlashFile] = useState("");

  const handleFlashBrowse = async () => {
    const file = await open();
    if (file) setFlashFile(file as string);
  };

  const handleFlash = async () => {
    if (!flashPart || !flashFile) return;
    await flashPartition(flashPart, flashFile);
  };

  const isActive = samsungStatus !== "idle" && samsungStatus !== "error" && samsungStatus !== "detected";

  return (
    <div className="samsung-page">
      <div className="samsung-header">
        <div className="samsung-title">
          <span className="samsung-icon">🔵</span>
          <h1>SAMSUNG ODIN</h1>
          <span className="samsung-subtitle">Download Mode — VID 0x04e8</span>
        </div>
        <span className={`samsung-status samsung-status--${samsungStatus}`}>
          {samsungStatus.replace("_", " ").toUpperCase()}
        </span>
      </div>

      {/* Workflow Steps */}
      <div className="samsung-steps">
        <button
          className={`samsung-step-btn ${device ? "done" : ""}`}
          onClick={detectDevice}
          disabled={isActive}
        >
          1. Detect Device
        </button>
        <span className="samsung-step-arrow">→</span>
        <button
          className={`samsung-step-btn ${odinInfo ? "done" : ""}`}
          onClick={doHandshake}
          disabled={!device || isActive}
        >
          2. Handshake
        </button>
        <span className="samsung-step-arrow">→</span>
        <button
          className={`samsung-step-btn ${pitEntries.length > 0 ? "done" : ""}`}
          onClick={readPit}
          disabled={!odinInfo || isActive}
        >
          3. Read PIT
        </button>
      </div>

      {error && <div className="samsung-error">⚠ {error}</div>}

      <div className="samsung-two-col">
        {/* Device Info */}
        <div className="samsung-card">
          <h2>Device Info</h2>
          {device ? (
            <div className="samsung-info-list">
              <div><span>VID</span><strong>0x{device.vid.toString(16)}</strong></div>
              <div><span>PID</span><strong>0x{device.pid.toString(16)}</strong></div>
              <div><span>Mode</span><strong>{device.mode}</strong></div>
              {odinInfo && (
                <>
                  <div><span>Protocol</span><strong>{odinInfo.protocol_version}</strong></div>
                  <div><span>PIT Size</span><strong>{fmt(odinInfo.pit_size)}</strong></div>
                </>
              )}
            </div>
          ) : (
            <p className="samsung-placeholder">No device detected</p>
          )}
        </div>

        {/* Operations */}
        <div className="samsung-card">
          <h2>Operations</h2>
          <div className="samsung-ops">
            <button className="samsung-btn" onClick={readPit} disabled={!odinInfo || isActive}>
              Read PIT
            </button>
            <button
              className="samsung-btn samsung-btn--danger"
              onClick={eraseFrp}
              disabled={!odinInfo || isActive}
            >
              Erase FRP ⚠
            </button>
            <button className="samsung-btn" onClick={() => reboot(0)} disabled={!device || isActive}>
              Reboot Normal
            </button>
            <button className="samsung-btn" onClick={() => reboot(1)} disabled={!device || isActive}>
              Reboot Download
            </button>
            <button className="samsung-btn" onClick={() => reboot(3)} disabled={!device || isActive}>
              Reboot Recovery
            </button>
          </div>
        </div>
      </div>

      {/* PIT Table */}
      {pitEntries.length > 0 && (
        <div className="samsung-card">
          <h2>PIT Table ({pitEntries.length} partitions)</h2>
          <div className="samsung-table-wrap">
            <table className="samsung-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>ID</th>
                  <th>Type</th>
                  <th>Device</th>
                  <th>Offset</th>
                  <th>Size</th>
                  <th>File</th>
                </tr>
              </thead>
              <tbody>
                {pitEntries.map((p, i) => (
                  <tr
                    key={i}
                    className={
                      p.partition_name.toLowerCase() === "frp"
                        ? "samsung-row--frp"
                        : ""
                    }
                    onClick={() => setFlashPart(p.partition_name)}
                    style={{ cursor: "pointer" }}
                    title="Click to select for flashing"
                  >
                    <td className="samsung-part-name">{p.partition_name}</td>
                    <td>{p.partition_id}</td>
                    <td>{p.partition_type}</td>
                    <td>{p.device_type}</td>
                    <td className="samsung-mono">0x{p.offset.toString(16)}</td>
                    <td>{fmt(p.size)}</td>
                    <td className="samsung-filename">{p.flash_filename || "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Flash Partition */}
      {odinInfo && (
        <div className="samsung-card">
          <h2>Flash Partition</h2>
          <div className="samsung-flash-row">
            <input
              className="samsung-input"
              placeholder="Partition name (e.g. BOOT)"
              value={flashPart}
              onChange={(e) => setFlashPart(e.target.value)}
            />
            <input
              className="samsung-input samsung-input--file"
              placeholder="File path..."
              value={flashFile}
              readOnly
            />
            <button className="samsung-btn" onClick={handleFlashBrowse}>
              Browse
            </button>
            <button
              className="samsung-btn samsung-btn--primary"
              onClick={handleFlash}
              disabled={!flashPart || !flashFile || isActive}
            >
              Flash
            </button>
          </div>
          {samsungStatus === "flashing" && (
            <div className="samsung-progress-bar">
              <div
                className="samsung-progress-fill"
                style={{ width: `${flashProgress}%` }}
              />
              <span className="samsung-progress-label">{flashProgress}%</span>
            </div>
          )}
        </div>
      )}

      {/* Activity Log */}
      <div className="samsung-card samsung-log-card">
        <h2>Activity Log</h2>
        <div className="samsung-log">
          {log.length === 0 ? (
            <div className="samsung-log-empty">No activity yet...</div>
          ) : (
            log.map((line, i) => (
              <div
                key={i}
                className={`samsung-log-line ${line.includes("ERROR") ? "samsung-log--error" : ""} ${line.includes("✅") ? "samsung-log--success" : ""}`}
              >
                {line}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
