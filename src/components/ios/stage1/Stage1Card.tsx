import { invoke } from "@tauri-apps/api/core"
import { listen } from "@tauri-apps/api/event"
import { useState, useEffect, useRef } from "react"
import { SignalBypassFlow } from "../SignalBypassFlow"
import "./Stage1Card.css"

interface AutoRunProps {
  isRunningAll: boolean
  autoMode: boolean
  currentStage: number
  logs: string[]
  finalResult: boolean
  stageResults: Record<number, any>
  runAllStagesAuto: () => Promise<void>
}

interface Stage1Result {
  udid: string
  model_name: string
  model_id: string
  ios_version: string
  build_version: string
  imei: string
  imei2?: string
  meid: string
  serial_number: string
  ecid: string
  chip: string
  is_a12_plus: boolean
  iccid: string
  sim_status_raw: string
  carrier_raw: string
  battery_level: string
  storage_total: string
  wifi_mac: string
  stage_passed: boolean
  stage_message: string
}

export function Stage1Card({
  onPass,
  onClose,
  autoRunProps,
}: {
  onPass: (r: Stage1Result) => void
  onClose?: () => void
  autoRunProps?: AutoRunProps
}) {
  const [loading, setLoading] = useState(false)
  const [logs, setLogs] = useState<string[]>([])
  const [result, setResult] = useState<Stage1Result | null>(null)
  const [error, setError] = useState<string | null>(null)
  const logRef = useRef<HTMLDivElement>(null)
  const [flowOpen, setFlowOpen] = useState(false)
  const isRunningAll = autoRunProps?.isRunningAll || false
  const currentStage = autoRunProps?.currentStage || 1
  const autoLogs = autoRunProps?.logs || []
  const finalResult = autoRunProps?.finalResult || false
  const runAllStagesAuto = autoRunProps?.runAllStagesAuto || (() => Promise.resolve())

  useEffect(() => {
    const u = listen<string>("s1-log", (e) =>
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
      const r = await invoke<Stage1Result>("signal_stage1_detect")
      setResult(r)
    } catch (e) {
      setError(String(e))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="s1-card">
      {/* Stage pill */}
      <div className="s1-top-row">
        <span className="stage-pill">Stage 1 / 10</span>
        <div className="score-orb">99</div>
      </div>

      {/* Model line */}
      <div className="model-line">A12–A18 (XR–16 PRO MAX)</div>

      <h2 className="s1-title">A12+ Bypass — Full Signal</h2>

      <p className="s1-sub">
        Full SIM/calls on A12+. IMEI registration.
      </p>

      {/* Badge rows — exact match screenshot */}
      <div className="badge-row">
        <span className="bd green">● SIGNAL+</span>
        <span className="bd outline">USB</span>
        <span className="bd amber">35¢</span>
      </div>
      <div className="badge-row">
        <span className="bd outline">IMEI Registration</span>
        <span className="bd orange">UNTH</span>
        <span className="bd safe">SAFE</span>
      </div>

      {/* Log console */}
      {logs.length > 0 && (
        <div className="log-console" ref={logRef}>
          {logs.map((l, i) => (
            <div
              key={i}
              className={
                l.startsWith("✅") || l.startsWith("╔")
                  ? "log-ok"
                  : l.startsWith("❌") || l.startsWith("⛔")
                    ? "log-err"
                    : l.startsWith("📡") || l.startsWith("📱")
                      ? "log-info"
                      : "log-line"
              }
            >
              {l}
            </div>
          ))}
        </div>
      )}

      {/* Error box */}
      {error && (
        <div className="err-box">
          {error.split("\n").map((l, i) => (
            <div key={i}>{l}</div>
          ))}
        </div>
      )}

      {/* Result info grid */}
      {result && (
        <div className="result-grid">
          <Row
            l="Model"
            v={`${result.model_name}`}
            sub={result.model_id}
          />
          <Row
            l="iOS"
            v={result.ios_version}
            sub={result.build_version}
          />
          <Row
            l="Chip"
            v={`${result.chip} ${result.is_a12_plus ? "✅" : "⛔"}`}
            cls={result.is_a12_plus ? "v-ok" : "v-err"}
          />
          <Row l="IMEI" v={result.imei} mono />
          {result.imei2 && <Row l="IMEI 2" v={result.imei2} mono />}
          <Row l="ECID" v={result.ecid} mono />
          <Row l="Serial" v={result.serial_number} />
          <Row
            l="SIM"
            v={result.sim_status_raw}
            cls={
              result.sim_status_raw.includes("Ready")
                ? "v-ok"
                : "v-warn"
            }
          />
          <Row l="Carrier" v={result.carrier_raw} />
          <Row l="Battery" v={result.battery_level} />
          <Row l="Storage" v={result.storage_total} />
        </div>
      )}

      {/* Stage result banner */}
      {result && (
        <div
          className={
            result.stage_passed ? "pass-banner" : "fail-banner"
          }
        >
          {result.stage_message}
        </div>
      )}

      {/* AUTO RUN ALL button */}
      {!onClose && !flowOpen && (
        <>
          {/* Stage progress indicator */}
          {isRunningAll && (
            <div className="stages-track">
              {Array.from({ length: 10 }, (_, i) => (
                <div
                  key={i}
                  className={`stage-dot ${
                    i + 1 < currentStage ? "done" :
                    i + 1 === currentStage ? "active" :
                    "pending"
                  }`}
                  title={`Stage ${i + 1}`}
                >
                  {i + 1 < currentStage ? "✓" : i + 1}
                </div>
              ))}
            </div>
          )}

          <button
            className="btn-auto-run-all"
            onClick={runAllStagesAuto}
            disabled={isRunningAll}
          >
            {isRunningAll
              ? `⚡ Running Stage ${currentStage}/10...`
              : "⚡ AUTO RUN ALL (10 Stages)"}
          </button>

          {/* Progress bar */}
          {isRunningAll && (
            <div className="progress-bar-bg">
              <div
                className="progress-bar-fill"
                style={{ width: `${(currentStage / 10) * 100}%` }}
              />
            </div>
          )}

          {/* Auto-run logs */}
          {autoLogs.length > 0 && (
            <div className="log-console" ref={logRef}>
              {autoLogs.map((l, i) => (
                <div
                  key={i}
                  className={
                    l.includes("✅") || l.startsWith("🚀") || l.includes("═")
                      ? "log-ok"
                      : l.includes("❌") || l.includes("⛔")
                        ? "log-err"
                        : l.includes("⚡") || l.includes("🎯")
                          ? "log-info"
                          : "log-line"
                  }
                >
                  {l}
                </div>
              ))}
            </div>
          )}

          {/* Final result banner */}
          {finalResult && (
            <div className="pass-banner">
              🎯 All 10 stages complete! Check results above.
            </div>
          )}
        </>
      )}

      {/* RUN button */}
      {!onClose ? (
        flowOpen ? (
          <SignalBypassFlow
            onClose={() => setFlowOpen(false)}
          />
        ) : (
          <button className="run-btn" onClick={() => setFlowOpen(true)}>
            ⚡ RUN
          </button>
        )
      ) : (
        <button className="run-btn" onClick={run} disabled={loading}>
          {loading ? "⏳ Scanning..." : "⚡ RUN"}
        </button>
      )}

      {/* Next stage button */}
      {result?.stage_passed && (
        <button className="next-btn" onClick={() => onPass(result)}>
          → Stage 2: Activation Check
        </button>
      )}
    </div>
  )
}

function Row({
  l,
  v,
  sub,
  mono,
  cls,
}: {
  l: string
  v: string
  sub?: string
  mono?: boolean
  cls?: string
}) {
  return (
    <div className="info-row">
      <span className="info-label">{l}</span>
      <span className={`info-val ${mono ? "mono" : ""} ${cls || ""}`}>
        {v}
        {sub && <span className="info-sub">{sub}</span>}
      </span>
    </div>
  )
}
