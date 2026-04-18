import { invoke } from "@tauri-apps/api/core"
import { listen } from "@tauri-apps/api/event"
import { useState, useEffect, useRef } from "react"
import "../stage1/Stage1Card.css"
import "./Stage2Card.css"

interface Stage2Result {
  udid: string
  activation_status: string
  activation_enum: string
  is_icloud_locked: boolean
  is_activated: boolean
  apple_id_linked: string
  find_my_enabled: boolean
  supervision_enabled: boolean
  supervised_by: string
  escrow_bag: string
  activation_blob: string
  bypass_possible: boolean
  recommended_action: string
  stage_passed: boolean
  stage_message: string
}

export function Stage2Card({
  udid,
  onPass,
  onBack,
}: {
  udid: string
  onPass: (r: Stage2Result) => void
  onBack: () => void
}) {
  const [loading, setLoading] = useState(false)
  const [logs, setLogs] = useState<string[]>([])
  const [result, setResult] = useState<Stage2Result | null>(null)
  const [error, setError] = useState<string | null>(null)
  const logRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const u = listen<string>("s2-log", (e) =>
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
      const r = await invoke<Stage2Result>("signal_stage2_activation", {
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
      {/* Header */}
      <div className="s1-top-row">
        <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
          <button className="back-btn" onClick={onBack}>
            ← 1
          </button>
          <span className="stage-pill">Stage 2 / 10</span>
        </div>
        <div className="score-orb">99</div>
      </div>

      <div className="model-line">ACTIVATION LOCK ANALYSIS</div>
      <h2 className="s1-title">Activation Check</h2>
      <p className="s1-sub">iCloud lock · Find My · MDM supervision</p>

      {/* Status badges — dynamic */}
      {result && (
        <>
          <div className="badge-row">
            <span
              className={`bd ${result.is_activated ? "green" : "orange"}`}
            >
              {result.is_activated ? "● ACTIVATED" : "● NOT ACTIVATED"}
            </span>
            <span
              className={`bd ${result.find_my_enabled ? "orange" : "safe"}`}
            >
              {result.find_my_enabled ? "FIND MY ON" : "FIND MY OFF"}
            </span>
          </div>
          <div className="badge-row">
            <span
              className={`bd ${result.is_icloud_locked ? "orange" : "safe"}`}
            >
              {result.is_icloud_locked ? "iCLOUD LOCKED" : "iCLOUD OK"}
            </span>
            <span
              className={`bd ${result.supervision_enabled ? "orange" : "outline"}`}
            >
              {result.supervision_enabled ? "MDM SUPERVISED" : "NO MDM"}
            </span>
          </div>
        </>
      )}

      {/* Static badges before run */}
      {!result && (
        <>
          <div className="badge-row">
            <span className="bd outline">ACTIVATION STATE</span>
            <span className="bd outline">iCLOUD CHECK</span>
          </div>
          <div className="badge-row">
            <span className="bd outline">FIND MY</span>
            <span className="bd outline">MDM</span>
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
                l.startsWith("✅")
                  ? "log-ok"
                  : l.startsWith("❌") || l.startsWith("⛔")
                    ? "log-err"
                    : l.startsWith("⚠️")
                      ? "log-warn"
                      : l.startsWith("☁️") ||
                          l.startsWith("🍎") ||
                          l.startsWith("🔍") ||
                          l.startsWith("🔑") ||
                          l.startsWith("🔐") ||
                          l.startsWith("🏢")
                        ? "log-info"
                        : "log-line"
              }
            >
              {l}
            </div>
          ))}
        </div>
      )}

      {/* Error */}
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
          <Row
            l="State"
            v={result.activation_enum}
            cls={result.is_activated ? "v-ok" : "v-warn"}
          />
          <Row
            l="iCloud"
            v={result.is_icloud_locked ? "LOCKED ⛔" : "Clear ✅"}
            cls={result.is_icloud_locked ? "v-err" : "v-ok"}
          />
          <Row
            l="Find My"
            v={result.find_my_enabled ? "ON ⚠️" : "OFF ✅"}
            cls={result.find_my_enabled ? "v-warn" : "v-ok"}
          />
          <Row
            l="MDM"
            v={result.supervision_enabled ? "Supervised ⚠️" : "None ✅"}
            cls={result.supervision_enabled ? "v-warn" : "v-ok"}
          />
          {result.supervision_enabled && (
            <Row l="Org" v={result.supervised_by} />
          )}
          <Row l="Apple ID" v={result.apple_id_linked} />
          <Row l="Action" v={result.recommended_action} />
        </div>
      )}

      {/* Pass/fail banner */}
      {result && (
        <div
          className={result.stage_passed ? "pass-banner" : "fail-banner"}
        >
          {result.stage_message}
        </div>
      )}

      <button className="run-btn" onClick={run} disabled={loading}>
        {loading ? "⏳ Checking..." : "⚡ RUN"}
      </button>

      {result?.stage_passed && (
        <button className="next-btn" onClick={() => onPass(result)}>
          → Stage 3: Baseband Analysis
        </button>
      )}
    </div>
  )
}

function Row({
  l,
  v,
  cls,
}: {
  l: string
  v: string
  cls?: string
}) {
  return (
    <div className="info-row">
      <span className="info-label">{l}</span>
      <span className={`info-val ${cls || ""}`}>{v}</span>
    </div>
  )
}
