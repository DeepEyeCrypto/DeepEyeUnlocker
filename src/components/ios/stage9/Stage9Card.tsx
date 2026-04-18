import { invoke } from "@tauri-apps/api/core"
import { listen } from "@tauri-apps/api/event"
import { useState, useEffect, useRef } from "react"
import "../stage1/Stage1Card.css"
import "../stage2/Stage2Card.css"

interface VerificationCheck {
  name: string
  expected: string
  actual: string
  passed: boolean
  critical: boolean
}

interface Stage9Result {
  udid: string
  checks: VerificationCheck[]
  total_checks: number
  passed_checks: number
  failed_critical: number
  final_carrier: string
  final_sim_status: string
  final_phone_number: string
  final_imei: string
  final_mcc: string
  final_mnc: string
  final_baseband: string
  final_activation_state: string
  signal_ok: boolean
  sim_ok: boolean
  carrier_ok: boolean
  imei_ok: boolean
  activation_ok: boolean
  calls_ok: boolean
  data_ok: boolean
  bypass_score: number
  bypass_grade: string
  stage_passed: boolean
  ready_for_completion: boolean
  stage_message: string
}

export function Stage9Card({
  udid,
  onPass,
  onBack,
}: {
  udid: string
  onPass: (r: Stage9Result) => void
  onBack: () => void
}) {
  const [loading, setLoading] = useState(false)
  const [logs, setLogs] = useState<string[]>([])
  const [result, setResult] = useState<Stage9Result | null>(null)
  const [error, setError] = useState<string | null>(null)
  const logRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const u = listen<string>("s9-log", (e) =>
      setLogs((p) => [...p, e.payload])
    )
    return () => {
      u.then((f) => f())
    }
  }, [])

  useEffect(() => {
    if (logRef.current) logRef.current.scrollTop = logRef.current.scrollHeight
  }, [logs])

  async function run() {
    setLoading(true)
    setError(null)
    setLogs([])
    setResult(null)
    try {
      const r = await invoke<Stage9Result>("signal_stage9_verify", { udid })
      setResult(r)
    } catch (e) {
      setError(String(e))
    } finally {
      setLoading(false)
    }
  }

  // Score ring color
  const scoreColor = (s: number) =>
    s >= 90
      ? "#22c55e"
      : s >= 75
        ? "#a78bfa"
        : s >= 60
          ? "#fb923c"
          : "#ef4444"

  return (
    <div className="s1-card">
      <div className="s1-top-row">
        <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
          <button className="back-btn" onClick={onBack}>
            ← 8
          </button>
          <span className="stage-pill">Stage 9 / 10</span>
        </div>
        {/* Score ring */}
        <div
          className="score-orb"
          style={{
            background: result ? scoreColor(result.bypass_score) : undefined,
          }}
        >
          {result ? result.bypass_score : "99"}
        </div>
      </div>

      <div className="model-line">VERIFICATION · 10-POINT CHECK · SCORING</div>
      <h2 className="s1-title">Final Verification</h2>
      <p className="s1-sub">
        10-check verification — SIM · carrier · IMEI · activation · phone · MCC
        · ICCID
      </p>

      {/* Badges */}
      {result ? (
        <>
          <div className="badge-row">
            <span
              className={`bd ${
                result.bypass_score >= 90
                  ? "green"
                  : result.bypass_score >= 75
                    ? "safe"
                    : "orange"
              }`}
            >
              Grade {result.bypass_grade}
            </span>
            <span
              className={`bd ${result.bypass_score >= 75 ? "green" : "orange"}`}
            >
              {result.bypass_score}/100
            </span>
            <span
              className={`bd ${result.failed_critical === 0 ? "safe" : "orange"}`}
            >
              {result.passed_checks}/{result.total_checks} PASS
            </span>
          </div>
          <div className="badge-row">
            <span className={`bd ${result.calls_ok ? "green" : "outline"}`}>
              {result.calls_ok ? "📞 CALLS ✅" : "📞 PENDING"}
            </span>
            <span className={`bd ${result.data_ok ? "green" : "outline"}`}>
              {result.data_ok ? "📶 DATA ✅" : "📶 PENDING"}
            </span>
          </div>
        </>
      ) : (
        <>
          <div className="badge-row">
            <span className="bd outline">SIM CHECK</span>
            <span className="bd outline">CARRIER</span>
            <span className="bd outline">IMEI</span>
          </div>
          <div className="badge-row">
            <span className="bd outline">ACTIVATION</span>
            <span className="bd outline">PHONE</span>
            <span className="bd outline">MCC/ICCID</span>
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
                l.includes("VERIFIED") || l.startsWith("✅")
                  ? "log-ok"
                  : l.startsWith("❌")
                    ? "log-err"
                    : l.startsWith("⚠️")
                      ? "log-warn"
                      : l.startsWith("🔍") ||
                          l.startsWith("✅") ||
                          l.startsWith("🔑") ||
                          l.startsWith("📊") ||
                          l.startsWith("📱")
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

      {/* Checks table */}
      {result && result.checks.length > 0 && (
        <div className="result-grid">
          <div className="info-row">
            <span
              className="info-label"
              style={{ color: "#a78bfa", fontWeight: 600 }}
            >
              Verification Checks
            </span>
            <span className="info-val" style={{ color: "#a78bfa" }}>
              {result.passed_checks}/{result.total_checks}
            </span>
          </div>
          {result.checks.map((c, i) => (
            <div key={i} className="info-row">
              <span className="info-label">
                {c.critical ? "🔴" : "🔵"} {c.name}
              </span>
              <span
                className={`info-val ${
                  c.passed ? "v-ok" : c.critical ? "v-err" : "v-warn"
                }`}
                style={{ fontSize: "11px" }}
              >
                {c.passed ? "✅" : c.critical ? "❌" : "⚠️"}{" "}
                {c.actual.length > 22
                  ? c.actual.slice(0, 22) + "…"
                  : c.actual}
              </span>
            </div>
          ))}
        </div>
      )}

      {/* Capability matrix */}
      {result && (
        <div className="result-grid" style={{ marginTop: 8 }}>
          <div className="info-row">
            <span
              className="info-label"
              style={{ color: "#a78bfa", fontWeight: 600 }}
            >
              Capabilities
            </span>
          </div>
          {[
            ["Signal", result.signal_ok],
            ["SIM", result.sim_ok],
            ["Carrier", result.carrier_ok],
            ["Calls", result.calls_ok],
            ["Data", result.data_ok],
            ["IMEI", result.imei_ok],
            ["Activation", result.activation_ok],
          ].map(([label, ok], i) => (
            <div key={i} className="info-row">
              <span className="info-label">{label as string}</span>
              <span className={`info-val ${(ok as boolean) ? "v-ok" : "v-warn"}`}>
                {(ok as boolean) ? "✅ YES" : "⚠️ PENDING"}
              </span>
            </div>
          ))}
        </div>
      )}

      {/* Final values */}
      {result && (
        <div className="result-grid" style={{ marginTop: 8 }}>
          <Row
            l="Carrier"
            v={result.final_carrier}
            cls={result.carrier_ok ? "v-ok" : "v-warn"}
          />
          <Row l="Phone" v={result.final_phone_number} />
          <Row
            l="SIM"
            v={simpleSim(result.final_sim_status)}
            cls={result.sim_ok ? "v-ok" : "v-warn"}
          />
          <Row
            l="Activation"
            v={result.final_activation_state}
            cls={result.activation_ok ? "v-ok" : "v-warn"}
          />
          <Row
            l="MCC/MNC"
            v={`${result.final_mcc}/` + result.final_mnc}
          />
          <Row l="Baseband" v={result.final_baseband} mono />
          <Row l="IMEI" v={result.final_imei} mono />
        </div>
      )}

      {result && (
        <div
          className={
            result.bypass_score >= 75
              ? "pass-banner"
              : result.bypass_score >= 60
                ? "warn-banner"
                : "fail-banner"
          }
          style={{ whiteSpace: "pre-wrap" }}
        >
          {result.stage_message}
        </div>
      )}

      <button className="run-btn" onClick={run} disabled={loading}>
        {loading ? "⏳ Verifying..." : "⚡ RUN"}
      </button>

      {result?.stage_passed && (
        <button className="next-btn" onClick={() => onPass(result)}>
          → Stage 10: Completion Report
        </button>
      )}
    </div>
  )
}

function simpleSim(s: string): string {
  if (s.includes("Ready")) return "Ready"
  if (s.includes("Absent")) return "Absent"
  if (s.includes("Not")) return "Not Ready"
  return s.split(/(?=[A-Z])/).pop() || s
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
