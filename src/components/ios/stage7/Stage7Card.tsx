import { invoke } from "@tauri-apps/api/core"
import { listen } from "@tauri-apps/api/event"
import { useState, useEffect, useRef } from "react"
import "../stage1/Stage1Card.css"
import "../stage2/Stage2Card.css"

interface ImeiCheckResult {
  imei: string
  is_valid_format: boolean
  is_blacklisted: boolean
  tac_code: string
  manufacturer: string
  model_hint: string
  check_digit: number
  luhn_valid: boolean
}

interface Stage7Result {
  udid: string
  imei_primary: string
  imei2?: string
  meid: string
  serial_number: string
  imei_check: ImeiCheckResult
  imei_matches_device: boolean
  activation_attempted: boolean
  activation_output: string
  activation_success: boolean
  gestalt_registration: boolean
  gestalt_output: string
  lockdown_registration: boolean
  lockdown_output: string
  sim_status_after: string
  carrier_after: string
  phone_number_after: string
  imei_confirmed: string
  registration_achieved: boolean
  stage_passed: boolean
  stage_message: string
}

export function Stage7Card({
  udid,
  onPass,
  onBack,
}: {
  udid: string
  onPass: (r: Stage7Result) => void
  onBack: () => void
}) {
  const [loading, setLoading] = useState(false)
  const [logs, setLogs] = useState<string[]>([])
  const [result, setResult] = useState<Stage7Result | null>(null)
  const [error, setError] = useState<string | null>(null)
  const logRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const u = listen<string>("s7-log", (e) =>
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
      const r = await invoke<Stage7Result>("signal_stage7_imei", { udid })
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
            ← 6
          </button>
          <span className="stage-pill">Stage 7 / 10</span>
        </div>
        <div className="score-orb">99</div>
      </div>

      <div className="model-line">IMEI · LUHN · TAC · REGISTRATION</div>
      <h2 className="s1-title">IMEI Registration</h2>
      <p className="s1-sub">
        Validate · cross-check · register via activation + MobileGestalt
      </p>

      {/* Badges */}
      {result ? (
        <>
          <div className="badge-row">
            <span
              className={`bd ${result.imei_check.luhn_valid ? "green" : "orange"}`}
            >
              {result.imei_check.luhn_valid ? "✅ LUHN OK" : "⚠️ LUHN FAIL"}
            </span>
            <span
              className={`bd ${result.registration_achieved ? "safe" : "amber"}`}
            >
              {result.registration_achieved ? "REGISTERED" : "PARTIAL"}
            </span>
          </div>
          <div className="badge-row">
            <span className="bd outline">TAC: {result.imei_check.tac_code}</span>
            <span className="bd outline">{result.imei_check.manufacturer}</span>
            {result.imei2 && <span className="bd amber">DUAL SIM</span>}
          </div>
        </>
      ) : (
        <>
          <div className="badge-row">
            <span className="bd outline">LUHN CHECK</span>
            <span className="bd outline">TAC LOOKUP</span>
            <span className="bd outline">IMEI</span>
          </div>
          <div className="badge-row">
            <span className="bd outline">ACTIVATION</span>
            <span className="bd outline">GESTALT REG</span>
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
                l.includes("registered") ||
                l.startsWith("✅")
                  ? "log-ok"
                  : l.startsWith("❌") || l.includes("BLOCKED")
                    ? "log-err"
                    : l.startsWith("⚠️") || l.includes("PARTIAL")
                      ? "log-warn"
                      : l.startsWith("📡") ||
                          l.startsWith("🔢") ||
                          l.startsWith("🔍") ||
                          l.startsWith("🍎") ||
                          l.startsWith("📱") ||
                          l.startsWith("🔐") ||
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

      {/* IMEI validation card */}
      {result && (
        <div className="result-grid">
          <Row l="IMEI" v={result.imei_primary} mono />
          {result.imei2 && <Row l="IMEI 2" v={result.imei2} mono />}
          <Row
            l="Luhn"
            v={result.imei_check.luhn_valid ? "Valid ✅" : "Invalid ⚠️"}
            cls={result.imei_check.luhn_valid ? "v-ok" : "v-warn"}
          />
          <Row l="TAC" v={result.imei_check.tac_code} mono />
          <Row l="Manufacturer" v={result.imei_check.manufacturer} />
          <Row l="Check Digit" v={String(result.imei_check.check_digit)} />
          <Row
            l="Activation"
            v={result.activation_success ? "Success ✅" : "Attempted ⚠️"}
            cls={result.activation_success ? "v-ok" : "v-warn"}
          />
          <Row
            l="Gestalt Reg"
            v={result.gestalt_registration ? "Yes ✅" : "Partial ⚠️"}
            cls={result.gestalt_registration ? "v-ok" : "v-warn"}
          />
          <Row l="Confirmed" v={result.imei_confirmed} mono />
          <Row l="Carrier" v={result.carrier_after} />
          <Row l="Phone" v={result.phone_number_after} />
          <Row
            l="Registered"
            v={result.registration_achieved ? "Yes ✅" : "Stage 8 needed ⚠️"}
            cls={result.registration_achieved ? "v-ok" : "v-warn"}
          />
        </div>
      )}

      {result && (
        <div className={result.stage_passed ? "pass-banner" : "fail-banner"}>
          {result.stage_message}
        </div>
      )}

      <button className="run-btn" onClick={run} disabled={loading}>
        {loading ? "⏳ Registering..." : "⚡ RUN"}
      </button>

      {result?.stage_passed && (
        <button className="next-btn" onClick={() => onPass(result)}>
          → Stage 8: Signal Restore
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
