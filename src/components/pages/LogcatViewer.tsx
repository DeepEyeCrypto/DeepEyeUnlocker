import { useEffect, useMemo, useRef, useState } from "react";
import {
  ArrowDown,
  Copy,
  Download,
  Play,
  RefreshCw,
  Square,
  Terminal,
  Trash2,
} from "lucide-react";
import { useDevicePolling } from "../../hooks/useDevicePolling";
import { useLogcat, type UseLogcatReturn } from "../../hooks/useLogcat";
import type { LogcatEntry } from "../../lib/logcat";
import { Card } from "../ui/Card";
import "../../styles/logcat.css";

const LOG_LEVEL_OPTIONS = ["ALL", "V", "D", "I", "W", "E", "F"];

function buildEntryKey(entry: LogcatEntry, index: number): string {
  return `${entry.timestamp}-${entry.pid ?? "na"}-${entry.tid ?? "na"}-${entry.tag}-${index}`;
}

function formatEntryLine(entry: LogcatEntry): string {
  return entry.raw.trim() || `${entry.timestamp} ${entry.level} ${entry.tag}: ${entry.message}`.trim();
}

function levelClass(level: string): string {
  const normalized = level.trim().toUpperCase();
  switch (normalized) {
    case "V":
      return "verbose";
    case "D":
      return "debug";
    case "I":
      return "info";
    case "W":
      return "warn";
    case "E":
      return "error";
    case "F":
      return "fatal";
    default:
      return "default";
  }
}

async function copyText(value: string): Promise<void> {
  await navigator.clipboard.writeText(value);
}

export default function LogcatViewer() {
  const {
    devices,
    state: devicePollingState,
    refresh,
  } = useDevicePolling(2000);
  const {
    entries,
    filteredEntries,
    isRunning,
    selectedSerial,
    levelFilter,
    tagFilter,
    textFilter,
    pidFilter,
    autoScroll,
    paused,
    statusMessage,
    errorMessage,
    setSelectedSerial,
    setLevelFilter,
    setTagFilter,
    setTextFilter,
    setPidFilter,
    setAutoScroll,
    setPaused,
    start,
    stop,
    clear,
    exportToFile,
    clearEntries,
  }: UseLogcatReturn = useLogcat();
  const terminalRef = useRef<HTMLDivElement>(null);
  const [selectedEntryKey, setSelectedEntryKey] = useState<string | null>(null);
  const [uiFeedback, setUiFeedback] = useState("");

  const adbDevices = useMemo(
    () => devices.filter((device) => device.source === "adb" || device.mode.toUpperCase().startsWith("ADB")),
    [devices],
  );

  useEffect(() => {
    const serialExists = selectedSerial !== null && adbDevices.some((device) => device.serial === selectedSerial);

    if (serialExists) {
      return;
    }

    if (adbDevices.length === 1) {
      setSelectedSerial(adbDevices[0].serial);
      return;
    }

    if (adbDevices.length === 0 || selectedSerial !== null) {
      setSelectedSerial(null);
    }
  }, [adbDevices, selectedSerial, setSelectedSerial]);

  useEffect(() => {
    if (!selectedSerial || !isRunning) {
      return;
    }

    const stillConnected = adbDevices.some((device) => device.serial === selectedSerial);
    if (!stillConnected) {
      void stop();
    }
  }, [adbDevices, isRunning, selectedSerial, stop]);

  useEffect(() => {
    if (!selectedEntryKey) {
      return;
    }

    const stillVisible = filteredEntries.some((entry, index) => buildEntryKey(entry, index) === selectedEntryKey);
    if (!stillVisible) {
      setSelectedEntryKey(null);
    }
  }, [filteredEntries, selectedEntryKey]);

  useEffect(() => {
    if (!terminalRef.current || !autoScroll || paused) {
      return;
    }

    terminalRef.current.scrollTo({
      top: terminalRef.current.scrollHeight,
      behavior: "smooth",
    });
  }, [autoScroll, filteredEntries, paused]);

  const selectedEntry = useMemo(
    () =>
      filteredEntries.find((entry, index) => buildEntryKey(entry, index) === selectedEntryKey) ?? null,
    [filteredEntries, selectedEntryKey],
  );

  const statusTone = errorMessage
    ? "error"
    : adbDevices.length === 0
      ? "warn"
      : uiFeedback
        ? "success"
        : "info";
  const bannerText = errorMessage
    || uiFeedback
    || (adbDevices.length === 0
      ? "No ADB devices detected. Connect a device with USB debugging enabled to start logcat."
      : adbDevices.length > 1 && !selectedSerial
        ? "Multiple ADB devices detected. Select a serial before starting the log stream."
        : statusMessage);

  const canStart = adbDevices.length > 0 && (adbDevices.length === 1 || selectedSerial !== null) && !isRunning;

  const handleJumpBottom = () => {
    if (!terminalRef.current) {
      return;
    }

    terminalRef.current.scrollTo({
      top: terminalRef.current.scrollHeight,
      behavior: "smooth",
    });
  };

  const handleCopyVisible = async () => {
    if (filteredEntries.length === 0) {
      setUiFeedback("No visible log entries available to copy");
      return;
    }

    await copyText(filteredEntries.map(formatEntryLine).join("\n"));
    setUiFeedback(`Copied ${filteredEntries.length} visible log lines`);
  };

  const handleCopyAll = async () => {
    if (entries.length === 0) {
      setUiFeedback("No log entries available to copy");
      return;
    }

    await copyText(entries.map(formatEntryLine).join("\n"));
    setUiFeedback(`Copied ${entries.length} total log lines`);
  };

  const handleCopySelected = async () => {
    if (!selectedEntry) {
      setUiFeedback("Select a log row to copy a single entry");
      return;
    }

    await copyText(formatEntryLine(selectedEntry));
    setUiFeedback("Copied selected log entry");
  };

  return (
    <div className="logcat-page">
      <div className="logcat-hero">
        <div>
          <div className="logcat-kicker">
            <Terminal size={16} />
            <span>DAY 15 · REAL-TIME ADB LOGCAT VIEWER</span>
          </div>
          <h1 className="logcat-title">Logcat</h1>
          <p className="logcat-subtitle">
            Structured threadtime streaming with device routing, pause control, live filters,
            export, and disconnect-safe session handling.
          </p>
        </div>

        <div className={`logcat-status-pill ${isRunning ? "running" : "idle"}`}>
          <span className="logcat-status-pill__dot" />
          {isRunning ? "ADB ACTIVE" : "IDLE"}
        </div>
      </div>

      <Card className="logcat-card">
        <div className="logcat-controls">
          <div className="logcat-controls__grid">
            <label className="logcat-field">
              <span className="logcat-field__label">Device</span>
              <select
                className="logcat-select"
                value={selectedSerial ?? ""}
                onChange={(event) => setSelectedSerial(event.target.value || null)}
              >
                <option value="">Select ADB serial</option>
                {adbDevices.map((device) => (
                  <option key={device.id} value={device.serial}>
                    {device.model} · {device.serial}
                  </option>
                ))}
              </select>
            </label>

            <label className="logcat-field">
              <span className="logcat-field__label">Level</span>
              <select
                className="logcat-select"
                value={levelFilter}
                onChange={(event) => setLevelFilter(event.target.value)}
              >
                {LOG_LEVEL_OPTIONS.map((level) => (
                  <option key={level} value={level}>
                    {level}
                  </option>
                ))}
              </select>
            </label>

            <label className="logcat-field">
              <span className="logcat-field__label">Tag Filter</span>
              <input
                className="logcat-input"
                value={tagFilter}
                onChange={(event) => setTagFilter(event.target.value)}
                placeholder="ActivityManager"
              />
            </label>

            <label className="logcat-field">
              <span className="logcat-field__label">Keyword</span>
              <input
                className="logcat-input"
                value={textFilter}
                onChange={(event) => setTextFilter(event.target.value)}
                placeholder="Search log text"
              />
            </label>

            <label className="logcat-field">
              <span className="logcat-field__label">PID</span>
              <input
                className="logcat-input"
                inputMode="numeric"
                value={pidFilter}
                onChange={(event) => setPidFilter(event.target.value)}
                placeholder="Optional PID"
              />
            </label>
          </div>

          <div className="logcat-toolbar">
            <button className="logcat-button logcat-button--secondary" type="button" onClick={() => void refresh()}>
              <RefreshCw size={16} />
              Refresh Devices
            </button>
            <button
              className="logcat-button logcat-button--primary"
              type="button"
              disabled={!canStart}
              onClick={() => void start()}
            >
              <Play size={16} />
              Start
            </button>
            <button
              className="logcat-button logcat-button--secondary"
              type="button"
              disabled={!isRunning}
              onClick={() => void stop()}
            >
              <Square size={16} />
              Stop
            </button>
            <button
              className="logcat-button logcat-button--danger"
              type="button"
              disabled={!selectedSerial}
              onClick={() => void clear()}
            >
              <Trash2 size={16} />
              Clear Device
            </button>
            <button
              className="logcat-button logcat-button--secondary"
              type="button"
              disabled={filteredEntries.length === 0}
              onClick={() => void exportToFile()}
            >
              <Download size={16} />
              Export
            </button>
          </div>

          <div className="logcat-toggle-row">
            <label className="logcat-toggle">
              <input
                type="checkbox"
                checked={autoScroll}
                onChange={(event) => setAutoScroll(event.target.checked)}
              />
              <span>Auto-scroll</span>
            </label>

            <label className="logcat-toggle">
              <input
                type="checkbox"
                checked={paused}
                onChange={(event) => setPaused(event.target.checked)}
              />
              <span>{paused ? "Resume stream buffering" : "Pause stream view"}</span>
            </label>

            <div className="logcat-toggle-row__meta">
              Transport: {selectedSerial ?? "default transport"} · Discovery: {devicePollingState}
            </div>
          </div>
        </div>
      </Card>

      <div className={`logcat-banner logcat-banner--${statusTone}`}>{bannerText}</div>

      <Card className="logcat-card">
        <div className="logcat-summary">
          <div className="logcat-summary__item">
            <span className="logcat-summary__label">Lines</span>
            <strong>{entries.length}</strong>
          </div>
          <div className="logcat-summary__item">
            <span className="logcat-summary__label">Visible</span>
            <strong>{filteredEntries.length}</strong>
          </div>
          <div className="logcat-summary__item">
            <span className="logcat-summary__label">Selected Serial</span>
            <strong>{selectedSerial ?? "Unassigned"}</strong>
          </div>
          <div className="logcat-summary__item">
            <span className="logcat-summary__label">Pause State</span>
            <strong>{paused ? "Paused" : "Live"}</strong>
          </div>
        </div>

        <div ref={terminalRef} className="logcat-terminal">
          {filteredEntries.length === 0 ? (
            <div className="logcat-empty-state">
              {entries.length === 0
                ? "Waiting for log output. Start a stream to view Android logcat entries."
                : "No log entries matched the current filters."}
            </div>
          ) : (
            filteredEntries.map((entry, index) => {
              const key = buildEntryKey(entry, index);
              const isSelected = key === selectedEntryKey;
              const rowLevelClass = levelClass(entry.level);

              return (
                <button
                  key={key}
                  type="button"
                  className={`logcat-row logcat-row--${rowLevelClass} ${isSelected ? "selected" : ""}`}
                  onClick={() => {
                    setSelectedEntryKey(key);
                    setUiFeedback("");
                  }}
                >
                  <span className="logcat-row__timestamp">{entry.timestamp || "--"}</span>
                  <span className="logcat-row__pid">{entry.pid ?? "—"}</span>
                  <span className={`logcat-row__level logcat-row__level--${rowLevelClass}`}>
                    {entry.level || "?"}
                  </span>
                  <span className="logcat-row__tag">{entry.tag || "raw"}</span>
                  <span className="logcat-row__message">{entry.message || entry.raw}</span>
                </button>
              );
            })
          )}
        </div>

        <div className="logcat-terminal__actions">
          <button className="logcat-button logcat-button--ghost" type="button" onClick={handleJumpBottom}>
            <ArrowDown size={16} />
            Jump Bottom
          </button>
          <button
            className="logcat-button logcat-button--ghost"
            type="button"
            disabled={!selectedEntry}
            onClick={() => void handleCopySelected()}
          >
            <Copy size={16} />
            Copy Selected
          </button>
          <button
            className="logcat-button logcat-button--ghost"
            type="button"
            disabled={filteredEntries.length === 0}
            onClick={() => void handleCopyVisible()}
          >
            <Copy size={16} />
            Copy Visible
          </button>
          <button
            className="logcat-button logcat-button--ghost"
            type="button"
            disabled={entries.length === 0}
            onClick={() => void handleCopyAll()}
          >
            <Copy size={16} />
            Copy All
          </button>
          <button
            className="logcat-button logcat-button--ghost"
            type="button"
            disabled={entries.length === 0}
            onClick={() => {
              clearEntries();
              setSelectedEntryKey(null);
              setUiFeedback("Cleared in-memory log rows from the viewer");
            }}
          >
            <Trash2 size={16} />
            Clear UI
          </button>
        </div>
      </Card>
    </div>
  );
}
