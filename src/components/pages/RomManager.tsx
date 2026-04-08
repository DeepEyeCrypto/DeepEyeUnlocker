import type { DragEvent } from "react";
import { useEffect, useMemo } from "react";
import type { UnlistenFn } from "@tauri-apps/api/event";
import { getCurrentWindow } from "@tauri-apps/api/window";
import {
  Archive,
  Cable,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  Cpu,
  FolderOpen,
  HardDriveDownload,
  Info,
  Layers3,
  Package,
  RefreshCw,
  ShieldAlert,
  ShieldCheck,
  Smartphone,
  Trash2,
  TriangleAlert,
  UploadCloud,
  XCircle,
} from "lucide-react";
import { useDevicePolling } from "../../hooks/useDevicePolling";
import { useRomManager } from "../../hooks/useRomManager";
import type {
  CompatibilityState,
  FlashEntry,
  QueueItem,
  QueueStatus,
  RiskLevel,
} from "../../lib/rom_manager";
import type { ConnectedDevice } from "../../lib/devices";
import { Card } from "../ui/Card";
import "../../styles/rom-manager.css";

type DragPathFile = File & {
  path?: string;
};

function formatBytes(bytes: number | null | undefined): string {
  if (!bytes || bytes <= 0) {
    return "—";
  }

  const units = ["B", "KB", "MB", "GB", "TB"];
  let value = bytes;
  let unitIndex = 0;

  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024;
    unitIndex += 1;
  }

  const precision = value >= 100 || unitIndex === 0 ? 0 : 1;
  return `${value.toFixed(precision)} ${units[unitIndex]}`;
}

function humanizeToken(value: string): string {
  return value
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .replace(/[_-]+/g, " ")
    .replace(/\s+/g, " ")
    .trim()
    .replace(/\b\w/g, (character) => character.toUpperCase());
}

function queueTone(status: QueueStatus): "ready" | "warn" | "error" | "idle" {
  switch (status) {
    case "ready":
    case "completed":
      return "ready";
    case "blocked":
    case "failed":
      return "error";
    case "validating":
    case "flashing":
      return "warn";
    default:
      return "idle";
  }
}

function compatibilityTone(
  state: CompatibilityState,
): "positive" | "warn" | "error" | "neutral" {
  switch (state) {
    case "compatible":
      return "positive";
    case "likelyCompatible":
      return "warn";
    case "incompatible":
      return "error";
    default:
      return "neutral";
  }
}

function riskTone(riskLevel: RiskLevel): "low" | "medium" | "high" | "critical" {
  return riskLevel;
}

function statusIcon(status: QueueStatus) {
  switch (status) {
    case "ready":
    case "completed":
      return <CheckCircle2 size={14} />;
    case "blocked":
    case "failed":
      return <XCircle size={14} />;
    case "validating":
    case "flashing":
      return <RefreshCw size={14} />;
    default:
      return <Info size={14} />;
  }
}

function isAndroidDevice(device: ConnectedDevice): boolean {
  return device.source !== "apple" && !device.os.toLowerCase().includes("ios");
}

function canToggleEntry(entry: FlashEntry, totalEntries: number): boolean {
  if (entry.actionType === "applyOta" || entry.actionType === "flashPackage") {
    return false;
  }

  return totalEntries > 1;
}

function firstArchiveRows(item: QueueItem | null): Array<{ path: string; size: string }> {
  if (!item) {
    return [];
  }

  return item.analysis.archiveEntries.slice(0, 16).map((entry) => ({
    path: entry.path,
    size: formatBytes(entry.uncompressedSize),
  }));
}

function selectedPartitionSet(item: QueueItem | null): Set<string> {
  return new Set((item?.selectedPartitions ?? []).map((partition) => partition.toLowerCase()));
}

export default function RomManager() {
  const {
    queue,
    selectedItem,
    selectedQueueId,
    isLoadingQueue,
    isImporting,
    busyQueueId,
    dragActive,
    statusMessage,
    errorMessage,
    setDragActive,
    selectQueueItem,
    refreshQueue,
    pickRom,
    importPaths,
    removeQueueItem,
    clearQueue,
    moveQueueItemByOffset,
    togglePartition,
    reanalyzeSelected,
  } = useRomManager();
  const { devices, state: devicePollingState } = useDevicePolling(2000);

  const androidDevices = useMemo(() => devices.filter(isAndroidDevice), [devices]);
  const autoAssociatedDevice = selectedItem?.analysis.compatibility.connectedDevice
    ?? (androidDevices.length === 1
      ? {
          id: androidDevices[0].id,
          model: androidDevices[0].model,
          serial: androidDevices[0].serial,
          mode: androidDevices[0].mode,
          source: androidDevices[0].source,
          bootloaderStatus: androidDevices[0].bootloaderStatus,
          carrier: androidDevices[0].carrier ?? null,
        }
      : null);
  const queueStatusSummary = useMemo(() => {
    return queue.reduce<Record<QueueStatus, number>>(
      (summary, item) => {
        summary[item.status] += 1;
        return summary;
      },
      {
        pending: 0,
        validating: 0,
        ready: 0,
        blocked: 0,
        flashing: 0,
        completed: 0,
        failed: 0,
      },
    );
  }, [queue]);
  const partitions = useMemo(() => selectedPartitionSet(selectedItem), [selectedItem]);
  const archiveRows = useMemo(() => firstArchiveRows(selectedItem), [selectedItem]);

  useEffect(() => {
    let unlisten: UnlistenFn | null = null;

    void getCurrentWindow()
      .onDragDropEvent((event) => {
        if (event.payload.type === "enter" || event.payload.type === "over") {
          setDragActive(true);
          return;
        }

        if (event.payload.type === "leave") {
          setDragActive(false);
          return;
        }

        setDragActive(false);
        void importPaths(event.payload.paths);
      })
      .then((cleanup) => {
        unlisten = cleanup;
      })
      .catch(() => undefined);

    return () => {
      if (unlisten) {
        unlisten();
      }
    };
  }, [importPaths, setDragActive]);

  const handleDropZoneDragEnter = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    setDragActive(true);
  };

  const handleDropZoneDragOver = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    setDragActive(true);
  };

  const handleDropZoneDragLeave = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    setDragActive(false);
  };

  const handleDropZoneDrop = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    setDragActive(false);

    const paths = Array.from(event.dataTransfer.files)
      .map((file) => (file as DragPathFile).path)
      .filter((path): path is string => Boolean(path));

    if (paths.length > 0) {
      void importPaths(paths);
    }
  };

  const bannerTone = errorMessage ? "error" : selectedItem?.status === "blocked" ? "warn" : "info";
  const selectedFlashEntries = selectedItem?.analysis.flashPlan.flashEntries ?? [];
  const totalWarnings = selectedItem?.warnings.length ?? 0;
  const totalBlockers = selectedItem?.blockers.length ?? 0;
  const matchedDevice = selectedItem?.analysis.compatibility.matchedDatabaseEntry ?? null;

  return (
    <div className={`rom-manager-page ${dragActive ? "rom-manager-page--drag-active" : ""}`}>
      <div className="rom-manager-hero">
        <div>
          <div className="rom-manager-kicker">
            <Archive size={16} />
            <span>DAY 16 · ZIP ANALYSIS + FLASH QUEUE</span>
          </div>
          <h1 className="rom-manager-title">ROM Manager</h1>
          <p className="rom-manager-subtitle">
            Import firmware ZIP packages, inspect real archive metadata, derive a validated flash
            plan, and manage a queue without fake device or package data.
          </p>
        </div>

        <div className="rom-manager-hero__actions">
          <button className="rom-manager-button rom-manager-button--secondary" type="button" onClick={() => void refreshQueue()}>
            <RefreshCw size={16} />
            Refresh Queue
          </button>
          <button className="rom-manager-button rom-manager-button--primary" type="button" onClick={() => void pickRom()}>
            <FolderOpen size={16} />
            Select ZIP
          </button>
        </div>
      </div>

      <div className={`rom-manager-banner rom-manager-banner--${bannerTone}`}>
        {errorMessage || statusMessage}
      </div>

      <div className="rom-manager-grid">
        <div className="rom-manager-main">
          <Card className="rom-manager-card rom-manager-drop-card">
            <div
              className={`rom-manager-dropzone ${dragActive ? "active" : ""}`}
              onDragEnter={handleDropZoneDragEnter}
              onDragOver={handleDropZoneDragOver}
              onDragLeave={handleDropZoneDragLeave}
              onDrop={handleDropZoneDrop}
            >
              <div className="rom-manager-dropzone__icon">
                <UploadCloud size={28} />
              </div>
              <div className="rom-manager-dropzone__content">
                <h2>Drop firmware ZIP packages anywhere in the window</h2>
                <p>
                  Supports fastboot ROMs, OTA packages, MediaTek scatter bundles, Qualcomm EDL
                  payloads, and Samsung Odin archives.
                </p>
              </div>
              <div className="rom-manager-dropzone__actions">
                <button className="rom-manager-button rom-manager-button--primary" type="button" onClick={() => void pickRom()}>
                  <FolderOpen size={16} />
                  Browse ZIPs
                </button>
                <span className="rom-manager-dropzone__hint">
                  {isImporting ? "Inspecting package contents..." : "Multi-file drop is supported"}
                </span>
              </div>
            </div>

            <div className="rom-manager-device-row">
              <div className="rom-manager-device-pill">
                <Smartphone size={14} />
                <span>Android Devices: {androidDevices.length}</span>
              </div>
              <div className="rom-manager-device-pill">
                <Cable size={14} />
                <span>Discovery: {humanizeToken(devicePollingState)}</span>
              </div>
              <div className="rom-manager-device-pill">
                <HardDriveDownload size={14} />
                <span>
                  Auto-Association: {autoAssociatedDevice ? `${autoAssociatedDevice.model} · ${autoAssociatedDevice.serial}` : "Pending"}
                </span>
              </div>
            </div>
          </Card>

          {selectedItem ? (
            <>
              <Card
                className="rom-manager-card"
                title="ROM Summary"
                action={
                  <button className="rom-manager-button rom-manager-button--ghost" type="button" onClick={() => void reanalyzeSelected()}>
                    <RefreshCw size={14} />
                    Re-analyze
                  </button>
                }
              >
                <div className="rom-manager-summary-head">
                  <div>
                    <h2 className="rom-manager-package-name">{selectedItem.fileName}</h2>
                    <p className="rom-manager-package-path">{selectedItem.filePath}</p>
                  </div>

                  <div className="rom-manager-chip-row">
                    <span className={`rom-manager-chip rom-manager-chip--${queueTone(selectedItem.status)}`}>
                      {statusIcon(selectedItem.status)}
                      {humanizeToken(selectedItem.status)}
                    </span>
                    <span className="rom-manager-chip">{humanizeToken(selectedItem.romType)}</span>
                    <span className="rom-manager-chip">{humanizeToken(selectedItem.flashMode)}</span>
                    <span className="rom-manager-chip">{humanizeToken(selectedItem.detectedPlatform)}</span>
                    {selectedItem.detectedBrand && (
                      <span className="rom-manager-chip">{selectedItem.detectedBrand}</span>
                    )}
                  </div>
                </div>

                <div className="rom-manager-metrics-grid">
                  <div className="rom-manager-metric">
                    <span>Archive Entries</span>
                    <strong>{selectedItem.analysis.summary.archiveEntryCount}</strong>
                  </div>
                  <div className="rom-manager-metric">
                    <span>Compressed Size</span>
                    <strong>{formatBytes(selectedItem.analysis.summary.totalCompressedSize)}</strong>
                  </div>
                  <div className="rom-manager-metric">
                    <span>Payload Files</span>
                    <strong>{selectedItem.analysis.payloadFiles.length}</strong>
                  </div>
                  <div className="rom-manager-metric">
                    <span>Compatibility Score</span>
                    <strong>{selectedItem.analysis.compatibility.score}%</strong>
                  </div>
                </div>

                <div className="rom-manager-details-grid">
                  <div className="rom-manager-detail-block">
                    <span className="rom-manager-detail-label">Required Device State</span>
                    <strong>{selectedItem.analysis.flashPlan.requiredDeviceState}</strong>
                  </div>
                  <div className="rom-manager-detail-block">
                    <span className="rom-manager-detail-label">Archive SHA256</span>
                    <strong className="rom-manager-mono">{selectedItem.analysis.archiveSha256}</strong>
                  </div>
                  <div className="rom-manager-detail-block">
                    <span className="rom-manager-detail-label">Matched Device DB Entry</span>
                    <strong>
                      {matchedDevice
                        ? `${matchedDevice.brand} ${matchedDevice.model} (${matchedDevice.codename})`
                        : "No exact Device DB match"}
                    </strong>
                  </div>
                  <div className="rom-manager-detail-block">
                    <span className="rom-manager-detail-label">Execution Path</span>
                    <strong>
                      {selectedItem.executionSupported
                        ? "Queue execution supported in desktop pipeline"
                        : "Analysis only — execution currently blocked"}
                    </strong>
                  </div>
                </div>
              </Card>

              <Card className="rom-manager-card" title="Flash Plan">
                <div className="rom-manager-plan-meta">
                  <div className={`rom-manager-compatibility rom-manager-compatibility--${compatibilityTone(selectedItem.analysis.compatibility.state)}`}>
                    {selectedItem.analysis.compatibility.state === "compatible" ? <ShieldCheck size={15} /> : <ShieldAlert size={15} />}
                    <span>{humanizeToken(selectedItem.analysis.compatibility.state)}</span>
                  </div>
                  <div className="rom-manager-chip-row">
                    {selectedItem.analysis.flashPlan.dataWipeImplied && (
                      <span className="rom-manager-chip rom-manager-chip--warn">Data wipe implied</span>
                    )}
                    {selectedItem.analysis.flashPlan.bootloaderUnlockRequired && (
                      <span className="rom-manager-chip rom-manager-chip--warn">Bootloader unlock required</span>
                    )}
                    {selectedItem.analysis.flashPlan.looksDangerousOrIncomplete && (
                      <span className="rom-manager-chip rom-manager-chip--error">High-risk package</span>
                    )}
                  </div>
                </div>

                <div className="rom-manager-table-wrap">
                  <table className="rom-manager-table">
                    <thead>
                      <tr>
                        <th>#</th>
                        <th>Enable</th>
                        <th>Target</th>
                        <th>Action</th>
                        <th>Source</th>
                        <th>Size</th>
                        <th>Protocol</th>
                        <th>Risk</th>
                      </tr>
                    </thead>
                    <tbody>
                      {selectedFlashEntries.map((entry) => {
                        const enabled = partitions.has(entry.partition.toLowerCase());
                        const toggleAllowed = canToggleEntry(entry, selectedFlashEntries.length);
                        const entryBusy = busyQueueId === selectedItem.id;

                        return (
                          <tr key={`${entry.partition}-${entry.order}`}>
                            <td>{entry.order}</td>
                            <td>
                              <input
                                className="rom-manager-checkbox"
                                type="checkbox"
                                checked={enabled}
                                disabled={!toggleAllowed || entryBusy}
                                onChange={(event) => {
                                  void togglePartition(selectedItem.id, entry.partition, event.target.checked);
                                }}
                              />
                            </td>
                            <td>
                              <div className="rom-manager-target-cell">
                                <strong>{entry.partition}</strong>
                                {entry.checksumAvailable && <span className="rom-manager-inline-note">checksum</span>}
                              </div>
                            </td>
                            <td>{humanizeToken(entry.actionType)}</td>
                            <td className="rom-manager-source-cell">
                              <span>{entry.sourceFile}</span>
                              {entry.notes.length > 0 && (
                                <small>{entry.notes[0]}</small>
                              )}
                            </td>
                            <td>{formatBytes(entry.estimatedSize)}</td>
                            <td>{humanizeToken(entry.requiredProtocol)}</td>
                            <td>
                              <span className={`rom-manager-risk rom-manager-risk--${riskTone(entry.riskLevel)}`}>
                                {humanizeToken(entry.riskLevel)}
                              </span>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </Card>

              <div className="rom-manager-detail-grid">
                <Card className="rom-manager-card" title="Compatibility, Warnings & Blockers">
                  <div className="rom-manager-section">
                    <div className="rom-manager-section__header">
                      <ShieldCheck size={16} />
                      <h3>Compatibility Report</h3>
                    </div>
                    <ul className="rom-manager-list">
                      {selectedItem.analysis.compatibility.reasons.map((reason) => (
                        <li key={reason}>{reason}</li>
                      ))}
                    </ul>
                  </div>

                  <div className="rom-manager-section">
                    <div className="rom-manager-section__header">
                      <TriangleAlert size={16} />
                      <h3>Warnings ({totalWarnings})</h3>
                    </div>
                    {totalWarnings > 0 ? (
                      <ul className="rom-manager-list rom-manager-list--warn">
                        {selectedItem.warnings.map((warning) => (
                          <li key={warning}>{warning}</li>
                        ))}
                      </ul>
                    ) : (
                      <p className="rom-manager-empty-text">No validation warnings for this package.</p>
                    )}
                  </div>

                  <div className="rom-manager-section">
                    <div className="rom-manager-section__header">
                      <XCircle size={16} />
                      <h3>Blockers ({totalBlockers})</h3>
                    </div>
                    {totalBlockers > 0 ? (
                      <ul className="rom-manager-list rom-manager-list--error">
                        {selectedItem.blockers.map((blocker) => (
                          <li key={blocker}>{blocker}</li>
                        ))}
                      </ul>
                    ) : (
                      <p className="rom-manager-empty-text">No hard blockers were detected.</p>
                    )}
                  </div>
                </Card>

                <Card className="rom-manager-card" title="Archive Analysis">
                  <div className="rom-manager-analysis-grid">
                    <div>
                      <div className="rom-manager-section__header">
                        <Package size={16} />
                        <h3>Top-Level Folders</h3>
                      </div>
                      <div className="rom-manager-chip-row">
                        {selectedItem.analysis.summary.topLevelFolders.length > 0 ? (
                          selectedItem.analysis.summary.topLevelFolders.map((folder) => (
                            <span key={folder} className="rom-manager-chip">
                              {folder}
                            </span>
                          ))
                        ) : (
                          <span className="rom-manager-chip">Archive root only</span>
                        )}
                      </div>
                    </div>

                    <div>
                      <div className="rom-manager-section__header">
                        <Layers3 size={16} />
                        <h3>Payload Files</h3>
                      </div>
                      <ul className="rom-manager-mini-list">
                        {selectedItem.analysis.payloadFiles.slice(0, 10).map((file) => (
                          <li key={file}>{file}</li>
                        ))}
                      </ul>
                    </div>

                    <div>
                      <div className="rom-manager-section__header">
                        <Cpu size={16} />
                        <h3>Manifest Files</h3>
                      </div>
                      <ul className="rom-manager-mini-list">
                        {selectedItem.analysis.manifestFiles.slice(0, 10).map((file) => (
                          <li key={file}>{file}</li>
                        ))}
                      </ul>
                    </div>

                    <div>
                      <div className="rom-manager-section__header">
                        <Archive size={16} />
                        <h3>Archive Preview</h3>
                      </div>
                      <div className="rom-manager-archive-preview">
                        {archiveRows.map((row) => (
                          <div key={row.path} className="rom-manager-archive-row">
                            <span>{row.path}</span>
                            <strong>{row.size}</strong>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                </Card>
              </div>
            </>
          ) : (
            <Card className="rom-manager-card">
              <div className="rom-manager-empty-state">
                <Archive size={30} />
                <h2>No ROM package selected</h2>
                <p>
                  Import a ZIP package to inspect archive contents, validate compatibility, and
                  populate the queue with a generated flash plan.
                </p>
              </div>
            </Card>
          )}
        </div>

        <div className="rom-manager-sidebar">
          <Card
            className="rom-manager-card"
            title="Flash Queue"
            action={
              <button
                className="rom-manager-button rom-manager-button--ghost"
                type="button"
                disabled={queue.length === 0 || busyQueueId === "__all__"}
                onClick={() => void clearQueue()}
              >
                <Trash2 size={14} />
                Clear Queue
              </button>
            }
          >
            <div className="rom-manager-queue-summary">
              <span>Ready: {queueStatusSummary.ready}</span>
              <span>Blocked: {queueStatusSummary.blocked}</span>
              <span>Total: {queue.length}</span>
            </div>

            <div className="rom-manager-queue-list">
              {isLoadingQueue ? (
                <div className="rom-manager-empty-text">Loading queue state...</div>
              ) : queue.length === 0 ? (
                <div className="rom-manager-empty-text">No firmware packages queued yet.</div>
              ) : (
                queue.map((item, index) => {
                  const isSelected = item.id === selectedQueueId;
                  const isBusy = busyQueueId === item.id;

                  return (
                    <div
                      key={item.id}
                      className={`rom-manager-queue-item ${isSelected ? "selected" : ""}`}
                    >
                      <button
                        className="rom-manager-queue-item__select"
                        type="button"
                        onClick={() => selectQueueItem(item.id)}
                      >
                        <div className="rom-manager-queue-item__head">
                          <strong>{item.fileName}</strong>
                          <span className={`rom-manager-chip rom-manager-chip--${queueTone(item.status)}`}>
                            {humanizeToken(item.status)}
                          </span>
                        </div>

                        <div className="rom-manager-queue-item__meta">
                          <span>{humanizeToken(item.romType)}</span>
                          <span>{humanizeToken(item.flashMode)}</span>
                          <span>{item.detectedBrand ?? humanizeToken(item.detectedPlatform)}</span>
                        </div>

                        <div className="rom-manager-queue-item__footer">
                          <span>{item.selectedPartitions.length} partitions enabled</span>
                          <span>{item.blockers.length} blockers</span>
                        </div>
                      </button>

                      <div className="rom-manager-queue-item__actions">
                        <button
                          className="rom-manager-icon-button"
                          type="button"
                          disabled={index === 0 || isBusy}
                          onClick={() => void moveQueueItemByOffset(item.id, -1)}
                        >
                          <ChevronUp size={14} />
                        </button>
                        <button
                          className="rom-manager-icon-button"
                          type="button"
                          disabled={index === queue.length - 1 || isBusy}
                          onClick={() => void moveQueueItemByOffset(item.id, 1)}
                        >
                          <ChevronDown size={14} />
                        </button>
                        <button
                          className="rom-manager-icon-button rom-manager-icon-button--danger"
                          type="button"
                          disabled={isBusy}
                          onClick={() => void removeQueueItem(item.id)}
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}
