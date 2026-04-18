import { invoke } from "@tauri-apps/api/core"
import { listen } from "@tauri-apps/api/event"
import { useState, useEffect, useRef } from "react"
import "../stage1/Stage1Card.css"
import "../stage2/Stage2Card.css"

interface SignalReadout {
  carrier: string
  sim_status: string
  phone_number: string
  current_mcc: string
  current_mnc: string
  registration_status: string
  signal_bars: string
  data_roaming: string
  voice_roaming: string
}

interface BasebandInfo {
  version: string
  chip_id: string
  serial_number: string
  is_supported: boolean
  patch_strategy: string
}

interface Stage8Result {
  udid: string
  baseband: BasebandInfo
  signal_before: SignalReadout
  step_activation_refresh: boolean
  step_network_poke: boolean
  step_sim_reinit: boolean
  step_carrier_services_reset: boolean
  step_baseband_comm_reset: boolean
  signal_after: SignalReadout
  signal_restored: boolean
  sim_ready: boolean
  carrier_registered: boolean
  calls_capable: boolean
  data_capable: boolean
  patch_output: string
  stage_passed: boolean
  stage_message: string
}

export function Stage8Card({
  udid,
  onPass,
  onBack,
}: {
  udid: string
  onPass: (r: Stage8Result) => void
  onBack: () => void
}) {
  const [loading, setLoading] = useState(false)
  const [logs, setLogs] = useState<string[]>([])
  const [result, setResult] = useState<Stage8Result | null>(null)
  const [error, setError] = useState<string | null>(null)
  const logRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const u = listen<string>("s8-log", (e) =>
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
      const r = await invoke<Stage8Result>("signal_stage8_baseband", { udid })
      setResult(r)
    } catch (e) {
      setError(String(e))
    } finally {
      setLoading(false)
    }
  }

  const steps = result
    ? [
        { label: "Activation Refresh", ok: result.step_activation_refresh },
        { label: "Network Stack Poke", ok: result.step_network_poke },
        { label: "SIM Re-init", ok: result.step_sim_reinit },
        { label: "Carrier Services Reset", ok: result.step_carrier_services_reset },
        { label: "Baseband Comm Flush", ok: result.step_baseband_comm_reset },
      ]
    : []

  return (
    <div className="s1-card">
      <div className="s1-top-row">
        <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
          <button className="back-btn" onClick={onBack}>
            ← 7
          </button>
          <span className="stage-pill">Stage 8 / 10</span>
        </div>
        <div className="score-orb">99</div>
      </div>

      <div className="model-line">BASEBAND · SIGNAL RESTORE · 5-STEP PATCH</div>
      <h2 className="s1-title">Signal Restore</h2>
      <p className="s1-sub">
        Baseband patch: activation · network · SIM · carrier · BB flush
      </p>

      {/* Badges */}
      {result ? (
        <>
          <div className="badge-row">
            <span
              className={`bd ${result.signal_restored ? "green" : "orange"}`}
            >
              {result.signal_restored ? "● SIGNAL RESTORED" : "⚡ PATCH APPLIED"}
            </span>
            <span className={`bd ${result.sim_ready ? "safe" : "outline"}`}>
              SIM {result.sim_ready ? "READY" : "SETTLING"}
            </span>
          </div>
          <div className="badge-row">
            <span className={`bd ${result.calls_capable ? "green" : "outline"}`}>
              {result.calls_capable ? "📞 CALLS" : "📞 PENDING"}
            </span>
            <span className={`bd ${result.data_capable ? "green" : "outline"}`}>
              {result.data_capable ? "📶 DATA" : "📶 PENDING"}
            </span>
            <span className="bd outline">
              BB {result.baseband.version.split(".").slice(0, 2).join(".")}
            </span>
          </div>
        </>
      ) : (
        <>
          <div className="badge-row">
            <span className="bd outline">BB IDENTIFY</span>
            <span className="bd outline">ACT REFRESH</span>
            <span className="bd outline">NET POKE</span>
          </div>
          <div className="badge-row">
            <span className="bd outline">SIM RE-INIT</span>
            <span className="bd outline">CARRIER SVC</span>
            <span className="bd outline">BB FLUSH</span>
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
                l.includes("RESTORED") || l.startsWith("✅")
                  ? "log-ok"
                  : l.includes("BLOCKED") || l.startsWith("❌")
                    ? "log-err"
                    : l.startsWith("⚠️")
                      ? "log-warn"
                      : l.startsWith("📡") ||
                          l.startsWith("📸") ||
                          l.startsWith("⚡") ||
                          l.startsWith("🌐") ||
                          l.startsWith("💳") ||
                          l.startsWith("📦") ||
                          l.startsWith("🔧") ||
                          l.startsWith("🔑") ||
                          l.startsWith("📊") ||
                          l.startsWith("📈")
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

      {/* 5 Patch Steps */}
      {result && (
        <div className="result-grid">
          <div className="info-row">
            <span
              className="info-label"
              style={{ color: "#a78bfa", fontWeight: 600 }}
            >
              Patch Steps
            </span>
          </div>
          {steps.map((s, i) => (
            <div key={i} className="info-row">
              <span className="info-label">Step {i + 1}</span>
              <span className={`info-val ${s.ok ? "v-ok" : "v-warn"}`}>
                {s.ok ? "✅" : "⚠️"} {s.label}
              </span>
            </div>
          ))}
        </div>
      )}

      {/* Before/After Signal */}
      {result && (
        <div className="result-grid" style={{ marginTop: 8 }}>
          <div className="info-row">
            <span
              className="info-label"
              style={{ color: "#a78bfa", fontWeight: 600 }}
            >
              Signal Delta
            </span>
          </div>
          <Row l="Carrier Before" v={result.signal_before.carrier} />
          <Row
            l="Carrier After"
            v={result.signal_after.carrier}
            cls={result.carrier_registered ? "v-ok" : "v-warn"}
          />
          <Row l="SIM Before" v={simpleSim(result.signal_before.sim_status)} />
          <Row
            l="SIM After"
            v={simpleSim(result.signal_after.sim_status)}
            cls={result.sim_ready ? "v-ok" : "v-warn"}
          />
          <Row l="Phone" v={result.signal_after.phone_number} />
          <Row
            l="MCC/MNC"
            v={`${result.signal_after.current_mcc}/${result.signal_after.current_mnc}`}
          />
          <Row l="Baseband" v={result.baseband.version} mono />
          <Row
            l="Strategy"
            v={result.baseband.patch_strategy}
            style={{ fontSize: "11px" }}
          />
        </div>
      )}

      {result && (
        <div
          className={
            result.signal_restored
              ? "pass-banner"
              : result.carrier_registered
                ? "pass-banner"
                : "warn-banner"
          }
        >
          {result.stage_message}
        </div>
      )}

      <button className="run-btn" onClick={run} disabled={loading}>
        {loading ? "⏳ Patching..." : "⚡ RUN"}
      </button>

      {result?.stage_passed && (
        <button className="next-btn" onClick={() => onPass(result)}>
          → Stage 9: Final Verification
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
  style,
}: {
  l: string
  v: string
  mono?: boolean
  cls?: string
  style?: React.CSSProperties
}) {
  return (
    <div className="info-row">
      <span className="info-label">{l}</span>
      <span className={`info-val ${mono ? "mono" : ""} ${cls || ""}`} style={style}>
        {v}
      </span>
    </div>
  )
}
