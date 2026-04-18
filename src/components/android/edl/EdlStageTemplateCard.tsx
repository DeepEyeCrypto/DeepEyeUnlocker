import { invoke } from "@tauri-apps/api/core"
import { useEffect, useMemo, useRef, useState } from "react"

import "./edlPipeline.css"
import type { EdlPipelineStageResult } from "./types"

interface EdlStageTemplateCardProps {
  stage: number
  title: string
  subtitle: string
  command: string
  serial: string
  accent: string
  onPass?: (result: EdlPipelineStageResult) => void
  onBack?: () => void
  onClose?: () => void
  isFinalStage?: boolean
}

export function EdlStageTemplateCard({
  stage,
  title,
  subtitle,
  command,
  serial,
  accent,
  onPass,
  onBack,
  onClose,
  isFinalStage = false,
}: EdlStageTemplateCardProps) {
  const [loading, setLoading] = useState(false)
  const [logs, setLogs] = useState<string[]>([])
  const [result, setResult] = useState<EdlPipelineStageResult | null>(null)
  const [error, setError] = useState<string | null>(null)
  const logRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (logRef.current) {
      logRef.current.scrollTop = logRef.current.scrollHeight
    }
  }, [logs])

  const progress = useMemo(() => Array.from({ length: 20 }, (_, index) => index + 1), [])

  async function runStage() {
    setLoading(true)
    setError(null)
    setResult(null)
    setLogs([`[stage ${stage}] starting ${title.toLowerCase()}...`])

    try {
      const payload = serial ? { serial } : { serial: null }
      const stageResult = await invoke<EdlPipelineStageResult>(command, payload)
      setResult(stageResult)
      setLogs((prev) => [
        ...prev,
        `[tool] ${stageResult.tool_name} => ${stageResult.tool_available ? "ready" : "missing"}`,
        `[firehose] ${stageResult.firehose_found ? stageResult.firehose_path : "not found"}`,
        `[result] ${stageResult.stage_message}`,
      ])
    } catch (caughtError: unknown) {
      const message = String(caughtError)
      setError(message)
      setLogs((prev) => [...prev, `[error] ${message}`])
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="edl-flow-shell">
      <div className="edl-stage-card">
        <div className="edl-stage-top-row">
          <div className="edl-stage-top-actions">
            {onClose ? (
              <button className="edl-icon-btn" onClick={onClose} type="button">
                ✕
              </button>
            ) : null}
            {onBack ? (
              <button className="edl-icon-btn" onClick={onBack} type="button">
                ←
              </button>
            ) : null}
            <span className="edl-stage-pill" style={{ background: accent }}>
              EDL {stage}/20
            </span>
          </div>

          <div className="edl-score-orb">{result?.stage_passed ? "OK" : loading ? "…" : stage}</div>
        </div>

        <div className="edl-model-line">QUALCOMM · EDL PIPELINE · STAGE {stage}</div>
        <h2 className="edl-stage-title">{title}</h2>
        <p className="edl-stage-sub">{subtitle}</p>

        <div className="edl-badge-row">
          <span className={`edl-badge ${result?.tool_available ? "safe" : "outline"}`}>
            {result?.tool_available ? `✅ ${result.tool_name}` : `⚠️ ${result?.tool_name ?? "tool"}`}
          </span>
          <span className={`edl-badge ${result?.firehose_found ? "safe" : "outline"}`}>
            {result?.firehose_found ? "✅ FIREHOSE" : "⚠️ FIREHOSE"}
          </span>
          <span className={`edl-badge ${serial ? "green" : "warn"}`}>
            {serial ? `SERIAL ${serial.slice(0, 12)}` : "USB TRANSPORT"}
          </span>
        </div>

        <div className="edl-progress-strip">
          {progress.map((item) => (
            <span
              key={item}
              className={`edl-progress-step ${item <= stage ? "active" : ""}`}
            />
          ))}
        </div>

        {logs.length > 0 ? (
          <div className="edl-console" ref={logRef}>
            {logs.map((line, index) => (
              <div
                key={`${line}-${index}`}
                className={
                  line.includes("[error]")
                    ? "log-err"
                    : line.includes("[result]")
                      ? result?.stage_passed
                        ? "log-ok"
                        : "log-warn"
                      : line.includes("[tool]") || line.includes("[firehose]")
                        ? "log-info"
                        : "edl-log-line"
                }
              >
                {line}
              </div>
            ))}
          </div>
        ) : null}

        {error ? <div className="edl-error-box">{error}</div> : null}

        {result ? (
          <>
            <div className="edl-result-grid">
              <div className="edl-info-row">
                <span className="edl-info-label">Command</span>
                <span className="edl-info-value">{command}</span>
              </div>
              <div className="edl-info-row">
                <span className="edl-info-label">Next Stage</span>
                <span className="edl-info-value">{result.next_stage_title}</span>
              </div>
              <div className="edl-info-row">
                <span className="edl-info-label">Firehose Path</span>
                <span className="edl-info-value">
                  {result.firehose_found ? result.firehose_path.split("/").pop() : "Not detected"}
                </span>
              </div>
            </div>

            <div className="edl-result-grid">
              <div className="edl-info-row">
                <span className="edl-info-label">Suggested Actions</span>
                <span className="edl-info-value" />
              </div>
              <ul className="edl-guide-list">
                {result.suggested_actions.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            </div>

            <div className={`edl-banner ${result.stage_passed ? "success" : "warn"}`}>
              {result.stage_message}
            </div>
          </>
        ) : null}

        <div className="edl-actions" style={{ marginTop: "1rem" }}>
          <button className="edl-primary-btn" disabled={loading} onClick={() => void runStage()} type="button">
            {loading ? `⏳ Running Stage ${stage}...` : `⚡ Run Stage ${stage}`}
          </button>

          {result?.stage_passed && onPass ? (
            <button
              className="edl-secondary-btn"
              onClick={() => onPass(result)}
              type="button"
            >
              → {result.next_stage_title}
            </button>
          ) : null}

          {result?.stage_passed && isFinalStage && onClose ? (
            <button className="edl-secondary-btn" onClick={onClose} type="button">
              ✓ Finish Pipeline
            </button>
          ) : null}
        </div>
      </div>
    </div>
  )
}
