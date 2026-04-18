import { invoke } from "@tauri-apps/api/core"
import { listen } from "@tauri-apps/api/event"
import { useState, useEffect, useRef } from "react"
import "../stage1/Stage1Card.css"
import "../stage2/Stage2Card.css"

interface UnlockAttempt {
  method: string
  success: boolean
  output: string
}

interface Stage6Result {
  udid: string
  carrier_before: string
  sim_status_before: string
  is_locked_before: boolean
  attempts: UnlockAttempt[]
  total_attempts: number
  successful_attempts: number
  carrier_after: string
  sim_status_after: string
  is_unlocked_after: boolean
  phone_number_after: string
  tried_lockdown_reset: boolean
  tried_carrier_bundle_reset: boolean
  tried_network_reset: boolean
  tried_activation_reset: boolean
  unlock_achieved: boolean
  stage_passed: boolean
  stage_message: string
}

export function Stage6Card({
  udid,
  onPass,
  onBack,
}: {
  udid: string
  onPass: (r: Stage6Result) => void
  onBack: () => void
}) {
  const [loading, setLoading] = useState(false)
  const [logs, setLogs] = useState<string[]>([])
  const [result, setResult] = useState<Stage6Result | null>(null)
  const [error, setError] = useState<string | null>(null)
  const logRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const u = listen<string>("s6-log", (e) =>
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
      const r = await invoke<Stage6Result>("signal_stage6_carrier", {
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
            ← 5
          </button>
          <span className="stage-pill">Stage 6 / 10</span>
        </div>
        <div className="score-orb">99</div>
      </div>

      <div className="model-line">
        CARRIER LOCK · SIGNAL RESTORE · SIM UNLOCK
      </div>
      <h2 className="s1-title">Carrier Restriction Bypass</h2>
      <p className="s1-sub">
        5-method bypass: activation · lockdown · bundle · network · SIM
        re-read
      </p>

      {/* Badges */}
      {result ? (
        <>
          <div className="badge-row">
            <span
              className={`bd ${result.unlock_achieved ? "green" : "orange"}`}
            >
              {result.unlock_achieved ? "● UNLOCKED" : "⚠️ PARTIAL"}
            </span>
            <span
              className={`bd ${result.is_unlocked_after ? "safe" : "outline"}`}
            >
              SIM {result.is_unlocked_after ? "READY" : "PENDING"}
            </span>
          </div>
          <div className="badge-row">
            <span className="bd outline">
              {result.carrier_after !== "N/A"
                ? result.carrier_after
                : "No Carrier"}
            </span>
            <span className="bd amber">
              {result.successful_attempts}/{result.total_attempts} OK
            </span>
          </div>
        </>
      ) : (
        <>
          <div className="badge-row">
            <span className="bd outline">ACTIVATION RESET</span>
            <span className="bd outline">LOCKDOWN</span>
          </div>
          <div className="badge-row">
            <span className="bd outline">CARRIER BUNDLE</span>
            <span className="bd outline">SIM RE-READ</span>
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
                l.includes("PASSED") ||
                l.includes("UNLOCKED") ||
                l.startsWith("✅")
                  ? "log-ok"
                  : l.includes("BLOCKED") || l.startsWith("❌")
                    ? "log-err"
                    : l.startsWith("⚠️") || l.includes("PARTIAL")
                      ? "log-warn"
                      : l.startsWith("📸") ||
                          l.startsWith("🔑") ||
                          l.startsWith("🔓") ||
                          l.startsWith("📦") ||
                          l.startsWith("🌐") ||
                          l.startsWith("💳") ||
                          l.startsWith("📊")
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

      {/* Attempts list */}
      {result && result.attempts.length > 0 && (
        <div className="result-grid">
          <div className="info-row">
            <span
              className="info-label"
              style={{ color: "#a78bfa", fontWeight: 600 }}
            >
              Bypass Attempts
            </span>
          </div>
          {result.attempts.map((a, i) => (
            <div key={i} className="info-row">
              <span className="info-label">{a.success ? "✅" : "⚠️"}</span>
              <span className="info-val" style={{ fontSize: "11px" }}>
                {a.method}
                <span className="info-sub">{a.output}</span>
              </span>
            </div>
          ))}
        </div>
      )}

      {/* Before/After grid */}
      {result && (
        <div className="result-grid" style={{ marginTop: 8 }}>
          <Row
            l="Before"
            v={`${result.carrier_before} — ${
              result.sim_status_before.split("Status").pop()?.replace(/^SIM/, "") ||
              "?"
            }`}
          />
          <Row
            l="After"
            v={`${result.carrier_after} — ${
              result.sim_status_after.split("Status").pop()?.replace(/^SIM/, "") ||
              "?"
            }`}
            cls={result.is_unlocked_after ? "v-ok" : "v-warn"}
          />
          <Row l="Phone" v={result.phone_number_after} />
          <Row
            l="Unlocked"
            v={result.unlock_achieved ? "Yes ✅" : "Partial ⚠️"}
            cls={result.unlock_achieved ? "v-ok" : "v-warn"}
          />
        </div>
      )}

      {result && (
        <div className={result.stage_passed ? "pass-banner" : "fail-banner"}>
          {result.stage_message}
        </div>
      )}

      <button className="run-btn" onClick={run} disabled={loading}>
        {loading ? "⏳ Bypassing..." : "⚡ RUN"}
      </button>

      {result?.stage_passed && (
        <button className="next-btn" onClick={() => onPass(result)}>
          → Stage 7: IMEI Registration
        </button>
      )}
    </div>
  )
}

function Row({ l, v, cls }: { l: string; v: string; cls?: string }) {
  return (
    <div className="info-row">
      <span className="info-label">{l}</span>
      <span className={`info-val ${cls || ""}`}>{v}</span>
    </div>
  )
}
