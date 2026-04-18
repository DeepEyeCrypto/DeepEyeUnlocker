import { invoke } from "@tauri-apps/api/core"
import { listen } from "@tauri-apps/api/event"
import { useState, useEffect, useRef } from "react"
import "../stage1/Stage1Card.css"
import "../stage2/Stage2Card.css"

interface PersistenceCheck {
  label: string
  value: string
  persisted: boolean
}

interface BypassReport {
  udid: string
  serial: string
  product_type: string
  ios_version: string
  model_name: string
  color: string
  capacity: string
  carrier: string
  sim_status: string
  phone_number: string
  imei: string
  iccid: string
  mcc: string
  mnc: string
  baseband_version: string
  activation_state: string
  persistence: PersistenceCheck[]
  persistence_score: number
  bypass_score: number
  bypass_grade: string
  signal_restored: boolean
  sim_ready: boolean
  calls_capable: boolean
  data_capable: boolean
  completed_at: number
  report_id: string
  stages_summary: string[]
  stage_passed: boolean
  completion_message: string
}

export function Stage10Card({
  udid,
  stage9Score,
  onBack,
}: {
  udid: string
  stage9Score: number
  onBack: () => void
}) {
  const [loading, setLoading] = useState(false)
  const [logs, setLogs] = useState<string[]>([])
  const [report, setReport] = useState<BypassReport | null>(null)
  const [error, setError] = useState<string | null>(null)
  const logRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const u = listen<string>("s10-log", (e) =>
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
    setReport(null)
    try {
      const r = await invoke<BypassReport>("signal_stage10_complete", {
        udid,
        stage9Score,
      })
      setReport(r)
    } catch (e) {
      setError(String(e))
    } finally {
      setLoading(false)
    }
  }

  const scoreColor = (s: number) =>
    s >= 90
      ? "#22c55e"
      : s >= 75
        ? "#a78bfa"
        : s >= 60
          ? "#fb923c"
          : "#ef4444"

  const fmtDate = (ts: number) => {
    if (!ts) return "N/A"
    return new Date(ts * 1000).toLocaleString("en-IN", {
      day: "2-digit",
      month: "short",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    })
  }

  return (
    <div className="s1-card">
      <div className="s1-top-row">
        <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
          <button className="back-btn" onClick={onBack}>
            ← 9
          </button>
          <span
            className="stage-pill"
            style={{
              background: "linear-gradient(90deg, #7c3aed, #a78bfa)",
            }}
          >
            Stage 10 / 10 🏁
          </span>
        </div>
        <div
          className="score-orb"
          style={{
            background: report ? scoreColor(report.bypass_score) : undefined,
          }}
        >
          {report ? report.bypass_score : "99"}
        </div>
      </div>

      <div className="model-line">PERSISTENCE · FINAL REPORT · COMPLETE</div>
      <h2 className="s1-title">Completion Report</h2>
      <p className="s1-sub">
        Persistence verification · final score · 10-stage summary report
      </p>

      {/* Top badges */}
      {report ? (
        <>
          <div className="badge-row">
            <span
              className={`bd ${
                report.bypass_score >= 90
                  ? "green"
                  : report.bypass_score >= 75
                    ? "safe"
                    : "orange"
              }`}
            >
              🏆 Grade {report.bypass_grade}
            </span>
            <span
              className={`bd ${report.bypass_score >= 75 ? "green" : "orange"}`}
            >
              {report.bypass_score}/100
            </span>
          </div>
          <div className="badge-row">
            <span
              className={`bd ${report.signal_restored ? "green" : "outline"}`}
            >
              {report.signal_restored ? "● SIGNAL" : "○ NO SIGNAL"}
            </span>
            <span
              className={`bd ${report.calls_capable ? "green" : "outline"}`}
            >
              {report.calls_capable ? "📞 CALLS" : "📞 PENDING"}
            </span>
            <span
              className={`bd ${report.data_capable ? "green" : "outline"}`}
            >
              {report.data_capable ? "📶 DATA" : "📶 PENDING"}
            </span>
          </div>
        </>
      ) : (
        <>
          <div className="badge-row">
            <span className="bd outline">PERSISTENCE CHECK</span>
            <span className="bd outline">FINAL SCORE</span>
          </div>
          <div className="badge-row">
            <span className="bd outline">REPORT GENERATION</span>
            <span className="bd outline">10-STAGE SUMMARY</span>
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
                l.includes("COMPLETE") ||
                l.includes("BYPASS COMPLETE") ||
                l.startsWith("✅") ||
                l.includes("🏆")
                  ? "log-ok"
                  : l.startsWith("❌")
                    ? "log-err"
                    : l.startsWith("⚠️")
                      ? "log-warn"
                      : l.startsWith("📱") ||
                          l.startsWith("🔒") ||
                          l.startsWith("🔑") ||
                          l.startsWith("📊") ||
                          l.startsWith("🆔") ||
                          l.startsWith("📡")
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

      {/* Persistence */}
      {report && (
        <div className="result-grid">
          <div className="info-row">
            <span
              className="info-label"
              style={{ color: "#a78bfa", fontWeight: 600 }}
            >
              Persistence
            </span>
            <span className="info-val" style={{ color: "#a78bfa" }}>
              {report.persistence_score}/100
            </span>
          </div>
          {report.persistence.map((p, i) => (
            <div key={i} className="info-row">
              <span className="info-label">{p.label}</span>
              <span
                className={`info-val ${p.persisted ? "v-ok" : "v-warn"}`}
                style={{ fontSize: "11px" }}
              >
                {p.persisted ? "✅ Stable" : "⚠️"}{" "}
                {p.value.length > 18 ? p.value.slice(0, 18) + "…" : p.value}
              </span>
            </div>
          ))}
        </div>
      )}

      {/* Device info */}
      {report && (
        <div className="result-grid" style={{ marginTop: 8 }}>
          <div className="info-row">
            <span
              className="info-label"
              style={{ color: "#a78bfa", fontWeight: 600 }}
            >
              Device Report
            </span>
            <span
              className="info-val"
              style={{ color: "#6b7280", fontSize: "10px" }}
            >
              {report.report_id}
            </span>
          </div>
          <Row l="Device" v={`${report.product_type} ${report.ios_version}`} />
          <Row l="Serial" v={report.serial} mono />
          <Row
            l="Carrier"
            v={report.carrier}
            cls={report.signal_restored ? "v-ok" : "v-warn"}
          />
          <Row l="Phone" v={report.phone_number} />
          <Row
            l="SIM"
            v={simpleSim(report.sim_status)}
            cls={report.sim_ready ? "v-ok" : "v-warn"}
          />
          <Row l="IMEI" v={report.imei} mono />
          <Row
            l="ICCID"
            v={
              report.iccid.length > 10
                ? report.iccid.slice(0, 10) + "****"
                : report.iccid
            }
            mono
          />
          <Row l="MCC/MNC" v={`${report.mcc}/${report.mnc}`} />
          <Row l="Baseband" v={report.baseband_version} mono />
          <Row
            l="Activation"
            v={report.activation_state}
            cls={
              report.activation_state.includes("Activated")
                ? "v-ok"
                : "v-warn"
            }
          />
          <Row l="Completed" v={fmtDate(report.completed_at)} />
        </div>
      )}

      {/* 10-Stage summary */}
      {report && (
        <div className="result-grid" style={{ marginTop: 8 }}>
          <div className="info-row">
            <span
              className="info-label"
              style={{ color: "#a78bfa", fontWeight: 600 }}
            >
              Pipeline Summary
            </span>
          </div>
          {report.stages_summary.map((s, i) => (
            <div key={i} className="info-row">
              <span
                className="info-val v-ok"
                style={{ fontSize: "11px", fontFamily: "monospace" }}
              >
                {s}
              </span>
            </div>
          ))}
        </div>
      )}

      {/* Completion banner */}
      {report && (
        <div
          className={
            report.bypass_score >= 75 ? "pass-banner" : "warn-banner"
          }
          style={{
            whiteSpace: "pre-wrap",
            fontSize: "12px",
            lineHeight: "1.7",
          }}
        >
          {report.completion_message}
        </div>
      )}

      <button
        className="run-btn"
        onClick={run}
        disabled={loading}
        style={{ background: "linear-gradient(90deg, #7c3aed, #a78bfa)" }}
      >
        {loading ? "⏳ Generating Report..." : "⚡ COMPLETE BYPASS"}
      </button>
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
