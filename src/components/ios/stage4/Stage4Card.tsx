import { invoke } from "@tauri-apps/api/core"
import { listen } from "@tauri-apps/api/event"
import { useState, useEffect, useRef } from "react"
import "../stage1/Stage1Card.css"
import "../stage2/Stage2Card.css"

interface Stage4Result {
  udid: string
  activation_state: string
  is_icloud_locked: boolean
  is_activation_locked: boolean
  is_demo_unit: boolean
  is_internal_build: boolean
  activation_record_exists: boolean
  activation_ticket_hash: string
  wildcard_ticket: boolean
  eligible_for_ios_update: boolean
  device_color: string
  region_info: string
  product_name: string
  act_tool_output: string
  act_tool_available: boolean
  find_my_state: string
  owner_apple_id: string
  dst_root_available: boolean
  activation_server_reachable: boolean
  lock_severity: string
  bypass_route: string
  stage_passed: boolean
  stage_message: string
}

export function Stage4Card({
  udid,
  onPass,
  onBack,
}: {
  udid: string
  onPass: (r: Stage4Result) => void
  onBack: () => void
}) {
  const [loading, setLoading] = useState(false)
  const [logs, setLogs] = useState<string[]>([])
  const [result, setResult] = useState<Stage4Result | null>(null)
  const [error, setError] = useState<string | null>(null)
  const logRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const u = listen<string>("s4-log", (e) =>
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
      const r = await invoke<Stage4Result>("signal_stage4_icloud", {
        udid,
      })
      setResult(r)
    } catch (e) {
      setError(String(e))
    } finally {
      setLoading(false)
    }
  }

  const severityColor = (s: string) =>
    s === "Hard Lock" ? "v-err" : s === "Soft Lock" ? "v-warn" : "v-ok"

  return (
    <div className="s1-card">
      <div className="s1-top-row">
        <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
          <button className="back-btn" onClick={onBack}>
            ← 3
          </button>
          <span className="stage-pill">Stage 4 / 10</span>
        </div>
        <div className="score-orb">99</div>
      </div>

      <div className="model-line">iCLOUD · FIND MY · ACTIVATION LOCK</div>
      <h2 className="s1-title">iCloud Deep Scan</h2>
      <p className="s1-sub">
        Activation lock · wildcard ticket · Apple server check
      </p>

      {/* Badges */}
      {result ? (
        <>
          <div className="badge-row">
            <span
              className={`bd ${result.is_icloud_locked ? "orange" : "green"}`}
            >
              {result.is_icloud_locked
                ? "☁️ iCLOUD LOCKED"
                : "☁️ iCLOUD CLEAR"}
            </span>
            <span
              className={`bd ${result.find_my_state === "On" ? "orange" : "safe"}`}
            >
              {result.find_my_state === "On" ? "FIND MY ON" : "FIND MY OFF"}
            </span>
          </div>
          <div className="badge-row">
            <span
              className={`bd ${
                result.lock_severity === "Hard Lock"
                  ? "orange"
                  : result.lock_severity === "Soft Lock"
                    ? "amber"
                    : "safe"
              }`}
            >
              {result.lock_severity.toUpperCase()}
            </span>
            <span
              className={`bd ${result.wildcard_ticket ? "amber" : "outline"}`}
            >
              {result.wildcard_ticket ? "WILDCARD ⚡" : "NO WILDCARD"}
            </span>
            <span
              className={`bd ${result.activation_server_reachable ? "safe" : "orange"}`}
            >
              {result.activation_server_reachable ? "SERVER OK" : "SERVER ⚠️"}
            </span>
          </div>
        </>
      ) : (
        <>
          <div className="badge-row">
            <span className="bd outline">iCLOUD LOCK</span>
            <span className="bd outline">FIND MY</span>
            <span className="bd outline">WILDCARD</span>
          </div>
          <div className="badge-row">
            <span className="bd outline">ACT RECORD</span>
            <span className="bd outline">SERVER CHECK</span>
          </div>
        </>
      )}

      {/* Logs */}
      {logs.length > 0 && (
        <div className="log-console" ref={logRef}>
          {logs.map((l, i) => (
            <div
              key={i}
              className={
                l.includes("PASSED") || l.startsWith("✅")
                  ? "log-ok"
                  : l.includes("BLOCKED") ||
                      l.startsWith("❌") ||
                      l.startsWith("⛔")
                    ? "log-err"
                    : l.startsWith("⚠️")
                      ? "log-warn"
                      : l.startsWith("☁️") ||
                          l.startsWith("🔍") ||
                          l.startsWith("🎟️") ||
                          l.startsWith("🍎") ||
                          l.startsWith("🌐") ||
                          l.startsWith("🔐")
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
          <Row
            l="Act State"
            v={result.activation_state}
            cls={result.is_icloud_locked ? "v-err" : "v-ok"}
          />
          <Row
            l="iCloud"
            v={result.is_icloud_locked ? "LOCKED ⛔" : "Clear ✅"}
            cls={result.is_icloud_locked ? "v-err" : "v-ok"}
          />
          <Row
            l="Find My"
            v={result.find_my_state}
            cls={result.find_my_state === "On" ? "v-warn" : "v-ok"}
          />
          <Row
            l="Severity"
            v={result.lock_severity}
            cls={severityColor(result.lock_severity)}
          />
          <Row
            l="Wildcard"
            v={result.wildcard_ticket ? "Yes ⚡" : "No"}
            cls={result.wildcard_ticket ? "v-warn" : "v-ok"}
          />
          <Row l="Ticket" v={result.activation_ticket_hash} mono />
          <Row l="Apple ID" v={result.owner_apple_id} />
          <Row l="Product" v={result.product_name} />
          <Row l="Region" v={result.region_info} />
          <Row
            l="Server"
            v={
              result.activation_server_reachable
                ? "Reachable ✅"
                : "Offline ⚠️"
            }
            cls={result.activation_server_reachable ? "v-ok" : "v-warn"}
          />
          <Row l="Route" v={result.bypass_route} />
        </div>
      )}

      {result && (
        <div className={result.stage_passed ? "pass-banner" : "fail-banner"}>
          {result.stage_message}
        </div>
      )}

      <button className="run-btn" onClick={run} disabled={loading}>
        {loading ? "⏳ Scanning..." : "⚡ RUN"}
      </button>

      {result?.stage_passed && (
        <button className="next-btn" onClick={() => onPass(result)}>
          → Stage 5: MDM Profile Removal
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
