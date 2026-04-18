import { invoke } from "@tauri-apps/api/core"
import { listen } from "@tauri-apps/api/event"
import { useState, useEffect, useRef } from "react"
import "../stage1/Stage1Card.css"
import "../stage2/Stage2Card.css"

interface Stage3Result {
  udid: string
  baseband_version: string
  baseband_serial: string
  baseband_cert_id: string
  baseband_chip_id: string
  sim_status: string
  sim_status_label: string
  sim_tray_status: string
  iccid: string
  imsi: string
  mcc_mnc: string
  carrier_name: string
  carrier_bundle: string
  carrier_roaming: boolean
  sim_lock_type: string
  is_carrier_locked: boolean
  is_sim_absent: boolean
  is_sim_blocked: boolean
  current_mcc: string
  current_mnc: string
  phone_number: string
  data_roaming: boolean
  lock_analysis: string
  bypass_method: string
  stage_passed: boolean
  stage_message: string
}

export function Stage3Card({
  udid,
  onPass,
  onBack,
}: {
  udid: string
  onPass: (r: Stage3Result) => void
  onBack: () => void
}) {
  const [loading, setLoading] = useState(false)
  const [logs, setLogs] = useState<string[]>([])
  const [result, setResult] = useState<Stage3Result | null>(null)
  const [error, setError] = useState<string | null>(null)
  const logRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const u = listen<string>("s3-log", (e) =>
      setLogs((p) => [...p, e.payload])
    )
    return () => {
      u.then((f) => f())
    }
  }, [])

  useEffect(() => {
    if (logRef.current)
      logRef.current.scrollTop = logRef.current.scrollHeight
  }, [logs])

  async function run() {
    setLoading(true)
    setError(null)
    setLogs([])
    setResult(null)
    try {
      const r = await invoke<Stage3Result>("signal_stage3_baseband", {
        udid,
      })
      setResult(r)
    } catch (e) {
      setError(String(e))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="s1-card">
      <div className="s1-top-row">
        <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
          <button className="back-btn" onClick={onBack}>
            ← 2
          </button>
          <span className="stage-pill">Stage 3 / 10</span>
        </div>
        <div className="score-orb">99</div>
      </div>

      <div className="model-line">BASEBAND · SIM · CARRIER LOCK</div>
      <h2 className="s1-title">Baseband Analysis</h2>
      <p className="s1-sub">
        SIM lock · carrier identity · network registration
      </p>

      {/* Dynamic badges after result */}
      {result ? (
        <>
          <div className="badge-row">
            <span
              className={`bd ${result.is_carrier_locked ? "orange" : "green"}`}
            >
              {result.is_carrier_locked ? "🔒 LOCKED" : "● UNLOCKED"}
            </span>
            <span
              className={`bd ${result.is_sim_absent ? "orange" : "safe"}`}
            >
              {result.is_sim_absent ? "NO SIM" : "SIM OK"}
            </span>
            <span className="bd outline">{result.baseband_version}</span>
          </div>
          <div className="badge-row">
            <span className="bd outline">
              {result.carrier_name !== "N/A"
                ? result.carrier_name
                : "No Carrier"}
            </span>
            <span className="bd amber">MCC {result.mcc_mnc}</span>
          </div>
        </>
      ) : (
        <>
          <div className="badge-row">
            <span className="bd outline">BASEBAND</span>
            <span className="bd outline">SIM LOCK</span>
            <span className="bd outline">CARRIER</span>
          </div>
          <div className="badge-row">
            <span className="bd outline">IMSI</span>
            <span className="bd outline">MCC/MNC</span>
          </div>
        </>
      )}

      {/* Log console */}
      {logs.length > 0 && (
        <div className="log-console" ref={logRef}>
          {logs.map((l, i) => (
            <div
              key={i}
              className={
                l.startsWith("✅") || l.includes("PASSED")
                  ? "log-ok"
                  : l.startsWith("❌") || l.includes("BLOCKED")
                    ? "log-err"
                    : l.startsWith("⚠️")
                      ? "log-warn"
                      : l.startsWith("📡") ||
                          l.startsWith("💳") ||
                          l.startsWith("📶") ||
                          l.startsWith("🌐") ||
                          l.startsWith("🔒")
                        ? "log-info"
                        : "log-line"
              }
            >
              {l}
            </div>
          ))}
        </div>
      )}

      {error && (
        <div className="err-box">
          {error.split("\n").map((l, i) => (
            <div key={i}>{l}</div>
          ))}
        </div>
      )}

      {/* Result grid */}
      {result && (
        <div className="result-grid">
          <Row l="Baseband" v={result.baseband_version} />
          <Row l="BB Serial" v={result.baseband_serial} mono />
          <Row
            l="SIM"
            v={result.sim_status_label}
            cls={
              result.is_carrier_locked
                ? "v-warn"
                : result.is_sim_absent
                  ? "v-warn"
                  : "v-ok"
            }
          />
          <Row l="Tray" v={result.sim_tray_status} />
          <Row l="ICCID" v={result.iccid} mono />
          <Row l="IMSI" v={result.imsi} mono />
          <Row l="MCC/MNC" v={result.mcc_mnc} />
          <Row l="Carrier" v={result.carrier_name} />
          <Row l="Phone #" v={result.phone_number} />
          <Row
            l="Lock Type"
            v={result.sim_lock_type}
            cls={result.sim_lock_type === "None" ? "v-ok" : "v-warn"}
          />
          <Row l="Method" v={result.bypass_method} />
        </div>
      )}

      {result && (
        <div className={result.stage_passed ? "pass-banner" : "fail-banner"}>
          {result.stage_message}
        </div>
      )}

      <button className="run-btn" onClick={run} disabled={loading}>
        {loading ? "⏳ Analyzing..." : "⚡ RUN"}
      </button>

      {result?.stage_passed && (
        <button className="next-btn" onClick={() => onPass(result)}>
          → Stage 4: iCloud Lock Detection
        </button>
      )}
    </div>
  )
}

function Row({
  l,
  v,
  mono,
  cls,
}: {
  l: string
  v: string
  mono?: boolean
  cls?: string
}) {
  return (
    <div className="info-row">
      <span className="info-label">{l}</span>
      <span className={`info-val ${mono ? "mono" : ""} ${cls || ""}`}>
        {v}
      </span>
    </div>
  )
}
