import { invoke } from "@tauri-apps/api/core"
import { listen } from "@tauri-apps/api/event"
import { useEffect, useRef, useState } from "react"

import "../edlPipeline.css"
import type { EdlStage1Result, QcomDevice } from "../types"

export function EdlStage1Card({
  onPass,
  onClose,
}: {
  onPass: (result: EdlStage1Result) => void
  onClose: () => void
}) {
  const [loading, setLoading] = useState(false)
  const [logs, setLogs] = useState<string[]>([])
  const [result, setResult] = useState<EdlStage1Result | null>(null)
  const [error, setError] = useState<string | null>(null)
  const logRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    let unlisten: (() => void) | null = null

    void listen<string>("edl-s1", (event) => {
      setLogs((previous) => [...previous, event.payload])
    }).then((dispose) => {
      unlisten = dispose
    })

    return () => {
      if (unlisten) {
        unlisten()
      }
    }
  }, [])

  useEffect(() => {
    if (logRef.current) {
      logRef.current.scrollTop = logRef.current.scrollHeight
    }
  }, [logs])

  async function run() {
    setLoading(true)
    setError(null)
    setLogs([])
    setResult(null)

    try {
      const stageResult = await invoke<EdlStage1Result>("edl_stage1_detect")
      setResult(stageResult)
    } catch (caughtError: unknown) {
      setError(String(caughtError))
    } finally {
      setLoading(false)
    }
  }

  const pidColor = (device: QcomDevice): string => {
    if (device.is_edl_mode) {
      return "#22c55e"
    }

    if (device.is_fastboot) {
      return "#fb923c"
    }

    return "#60a5fa"
  }

  const pidLabel = (device: QcomDevice): string => {
    if (device.is_edl_mode) {
      return "9008"
    }

    if (device.is_fastboot) {
      return "FB"
    }

    return "ADB"
  }

  return (
    <div className="edl-flow-shell">
      <div className="edl-stage-card">
        <div className="edl-stage-top-row">
          <div className="edl-stage-top-actions">
            <button className="edl-icon-btn" onClick={onClose} type="button">
              ✕
            </button>
            <span className="edl-stage-pill">EDL 1/20</span>
          </div>

          <div
            className="edl-score-orb"
            style={{
              background: result ? (result.edl_count > 0 ? "#22c55e" : "#f97316") : undefined,
              fontSize: "10px",
              fontFamily: "monospace",
            }}
          >
            {result ? (result.edl_count > 0 ? "9008" : "⚠️") : "⚡"}
          </div>
        </div>

        <div className="edl-model-line">QUALCOMM · VID:05C6 · PID:9008 · EDL</div>
        <h2 className="edl-stage-title">EDL Device Detection</h2>
        <p className="edl-stage-sub">
          Scans 9008 mode, attempts ADB or Fastboot transitions into EDL, and surfaces a
          seven-method hardware entry guide.
        </p>

        {result ? (
          <>
            <div className="edl-badge-row">
              <span className={`edl-badge ${result.edl_count > 0 ? "green" : "orange"}`}>
                {result.edl_count > 0 ? `✅ ${result.edl_count} × 9008` : "⚠️ NO EDL"}
              </span>
              <span className={`edl-badge ${result.fastboot_count > 0 ? "safe" : "outline"}`}>
                {result.fastboot_count} FB
              </span>
              <span className={`edl-badge ${result.adb_count > 0 ? "safe" : "outline"}`}>
                {result.adb_count} ADB
              </span>
            </div>

            <div className="edl-badge-row">
              <span className={`edl-badge ${result.qdl_available ? "safe" : "outline"}`}>
                {result.qdl_available ? "✅ QDL" : "⚠️ QDL"}
              </span>
              <span className={`edl-badge ${result.edl_prog_found ? "safe" : "outline"}`}>
                {result.edl_prog_found ? "✅ FIREHOSE" : "⚠️ FIREHOSE"}
              </span>
              <span className={`edl-badge ${result.adb_available ? "safe" : "outline"}`}>
                {result.adb_available ? "✅ ADB" : "⚠️ ADB"}
              </span>
              <span className={`edl-badge ${result.fastboot_available ? "safe" : "outline"}`}>
                {result.fastboot_available ? "✅ FB" : "⚠️ FB"}
              </span>
            </div>
          </>
        ) : (
          <div className="edl-badge-row">
            <span className="edl-badge outline">9008</span>
            <span className="edl-badge outline">ADB</span>
            <span className="edl-badge outline">FB</span>
            <span className="edl-badge outline">QDL</span>
          </div>
        )}

        {logs.length > 0 ? (
          <div className="edl-console" ref={logRef}>
            {logs.map((line, index) => (
              <div
                key={`${line}-${index}`}
                className={
                  line.startsWith("✅") || line.includes("9008")
                    ? "log-ok"
                    : line.startsWith("❌")
                      ? "log-err"
                      : line.startsWith("⚠️")
                        ? "log-warn"
                        : line.startsWith("🔧") ||
                            line.startsWith("🔍") ||
                            line.startsWith("📡") ||
                            line.startsWith("⚡")
                          ? "log-info"
                          : "edl-log-line"
                }
              >
                {line}
              </div>
            ))}
          </div>
        ) : null}

        {error ? <div className="edl-error-box">{error}</div> : null}

        {result && result.devices.length > 0 ? (
          <div className="edl-result-grid">
            <div className="edl-info-row">
              <span className="edl-info-label" style={{ color: "#16a34a", fontWeight: 600 }}>
                Detected Devices
              </span>
              <span className="edl-info-value" />
            </div>

            {result.devices.map((device, index) => (
              <div key={`${device.pid}-${index}`} className="edl-info-row">
                <span
                  className="edl-info-label"
                  style={{ color: pidColor(device), fontFamily: "monospace", fontSize: "11px" }}
                >
                  [{pidLabel(device)}]
                </span>
                <span className="edl-info-value" style={{ fontSize: "11px" }}>
                  {device.chipset_hint} · {device.brand_hint}
                  {device.serial ? ` · ${device.serial.slice(0, 12)}` : ""}
                </span>
              </div>
            ))}
          </div>
        ) : null}

        {result?.edl_prog_found ? (
          <div className="edl-result-grid">
            <div className="edl-info-row">
              <span className="edl-info-label">Firehose</span>
              <span
                className="edl-info-value"
                style={{ fontSize: "10px", fontFamily: "monospace", color: "#22c55e" }}
              >
                {result.firehose_path.split("/").pop()}
              </span>
            </div>
          </div>
        ) : null}

        {result && !result.stage_passed ? (
          <div className="edl-result-grid">
            <div className="edl-info-row">
              <span className="edl-info-label" style={{ color: "#f97316", fontWeight: 600 }}>
                How to enter EDL
              </span>
              <span className="edl-info-value" />
            </div>
            <ul className="edl-guide-list">
              {result.how_to_edl.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </div>
        ) : null}

        {result ? (
          <div className={`edl-banner ${result.stage_passed ? "success" : "warn"}`}>
            {result.stage_message}
          </div>
        ) : null}

        <div className="edl-actions" style={{ marginTop: "1rem" }}>
          <button className="edl-primary-btn" onClick={() => void run()} disabled={loading} type="button">
            {loading ? "⏳ Scanning USB..." : "⚡ Scan for EDL"}
          </button>

          {result?.stage_passed ? (
            <button className="edl-secondary-btn" onClick={() => onPass(result)} type="button">
              → Stage 2: Sahara Handshake
            </button>
          ) : null}
        </div>
      </div>
    </div>
  )
}
