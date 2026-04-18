import { invoke } from "@tauri-apps/api/core"
import { listen } from "@tauri-apps/api/event"
import { useState, useEffect, useRef } from "react"
import "../stage1/Stage1Card.css"
import "../stage2/Stage2Card.css"

interface MdmProfile {
  id: string
  name: string
  org: string
  profile_type: string
  is_removable: boolean
  removed: boolean
}

interface Stage5Result {
  udid: string
  is_supervised: boolean
  supervised_by: string
  profiles_found: MdmProfile[]
  profile_count: number
  removed_count: number
  failed_count: number
  mdm_locked: boolean
  dep_enrolled: boolean
  abm_enrolled: boolean
  carrier_profiles_removed: number
  restrictions_removed: boolean
  provision_output: string
  provision_tool_available: boolean
  stage_passed: boolean
  stage_message: string
}

export function Stage5Card({
  udid,
  onPass,
  onBack,
}: {
  udid: string
  onPass: (r: Stage5Result) => void
  onBack: () => void
}) {
  const [loading, setLoading] = useState(false)
  const [logs, setLogs] = useState<string[]>([])
  const [result, setResult] = useState<Stage5Result | null>(null)
  const [error, setError] = useState<string | null>(null)
  const logRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const u = listen<string>("s5-log", (e) =>
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
      const r = await invoke<Stage5Result>("signal_stage5_mdm", { udid })
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
            ← 4
          </button>
          <span className="stage-pill">Stage 5 / 10</span>
        </div>
        <div className="score-orb">99</div>
      </div>

      <div className="model-line">MDM · DEP · SUPERVISION · PROFILES</div>
      <h2 className="s1-title">MDM Profile Removal</h2>
      <p className="s1-sub">Remove carrier + MDM profiles blocking signal</p>

      {/* Badges */}
      {result ? (
        <>
          <div className="badge-row">
            <span
              className={`bd ${result.is_supervised ? "orange" : "green"}`}
            >
              {result.is_supervised ? "⚠️ SUPERVISED" : "✅ NOT SUPERVISED"}
            </span>
            <span className={`bd ${result.mdm_locked ? "orange" : "safe"}`}>
              {result.mdm_locked ? "DEP LOCKED" : "DEP CLEAR"}
            </span>
          </div>
          <div className="badge-row">
            <span className="bd outline">{result.profile_count} PROFILES</span>
            <span className="bd green">✅ {result.removed_count} REMOVED</span>
            {result.failed_count > 0 && (
              <span className="bd orange">⚠️ {result.failed_count} FAILED</span>
            )}
          </div>
        </>
      ) : (
        <>
          <div className="badge-row">
            <span className="bd outline">MDM CHECK</span>
            <span className="bd outline">DEP/ABM</span>
            <span className="bd outline">PROFILES</span>
          </div>
          <div className="badge-row">
            <span className="bd outline">SUPERVISION</span>
            <span className="bd outline">CARRIER PROFILES</span>
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
                  : l.startsWith("❌") || l.includes("BLOCKED")
                    ? "log-err"
                    : l.startsWith("⚠️")
                      ? "log-warn"
                      : l.startsWith("🏢") ||
                          l.startsWith("📋") ||
                          l.startsWith("🗑️") ||
                          l.startsWith("🔒") ||
                          l.startsWith("🚫")
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

      {/* Profiles list */}
      {result && result.profiles_found.length > 0 && (
        <div className="result-grid">
          <div className="info-row">
            <span
              className="info-label"
              style={{ color: "var(--color,#a78bfa)", fontWeight: 600 }}
            >
              Profiles Found
            </span>
          </div>
          {result.profiles_found.map((p, i) => (
            <div key={i} className="info-row">
              <span className="info-label">{p.profile_type}</span>
              <span className={`info-val ${p.removed ? "v-ok" : "v-warn"}`}>
                {p.name}
                {p.removed ? " ✅" : " ⚠️"}
              </span>
            </div>
          ))}
        </div>
      )}

      {/* Stats grid */}
      {result && (
        <div className="result-grid" style={{ marginTop: 8 }}>
          <Row
            l="Supervised"
            v={result.is_supervised ? result.supervised_by : "No"}
            cls={result.is_supervised ? "v-warn" : "v-ok"}
          />
          <Row
            l="DEP"
            v={result.dep_enrolled ? "Enrolled ⚠️" : "Clear ✅"}
            cls={result.dep_enrolled ? "v-warn" : "v-ok"}
          />
          <Row
            l="ABM"
            v={result.abm_enrolled ? "Enrolled ⚠️" : "Clear ✅"}
            cls={result.abm_enrolled ? "v-warn" : "v-ok"}
          />
          <Row
            l="Profiles"
            v={`${result.profile_count} found, ${result.removed_count} removed`}
          />
          <Row
            l="Carrier"
            v={result.restrictions_removed ? "Cleared ✅" : "Stage 6 needed ⚠️"}
            cls={result.restrictions_removed ? "v-ok" : "v-warn"}
          />
          <Row
            l="MDM Lock"
            v={result.mdm_locked ? "Persistent ⚠️" : "None ✅"}
            cls={result.mdm_locked ? "v-warn" : "v-ok"}
          />
        </div>
      )}

      {result && (
        <div className={result.stage_passed ? "pass-banner" : "fail-banner"}>
          {result.stage_message}
        </div>
      )}

      <button className="run-btn" onClick={run} disabled={loading}>
        {loading ? "⏳ Removing..." : "⚡ RUN"}
      </button>

      {result?.stage_passed && (
        <button className="next-btn" onClick={() => onPass(result)}>
          → Stage 6: Carrier Restriction Bypass
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
