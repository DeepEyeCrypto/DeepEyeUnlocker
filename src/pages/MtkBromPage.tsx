import { useCallback, useEffect, useRef, useState } from "react";
import { open } from "@tauri-apps/plugin-dialog";
import { useMtkBrom, type BromStatus } from "../hooks/useMtkBrom";
import { DeviceSelector } from "../components/DeviceSelector";
import type { DeviceEntry } from "../lib/devices";
import { Card } from "../components/ui/Card";
import "../styles/mtk-brom.css";

function formatSize(bytes: number): string {
  if (bytes >= 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB`;
  if (bytes >= 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  if (bytes >= 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${bytes} B`;
}

function partitionTypeName(t: number): { label: string; cls: string } {
  switch (t) {
    case 0x01: return { label: "boot", cls: "boot" };
    case 0x02: return { label: "system", cls: "system" };
    default:   return { label: "normal", cls: "normal" };
  }
}

type StatusCategory = "idle" | "working" | "connected" | "ready" | "error";

function statusCategory(s: BromStatus): StatusCategory {
  if (s === "idle") return "idle";
  if (s === "error") return "error";
  if (
    s === "da-ready" || s === "frp-erased" || s === "formatted" ||
    s === "imei-read" || s === "imei-written" || s === "rebooted" ||
    s === "partitions-listed" || s === "partition-read" ||
    s === "partition-written" || s === "preloader-dumped" ||
    s === "partition-erased"
  ) return "ready";
  if (s === "identified" || s === "auth-detected" || s === "sla-bypassed" ||
      s === "da-uploaded") return "connected";
  return "working";
}

function statusLabel(s: BromStatus): string {
  return s.replace(/-/g, " ").replace(/\b\w/g, (c) => c.toUpperCase());
}

function isWorking(s: BromStatus): boolean {
  return statusCategory(s) === "working";
}

function isDaReady(s: BromStatus): boolean {
  const cat = statusCategory(s);
  return cat === "ready" || s === "da-ready";
}

function isImeiValid(value: string): boolean {
  return value.length === 15 && /^\d{15}$/.test(value);
}

export default function MtkBromPage() {
  const {
    chipInfo, authType, daUploadResult, daJumpInfo, imeiInfo,
    partitions, bromStatus, error,
    detect, bypassSla, uploadDa, jumpToDa,
    eraseFrp, formatUserdata, readImei, writeImei,
    reboot, listPartitions, readPartition, writePartition,
    dumpPreloader, erasePartition,
  } = useMtkBrom();

  const [selectedDevice, setSelectedDevice] = useState<DeviceEntry | null>(null);
  const [autoRouted, setAutoRouted] = useState(false);
  const [log, setLog] = useState<string[]>(["[system] MTK BROM interface ready"]);
  const [imei1Input, setImei1Input] = useState("");
  const [imei2Input, setImei2Input] = useState("");
  const [selectedPartition, setSelectedPartition] = useState<string | null>(null);
  const logRef = useRef<HTMLDivElement>(null);

  const addLog = useCallback((msg: string) => {
    setLog((prev) => [...prev.slice(-49), msg]);
  }, []);

  // Auto-log on status change
  useEffect(() => {
    const ts = new Date().toLocaleTimeString();
    const isErr = bromStatus === "error";
    addLog(`[${ts}] ${bromStatus}${isErr && error ? ` — ${error}` : ""}`);
    logRef.current?.scrollTo(0, 99999);
  }, [bromStatus, error, addLog]);

  // Populate IMEI inputs when read
  useEffect(() => {
    if (imeiInfo) {
      setImei1Input(imeiInfo.imei1);
      setImei2Input(imeiInfo.imei2 ?? "");
    }
  }, [imeiInfo]);

  // Handle auto-routing data
  useEffect(() => {
    const state = window.history.state;
    if (state?.autoRouted && state?.device) {
      setAutoRouted(true);
      addLog(`[info] Auto-routed intelligence: ${state.device.brand} ${state.device.model}`);
      if (state.preFill?.da_path) {
        addLog(`[info] Suggested DA: ${state.preFill.da_path}`);
      }
      // Clear state flag to prevent re-toast
      window.history.replaceState({ ...state, autoRouted: false }, "");
    }
  }, [addLog]);

  const working = isWorking(bromStatus);
  const daReady = isDaReady(bromStatus);
  const cat = statusCategory(bromStatus);

  // ── Handlers ──────────────────────────────────────────────────

  const handleConnect = async () => {
    try {
      const result = await detect();
      if (selectedDevice) {
        addLog(`[success] Detected: ${selectedDevice.brand} ${selectedDevice.model} ✓`);
      }
      if (result.authType !== "None") {
        addLog(`[info] Auth required: ${result.authType}`);
      }
    } catch { /* error state handled by hook */ }
  };

  const handleBypassAndUpload = async () => {
    try {
      if (authType && authType !== "None") {
        addLog("[action] Bypassing SLA...");
        await bypassSla();
      }
      addLog("[action] Select DA binary file...");
      const path = await open({
        filters: [{ name: "DA Binary", extensions: ["bin"] }],
      });
      if (!path) { addLog("[cancelled] No file selected"); return; }
      addLog(`[action] Uploading DA: ${path}`);
      await uploadDa(path as string);
      addLog("[action] Jumping to DA...");
      await jumpToDa();
    } catch { /* hook handles error */ }
  };

  const handlePickAndUploadDa = async () => {
    try {
      const path = await open({
        filters: [{ name: "DA Binary", extensions: ["bin"] }],
      });
      if (!path) { addLog("[cancelled] No file selected"); return; }
      addLog(`[action] Uploading DA: ${path}`);
      await uploadDa(path as string);
    } catch { /* hook handles error */ }
  };

  const handleDumpPreloader = async () => {
    try {
      const path = await open({
        directory: false,
        filters: [{ name: "Preloader Dump", extensions: ["bin", "img"] }],
      });
      if (!path) return;
      const bytes = await dumpPreloader(path as string);
      addLog(`[success] Preloader dumped: ${formatSize(bytes ?? 0)}`);
    } catch { /* hook handles error */ }
  };

  const handleReadPartition = async () => {
    if (!selectedPartition) return;
    try {
      const path = await open({
        directory: false,
        filters: [{ name: "Partition Image", extensions: ["img", "bin"] }],
      });
      if (!path) return;
      const p = partitions.find((x) => x.name === selectedPartition);
      const bytes = await readPartition(
        selectedPartition, 0, p?.size ?? 0, path as string,
      );
      addLog(`[success] Read ${selectedPartition}: ${formatSize(bytes ?? 0)}`);
    } catch { /* hook handles error */ }
  };

  const handleWritePartition = async () => {
    if (!selectedPartition) return;
    try {
      const path = await open({
        filters: [{ name: "Partition Image", extensions: ["img", "bin"] }],
      });
      if (!path) return;
      addLog(`[action] Writing ${selectedPartition} from ${path}`);
      await writePartition(selectedPartition, 0, path as string);
      addLog(`[success] ${selectedPartition} written`);
    } catch { /* hook handles error */ }
  };

  const handleWriteImei = async () => {
    if (!isImeiValid(imei1Input)) return;
    try {
      await writeImei(imei1Input, imei2Input.length === 15 ? imei2Input : undefined);
      addLog("[success] IMEI written");
    } catch { /* hook handles error */ }
  };



  // ── Render ────────────────────────────────────────────────────

  return (
    <div className="page page-enter">
      <DeviceSelector 
        selectedDevice={selectedDevice} 
        onSelect={setSelectedDevice} 
      />

      {/* Header */}
      <div className="mtk-header">
        <div className="mtk-header-info">
          <h2 className="page-title">🔌 MTK BROM</h2>
          <p className="page-subtitle">
            MediaTek Boot ROM protocol — DA upload, partition access, FRP bypass
          </p>
          {autoRouted && (
            <div className="mtk-chip-label" style={{background: 'rgba(245, 158, 11, 0.2)', border: '1px solid #f59e0b', color: '#f59e0b'}}>
              ✨ <strong> INTELLIGENCE ACTIVE:</strong> Auto-Routing for Detected Model
            </div>
          )}
          {chipInfo && (
            <div className="mtk-chip-label">
              Chip: <strong>{chipInfo.chip_name}</strong>
              {authType && authType !== "None" && (
                <> · Auth: <strong>{authType}</strong></>
              )}
            </div>
          )}
        </div>
        <div className={`mtk-status-badge ${cat}`}>
          <span className="status-dot" />
          {statusLabel(bromStatus)}
        </div>
      </div>

      {/* Error banner */}
      {error && bromStatus === "error" && (
        <div className="mtk-error-banner">
          <span>⚠</span>
          <span>{error}</span>
        </div>
      )}

      {/* Connect button */}
      <Card title="Connection">
        <div className="action-row">
          <button
            className="btn btn-primary btn-sm"
            disabled={working || !selectedDevice || selectedDevice.brom_support === 'none'}
            onClick={handleConnect}
          >
            {working && bromStatus === "detecting" ? (
              <><span className="mtk-spinner" /> Scanning...</>
            ) : "Connect & Identify"}
          </button>

          {chipInfo && (
            <button
              className="btn btn-sm"
              disabled={working}
              onClick={handleBypassAndUpload}
            >
              {working ? <><span className="mtk-spinner" /> Working...</> : "SLA → Upload DA → Jump"}
            </button>
          )}

          {chipInfo && (
            <button
              className="btn btn-sm"
              disabled={working}
              onClick={handlePickAndUploadDa}
            >
              Upload DA
            </button>
          )}

          {daUploadResult && !daJumpInfo && (
            <button
              className="btn btn-sm success"
              disabled={working}
              onClick={() => jumpToDa().catch(() => {})}
            >
              Jump to DA
            </button>
          )}
        </div>
      </Card>

      {/* Two-column: Device Info + Operations */}
      <div className="mtk-columns">
        {/* Device info */}
        <Card title="Device Info">
          <div className="mtk-info-grid">
            <div className="mtk-info-item">
              <span className="mtk-info-label">HW Code</span>
              <span className="mtk-info-value accent">
                {chipInfo ? `0x${chipInfo.hw_code.toString(16).toUpperCase()}` : "—"}
              </span>
            </div>
            <div className="mtk-info-item">
              <span className="mtk-info-label">Chip</span>
              <span className="mtk-info-value">
                {chipInfo?.chip_name ?? "—"}
              </span>
            </div>
            <div className="mtk-info-item">
              <span className="mtk-info-label">Auth Type</span>
              <span className={`mtk-info-value ${authType === "None" ? "success" : "warn"}`}>
                {authType ?? "—"}
              </span>
            </div>
            <div className="mtk-info-item">
              <span className="mtk-info-label">HW Version</span>
              <span className="mtk-info-value">
                {chipInfo ? `0x${chipInfo.hw_ver.toString(16).toUpperCase()}` : "—"}
              </span>
            </div>
            <div className="mtk-info-item">
              <span className="mtk-info-label">SW Version</span>
              <span className="mtk-info-value">
                {chipInfo ? `0x${chipInfo.sw_ver.toString(16).toUpperCase()}` : "—"}
              </span>
            </div>
            <div className="mtk-info-item">
              <span className="mtk-info-label">DA Version</span>
              <span className="mtk-info-value accent">
                {daJumpInfo ? `v${daJumpInfo.version}` : "—"}
              </span>
            </div>
            {daUploadResult && (
              <>
                <div className="mtk-info-item">
                  <span className="mtk-info-label">DA Uploaded</span>
                  <span className="mtk-info-value success">
                    {formatSize(daUploadResult.bytes_uploaded)}
                  </span>
                </div>
                <div className="mtk-info-item">
                  <span className="mtk-info-label">Checksum</span>
                  <span className="mtk-info-value">
                    {`0x${daUploadResult.checksum.toString(16).toUpperCase()}`}
                  </span>
                </div>
              </>
            )}
          </div>
        </Card>

        {/* Operations */}
        <Card title="Operations">
          <div className="mtk-ops-grid">
            <button
              className="btn btn-sm danger"
              disabled={!daReady || working}
              onClick={() => eraseFrp().catch(() => {})}
            >
              <span className="btn-icon">🗑</span> Erase FRP
            </button>
            <button
              className="btn btn-sm warn"
              disabled={!daReady || working}
              onClick={() => formatUserdata().catch(() => {})}
            >
              <span className="btn-icon">💣</span> Format Userdata
            </button>
            <button
              className="btn btn-sm"
              disabled={!daReady || working}
              onClick={() => readImei().catch(() => {})}
            >
              <span className="btn-icon">📱</span> Read IMEI
            </button>
            <button
              className="btn btn-sm"
              disabled={!daReady || working}
              onClick={handleDumpPreloader}
            >
              <span className="btn-icon">💾</span> Dump Preloader
            </button>
            <button
              className="btn btn-sm"
              disabled={!daReady || working}
              onClick={() => listPartitions().catch(() => {})}
            >
              <span className="btn-icon">📋</span> List Partitions
            </button>
            <button
              className="btn btn-sm"
              disabled={!daReady || working}
              onClick={() => reboot("normal").catch(() => {})}
            >
              <span className="btn-icon">🔄</span> Reboot Normal
            </button>
            <button
              className="btn btn-sm"
              disabled={!daReady || working}
              onClick={() => reboot("recovery").catch(() => {})}
            >
              <span className="btn-icon">🛠</span> Reboot Recovery
            </button>
            <button
              className="btn btn-sm"
              disabled={!daReady || working}
              onClick={() => reboot("fastboot").catch(() => {})}
            >
              <span className="btn-icon">⚡</span> Reboot Fastboot
            </button>
          </div>
        </Card>
      </div>

      {/* Partitions */}
      {partitions.length > 0 && (
        <Card
          title={`Partitions (${partitions.length})`}
          action={
            <div className="action-row">
              <button
                className="btn btn-sm"
                disabled={!selectedPartition || working}
                onClick={handleReadPartition}
              >Read</button>
              <button
                className="btn btn-sm btn-primary"
                disabled={!selectedPartition || working}
                onClick={handleWritePartition}
              >Write</button>
              <button
                className="btn btn-sm danger"
                disabled={!selectedPartition || working}
                onClick={() => selectedPartition && erasePartition(selectedPartition).catch(() => {})}
              >Erase</button>
            </div>
          }
        >
          <div className="mtk-partition-scroll">
            <table className="mtk-partition-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Start</th>
                  <th>Size</th>
                  <th>Type</th>
                </tr>
              </thead>
              <tbody>
                {partitions.map((p) => {
                  const pt = partitionTypeName(p.partition_type);
                  const selected = selectedPartition === p.name;
                  return (
                    <tr
                      key={p.name}
                      className={selected ? "selected" : ""}
                      onClick={() => setSelectedPartition(selected ? null : p.name)}
                      style={{ cursor: "pointer" }}
                    >
                      <td className="pt-name">{p.name}</td>
                      <td>{`0x${p.start.toString(16).toUpperCase()}`}</td>
                      <td>{formatSize(p.size)}</td>
                      <td><span className={`pt-type ${pt.cls}`}>{pt.label}</span></td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* IMEI Repair */}
      <Card title="IMEI Repair">
        <div className="mtk-imei-row">
          <div className="mtk-imei-field">
            <label>IMEI 1</label>
            <input
              type="text"
              maxLength={15}
              placeholder="123456789012345"
              value={imei1Input}
              onChange={(e) => setImei1Input(e.target.value.replace(/\D/g, "").slice(0, 15))}
              className={
                imei1Input.length === 0
                  ? ""
                  : isImeiValid(imei1Input) ? "valid" : "invalid"
              }
            />
            {imei1Input.length > 0 && !isImeiValid(imei1Input) && (
              <div className="mtk-imei-hint">{15 - imei1Input.length} digits remaining</div>
            )}
          </div>
          <div className="mtk-imei-field">
            <label>IMEI 2 (optional)</label>
            <input
              type="text"
              maxLength={15}
              placeholder="123456789012345"
              value={imei2Input}
              onChange={(e) => setImei2Input(e.target.value.replace(/\D/g, "").slice(0, 15))}
              className={
                imei2Input.length === 0
                  ? ""
                  : isImeiValid(imei2Input) ? "valid" : "invalid"
              }
            />
          </div>
          <button
            className="btn btn-primary btn-sm"
            disabled={!daReady || working || !isImeiValid(imei1Input)}
            onClick={handleWriteImei}
          >
            {working && bromStatus === "writing-imei"
              ? <><span className="mtk-spinner" /> Writing...</>
              : "Write IMEI"}
          </button>
        </div>
        {imeiInfo && (
          <div className="mtk-info-grid" style={{ marginTop: 12 }}>
            <div className="mtk-info-item">
              <span className="mtk-info-label">Current IMEI 1</span>
              <span className="mtk-info-value accent">{imeiInfo.imei1}</span>
            </div>
            <div className="mtk-info-item">
              <span className="mtk-info-label">Current IMEI 2</span>
              <span className="mtk-info-value">{imeiInfo.imei2 ?? "N/A"}</span>
            </div>
          </div>
        )}
      </Card>

      {/* Activity Log */}
      <div className="panel">
        <div className="row-between" style={{ marginBottom: 8 }}>
          <div className="action-title">Activity Log</div>
          <button className="btn btn-ghost btn-sm" onClick={() => setLog([])}>Clear</button>
        </div>
        <div className="mtk-activity-log" ref={logRef}>
          {log.map((line, i) => (
            <div
              key={i}
              className={`log-line${line.includes("error") || line.includes("⚠") ? " error" : ""}`}
            >
              {line}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
